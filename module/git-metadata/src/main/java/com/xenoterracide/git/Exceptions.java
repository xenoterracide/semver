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
    RuntimeException result;
    if (throwable instanceof RuntimeException re) {
      result = re;
    } else if (throwable instanceof IOException ioe) {
      result = new UncheckedIOException(ioe);
    } else {
      result = new RuntimeException(throwable);
    }
    return result;
  }
}
