package com.butent.bee.server.modules.finance;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.butent.bee.server.utils.ScriptUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NonNullList;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.analysis.AnalysisSplitType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisSplitValue;
import com.butent.bee.shared.modules.finance.analysis.AnalysisUtils;
import com.butent.bee.shared.modules.finance.analysis.AnalysisValue;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

final class AnalysisScripting {

  private static BeeLogger logger = LogUtils.getLogger(AnalysisScripting.class);

  private static final String VAR_IS_BUDGET = "_b";
  private static final String VAR_CURRENT_VALUE = "_v";

  private static final String VAR_COLUMN = "_c";
  private static final String VAR_ROW = "_r";

  private static final Pattern currentValuePattern = getDetectionPattern(VAR_CURRENT_VALUE);

  private static final int ROOT_LEVEL = 0;

  private static final double MIN_VALUE = 1e-5;

  static Multimap<Integer, Long> buildSecondaryCalculationSequence(Collection<BeeRow> input,
      int indicatorIndex, int abbreviationIndex, int scriptIndex, Consumer<String> errorHandler) {

    Map<Long, String> scripts = new HashMap<>();
    Map<Long, Pattern> variables = new HashMap<>();

    if (!BeeUtils.isEmpty(input)) {
      for (BeeRow row : input) {
        if (!DataUtils.isId(row.getLong(indicatorIndex))) {
          String script = row.getString(scriptIndex);

          if (!BeeUtils.isEmpty(script) && !isScriptPrimary(script)) {
            scripts.put(row.getId(), script);

            String abbreviation = row.getString(abbreviationIndex);
            if (AnalysisUtils.isValidAbbreviation(abbreviation)) {
              variables.put(row.getId(), getDetectionPattern(abbreviation));
            }
          }
        }
      }
    }

    return buildSecondaryCalculationSequence(scripts, variables, errorHandler);
  }

  private static Multimap<Integer, Long> buildSecondaryCalculationSequence(
      Map<Long, String> scripts, Map<Long, Pattern> variables, Consumer<String> errorHandler) {

    Multimap<Integer, Long> result = ArrayListMultimap.create();
    if (scripts.isEmpty()) {
      return result;
    }

    if (variables.isEmpty()) {
      for (Long id : scripts.keySet()) {
        result.put(ROOT_LEVEL, id);
      }
      return result;
    }

    int level = ROOT_LEVEL;

    while (!scripts.isEmpty()) {
      Set<Long> ids = new HashSet<>(scripts.keySet());
      Set<Long> levelIds = new HashSet<>();

      for (Long id : ids) {
        String script = scripts.get(id);

        boolean free = true;
        for (Pattern pattern : variables.values()) {
          if (find(script, pattern)) {
            free = false;
            break;
          }
        }

        if (free) {
          levelIds.add(id);
        }
      }

      if (levelIds.isEmpty()) {
        break;
      }

      for (Long id : levelIds) {
        result.put(level, id);

        scripts.remove(id);
        variables.remove(id);
      }

      if (scripts.isEmpty()) {
        break;
      }

      level++;
    }

    if (!scripts.isEmpty() && errorHandler != null) {
      errorHandler.accept(BeeUtils.joinWords("recursive scripts:", scripts));
    }

    return result;
  }

  static Multimap<Integer, Long> buildIndicatorCalculationSequence(long indicator,
      Map<Long, String> scripts, Map<Long, Pattern> variables, Consumer<String> errorHandler) {

    Multimap<Integer, Long> result = ArrayListMultimap.create();
    if (!BeeUtils.containsKey(scripts, indicator)) {
      return result;
    }

    Set<Long> primaryIds = new HashSet<>();

    Set<Long> secondaryIds = new HashSet<>();
    secondaryIds.add(indicator);

    int level = scripts.size();

    while (!secondaryIds.isEmpty() && level >= 0) {
      Set<Long> nextIds = new HashSet<>();

      for (Long secondaryId : secondaryIds) {
        String script = scripts.get(secondaryId);

        for (Map.Entry<Long, Pattern> entry : variables.entrySet()) {
          if (find(script, entry.getValue())) {
            Long variableId = entry.getKey();

            if (Objects.equals(secondaryId, variableId)) {
              if (errorHandler != null) {
                errorHandler.accept(BeeUtils.joinWords("indicator", secondaryId,
                    script, "self reference", entry.getValue()));
              }

            } else if (scripts.containsKey(variableId)) {
              nextIds.add(variableId);

            } else {
              primaryIds.add(variableId);
            }
          }
        }
      }

      for (Long secondaryId : secondaryIds) {
        if (!nextIds.contains(secondaryId)) {
          if (result.containsValue(secondaryId)) {
            nextIds.add(secondaryId);
          } else {
            result.put(level, secondaryId);
          }
        }
      }

      if (nextIds.equals(secondaryIds)) {
        break;
      }

      secondaryIds.clear();
      secondaryIds.addAll(nextIds);

      level--;
    }

    if (!secondaryIds.isEmpty() && errorHandler != null) {
      secondaryIds.forEach(id -> errorHandler.accept(BeeUtils.joinWords(
          "recursive indicator", id, scripts.get(id), variables.get(id))));
    }

    if (!primaryIds.isEmpty()) {
      result.putAll(level, primaryIds);
    }

    return result;
  }

