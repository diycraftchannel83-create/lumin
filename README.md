# PhotoStudio — Production Roadmap

> **Project direction:** Rebrand the current Lumin app into **PhotoStudio**, then improve and validate it systematically toward a reliable production release.
>
> This README is the master checklist. Update the checkboxes and notes after each implementation milestone. Do not mark a task complete until code changes are committed and relevant tests/verification are documented.

## Product Vision

**PhotoStudio** is a modern mobile photo-editing studio with professional editing controls, creative tools, and AI-assisted workflows.

### Brand direction (working proposal)
- **App name:** PhotoStudio
- **Product description:** AI Photo Editor & Creative Studio
- **Tagline options:** *Create. Edit. Inspire.* / *Your photos, your studio.*
- **Visual direction:** premium, modern, photography-first; dark studio interface with luminous accent color.
- **Working palette:** Studio Black `#101114`, Surface `#1B1B20`, Luminous Lime `#C6F36B`, Warm White `#F4F3EF`, Muted Gray `#909098`.
- **Logo concept:** simple camera/aperture or light symbol; readable at launcher-icon scale.
- Brand name, trademark, domain, social handle, and Google Play listing availability must be checked before finalizing.

## Roadmap Status

- [x] Phase 0 — Brand, naming & product foundation (initial rebrand pass; remaining tasks below)
- [ ] Phase 1 — Android release configuration and security blockers
- [ ] Phase 2 — Core image engine stability
- [ ] Phase 3 — Essential editing workflow and non-destructive history
- [ ] Phase 4 — Advanced creative tools
- [ ] Phase 5 — AI editing reliability and privacy
- [ ] Phase 6 — Export, file management and sharing
- [ ] Phase 7 — Billing, Premium and advertising
- [ ] Phase 8 — Performance, accessibility and QA
- [ ] Phase 9 — CI/CD and release automation
- [ ] Phase 10 — Google Play listing, closed testing and production launch

---

## Phase 0 — Brand & Product Foundation

### Rebranding
- [ ] Confirm final brand name: PhotoStudio (availability/trademark check pending)
- [x] Update Android app display name
- [x] Update repository README and project metadata
- [ ] Update in-app branding, titles, dialogs, onboarding and settings (empty workspace branding added)
- [x] Apply supplied PhotoStudioIcon.png to launcher and adaptive icon resources
- [x] Add PhotoStudio splash branding from supplied PhotoStudioIcon.png
- [x] Define initial color tokens and theme palette (typography/spacing refinement remains)
- [x] Rename legacy application and typography code symbols to PhotoStudio; legacy billing SKU intentionally retained for product compatibility
- [x] Remove remaining user-visible Lumin branding from premium/export messaging (legacy SKU kept intentionally; broader strings audit remains)
- [ ] Update export metadata, default filenames and share text where applicable
- [x] Set final Android application ID to `com.winsmediatama.photostudio` (new Play Store app identity; do not expect upgrades from the previous AI Studio ID)

### Design system
- [ ] Dark studio theme as primary
- [ ] Light theme (optional; evaluate after dark theme)
- [ ] Consistent buttons, sliders, cards, dialogs, bottom sheets and tool panels
- [ ] Clear Free/Premium labels and tool states
- [ ] Responsive layout for phones and tablets
- [ ] Accessible contrast, touch targets and text scaling

**Exit criteria:** consistent PhotoStudio branding throughout the app and repository.

## Phase 1 — Android Configuration & Release Blockers

