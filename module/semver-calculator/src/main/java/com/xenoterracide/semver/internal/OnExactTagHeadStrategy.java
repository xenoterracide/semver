// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver.internal;

import com.xenoterracide.semver.GitContext;
import com.xenoterracide.semver.VersionStrategy;
import org.semver4j.Semver;

/**
 * Strategy: HEAD is exactly on a tag, and we're on the
 * <a href="https://git-scm.com/docs/git-remote#Documentation/git-remote.txt-emset-headem">HEAD branch</a>
 * (e.g., main, develop).
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>v1.0.0 on main → {@code 1.0.0}</li>
 * </ul>
 *
 * <p>This is the cleanest strategy - no prerelease or build metadata needed.</p>
 */
public final class OnExactTagHeadStrategy implements VersionStrategy {

  private final GitContext ctx;

  public OnExactTagHeadStrategy(GitContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public Semver calculate() {
    var baseVersion = Objects.requireNonNullElseThrow(
      this.ctx.baseVersion(),
      "OnExactTagHeadStrategy requires a tag but baseVersion is null"
    );

    var semver = Objects.requireNonNullElseThrow(
      Semver.parse(baseVersion),
      "Invalid tag format: " + this.ctx.nearestTag()
    );

    // On exact tag on HEAD branch: pure version without any suffixes
    return semver;
  }
}
