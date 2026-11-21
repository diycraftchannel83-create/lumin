#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <string>
#include <vector>
#include <cstring>
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
    if (!sourceArgb || width <= 0 || height <= 0 || (scale != 2 && scale != 4) ||\n        (int64_t)width * height > 12000000LL || (int64_t)width * height * scale * scale > 24000000LL) {
        return nullptr;
    }
    const jsize sourceCount = env->GetArrayLength(sourceArgb);
    if (sourceCount != width * height) return nullptr;

    std::vector<jint> argb(sourceCount);
    env->GetIntArrayRegion(sourceArgb, 0, sourceCount, argb.data());
    if (env->ExceptionCheck()) return nullptr;

    ncnn::Mat source(width, height, 3, (size_t)1u);
    for (int y = 0; y < height; ++y) {
        unsigned char* rowR = source.channel(0) + y * source.w;
        unsigned char* rowG = source.channel(1) + y * source.w;
        unsigned char* rowB = source.channel(2) + y * source.w;
        for (int x = 0; x < width; ++x) {
            const uint32_t pixel = static_cast<uint32_t>(argb[y * width + x]);
            rowR[x] = (pixel >> 16) & 0xff;
            rowG[x] = (pixel >> 8) & 0xff;
            rowB[x] = pixel & 0xff;
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
    ncnn::Mat output(width * scale, height * scale, 3, (size_t)1u);

    for (int y = 0; y < height; y += tile) {
        for (int x = 0; x < width; x += tile) {
            const int tileW = std::min(tile, width - x);
            const int tileH = std::min(tile, height - y);
            ncnn::Mat core = source.crop(x, y, tileW, tileH);
            ncnn::Mat padded;
            ncnn::copy_make_border(core, padded, padding, padding, padding, padding, ncnn::BORDER_REPLICATE);

            ncnn::Extractor extractor = net.create_extractor();
            extractor.set_light_mode(true);
            extractor.set_num_threads(2);
            if (extractor.input("data", padded) != 0) {
                LOGE("Failed to submit input tile");
                net.clear();

                if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
                return nullptr;
            }
            ncnn::Mat tileOutput;
            if (extractor.extract("output", tileOutput) != 0 || tileOutput.empty()) {
                LOGE("Failed to infer tile");
                net.clear();

                if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
                return nullptr;
            }

            const int outX = x * scale;
            const int outY = y * scale;
            const int coreW = tileW * scale;
            const int coreH = tileH * scale;
            const int cropPad = padding * scale;
            if (tileOutput.w < coreW + cropPad * 2 || tileOutput.h < coreH + cropPad * 2) {
                LOGE("Unexpected model output shape: %dx%d", tileOutput.w, tileOutput.h);
                net.clear();

                if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
                return nullptr;
            }
            for (int oy = 0; oy < coreH; ++oy) {
                const int srcY = oy + cropPad;
                const int dstY = outY + oy;
                for (int ox = 0; ox < coreW; ++ox) {
                    const int srcX = ox + cropPad;
                    const int dstX = outX + ox;
                    for (int c = 0; c < 3; ++c) {
                        output.channel(c)[dstY * output.w + dstX] =
                            tileOutput.channel(c)[srcY * tileOutput.w + srcX];
                    }
                }
            }
        }
    }

    net.clear();
    if (useVulkan == JNI_TRUE) ncnn::destroy_gpu_instance();
    std::vector<unsigned char> rgb((size_t)output.w * output.h * 3);
    ncnn::Mat packed = output;
    packed.to_pixels(rgb.data(), ncnn::Mat::PIXEL_RGB);
    std::vector<jint> result((size_t)output.w * output.h);
    for (size_t i = 0; i < result.size(); ++i) {
        const unsigned int r = rgb[i * 3];
        const unsigned int g = rgb[i * 3 + 1];
        const unsigned int b = rgb[i * 3 + 2];
        result[i] = static_cast<jint>(0xff000000u | (r << 16) | (g << 8) | b);
    }
    jintArray outArray = env->NewIntArray(static_cast<jsize>(result.size()));
    if (!outArray) return nullptr;
    env->SetIntArrayRegion(outArray, 0, static_cast<jsize>(result.size()), result.data());
    return outArray;
}
