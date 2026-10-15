# TDA Lab — SKaiNET example

Interactive **topological data analysis** on a 2-D point cloud: grow the Vietoris-Rips radius ε
with a slider and watch edges appear, components merge, loops open and close, and the
persistence barcode fill in. One Kotlin codebase for Android, iOS, Desktop (JVM) and the
browser (WebAssembly).

It is a Kotlin Multiplatform re-implementation of [TDA Lab](https://github.com/Starchild13/TDAProject),
an Android app by Starchild13. The idea, the screens and the explanatory texts follow the
original; the numerics were rewritten so that H₁ is computed by a real ℤ/2 boundary-matrix
reduction instead of a heuristic, and the distance matrix is a SKaiNET tensor expression.

## Modules

- **`:tda`** — the library, no UI, no platform I/O:
  - `PointCloud` — flat `[x0, y0, x1, y1, …]` storage (the row-major `[n, 2]` tensor layout),
    CSV parsing, centering, subsampling.
  - `DistanceMatrix` — `compute()` builds the `n × n` Euclidean distances with SKaiNET
    (`D² = s[:,None] + s[None,:] − 2·X·Xᵀ`, then clamp and sqrt); `plain()` is the scalar
    reference the tests compare against. Also the *enclosing radius*, the natural end of the
    filtration.
  - `Filtration` — all edges sorted by length; the 1-skeleton at ε is a prefix.
  - `VietorisRips.persistence()` — H₀ by union-find, H₁ by column reduction over ℤ/2 with
    triangles generated per maximal edge (apparent pairs cost O(1)). Returns a
    `PersistenceDiagram` with `betti(dim, ε)` and `bars(dim)`.
  - `Datasets` — synthetic clouds with known topology (circle, two circles, figure eight,
    blobs, noise) so the app and the tests work offline.
- **`:app`** — Compose Multiplatform UI on the shared `skainet-ui` design system: dataset
  picker and CSV paste, ε slider, zoomable scatter plot with the current edges, Betti numbers in
  `H₁ ≅ ℤ ⊕ ℤ` notation, barcode and persistence diagram, and a "How it works" page.
- **`:androidApp`** — the Android entry point (AGP 9 keeps `com.android.application` out of
  KMP modules).

## Run

```bash
./gradlew :tda:runDemo                        # barcodes of the bundled datasets on the JVM
./gradlew :app:run                            # desktop UI
./gradlew :app:wasmJsBrowserDevelopmentRun    # web UI in the browser
./gradlew :androidApp:assembleDebug           # Android APK (needs an Android SDK)
```

iOS targets are declared and build on a macOS host (`:app` produces a `TDAApp` framework).

## Tests

```bash
./gradlew :tda:jvmTest :tda:jsNodeTest :tda:wasmJsNodeTest
```

The same tests run on all three targets: CSV parsing, SKaiNET vs. scalar distance matrix,
filtration order, and persistence on fixtures with known answers (unit square: one loop born at
1 and dying at √2; a 40-gon: one loop dying at r·√3; two circles: two loops and two
components; blobs: no loop).

## Where SKaiNET is used, and where it is not

The dense numeric step, the distance matrix, is a tensor expression and runs in the backend's
vectorised kernels on every platform. The combinatorial steps (sorting edges, union-find,
sparse ℤ/2 column reduction) are integer and sparse work, not tensor operations, so they are
ordinary Kotlin living next to the tensors. This is the same split as the GloVe example:
plain Kotlin for the pipeline, SKaiNET for the dense linear algebra.

What this example needed and SKaiNET does not have yet: pairwise distance (`cdist`), `max`/`min`
reductions, `argsort`/`topK`, and a mask-to-indices (`nonzero`) op. The point budget in the UI
exists because H₁ reduction visits every triangle; 150 points is instant, 400 takes a few
seconds in the browser.

## Point budget and precision

Everything is FP32, which SKaiNET's CPU backend supports; the cloud is centred before the
`‖a‖² + ‖b‖² − 2a·b` formula is applied, otherwise FP32 cancellation corrupts small distances
between far-away points. Pasted clouds larger than the point budget are randomly subsampled.
