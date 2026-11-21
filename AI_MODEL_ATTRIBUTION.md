# AI Model and Native Engine Attribution

## Real-ESRGAN

Real-ESRGAN is developed by Xintao Wang, Liangbin Xie, Chao Dong, and Ying Shan.
Paper: *Real-ESRGAN: Training Real-World Blind Super-Resolution with Pure Synthetic Data* (ICCV Workshops 2021).

- Original project: https://github.com/xinntao/Real-ESRGAN
- Original project license: BSD 3-Clause (verify the upstream license file before redistribution)
- NCNN Vulkan implementation: Tencent NCNN and the Real-ESRGAN ncnn-vulkan implementation by nihui

## Runtime model mirrors

PhotoStudio downloads model weights at runtime; they are not bundled in the APK.

- Photo x4plus and AnimeVideo v3 x2 NCNN model files: https://huggingface.co/edgetools/realesrgan
  - Mirror description states these files mirror the upstream NCNN model files byte-for-byte and are distributed under BSD-3-Clause.
- Anime x4 model pair: https://huggingface.co/spaces/KudoSin/AyayaUpscale
  - Model mirror files are used as NCNN weights; confirm the original model provenance/license and verify hashes before production distribution.

## NCNN

- Project: https://github.com/Tencent/ncnn
- License: BSD 3-Clause
- Android Vulkan prebuilt SDK is downloaded by the manual Debug APK workflow at build time.

## Important release checks

- [ ] Verify each model file SHA-256 from trusted upstream metadata before download/install.
- [ ] Review license/attribution for every model mirror and weight file.
- [ ] Include applicable third-party notices in the final app.
- [ ] Verify 16 KB page-size compatibility of native libraries for current Android requirements.
- [ ] Verify Vulkan inference on multiple GPU vendors and CPU fallback on unsupported devices.
