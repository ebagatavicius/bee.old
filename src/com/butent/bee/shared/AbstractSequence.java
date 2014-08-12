package com.butent.bee.shared;

import java.util.Iterator;

/**
 * Is an abstract class for sequence implementing classes in the system, describes iterator
 * operations.
 */

public abstract class AbstractSequence<T> implements Sequence<T> {

  private class SequenceIterator implements Iterator<T> {
    private int index = -1;

    @Override
    public boolean hasNext() {
      return index < (getLength() - 1);
    }

    @Override
    public T next() {
      if (index >= getLength()) {
        Assert.untouchable();
      }
      return get(++index);
    }

    @Override
    public void remove() {
      Assert.state(index >= 0 && index < getLength());
      AbstractSequence.this.remove(index--);
    }
  }

  /**
   * Insert object value to end of array.
   */
  @Override
  public void add(T value) {
    insert(getLength(), value);
  }

  /**
   * @return a new SequenceIterator
   */
  @Override
  public Iterator<T> iterator() {
    return new SequenceIterator();
  }

  protected void assertIndex(int index) {
    Assert.betweenExclusive(index, 0, getLength());
  }

  protected void assertInsert(int index) {
    Assert.betweenInclusive(index, 0, getLength());
  }
}
