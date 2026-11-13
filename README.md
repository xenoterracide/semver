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

    asdf install
    npm run contribute
    ./mvnw -q verify

`npm run contribute` installs tools and dependencies, and heals the
toolchain through `.config/git/hooks/heal.sh`. Node 25+ drops corepack, so
the bootstrap starts from npm. `direnv allow` loads the environment if you
use direnv.

## Build and test

- `pnpm lint` — prettier and REUSE checks
- `./mvnw -q verify` — compile, test, checkstyle, spotbugs, and jacoco
- `./mvnw -q test` — unit tests only

## Consuming

Artifacts are published to
[GitHub Packages](https://maven.pkg.github.com/xenoterracide/semver).

`settings.xml` server (use your GitHub username and a PAT with `read:packages`):

```xml
<servers>
  <server>
    <id>gh</id>
    <username>${env.GITHUB_USER}</username>
    <password>${env.GITHUB_TOKEN}</password>
  </server>
</servers>
```

Run the CLI with JBang (the `gh` repo matches the server id above):

```bash
jbang --repos=gh=https://maven.pkg.github.com/xenoterracide/semver com.xenoterracide:semver-cli:<version>
```

Or add the library as a Maven dependency:

```xml
<dependency>
  <groupId>com.xenoterracide</groupId>
  <artifactId>semver-calculator</artifactId>
  <version>VERSION</version>
</dependency>
```

## FAQ

### Shallow clones

Version distance is wrong from a shallow clone, so the CLI fails fast with an
error telling you to fetch the full history. Locally, run:

```bash
git fetch --all --filter blob:none
```

In GitHub Actions, use `actions/checkout` with `filter: "blob:none"` and
`fetch-depth: 0`.

### Annotated tags on tag pushes

GitHub Actions tag-push checkouts can miss the tag entirely. See
[actions/checkout#882](https://github.com/actions/checkout/issues/882). Use an
explicit `ref: ${{ github.ref_name }}` and `fetch-tags: true`, or the tag may not
resolve.

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