  static AnalysisValue calculateUnboundValue(ScriptEngine engine, String script,
      long columnId, String columnAbbreviation, long rowId, String rowAbbreviation,
      boolean needsActual, boolean needsBudget, ResponseObject errorCollector) {

    String actualValue;
    if (needsActual) {
      Bindings actualBindings = createActualBindings(engine);
      putColumnAndRow(actualBindings, columnAbbreviation, rowAbbreviation);

      actualValue = ScriptUtils.evalToString(engine, actualBindings, script, errorCollector);
    } else {
      actualValue = null;
    }

    String budgetValue;
    if (needsBudget) {
      Bindings budgetBindings = createBudgetBindings(engine);
      putColumnAndRow(budgetBindings, columnAbbreviation, rowAbbreviation);

      budgetValue = ScriptUtils.evalToString(engine, budgetBindings, script, errorCollector);
    } else {
      budgetValue = null;
    }

    if (BeeUtils.anyNotEmpty(actualValue, budgetValue)) {
      return AnalysisValue.of(columnId, rowId, actualValue, budgetValue);
    } else {
      return null;
    }
  }

  private static Set<AnalysisSplitType> getColumnSplitTypes(Collection<AnalysisValue> values) {
    Set<AnalysisSplitType> types = EnumSet.noneOf(AnalysisSplitType.class);
    if (values != null) {
      values.forEach(value -> types.addAll(value.getColumnSplitTypes()));
    }
    return types;
  }

  private static Set<AnalysisSplitType> getRowSplitTypes(Collection<AnalysisValue> values) {
    Set<AnalysisSplitType> types = EnumSet.noneOf(AnalysisSplitType.class);
    if (values != null) {
      values.forEach(value -> types.addAll(value.getRowSplitTypes()));
    }
    return types;
  }

  private static AnalysisValue calculateValue(ScriptEngine engine, String script,
      long columnId, String columnAbbreviation, long rowId, String rowAbbreviation,
      Map<AnalysisSplitType, AnalysisSplitValue> columnSplit,
      Map<AnalysisSplitType, AnalysisSplitValue> rowSplit,
      Collection<String> variables, Multimap<String, AnalysisValue> input,
      boolean needsActual, boolean needsBudget, ResponseObject errorCollector) {

    Map<String, Double> actualValues = new HashMap<>();
    Map<String, Double> budgetValues = new HashMap<>();

    variables.forEach(key -> {
      actualValues.put(key, BeeConst.DOUBLE_ZERO);
      budgetValues.put(key, BeeConst.DOUBLE_ZERO);

      if (input.containsKey(key)) {
        Collection<AnalysisValue> values = input.get(key);

        Map<AnalysisSplitType, AnalysisSplitValue> vcSplit = new HashMap<>();
        Map<AnalysisSplitType, AnalysisSplitValue> vrSplit = new HashMap<>();

        if (!BeeUtils.isEmpty(columnSplit)) {
          Set<AnalysisSplitType> csTypes = getColumnSplitTypes(values);

          columnSplit.forEach((k, v) -> {
            if (csTypes.contains(k)) {
              vcSplit.put(k, v);
            }
          });
        }

        if (!BeeUtils.isEmpty(rowSplit)) {
          Set<AnalysisSplitType> rsTypes = getRowSplitTypes(values);

          rowSplit.forEach((k, v) -> {
            if (rsTypes.contains(k)) {
              vrSplit.put(k, v);
            }
          });
        }

        values.forEach(value -> {
          if (value.containsColumnSplit(vcSplit) && value.containsRowSplit(vrSplit)) {
            if (needsActual && value.hasActualValue()) {
              actualValues.merge(key, value.getActualNumber(), Double::sum);
            }
            if (needsBudget && value.hasBudgetValue()) {
              budgetValues.merge(key, value.getBudgetNumber(), Double::sum);
            }
          }
        });
      }
    });

    Double actualValue;
    if (needsActual) {
      Bindings actualBindings = createActualBindings(engine);
      putColumnAndRow(actualBindings, columnAbbreviation, rowAbbreviation);
      actualBindings.putAll(actualValues);

      actualValue = ScriptUtils.evalToDouble(engine, actualBindings, script, errorCollector);
    } else {
      actualValue = null;
    }

    Double budgetValue;
    if (needsBudget) {
      Bindings budgetBindings = createBudgetBindings(engine);
      putColumnAndRow(budgetBindings, columnAbbreviation, rowAbbreviation);
      budgetBindings.putAll(budgetValues);

      budgetValue = ScriptUtils.evalToDouble(engine, budgetBindings, script, errorCollector);
    } else {
      budgetValue = null;
    }

    if (isValue(actualValue) || isValue(budgetValue)) {
      return AnalysisValue.of(columnId, rowId, columnSplit, rowSplit, actualValue, budgetValue);
    } else {
      return null;
    }
  }

