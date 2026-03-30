// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import com.xenoterracide.git.GitMetadata;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.semver4j.Semver;

/**
 * Calculates semantic versions from git metadata.
 *
 * <p>This is the main entry point for version calculation. It takes git metadata
 * and produces a semantic version based on the current git state.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * var gitMetadata = GitMetadataFactory.create(new File("."));
 * var version = VersionCalculator.calculate(gitMetadata);
 * System.out.println(version); // e.g., "1.0.1-alpha.0.5+git.5.abc123"
 * }</pre>
 */
final class VersionCalculator {

  private VersionCalculator() {
    // utility class
  }

  /**
   * Calculates the semantic version from git metadata.
   *
   * @param metadata the git metadata
   * @return the calculated semantic version
   * @throws NullPointerException if metadata is null
   */
  public static Semver calculate(GitMetadata metadata) {
    Objects.requireNonNull(metadata, "metadata must not be null");
    var ctx = toGitContext(metadata);
    var strategy = VersionStrategyFactory.determineStrategy(ctx);
    return strategy.calculate();
  }

  /**
   * Creates a GitContext from GitMetadata.
   *
   * @param metadata the git metadata
   * @return the git context
   */
  static GitContext toGitContext(GitMetadata metadata) {
    var tag = metadata.tag();
    var distance = metadata.distance();
    var branch = metadata.branch();
    var headBranch = extractHeadBranch(metadata);
    var isHeadBranch = branch != null && branch.equals(headBranch);

    return GitContext.builder()
      .nearestTag(tag)
      .distanceFromTag(distance)
      .isOnTagExact(distance == 0 && tag != null)
      .currentBranch(branch)
      .headBranch(headBranch)
      .isHeadBranch(isHeadBranch)
      .distanceFromMergeBase(distance)
      .shortSha(metadata.uniqueShort())
      .fullSha(metadata.commit())
      .isDirty(metadata.status().name().equals("DIRTY"))
      .isShallowClone(false)
      .build();
  }

  private static @Nullable String extractHeadBranch(GitMetadata metadata) {
    return metadata
      .remotes()
      .stream()
      .filter(r -> r.name().equals("origin"))
      .findFirst()
      .map(r -> r.headBranch())
      .orElse(null);
  }
}
