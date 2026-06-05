# Contributing to Lumen

## Branch strategy

| Branch | Purpose |
|---|---|
| `master` | Always stable. Tagged releases only. No direct commits. |
| `dev` | Integration branch. All features merge here before `master`. |
| `feature/xxx` | New features. Branch off `dev`, merge back to `dev` via PR. |
| `hotfix/xxx` | Urgent production fixes. Branch off `master`, merge into both `master` and `dev`. |

```
master ──────────────────────────────── (tagged releases)
           ↑                   ↑
         merge               merge
           │                   │
dev ───┼───────────────────┼────────
           │         ↑         │
         branch    merge     branch
           │         │
feature/x ─┴─────────┘
```

## Versioning — Semantic Versioning (semver)

Lumen follows [Semantic Versioning](https://semver.org): `MAJOR.MINOR.PATCH`

### Patch release (e.g. 1.0.0 → 1.0.1)

A patch release fixes bugs without changing any public API.

**Triggers a patch:**
- Bug fix in existing behaviour
- Performance improvement with no API change
- Documentation correction
- Dependency version bump (patch/minor third-party updates)

**Does NOT trigger a patch:**
- Any new public class, annotation, or interface
- Any change to method signatures
- Any change to configuration properties

**Process:**
1. Branch `hotfix/xxx` off `master`
2. Fix the bug, add a regression test
3. PR into `master`, squash merge
4. PR the same fix into `dev`
5. `mvn versions:set -DnewVersion=1.0.1`
6. Commit `fix: <description>`, tag `v1.0.1`, push tag

---

### Minor release (e.g. 1.0.0 → 1.1.0)

A minor release adds new functionality in a backwards-compatible way. Existing applications must continue to work without any changes.

**Triggers a minor:**
- New module or starter (e.g. `lumen-boot-starter-ratelimit`)
- New annotation (`@RateLimit`, `@ConditionalOnBean`, etc.)
- New SPI interface
- New configuration property
- New endpoint or method added to an existing public API
- New integration (e.g. Bucket4j, STOMP)

**Must NOT:**
- Remove or rename any existing public class, method, annotation, or property
- Change the behaviour of any existing annotation or SPI in a breaking way
- Require existing application code to change

**Process:**
1. All work happens on `feature/xxx` branches off `dev`
2. Features merge into `dev` via PR (at least 1 review)
3. When the milestone is complete, PR `dev` → `master`
4. `mvn versions:set -DnewVersion=1.1.0`
5. Commit `release: Lumen Framework 1.1.0`, tag `v1.1.0`, push tag

---

### Major release (e.g. 1.0.0 → 2.0.0)

A major release may contain breaking changes. Users may need to update their application code.

**Triggers a major:**
- Removing or renaming a public class, annotation, interface, or method
- Changing the contract of an existing SPI (e.g. adding a required method to `LumenModule`)
- Changing a configuration property name or default value in a breaking way
- Dropping support for a Java version
- Architectural change that makes existing module combinations incompatible

**Process:**
1. Major development happens on `dev` (or a dedicated `v2` branch for large efforts)
2. Maintain a `MIGRATION.md` documenting every breaking change and how to update
3. PR `dev` → `master` only when the release is feature-complete and all tests pass
4. `mvn versions:set -DnewVersion=2.0.0`
5. Commit `release: Lumen Framework 2.0.0`, tag `v2.0.0`, push tag

---

## Commit message format

Use [Conventional Commits](https://www.conventionalcommits.org):

```
<type>: <short description>

<optional body>
```

| Type | When to use |
|---|---|
| `feat` | New feature or module |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change with no behaviour change |
| `test` | Adding or fixing tests |
| `chore` | Build, CI, dependency updates |
| `release` | Version bump and release commit |

---

## Pull request rules

- Every PR targets `dev` (or `master` for hotfixes)
- At least 1 reviewer approval required
- All tests must pass (`mvn clean install`)
- PR description must state: what changed, why, and any migration notes if breaking