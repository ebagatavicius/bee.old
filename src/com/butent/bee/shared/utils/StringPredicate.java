package com.butent.bee.shared.utils;

import com.google.common.base.Predicate;

public enum StringPredicate implements Predicate<String> {

  IS_EMPTY {
    @Override
    public boolean apply(String s) {
      return s == null || s.isEmpty();
    }
  },

  NOT_EMPTY {
    @Override
    public boolean apply(String s) {
      return s != null && !s.isEmpty();
    }
  },

  IS_NULL {
    @Override
    public boolean apply(String s) {
      return s == null;
    }
  },

  NOT_NULL {
    @Override
    public boolean apply(String s) {
      return s != null;
    }
  }
}