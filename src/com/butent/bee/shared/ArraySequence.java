package com.butent.bee.shared;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Enables operations with arrays.
 *
 * @param <T> the object type of array
 */
public abstract class ArraySequence<T> extends AbstractSequence<T> {

  private T[] values;
  private int length;

  /**
   * Creates object with array sequence. All operations changes {@code values} array in this object.
   *
   * @param values the array of objects
   */
  public ArraySequence(T[] values) {
    Assert.notNull(values);
    this.values = values;
    this.length = values.length;
  }

  /**
   * Clear the array of object.
   */
  @Override
  public void clear() {
    this.length = 0;
  }

  /**
   * Returns the object of array contains of the index.
   *
   * @param index the object index of array
   * @return the object of array contains of the index.
   */
  @Override
  public T get(int index) {
    assertIndex(index);
    return values[index];
  }

  /**
   * Returns the array of objects.
   *
   * @return the array of objects
   */
  @Override
  public Pair<T[], Integer> getArray(T[] a) {
    return Pair.of(values, getLength());
  }

  @Override
  public int getLength() {
    return length;
  }

  /**
   * Returns the {@code List} of objects.
   *
   * @return the list of objects
   */
  @Override
  public List<T> getList() {
    List<T> list = Lists.newArrayListWithCapacity(getLength());
    if (getLength() > 0) {
      for (int i = 0; i < getLength(); i++) {
        list.add(get(i));
      }
    }
    return list;
  }

  /**
   * Insert object to array specified index of array.
   *
   * @param index index of object array
   * @param value the object inserts to array
   */
  @Override
  public void insert(int index, T value) {
    assertInsert(index);
    List<T> list = getList();
    list.add(index, value);
    setValues(list);
  }

  /**
   * Removes the object of array specified by index.
   *
   * @param index the index of object array
   */
  @Override
  public void remove(int index) {
    assertIndex(index);
    List<T> list = getList();
    list.remove(index);
    setValues(list);
  }

  /**
   * Change object to other object of array specified by index.
   *
   * @param index the index of object array.
   */
  @Override
  public void set(int index, T value) {
    assertIndex(index);
    values[index] = value;
  }

  /**
   * Converts a list {@code lst} to an array and sets it to {@code values} using.
   * {@link #setValues(T[])}
   *
   * @param lst the new value to set
   */
  @Override
  public void setValues(List<T> lst) {
    Assert.notNull(lst);
    values = lst.toArray(values);
    length = lst.size();
  }

  /**
   * Sets {@code values} to a specified array {@code arr}.
   *
   * @param arr the new value to set
   */
  @Override
  public void setValues(T[] arr) {
    Assert.notNull(arr);
    values = arr;
    length = values.length;
  }
}
