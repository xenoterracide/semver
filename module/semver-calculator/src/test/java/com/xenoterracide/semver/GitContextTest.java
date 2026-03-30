// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link GitContext}.
 */
class GitContextTest {

  @Test
  void createsContextWithAllValues() {
    var ctx = GitContext.builder()
      .nearestTag("v1.0.0")
      .distanceFromTag(5)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(5)
      .shortSha("abc123")
      .fullSha("fullsha123")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.nearestTag()).isEqualTo("v1.0.0");
    assertThat(ctx.distanceFromTag()).isEqualTo(5);
    assertThat(ctx.isOnTagExact()).isFalse();
    assertThat(ctx.currentBranch()).isEqualTo("main");
    assertThat(ctx.headBranch()).isEqualTo("main");
    assertThat(ctx.isHeadBranch()).isTrue();
    assertThat(ctx.distanceFromMergeBase()).isEqualTo(5);
    assertThat(ctx.shortSha()).isEqualTo("abc123");
    assertThat(ctx.fullSha()).isEqualTo("fullsha123");
    assertThat(ctx.isDirty()).isFalse();
    assertThat(ctx.isShallowClone()).isFalse();
  }

  @Test
  void createsContextWithNullValues() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(0)
      .isOnTagExact(false)
      .currentBranch(null)
      .headBranch(null)
      .isHeadBranch(false)
      .distanceFromMergeBase(0)
      .shortSha(null)
      .fullSha(null)
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.nearestTag()).isNull();
    assertThat(ctx.currentBranch()).isNull();
    assertThat(ctx.headBranch()).isNull();
    assertThat(ctx.shortSha()).isNull();
    assertThat(ctx.fullSha()).isNull();
  }

  @ParameterizedTest
  @CsvSource({ "v1.0.0, true", "v2.1.3-alpha.1, true", "null, false" })
  void variousTagsReturnsExpected(String tag, boolean expected) {
    var ctx = GitContext.builder()
      .nearestTag("null".equals(tag) ? null : tag)
      .distanceFromTag(0)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(0)
      .shortSha("abc")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.hasTagInHistory()).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = { "v1.0.0", "v2.1.3-alpha.1", "v0.0.1" })
  void withVPrefixStripsV(String tag) {
    var ctx = GitContext.builder()
      .nearestTag(tag)
      .distanceFromTag(0)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(0)
      .shortSha("abc")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.baseVersion()).isEqualTo(tag.substring(1));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1.0.0", "2.1.3", "0.0.1-rc.1" })
  void withoutVPrefixReturnsAsIs(String tag) {
    var ctx = GitContext.builder()
      .nearestTag(tag)
      .distanceFromTag(0)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(0)
      .shortSha("abc")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.baseVersion()).isEqualTo(tag);
  }

  @Test
  void nullTagReturnsNull() {
    var ctx = GitContext.builder()
      .nearestTag(null)
      .distanceFromTag(0)
      .isOnTagExact(false)
      .currentBranch("main")
      .headBranch("main")
      .isHeadBranch(true)
      .distanceFromMergeBase(0)
      .shortSha("abc")
      .fullSha("full")
      .isDirty(false)
      .isShallowClone(false)
      .build();

    assertThat(ctx.baseVersion()).isNull();
  }
}
