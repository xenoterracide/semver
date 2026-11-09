<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# semver

Semantic version calculation from Git metadata.

## Modules

- `git-metadata` — reads tags, commits, branches, remotes, and status.
- `semver-calculator` — turns that metadata into a version.
- `semver-cli` — command-line entry point.

## Getting started

<<<<<<< before updating
    asdf install
    pnpm contribute
    ./mvnw -q verify
=======
- `npm run contribute` installs tools and dependencies, and heals the
  toolchain through `.config/git/hooks/heal.sh`. Node 25+ drops corepack,
  so the bootstrap starts from npm.
- `direnv allow` (if you use direnv) loads the environment.
>>>>>>> after updating

`pnpm contribute` installs tools and dependencies, and heals the toolchain
through `.config/git/hooks/heal.sh`. `direnv allow` loads the environment
if you use direnv.

## Build and test

- `pnpm lint` — prettier and REUSE checks
- `./mvnw -q verify` — compile, test, checkstyle, spotbugs, and jacoco
- `./mvnw -q test` — unit tests only

## Source of truth

This project is managed with [copier](https://copier.readthedocs.io). The
shared tooling lives in
[xenoterracide/base-template](https://github.com/xenoterracide/base-template).
Refresh it with:

    uv run --group dev copier update --trust

Renovate opens an update PR whenever the template tags a release. Merge it
like any other dependency update.

## AI Agent Skills

This repository uses skills from
[xenoterracide/agent-skills](https://github.com/xenoterracide/agent-skills),
an agentic skills plugin providing AI coding agent capabilities for Java,
Gradle, GitHub, and development workflows.

## License

[GPL-3.0-or-later](LICENSES/GPL-3.0-or-later.txt).
