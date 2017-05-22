package com.butent.bee.shared.utils;

import com.butent.bee.shared.utils.Wildcards.Pattern;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.utils.Wildcards}.
 */
@SuppressWarnings("static-method")
public class TestWildcards {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetDefaultAny() {

    assertEquals('*', Wildcards.getDefaultAny());
  }

  @Test
  public final void testGetDefaultOne() {
    assertEquals('?', Wildcards.getDefaultOne());
  }

  @Test
  public final void testGetDefaultPatternString() {
    assertEquals("abc>def<c<", Wildcards.getDefaultPattern("abc>def<c<").toString());
  }

  @Test
  public final void testGetDefaultPatternStringBoolean() {
    assertEquals("abc>def<c< (sensitive)",
        Wildcards.getDefaultPattern("abc>def<c<", true).toString());
  }

  @Test
  public final void testGetFsAny() {
    assertEquals('*', Wildcards.getFsAny());
  }

  @Test
  public final void testGetFsOne() {
    assertEquals('?', Wildcards.getFsOne());
  }

  @Test
  public final void testGetFsPatternString() {
    assertEquals("abc>def<c<", Wildcards.getFsPattern("abc>def<c<").toString());
  }

  @Test
  public final void testGetFsPatternStringBoolean() {
    assertEquals("abc>def<c< (sensitive)",
        Wildcards.getFsPattern("abc>def<c<", true).toString());
  }

  @Test
  public final void testGetSqlAny() {
    assertEquals('%', Wildcards.getSqlAny());
  }

  @Test
  public final void testGetSqlOne() {
    assertEquals('_', Wildcards.getSqlOne());
  }

  @Test
  public final void testGetSqlPatternString() {
    assertEquals("abc>def<c<", Wildcards.getSqlPattern("abc>def<c<").toString());
  }

  @Test
  public final void testGetSqlPatternStringBoolean() {
    assertEquals("abc>def<c< (sensitive)",
        Wildcards.getSqlPattern("abc>def<c<", true).toString());
  }

  @Test
  public final void testHasDefaultWildcards() {
    assertEquals(false, Wildcards.hasDefaultWildcards(""));
    assertEquals(false, Wildcards.hasDefaultWildcards(null));
    assertEquals(true,
        Wildcards.hasDefaultWildcards("all*default?wildcards"));
    assertEquals(true,
        Wildcards.hasDefaultWildcards("one*default wildcard"));
    assertEquals(true,
        Wildcards.hasDefaultWildcards("one?default wildcard"));
    assertEquals(false,
        Wildcards.hasDefaultWildcards("no default wildcards"));
  }

  @Test
  public final void testHasFsWildcards() {
    assertEquals(false, Wildcards.hasFsWildcards(""));
    assertEquals(false, Wildcards.hasFsWildcards(null));
    assertEquals(true, Wildcards.hasFsWildcards("all*default?wildcards"));
    assertEquals(true, Wildcards.hasFsWildcards("one*default wildcard"));
    assertEquals(true, Wildcards.hasFsWildcards("one?default wildcard"));
    assertEquals(false, Wildcards.hasFsWildcards("no default wildcards"));
  }

  @Test
  public final void testHasSqlWildcards() {
    assertEquals(false, Wildcards.hasSqlWildcards(""));
    assertEquals(false, Wildcards.hasSqlWildcards(null));
    assertEquals(true, Wildcards.hasSqlWildcards("all%default_wildcards"));
    assertEquals(true, Wildcards.hasSqlWildcards("one%default wildcard"));
    assertEquals(true, Wildcards.hasSqlWildcards("one_default wildcard"));
    assertEquals(false, Wildcards.hasSqlWildcards("no default wildcards"));
  }

  @Test
  public final void testIsDefaultAny() {
    assertEquals(false, Wildcards.isDefaultAny(null));
    assertEquals(false, Wildcards.isDefaultAny(""));
    assertEquals(false, Wildcards.isDefaultAny("abcd"));
    assertEquals(false, Wildcards.isDefaultAny("?"));
    assertEquals(true, Wildcards.isDefaultAny("*"));
  }

  @Test
  public final void testIsDefaultCaseSensitive() {
    assertEquals(false, Wildcards.isDefaultCaseSensitive());
  }

