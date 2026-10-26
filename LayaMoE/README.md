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

> The Q8_0 lane on the desktop JVM needs SKaiNET 0.62.1 or later (the pin here is 0.62.4): on 0.62.0 the native FFM bindings
> of the Q8_0 / Q4_0 / Q5_0 / Q5_1 kernels zeroed every row but the last of a multi-row product, so a Laya
> request on that lane answered every question with uniform probabilities. Found while building this
> example, fixed in SKaiNET 0.62.1 (PR #1436). The FP32 lane was unaffected, and so was the browser's
> Q8_0 lane (the SIMD128 sidecar has its own bindings).

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

Desktop, 12-core laptop, JDK 21, SKaiNET 0.62.0 (FP32 rows; the Q8_0 rows on 0.62.1) with the native FFM kernels, transformers 0.63.0,
FP32 lane, 2026-10-21, `cli eval` over all 108 cases (312 answers). The accuracies are the ones
layaMOE published for its PyTorch run — the port reproduces them to the answer (the library's gated
`LayaMoeEvalTest` checks the probabilities themselves, within 5e-5 of the published values).

| domain | cases | answers | general | routed | oracle | routed to | ms/case |
|---|---|---|---|---|---|---|---|
| support-tickets | 12 | 36 | 66.7 % | 83.3 % | 80.6 % | customer_ops 11, general 1 | 16640 |
| product-reviews | 12 | 36 | 77.8 % | 77.8 % | 77.8 % | general 9, customer_ops 3 | 13740 |
| agent-guardrails | 12 | 48 | 39.6 % | 66.7 % | 66.7 % | safety 12 | 15364 |
| it-incidents | 12 | 36 | 61.1 % | 61.1 % | 61.1 % | general 12 | 11914 |
| content-moderation | 12 | 36 | 75.0 % | 75.0 % | 83.3 % | general 8, safety 3, customer_ops 1 | 11749 |
| email-triage | 12 | 36 | 52.8 % | 50.0 % | 58.3 % | safety 2, general 5, customer_ops 5 | 12798 |
| delivery-exceptions | 12 | 36 | 47.2 % | 63.9 % | 63.9 % | customer_ops 11, general 1 | 14085 |
| sales-leads | 12 | 24 | 41.7 % | 41.7 % | 41.7 % | general 12 | 10661 |
| patient-messages | 12 | 24 | 83.3 % | 83.3 % | 83.3 % | general 12 | 10539 |
| **all** | 108 | 312 | **59.6 %** | **67.3 %** | 68.9 % | customer_ops 31, general 60, safety 17 | 13054 |

With an expert available (five domains): general 55.2 %, routed 67.7 %, oracle 70.3 %; without: 66.7 %
either way. Twelve cases per domain, so differences under about ten points are noise — the README of
layaMOE says the same. `general` is the original head, `routed` the prompted router's pick, `oracle` the
expert of the case's domain. A score counts as right when its most probable level is the label, a
yes/no at `P(true) ≥ 0.5`.

Cost on the same machine: model load 13 s (843 MB F16 up-cast to FP32 plus two heads); the
"drop a production table" preset (three questions plus the router, 370 tokens) 13.0 s per request;
the support-ticket preset (JSON state, four-option choice, 433 tokens) 18.3 s. Every question is
one ModernBERT-large encoder pass (28 layers, hidden 1024) and the router adds one more; the two
head blocks and the scorer are about a tenth of it.

**Web** (the same laptop, headless Chromium on a cross-origin-isolated page, `Wasm SIMD128 (threads)`,
Q8_0 lane in the Web Worker): first visit downloads the 1.05 GB in 95 s and keeps it in the Cache
API; building the Q8_0 model takes 104 s (the base, plus 2 s for the two heads), so the page is ready
about 1 min 50 s after "Load model" on a cached visit and about 3 min 30 s on the first. The
"drop a production table" preset then takes 31.9 s (encoder 30.0 s, router and heads 1.9 s) and
answers exactly like the desktop within quantization noise: destructive 67.2 % (FP32: 65.1 %),
needs_human 21.7 % (23.3 %), risk 2.12 (2.11), routed to `safety` at 0.746 on `agent_action`.

**Desktop, FP32 lane on SKaiNET 0.62.2** (same machine, 2026-10-24): the same preset answers in 6.5 s per request
(three questions plus the router, 370 tokens; 6495 / 6469 / 6424 ms over three repetitions after a warm-up) against
13.0 s on 0.62.1. The engine's dense FP32 `x · Wᵀ` kernel, two thirds of a request, now runs on every core and, on
x86-64, on AVX2/FMA; routing and answers are unchanged.

**Desktop, Q8_0 lane** (same machine, SKaiNET 0.62.1): the "drop a production table" preset
answers like the FP32 lane within quantization noise (destructive 67.2 %, needs_human 21.7 %, risk 2.12 —
the browser's Q8_0 figures to the digit) but takes 25.3 s per request against 13.0 s on FP32: on the eager
JVM path the packed kernels run one activation row at a time, so that lane is a memory measure (about
450 MB of weights instead of 1.6 GB), not a speed one. A two-case-per-domain `eval --q8` on the same
engine routed 18 cases at 26.3 s each; with twelve-case domains the accuracy table above is the one to
quote.

_Android: the APK builds (arm64-v8a, Q8_0 lane, JNI NEON kernels); numbers from a device follow._

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

Library pieces this example rests on (SKaiNET-transformers 0.63.0, SKaiNET 0.62.4):
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