  static List<AnalysisValue> calculateValues(ScriptEngine engine, String script,
      long columnId, String columnAbbreviation, long rowId, String rowAbbreviation,
      Collection<String> variables, Multimap<String, AnalysisValue> input,
      List<AnalysisSplitType> columnSplitTypes,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> columnSplitValues,
      List<AnalysisSplitType> rowSplitTypes,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> rowSplitValues,
      boolean needsActual, boolean needsBudget, ResponseObject errorCollector) {

    List<AnalysisValue> values = new NonNullList<>();

    boolean hasColumnSplits = !BeeUtils.isEmpty(columnSplitTypes)
        && !BeeUtils.isEmpty(columnSplitValues);
    boolean hasRowSplits = !BeeUtils.isEmpty(rowSplitTypes) && !BeeUtils.isEmpty(rowSplitValues);

    if (hasColumnSplits && hasRowSplits) {
      List<Map<AnalysisSplitType, AnalysisSplitValue>> columnPermutations =
          AnalysisSplitValue.getPermutations(null, columnSplitTypes, 0, columnSplitValues, 0);
      List<Map<AnalysisSplitType, AnalysisSplitValue>> rowPermutations =
          AnalysisSplitValue.getPermutations(null, rowSplitTypes, 0, rowSplitValues, 0);

      if (!columnPermutations.isEmpty() && !rowPermutations.isEmpty()) {
        columnPermutations.forEach(columnPermutation -> rowPermutations.forEach(rowPermutation ->
            values.add(calculateValue(engine, script,
                columnId, columnAbbreviation, rowId, rowAbbreviation,
                columnPermutation, rowPermutation,
                variables, input, needsActual, needsBudget, errorCollector))));
      }

    } else if (hasColumnSplits) {
      AnalysisSplitValue.getPermutations(null, columnSplitTypes, 0, columnSplitValues, 0)
          .forEach(permutation -> values.add(calculateValue(engine, script,
              columnId, columnAbbreviation, rowId, rowAbbreviation,
              permutation, null, variables, input, needsActual, needsBudget, errorCollector)));

    } else if (hasRowSplits) {
      AnalysisSplitValue.getPermutations(null, rowSplitTypes, 0, rowSplitValues, 0)
          .forEach(permutation -> values.add(calculateValue(engine, script,
              columnId, columnAbbreviation, rowId, rowAbbreviation,
              null, permutation, variables, input, needsActual, needsBudget, errorCollector)));

    } else {
      values.add(calculateValue(engine, script,
          columnId, columnAbbreviation, rowId, rowAbbreviation, null, null,
          variables, input, needsActual, needsBudget, errorCollector));
    }

    return values;
  }

