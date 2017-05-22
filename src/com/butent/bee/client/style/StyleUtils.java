package com.butent.bee.client.style;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.css.CssAngle;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.HasCssName;
import com.butent.bee.shared.css.values.BorderCollapse;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontStyle;
import com.butent.bee.shared.css.values.FontVariant;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.Overflow;
import com.butent.bee.shared.css.values.Position;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.TextTransform;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.StringPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elemental.js.JsBrowser;
import elemental.js.css.JsCSSRuleList;
import elemental.js.css.JsCSSStyleSheet;
import elemental.js.stylesheets.JsStyleSheetList;

/**
 * Contains utility functions used for working with Cascade Style Sheets (CSS).
 */

public final class StyleUtils {

  public enum ScrollBars {
    NONE, HORIZONTAL, VERTICAL, BOTH
  }

  public static final String DND_SOURCE = BeeConst.CSS_CLASS_PREFIX + "dndSource";
  public static final String DND_OVER = BeeConst.CSS_CLASS_PREFIX + "dndOver";

  public static final String STYLE_WIDTH = "width";
  public static final String STYLE_MIN_WIDTH = "minWidth";
  public static final String STYLE_MAX_WIDTH = "maxWidth";

  public static final String STYLE_HEIGHT = "height";
  public static final String STYLE_MIN_HEIGHT = "minHeight";
  public static final String STYLE_MAX_HEIGHT = "maxHeight";

  public static final String STYLE_LEFT = "left";
  public static final String STYLE_RIGHT = "right";
  public static final String STYLE_TOP = "top";
  public static final String STYLE_BOTTOM = "bottom";

  public static final String STYLE_BORDER_LEFT = "borderLeft";
  public static final String STYLE_BORDER_RIGHT = "borderRight";
  public static final String STYLE_BORDER_TOP = "borderTop";
  public static final String STYLE_BORDER_BOTTOM = "borderBottom";

  public static final String STYLE_BORDER_WIDTH = "borderWidth";
  public static final String STYLE_BORDER_STYLE = "borderStyle";
  public static final String STYLE_BORDER_COLOR = "borderColor";

  public static final String STYLE_OUTLINE_WIDTH = "outlineWidth";
  public static final String STYLE_OUTLINE_STYLE = "outlineStyle";
  public static final String STYLE_OUTLINE_COLOR = "outlineColor";

  public static final String STYLE_BORDER_COLLAPSE = "borderCollapse";
  public static final String STYLE_BORDER_SPACING = "borderSpacing";
  public static final String STYLE_TABLE_LAYOUT = "tableLayout";
  public static final String STYLE_VERTICAL_ALIGN = "verticalAlign";

  public static final String STYLE_OVERFLOW = "overflow";
  public static final String STYLE_OVERFLOW_X = "overflowX";
  public static final String STYLE_OVERFLOW_Y = "overflowY";

  public static final String STYLE_FONT_STYLE = "fontStyle";
  public static final String STYLE_FONT_VARIANT = "fontVariant";
  public static final String STYLE_FONT_WEIGHT = "fontWeight";
  public static final String STYLE_FONT_SIZE = "fontSize";
  public static final String STYLE_FONT_FAMILY = "fontFamily";

  public static final String STYLE_WHITE_SPACE = "whiteSpace";

  public static final String STYLE_PADDING = "padding";
  public static final String STYLE_PADDING_TOP = "paddingTop";
  public static final String STYLE_PADDING_RIGHT = "paddingRight";
  public static final String STYLE_PADDING_LEFT = "paddingLeft";
  public static final String STYLE_PADDING_BOTTOM = "paddingBottom";

  public static final String STYLE_MARGIN = "margin";
  public static final String STYLE_MARGIN_TOP = "marginTop";
  public static final String STYLE_MARGIN_RIGHT = "marginRight";
  public static final String STYLE_MARGIN_LEFT = "marginLeft";
  public static final String STYLE_MARGIN_BOTTOM = "marginBottom";

  public static final String STYLE_Z_INDEX = "zIndex";

  public static final String STYLE_POSITION = "position";

  public static final String STYLE_BACKGROUND_IMAGE = "backgroundImage";
  public static final String STYLE_BACKGROUND_COLOR = "backgroundColor";
  public static final String STYLE_COLOR = "color";

  public static final String STYLE_CURSOR = "cursor";

  public static final String STYLE_TEXT_ALIGN = "textAlign";
  public static final String STYLE_TEXT_DECORATION = "textDecoration";

  public static final String STYLE_LIST_IMAGE = "listStyleImage";
  public static final String STYLE_LIST_POSITION = "listStylePosition";
  public static final String STYLE_LIST_TYPE = "listStyleType";

  public static final String STYLE_CLIP = "clip";

  public static final String STYLE_LINE_HEIGHT = "lineHeight";
  public static final String STYLE_TEXT_TRANSFORM = "textTransform";
  public static final String STYLE_LETTER_SPACING = "letterSpacing";

  public static final String VALUE_AUTO = "auto";
  public static final String VALUE_FIXED = "fixed";
  public static final String VALUE_HIDDEN = "hidden";
  public static final String VALUE_INHERIT = "inherit";
  public static final String VALUE_INITIAL = "initial";
  public static final String VALUE_NONE = "none";

  public static final String SUFFIX_HORIZONTAL = "horizontal";
  public static final String SUFFIX_VERTICAL = "vertical";
  public static final String SUFFIX_FOCUSED = "focused";
  public static final String SUFFIX_DISABLED = "disabled";

  public static final String NAME_UNSELECTABLE = "unselectable";

  public static final String NAME_FLEX_BOX_HORIZONTAL = "flexBox-horizontal";
  public static final String NAME_FLEX_BOX_VERTICAL = "flexBox-vertical";
  public static final String NAME_FLEX_BOX_CENTER = "flexBox-center";
  public static final String NAME_FLEXIBLE = "flexible";

  public static final String NAME_ERROR = BeeConst.CSS_CLASS_PREFIX + "error";
  public static final String NAME_REQUIRED = BeeConst.CSS_CLASS_PREFIX + "required";
  public static final String NAME_HAS_DEFAULTS = BeeConst.CSS_CLASS_PREFIX + "hasDefaults";
  public static final String NAME_RESIZABLE = BeeConst.CSS_CLASS_PREFIX + "resizable";
  public static final String NAME_FOCUSABLE = BeeConst.CSS_CLASS_PREFIX + "focusable";
  public static final String NAME_DISABLED = BeeConst.CSS_CLASS_PREFIX + SUFFIX_DISABLED;
  public static final String NAME_LOADING = BeeConst.CSS_CLASS_PREFIX + "loading";
  public static final String NAME_POTENTIALLY_BOLD = BeeConst.CSS_CLASS_PREFIX + "potentially-bold";
  public static final String NAME_EMPTY = BeeConst.CSS_CLASS_PREFIX + "empty";

  public static final String NAME_TEXT_BOX = BeeConst.CSS_CLASS_PREFIX + "TextBox";
  public static final String NAME_FORM = BeeConst.CSS_CLASS_PREFIX + "Form";

  public static final String NAME_INFO_TABLE = BeeConst.CSS_CLASS_PREFIX + "info-table";

  public static final String NAME_ANIMATE_HOVER = BeeConst.CSS_CLASS_PREFIX + "animate-hover";
  public static final String NAME_ANIMATE_ACTIVE = BeeConst.CSS_CLASS_PREFIX + "animate-active";
  public static final String NAME_ACTIVE = BeeConst.CSS_CLASS_PREFIX + "active";

  public static final String NAME_LINK = BeeConst.CSS_CLASS_PREFIX + "InternalLink";

  public static final String TRANSFORM_ROTATE = "rotate";
  public static final String TRANSFORM_SCALE = "scale";
  public static final String TRANSFORM_SKEW = "skew";
  public static final String TRANSFORM_TRANSLATE = "translate";

  public static final SafeStyles PREFAB_POSITION_ABSOLUTE =
      buildStyle(STYLE_POSITION, Position.ABSOLUTE.getCssName());

  private static final char CLASS_NAME_SEPARATOR = ' ';
  private static final Splitter CLASS_NAME_SPLITTER =
      Splitter.on(CLASS_NAME_SEPARATOR).omitEmptyStrings().trimResults();
  private static final Joiner CLASS_NAME_JOINER = Joiner.on(CLASS_NAME_SEPARATOR);

  private static final char ADD_CLASS = '+';
  private static final char REMOVE_CLASS = '-';
  private static final char REPLACE_CLASS = '=';

  private static final String DEFINITION_SEPARATOR = ";";
  private static final String NAME_VALUE_SEPARATOR = ":";

  private static final Splitter DEFINITION_SPLITTER =
      Splitter.on(DEFINITION_SEPARATOR).omitEmptyStrings().trimResults();

  private static final String PROPERTY_CSS_TEXT = "cssText";

  private static final char NAME_DELIMITER = '-';

  private static final CssUnit DEFAULT_UNIT = CssUnit.PX;

  private static final String IMAGE_URL_PREFIX = "url(";
  private static final String IMAGE_URL_SUFFIX = ")";

  private static String styleTransform;

  public static int addClassName(Collection<? extends Element> elements, String className) {
    Assert.notNull(elements);
    Assert.notEmpty(className);

    int cnt = 0;
    for (Element el : elements) {
      if (el != null) {
        el.addClassName(className);
        cnt++;
      }
    }
    return cnt;
  }

  public static int addClassName(NodeList<Element> nodes, String className) {
    Assert.notNull(nodes);
    Assert.notEmpty(className);

    int cnt = nodes.getLength();
    for (int i = 0; i < cnt; i++) {
      nodes.getItem(i).addClassName(className);
    }
    return cnt;
  }

  public static String addClassName(String classes, String className) {
    Assert.notEmpty(className);
    if (BeeUtils.isEmpty(classes)) {
      return className.trim();
    }
    if (containsClassName(classes, className)) {
      return classes.trim();
    }

    return buildClasses(classes, className);
  }

  public static void addStyle(Element el, Style st) {
    Assert.notNull(el);
    Assert.notNull(st);

    String text = getCssText(st);
    if (!BeeUtils.isEmpty(text)) {
      updateStyle(el.getStyle(), text);
    }
  }

  public static void addStyleDependentName(Element el, String style) {
    setStyleDependentName(el, style, true);
  }

