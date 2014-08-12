package com.butent.bee.client.decorator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsFunction;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.utils.XmlHelper;

import java.util.List;
import java.util.Map;

class Decorator implements HasEnabled, HasExtendedInfo {

  private final class Fields implements HasExtendedInfo {

    private final List<Parameter> params = Lists.newArrayList();
    private final Map<String, String> constants = Maps.newHashMap();
    private final Map<String, String> css = Maps.newHashMap();
    private Lifecycle lifecycle;
    private final List<Handler> handlers = Lists.newArrayList();
    private Template template;

    private String eventTarget;
    private String appearanceTarget;
    private boolean appearanceDeep;

    private Fields(List<Parameter> params, Map<String, String> constants, Map<String, String> css,
        Lifecycle lifecycle, List<Handler> handlers, Template template, String eventTarget,
        String appearanceTarget, boolean appearanceDeep) {
      super();

      if (params != null && !params.isEmpty()) {
        this.params.addAll(params);
      }
      if (constants != null && !constants.isEmpty()) {
        this.constants.putAll(constants);
      }

      if (css != null && !css.isEmpty()) {
        this.css.putAll(css);
      }

      this.lifecycle = lifecycle;

      if (handlers != null && !handlers.isEmpty()) {
        this.handlers.addAll(handlers);
      }
      this.template = template;

      this.eventTarget = eventTarget;
      this.appearanceTarget = appearanceTarget;
      this.appearanceDeep = appearanceDeep;
    }

    @Override
    public List<ExtendedProperty> getExtendedInfo() {
      List<ExtendedProperty> result = Lists.newArrayList();

      int idx;
      if (!params.isEmpty()) {
        PropertyUtils.addExtended(result, "parameters", BeeUtils.bracket(params.size()));
        idx = 0;
        for (Parameter param : params) {
          PropertyUtils.addChildren(result, BeeUtils.joinWords("param", ++idx),
              "Name", param.getName(),
              "Default", param.getDefaultValue(),
              "Required", param.isRequired());
        }
      }
      if (!constants.isEmpty()) {
        PropertyUtils.addExtended(result, "constants", BeeUtils.bracket(constants.size()));
        idx = 0;
        for (Map.Entry<String, String> entry : constants.entrySet()) {
          result.add(new ExtendedProperty(BeeUtils.joinWords(DecoratorConstants.TAG_CONST, ++idx),
              entry.getKey(), entry.getValue()));
        }
      }
      if (!css.isEmpty()) {
        PropertyUtils.addExtended(result, "styles", BeeUtils.bracket(css.size()));
        idx = 0;
        for (Map.Entry<String, String> entry : css.entrySet()) {
          PropertyUtils.addExtended(result,
              BeeUtils.joinWords(DecoratorConstants.TAG_STYLE, ++idx),
              entry.getKey(), entry.getValue());
        }
      }
      if (lifecycle != null) {
        PropertyUtils.addChildren(result, DecoratorConstants.TAG_LIFECYCLE,
            DecoratorConstants.TAG_CREATED, lifecycle.getCreated(),
            DecoratorConstants.TAG_INSERTED, lifecycle.getInserted(),
            DecoratorConstants.TAG_REMOVED, lifecycle.getRemoved());
      }
      if (!handlers.isEmpty()) {
        PropertyUtils.addExtended(result, "handlers", BeeUtils.bracket(handlers.size()));
        idx = 0;
        for (Handler handler : handlers) {
          PropertyUtils.addExtended(result, BeeUtils.joinWords(DecoratorConstants.TAG_HANDLER,
              ++idx),
              handler.getType(), handler.getBody());
        }
      }
      if (template != null) {
        PropertyUtils.addExtended(result, DecoratorConstants.TAG_TEMPLATE,
            template.getId(), template.getMarkup());
      }

      if (!BeeUtils.isEmpty(eventTarget)) {
        PropertyUtils.addExtended(result, DecoratorConstants.ATTR_EVENT_TARGET, eventTarget);
      }
      if (!BeeUtils.isEmpty(appearanceTarget)) {
        PropertyUtils.addExtended(result, DecoratorConstants.ATTR_APPEARANCE_TARGET,
            appearanceTarget);
        PropertyUtils.addExtended(result, DecoratorConstants.ATTR_APPEARANCE_DEEP,
            appearanceDeep);
      }

      return result;
    }

