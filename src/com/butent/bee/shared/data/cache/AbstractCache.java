package com.butent.bee.shared.data.cache;

import com.google.common.collect.Multimap;

import com.butent.bee.shared.Assert;

abstract class AbstractCache<K, V> {

  private final int maxSize;
  private final ReplacementPolicy replacementPolicy;

  AbstractCache(int maxSize, ReplacementPolicy replacementPolicy) {
    this.maxSize = maxSize;
    this.replacementPolicy = Assert.notNull(replacementPolicy);
  }

  public abstract void add(K key, V value);

  public abstract void clear();

  public abstract boolean containsKey(K key);

  public abstract boolean deleteKey(K key);

  public abstract K deleteValue(V value);

  public abstract void evict(Multimap<K, Long> history);

  public abstract V get(K key);

  public double getFillRatio() {
    if (maxSize <= 0) {
      return -1.0;
    }
    int size = getSize();
    if (size > 0) {
      return (double) size / maxSize;
    } else {
      return 0.0;
    }
  }

  public int getMaxSize() {
    return maxSize;
  }

  public ReplacementPolicy getReplacementPolicy() {
    return replacementPolicy;
  }

  public abstract int getSize();

  public boolean isEmpty() {
    return getSize() <= 0;
  }

  public boolean isFull() {
    if (maxSize > 0) {
      return getSize() >= maxSize;
    } else {
      return false;
    }
  }

  public abstract void onAccess(K key);

  public abstract boolean update(K key, V value);
}
