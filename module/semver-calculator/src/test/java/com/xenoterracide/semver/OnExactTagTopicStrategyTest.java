// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OnExactTagTopicStrategy}.
 */
class OnExactTagTopicStrategyTest {

  @Test
  void onFeatureBranchReturnsVersionWithBranchMetadata() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0+branch.feature-x.git.0.abc123");
  }

  @Test
  void dirtyWorkingTreeAddsDirtyMarker() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(true)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0+branch.feature-x.git.0.abc123.dirty");
  }

  @Test
  void branchWithSpecialCharactersSanitizesBranchName() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("feature/x.y_z")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0+branch.feature-x-y-z.git.0.abc123");
  }

  @Test
  void nullBranchUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch(null)
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0+branch.unknown.git.0.abc123");
  }

  @Test
  void nullShortShaUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha(null)
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version.toString()).isEqualTo("1.0.0+branch.feature-x.git.0.unknown");
  }

  @Test
  void nullBaseVersionThrows() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("feature-x")
      .headBranch("main")
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new OnExactTagTopicStrategy(ctx);

    assertThatThrownBy(strategy::calculate)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("OnExactTagTopicStrategy requires a tag");
  }
}
