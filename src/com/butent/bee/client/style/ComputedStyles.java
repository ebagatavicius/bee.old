package com.butent.bee.client.style;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.HasLength;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;

import java.util.List;
import java.util.Map;

public class ComputedStyles implements HasLength, HasInfo {

  private static final char NAME_SEPARATOR = '-';
  private static final int DEFAULT_PIXEL_VALUE = 0;

  public static String get(Element el, String p) {
    Assert.notNull(el);
    Assert.notEmpty(p);

    return getComputedStyle(el, NameUtils.decamelize(p, NAME_SEPARATOR),
        NameUtils.camelize(p, NAME_SEPARATOR));
  }

  public static String get(UIObject obj, String p) {
    Assert.notNull(obj);
    return get(obj.getElement(), p);
  }

  public static Map<String, String> getNormalized(Element el) {
    Assert.notNull(el);
    Map<String, String> result = Maps.newHashMap();

    ComputedStyles cs = new ComputedStyles(el);
    for (int i = 0; i < cs.getLength(); i++) {
      result.put(normalize(cs.getItem(i)), cs.getValue(i));
    }
    return result;
  }

  public static int getPixels(Element el, String p) {
    String value = get(el, p);

    if (BeeUtils.isEmpty(value)) {
      return DEFAULT_PIXEL_VALUE;

    } else if (BeeUtils.containsSame(value, CssUnit.PX.getCaption())) {
      Pair<Double, CssUnit> cssLength = StyleUtils.parseCssLength(value);
      if (cssLength.getA() != null && CssUnit.PX.equals(cssLength.getB())) {
        return BeeUtils.toInt(cssLength.getA());
      } else {
        return DEFAULT_PIXEL_VALUE;
      }

    } else if (BeeUtils.isInt(value)) {
      return BeeUtils.toInt(value);

    } else if (BeeUtils.isDouble(value)) {
      return BeeUtils.toInt(BeeUtils.toDouble(value));

    } else {
      return DEFAULT_PIXEL_VALUE;
    }
  }

  public static String normalize(String s) {
    if (s == null) {
      return null;
    }
    return BeeUtils.remove(s, NAME_SEPARATOR).trim().toLowerCase();
  }

//@formatter:off
  private static native String getComputedStyle(Element el, String p, String c) /*-{
    if ("getComputedStyle" in $wnd) {
      return $wnd.getComputedStyle(el, null).getPropertyValue(p);
    } else if ("currentStyle" in el) {
      return el.currentStyle[c];
    } else {
      return null;
    }
  }-*/;

  private static native JsArrayString getComputedStyles(Element el) /*-{
    var arr = [];

    if ("getComputedStyle" in $wnd) {
      var cs = $wnd.getComputedStyle(el, null);
      if (cs.length) {
        for (var i = 0; i < cs.length; i++) {
          arr.push(cs.item(i), cs.getPropertyValue(cs.item(i)));
        }
      } else {
        for ( var p in cs) {
          if (cs.hasOwnProperty(p)) {
            arr.push(p, cs[p]);
          }
        }
      }

    } else if ("currentStyle" in el) {
      var cs = el.currentStyle;
      for ( var p in cs) {
        arr.push(p, cs[p]);
      }
    }

    return arr;
  }-*/;
//@formatter:on

  private final JsArrayString styles;

  public ComputedStyles(Element el) {
    Assert.notNull(el);
    styles = getComputedStyles(el);
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();
    for (int i = 0; i < getLength(); i++) {
      info.add(new Property(getItem(i), getValue(i)));
    }
    return info;
  }

  @Override
  public int getLength() {
    return getStyles().length() / 2;
  }

  public String getPropertyValue(String propertyName) {
    String value = null;
    if (BeeUtils.isEmpty(propertyName) || getLength() <= 0) {
      return value;
    }

    String p = normalize(propertyName);
    for (int i = 0; i < getLength(); i++) {
      if (p.equals(normalize(getItem(i)))) {
        value = getValue(i);
        break;
      }
    }
    return value;
  }

  private String getItem(int index) {
    return getStyles().get(index * 2);
  }

  private JsArrayString getStyles() {
    return styles;
  }

  private String getValue(int index) {
    return getStyles().get(index * 2 + 1);
  }
}
