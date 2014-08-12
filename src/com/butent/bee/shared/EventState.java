package com.butent.bee.shared;

public enum EventState {
  PROCESSING {
    @Override
    public boolean proceed() {
      return true;
    }
  },
  CONSUMED {
    @Override
    public boolean proceed() {
      return false;
    }
  },
  CANCELED {
    @Override
    public boolean proceed() {
      return false;
    }
  };

  public abstract boolean proceed();
}
