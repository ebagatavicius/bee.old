package com.butent.bee.shared.data.view;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NullOrdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Implements sorting functionality in data objects.
 */

public class Order implements BeeSerializable {

  public static final class Column implements BeeSerializable {

    private String name;
    private final List<String> sources = new ArrayList<>();

    private boolean ascending;
    private NullOrdering nullOrdering;

    private Column(String name, List<String> sources, boolean ascending,
        NullOrdering nullOrdering) {

      this.name = name;
      this.sources.addAll(sources);
      this.ascending = ascending;
      this.nullOrdering = nullOrdering;
    }

    private Column() {
      super();
    }

    private Column(String name, String source) {
      this(name, source, true);
    }

    private Column(String name, String source, boolean ascending) {
      this(name, source, ascending, null);
    }

    private Column(String name, String source, boolean ascending, NullOrdering nullOrdering) {
      this(name, Lists.newArrayList(source), ascending, nullOrdering);
    }

    @Override
    public void deserialize(String s) {
      String[] arr = Codec.beeDeserializeCollection(s);
      Assert.minLength(ArrayUtils.length(arr), 4);

      int i = 0;
      name = arr[i++];
      setAscending(Codec.unpack(arr[i++]));
      setNullOrdering(Codec.unpack(NullOrdering.class, arr[i++]));

      if (!sources.isEmpty()) {
        sources.clear();
      }
      sources.addAll(Lists.newArrayList(ArrayUtils.slice(arr, i)));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;

      } else if (obj instanceof Column) {
        Column other = (Column) obj;
        return BeeUtils.same(getName(), other.getName())
            && isAscending() == other.isAscending()
            && getNullOrdering() == other.getNullOrdering();

      } else {
        return false;
      }
    }

    public String getName() {
      return name;
    }

    public NullOrdering getNullOrdering() {
      return nullOrdering;
    }

