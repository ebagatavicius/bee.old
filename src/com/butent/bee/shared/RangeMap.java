package com.butent.bee.shared;

import com.google.common.collect.Range;

import java.util.HashMap;

@SuppressWarnings("serial")
public final class RangeMap<K extends Comparable<?>, V> extends HashMap<Range<K>, V> {

  public static <K extends Comparable<?>, V> RangeMap<K, V> create() {
    return new RangeMap<>();
  }

  public static <K extends Comparable<?>, V> RangeMap<K, V> create(Range<K> range, V value) {
    RangeMap<K, V> rangeMap = create();
    rangeMap.put(range, value);
    return rangeMap;
  }

  public static <K extends Comparable<?>, V> RangeMap<K, V> create(Range<K> r1, V v1,
      Range<K> r2, V v2) {
    RangeMap<K, V> rangeMap = create();
    rangeMap.put(r1, v1);
    rangeMap.put(r2, v2);
    return rangeMap;
  }

  public static <K extends Comparable<?>, V> RangeMap<K, V> create(Range<K> r1, V v1,
      Range<K> r2, V v2, Range<K> r3, V v3) {
    RangeMap<K, V> rangeMap = create();
    rangeMap.put(r1, v1);
    rangeMap.put(r2, v2);
    rangeMap.put(r3, v3);
    return rangeMap;
  }

  private RangeMap() {
    super();
  }

  public V get(K key) {
    Range<K> bestRange = null;

    for (Range<K> range : keySet()) {
      if (range != null && range.contains(key)
          && (bestRange == null || bestRange.encloses(range))) {
        bestRange = range;
      }
    }

    if (bestRange == null) {
      return null;
    }
    return super.get(bestRange);
  }
}
