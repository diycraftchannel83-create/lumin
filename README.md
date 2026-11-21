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
- [ ] Remove old Lumin branding and placeholder labels
- [ ] Update export metadata, default filenames and share text where applicable
- [ ] Decide final package/application ID before Play Store release (changing it later creates a separate app identity)

### Design system
- [ ] Dark studio theme as primary
- [ ] Light theme (optional; evaluate after dark theme)
- [ ] Consistent buttons, sliders, cards, dialogs, bottom sheets and tool panels
- [ ] Clear Free/Premium labels and tool states
- [ ] Responsive layout for phones and tablets
- [ ] Accessible contrast, touch targets and text scaling

**Exit criteria:** consistent PhotoStudio branding throughout the app and repository.

## Phase 1 — Android Configuration & Release Blockers

- [ ] Confirm final namespace and application ID
- [ ] Review minSdk, targetSdk and compileSdk
- [ ] Audit AndroidManifest permissions and remove unnecessary permissions
- [ ] Validate exported activities/services/receivers and intent filters
- [ ] Validate FileProvider configuration and secure file sharing
- [ ] Review backup/data-extraction rules; exclude sensitive temporary/editor data as appropriate
- [ ] Separate Debug and Release configurations
- [ ] Verify Firebase configuration for production
- [ ] Ensure debug App Check tokens/debug providers cannot be enabled in Release
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

- [ ] GitHub Actions workflow on pull requests
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

| Date | Milestone | Changes | Verification | Commit |
|---|---|---|---|---|
| 2026-11-21 | Phase 0 — branding continuation | Renamed application class and typography symbol to PhotoStudio; added branded empty-editor workspace; preserved legacy billing SKU to avoid product-ID breakage | Build/unit tests not run; source/resource inspection only | See latest commit |

## Current Known Release Risks (Initial Audit)

- [ ] Final brand/package identity not confirmed (applicationId intentionally unchanged pending Play Store identity decision)
- [ ] Release configuration and signing need verification
- [ ] Production Firebase/App Check configuration needs verification
- [ ] AdMob production identifiers need verification
- [ ] Billing purchase/restore/revocation paths need end-to-end tests
- [ ] AI object/background removal quality and failure handling need validation
- [ ] High-resolution memory/performance needs device testing
- [ ] Export formats/interoperability need verification
- [ ] CI/CD release workflow needs implementation or verification
- [ ] Play Store privacy, Data Safety and listing assets need completion

> **Working rule:** proceed phase-by-phase. Prioritize release blockers and correctness before adding more feature scope. Update this document continuously so future work can resume without losing the project state.
