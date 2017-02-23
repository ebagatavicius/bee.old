package com.butent.bee.shared.i18n;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestDateOrdering {

  @Test
  public void testDate() {
    JustDate d1 = new JustDate(2017, 2, 16);
    JustDate d2 = new JustDate(2015, 1, 5);
    JustDate d3 = new JustDate(2009, 12, 1);
    JustDate d4 = new JustDate(2000, 1, 1);

    DateOrdering o = DateOrdering.YMD;
    assertEquals(d1, o.date(17, 2, 16));
    assertEquals(d1, o.date(2017, 2, 16));
    assertEquals(d1, o.date(17, 16, 2));
    assertEquals(d1, o.date(16, 2017, 2));
    assertEquals(d1, o.date(2, 2017, 16));
    assertEquals(d1, o.date(16, 2, 2017));
    assertEquals(d1, o.date(2, 16, 2017));

    assertEquals(new JustDate(2002, 1, 17), o.date(2, 16, 17));
    assertNull(o.date(2017, 1916, 17));
    assertEquals(d2, o.date(15, 0, 5));
    assertEquals(d2, o.date(15, 1, 5));
    assertEquals(d2, o.date(15, 32, 5));
    assertEquals(d3, o.date(9, 12, 0));
    assertEquals(d3, o.date(9, 12, 1));
    assertEquals(d3, o.date(9, 12, 32));

    assertEquals(d4, o.date(0, 1, 1));
    assertEquals(d4, o.date(0, 1, 0));
    assertEquals(d4, o.date(0, 0, 1));
    assertEquals(d4, o.date(0, 0, 0));
    assertEquals(d4, o.date(0, 32, 32));

    o = DateOrdering.DMY;
    assertEquals(d1, o.date(16, 2, 17));
    assertEquals(d1, o.date(2017, 2, 16));
    assertEquals(d1, o.date(2, 16, 17));
    assertEquals(d1, o.date(16, 2017, 2));
    assertEquals(d1, o.date(2, 2017, 16));
    assertEquals(d1, o.date(16, 2, 2017));
    assertEquals(d1, o.date(2, 16, 2017));

    assertEquals(new JustDate(2002, 1, 17), o.date(17, 16, 2));
    assertNull(o.date(2017, 1, 2017));
    assertEquals(d2, o.date(5, 0, 15));
    assertEquals(d2, o.date(5, 1, 15));
    assertEquals(d2, o.date(5, 32, 15));
    assertEquals(d3, o.date(0, 12, 9));
    assertEquals(d3, o.date(1, 12, 9));
    assertEquals(d3, o.date(32, 12, 9));

    assertEquals(d4, o.date(1, 1, 0));
    assertEquals(d4, o.date(0, 1, 0));
    assertEquals(d4, o.date(1, 0, 0));
    assertEquals(d4, o.date(0, 0, 0));
    assertEquals(d4, o.date(32, 32, 0));

    o = DateOrdering.MDY;
    assertEquals(d1, o.date(16, 2, 17));
    assertEquals(d1, o.date(2017, 2, 16));
    assertEquals(d1, o.date(2, 16, 17));
    assertEquals(d1, o.date(16, 2017, 2));
    assertEquals(d1, o.date(2, 2017, 16));
    assertEquals(d1, o.date(16, 2, 2017));
    assertEquals(d1, o.date(2, 16, 2017));

    assertEquals(new JustDate(2002, 1, 17), o.date(16, 17, 2));
    assertNull(o.date(0, 2017, 2017));
    assertEquals(d2, o.date(0, 5, 15));
    assertEquals(d2, o.date(1, 5, 15));
    assertEquals(d2, o.date(32, 5, 15));
    assertEquals(d3, o.date(12, 0, 9));
    assertEquals(d3, o.date(12, 1, 9));
    assertEquals(d3, o.date(12, 32, 9));

    assertEquals(d4, o.date(1, 1, 0));
    assertEquals(d4, o.date(0, 1, 0));
    assertEquals(d4, o.date(1, 0, 0));
    assertEquals(d4, o.date(0, 0, 0));
    assertEquals(d4, o.date(32, 32, 0));
  }

  @Test
  public void testDateTime() {
    DateTime dt = new DateTime(2017, 2, 18, 11, 24, 45, 789);

    assertEquals(dt, DateOrdering.YMD.dateTime(17, 2, 18, 11, 24, 45, 789));
    assertEquals(dt, DateOrdering.DMY.dateTime(18, 2, 17, 11, 24, 45, 789));
    assertEquals(dt, DateOrdering.MDY.dateTime(2, 18, 17, 11, 24, 45, 789));
  }

  @Test
  public void testMonth() {
    assertEquals(1, DateOrdering.YMD.month(1, 2));
    assertEquals(-1, DateOrdering.YMD.month(-1, 0));
    assertEquals(4, DateOrdering.DMY.month(3, 4));
    assertEquals(3, DateOrdering.MDY.month(3, 4));
  }

  @Test
  public void testDay() {
    assertEquals(2, DateOrdering.YMD.day(1, 2));
    assertEquals(0, DateOrdering.YMD.day(-1, 0));
    assertEquals(3, DateOrdering.DMY.day(3, 4));
    assertEquals(4, DateOrdering.MDY.day(3, 4));
  }

  @Test
  public void testGetDateSplitLengths() {
    String s0 = "";
    String s1 = "1";
    String s2 = "12";

    String s3a = "123";
    String s3b = "218";
    String s3c = "182";
    String s3d = "034";
    String s3e = "999";

    String s4 = "1234";
    String s5 = "12312";
    String s6 = "123123";
    String s7 = "1231231";
    String s8 = "12312312";
    String s9 = "123123123";

    String y2 = "17";
    String y4 = "2017";

    List<Integer> r0 = Collections.emptyList();
    List<Integer> r1 = Collections.singletonList(1);
    List<Integer> r2 = Arrays.asList(1, 1);

    List<Integer> r3x = Arrays.asList(1, 2);
    List<Integer> r3y = Arrays.asList(2, 1);

    List<Integer> r4 = Arrays.asList(2, 2);
    List<Integer> r6 = Arrays.asList(2, 2, 2);

    DateOrdering o = DateOrdering.YMD;
    List<Integer> r5x = Arrays.asList(2, 1, 2);
    List<Integer> r5y = Arrays.asList(2, 2, 1);
    List<Integer> r7x = Arrays.asList(4, 1, 2);
    List<Integer> r7y = Arrays.asList(4, 2, 1);
    List<Integer> r8 = Arrays.asList(4, 2, 2);

    assertEquals(r0, o.getDateSplitLengths(null));
    assertEquals(r0, o.getDateSplitLengths(s0));
    assertEquals(r1, o.getDateSplitLengths(s1));
    assertEquals(r2, o.getDateSplitLengths(s2));

    assertEquals(r3x, o.getDateSplitLengths(s3a));
    assertEquals(r3x, o.getDateSplitLengths(s3b));
    assertEquals(r0, o.getDateSplitLengths(s3c));
    assertEquals(r3y, o.getDateSplitLengths(s3d));
    assertEquals(r0, o.getDateSplitLengths(s3e));

    assertEquals(r4, o.getDateSplitLengths(s4));

    assertEquals(r5x, o.getDateSplitLengths(s5));
    assertEquals(r5x, o.getDateSplitLengths(y2 + s3a));
    assertEquals(r5x, o.getDateSplitLengths(y2 + s3b));
    assertEquals(r0, o.getDateSplitLengths(y2 + s3c));
    assertEquals(r5y, o.getDateSplitLengths(y2 + s3d));
    assertEquals(r0, o.getDateSplitLengths(y2 + s3e));

    assertEquals(r6, o.getDateSplitLengths(s6));

    assertEquals(r0, o.getDateSplitLengths(s7));
    assertEquals(r7x, o.getDateSplitLengths(y4 + s3a));
    assertEquals(r7x, o.getDateSplitLengths(y4 + s3b));
    assertEquals(r0, o.getDateSplitLengths(y4 + s3c));
    assertEquals(r7y, o.getDateSplitLengths(y4 + s3d));
    assertEquals(r0, o.getDateSplitLengths(y4 + s3e));

    assertEquals(r8, o.getDateSplitLengths(s8));
    assertEquals(r8, o.getDateSplitLengths(s9));

    o = DateOrdering.DMY;
    r5y = Arrays.asList(2, 1, 2);
    r7y = Arrays.asList(2, 1, 4);
    r8 = Arrays.asList(2, 2, 4);

    assertEquals(r0, o.getDateSplitLengths(null));
    assertEquals(r0, o.getDateSplitLengths(s0));
    assertEquals(r1, o.getDateSplitLengths(s1));
    assertEquals(r2, o.getDateSplitLengths(s2));

    assertEquals(r3y, o.getDateSplitLengths(s3a));
    assertEquals(r3y, o.getDateSplitLengths(s3b));
    assertEquals(r3y, o.getDateSplitLengths(s3c));
    assertEquals(r3y, o.getDateSplitLengths(s3d));
    assertEquals(r0, o.getDateSplitLengths(s3e));

    assertEquals(r4, o.getDateSplitLengths(s4));

    assertEquals(r5y, o.getDateSplitLengths(s5));
    assertEquals(r5y, o.getDateSplitLengths(s3a + y2));
    assertEquals(r5y, o.getDateSplitLengths(s3b + y2));
    assertEquals(r5y, o.getDateSplitLengths(s3c + y2));
    assertEquals(r5y, o.getDateSplitLengths(s3d + y2));
    assertEquals(r0, o.getDateSplitLengths(s3e + y2));

    assertEquals(r6, o.getDateSplitLengths(s6));

    assertEquals(r0, o.getDateSplitLengths(s7));
    assertEquals(r7y, o.getDateSplitLengths(s3a + y4));
    assertEquals(r7y, o.getDateSplitLengths(s3b + y4));
    assertEquals(r7y, o.getDateSplitLengths(s3c + y4));
    assertEquals(r7y, o.getDateSplitLengths(s3d + y4));
    assertEquals(r0, o.getDateSplitLengths(s3e + y4));

    assertEquals(r8, o.getDateSplitLengths(s8));
    assertEquals(r8, o.getDateSplitLengths(s9));

    o = DateOrdering.MDY;
    r5x = Arrays.asList(1, 2, 2);
    r5y = Arrays.asList(2, 1, 2);
    r7x = Arrays.asList(1, 2, 4);
    r7y = Arrays.asList(2, 1, 4);
    r8 = Arrays.asList(2, 2, 4);

    assertEquals(r0, o.getDateSplitLengths(null));
    assertEquals(r0, o.getDateSplitLengths(s0));
    assertEquals(r1, o.getDateSplitLengths(s1));
    assertEquals(r2, o.getDateSplitLengths(s2));

    assertEquals(r3x, o.getDateSplitLengths(s3a));
    assertEquals(r3x, o.getDateSplitLengths(s3b));
    assertEquals(r0, o.getDateSplitLengths(s3c));
    assertEquals(r3y, o.getDateSplitLengths(s3d));
    assertEquals(r0, o.getDateSplitLengths(s3e));

    assertEquals(r4, o.getDateSplitLengths(s4));

    assertEquals(r5x, o.getDateSplitLengths(s5));
    assertEquals(r5x, o.getDateSplitLengths(s3a + y2));
    assertEquals(r5x, o.getDateSplitLengths(s3b + y2));
    assertEquals(r0, o.getDateSplitLengths(s3c + y2));
    assertEquals(r5y, o.getDateSplitLengths(s3d + y2));
    assertEquals(r0, o.getDateSplitLengths(s3e + y2));

    assertEquals(r6, o.getDateSplitLengths(s6));

    assertEquals(r0, o.getDateSplitLengths(s7));
    assertEquals(r7x, o.getDateSplitLengths(s3a + y4));
    assertEquals(r7x, o.getDateSplitLengths(s3b + y4));
    assertEquals(r0, o.getDateSplitLengths(s3c + y4));
    assertEquals(r7y, o.getDateSplitLengths(s3d + y4));
    assertEquals(r0, o.getDateSplitLengths(s3e + y4));

    assertEquals(r8, o.getDateSplitLengths(s8));
    assertEquals(r8, o.getDateSplitLengths(s9));
  }
}
