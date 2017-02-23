package com.butent.bee.client.modules.finance.analysis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.finance.FinanceKeeper;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.analysis.AnalysisCellType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisLabel;
import com.butent.bee.shared.modules.finance.analysis.AnalysisResults;
import com.butent.bee.shared.modules.finance.analysis.AnalysisSplitType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisSplitValue;
import com.butent.bee.shared.modules.finance.analysis.AnalysisValue;
import com.butent.bee.shared.modules.finance.analysis.AnalysisValueType;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class AnalysisViewer extends Flow implements HasCaption, HandlesActions, Printable {

  private static final class SplitTree implements Comparable<SplitTree> {

    private final int valueIndex;

    private final List<SplitTree> children = new ArrayList<>();

    private SplitTree(int valueIndex) {
      this.valueIndex = valueIndex;
    }

    private SplitTree addChild(int index) {
      for (SplitTree child : children) {
        if (child.valueIndex == index) {
          return child;
        }
      }

      SplitTree child = new SplitTree(index);
      children.add(child);

      return child;
    }

    @Override
    public int compareTo(SplitTree o) {
      return Integer.compare(valueIndex, o.valueIndex);
    }

    private int size() {
      if (children.isEmpty()) {
        return 1;
      } else {
        return children.stream().mapToInt(SplitTree::size).sum();
      }
    }

    private void sort() {
      if (!children.isEmpty()) {
        if (children.size() > 1) {
          children.sort(null);
        }

        children.forEach(SplitTree::sort);
      }
    }

    @Override
    public String toString() {
      if (children.isEmpty()) {
        return Integer.toString(valueIndex);
      } else {
        return BeeUtils.joinWords(valueIndex, children);
      }
    }

    private void collect(int depth, ListMultimap<Integer, SplitTree> collector) {
      collector.put(depth, this);

      if (!children.isEmpty()) {
        for (SplitTree child : children) {
          child.collect(depth + 1, collector);
        }
      }
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(AnalysisViewer.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "fin-AnalysisViewer-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_BODY = STYLE_PREFIX + "body";

  private static final String STYLE_CAPTION = STYLE_PREFIX + "caption";

  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_COLUMN = STYLE_PREFIX + "column";
  private static final String STYLE_COLUMN_UNDEF = STYLE_COLUMN + "-undef";
  private static final String STYLE_ROW = STYLE_PREFIX + "row";

  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_LABEL_PREFIX = STYLE_LABEL + "-";
  private static final String STYLE_LABEL_UNDEF = STYLE_LABEL_PREFIX + "undef";

  private static final String STYLE_SPLIT = STYLE_PREFIX + "split";
  private static final String STYLE_SPLIT_PREFIX = STYLE_SPLIT + "-";
  private static final String STYLE_SPLIT_EMPTY = STYLE_SPLIT_PREFIX + "empty";
  private static final String STYLE_SPLIT_UNDEF = STYLE_SPLIT_PREFIX + "undef";

  private static final String STYLE_TYPE = STYLE_PREFIX + "type";
  private static final String STYLE_TYPE_PREFIX = STYLE_TYPE + "-";
  private static final String STYLE_TYPE_UNDEF = STYLE_TYPE_PREFIX + "undef";

  private static final String STYLE_VALUE = STYLE_PREFIX + "value";
  private static final String STYLE_VALUE_PREFIX = STYLE_VALUE + "-";
  private static final String STYLE_VALUE_EMPTY = STYLE_VALUE_PREFIX + "empty";

  private static final String STYLE_PERFORMANCE = STYLE_PREFIX + "performance";
  private static final String STYLE_STATS = STYLE_PREFIX + "stats";
  private static final String STYLE_TIME = STYLE_PREFIX + "time";
  private static final String STYLE_DURATION = STYLE_PREFIX + "duration";

  private static final String PERIOD_SEPARATOR = " - ";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private final AnalysisResults results;

  private final List<Long> columnIds = new ArrayList<>();
  private final Map<Long, List<AnalysisLabel>> columnLabels = new HashMap<>();

  private final Map<Long, List<AnalysisSplitType>> columnSplitTypes = new HashMap<>();
  private final Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> columnSplitValues =
      new HashMap<>();
  private final Map<Long, List<SplitTree>> columnSplitTree = new HashMap<>();

  private final Map<Long, List<AnalysisCellType>> columnCellTypes = new HashMap<>();

  private final Map<Long, Integer> columnSpan = new HashMap<>();

  private final List<Long> rowIds = new ArrayList<>();
  private final Map<Long, List<AnalysisLabel>> rowLabels = new HashMap<>();

  private final Map<Long, List<AnalysisSplitType>> rowSplitTypes = new HashMap<>();
  private final Map<Long, Map<AnalysisSplitType, List<AnalysisSplitValue>>> rowSplitValues =
      new HashMap<>();
  private final Map<Long, List<SplitTree>> rowSplitTree = new HashMap<>();

  private final Map<Long, List<AnalysisCellType>> rowCellTypes = new HashMap<>();

  private final Map<Long, Integer> rowSpan = new HashMap<>();

  private final Table<Long, Long, List<AnalysisValue>> values = HashBasedTable.create();

  AnalysisViewer(AnalysisResults results, Set<Action> enabledActions) {
    super(STYLE_CONTAINER);

    this.results = results;

    HeaderView header = new HeaderImpl();
    header.create(results.getHeaderString(COL_ANALYSIS_NAME), false, true, null, uiOptions,
        enabledActions, Action.NO_ACTIONS, Action.NO_ACTIONS);

    header.setActionHandler(this);
    add(header);

    layout();

    Flow body = new Flow(STYLE_BODY);
    StyleUtils.setTop(body, header.getHeight());

    render(body);
    add(body);
  }

  @Override
  public String getCaption() {
    HeaderView header = getHeader();
    return (header == null) ? results.getHeaderString(COL_ANALYSIS_NAME) : header.getCaption();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      case PRINT:
        Printer.print(this);
        break;

      case SAVE:
        onSave();
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    return !StyleUtils.hasClassName(source, STYLE_PERFORMANCE)
        && !StyleUtils.hasClassName(source, STYLE_STATS);
  }

  public HeaderView getHeader() {
    for (Widget widget : getChildren()) {
      if (widget instanceof HeaderView) {
        return (HeaderView) widget;
      }
    }
    return null;
  }

  private void onSave() {
    List<String> labels = results.getHeaderLabels(formatRange(results.getHeaderRange()), false)
        .stream()
        .map(AnalysisLabel::getText).collect(Collectors.toList());

    String caption = BeeUtils.isEmpty(labels) ? getCaption() : BeeUtils.joinItems(labels);
    int maxLength = Data.getColumnPrecision(VIEW_ANALYSIS_RESULTS, COL_ANALYSIS_RESULT_CAPTION);

    Global.inputString(Localized.dictionary().actionSave(),
        Data.getColumnLabel(VIEW_ANALYSIS_RESULTS, COL_ANALYSIS_RESULT_CAPTION),
        new StringCallback() {
          @Override
          public void onSuccess(String value) {
            doSave(value);
          }
        }, null, caption, maxLength, null, 30, CssUnit.REM);
  }

  private void doSave(String caption) {
    HeaderView header = getHeader();
    if (header != null) {
      header.showAction(Action.SAVE, false);
    }

    ParameterList parameters = FinanceKeeper.createArgs(SVC_SAVE_ANALYSIS_RESULTS);
    parameters.addQueryItem(COL_ANALYSIS_HEADER, results.getHeaderId());
    parameters.addQueryItem(COL_ANALYSIS_RESULT_DATE, results.getComputeEnd());

    parameters.addDataItem(COL_ANALYSIS_RESULT_CAPTION, caption);
    parameters.addDataItem(COL_ANALYSIS_RESULTS, results.serialize());

    BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ANALYSIS_RESULTS);
      }
    });
  }

  private void layout() {
    for (BeeRow column : results.getColumns()) {
      if (results.isColumnVisible(column)) {
        long id = column.getId();
        columnIds.add(id);

        String period = formatRange(results.getColumnRange(column));
        columnLabels.put(id, results.getColumnLabels(column, period));

        List<AnalysisSplitType> st = results.getColumnSplitTypes(id);
        if (!BeeUtils.isEmpty(st)) {
          columnSplitTypes.put(id, st);
        }

        Map<AnalysisSplitType, List<AnalysisSplitValue>> sv = results.getColumnSplitValues(id);
        if (!BeeUtils.isEmpty(sv)) {
          columnSplitValues.put(id, sv);
        }

        List<AnalysisCellType> cellTypes = results.getColumnCellTypes(column);
        columnCellTypes.put(id, cellTypes);
      }
    }

    logger.debug("columns", columnIds);
    logger.debug("c labels", columnLabels);
    logger.debug("c split types", columnSplitTypes);
    logger.debug("c split values", columnSplitValues);
    logger.debug("c cell types", columnCellTypes);
    logger.addSeparator();

    for (BeeRow row : results.getRows()) {
      if (results.isRowVisible(row)) {
        long id = row.getId();
        rowIds.add(id);

        String period = formatRange(results.getRowRange(row));
        rowLabels.put(id, results.getRowLabels(row, period));

        List<AnalysisSplitType> st = results.getRowSplitTypes(id);
        if (!BeeUtils.isEmpty(st)) {
          rowSplitTypes.put(id, st);
        }

        Map<AnalysisSplitType, List<AnalysisSplitValue>> sv = results.getRowSplitValues(id);
        if (!BeeUtils.isEmpty(sv)) {
          rowSplitValues.put(id, sv);
        }

        List<AnalysisCellType> cellTypes = results.getRowCellTypes(row);
        this.rowCellTypes.put(id, cellTypes);
      }
    }

    logger.debug("rows", rowIds);
    logger.debug("r labels", rowLabels);
    logger.debug("r split types", rowSplitTypes);
    logger.debug("r split values", rowSplitValues);
    logger.debug("r cell types", rowCellTypes);
    logger.addSeparator();

    for (AnalysisValue value : results.getValues()) {
      long rowId = value.getRowId();
      long columnId = value.getColumnId();

      if (rowIds.contains(rowId) && columnIds.contains(columnId)) {
        if (values.contains(rowId, columnId)) {
          values.get(rowId, columnId).add(value);

        } else {
          List<AnalysisValue> list = new ArrayList<>();
          list.add(value);
          values.put(rowId, columnId, list);
        }

        if (rowSplitTypes.containsKey(rowId) && !value.getRowSplit().isEmpty()) {
          buildSplitTree(rowId, false, value.getRowSplit());
        }
        if (columnSplitTypes.containsKey(columnId) && !value.getColumnSplit().isEmpty()) {
          buildSplitTree(columnId, true, value.getColumnSplit());
        }
      }
    }

    if (!columnSplitTree.isEmpty()) {
      columnSplitTree.forEach((id, list) -> {
        sort(list);
        logger.debug("cst", id, list);
      });
    }

    if (!rowSplitTree.isEmpty()) {
      rowSplitTree.forEach((id, list) -> {
        sort(list);
        logger.debug("rst", id, list);
      });
    }

    for (long id : columnIds) {
      columnSpan.put(id, getSpan(columnSplitTree.get(id), columnCellTypes.get(id)));
    }
    logger.debug("c span", columnSpan);

    for (long id : rowIds) {
      rowSpan.put(id, getSpan(rowSplitTree.get(id), rowCellTypes.get(id)));
    }
    logger.debug("r span", rowSpan);
    logger.addSeparator();

    logger.debug("values", values.size(),
        values.values().stream().mapToInt(List::size).sum());
    values.values().forEach(list -> list.forEach(value -> logger.debug(value)));
    logger.addSeparator();
  }

  private static void sort(List<SplitTree> list) {
    if (list.size() > 1) {
      list.sort(null);
    }

    list.forEach(SplitTree::sort);
  }

  private void buildSplitTree(long id, boolean isColumn,
      Map<AnalysisSplitType, AnalysisSplitValue> split) {

    List<AnalysisSplitType> splitTypes =
        isColumn ? columnSplitTypes.get(id) : rowSplitTypes.get(id);

    Map<AnalysisSplitType, List<AnalysisSplitValue>> splitValuesByType =
        isColumn ? columnSplitValues.get(id) : rowSplitValues.get(id);

    SplitTree splitTree = null;

    if (!BeeUtils.isEmpty(splitTypes) && !BeeUtils.isEmpty(splitValuesByType)) {
      for (int typeIndex = 0; typeIndex < splitTypes.size(); typeIndex++) {
        AnalysisSplitType splitType = splitTypes.get(typeIndex);
        AnalysisSplitValue splitValue = split.get(splitType);

        int valueIndex = BeeUtils.indexOf(splitValuesByType.get(splitType), splitValue);

        if (!BeeConst.isUndef(valueIndex)) {
          if (typeIndex == 0) {
            splitTree = null;
            List<SplitTree> trees = isColumn ? columnSplitTree.get(id) : rowSplitTree.get(id);

            if (!BeeUtils.isEmpty(trees)) {
              for (SplitTree tree : trees) {
                if (tree.valueIndex == valueIndex) {
                  splitTree = tree;
                  break;
                }
              }
            }

            if (splitTree == null) {
              splitTree = new SplitTree(valueIndex);

              if (BeeUtils.isEmpty(trees)) {
                List<SplitTree> list = new ArrayList<>();
                list.add(splitTree);

                if (isColumn) {
                  columnSplitTree.put(id, list);
                } else {
                  rowSplitTree.put(id, list);
                }

              } else {
                trees.add(splitTree);
              }
            }

          } else {
            splitTree = splitTree.addChild(valueIndex);
          }
        }
      }
    }
  }

  private static int getSpan(Collection<SplitTree> splitTrees, List<AnalysisCellType> cellTypes) {
    int span;

    if (BeeUtils.isEmpty(splitTrees)) {
      span = 1;

    } else {
      span = splitTrees.stream().mapToInt(SplitTree::size).sum();
      if (span <= 0) {
        span = 1;
      }
    }

    if (!BeeUtils.isEmpty(cellTypes)) {
      span *= cellTypes.size();
    }

    return span;
  }

  public static String formatRange(MonthRange range) {
    if (range == null) {
      return BeeConst.STRING_EMPTY;
    }

    YearMonth minYm = range.getMinMonth();
    YearMonth maxYm = range.getMaxMonth();

    if (minYm.equals(maxYm)) {
      return Format.renderYearMonth(minYm);

    } else if (minYm.getYear() == maxYm.getYear()) {
      if (minYm.getMonth() == 1 && maxYm.getMonth() == 12) {
        return BeeUtils.toString(minYm.getYear());

      } else if (minYm.getMonth() % 3 == 1 && maxYm.getMonth() == minYm.getMonth() + 2) {
        return BeeUtils.joinWords(minYm.getYear(), Format.quarterFull(minYm.getQuarter()));

      } else {
        return BeeUtils.join(PERIOD_SEPARATOR,
            Format.renderYearMonth(minYm), Format.renderMonthFullStandalone(maxYm));
      }

    } else {
      String lower = BeeUtils.isMore(minYm, ANALYSIS_MIN_YEAR_MONTH)
          ? Format.renderYearMonth(minYm) : BeeConst.STRING_EMPTY;
      String upper = BeeUtils.isLess(maxYm, ANALYSIS_MAX_YEAR_MONTH)
          ? Format.renderYearMonth(maxYm) : BeeConst.STRING_EMPTY;

      if (BeeUtils.allEmpty(lower, upper)) {
        return BeeConst.STRING_EMPTY;
      } else {
        return lower + PERIOD_SEPARATOR + upper;
      }
    }
  }

  private void render(Flow panel) {
    Horizontal performance = new Horizontal(STYLE_PERFORMANCE);

    if (Global.isDebug()) {
      performance.add(renderMillis(results.getInitStart()));
      performance.add(renderDuration(results.getValidateStart() - results.getInitStart()));

      performance.add(renderMillis(results.getValidateStart()));
      performance.add(renderDuration(results.getComputeStart() - results.getValidateStart()));

      performance.add(renderMillis(results.getComputeStart()));
      performance.add(renderDuration(results.getComputeEnd() - results.getComputeStart()));

      performance.add(renderMillis(results.getComputeEnd()));
      performance.add(renderDuration(results.getComputeEnd() - results.getInitStart()));

    } else {
      performance.add(renderMillis(results.getComputeEnd()));
      performance.add(renderDuration(results.getComputeEnd() - results.getInitStart()));
    }

    panel.add(performance);

    if (results.getQueryCount() > 0) {
      Horizontal stats = new Horizontal(STYLE_STATS);

      stats.add(new Label(BeeUtils.toString(results.getQueryCount())));
      stats.add(renderDuration(results.getQueryDuration() / results.getQueryCount()));
      stats.add(renderDuration(results.getQueryDuration()));

      panel.add(stats);
    }

    Flow caption = new Flow(STYLE_CAPTION);
    results.getHeaderLabels(formatRange(results.getHeaderRange()), false).forEach(analysisLabel ->
        caption.add(render(analysisLabel)));
    panel.add(caption);

    Flow wrapper = new Flow(STYLE_WRAPPER);
    wrapper.add(renderTable());
    panel.add(wrapper);
  }

  private Widget renderTable() {
    int maxColumnLabels = columnLabels.values().stream()
        .mapToInt(List::size)
        .max()
        .orElseGet(BeeConst.INT_ZERO_SUPPLIER);

    int maxColumnSplitTypes = columnSplitTypes.values().stream()
        .mapToInt(List::size)
        .max()
        .orElseGet(BeeConst.INT_ZERO_SUPPLIER);

    boolean columnsNeedBudget = columnCellTypes.values().stream()
        .anyMatch(AnalysisCellType::needsBudget);

    int maxRowLabels = rowLabels.values().stream()
        .mapToInt(List::size)
        .max()
        .orElseGet(BeeConst.INT_ZERO_SUPPLIER);

    int maxRowSplitTypes = rowSplitTypes.values().stream()
        .mapToInt(List::size)
        .max()
        .orElseGet(BeeConst.INT_ZERO_SUPPLIER);

    boolean rowsNeedBudget = rowCellTypes.values().stream()
        .anyMatch(AnalysisCellType::needsBudget);

    int rStartValues = maxColumnLabels + maxColumnSplitTypes + (columnsNeedBudget ? 1 : 0);
    int cStartValues = maxRowLabels + maxRowSplitTypes + (rowsNeedBudget ? 1 : 0);

    HtmlTable table = new HtmlTable(STYLE_TABLE);

    int r;
    int c;

    if (maxColumnLabels > 0) {
      r = 0;
      c = cStartValues;

      for (long columnId : columnIds) {
        List<AnalysisLabel> labels = columnLabels.get(columnId);
        Integer span = columnSpan.get(columnId);

        for (int i = 0; i < maxColumnLabels; i++) {
          int y = r + i;

          if (BeeUtils.isIndex(labels, i)) {
            table.setWidgetAndStyle(y, c, render(labels.get(i)), STYLE_COLUMN);
          } else {
            table.setText(y, c, null, STYLE_COLUMN_UNDEF, STYLE_LABEL_UNDEF);
          }

          if (BeeUtils.isMore(span, 1)) {
            table.getCellFormatter().setColSpan(y, c, span);
          }
        }

        c++;
      }

      logger.debug("column labels", maxColumnLabels);
    }

    if (maxColumnSplitTypes > 0) {
      c = cStartValues;
      Map<Integer, Integer> lastCells = new HashMap<>();

      for (long columnId : columnIds) {
        List<AnalysisSplitType> splitTypes = columnSplitTypes.get(columnId);
        int maxTypeIndex = -1;

        if (!BeeUtils.isEmpty(splitTypes)) {
          Map<AnalysisSplitType, List<AnalysisSplitValue>> splitValuesByType =
              columnSplitValues.get(columnId);
          List<SplitTree> splitTrees = columnSplitTree.get(columnId);

          if (!BeeUtils.isEmpty(splitValuesByType) && !BeeUtils.isEmpty(splitTrees)) {
            List<AnalysisCellType> cellTypes = columnCellTypes.get(columnId);
            int cellSize = Math.max(BeeUtils.size(cellTypes), 1);

            for (SplitTree splitTree : splitTrees) {
              ListMultimap<Integer, SplitTree> treeCollector = ArrayListMultimap.create();
              splitTree.collect(0, treeCollector);

              for (int typeIndex : treeCollector.keySet()) {
                AnalysisSplitType splitType = BeeUtils.getQuietly(splitTypes, typeIndex);
                List<AnalysisSplitValue> splitValues =
                    BeeUtils.getQuietly(splitValuesByType, splitType);

                List<SplitTree> trees = treeCollector.get(typeIndex);

                for (int j = 0; j < trees.size(); j++) {
                  SplitTree tree = trees.get(j);
                  logger.debug("c flat", typeIndex, j, tree.valueIndex, tree.size());

                  AnalysisSplitValue splitValue = BeeUtils.getQuietly(splitValues, tree.valueIndex);
                  if (splitValue != null) {
                    int y = maxColumnLabels + typeIndex;

                    int x;
                    if (lastCells.containsKey(y)) {
                      x = lastCells.get(y) + 1;
                    } else {
                      x = c;
                    }
                    lastCells.put(y, x);

                    table.setWidgetAndStyle(y, x, render(splitType, splitValue), STYLE_COLUMN);

                    int span = tree.size() * cellSize;
                    if (span > 1) {
                      table.getCellFormatter().setColSpan(y, x, span);
                    }

                    maxTypeIndex = Math.max(maxTypeIndex, typeIndex);
                  }
                }
              }
            }
          }
        }

        int cSpan = columnSpan.get(columnId);

        if (maxTypeIndex < maxColumnSplitTypes - 1) {
          for (int i = maxTypeIndex + 1; i < maxColumnSplitTypes; i++) {
            int y = maxColumnLabels + i;

            int x;
            if (lastCells.containsKey(y)) {
              x = lastCells.get(y) + 1;
            } else {
              x = c;
            }
            lastCells.put(y, x);

            table.setText(y, x, null, STYLE_COLUMN_UNDEF, STYLE_SPLIT_UNDEF);
            if (cSpan > 1) {
              table.getCellFormatter().setColSpan(y, x, cSpan);
            }
          }
        }

        c += cSpan;
      }

      logger.debug("column splits", maxColumnSplitTypes);
    }

    if (columnsNeedBudget) {
      r = maxColumnLabels + maxColumnSplitTypes;
      c = cStartValues;

      for (long columnId : columnIds) {
        List<AnalysisCellType> cellTypes = columnCellTypes.get(columnId);
        int span = columnSpan.get(columnId);

        if (AnalysisCellType.needsBudget(cellTypes)) {
          int size = cellTypes.size();

          for (int i = 0; i < span; i += size) {
            for (int j = 0; j < size; j++) {
              table.setWidgetAndStyle(r, c + i + j, render(cellTypes.get(j)), STYLE_COLUMN);
            }
          }

        } else {
          for (int i = 0; i < span; i++) {
            table.setText(r, c + i, null, STYLE_COLUMN_UNDEF, STYLE_TYPE_UNDEF);
          }
        }

        c += span;
      }

      logger.debug("column cell types");
    }

    r = rStartValues;

    Map<Integer, Integer> lastSplitCells = new HashMap<>();

    for (long rowId : rowIds) {
      logger.debug("row", rowId);

      if (maxRowLabels > 0) {
        List<AnalysisLabel> labels = rowLabels.get(rowId);

        if (!BeeUtils.isEmpty(labels)) {
          c = 0;

          for (int i = 0; i < labels.size(); i++) {
            table.setWidgetAndStyle(r, c + i, render(labels.get(i)), STYLE_ROW);
          }

          logger.debug("labels", labels.size());
        }
      }

      if (maxRowSplitTypes > 0) {
        List<AnalysisSplitType> splitTypes = rowSplitTypes.get(rowId);

        if (!BeeUtils.isEmpty(splitTypes)) {
          Map<AnalysisSplitType, List<AnalysisSplitValue>> splitValuesByType =
              rowSplitValues.get(rowId);
          List<SplitTree> splitTrees = rowSplitTree.get(rowId);

          if (!BeeUtils.isEmpty(splitValuesByType) && !BeeUtils.isEmpty(splitTrees)) {
            List<AnalysisCellType> cellTypes = rowCellTypes.get(rowId);
            int cellSize = Math.max(BeeUtils.size(cellTypes), 1);

            for (SplitTree splitTree : splitTrees) {
              ListMultimap<Integer, SplitTree> treeCollector = ArrayListMultimap.create();
              splitTree.collect(0, treeCollector);

              for (int typeIndex : treeCollector.keySet()) {
                AnalysisSplitType splitType = BeeUtils.getQuietly(splitTypes, typeIndex);
                List<AnalysisSplitValue> splitValues =
                    BeeUtils.getQuietly(splitValuesByType, splitType);

                List<SplitTree> trees = treeCollector.get(typeIndex);

                for (int i = 0; i < trees.size(); i++) {
                  SplitTree tree = trees.get(i);
                  logger.debug("r flat", typeIndex, i, tree.valueIndex, tree.size());

                  AnalysisSplitValue splitValue = BeeUtils.getQuietly(splitValues, tree.valueIndex);
                  if (splitValue != null) {
                    int x = maxRowLabels + typeIndex;
                    int y;

                    if (lastSplitCells.containsKey(x)) {
                      y = Math.max(lastSplitCells.get(x) + 1, r);
                    } else {
                      y = r;
                    }
                    lastSplitCells.put(x, y);

                    table.setWidgetAndStyle(y, x, render(splitType, splitValue), STYLE_ROW);

                    int span = tree.size() * cellSize;
                    if (span > 1) {
                      lastSplitCells.put(x, y + span - 1);
                    }
                  }
                }
              }
            }
          }

          logger.debug("splits", splitTypes.size());
        }
      }

      if (rowsNeedBudget) {
        List<AnalysisCellType> cellTypes = rowCellTypes.get(rowId);

        if (AnalysisCellType.needsBudget(cellTypes)) {
          c = maxRowLabels + maxRowSplitTypes;

          int size = cellTypes.size();
          int span = rowSpan.get(rowId);

          for (int i = 0; i < span; i += size) {
            for (int j = 0; j < size; j++) {
              table.setWidgetAndStyle(r + i + j, c, render(cellTypes.get(j)), STYLE_ROW);
            }
          }

          logger.debug("cell types", size);
        }
      }

      c = cStartValues;
      for (long columnId : columnIds) {
        if (values.contains(rowId, columnId)) {
          List<AnalysisValue> analysisValues = values.get(rowId, columnId);

          renderValues(analysisValues, table, r, c);
          logger.debug("column", columnId, "values", analysisValues.size());
        }

        c += columnSpan.get(columnId);
      }

      r += rowSpan.get(rowId);
    }

    int maxCellCount = 0;
    for (int i = rStartValues; i < table.getRowCount(); i++) {
      maxCellCount = Math.max(maxCellCount, table.getCellCount(i));
    }

    if (maxCellCount > cStartValues) {
      for (int i = rStartValues; i < table.getRowCount(); i++) {
        if (table.getCellCount(i) < maxCellCount) {
          table.setText(i, maxCellCount - 1, null);
        }

        for (int j = cStartValues; j < maxCellCount; j++) {
          TableCellElement cellElement = table.getCellFormatter().getElement(i, j);
          if (BeeUtils.allEmpty(cellElement.getClassName(), cellElement.getInnerText())) {
            cellElement.addClassName(STYLE_VALUE_EMPTY);
          }
        }
      }
    }

    return table;
  }

  private void renderValues(Collection<AnalysisValue> analysisValues,
      HtmlTable table, int r, int c) {

    for (AnalysisValue value : analysisValues) {
      int rowOffset = getOffset(value, false);
      int columnOffset = getOffset(value, true);

      List<AnalysisCellType> rowTypes = rowCellTypes.get(value.getRowId());
      List<AnalysisCellType> columnTypes = columnCellTypes.get(value.getColumnId());

      int rowSize = BeeUtils.size(rowTypes);
      int columnSize = BeeUtils.size(columnTypes);

      AnalysisCellType cellType;

      for (int i = 0; i < rowSize; i++) {
        AnalysisCellType rowType = rowTypes.get(i);

        for (int j = 0; j < columnSize; j++) {
          AnalysisCellType columnType = columnTypes.get(j);

          if (i > 0) {
            cellType = rowType;
          } else if (j > 0) {
            cellType = columnType;
          } else if (columnType.isDefault()) {
            cellType = rowType;
          } else {
            cellType = columnType;
          }

          String text = cellType.render(value);
          if (!BeeUtils.isEmpty(text)) {
            String typeSuffix = cellType.getAnalysisValueType().name().toLowerCase();
            table.setText(r + rowOffset * rowSize + i, c + columnOffset * columnSize + j, text,
                STYLE_VALUE, STYLE_VALUE_PREFIX + typeSuffix);
          }
        }
      }
    }
  }

  private int getOffset(AnalysisValue value, boolean isColumn) {
    int offset = 0;

    Map<AnalysisSplitType, AnalysisSplitValue> split =
        isColumn ? value.getColumnSplit() : value.getRowSplit();
    if (BeeUtils.isEmpty(split)) {
      return offset;
    }

    long id = isColumn ? value.getColumnId() : value.getRowId();

    List<AnalysisSplitType> splitTypes =
        isColumn ? columnSplitTypes.get(id) : rowSplitTypes.get(id);
    if (BeeUtils.isEmpty(splitTypes)) {
      return offset;
    }

    Map<AnalysisSplitType, List<AnalysisSplitValue>> splitValues =
        isColumn ? columnSplitValues.get(id) : rowSplitValues.get(id);
    if (BeeUtils.isEmpty(splitValues)) {
      return offset;
    }

    List<SplitTree> splitTrees = isColumn ? columnSplitTree.get(id) : rowSplitTree.get(id);
    if (BeeUtils.isEmpty(splitTrees)) {
      return offset;
    }

    for (AnalysisSplitType splitType : splitTypes) {
      int valueIndex = BeeUtils.indexOf(splitValues.get(splitType), split.get(splitType));

      if (!BeeConst.isUndef(valueIndex)) {
        List<SplitTree> children = null;

        for (SplitTree splitTree : splitTrees) {
          if (splitTree.valueIndex == valueIndex) {
            children = splitTree.children;
            break;

          } else {
            offset += splitTree.size();
          }
        }

        if (BeeUtils.isEmpty(children)) {
          break;
        } else {
          splitTrees = children;
        }
      }
    }

    return offset;
  }

  private static Widget renderMillis(long millis) {
    Label label = new Label(Format.renderDateTime(millis));
    label.addStyleName(STYLE_TIME);
    return label;
  }

  private static Widget renderDuration(long millis) {
    Label label = new Label(BeeUtils.bracket(TimeUtils.renderMillis(millis)));
    label.addStyleName(STYLE_DURATION);
    return label;
  }

  private static Widget render(AnalysisLabel label, String... styleNames) {
    Label widget = new Label(label.getText());

    if (styleNames != null) {
      for (String styleName : styleNames) {
        widget.addStyleName(styleName);
      }
    }

    widget.addStyleName(STYLE_LABEL);
    widget.addStyleName(STYLE_LABEL_PREFIX + label.getSource().toLowerCase());

    setColors(widget.getElement(), label.getBackground(), label.getForeground());

    return widget;
  }

  private static Widget render(AnalysisSplitType splitType, AnalysisSplitValue splitValue,
      String... styleNames) {

    String text = splitValue.isEmpty()
        ? BeeUtils.bracket(splitType.getCaption()) : splitValue.getValue();

    Label widget = new Label(text);

    if (styleNames != null) {
      for (String styleName : styleNames) {
        widget.addStyleName(styleName);
      }
    }

    widget.addStyleName(STYLE_SPLIT);
    widget.addStyleName(STYLE_SPLIT_PREFIX + splitType.name().toLowerCase());
    if (splitValue.isEmpty()) {
      widget.addStyleName(STYLE_SPLIT_EMPTY);
    }

    setColors(widget.getElement(), splitValue.getBackground(), splitValue.getForeground());

    return widget;
  }

  private static Widget render(AnalysisCellType cellType, String... styleNames) {
    AnalysisValueType valueType = cellType.getAnalysisValueType();
    Label widget = new Label(valueType.getCaption());

    if (styleNames != null) {
      for (String styleName : styleNames) {
        widget.addStyleName(styleName);
      }
    }

    widget.addStyleName(STYLE_TYPE);
    widget.addStyleName(STYLE_TYPE_PREFIX + valueType.name().toLowerCase());

    return widget;
  }

  private static void setColors(Element target, String bg, String fg) {
    if (!BeeUtils.same(bg, fg)) {
      if (!BeeUtils.isEmpty(bg)) {
        target.getStyle().setBackgroundColor(bg);
      }

      if (!BeeUtils.isEmpty(fg)) {
        target.getStyle().setColor(fg);
      }
    }
  }
}