    private void addConstant(String name, String value, boolean override) {
      if (override || !constants.containsKey(name.trim())) {
        constants.put(name.trim(), BeeUtils.trim(value));
      }
    }

    private void addCss(String name, String text, boolean override) {
      if (override || !css.containsKey(name.trim())) {
        css.put(name.trim(), BeeUtils.trim(text));
      }
    }

    private void addParameter(Parameter param, boolean override) {
      int index = getParameterIndex(param);
      if (index >= 0) {
        if (override) {
          params.remove(index);
        } else {
          return;
        }
      }
      params.add(param);
    }

    private String getAppearanceTarget() {
      return appearanceTarget;
    }

    private Fields getCopy() {
      return new Fields(params, constants, css, lifecycle, handlers, template, eventTarget,
          appearanceTarget, appearanceDeep);
    }

    private String getEventTarget() {
      return eventTarget;
    }

    private Lifecycle getLifecycle() {
      return lifecycle;
    }

    private int getParameterIndex(Parameter param) {
      int index = BeeConst.UNDEF;
      if (params.isEmpty()) {
        return index;
      }

      for (int i = 0; i < params.size(); i++) {
        if (BeeUtils.same(params.get(i).getName(), param.getName())) {
          index = i;
          break;
        }
      }
      return index;
    }

    private Template getTemplate() {
      return template;
    }

    private boolean isAppearanceDeep() {
      return appearanceDeep;
    }

    private void setAppearanceDeep(boolean appearanceDeep) {
      this.appearanceDeep = appearanceDeep;
    }

    private void setAppearanceTarget(String appearanceTarget) {
      this.appearanceTarget = appearanceTarget;
    }

    private void setEventTarget(String eventTarget) {
      this.eventTarget = eventTarget;
    }

    private void setLifecycle(Lifecycle lifecycle) {
      this.lifecycle = lifecycle;
    }

    private void setTemplate(Template template) {
      this.template = template;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Decorator.class);

  private final boolean isAbstract;

  private final String id;
  private final String parent;

  private final Fields definedFields;
  private Fields fields;

  private boolean initialized;
  private int counter;

  private boolean enabled = true;

