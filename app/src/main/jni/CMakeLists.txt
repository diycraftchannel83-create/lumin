cmake_minimum_required(VERSION 3.22.1)
project(PhotoStudioRealESRGAN)

set(ncnn_DIR "${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}/lib/cmake/ncnn")
find_package(ncnn REQUIRED)

add_library(photostudio_realesrgan SHARED realesrgan_jni.cpp)
target_compile_features(photostudio_realesrgan PRIVATE cxx_std_17)
target_compile_options(photostudio_realesrgan PRIVATE -O3 -fno-exceptions)
target_link_libraries(photostudio_realesrgan PRIVATE ncnn android log)
