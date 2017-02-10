package com.butent.bee.server.modules.mail;

public class ProgressInterruptedException extends Exception {
  private final int processed;

  public ProgressInterruptedException(int processed) {
    this.processed = processed;
  }

  public int getProcessed() {
    return processed;
  }
}
