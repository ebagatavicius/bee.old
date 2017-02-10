package com.butent.bee.shared.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

public class AnalysisLabel {

  public static AnalysisLabel period(String text) {
    return BeeUtils.isEmpty(text) ? null : new AnalysisLabel(SOURCE_PERIOD, text);
  }

  public static AnalysisLabel indicator(BeeRow row, Map<String, Integer> indexes, String source) {
    if (DataUtils.isId(row.getLong(indexes.get(source)))) {
      String text = row.getString(indexes.get(COL_FIN_INDICATOR_NAME));

      if (!BeeUtils.isEmpty(text)) {
        return new AnalysisLabel(source, text,
            row.getString(indexes.get(ALS_INDICATOR_BACKGROUND)),
            row.getString(indexes.get(ALS_INDICATOR_FOREGROUND)));
      }
    }
    return null;
  }

  public static AnalysisLabel turnoverOrBalance(BeeRow row, Map<String, Integer> indexes,
      String source) {

    TurnoverOrBalance tob = row.getEnum(indexes.get(source), TurnoverOrBalance.class);
    return (tob == null) ? null : new AnalysisLabel(source, tob.getCaption());
  }

  public static AnalysisLabel budgetType(BeeRow row, Map<String, Integer> indexes, String source) {
    if (DataUtils.isId(row.getLong(indexes.get(source)))) {
      String text = row.getString(indexes.get(COL_BUDGET_TYPE_NAME));

      if (!BeeUtils.isEmpty(text)) {
        return new AnalysisLabel(source, text,
            row.getString(indexes.get(ALS_BUDGET_TYPE_BACKGROUND)),
            row.getString(indexes.get(ALS_BUDGET_TYPE_FOREGROUND)));
      }
    }
    return null;
  }

  public static AnalysisLabel dimension(BeeRow row, Map<String, Integer> indexes, int ordinal) {
    String source = Dimensions.getRelationColumn(ordinal);

    if (DataUtils.isId(row.getLong(indexes.get(source)))) {
      String text = row.getString(indexes.get(Dimensions.getNameColumn(ordinal)));

      if (!BeeUtils.isEmpty(text)) {
        return new AnalysisLabel(source, text,
            row.getString(indexes.get(Dimensions.getBackgroundColumn(ordinal))),
            row.getString(indexes.get(Dimensions.getForegroundColumn(ordinal))));
      }
    }
    return null;
  }

  public static AnalysisLabel employee(BeeRow row, Map<String, Integer> indexes, String source) {
    if (DataUtils.isId(row.getLong(indexes.get(source)))) {
      String text = BeeUtils.joinWords(
          row.getString(indexes.get(ALS_EMPLOYEE_FIRST_NAME)),
          row.getString(indexes.get(ALS_EMPLOYEE_LAST_NAME)));

      if (!BeeUtils.isEmpty(text)) {
        return new AnalysisLabel(source, text);
      }
    }
    return null;
  }

  public static AnalysisLabel currency(BeeRow row, Map<String, Integer> indexes, String source) {
    if (DataUtils.isId(row.getLong(indexes.get(source)))) {
      String text = row.getString(indexes.get(AdministrationConstants.ALS_CURRENCY_NAME));

      if (!BeeUtils.isEmpty(text)) {
        return new AnalysisLabel(source, text);
      }
    }
    return null;
  }

  public static AnalysisLabel value(BeeRow row, Map<String, Integer> indexes, String source) {
    String text = row.getString(indexes.get(source));
    return BeeUtils.isEmpty(text) ? null : new AnalysisLabel(source, text);
  }

  public static AnalysisLabel value(BeeRow row, Map<String, Integer> indexes, String source,
      String bgSource, String fgSource) {

    String text = row.getString(indexes.get(source));

    if (BeeUtils.isEmpty(text)) {
      return null;
    } else {
      return new AnalysisLabel(source, text,
          row.getString(indexes.get(bgSource)), row.getString(indexes.get(fgSource)));
    }
  }

  private static final String SOURCE_PERIOD = "Period";

  private final String source;
  private final String text;

  private final String background;
  private final String foreground;

  public AnalysisLabel(String source, String text) {
    this(source, text, null, null);
  }

  public AnalysisLabel(String source, String text, String background, String foreground) {
    this.source = source;
    this.text = text;

    this.background = background;
    this.foreground = foreground;
  }

  public String getSource() {
    return source;
  }

  public String getText() {
    return text;
  }

  public String getBackground() {
    return background;
  }

  public String getForeground() {
    return foreground;
  }

  public boolean isPeriod() {
    return SOURCE_PERIOD.equals(source);
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(source, text, background, foreground);
  }
}
