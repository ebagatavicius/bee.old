package com.butent.bee.client.modules.service;

import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.Longs;

import com.butent.bee.client.i18n.Collator;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Comparator;
import java.util.Objects;

class ServiceCompanyWrapper implements Comparable<ServiceCompanyWrapper> {

  private static final Comparator<ServiceCompanyWrapper> comparator =
      new Comparator<ServiceCompanyWrapper>() {
        @Override
        public int compare(ServiceCompanyWrapper o1, ServiceCompanyWrapper o2) {
          return ComparisonChain.start()
              .compare(o1.getName(), o2.getName(), Collator.CASE_INSENSITIVE_NULLS_FIRST)
              .compare(o1.getId(), o2.getId())
              .result();
        }
      };

  private final long id;
  private final String name;

  ServiceCompanyWrapper(long id, String name) {
    this.id = id;
    this.name = BeeUtils.trim(name);
  }

  @Override
  public int compareTo(ServiceCompanyWrapper other) {
    return comparator.compare(this, other);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServiceCompanyWrapper) {
      return Objects.equals(id, ((ServiceCompanyWrapper) obj).id);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(id);
  }

  long getId() {
    return id;
  }

  String getName() {
    return name;
  }
}
