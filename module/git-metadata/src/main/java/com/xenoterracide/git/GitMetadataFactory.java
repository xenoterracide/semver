// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.git;

import com.xenoterracide.git.internal.GitMetadataImpl;
import com.xenoterracide.git.internal.TryGit;
import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jgit.api.Git;
import org.jspecify.annotations.Nullable;

/**
 * Factory for creating GitMetadata instances.
 */
public final class GitMetadataFactory {

  private GitMetadataFactory() {}

  /**
   * Creates a GitMetadata instance for the given directory.
   *
   * @param directory the git repository directory
   * @return GitMetadata instance, or null if not a valid git repository
   */
  public static @Nullable GitMetadata create(File directory) {
    Objects.requireNonNull(directory, "directory must not be null");
    return Try.ofCallable(() -> Git.open(directory))
      .map(git -> (TryGit) () -> git)
      .map(GitMetadataImpl::new)
      .getOrNull();
  }

  /**
   * Creates a GitMetadata instance for the given path.
   *
   * @param path the git repository path
   * @return GitMetadata instance, or null if not a valid git repository
   */
  public static @Nullable GitMetadata create(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    return create(path.toFile());
  }

  /**
   * Creates a GitMetadata instance for the current working directory.
   *
   * @return GitMetadata instance, or null if not in a git repository
   */
  public static @Nullable GitMetadata create() {
    return create(new File("."));
  }
}
