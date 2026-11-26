<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# XTC-23 Maven publishing metadata design

## Intent

Bring `java-commons` and `semver` publishing metadata to the level the Gradle
convention plugins provided. Publishable artifacts need SCM data, inception
year, license data, signing, and a GitHub Packages target. Java code in both
repos moves to the PolyForm dual license. Generated Javadoc names the
documentation license.

## Goals

- Add SCM, inception year, and two SPDX license entries to each parent POM.
- Keep or add GitHub Packages `distributionManagement` in each parent POM.
- Keep or add a `sign` profile.
- Add a `staging` profile that writes to `target/repo`.
- Change Java source headers and each Java lint-staged license rule.
- Add a Javadoc footer for `CC-BY-NC-SA-4.0`.

## Non-goals

- No CI workflow changes.
- No changes to `base-template` in XTC-23. XTC-67 owns the shared lint-staged rule.
- No changes to the shared `github` workflows.
- No license change for Markdown, configuration, or scripts.
- No retrofit of other repositories.

## Constraints

- Maven POM licenses cannot hold an SPDX expression. Use one entry per license
  identifier. Put the OR expression in file headers and README.
- REUSE 6.3.0 cannot generate an OR expression from repeated `--license`
  flags. Pass the expression as one `--license` value.
- `base-template` owns `.lintstagedrc.cjs`. XTC-67 tracks its code license rule.
- The Maven workflow runs `verify` only. It does not deploy.
- `semver` already has `distributionManagement`, a `sign` profile, and GPG
  plugin management. Preserve those.

## Approaches

1. **Parent POMs and Java headers only.** Keep all changes inside the two
   repos. The staging profile deploys to `target/repo` when invoked. Chosen:
   smallest scope, no cross-repo workflow effect.
2. **CI publishing job.** Deploy and archive `target/repo` in CI. Rejected:
   touches shared workflow consumers and needs credentials.
3. **Template-first change.** Reproduce the metadata in `base-template` and
   regenerate consumers. Rejected for POM work: consumers own their POMs.
   XTC-67 owns the shared lint-staged rule.

## Decisions

- SCM uses GitHub HTTPS. `connection` and `developerConnection` use the
  `scm:git:` prefix. `url` points to the repository page.
- `inceptionYear` is `2015` for `java-commons`, from its README copyright
  range. It is `2024` for `semver`, from its Java copyright range.
- License identifiers are `PolyForm-Small-Business-1.0.0` and
  `PolyForm-Noncommercial-1.0.0`. The file expression uses `OR`.
- POM license URLs use `https://spdx.org/licenses/<identifier>.html`.
- POM license entries replace the current Apache-2.0 or GPL-3.0-or-later
  entry.
- GitHub Packages URLs use the existing `xenoterracide/<repo>` pattern.
- Signing uses `maven-gpg-plugin` 3.2.8. `semver` already pins it.
- Staging uses `altDeploymentRepository` in a `staging` profile.
- The Javadoc `bottom` text names `CC BY-NC-SA 4.0` and links its license.
- XTC-23 holds two Subtasks, one per repo. XTC-67 owns the base-template
  lint-staged rule.

## Remaining to builder

- Add both PolyForm license texts under each repo's `LICENSES/`.
- Update each README code license line. Leave documentation, configuration,
  and script license lines unchanged.
