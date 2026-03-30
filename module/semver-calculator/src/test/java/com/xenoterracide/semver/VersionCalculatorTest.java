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
    var metadata = createMetadata("v1.0.0", 0, "main", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void onExactTagTopicBranchReturnsVersionWithBranchMetadata() {
    var metadata = createMetadata("v1.0.0", 0, "feature-x", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "0", "abc123");
  }

  @Test
  void afterTagHeadBranchReturnsVersionWithPrerelease() {
    var metadata = createMetadata("v1.0.0", 5, "main", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("git", "5", "abc123");
  }

  @Test
  void afterTagTopicBranchReturnsVersionWithBranchMetadata() {
    var metadata = createMetadata("v1.0.0", 5, "feature-x", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "5", "abc123");
  }

  @Test
  void noTagHeadBranchReturnsZeroVersionWithPrerelease() {
    var metadata = createMetadata(null, 5, "main", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(0);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("git", "5", "abc123");
  }

  @Test
  void noTagTopicBranchReturnsZeroVersionWithBranchMetadata() {
    var metadata = createMetadata(null, 5, "feature-x", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(0);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "5", "abc123");
  }

  @Test
  void prereleaseTagAfterTagHeadBranchAppendsDistance() {
    var metadata = createMetadata("v1.0.0-rc.1", 3, "main", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).containsExactly("rc", "1", "3");
    assertThat(version.getBuild()).containsExactly("git", "3", "abc123");
  }

  @Test
  void dirtyWorkingTreeAfterTagAddsDirtyMarker() {
    var metadata = createMetadata("v1.0.0", 3, "main", "main", "abc123", GitStatus.DIRTY);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getBuild()).containsExactly("git", "3", "abc123", "dirty");
  }

  @Test
  void tagWithoutVPrefixParsesCorrectly() {
    var metadata = createMetadata("1.0.0", 0, "main", "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
  }

  @Test
  void detachedHeadNoBranch() {
    var metadata = createMetadata("v1.0.0", 0, null, "main", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    // detached HEAD is treated as topic branch
    assertThat(version.getBuild()).containsExactly("branch", "unknown", "git", "0", "abc123");
  }

  @Test
  void noRemotesNullHeadBranch() {
    var metadata = new SimpleGitMetadata("v1.0.0", 0, "main", null, "abc123", GitStatus.CLEAN, List.of());

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    // no origin remote, so current branch != head branch (null), treated as topic
    assertThat(version.getBuild()).containsExactly("branch", "main", "git", "0", "abc123");
  }

  // Helper methods

  private static GitMetadata createMetadata(
    @Nullable String tag,
    long distance,
    @Nullable String branch,
    @Nullable String headBranch,
    String shortSha,
    GitStatus status
  ) {
    var remote = headBranch != null ? new SimpleGitRemote("origin", headBranch) : null;
    var remotes = remote != null ? java.util.List.<GitRemote>of(remote) : java.util.List.<GitRemote>of();
    return new SimpleGitMetadata(tag, distance, branch, "fullsha123", shortSha, status, remotes);
  }

  /**
   * Simple record implementation of GitMetadata for testing.
   */
  private record SimpleGitMetadata(
    @Nullable String tag,
    long distance,
    @Nullable String branch,
    @Nullable String commit,
    @Nullable String uniqueShort,
    GitStatus status,
    List<GitRemote> remotes
  ) implements GitMetadata {}

  /**
   * Simple record implementation of GitRemote for testing.
   */
  private record SimpleGitRemote(String name, String headBranch) implements GitRemote {
    @Override
    public String headBranchRefName() {
      return headBranch != null ? "refs/remotes/" + name + "/" + headBranch : null;
    }
  }
}
