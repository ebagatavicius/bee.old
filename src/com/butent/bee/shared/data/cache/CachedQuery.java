package com.butent.bee.shared.data.cache;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.List;

class CachedQuery extends SimpleCache<Integer, Long> {

  private final String strFilter;
  private final String strOrder;

  private int rowCount = BeeConst.UNDEF;

  CachedQuery(Filter filter, Order order, int maxSize, ReplacementPolicy replacementPolicy) {
    super(maxSize, replacementPolicy);
    this.strFilter = transformFilter(filter);
    this.strOrder = transformOrder(order);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CachedQuery)) {
      return false;
    }
    CachedQuery other = (CachedQuery) obj;
    return BeeUtils.same(this.strFilter, other.strFilter)
        && BeeUtils.same(this.strOrder, other.strOrder);
  }

  @Override
  public List<Property> getInfo() {
    List<Property> lst = Lists.newArrayList();

    if (!BeeUtils.isEmpty(strFilter)) {
      lst.add(new Property("Filter", strFilter));
    }
    if (!BeeUtils.isEmpty(strOrder)) {
      lst.add(new Property("Order", strOrder));
    }
    lst.add(new Property("Row Count", BeeUtils.toString(getRowCount())));

    lst.addAll(super.getInfo());
    return lst;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(strFilter, strOrder);
  }

  @Override
  protected synchronized boolean deleteKey(Integer key) {
    boolean ok = super.deleteKey(key);
    if (ok && getRowCount() > 0) {
      setRowCount(getRowCount() - 1);
    }
    return ok;
  }

  @Override
  protected synchronized int deleteValue(Long value) {
    int cnt = super.deleteValue(value);
    if (cnt > 0 && getRowCount() > 0) {
      setRowCount(Math.max(getRowCount() - cnt, 0));
    }
    return cnt;
  }

  void addRange(int offset, List<Long> idList) {
    Assert.nonNegative(offset);
    Assert.notNull(idList);

    for (int i = 0; i < idList.size(); i++) {
      add(offset + i, idList.get(i));
    }
  }

  boolean containsColumn(String columnId) {
    if (BeeUtils.isEmpty(columnId)) {
      return false;
    }
    return BeeUtils.containsSame(strFilter, columnId) || BeeUtils.containsSame(strOrder, columnId);
  }

  boolean containsRange(int start, int length) {
    if (start < 0 || length <= 0) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (!containsKey(start + i)) {
        return false;
      }
    }
    return true;
  }

  int getRowCount() {
    return rowCount;
  }

  List<Long> getRowIds(int start, int length) {
    Assert.nonNegative(start);
    Assert.isPositive(length);

    List<Long> result = null;
    Long rowId;

    for (int i = 0; i < length; i++) {
      rowId = get(start + i);
      if (rowId == null) {
        result = null;
        break;
      }
      if (result == null) {
        result = Lists.newArrayListWithCapacity(length);
      }
      result.add(rowId);
    }
    return result;
  }

  String getStrFilter() {
    return strFilter;
  }

  String getStrOrder() {
    return strOrder;
  }

  boolean same(Filter flt, Order ord) {
    return sameFilter(flt) && sameOrder(ord);
  }

  boolean sameFilter(Filter flt) {
    return BeeUtils.same(this.strFilter, transformFilter(flt));
  }

  boolean sameOrder(Order ord) {
    return BeeUtils.same(this.strOrder, transformOrder(ord));
  }

  void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }

  private static String transformFilter(Filter flt) {
    return (flt == null) ? BeeConst.STRING_EMPTY : flt.toString();
  }

  private static String transformOrder(Order ord) {
    return (ord == null) ? BeeConst.STRING_EMPTY : ord.toString();
  }
}
