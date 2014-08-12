package com.butent.bee.client.dom;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Enables using DOM selectors, which are a way to select specified set of elements fitting certain
 * filters.
 */

public final class Selectors {

  private static final String UNIVERSAL_SELECTOR = "*";
  private static final String SELECTOR_SEPARATOR = ", ";

  private static final String EMPTY_ATTRIBUTE_VALUE = "\"\"";

  private static final String ATTRIBUTE_SELECTOR_PREFIX = "[";
  private static final String ATTRIBUTE_SELECTOR_SUFFIX = "]";
  private static final String CLASS_SELECTOR_PREFIX = ".";
  private static final String ID_SELECTOR_PREFIX = "#";
  private static final String PSEUDO_CLASS_SELECTOR_PREFIX = ":";

  private static final char SINGLE_QUOTE = '\'';
  private static final char DOUBLE_QUOTE = '"';
  private static final char BACKSLASH = '\\';

  public static String adjacentSiblingCombinator(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 2);
    return ArrayUtils.join(" + ", selectors);
  }

  public static String attributeContains(String att, String val) {
    Assert.notEmpty(val);
    return buildAttributeSelector(att, "*=", val);
  }

  public static String attributeContainsWord(String att, String val) {
    Assert.notEmpty(val);
    Assert.isTrue(!BeeUtils.containsWhitespace(val.trim()));
    return buildAttributeSelector(att, "~=", val);
  }

  public static String attributeEndsWith(String att, String val) {
    Assert.notEmpty(val);
    return buildAttributeSelector(att, "$=", val);
  }

  public static String attributeEquals(String att, int val) {
    return attributeEquals(att, BeeUtils.toString(val));
  }

  public static String attributeEquals(String att, long val) {
    return attributeEquals(att, BeeUtils.toString(val));
  }

  public static String attributeEquals(String att, String val) {
    return buildAttributeSelector(att, "=", val);
  }

  public static String attributeHyphenatedStartsWith(String att, String val) {
    Assert.notEmpty(val);
    return buildAttributeSelector(att, "|=", val);
  }

  public static String attributePresence(String att) {
    assertIdentifier(att);
    return ATTRIBUTE_SELECTOR_PREFIX + att.trim() + ATTRIBUTE_SELECTOR_SUFFIX;
  }

  public static String attributeStartsWith(String att, String val) {
    Assert.notEmpty(val);
    return buildAttributeSelector(att, "^=", val);
  }

  public static String buildSelectors(Collection<String> selectors) {
    Assert.notNull(selectors);
    Assert.isTrue(!selectors.isEmpty());

    return BeeUtils.join(SELECTOR_SEPARATOR, selectors);
  }

  public static String buildSelectors(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 1);
    return ArrayUtils.join(SELECTOR_SEPARATOR, selectors);
  }

  public static String childCombinator(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 2);
    return ArrayUtils.join(" > ", selectors);
  }

  public static String classSelector(String value) {
    assertIdentifier(value);
    return CLASS_SELECTOR_PREFIX + value.trim();
  }

  public static String conjunction(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 2);
    return conjunction(Arrays.asList(selectors));
  }

  public static String conjunction(List<String> selectors) {
    Assert.notNull(selectors);
    Assert.isTrue(!selectors.isEmpty());

    StringBuilder sb = new StringBuilder();
    for (String selector : selectors) {
      if (!BeeUtils.isEmpty(selector)) {
        sb.append(selector.trim());
      }
    }
    return sb.toString();
  }

  public static String descendantCombinator(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 2);
    return ArrayUtils.join(BeeConst.STRING_SPACE, selectors);
  }

  public static String generalSiblingCombinator(String... selectors) {
    Assert.notNull(selectors);
    Assert.parameterCount(selectors.length, 2);
    return ArrayUtils.join(" ~ ", selectors);
  }

  public static Element getElement(Collection<String> selectors) {
    return getElement(buildSelectors(selectors));
  }

  public static Element getElement(Element root, Collection<String> selectors) {
    return getElement(root, buildSelectors(selectors));
  }

  public static Element getElement(Element root, String selectors) {
    assertSupported();
    Assert.notNull(root);
    Assert.notEmpty(selectors);
    return querySelector(root, selectors);
  }

  public static Element getElement(String selectors) {
    assertSupported();
    Assert.notEmpty(selectors);
    return querySelector(selectors);
  }

  public static Element getElement(UIObject root, Collection<String> selectors) {
    return getElement(root, buildSelectors(selectors));
  }

  public static Element getElement(UIObject root, String selectors) {
    Assert.notNull(root);
    return getElement(root.getElement(), selectors);
  }

  public static Element getElementByDataIndex(UIObject root, long idx) {
    return getElement(root, attributeEquals(DomUtils.ATTRIBUTE_DATA_INDEX, idx));
  }

  public static NodeList<Element> getNodes(Collection<String> selectors) {
    return getNodes(buildSelectors(selectors));
  }

  public static NodeList<Element> getNodes(Element root, Collection<String> selectors) {
    return getNodes(root, buildSelectors(selectors));
  }

  public static NodeList<Element> getNodes(Element root, String selectors) {
    assertSupported();
    Assert.notNull(root);
    Assert.notEmpty(selectors);
    return querySelectorAll(root, selectors);
  }

  public static NodeList<Element> getNodes(String selectors) {
    assertSupported();
    Assert.notEmpty(selectors);
    return querySelectorAll(selectors);
  }

  public static NodeList<Element> getNodes(UIObject root, Collection<String> selectors) {
    return getNodes(root, buildSelectors(selectors));
  }

  public static NodeList<Element> getNodes(UIObject root, String selectors) {
    Assert.notNull(root);
    return getNodes(root.getElement(), selectors);
  }

  public static String idSelector(String id) {
    assertIdentifier(id);
    return ID_SELECTOR_PREFIX + id.trim();
  }

  public static boolean isIdentifier(String s) {
    if (s == null) {
      return false;
    }
    String id = s.trim();
    if (id.isEmpty()) {
      return false;
    }

    for (int i = 0; i < id.length(); i++) {
      char ch = id.charAt(i);
      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch >= '\u00a0') {
        continue;
      }
      if (i > 0 && (ch >= '0' && ch <= '9' || ch == '-')) {
        continue;
      }
      return false;
    }
    return true;
  }

  public static String pseudoClassSelector(String value) {
    assertIdentifier(value);
    return PSEUDO_CLASS_SELECTOR_PREFIX + value.trim();
  }

  private static void assertIdentifier(String s) {
    Assert.isTrue(isIdentifier(s), "Invalid CSS identifier " + s);
  }

  private static void assertSupported() {
    Assert.isTrue(Features.supportsSelectors(), "Selectors API not supported");
  }

  private static String buildAttributeSelector(String att, String op, String val) {
    assertIdentifier(att);
    return ATTRIBUTE_SELECTOR_PREFIX + att.trim() + op.trim() + transformAttributeValue(val)
        + ATTRIBUTE_SELECTOR_SUFFIX;
  }

