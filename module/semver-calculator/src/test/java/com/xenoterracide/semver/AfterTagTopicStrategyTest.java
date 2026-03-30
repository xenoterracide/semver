// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AfterTagTopicStrategy}.
 */
class AfterTagTopicStrategyTest {

  @Test
  void stableTagReturnsVersionWithBranchMetadata() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    // Prerelease uses distance from tag (5), metadata uses distance from merge base (3)
    assertThat(version.toString()).isEqualTo("1.0.1-alpha.0.5+branch.feature-x.git.3.abc123");
  }

  @Test
  void prereleaseTagAppendsToPrerelease() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0-rc.1")
      .distanceFromTag(3)
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
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0-rc.1.3+branch.feature-x.git.2.abc123");
  }

  @Test
  void differentDistancesUsesCorrectValues() {
    // distanceFromTag = 10 (total commits from tag)
    // distanceFromMergeBase = 4 (commits on topic branch only)
    var ctx = GitContext.builder()
      .nearestTag("v2.0.0")
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
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    // Prerelease: alpha.0.10 (uses distanceFromTag)
    // Metadata: branch.feature-y.git.4.def456 (uses distanceFromMergeBase)
    assertThat(version.toString()).isEqualTo("2.0.1-alpha.0.10+branch.feature-y.git.4.def456");
  }

  @Test
  void dirtyWorkingTreeAddsDirtyMarker() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(true)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.1-alpha.0.5+branch.feature-x.git.3.abc123.dirty");
  }

  @Test
  void branchWithSpecialCharactersSanitizesBranchName() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature/x.y_z")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.1-alpha.0.5+branch.feature-x-y-z.git.3.abc123");
  }

  @Test
  void nullBranchUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch(null)
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.1-alpha.0.5+branch.unknown.git.3.abc123");
  }

  @Test
  void nullShortShaUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha(null)
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.1-alpha.0.5+branch.feature-x.git.3.unknown");
  }

  @Test
  void nullBaseVersionThrows() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagTopicStrategy(ctx);

    assertThatThrownBy(strategy::calculate)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("AfterTagTopicStrategy requires a tag");
  }
}
