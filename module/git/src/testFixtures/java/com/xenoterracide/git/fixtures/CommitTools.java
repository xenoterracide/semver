// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.git.fixtures;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.jspecify.annotations.Nullable;

/**
 * Test fixtures for creating git commits.
 */
public final class CommitTools {

  private static final ThreadLocal<IntSupplier> NEXT_INT = ThreadLocal.withInitial(() ->
    IntStream.iterate(0, i -> i + 1).iterator()::nextInt
  );

  private CommitTools() {}

  /**
   * Creates a commit in the given repository.
   *
   * @param git the git repository
   * @return the commit object id
   * @throws GitAPIException if commit fails
   */
  public static ObjectId commit(Git git) throws GitAPIException {
    var message = "commit %d".formatted(NEXT_INT.get().getAsInt());
    var commit = git.commit().setMessage(message).call();
    return commit.toObjectId();
  }

  /**
   * Creates a commit using a git supplier.
   *
   * @param git the git supplier
   * @return the commit object id
   * @throws GitAPIException if commit fails
   */
  public static ObjectId commit(Supplier<Git> git) throws GitAPIException {
    return commit(git.get());
  }

  /**
   * Just for silly single statement one-liners that reduce boilerplate.
   *
   * @param ignored ignored parameter for chaining
   * @param supplier the supplier
   * @return the supplied value
   * @param <T> the type
   */
  public static <T> T supplies(@Nullable ObjectId ignored, Supplier<T> supplier) {
    return supplier.get();
  }
}
