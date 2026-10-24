# VisionDemo — live object detection from a SKaiNET cartridge

Live camera object detection on Android: YOLOv8n (COCO, 80 classes) compiled through IREE,
running on the phone's GPU (Vulkan) or CPU, consumed as a **SKaiNET cartridge**. The app is
the published cartridge API end to end — glass HUD with FPS readout and sparkline, backend
switch (CPU / VULKAN / VK·320), capture gallery, a detections viewer and an image-picker
path beside the live feed.

Verified on a Pixel 8a: **~57 ms per frame (≈17 fps) live** on the 320-input Vulkan pack,
~174 ms on the 640 one; boxes and labels map 1:1 onto the analyzed frames.

## How it consumes the model

This example owns **no model code and no weights**. It depends on two small libraries from
Maven Central, published out of
[SKaiNET-cartridge-blueprints](https://github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints):

```kotlin
implementation("sk.ainet.cartridge:detection-yolov8-iree:0.2.2")   // opens, verifies, runs packs
// brings sk.ainet.cartridge.profiles:yolo-v1 (the task-profile contract) transitively
```

What actually runs is a **pack** — compiled graphs (`.vmfb`), converted parameters (`.irpa`)
and the native runtime, under a signed manifest — and packs are **never distributed**: the
YOLOv8 weights are AGPL-3.0 with `redistribution: restricted`, so you materialize your own,
on your machine, from the blueprint:

```sh
git clone https://github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints
cd SKaiNET-cartridge-blueprints
./gradlew :blueprints:detection-yolov8-iree:materializeCartridge \
    -Pprofile=profiles/mine.json   # start from profiles/example.cpu-arm64.json
```

The [blueprint README](https://github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints/tree/main/blueprints/detection-yolov8-iree)
is the step-by-step guide (fetch → convert → export → compile → measure → sign), including the
Vulkan and 320-input speed variants this demo's numbers come from.

The blueprint downloads the weights from their publisher (pinned by revision + SHA-256,
under terms **you** accept — it never accepts a license for you), converts, compiles and
signs. The pack lands in `blueprints/detection-yolov8-iree/build/cartridge/`.

## Run it

```sh
./gradlew :androidApp:installDebug
```

`bundlePacks` copies any packs it finds into the APK assets. By default it looks in a
sibling `SKaiNET-cartridge-blueprints` checkout (`../../SKaiNET-cartridge-blueprints/
blueprints/detection-yolov8-iree/build/cartridge`); point it elsewhere with
`-PpacksDir=/path/to/packs`. **No packs is fine**: a fresh clone builds, installs and runs
with no weights — the app then shows the materialization steps instead of the camera feed.

Camera permission is asked on first start; the image-picker path works without it.

## Licensing

- App code: MIT, like the rest of this repository.
- `sk.ainet.cartridge:*` artifacts: MIT, code only.
- Orbitron display font: SIL OFL-1.1 (`androidApp/src/main/assets/fonts/OFL-1.1.txt`).
- YOLOv8n weights: **AGPL-3.0** (Ultralytics), `redistribution: restricted`. They are not in
  this repository, not on Maven, and not in CI. A pack you materialize derives from them:
  bundling it into your locally built APK is private use, but **distributing** such an APK
  carries the AGPL obligations with it. Don't publish pack-bundled builds.

## Structure

- `composeApp/` — Compose Multiplatform library. All UI is `commonMain`, written against the
  `yolo/v1` contract (`YoloEngine`, `Detection`). The platform seams are five
  `expect` declarations — camera feed, pack discovery, engine opening, image picker, display
  font — and Android (CameraX frames-as-preview + the IREE module runner) is the first wired
  platform; a desktop or iOS target needs only its own actuals.
- `androidApp/` — the APK shell (AGP 9 does not allow a KMP module to be the application):
  manifest, Orbitron asset, the `bundlePacks` task. No code.
