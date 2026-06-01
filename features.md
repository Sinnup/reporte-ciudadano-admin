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