- [x] Confirm application ID `com.winsmediatama.photostudio`; Kotlin/Android namespace remains `com.example` for this source/R-resource package pending optional source-package migration
- [ ] Review minSdk, targetSdk and compileSdk
- [ ] Audit AndroidManifest permissions and remove unnecessary permissions
- [ ] Validate exported activities/services/receivers and intent filters
- [ ] Validate FileProvider configuration and secure file sharing
- [ ] Review backup/data-extraction rules; exclude sensitive temporary/editor data as appropriate
- [ ] Separate Debug and Release configurations
- [ ] Verify Firebase configuration for production; `GOOGLE_SERVICES_JSON_BASE64` is reported by owner as configured in repository secrets, but its package match and decode workflow are not verified
- [ ] Isolate Firebase App Check Debug provider into Debug-only source/dependency (shared initializer currently references it; dependency must remain available until source split)
- [ ] Verify production App Check provider and key configuration
- [ ] Remove secrets from source code and repository history where applicable
- [ ] Review logging so sensitive image/user data is not logged
- [ ] Verify dependencies and resolve known vulnerabilities
- [ ] Enable and validate R8/ProGuard for Release
- [ ] Verify release signing configuration and key backup procedure
- [ ] Build Debug APK successfully
- [ ] Build signed Release AAB successfully
- [ ] Install and smoke-test signed release build

**Exit criteria:** reproducible release configuration, no debug-only configuration leaking into production, signed AAB builds successfully.

## Phase 2 — Core Image Engine & Stability

### Image loading
- [ ] JPEG, PNG and WebP loading
- [ ] HEIC support where platform/device supports it
- [ ] EXIF orientation correctness
- [ ] Correct color space handling
- [ ] Safe downsampling for preview
- [ ] Full-resolution processing without avoidable memory spikes
- [ ] Graceful handling of corrupt, unsupported or inaccessible files
- [ ] Loading progress, cancellation and error states

### Editing engine
- [ ] Brightness/exposure
- [ ] Contrast
- [ ] Highlights and shadows
- [ ] Whites and blacks
- [ ] Temperature and tint
- [ ] Saturation and vibrance
- [ ] Color grading
- [ ] Tone curve
- [ ] Histogram
- [ ] Crop and aspect-ratio presets
- [ ] Free crop
- [ ] Rotate and flip
- [ ] Resize/custom dimensions
- [ ] Preview/full-resolution render consistency
- [ ] Avoid stale preview/render state

### Non-destructive workflow
- [ ] Editing operations stored as replayable state
- [ ] Render from original image and current operation stack
- [ ] Undo/redo
- [ ] Reset individual tool
- [ ] Reset all edits
- [ ] Before/after comparison
- [ ] Save and restore editing session
- [ ] Handle process death/activity recreation

**Exit criteria:** edits are stable, repeatable, undoable and render consistently from original source data.

## Phase 3 — Essential Editing UX

- [ ] Editing workspace redesign around photo canvas
- [ ] Tool categories and clear navigation
- [ ] Live preview while adjusting values
- [ ] Slider value display and reset controls
- [ ] Compare before/after
- [ ] Undo/redo visible and reliable
- [ ] Crop/transform preview and Apply/Cancel
- [ ] Prevent accidental loss of edits when leaving workspace
- [ ] Empty, loading, error and permission states
- [ ] Keyboard/accessibility support where applicable

**Exit criteria:** users can complete a basic edit from import to export without confusing or broken navigation.

## Phase 4 — Advanced Creative Tools

### Filters & presets
- [ ] Built-in filter collection
- [ ] Filter intensity
- [ ] Categories and search
- [ ] User presets
- [ ] Save/import/export presets
- [ ] Preview thumbnails

### Layers & compositing
- [ ] Layer manager
- [ ] Add image layer
- [ ] Reorder layers
- [ ] Visibility toggle
- [ ] Opacity
- [ ] Blend modes
- [ ] Layer transform
- [ ] Layer delete/duplicate
- [ ] Undo/redo layer operations

### Masks & retouching
- [ ] Brush and eraser
- [ ] Mask preview
- [ ] Brush size, hardness and opacity
- [ ] Feathering
- [ ] Blur
- [ ] Sharpen
- [ ] Noise reduction
- [ ] Vignette
- [ ] Grain
- [ ] Text
- [ ] Shapes
- [ ] Stickers
- [ ] Watermark
- [ ] Frames/borders

**Exit criteria:** advanced tools are functional, stable, and have consistent history/preview behavior.

## Phase 5 — AI Editing & Privacy

### On-device Real-ESRGAN (selected model set)

