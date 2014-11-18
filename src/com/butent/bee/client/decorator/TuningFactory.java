package com.butent.bee.client.decorator;

import com.google.common.collect.Lists;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TuningFactory {

  private static final BeeLogger logger = LogUtils.getLogger(TuningFactory.class);

  private static final Map<String, Decorator> decorators = new HashMap<>();

  private static boolean enabled = true;

  public static void clear() {
    decorators.clear();
  }

  public static IdentifiableWidget decorate(String decoratorId, Element element,
      IdentifiableWidget widget, WidgetDescription widgetDescription) {

    if (widget == null || BeeUtils.isEmpty(decoratorId) || !isEnabled()) {
      return widget;
    }

    Decorator decorator = decorators.get(normalize(decoratorId));
    if (decorator == null) {
      logger.warning("decorator not found:", decoratorId);
      return widget;
    }
    if (!decorator.isEnabled()) {
      return widget;
    }
    if (decorator.isAbstract()) {
      logger.warning("decorator is abstract:", decoratorId);
      return widget;
    }

    if (!decorator.isInitialized()) {
      if (!initialize(decorator, null)) {
        decorator.setEnabled(false);
        return widget;
      }
    }
    return decorator.decorate(widget, getOptions(element, widgetDescription));
  }

  public static List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> result = new ArrayList<>();
    PropertyUtils.addExtended(result, DecoratorConstants.TAG_DECORATORS,
        BeeUtils.bracket(decorators.size()));

    if (!decorators.isEmpty()) {
      int idx = 0;
      for (Map.Entry<String, Decorator> entry : decorators.entrySet()) {
        PropertyUtils.addExtended(result, DecoratorConstants.TAG_DECORATOR,
            BeeUtils.progress(++idx, decorators.size()), entry.getKey());
        PropertyUtils.appendWithPrefix(result, entry.getKey(), entry.getValue().getExtendedInfo());
      }
    }

    return result;
  }

  public static Map<String, String> getOptions(Element element, WidgetDescription wd) {
    Map<String, String> result = new HashMap<>();

    if (element != null) {
      NamedNodeMap attributes = element.getAttributes();
      int cnt = (attributes == null) ? 0 : attributes.getLength();

      Attr attr;
      for (int i = 0; i < cnt; i++) {
        attr = (Attr) attributes.item(i);
        if (BeeUtils.same(attr.getNamespaceURI(), DecoratorConstants.NAMESPACE)) {
          result.put(XmlUtils.getLocalName(attr), attr.getValue());
        }
      }
    }

    if (wd != null) {
      String caption = wd.getCaption();
      if (!BeeUtils.isEmpty(caption) && !result.containsKey(DecoratorConstants.OPTION_CAPTION)) {
        result.put(DecoratorConstants.OPTION_CAPTION, caption);
      }

      if ((BeeUtils.isTrue(wd.getRequired()) || BeeUtils.isFalse(wd.getNullable()))
          && !result.containsKey(DecoratorConstants.OPTION_VALUE_REQUIRED)) {
        result.put(DecoratorConstants.OPTION_VALUE_REQUIRED, BeeConst.STRING_TRUE);
      }
      if (BeeUtils.isTrue(wd.getHasDefaults())
          && !result.containsKey(DecoratorConstants.OPTION_HAS_DEFAULTS)) {
        result.put(DecoratorConstants.OPTION_HAS_DEFAULTS, BeeConst.STRING_TRUE);
      }
    }

    return result;
  }

  public static void getTools() {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_DECORATORS, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(String.class)) {
          parseDecorators((String) response.getResponse());
        }
      }
    });
  }

  public static boolean isEnabled() {
    return enabled;
  }

  public static void parseDecorators(String xml) {
    Document document = XmlUtils.parse(xml);
    if (document == null) {
      return;
    }

    List<Element> elements = XmlUtils.getChildrenElements(document.getDocumentElement());
    if (elements.isEmpty()) {
      logger.warning("no decorators found");
    }

    clear();

    String tag;
    String id;
    String ext;

    List<Parameter> params = new ArrayList<>();
    Map<String, String> constants = new HashMap<>();
    Map<String, String> css = new HashMap<>();
    Lifecycle lc;
    List<Handler> handlers = new ArrayList<>();

    Template template;

    for (Element element : elements) {
      tag = XmlUtils.getLocalName(element);
      if (!BeeUtils.inListSame(tag, DecoratorConstants.TAG_ABSTRACT,
          DecoratorConstants.TAG_DECORATOR)) {
        logger.warning("unrecognized decorator tag:", tag);
        continue;
      }

      id = element.getAttribute(DecoratorConstants.ATTR_ID);
      if (BeeUtils.isEmpty(id)) {
        logger.warning("decorator id not found");
        continue;
      }

      ext = element.getAttribute(DecoratorConstants.ATTR_EXTENDS);

      params.clear();
      constants.clear();
      css.clear();
      lc = null;
      handlers.clear();
      template = null;

      for (Element child : XmlUtils.getChildrenElements(element)) {
        String childTag = XmlUtils.getLocalName(child);

        if (BeeUtils.inListSame(childTag, DecoratorConstants.TAG_REQUIRED_PARAM,
            DecoratorConstants.TAG_OPTIONAL_PARAM)) {
          params.addAll(Parameter.getParameters(child));

        } else if (BeeUtils.same(childTag, DecoratorConstants.TAG_CONST)) {
          String name = child.getAttribute(UiConstants.ATTR_NAME);
          String value = XmlUtils.getInnerXml(child);
          if (!BeeUtils.isEmpty(name)) {
            constants.put(name.trim(), BeeUtils.trim(value));
          }

        } else if (BeeUtils.same(childTag, DecoratorConstants.TAG_STYLE)) {
          String name = child.getAttribute(DecoratorConstants.ATTR_ID);
          String text = XmlUtils.getText(child);
          if (!BeeUtils.isEmpty(name) && !BeeUtils.isEmpty(text)) {
            css.put(name.trim(), text.trim());
          }

        } else if (BeeUtils.same(childTag, DecoratorConstants.TAG_LIFECYCLE)) {
          lc = Lifecycle.getLifecycle(child);

        } else if (BeeUtils.same(childTag, DecoratorConstants.TAG_HANDLER)) {
          Handler handler = Handler.getHandler(child);
          if (handler != null) {
            handlers.add(handler);
          }

        } else if (BeeUtils.same(childTag, DecoratorConstants.TAG_TEMPLATE)) {
          template = Template.getTemplate(child);
        }
      }

      String key = normalize(id);
      if (decorators.containsKey(key)) {
        logger.warning("duplicate decorator id:", id);
      } else {
        String eventTarget = element.getAttribute(DecoratorConstants.ATTR_EVENT_TARGET);
        String appearanceTarget = element.getAttribute(DecoratorConstants.ATTR_APPEARANCE_TARGET);
        Boolean appearanceDeep =
            XmlUtils.getAttributeBoolean(element, DecoratorConstants.ATTR_APPEARANCE_DEEP);

        decorators.put(key, new Decorator(id, ext,
            BeeUtils.same(tag, DecoratorConstants.TAG_ABSTRACT), params, constants, css, lc,
            handlers, template, eventTarget, appearanceTarget, BeeUtils.unbox(appearanceDeep)));
      }
    }
    logger.info("decorators", decorators.size());
  }

  public static void refresh() {
    getTools();
  }

  public static void setEnabled(boolean enabled) {
    TuningFactory.enabled = enabled;
  }

  private static boolean initialize(Decorator decorator, Collection<String> relatives) {
    String ext = decorator.getParent();
    if (BeeUtils.isEmpty(ext)) {
      decorator.init(null);
      return true;
    }

    String id = decorator.getId();
    if (BeeUtils.same(id, ext) || relatives != null && relatives.contains(normalize(ext))) {
      logger.severe("incest detected:", id, ext, relatives);
      return false;
    }

    Decorator parent = decorators.get(normalize(ext));
    if (parent == null) {
      logger.severe("parent decorator not found id:", id, "extends:", ext);
      return false;
    }

    if (!parent.isInitialized()) {
      List<String> children = Lists.newArrayList(normalize(id));
      if (relatives != null) {
        children.addAll(relatives);
      }
      if (!initialize(parent, children)) {
        return false;
      }
    }

    decorator.init(parent.getFields());
    return true;
  }

  private static String normalize(String key) {
    return BeeUtils.normalize(key);
  }

  private TuningFactory() {
  }
}
