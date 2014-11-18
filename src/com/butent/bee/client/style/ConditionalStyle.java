package com.butent.bee.client.style;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gwt.core.client.JavaScriptObject;

import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enables using conditional CSS styles for elements of user interface.
 */

public final class ConditionalStyle {

  private static final class Entry {

    private static JavaScriptObject initInterpreter(StyleDescriptor sd) {
      if (sd == null || BeeUtils.isEmpty(STYLE_INTERPRETER_PREFIX)) {
        return null;
      }
      String s = sd.getInline();
      if (BeeUtils.isEmpty(s) || !s.startsWith(STYLE_INTERPRETER_PREFIX)) {
        return null;
      }

      String xpr = s.substring(STYLE_INTERPRETER_PREFIX.length());
      if (BeeUtils.isEmpty(xpr)) {
        return null;
      }
      return Evaluator.createFuncInterpreter("return " + xpr.trim());
    }

    private final StyleDescriptor styleDescriptor;

    private final Evaluator evaluator;
    private final JavaScriptObject styleInterpreter;

    private Entry(StyleDescriptor styleDescriptor, Evaluator evaluator) {
      this.styleDescriptor = styleDescriptor;
      this.evaluator = evaluator;
      this.styleInterpreter = initInterpreter(styleDescriptor);
    }

    private Evaluator getEvaluator() {
      return evaluator;
    }

    private StyleDescriptor getStyleDescriptor() {
      return styleDescriptor;
    }

    private JavaScriptObject getStyleInterpreter() {
      return styleInterpreter;
    }

    private boolean hasStyleInterpreter() {
      return getStyleInterpreter() != null;
    }

    private boolean hasStyleReplacement() {
      if (getStyleDescriptor() == null) {
        return false;
      }
      String s = getStyleDescriptor().getInline();
      if (BeeUtils.isEmpty(s)) {
        return false;
      }
      return s.contains(Evaluator.DEFAULT_REPLACE_PREFIX)
          && s.contains(Evaluator.DEFAULT_REPLACE_SUFFIX);
    }
  }

  private static final String STYLE_INTERPRETER_PREFIX = "=";

  private static final Map<String, StyleProvider> gridRowStyleProviders = new HashMap<>();
  private static final Table<String, String, StyleProvider> gridColumnStyleProviders =
      HashBasedTable.create();

  public static ConditionalStyle create(Collection<ConditionalStyleDeclaration> declarations,
      String colName, List<? extends IsColumn> dataColumns) {
    if (declarations == null || declarations.isEmpty()) {
      return null;
    }

    ConditionalStyle conditionalStyle = null;
    for (ConditionalStyleDeclaration csd : declarations) {
      if (csd == null || !csd.validState()) {
        continue;
      }
      if (conditionalStyle == null) {
        conditionalStyle = new ConditionalStyle(null);
      }

      Evaluator evaluator;
      if (csd.getCondition() == null) {
        evaluator = Evaluator.createEmpty(colName, dataColumns);
      } else {
        evaluator = Evaluator.create(csd.getCondition(), colName, dataColumns);
      }

      conditionalStyle.addStyle(StyleDescriptor.copyOf(csd.getStyle()), evaluator);
    }
    return conditionalStyle;
  }

  public static ConditionalStyle create(StyleProvider provider) {
    if (provider == null) {
      return null;
    } else {
      return new ConditionalStyle(provider);
    }
  }

  public static StyleProvider getGridColumnStyleProvider(String gridName, String columnName) {
    return gridColumnStyleProviders.get(gridName, columnName);
  }

  public static StyleProvider getGridRowStyleProvider(String gridName) {
    return gridRowStyleProviders.get(gridName);
  }

  public static void registerGridColumnStyleProvider(String gridName, String columnName,
      StyleProvider styleProvider) {
    Assert.notEmpty(gridName);
    Assert.notEmpty(columnName);
    Assert.notNull(styleProvider);

    gridColumnStyleProviders.put(gridName, columnName, styleProvider);
  }

  public static void registerGridRowStyleProvider(String gridName, StyleProvider styleProvider) {
    Assert.notEmpty(gridName);
    Assert.notNull(styleProvider);

    gridRowStyleProviders.put(gridName, styleProvider);
  }

  private final List<Entry> entries = new ArrayList<>();
  private final StyleProvider provider;

  private ConditionalStyle(StyleProvider provider) {
    this.provider = provider;
  }

  public Integer getExportStyleRef(IsRow row, XSheet sheet) {
    return (provider == null) ? null : provider.getExportStyleRef(row, sheet);
  }

  public StyleDescriptor getStyleDescriptor(IsRow rowValue, int rowIndex, int colIndex) {
    return getStyleDescriptor(rowValue, rowIndex, colIndex, false, null, null);
  }

  public StyleDescriptor getStyleDescriptor(IsRow rowValue, int rowIndex, int colIndex,
      ValueType cellType, String cellValue) {
    return getStyleDescriptor(rowValue, rowIndex, colIndex, true, cellType, cellValue);
  }

  private void addEntry(Entry entry) {
    if (entry != null) {
      entries.add(entry);
    }
  }

  private void addStyle(StyleDescriptor sd, Evaluator ev) {
    if (sd != null && ev != null) {
      addEntry(new Entry(sd, ev));
    }
  }

  private StyleDescriptor getStyleDescriptor(IsRow rowValue, int rowIndex, int colIndex,
      boolean updateCell, ValueType cellType, String cellValue) {

    if (rowValue == null) {
      return null;
    }

    if (provider != null) {
      return provider.getStyleDescriptor(rowValue);
    }

    for (Entry entry : entries) {
      if (updateCell && cellType != null) {
        entry.getEvaluator().update(rowValue, rowIndex, colIndex, cellType, cellValue);
      } else {
        entry.getEvaluator().update(rowValue, rowIndex, colIndex);
      }

      if (entry.getEvaluator().hasInterpreter()) {
        String z = entry.getEvaluator().evaluate();
        if (BeeUtils.isEmpty(z) || BeeConst.STRING_FALSE.equalsIgnoreCase(z)) {
          continue;
        }
      }

      if (!entry.hasStyleInterpreter() && !entry.hasStyleReplacement()) {
        return entry.getStyleDescriptor();
      }

      StyleDescriptor copy = StyleDescriptor.copyOf(entry.getStyleDescriptor());
      String replaced;
      if (entry.hasStyleInterpreter()) {
        replaced = entry.getEvaluator().evaluate(entry.getStyleInterpreter());
      } else {
        replaced = entry.getEvaluator().replace(copy.getInline());
      }
      copy.setInline(replaced);

      return copy;
    }
    return null;
  }
}
