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
    var metadata = createMetadata("v1.0.0", 5, "feature-x", "main", "abc123", GitStatus.CLEAN, 3);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
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
    var metadata = createMetadata(null, 5, "feature-x", "main", "abc123", GitStatus.CLEAN, 3);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(0);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(1);
    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
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
  void noRemoteHeadTreatsCurrentBranchAsHead() {
    var metadata = createMetadata("v1.0.0", 0, "main", null, "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void detachedHeadOnTagReturnsCleanVersion() {
    var metadata = createMetadata("v1.0.0", 0, null, "develop", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(0);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getPreRelease()).isEmpty();
    assertThat(version.getBuild()).isEmpty();
  }

  @Test
  void detachedHeadAfterTagUsesHeadStyleMetadata() {
    var metadata = createMetadata("v1.0.0", 2, null, "develop", "abc123", GitStatus.CLEAN);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version).hasToString("1.0.1-alpha.0.2+git.2.abc123");
  }

  @Test
  void afterTagTopicBranchUsesMergeBaseDistance() {
    var metadata = createMetadata("v1.0.0", 5, "feature-x", "main", "abc123", GitStatus.CLEAN, 3);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version.getPreRelease()).containsExactly("alpha", "0", "5");
    assertThat(version.getBuild()).containsExactly("branch", "feature-x", "git", "3", "abc123");
  }

  @Test
  void mergeBaseDistanceLargerThanTagDistance() {
    var metadata = createMetadata("v1.0.0", 5, "feature-x", "main", "abc123", GitStatus.CLEAN, 11);

    var version = VersionCalculator.calculate(metadata);

    assertThat(version).hasToString("1.0.1-alpha.0.5+branch.feature-x.git.11.abc123");
  }

  @Test
  void shallowCloneThrows() {
    var metadata = createMetadata("v1.0.0", 0, "main", "main", "abc123", GitStatus.CLEAN, 0, true);

    assertThatThrownBy(() -> VersionCalculator.calculate(metadata))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("shallow")
      .hasMessageContaining("git fetch --unshallow origin")
      .hasMessageContaining("fetch-depth: 0");
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
    return createMetadata(tag, distance, branch, headBranch, shortSha, status, distance);
  }

  private static GitMetadata createMetadata(
    @Nullable String tag,
    long distance,
    @Nullable String branch,
    @Nullable String headBranch,
    String shortSha,
    GitStatus status,
    long mergeBaseDistance
  ) {
    return createMetadata(tag, distance, branch, headBranch, shortSha, status, mergeBaseDistance, false);
  }

  // CHECKSTYLE.OFF: ParameterNumber - test helper needs every metadata field
  private static GitMetadata createMetadata(
    @Nullable String tag,
    long distance,
    @Nullable String branch,
    @Nullable String headBranch,
    String shortSha,
    GitStatus status,
    long mergeBaseDistance,
    boolean isShallowClone
  ) {
    // CHECKSTYLE.ON: ParameterNumber
    var remote = headBranch != null ? new SimpleGitRemote("origin", headBranch) : null;
    var remotes = remote != null ? java.util.List.<GitRemote>of(remote) : java.util.List.<GitRemote>of();
    return new SimpleGitMetadata(
      tag,
      distance,
      mergeBaseDistance,
      branch,
      "fullsha123",
      shortSha,
      status,
      remotes,
      isShallowClone
    );
  }

  // CHECKSTYLE.OFF: RecordComponentNumber - test record mirrors GitMetadata interface
  /**
   * Simple record implementation of GitMetadata for testing.
   */
  private record SimpleGitMetadata(
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
    // CHECKSTYLE.ON: RecordComponentNumber
    @Override
    public long distanceFromMergeBase() {
      return this.mergeBaseDistance;
    }
  }

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
