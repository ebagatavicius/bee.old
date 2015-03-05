package com.butent.bee.shared.data.cache;

import com.google.common.collect.Multimap;

import java.util.LinkedList;

class ListImpl<K, V> extends AbstractCache<K, V> {

  private final LinkedList<K> keys = new LinkedList<>();
  private final LinkedList<V> values = new LinkedList<>();

  ListImpl(int maxSize, ReplacementPolicy replacementPolicy) {
    super(maxSize, replacementPolicy);
  }

  @Override
  public void add(K key, V value) {
    if (getReplacementPolicy().addLast()) {
      keys.addLast(key);
      values.addLast(value);
    } else {
      keys.addFirst(key);
      values.addFirst(value);
    }
  }

  @Override
  public void clear() {
    keys.clear();
    values.clear();
  }

  @Override
  public boolean containsKey(K key) {
    return keys.contains(key);
  }

  @Override
  public boolean deleteKey(K key) {
    int index = keys.indexOf(key);
    if (index >= 0) {
      remove(index);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public K deleteValue(V value) {
    int index = values.indexOf(value);
    if (index >= 0) {
      K key = keys.get(index);
      remove(index);
      return key;
    } else {
      return null;
    }
  }

  @Override
  public void evict(Multimap<K, Long> history) {
    int index = getReplacementPolicy().getEvictionIndex(keys, history);
    remove(index);
  }

  @Override
  public V get(K key) {
    int index = keys.indexOf(key);
    if (index >= 0) {
      V value = values.get(index);
      return value;
    } else {
      return null;
    }
  }

  @Override
  public int getSize() {
    return keys.size();
  }

  @Override
  public void onAccess(K key) {
    if (getReplacementPolicy().isAccessOrder()) {
      int index = keys.indexOf(key);
      if (index < 0 || index == getSize()) {
        return;
      }

      V value = values.get(index);

      keys.remove(index);
      values.remove(index);

      keys.add(key);
      values.add(value);
    }
  }

  @Override
  public boolean update(K key, V value) {
    int index = keys.indexOf(key);
    if (index >= 0) {
      values.set(index, value);
      return true;
    } else {
      return false;
    }
  }

  private void remove(int index) {
    keys.remove(index);
    values.remove(index);
  }
}
