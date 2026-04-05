// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;

import com.xenoterracide.semver.Datatypes_GitContext.GitContext_;
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

    assertThat(ctx)
      .hasFieldOrPropertyWithValue(GitContext_.NEAREST_TAG_, "v1.0.0")
      .hasFieldOrPropertyWithValue(GitContext_.DISTANCE_FROM_TAG_, 5L)
      .hasFieldOrPropertyWithValue(GitContext_.IS_ON_TAG_EXACT_, false)
      .hasFieldOrPropertyWithValue(GitContext_.CURRENT_BRANCH_, "main")
      .hasFieldOrPropertyWithValue(GitContext_.HEAD_BRANCH_, "main")
      .hasFieldOrPropertyWithValue(GitContext_.IS_HEAD_BRANCH_, true)
      .hasFieldOrPropertyWithValue(GitContext_.DISTANCE_FROM_MERGE_BASE_, 5L)
      .hasFieldOrPropertyWithValue(GitContext_.SHORT_SHA_, "abc123")
      .hasFieldOrPropertyWithValue(GitContext_.FULL_SHA_, "fullsha123")
      .hasFieldOrPropertyWithValue(GitContext_.IS_DIRTY_, false)
      .hasFieldOrPropertyWithValue(GitContext_.IS_SHALLOW_CLONE_, false);
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

    assertThat(ctx)
      .hasFieldOrPropertyWithValue(GitContext_.NEAREST_TAG_, null)
      .hasFieldOrPropertyWithValue(GitContext_.CURRENT_BRANCH_, null)
      .hasFieldOrPropertyWithValue(GitContext_.HEAD_BRANCH_, null)
      .hasFieldOrPropertyWithValue(GitContext_.SHORT_SHA_, null)
      .hasFieldOrPropertyWithValue(GitContext_.FULL_SHA_, null);
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
