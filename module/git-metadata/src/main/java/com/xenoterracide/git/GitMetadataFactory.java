// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.jspecify.annotations.Nullable;

/**
 * Factory for creating {@link GitMetadata} instances.
 *
 * <p>Provides convenient static methods for creating GitMetadata
 * from files or paths.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * var metadata = GitMetadataFactory.create(new File("."));
 * if (metadata != null) {
 *   System.out.println(metadata.tag());
 * }
 * }</pre>
 */
public final class GitMetadataFactory {

  private GitMetadataFactory() {
    // utility class
  }

  /**
   * Creates a {@link GitMetadata} instance from a directory.
   *
   * @param directory the directory containing a Git repository
   * @return a GitMetadata instance, or null if the directory
   *   is not a valid Git repository
   */
  public static @Nullable GitMetadata create(File directory) {
    try {
      var git = Git.open(directory);
      return new GitMetadataImpl(() -> git);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Creates a {@link GitMetadata} instance from a path string.
   *
   * @param path the path to a directory containing a Git repository
   * @return a GitMetadata instance, or null if the path
   *   is not a valid Git repository
   */
  public static @Nullable GitMetadata create(String path) {
    return create(new File(path));
  }
}
