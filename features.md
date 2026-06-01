# Features — ReporteCiudadanoAdmin

Feature tracking for the admin dashboard. Each entry follows the pipeline:
`Draft → Design → Ready → In Progress → Done`

---

## Feature Template

```
### FEAT-NNN — Feature Name
**Status**: Draft | Design | Ready | In Progress | Done
**Branch**: feature/feat-NNN-short-name

#### Architect Notes
(filled by architect agent)

#### User Story
As a [role], I want [action] so that [benefit].

#### Acceptance Criteria
- [ ] Given ... when ... then ...

#### UX/UI Proposal
(filled by designer agent)
```

---

### FEAT-002 — Gradle 9 Upgrade
**Status**: Done
**Branch**: `feature/feat-002-gradle-9-upgrade`

#### Architect Notes
Bump Gradle wrapper from 8.10.2 to 9.5.1. Switch Shadow plugin from `com.github.johnrengelman.shadow` (dropped Gradle 9 support) to `com.gradleup.shadow` at 9.0.0. Bump Kotlin to 2.3.0, Compose Multiplatform to 1.8.0, and kotlinx-serialization to 1.8.0. Frontend dev server pinned to port 3000 via `devServerProperty` in `runTask`.

#### User Story
As a developer, I want the build toolchain on Gradle 9 so that the project stays on a supported version and benefits from improved build performance and APIs.

#### Acceptance Criteria
- [x] `./gradlew :backend:compileKotlin` succeeds on Gradle 9.5.1
- [x] `./gradlew :backend:shadowJar` produces a runnable fat JAR
- [x] `./gradlew :frontend:wasmJsBrowserDevelopmentRun` starts the dev server
- [x] `docker compose up` starts and `GET /health` returns `200 ok`

---

### FEAT-001 — Project Scaffolding & Gradle Setup
**Status**: Done
**Branch**: `feature/feat-001-scaffold`

#### Architect Notes
Multi-project Gradle setup with `:backend` (Ktor JVM, Netty) and `:frontend` (Compose Multiplatform wasmJs). Version catalog in `gradle/libs.versions.toml`. Backend fat JAR via Shadow plugin. Gradle wrapper bootstrapped from mobile project. Domain models (`Report`, `ReportStatus`) declared. Dockerfile + docker-compose for local dev.

#### User Story
As a developer, I want a working Gradle project scaffold so that I can build and run both the backend and frontend from a single repository.

#### Acceptance Criteria
- [x] `./gradlew :backend:compileKotlin` succeeds with no errors
- [x] `./gradlew :backend:shadowJar` produces a runnable fat JAR
- [x] `backend/Dockerfile` builds a runnable image (multi-stage, eclipse-temurin:21)
- [x] `docker-compose up` starts the backend on port 8080
- [x] `GET /health` returns `200 ok`
- [x] `.gitignore` excludes credentials and build artefacts
- [x] Frontend `wasmJsMain` entry point compiles without errors
