# Package organization implementation plan

> Execute inline, using the approved proposal in the conversation. The existing structure-refactor.md is excluded from this work.

**Goal:** Organize the single Maven application by feature, normalize Java packages, and document module ownership without changing public HTTP APIs, database schemas, or distribution layout.

**Architecture:** Bootstrap wires the application. Bot contains message dispatch contracts and sessions; integration.onebot contains protocol adapters. Features own handlers, services, persistence and models. Configuration and infrastructure provide shared capabilities.

**Tech stack:** Java 21, Spring Boot, MyBatis, Maven, Vue 2.

- [x] Establish baseline compilation and inspect runtime class/package references.
- [x] Move Java sources and tests to com.haruhi.botserver, grouped by responsibility; update imports, XML, logging, AOP and reflection references.
- [x] Correct class spelling, move assembly descriptor and build scripts, update build references and frontend package identity.
- [x] Document architecture, ownership, migration compatibility and development commands.
- [x] Run clean compilation, 63 core regression tests, Maven packaging and frontend build; inspect changed files and remaining legacy references.

The initial full test run had 29 errors due to stale generated YAML. After cleaning, subsequent tests exposed network-dependent cases and cross-context static handler accumulation. Registries are now instance-owned, with a regression test. The final external-service suite was blocked by automatic approval review and requires explicit authorization; it is not claimed to pass. The final local regression disables public-IP discovery via internet-host=127.0.0.1.

Validation must include Spring context and Mapper loading. Preserve external configuration behavior; support the old logging category during migration. Do not delete demo routes without proving they are unused. Structural moves and naming changes precede any future behavioral decomposition.
