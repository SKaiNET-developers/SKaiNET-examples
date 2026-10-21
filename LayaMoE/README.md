# LayaMoE — domain experts on top of Laya

One shared Laya encoder, small expert heads fine-tuned for a group of domains, and a router that
picks one head per request. Ask typed questions about any text — a `choice`, a `score`, a yes/no
`noul` probability — and get calibrated answers from the head that knows the domain, with the
original head as the fallback. Everything runs on the device: desktop JVM, Android, and the
browser (Kotlin/Wasm in a Web Worker on the SIMD128 kernel sidecar).

This is a SKaiNET port of [layaMOE](https://github.com/vishalmysore/layaMOE) by vishalmysore
(Apache-2.0, an unofficial experiment on the [laya-typed-decisions](https://huggingface.co/convaiinnovations/laya-typed-decisions)
checkpoint by ConvAI Innovations): the same weights, the same router, the same presets, the same
evaluation — the model code is SKaiNET-transformers' Laya module and its new expert-head API.

## What it demonstrates

- **One encoder, many heads.** A Laya checkpoint is a frozen ModernBERT-large encoder (395M
  parameters) plus a 26.5M-parameter decision head (type embedding, two transformer blocks,
  scorer). An expert is that head copied and fine-tuned on synthetic domain data, saved on its own
  as `expert.safetensors` + `expert.json`. `LayaExpertLoader` binds it to the loaded base model;
  `LayaMixtureAgent` runs the encoder once per question and any head on the result.
- **A prompted router.** The base head answers *"What kind of text is this?"* over ten fine-grained
  kinds with descriptions; five kinds map to an expert (`agent_action`, `public_comment` → `safety`;
  `support_ticket`, `delivery_issue`, `work_email` → `customer_ops`), the rest — and any top
  probability under 0.3 — stay with the original `general` head. Sequence-level routing, one head
  per request: **not** a sparse mixture of experts, and the page says so.
- **Honest numbers.** Every answer carries the head's calibrated confidence, the result shows what
  the encoder and the heads cost, and the CLI replays the 108 hand-labelled cases layaMOE
  published so you can see where the experts help and where they do not.
- **Full KMP.** The checkpoint spec, the download, the mixture, the presets, the question JSON, the
  worker protocol, the view model and the UI are common code; the platforms differ only in where
  the bytes come from (a cache directory, `filesDir`, the browser's Cache API + `Blob`), which
  kernels to install, and the Web Worker bootstrap.

## Run

The first run downloads about 1.05 GB (the base checkpoint, 843 MB F16, and two expert heads of
106 MB each) and caches it; nothing is redistributed in this repository. Build with JDK 21.

### Desktop

```bash
./gradlew :composeApp:run                       # FP32 reference lane, ~1.9 GB resident
LAYAMOE_LANE=q8 ./gradlew :composeApp:run       # packed Q8_0 lane, less memory, same answers to within quantization noise
```

The checkpoints go to `~/.skainet-examples/layamoe/` (override with `LAYAMOE_MODEL_DIR`). The
native FFM kernels of `skainet-backend-native-cpu` are on the classpath and register themselves.

### Android

```bash
./gradlew :androidApp:installDebug
```

`arm64-v8a`, JNI NEON kernels, the Q8_0 lane; the app warns when the device grants too little
memory per app. The download lands in the app's `filesDir`.

### Web (Wasm)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun       # dev server with COOP/COEP headers (SIMD128 + pthreads)
./gradlew :composeApp:wasmJsBrowserDistribution          # the deployable site: composeApp/build/dist/wasmJs/productionExecutable
```

The model lives in a Web Worker (`:webWorker`, copied under `moe-worker/`): the checkpoints are
fetched into the Cache API, the weights stay `Blob`s read by the engine's `JsBlobRandomAccessSource`
through `FileReaderSync`, and the projections are packed to Q8_0 as they are read. On a
cross-origin-isolated page (the examples site uses the `coi-serviceworker` shim) the sidecar runs
its pthreads build; elsewhere single-threaded.

### CLI (ask, replay the presets, measure, evaluate)

```bash
./gradlew :cli:installDist
cli/build/install/cli/bin/cli preset --index 0 --compare           # the first preset, expert vs general head
cli/build/install/cli/bin/cli ask --state 'Agent plan: run DROP TABLE invoices on production.' \
    --questions '{"destructive": {"type": "noul", "instructions": "The action is destructive and cannot be undone"}}'
cli/build/install/cli/bin/cli bench --index 3 --reps 5
cli/build/install/cli/bin/cli eval                                  # all 108 cases: general vs routed vs oracle, per domain
cli/build/install/cli/bin/cli eval --cases 2 --q8                   # two cases per domain on the Q8_0 lane
```

## Measured

_To be filled from the CLI runs on this branch (desktop FP32 and Q8_0 with the native FFM kernels,
the web build under headless Chromium) before the PR is marked ready._ The transformers library's
gated test (`LayaMoeEvalTest`) already replays the published evaluation: on the 18 cases it covers by
default, the routing is identical to the PyTorch run and 612 answer values agree to within 5e-5.

## Architecture

```
LayaMoE/
├── shared/           common: Checkpoints (pinned HF files), ModelFiles seam, LayaMoeRouter, Presets,
│                     QuestionsJson (SDK shape ↔ LayaQuestion), MixtureService (load + ask),
│                     MixtureClient (in-process | worker), MoeViewModel, WorkerProtocol
│   ├── jvmAndroidMain  FileModelFiles — download via skainet-data-source, open via openRandomAccessSource
│   ├── jvmMain / androidMain / wasmJsMain  Platform actuals; WebModelFiles + WorkerMixtureClient (web)
│   └── commonTest    question JSON, presets, protocol, DTOs, view model with a fake client
├── webWorker/        the Web Worker: MixtureService on WebModelFiles, Q8_0, SIMD128 sidecar
├── composeApp/       Compose Multiplatform UI (state, questions, expert, compare, threshold, result);
│                     desktop and web entry points; the web shell (index.html, app.html, coi-serviceworker)
├── androidApp/       MainActivity + LayaMoeApp (filesDir, Q8_0, device memory note)
├── cli/              ask | preset | bench | eval (the 108 cases bundled under src/main/resources/eval)
└── THIRD_PARTY_LICENSES/
```

Library pieces this example rests on (SKaiNET-transformers 0.63.0, SKaiNET 0.62.0):
`LayaModelLoader` (common, any `RandomAccessSource`), `LayaExpertLoader` / `LayaExpert`,
`LayaRouterSpec`, `LayaMixtureAgent`, `LayaMixtureDecisionAdapter` for the typed decision
runtime, `LayaWeightForms.q8_0For` for the packed lane, `JsBlobRandomAccessSource.openSync`
and the SIMD128 sidecar on the web.

## Out of scope

- The trained router of the layaMOE repository (a LayerNorm + linear classifier over the encoder's
  state tokens) is not published; the prompted router is what the page and this port use.
- One encoder pass for all questions of a request. Every question is its own sequence (the
  question text is part of it), so a request costs one encoder pass per question plus one for the
  router; the padded batch path of the library amortises some of it.
- iOS: the Laya and decisions modules of SKaiNET-transformers publish no Apple targets.

## Licenses

MIT for this example. Third-party content and licenses: `THIRD_PARTY_LICENSES/NOTICE` — the Laya
checkpoint (ConvAI Innovations, Apache-2.0; ModernBERT-large by Answer.AI and LightOn), the layaMOE
expert heads, router, presets and evaluation cases (vishalmysore, Apache-2.0, unofficial), and the
`coi-serviceworker` shim (MIT).
