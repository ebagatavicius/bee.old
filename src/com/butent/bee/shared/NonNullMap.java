package com.butent.bee.shared;

import java.util.HashMap;
import java.util.Map;

public class NonNullMap<K, V> extends HashMap<K, V> {

  public NonNullMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public NonNullMap(int initialCapacity) {
    super(initialCapacity);
  }

  public NonNullMap() {
  }

  public NonNullMap(Map<? extends K, ? extends V> m) {
    super();
    putAll(m);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return key != null && newValue != null && super.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    if (key == null || value == null) {
      return null;
    } else {
      return super.replace(key, value);
    }
  }

  @Override
  public V put(K key, V value) {
    if (key == null || value == null) {
      return null;
    } else {
      return super.put(key, value);
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    if (key == null || value == null) {
      return null;
    } else {
      return super.putIfAbsent(key, value);
    }
  }
}