  public static void alwaysScroll(Element el, ScrollBars scroll) {
    Assert.notNull(el);
    alwaysScroll(el.getStyle(), scroll);
  }

  public static void alwaysScroll(Style st, ScrollBars scroll) {
    setOverflow(st, scroll, Overflow.SCROLL);
  }

  public static void alwaysScroll(UIObject obj, ScrollBars scroll) {
    Assert.notNull(obj);
    alwaysScroll(obj.getElement(), scroll);
  }

  public static void animateActive(Element el) {
    Assert.notNull(el);
    el.addClassName(NAME_ANIMATE_ACTIVE);
  }

  public static void animateActive(UIObject obj) {
    Assert.notNull(obj);
    animateActive(obj.getElement());
  }

  public static void animateHover(Element el) {
    Assert.notNull(el);
    el.addClassName(NAME_ANIMATE_HOVER);
  }

  public static void animateHover(UIObject obj) {
    Assert.notNull(obj);
    animateHover(obj.getElement());
  }

  public static void autoHeight(Element el) {
    Assert.notNull(el);
    autoHeight(el.getStyle());
  }

  public static void autoHeight(Style st) {
    Assert.notNull(st);
    st.setProperty(STYLE_HEIGHT, VALUE_AUTO);
  }

  public static void autoHeight(UIObject obj) {
    Assert.notNull(obj);
    autoHeight(obj.getElement());
  }

  public static void autoScroll(Element el, ScrollBars scroll) {
    Assert.notNull(el);
    autoScroll(el.getStyle(), scroll);
  }

  public static void autoScroll(Style st, ScrollBars scroll) {
    setOverflow(st, scroll, Overflow.AUTO);
  }

  public static void autoScroll(UIObject obj, ScrollBars scroll) {
    Assert.notNull(obj);
    autoScroll(obj.getElement(), scroll);
  }

  public static void autoWidth(Element el) {
    Assert.notNull(el);
    autoWidth(el.getStyle());
  }

  public static void autoWidth(Style st) {
    Assert.notNull(st);
    st.setProperty(STYLE_WIDTH, VALUE_AUTO);
  }

  public static void autoWidth(UIObject obj) {
    Assert.notNull(obj);
    autoWidth(obj.getElement());
  }

  public static SafeStyles buildBorderBottomWidth(double value, CssUnit unit) {
    return buildBorderBottomWidth(toCssLength(value, unit));
  }

