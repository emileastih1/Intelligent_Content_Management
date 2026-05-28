# Project Rename: architecture_ddd → intelligent-content-management

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename all project identifiers from `architecture_ddd` / `com.ea.architecture.domain.driven` to `intelligent-content-management` / `com.ea.icm`, unblocking the Keycloak security PR (#41).

**Architecture:** Pure rename — no logic changes. Java sources are moved via `git mv` (preserves per-line blame history), then package/import declarations are replaced in-place with a bulk text substitution. Maven coordinates and documentation references are updated to match.

**Tech Stack:** Java 21, Maven (mvnw.cmd), Spring Boot 4.x, PowerShell on Windows

---

## File Map

| File / Path | Change |
|---|---|
| `src/main/java/com/ea/architecture/domain/driven/**` | Moved → `src/main/java/com/ea/icm/**` |
| `src/test/java/com/ea/architecture/domain/driven/**` | Moved → `src/test/java/com/ea/icm/**` |
| All `.java` files (93 total) | `com.ea.architecture.domain.driven` → `com.ea.icm` in package/import |
| `pom.xml` | groupId, artifactId, name |
| `CONTEXT.md` | 3 occurrences of `architecture_ddd` |

---

### Task 1: Move main Java sources with git mv

**Files:**
- Move: `src/main/java/com/ea/architecture/domain/driven/` → `src/main/java/com/ea/icm/`

- [ ] **Step 1: Run git mv for main sources**

```powershell
git mv src/main/java/com/ea/architecture/domain/driven src/main/java/com/ea/icm
```

- [ ] **Step 2: Verify git staged the renames**

```powershell
git status --short | Select-String "^R"
```

Expected: ~85 lines beginning with `R` (renamed), e.g.:
```
R  src/main/java/com/ea/architecture/domain/driven/DmsApplication.java -> src/main/java/com/ea/icm/DmsApplication.java
```

- [ ] **Step 3: Remove leftover empty ancestor directories**

After the mv, the directories `com/ea/architecture/domain/` and `com/ea/architecture/` are empty. Git leaves them on disk.

```powershell
Remove-Item -Recurse -Force src/main/java/com/ea/architecture
```

---

### Task 2: Move test Java sources with git mv

**Files:**
- Move: `src/test/java/com/ea/architecture/domain/driven/` → `src/test/java/com/ea/icm/`

- [ ] **Step 1: Run git mv for test sources**

```powershell
git mv src/test/java/com/ea/architecture/domain/driven src/test/java/com/ea/icm
```

- [ ] **Step 2: Verify git staged the test renames**

```powershell
git status --short | Select-String "^R" | Measure-Object | Select-Object -ExpandProperty Count
```

Expected: ~93 total renamed lines (85 main + 8 test).

- [ ] **Step 3: Remove leftover empty test ancestor directories**

```powershell
Remove-Item -Recurse -Force src/test/java/com/ea/architecture
```

---

### Task 3: Replace package and import declarations in all Java files

**Files:**
- Modify: all `*.java` under `src/` (93 files)

- [ ] **Step 1: Bulk replace `com.ea.architecture.domain.driven` → `com.ea.icm`**

```powershell
Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    $newContent = $content.Replace('com.ea.architecture.domain.driven', 'com.ea.icm')
    if ($content -ne $newContent) {
        [System.IO.File]::WriteAllText($_.FullName, $newContent, [System.Text.Encoding]::UTF8)
    }
}
```

- [ ] **Step 2: Verify no old package references remain**

```powershell
Select-String -Path "src/**/*.java" -Pattern "com\.ea\.architecture\.domain\.driven" -Recurse
```

Expected: no output (zero matches).

- [ ] **Step 3: Spot-check a file to confirm new package is correct**

```powershell
Get-Content src/main/java/com/ea/icm/DmsApplication.java | Select-Object -First 5
```

Expected first line: `package com.ea.icm;`

---

### Task 4: Update pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Update groupId, artifactId, and name**

In `pom.xml`, replace lines 11–14:

```xml
    <groupId>com.ea.icm</groupId>
    <artifactId>intelligent-content-management</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>intelligent-content-management</name>
```

(The `description` line does not need changing — it already describes the DMS correctly.)

- [ ] **Step 2: Verify no old pom identifiers remain**

```powershell
Select-String -Path "pom.xml" -Pattern "com\.ea\.architecture|domain\.driven|domain\.driver"
```

Expected: no output.

---

### Task 5: Update CONTEXT.md

**Files:**
- Modify: `CONTEXT.md` (lines 1, 9, 12)

- [ ] **Step 1: Replace the three `architecture_ddd` occurrences**

Line 1 — heading:
```markdown
# Context: Intelligent Document Management — intelligent-content-management
```

Line 9 — service table row (first cell):
```markdown
| `intelligent-content-management` (this service) | Document CRUD, metadata persistence (PostgreSQL), full-text indexing (Elasticsearch), document content extraction (Tika) |
```

Line 12 — inter-service description:
```markdown
The two services communicate over HTTP. `intelligent-content-management` delegates AI operations to `dms` via the `AiServiceClient` REST port.
```

- [ ] **Step 2: Verify no old name remains**

```powershell
Select-String -Path "CONTEXT.md" -Pattern "architecture_ddd"
```

Expected: no output.

---

### Task 6: Verify the build compiles

- [ ] **Step 1: Run Maven compile**

```powershell
.\mvnw.cmd compile -q
```

Expected: BUILD SUCCESS with no errors. If there are compilation errors, they indicate a missed reference — grep for the error's class/package and fix.

---

### Task 7: Verify tests pass

- [ ] **Step 1: Run the test suite**

```powershell
.\mvnw.cmd test -Pwindows-docker-desktop
```

Expected: BUILD SUCCESS. All tests green. (The `windows-docker-desktop` profile sets `api.version=1.44` for Testcontainers Docker Desktop compatibility.)

---

### Task 8: Commit everything

- [ ] **Step 1: Stage all changes**

```powershell
git add -A
```

- [ ] **Step 2: Verify staging looks right — should be renames + modifications, nothing unexpected**

```powershell
git status --short
```

Expected: ~93 `R` (renamed) lines for Java files, a handful of `M` (modified) for `pom.xml` and `CONTEXT.md`. No untracked files or deletions beyond the empty dirs already removed.

- [ ] **Step 3: Commit**

```powershell
git commit -m "refactor: rename project identifiers to intelligent-content-management (closes #42)

- Rename Java root package: com.ea.architecture.domain.driven -> com.ea.icm
- Update pom.xml: groupId, artifactId, name
- Update CONTEXT.md service name references

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```