- [x] Initial model catalog for Photo x4plus, Anime x4, and AnimeVideo v3 x2
- [x] Runtime model downloader into app-private storage with temporary-file finalization and basic size checks
- [x] Model install/remove UI in AI Studio
- [x] JNI bridge and tiled NCNN inference source with Vulkan capability check
- [x] Real-ESRGAN inference action wired into the editor state and preview refresh
- [x] Input/output memory guardrails (6 MP input / 12 MP output limits)
- [ ] Verify model download sources and hashes in automated integration tests
- [ ] Build native library and Debug APK successfully in CI
- [ ] Verify inference on Vulkan device and CPU-only fallback
- [x] CPU fallback retry if Vulkan inference fails (native errors returned to Kotlin)
- [ ] Progress/cancel for native inference
- [ ] Before/after compare, save/export and large-image device QA

**Selected models:** Photo x4plus (~33.4 MB), Anime x4plus (~8.9 MB), AnimeVideo v3 x2 (~1.2 MB). Model files are downloaded on demand and stored app-private; they are not bundled into the APK. The catalog/downloader and JNI/native source are implemented, but native build and device execution are not yet verified. Model source links are Hugging Face mirrors of NCNN-format weights; confirm license attribution and integrity hashes before production distribution.


### AI Photo Enhance

- [x] Add AI Enhance mode to AI Studio
- [x] AI clean-up prompt for blur/softness, noise, compression artifacts, and detail clarity
- [x] Preserve scene, composition, faces/identity, text and colors as instructed; avoid stylized edits
- [x] Process with Gemini image model and match output dimensions to source (input is downsampled to max 1280px before cloud processing)
- [x] Loading, success, timeout, quota and error states
- [x] Revert AI result to original loaded photo
- [ ] Device/real-image quality QA (AI can reconstruct or hallucinate details; enhancement is not guaranteed to recover true lost detail)

### AI Object Removal
- [ ] Mask brush and eraser
- [ ] Mask feather/refinement
- [ ] Preview before applying
- [ ] Accept/reject/retry
- [ ] Cancel operation
- [ ] Timeout and network failure handling
- [ ] Quota/rate-limit handling
- [ ] Clear progress state
- [ ] Prevent duplicate requests

### Background removal
- [ ] Improve edge quality
- [ ] Hair and fine-detail refinement
- [ ] Transparent output
- [ ] Solid-color replacement
- [ ] Gradient replacement
- [ ] Image replacement
- [ ] Shadow/foreground refinement
- [ ] Avoid green/chroma-key halo artifacts

### AI safety, privacy and cost
- [ ] Clearly disclose when an image is uploaded to a cloud AI service
- [ ] Obtain user consent before cloud processing
- [ ] Explain provider/data handling in privacy policy
- [ ] Avoid storing uploaded images longer than necessary
- [ ] Add request limits and abuse prevention
- [ ] Track AI usage/cost
- [ ] Handle unavailable model/provider gracefully
- [ ] App Check and backend protection where applicable

**Exit criteria:** AI tools have reliable error handling, user consent, privacy disclosures and cost/abuse controls.

## Phase 6 — Export, File Management & Sharing

- [ ] JPEG quality control
- [ ] PNG export
- [ ] WebP export
- [ ] TIFF export (validate compatibility and correctness before exposing as stable)
- [ ] Resolution presets
- [ ] Custom width/height
- [ ] Aspect ratio control
- [ ] Export progress
- [ ] Export cancellation
- [ ] Export success/failure feedback
- [ ] Correct output dimensions and orientation
- [ ] Metadata preserve/remove options
- [ ] Filename customization
- [ ] Handle filename collisions
- [ ] Save to gallery
- [ ] Save to selected folder/document provider
- [ ] Share sheet
- [ ] Cache cleanup and temporary-file cleanup
- [ ] Verify large image exports and low-memory devices

**Exit criteria:** all advertised formats produce valid files and export flow works across Android versions/devices.

## Phase 7 — Monetization: Billing, Premium & Ads

### Google Play Billing
- [ ] Verify product IDs and Play Console product setup
- [ ] Purchase flow
- [ ] Pending purchase handling
- [ ] Cancelled purchase handling
- [ ] Billing unavailable/offline handling
- [ ] Purchase acknowledgement
- [ ] Restore/re-query purchases
- [ ] Reinstall and entitlement restoration test
- [ ] Refund/revocation handling
- [ ] Premium entitlement source of truth
- [ ] Avoid trusting local-only entitlement state for critical premium access

