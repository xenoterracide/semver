// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NoTagTopicStrategy}.
 */
class NoTagTopicStrategyTest {

  @Test
  void newRepoTopicBranchReturnsZeroVersionWithBranchMetadata() {
    // 5 total commits, 2 commits on topic branch
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(2)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    // Prerelease uses distanceFromTag (total commits = 5)
    // Metadata uses distanceFromMergeBase (commits on topic branch = 2)
    assertThat(version).hasToString("0.0.1-alpha.0.5+branch.feature-x.git.2.abc123");
  }

  @Test
  void differentDistancesUsesCorrectValues() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(10)
      .isOnTagExact(false)
      .currentBranch("feature-y")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(4)
      .shortSha("def456")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.10+branch.feature-y.git.4.def456");
  }

  @Test
  void dirtyWorkingTreeAddsDirtyMarker() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(2)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(true)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+branch.feature-x.git.2.abc123.dirty");
  }

  @Test
  void branchWithSpecialCharactersSanitizesBranchName() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature/x.y_z")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(2)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+branch.feature-x-y-z.git.2.abc123");
  }

  @Test
  void nullBranchUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch(null)
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(2)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+branch.unknown.git.2.abc123");
  }

  @Test
  void nullShortShaUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(2)
      .shortSha(null)
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+branch.feature-x.git.2.unknown");
  }
}
