package com.butent.bee.shared.data;

public enum ProviderType {
  ASYNC, CACHED, LOCAL;

  public static final ProviderType DEFAULT = ASYNC;
}
