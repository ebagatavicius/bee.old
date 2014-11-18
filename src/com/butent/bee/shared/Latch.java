package com.butent.bee.shared;

public class Latch extends Holder<Integer> {

  public Latch(int value) {
    super(value);
  }

  public void decrement() {
    set(get() - 1);
  }

  public void increment() {
    set(get() + 1);
  }

  public boolean isClosed() {
    return !isOpen();
  }

  public boolean isOpen() {
    return get() == 0;
  }
}