  public static SafeStyles buildBorderBottomWidth(int width) {
    return buildBorderBottomWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildBorderBottomWidth(String value) {
    return buildStyle(CssProperties.BORDER_BOTTOM_WIDTH, value);
  }

  public static SafeStyles buildBorderLeftWidth(double value, CssUnit unit) {
    return buildBorderLeftWidth(toCssLength(value, unit));
  }

  public static SafeStyles buildBorderLeftWidth(int width) {
    return buildBorderLeftWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildBorderLeftWidth(String value) {
    return buildStyle(CssProperties.BORDER_LEFT_WIDTH, value);
  }

  public static SafeStyles buildBorderRightWidth(double value, CssUnit unit) {
    return buildBorderRightWidth(toCssLength(value, unit));
  }

  public static SafeStyles buildBorderRightWidth(int width) {
    return buildBorderRightWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildBorderRightWidth(String value) {
    return buildStyle(CssProperties.BORDER_RIGHT_WIDTH, value);
  }

  public static SafeStyles buildBorderTopWidth(double value, CssUnit unit) {
    return buildBorderTopWidth(toCssLength(value, unit));
  }

  public static SafeStyles buildBorderTopWidth(int width) {
    return buildBorderTopWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildBorderTopWidth(String value) {
    return buildStyle(CssProperties.BORDER_TOP_WIDTH, value);
  }

  public static SafeStyles buildBorderWidth(double value, CssUnit unit) {
    return buildBorderWidth(toCssLength(value, unit));
  }

  public static SafeStyles buildBorderWidth(int width) {
    return buildBorderWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildBorderWidth(String value) {
    return buildStyle(CssProperties.BORDER_WIDTH, value);
  }

  public static String buildClasses(Collection<String> styleNames) {
    Assert.notNull(styleNames);
    return CLASS_NAME_JOINER.join(styleNames.stream().filter(StringPredicate.NOT_EMPTY).iterator());
  }

  public static String buildClasses(String... styleNames) {
    return ArrayUtils.join(String.valueOf(CLASS_NAME_SEPARATOR), styleNames);
  }

  public static SafeStyles buildFontFamily(String family) {
    return buildStyle(CssProperties.FONT_FAMILY, family);
  }

  public static SafeStyles buildFontSize(double size, CssUnit unit) {
    Assert.isPositive(size);
    return buildStyle(CssProperties.FONT_SIZE, toCssLength(size, normalizeUnit(unit)));
  }

  public static SafeStyles buildFontSize(FontSize size) {
    Assert.notNull(size);
    return buildStyle(CssProperties.FONT_SIZE, size.getCssName());
  }

  public static SafeStyles buildFontStyle(FontStyle style) {
    Assert.notNull(style);
    return buildStyle(CssProperties.FONT_STYLE, style.getCssName());
  }

  public static SafeStyles buildFontVariant(FontVariant variant) {
    Assert.notNull(variant);
    return buildStyle(CssProperties.FONT_VARIANT, variant.getCssName());
  }

  public static SafeStyles buildFontWeight(FontWeight weight) {
    Assert.notNull(weight);
    return buildStyle(CssProperties.FONT_WEIGHT, weight.getCssName());
  }

  public static SafeStyles buildHeight(double value, CssUnit unit) {
    return buildStyle(STYLE_HEIGHT, toCssLength(value, unit));
  }

  public static SafeStyles buildHeight(int height) {
    return buildHeight(height, DEFAULT_UNIT);
  }

  public static SafeStyles buildLeft(double value, CssUnit unit) {
    return buildStyle(STYLE_LEFT, toCssLength(value, unit));
  }

  public static SafeStyles buildLeft(int left) {
    return buildLeft(left, DEFAULT_UNIT);
  }

  public static SafeStyles buildLetterSpacing(String value) {
    return buildStyle(CssProperties.LETTER_SPACING, value);
  }

  public static SafeStyles buildLineHeight(String value) {
    return buildStyle(CssProperties.LINE_HEIGHT, value);
  }

  public static SafeStyles buildLineHeight(int px) {
    return buildLineHeight(toCssLength(px, DEFAULT_UNIT));
  }

  public static SafeStyles buildMargin(String value) {
    return buildStyle(STYLE_MARGIN, value);
  }

  public static SafeStyles buildPadding(String value) {
    return buildStyle(STYLE_PADDING, value);
  }

  public static String buildRule(String selector, SafeStyles... styles) {
    Assert.notEmpty(selector);
    Assert.notNull(styles);
    Assert.parameterCount(styles.length, 1);

    StringBuilder sb = new StringBuilder();
    sb.append(selector).append(BeeConst.STRING_LEFT_BRACE);

    for (SafeStyles style : styles) {
      if (style != null) {
        sb.append(style.asString());
      }
    }

    sb.append(BeeConst.STRING_RIGHT_BRACE);
    return sb.toString();
  }

  public static SafeStyles buildStyle(SafeStyles... styles) {
    Assert.notNull(styles);
    Assert.parameterCount(styles.length, 1);

    SafeStylesBuilder ssb = new SafeStylesBuilder();
    for (SafeStyles style : styles) {
      if (style != null) {
        ssb.append(style);
      }
    }
    return ssb.toSafeStyles();
  }

  public static SafeStyles buildStyle(String name, String value) {
    Assert.notEmpty(name);
    Assert.notEmpty(value);
    return SafeStylesUtils.fromTrustedString(name + NAME_VALUE_SEPARATOR + value
        + DEFINITION_SEPARATOR);
  }

  public static SafeStyles buildStyle(String name, int px) {
    return buildStyle(name, px, DEFAULT_UNIT);
  }

  public static SafeStyles buildStyle(String name, double value, CssUnit unit) {
    return buildStyle(name, toCssLength(value, unit));
  }

  public static SafeStyles buildStyle(String n1, String v1, String n2, String v2) {
    return buildStyle(buildStyle(n1, v1), buildStyle(n2, v2));
  }

  public static SafeStyles buildTextTransform(TextTransform textTransform) {
    Assert.notNull(textTransform);
    return buildStyle(CssProperties.TEXT_TRANSFORM, textTransform.getCssName());
  }

  public static SafeStyles buildTop(double value, CssUnit unit) {
    return buildStyle(STYLE_TOP, toCssLength(value, unit));
  }

  public static SafeStyles buildTop(int top) {
    return buildTop(top, DEFAULT_UNIT);
  }

  public static SafeStyles buildWidth(double value, CssUnit unit) {
    return buildStyle(STYLE_WIDTH, toCssLength(value, unit));
  }

  public static SafeStyles buildWidth(int width) {
    return buildWidth(width, DEFAULT_UNIT);
  }

  public static SafeStyles buildZIndex(int value) {
    return buildStyle(CssProperties.Z_INDEX, BeeUtils.toString(value));
  }

  public static <E extends Enum<?> & HasCssName> String className(E value) {
    Assert.notNull(value);
    return BeeConst.CSS_CLASS_PREFIX + NameUtils.getClassName(value.getDeclaringClass())
        + NAME_DELIMITER + value.getCssName().replace(BeeConst.CHAR_SPACE, NAME_DELIMITER);
  }

  public static void clearClip(Element el) {
    clearClip(Assert.notNull(el).getStyle());
  }

  public static void clearClip(Style st) {
    Assert.notNull(st).clearProperty(STYLE_CLIP);
  }

  public static void clearClip(UIObject obj) {
    clearClip(Assert.notNull(obj).getElement());
  }

  public static void clearDisplay(Element el) {
    Assert.notNull(el);
    if (!BeeUtils.isEmpty(el.getStyle().getDisplay())) {
      el.getStyle().clearDisplay();
    }
  }

  public static void clearDisplay(UIObject obj) {
    Assert.notNull(obj);
    clearDisplay(obj.getElement());
  }

  public static void clearHeight(Element el) {
    Assert.notNull(el);
    el.getStyle().clearHeight();
  }

  public static void clearHeight(UIObject obj) {
    Assert.notNull(obj);
    clearHeight(obj.getElement());
  }

  public static void clearProperties(Element el, String... names) {
    Assert.notNull(el);
    clearProperties(el.getStyle(), names);
  }

  public static void clearProperties(Style st, String... names) {
    Assert.notNull(st);
    Assert.notNull(names);

    for (String name : names) {
      if (!BeeUtils.isEmpty(name)) {
        clearStyleProperty(st, name);
      }
    }
  }

  public static void clearTableLayout(Element el) {
    Assert.notNull(el);
    clearTableLayout(el.getStyle());
  }

  public static void clearTableLayout(Style st) {
    Assert.notNull(st);
    st.clearProperty(STYLE_TABLE_LAYOUT);
  }

  public static void clearTableLayout(UIObject obj) {
    Assert.notNull(obj);
    clearTableLayout(obj.getElement());
  }

  public static void clearTransform(Style st) {
    Assert.notNull(st);
    st.clearProperty(getStyleTransformPropertyName(st));
  }

  public static void clearWidth(Element el) {
    Assert.notNull(el);
    el.getStyle().clearWidth();
  }

  public static void clearWidth(UIObject obj) {
    Assert.notNull(obj);
    clearWidth(obj.getElement());
  }

  public static void collapseBorders(Element el) {
    Assert.notNull(el);
    collapseBorders(el.getStyle());
  }

  public static void collapseBorders(Style st) {
    Assert.notNull(st);
    st.setProperty(STYLE_BORDER_COLLAPSE, BorderCollapse.COLLAPSE.getCssName());
  }

  public static void collapseBorders(UIObject obj) {
    Assert.notNull(obj);
    collapseBorders(obj.getElement());
  }

  public static boolean containsClassName(String classes, String className) {
    return indexOfClassName(className, classes) >= 0;
  }

  public static void copyBorder(Style src, Style dst) {
    copyProperties(src, dst, STYLE_BORDER_WIDTH, STYLE_BORDER_STYLE, STYLE_BORDER_COLOR,
        STYLE_BORDER_LEFT, STYLE_BORDER_RIGHT, STYLE_BORDER_TOP, STYLE_BORDER_BOTTOM);
  }

  public static void copyBox(Element src, Element dst) {
    Assert.notNull(src);
    Assert.notNull(dst);
    if (src.equals(dst)) {
      return;
    }
    copyBox(src.getStyle(), dst.getStyle());
  }

  public static void copyBox(Style src, Style dst) {
    copyRectangle(src, dst);
    copyPadding(src, dst);
    copyBorder(src, dst);
    copyMargin(src, dst);
  }

  public static void copyFont(Element src, Element dst) {
    Assert.notNull(src);
    Assert.notNull(dst);
    if (src.equals(dst)) {
      return;
    }
    copyFont(src.getStyle(), dst.getStyle());
  }

  public static void copyFont(Style src, Style dst) {
    copyProperties(src, dst, STYLE_FONT_STYLE, STYLE_FONT_VARIANT, STYLE_FONT_WEIGHT,
        STYLE_FONT_SIZE, STYLE_FONT_FAMILY, STYLE_LINE_HEIGHT, STYLE_TEXT_TRANSFORM,
        STYLE_LETTER_SPACING);
  }

  public static void copyMargin(Style src, Style dst) {
    copyProperties(src, dst, STYLE_MARGIN,
        STYLE_MARGIN_LEFT, STYLE_MARGIN_RIGHT, STYLE_MARGIN_TOP, STYLE_MARGIN_BOTTOM);
  }

  public static void copyPadding(Style src, Style dst) {
    copyProperties(src, dst, STYLE_PADDING,
        STYLE_PADDING_LEFT, STYLE_PADDING_RIGHT, STYLE_PADDING_TOP, STYLE_PADDING_BOTTOM);
  }

  public static void copyProperties(Element src, Element dst, String... names) {
    Assert.notNull(src);
    Assert.notNull(dst);
    if (src.equals(dst)) {
      return;
    }
    copyProperties(src.getStyle(), dst.getStyle(), names);
  }

  public static void copyProperties(Style src, Style dst, String... names) {
    Assert.notNull(src);
    Assert.notNull(dst);
    Assert.notNull(names);
    Assert.isPositive(names.length);
    if (src.equals(dst)) {
      return;
    }

    for (String name : names) {
      if (!BeeUtils.isEmpty(name)) {
        copyStyleProperty(src, dst, name);
      }
    }
  }

  public static void copyRectangle(Element src, Element dst) {
    Assert.notNull(src);
    Assert.notNull(dst);
    if (src.equals(dst)) {
      return;
    }
    copyRectangle(src.getStyle(), dst.getStyle());
  }

  public static void copyRectangle(Style src, Style dst) {
    copyProperties(src, dst, STYLE_LEFT, STYLE_RIGHT, STYLE_TOP, STYLE_BOTTOM,
        STYLE_WIDTH, STYLE_HEIGHT);
  }

  public static void copySize(Element src, Element dst) {
    Assert.notNull(src);
    Assert.notNull(dst);
    if (src.equals(dst)) {
      return;
    }
    copySize(src.getStyle(), dst.getStyle());
  }

  public static void copySize(Style src, Style dst) {
    copyProperties(src, dst, STYLE_WIDTH, STYLE_HEIGHT);
  }

  public static void enableAnimation(Action action, UIObject obj) {
    Assert.notNull(action);

    animateHover(obj);
    if (action.animate()) {
      animateActive(obj);
    }
  }

  public static void fillHorizontal(Element el) {
    Assert.notNull(el);
    fillHorizontal(el.getStyle());
  }

  public static void fillHorizontal(Style st) {
    Assert.notNull(st);

    if (!isZero(st.getLeft())) {
      setLeft(st, 0, DEFAULT_UNIT);
    }
    if (hasProperty(st, STYLE_RIGHT)) {
      st.clearRight();
    }

    setWidth(st, 100, CssUnit.PCT);
  }

  public static void fillHorizontal(UIObject obj) {
    Assert.notNull(obj);
    fillHorizontal(obj.getElement());
  }

  public static void fillVertical(Element el) {
    Assert.notNull(el);
    fillVertical(el.getStyle());
  }

  public static void fillVertical(Style st) {
    Assert.notNull(st);

    if (!isZero(st.getTop())) {
      setTop(st, 0, DEFAULT_UNIT);
    }
    if (hasProperty(st, STYLE_BOTTOM)) {
      st.clearBottom();
    }

    setHeight(st, 100, CssUnit.PCT);
  }

  public static void fillVertical(UIObject obj) {
    Assert.notNull(obj);
    fillVertical(obj.getElement());
  }

  public static void fixedTableLayout(Element el) {
    Assert.notNull(el);
    fixedTableLayout(el.getStyle());
  }

  public static void fixedTableLayout(Style st) {
    Assert.notNull(st);
    st.setProperty(STYLE_TABLE_LAYOUT, VALUE_FIXED);
  }

  public static void fixedTableLayout(UIObject obj) {
    Assert.notNull(obj);
    fixedTableLayout(obj.getElement());
  }

  public static void fullHeight(Element el) {
    Assert.notNull(el);
    fullHeight(el.getStyle());
  }

  public static void fullHeight(Style st) {
    Assert.notNull(st);
    setHeight(st, 100, CssUnit.PCT);
  }

  public static void fullHeight(UIObject obj) {
    Assert.notNull(obj);
    fullHeight(obj.getElement());
  }

  public static void fullWidth(Element el) {
    Assert.notNull(el);
    fullWidth(el.getStyle());
  }

  public static void fullWidth(Style st) {
    Assert.notNull(st);
    setWidth(st, 100, CssUnit.PCT);
  }

  public static void fullWidth(UIObject obj) {
    Assert.notNull(obj);
    fullWidth(obj.getElement());
  }

  public static String getBackgroundImage(Element el) {
    Assert.notNull(el);
    return getBackgroundImage(el.getStyle());
  }

  public static String getBackgroundImage(Style st) {
    Assert.notNull(st);
    String url = st.getBackgroundImage();

    if (BeeUtils.isEmpty(url)) {
      return null;
    } else if (url.startsWith(IMAGE_URL_PREFIX) && url.endsWith(IMAGE_URL_SUFFIX)) {
      return url.substring(IMAGE_URL_PREFIX.length(), url.length() - IMAGE_URL_SUFFIX.length());
    } else {
      return url;
    }
  }

  public static String getBackgroundImage(UIObject obj) {
    Assert.notNull(obj);
    return getBackgroundImage(obj.getElement());
  }

  public static int getBottom(Element el) {
    Assert.notNull(el);
    return getBottom(el.getStyle());
  }

  public static int getBottom(Style st) {
    Assert.notNull(st);
    return BeeUtils.val(st.getBottom(), false);
  }

  public static int getBottom(UIObject obj) {
    Assert.notNull(obj);
    return getBottom(obj.getElement());
  }

  public static List<String> getClassNames(Element el) {
    Assert.notNull(el);
    return splitClasses(DomUtils.getClassName(el));
  }

  public static String getCssText(Element el) {
    Assert.notNull(el);
    return getCssText(el.getStyle());
  }

  public static String getCssText(Style st) {
    Assert.notNull(st);
    return st.getProperty(PROPERTY_CSS_TEXT);
  }

  public static String getCssText(UIObject obj) {
    Assert.notNull(obj);
    return getCssText(obj.getElement());
  }

  public static int getHeight(Element el) {
    Assert.notNull(el);
    return getHeight(el.getStyle());
  }

  public static int getHeight(Style st) {
    Assert.notNull(st);
    return BeeUtils.val(st.getHeight(), false);
  }

  public static int getHeight(UIObject obj) {
    Assert.notNull(obj);
    return getHeight(obj.getElement());
  }

  public static int getLeft(Element el) {
    Assert.notNull(el);
    return getLeft(el.getStyle());
  }

  public static int getLeft(Style st) {
    Assert.notNull(st);
    return BeeUtils.val(st.getLeft(), false);
  }

  public static int getLeft(UIObject obj) {
    Assert.notNull(obj);
    return getLeft(obj.getElement());
  }

  public static int getParentZIndex(Element el, boolean computed, boolean incl) {
    return BeeUtils.toInt(getParentProperty(el, STYLE_Z_INDEX, computed, null, incl));
  }

  public static int getParentZIndex(UIObject obj, boolean computed, boolean incl) {
    Assert.notNull(obj);
    return getParentZIndex(obj.getElement(), computed, incl);
  }

  public static Rectangle getRectangle(Element el) {
    Assert.notNull(el);
    return getRectangle(el.getStyle());
  }

  public static Rectangle getRectangle(Style st) {
    Assert.notNull(st);
    return new Rectangle(st);
  }

  public static Rectangle getRectangle(UIObject obj) {
    Assert.notNull(obj);
    return getRectangle(obj.getElement());
  }

  public static String getRules() {
    JsStyleSheetList sheets = JsBrowser.getDocument().getStyleSheets();
    if (sheets == null) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < sheets.getLength(); i++) {
      JsCSSStyleSheet sheet = (JsCSSStyleSheet) sheets.item(i);
      if (sheet == null) {
        continue;
      }

      JsCSSRuleList rules = sheet.getCssRules();
      if (rules == null) {
        rules = sheet.getRules();
        if (rules == null) {
          continue;
        }
      }

      for (int j = 0; j < rules.length(); j++) {
        String text = rules.item(j).getCssText();
        if (!BeeUtils.isEmpty(text)) {
          sb.append(' ').append(text);
        }
      }
    }
    return sb.toString();
  }

  public static String getStylePrimaryName(Element el) {
    Assert.notNull(el);
    String className = DomUtils.getClassName(el);
    if (BeeUtils.isEmpty(className)) {
      return BeeConst.STRING_EMPTY;
    }

    int idx = className.indexOf(CLASS_NAME_SEPARATOR);
    if (idx >= 0) {
      return className.substring(0, idx);
    }
    return className;
  }

  public static int getTop(Element el) {
    Assert.notNull(el);
    return getTop(el.getStyle());
  }

  public static int getTop(Style st) {
    Assert.notNull(st);
    return BeeUtils.val(st.getTop(), false);
  }

  public static int getTop(UIObject obj) {
    Assert.notNull(obj);
    return getTop(obj.getElement());
  }

  public static int getWidth(Element el) {
    Assert.notNull(el);
    return getWidth(el.getStyle());
  }

  public static int getWidth(Style st) {
    Assert.notNull(st);
    return BeeUtils.val(st.getWidth(), false);
  }

  public static int getWidth(UIObject obj) {
    Assert.notNull(obj);
    return getWidth(obj.getElement());
  }

  public static int getZIndex(Element el) {
    Assert.notNull(el);
    return getZIndex(el.getStyle());
  }

  public static int getZIndex(Style st) {
    Assert.notNull(st);
    return BeeUtils.toInt(JsUtils.getProperty(st, STYLE_Z_INDEX));
  }

  public static int getZIndex(UIObject obj) {
    Assert.notNull(obj);
    return getZIndex(obj.getElement());
  }

  public static boolean hasAnyClass(Element el, Collection<String> classes) {
    Assert.notNull(classes);

    boolean ok = false;
    for (String className : classes) {
      if (hasClassName(el, className)) {
        ok = true;
        break;
      }
    }
    return ok;
  }

  public static boolean hasClassName(Element el, String className) {
    if (el == null || BeeUtils.isEmpty(className)) {
      return false;
    }
    return containsClassName(DomUtils.getClassName(el), className);
  }

  public static void hideDisplay(Element el) {
    Assert.notNull(el);
    setProperty(el.getStyle(), CssProperties.DISPLAY, Display.NONE);
  }

  public static void hideDisplay(String id) {
    hideDisplay(DomUtils.getElement(id));
  }

  public static void hideDisplay(UIObject obj) {
    Assert.notNull(obj);
    hideDisplay(obj.getElement());
  }

  public static void hideOutline(Element el) {
    Assert.notNull(el);
    hideOutline(el.getStyle());
  }

  public static void hideOutline(Style st) {
    setOutlineStyle(st, BorderStyle.NONE);
  }

  public static void hideOutline(UIObject obj) {
    Assert.notNull(obj);
    hideOutline(obj.getElement());
  }

  public static void hideScroll(Element el) {
    hideScroll(el, ScrollBars.BOTH);
  }

  public static void hideScroll(Element el, ScrollBars scroll) {
    Assert.notNull(el);
    hideScroll(el.getStyle(), scroll);
  }

  public static void hideScroll(Style st) {
    hideScroll(st, ScrollBars.BOTH);
  }

  public static void hideScroll(Style st, ScrollBars scroll) {
    setOverflow(st, scroll, Overflow.HIDDEN);
  }

  public static void hideScroll(UIObject obj) {
    hideScroll(obj, ScrollBars.BOTH);
  }

  public static void hideScroll(UIObject obj, ScrollBars scroll) {
    Assert.notNull(obj);
    hideScroll(obj.getElement(), scroll);
  }

  public static void insertClassName(Element el, String className, String beforeName) {
    Assert.notNull(el);
    Assert.notEmpty(className);
    Assert.notEmpty(beforeName);
    Assert.isFalse(BeeUtils.same(className, beforeName));

    String classes = DomUtils.getClassName(el);
    int beforeIndex = indexOfClassName(beforeName, classes);
    if (beforeIndex < 0) {
      el.addClassName(className);
      return;
    }

    int classIndex = indexOfClassName(className, classes);
    if (classIndex >= 0 && classIndex == beforeIndex - 1) {
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (String name : CLASS_NAME_SPLITTER.split(classes.trim())) {
      if (classIndex >= 0 && BeeUtils.same(name, className)) {
        continue;
      }
      if (BeeUtils.same(name, beforeName)) {
        if (sb.length() > 0) {
          sb.append(CLASS_NAME_SEPARATOR);
        }
        sb.append(className);
      }

      if (sb.length() > 0) {
        sb.append(CLASS_NAME_SEPARATOR);
      }
      sb.append(name);
    }

    el.setClassName(sb.toString());
  }

  public static int insertClassName(NodeList<Element> nodes, String className, String beforeName) {
    Assert.notNull(nodes);

    int cnt = nodes.getLength();
    for (int i = 0; i < cnt; i++) {
      insertClassName(nodes.getItem(i), className, beforeName);
    }
    return cnt;
  }

  public static boolean isPositioned(Style st) {
    Assert.notNull(st);
    String value = st.getPosition();
    if (BeeUtils.isEmpty(value)) {
      return false;
    }

    Position position = parseCssName(Position.class, value);
    return position != null && !Position.STATIC.equals(position);
  }

  public static boolean isUnitFragment(char ch) {
    boolean ok = false;

    for (CssUnit constant : CssUnit.class.getEnumConstants()) {
      if (constant.getCaption().indexOf(ch) >= 0) {
        ok = true;
        break;
      }
    }
    return ok;
  }

  public static String joinName(String prefix, String suffix) {
    Assert.notEmpty(prefix);
    Assert.notEmpty(suffix);
    return prefix.trim() + NAME_DELIMITER + suffix.trim();
  }

  public static void makeAbsolute(Element el) {
    Assert.notNull(el);
    makeAbsolute(el.getStyle());
  }

  public static void makeAbsolute(Style st) {
    Assert.notNull(st);
    setProperty(st, CssProperties.POSITION, Position.ABSOLUTE);
  }

  public static void makeAbsolute(UIObject obj) {
    Assert.notNull(obj);
    makeAbsolute(obj.getElement());
  }

  public static void makeFlexible(Element el) {
    Assert.notNull(el);
    el.addClassName(NAME_FLEXIBLE);
  }

  public static void makeFlexible(UIObject obj) {
    Assert.notNull(obj);
    obj.addStyleName(NAME_FLEXIBLE);
  }

  public static void makeRelative(Element el) {
    Assert.notNull(el);
    setProperty(el.getStyle(), CssProperties.POSITION, Position.RELATIVE);
  }

  public static void makeRelative(UIObject obj) {
    Assert.notNull(obj);
    makeRelative(obj.getElement());
  }

  public static void occupy(Element el) {
    Assert.notNull(el);
    occupy(el.getStyle());
  }

  public static void occupy(Style st) {
    Assert.notNull(st);
    setProperty(st, CssProperties.POSITION, Position.ABSOLUTE);

    setLeft(st, 0, CssUnit.PX);
    setRight(st, 0, CssUnit.PX);
    setTop(st, 0, CssUnit.PX);
    setBottom(st, 0, CssUnit.PX);
  }

  public static void occupy(UIObject obj) {
    Assert.notNull(obj);
    occupy(obj.getElement());
  }

  public static Pair<Double, CssUnit> parseCssLength(String input) {
    Assert.notEmpty(input);
    Double value = null;
    CssUnit unit = null;

    String s = input.trim();
    int p = s.length() - 1;
    while (p >= 0 && isUnitFragment(s.charAt(p))) {
      p--;
    }

    if (p >= 0 && p < s.length() - 1) {
      String v = s.substring(0, p + 1).trim();
      String u = s.substring(p + 1).trim();
      if (BeeUtils.isDouble(v)) {
        value = BeeUtils.toDouble(v);
        unit = CssUnit.parse(u);
      }
    }
    return Pair.of(value, unit);
  }

  public static <E extends Enum<?> & HasCssName> E parseCssName(Class<E> clazz, String input) {
    Assert.notNull(clazz);

    for (E constant : clazz.getEnumConstants()) {
      if (BeeUtils.same(constant.getCssName(), input)) {
        return constant;
      }
    }
    return null;
  }

  public static FontSize parseFontSize(String input) {
    return parseCssName(FontSize.class, input);
  }

  public static FontStyle parseFontStyle(String input) {
    return parseCssName(FontStyle.class, input);
  }

  public static FontVariant parseFontVariant(String input) {
    return parseCssName(FontVariant.class, input);
  }

  public static FontWeight parseFontWeight(String input) {
    return parseCssName(FontWeight.class, input);
  }

  public static ScrollBars parseScrollBars(String input) {
    Assert.notEmpty(input);

    for (ScrollBars sb : ScrollBars.values()) {
      if (BeeUtils.same(sb.name(), input)) {
        return sb;
      }
    }
    return null;
  }

  public static ScrollBars parseScrollBars(String input, ScrollBars def) {
    if (BeeUtils.isEmpty(input)) {
      return def;
    }
    ScrollBars sb = parseScrollBars(input);
    if (sb == null) {
      return def;
    }
    return sb;
  }

  public static List<Property> parseStyles(String styles) {
    Assert.notEmpty(styles);
    List<Property> result = new ArrayList<>();

    for (String style : DEFINITION_SPLITTER.split(styles)) {
      String name = BeeUtils.getPrefix(style, NAME_VALUE_SEPARATOR);
      String value = BeeUtils.getSuffix(style, NAME_VALUE_SEPARATOR);
      if (!BeeUtils.isEmpty(name) && !BeeUtils.isEmpty(value)) {
        result.add(new Property(name, value));
      }
    }
    return result;
  }

  public static TextAlign parseTextAlign(String input) {
    return parseCssName(TextAlign.class, input);
  }

  public static TextTransform parseTextTransform(String input) {
    return parseCssName(TextTransform.class, input);
  }

  public static VerticalAlign parseVerticalAlign(String input) {
    return parseCssName(VerticalAlign.class, input);
  }

  public static WhiteSpace parseWhiteSpace(String input) {
    return parseCssName(WhiteSpace.class, input);
  }

  public static int removeClassName(Collection<? extends Element> elements, String className) {
    Assert.notNull(elements);
    Assert.notEmpty(className);

    int cnt = 0;
    for (Element el : elements) {
      if (el != null) {
        el.removeClassName(className);
        cnt++;
      }
    }
    return cnt;
  }

  public static int removeClassName(NodeList<Element> nodes, String className) {
    Assert.notNull(nodes);
    Assert.notEmpty(className);

    int cnt = nodes.getLength();
    for (int i = 0; i < cnt; i++) {
      nodes.getItem(i).removeClassName(className);
    }
    return cnt;
  }

  public static String removeClassName(String classes, String className) {
    Assert.notEmpty(className);
    if (BeeUtils.isEmpty(classes)) {
      return BeeConst.STRING_EMPTY;
    }
    if (!BeeUtils.containsSame(classes, className)) {
      return classes.trim();
    }

    StringBuilder sb = new StringBuilder();
    for (String name : CLASS_NAME_SPLITTER.split(classes.trim())) {
      if (!BeeUtils.same(name, className)) {
        if (sb.length() > 0) {
          sb.append(CLASS_NAME_SEPARATOR);
        }
        sb.append(name);
      }
    }

    return sb.toString();
  }

  public static void removeStyleDependentName(Element el, String style) {
    setStyleDependentName(el, style, false);
  }

  public static void restartAnimation(Element el, String className) {
    Assert.notNull(el);
    Assert.notEmpty(className);

    if (el.hasClassName(className)) {
      el.removeClassName(className);

      if (el.getOffsetHeight() >= 0) { // trigger reflow
        el.addClassName(className);
      }

    } else {
      el.addClassName(className);
    }
  }

  public static void setBackgroundColor(Element el, String color) {
    Assert.notNull(el);
    setBackgroundColor(el.getStyle(), color);
  }

  public static void setBackgroundColor(Style st, String color) {
    Assert.notNull(st);
    if (BeeUtils.isEmpty(color)) {
      st.clearBackgroundColor();
    } else {
      st.setBackgroundColor(color);
    }
  }

  public static void setBackgroundColor(UIObject obj, String color) {
    Assert.notNull(obj);
    setBackgroundColor(obj.getElement(), color);
  }

  public static void setBackgroundImage(Element el, String url) {
    Assert.notNull(el);
    setBackgroundImage(el.getStyle(), url);
  }

  public static void setBackgroundImage(Style st, String url) {
    Assert.notNull(st);
    if (BeeUtils.isEmpty(url)) {
      st.clearBackgroundImage();
    } else {
      st.setBackgroundImage(IMAGE_URL_PREFIX + url.trim() + IMAGE_URL_SUFFIX);
    }
  }

  public static void setBackgroundImage(UIObject obj, String url) {
    Assert.notNull(obj);
    setBackgroundImage(obj.getElement(), url);
  }

  public static void setBorderColor(Element el, String color) {
    Assert.notNull(el);
    setBorderColor(el.getStyle(), color);
  }

  public static void setBorderColor(Style st, String color) {
    Assert.notNull(st);
    if (BeeUtils.isEmpty(color)) {
      st.clearBorderColor();
    } else {
      st.setBorderColor(color);
    }
  }

  public static void setBorderColor(UIObject obj, String color) {
    Assert.notNull(obj);
    setBorderColor(obj.getElement(), color);
  }

  public static void setBorderSpacing(Element el, int px) {
    Assert.notNull(el);
    setBorderSpacing(el.getStyle(), px);
  }

  public static void setBorderSpacing(Style st, int px) {
    Assert.notNull(st);
    Assert.nonNegative(px);
    st.setPropertyPx(STYLE_BORDER_SPACING, px);
  }

  public static void setBorderSpacing(UIObject obj, int px) {
    Assert.notNull(obj);
    setBorderSpacing(obj.getElement(), px);
  }

  public static void setBorderStyle(Element el, BorderStyle value) {
    Assert.notNull(el);
    setBorderStyle(el.getStyle(), value);
  }

  public static void setBorderStyle(Style st, BorderStyle value) {
    Assert.notNull(st);
    Assert.notNull(value);
    st.setProperty(STYLE_BORDER_STYLE, value.getCssName());
  }

  public static void setBorderStyle(UIObject obj, BorderStyle value) {
    Assert.notNull(obj);
    setBorderStyle(obj.getElement(), value);
  }

  public static void setBorderWidth(Element el, int px) {
    Assert.notNull(el);
    setBorderWidth(el.getStyle(), px);
  }

  public static void setBorderWidth(Style st, int px) {
    Assert.notNull(st);
    Assert.nonNegative(px);
    st.setPropertyPx(STYLE_BORDER_WIDTH, px);
  }

  public static void setBorderWidth(UIObject obj, int px) {
    Assert.notNull(obj);
    setBorderWidth(obj.getElement(), px);
  }

  public static void setBottom(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setBottom(el.getStyle(), value, unit);
  }

  public static void setBottom(Element el, int px) {
    Assert.notNull(el);
    setBottom(el.getStyle(), px);
  }

  public static void setBottom(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_BOTTOM, value, normalizeUnit(unit));
  }

  public static void setBottom(Style st, int px) {
    setBottom(st, px, DEFAULT_UNIT);
  }

  public static void setBottom(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setBottom(obj.getElement(), value, unit);
  }

  public static void setBottom(UIObject obj, int px) {
    Assert.notNull(obj);
    setBottom(obj.getElement(), px);
  }

  public static void setClip(Element el, Edges edges) {
    setClip(Assert.notNull(el).getStyle(), edges);
  }

  public static void setClip(Element el, int top, int right, int bottom, int left) {
    setClip(Assert.notNull(el).getStyle(), top, right, bottom, left);
  }

  public static void setClip(Style st, Edges edges) {
    Assert.notNull(st).setProperty(STYLE_CLIP, Assert.notNull(edges).getCssShape());
  }

  public static void setClip(Style st, int top, int right, int bottom, int left) {
    setClip(st, new Edges(top, right, bottom, left));
  }

  public static void setClip(UIObject obj, Edges edges) {
    setClip(Assert.notNull(obj).getElement(), edges);
  }

  public static void setClip(UIObject obj, int top, int right, int bottom, int left) {
    setClip(Assert.notNull(obj).getElement(), top, right, bottom, left);
  }

  public static void setColor(Element el, String color) {
    Assert.notNull(el);
    setColor(el.getStyle(), color);
  }

  public static void setColor(Style st, String color) {
    Assert.notNull(st);
    if (BeeUtils.isEmpty(color)) {
      st.clearColor();
    } else {
      st.setColor(color);
    }
  }

  public static void setColor(UIObject obj, String color) {
    Assert.notNull(obj);
    setColor(obj.getElement(), color);
  }

  public static void setCssText(Element el, String text) {
    Assert.notNull(el);
    setCssText(el.getStyle(), text);
  }

  public static void setCssText(Style st, String text) {
    Assert.notNull(st);
    Assert.notNull(text);
    st.setProperty(PROPERTY_CSS_TEXT, text);
  }

  public static void setCssText(UIObject obj, String text) {
    Assert.notNull(obj);
    setCssText(obj.getElement(), text);
  }

  public static void setDisplay(Element el, Display value) {
    Assert.notNull(el);
    setProperty(el.getStyle(), CssProperties.DISPLAY, value);
  }

  public static void setDisplay(UIObject obj, Display value) {
    Assert.notNull(obj);
    setDisplay(obj.getElement(), value);
  }

  public static void setEmptiness(Element el, boolean empty) {
    Assert.notNull(el);
    if (empty) {
      el.addClassName(NAME_EMPTY);
    } else {
      el.removeClassName(NAME_EMPTY);
    }
  }

  public static void setEmptiness(UIObject obj, boolean empty) {
    Assert.notNull(obj);
    setEmptiness(obj.getElement(), empty);
  }

  public static void setFontFamily(Element el, String family) {
    Assert.notNull(el);
    setFontFamily(el.getStyle(), family);
  }

  public static void setFontFamily(Style st, String family) {
    Assert.notNull(st);
    Assert.notEmpty(family);
    st.setProperty(STYLE_FONT_FAMILY, family);
  }

  public static void setFontFamily(UIObject obj, String family) {
    Assert.notNull(obj);
    setFontFamily(obj.getElement(), family);
  }

  public static void setFontSize(Element el, double size, CssUnit unit) {
    Assert.notNull(el);
    setFontSize(el.getStyle(), size, unit);
  }

  public static void setFontSize(Element el, FontSize size) {
    Assert.notNull(el);
    setFontSize(el.getStyle(), size);
  }

  public static void setFontSize(Element el, int size) {
    Assert.notNull(el);
    setFontSize(el.getStyle(), size);
  }

  public static void setFontSize(Style st, double size, CssUnit unit) {
    Assert.notNull(st);
    Assert.isPositive(size);
    setProperty(st, STYLE_FONT_SIZE, size, normalizeUnit(unit));
  }

  public static void setFontSize(Style st, FontSize size) {
    Assert.notNull(size);
    setFontSize(st, size.getCssName());
  }

  public static void setFontSize(Style st, int size) {
    Assert.notNull(st);
    Assert.nonNegative(size);
    setFontSize(st, size, DEFAULT_UNIT);
  }

  public static void setFontSize(UIObject obj, double size, CssUnit unit) {
    Assert.notNull(obj);
    setFontSize(obj.getElement(), size, unit);
  }

  public static void setFontSize(UIObject obj, FontSize size) {
    Assert.notNull(obj);
    setFontSize(obj.getElement(), size);
  }

  public static void setFontSize(UIObject obj, int size) {
    Assert.notNull(obj);
    setFontSize(obj.getElement(), size);
  }

  public static void setFontVariant(Element el, FontVariant variant) {
    Assert.notNull(el);
    setFontVariant(el.getStyle(), variant);
  }

  public static void setFontVariant(Style st, FontVariant variant) {
    Assert.notNull(st);
    Assert.notNull(variant);
    st.setProperty(STYLE_FONT_VARIANT, variant.getCssName());
  }

  public static void setFontVariant(UIObject obj, FontVariant variant) {
    Assert.notNull(obj);
    setFontVariant(obj.getElement(), variant);
  }

  public static void setHeight(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setHeight(el.getStyle(), value, unit);
  }

  public static void setHeight(Element el, int px) {
    Assert.notNull(el);
    setHeight(el.getStyle(), px);
  }

  public static void setHeight(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_HEIGHT, value, normalizeUnit(unit));
  }

  public static void setHeight(Style st, int px) {
    setHeight(st, px, DEFAULT_UNIT);
  }

  public static void setHeight(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setHeight(obj.getElement(), value, unit);
  }

  public static void setHeight(UIObject obj, int px) {
    Assert.notNull(obj);
    setHeight(obj.getElement(), px);
  }

  public static void setHorizontalPadding(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setHorizontalPadding(el.getStyle(), value, unit);
  }

  public static void setHorizontalPadding(Element el, int px) {
    Assert.notNull(el);
    setHorizontalPadding(el.getStyle(), px);
  }

  public static void setHorizontalPadding(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_PADDING_LEFT, value, normalizeUnit(unit));
    setProperty(st, STYLE_PADDING_RIGHT, value, normalizeUnit(unit));
  }

  public static void setHorizontalPadding(Style st, int px) {
    setHorizontalPadding(st, px, DEFAULT_UNIT);
  }

  public static void setHorizontalPadding(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setHorizontalPadding(obj.getElement(), value, unit);
  }

  public static void setHorizontalPadding(UIObject obj, int px) {
    Assert.notNull(obj);
    setHorizontalPadding(obj.getElement(), px);
  }

  public static void setLeft(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setLeft(el.getStyle(), value, unit);
  }

  public static void setLeft(Element el, int px) {
    Assert.notNull(el);
    setLeft(el.getStyle(), px);
  }

  public static void setLeft(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_LEFT, value, normalizeUnit(unit));
  }

  public static void setLeft(Style st, int px) {
    setLeft(st, px, DEFAULT_UNIT);
  }

  public static void setLeft(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setLeft(obj.getElement(), value, unit);
  }

  public static void setLeft(UIObject obj, int px) {
    Assert.notNull(obj);
    setLeft(obj.getElement(), px);
  }

  public static void setLetterSpacing(Element el, String value) {
    Assert.notNull(el);
    setLetterSpacing(el.getStyle(), value);
  }

  public static void setLetterSpacing(Style st, String value) {
    Assert.notNull(st);
    Assert.notEmpty(value);
    st.setProperty(STYLE_LETTER_SPACING, value);
  }

  public static void setLetterSpacing(UIObject obj, String value) {
    Assert.notNull(obj);
    setLetterSpacing(obj.getElement(), value);
  }

  public static void setLineHeight(Element el, String value) {
    Assert.notNull(el);
    setLineHeight(el.getStyle(), value);
  }

  public static void setLineHeight(Style st, String value) {
    Assert.notNull(st);
    Assert.notEmpty(value);
    st.setProperty(STYLE_LINE_HEIGHT, value);
  }

  public static void setLineHeight(UIObject obj, String value) {
    Assert.notNull(obj);
    setLineHeight(obj.getElement(), value);
  }

  public static void setLineHeight(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setLineHeight(el.getStyle(), value, unit);
  }

  public static void setLineHeight(Element el, int px) {
    Assert.notNull(el);
    setLineHeight(el.getStyle(), px);
  }

  public static void setLineHeight(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_LINE_HEIGHT, value, unit);
  }

