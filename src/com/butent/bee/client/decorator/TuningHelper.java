package com.butent.bee.client.decorator;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.ComputedStyles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class TuningHelper {

  private static final BeeLogger logger = LogUtils.getLogger(TuningHelper.class);

  private static final String CLASS_NAME = "className";
  private static final String STYLE = "style";

  private static final String VALUE = "value";

  public static List<Element> getActors(Element root, String role) {
    return getActors(root, role, null, null);
  }

  public static List<Element> getActors(Element root, String role, Element excl, Element cut) {
    return DomUtils.getElementsByAttributeValue(root, DomUtils.ATTRIBUTE_ROLE, role, excl, cut);
  }

  public static void updateActor(JavaScriptObject obj, String role, String name, String value) {
    Element root = null;
    if (Element.is(obj)) {
      root = Element.as(obj);
    } else if (Node.is(obj)) {
      root = Node.as(obj).getParentElement();
    }

    if (root == null) {
      logger.warning("updateActor: not an element", obj);
      return;
    }
    if (BeeUtils.isEmpty(role)) {
      logger.severe("updateActor: role not specified");
      return;
    }
    if (BeeUtils.isEmpty(name)) {
      logger.severe("updateActor: property not specified");
      return;
    }

    if (BeeUtils.same(role, DecoratorConstants.ROLE_ROOT)) {
      updateElement(root, name, value);
      return;
    }

    List<Element> actors = getActors(root, role, null, null);
    if (actors.isEmpty()) {
      if (Global.isDebug()) {
        logger.warning("updateActor:", root.getId(), role, name, value,
            "no actors found");
      }
      return;
    }

    for (Element actor : actors) {
      updateElement(actor, name, value);
    }
  }

  public static void updateRoleClasses(Element root, String classes) {
    Assert.notNull(root);
    if (BeeUtils.isEmpty(classes)) {
      return;
    }

    for (String roleDefinition : DecoratorConstants.ROLE_DEFINITION_SPLITTER.split(classes)) {
      String role = BeeUtils.getPrefix(roleDefinition, DecoratorConstants.ROLE_VALUE_SEPARATOR);
      String value = BeeUtils.getSuffix(roleDefinition, DecoratorConstants.ROLE_VALUE_SEPARATOR);

      if (BeeUtils.allNotEmpty(role, value)) {
        List<Element> actors = getActors(root, role);
        if (actors.isEmpty()) {
          logger.warning("classes", classes, "role", role, "no actors found");
          continue;
        }
        for (Element actor : actors) {
          StyleUtils.updateClasses(actor, value);
        }
      }
    }
  }

  public static void updateRoleStyles(Element root, String styles) {
    Assert.notNull(root);
    if (BeeUtils.isEmpty(styles)) {
      return;
    }

    for (String roleDefinition : DecoratorConstants.ROLE_DEFINITION_SPLITTER.split(styles)) {
      String role = BeeUtils.getPrefix(roleDefinition, DecoratorConstants.ROLE_VALUE_SEPARATOR);
      String value = BeeUtils.getSuffix(roleDefinition, DecoratorConstants.ROLE_VALUE_SEPARATOR);

      if (BeeUtils.allNotEmpty(role, value)) {
        List<Element> actors = getActors(root, role);
        if (actors.isEmpty()) {
          logger.warning("styles", styles, "role", role, "no actors found");
          continue;
        }
        for (Element actor : actors) {
          StyleUtils.updateStyle(actor.getStyle(), value);
        }
      }
    }
  }

  private static void incrementFontSize(Element element, int increment) {
    String oldSize = element.getStyle().getFontSize();
    if (BeeUtils.isEmpty(oldSize)) {
      oldSize = ComputedStyles.get(element, StyleUtils.STYLE_FONT_SIZE);
    }

    Double value = null;
    CssUnit unit = null;
    if (!BeeUtils.isEmpty(oldSize)) {
      Pair<Double, CssUnit> cssLength = StyleUtils.parseCssLength(oldSize);
      value = cssLength.getA();
      unit = cssLength.getB();
    }

    if (!BeeUtils.isPositive(value)) {
      value = 13.0;
    }
    if (unit == null) {
      unit = CssUnit.PX;
    }

    value = Math.max(value + increment, 1.0);
    StyleUtils.setFontSize(element, value, unit);
  }

  private static void updateElement(Element element, String name, String value) {
    if (BeeUtils.same(name, CLASS_NAME)) {
      if (!BeeUtils.isEmpty(value)) {
        StyleUtils.updateClasses(element, value);
      }
      return;
    }

    if (BeeUtils.same(name, STYLE)) {
      if (!BeeUtils.isEmpty(value)) {
        StyleUtils.updateStyle(element.getStyle(), value);
      }
      return;
    }

    if (BeeUtils.same(name, VALUE)) {
      element.setPropertyString(VALUE, BeeUtils.trim(value));
      return;
    }

    if (BeeUtils.same(name, StyleUtils.STYLE_FONT_SIZE)) {
      if (BeeUtils.same(value, BeeConst.STRING_PLUS)) {
        incrementFontSize(element, 1);
      } else if (BeeUtils.same(value, BeeConst.STRING_MINUS)) {
        incrementFontSize(element, -1);
      } else if (!BeeUtils.isEmpty(value)) {
        element.getStyle().setProperty(StyleUtils.STYLE_FONT_SIZE, value);
      }
      return;
    }

    if (!BeeUtils.isEmpty(value)) {
      element.setPropertyString(BeeUtils.trim(name), BeeUtils.trim(value));
    }
  }

  private TuningHelper() {
  }
}
