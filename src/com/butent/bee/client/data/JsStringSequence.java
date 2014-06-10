package com.butent.bee.client.data;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.JsArrayString;

import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.AbstractSequence;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;

import java.util.List;

/**
 * Enables to set values, clear and get information for a wrapper around a homogeneous native array
 * of string values.
 */

public class JsStringSequence extends AbstractSequence<String> {

  private JsArrayString values;

  public JsStringSequence(int size) {
    Assert.nonNegative(size);
    this.values = JsUtils.createArray(size);
  }

  public JsStringSequence(JsArrayString values) {
    Assert.notNull(values);
    this.values = values;
  }

  @Override
  public void clear() {
    values.setLength(0);
  }

  @Override
  public String get(int index) {
    assertIndex(index);
    return values.get(index);
  }

  @Override
  public Pair<String[], Integer> getArray(String[] a) {
    String[] arr = new String[getLength()];
    for (int i = 0; i < getLength(); i++) {
      arr[i] = get(i);
    }
    return Pair.of(arr, getLength());
  }

  @Override
  public int getLength() {
    return values.length();
  }

  @Override
  public List<String> getList() {
    List<String> lst = Lists.newArrayListWithCapacity(getLength());
    for (int i = 0; i < getLength(); i++) {
      lst.add(get(i));
    }
    return lst;
  }

  @Override
  public void insert(int index, String value) {
    assertInsert(index);
    JsUtils.insert(values, index, value);
  }

  @Override
  public void remove(int index) {
    assertIndex(index);
    JsUtils.remove(values, index);
  }

  @Override
  public void set(int index, String value) {
    assertIndex(index);
    values.set(index, value);
  }

  @Override
  public void setValues(List<String> lst) {
    Assert.notNull(lst);
    if (getLength() != lst.size()) {
      this.values = JsUtils.createArray(lst.size());
    }
    for (int i = 0; i < getLength(); i++) {
      set(i, lst.get(i));
    }
  }

  @Override
  public void setValues(String[] arr) {
    Assert.notNull(arr);
    if (getLength() != arr.length) {
      this.values = JsUtils.createArray(arr.length);
    }
    for (int i = 0; i < getLength(); i++) {
      set(i, arr[i]);
    }
  }
}
