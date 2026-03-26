// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-License-Identifier: GPL-3.0-or-later WITH Classpath-exception-2.0

package com.xenoterracide.git.internal;

import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.util.SystemReader.Delegate;

/**
 * System reader that prevents execution of external git commands.
 */
public class NoExecSystemReader extends Delegate {

  static {
    getCreateAndSet();
  }

  public NoExecSystemReader() {
    super(SystemReader.getInstance());
  }

  static SystemReader getOrNew() {
    if (SystemReader.getInstance() instanceof NoExecSystemReader) return SystemReader.getInstance();
    return new NoExecSystemReader();
  }

  static SystemReader getCreateAndSet() {
    var reader = getOrNew();
    if (reader != SystemReader.getInstance()) {
      SystemReader.setInstance(reader);
    }
    return reader;
  }

  @Override
  public String getenv(String variable) {
    if ("PATH".equals(variable)) {
      return "";
    } else {
      return super.getenv(variable);
    }
  }
}
