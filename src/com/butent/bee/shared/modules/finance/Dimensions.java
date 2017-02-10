package com.butent.bee.shared.modules.finance;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Dimensions {

  public static final int SPACETIME = 10;

  public static final String TBL_NAMES = "DimensionNames";
  public static final String VIEW_NAMES = "DimensionNames";

  public static final String TBL_EXTRA_DIMENSIONS = "ExtraDimensions";

  public static final String COL_ORDINAL = "Ordinal";
  public static final String COL_PLURAL_NAME = "PluralName";
  public static final String COL_SINGULAR_NAME = "SingularName";

  public static final String COL_EXTRA_DIMENSIONS = "ExtraDimensions";

  public static final String GRID_NAMES = "DimensionNames";

  public static final String PRM_DIMENSIONS = "Dimensions";

  public static final String STYLE_SUMMARY = BeeConst.CSS_CLASS_PREFIX + "Dimensions-summary";

  private static final BeeLogger logger = LogUtils.getLogger(Dimensions.class);

  private static final String[] VIEWS = new String[SPACETIME];
  private static final String[] RELATION_COLUMNS = new String[SPACETIME];

  private static final Map<Integer, String> pluralNames = new HashMap<>();
  private static final Map<Integer, String> singularNames = new HashMap<>();

  private static int observed;

  static {
    for (int i = 0; i < SPACETIME; i++) {
      VIEWS[i] = "Dimensions" + BeeUtils.toLeadingZeroes(i + 1, 2);
      RELATION_COLUMNS[i] = getColumnPrefix(i + 1) + "Rel";
    }
  }

  public static String plural(Integer ordinal) {
    if (isValid(ordinal)) {
      String name = pluralNames.get(ordinal);
      return BeeUtils.isEmpty(name) ? Localized.dictionary().dimensionNameDefault(ordinal) : name;

    } else {
      return null;
    }
  }

  public static String singular(Integer ordinal) {
    if (isValid(ordinal)) {
      String name = singularNames.get(ordinal);
      return BeeUtils.isEmpty(name) ? Localized.dictionary().dimensionNameDefault(ordinal) : name;

    } else {
      return null;
    }
  }

  public static String getTableName(Integer ordinal) {
    return getViewName(ordinal);
  }

  public static String getViewName(Integer ordinal) {
    return isValid(ordinal) ? VIEWS[ordinal - 1] : null;
  }

  public static Integer getViewOrdinal(String viewName) {
    int index = ArrayUtils.indexOf(VIEWS, viewName);
    if (index >= 0) {
      return index + 1;
    } else {
      return null;
    }
  }

  public static boolean isDimensionView(String viewName) {
    return ArrayUtils.contains(VIEWS, viewName);
  }

  public static boolean isObserved(Integer ordinal) {
    return isValid(ordinal) && ordinal <= observed;
  }

  public static String getGridName(Integer ordinal) {
    return getViewName(ordinal);
  }

  public static String getNameColumn(Integer ordinal) {
    return isValid(ordinal) ? getColumnPrefix(ordinal) + "Name" : null;
  }

  public static String getRelationColumn(Integer ordinal) {
    return isValid(ordinal) ? RELATION_COLUMNS[ordinal - 1] : null;
  }

  public static Integer getRelationColumnOrdinal(String columnName) {
    int index = ArrayUtils.indexOf(RELATION_COLUMNS, columnName);
    if (index >= 0) {
      return index + 1;
    } else {
      return null;
    }
  }

  public static String getForegroundColumn(Integer ordinal) {
    return isValid(ordinal) ? getColumnPrefix(ordinal) + "Foreground" : null;
  }

  public static String getBackgroundColumn(Integer ordinal) {
    return isValid(ordinal) ? getColumnPrefix(ordinal) + "Background" : null;
  }

  public static Collection<String> getObservedRelationColumns() {
    Set<String> result = new HashSet<>();
    for (int ordinal = 1; ordinal < getObserved(); ordinal++) {
      result.add(getRelationColumn(ordinal));
    }
    return result;
  }

  public static Collection<String> getHiddenRelationColumns() {
    Set<String> result = new HashSet<>();
    for (int ordinal = observed + 1; ordinal <= SPACETIME; ordinal++) {
      result.add(getRelationColumn(ordinal));
    }
    return result;
  }

  public static String menuParameter(Integer ordinal) {
    return isValid(ordinal) ? BeeUtils.toString(ordinal) : null;
  }

  public static void load(String serialized) {
    BeeRowSet rowSet = BeeRowSet.restore(serialized);

    setObserved(BeeUtils.toIntOrNull(rowSet.getTableProperty(PRM_DIMENSIONS)));
    loadNames(rowSet, Localized.dictionary().languageTag());

    logger.info("dimensions", observed);
  }

  public static synchronized void loadNames(BeeRowSet rowSet, String language) {
    if (!DataUtils.isEmpty(rowSet)) {
      int ordinalIndex = rowSet.getColumnIndex(COL_ORDINAL);

      for (BeeRow row : rowSet) {
        Integer ordinal = row.getInteger(ordinalIndex);

        setPlural(ordinal, DataUtils.getTranslation(rowSet, row, COL_PLURAL_NAME, language));
        setSingular(ordinal, DataUtils.getTranslation(rowSet, row, COL_SINGULAR_NAME, language));
      }
    }
  }

  public static int getObserved() {
    return observed;
  }

  public static synchronized void setObserved(Integer count) {
    if (count != null) {
      Dimensions.observed = BeeUtils.clamp(count, 0, SPACETIME);
    }
  }

  public static void setPlural(Integer ordinal, String name) {
    if (isValid(ordinal) && !BeeUtils.isEmpty(name)) {
      pluralNames.put(ordinal, name.trim());
    }
  }

  public static void setSingular(Integer ordinal, String name) {
    if (isValid(ordinal) && !BeeUtils.isEmpty(name)) {
      singularNames.put(ordinal, name.trim());
      Localized.setColumnLabel(RELATION_COLUMNS[ordinal - 1], name.trim());
    }
  }

  public static Dimensions create(BeeRowSet rowSet, IsRow row) {
    Assert.notNull(rowSet);
    return create(rowSet.getColumns(), row);
  }

  public static Dimensions create(List<? extends IsColumn> columns, IsRow row) {
    Assert.notEmpty(columns);
    Assert.notNull(row);

    Long[] values = new Long[observed];

    for (int i = 0; i < values.length; i++) {
      values[i] = DataUtils.getLong(columns, row, RELATION_COLUMNS[i]);
    }

    return new Dimensions(values);
  }

  public static Dimensions merge(List<Dimensions> list) {
    Assert.notEmpty(list);

    Long[] values = new Long[observed];

    for (Dimensions dim : list) {
      if (dim != null && !dim.isEmpty()) {
        for (int i = 0; i < Math.min(values.length, dim.values.length); i++) {
          if (values[i] == null) {
            values[i] = dim.values[i];
          }
        }
      }
    }

    return new Dimensions(values);
  }

  private static String getColumnPrefix(int ordinal) {
    return "Dim" + BeeUtils.toLeadingZeroes(ordinal, 2);
  }

  private static boolean isValid(Integer ordinal) {
    return ordinal != null && ordinal >= 1 && ordinal <= SPACETIME;
  }

  private final Long[] values;

  private Dimensions(Long[] values) {
    this.values = values;
  }

  public void applyTo(List<? extends IsColumn> columns, IsRow row) {
    Assert.notEmpty(columns);
    Assert.notNull(row);

    for (int i = 0; i < values.length; i++) {
      if (values[i] != null) {
        row.setValue(DataUtils.getColumnIndex(RELATION_COLUMNS[i], columns), values[i]);
      }
    }
  }

  public boolean isEmpty() {
    for (Long value : values) {
      if (value != null) {
        return false;
      }
    }
    return true;
  }

  public Filter getFilter() {
    CompoundFilter filter = Filter.and();
    for (int i = 0; i < values.length; i++) {
      filter.add(Filter.equalsOrIsNull(RELATION_COLUMNS[i], values[i]));
    }
    return filter;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return BeeConst.EMPTY;

    } else {
      List<String> items = new ArrayList<>();
      for (int i = 0; i < values.length; i++) {
        if (values[i] != null) {
          items.add(NameUtils.addName(RELATION_COLUMNS[i], BeeUtils.toString(values[i])));
        }
      }
      return BeeUtils.joinItems(items);
    }
  }
}
