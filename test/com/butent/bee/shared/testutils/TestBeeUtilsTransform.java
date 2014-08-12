package com.butent.bee.shared.testutils;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link com.butent.bee.shared.utils.BeeUtils}.
 */
@SuppressWarnings("static-method")
public class TestBeeUtilsTransform extends TestCase implements ILogger {

  private final boolean allowLogging = false;

  @Override
  public void log(String msg) {
    if (this.allowLogging) {
      System.out.print(msg);
    }
  }

  @Override
  @Before
  public void setUp() throws Exception {
    log("Starting test TestBeeUtilsTransform \n \r");
  }

  @Override
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testAllEmpty() {
    assertEquals(false, BeeUtils.allEmpty("a", null));
    assertEquals(true, BeeUtils.allEmpty(" ", null));
  }

  @Test
  public void testAllNotEmpty() {
    assertEquals(false, BeeUtils.allNotEmpty("x", null));
    assertEquals(true, BeeUtils.allNotEmpty("a", "b"));
  }

  @Test
  public void testBetweenExclusive() {
    assertEquals(true, BeeUtils.betweenExclusive(5, 5, 10));
    assertEquals(false, BeeUtils.betweenExclusive(1, 5, 10));
    assertEquals(false, BeeUtils.betweenExclusive(10, 5, 10));
    assertEquals(false, BeeUtils.betweenExclusive(100, 5, 10));
  }

  @Test
  public void testBetweenInclusive() {
    assertEquals(true, BeeUtils.betweenInclusive(5, 5, 10));
    assertEquals(false, BeeUtils.betweenInclusive(1, 5, 10));
    assertEquals(true, BeeUtils.betweenInclusive(10, 5, 10));
    assertEquals(false, BeeUtils.betweenInclusive(100, 5, 10));
  }

  @Test
  public void testBracket() {
    assertEquals("[Win]", BeeUtils.bracket("Win"));
    assertEquals("", BeeUtils.bracket(""));
    assertEquals("", BeeUtils.bracket(null));
    assertEquals("[Win Win]", BeeUtils.bracket("Win Win"));
  }

  @Test
  public void testClamp() {
    assertEquals(10, BeeUtils.clamp(5, 10, 15));
    assertEquals(15, BeeUtils.clamp(20, 1, 15));
    assertEquals(5, BeeUtils.clamp(5, 1, 15));
    assertEquals(10.0, BeeUtils.clamp(5.0, 10.0, 15.0));
    assertEquals(15.0, BeeUtils.clamp(20.0, 1.0, 15.0));
    assertEquals(5.0, BeeUtils.clamp(5.0, 1.00, 15.0));
    assertEquals(10.0, BeeUtils.clamp(5, 10.0, 15.0));
    assertEquals(1.0, BeeUtils.clamp(Double.MAX_VALUE * 5, 1.0, 15.0));
    assertEquals(15.0, BeeUtils.clamp(Double.MAX_VALUE * 5, Double.MAX_VALUE * 5, 15.0));
    assertEquals(Double.POSITIVE_INFINITY, BeeUtils.clamp(Double.MAX_VALUE * 5,
        Double.MAX_VALUE * 5, Double.MAX_VALUE * 5));
    assertEquals(5.0, BeeUtils.clamp(5.0, 1, 15));
    assertEquals(5.0, BeeUtils.clamp(5.0, Double.MAX_VALUE * 5, Double.MAX_VALUE * 5));
    assertEquals(5.0, BeeUtils.clamp(5.0, 2.0, Double.MAX_VALUE * 5));
    assertEquals(5.0, BeeUtils.clamp(5.0, Double.MAX_VALUE * 5, 10.0));
  }

  @Test
  public void testClip() {
    assertEquals("This i...[6/18]", BeeUtils.clip("This is a sentence", 6));
    assertEquals("", BeeUtils.clip("", 6));
    assertEquals("Win", BeeUtils.clip("Win", 6));
    assertEquals("Win", BeeUtils.clip("Win          ", 6));
  }