  @Test
  public final void testIsDefaultPattern() {
    assertEquals(false, Wildcards.isDefaultPattern(""));
    assertEquals(false, Wildcards.isDefaultPattern(null));
    assertEquals(true, Wildcards.isDefaultPattern("abc*bcd"));
    assertEquals(false, Wildcards.isDefaultPattern("*"));
    assertEquals(true, Wildcards.isDefaultPattern("abbcd"));
  }

  @Test
  public final void testIsFsAny() {
    assertEquals(false, Wildcards.isFsAny(null));
    assertEquals(false, Wildcards.isFsAny(""));
    assertEquals(false, Wildcards.isFsAny("abcd"));
    assertEquals(false, Wildcards.isFsAny("?"));
    assertEquals(true, Wildcards.isFsAny("*"));
  }

  @Test
  public final void testIsFsCaseSensitive() {
    assertEquals(false, Wildcards.isFsCaseSensitive());
  }

  @Test
  public final void testIsFsLikeStringString() {
    assertEquals(true, Wildcards.isFsLike("abc", "ab?"));
    assertEquals(true, Wildcards.isFsLike("abc", "*b?"));
    assertEquals(false, Wildcards.isFsLike("aabc", "?b?"));
  }

  @Test
  public final void testIsFsLikeStringStringBoolean() {
    assertEquals(true, Wildcards.isFsLike("abc", "ab?", true));
    assertEquals(true, Wildcards.isFsLike("abc", "*b?", false));
    assertEquals(false, Wildcards.isFsLike("aaBc", "*b?", true));
  }

  @Test
  public final void testIsFsPattern() {
    assertEquals(false, Wildcards.isFsPattern(""));
    assertEquals(false, Wildcards.isFsPattern(null));
    assertEquals(true, Wildcards.isFsPattern("abc*bcd"));
    assertEquals(false, Wildcards.isFsPattern("*"));
    assertEquals(true, Wildcards.isFsPattern("abbcd"));
  }

  @Test
  public final void testIsLikeStringPattern() {
    // Pattern a = Wildcards.getPattern("File*.txt=5", '*', '\0', false);
    Pattern a = Wildcards.getPattern("expression", '>', '<', false, null);
    Pattern aa = Wildcards.getPattern("expression", '>', '<', true, null);
    Pattern a2 = Wildcards.getPattern("=expression", '>', '<', false, '=');
    Pattern aa2 = Wildcards.getPattern("=expression", '>', '<', true, '=');
    assertEquals(false, Wildcards.isLike("abc", a));
    assertEquals(false, Wildcards.isLike("abc", a2));
    assertEquals(false, Wildcards.isLike("abc", a2));
    assertEquals(false, Wildcards.isLike("abc", a2));
    assertEquals(false, Wildcards.isLike("", a2));
    assertEquals(false, Wildcards.isLike("abc", aa));
    assertEquals(false, Wildcards.isLike("abc", aa2));

    assertEquals(true,
        Wildcards.isLike("abc", Wildcards.getPattern("*b?", '*', '?', false, null)));
    assertEquals(true,
        Wildcards.isLike("abc", Wildcards.getPattern("*b?", '*', '?', true, null)));
    assertEquals(false,
        Wildcards.isLike("ABc", Wildcards.getPattern("*b?", '*', '?', true, null)));
    assertEquals(true,
        Wildcards.isLike("aaabc", Wildcards.getPattern("*b?", '*', '?', false, null)));
    assertEquals(false,
        Wildcards.isLike("abcccc", Wildcards.getPattern("*b?", '*', '?', false, null)));

    assertEquals(false,
        Wildcards.isLike("abcccc", Wildcards.getPattern("*b?????", '*', '?', false, null)));
    assertEquals(true,
        Wildcards.isLike("abcccc", Wildcards.getPattern("abcc*", '*', '?', false, null)));
    assertEquals(false,
        Wildcards.isLike("abcccc", Wildcards.getPattern("abcc=", '*', '?', false, '=')));

    assertEquals(true,
        Wildcards.isLike("abcdefghijklm",
            Wildcards.getPattern("*cde*ghijk?m", '*', '?', false, null)));
    assertEquals(true,
        Wildcards.isLike("abc", Wildcards.getPattern("***", '*', '?', false, null)));
    assertEquals(false,
        Wildcards.isLike("abc", Wildcards.getPattern(">>>", '*', '?', false, null)));
  }

  @Test
  public final void testIsLikeStringString() {
    assertEquals(true, Wildcards.isLike("abccc", "?Bc??"));
    assertEquals(true, Wildcards.isLike("abccc", "*Bc*"));
    assertEquals(true, Wildcards.isLike("aa", "aa"));
  }