  public static void setLineHeight(Style st, int px) {
    setLineHeight(st, px, DEFAULT_UNIT);
  }

  public static void setLineHeight(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setLineHeight(obj.getElement(), value, unit);
  }

  public static void setLineHeight(UIObject obj, int px) {
    Assert.notNull(obj);
    setLineHeight(obj.getElement(), px);
  }

  public static void setMaxHeight(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setMaxHeight(el.getStyle(), value, unit);
  }

  public static void setMaxHeight(Element el, int px) {
    Assert.notNull(el);
    setMaxHeight(el.getStyle(), px);
  }

  public static void setMaxHeight(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    Assert.isPositive(value);
    setProperty(st, STYLE_MAX_HEIGHT, value, normalizeUnit(unit));
  }

  public static void setMaxHeight(Style st, int px) {
    Assert.notNull(st);
    Assert.isPositive(px);
    st.setPropertyPx(STYLE_MAX_HEIGHT, px);
  }

  public static void setMaxHeight(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setMaxHeight(obj.getElement(), value, unit);
  }

  public static void setMaxHeight(UIObject obj, int px) {
    Assert.notNull(obj);
    setMaxHeight(obj.getElement(), px);
  }

  public static void setMaxWidth(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setMaxWidth(el.getStyle(), value, unit);
  }

