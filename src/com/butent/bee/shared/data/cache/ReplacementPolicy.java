package com.butent.bee.shared.data.cache;

import com.google.common.collect.Multimap;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

/**
 * Contains a list of possible cache objects replacement management types, for example first in
 * first out or least recently used.
 */

public enum ReplacementPolicy {
  LEAST_FREQUENTLY_USED(true, false, false, CacheImpl.LIST),
  LEAST_RECENTLY_USED(true, false, true, CacheImpl.MAP),
  LEAST_RECENTLY_USED_2(true, false, true, CacheImpl.MAP),
  TWO_QUEUES(true, false, false, CacheImpl.MAP),
  ADAPTIVE_REPLACEMENT_CACHE(true, false, false, CacheImpl.MAP),
  MOST_RECENTLY_USED(false, true, false, CacheImpl.LIST),
  FIRST_IN_FIRST_OUT(false, false, false, CacheImpl.MAP),
  SECOND_CHANCE(true, false, false, CacheImpl.MAP),
  CLOCK(true, false, false, CacheImpl.MAP),
  SIMPLE_TIME_BASED(true, false, false, CacheImpl.MAP),
  EXTENDED_TIME_BASED(true, false, false, CacheImpl.MAP),
  SLIDING_TIME_BASED(true, false, false, CacheImpl.MAP),
  RANDOM(false, false, false, CacheImpl.LIST);

  private final boolean requiresHistory;
  private final boolean addFirst;
  private final boolean accessOrder;

  private final CacheImpl defaultImpl;

  private ReplacementPolicy(boolean requiresHistory, boolean addFirst, boolean accessOrder,
      CacheImpl defaultImpl) {
    this.requiresHistory = requiresHistory;
    this.addFirst = addFirst;
    this.accessOrder = accessOrder;

    this.defaultImpl = defaultImpl;
  }

  public boolean addFirst() {
    return addFirst;
  }

  public boolean addLast() {
    return !addFirst;
  }

  public CacheImpl getDefaultImpl() {
    return defaultImpl;
  }

  public <K> K getEvictee(Collection<K> keys, Multimap<K, Long> history) {
    Assert.notEmpty(keys);
    K result = BeeUtils.peek(keys);

    int size = keys.size();
    if (size <= 1 || history == null || history.isEmpty()) {
      return result;
    }

    switch (this) {
      case LEAST_FREQUENTLY_USED:
        Collection<Long> hits = history.get(result);
        if (hits == null) {
          return result;
        }
        int minCnt = hits.size();
        if (minCnt <= 0) {
          return result;
        }

        int cnt;
        for (K key : keys) {
          if (result == key) {
            break;
          }
          hits = history.get(key);
          if (hits == null) {
            result = key;
            break;
          }

          cnt = hits.size();
          if (cnt <= 0) {
            result = key;
            break;
          }
          if (cnt < minCnt) {
            result = key;
            minCnt = cnt;
          }
        }
        break;

      default:
    }
    return result;
  }

  public <K> int getEvictionIndex(List<K> keys, Multimap<K, Long> history) {
    Assert.notEmpty(keys);
    int size = keys.size();
    if (size <= 1 || history == null || history.isEmpty()) {
      return 0;
    }

    int idx;
    switch (this) {
      case MOST_RECENTLY_USED:
        idx = size - 1;
        break;

      case RANDOM:
        idx = BeeUtils.randomInt(0, size);
        break;

      case LEAST_FREQUENTLY_USED:
        int minIdx = 0;
        Collection<Long> hits = history.get(keys.get(minIdx));
        if (hits == null) {
          return minIdx;
        }
        int minCnt = hits.size();
        if (minCnt <= 0) {
          return minIdx;
        }

        int cnt;
        for (int i = minIdx + 1; i < size; i++) {
          hits = history.get(keys.get(i));
          if (hits == null) {
            minIdx = i;
            break;
          }

          cnt = hits.size();
          if (cnt <= 0) {
            minIdx = i;
            break;
          }
          if (cnt < minCnt) {
            minIdx = i;
            minCnt = cnt;
          }
        }

        idx = minIdx;
        break;

      default:
        idx = 0;
    }
    return idx;
  }

  public boolean isAccessOrder() {
    return accessOrder;
  }

  public boolean isHistoryRequired() {
    return requiresHistory;
  }
}
