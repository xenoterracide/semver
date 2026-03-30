// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AfterTagHeadStrategy}.
 */
class AfterTagHeadStrategyTest {

  @Test
  void stableTagWithDistanceReturnsVersionWithAlphaPrerelease() {
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
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    // Stable tag: increment patch, add alpha prerelease
    assertThat(version).hasToString("1.0.1-alpha.0.5+git.5.abc123");
  }

  @Test
  void distanceOneReturnsCorrectVersion() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
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
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.0.1-alpha.0.1+git.1.abc123");
  }

  @Test
  void prereleaseTagWithDistanceAppendsToPrerelease() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0-rc.1")
      .distanceFromTag(3)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(3)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    // Prerelease tag: keep base, append distance to prerelease
    assertThat(version).hasToString("1.0.0-rc.1.3+git.3.abc123");
  }

  @Test
  void complexPrereleaseAppendsDistance() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0-alpha.1.beta.2")
      .distanceFromTag(4)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(4)
      .shortSha("abc123")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.0.0-alpha.1.beta.2.4+git.4.abc123");
  }

  @Test
  void dirtyWorkingTreeAddsDirtyMarker() {
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
      .isDirty(true)
      .isShallowClone(false)
      .build();
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.0.1-alpha.0.5+git.5.abc123.dirty");
  }

  @Test
  void nullShortShaUsesUnknown() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
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
    var strategy = new AfterTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.0.1-alpha.0.5+git.5.unknown");
  }

  @Test
  void nullBaseVersionThrows() {
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
    var strategy = new AfterTagHeadStrategy(ctx);

    assertThatThrownBy(strategy::calculate)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("AfterTagHeadStrategy requires a tag");
  }
}