  @Test
  public void testCompare() {
    List<Object> testc1 = new ArrayList<>();
    List<Object> testc2 = new ArrayList<>();
    testc1.add("Text");
    testc1.add(5);

    testc2.add(5);
    testc2.add(2);
    testc2.add(10);

    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst("", ""));
    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst("Compare", "Compare"));
    assertEquals(true, BeeUtils.compareNullsFirst("Aompare", "Compare") < 0);
    assertEquals(true, BeeUtils.compareNullsFirst("Compare", "Aompare") > 0);
    assertEquals(true, BeeUtils.compareNullsFirst(null, "Aompare") < 0);
    assertEquals(true, BeeUtils.compareNullsFirst("Compare", null) > 0);
    assertEquals(true, BeeUtils.compareNullsFirst(7, 6) > 0);
    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst(6, 6));
    assertEquals(true, BeeUtils.compareNullsFirst(5, 6) < 0);

    assertEquals(true, BeeUtils.compareNullsFirst('a', 'c') < 0);
    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst('c', 'c'));
    assertEquals(false, BeeUtils.compareNullsFirst('c', 'a') < 0);

    assertEquals(true, BeeUtils.compareNullsFirst(true, false) > 0);
    assertEquals(true, BeeUtils.compareNullsFirst(false, true) < 0);
    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst(true, true));
    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst(false, false));

    assertEquals(BeeConst.COMPARE_EQUAL, BeeUtils.compareNullsFirst(5.0, 5.0));
    assertEquals(true, BeeUtils.compareNullsFirst(-2.0, 5.69) < 0);
    assertEquals(true, BeeUtils.compareNullsFirst(5.11, 3.0) > 0);
  }

  @Test
  public final void testCount() {
    assertEquals(0, BeeUtils.count(null, 'a'));
    assertEquals(0, BeeUtils.count("", 'a'));
    assertEquals(5, BeeUtils.count("abracadabra", 'a'));
    assertEquals(0, BeeUtils.count("AbrA cAdAbrA", 'a'));
  }

  @Test
  public void testDelete() {
    assertEquals("", BeeUtils.delete(null, 0, 5));
    assertEquals("", BeeUtils.delete("", 0, 5));
    assertEquals("", BeeUtils.delete("This is a string", -5, 88));
    assertEquals(" a string", BeeUtils.delete("This is a string", -5, 7));
    assertEquals("This is a string", BeeUtils.delete("This is a string", 30, 5));
    assertEquals("", BeeUtils.delete("This is a string", 0, 50));
    assertEquals("Thi", BeeUtils.delete("This is a string", 3, 50));
    assertEquals("Thiis a string", BeeUtils.delete("This is a string", 3, 5));

    assertEquals("This is a string", BeeUtils.delete("This is a string", -5, -10));
    assertEquals("This is a string", BeeUtils.delete("This is a string", -10, -5));
    assertEquals("s is a string", BeeUtils.delete("This is a string", -5, 3));
    assertEquals("This is a string", BeeUtils.delete("This is a string", 80, 40));
  }

  @Test
  public void testEqualsTrim() {
    assertEquals(true, BeeUtils.equalsTrim("", ""));
    assertEquals(true, BeeUtils.equalsTrim("   ", "   "));
    assertEquals(true, BeeUtils.equalsTrim("   This is a sentence", "This is a sentence     "));
    assertEquals(true, BeeUtils.equalsTrim("   This is a sentence \n\t",
        "This is a sentence     "));
    assertEquals(true, BeeUtils.equalsTrim(null, null));
    assertEquals(false, BeeUtils.equalsTrim("5", null));
    assertEquals(false, BeeUtils.equalsTrim(null, "5"));
  }

  @Test
  public void testExp10() {
    assertEquals(100, BeeUtils.exp10(2));
    assertEquals(1000, BeeUtils.exp10(3));
  }

  @Test
  public void testFilterContext() {
    List<String> testc1 = new ArrayList<>();
    List<String> testc2 = new ArrayList<>();
    testc1.add("A");
    testc1.add("simple text");
    testc1.add("TEXT2");

    List<String> rez1 = new ArrayList<>();
    rez1.add("simple text");
    rez1.add("TEXT2");

    List<String> rez2 = new ArrayList<>();
    rez2.add("TEXT2");

    List<String> rez3 = new ArrayList<>();

    assertEquals(rez1, BeeUtils.filterContext(testc1, "text"));
    assertEquals(rez2, BeeUtils.filterContext(testc1, "text2"));
    assertEquals(rez3, BeeUtils.filterContext(testc1, "texx"));
    assertEquals(rez3, BeeUtils.filterContext(testc1, ""));
    assertEquals(testc2, BeeUtils.filterContext(testc2, "text"));
  }

  @Test
  public void testFitStart() {
    assertEquals(0, BeeUtils.fitStart(0, 5, 10));
    assertEquals(-5, BeeUtils.fitStart(0, 15, 10));
    assertEquals(4, BeeUtils.fitStart(5, 6, 10));
  }

  @Test
  public void testFromHex() {
    assertEquals("$", new String(BeeUtils.fromHex("24")));
    assertEquals("]", new String(BeeUtils.fromHex("005D")));
    assertEquals("]]", new String(BeeUtils.fromHex("005D005D")));
    assertEquals("]", new String(BeeUtils.fromHex("005d")));
    assertEquals("!P", new String(BeeUtils.fromHex("210050")));
    assertEquals("]]", new String(BeeUtils.fromHex("005d005d")));
    assertEquals(null, BeeUtils.fromHex("242Z"));
    assertEquals(null, BeeUtils.fromHex(null));
  }

  @Test
  public final void testGetDecimals() {
    assertEquals(0, BeeUtils.getDecimals(null));
    assertEquals(0, BeeUtils.getDecimals(""));
    assertEquals(0, BeeUtils.getDecimals("   "));
    assertEquals(0, BeeUtils.getDecimals("."));
    assertEquals(0, BeeUtils.getDecimals("  ."));
    assertEquals(0, BeeUtils.getDecimals("0"));
    assertEquals(1, BeeUtils.getDecimals(". "));
    assertEquals(3, BeeUtils.getDecimals(".000"));
    assertEquals(5, BeeUtils.getDecimals("0.12345"));
    assertEquals(4, BeeUtils.getDecimals("1.0E17"));
  }

  @Test
  public final void testGetClassName() {
    assertEquals("TestBeeUtilsTransform", NameUtils.getClassName(this.getClass()));
    assertEquals("BeeUtils", NameUtils.getClassName(BeeUtils.class));
  }

  @Test
  public void testGetPrefix() {
    assertEquals("for example", BeeUtils.getPrefix("for example, this test...", ','));
    assertEquals("", BeeUtils.getPrefix(", bad example", ','));
    assertEquals("", BeeUtils.getPrefix("There is not a seperator", ','));
    assertEquals("", BeeUtils.getPrefix("for example, this test...", '\0'));
    assertEquals("for example, t", BeeUtils.getPrefix("for example, t\0his test...", '\0'));
    assertEquals("", BeeUtils.getPrefix(null, '\0'));

    assertEquals("for example", BeeUtils.getPrefix("for example, this test...", ","));
    assertEquals("", BeeUtils.getPrefix(", bad example", ","));
    assertEquals("", BeeUtils.getPrefix("There is not a seperator", ","));
    assertEquals("", BeeUtils.getPrefix("for example, this test...", "\0"));
    assertEquals("for example, t", BeeUtils.getPrefix("for example, t\0his test...", "\0"));
    assertEquals("for example,", BeeUtils.getPrefix("for example, this test...", "this"));
    assertEquals("", BeeUtils.getPrefix("for example, this test...", null));
  }

  @Test
  public void testGetQuietly() {
    List<CharSequence> testc1 = new ArrayList<>();
    List<CharSequence> testc2 = new ArrayList<>();
    List<CharSequence> rez1 = new ArrayList<>();
    testc1.add("A");
    testc1.add("simple text");
    testc1.add("TEXT2");

    rez1.add("simple text");

    assertEquals("simple text", BeeUtils.getQuietly(testc1, 1));
    assertEquals(null, BeeUtils.getQuietly(testc1, -1));
    assertEquals(null, BeeUtils.getQuietly(testc2, 5));
  }

  @Test
  public void testGetSuffix() {
    assertEquals("this test...", BeeUtils.getSuffix("for example, this test...", ','));
    assertEquals("", BeeUtils.getSuffix(" bad example,", ','));
    assertEquals("", BeeUtils.getSuffix("There is not a seperator", ','));
    assertEquals("", BeeUtils.getSuffix("for example, this test...", '\0'));
    assertEquals("his test...", BeeUtils.getSuffix("for example, t\0his test...", '\0'));
    assertEquals("", BeeUtils.getSuffix(null, '\0'));

    assertEquals("test...", BeeUtils.getSuffix("for example, this ,test...", ","));
    assertEquals("", BeeUtils.getSuffix(", bad example,", ","));
    assertEquals("", BeeUtils.getSuffix("There is not a seperator", ","));
    assertEquals("", BeeUtils.getSuffix("for example, this test...", "\0"));
    assertEquals("his test...", BeeUtils.getSuffix("for example, t\0his test...", "\0"));
    assertEquals("test...", BeeUtils.getSuffix("for example, this test...", "this"));
    assertEquals("", BeeUtils.getSuffix("for example, this test...", null));
  }

  @Test
  public void testIfString() {
    assertEquals("test", BeeUtils.notEmpty("test", "not"));
    assertEquals("not", BeeUtils.notEmpty(null, "not"));
    assertEquals("test", BeeUtils.notEmpty("test", null));
    assertEquals("not", BeeUtils.notEmpty("", "not"));
    assertEquals(null, BeeUtils.notEmpty("", null));
    assertEquals("", BeeUtils.notEmpty("", ""));
  }

  @Test
  public void testInList() {
    assertEquals(true, BeeUtils.inList("text", "this", "is", "a", "text"));
    assertEquals(false, BeeUtils.inList("texts", "this", "is", "a", "text"));
  }

  @Test
  public void testInListSame() {
    assertEquals(true, BeeUtils.inListSame("text", "this", "is", "a", "tExt"));
    assertEquals(false, BeeUtils.inListSame("texts", "this", "is", "a", "text"));
    assertEquals(true, BeeUtils.inListSame("TeXT", "this", "is", "a", "text"));
    assertEquals(false, BeeUtils.inListSame("TEXTS", "this", "is", "a", "text"));

    assertEquals(true, BeeUtils.inListSame("text       ", "this", "is", "a", "tExt"));
    assertEquals(false, BeeUtils.inListSame("texts", "this", "is", "a", "      text"));
    assertEquals(true, BeeUtils.inListSame("  \n \0 teXT   ", "this", "is", "a", "text    "));
    assertEquals(false, BeeUtils.inListSame("TS    ", "this", "   is", "a", "text"));
    assertEquals(true, BeeUtils.inListSame("is   ", "this", null, "   is", "text"));
  }

  @Test
  public void testInsert() {
    assertEquals("This is a test", BeeUtils.insert("Ths is a test", 2, 'i'));
    assertEquals("iThs is a test", BeeUtils.insert("Ths is a test", 0, 'i'));
    assertEquals("Ths is a testi", BeeUtils.insert("Ths is a test", 13, 'i'));
  }

  @Test
  public void testIsBoolean() {
    assertEquals(false, BeeUtils.isBoolean(null));
    assertEquals(false, BeeUtils.isBoolean("pick"));
    assertEquals(false, BeeUtils.isBoolean(""));
    assertEquals(true, BeeUtils.isBoolean("true"));
    assertEquals(true, BeeUtils.isBoolean("false"));
    assertEquals(true, BeeUtils.isBoolean("yEs"));
    assertEquals(true, BeeUtils.isBoolean("no"));
  }

  @Test
  public void testIsDigit() {
    assertEquals(true, BeeUtils.isDigit('8'));
    assertEquals(false, BeeUtils.isDigit('c'));
    assertEquals(false, BeeUtils.isDigit('-'));
    assertEquals(false, BeeUtils.isDigit(null));
    assertEquals(true, BeeUtils.isDigit("123"));
    assertEquals(false, BeeUtils.isDigit("12ab"));
    assertEquals(false, BeeUtils.isDigit("ab12"));
    assertEquals(false, BeeUtils.isDigit(""));
  }

  @Test
  public void testIsDouble() {
    assertEquals(true, BeeUtils.isDouble((double) 1));
    assertEquals(true, BeeUtils.isDouble((double) 'c'));
    assertEquals(true, BeeUtils.isDouble((double) '5'));
    assertEquals(true, BeeUtils.isDouble(-5.0));
    assertEquals(false, BeeUtils.isDouble(""));
    assertEquals(false, BeeUtils.isDouble((Double) null));
    assertEquals(false, BeeUtils.isDouble("55e"));
    assertEquals(false, BeeUtils.isDouble("e55"));
    assertEquals(true, BeeUtils.isDouble("55"));
    assertEquals(false, BeeUtils.isDouble(Double.MAX_VALUE * 5));
  }

  @Test
  public void testIsIdentifier() {
    assertEquals(false, NameUtils.isIdentifier(null));
    assertEquals(false, NameUtils.isIdentifier(""));
    assertEquals(true, NameUtils.isIdentifier("aaa5"));
    assertEquals(false, NameUtils.isIdentifier("5a"));
    assertEquals(false, NameUtils.isIdentifier("5a_a"));
    assertEquals(false, NameUtils.isIdentifier("\t\n\r"));
    assertEquals(false, NameUtils.isIdentifier("&&&^%$$&**()"));
  }

  @Test
  public void testIsInt() {
    assertEquals(false, BeeUtils.isInt(null));
    assertEquals(false, BeeUtils.isInt(""));
    assertEquals(false, BeeUtils.isInt("asd"));
    assertEquals(true, BeeUtils.isInt("44444"));
    assertEquals(false, BeeUtils.isInt("3.141123546"));
  }

  @Test
  public void testIsLong() {
    assertEquals(false, BeeUtils.isLong(-555555555555555555555555555555555555555.5));
    assertEquals(true, BeeUtils.isLong(-5555.5555555555));
    assertEquals(false, BeeUtils.isLong(555555555555555555555555555555555555555555.555));
  }

  @Test
  public void testIsOrdinal() {
    assertEquals(true, BeeUtils.isOrdinal(State.class, 5));
    assertEquals(false, BeeUtils.isOrdinal(State.class, 500));
  }

  @Test
  public void testIsPositive() {
    assertEquals(false, BeeUtils.isPositiveInt(""));
    assertEquals(true, BeeUtils.isPositiveInt("5"));
    assertEquals(false, BeeUtils.isPositiveInt("-5"));
    assertEquals(false, BeeUtils.isPositive(-5));
    assertEquals(true, BeeUtils.isPositive(5));
    assertEquals(true, BeeUtils.isPositive(5.0));
    assertEquals(false, BeeUtils.isPositive(0.0));
  }

  @Test
  public void testIsTrue() {
    assertEquals(false, BeeUtils.isTrue(null));
    assertEquals(true, BeeUtils.isTrue(true));
    assertEquals(false, BeeUtils.isTrue(false));
  }

  @Test
  public void testJoin() {
    assertEquals("is:a:test", BeeUtils.join(":", "is", "a", "test"));
  }

  @Test
  public void testLeft() {
    assertEquals("This is a", BeeUtils.left("This is a string", 9));
    assertEquals("", BeeUtils.left("This is a string", -9));
    assertEquals(null, BeeUtils.left(null, 9));
    assertEquals("This is a", BeeUtils.left("This is a", 55));
    assertEquals("", BeeUtils.left("This is a string", 0));
  }

  @Test
  public void testLength() {
    assertEquals(0, BeeUtils.length(null));
    assertEquals(6, BeeUtils.length("getout"));
  }

  @Test
  public final void testNormalize() {
    assertEquals("", BeeUtils.normalize(null));
    assertEquals("abracadabra", BeeUtils.normalize("abracadabra"));
    assertEquals("abra cadabra", BeeUtils.normalize("AbrA cAdAbrA"));
  }

  @Test
  public void testNvl() {
    assertEquals("string", BeeUtils.nvl(null, "string"));
    assertEquals(null, BeeUtils.nvl(null, null));
    assertEquals("stringas", BeeUtils.nvl("stringas", null));
  }

  @Test
  public void testPadLeft() {
    assertEquals("     This is a string", BeeUtils.padLeft("This is a string", 21, ' '));
    assertEquals("     ", BeeUtils.padLeft("", 5, ' '));
    assertEquals(null, BeeUtils.padLeft(null, 5, ' '));
    assertEquals("", BeeUtils.padLeft("This is a string", 0, ' '));
    assertEquals("This is a string", BeeUtils.padLeft("This is a string", 5, ' '));
  }

  @Test
  public void testParenthesize() {
    assertEquals("(This is a string)", BeeUtils.parenthesize("This is a string"));
    assertEquals("(5)", BeeUtils.parenthesize(5));
    assertEquals("", BeeUtils.parenthesize((Object) null));
  }

  @Test
  public void testProgress() {
    assertEquals("5/10", BeeUtils.progress(5, 10));
    assertEquals("-5/10", BeeUtils.progress(-5, 10));
  }

  @Test
  public void testProper() {
    assertEquals("", BeeUtils.proper(null, ';'));
    assertEquals("", BeeUtils.proper("  ", ';'));
    assertEquals("S", BeeUtils.proper(" s ", null));
    assertEquals("Ssssss", BeeUtils.proper(" ssssss ", null));
    assertEquals("Ssssss Ssss Aaaa Bbbb", BeeUtils.proper(" ssssss.ssss.aaaa.bbbb ", '.'));
  }

  @Test
  public void testRandomInt() {
    for (int i = 0; i < 20; i++) {
      assertEquals(true, BeeUtils.randomInt(5, 10) <= 5 + 5);
      assertEquals(true, BeeUtils.randomInt(5, 10) >= 5);
    }
  }

  @Test
  public void testRandomString() {
    for (int i = 0; i < 20; i++) {
      assertEquals(true, BeeUtils.randomString(5, 10, 'a', 'c').compareTo("cccccccccc") <= 1);
      assertEquals(true, BeeUtils.randomString(5, 10, 'a', 'c').compareTo("aaaaa") >= -1);
      assertEquals(true, BeeUtils.randomString(5, 5, 'a', 'c').length() == 5);
      assertEquals("aaaaa", BeeUtils.randomString(5, 5, 'a', 'a'));
    }
  }

  @Test
  public void testRandomStringIntChar() {
    for (int i = 0; i < 20; i++) {
      assertEquals(true, BeeUtils.randomString(5, "ab").compareTo("bbbbb") <= 1);
      assertEquals(true, BeeUtils.randomString(5, "ab").compareTo("aaaa") >= -1);
      assertEquals(true, BeeUtils.randomString(5, "a").compareTo("aaaaa") == 0);
    }
  }

  @Test
  public final void testRemoveTrailingZeros() {
    assertEquals(null, BeeUtils.removeTrailingZeros(null));
    assertEquals("AA", BeeUtils.removeTrailingZeros("AA"));
    assertEquals("00.000.AA", BeeUtils.removeTrailingZeros("00.000.AA"));
    assertEquals(".00.000", BeeUtils.removeTrailingZeros(".00.000"));
    assertEquals("AA.AAA.000", BeeUtils.removeTrailingZeros("AA.AAA.000"));
    assertEquals("00", BeeUtils.removeTrailingZeros("00.00"));
    assertEquals("00", BeeUtils.removeTrailingZeros("00.0000000000000"));
    assertEquals("XX", BeeUtils.removeTrailingZeros("XX.0000000000000"));
    assertEquals("1.0E10", BeeUtils.removeTrailingZeros("1.0E10"));
  }

  @Test
  public void testReplace() {
    assertEquals("text", BeeUtils.replace("test", 2, 3, 'x'));
    assertEquals("texst", BeeUtils.replace("test", 2, 3, "xs"));
    assertEquals("text", BeeUtils.replace("test", "s", "x"));
    assertEquals("test", BeeUtils.replace("test", "a", "x"));

    assertEquals("text text", BeeUtils.replace("test test", "s", "x", 2));
    assertEquals("text test", BeeUtils.replace("test test", "s", "x", 1));

    assertEquals(null, BeeUtils.replace(null, "a", "x", 2));
    assertEquals("test", BeeUtils.replace("test", null, "x", 2));
    assertEquals("test", BeeUtils.replace("test", "s", null, 2));
    assertEquals("test", BeeUtils.replace("test", "s", "x", 0));
    assertEquals(null, BeeUtils.replace(null, null, null, 0));
  }

  @Test
  public void testReplicate() {
    assertEquals("t", BeeUtils.replicate('t', 1));
    assertEquals("ttttt", BeeUtils.replicate('t', 5));
  }

  @Test
  public void testRound() {
    assertEquals(3.14, BeeUtils.round(3.1412, 2));
    assertEquals(0.0, BeeUtils.round(Double.POSITIVE_INFINITY, 2));
    assertEquals(3.1, BeeUtils.round(3.1412, 1));
    assertEquals(3.5, BeeUtils.round(3.499, 1));
    assertEquals(3.0, BeeUtils.round(3, 5));
    assertEquals(9.223372036854776E18, BeeUtils.round(Long.MAX_VALUE, 5));
    assertEquals(-9.223372036854776E18, BeeUtils.round(Long.MIN_VALUE, 5));
    assertEquals(0.0, BeeUtils.round(Long.valueOf(0), 5));
    assertEquals(0.0, BeeUtils.round(0.0, 5));

    assertEquals("0", BeeUtils.round("0.499", 0));
    assertEquals("0.50", BeeUtils.round("0.499", 2));
    assertEquals("3.1416", BeeUtils.round(Double.toString(Math.PI), 4));
    assertEquals("10000000000", BeeUtils.round("1.0E10", 0));
  }

  @Test
  public void testSame() {
    assertEquals(true, BeeUtils.same("pick", " PicK "));
    assertEquals(false, BeeUtils.same(null, " PicK "));
    assertEquals(false, BeeUtils.same("pick", null));
    assertEquals(true, BeeUtils.same("", null));
    assertEquals(true, BeeUtils.same(null, null));
  }

  @Test
  public void testSpace() {
    assertEquals(BeeConst.STRING_SPACE, BeeUtils.space(1));
    assertEquals(BeeConst.STRING_EMPTY, BeeUtils.space(-1));
    assertEquals("     ", BeeUtils.space(5));
  }

  @Test
  public void testSplit() {
    String[] a = {"string epic", "epic", "string", ";"};

    assertEquals(a[0], BeeUtils.split("string epic", ';')[0]);
    assertEquals(a[2], BeeUtils.split("string;epic", ';')[0]);
    assertEquals(a[1], BeeUtils.split("string;epic", ';')[1]);
    assertEquals(a[2], BeeUtils.split("string", ';')[0]);
    assertEquals(null, BeeUtils.split(null, ';'));
    assertEquals(BeeConst.EMPTY_STRING_ARRAY, BeeUtils.split("", ';'));
  }

  @Test
  public void testStartsSame() {
    assertEquals(true, BeeUtils.startsSame("string", "string and another one"));
    assertEquals(true, BeeUtils.startsSame("str", "string and"));
    assertEquals(false, BeeUtils.startsSame(null, "string and"));
    assertEquals(false, BeeUtils.startsSame("string", null));
    assertEquals(false, BeeUtils.startsSame("", ""));
  }

  @Test
  public void testToBoolean() {
    assertEquals(true, BeeUtils.toBoolean(1));
    assertEquals(false, BeeUtils.toBoolean(0));
    assertEquals(false, BeeUtils.toBoolean(-5));
    assertEquals(false, BeeUtils.toBoolean("false"));
    assertEquals(false, BeeUtils.toBoolean("no"));
    assertEquals(true, BeeUtils.toBoolean("yes"));
    assertEquals(true, BeeUtils.toBoolean("true"));
    assertEquals(false, BeeUtils.toBoolean("hjjj"));
    assertEquals(false, BeeUtils.toBoolean(null));
  }

  @Test
  public final void testToBooleanOrNull() {
    assertEquals(null, BeeUtils.toBooleanOrNull(null));
    assertEquals(Boolean.FALSE, BeeUtils.toBooleanOrNull("false"));
    assertEquals(Boolean.FALSE, BeeUtils.toBooleanOrNull("asdasd"));
    assertEquals(Boolean.TRUE, BeeUtils.toBooleanOrNull("1"));
  }

  @Test
  public void testToDouble() {
    assertEquals(0.0, BeeUtils.toDouble("0.0"));
    assertEquals(-0.569, BeeUtils.toDouble("     -0.569  \r"));
    assertEquals(3.145, BeeUtils.toDouble("3.145"));
    assertEquals(0.0, BeeUtils.toDouble("     -0.5/0 infinity69  \r"));
    assertEquals(0.0, BeeUtils.toDouble("     "));
    assertEquals(0.0, BeeUtils.toDouble(null));
  }

  @Test
  public final void testToDoubleOrNull() {
    assertEquals(null, BeeUtils.toDoubleOrNull(null));
    assertEquals(null, BeeUtils.toDoubleOrNull("asd"));
    assertEquals(15.0, BeeUtils.toDoubleOrNull("15.0"));
  }

  @Test
  public void testToInt() {
    assertEquals(1, BeeUtils.toInt(true));
    assertEquals(0, BeeUtils.toInt(false));
  }

  @Test
  public final void testToIntOrNull() {
    assertEquals(null, BeeUtils.toIntOrNull(null));
    assertEquals(Integer.valueOf(0), BeeUtils.toIntOrNull("0"));
    assertEquals(Integer.valueOf(15), BeeUtils.toIntOrNull("15.0"));
    assertEquals(null, BeeUtils.toIntOrNull(""));
  }

  @Test
  public void testToLeadingZeroes() {
    assertEquals("0010", BeeUtils.toLeadingZeroes(10, 4));
    assertEquals("10", BeeUtils.toLeadingZeroes(10, 2));
    assertEquals("-10", BeeUtils.toLeadingZeroes(-10, 15));
  }

  @Test
  public void testToLong() {
    assertEquals(0, BeeUtils.toLong("0.5"));
    assertEquals(0, BeeUtils.toLong("0"));
    assertEquals(-1, BeeUtils.toLong("     -1  \r"));
    assertEquals(3, BeeUtils.toLong("3"));
    assertEquals(0, BeeUtils.toLong("     -0.5/0 infinity69  \r"));
    assertEquals(0, BeeUtils.toLong("     "));
  }

  @Test
  public final void testToLongOrNull() {
    assertEquals(null, BeeUtils.toLongOrNull(null));
    assertEquals(Long.valueOf(0), BeeUtils.toLongOrNull("0"));
    assertEquals(Long.valueOf(15), BeeUtils.toLongOrNull("15.0"));
    assertEquals(null, BeeUtils.toLongOrNull(""));
  }

  @Test
  public final void testToNonNegativeIntDouble() {
    assertEquals(0, BeeUtils.toNonNegativeInt((Double) null));
    assertEquals(5, BeeUtils.toNonNegativeInt(Double.valueOf("5.0")));
    assertEquals(0, BeeUtils.toNonNegativeInt(Double.valueOf("-15.0")));
    assertEquals(0, BeeUtils.toNonNegativeInt(Double.valueOf("-99.0")));
  }

  @Test
  public final void testToNonNegativeIntInteger() {
    assertEquals(0, BeeUtils.toNonNegativeInt((Integer) null));
    assertEquals(5, BeeUtils.toNonNegativeInt(Integer.valueOf("5")));
    assertEquals(0, BeeUtils.toNonNegativeInt(Integer.valueOf("-15")));
    assertEquals(0, BeeUtils.toNonNegativeInt(Integer.valueOf("-99")));
  }

  @Test
  public void testToString() {
    assertEquals("true", BeeUtils.toString(true));
    assertEquals("false", BeeUtils.toString(false));
    assertEquals("1.2", BeeUtils.toString(1.2));
    assertEquals("5", BeeUtils.toString(5));
    assertEquals("Infinity", BeeUtils.toString(Double.POSITIVE_INFINITY));
    assertEquals("9", BeeUtils.toString((long) 9));

    assertEquals("0", BeeUtils.toString(0.0));
    assertEquals("0.123", BeeUtils.toString(0.123));
    assertEquals("1000000", BeeUtils.toString(1e6));
    assertEquals("1.0E-6", BeeUtils.toString(0.000001));
    assertEquals("-1.0E-6", BeeUtils.toString(-1e-6));
    assertEquals("1.000001", BeeUtils.toString(1.000001));

    assertEquals("0", BeeUtils.toString(0.0, 10));
    assertEquals("0", BeeUtils.toString(0.0, -1));
    assertEquals("0", BeeUtils.toString(0.0, -0));
    assertEquals("NaN", BeeUtils.toString(Double.NaN, 10));
    assertEquals("1.0E20", BeeUtils.toString(1e20, 2));
    assertEquals("0.667", BeeUtils.toString(0.666666666, 3));
    assertEquals("-1", BeeUtils.toString(-0.666, 0));
  }

  @Test
  public void testTransformClass() {
    int a = 0;
    assertEquals(BeeConst.NULL, NameUtils.transformClass(null));
    assertEquals(this.getClass().getName(), NameUtils.transformClass(this));
    assertEquals("java.lang.Integer", NameUtils.transformClass(a));
  }

  @Test
  public final void testTrim() {
    assertEquals("", BeeUtils.trim(null));
    assertEquals("", BeeUtils.trim(""));
    assertEquals("12345", BeeUtils.trim("12345"));
    assertEquals("12345", BeeUtils.trim(" 12345   "));
  }

  @Test
  public final void testUnboxBoolInt() {
    assertEquals(false, BeeUtils.unbox((Boolean) null));
    assertEquals(true, BeeUtils.unbox(Boolean.TRUE));
    assertEquals(false, BeeUtils.unbox(Boolean.FALSE));

    assertEquals(0, BeeUtils.unbox((Integer) null));
    assertEquals(5, BeeUtils.unbox(Integer.valueOf(5)));
    assertEquals(-55, BeeUtils.unbox(Integer.valueOf("-55")));
  }

  @Test
  public void testVal() {
    assertEquals(10, BeeUtils.val("abcd 10 efg", true));
    assertEquals(10, BeeUtils.val("10", false));
    assertEquals(10, BeeUtils.val("10.5", false));
    assertEquals(-10, BeeUtils.val("   -10.5", true));
    assertEquals(0, BeeUtils.val("-inf", false));
    assertEquals(10, BeeUtils.val("     10   ", true));
    assertEquals(0, BeeUtils.val("abcd  rft 10 4 6 kl", false));
    assertEquals(0, BeeUtils.val("", false));
    assertEquals(0, BeeUtils.val("         ", true));
    assertEquals(0, BeeUtils.val(null, false));
  }
}