    public List<String> getSources() {
      return sources;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getName(), isAscending());
    }

    public boolean isAscending() {
      return ascending;
    }

    @Override
    public String serialize() {
      List<String> values = new ArrayList<>();

      values.add(getName());
      values.add(Codec.pack(isAscending()));
      values.add(Codec.pack(getNullOrdering()));

      values.addAll(getSources());

      return Codec.beeSerialize(values);
    }

    public void setAscending(boolean ascending) {
      this.ascending = ascending;
    }

    public void setNullOrdering(NullOrdering nullOrdering) {
      this.nullOrdering = nullOrdering;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getName());

      if (getSources().size() != 1 || !BeeUtils.same(getName(), getSources().get(0))) {
        sb.append(BeeConst.CHAR_EQ).append(getSources());
      }

      if (!isAscending()) {
        sb.append(BeeConst.CHAR_SPACE).append(SORT_DESCENDING);
      }
      if (getNullOrdering() != null) {
        sb.append(BeeConst.CHAR_SPACE).append(getNullOrdering().name());
      }

      return sb.toString();
    }

    private boolean is(String id) {
      return BeeUtils.same(name, id);
    }
  }

  public static final String SORT_ASCENDING = "ascending";
  public static final String SORT_DESCENDING = "descending";

  private static BeeLogger logger = LogUtils.getLogger(Order.class);

  public static Order ascending(String source) {
    return new Order(source, true);
  }

  public static Order ascending(String first, String second) {
    Order order = new Order();

    order.add(first, true);
    order.add(second, true);

    return order;
  }

  public static Order ascending(String first, String second, String third) {
    Order order = ascending(first, second);
    order.add(third, true);

    return order;
  }

  private static final Splitter ITEM_SPLITTER =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();
  private static final Splitter FIELD_SPLITTER =
      Splitter.on(BeeConst.CHAR_SPACE).omitEmptyStrings().trimResults();

  public static boolean isNullsFirst(String s) {
    return BeeUtils.same(s, NullOrdering.NULLS_FIRST.name());
  }

  public static boolean isNullsLast(String s) {
    return BeeUtils.same(s, NullOrdering.NULLS_LAST.name());
  }

  public static boolean isSortAscending(String s) {
    return BeeUtils.inListSame(s, SORT_ASCENDING, BeeConst.STRING_PLUS)
        || BeeUtils.isPrefix(SORT_ASCENDING, s);
  }

  public static boolean isSortDescending(String s) {
    return BeeUtils.inListSame(s, SORT_DESCENDING, BeeConst.STRING_MINUS)
        || BeeUtils.isPrefix(SORT_DESCENDING, s);
  }

  public static Order parse(String input, Collection<String> colNames) {
    Assert.notEmpty(input);
    Assert.notEmpty(colNames);

    Order order = new Order();

    String name;
    boolean asc;
    NullOrdering nulls;

    for (String item : ITEM_SPLITTER.split(input)) {
      asc = true;
      nulls = null;

      if (item.indexOf(BeeConst.CHAR_SPACE) > 0) {
        name = null;

        for (String v : FIELD_SPLITTER.split(item)) {
          if (isSortAscending(v)) {
            asc = true;
          } else if (isSortDescending(v)) {
            asc = false;

          } else if (isNullsFirst(v)) {
            nulls = NullOrdering.NULLS_FIRST;
          } else if (isNullsLast(v)) {
            nulls = NullOrdering.NULLS_LAST;

          } else {
            name = v;
          }
        }

      } else if (BeeUtils.isPrefixOrSuffix(item, BeeConst.CHAR_PLUS)) {
        name = BeeUtils.removePrefixAndSuffix(item, BeeConst.CHAR_PLUS);

      } else if (BeeUtils.isPrefixOrSuffix(item, BeeConst.CHAR_MINUS)) {
        name = BeeUtils.removePrefixAndSuffix(item, BeeConst.CHAR_MINUS);
        asc = false;

      } else {
        name = item;
      }

      String colName = BeeUtils.getSame(colNames, name);
      if (BeeUtils.isEmpty(colName)) {
        logger.warning("cannot parse order item: " + item);
        continue;
      }

      order.add(colName, asc, nulls);
    }

    if (order.isEmpty()) {
      return null;
    }
    return order;
  }

  public static Order restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Order order = new Order();
    order.deserialize(s);
    return order;
  }

  private final List<Column> columns = new ArrayList<>();

  public Order() {
    super();
  }

  public Order(String name, boolean ascending) {
    this();
    add(name, ascending);
  }

  public void add(String name, boolean ascending) {
    add(name, ascending, null);
  }

  public void add(String name, boolean ascending, NullOrdering nullOrdering) {
    add(name, Lists.newArrayList(name), ascending, nullOrdering);
  }

  public void add(String name, List<String> sources, boolean ascending) {
    add(name, sources, ascending, null);
  }

  public void add(String name, List<String> sources, boolean ascending, NullOrdering nullOrdering) {
    Assert.notEmpty(name);

    Column found = find(name);
    if (found != null) {
      columns.remove(found);
    }
    columns.add(new Column(name.trim(), sources, ascending, nullOrdering));
  }

  public void clear() {
    columns.clear();
  }

  @Override
  public void deserialize(String s) {
    if (!columns.isEmpty()) {
      columns.clear();
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    if (ArrayUtils.isEmpty(arr)) {
      return;
    }

    for (String cs : arr) {
      Column column = new Column();
      column.deserialize(cs);

      columns.add(column);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Order)) {
      return false;
    }
    return columns.equals(((Order) obj).columns);
  }

  public List<Column> getColumns() {
    return columns;
  }

  public int getIndex(String name) {
    Assert.notNull(name);
    for (int i = 0; i < getSize(); i++) {
      if (columns.get(i).is(name)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  public NullOrdering getNullOrdering(String name) {
    Assert.notNull(name);

    for (Column col : columns) {
      if (col.is(name)) {
        return col.getNullOrdering();
      }
    }
    return null;
  }

  public int getSize() {
    return columns.size();
  }

  @Override
  public int hashCode() {
    return columns.hashCode();
  }

  public boolean isAscending(String name) {
    Assert.notNull(name);

    for (Column col : columns) {
      if (col.is(name)) {
        return col.isAscending();
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return columns.isEmpty();
  }

  public boolean remove(String name) {
    Assert.notEmpty(name);

    Column found = find(name);
    if (found != null) {
      columns.remove(found);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(columns);
  }

  public void setAscending(String name, boolean ascending) {
    Column col = find(name);
    Assert.notNull(col);

    col.setAscending(ascending);
  }

  public void setNullOrdering(String name, NullOrdering nullOrdering) {
    Column col = find(name);
    Assert.notNull(col);

    col.setNullOrdering(nullOrdering);
  }

  @Override
  public String toString() {
    return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, getColumns());
  }

  private Column find(String name) {
    if (BeeUtils.isEmpty(name)) {
      return null;
    }
    for (Column col : columns) {
      if (col.is(name)) {
        return col;
      }
    }
    return null;
  }
}
