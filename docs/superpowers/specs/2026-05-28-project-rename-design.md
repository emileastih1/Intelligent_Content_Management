# Design: Rename project from architecture_ddd to intelligent-content-management

**Date:** 2026-05-28
**Issue:** #42
**Status:** Approved

## Problem

The service carries the name `architecture_ddd` — a learning-exercise label that no longer reflects the domain it serves. All identifiers (Maven coordinates, Java package root, documentation) must align on `intelligent-content-management` / `com.ea.icm` before the Keycloak security PR (#41) lands, since that PR commits a client ID of `intelligent-content-management-api`.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Java package name | `com.ea.icm` | Short, stable abbreviation; matches Keycloak client ID prefix |
| Maven `artifactId` | `intelligent-content-management` | Matches Keycloak client ID and service identity |
| Maven `groupId` | `com.ea.icm` | Consistent with new package root |
| Filesystem folder rename | Deferred — done outside this session | Would break the open Claude Code session; safe to do after commit |
| Spring `application.name` | No change — already `intelligent-document-management` | Already partially updated |
| Docker Compose app service | No change — no app service entry exists | Postgres/ES/pgadmin service names are out of scope per issue |

## Approach: `git mv` + text replacement (Option B)

Use `git mv` to rename the Java source directories first so git tracks the renames explicitly (preserves `git blame` line-level history). Then do a bulk text replacement on package declarations and import statements in the moved files.

## Scope

### `pom.xml`
- `groupId`: `com.ea.architecture` → `com.ea.icm`
- `artifactId`: `domain.driven` → `intelligent-content-management`
- `name`: `domain.driver` *(fixes existing typo)* → `intelligent-content-management`
- `description`: already describes DMS correctly — no change

### Java sources (93 files — main + test)
- Directory: `src/main/java/com/ea/architecture/domain/driven/` → `src/main/java/com/ea/icm/`
- Directory: `src/test/java/com/ea/architecture/domain/driven/` → `src/test/java/com/ea/icm/`
- All `package com.ea.architecture.domain.driven` declarations → `package com.ea.icm`
- All `import com.ea.architecture.domain.driven` statements → `import com.ea.icm`
- Old empty ancestor directories (`com/ea/architecture/`) removed after move

### Documentation
- `CONTEXT.md`: update service name references from `architecture_ddd` to `intelligent-content-management`
- `README.md`: update `architecture_ddd` references

### Out of scope
- `spring.application.name` (already correct)
- Docker Compose infrastructure service names (Postgres, ES, pgadmin)
- No Dockerfile exists
- GitHub repository rename (done separately via GitHub settings)
- Filesystem folder rename (done after closing this session)

## Execution order

1. `git mv` source directories (main + test) to new package path
2. Bulk replace `com.ea.architecture.domain.driven` → `com.ea.icm` in all `.java` files
3. Remove leftover empty ancestor directories
4. Update `pom.xml`
5. Update `CONTEXT.md` and `README.md`
6. Build + test to verify
7. Commit

## Risk

Low. This is a pure rename with no logic changes. The only regression vector is a missed reference — mitigated by verifying the build compiles and tests pass after the change.