  public static void setMaxWidth(Element el, int px) {
    Assert.notNull(el);
    setMaxWidth(el.getStyle(), px);
  }

  public static void setMaxWidth(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    Assert.isPositive(value);
    setProperty(st, STYLE_MAX_WIDTH, value, normalizeUnit(unit));
  }

  public static void setMaxWidth(Style st, int px) {
    Assert.notNull(st);
    Assert.isPositive(px);
    st.setPropertyPx(STYLE_MAX_WIDTH, px);
  }

  public static void setMaxWidth(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setMaxWidth(obj.getElement(), value, unit);
  }

  public static void setMaxWidth(UIObject obj, int px) {
    Assert.notNull(obj);
    setMaxWidth(obj.getElement(), px);
  }

  public static void setMinHeight(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setMinHeight(el.getStyle(), value, unit);
  }

  public static void setMinHeight(Element el, int px) {
    Assert.notNull(el);
    setMinHeight(el.getStyle(), px);
  }

  public static void setMinHeight(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    Assert.isPositive(value);
    setProperty(st, STYLE_MIN_HEIGHT, value, normalizeUnit(unit));
  }

  public static void setMinHeight(Style st, int px) {
    Assert.notNull(st);
    Assert.isPositive(px);
    st.setPropertyPx(STYLE_MIN_HEIGHT, px);
  }