  private static AnalysisValue calculateSecondaryIndicator(ScriptEngine engine, String script,
      long columnId, long rowId,
      Map<AnalysisSplitType, AnalysisSplitValue> columnSplit,
      Map<AnalysisSplitType, AnalysisSplitValue> rowSplit,
      Collection<String> variables, Multimap<String, AnalysisValue> input,
      ResponseObject errorCollector) {

    Map<String, Double> values = new HashMap<>();

    variables.forEach(key -> values.put(key, BeeConst.DOUBLE_ZERO));

    input.forEach((key, value) -> {
      if (value.containsColumnSplit(columnSplit) && value.containsRowSplit(rowSplit)) {
        values.merge(key, value.getActualNumber(), Double::sum);
      }
    });

    Bindings bindings;
    if (values.isEmpty()) {
      bindings = null;
    } else {
      bindings = engine.createBindings();
      bindings.putAll(values);
    }

    Double actualValue = ScriptUtils.evalToDouble(engine, bindings, script, errorCollector);

    if (isValue(actualValue)) {
      return AnalysisValue.of(columnId, rowId, columnSplit, rowSplit, actualValue, null);
    } else {
      return null;
    }
  }

  static List<AnalysisValue> calculateSecondaryIndicator(ScriptEngine engine, String script,
      long columnId, long rowId,
      Collection<String> variables, Multimap<String, AnalysisValue> input,
      List<AnalysisSplitType> columnSplitTypes,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> columnSplitValues,
      List<AnalysisSplitType> rowSplitTypes,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> rowSplitValues,
      ResponseObject errorCollector) {

    List<AnalysisValue> values = new NonNullList<>();

    boolean hasColumnSplits = !BeeUtils.isEmpty(columnSplitTypes)
        && !BeeUtils.isEmpty(columnSplitValues);
    boolean hasRowSplits = !BeeUtils.isEmpty(rowSplitTypes) && !BeeUtils.isEmpty(rowSplitValues);

    if (hasColumnSplits && hasRowSplits) {
      List<Map<AnalysisSplitType, AnalysisSplitValue>> columnPermutations =
          AnalysisSplitValue.getPermutations(null, columnSplitTypes, 0, columnSplitValues, 0);
      List<Map<AnalysisSplitType, AnalysisSplitValue>> rowPermutations =
          AnalysisSplitValue.getPermutations(null, rowSplitTypes, 0, rowSplitValues, 0);

      if (!columnPermutations.isEmpty() && !rowPermutations.isEmpty()) {
        columnPermutations.forEach(columnPermutation -> rowPermutations.forEach(rowPermutation ->
            values.add(calculateSecondaryIndicator(engine, script, columnId, rowId,
                columnPermutation, rowPermutation, variables, input, errorCollector))));
      }

    } else if (hasColumnSplits) {
      AnalysisSplitValue.getPermutations(null, columnSplitTypes, 0, columnSplitValues, 0)
          .forEach(permutation -> values.add(calculateSecondaryIndicator(engine, script,
              columnId, rowId, permutation, null, variables, input, errorCollector)));

    } else if (hasRowSplits) {
      AnalysisSplitValue.getPermutations(null, rowSplitTypes, 0, rowSplitValues, 0)
          .forEach(permutation -> values.add(calculateSecondaryIndicator(engine, script,
              columnId, rowId, null, permutation, variables, input, errorCollector)));

    } else {
      values.add(calculateSecondaryIndicator(engine, script, columnId, rowId, null, null,
          variables, input, errorCollector));
    }

    return values;
  }

  static Bindings createActualBindings(ScriptEngine engine) {
    return createActualOrBudgetBindings(engine, false);
  }

  static Bindings createBudgetBindings(ScriptEngine engine) {
    return createActualOrBudgetBindings(engine, true);
  }

  private static Bindings createActualOrBudgetBindings(ScriptEngine engine, boolean isBudget) {
    Bindings bindings = (engine == null) ? null : engine.createBindings();
    if (bindings != null) {
      bindings.put(VAR_IS_BUDGET, isBudget);
    }
    return bindings;
  }

  static boolean containsVariable(String script, String variable) {
    return find(script, getDetectionPattern(variable));
  }

  private static boolean find(String s, Pattern p) {
    if (s == null || p == null) {
      return false;
    } else {
      return p.matcher(s).find();
    }
  }

  static Pattern getDetectionPattern(String variable) {
    return BeeUtils.isEmpty(variable) ? null : Pattern.compile("\\b" + variable.trim() + "\\b");
  }

