package com.butent.bee.client.rights;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public abstract class RightsForm extends AbstractFormInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(RightsForm.class);

  protected static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Rights-";
  protected static final String STYLE_SUFFIX_CELL = "-cell";

  private static final String STYLE_PANEL = STYLE_PREFIX + "panel";

  private static final String STYLE_OBJECT_LABEL = STYLE_PREFIX + "object-label";
  private static final String STYLE_OBJECT_LABEL_CELL = STYLE_OBJECT_LABEL + STYLE_SUFFIX_CELL;
  private static final String STYLE_OBJECT_TOGGLE = STYLE_PREFIX + "object-toggle";
  private static final String STYLE_OBJECT_TOGGLE_CELL = STYLE_OBJECT_TOGGLE + STYLE_SUFFIX_CELL;

  private static final String STYLE_VALUE_TOGGLE = STYLE_PREFIX + "value-toggle";
  private static final String STYLE_VALUE_CELL = STYLE_PREFIX + "value-cell";
  private static final String STYLE_VALUE_ROW = STYLE_PREFIX + "value-row";
  private static final String STYLE_VALUE_CHANGED = STYLE_PREFIX + "value-changed";
  private static final String STYLE_VALUE_DISABLED = STYLE_PREFIX + "value-disabled";

  private static final String STYLE_FILTER_NOT_MATCHED = STYLE_PREFIX + "filter-not-matched";

  private static final String STYLE_OBJECT_LEVEL_PREFIX = STYLE_PREFIX + "object-level-";
  private static final String STYLE_OBJECT_HAS_CHILDREN = STYLE_PREFIX + "object-has-children";
  private static final String STYLE_OBJECT_LEAF = STYLE_PREFIX + "object-leaf";

  private static final String STYLE_OBJECT_OPEN = STYLE_PREFIX + "object-open";
  private static final String STYLE_OBJECT_CLOSED = STYLE_PREFIX + "object-closed";
  private static final String STYLE_OBJECT_HIDDEN = STYLE_PREFIX + "object-hidden";

  private static final String DATA_TYPE_OBJECT_LABEL = "ol";
  private static final String DATA_TYPE_OBJECT_TOGGLE = "ot";
  private static final String DATA_TYPE_VALUE = "v";

  private static final String DATA_KEY_TYPE = "rights-type";
  private static final String DATA_KEY_OBJECT = "rights-object";

  private static final int LABEL_ROW = 0;
  private static final int LABEL_COL = 0;

  public static void register() {
    FormFactory.registerFormInterceptor("ModuleRights", new ModuleRightsHandler());
    FormFactory.registerFormInterceptor("MenuRights", new MenuRightsHandler());

    FormFactory.registerFormInterceptor("DataRights", new DataRightsHandler());
    FormFactory.registerFormInterceptor("FieldRights", new FieldRightsHandler());

    FormFactory.registerFormInterceptor("WidgetRights", new WidgetRightsHandler());
  }

  protected static Toggle createValueToggle(String objectName) {
    Toggle toggle = new Toggle(FontAwesome.TIMES, FontAwesome.CHECK, STYLE_VALUE_TOGGLE, false);

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_TYPE, DATA_TYPE_VALUE);
    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_OBJECT, objectName);

    return toggle;
  }

  protected static void debug(Object... messages) {
    logger.debug(messages);
  }

  protected static void enableValueWidget(Element elem, boolean enabled) {
    if (enabled) {
      elem.removeClassName(STYLE_VALUE_DISABLED);
    } else {
      elem.addClassName(STYLE_VALUE_DISABLED);
    }
  }

  protected static String getObjectName(Element elem) {
    String objectName = DomUtils.getDataProperty(elem, DATA_KEY_OBJECT);
    if (BeeUtils.isEmpty(objectName)) {
      severe("element", elem.getId(), "has no object name");
    }
    return objectName;
  }

  protected static String getObjectName(Widget widget) {
    return getObjectName(widget.getElement());
  }

  protected static void markObjectLabel(Widget widget, RightsObject object) {
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_LABEL);
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_OBJECT, object.getName());
  }

  protected static void setNotMatched(Collection<? extends Element> elements) {
    if (!BeeUtils.isEmpty(elements)) {
      StyleUtils.addClassName(elements, STYLE_FILTER_NOT_MATCHED);
    }
  }

  protected static void severe(Object... messages) {
    logger.severe(messages);
  }

  protected static void updateValueCell(Widget widget, boolean isChanged) {
    TableCellElement cellElement = DomUtils.getParentCell(widget, false);

    if (cellElement == null) {
      logger.warning("cell not found");
    } else if (isChanged) {
      cellElement.addClassName(STYLE_VALUE_CHANGED);
    } else {
      cellElement.removeClassName(STYLE_VALUE_CHANGED);
    }
  }

  protected static void warning(Object... messages) {
    logger.warning(messages);
  }

  private static void removeClassName(UIObject root, String className) {
    NodeList<Element> nodes = Selectors.getNodes(root, Selectors.classSelector(className));
    if (!DomUtils.isEmpty(nodes)) {
      StyleUtils.removeClassName(nodes, className);
    }
  }

  private final List<RightsObject> objects = new ArrayList<>();

  private RightsTable table;

  @Override
  public boolean beforeAction(Action action, final Presenter presenter) {
    switch (action) {
      case CLOSE:
        if (!getChangedNames().isEmpty()) {
          onClose(() -> BeeKeeper.getScreen().closeWidget(presenter.getMainView()));
          return false;

        } else {
          return true;
        }

      case SAVE:
        save(null);
        return false;

      default:
        return super.beforeAction(action, presenter);
    }
  }

  protected void addColumnLabel(int col, Widget widget, String cellStyleName) {
    table.setWidget(LABEL_ROW, col, widget, cellStyleName);
  }

  protected void addColumnToggle(int col, Widget widget, String cellStyleName) {
    table.setWidget(getValueStartRow() - 1, col, widget, cellStyleName);
  }

  protected void addObjectLabel(int row, RightsObject object) {
    table.setWidget(row, LABEL_COL, createObjectLabel(object), STYLE_OBJECT_LABEL_CELL);
  }

  protected void addObjectToggle(int row, RightsObject object) {
    table.setWidget(row, table.getValueStartCol() - 1, createObjectToggle(object.getName()),
        STYLE_OBJECT_TOGGLE_CELL);
  }

  protected void addValueToggle(int row, int col, Toggle toggle) {
    table.setWidget(row, col, toggle, STYLE_VALUE_CELL);
  }

  protected void afterCreateValueRow(int row, RightsObject object) {
    if (object.hasParent()) {
      StyleUtils.addClassName(table.getRowCells(row), STYLE_OBJECT_HIDDEN);
    }
    table.getRowFormatter().addStyleName(row, STYLE_VALUE_ROW);
  }

  protected void createUi() {
    HasIndexedWidgets panel;

    if (getFormView() != null && getFormView().getRootWidget() instanceof HasIndexedWidgets) {
      getFormView().getRootWidget().addStyleName(STYLE_PANEL);
      getFormView().getRootWidget().addStyleName(getPanelStyleName());

      panel = (HasIndexedWidgets) getFormView().getRootWidget();

    } else {
      logger.severe("root panel not available");
      return;
    }

    if (!panel.isEmpty()) {
      panel.clear();
    }

    if (table == null) {
      this.table = new RightsTable() {
        @Override
        public int getValueStartCol() {
          return RightsForm.this.getValueStartCol();
        }
      };
    } else if (!table.isEmpty()) {
      table.clear();
    }

    populateTable();
    panel.add(table);
  }

  protected List<RightsObject> filterByModule(ModuleAndSub moduleAndSub) {
    return RightsHelper.filterByModule(objects, moduleAndSub);
  }

  protected List<RightsObject> filterByParent(String parent) {
    List<RightsObject> result = new ArrayList<>();

    for (RightsObject object : objects) {
      if (Objects.equals(object.getParent(), parent)) {
        result.add(object);
      }
    }

    return result;
  }

  protected RightsObject findObject(String objectName) {
    for (RightsObject object : objects) {
      if (object.getName().equals(objectName)) {
        return object;
      }
    }

    logger.severe("object", objectName, "not found");
    return null;
  }

  protected abstract Set<String> getChangedNames();

  protected abstract String getChangeMessage(RightsObject object);

  protected String getDialogCaption() {
    return getFormView().getCaption();
  }

  protected List<ModuleAndSub> getModules() {
    List<ModuleAndSub> modules = new ArrayList<>();
    boolean emptyModule = false;

    for (RightsObject object : objects) {
      if (object.getModuleAndSub() == null) {
        emptyModule = true;
      } else if (!modules.contains(object.getModuleAndSub())) {
        modules.add(object.getModuleAndSub());
      }
    }

    if (modules.size() > 1) {
      Collections.sort(modules);
    }
    if (emptyModule) {
      modules.add(null);
    }

    return modules;
  }

  protected List<TableCellElement> getObjectCells(String objectName) {
    List<TableCellElement> cells = new ArrayList<>();

    NodeList<Element> nodes = Selectors.getNodes(table,
        Selectors.attributeEquals(Attributes.DATA_PREFIX + DATA_KEY_OBJECT, objectName));

    if (DomUtils.isEmpty(nodes)) {
      logger.warning("object", objectName, "nodes nof found");

    } else {
      for (int i = 0; i < nodes.getLength(); i++) {
        TableCellElement cell = DomUtils.getParentCell(nodes.getItem(i), true);
        if (cell != null) {
          cells.add(cell);
        }
      }
    }

    return cells;
  }

  protected List<RightsObject> getObjects() {
    return objects;
  }

  protected Toggle getObjectToggle(String objectName) {
    for (Widget widget : table) {
      if (widget instanceof Toggle
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_TOGGLE)
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_OBJECT, objectName)) {
        return (Toggle) widget;
      }
    }

    logger.severe("object", objectName, "toggle not found");
    return null;
  }

  protected List<Toggle> getObjectToggles() {
    List<Toggle> toggles = new ArrayList<>();

    for (Widget widget : table) {
      if (widget instanceof Toggle
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_TOGGLE)) {
        toggles.add((Toggle) widget);
      }
    }

    return toggles;
  }

  protected abstract RightsObjectType getObjectType();

  protected abstract String getPanelStyleName();

  protected RightsTable getTable() {
    return table;
  }

  protected static String getValueSelector() {
    return Selectors.attributeEquals(Attributes.DATA_PREFIX + DATA_KEY_TYPE, DATA_TYPE_VALUE);
  }

  protected abstract int getValueStartCol();

  protected static int getValueStartRow() {
    return 2;
  }

  protected static boolean hasObject(Widget widget, String objectName) {
    return DomUtils.dataEquals(widget.getElement(), DATA_KEY_OBJECT, objectName);
  }

  protected void init() {
    initObjects(typeObjects -> {
      if (BeeUtils.isEmpty(typeObjects)) {
        logger.severe(getObjectType(), "objects not available");
        return;
      }

      if (!objects.isEmpty()) {
        objects.clear();
      }
      objects.addAll(typeObjects);

      initData(input -> {
        if (BeeUtils.isTrue(input)) {
          createUi();
        }
      });
    });
  }

  protected abstract void initData(Consumer<Boolean> callback);

  protected abstract void initObjects(Consumer<List<RightsObject>> consumer);

  protected static boolean isDataType(Widget widget, String type) {
    return DomUtils.dataEquals(widget.getElement(), DATA_KEY_TYPE, type);
  }

  protected abstract boolean isObjectChecked(String objectName);

  protected static boolean isValueWidget(Widget widget) {
    return DomUtils.dataEquals(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_VALUE);
  }

  protected void markValueRow(int row) {
    table.getRowFormatter().addStyleName(row, STYLE_VALUE_ROW);
  }

  protected void markValueRows() {
    for (int row = getValueStartRow(); row < table.getRowCount(); row++) {
      markValueRow(row);
    }
  }

  protected void onClearChanges() {
    removeClassName(table, STYLE_VALUE_CHANGED);
  }

  protected void onClose(final Runnable runnable) {
    Set<String> changedNames = getChangedNames();

    String message = BeeUtils.joinWords(Localized.dictionary().changedValues(),
        BeeUtils.bracket(changedNames.size()));
    List<String> messages = Lists.newArrayList(message);

    List<RightsObject> changedObjects = new ArrayList<>();
    for (RightsObject object : objects) {
      if (changedNames.contains(object.getName())) {
        changedObjects.add(object);
      }
    }

    int limit = BeeUtils.resize(BeeKeeper.getScreen().getHeight(), 200, 1000, 2, 10);
    int count = (changedObjects.size() > limit * 3 / 2) ? limit : changedObjects.size();

    for (int i = 0; i < count; i++) {
      messages.add(getChangeMessage(changedObjects.get(i)));
    }

    if (count < changedObjects.size()) {
      messages.add(BeeUtils.joinWords(BeeUtils.parenthesize(changedObjects.size() - count),
          BeeConst.ELLIPSIS));
    }

    messages.add(Localized.dictionary().saveChanges());

    DecisionCallback callback = new DecisionCallback() {
      @Override
      public void onConfirm() {
        save(input -> {
          if (BeeUtils.isTrue(input)) {
            runnable.run();
          }
        });
      }

      @Override
      public void onDeny() {
        runnable.run();
      }
    };

    Global.decide(getDialogCaption(), messages, callback, DialogConstants.DECISION_YES);
  }

  protected abstract void onObjectToggle(boolean checked);

  protected abstract void populateTable();

  protected void resetFilter() {
    removeClassName(table, STYLE_FILTER_NOT_MATCHED);
  }

  protected abstract void save(Consumer<Boolean> callback);

  protected static void setDataType(Widget widget, String type) {
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_TYPE, type);
  }

  protected abstract void updateObjectValueToggles(String objectName, boolean checked);

  private Widget createObjectLabel(RightsObject object) {
    Label widget = new Label(object.getCaption());
    widget.addStyleName(STYLE_OBJECT_LABEL);

    widget.addStyleName(STYLE_OBJECT_LEVEL_PREFIX + object.getLevel());
    widget.addStyleName(object.hasChildren() ? STYLE_OBJECT_HAS_CHILDREN : STYLE_OBJECT_LEAF);

    widget.setTitle(object.getName());

    markObjectLabel(widget, object);

    if (object.hasChildren()) {
      widget.addStyleName(STYLE_OBJECT_CLOSED);

      widget.addClickHandler(event -> {
        if (event.getSource() instanceof Widget) {
          Widget source = (Widget) event.getSource();
          String objectName = getObjectName(source);
          boolean wasOpen = source.getElement().hasClassName(STYLE_OBJECT_OPEN);

          if (!BeeUtils.isEmpty(objectName)) {
            source.setStyleName(STYLE_OBJECT_OPEN, !wasOpen);
            source.setStyleName(STYLE_OBJECT_CLOSED, wasOpen);

            setChildrenVisibility(objectName, !wasOpen);
          }
        }
      });
    }

    return widget;
  }

  private Widget createObjectToggle(String objectName) {
    Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_OBJECT_TOGGLE, isObjectChecked(objectName));

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_TOGGLE);
    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_OBJECT, objectName);

    toggle.addClickHandler(event -> {
      if (event.getSource() instanceof Toggle) {
        Toggle ot = (Toggle) event.getSource();
        updateObjectValueToggles(getObjectName(ot), ot.isChecked());

        onObjectToggle(ot.isChecked());
      }
    });

    return toggle;
  }

  private Widget getObjectLabel(String objectName) {
    for (Widget widget : table) {
      if (DomUtils.dataEquals(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_LABEL)
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_OBJECT, objectName)) {
        return widget;
      }
    }

    logger.severe("object", objectName, "label not found");
    return null;
  }

  private void setChildrenVisibility(String parent, boolean visible) {
    for (RightsObject object : objects) {
      if (parent.equals(object.getParent())) {
        List<TableCellElement> cells = getObjectCells(object.getName());

        if (!cells.isEmpty()) {
          if (visible) {
            StyleUtils.removeClassName(cells, STYLE_OBJECT_HIDDEN);
          } else {
            StyleUtils.addClassName(cells, STYLE_OBJECT_HIDDEN);
          }
        }

        if (!visible && object.hasChildren()) {
          Widget labelWidget = getObjectLabel(object.getName());

          if (labelWidget != null && labelWidget.getElement().hasClassName(STYLE_OBJECT_OPEN)) {
            labelWidget.removeStyleName(STYLE_OBJECT_OPEN);
            labelWidget.addStyleName(STYLE_OBJECT_CLOSED);

            setChildrenVisibility(object.getName(), visible);
          }
        }
      }
    }
  }
}