//@formatter:off
  private static native Element querySelector(Element root, String selectors) /*-{
    return root.querySelector(selectors);
  }-*/;

  private static native Element querySelector(String selectors) /*-{
    return $doc.querySelector(selectors);
  }-*/;

  private static native NodeList<Element> querySelectorAll(Element root, String selectors) /*-{
    return root.querySelectorAll(selectors);
  }-*/;

  private static native NodeList<Element> querySelectorAll(String selectors) /*-{
    return $doc.querySelectorAll(selectors);
  }-*/;
//@formatter:on

  private static String transformAttributeValue(String value) {
    Assert.notNull(value);
    if (value.isEmpty()) {
      return EMPTY_ATTRIBUTE_VALUE;
    }
    StringBuilder sb = new StringBuilder();
    String v = value.trim();

    if (value.indexOf(DOUBLE_QUOTE) < 0) {
      sb.append(DOUBLE_QUOTE).append(v).append(DOUBLE_QUOTE);
    } else if (BeeUtils.isDelimited(v, DOUBLE_QUOTE)) {
      sb.append(v);
    } else if (value.indexOf(SINGLE_QUOTE) < 0) {
      sb.append(SINGLE_QUOTE).append(v).append(SINGLE_QUOTE);
    } else if (BeeUtils.isDelimited(v, SINGLE_QUOTE)) {
      sb.append(v);
    } else {
      sb.append(DOUBLE_QUOTE);
      for (int i = 0; i < v.length(); i++) {
        char ch = v.charAt(i);
        if (ch == DOUBLE_QUOTE) {
          sb.append(BACKSLASH);
        }
        sb.append(ch);
      }
      sb.append(DOUBLE_QUOTE);
    }
    return sb.toString();
  }

  private Selectors() {
  }
}