  public static void setMinHeight(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setMinHeight(obj.getElement(), value, unit);
  }

  public static void setMinHeight(UIObject obj, int px) {
    Assert.notNull(obj);
    setMinHeight(obj.getElement(), px);
  }

  public static void setMinWidth(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setMinWidth(el.getStyle(), value, unit);
  }

  public static void setMinWidth(Element el, int px) {
    Assert.notNull(el);
    setMinWidth(el.getStyle(), px);
  }

  public static void setMinWidth(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    Assert.isPositive(value);
    setProperty(st, STYLE_MIN_WIDTH, value, normalizeUnit(unit));
  }

  public static void setMinWidth(Style st, int px) {
    Assert.notNull(st);
    Assert.isPositive(px);
    st.setPropertyPx(STYLE_MIN_WIDTH, px);
  }

  public static void setMinWidth(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setMinWidth(obj.getElement(), value, unit);
  }

  public static void setMinWidth(UIObject obj, int px) {
    Assert.notNull(obj);
    setMinWidth(obj.getElement(), px);
  }

  public static void setOpacity(Element el, double value) {
    Assert.notNull(el);
    el.getStyle().setOpacity(value);
  }

  public static void setOpacity(UIObject obj, double value) {
    Assert.notNull(obj);
    setOpacity(obj.getElement(), value);
  }

  public static void setOutlineStyle(Element el, BorderStyle value) {
    Assert.notNull(el);
    setOutlineStyle(el.getStyle(), value);
  }

  public static void setOutlineStyle(Style st, BorderStyle value) {
    Assert.notNull(st);
    Assert.notNull(value);
    st.setProperty(STYLE_OUTLINE_STYLE, value.getCssName());
  }

  public static void setOutlineStyle(UIObject obj, BorderStyle value) {
    Assert.notNull(obj);
    setOutlineStyle(obj.getElement(), value);
  }

  public static void setOverflow(Element el, ScrollBars scroll, Overflow value) {
    Assert.notNull(el);
    setOverflow(el.getStyle(), scroll, value);
  }

  public static void setOverflow(Style st, ScrollBars scroll, Overflow value) {
    Assert.notNull(st);
    Assert.notNull(scroll);
    Assert.notNull(value);

    switch (scroll) {
      case BOTH:
        setProperty(st, STYLE_OVERFLOW, value);
        break;
      case HORIZONTAL:
        setProperty(st, STYLE_OVERFLOW_X, value);
        break;
      case VERTICAL:
        setProperty(st, STYLE_OVERFLOW_Y, value);
        break;
      case NONE:
        clearStyleProperty(st, STYLE_OVERFLOW);
        clearStyleProperty(st, STYLE_OVERFLOW_X);
        clearStyleProperty(st, STYLE_OVERFLOW_Y);
        break;
    }
  }

  public static void setOverflow(UIObject obj, ScrollBars scroll, Overflow value) {
    Assert.notNull(obj);
    setOverflow(obj.getElement(), scroll, value);
  }

  public static void setProperty(Element el, String name, HasCssName value) {
    Assert.notNull(el);
    setProperty(el.getStyle(), name, value);
  }

  public static void setProperty(Style st, String name, double value, CssUnit unit) {
    String v = (unit == null) ? BeeUtils.toString(value) : (value + unit.getCaption());
    st.setProperty(checkPropertyName(name), v);
  }

  public static void setProperty(Style st, String name, HasCssName value) {
    if (value == null) {
      st.clearProperty(checkPropertyName(name));
    } else {
      st.setProperty(checkPropertyName(name), value.getCssName());
    }
  }

  public static void setProperty(UIObject obj, String name, HasCssName value) {
    Assert.notNull(obj);
    setProperty(obj.getElement(), name, value);
  }

  public static void setRectangle(Element el, int left, int top, int width, int height) {
    Assert.notNull(el);
    setRectangle(el.getStyle(), left, top, width, height);
  }

