// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xenoterracide.git.GitMetadata;
import com.xenoterracide.git.GitRemote;
import com.xenoterracide.git.GitStatus;
import java.util.List;
import org.immutables.datatype.Data;
import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VersionCalculator}.
 */
class VersionCalculatorTest {

  @Test
  void nullMetadataThrows() {
    assertThatThrownBy(() -> VersionCalculator.calculate(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("metadata must not be null");
  }

  @Test
  void onExactTagHeadBranchReturnsCleanVersion() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void onExactTagTopicBranchReturnsVersionWithBranchMetadata() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch("feature-x")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "0", "abc123");
  }

  @Test
  void afterTagHeadBranchReturnsVersionWithPrerelease() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(5)
      .mergeBaseDistance(5)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("git", "5", "abc123");
  }

  @Test
  void afterTagTopicBranchReturnsVersionWithBranchMetadata() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(5)
      .mergeBaseDistance(3)
      .branch("feature-x")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
  }

  @Test
  void noTagHeadBranchReturnsZeroVersionWithPrerelease() {
    var metadata = SimpleGitMetadata.builder()
      .distance(5)
      .mergeBaseDistance(5)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(0);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("git", "5", "abc123");
  }

  @Test
  void noTagTopicBranchReturnsZeroVersionWithBranchMetadata() {
    var metadata = SimpleGitMetadata.builder()
      .distance(5)
      .mergeBaseDistance(3)
      .branch("feature-x")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(0);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
  }

  @Test
  void prereleaseTagAfterTagHeadBranchAppendsDistance() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0-rc.1")
      .distance(3)
      .mergeBaseDistance(3)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).containsExactly("rc", "1", "3");
    assertThat(version.getBuild()).containsExactly("git", "3", "abc123");
  }

  @Test
  void dirtyWorkingTreeAfterTagAddsDirtyMarker() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(3)
      .mergeBaseDistance(3)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.DIRTY)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getBuild()).containsExactly("git", "3", "abc123", "dirty");
  }

  @Test
  void tagWithoutVPrefixParsesCorrectly() {
    var metadata = SimpleGitMetadata.builder()
      .tag("1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
  }

  @Test
  void noRemoteHeadTreatsCurrentBranchAsHead() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void detachedHeadOnTagReturnsCleanVersion() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch(null)
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "develop")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void detachedHeadAfterTagUsesHeadStyleMetadata() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(2)
      .mergeBaseDistance(2)
      .branch(null)
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "develop")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version).hasToString("1.0.1-alpha.0.2+git.2.abc123");
  }

  @Test
  void afterTagTopicBranchUsesMergeBaseDistance() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(5)
      .mergeBaseDistance(3)
      .branch("feature-x")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
  }

  @Test
  void mergeBaseDistanceLargerThanTagDistance() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(5)
      .mergeBaseDistance(11)
      .branch("feature-x")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(false)
      .build();

    var version = VersionCalculator.calculate(metadata);

    assertThat(version).hasToString("1.0.1-alpha.0.5+branch.feature-x.git.11.abc123");
  }

  @Test
  void shallowCloneThrows() {
    var metadata = SimpleGitMetadata.builder()
      .tag("v1.0.0")
      .distance(0)
      .mergeBaseDistance(0)
      .branch("main")
      .commit("fullsha123")
      .uniqueShort("abc123")
      .status(GitStatus.CLEAN)
      .remotes(List.of(new SimpleGitRemote("origin", "main")))
      .isShallowClone(true)
      .build();

    assertThatThrownBy(() -> VersionCalculator.calculate(metadata))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("shallow")
      .hasMessageContaining("git fetch --unshallow origin")
      .hasMessageContaining("fetch-depth: 0");
  }

  /**
   * Simple record implementation of GitMetadata for testing.
   */
  @Value.Builder
  @Data
  record SimpleGitMetadata(
    @Nullable String tag,
    long distance,
    long mergeBaseDistance,
    @Nullable String branch,
    @Nullable String commit,
    @Nullable String uniqueShort,
    GitStatus status,
    List<GitRemote> remotes,
    boolean isShallowClone
  ) implements GitMetadata {
    @Override
    public long distanceFromMergeBase() {
      return this.mergeBaseDistance;
    }

    static SimpleGitMetadataBuilder builder() {
      return new SimpleGitMetadataBuilder();
    }
  }

  /**
   * Simple record implementation of GitRemote for testing.
   */
  record SimpleGitRemote(String name, String headBranch) implements GitRemote {
    @Override
    public String headBranchRefName() {
      return headBranch != null ? "refs/remotes/" + name + "/" + headBranch : null;
    }
  }
}
