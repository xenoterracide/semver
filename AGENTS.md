<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Project Overview

This is **semver**, a Java library for semantic versioning calculation based on Git metadata. It provides tools to extract Git repository information and calculate semantic versions according to the SemVer specification.

The project consists of two main modules:

1. **git-metadata**: Extracts Git repository metadata (tags, commits, branches, remotes, status)
2. **semver-calculator**: Calculates semantic versions from Git metadata using various strategies

## Technology Stack

- **Java**: 21+ (Temurin distribution)
- **Build Tool**: Maven 3.x (with wrapper `./mvnw`)
- **Module System**: JPMS (Java Platform Module System)
- **Git Library**: Eclipse JGit 7.6.0
- **Version Parsing**: Semver4j 6.0.0
- **Functional Programming**: Vavr 1.0.1
- **Null Safety**: JSpecify 1.0.0 annotations
- **Immutables**: Immutables 2.12.1 for value objects

### Tooling Stack

- **Python**: 3.12+ (managed via `uv`)
- **Node.js**: 24.14.1 (managed via `yarn` 4.13.0 with Plug'n'Play)
- **Formatting**: Prettier 3.x with plugins for Java, XML, TOML, Properties
- **License Compliance**: REUSE specification via `reuse` tool
- **Git Hooks**: Custom hooks for commit validation and dependency syncing

## Project Structure

```
.
├── module/                          # Maven multi-modules
│   ├── git-metadata/               # Git metadata extraction library
│   │   ├── src/
│   │   │   ├── main/java/          # Production code
│   │   │   │   └── com/xenoterracide/git/
│   │   │   ├── test/java/          # Unit tests
│   │   │   └── testFixtures/java/  # Test fixtures (CommitTools)
│   │   └── pom.xml
│   └── semver-calculator/          # Semantic version calculation
│       ├── src/
│       │   ├── main/java/          # Production code
│       │   │   ├── com/xenoterracide/semver/
│       │   │   └── com/xenoterracide/semver/internal/  # Strategy implementations
│       │   └── test/java/          # Unit tests
│       └── pom.xml
├── pom.xml                          # Parent POM
├── package.json                     # Node.js tooling configuration
├── pyproject.toml                   # Python tooling configuration
├── .share/                          # Shared git hooks and tooling (git submodule)
├── .github/workflows/               # GitHub Actions
│   ├── build.yml                   # Build and test workflow
│   └── pre-commit.yml              # License and formatting checks
└── config/                         # Checkstyle configuration (created on build)
```

## Module Architecture

### git-metadata Module

Exports `com.xenoterracide.git` package:

- `GitMetadata` - Interface for Git metadata access
- `GitMetadataImpl` - Implementation using JGit
- `GitStatus` - Enum for repository status (CLEAN, DIRTY, NO_REPO)
- `GitRemote` - Interface for remote repository info
- `DistanceCalculator` - Calculates commit distance from tags
- `MergeBaseFinder` - Finds merge base between branches
- `TryGit` - Functional wrapper for JGit operations

**Test Fixtures** (`src/testFixtures/java`):

- `CommitTools` - Helper for creating test commits with auto-incrementing messages

### semver-calculator Module

Exports `com.xenoterracide.semver` package:

- `VersionCalculator` - Main entry point for version calculation
- `GitContext` - Immutable context object built via Immutables
- `VersionStrategy` - Interface for version calculation strategies

**Internal Strategies** (`internal` package):

Strategies are selected based on two dimensions:

1. Tag relationship: `ON_EXACT_TAG`, `AFTER_TAG`, or `NO_TAG`
2. Branch type: `HEAD_BRANCH` or `TOPIC_BRANCH`

- `OnExactTagHeadStrategy` - On a tag on the main branch
- `OnExactTagTopicStrategy` - On a tag on a topic branch
- `AfterTagHeadStrategy` - After a tag on the main branch
- `AfterTagTopicStrategy` - After a tag on a topic branch
- `NoTagHeadStrategy` - No tags exist, on main branch
- `NoTagTopicStrategy` - No tags exist, on topic branch
- `VersionStrategyFactory` - Factory to determine appropriate strategy

## Build and Test Commands

### Maven Commands

```bash
# Compile all modules
./mvnw compile

# Run unit tests
./mvnw test

# Run integration tests (in src/testIntegration/java)
./mvnw verify

# Run full build with all checks
./mvnw clean verify --batch-mode --fail-at-end

# Generate code coverage report (JaCoCo)
./mvnw jacoco:report

# Run code quality checks (Checkstyle, SpotBugs)
./mvnw checkstyle:check spotbugs:check

# Install to local repository
./mvnw install
```

### Yarn/Node Commands

```bash
# Setup development environment (run after clone)
yarn contribute

# Run all linting checks
yarn lint

# Check formatting with Prettier
yarn lint:prettier

# Check license compliance
yarn lint:reuse

# Run all workspace tests
yarn test
```

### Python/uv Commands

```bash
# Sync Python dependencies
uv sync --frozen

# Run REUSE license tool
uv run --frozen --group dev reuse lint
```

## Code Style Guidelines

### EditorConfig (`.editorconfig`)

- **Charset**: UTF-8
- **Line endings**: LF
- **Indent**: 2 spaces
- **Final newline**: Required

### Java Code Style