  public static void setRectangle(Style st, int left, int top, int width, int height) {
    Assert.notNull(st);

    setLeft(st, left, DEFAULT_UNIT);
    setTop(st, top, DEFAULT_UNIT);
    setWidth(st, width, DEFAULT_UNIT);
    setHeight(st, height, DEFAULT_UNIT);
  }

  public static void setRectangle(UIObject obj, int left, int top, int width, int height) {
    Assert.notNull(obj);
    setRectangle(obj.getElement(), left, top, width, height);
  }

  public static void setRight(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setRight(el.getStyle(), value, unit);
  }

  public static void setRight(Element el, int px) {
    Assert.notNull(el);
    setRight(el.getStyle(), px);
  }

  public static void setRight(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_RIGHT, value, normalizeUnit(unit));
  }

  public static void setRight(Style st, int px) {
    setRight(st, px, DEFAULT_UNIT);
  }

  public static void setRight(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setRight(obj.getElement(), value, unit);
  }

  public static void setRight(UIObject obj, int px) {
    Assert.notNull(obj);
    setRight(obj.getElement(), px);
  }

  public static void setSize(Element el, int width, int height) {
    Assert.notNull(el);
    setSize(el.getStyle(), width, height);
  }

  public static void setSize(Element el, Size size) {
    Assert.notNull(el);
    setSize(el.getStyle(), size);
  }

  public static void setSize(Style st, int width, int height) {
    Assert.notNull(st);
    setWidth(st, width, DEFAULT_UNIT);
    setHeight(st, height, DEFAULT_UNIT);
  }

  public static void setSize(Style st, Size size) {
    Assert.notNull(st);
    Assert.notNull(size);

    setWidth(st, size.getWidth(), DEFAULT_UNIT);
    setHeight(st, size.getHeight(), DEFAULT_UNIT);
  }

  public static void setSize(UIObject obj, int width, int height) {
    Assert.notNull(obj);
    setSize(obj.getElement(), width, height);
  }

  public static void setSize(UIObject obj, Size size) {
    Assert.notNull(obj);
    setSize(obj.getElement(), size);
  }

  public static void setStyleDependentName(Element el, String style, boolean add) {
    Assert.notNull(el);
    Assert.notEmpty(style);

    String primary = getStylePrimaryName(el);
    Assert.notEmpty(primary, "element has no primary style");

    setStyleName(el, primary + BeeConst.CHAR_MINUS + style.trim(), add);
  }

  public static void setStyleName(Element el, String st, boolean add) {
    Assert.notNull(el);
    Assert.notEmpty(st);

    if (add) {
      el.addClassName(st.trim());
    } else {
      el.removeClassName(st.trim());
    }
  }

  public static int setStyleProperty(NodeList<Element> nodes, String name,
      double value, CssUnit unit) {
    Assert.notNull(nodes);
    Assert.notEmpty(name);

    return setStyleProperty(nodes, name, value + normalizeUnit(unit).getCaption());
  }

  public static int setStyleProperty(NodeList<Element> nodes, String name, String value) {
    Assert.notNull(nodes);
    Assert.notEmpty(name);

    int cnt = nodes.getLength();
    for (int i = 0; i < cnt; i++) {
      nodes.getItem(i).getStyle().setProperty(name, value);
    }
    return cnt;
  }

  public static int setStylePropertyPx(NodeList<Element> nodes, String name, int value) {
    Assert.notNull(nodes);
    Assert.notEmpty(name);

    return setStyleProperty(nodes, name, value, CssUnit.PX);
  }

  public static void setTextAlign(Element el, TextAlign align) {
    Assert.notNull(el);
    setTextAlign(el.getStyle(), align);
  }

  public static void setTextAlign(Style st, TextAlign align) {
    Assert.notNull(st);
    Assert.notNull(align);
    st.setProperty(STYLE_TEXT_ALIGN, align.getCssName());
  }

  public static void setTop(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setTop(el.getStyle(), value, unit);
  }

  public static void setTop(Element el, int px) {
    Assert.notNull(el);
    setTop(el.getStyle(), px);
  }

  public static void setTop(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_TOP, value, normalizeUnit(unit));
  }

  public static void setTop(Style st, int px) {
    setTop(st, px, DEFAULT_UNIT);
  }

  public static void setTop(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setTop(obj.getElement(), value, unit);
  }

  public static void setTop(UIObject obj, int px) {
    Assert.notNull(obj);
    setTop(obj.getElement(), px);
  }

  public static void setTransformRotate(Element el, Axis axis, double value, CssAngle angle) {
    Assert.notNull(el);
    setTransformRotate(el.getStyle(), axis, value, angle);
  }

  public static void setTransformRotate(Element el, double value, CssAngle angle) {
    Assert.notNull(el);
    setTransformRotate(el.getStyle(), value, angle);
  }

  public static void setTransformRotate(Style st, Axis axis, double value, CssAngle angle) {
    Assert.notNull(st);
    Assert.notNull(axis);
    st.setProperty(getStyleTransformPropertyName(st), axis.rotate(value, angle));
  }

  public static void setTransformRotate(Style st, double value, CssAngle angle) {
    Assert.notNull(st);
    st.setProperty(getStyleTransformPropertyName(st),
        TRANSFORM_ROTATE + BeeUtils.parenthesize(CssAngle.format(value, angle)));
  }

  public static void setTransformRotate(UIObject obj, Axis axis, double value, CssAngle angle) {
    Assert.notNull(obj);
    setTransformRotate(obj.getElement(), axis, value, angle);
  }

  public static void setTransformRotate(UIObject obj, double value, CssAngle angle) {
    Assert.notNull(obj);
    setTransformRotate(obj.getElement(), value, angle);
  }

  public static void setTransformScale(Element el, Axis axis, double value) {
    Assert.notNull(el);
    setTransformScale(el.getStyle(), axis, value);
  }

  public static void setTransformScale(Element el, double x, double y) {
    Assert.notNull(el);
    setTransformScale(el.getStyle(), x, y);
  }

  public static void setTransformScale(Style st, Axis axis, double value) {
    Assert.notNull(st);
    Assert.notNull(axis);
    st.setProperty(getStyleTransformPropertyName(st), axis.scale(value));
  }

  public static void setTransformScale(Style st, double x, double y) {
    Assert.notNull(st);
    st.setProperty(getStyleTransformPropertyName(st),
        TRANSFORM_SCALE + BeeUtils.parenthesize(x + BeeConst.STRING_COMMA + y));
  }

  public static void setTransformScale(UIObject obj, Axis axis, double value) {
    Assert.notNull(obj);
    setTransformScale(obj.getElement(), axis, value);
  }

  public static void setTransformScale(UIObject obj, double x, double y) {
    Assert.notNull(obj);
    setTransformScale(obj.getElement(), x, y);
  }

  public static void setTransformSkew(Element el, Axis axis, int value, CssAngle angle) {
    Assert.notNull(el);
    setTransformSkew(el.getStyle(), axis, value, angle);
  }

  public static void setTransformSkew(Style st, Axis axis, int value, CssAngle angle) {
    Assert.notNull(st);
    Assert.notNull(axis);
    st.setProperty(getStyleTransformPropertyName(st), axis.skew(value, angle));
  }

  public static void setTransformSkew(UIObject obj, Axis axis, int value, CssAngle angle) {
    Assert.notNull(obj);
    setTransformSkew(obj.getElement(), axis, value, angle);
  }

  public static void setTransformTranslate(Element el, Axis axis, double value, CssUnit unit) {
    Assert.notNull(el);
    setTransformTranslate(el.getStyle(), axis, value, unit);
  }

  public static void setTransformTranslate(Element el, double x, CssUnit xu, double y, CssUnit yu) {
    Assert.notNull(el);
    setTransformTranslate(el.getStyle(), x, xu, y, yu);
  }

  public static void setTransformTranslate(Style st, Axis axis, double value, CssUnit unit) {
    Assert.notNull(st);
    st.setProperty(getStyleTransformPropertyName(st), axis.translate(value, unit));
  }

  public static void setTransformTranslate(Style st, double x, CssUnit xu, double y, CssUnit yu) {
    Assert.notNull(st);
    st.setProperty(getStyleTransformPropertyName(st), TRANSFORM_TRANSLATE
        + BeeUtils.parenthesize(toCssLength(x, xu) + BeeConst.STRING_COMMA + toCssLength(y, yu)));
  }

  public static void setTransformTranslate(UIObject obj, Axis axis, double value, CssUnit unit) {
    Assert.notNull(obj);
    setTransformTranslate(obj.getElement(), axis, value, unit);
  }

  public static void setTransformTranslate(UIObject obj, double x, CssUnit xu,
      double y, CssUnit yu) {
    Assert.notNull(obj);
    setTransformTranslate(obj.getElement(), x, xu, y, yu);
  }

  public static void setVerticalAlign(Element el, VerticalAlign align) {
    Assert.notNull(el);
    setVerticalAlign(el.getStyle(), align);
  }

  public static void setVerticalAlign(Style st, VerticalAlign align) {
    Assert.notNull(st);
    Assert.notNull(align);
    st.setProperty(STYLE_VERTICAL_ALIGN, align.getCssName());
  }

  public static void setVisible(Element el, boolean visible) {
    Assert.notNull(el);
    if (UIObject.isVisible(el) != visible) {
      if (visible) {
        el.getStyle().clearDisplay();
      } else {
        setProperty(el.getStyle(), CssProperties.DISPLAY, Display.NONE);
      }
    }
  }

  public static void setVisible(UIObject obj, boolean visible) {
    Assert.notNull(obj);
    setVisible(obj.getElement(), visible);
  }

  public static void setWhiteSpace(Element el, WhiteSpace value) {
    Assert.notNull(el);
    setWhiteSpace(el.getStyle(), value);
  }

  public static void setWhiteSpace(Style st, WhiteSpace value) {
    Assert.notNull(st);
    Assert.notNull(value);
    st.setProperty(STYLE_WHITE_SPACE, value.getCssName());
  }

  public static void setWhiteSpace(UIObject obj, WhiteSpace value) {
    Assert.notNull(obj);
    setWhiteSpace(obj.getElement(), value);
  }

  public static void setWidth(Element el, double value, CssUnit unit) {
    Assert.notNull(el);
    setWidth(el.getStyle(), value, unit);
  }

  public static void setWidth(Element el, int px) {
    Assert.notNull(el);
    setWidth(el.getStyle(), px);
  }

  public static void setWidth(Style st, double value, CssUnit unit) {
    Assert.notNull(st);
    setProperty(st, STYLE_WIDTH, value, normalizeUnit(unit));
  }

  public static void setWidth(Style st, int px) {
    setWidth(st, px, DEFAULT_UNIT);
  }

  public static void setWidth(UIObject obj, double value, CssUnit unit) {
    Assert.notNull(obj);
    setWidth(obj.getElement(), value, unit);
  }

  public static void setWidth(UIObject obj, int px) {
    Assert.notNull(obj);
    setWidth(obj.getElement(), px);
  }

  public static void setWordWrap(Element el, boolean wrap) {
    Assert.notNull(el);
    setWordWrap(el.getStyle(), wrap);
  }

  public static void setWordWrap(Style st, boolean wrap) {
    setWhiteSpace(st, wrap ? WhiteSpace.NORMAL : WhiteSpace.NOWRAP);
  }

  public static void setWordWrap(UIObject obj, boolean wrap) {
    Assert.notNull(obj);
    setWordWrap(obj.getElement(), wrap);
  }

  public static void setZIndex(Element el, int value) {
    Assert.notNull(el);
    setZIndex(el.getStyle(), value);
  }

  public static void setZIndex(Style st, int value) {
    Assert.notNull(st);
    st.setZIndex(value);
  }

  public static void setZIndex(UIObject obj, int value) {
    Assert.notNull(obj);
    setZIndex(obj.getElement(), value);
  }

  public static List<String> splitClasses(String classes) {
    List<String> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(classes)) {
      for (String name : CLASS_NAME_SPLITTER.split(classes)) {
        if (!result.contains(name)) {
          result.add(name);
        }
      }
    }

    return result;
  }

  public static String toCssLength(double value, CssUnit unit) {
    return BeeUtils.toString(value, 5) + normalizeUnit(unit).getCaption();
  }

  public static SafeStyles toSafeStyles(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else if (s.trim().endsWith(DEFINITION_SEPARATOR)) {
      return SafeStylesUtils.fromTrustedString(s.trim());
    } else {
      return SafeStylesUtils.fromTrustedString(s.trim() + DEFINITION_SEPARATOR);
    }
  }

  public static void unhideDisplay(Element el) {
    Assert.notNull(el);
    if (BeeUtils.same(el.getStyle().getDisplay(), Display.NONE.getCssName())) {
      el.getStyle().clearDisplay();
    }
  }

  public static void unhideDisplay(String id) {
    unhideDisplay(DomUtils.getElement(id));
  }

  public static void unhideDisplay(UIObject obj) {
    Assert.notNull(obj);
    unhideDisplay(obj.getElement());
  }

  public static void updateAppearance(Element el, String classes, String styles) {
    Assert.notNull(el);

    if (!BeeUtils.isEmpty(classes)) {
      updateClasses(el, classes);
    }
    if (!BeeUtils.isEmpty(styles)) {
      updateStyle(el.getStyle(), styles);
    }
  }

  public static void updateAppearance(UIObject obj, String className, String styles) {
    Assert.notNull(obj);
    updateAppearance(obj.getElement(), className, styles);
  }

  public static void updateClasses(Element el, String classes) {
    Assert.notNull(el);
    Assert.notNull(classes);

    if (BeeUtils.same(classes, String.valueOf(REMOVE_CLASS))) {
      if (!BeeUtils.isEmpty(DomUtils.getClassName(el))) {
        el.setClassName(BeeConst.STRING_EMPTY);
      }
      return;
    }

    if (BeeUtils.isPrefixOrSuffix(classes, REPLACE_CLASS)) {
      el.setClassName(BeeUtils.removePrefixAndSuffix(classes, REPLACE_CLASS));
      return;
    }

    for (String name : CLASS_NAME_SPLITTER.split(classes.trim())) {
      if (BeeUtils.isPrefixOrSuffix(name, ADD_CLASS)) {
        String z = BeeUtils.removePrefixAndSuffix(name, ADD_CLASS);
        if (!BeeUtils.isEmpty(z)) {
          el.addClassName(z);
        }

      } else if (BeeUtils.isPrefixOrSuffix(name, REMOVE_CLASS)) {
        String z = BeeUtils.removePrefixAndSuffix(name, REMOVE_CLASS);
        if (!BeeUtils.isEmpty(z)) {
          el.removeClassName(z);
        }

      } else if (BeeUtils.isPrefixOrSuffix(name, REPLACE_CLASS)) {
        el.setClassName(BeeUtils.removePrefixAndSuffix(name, REPLACE_CLASS));

      } else {
        el.addClassName(name);
      }
    }
  }

  public static void updateClasses(UIObject obj, String classes) {
    Assert.notNull(obj);
    updateClasses(obj.getElement(), classes);
  }

  public static void updateStyle(Element el, List<Property> properties) {
    Assert.notNull(el);
    updateStyle(el.getStyle(), properties);
  }

  public static void updateStyle(Element el, String styles) {
    Assert.notNull(el);
    updateStyle(el.getStyle(), styles);
  }

  public static void updateStyle(Style st, List<Property> properties) {
    Assert.notNull(st);
    Assert.notNull(properties);

    for (Property property : properties) {
      st.setProperty(checkPropertyName(property.getName()), property.getValue());
    }
  }

  public static void updateStyle(Style st, String styles) {
    List<Property> properties = parseStyles(styles);
    if (properties != null) {
      updateStyle(st, properties);
    }
  }

  public static void updateStyle(UIObject obj, List<Property> properties) {
    Assert.notNull(obj);
    updateStyle(obj.getElement(), properties);
  }

  public static void updateStyle(UIObject obj, String styles) {
    Assert.notNull(obj);
    updateStyle(obj.getElement(), styles);
  }

  public static void zeroLeft(Element el) {
    Assert.notNull(el);
    zeroLeft(el.getStyle());
  }

  public static void zeroLeft(Style st) {
    Assert.notNull(st);
    st.setPropertyPx(STYLE_LEFT, 0);
  }

  public static void zeroLeft(UIObject obj) {
    Assert.notNull(obj);
    zeroLeft(obj.getElement());
  }

  public static void zeroTop(Element el) {
    Assert.notNull(el);
    zeroTop(el.getStyle());
  }

  public static void zeroTop(Style st) {
    Assert.notNull(st);
    st.setPropertyPx(STYLE_TOP, 0);
  }

  public static void zeroTop(UIObject obj) {
    Assert.notNull(obj);
    zeroTop(obj.getElement());
  }

  public static void zeroWidth(Element el) {
    Assert.notNull(el);
    zeroWidth(el.getStyle());
  }

  public static void zeroWidth(Style st) {
    Assert.notNull(st);
    st.setPropertyPx(STYLE_WIDTH, 0);
  }

  public static void zeroWidth(UIObject obj) {
    Assert.notNull(obj);
    zeroWidth(obj.getElement());
  }

  private static String camelize(String name) {
    return NameUtils.camelize(name, NAME_DELIMITER);
  }

  private static String checkPropertyName(String name) {
    if ("float".equals(name)) {
      return "cssFloat";
    } else {
      return camelize(name);
    }
  }

  private static void clearStyleProperty(Style style, String name) {
    if (!BeeUtils.isEmpty(style.getProperty(name))) {
      style.clearProperty(name);
    }
  }

  private static void copyStyleProperty(Style src, Style dst, String name) {
    String value = src.getProperty(name);
    if (!BeeUtils.equalsTrim(value, dst.getProperty(name))) {
      dst.setProperty(name, value);
    }
  }

  private static String getParentProperty(Element el, String name, boolean computed,
      Collection<String> ignore, boolean incl) {
    Assert.notNull(el);
    Assert.notEmpty(name);

    if (incl) {
      String value = el.getStyle().getProperty(name);
      boolean ok = !BeeUtils.isEmpty(value);

      if (!ok && computed) {
        value = ComputedStyles.get(el, name);
        ok = !BeeUtils.isEmpty(value);
      }

      if (ok) {
        if (BeeUtils.isEmpty(ignore)) {
          ok = !BeeUtils.inListSame(value, VALUE_AUTO, VALUE_INHERIT);
        } else {
          ok = !ignore.contains(value);
        }
      }

      if (ok) {
        return value;
      }
    }

    Element parent = el.getParentElement();
    if (parent == null) {
      return null;
    }
    return getParentProperty(parent, name, computed, ignore, true);
  }

  private static String getStyleTransformPropertyName(Style st) {
    if (styleTransform == null) {
      String property = "webkitTransform";
      if (JsUtils.hasProperty(st, property)) {
        styleTransform = property;
      } else {
        styleTransform = "transform";
      }
    }
    return styleTransform;
  }

  private static boolean hasProperty(Style st, String name) {
    if (st == null || BeeUtils.isEmpty(name)) {
      return false;
    } else {
      return !BeeUtils.isEmpty(st.getProperty(name));
    }
  }

  private static int indexOfClassName(String className, String classes) {
    if (className == null || !BeeUtils.containsSame(classes, className)) {
      return BeeConst.UNDEF;
    }

    int idx = 0;
    for (String name : CLASS_NAME_SPLITTER.split(classes.trim())) {
      if (BeeUtils.same(name, className)) {
        return idx;
      }
      idx++;
    }
    return BeeConst.UNDEF;
  }

  private static boolean isZero(String s) {
    if (BeeUtils.isEmpty(s)) {
      return false;
    }
    if (s.trim().charAt(0) != BeeConst.CHAR_ZERO) {
      return false;
    }
    boolean ok = true;

    for (int i = 1; i < s.trim().length(); i++) {
      char z = s.trim().charAt(i);

      if (z == BeeConst.CHAR_ZERO || z == BeeConst.CHAR_SPACE || z == BeeConst.CHAR_POINT) {
        continue;
      } else {
        if (Character.isDigit(z)) {
          ok = false;
        }
        break;
      }
    }
    return ok;
  }

  private static CssUnit normalizeUnit(CssUnit unit) {
    return (unit == null) ? DEFAULT_UNIT : unit;
  }

  private static void setFontSize(Style st, String value) {
    Assert.notNull(st);
    Assert.notEmpty(value);
    st.setProperty(STYLE_FONT_SIZE, value);
  }

  private StyleUtils() {
  }
}
