// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NoTagHeadStrategy}.
 */
class NoTagHeadStrategyTest {

  @Test
  void newRepoWithCommitsReturnsZeroVersionWithPrerelease() {
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
    var strategy = new NoTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+git.5.abc123");
  }

  @Test
  void singleCommitReturnsCorrectVersion() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(1)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(1)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.1+git.1.abc123");
  }

  @Test
  void manyCommitsReturnsCorrectVersion() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(100)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(100)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.100+git.100.abc123");
  }

  @Test
  void dirtyWorkingTreeAddsDirtyMarker() {
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
      .isDirty(true)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+git.5.abc123.dirty");
  }

  @Test
  void nullShortShaUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(5)
      .shortSha(null)
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new NoTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("0.0.1-alpha.0.5+git.5.unknown");
  }
}
