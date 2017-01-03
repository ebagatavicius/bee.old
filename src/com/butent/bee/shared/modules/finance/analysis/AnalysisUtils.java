package com.butent.bee.shared.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AnalysisUtils {

  public static String formatYearMonth(Integer year, Integer month) {
    String y = (year == null) ? null : TimeUtils.yearToString(year);
    String m = (month == null) ? null : TimeUtils.monthToString(month);

    return BeeUtils.join(BeeConst.STRING_POINT, y, m);
  }

  public static Filter getFilter(String column, MonthRange range) {
    if (range == null) {
      return null;
    }

    YearMonth minYm = range.getMinMonth();
    YearMonth maxYm = range.getMaxMonth();

    DateTimeValue minDt;
    if (minYm != null && BeeUtils.isMore(minYm, ANALYSIS_MIN_YEAR_MONTH)) {
      minDt = new DateTimeValue(minYm.getDate().getDateTime());
    } else {
      minDt = null;
    }

    DateTimeValue maxDt;
    if (maxYm != null && BeeUtils.isLess(maxYm, ANALYSIS_MAX_YEAR_MONTH)) {
      maxDt = new DateTimeValue(maxYm.nextMonth().getDate().getDateTime());
    } else {
      maxDt = null;
    }

    if (minDt != null && maxDt != null) {
      return Filter.and(Filter.isMoreEqual(column, minDt), Filter.isLess(column, maxDt));

    } else if (minDt != null) {
      return Filter.isMoreEqual(column, minDt);

    } else if (maxDt != null) {
      return Filter.isLess(column, maxDt);

    } else {
      return null;
    }
  }

  public static Map<String, Integer> getIndexes(BeeRowSet rowSet) {
    Map<String, Integer> indexes = new HashMap<>();

    if (rowSet != null) {
      for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
        indexes.put(rowSet.getColumn(i).getId(), i);
      }
    }

    return indexes;
  }

  public static MonthRange getRange(Integer yearFrom, Integer monthFrom,
      Integer yearUntil, Integer monthUntil) {

    if (isValidRange(yearFrom, monthFrom, yearUntil, monthUntil)) {
      Integer y1 = (yearFrom == null) ? Integer.valueOf(ANALYSIS_MIN_YEAR) : yearFrom;
      Integer y2 = (yearUntil == null && monthUntil == null)
          ? Integer.valueOf(ANALYSIS_MAX_YEAR) : yearUntil;

      return MonthRange.closed(y1, monthFrom, y2, monthUntil);

    } else {
      return null;
    }
  }

  public static MonthRange intersection(MonthRange first, MonthRange second) {
    if (first == null) {
      return second;
    } else if (second == null) {
      return first;
    } else {
      return first.intersection(second);
    }
  }

  public static boolean isValidAbbreviation(String input) {
    return NameUtils.isIdentifier(input);
  }

  public static boolean isValidRange(Integer yearFrom, Integer monthFrom,
      Integer yearUntil, Integer monthUntil) {

    if (yearFrom == null) {
      if (monthFrom != null) {
        return false;
      }

    } else if (isValidYear(yearFrom)) {
      if (monthFrom != null && !TimeUtils.isMonth(monthFrom)) {
        return false;
      }

    } else {
      return false;
    }

    if (yearUntil == null) {
      if (monthUntil == null) {
        return true;

      } else if (TimeUtils.isMonth(monthUntil)) {
        if (yearFrom == null) {
          return false;
        } else if (monthFrom == null) {
          return true;
        } else {
          return BeeUtils.isLeq(monthFrom, monthUntil);
        }

      } else {
        return false;
      }

    } else if (isValidYear(yearUntil)) {
      if (yearFrom != null && BeeUtils.isMore(yearFrom, yearUntil)) {
        return false;
      }

      if (monthUntil == null) {
        return true;

      } else if (TimeUtils.isMonth(monthUntil)) {
        if (yearFrom == null || monthFrom == null) {
          return true;
        } else if (Objects.equals(yearFrom, yearUntil)) {
          return BeeUtils.isLeq(monthFrom, monthUntil);
        } else {
          return true;
        }

      } else {
        return false;
      }

    } else {
      return false;
    }
  }

  private static boolean isValidYear(int year) {
    return year >= ANALYSIS_MIN_YEAR && year <= ANALYSIS_MAX_YEAR;
  }

  public static Filter joinFilters(CompoundFilter include, CompoundFilter exclude) {
    if (include.isEmpty() && exclude.isEmpty()) {
      return null;

    } else if (exclude.isEmpty()) {
      return include;

    } else if (include.isEmpty()) {
      return Filter.isNot(exclude);

    } else {
      return Filter.and(include, Filter.isNot(exclude));
    }
  }

  public static Filter normalize(CompoundFilter filter) {
    if (filter == null || filter.isEmpty()) {
      return null;
    } else {
      return filter;
    }
  }

  private AnalysisUtils() {
  }
}