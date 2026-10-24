# SKaiNET Examples

Sample applications built with [**SKaiNET**](https://github.com/SKaiNET-developers/SKaiNET) —
the Kotlin Multiplatform AI framework. Each example is a self-contained project;
most are Compose Multiplatform apps that run on Android, iOS, Desktop and the web
from a single Kotlin codebase.

> [!IMPORTANT]
> **About the name**
>
> “SKaiNET” is a **working project name** chosen early in the project’s life as part of a personal learning and experimentation effort, before any trademark considerations were known.
>
> The name is **not intended to reference, infringe, or imply association with any existing trademarks, companies, or products**. It is not a commercial brand and is **not claimed or assignable** to any company or organization that contributors may be affiliated with.
>
> If a naming conflict arises, the project name may be changed in the future.

## Examples

### 🌊 Sinus Approximator

A tiny neural network that learns to approximate `sin(x)` — and you can **train it
live in the app**. Visualises the target curve, the model's prediction, and the
network architecture. One Kotlin codebase running on Android, iOS, Desktop (JVM)
and WebAssembly.

<img src="SinusApproximator/docs/screenshots/jvm.png" alt="Sinus Approximator — in-app training screen" width="640" />

📂 [`SinusApproximator/`](SinusApproximator/) · runs on Android · iOS · Desktop · Wasm

---

### ✏️ MNIST Demo

Handwritten-digit recognition: draw a digit and a convolutional network classifies
it on the spot. A clean-architecture Compose Multiplatform app that loads a
pretrained GGUF model and can also retrain it in-app.

<img src="MNISTDemo/docs/screenshots/wasm.png" alt="MNIST Demo — draw and classify a digit" width="640" />

📂 [`MNISTDemo/`](MNISTDemo/) · runs on Android · iOS · Desktop · Wasm

---

### 🔤 GloVe Embeddings

Word-vector arithmetic and nearest-neighbour search over pretrained GloVe
embeddings — `king - man + woman ≈ queen`, fully offline. A Compose Multiplatform
app that shows how SKaiNET handles plain vector data, not just neural nets.

📂 [`GloVeEmbeddings/`](GloVeEmbeddings/) · runs on Android · iOS · Desktop · Wasm

---

### 🤏 Tiny Transformer (KI-ENNA)

Train a **tiny decoder-only transformer live in the browser** — word-level
tokenizer, token + positional embeddings, single-head causal self-attention and
next-word prediction, with a live attention heatmap and loss curve. A SKaiNET
port of the German educational page
[KI-ENNA](https://statistical-thinking.de/ki-enna-transformer.html), built from
scratch with the core NN DSL (no transformers library), with an English/German
language toggle.

<img src="TinyTransformer/docs/screenshots/wasm.png" alt="Tiny Transformer — live training with attention heatmap and loss curve" width="640" />

📂 [`TinyTransformer/`](TinyTransformer/) · runs on Android · iOS · Desktop · Wasm

---

### 🔵 TDA Lab

Interactive **topological data analysis**: pick or paste a 2-D point cloud, grow the
Vietoris-Rips radius ε with a slider and watch edges appear, components merge and loops open
and close, with live Betti numbers, a persistence barcode and a persistence diagram. The
distance matrix is a SKaiNET tensor expression; H₀ and H₁ come from a real ℤ/2 boundary-matrix
reduction in plain Kotlin. A Kotlin Multiplatform re-implementation of
[TDA Lab](https://github.com/Starchild13/TDAProject) for Android.

📂 [`TDALab/`](TDALab/) · runs on Android · iOS · Desktop · Wasm

---

### 🏁 Kernel Race

On-device LLM chat accelerated by SKaiNET's hand-written ARM NEON kernels on Android, with a
built-in **NEON vs scalar** A/B comparison — including a one-phone split-screen race. The same
Kotlin codebase also runs on Desktop and in the browser, showing the other kernel tiers SKaiNET
falls back to without NEON hardware.

<img src="KernelRace/docs/screenshots/split_race.png" alt="Kernel Race — split-screen NEON vs scalar generation" width="640" />

📂 [`KernelRace/`](KernelRace/) · runs on Android · Desktop · Wasm

---

### 🔎 Semantic Emoji Filter

Type what you mean, get the emoji. Every keystroke is embedded on-device with the MongoDB LEAF
retrieval encoder (23M parameters, BERT in the SKaiNET NN DSL) and ranked against a bilingual
catalog of about 200 emoji; items above a calibrated threshold move up with a confidence badge.
A second stage, the **Laya decision model**, answers a typed yes/no question per candidate
through `sk.ainet.decisions`, so 🍕 drops for "healthy things to eat" while 🥗 stays, and an
unsure model says `?` instead of guessing. On desktop and Android it loads from files; in the
browser it runs in a Web Worker on the packed Q8_0 lane after a one-time opt-in download.

<img src="SemanticEmojiFilter/docs/screenshots/grid.png" alt="Semantic Emoji Filter — matches above the threshold with vector and Laya confidence" width="640" />

📂 [`SemanticEmojiFilter/`](SemanticEmojiFilter/) · runs on Android · Desktop · Wasm

---

### 🧭 LayaMoE

Domain experts on top of the **Laya decision model**: one shared ModernBERT-large encoder, small
expert heads fine-tuned for safety and customer-operations questions, and a router that picks one
per request — the base head answers "what kind of text is this?" and the matching expert answers
the typed `choice` / `score` / yes-no questions with its own calibration, the original head as the
fallback. A port of layaMOE (Apache-2.0) on SKaiNET-transformers' expert-head API; the CLI replays
its 108 hand-labelled cases. Desktop and Android load the 1.05 GB from a cache directory; the
browser runs it in a Web Worker on the packed Q8_0 lane and the SIMD128 sidecar.

<img src="LayaMoE/docs/screenshots/web.png" alt="LayaMoE — a routed request with per-option probabilities and the router's kinds" width="640" />

📂 [`LayaMoE/`](LayaMoE/) · runs on Android · Desktop · Wasm

---

### 💬 Kllama Demo

A Qwen3 LLM **playground in the browser** — chat, completion, translation, tool
calls, a tokenizer view and a transformer explainer that visualises attention and
residuals, all running on-device with no server and no API key.

📂 [`KllamaDemo/`](KllamaDemo/) · runs on Android · iOS · Desktop · Web · Server

---

### ☕ MNIST Java Demo

The same MNIST digit detector as a **pure-Java CLI** — proof of SKaiNET's
first-class Java interop from a Kotlin Multiplatform engine.

```text
Predicted digit: 7
Confidence: 98.3%
```

📂 [`MnistJavaDemo/`](MnistJavaDemo/) · command-line · Java 21+

### 📷 Vision Demo

Live camera **object detection** from a SKaiNET cartridge: YOLOv8n (COCO, 80 classes)
compiled through IREE runs on the phone's GPU — ~57 ms per frame (≈17 fps) on a Pixel 8a —
with a glass HUD, FPS sparkline, CPU/Vulkan backend switch, capture gallery and detections
viewer. The app consumes the published `sk.ainet.cartridge` artifacts (code only); the
pack it runs is materialized locally from the
[detection blueprint](https://github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints) —
the AGPL-3.0 weights never ship, and a fresh clone builds and runs pack-less.

📂 [`VisionDemo/`](VisionDemo/) · runs on Android

---

### 🌸 Iris Classifier

The classic Iris dataset as the smallest possible **training loop**: a two-layer dense
network from the `sequential` DSL, categorical cross-entropy, Adam and the training
runner, with a train/test split and the test accuracy printed at the end.

📂 [`IrisDemo/`](IrisDemo/) · command-line · JVM

## New here?

If this is your first SKaiNET example, start with **Sinus Approximator** — it is
the smallest end-to-end story (define a network, train it, see it predict) and
runs on every platform. Then try **MNIST Demo** for a real convolutional model,
**GloVe Embeddings** for vector arithmetic, and **Kllama Demo** to run a local LLM.

Each example folder has its own `README.md` with build and run instructions.
