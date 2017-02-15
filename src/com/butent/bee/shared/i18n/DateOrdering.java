package com.butent.bee.shared.i18n;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public enum DateOrdering {
  YMD {
    @Override
    public JustDate date(int v1, int v2, int v3) {
      return new JustDate(TimeUtils.normalizeYear(v1), v2, v3);
    }

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
  },

  DMY {
    @Override
    public JustDate date(int v1, int v2, int v3) {
      return new JustDate(TimeUtils.normalizeYear(v3), v2, v1);
    }

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
        if (!mdSlices.isEmpty() && TimeUtils.isYear(BeeUtils.toInt(digits.substring(4)))) {
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
  },

  MDY {
    @Override
    public JustDate date(int v1, int v2, int v3) {
      return new JustDate(TimeUtils.normalizeYear(v3), v1, v2);
    }

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
        if (!mdSlices.isEmpty() && TimeUtils.isYear(BeeUtils.toInt(digits.substring(4)))) {
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
  };

  public static final DateOrdering DEFAULT = YMD;

  public abstract JustDate date(int v1, int v2, int v3);

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
}
