# Changelog — ReporteCiudadanoAdmin

All notable changes documented here. Updated per completed feature before merging to `main`.

Format: `[version] — YYYY-MM-DD`

Versioning: Semantic Versioning (`MAJOR.MINOR.PATCH`)
- `MAJOR`: breaking change
- `MINOR`: new backward-compatible feature
- `PATCH`: backward-compatible bug fix

---

## [Unreleased]

---

## [0.1.0] — 2026-06-01

### Added — FEAT-001 Project Scaffolding

- Gradle multi-project setup (`:backend`, `:frontend`) with version catalog (`gradle/libs.versions.toml`)
- `:backend` — Ktor 3.1.3 (Netty) JVM module; Shadow plugin fat JAR; `GET /health` route; domain models `Report` + `ReportStatus`
- `:frontend` — Compose Multiplatform 1.7.3 wasmJs module; placeholder `ComposeViewport` entry point; `index.html` with Leaflet 1.9.4 pre-loaded
- `backend/Dockerfile` — multi-stage build on eclipse-temurin:21; minimal JRE runtime image
- `docker-compose.yml` — local dev stack with `~/.aws` credentials mount
- `.gitignore` — excludes build artefacts and credentials
- `CLAUDE.md` — self-contained project context for AI sessions
