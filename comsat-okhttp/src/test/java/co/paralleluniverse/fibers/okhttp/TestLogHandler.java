/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Based on the corresponding class in okhttp-tests.
 * Copyright 2014 Square, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package co.paralleluniverse.fibers.okhttp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A log handler that records which log messages were published so that a calling test can make
 * assertions about them.
 */
public final class TestLogHandler extends Handler {
  private final List<String> logs = new ArrayList<>();

  @Override public synchronized void publish(LogRecord logRecord) {
    logs.add(logRecord.getLevel() + ": " + logRecord.getMessage());
    notifyAll();
  }

  @Override public void flush() {
  }

  @Override public void close() throws SecurityException {
  }

  public synchronized String take() throws InterruptedException {
    while (logs.isEmpty()) {
      wait();
    }
    return logs.remove(0);
  }
}