  @Test
  public final void testIsLikeStringStringBoolean() {
    assertEquals(false, Wildcards.isLike("abccc", "?Bc??", true));
    assertEquals(true, Wildcards.isLike("abccc", "*bc*", true));
    assertEquals(false, Wildcards.isLike("aa", "AA", true));
  }

  @Test
  public final void testIsSqlAny() {
    assertEquals(true, Wildcards.isSqlAny("%"));
    assertEquals(false, Wildcards.isSqlAny("%+"));
    assertEquals(false, Wildcards.isSqlAny("+"));
  }

  @Test
  public final void testIsSqlCaseSensitive() {
    assertEquals(false, Wildcards.isSqlCaseSensitive());
  }

  @Test
  public final void testIsSqlLikeStringString() {
    assertEquals(true, Wildcards.isSqlLike("abc", "ab%"));
    assertEquals(true, Wildcards.isSqlLike("abc", "%b%"));
    assertEquals(false, Wildcards.isSqlLike("aabc", "_b_"));
  }

  @Test
  public final void testIsSqlLikeStringStringBoolean() {
    assertEquals(true, Wildcards.isSqlLike("Abc", "Ab%", true));
    assertEquals(false, Wildcards.isSqlLike("abc", "*B%", true));
    assertEquals(false, Wildcards.isSqlLike("aabc", "_b_", false));
  }

  @Test
  public final void testIsSqlPattern() {
    assertEquals(false, Wildcards.isSqlPattern("%"));
    assertEquals(true, Wildcards.isSqlPattern("%ab"));
    assertEquals(true, Wildcards.isSqlPattern("_ab"));
    assertEquals(true, Wildcards.isSqlPattern("asdb"));
  }

  @Test
  public final void testSetDefaultCaseSensitivity() {
    Wildcards.setDefaultCaseSensitivity(true);
    assertEquals(Wildcards.isDefaultCaseSensitive(), true);
    Wildcards.setDefaultCaseSensitivity(false);
    assertEquals(Wildcards.isDefaultCaseSensitive(), false);
  }

  @Test
  public final void testSetFsCaseSensitivity() {
    Wildcards.setFsCaseSensitivity(true);
    assertEquals(Wildcards.isFsCaseSensitive(), true);
    Wildcards.setFsCaseSensitivity(false);
    assertEquals(Wildcards.isFsCaseSensitive(), false);
  }

  @Test
  public final void testSetSqlCaseSensitivity() {
    Wildcards.setSqlCaseSensitivity(true);
    assertEquals(Wildcards.isSqlCaseSensitive(), true);
    Wildcards.setSqlCaseSensitivity(false);
    assertEquals(Wildcards.isSqlCaseSensitive(), false);
  }

  @Test
  public final void testGetExpr() {
    Pattern a = Wildcards.getPattern("File*.txt=5", '*', '\0', false, null);

    assertEquals("File*.txt=5", a.getExpr());

    a = Wildcards.getPattern("     File*.txt=5   \t \t \t \r ", '*', '\0', false, null);
    assertEquals("File*.txt=5", a.getExpr());
  }

  @Test
  public final void testGetTokens() {
    Pattern a = Wildcards.getPattern("File*.txt=5", '*', '\0', false, null);

    String[] b = {"File", "*", ".txt=5"};
    assertArrayEquals(b, a.getTokens());

    a = Wildcards.getPattern("File*.txt=5", '*', '.', false, null);
    String[] c = {"File", ".", "*", "txt=5"};
    assertArrayEquals(c, a.getTokens());

    assertEquals("File*.txt=5", a.toString());

    a = Wildcards.getPattern("File*.txt=5", '*', '.', true, null);
    assertEquals("File*.txt=5 (sensitive)", a.toString());

    a = Wildcards.getPattern("File*.txt5=", ';', '>', true, '=');
    assertEquals("File*.txt5 (sensitive) (exact)", a.toString());

    a = Wildcards.getPattern("File*.txt5", ';', '>', true, null);
    assertEquals("File*.txt5 (sensitive)", a.toString());

    a = Wildcards.getPattern("abcd>efgh>opkg<asdda>>>", '>', '<', false, null);
    assertEquals("abcd>efgh>opkg<asdda>>>", a.toString());

    Object[] mas = {"abcd", ">", "efgh", ">", "opkg", "<", "asdda", ">"};
    assertArrayEquals(mas, a.getTokens());
  }
}
