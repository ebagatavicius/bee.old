package com.butent.bee.shared.data.cache;

import com.google.common.collect.Multimap;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * Enables to store frequently used data into memory, contains usage attributes like hit count or
 * last hit.
 */

public class SimpleCache<K, V> implements HasInfo {

  private final AbstractCache<K, V> impl;

  private final Multimap<K, Long> history;

  private int hitCount;
  private int missCount;

  private int addCount;
  private int evictionCount;

  private long lastHit;
  private long lastMiss;

  private long lastAdd;
  private long lastEviction;

  private boolean recordStats;

  public SimpleCache(int maxSize, ReplacementPolicy replacementPolicy) {
    this.impl = Factory.getCacheImpl(maxSize, replacementPolicy);
    this.history = Factory.getHistoryImpl(replacementPolicy);
  }

  public SimpleCache(AbstractCache<K, V> impl, Multimap<K, Long> history) {
    this.impl = impl;
    this.history = history;
  }

  public int getAddCount() {
    return addCount;
  }

  public int getEvictionCount() {
    return evictionCount;
  }

  public int getHistorySize() {
    if (history == null) {
      return BeeConst.UNDEF;
    } else {
      return history.size();
    }
  }

  public int getHitCount() {
    return hitCount;
  }

  public double getHitRatio() {
    if (hitCount > 0) {
      return (double) hitCount / (hitCount + missCount);
    } else {
      return 0.0;
    }
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("Size", impl.getSize(),
        "Max Size", impl.getMaxSize(),
        "Is Full", impl.isFull(),
        "Fill Ratio", impl.getFillRatio(),
        "Replacement Policy", impl.getReplacementPolicy(),
        "History Size", getHistorySize(),
        "Impl", NameUtils.getName(impl),
        "Record Stats", recordStats());

    if (recordStats()) {
      PropertyUtils.addProperties(info, "Add Count", getAddCount(),
          "Eviction Count", getEvictionCount(),
          "Last Add", transformMillis(getLastAdd()),
          "Last Eviction", transformMillis(getLastEviction()),
          "Hit Count", getHitCount(),
          "Miss Count", getMissCount(),
          "Hit Ratio", getHitRatio(),
          "Last Hit", transformMillis(getLastHit()),
          "Last Miss", transformMillis(getLastMiss()));
    }
    return info;
  }

  public long getLastAdd() {
    return lastAdd;
  }

  public long getLastEviction() {
    return lastEviction;
  }

  public long getLastHit() {
    return lastHit;
  }

  public long getLastMiss() {
    return lastMiss;
  }

  public int getMissCount() {
    return missCount;
  }

  public boolean isEmpty() {
    return impl.isEmpty();
  }

  public boolean isFull() {
    return impl.isFull();
  }

  public boolean recordStats() {
    return recordStats;
  }

  public void setRecordStats(boolean recordStats) {
    this.recordStats = recordStats;
  }

  protected synchronized void add(K key, V value) {
    if (recordStats) {
      addCount++;
      lastAdd = currentMillis();
    }

    if (impl.update(key, value)) {
      return;
    }

    if (impl.isFull()) {
      evict();
    }
    impl.add(key, value);
  }

  protected synchronized void clearHistory() {
    if (history != null) {
      history.clear();
    }
  }

  protected synchronized boolean containsKey(K key) {
    return impl.containsKey(key);
  }

  protected synchronized boolean deleteKey(K key) {
    if (impl.deleteKey(key)) {
      if (history != null) {
        history.removeAll(key);
      }
      return true;
    } else {
      return false;
    }
  }

  protected synchronized int deleteValue(V value) {
    K key = impl.deleteValue(value);
    if (key == null) {
      return 0;
    }

    if (history != null) {
      history.removeAll(key);
    }
    return deleteValue(value) + 1;
  }

  protected synchronized V get(K key) {
    V value = impl.get(key);
    if (value == null) {
      setMiss();
    } else {
      setHit(key);
    }
    return value;
  }

  protected void invalidate() {
    clear();
  }

  private synchronized void clear() {
    impl.clear();
    clearHistory();

    hitCount = 0;
    missCount = 0;
    addCount = 0;
    evictionCount = 0;

    lastHit = 0;
    lastMiss = 0;
    lastAdd = 0;
    lastEviction = 0;
  }

  private static long currentMillis() {
    return System.currentTimeMillis();
  }

  private void evict() {
    if (recordStats) {
      evictionCount++;
      lastEviction = currentMillis();
    }
    impl.evict(history);
  }

  private void setHit(K key) {
    if (recordStats) {
      hitCount++;
      lastHit = currentMillis();
    }
    if (history != null) {
      history.put(key, currentMillis());
    }
    impl.onAccess(key);
  }

  private void setMiss() {
    if (recordStats) {
      missCount++;
      lastMiss = currentMillis();
    }
  }

  private static String transformMillis(long millis) {
    if (millis <= 0) {
      return BeeConst.STRING_MINUS;
    } else {
      return new DateTime(millis).toString();
    }
  }
}
