// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.gradle.git;

import org.gradle.api.provider.Provider;

/**
 * A provider of a value.
 *
 * @param <T>
 *   the type of value
 */
public interface Provides<T> {
  /**
   * Returns a provider of the value.
   *
   * @return a provider of the value
   */
  Provider<T> getProvider();
}
