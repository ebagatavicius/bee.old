package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NullOrdering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RowOrdering<R extends IsRow> implements Comparator<R> {

  public static final NullOrdering NULL_ORDERING = NullOrdering.DEFAULT;

  private final List<Integer> indexes = new ArrayList<>();
  private final List<Boolean> ascending = new ArrayList<>();
  private final List<ValueType> types = new ArrayList<>();

  private final Comparator<String> collator;

  public RowOrdering(List<? extends IsColumn> columns, List<Pair<Integer, Boolean>> sortInfo,
      Comparator<String> collator) {

    Assert.notEmpty(columns);
    Assert.notNull(sortInfo);
    Assert.isTrue(sortInfo.size() >= 1);

    ValueType type;

    for (int i = 0; i < sortInfo.size(); i++) {
      Assert.notNull(sortInfo.get(i).getA());
      int index = sortInfo.get(i).getA();
      if (containsIndex(index)) {
        continue;
      }

      switch (index) {
        case DataUtils.ID_INDEX:
          type = DataUtils.ID_TYPE;
          break;

        case DataUtils.VERSION_INDEX:
          type = DataUtils.VERSION_TYPE;
          break;

        default:
          Assert.isIndex(columns, index);
          type = columns.get(index).getType();
      }

      indexes.add(index);
      ascending.add(BeeUtils.unbox(sortInfo.get(i).getB()));
      types.add(type);
    }

    this.collator = collator;
  }

  @Override
  public int compare(R row1, R row2) {
    if (row1 == row2) {
      return BeeConst.COMPARE_EQUAL;
    }
    if (row1 == null) {
      return ascending.get(0) ? BeeConst.COMPARE_LESS : BeeConst.COMPARE_MORE;
    }
    if (row2 == null) {
      return ascending.get(0) ? BeeConst.COMPARE_MORE : BeeConst.COMPARE_LESS;
    }

    int z;
    for (int i = 0; i < indexes.size(); i++) {
      int index = indexes.get(i);

      switch (index) {
        case DataUtils.ID_INDEX:
          z = BeeUtils.compare(row1.getId(), row2.getId(), NULL_ORDERING);
          break;
        case DataUtils.VERSION_INDEX:
          z = BeeUtils.compare(row1.getVersion(), row2.getVersion(), NULL_ORDERING);
          break;

        default:
          switch (types.get(i)) {
            case BOOLEAN:
              z = BeeUtils.compare(row1.getBoolean(index), row2.getBoolean(index), NULL_ORDERING);
              break;
            case DATE:
              z = BeeUtils.compare(row1.getDate(index), row2.getDate(index), NULL_ORDERING);
              break;
            case DATE_TIME:
              z = BeeUtils.compare(row1.getDateTime(index), row2.getDateTime(index), NULL_ORDERING);
              break;
            case NUMBER:
              z = BeeUtils.compare(row1.getDouble(index), row2.getDouble(index), NULL_ORDERING);
              break;
            case INTEGER:
              z = BeeUtils.compare(row1.getInteger(index), row2.getInteger(index), NULL_ORDERING);
              break;
            case LONG:
              z = BeeUtils.compare(row1.getLong(index), row2.getLong(index), NULL_ORDERING);
              break;
            case DECIMAL:
              z = BeeUtils.compare(row1.getDecimal(index), row2.getDecimal(index), NULL_ORDERING);
              break;

            default:
              if (collator == null) {
                z = BeeUtils.compare(row1.getString(index), row2.getString(index), NULL_ORDERING);
              } else {
                z = collator.compare(row1.getString(index), row2.getString(index));
              }
          }
      }

      if (z != BeeConst.COMPARE_EQUAL) {
        return ascending.get(i) ? z : -z;
      }
    }
    return BeeConst.COMPARE_EQUAL;
  }

  private boolean containsIndex(int index) {
    return indexes.contains(index);
  }
}
