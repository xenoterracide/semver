// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VersionStrategyFactory}.
 */
class VersionStrategyFactoryTest {

  @Test
  void exactTagHeadBranchReturnsOnExactTagHeadStrategy() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(0)
      .isOnTagExact(true)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(0)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(OnExactTagHeadStrategy.class);
  }

  @Test
  void exactTagTopicBranchReturnsOnExactTagTopicStrategy() {
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

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(OnExactTagTopicStrategy.class);
  }

  @Test
  void afterTagHeadBranchReturnsAfterTagHeadStrategy() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(5)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(AfterTagHeadStrategy.class);
  }

  @Test
  void afterTagTopicBranchReturnsAfterTagTopicStrategy() {
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

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(AfterTagTopicStrategy.class);
  }

  @Test
  void noTagHeadBranchReturnsNoTagHeadStrategy() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(5)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(NoTagHeadStrategy.class);
  }

  @Test
  void noTagTopicBranchReturnsNoTagTopicStrategy() {
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

    var strategy = VersionStrategyFactory.determineStrategy(ctx);

    assertThat(strategy).isInstanceOf(NoTagTopicStrategy.class);
  }
}
