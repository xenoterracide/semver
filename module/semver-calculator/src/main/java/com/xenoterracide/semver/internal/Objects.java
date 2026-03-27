// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.xenoterracide.semver.internal;

import org.jspecify.annotations.Nullable;

/**
 * Internal utility class for object operations.
 */
public final class Objects {

  private Objects() {}

  /**
   * Returns the value if non-null, otherwise throws IllegalStateException with the given message.
   *
   * @param value the value to check
   * @param message the exception message
   * @return the value if non-null
   * @param <T> the type of the value
   * @throws IllegalStateException if value is null
   */
  public static <T> T requireNonNullElseThrow(@Nullable T value, String message) {
    if (value == null) {
      throw new IllegalStateException(message);
    }
    return value;
  }
}
