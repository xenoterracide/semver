// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.semver;

import org.jspecify.annotations.Nullable;

/**
 * Immutable value object containing all git-derived context for version calculation.
 *
 * @param nearestTag the nearest semantic version tag (e.g., "v1.0.0"), or null if no tags exist
 * @param distanceFromTag number of commits from the nearest tag to HEAD
 * @param isOnTagExact true if HEAD is exactly on a tag (distanceFromTag == 0)
 * @param currentBranch the current branch name, or null if detached HEAD
 * @param headBranch the HEAD branch name from origin (e.g., "main", "develop"), or null if unknown
 * @param isHeadBranch true if current branch is the HEAD branch
 * @param distanceFromMergeBase number of commits from merge base to HEAD
 * @param shortSha the abbreviated commit SHA, or null if not available
 * @param fullSha the full 40-character commit SHA, or null if not available
 * @param isDirty true if working tree has uncommitted changes
 * @param isShallowClone true if repository is a shallow clone
 */
public record GitContext(
  @Nullable String nearestTag,
  long distanceFromTag,
  boolean isOnTagExact,
  @Nullable String currentBranch,
  @Nullable String headBranch,
  boolean isHeadBranch,
  long distanceFromMergeBase,
  @Nullable String shortSha,
  @Nullable String fullSha,
  boolean isDirty,
  boolean isShallowClone
) {
  /**
   * Creates a builder for GitContext.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if any tags exist in the repository history.
   *
   * @return true if a nearest tag exists
   */
  public boolean hasTagInHistory() {
    return this.nearestTag != null;
  }

  /**
   * Gets the base version string without the 'v' prefix.
   *
   * @return the base version (e.g., "1.0.0"), or null if no tag
   */
  public @Nullable String baseVersion() {
    if (this.nearestTag == null) {
      return null;
    }
    return this.nearestTag.startsWith("v") ? this.nearestTag.substring(1) : this.nearestTag;
  }

  /**
   * Builder for GitContext.
   */
  public static class Builder {

    private @Nullable String nearestTag;
    private long distanceFromTag;
    private boolean isOnTagExact;
    private @Nullable String currentBranch;
    private @Nullable String headBranch;
    private boolean isHeadBranch;
    private long distanceFromMergeBase;
    private @Nullable String shortSha;
    private @Nullable String fullSha;
    private boolean isDirty;
    private boolean isShallowClone;

    public Builder nearestTag(@Nullable String nearestTag) {
      this.nearestTag = nearestTag;
      return this;
    }

    public Builder distanceFromTag(long distanceFromTag) {
      this.distanceFromTag = distanceFromTag;
      return this;
    }

    public Builder isOnTagExact(boolean isOnTagExact) {
      this.isOnTagExact = isOnTagExact;
      return this;
    }

    public Builder currentBranch(@Nullable String currentBranch) {
      this.currentBranch = currentBranch;
      return this;
    }

    public Builder headBranch(@Nullable String headBranch) {
      this.headBranch = headBranch;
      return this;
    }

    public Builder isHeadBranch(boolean isHeadBranch) {
      this.isHeadBranch = isHeadBranch;
      return this;
    }

    public Builder distanceFromMergeBase(long distanceFromMergeBase) {
      this.distanceFromMergeBase = distanceFromMergeBase;
      return this;
    }

    public Builder shortSha(@Nullable String shortSha) {
      this.shortSha = shortSha;
      return this;
    }

    public Builder fullSha(@Nullable String fullSha) {
      this.fullSha = fullSha;
      return this;
    }

    public Builder isDirty(boolean isDirty) {
      this.isDirty = isDirty;
      return this;
    }

    public Builder isShallowClone(boolean isShallowClone) {
      this.isShallowClone = isShallowClone;
      return this;
    }

    public GitContext build() {
      return new GitContext(
        nearestTag,
        distanceFromTag,
        isOnTagExact,
        currentBranch,
        headBranch,
        isHeadBranch,
        distanceFromMergeBase,
        shortSha,
        fullSha,
        isDirty,
        isShallowClone
      );
    }
  }
}
