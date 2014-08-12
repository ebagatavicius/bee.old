package com.butent.bee.shared.data.cache;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

class MapImpl<K, V> extends AbstractCache<K, V> {

  @SuppressWarnings("serial")
  private class LimitedMap<L, U> extends LinkedHashMap<L, U> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public LimitedMap(boolean accessOrder) {
      super(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, accessOrder);
    }

    @Override
    protected boolean removeEldestEntry(Entry<L, U> eldest) {
      return this.size() > MapImpl.this.getMaxSize();
    }
  }

  private final Map<K, V> map;

  MapImpl(int maxSize, ReplacementPolicy replacementPolicy) {
    super(maxSize, replacementPolicy);

    if (maxSize > 0) {
      this.map = new LimitedMap<>(replacementPolicy.isAccessOrder());
    } else {
      this.map = Maps.newHashMap();
    }
  }

  @Override
  public void add(K key, V value) {
    map.put(key, value);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean containsKey(K key) {
    return map.containsKey(key);
  }

  @Override
  public boolean deleteKey(K key) {
    return map.remove(key) != null;
  }

  @Override
  public K deleteValue(V value) {
    K key = null;
    for (Map.Entry<K, V> entry : map.entrySet()) {
      if (entry.getValue().equals(value)) {
        key = entry.getKey();
        break;
      }
    }

    if (key != null) {
      map.remove(key);
    }
    return key;
  }

  @Override
  public void evict(Multimap<K, Long> history) {
  }

  @Override
  public V get(K key) {
    return map.get(key);
  }

  @Override
  public int getSize() {
    return map.size();
  }

  @Override
  public void onAccess(K key) {
  }

  @Override
  public boolean update(K key, V value) {
    if (map.containsKey(key)) {
      map.put(key, value);
      return true;
    } else {
      return false;
    }
  }
}
