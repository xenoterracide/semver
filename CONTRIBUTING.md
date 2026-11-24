<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Contributing to semver

## Bootstrap

    git clone <repo-url>
    cd semver
    npm run contribute

`npm run contribute` installs tools and dependencies. It heals the toolchain
through `.config/git/hooks/heal.sh`. Node 25+ drops corepack, so bootstrap
starts from npm. With direnv, run `direnv allow` to load the environment.

    pnpm lint
    ./mvnw -q verify

`pnpm check` runs `pnpm lint`. `git push` runs `pnpm lint` first through
the pre-push hook; `git push --no-verify` bypasses it. The first commit's
pre-commit hook stamps headers and seeds `LICENSES`; reuse checks fail
before it runs.

## Updating from the template

    uvx copier update --trust

Conflicts appear as `.rej` files. Resolve them and commit.

## License headers

The template renders no SPDX headers. `reuse annotate`, run by the
pre-commit hook through lint-staged, stamps each file's license.