- **Source/Target**: Java 21
- **Print width**: 120 characters (Prettier)
- **Null annotations**: Use JSpecify `@Nullable` and `@NonNull`
- **Immutables**: Use `@Value.Immutable` for value objects with Jakarta annotations
- **Package-private**: Prefer package-private visibility for internal classes

### File Type Conventions

| File Type                              | License          | Formatter            |
| -------------------------------------- | ---------------- | -------------------- |
| `*.java`                               | GPL-3.0-or-later | Prettier             |
| `*.js`, `*.cjs`, `*.yml`               | MIT              | Prettier             |
| `package.json`                         | MIT              | Prettier             |
| `*.json` (non-package)                 | CC0-1.0          | Prettier             |
| `*.md`                                 | CC-BY-NC-SA-4.0  | Prettier             |
| `*.xml`, `*.yaml`, `*.toml`, `*.json5` | CC0-1.0          | Prettier             |
| Shell scripts                          | MIT              | shfmt (python style) |

### Licensing

All files MUST have SPDX license headers. The project uses:

- **REUSE specification** for license compliance
- **lint-staged** automatically adds headers via `reuse annotate`

Licenses used:

- **GPL-3.0-or-later**: Source code (Java, TypeScript)
- **MIT**: Scripts, configuration, GitHub workflows
- **CC0-1.0**: Configuration files, generated files
- **CC-BY-NC-SA-4.0**: Documentation (README, AGENTS.md)

## Testing Instructions

### Unit Tests

```bash
# Run all unit tests
./mvnw test
```

Uses:

- **JUnit 5** (Jupiter) for test framework
- **AssertJ** for assertions
- **JGit** in-memory repositories for Git testing
- `@TempDir` with `CleanupMode.ON_SUCCESS` for temporary directories

### Test Fixtures

The `git-metadata` module provides test fixtures in `src/testFixtures/java`:

- `CommitTools.commit(Git)` - Creates a commit with auto-incrementing message
- `CommitTools.supplies(ObjectId, Supplier<T>)` - Helper for chaining in tests

### Integration Tests

Integration tests are in `src/testIntegration/java` and run via Maven Failsafe plugin:

```bash
./mvnw verify  # Runs both unit and integration tests
```

### Code Coverage

JaCoCo is configured with a minimum 30% line coverage threshold:

```bash
./mvnw jacoco:report  # Generate HTML report in target/site/jacoco/
```

## Git Workflow

### Conventional Commits

Allowed types (from `git-conventional-commits.yaml`):

- `ci`, `feat`, `fix`, `perf`, `refactor`, `style`, `test`
- `build`, `ops`, `docs`, `chore`, `merge`, `revert`

Release tags follow pattern: `v[0-9]*.[0-9]*.[0-9]*`

### Git Hooks

Git hooks are configured via `git config core.hooksPath .share/git/hooks`:

1. **pre-commit**: Runs `lint-staged` to format and add license headers
2. **commit-msg**: Validates conventional commit format
3. **post-checkout/post-merge**: Auto-runs dependency sync if lockfiles changed

### AI-Assisted Merge Workflow

The `.share/` directory (git submodule) provides AI-assisted PR workflows:

```bash
# Generate PR message and merge using different AI engines
yarn merge:kimi      # Uses Kimi CLI
yarn merge:junie     # Uses Junie CLI
yarn merge:copilot   # Uses GitHub Copilot CLI
```

## Security Considerations

1. **CI Detection**: Git hooks check `[ -n "$CI" ]` and exit early in CI environments
2. **Shallow Clone Detection**: Code warns when repository has fewer than 4 commits
3. **Error Handling**: Uses Vavr's `Try` for functional error handling
4. **Null Safety**: JSpecify annotations for compile-time null checking

## Development Setup

1. Install prerequisites:

   ```bash
   asdf install  # Reads .tool-versions (Java 25, Node.js 24)
   ```

2. Setup environment:

   ```bash
   yarn contribute
   # This will:
   # - Initialize git submodules
   # - Sync Python virtual environment
   # - Configure git hooks path
   ```

3. Verify build:
   ```bash
   ./mvnw clean verify
   ```

## Dependency Management

### Key Dependencies

| Dependency          | Version | Purpose                         |
| ------------------- | ------- | ------------------------------- |
| Eclipse JGit        | 7.6.0   | Git operations                  |
| Semver4j            | 6.0.0   | Semantic version parsing        |
| Vavr                | 1.0.1   | Functional programming          |
| Guava               | 33.5.0  | Utility libraries               |
| Apache Commons Lang | 3.20.0  | String utilities                |
| Immutables          | 2.12.1  | Code generation for value types |
| JSpecify            | 1.0.0   | Nullability annotations         |

### Quality Plugins

| Plugin     | Version | Purpose               |
| ---------- | ------- | --------------------- |
| Checkstyle | 13.3.0  | Code style checking   |
| SpotBugs   | 4.9.8   | Bug pattern detection |
| JaCoCo     | 0.8.14  | Code coverage         |

### Lock Files

- `uv.lock` - Python dependencies
- `yarn.lock` - Node.js dependencies
- Changes trigger automatic sync via git hooks

## CI/CD

### GitHub Actions Workflows

**build.yml**:

- Runs on every push to any branch and version tags
- Uses Java 21 (Temurin)
- Executes `./mvnw verify --batch-mode --fail-at-end`

**pre-commit.yml**:

- Runs license and prettier checks
- Uses reusable workflows from `xenoterracide/github`
