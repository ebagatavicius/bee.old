package com.butent.bee.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class NonNullList<T> extends ArrayList<T> {

  public NonNullList(int initialCapacity) {
    super(initialCapacity);
  }

  public NonNullList() {
  }

  public NonNullList(Collection<? extends T> c) {
    super();
    addAll(c);
  }

  @Override
  public boolean add(T t) {
    if (t == null) {
      return false;
    } else {
      return super.add(t);
    }
  }

  @Override
  public void add(int index, T element) {
    if (element != null) {
      super.add(index, element);
    }
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    return super.addAll(c.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    return super.addAll(index, c.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }
}