  Decorator(String id, String parent, boolean isAbstract, List<Parameter> params,
      Map<String, String> constants, Map<String, String> css, Lifecycle lifecycle,
      List<Handler> handlers, Template template, String eventTarget, String appearanceTarget,
      boolean appearanceDeep) {
    super();
    this.id = id;
    this.parent = parent;
    this.isAbstract = isAbstract;

    this.definedFields = new Fields(params, constants, css, lifecycle, handlers, template,
        eventTarget, appearanceTarget, appearanceDeep);
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> result = Lists.newArrayList();
    PropertyUtils.addProperties(result, false,
        DecoratorConstants.ATTR_ID, getId(),
        DecoratorConstants.ATTR_EXTENDS, getParent(),
        DecoratorConstants.TAG_ABSTRACT, isAbstract(),
        "initialized", isInitialized(),
        "counter", getCounter(),
        "enabled", isEnabled());

    PropertyUtils.appendWithPrefix(result, "def", definedFields.getExtendedInfo());
    if (getFields() != null) {
      PropertyUtils.appendExtended(result, getFields().getExtendedInfo());
    }
    return result;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  IdentifiableWidget decorate(IdentifiableWidget widget, Map<String, String> options) {
    if (widget == null) {
      logger.severe("decorator", getId(), "widget is null");
      return widget;
    }

    if (getFields() == null) {
      logger.severe("decorator", getId(), "has no fields");
      return widget;
    }
    if (!checkRequiredParameters(options)) {
      return widget;
    }

    if (!hasTemplate()) {
      logger.severe("decorator", getId(), "has no template");
      return widget;
    }

    Map<String, String> substitutes = getSubstitutes(options);
    Element widgetElement = widget.asWidget().getElement();

    Element root = createElement(substitutes, widgetElement);
    if (root == null) {
      logger.severe("decorator", getId(), "cannot create element");
      return widget;
    }
    if (BeeUtils.same(root.getTagName(), DecoratorConstants.TAG_CONTENT)) {
      logger.severe("decorator", getId(), "template root element equals content");
      logger.severe(root.toString());
      return widget;
    }

    NodeList<Element> list = root.getElementsByTagName(DecoratorConstants.TAG_CONTENT);
    int cnt = (list == null) ? 0 : list.getLength();
    if (cnt < 1) {
      logger.severe("decorator", getId(), "template has no content");
      logger.severe(root.toString());
      return widget;
    } else if (cnt > 1) {
      logger.severe("decorator", getId(), "template has", cnt, "contents");
      logger.severe(root.toString());
      return widget;
    }

    Element templateContent = list.getItem(0);
    String role = templateContent.getAttribute(DomUtils.ATTRIBUTE_ROLE);
    Style style = templateContent.getStyle();
    String classes = templateContent.getClassName();

    Element contentParent = templateContent.getParentElement();
    contentParent.replaceChild(widgetElement, templateContent);

    if (!BeeUtils.isEmpty(role)) {
      widgetElement.setAttribute(DomUtils.ATTRIBUTE_ROLE, role);
    }
    if (style != null) {
      StyleUtils.addStyle(widgetElement, style);
    }
    if (!BeeUtils.isEmpty(classes)) {
      StyleUtils.updateClasses(widgetElement, classes);
    }
    widgetElement.addClassName(getId() + "-content");

    addAppearance(root, widgetElement, options);

    if (getCounter() <= 0) {
      addStyleSheets(substitutes);
    }

    JsFunction onCreated = null;
    JsFunction onInserted = null;
    JsFunction onRemoved = null;

    Lifecycle lifecycle = getFields().getLifecycle();
    if (lifecycle != null) {
      onCreated = createFunction(lifecycle.getCreated(), substitutes);
      onInserted = createFunction(lifecycle.getInserted(), substitutes);
      onRemoved = createFunction(lifecycle.getRemoved(), substitutes);
    }

    DecoratedWidget decoratedWidget = new DecoratedWidget(widget.asWidget(), root, onInserted,
        onRemoved);

    addHandlers(decoratedWidget.asWidget(), widget.asWidget(), substitutes);

    if (onCreated != null) {
      onCreated.call(root);
    }

    setCounter(getCounter() + 1);
    return decoratedWidget;
  }

  Fields getFields() {
    return fields;
  }

  String getId() {
    return id;
  }

  String getParent() {
    return parent;
  }

  void init(Fields parentFields) {
    setFields(definedFields.getCopy());

    if (parentFields != null) {
      if (!parentFields.params.isEmpty()) {
        for (Parameter param : parentFields.params) {
          getFields().addParameter(param, false);
        }
      }
      if (!parentFields.constants.isEmpty()) {
        for (Map.Entry<String, String> constant : parentFields.constants.entrySet()) {
          getFields().addConstant(constant.getKey(), constant.getValue(), false);
        }
      }
      if (!parentFields.css.isEmpty()) {
        for (Map.Entry<String, String> sheet : parentFields.css.entrySet()) {
          getFields().addCss(sheet.getKey(), sheet.getValue(), false);
        }
      }
      if (parentFields.getLifecycle() != null) {
        if (getFields().getLifecycle() == null) {
          getFields().setLifecycle(parentFields.getLifecycle().getCopy());
        } else {
          getFields().getLifecycle().updateFrom(parentFields.getLifecycle(), false);
        }
      }

      if (!parentFields.handlers.isEmpty()) {
        String eventTarget = parentFields.getEventTarget();
        for (Handler parentHandler : parentFields.handlers) {
          Handler handler = parentHandler.copyOf();
          if (BeeUtils.isEmpty(handler.getTarget()) && !BeeUtils.isEmpty(eventTarget)) {
            handler.setTarget(eventTarget);
          }
          getFields().handlers.add(handler);
        }
      }

      if (getFields().getTemplate() == null && parentFields.getTemplate() != null) {
        getFields().setTemplate(parentFields.getTemplate().getCopy());
      }

      if (BeeUtils.isEmpty(getFields().getEventTarget())
          && !BeeUtils.isEmpty(parentFields.getEventTarget())) {
        getFields().setEventTarget(parentFields.getEventTarget());
      }
      if (BeeUtils.isEmpty(getFields().getAppearanceTarget())
          && !BeeUtils.isEmpty(parentFields.getAppearanceTarget())) {
        getFields().setAppearanceTarget(parentFields.getAppearanceTarget());
        getFields().setAppearanceDeep(parentFields.isAppearanceDeep());
      }
    }

    setInitialized(true);
  }

  boolean isAbstract() {
    return isAbstract;
  }

  boolean isInitialized() {
    return initialized;
  }

  void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  private void addAppearance(Element root, Element content, Map<String, String> options) {
    if (options == null || options.isEmpty()) {
      return;
    }

    String classes = options.get(DecoratorConstants.OPTION_ROOT_CLASS);
    String styles = options.get(DecoratorConstants.OPTION_ROOT_STYLE);
    StyleUtils.updateAppearance(root, classes, styles);

    classes = options.get(DecoratorConstants.OPTION_CONTENT_CLASS);
    styles = options.get(DecoratorConstants.OPTION_CONTENT_STYLE);
    StyleUtils.updateAppearance(content, classes, styles);

    classes = options.get(DecoratorConstants.OPTION_ROLE_CLASS);
    styles = options.get(DecoratorConstants.OPTION_ROLE_STYLE);
    if (!BeeUtils.isEmpty(classes)) {
      TuningHelper.updateRoleClasses(root, classes);
    }
    if (!BeeUtils.isEmpty(styles)) {
      TuningHelper.updateRoleStyles(root, styles);
    }

    String role = getFields().getAppearanceTarget();
    if (BeeUtils.isEmpty(role)) {
      return;
    }

    classes = options.get(DecoratorConstants.OPTION_CLASS);
    styles = options.get(DecoratorConstants.OPTION_STYLE);
    if (BeeUtils.allEmpty(classes, styles)) {
      return;
    }

    if (BeeUtils.same(role, DecoratorConstants.ROLE_ROOT)) {
      StyleUtils.updateAppearance(root, classes, styles);
      return;
    }
    if (BeeUtils.same(role, DecoratorConstants.ROLE_CONTENT)) {
      StyleUtils.updateAppearance(content, classes, styles);
      return;
    }

    Element cutoff = getFields().isAppearanceDeep() ? null : content;
    List<Element> targets = TuningHelper.getActors(root, role, null, cutoff);
    if (targets.isEmpty()) {
      logger.warning("decorator", getId(), "appearance role", role, "no actors found");
      return;
    }

    for (Element target : targets) {
      StyleUtils.updateAppearance(target, classes, styles);
    }
  }

  private void addHandlers(Widget decorated, Widget content, Map<String, String> substitutes) {
    String eventTarget = getFields().getEventTarget();
    Element rootElement = decorated.getElement();
    Element contentElement = content.getElement();

    for (Handler handler : getFields().handlers) {
      String type = handler.getType();
      String body = substitute(handler.getBody(), substitutes);
      if (BeeUtils.isEmpty(type) || BeeUtils.isEmpty(body)) {
        continue;
      }

      String role = BeeUtils.notEmpty(handler.getTarget(), eventTarget);

      if (BeeUtils.isEmpty(role) || BeeUtils.same(role, DecoratorConstants.ROLE_ROOT)) {
        EventUtils.addDomHandler(decorated, type, body);
        continue;
      }
      if (BeeUtils.same(role, DecoratorConstants.ROLE_CONTENT)) {
        EventUtils.addDomHandler(content, type, body);
        continue;
      }

      Element cutoff = handler.isDeep() ? null : contentElement;
      List<Element> targets = TuningHelper.getActors(rootElement, role, null, cutoff);
      if (targets.isEmpty()) {
        logger.warning("decorator", getId(), "handler", type, "role", role,
            "no actors found");
        continue;
      }

      for (Element target : targets) {
        if (target.equals(rootElement)) {
          EventUtils.addDomHandler(decorated, type, body);
          continue;
        }
        if (target.equals(contentElement)) {
          EventUtils.addDomHandler(content, type, body);
          continue;
        }

        EventListener eventListener = DOM.getEventListener(target);
        if (eventListener == null) {
          DOM.setEventListener(target, decorated);
        } else if (!decorated.equals(eventListener)) {
          continue;
        }
        EventUtils.addDomHandler(decorated, type, body);
      }
    }
  }

  private void addStyleSheets(Map<String, String> substitutes) {
    for (Map.Entry<String, String> sheet : getFields().css.entrySet()) {
      String name = sheet.getKey();
      String text = sheet.getValue();
      Global.addStyleSheet(substitute(name, substitutes), substitute(text, substitutes));
    }
  }

  private boolean checkRequiredParameters(Map<String, String> options) {
    if (getFields().params.isEmpty()) {
      return true;
    }

    boolean ok = true;
    for (Parameter param : getFields().params) {
      if (!param.isRequired()) {
        continue;
      }
      if (options == null || BeeUtils.isEmpty(options.get(param.getName()))) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  private Element createElement(Map<String, String> substitutes, Element content) {
    DivElement tmpDiv = Document.get().createDivElement();
    tmpDiv.setInnerHTML(substitute(getFields().getTemplate().getMarkup(), substitutes));
    Element element = tmpDiv.getFirstChildElement();

    if (element == null) {
      return element;
    }

    List<Element> children = DomUtils.getElementsByAttributeValue(element, XmlHelper.ATTR_XMLNS,
        DecoratorConstants.NAMESPACE, content, content);
    for (Element child : children) {
      child.removeAttribute(XmlHelper.ATTR_XMLNS);
    }

    return element;
  }

  private static JsFunction createFunction(String source, Map<String, String> substitutes) {
    String body = substitute(source, substitutes);

    if (BeeUtils.isEmpty(body)) {
      return null;
    } else {
      return JsFunction.create(body);
    }
  }

  private int getCounter() {
    return counter;
  }

  private Map<String, String> getSubstitutes(Map<String, String> options) {
    Map<String, String> result = Maps.newHashMap();
    for (Parameter param : getFields().params) {
      result.put(wrapSubstitute(param.getName()), param.getValue(options));
    }
    for (Map.Entry<String, String> constant : getFields().constants.entrySet()) {
      result.put(wrapSubstitute(constant.getKey()), BeeUtils.trim(constant.getValue()));
    }

    for (Map.Entry<String, String> entry : result.entrySet()) {
      String value = substitute(entry.getValue(), result);
      if (!BeeUtils.equalsTrim(value, entry.getValue())) {
        entry.setValue(value);
      }
    }
    return result;
  }

  private boolean hasTemplate() {
    return getFields().getTemplate() != null
        && !BeeUtils.isEmpty(getFields().getTemplate().getMarkup());
  }

  private void setCounter(int counter) {
    this.counter = counter;
  }

  private void setFields(Fields fields) {
    this.fields = fields;
  }

  private static String substitute(String source, Map<String, String> substitutes) {
    if (BeeUtils.isEmpty(source) || substitutes.isEmpty()) {
      return source;
    }

    String result = source.trim();
    for (Map.Entry<String, String> entry : substitutes.entrySet()) {
      if (entry.getValue() != null) {
        result = result.replace(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private static String wrapSubstitute(String s) {
    return DecoratorConstants.SUBSTITUTE_PREFIX + s.trim() + DecoratorConstants.SUBSTITUTE_SUFFIX;
  }
}