  static boolean isScriptPrimary(String script) {
    return find(script, currentValuePattern);
  }

  static void putCurrentValue(Bindings bindings, double value) {
    bindings.put(VAR_CURRENT_VALUE, value);
  }

  static void putColumnAndRow(Bindings bindings, String column, String row) {
    bindings.put(VAR_COLUMN, BeeUtils.trim(column));
    bindings.put(VAR_ROW, BeeUtils.trim(row));
  }

  static List<String> validateAnalysisScripts(Collection<BeeRow> input,
      int indicatorIndex, int abbreviationIndex, int scriptIndex,
      Function<BeeRow, String> labelFunction) {

    List<String> messages = new ArrayList<>();

    if (!BeeUtils.isEmpty(input)
        && input.stream().anyMatch(row -> !row.isEmpty(scriptIndex))) {

      ScriptEngine engine = ScriptUtils.getEngine();
      if (engine == null) {
        messages.add("script engine not available");

      } else {
        Map<Long, String> variables = new HashMap<>();

        for (BeeRow row : input) {
          String abbreviation = row.getString(abbreviationIndex);

          if (AnalysisUtils.isValidAbbreviation(abbreviation)) {
            variables.put(row.getId(), abbreviation);
          }
        }

        Bindings primaryBindings = createActualBindings(engine);
        putCurrentValue(primaryBindings, BeeConst.DOUBLE_ZERO);
        putColumnAndRow(primaryBindings, null, null);

        Bindings bindings;

        for (BeeRow row : input) {
          String script = row.getString(scriptIndex);

          if (!BeeUtils.isEmpty(script)) {
            boolean primary = DataUtils.isId(row.getLong(indicatorIndex))
                || isScriptPrimary(script);

            if (primary) {
              bindings = primaryBindings;

            } else {
              bindings = createActualBindings(engine);
              putColumnAndRow(bindings, null, null);

              for (Map.Entry<Long, String> entry : variables.entrySet()) {
                if (!Objects.equals(entry.getKey(), row.getId())) {
                  bindings.put(entry.getValue(), BeeConst.DOUBLE_ZERO);
                }
              }
            }

            try {
              engine.eval(script, bindings);

            } catch (ScriptException ex) {
              String label = labelFunction.apply(row);

              logger.severe(label, script, bindings, ex.getMessage());
              messages.add(BeeUtils.joinWords(label, ex.getMessage()));
            }
          }
        }
      }

      if (messages.isEmpty()) {
        buildSecondaryCalculationSequence(input, indicatorIndex, abbreviationIndex, scriptIndex,
            messages::add);
      }
    }

    return messages;
  }

  static List<String> validateIndicatorScript(long indicator, String name, String script,
      Map<Long, String> variables) {

    List<String> messages = new ArrayList<>();

    ScriptEngine engine = ScriptUtils.getEngine();
    if (engine == null) {
      messages.add(BeeUtils.joinWords(name, "script engine not available"));

    } else {
      Bindings bindings;

      if (BeeUtils.isEmpty(variables)
          || variables.containsKey(indicator) && variables.size() == 1) {

        bindings = null;

      } else {
        bindings = engine.createBindings();

        for (Map.Entry<Long, String> entry : variables.entrySet()) {
          if (!Objects.equals(entry.getKey(), indicator)) {
            bindings.put(entry.getValue(), BeeConst.DOUBLE_ZERO);
          }
        }
      }

      try {
        if (bindings == null) {
          engine.eval(script);
        } else {
          engine.eval(script, bindings);
        }

      } catch (ScriptException ex) {
        logger.severe(name, script, bindings, ex.getMessage());
        messages.add(BeeUtils.joinWords(name, ex.getMessage()));
      }
    }

    return messages;
  }

  static Multimap<String, AnalysisValue> transformInput(Multimap<Long, AnalysisValue> values,
      Map<Long, String> variables) {

    Multimap<String, AnalysisValue> result = ArrayListMultimap.create();

    for (long id : values.keySet()) {
      String variable = variables.get(id);
      if (!BeeUtils.isEmpty(variable)) {
        result.putAll(variable, values.get(id));
      }
    }

    return result;
  }

  private static boolean isValue(Double value) {
    return BeeUtils.isDouble(value) && Math.abs(value) >= MIN_VALUE;
  }

  private AnalysisScripting() {
  }
}
