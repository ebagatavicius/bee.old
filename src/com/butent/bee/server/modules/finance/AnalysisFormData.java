package com.butent.bee.server.modules.finance;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.analysis.AnalysisCellType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisSplitType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisUtils;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class AnalysisFormData {

  private final Map<String, Integer> headerIndexes;
  private final BeeRow header;

  private final Map<String, Integer> columnIndexes;
  private final List<BeeRow> columns = new ArrayList<>();

  private final Map<String, Integer> rowIndexes;
  private final List<BeeRow> rows = new ArrayList<>();

  private final Map<String, Integer> filterIndexes;
  private final List<BeeRow> headerFilters = new ArrayList<>();
  private final Multimap<Long, BeeRow> columnFilters = ArrayListMultimap.create();
  private final Multimap<Long, BeeRow> rowFilters = ArrayListMultimap.create();

  AnalysisFormData(BeeRowSet headerData, BeeRowSet columnData, BeeRowSet rowData,
      BeeRowSet filterData) {

    this.headerIndexes = AnalysisUtils.getIndexes(headerData);
    this.header = DataUtils.isEmpty(headerData) ? null : headerData.getRow(0);

    this.columnIndexes = AnalysisUtils.getIndexes(columnData);
    if (!DataUtils.isEmpty(columnData)) {
      this.columns.addAll(columnData.getRows());
    }

    this.rowIndexes = AnalysisUtils.getIndexes(rowData);
    if (!DataUtils.isEmpty(rowData)) {
      this.rows.addAll(rowData.getRows());
    }

    this.filterIndexes = AnalysisUtils.getIndexes(filterData);

    if (!DataUtils.isEmpty(filterData)) {
      for (BeeRow row : filterData) {
        Long parent = row.getLong(filterIndexes.get(COL_ANALYSIS_HEADER));

        if (DataUtils.isId(parent)) {
          headerFilters.add(row);

        } else {
          parent = row.getLong(filterIndexes.get(COL_ANALYSIS_COLUMN));
          if (DataUtils.isId(parent)) {
            columnFilters.put(parent, row);

          } else {
            parent = row.getLong(filterIndexes.get(COL_ANALYSIS_ROW));
            if (DataUtils.isId(parent)) {
              rowFilters.put(parent, row);
            }
          }
        }
      }
    }
  }

  Map<String, Integer> getHeaderIndexes() {
    return headerIndexes;
  }

  BeeRow getHeader() {
    return header;
  }

  Filter getHeaderFilter(String employeeColumnName, Function<String, Filter> filterParser) {
    CompoundFilter filter = Filter.and();

    filter.add(getEmployeeFilter(header, headerIndexes.get(COL_ANALYSIS_HEADER_EMPLOYEE),
        employeeColumnName));
    addDimensionFilters(filter, header, headerIndexes, null);

    if (!BeeUtils.isEmpty(headerFilters)) {
      filter.add(getFilter(headerFilters, employeeColumnName, filterParser));
    }

    return AnalysisUtils.normalize(filter);
  }

  AnalysisFilter getHeaderAnalysisFilter() {
    AnalysisFilter af = new AnalysisFilter(header, headerIndexes, COL_ANALYSIS_HEADER_EMPLOYEE);
    af.setSubFilters(headerFilters, filterIndexes);
    return af;
  }

  MonthRange getHeaderRange() {
    return getHeaderRange(null);
  }

  private MonthRange getHeaderRange(BiConsumer<String, String> errorConsumer) {
    Integer yearFrom = getHeaderInteger(COL_ANALYSIS_HEADER_YEAR_FROM);
    Integer monthFrom = getHeaderInteger(COL_ANALYSIS_HEADER_MONTH_FROM);
    Integer yearUntil = getHeaderInteger(COL_ANALYSIS_HEADER_YEAR_UNTIL);
    Integer monthUntil = getHeaderInteger(COL_ANALYSIS_HEADER_MONTH_UNTIL);

    return getRange(yearFrom, monthFrom, yearUntil, monthUntil, errorConsumer);
  }

  Map<String, Integer> getColumnIndexes() {
    return columnIndexes;
  }

  List<BeeRow> getColumns() {
    return columns;
  }

  Filter getColumnFilter(BeeRow column, String employeeColumnName,
      Function<String, Filter> filterParser) {

    CompoundFilter filter = Filter.and();

    if (isHeaderTrue(COL_ANALYSIS_SHOW_COLUMN_EMPLOYEE)) {
      filter.add(getEmployeeFilter(column, columnIndexes.get(COL_ANALYSIS_COLUMN_EMPLOYEE),
          employeeColumnName));
    }

    addDimensionFilters(filter, column, columnIndexes,
        ordinal -> isHeaderTrue(colAnalysisShowColumnDimension(ordinal)));

    if (columnFilters.containsKey(column.getId()) && isHeaderTrue(COL_ANALYSIS_COLUMN_FILTERS)) {
      filter.add(getFilter(columnFilters.get(column.getId()), employeeColumnName, filterParser));
    }

    return AnalysisUtils.normalize(filter);
  }

  AnalysisFilter getColumnAnalysisFilter(BeeRow column) {
    Map<String, Integer> indexes = new HashMap<>();
    indexes.putAll(columnIndexes);

    if (!isHeaderTrue(COL_ANALYSIS_SHOW_COLUMN_EMPLOYEE)) {
      indexes.remove(COL_ANALYSIS_COLUMN_EMPLOYEE);
    }

    if (Dimensions.getObserved() > 0) {
      for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
        if (!isHeaderTrue(colAnalysisShowColumnDimension(ordinal))) {
          indexes.remove(Dimensions.getRelationColumn(ordinal));
        }
      }
    }

    AnalysisFilter af = new AnalysisFilter(column, indexes, COL_ANALYSIS_COLUMN_EMPLOYEE);

    if (columnFilters.containsKey(column.getId()) && isHeaderTrue(COL_ANALYSIS_COLUMN_FILTERS)) {
      af.setSubFilters(columnFilters.get(column.getId()), filterIndexes);
    }

    return af;
  }

  MonthRange getColumnRange(BeeRow column) {
    return getColumnRange(column, null);
  }

  private MonthRange getColumnRange(BeeRow column, BiConsumer<String, String> errorConsumer) {
    Integer yearFrom = getColumnInteger(column, COL_ANALYSIS_COLUMN_YEAR_FROM);
    Integer monthFrom = getColumnInteger(column, COL_ANALYSIS_COLUMN_MONTH_FROM);
    Integer yearUntil = getColumnInteger(column, COL_ANALYSIS_COLUMN_YEAR_UNTIL);
    Integer monthUntil = getColumnInteger(column, COL_ANALYSIS_COLUMN_MONTH_UNTIL);

    return getRange(yearFrom, monthFrom, yearUntil, monthUntil, errorConsumer);
  }

  Map<String, Integer> getRowIndexes() {
    return rowIndexes;
  }

  List<BeeRow> getRows() {
    return rows;
  }

  Filter getRowFilter(BeeRow row, String employeeColumnName,
      Function<String, Filter> filterParser) {

    CompoundFilter filter = Filter.and();

    if (isHeaderTrue(COL_ANALYSIS_SHOW_ROW_EMPLOYEE)) {
      filter.add(getEmployeeFilter(row, rowIndexes.get(COL_ANALYSIS_ROW_EMPLOYEE),
          employeeColumnName));
    }

    addDimensionFilters(filter, row, rowIndexes,
        ordinal -> isHeaderTrue(colAnalysisShowRowDimension(ordinal)));

    if (rowFilters.containsKey(row.getId()) && isHeaderTrue(COL_ANALYSIS_ROW_FILTERS)) {
      filter.add(getFilter(rowFilters.get(row.getId()), employeeColumnName, filterParser));
    }

    return AnalysisUtils.normalize(filter);
  }

  AnalysisFilter getRowAnalysisFilter(BeeRow row) {
    Map<String, Integer> indexes = new HashMap<>();
    indexes.putAll(rowIndexes);

    if (!isHeaderTrue(COL_ANALYSIS_SHOW_ROW_EMPLOYEE)) {
      indexes.remove(COL_ANALYSIS_ROW_EMPLOYEE);
    }

    if (Dimensions.getObserved() > 0) {
      for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
        if (!isHeaderTrue(colAnalysisShowRowDimension(ordinal))) {
          indexes.remove(Dimensions.getRelationColumn(ordinal));
        }
      }
    }

    AnalysisFilter af = new AnalysisFilter(row, indexes, COL_ANALYSIS_ROW_EMPLOYEE);

    if (rowFilters.containsKey(row.getId()) && isHeaderTrue(COL_ANALYSIS_ROW_FILTERS)) {
      af.setSubFilters(rowFilters.get(row.getId()), filterIndexes);
    }

    return af;
  }

  MonthRange getRowRange(BeeRow row) {
    return getRowRange(row, null);
  }

  private MonthRange getRowRange(BeeRow row, BiConsumer<String, String> errorConsumer) {
    Integer yearFrom = getRowInteger(row, COL_ANALYSIS_ROW_YEAR_FROM);
    Integer monthFrom = getRowInteger(row, COL_ANALYSIS_ROW_MONTH_FROM);
    Integer yearUntil = getRowInteger(row, COL_ANALYSIS_ROW_YEAR_UNTIL);
    Integer monthUntil = getRowInteger(row, COL_ANALYSIS_ROW_MONTH_UNTIL);

    return getRange(yearFrom, monthFrom, yearUntil, monthUntil, errorConsumer);
  }

  boolean hasScripting() {
    return columns.stream().anyMatch(this::columnHasScript)
        || rows.stream().anyMatch(this::rowHasScript);
  }

  List<String> validate(Dictionary dictionary, Predicate<String> filterValidator) {
    List<String> messages = new ArrayList<>();

    if (header == null) {
      messages.add("header not available");
      return messages;
    }
    if (BeeUtils.isEmpty(columns)) {
      messages.add(dictionary.dataNotAvailable(dictionary.finAnalysisColumns()));
      return messages;
    }
    if (BeeUtils.isEmpty(rows)) {
      messages.add(dictionary.dataNotAvailable(dictionary.finAnalysisRows()));
      return messages;
    }

    if (columns.stream().noneMatch(column -> isColumnTrue(column, COL_ANALYSIS_COLUMN_SELECTED))) {
      messages.add(dictionary.finAnalysisSelectColumns());
    }
    if (rows.stream().noneMatch(row -> isRowTrue(row, COL_ANALYSIS_ROW_SELECTED))) {
      messages.add(dictionary.finAnalysisSelectRows());
    }

    boolean columnsHasIndicator = columns.stream().anyMatch(this::columnHasIndicator);
    boolean rowsHasIndicator = rows.stream().anyMatch(this::rowHasIndicator);

    if (!columnsHasIndicator && !rowsHasIndicator) {
      messages.add(dictionary.finAnalysisSpecifyIndicators());
    }

    MonthRange headerRange = getHeaderRange((from, until) ->
        messages.add(dictionary.invalidPeriod(from, until)));

    Integer columnSplitLevels = getHeaderInteger(COL_ANALYSIS_COLUMN_SPLIT_LEVELS);

    for (BeeRow column : columns) {
      String columnLabel = getColumnLabel(dictionary, column);

      MonthRange columnRange = getColumnRange(column, (from, until) ->
          messages.add(BeeUtils.joinWords(columnLabel, dictionary.invalidPeriod(from, until))));

      if (columnRange != null && headerRange != null && !headerRange.intersects(columnRange)) {
        messages.add(BeeUtils.joinWords(columnLabel,
            dictionary.finAnalysisColumnAndFormPeriodsDoNotIntersect()));
      }

      messages.addAll(validateSplits(dictionary, columnLabel,
          getColumnSplits(column, columnSplitLevels)));
    }

    if (columns.stream().noneMatch(this::columnIsPrimary)) {
      messages.add(dictionary.finAnalysisPrimaryColumnsNotAvailable());
    }

    messages.addAll(validateAbbreviations(dictionary, columns,
        columnIndexes.get(COL_ANALYSIS_COLUMN_ABBREVIATION),
        column -> getColumnLabel(dictionary, column)));

    messages.addAll(AnalysisScripting.validateAnalysisScripts(columns,
        columnIndexes.get(COL_ANALYSIS_COLUMN_INDICATOR),
        columnIndexes.get(COL_ANALYSIS_COLUMN_ABBREVIATION),
        columnIndexes.get(COL_ANALYSIS_COLUMN_SCRIPT),
        column -> getColumnLabel(dictionary, column)));

    Integer rowSplitLevels = getHeaderInteger(COL_ANALYSIS_ROW_SPLIT_LEVELS);

    for (BeeRow row : rows) {
      String rowLabel = getRowLabel(dictionary, row);

      MonthRange rowRange = getRowRange(row, (from, until) ->
          messages.add(BeeUtils.joinWords(rowLabel, dictionary.invalidPeriod(from, until))));

      if (rowRange != null && headerRange != null && !headerRange.intersects(rowRange)) {
        messages.add(BeeUtils.joinWords(rowLabel,
            dictionary.finAnalysisRowAndFormPeriodsDoNotIntersect()));
      }

      messages.addAll(validateSplits(dictionary, rowLabel, getRowSplits(row, rowSplitLevels)));
    }

    if (rows.stream().noneMatch(this::rowIsPrimary)) {
      messages.add(dictionary.finAnalysisPrimaryRowsNotAvailable());
    }

    messages.addAll(validateAbbreviations(dictionary, rows,
        rowIndexes.get(COL_ANALYSIS_ROW_ABBREVIATION),
        row -> getRowLabel(dictionary, row)));

    messages.addAll(AnalysisScripting.validateAnalysisScripts(rows,
        rowIndexes.get(COL_ANALYSIS_ROW_INDICATOR),
        rowIndexes.get(COL_ANALYSIS_ROW_ABBREVIATION),
        rowIndexes.get(COL_ANALYSIS_ROW_SCRIPT),
        row -> getRowLabel(dictionary, row)));

    if (!DataUtils.isId(getHeaderLong(COL_ANALYSIS_HEADER_BUDGET_TYPE))) {
      messages.addAll(validateBudgetType(dictionary));
    }

    if (!BeeUtils.isEmpty(filterIndexes) && filterValidator != null) {
      messages.addAll(validateFilters(dictionary, filterValidator));
    }

    return messages;
  }

  private Integer getHeaderInteger(String key) {
    return header.getInteger(headerIndexes.get(key));
  }

  Long getHeaderLong(String key) {
    return header.getLong(headerIndexes.get(key));
  }

  private String getHeaderString(String key) {
    return header.getString(headerIndexes.get(key));
  }

  private boolean isHeaderTrue(String key) {
    return header.isTrue(headerIndexes.get(key));
  }

  private BeeRow getColumnById(Long id) {
    for (BeeRow column : columns) {
      if (DataUtils.idEquals(column, id)) {
        return column;
      }
    }
    return null;
  }

  private String getColumnLabel(Dictionary dictionary, BeeRow column) {
    String label = BeeUtils.joinWords(getColumnString(column, COL_ANALYSIS_COLUMN_ORDINAL),
        BeeUtils.notEmpty(getColumnString(column, COL_ANALYSIS_COLUMN_NAME),
            getColumnAbbreviation(column)));

    if (BeeUtils.isEmpty(label)) {
      return BeeUtils.joinWords(dictionary.column(), DataUtils.ID_TAG, column.getId());
    } else {
      return BeeUtils.joinWords(dictionary.column(), label);
    }
  }

  Integer getColumnInteger(BeeRow column, String key) {
    return column.getInteger(columnIndexes.get(key));
  }

  <E extends Enum<?>> E getColumnEnum(BeeRow column, String key, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getColumnInteger(column, key));
  }

  Long getColumnLong(BeeRow column, String key) {
    return column.getLong(columnIndexes.get(key));
  }

  String getColumnString(BeeRow column, String key) {
    return column.getString(columnIndexes.get(key));
  }

  private boolean isColumnTrue(BeeRow column, String key) {
    return column.isTrue(columnIndexes.get(key));
  }

  List<AnalysisCellType> getColumnCellTypes(BeeRow column) {
    return AnalysisCellType.normalize(getColumnString(column, COL_ANALYSIS_COLUMN_VALUES));
  }

  List<AnalysisSplitType> getColumnSplits(BeeRow column) {
    return getColumnSplits(column, getHeaderInteger(COL_ANALYSIS_COLUMN_SPLIT_LEVELS));
  }

  private List<AnalysisSplitType> getColumnSplits(BeeRow column, Integer levels) {
    List<AnalysisSplitType> splits = new ArrayList<>();

    if (BeeUtils.isPositive(levels)) {
      int max = Math.min(levels, COL_ANALYSIS_COLUMN_SPLIT.length);

      for (int i = 0; i < max; i++) {
        String value = getColumnString(column, COL_ANALYSIS_COLUMN_SPLIT[i]);
        AnalysisSplitType split = EnumUtils.getEnumByName(AnalysisSplitType.class, value);

        if (split != null) {
          splits.add(split);
        }
      }
    }

    return splits;
  }

  Integer getColumnScale(BeeRow column) {
    return getColumnInteger(column, COL_ANALYSIS_COLUMN_SCALE);
  }

  String getColumnScript(BeeRow column) {
    return getColumnString(column, COL_ANALYSIS_COLUMN_SCRIPT);
  }

  String getColumnAbbreviation(BeeRow column) {
    return getColumnString(column, COL_ANALYSIS_COLUMN_ABBREVIATION);
  }

  private boolean columnHasIndicator(BeeRow column) {
    return DataUtils.isId(getColumnLong(column, COL_ANALYSIS_COLUMN_INDICATOR));
  }

  private boolean columnHasScript(BeeRow column) {
    return !BeeUtils.isEmpty(getColumnScript(column));
  }

  boolean columnIsPrimary(BeeRow column) {
    if (columnHasIndicator(column)) {
      return true;
    }

    String script = getColumnScript(column);
    return BeeUtils.isEmpty(script) || AnalysisScripting.isScriptPrimary(script);
  }

  Map<Long, String> getColumnVariables(BeeRow column) {
    Map<Long, String> variables = new HashMap<>();

    String script = getColumnScript(column);
    if (BeeUtils.isEmpty(script) || columnIsPrimary(column)) {
      return variables;
    }

    for (BeeRow c : columns) {
      if (c.getId() != column.getId()) {
        String abbreviation = getColumnAbbreviation(c);

        if (AnalysisUtils.isValidAbbreviation(abbreviation)
            && AnalysisScripting.containsVariable(script, abbreviation)) {

          variables.put(c.getId(), abbreviation);
        }
      }
    }

    return variables;
  }

  List<BeeRow> getSecondaryColumns(Consumer<String> errorHandler) {
    List<BeeRow> result = new ArrayList<>();

    Multimap<Integer, Long> sequence =
        AnalysisScripting.buildSecondaryCalculationSequence(getColumns(),
            columnIndexes.get(COL_ANALYSIS_COLUMN_INDICATOR),
            columnIndexes.get(COL_ANALYSIS_COLUMN_ABBREVIATION),
            columnIndexes.get(COL_ANALYSIS_COLUMN_SCRIPT),
            errorHandler);

    if (sequence != null && !sequence.isEmpty()) {
      List<Integer> levels = new ArrayList<>(sequence.keySet());
      levels.sort(null);

      for (int level : levels) {
        for (long id : sequence.get(level)) {
          BeeRow column = getColumnById(id);
          if (column != null) {
            result.add(column);
          }
        }
      }
    }

    return result;
  }

  private BeeRow getRowById(Long id) {
    for (BeeRow row : rows) {
      if (DataUtils.idEquals(row, id)) {
        return row;
      }
    }
    return null;
  }

  private String getRowLabel(Dictionary dictionary, BeeRow row) {
    String label = BeeUtils.joinWords(getRowString(row, COL_ANALYSIS_ROW_ORDINAL),
        BeeUtils.notEmpty(getRowString(row, COL_ANALYSIS_ROW_NAME), getRowAbbreviation(row)));

    if (BeeUtils.isEmpty(label)) {
      return BeeUtils.joinWords(dictionary.row(), DataUtils.ID_TAG, row.getId());
    } else {
      return BeeUtils.joinWords(dictionary.row(), label);
    }
  }

  Integer getRowInteger(BeeRow row, String key) {
    return row.getInteger(rowIndexes.get(key));
  }

  <E extends Enum<?>> E getRowEnum(BeeRow row, String key, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getRowInteger(row, key));
  }

  Long getRowLong(BeeRow row, String key) {
    return row.getLong(rowIndexes.get(key));
  }

  String getRowString(BeeRow row, String key) {
    return row.getString(rowIndexes.get(key));
  }

  private boolean isRowTrue(BeeRow row, String key) {
    return row.isTrue(rowIndexes.get(key));
  }

  List<AnalysisCellType> getRowCellTypes(BeeRow row) {
    return AnalysisCellType.normalize(getRowString(row, COL_ANALYSIS_ROW_VALUES));
  }

  List<AnalysisSplitType> getRowSplits(BeeRow row) {
    return getRowSplits(row, getHeaderInteger(COL_ANALYSIS_ROW_SPLIT_LEVELS));
  }

  private List<AnalysisSplitType> getRowSplits(BeeRow row, Integer levels) {
    List<AnalysisSplitType> splits = new ArrayList<>();

    if (BeeUtils.isPositive(levels)) {
      int max = Math.min(levels, COL_ANALYSIS_ROW_SPLIT.length);

      for (int i = 0; i < max; i++) {
        String value = getRowString(row, COL_ANALYSIS_ROW_SPLIT[i]);
        AnalysisSplitType split = EnumUtils.getEnumByName(AnalysisSplitType.class, value);

        if (split != null) {
          splits.add(split);
        }
      }
    }

    return splits;
  }

  Integer getRowScale(BeeRow row) {
    return getRowInteger(row, COL_ANALYSIS_ROW_SCALE);
  }

  String getRowScript(BeeRow row) {
    return getRowString(row, COL_ANALYSIS_ROW_SCRIPT);
  }

  String getRowAbbreviation(BeeRow row) {
    return getRowString(row, COL_ANALYSIS_ROW_ABBREVIATION);
  }

  private boolean rowHasIndicator(BeeRow row) {
    return DataUtils.isId(getRowLong(row, COL_ANALYSIS_ROW_INDICATOR));
  }

  private boolean rowHasScript(BeeRow row) {
    return !BeeUtils.isEmpty(getRowScript(row));
  }

  boolean rowIsPrimary(BeeRow row) {
    if (rowHasIndicator(row)) {
      return true;
    }

    String script = getRowScript(row);
    return BeeUtils.isEmpty(script) || AnalysisScripting.isScriptPrimary(script);
  }

  Map<Long, String> getRowVariables(BeeRow row) {
    Map<Long, String> variables = new HashMap<>();

    String script = getRowScript(row);
    if (BeeUtils.isEmpty(script) || rowIsPrimary(row)) {
      return variables;
    }

    for (BeeRow r : rows) {
      if (r.getId() != row.getId()) {
        String abbreviation = getRowAbbreviation(r);

        if (AnalysisUtils.isValidAbbreviation(abbreviation)
            && AnalysisScripting.containsVariable(script, abbreviation)) {

          variables.put(r.getId(), abbreviation);
        }
      }
    }

    return variables;
  }

  List<BeeRow> getSecondaryRows(Consumer<String> errorHandler) {
    List<BeeRow> result = new ArrayList<>();

    Multimap<Integer, Long> sequence =
        AnalysisScripting.buildSecondaryCalculationSequence(getRows(),
            rowIndexes.get(COL_ANALYSIS_ROW_INDICATOR),
            rowIndexes.get(COL_ANALYSIS_ROW_ABBREVIATION),
            rowIndexes.get(COL_ANALYSIS_ROW_SCRIPT),
            errorHandler);

    if (sequence != null && !sequence.isEmpty()) {
      List<Integer> levels = new ArrayList<>(sequence.keySet());
      levels.sort(null);

      for (int level : levels) {
        for (long id : sequence.get(level)) {
          BeeRow row = getRowById(id);
          if (row != null) {
            result.add(row);
          }
        }
      }
    }

    return result;
  }

  Set<Long> getIndicators() {
    Set<Long> indicators = new HashSet<>();

    int index = columnIndexes.get(COL_ANALYSIS_COLUMN_INDICATOR);
    for (BeeRow column : columns) {
      Long indicator = column.getLong(index);
      if (DataUtils.isId(indicator)) {
        indicators.add(indicator);
      }
    }

    index = rowIndexes.get(COL_ANALYSIS_ROW_INDICATOR);
    for (BeeRow row : rows) {
      Long indicator = row.getLong(index);
      if (DataUtils.isId(indicator)) {
        indicators.add(indicator);
      }
    }

    return indicators;
  }

  private static List<String> getSplitCaptions(Dictionary dictionary,
      List<AnalysisSplitType> splits) {

    return splits.stream().map(e -> e.getCaption(dictionary)).collect(Collectors.toList());
  }

  private static List<String> validateAbbreviations(Dictionary dictionary,
      Collection<BeeRow> input, int abbreviationIndex, Function<BeeRow, String> labelFunction) {

    List<String> messages = new ArrayList<>();
    Multiset<String> abbreviations = HashMultiset.create();

    if (!BeeUtils.isEmpty(input)) {
      for (BeeRow row : input) {
        String abbreviation = row.getString(abbreviationIndex);

        if (!BeeUtils.isEmpty(abbreviation)) {
          if (AnalysisUtils.isValidAbbreviation(abbreviation)) {
            abbreviations.add(abbreviation);
          } else {
            messages.add(BeeUtils.joinWords(labelFunction.apply(row),
                dictionary.finAnalysisInvalidAbbreviation(), abbreviation));
          }
        }
      }

      if (abbreviations.size() > abbreviations.entrySet().size()) {
        for (BeeRow row : input) {
          String abbreviation = row.getString(abbreviationIndex);

          if (!BeeUtils.isEmpty(abbreviation) && abbreviations.count(abbreviation) > 1) {
            messages.add(BeeUtils.joinWords(labelFunction.apply(row),
                dictionary.valueNotUnique(abbreviation)));
          }
        }
      }
    }

    return messages;
  }

  private List<String> validateBudgetType(Dictionary dictionary) {
    List<String> messages = new ArrayList<>();

    int columnBudgetTypeIndex = columnIndexes.get(COL_ANALYSIS_COLUMN_BUDGET_TYPE);
    int columnValuesIndex = columnIndexes.get(COL_ANALYSIS_COLUMN_VALUES);

    boolean columnsNeedBudget = columns.stream()
        .anyMatch(column -> !DataUtils.isId(column.getLong(columnBudgetTypeIndex))
            && AnalysisCellType.needsBudget(column.getString(columnValuesIndex)));

    int rowBudgetTypeIndex = rowIndexes.get(COL_ANALYSIS_ROW_BUDGET_TYPE);
    int rowValuesIndex = rowIndexes.get(COL_ANALYSIS_ROW_VALUES);

    for (BeeRow row : rows) {
      if (!DataUtils.isId(row.getLong(rowBudgetTypeIndex))
          && (AnalysisCellType.needsBudget(row.getString(rowValuesIndex))
          || columnsNeedBudget && rowHasIndicator(row))) {

        messages.add(BeeUtils.joinWords(getRowLabel(dictionary, row),
            dictionary.finAnalysisSpecifyBudgetType()));
      }
    }

    return messages;
  }

  private List<String> validateFilters(Dictionary dictionary, Predicate<String> validator) {
    List<String> messages = new ArrayList<>();

    int extraFilterIndex = filterIndexes.get(COL_ANALYSIS_FILTER_EXTRA);

    if (!BeeUtils.isEmpty(headerFilters)) {
      for (BeeRow filter : headerFilters) {
        String extraFilter = filter.getString(extraFilterIndex);
        if (!validator.test(extraFilter)) {
          messages.add(BeeUtils.joinWords(dictionary.finAnalysisInvalidExtraFilter(),
              extraFilter));
        }
      }
    }

    if (!columnFilters.isEmpty() && isHeaderTrue(COL_ANALYSIS_COLUMN_FILTERS)) {
      for (Long columnId : columnFilters.keySet()) {
        for (BeeRow filter : columnFilters.get(columnId)) {
          String extraFilter = filter.getString(extraFilterIndex);

          if (!validator.test(extraFilter)) {
            BeeRow column = getColumnById(columnId);
            if (column != null) {
              messages.add(BeeUtils.joinWords(getColumnLabel(dictionary, column),
                  dictionary.finAnalysisInvalidExtraFilter(), extraFilter));
            }
          }
        }
      }
    }

    if (!rowFilters.isEmpty() && isHeaderTrue(COL_ANALYSIS_ROW_FILTERS)) {
      for (Long rowId : rowFilters.keySet()) {
        for (BeeRow filter : rowFilters.get(rowId)) {
          String extraFilter = filter.getString(extraFilterIndex);

          if (!validator.test(extraFilter)) {
            BeeRow row = getRowById(rowId);
            if (row != null) {
              messages.add(BeeUtils.joinWords(getRowLabel(dictionary, row),
                  dictionary.finAnalysisInvalidExtraFilter(), extraFilter));
            }
          }
        }
      }
    }

    return messages;
  }


  private static List<String> validateSplits(Dictionary dictionary, String label,
      List<AnalysisSplitType> splits) {

    List<String> messages = new ArrayList<>();

    if (!BeeUtils.isEmpty(splits)) {
      if (AnalysisSplitType.validateSplits(splits)) {
        for (AnalysisSplitType split : splits) {
          if (!split.isVisible()) {
            messages.add(BeeUtils.joinWords(label,
                dictionary.finAnalysisInvalidSplit(), split.getCaption(dictionary)));
          }
        }

      } else {
        messages.add(BeeUtils.joinWords(label,
            dictionary.finAnalysisInvalidSplit(), getSplitCaptions(dictionary, splits)));
      }
    }

    return messages;
  }

  private static void addDimensionFilters(CompoundFilter builder, BeeRow row,
      Map<String, Integer> indexes, IntPredicate predicate) {

    if (Dimensions.getObserved() > 0) {
      for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
        if (predicate == null || predicate.test(ordinal)) {
          String column = Dimensions.getRelationColumn(ordinal);
          Long value = row.getLong(indexes.get(column));

          if (DataUtils.isId(value)) {
            builder.add(Filter.equals(column, value));
          }
        }
      }
    }
  }

  private static Filter getEmployeeFilter(BeeRow row, int index, String colName) {
    Long employee = row.getLong(index);

    if (DataUtils.isId(employee)) {
      return Filter.equals(colName, employee);
    } else {
      return null;
    }
  }

  private Filter getFilter(Collection<BeeRow> data, String employeeColumnName,
      Function<String, Filter> filterParser) {

    CompoundFilter include = Filter.or();
    CompoundFilter exclude = Filter.or();

    if (!BeeUtils.isEmpty(data)) {
      for (BeeRow row : data) {
        Filter filter = getFilter(row, employeeColumnName, filterParser);

        if (filter != null) {
          if (row.isTrue(filterIndexes.get(COL_ANALYSIS_FILTER_INCLUDE))) {
            include.add(filter);
          } else {
            exclude.add(filter);
          }
        }
      }
    }

    return AnalysisUtils.joinFilters(include, exclude);
  }

  private Filter getFilter(BeeRow row, String employeeColumnName,
      Function<String, Filter> filterParser) {

    CompoundFilter filter = Filter.and();

    filter.add(getEmployeeFilter(row, filterIndexes.get(COL_ANALYSIS_FILTER_EMPLOYEE),
        employeeColumnName));
    addDimensionFilters(filter, row, filterIndexes, null);

    if (filterParser != null) {
      String extraFilter = row.getString(filterIndexes.get(COL_ANALYSIS_FILTER_EXTRA));

      if (!BeeUtils.isEmpty(extraFilter)) {
        filter.add(filterParser.apply(extraFilter));
      }
    }

    return AnalysisUtils.normalize(filter);
  }

  private static MonthRange getRange(Integer yearFrom, Integer monthFrom,
      Integer yearUntil, Integer monthUntil, BiConsumer<String, String> errorConsumer) {

    MonthRange range = AnalysisUtils.getRange(yearFrom, monthFrom, yearUntil, monthUntil);

    if (range == null && errorConsumer != null) {
      errorConsumer.accept(AnalysisUtils.formatYearMonth(yearFrom, monthFrom),
          AnalysisUtils.formatYearMonth(yearUntil, monthUntil));
    }

    return range;
  }
}
