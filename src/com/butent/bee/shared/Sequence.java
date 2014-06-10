package com.butent.bee.shared;

import java.util.List;

/**
 * Enables using string sequences to store data in them.
 */

public interface Sequence<T> extends HasLength, Iterable<T> {
  
  void add(T value);

  void clear();
  
  T get(int index);

  Pair<T[], Integer> getArray(T[] a);

  List<T> getList();

  void insert(int index, T value);

  void remove(int index);

  void set(int index, T value);

  void setValues(List<T> lst);

  void setValues(T[] arr);
}