### AdMob
- [ ] Verify production App ID and ad unit IDs
- [ ] Keep test IDs separate from production
- [ ] Test-device configuration
- [ ] App Open ad behavior
- [ ] Interstitial frequency caps
- [ ] No ads interrupting active editing or export
- [ ] Handle ad loading/failure safely
- [ ] Premium users receive expected ad-free experience
- [ ] Review consent/privacy requirements for ads

### Monetization QA
- [ ] Free-user flow
- [ ] Premium-user flow
- [ ] Purchase success
- [ ] Purchase cancellation
- [ ] Restore purchases
- [ ] Billing unavailable
- [ ] Ad failure
- [ ] No accidental duplicate purchase or entitlement

**Exit criteria:** monetization works in Play test tracks and production configuration is verified.

## Phase 8 — QA, Performance & Accessibility

### Automated tests
- [ ] Image loading and orientation tests
- [ ] Crop/transform tests
- [ ] Tone curve tests
- [ ] Histogram tests
- [ ] Operation replay tests
- [ ] Undo/redo tests
- [ ] Layer/compositing tests
- [ ] Export format tests
- [ ] Billing state tests
- [ ] AI state/error tests
- [ ] UI navigation tests
- [ ] Regression tests for every production bug

### Device matrix
- [ ] Minimum supported Android version
- [ ] Latest Android version
- [ ] Low-memory device
- [ ] Mid-range device
- [ ] High-end device
- [ ] Different screen sizes and aspect ratios
- [ ] Tablet layout

### Stress/performance
- [ ] 12 MP image
- [ ] 24 MP image
- [ ] 48 MP image
- [ ] Large panorama
- [ ] Repeated editing/undo/redo
- [ ] Long editing session
- [ ] Background/foreground
- [ ] Rotation and activity recreation
- [ ] Process death/session recovery
- [ ] Low-memory behavior
- [ ] No leaks or stale bitmap references
- [ ] No critical crash or ANR
- [ ] Smooth preview interaction

### Accessibility
- [ ] Content descriptions
- [ ] Screen reader navigation
- [ ] Touch target sizing
- [ ] Text scaling
- [ ] Contrast review
- [ ] Color is not the only state indicator

**Exit criteria:** no known release-blocking crash, data-loss, export, privacy or monetization issue.

## Phase 9 — CI/CD & Release Automation

- [ ] GitHub Actions workflow on pull requests (repository currently has no workflow files)
- [ ] Kotlin/Gradle compile checks
- [ ] Android lint
- [ ] Unit tests
- [ ] Robolectric tests
- [ ] Debug build artifact
- [ ] Release build validation
- [ ] Dependency/security checks
- [ ] Build artifacts retained for review
- [ ] Version code/name validation
- [ ] Release notes template
- [ ] Signed AAB workflow (secrets secured)
- [ ] Tag-based release process
- [ ] CI status documented in README

**Exit criteria:** main branch changes are automatically checked and release builds can be reproduced.

## Phase 10 — Google Play Preparation & Closed Testing

- [ ] Privacy policy URL
- [ ] Data Safety form
- [ ] AI/cloud processing disclosure
- [ ] Photo/media permission justification
- [ ] Ads disclosure and consent requirements
- [ ] Billing disclosure
- [ ] Data deletion/retention policy
- [ ] Store title
- [ ] Short description
- [ ] Full description
- [ ] App icon
- [ ] Feature graphic
- [ ] Phone screenshots
- [ ] Tablet screenshots (if supported)
- [ ] Promotional video (optional)
- [ ] Contact email and support URL
- [ ] Internal test
- [ ] Closed test
- [ ] Tester feedback collection
- [ ] Fix test findings
- [ ] Retest critical workflows
- [ ] Production readiness review

**Exit criteria:** Play Console declarations and store assets complete; closed test findings resolved or explicitly accepted.

## Phase 11 — Production Release & Monitoring

