package com.butent.bee.shared.data.filter;

import com.google.common.collect.ImmutableList;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements complex filters, containing more than one condition.
 */

public class CompoundFilter extends Filter {

  /**
   * Contains a list of filter parts which go through serialization.
   */
  private enum Serial {
    JOINTYPE, SUBFILTERS
  }

  private CompoundType type;
  private final List<Filter> subFilters = new ArrayList<>();

  protected CompoundFilter() {
    super();
  }

  protected CompoundFilter(CompoundType joinType, Filter... filters) {
    this.type = joinType;
    if (filters != null) {
      add(filters);
    }
  }

  public CompoundFilter add(Filter... filters) {
    if (!isEmpty() && type == CompoundType.NOT) {
      Assert.unsupported();
    }
    if (filters != null) {
      for (Filter filter : filters) {
        if (filter != null) {
          subFilters.add(filter);
        }
      }
    }
    return this;
  }

  @Override
  public void deserialize(String s) {
    setSafe();
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case JOINTYPE:
          type = CompoundType.valueOf(value);
          break;
        case SUBFILTERS:
          String[] filters = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(filters)) {
            for (String flt : filters) {
              add(Filter.restore(flt));
            }
          }
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CompoundFilter other = (CompoundFilter) obj;

    if (!type.equals(other.type)) {
      return false;
    }
    if (!subFilters.equals(other.subFilters)) {
      return false;
    }
    return true;
  }

  public List<Filter> getSubFilters() {
    return ImmutableList.copyOf(subFilters);
  }

  public CompoundType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + type.hashCode();
    result = prime * result + subFilters.hashCode();
    return result;
  }

  @Override
  public boolean involvesColumn(String colName) {
    if (!isEmpty()) {
      for (Filter subFilter : subFilters) {
        if (subFilter.involvesColumn(colName)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(subFilters);
  }

  @Override
  public boolean isMatch(List<? extends IsColumn> columns, IsRow row) {
    if (!isEmpty()) {
      for (Filter subFilter : subFilters) {
        boolean result = subFilter.isMatch(columns, row);

        if (type == CompoundType.NOT) {
          return !result;
        } else if ((type == CompoundType.AND && !result) || (type == CompoundType.OR && result)) {
          return result;
        }
      }
      return type == CompoundType.AND;
    }
    return true;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case JOINTYPE:
          arr[i++] = type;
          break;
        case SUBFILTERS:
          arr[i++] = subFilters;
          break;
      }
    }
    return super.serialize(arr);
  }

  public int size() {
    return subFilters.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (Filter subFilter : subFilters) {
      String expr = subFilter.toString();

      if (!BeeUtils.isEmpty(expr) && sb.length() > 0) {
        sb.append(type.toTextString());
      }
      sb.append(expr);
    }
    String filter = sb.toString();

    if (!BeeUtils.isEmpty(filter) && type != CompoundType.AND) {
      filter = BeeUtils.parenthesize(filter);

      if (type == CompoundType.NOT) {
        filter = CompoundType.NOT.toTextString() + filter;
      }
    }
    return filter;
  }
}
