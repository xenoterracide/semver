// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.git;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Internal utility class for exception handling.
 */
final class Exceptions {

  private Exceptions() {}

  /**
   * Converts a checked exception to a runtime exception.
   * IOException is wrapped in UncheckedIOException.
   *
   * @param throwable the exception to convert
   * @return a runtime exception
   */
  static RuntimeException toRuntime(Throwable throwable) {
    if (throwable instanceof RuntimeException re) {
      return re;
    }
    if (throwable instanceof IOException ioe) {
      return new UncheckedIOException(ioe);
    }
    return new RuntimeException(throwable);
  }
}
