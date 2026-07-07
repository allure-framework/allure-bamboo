# Project Guide

Use [Allure Agent Mode](docs/allure-agent-mode.md) for all test-related work in this repository.

A test run is an instrument, not a pass/fail to scrape. Running tests through `allure agent` turns them into a reviewable account of what actually happened — steps, attachments, assertion evidence — plus automated findings about the tests themselves (for example, a test that ran with no assertions). State what the run should confirm with `--goal`; the report carries the goal and the evidence, so you, another agent, or a human can validate it. The console gives a status; the agent output gives understanding and a critique.

- Run result-informing test commands as `allure agent --goal "<what this run should confirm>" -- <test cmd>`, then read the agent output (the output dir's `AGENTS.md` guide, `index.md`, the findings) — not the console tail.
- On failure: read the exact failures and evidence from the output and fix in place; confirm flakiness by rerunning just the failed tests through agent mode. For CI failures, when `docs/allure-agent-mode.md` confirms `allure agent inspect` support, inspect the downloaded `allure-results` instead of reproducing locally; otherwise follow the fallback it documents.
- Use the relevant test under agent mode as a debugging instrument — it already exercises the behavior and captures the evidence — rather than ad-hoc scripts or driving a browser MCP by hand.
- Before writing or materially changing a test, invoke the `$allure-agent-mode` skill and read its test-design rules; if it isn't installed, follow `docs/allure-agent-mode.md`.
- Read `docs/allure-agent-mode.md` for the local commands, capabilities, and conventions.

The run prints a summary digest to stdout (counts, findings, report link) — read it, then open the agent output for depth. Reducing the run with `tail`/`grep`/`head` or `>/dev/null`, or stopping at the counts, throws away the findings and evidence — the agent output is the signal.