### Pre-release checklist
- [ ] Final signed AAB verified
- [ ] Version code is unique
- [ ] Version name correct
- [ ] Release notes prepared
- [ ] Privacy policy reachable
- [ ] Store listing reviewed
- [ ] Billing tested
- [ ] Ads tested
- [ ] AI services tested
- [ ] Data Safety declarations verified
- [ ] Crash reporting/monitoring enabled
- [ ] Support channel ready
- [ ] Rollback/halt rollout plan prepared

### Staged rollout
- [ ] 10% rollout
- [ ] Monitor crashes, ANRs, feedback and billing for 24–72 hours
- [ ] Expand to 25%
- [ ] Monitor again
- [ ] Expand to 50%
- [ ] Expand to 100%
- [ ] Post-launch review and prioritized backlog

**Exit criteria:** stable production rollout and post-launch review completed.

---

## Definition of Done (DoD)

A task can be marked complete only when:
1. Implementation is committed to GitHub.
2. Relevant tests pass, or a test limitation is clearly documented.
3. No regression is introduced in related workflows.
4. UI includes loading, empty, success and failure states where relevant.
5. Privacy/security implications are reviewed when applicable.
6. Roadmap checkbox is updated in the same milestone.
7. Build verification status is recorded.

## Progress Log

| 2026-11-21 | Phase 5 — AI Photo Enhance | Added AI Enhance mode to AI Studio with Gemini enhancement prompt, cloud processing, output dimension matching, status/error handling and Revert AI. Input is capped at 1280px before AI; returned image is resized to original dimensions, so users should be informed AI may reconstruct details and cannot guarantee true recovery of blur. | Source-level only; build/device QA pending | `010223a` (implementation commit) |

