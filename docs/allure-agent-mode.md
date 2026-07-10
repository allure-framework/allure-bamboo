# Allure Agent Mode

Use Allure agent mode to design, review, validate, debug, and enrich tests in this project.

This file is project-specific guidance; the durable agent-mode, test-design, expectation, and evidence rules live in the `allure-agent-mode` skill. Before authoring or materially changing a test, invoke the `$allure-agent-mode` skill and read its `references/test-design.md` — do not author tests from general knowledge. If the skill is not installed, use this file plus the core non-negotiables below as the floor and keep conclusions conservative.

## Why Agent Mode

A test run is an instrument, not a pass/fail to scrape. Running tests through `allure agent` turns them into a reviewable account of what actually happened — steps, attachments, assertion evidence — plus automated findings about the tests themselves (for example, a test that ran with no assertions). State what the run should confirm with `--goal`; the report carries the goal and the evidence, so you, an upstream agent, or a human can validate it. The console gives a status; the agent output gives understanding and a critique.

Reach for agent mode through the loops in [Core Loops](#core-loops): run with a goal, make evidence checkable with expectations, triage failures and rerun just the failed tests, inspect existing results without re-running, and use a test as a debugging instrument. The run prints a summary digest to stdout (counts, findings, report link); read it, then open the agent output for depth (start at the output dir's `AGENTS.md` guide). Run `allure agent capabilities` to confirm the local surface. Reducing the run with `tail`/`grep`/`head` or `>/dev/null`, or stopping at the counts, throws away the findings and evidence — the agent output is the signal.

## Local Capability Snapshot

Refresh this section when Allure, test runners, Allure results paths, CI, or project wrappers change.

- Allure wrapper: system-wide `allure` (Allure 3 CLI); no project-local wrapper
- Capability snapshot last checked: 2026-07-07
- Refresh capabilities with: `allure --version`, `allure agent --help`, and `allure agent capabilities --json`
- Agent execution: supported via flags — `allure agent --goal "<claim>" [--expect-* ...] -- <command>`
- **Interface warning:** the `ALLURE_AGENT_OUTPUT` / `ALLURE_AGENT_EXPECTATIONS` environment variables documented in an older revision of this file are **not honored** by the local CLI. Use the flag interface only. The older `allure run` spelling is also superseded by `allure agent`.
- Output option: `--output <dir>`; without it the CLI creates and prints a temp output directory
- HTML report mode: `--report auto|off|awesome|config` (default `auto`: generates a single-file Awesome report at `<output>/awesome/index.html` when the run stores ≤ 1000 visible results; status in `manifest/human-report.json`)
- Expectation controls: inline flags `--goal`, `--task-id`, `--expect-tests <n>`, `--expect-test <full name>`, `--expect-prefix <full-name prefix>`, `--expect-label <k=v>`, `--expect-env <k=v>`, `--forbid-label <k=v>`, `--expect-steps <n>`, `--expect-step-containing <substr>`, `--expect-attachments <n>`, `--expect-attachment <filter>`; file mode `--expectations <file>` (YAML or JSON) for large/generated contracts only
- Latest/state directory recovery: `allure agent latest`; `allure agent state-dir`; override with `ALLURE_AGENT_STATE_DIR`
- Selection/rerun support: `allure agent select (--latest | --from <dir>) --preset review|failed|unsuccessful|all`; rerun via `allure agent --rerun-latest -- <cmd>` or `--rerun-from <dir>` with `--rerun-preset` (test-plan transport `ALLURE_TESTPLAN_PATH`; JUnit 4 adapter honoring is unverified here — see [Output, State, And Reruns](#output-state-and-reruns))
- Existing-result or dump inspection: `allure agent inspect [<allure-results-dir-or-glob> ...]` and repeated `--dump <archive-or-glob>`
- Existing-result fallback commands: `allure generate` (uses `allurerc.mjs`, report to `./target/allure-report`) and `allure open`; weaker than inspect — prefer inspect
- Capability/helper commands: `capabilities`, `latest`, `query`, `select`, `state-dir` all supported

## Local Test Surfaces

- Test frameworks and runners: JUnit 4 via Maven Surefire (`./mvnw test`), Java 21; Mockito and AspectJ weaver attached as `-javaagent`s in the Surefire `argLine`
- Test roots: `src/test/java/io/qameta/allure/bamboo` (subpackages: root, `util`, `config`)
- Allure results path: `target/allure-results` (set in `src/test/resources/allure.properties`)
- Known selector support: Surefire `-Dtest=ClassName`, `-Dtest=ClassName#method`, and patterns like `-Dtest='Allure*Test'`
- Known environments needed for tests: JDK 21 (CI parity). The default system JDK here can be much newer and breaks the build (PMD dies with `StackOverflowError` on JDK 25); prefix local runs with `JAVA_HOME=$(/usr/libexec/java_home -v 21)` when `./mvnw` fails before Surefire
- Compatibility smoke scope: `compat/bamboo-specs` harness (Maven `exec:java`); **emits no Allure results** — an agent run over it reports zero logical tests by design. Review global stdout in the agent output plus `compat-artifacts/<version>/summary.md`. Requires a plugin JAR in `target/` (`./mvnw clean package` first) and a Bamboo DC timebomb license via `-Dcompat.productLicense`
- Manual Bamboo investigation via `atlas-run` is for local debugging only; it is not a substitute for agent-mode evidence from the narrowest automated scope

## Allure Integrations

- Existing Allure adapters/integrations: `allure-junit4` (Surefire listener `io.qameta.allure.junit4.AllureJunit4`) and `allure-assertj`, versions managed by `allure-bom`
- Runner config files: `pom.xml` (Surefire plugin: listener + `argLine` javaagents), `src/test/resources/allure.properties`
- Result-path configuration: `allure.results.directory=target/allure-results` in `allure.properties`
- Global labels: `allure.label.module=allure-bamboo` in `allure.properties` puts a `module` label on every test result (verified via `--expect-label module=allure-bamboo`)
- Report config: `allurerc.mjs` (name "Allure Bamboo", report output `./target/allure-report`, awesome plugin grouped by the `module` label, optional Allure service/TestOps publishing driven by `ALLURE_SERVICE_TOKEN`/`ALLURE_TOKEN`/`ALLURE_ENDPOINT` env — TestOps self-disables outside CI)
- Assertion visibility: AssertJ assertions appear as steps through `allure-assertj` (AspectJ weaving already wired in the Surefire `argLine`)
- Known unsupported or skipped integrations: the compat harness has no Allure adapter (console + `compat-artifacts` summary only)

## Project Test-Design Conventions

Core non-negotiables (the floor when the `$allure-agent-mode` skill is not loaded; full rules in its `references/test-design.md`):

- A new or changed regression test must fail for the intended bug before the fix and pass after; if reproducing the pre-fix failure is genuinely impossible, state why and what alternative evidence proves the fix.
- Do not weaken assertions, delete coverage, or skip, mute, or quarantine tests just to make a run pass; any such change needs explicit rationale.
- Keep tests boring and explicit — prefer readable, linear tests over loops, factories, or conditionals whose only value is saving a few repeated lines.
- Do not hide missing coverage behind runtime `if` branches, early returns, conditional test registration, or helper aliases; use the runner's explicit skip/assumption with a stated reason.
- Steps, attachments, parameters, and labels must reflect real behavior from the current run, not filler.

Project specifics:

- Accepted test layers: JUnit 4 unit tests (main suite); Bamboo compatibility smoke via the compat harness
- Framework: JUnit 4 only — do not introduce JUnit 5 imports or idioms into this suite
- Preferred assertion style: AssertJ (`assertThat...`), visible as steps via `allure-assertj`; Mockito for mocking
- Evidence helpers: `TestSupport.step(name, action)`, `TestSupport.attachText(name, value)`, `TestSupport.attachDirectoryTree(name, root)` — instrument at helper boundaries rather than wrapping every line
- Explicit skip/assumption mechanics: JUnit 4 `@Ignore(reason)` or `org.junit.Assume`; no silent conditional returns
- Suppression/quarantine policy: unknown — none documented in this repo

## Run Profiles

| Profile | Command or profile intent | Expected use | Confidence limits |
| --- | --- | --- | --- |
| single class | `allure agent --goal "<claim>" -- ./mvnw -Dtest=AllureExecutableTest test` | Focused validation of one class or a fixed change | Proves only the selected scope |
| unit suite | `allure agent --goal "<claim>" --expect-label module=allure-bamboo -- ./mvnw test` | Broad unit validation before conclusions about the plugin | No Bamboo integration coverage |
| compat smoke | `allure agent --goal "<claim>" -- ./mvnw -q -f compat/bamboo-specs/pom.xml -Dcompat.rootDir="$(pwd)" -Dcompat.version=<v> -Dcompat.productLicense='<license>' exec:java` | Real-Bamboo smoke for one version (after `./mvnw clean package`) | Zero logical tests by design; review global stdout + `compat-artifacts/<v>/summary.md`; needs license; slow |
| full verify | `./mvnw clean verify` | CI-equivalent gate (tests + checkstyle, spotbugs, spotless, jacoco) | Static checks dominate the runtime; for test evidence wrap it in `allure agent` |

## Execution Signal And CI Trust

Do not present ignored, excluded, swallowed, advisory, or non-gating test execution as proof that behavior is safe.

- Default local test command: `./mvnw test` (no default exclusions known)
- CI test jobs: GitHub Actions `Build` (`.github/workflows/build.yml`) runs `./mvnw clean verify -q` wrapped in `allure run --dump` on PRs and pushes to `main` — gating; a follow-up `report` job generates the Allure report and posts a PR summary (advisory only, runs even when the build fails)
- Compatibility smoke (`.github/workflows/nightly-compat.yml`) is **manual** (`workflow_dispatch` only, despite the filename) and needs the `BAMBOO_COMPAT_PRODUCT_LICENSE` secret — do not treat it as continuous coverage
- Test artifacts retained by CI: `Build` uploads the Allure dump `allure-results-build` (zip, 7-day retention), the generated `allure-report` (7-day retention), and the jacoco report; the compat workflow uploads `compat-artifacts/<version>` (summary + logs, no Allure results)
- Local or CI results/dumps suitable for `allure agent inspect`: local `target/allure-results` from a prior run; the `allure-results-build` dump artifact from any `Build` run within its 7-day retention (`gh run download <run-id> -n allure-results-build`, then `allure agent inspect --dump allure-results-build.zip`)

## Local Expectation Controls

Before each validation run, decide whether expectations reduce a real risk for the intended conclusion. When they do, use the smallest fresh inline options.

- Supported mechanism: inline CLI flags (see Capability Snapshot); `--expectations <file>` (YAML/JSON) only for large or generated contracts
- **Suite-wide scope: use `--expect-label module=allure-bamboo`** (global label from `allure.properties`, covers all subpackages) or `--expect-prefix io.qameta.allure.bamboo.` — both verified. Do not use `--expect-label package=io.qameta.allure.bamboo`: the `package` label is per-subpackage (`io.qameta.allure.bamboo`, `...bamboo.util`, `...bamboo.config`) and silently misses the subpackages
- Exact-test scope: `--expect-test <full name>` or `--expect-tests <count>` with a `-Dtest=` selection
- Excluded-scope controls: `--forbid-label <k=v>` only
- Evidence expectations: `--expect-steps`, `--expect-step-containing`, `--expect-attachments`, `--expect-attachment` (by name / content-type)
- Compat runs: expectations on logical tests are pointless (zero tests by design) — use `--goal` only

Expectations are fresh per run and state only what matters for this run: the claim, what should and should not run, and what evidence must be visible. Treat the goal as a claim boundary for review, not proof — if the goal is stale, keep the evidence and report what the run actually supports.

## Core Loops

### Test Review Loop

1. Identify the exact review scope and validation depth.
2. Add the smallest meaningful inline expectations when they protect the conclusion.
3. Choose report mode by audience: `--report off` for iterative agent-only loops, `--report auto|awesome|config` for final user-reviewable runs.
4. Run only that scope through `allure agent`.
5. Print the run's `index.md` path; for final runs share the report link (see Acceptance Rules).
6. Review `index.md`, `manifest/run.json`, `manifest/tests.jsonl`, `manifest/findings.jsonl`, `manifest/expected.json` (when expectations were used), and relevant per-test markdown.
7. Inspect source code only after runtime evidence explains what executed.
8. Call out weak scope, weak evidence, execution-signal limits, or partial runtime modeling.

### Existing Result Inspection Loop

Use when `target/allure-results` already holds results from a prior local run, or a CI run retained its Allure dump.

1. Local results: `allure agent inspect target/allure-results --goal "<what to review>"` (add `--report off` for intermediate passes).
2. CI failures: download the dump artifact (`gh run download <run-id> -n allure-results-build`), then `allure agent inspect --dump allure-results-build.zip --goal "<what to review>"` — prefer this over reproducing locally.
3. Review the produced agent output exactly like a live run (`index.md`, manifests, per-test markdown).
4. If an HTML report is needed, check `manifest/human-report.json` before regenerating anything.
5. Keep staleness explicit: inspected results reflect the commit and time they were produced, not the current working tree.
6. If the dump artifact is expired (7-day retention) or missing (for example, the build failed before tests ran), fall back to local reproduction at the narrowest scope.

### Test Authoring Loop

1. Understand the feature, issue, expected behavior, and risk.
2. Read the `$allure-agent-mode` skill's test-design guidance when available.
3. Add the smallest meaningful expectations for the intended scope.
4. Write or update focused JUnit 4 tests without weakening useful coverage.
5. Run the intended scope through agent mode (`-Dtest=` for the touched classes).
6. Review scope, checks, evidence, and execution signal before claiming validation.
7. Enrich tests when evidence is weak, then rerun with fresh agent output.

### Evidence And Metadata Enrichment Loop

Use when tests pass but are hard to review:

1. Identify weak evidence, missing checks, missing setup state, or noisy metadata from the findings.
2. Prefer `TestSupport` helper-boundary instrumentation (`step`, `attachText`, `attachDirectoryTree`) over wrapping every line.
3. Keep per-test intent metadata inline with each test; redact sensitive values (for example, license strings) while preserving artifact shape.
4. Rerun the same intended scope and report evidence changes.

### Failure Triage And Rerun Loop

1. Get the run output (`allure agent latest` when the run used the default temp directory) and read `index.md`, `manifest/findings.jsonl`, and the failing per-test markdown before touching source.
2. When a runner-visible failure is missing from `manifest/tests.jsonl` (compile errors, suite-load failures), inspect `artifacts/global/` (for example stderr) and treat the run as a partial runtime review.
3. Classify each failure before editing: product bug, stale test, wrong expectation, fixture/environment problem, or flake. Do not weaken assertions or skip tests just to make the run pass.
4. Rerun the narrowest scope with fresh output. Prefer `allure agent --rerun-latest --rerun-preset failed -- ./mvnw test`; on first use confirm the observed scope actually narrowed (JUnit 4 test-plan honoring is unverified here) and fall back to explicit `-Dtest=Failing1,Failing2` if it did not.
5. Re-review the new output and keep flake and non-gating limits explicit before calling the failure resolved.

### Coverage Review Loop

1. Split broad audits into scoped groups (one test class, one subpackage, or one compat version).
2. Give concurrent groups their own caller-managed `--output` directories; sequential groups can use the default temp output.
3. Run each group through agent mode and review runtime artifacts before source.
4. Separate observed runtime coverage from inferred source-code coverage.
5. Mark the review incomplete until every group was validated through matched expectations, reviewed observed scope, or documented as a broad package-health audit.

## Runtime Artifact Review

After each agent-mode run:

- open the agent-output directory's `AGENTS.md` guide first; it carries the reading order and command map for that run
- read `manifest/run.json` (canonical run summary), then `manifest/test-events.jsonl`
- read `index.md` for the triage overview, and print its path so collaborators can open it
- read `manifest/tests.jsonl` and `manifest/findings.jsonl`
- read `manifest/expected.json` when the run used expectations
- read relevant per-test markdown before inspecting source
- inspect `artifacts/global/` (stderr/stdout) when runner-visible failures are not represented as logical tests — always for compat runs, whose only runtime signal lives there and in `compat-artifacts/<version>/`
- use `allure agent query (--latest | --from <dir>) [summary|tests|findings|test]` for focused JSON instead of hand-parsing manifests

## Output, State, And Reruns

Do not create persistent agent output or expectation paths. `allure agent` creates and prints a temp output directory when no `--output` is given and removes the previous run's CLI-provided temp output on the next run. An explicit `--output` path is caller-managed — remove or archive it when done. The framework's `target/allure-results` is separate reporting configuration; clearing it between runs is unnecessary (agent mode detects only the current run's new results).

- Agent output policy: CLI-provided temp dir by default; explicit `--output` only when organizing parallel or grouped runs
- Agent output cleanup: CLI-managed for default output; caller-managed for explicit `--output`
- Latest output recovery: `allure agent latest`
- State directory: `allure agent state-dir`; override with `ALLURE_AGENT_STATE_DIR`
- Rerun from prior output: `--rerun-latest` / `--rerun-from <dir>` with `--rerun-preset review|failed|unsuccessful|all` (transport `ALLURE_TESTPLAN_PATH`); JUnit 4 adapter honoring unverified — confirm observed scope on first use, fall back to `-Dtest=`
- Selection/test plan: `allure agent select (--latest | --from <dir>) --preset ... [-o testplan.json]`
- Inspect existing results/dumps: `allure agent inspect <results-dir-or-glob> ... [--dump <archive-or-glob>]`
- HTML report: `--report auto|off|awesome|config`; status in `manifest/human-report.json`; auto generates `awesome/index.html` at ≤ 1000 stored visible results
- Non-agent fallback: `allure generate` / `allure open` with `allurerc.mjs` (report at `./target/allure-report`) — weaker than inspect; use only when agent output is impossible
- **Parallel-run rule:** each concurrent run must pass its own caller-managed `--output` directory — the default temp output is unsafe for concurrency because each run clears the previous run's temp output — and output paths and expectation state must never be shared
- CI artifact retention: Allure dump `allure-results-build` and generated `allure-report` (7 days each) plus jacoco report (Build); `compat-artifacts/<version>` (compat workflow); no agent output is retained — inspect dumps locally instead

## Project Metadata Conventions

Per-test metadata belongs inline with the test. Do not centralize descriptions, labels, links, parameters, or intent-defining step names in helper wrappers or lookup tables keyed by test name. `TestSupport` handles mechanics only.

- Module label: `module=allure-bamboo` on every main-suite result, set globally in `src/test/resources/allure.properties` (`allure.label.module=`); the awesome report groups by it. A future second results-emitting module should set its own distinct `module` value the same way
- Suite/package taxonomy: `package` label is per-subpackage (`io.qameta.allure.bamboo`, `.util`, `.config`) — for suite-wide scope prefer `--expect-label module=allure-bamboo`
- Feature/story/severity/owner labels: none in use — do not invent taxonomy
- Issue links: unknown — none documented
- Metadata to avoid: decorative labels no review step uses

## Project Evidence Conventions

- Steps: `TestSupport.step("<what this block does>", ...)` around real setup, actions, or state transitions
- Attachments: `TestSupport.attachText` for textual runtime evidence; `TestSupport.attachDirectoryTree` for filesystem/fixture state
- Check/assertion visibility: AssertJ assertions surface as steps via `allure-assertj`
- Fixture/setup evidence: directory trees and generated fixture archives (see `TestSupport.createAllureDistributionZip`, `writeMinimalReport`)
- Sensitive data: never attach real Bamboo license strings; redact while preserving shape

## Acceptance Rules

Accept a run only when:

- observed scope matches the intended scope, or drift is explained
- coverage remains meaningful for the stated conclusion
- important checks are visible through supported reporting or source review covers the gap
- evidence is strong enough to explain what happened
- execution-signal limits are explicit (including the compat harness's zero-logical-tests shape)
- no high-confidence placeholder or noop evidence findings remain
- partial runtime modeling is called out

When local Allure results already exist, prefer `allure agent inspect` output over parsing raw files or generated HTML reports.

For final user-facing runs, include the generated report link when `manifest/human-report.json` reports `generated`; otherwise state the manifest status if a report was expected. Resolve relative manifest paths (such as `awesome/index.html`) against the agent output directory. Say `Here is the report: <link>` with a normal Markdown link to the absolute local report file — do not wrap the link in inline code.

If agent output is absent or incomplete, fix that first; do not silently accept console-only conclusions. Fall back to console only when agent mode is genuinely impossible, then name the blocker and mark the conclusion provisional.
