<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Contributing to semver

## Bootstrap

    git clone <repo-url>
    cd semver
    npm run contribute

## Linting and testing

    pnpm lint
    ./mvnw -q verify

`pnpm check` runs the CI set; on TypeScript it skips reuse, which the
license CI job runs. `git push` runs `pnpm lint` first (pre-push hook),
reuse included; `git push --no-verify` bypasses it. The first commit's
pre-commit hook stamps headers and seeds `LICENSES`; reuse checks fail
before it runs.

## Updating from the template

    uv run --group dev copier update --trust

Conflicts appear as `.rej` files. Resolve them and commit.

## License headers

The template renders no SPDX headers. `reuse annotate`, run by the
pre-commit hook through lint-staged, stamps each file's license.
