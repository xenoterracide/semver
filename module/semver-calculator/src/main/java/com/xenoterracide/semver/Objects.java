// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver;

import org.jspecify.annotations.Nullable;

/**
 * Internal utility class for object operations.
 */
final class Objects {

  private Objects() {}

  /**
   * Returns the value if non-null, otherwise throws IllegalStateException with the given message.
   *
   * @param <T> the type of the value
   * @param value the value to check
   * @param message the exception message
   * @return the value if non-null
   * @throws IllegalStateException if value is null
   */
  static <T> T requireNonNullElseThrow(@Nullable T value, String message) {
    if (value == null) {
      throw new IllegalStateException(message);
    }
    return value;
  }
}
