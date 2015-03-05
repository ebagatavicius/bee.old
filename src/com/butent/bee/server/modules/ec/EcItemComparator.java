package com.butent.bee.server.modules.ec;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.ec.EcItem;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

class EcItemComparator implements Comparator<EcItem> {

  private static String normalize(String s) {
    return (s == null) ? BeeConst.STRING_EMPTY : s.toLowerCase();
  }

  private final Collator collator;
  private final String queryCode;

  EcItemComparator(Locale locale, String queryCode) {
    this.collator = Collator.getInstance(locale);
    collator.setStrength(Collator.IDENTICAL);

    this.queryCode = queryCode;
  }

  @Override
  public int compare(EcItem o1, EcItem o2) {
    int diff;
    if (queryCode != null) {
      diff = Boolean.compare(queryCode.equals(o2.getCode()), queryCode.equals(o1.getCode()));
      if (diff != BeeConst.COMPARE_EQUAL) {
        return diff;
      }
    }

    diff = Double.compare(o2.getPrimaryStock(), o1.getPrimaryStock());
    if (diff != BeeConst.COMPARE_EQUAL) {
      return diff;
    }

    diff = Double.compare(o2.getSecondaryStock(), o1.getSecondaryStock());
    if (diff != BeeConst.COMPARE_EQUAL) {
      return diff;
    }

    diff = Integer.compare(o1.getPrice(), o2.getPrice());
    if (diff != BeeConst.COMPARE_EQUAL) {
      return diff;
    }

    diff = collator.compare(normalize(o1.getName()), o2.getName());
    if (diff != BeeConst.COMPARE_EQUAL) {
      return diff;
    }

    diff = collator.compare(normalize(o1.getCode()), o2.getCode());
    if (diff != BeeConst.COMPARE_EQUAL) {
      return diff;
    }

    return Long.compare(o1.getArticleId(), o2.getArticleId());
  }
}
