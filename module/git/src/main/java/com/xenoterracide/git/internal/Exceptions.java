// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.git.internal;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Internal utility class for exception handling.
 */
public final class Exceptions {

  private Exceptions() {}

  /**
   * Converts a checked exception to a runtime exception.
   * IOException is wrapped in UncheckedIOException.
   *
   * @param throwable the exception to convert
   * @return a runtime exception
   */
  public static RuntimeException toRuntime(Throwable throwable) {
    if (throwable instanceof RuntimeException re) {
      return re;
    }
    if (throwable instanceof IOException ioe) {
      return new UncheckedIOException(ioe);
    }
    return new RuntimeException(throwable);
  }
}
