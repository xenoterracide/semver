// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OnExactTagHeadStrategy}.
 */
class OnExactTagHeadStrategyTest {

  @Test
  void stableVersionReturnsCleanVersion() {
    var ctx = GitContext.builder()
      .nearestTag("v1.2.3")
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
    var strategy = new OnExactTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.2.3");
  }

  @Test
  void prereleaseVersionReturnsCleanVersion() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0-rc.1")
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
    var strategy = new OnExactTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("1.0.0-rc.1");
  }

  @Test
  void tagWithoutVPrefixWorks() {
    var ctx = GitContext.builder()
      .nearestTag("2.0.0")
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
    var strategy = new OnExactTagHeadStrategy(ctx);

    var version = strategy.calculate();

    assertThat(version).hasToString("2.0.0");
  }

  @Test
  void nullBaseVersionThrows() {
    var ctx = GitContext.builder()
      .nearestTag(null)
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
    var strategy = new OnExactTagHeadStrategy(ctx);

    assertThatThrownBy(strategy::calculate)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("OnExactTagHeadStrategy requires a tag");
  }
}
