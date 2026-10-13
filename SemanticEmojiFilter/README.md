# Semantic Emoji Filter

Type what you mean, get the emoji. A Compose Multiplatform app (Android, Desktop, Web) that ranks
a bilingual catalog of about 200 emoji by the meaning of a free-text query, on-device, with
SKaiNET. Desktop and Android add a second stage in which a typed decision model re-checks the top
hits, the part embeddings alone get wrong.

## What it demonstrates

**System 1: embedding filter, on every keystroke.** The query is encoded with the MongoDB LEAF
retrieval encoder (`mdbr-leaf-ir`, 23M parameters, Apache-2.0) through the SKaiNET-transformers
BERT runtime: WordPiece tokenizer from the engine, 6 encoder layers, mean pooling, the model's
`2_Dense` projection to 768 dimensions, L2 normalisation. The catalog embeddings are computed once
(or shipped precomputed for the web build), so a sweep is a dot product over 200 rows. Items whose
calibrated confidence passes the threshold move to the top tier.

**System 2: typed decisions, when the query settles.** Cosine similarity cannot do qualifiers:
"healthy things to eat" ranks 🍕 near 🥗 because *food* dominates the vector. After the query has
been stable for half a second, the top candidates go to the **Laya** decision model through the
`sk.ainet.decisions` contract, one boolean question per item ("is this emoji a good answer to the
search?"). The badge then shows both numbers, the vector confidence and Laya's probability; a
candidate the model is unsure about shows `?` instead of a guess (policy: reject below 0.2,
accept from 0.6). This stage is opt-in
(`-PlayaVerifier=true`), Desktop and Android only, and visibly asynchronous: Laya is a 322M-parameter
encoder run on the eager FP32 CPU path, several seconds per question on a laptop.

**Honest numbers.** The diagnostics row shows the kernel tier, model load time and the live query
latency; the CLI below produced the numbers in this README on the machine named there.

## Run

The LEAF checkpoint (92 MB) is downloaded on first run into `~/.skainet-examples/semantic-emoji-filter/leaf/`
(desktop, CLI) or the app's files directory (Android); the web build bundles it. Set
`LEAF_MODEL_DIR` to reuse an existing copy.

### Desktop

```bash
./gradlew :composeApp:run
# with the Laya verifier (SKaiNET-transformers 0.59.0 decisions + laya modules, from Maven Central)
./gradlew -PlayaVerifier=true :composeApp:run
```

### Android

```bash
./gradlew :androidApp:installDebug
./gradlew -PlayaVerifier=true :androidApp:installDebug   # verifier: needs a device that grants a 2 GB heap
```

### Web (Wasm)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun      # fetches the checkpoint into composeResources first
./gradlew :composeApp:wasmJsBrowserDistribution        # what the examples site deploys
```

### CLI (measure, calibrate, regenerate)

```bash
./gradlew :cli:run --args="bench"                      # load, catalog, query latency, RSS
./gradlew :cli:run --args="query healthy things to eat"
./gradlew :cli:run --args="embed-catalog"              # writes catalog-embeddings.bin + digest
./gradlew :cli:run --args="calibrate"                  # fits calibration.json on calibration-queries.json
./gradlew :cli:run --args="golden"                     # records golden-queries.json for the ranking test
```

### The Laya verifier

The typed-decision contract (`skainet-transformers-decisions-api`, `-decisions-runtime`) and the
Laya adapter (`skainet-transformers-inference-laya`) ship with SKaiNET-transformers 0.59.0, which
the version catalog pins; `-PlayaVerifier=true` adds the `:verifier` module and the Laya source
directories. The checkpoint (644 MB, `convaiinnovations/laya` `multilingual/`) downloads on the
first verification into `~/.skainet-examples/semantic-emoji-filter/laya-multilingual/`
(`LAYA_MODEL_DIR` overrides).

## Measured

Laptop, 12 cores, JDK 21 with the Vector API (`Panama Vector (SIMD)` tier), SKaiNET 0.58.0,
2026-10-13, `./gradlew :cli:run --args="bench"`:

| | DIRECT (default) | OPTIMIZED (traced graph) |
|---|---|---|
| LEAF load (SafeTensors, 92 MB) | 345 ms | 352 ms |
| catalog embedding, 208 items, once | 23.4 s (112 ms/item) | 45.2 s (217 ms/item) |
| query embedding, p50 / p95 (20 queries × 5) | 81 ms / 98 ms | 106 ms / 123 ms |
| cosine sweep over 208 × 768 | 0.28 ms | 0.26 ms |
| RSS after the run | 512 MB | 960 MB |

The query carries LEAF's retrieval prompt (`Represent this sentence for searching relevant passages: `),
which triples the token count of a short query and costs about 40 ms per keystroke; without the
prompt the ranking is useless (every cosine lands between 0.70 and 0.79 and 🍕 beats 🤒 for
"feeling sick"). OPTIMIZED traces one graph per sequence length and buys nothing here, so DIRECT is
the default. The catalog embedding is skipped when the shipped `catalog-embeddings.bin` matches the
catalog digest; the web build always uses it.

Calibration on 28 labelled English queries (151 positive pairs of 5 824): logistic fit
`k = 22.1`, `x0 = 0.279`, F1-optimal threshold 0.8 (cosine 0.341), top tier capped at 12 items;
training precision 0.60, recall 0.60. LEAF cosines are compressed (matches 0.30–0.40, the rest
0.10–0.30), which is exactly why a second stage earns its place: for "healthy things to eat" the
twelve items above the threshold include 🥬 🥗 🍎 🥒 🥕 🍅 and also 🥓 🥜 🍩 🍣.

LEAF is an English retrieval encoder; German queries work through the German names in the catalog
text but rank visibly worse, and were left out of the calibration. The Laya checkpoint is
multilingual.

**Laya verifier** (`./gradlew -PlayaVerifier=true :cli:run --args="verify ..."`, same laptop, eager
FP32 CPU path, exact GELU): checkpoint load 9.0 s, 1.8–3.6 s per decision (first one slowest),
RSS 4.8 GB.

| query | candidate | P(match) | verdict |
|---|---|---|---|
| feeling sick | 🤢 🤮 🤒 🤧 | 0.94 · 0.95 · 0.95 · 0.85 | yes, yes, yes, yes |
| healthy things to eat | 🥗 green salad | 0.34 | ? |
| | 🥬 leafy green | 0.17 | no |
| | 🍎 red apple · 🥓 bacon | 0.07 · 0.07 | no |
| | 🥜 peanuts · 🍩 doughnut | 0.03 · 0.03 | no |

The ordering is right (salad above leafy green above bacon above doughnut, under every wording
tried), the absolute probabilities are conservative: the base checkpoint is a starting point its
publisher expects to be specialised. The acceptance policy is therefore asymmetric, reject below
0.2, accept from 0.6, so the salad shows as "unsure" and the doughnut as a clear no, and the apple
is a wrong confident rejection the demo does not hide. Four wordings of the question were
compared from the CLI (`verify --statement ... --no ... --yes ...`); the default has the widest
spread between the right and the wrong answer.

## Architecture

```
shared/     commonMain  domain/   Catalog, CatalogItem.embeddingText(), Calibration (logistic fit), RankedItem/Tier
                        engine/   LeafEmbeddingEngine (BERT runtime + engine tokenizer), CatalogIndex (sweep, .bin),
                                  ByteArrayRandomAccessSource (web), LeafFiles (pinned HF revision)
                        model/    LeafProvider + LeafResolver (pure plan); jvmAndroidMain: streamed download
                        verify/   Verifier API (Verdict, VerifierState, NoopVerifier) — no Laya dependency
                        vm/       EmojiSearchViewModel: debounce 120 ms, mapLatest, stable-query trigger 500 ms
            jvmMain / androidMain / wasmJsMain   platform actuals, checkpoint providers
verifier/   jvm + android only   LayaVerifier (one request per item, cancellable between items, verdict cache),
                                 EmojiMatchSchema (decisionSchema { probability(...); policy { boolean { abstainBetween(0.4, 0.6) } } }),
                                 DecisionAdapterSource (the seam a decision cartridge replaces later), LayaFileSource
composeApp/ Compose UI; src/{jvm,android}{Verifier,NoVerifier}/ pick the verifier at build time
androidApp/ MainActivity, EmojiApp
cli/        bench | embed-catalog | calibrate | golden | query | tokens
scripts/    fetch-model.sh (LEAF, pinned revision), generate-catalog.py (CLDR -> catalog.json), catalog-selection.json
```

The ranking flow never waits for the verifier: verdicts stream into a separate state and are
cleared when the query changes. The verifier sends one request per candidate rather than one
request with many questions, because the Laya adapter runs one encoder pass per question either
way and a per-item request gives a cancellation point every few seconds instead of one long call.

## Catalog

`catalog.json` is generated from Unicode CLDR 47 annotations (English and German names and
keywords) for the emoji listed in `scripts/catalog-selection.json`; the `tags` and `group` fields
are this example's own curation so the showcase queries (healthy food, feeling sick, rock band)
have a defined answer set. The encoder never sees the tags: the text it embeds is
`"<name>. <keywords>. <German name>"` (`CatalogItem.embeddingText()`), and the calibration is
fitted on `calibration-queries.json`, 28 hand-labelled English queries. Regenerate with

```bash
python3 -I scripts/generate-catalog.py --cldr <dir with annotations_{en,de}.xml, annotationsDerived_{en,de}.xml> \
    --selection scripts/catalog-selection.json --out composeApp/src/commonMain/composeResources/files/catalog/catalog.json
./gradlew :cli:run --args="embed-catalog" && ./gradlew :cli:run --args="calibrate" && ./gradlew :cli:run --args="golden"
```

## Out of scope

* **iOS**: the bert, laya and decisions modules of SKaiNET-transformers publish no Apple targets yet.
* **A trained classifier head** over the embeddings (the PRD's "Mode B"); a follow-up example.
* **Laya in the browser**: 644 MB of weights against a web page budget. The seam is there if a
  smaller decision checkpoint appears.
* **Cartridges**: the verifier loads the checkpoint through `DecisionAdapterSource`, one file;
  a SKaiNET decision cartridge (task profile `decision/v1`, blueprint) replaces that source without
  touching the UI or the view model once it exists.

## Licenses

MIT for this example. Third-party content and licenses: `THIRD_PARTY_LICENSES/NOTICE` (LEAF and
Laya under Apache-2.0, CLDR under the Unicode License v3).
