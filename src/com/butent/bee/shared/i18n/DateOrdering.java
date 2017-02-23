package com.butent.bee.shared.i18n;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public enum DateOrdering {
  YMD {
    @Override
    public int month(int v1, int v2) {
      return v1;
    }

    @Override
    public int day(int v1, int v2) {
      return v2;
    }

    @Override
    List<Integer> getDateSlices(String digits, int len) {
      List<Integer> slices = new ArrayList<>();

      if (len == 3) {
        int v1 = BeeUtils.toInt(digits.substring(0, 1));
        int v2 = BeeUtils.toInt(digits.substring(1));

        if (TimeUtils.isMonth(v1) && TimeUtils.maybeDom(v2)) {
          slices.add(1);
          slices.add(2);

        } else {
          v1 = BeeUtils.toInt(digits.substring(0, 2));
          v2 = BeeUtils.toInt(digits.substring(2));

          if (TimeUtils.isMonth(v1) && TimeUtils.maybeDom(v2)) {
            slices.add(2);
            slices.add(1);
          }
        }

      } else if (len == 5) {
        List<Integer> mdSlices = getDateSlices(digits.substring(2), 3);
        if (!mdSlices.isEmpty()) {
          slices.add(2);
          slices.addAll(mdSlices);
        }

      } else if (len == 7) {
        List<Integer> mdSlices = getDateSlices(digits.substring(4), 3);
        if (!mdSlices.isEmpty() && TimeUtils.isYear(BeeUtils.toInt(digits.substring(0, 4)))) {
          slices.add(4);
          slices.addAll(mdSlices);
        }

      } else if (len >= 8) {
        slices.add(4);
        slices.add(2);
        slices.add(2);
      }

      return slices;
    }

    @Override
    Position yearPosition() {
      return Position.A;
    }

    @Override
    Position monthPosition() {
      return Position.B;
    }

    @Override
    Position dayPosition() {
      return Position.C;
    }
  },

  DMY {
    @Override
    public int month(int v1, int v2) {
      return v2;
    }

    @Override
    public int day(int v1, int v2) {
      return v1;
    }

    @Override
    List<Integer> getDateSlices(String digits, int len) {
      List<Integer> slices = new ArrayList<>();

      if (len == 3) {
        int v1 = BeeUtils.toInt(digits.substring(0, 2));
        int v2 = BeeUtils.toInt(digits.substring(2));

        if (TimeUtils.maybeDom(v1) && TimeUtils.isMonth(v2)) {
          slices.add(2);
          slices.add(1);

        } else {
          v1 = BeeUtils.toInt(digits.substring(0, 1));
          v2 = BeeUtils.toInt(digits.substring(1));

          if (TimeUtils.maybeDom(v1) && TimeUtils.isMonth(v2)) {
            slices.add(1);
            slices.add(2);
          }
        }

      } else if (len == 5) {
        List<Integer> mdSlices = getDateSlices(digits.substring(0, 3), 3);
        if (!mdSlices.isEmpty()) {
          slices.addAll(mdSlices);
          slices.add(2);
        }

      } else if (len == 7) {
        List<Integer> mdSlices = getDateSlices(digits.substring(0, 3), 3);
        if (!mdSlices.isEmpty() && TimeUtils.isYear(BeeUtils.toInt(digits.substring(3)))) {
          slices.addAll(mdSlices);
          slices.add(4);
        }

      } else if (len >= 8) {
        slices.add(2);
        slices.add(2);
        slices.add(4);
      }

      return slices;
    }

    @Override
    Position yearPosition() {
      return Position.C;
    }

    @Override
    Position monthPosition() {
      return Position.B;
    }

    @Override
    Position dayPosition() {
      return Position.A;
    }
  },

  MDY {
    @Override
    public int month(int v1, int v2) {
      return v1;
    }

    @Override
    public int day(int v1, int v2) {
      return v2;
    }

    @Override
    List<Integer> getDateSlices(String digits, int len) {
      List<Integer> slices = new ArrayList<>();

      if (len == 3) {
        slices.addAll(YMD.getDateSlices(digits, len));

      } else if (len == 5) {
        List<Integer> mdSlices = getDateSlices(digits.substring(0, 3), 3);
        if (!mdSlices.isEmpty()) {
          slices.addAll(mdSlices);
          slices.add(2);
        }

      } else if (len == 7) {
        List<Integer> mdSlices = getDateSlices(digits.substring(0, 3), 3);
        if (!mdSlices.isEmpty() && TimeUtils.isYear(BeeUtils.toInt(digits.substring(3)))) {
          slices.addAll(mdSlices);
          slices.add(4);
        }

      } else if (len >= 8) {
        slices.add(2);
        slices.add(2);
        slices.add(4);
      }

      return slices;
    }

    @Override
    Position yearPosition() {
      return Position.C;
    }

    @Override
    Position monthPosition() {
      return Position.A;
    }

    @Override
    Position dayPosition() {
      return Position.B;
    }
  };

  public static final DateOrdering DEFAULT = YMD;

  private enum Position {
    A, B, C
  }

  public JustDate date(int v1, int v2, int v3) {
    EnumMap<Position, Integer> input = new EnumMap<>(Position.class);
    input.put(Position.A, v1);
    input.put(Position.B, v2);
    input.put(Position.C, v3);

    List<Position> yearCandidates = new ArrayList<>();
    List<Position> monthCandidates = new ArrayList<>();
    List<Position> dayCandidates = new ArrayList<>();

    input.forEach((p, v) -> {
      if (TimeUtils.isYear(v)) {
        yearCandidates.add(p);

      } else {
        if (TimeUtils.isMonth(v)) {
          monthCandidates.add(p);
        }
        if (TimeUtils.maybeDom(v)) {
          dayCandidates.add(p);
        }
      }
    });

    if (yearCandidates.size() > 1) {
      return null;
    }
    if (yearCandidates.isEmpty()) {
      Position p = yearPosition();
      yearCandidates.add(p);

      monthCandidates.remove(p);
      dayCandidates.remove(p);
    }

    int y = input.get(yearCandidates.get(0));

    int mc = monthCandidates.size();
    int dc = dayCandidates.size();

    if (mc == 1 && dc > 1) {
      dayCandidates.remove(monthCandidates.get(0));
      dc = dayCandidates.size();
    }
    if (dc == 1 && mc > 1) {
      monthCandidates.remove(dayCandidates.get(0));
      mc = monthCandidates.size();
    }

    if (mc == 1 && dc == 1 && monthCandidates.equals(dayCandidates)) {
      Position p = monthCandidates.get(0);

      if (p == monthPosition()) {
        dayCandidates.clear();
        dc = 0;
      } else if (p == dayPosition()) {
        monthCandidates.clear();
        mc = 0;
      }
    }

    int m;
    int d;

    if (mc <= 0) {
      m = 1;
    } else if (mc == 1) {
      m = input.get(monthCandidates.get(0));
    } else {
      m = month(input.get(monthCandidates.get(0)), input.get(monthCandidates.get(1)));
    }

    if (dc <= 0) {
      d = 1;
    } else if (dc == 1) {
      d = input.get(dayCandidates.get(0));
    } else {
      d = day(input.get(dayCandidates.get(0)), input.get(dayCandidates.get(1)));
    }

    return new JustDate(TimeUtils.normalizeYear(y), m, d);
  }

  public DateTime dateTime(int v1, int v2, int v3, int hour, int minute, int second, long millis) {
    return new DateTime(date(v1, v2, v3), hour, minute, second, millis);
  }

  public abstract int month(int v1, int v2);

  public abstract int day(int v1, int v2);

  public List<Integer> getDateSplitLengths(String digits) {
    if (digits == null) {
      return new ArrayList<>();
    }

    int len = digits.length();

    List<Integer> slices = getDateSlices(digits, len);
    if (!slices.isEmpty()) {
      return slices;
    }

    slices = new ArrayList<>();

    switch (len) {
      case 1:
        slices.add(1);
        break;

      case 2:
        slices.add(1);
        slices.add(1);
        break;

      case 4:
        slices.add(2);
        slices.add(2);
        break;

      case 6:
        slices.add(2);
        slices.add(2);
        slices.add(2);
        break;
    }

    return slices;
  }

  abstract List<Integer> getDateSlices(String digits, int len);

  abstract Position yearPosition();

  abstract Position monthPosition();

  abstract Position dayPosition();
}
