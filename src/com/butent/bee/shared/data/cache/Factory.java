package com.butent.bee.shared.data.cache;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

final class Factory {

  static <K, V> AbstractCache<K, V> getCacheImpl(int maxSize, ReplacementPolicy replacementPolicy) {
    return replacementPolicy.getDefaultImpl().create(maxSize, replacementPolicy);
  }

  static <K> Multimap<K, Long> getHistoryImpl(ReplacementPolicy replacementPolicy) {
    if (replacementPolicy.isHistoryRequired()) {
      return ArrayListMultimap.create();
    } else {
      return null;
    }
  }

  private Factory() {
    super();
  }
}