| Date | Milestone | Changes | Verification | Commit |
|---|---|---|---|---|
| 2026-11-21 | Phase 9 — CI/CD discovery | Confirmed the repository currently has no `.github/workflows` files; automated build/test checks and secret decoding are not yet configured in repository workflows. CI workflow creation is deferred to the CI/CD roadmap stage to honor the request not to build yet. | Repository tree inspection only | Pending |
| 2026-11-21 | Phase 1 — App Check debug dependency isolation (reverted) | Attempted to move the debug provider dependency to `debugImplementation`, but the shared main-source initializer directly references `DebugAppCheckProviderFactory`; this would break Release compilation. Reverted to shared `implementation` pending a source-set split that safely isolates Debug provider code. | Source inspection; build intentionally deferred | `6680d88` |
| 2026-11-21 | Phase 1 — App Check debug dependency isolation | Moved Firebase App Check Debug provider dependency from shared `implementation` to `debugImplementation`, preventing the debug provider library from being included in Release dependency configuration. Firebase reCAPTCHA provider remains the production dependency. | Source config only; Release dependency graph/build not checked (build deferred) | `ee4caef` |
| 2026-11-21 | Phase 1 — package/application ID | Changed Android `applicationId` from the legacy AI Studio ID to `com.winsmediatama.photostudio`. Kept `namespace = com.example` so generated `R`/source package references remain stable; FileProvider authority uses `${applicationId}` and therefore follows the new app ID. Firebase `google-services.json` is absent from the repository and must be registered/configured for the new package before AI/release verification. This is a new Play Store identity, not an in-place upgrade from the old package. | Source inspection only; build/install/Firebase configuration not verified | `594d6ca` |
| 2026-11-21 | Phase 3 — startup workspace behavior | Removed automatic loading of the sample landscape on ViewModel initialization so the app opens in the branded empty workspace and users can select/import their own image. Sample assets remain available through the sample picker. | Source-level change only; build and tests deferred by user instruction | `7d4ddc5` |
| 2026-11-21 | Phase 5 — AI cloud-processing disclosure | Added a visible AI Studio notice explaining that selected photos are sent to Firebase/Gemini cloud services and cautioning against sensitive uploads without agreement. This is a disclosure only; explicit consent gating, privacy policy details and provider data-retention review remain release blockers. | Source-only; UI build and privacy review pending | `af64591` |
| 2026-11-21 | Phase 2 — source import race protection | Added source-load cancellation and generation tracking for gallery/sample image loads; a slower earlier import is prevented from replacing a later user selection. Cancellation is rethrown rather than surfaced as a false import error. | Source-only; concurrent import behavior needs tests | `8ce01dd` |
| 2026-11-21 | Phase 2 — filter thumbnail race protection | Added generation checks to filter thumbnail rendering so cancelled/outdated thumbnail work cannot replace the thumbnail set for a newer image base. | Source-only; tests/build deferred | `3e3014b` |
| 2026-11-21 | Phase 2 — preview render race protection | Added a monotonically increasing preview-render generation guard. Synchronous bitmap rendering may continue after coroutine cancellation; older results are now prevented from overwriting preview/histogram state for newer edits. | Source-only; concurrency and image tests deferred | `2d3033f` |
| 2026-11-21 | Phase 2 — image import orientation | Extended EXIF orientation handling to mirrored/transpose/transverse cases (orientations 5 and 7), in addition to rotations and horizontal/vertical flips, to avoid incorrect display for camera-originated images. | Source change only; device/build tests deferred | `652711b` |
| 2026-11-21 | Phase 3 — empty workspace HUD cleanup | Hide photo metadata and histogram HUD overlays while no image is loaded; they appear again once a preview bitmap exists. This prevents fake/sample metadata and histogram clutter from appearing in the initial workspace. | Source review only; build/tests intentionally deferred | `266ef79` |
| 2026-11-21 | Phase 3 — startup workspace behavior | Removed automatic loading of the sample landscape on ViewModel initialization so the app opens in the branded empty workspace and users can select/import their own image. Sample assets remain available through the sample picker. | Source-level change only; build and tests deferred by user instruction | `7d4ddc5` |
| 2026-11-21 | Phase 1 — Android configuration review | Reviewed app Gradle, manifest, backup/data-extraction rules and ProGuard. Findings: legacy applicationId retained intentionally; target SDK 36/min SDK 24; release minification disabled; App Check debug dependency is present alongside reCAPTCHA; backup is enabled with sample/empty rules and needs a data-sensitivity decision; FileProvider is non-exported with URI grants. No configuration changed yet to avoid impacting identity, AI, backup or release signing. | Source audit only; build and security runtime checks not performed | See latest commit |
| 2026-11-21 | Phase 0 — UI branding audit | Reviewed top bar, export/photo sheets, AI/masking panels, canvas workspace, MainActivity and shared strings; replaced empty-workspace tagline and prompt with shared Android string resources for localization-ready branding. Remaining user-visible hardcoded English and old-name audit is still pending. | Build/tests intentionally not run; source inspection only | See latest commit |
| 2026-11-21 | Phase 0 — release configuration audit | Updated BillingManager default/status messages to PhotoStudio Pro; added shared tagline and empty-workspace prompt strings; preserved legacy entitlement preferences, Billing SKU, and applicationId pending Play Store identity decision. Audit confirms AdMob App ID is Google test ID and applicationId remains legacy AI Studio ID — both require owner decision before release. | Build/tests not run; source-only inspection | See latest commit |
| 2026-11-21 | Phase 0 — branding continuation | Renamed application class and typography symbol to PhotoStudio; branded empty-editor workspace; renamed app reference variable; updated premium/export dialogs to PhotoStudio Pro; changed export filenames from LuminaLab to PhotoStudio; preserved legacy billing SKU to avoid product-ID breakage | Build/unit tests not run; source/resource inspection only | See latest commit |

## Current Known Release Risks (Initial Audit)

- [x] Application ID set to `com.winsmediatama.photostudio`; source/namespace migration from `com.example` remains pending
- [ ] Release configuration and signing need verification
- [ ] Verify `GOOGLE_SERVICES_JSON_BASE64` secret encodes Firebase config for `com.winsmediatama.photostudio`; verify production App Check reCAPTCHA and release dependency graph
- [ ] AdMob production identifiers need verification
- [ ] Billing purchase/restore/revocation paths need end-to-end tests
- [ ] AI object/background removal quality and failure handling need validation
- [ ] High-resolution memory/performance needs device testing
- [ ] Export formats/interoperability need verification
- [ ] CI/CD release workflow needs implementation or verification
- [ ] Play Store privacy, Data Safety and listing assets need completion

