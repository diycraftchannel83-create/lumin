#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <string>
#include <vector>
#include <cstring>
#include <cstdint>
#include "net.h"
#include "gpu.h"

#define LOG_TAG "PhotoStudioRealESRGAN"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::string jstringToString(JNIEnv* env, jstring value) {
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_ai_RealEsrganNative_nativeUpscale(
        JNIEnv* env, jobject /*thiz*/, jintArray sourceArgb,
        jint width, jint height, jstring paramPath, jstring modelPath,
        jint scale, jint tileSize, jboolean useVulkan) {
    if (!sourceArgb || width <= 0 || height <= 0 || width > 8192 || height > 8192 || (scale != 2 && scale != 4) ||
        (int64_t)width * height > 3000000LL || (int64_t)width * height * scale * scale > 8000000LL) {
        return nullptr;
    }
    const jsize sourceCount = env->GetArrayLength(sourceArgb);
    if (sourceCount != width * height) return nullptr;

    std::vector<jint> argb(sourceCount);
    env->GetIntArrayRegion(sourceArgb, 0, sourceCount, argb.data());
    if (env->ExceptionCheck()) return nullptr;

    ncnn::Mat source(width, height, 3);
    for (int y = 0; y < height; ++y) {
        float* rowR = source.channel(0).row<float>(y);
        float* rowG = source.channel(1).row<float>(y);
        float* rowB = source.channel(2).row<float>(y);
        for (int x = 0; x < width; ++x) {
            const uint32_t pixel = static_cast<uint32_t>(argb[y * width + x]);
            rowR[x] = ((pixel >> 16) & 0xff) / 255.f;
            rowG[x] = ((pixel >> 8) & 0xff) / 255.f;
            rowB[x] = (pixel & 0xff) / 255.f;
        }
    }

    if (useVulkan == JNI_TRUE) ncnn::create_gpu_instance();
    const bool gpuAvailable = useVulkan == JNI_TRUE && ncnn::get_gpu_count() > 0;
    ncnn::Net net;
    net.opt.use_vulkan_compute = gpuAvailable;
    net.opt.use_fp16_storage = gpuAvailable;
    net.opt.use_fp16_arithmetic = gpuAvailable;
    net.opt.use_fp16_packed = gpuAvailable;
    const std::string param = jstringToString(env, paramPath);
    const std::string model = jstringToString(env, modelPath);
    if (net.load_param(param.c_str()) != 0 || net.load_model(model.c_str()) != 0) {
        LOGE("Failed to load model pair");
        net.clear();

        if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
        return nullptr;
    }

    const int tile = tileSize >= 64 ? tileSize : 256;
    const int padding = 10;
    ncnn::Mat output(width * scale, height * scale, 3);

    for (int y = 0; y < height; y += tile) {
        for (int x = 0; x < width; x += tile) {
            const int tileW = std::min(tile, width - x);
            const int tileH = std::min(tile, height - y);

            // Include real neighboring pixels around each tile to reduce stitching seams.
            const int regionX0 = std::max(0, x - padding);
            const int regionY0 = std::max(0, y - padding);
            const int regionX1 = std::min(width, x + tileW + padding);
            const int regionY1 = std::min(height, y + tileH + padding);
            const int borderLeft = regionX0 == 0 ? padding : 0;
            const int borderTop = regionY0 == 0 ? padding : 0;
            const int borderRight = regionX1 == width ? padding : 0;
            const int borderBottom = regionY1 == height ? padding : 0;
            ncnn::Mat region = source.crop(regionX0, regionY0, regionX1 - regionX0, regionY1 - regionY0);
            ncnn::Mat padded;
            if (borderLeft || borderTop || borderRight || borderBottom) {
                ncnn::copy_make_border(region, padded, borderTop, borderBottom, borderLeft, borderRight, ncnn::BORDER_REPLICATE);
            } else {
                padded = region;
            }

            ncnn::Extractor extractor = net.create_extractor();
            extractor.set_light_mode(true);
            extractor.set_num_threads(2);
            if (extractor.input("data", padded) != 0) {
                LOGE("Failed to submit input tile");
                return nullptr;
            }
            ncnn::Mat tileOutput;
            if (extractor.extract("output", tileOutput) != 0 || tileOutput.empty()) {
                LOGE("Failed to infer tile");
                net.clear();
                if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
                return nullptr;
            }

            const int srcOffsetX = (x - regionX0 + borderLeft) * scale;
            const int srcOffsetY = (y - regionY0 + borderTop) * scale;
            const int outX = x * scale;
            const int outY = y * scale;
            const int coreW = tileW * scale;
            const int coreH = tileH * scale;
            if (tileOutput.w < srcOffsetX + coreW || tileOutput.h < srcOffsetY + coreH) {
                LOGE("Unexpected model output shape: %dx%d", tileOutput.w, tileOutput.h);
                net.clear();
                if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
                return nullptr;
            }
            for (int oy = 0; oy < coreH; ++oy) {
                const int srcY = srcOffsetY + oy;
                const int dstY = outY + oy;
                for (int ox = 0; ox < coreW; ++ox) {
                    const int srcX = srcOffsetX + ox;
                    const int dstX = outX + ox;
                    for (int c = 0; c < 3; ++c) {
                        output.channel(c).row<float>(dstY)[dstX] =
                            tileOutput.channel(c).row<float>(srcY)[srcX];
                    }
                }
            }
        }
    }

    net.clear();
    if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
    std::vector<jint> result((size_t)output.w * output.h);
    for (int y = 0; y < output.h; ++y) {
        const float* rowR = output.channel(0).row<float>(y);
        const float* rowG = output.channel(1).row<float>(y);
        const float* rowB = output.channel(2).row<float>(y);
        for (int x = 0; x < output.w; ++x) {
            const unsigned int r = static_cast<unsigned int>(std::clamp(rowR[x] * 255.f, 0.f, 255.f));
            const unsigned int g = static_cast<unsigned int>(std::clamp(rowG[x] * 255.f, 0.f, 255.f));
            const unsigned int b = static_cast<unsigned int>(std::clamp(rowB[x] * 255.f, 0.f, 255.f));
            result[(size_t)y * output.w + x] = static_cast<jint>(0xff000000u | (r << 16) | (g << 8) | b);
        }
    }
    jintArray outArray = env->NewIntArray(static_cast<jsize>(result.size()));
    if (!outArray) return nullptr;
    env->SetIntArrayRegion(outArray, 0, static_cast<jsize>(result.size()), result.data());
    return outArray;
}