> **Working rule:** proceed phase-by-phase. Prioritize release blockers and correctness before adding more feature scope. Update this document continuously so future work can resume without losing the project state.


## Release Signing & Manual GitHub Actions Workflow

The repository owner reports that these GitHub Actions secrets are already configured. The release Gradle signing configuration and manual workflow now use these exact names:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `GOOGLE_SERVICES_JSON_BASE64`

**Workflow:** `.github/workflows/manual-release.yml`

- Trigger: manual only (`workflow_dispatch)); it will not run automatically on pushes.
- Decodes the upload keystore into the GitHub runner's temporary directory.
- Decodes Firebase configuration into `app/google-services.json` for the job only.
- Runs `bundleRelease` with the configured signing environment.
- Uploads the signed AAB as a workflow artifact for 14 days.
- Does not commit decoded keystore or Firebase configuration files.

**Important:** The workflow has been added but intentionally has not been run. Build, test, Firebase package matching, signing validation and AAB inspection remain deferred.



## Manual Debug APK Workflow

Workflow file: `.github/workflows/debug-apk.yml`

- Triggered manually from GitHub Actions (`workflow_dispatch`).
- Sets up Java 17 and Android SDK.
- Optionally decodes `GOOGLE_SERVICES_JSON_BASE64` if configured.
- Runs `./gradlew --no-daemon assembleDebug`.
- Uploads the Debug APK artifact for 14 days.
- Does not run automatically on pushes and has not been executed yet.

To use: open the repository's **Actions** tab, select **PhotoStudio Android Debug APK**, choose **Run workflow**, and download the artifact from the completed run.


## CI Workflow Failure Fix — Android SDK Setup

The first manual Debug APK workflow run failed before Gradle started. Root cause from the Actions log: `android-actions/setup-android@v3` attempted to install the obsolete SDK package `tools`, which is no longer available (`Failed to find package 'tools'`).

Fix committed:
- Configure the setup action to install only `platform-tools`.
- Explicitly install `platforms;android-36` and `build-tools;36.0.0` with `sdkmanager`.
- Apply the same correction to the manual signed Release workflow, which used the same setup-action defaults.

The workflows are updated, but the corrected run has not yet been started/verified. The GitHub connector available here can inspect runs and logs but cannot dispatch a new workflow run.



### Follow-up SDK platform correction

Gradle's compile SDK is configured as Android API 36 with minor API level 1 (`compileSdk release(36) { minorApiLevel = 1 }`). The manual workflows now request `platforms;android-36.1` instead of `android-36`, matching that compile SDK configuration. This correction is committed to both Debug APK and signed Release workflows. It is still awaiting a fresh manual run; the Actions integration available here can inspect runs/logs but cannot dispatch workflow runs.


## Local Enhancement Engine (Free / Offline)

- [x] Add CPU-based local processing: noise smoothing, unsharp mask, and contrast recovery
- [x] Add **Local Enhance** action in AI Studio (no network/API/billing)
- [x] Add **Upscale 2×** action using high-quality bitmap interpolation
- [ ] Add adjustable sharpen/denoise/contrast sliders and compare preview
- [ ] Benchmark memory/performance across low/mid/high-end Android devices
- [ ] Evaluate licensed on-device neural restoration model; retain classical fallback

**Accuracy note:** The current Local Enhance is classical image processing, not a neural AI model. It improves perceived clarity but cannot reliably reconstruct missing detail. Upscale 2× is interpolation, not AI super-resolution.


### Local Enhance Studio — Phase 2
- [x] Add interactive Sharpen slider (0–100%)
- [x] Add Denoise slider (0–100%)
- [x] Add Local Contrast / Detail slider (0–100%)
- [x] Pass slider settings to local engine on Apply
- [ ] Add non-destructive parameter preview before applying
- [ ] Add before/after comparison dedicated to Local Enhance
- [ ] Optimize repeated parameter changes against a retained source bitmap
- [ ] Add instrumentation/unit tests for image quality and memory guards
