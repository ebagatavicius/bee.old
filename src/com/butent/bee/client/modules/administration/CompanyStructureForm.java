package com.butent.bee.client.modules.administration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallbackWithId;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.CellKind;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Badge;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Line;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.treelayout.Configuration;
import com.butent.bee.shared.treelayout.Configuration.AlignmentInLevel;
import com.butent.bee.shared.treelayout.Configuration.Location;
import com.butent.bee.shared.treelayout.NodeExtentProvider;
import com.butent.bee.shared.treelayout.TreeLayout;
import com.butent.bee.shared.treelayout.util.DefaultConfiguration;
import com.butent.bee.shared.treelayout.util.DefaultTreeForTreeLayout;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class CompanyStructureForm extends AbstractFormInterceptor implements HandlesAllDataEvents {

  private static final class DepartmentPosition implements Comparable<DepartmentPosition> {

    private final long id;
    private final String name;

    private int planned;
    private final List<Long> staff = new ArrayList<>();

    private DepartmentPosition(long id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public int compareTo(DepartmentPosition o) {
      if (Objects.equals(id, o.id)) {
        return BeeConst.COMPARE_EQUAL;
      } else {
        return Collator.DEFAULT.compare(name, o.name);
      }
    }

    private void addStaff(long emplId) {
      staff.add(emplId);
    }

    private String getStyleName() {
      if (staff.size() < planned) {
        return STYLE_POSITION_UNDER;
      } else if (staff.size() > planned) {
        return STYLE_POSITION_OVER;
      } else {
        return STYLE_POSITION_PLANNED;
      }
    }

    private void setPlanned(int planned) {
      this.planned = planned;
    }
  }

  private enum LineType {
    STRAIGHT, BROKEN
  }

  private static final BeeLogger logger = LogUtils.getLogger(CompanyStructureForm.class);

  private static final List<String> viewNames = Lists.newArrayList(VIEW_DEPARTMENTS,
      VIEW_DEPARTMENT_EMPLOYEES, VIEW_DEPARTMENT_POSITIONS);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "orgChart-";

  private static final String STYLE_LEVEL_PREFIX = "level-";

  private static final String STYLE_DEPARTMENT_PREFIX = STYLE_PREFIX + "department-";
  private static final String STYLE_DEPARTMENT_PANEL = STYLE_DEPARTMENT_PREFIX + "panel";
  private static final String STYLE_DEPARTMENT_LABEL = STYLE_DEPARTMENT_PREFIX + "label";
  private static final String STYLE_DEPARTMENT_CONTENT = STYLE_DEPARTMENT_PREFIX + "content";

  private static final String STYLE_DEPARTMENT_DRAG = STYLE_DEPARTMENT_PREFIX + "drag";
  private static final String STYLE_DEPARTMENT_DRAG_OVER = STYLE_DEPARTMENT_PREFIX + "dragOver";

  private static final String STYLE_BOSS_PREFIX = STYLE_PREFIX + "boss-";
  private static final String STYLE_BOSS_TABLE = STYLE_BOSS_PREFIX + "table";
  private static final String STYLE_BOSS_LABEL = STYLE_BOSS_PREFIX + "label";
  private static final String STYLE_BOSS_DRAG = STYLE_BOSS_PREFIX + "drag";

  private static final String STYLE_EMPLOYEE_PREFIX = STYLE_PREFIX + "employee-";
  private static final String STYLE_EMPLOYEE_TABLE = STYLE_EMPLOYEE_PREFIX + "table";
  private static final String STYLE_EMPLOYEE_SUMMARY = STYLE_EMPLOYEE_PREFIX + "summary";
  private static final String STYLE_EMPLOYEE_DETAILS = STYLE_EMPLOYEE_PREFIX + "details";
  private static final String STYLE_EMPLOYEE_LABEL = STYLE_EMPLOYEE_PREFIX + "label";
  private static final String STYLE_EMPLOYEE_POSITION = STYLE_EMPLOYEE_PREFIX + "position";
  private static final String STYLE_EMPLOYEE_COUNT = STYLE_EMPLOYEE_PREFIX + "count";
  private static final String STYLE_EMPLOYEE_DRAG = STYLE_EMPLOYEE_PREFIX + "drag";

  private static final String STYLE_PHOTO_PREFIX = STYLE_PREFIX + "photo-";
  private static final String STYLE_PHOTO_CONTAINER = STYLE_PHOTO_PREFIX + "container";

  private static final String STYLE_POPUP_CONTENT = STYLE_PREFIX + "popup-content";

  private static final String STYLE_POSITION_PREFIX = STYLE_PREFIX + "position-";
  private static final String STYLE_POSITION_TABLE = STYLE_POSITION_PREFIX + "table";
  private static final String STYLE_POSITION_LABEL = STYLE_POSITION_PREFIX + "label";
  private static final String STYLE_POSITION_STAFFED = STYLE_POSITION_PREFIX + "staffed";
  private static final String STYLE_POSITION_ACTUAL = STYLE_POSITION_PREFIX + "actual";
  private static final String STYLE_POSITION_PLANNED = STYLE_POSITION_PREFIX + "planned";
  private static final String STYLE_POSITION_OVER = STYLE_POSITION_PREFIX + "over";
  private static final String STYLE_POSITION_UNDER = STYLE_POSITION_PREFIX + "under";

  private static final String STYLE_POSITIONS_AND_EMPLOYEES = STYLE_PREFIX + "pos-and-empl";

  private static final String STYLE_TOGGLE_POSITIONS = STYLE_PREFIX + "toggle-positions";
  private static final String STYLE_TOGGLE_EMPLOYEES = STYLE_PREFIX + "toggle-employees";

  private static final String STYLE_LINE_PREFIX = STYLE_PREFIX + "line-";
  private static final String STYLE_LINE_STRAIGHT = STYLE_LINE_PREFIX + "straight";
  private static final String STYLE_LINE_HORIZONTAL = STYLE_LINE_PREFIX + "horizontal";
  private static final String STYLE_LINE_VERTICAL = STYLE_LINE_PREFIX + "vertical";

  private static final String STYLE_BUBBLE_PREFIX = STYLE_PREFIX + "bubble-";
  private static final String STYLE_BUBBLE_PARENT = STYLE_BUBBLE_PREFIX + "parent";
  private static final String STYLE_BUBBLE_CHILD = STYLE_BUBBLE_PREFIX + "child";

  private static final String STYLE_SETTINGS_PREFIX = STYLE_PREFIX + "settings-";
  private static final String STYLE_SETTINGS_DIALOG = STYLE_SETTINGS_PREFIX + "dialog";
  private static final String STYLE_SETTINGS_TABLE = STYLE_SETTINGS_PREFIX + "table";
  private static final String STYLE_SETTINGS_SEPARATOR = STYLE_SETTINGS_PREFIX + "separator";
  private static final String STYLE_SETTINGS_COMMAND_PANEL = STYLE_SETTINGS_PREFIX + "commands";
  private static final String STYLE_SETTINGS_SAVE = STYLE_SETTINGS_PREFIX + "save";
  private static final String STYLE_SETTINGS_CANCEL = STYLE_SETTINGS_PREFIX + "cancel";

  private static final String STYLE_SETTINGS_LABEL_SUFFIX = "-label";
  private static final String STYLE_SETTINGS_INPUT_SUFFIX = "-input";

  private static final String DATA_TYPE_DEPARTMENT = "OrgChartDepartment";
  private static final String DATA_TYPE_BOSS = "OrgChartBoss";
  private static final String DATA_TYPE_EMPLOYEE = "OrgChartEmployee";

  private static final Set<String> DND_TYPES = ImmutableSet.of(DATA_TYPE_DEPARTMENT,
      DATA_TYPE_BOSS, DATA_TYPE_EMPLOYEE);

  private static final String KEY_STAFF = "staff";

  private static final Long ROOT = 0L;

  private static final String NAME_MARGIN_LEFT = "MarginLeft";
  private static final String NAME_MARGIN_TOP = "MarginTop";
  private static final String NAME_NODE_MIN_WIDTH = "NodeMinWidth";
  private static final String NAME_NODE_MAX_WIDTH = "NodeMaxWidth";
  private static final String NAME_NODE_MIN_HEIGHT = "NodeMinHeight";
  private static final String NAME_NODE_MAX_HEIGHT = "NodeMaxHeight";
  private static final String NAME_NODE_GAP = "NodeGap";
  private static final String NAME_LEVEL_GAP = "LevelGap";
  private static final String NAME_ALIGNMENT_IN_LEVEL = "AlignmentInLevel";
  private static final String NAME_LINE_TYPE = "LineType";

  private static final String NAME_SHOW_POSITIONS = "ShowPositions";
  private static final String NAME_SHOW_EMPLOYEES = "ShowEmployees";

  private static final int DEFAULT_MARGIN_LEFT = 10;
  private static final int DEFAULT_MARGIN_TOP = 10;
  private static final int DEFAULT_NODE_MIN_WIDTH = 150;
  private static final int DEFAULT_NODE_MAX_WIDTH = 400;
  private static final int DEFAULT_NODE_MIN_HEIGHT = 100;
  private static final int DEFAULT_NODE_MAX_HEIGHT = 600;
  private static final int DEFAULT_NODE_GAP = 20;
  private static final int DEFAULT_LEVEL_GAP = 30;

  private static final int MIN_LEVEL_GAP_FOR_LINES = 5;
  private static final int MIN_LEVEL_GAP_FOR_BUBBLES = 10;

  private static final AlignmentInLevel DEFAULT_ALIGNMENT_IN_LEVEL = AlignmentInLevel.TOWARDS_ROOT;
  private static final LineType DEFAULT_LINE_TYPE = LineType.STRAIGHT;

  private static final boolean DEFAULT_SHOW_POSITIONS = false;
  private static final boolean DEFAULT_SHOW_EMPLOYEES = false;

  private static void addLevelStyle(Element el, String prefix, int level) {
    el.addClassName(prefix + STYLE_LEVEL_PREFIX + BeeUtils.toString(level));
  }

  private static String storagePrefix() {
    Long userId = BeeKeeper.getUser().getUserId();
    return "CompanyStructure-" + (userId == null ? "" : BeeUtils.toString(userId)) + "-";
  }

  private static String storageKey(String name) {
    return storagePrefix() + name;
  }

  private final List<HandlerRegistration> handlerRegistry = new ArrayList<>();

  private BeeRowSet departments;
  private BeeRowSet employees;
  private BeeRowSet positions;

  private int marginLeft;
  private int marginTop;

  private int nodeMinWidth;
  private int nodeMaxWidth;
  private int nodeMinHeight;
  private int nodeMaxHeight;

  private int nodeGap;
  private int levelGap;

  private AlignmentInLevel alignmentInLevel;
  private LineType lineType;

  private boolean showPositions;
  private boolean showEmployees;

  private int lastRpcId;

  CompanyStructureForm() {
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case ADD:
        RowFactory.createRow(VIEW_DEPARTMENTS, Modality.ENABLED, result -> refresh());
        return false;

      case CONFIGURE:
        editSettings();
        return false;

      case REFRESH:
        refresh();
        return false;

      default:
        return super.beforeAction(action, presenter);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyStructureForm();
  }

  @Override
  public void onLoad(FormView form) {
    if (handlerRegistry.isEmpty()) {
      handlerRegistry.addAll(BeeKeeper.getBus().registerDataHandler(this, false));
    }

    readSettings();

    if (form.getViewPresenter() != null) {
      HeaderView header = getHeaderView();
      if (header != null) {
        addCommands(header);
      }
    }

    refresh();

    super.onLoad(form);
  }

  private static boolean readBoolean(String name, boolean defValue) {
    String key = storageKey(name);
    if (BeeKeeper.getStorage().hasItem(key)) {
      return BeeKeeper.getStorage().getBoolean(key);
    } else {
      return defValue;
    }
  }

  private static int readInt(String name) {
    Integer value = BeeKeeper.getStorage().getInteger(storageKey(name));
    return (value == null) ? BeeConst.UNDEF : value;
  }

  private static void store(String name, Boolean value) {
    BeeKeeper.getStorage().set(storageKey(name), value);
  }

  private static void store(String name, Integer value) {
    BeeKeeper.getStorage().set(storageKey(name), value);
  }

  private void readSettings() {
    int value = readInt(NAME_MARGIN_LEFT);
    marginLeft = (value >= 0) ? value : DEFAULT_MARGIN_LEFT;

    value = readInt(NAME_MARGIN_TOP);
    marginTop = (value >= 0) ? value : DEFAULT_MARGIN_TOP;

    value = readInt(NAME_NODE_MIN_WIDTH);
    nodeMinWidth = (value > 0) ? value : DEFAULT_NODE_MIN_WIDTH;
    value = readInt(NAME_NODE_MAX_WIDTH);
    nodeMaxWidth = (value > 0) ? value : DEFAULT_NODE_MAX_WIDTH;

    value = readInt(NAME_NODE_MIN_HEIGHT);
    nodeMinHeight = (value > 0) ? value : DEFAULT_NODE_MIN_HEIGHT;
    value = readInt(NAME_NODE_MAX_HEIGHT);
    nodeMaxHeight = (value > 0) ? value : DEFAULT_NODE_MAX_HEIGHT;

    value = readInt(NAME_NODE_GAP);
    nodeGap = (value > 0) ? value : DEFAULT_NODE_GAP;

    value = readInt(NAME_LEVEL_GAP);
    levelGap = (value > 0) ? value : DEFAULT_LEVEL_GAP;

    value = readInt(NAME_ALIGNMENT_IN_LEVEL);
    alignmentInLevel = BeeUtils.nvl(EnumUtils.getEnumByIndex(AlignmentInLevel.class, value),
        DEFAULT_ALIGNMENT_IN_LEVEL);

    value = readInt(NAME_LINE_TYPE);
    lineType = BeeUtils.nvl(EnumUtils.getEnumByIndex(LineType.class, value), DEFAULT_LINE_TYPE);

    showPositions = readBoolean(NAME_SHOW_POSITIONS, DEFAULT_SHOW_POSITIONS);
    showEmployees = readBoolean(NAME_SHOW_EMPLOYEES, DEFAULT_SHOW_EMPLOYEES);
  }

  private static String settingsLabelStyle(String name) {
    return STYLE_SETTINGS_PREFIX + name + STYLE_SETTINGS_LABEL_SUFFIX;
  }

  private static String settingsInputStyle(String name) {
    return STYLE_SETTINGS_PREFIX + name + STYLE_SETTINGS_INPUT_SUFFIX;
  }

  private void editSettings() {
    final DialogBox dialog = DialogBox.create(Localized.dictionary().settings(),
        STYLE_SETTINGS_DIALOG);

    HtmlTable table = new HtmlTable(STYLE_SETTINGS_TABLE);
    table.setColumnCellKind(0, CellKind.LABEL);
    table.setColumnCellKind(2, CellKind.LABEL);

    int row = 0;
    int col = 0;

    Label marginLeftLabel = new Label(NAME_MARGIN_LEFT);
    table.setWidgetAndStyle(row, col++, marginLeftLabel, settingsLabelStyle(NAME_MARGIN_LEFT));

    final InputSpinner marginLeftInput = new InputSpinner(0, 100, 5);
    marginLeftInput.setValue(marginLeft);
    table.setWidgetAndStyle(row, col++, marginLeftInput, settingsInputStyle(NAME_MARGIN_LEFT));

    Label marginTopLabel = new Label(NAME_MARGIN_TOP);
    table.setWidgetAndStyle(row, col++, marginTopLabel, settingsLabelStyle(NAME_MARGIN_TOP));

    final InputSpinner marginTopInput = new InputSpinner(0, 100, 5);
    marginTopInput.setValue(marginTop);
    table.setWidgetAndStyle(row, col++, marginTopInput, settingsInputStyle(NAME_MARGIN_TOP));

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    col = 0;
    Label nodeMinWidthLabel = new Label(NAME_NODE_MIN_WIDTH);
    table.setWidgetAndStyle(row, col++, nodeMinWidthLabel, settingsLabelStyle(NAME_NODE_MIN_WIDTH));

    final InputSpinner nodeMinWidthInput = new InputSpinner(20, 500, 10);
    nodeMinWidthInput.setValue(nodeMinWidth);
    table.setWidgetAndStyle(row, col++, nodeMinWidthInput, settingsInputStyle(NAME_NODE_MIN_WIDTH));

    Label nodeMaxWidthLabel = new Label(NAME_NODE_MAX_WIDTH);
    table.setWidgetAndStyle(row, col++, nodeMaxWidthLabel, settingsLabelStyle(NAME_NODE_MAX_WIDTH));

    final InputSpinner nodeMaxWidthInput = new InputSpinner(20, 500, 10);
    nodeMaxWidthInput.setValue(nodeMaxWidth);
    table.setWidgetAndStyle(row, col++, nodeMaxWidthInput, settingsInputStyle(NAME_NODE_MAX_WIDTH));

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    col = 0;
    Label nodeMinHeightLabel = new Label(NAME_NODE_MIN_HEIGHT);
    table.setWidgetAndStyle(row, col++, nodeMinHeightLabel,
        settingsLabelStyle(NAME_NODE_MIN_HEIGHT));

    final InputSpinner nodeMinHeightInput = new InputSpinner(20, 1000, 10);
    nodeMinHeightInput.setValue(nodeMinHeight);
    table.setWidgetAndStyle(row, col++, nodeMinHeightInput,
        settingsInputStyle(NAME_NODE_MIN_HEIGHT));

    Label nodeMaxHeightLabel = new Label(NAME_NODE_MAX_HEIGHT);
    table.setWidgetAndStyle(row, col++, nodeMaxHeightLabel,
        settingsLabelStyle(NAME_NODE_MAX_HEIGHT));

    final InputSpinner nodeMaxHeightInput = new InputSpinner(20, 1000, 10);
    nodeMaxHeightInput.setValue(nodeMaxHeight);
    table.setWidgetAndStyle(row, col++, nodeMaxHeightInput,
        settingsInputStyle(NAME_NODE_MAX_HEIGHT));

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    col = 0;
    Label nodeGapLabel = new Label(NAME_NODE_GAP);
    table.setWidgetAndStyle(row, col++, nodeGapLabel, settingsLabelStyle(NAME_NODE_GAP));

    final InputSpinner nodeGapInput = new InputSpinner(0, 200, 2);
    nodeGapInput.setValue(nodeGap);
    table.setWidgetAndStyle(row, col++, nodeGapInput, settingsInputStyle(NAME_NODE_GAP));

    Label levelGapLabel = new Label(NAME_LEVEL_GAP);
    table.setWidgetAndStyle(row, col++, levelGapLabel, settingsLabelStyle(NAME_LEVEL_GAP));

    final InputSpinner levelGapInput = new InputSpinner(0, 200, 2);
    levelGapInput.setValue(levelGap);
    table.setWidgetAndStyle(row, col++, levelGapInput, settingsInputStyle(NAME_LEVEL_GAP));

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    Label alignmentLabel = new Label(NAME_ALIGNMENT_IN_LEVEL);
    table.setWidgetAndStyle(row, 0, alignmentLabel, settingsLabelStyle(NAME_ALIGNMENT_IN_LEVEL));

    final RadioGroup alignmentInput = new RadioGroup(Orientation.HORIZONTAL, alignmentInLevel,
        AlignmentInLevel.class);
    table.setWidgetAndStyle(row, 1, alignmentInput, settingsInputStyle(NAME_ALIGNMENT_IN_LEVEL));
    table.getCellFormatter().setColSpan(row, 1, 3);

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    Label lineTypeLabel = new Label(NAME_LINE_TYPE);
    table.setWidgetAndStyle(row, 0, lineTypeLabel, settingsLabelStyle(NAME_LINE_TYPE));

    final RadioGroup lineTypeInput = new RadioGroup(Orientation.HORIZONTAL, lineType,
        LineType.class);
    table.setWidgetAndStyle(row, 1, lineTypeInput, settingsInputStyle(NAME_LINE_TYPE));
    table.getCellFormatter().setColSpan(row, 1, 3);

    row++;
    table.setWidget(row, 0, new CustomDiv(), STYLE_SETTINGS_SEPARATOR);

    row++;
    Flow commands = new Flow();

    Button save = new Button(Localized.dictionary().actionSave());
    save.addStyleName(STYLE_SETTINGS_SAVE);

    save.addClickHandler(event -> {
      boolean changed = false;

      int value = marginLeftInput.getIntValue();
      if (value >= 0 && value != marginLeft) {
        marginLeft = value;
        store(NAME_MARGIN_LEFT, value);
        changed = true;
      }

      value = marginTopInput.getIntValue();
      if (value >= 0 && value != marginTop) {
        marginTop = value;
        store(NAME_MARGIN_TOP, value);
        changed = true;
      }

      value = nodeMinWidthInput.getIntValue();
      if (value > 0 && value != nodeMinWidth) {
        nodeMinWidth = value;
        store(NAME_NODE_MIN_WIDTH, value);
        changed = true;
      }

      value = nodeMaxWidthInput.getIntValue();
      if (value > 0 && value != nodeMaxWidth) {
        nodeMaxWidth = value;
        store(NAME_NODE_MAX_WIDTH, value);
        changed = true;
      }

      value = nodeMinHeightInput.getIntValue();
      if (value > 0 && value != nodeMinHeight) {
        nodeMinHeight = value;
        store(NAME_NODE_MIN_HEIGHT, value);
        changed = true;
      }

      value = nodeMaxHeightInput.getIntValue();
      if (value > 0 && value != nodeMaxHeight) {
        nodeMaxHeight = value;
        store(NAME_NODE_MAX_HEIGHT, value);
        changed = true;
      }

      value = nodeGapInput.getIntValue();
      if (value >= 0 && value != nodeGap) {
        nodeGap = value;
        store(NAME_NODE_GAP, value);
        changed = true;
      }

      value = levelGapInput.getIntValue();
      if (value >= 0 && value != levelGap) {
        levelGap = value;
        store(NAME_LEVEL_GAP, value);
        changed = true;
      }

      value = alignmentInput.getSelectedIndex();
      if (EnumUtils.isOrdinal(AlignmentInLevel.class, value)
          && (alignmentInLevel == null || value != alignmentInLevel.ordinal())) {

        alignmentInLevel = EnumUtils.getEnumByIndex(AlignmentInLevel.class, value);
        store(NAME_ALIGNMENT_IN_LEVEL, value);
        changed = true;
      }

      value = lineTypeInput.getSelectedIndex();
      if (EnumUtils.isOrdinal(LineType.class, value)
          && (lineType == null || value != lineType.ordinal())) {

        lineType = EnumUtils.getEnumByIndex(LineType.class, value);
        store(NAME_LINE_TYPE, value);
        changed = true;
      }

      dialog.close();
      if (changed) {
        redraw();
      }
    });

    commands.add(save);

    Button cancel = new Button(Localized.dictionary().actionCancel());
    cancel.addStyleName(STYLE_SETTINGS_CANCEL);

    cancel.addClickHandler(event -> dialog.close());

    commands.add(cancel);

    table.setWidgetAndStyle(row, 1, commands, STYLE_SETTINGS_COMMAND_PANEL);
    table.getCellFormatter().setColSpan(row, 1, 3);

    dialog.setWidget(table);

    dialog.setAnimationEnabled(true);
    dialog.center();
  }

  private void addCommands(HeaderView header) {
    CheckBox positionToggle = new CheckBox(Localized.dictionary().personPositions());
    positionToggle.addStyleName(STYLE_TOGGLE_POSITIONS);

    positionToggle.setValue(showPositions);

    positionToggle.addValueChangeHandler(event -> {
      showPositions = event.getValue();
      store(NAME_SHOW_POSITIONS, showPositions);

      redraw();
    });

    header.addCommandItem(positionToggle);

    CheckBox employeeToggle = new CheckBox(Localized.dictionary().employees());
    employeeToggle.addStyleName(STYLE_TOGGLE_EMPLOYEES);

    employeeToggle.setValue(showEmployees);

    employeeToggle.addValueChangeHandler(event -> {
      showEmployees = event.getValue();
      store(NAME_SHOW_EMPLOYEES, showEmployees);

      redraw();
    });

    header.addCommandItem(employeeToggle);
  }

  @Override
  public void onUnload(FormView form) {
    EventUtils.clearRegistry(handlerRegistry);
    super.onUnload(form);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  private static boolean isEventRelevant(DataEvent event) {
    for (String viewName : viewNames) {
      if (event.hasView(viewName)) {
        return true;
      }
    }
    return false;
  }

  private Flow getPanel() {
    IdentifiableWidget widget = getFormView().getRootWidget();
    if (widget instanceof Flow) {
      return (Flow) widget;
    } else {
      logger.severe(NameUtils.getName(this), "panel not found");
      return null;
    }
  }

  private TreeLayout<Long> layoutTree(NodeExtentProvider<Long> nodeExtentProvider) {
    DefaultTreeForTreeLayout<Long> tree = new DefaultTreeForTreeLayout<>(ROOT);

    Multimap<Long, Long> children = ArrayListMultimap.create();
    List<Long> parents = new ArrayList<>();

    List<Long> rowIds = departments.getRowIds();
    int parentIndex = departments.getColumnIndex(COL_DEPARTMENT_PARENT);

    for (BeeRow row : departments) {
      Long id = row.getId();
      Long parent = row.getLong(parentIndex);

      if (DataUtils.isId(parent) && !Objects.equals(id, parent) && rowIds.contains(parent)) {
        children.put(parent, id);
      } else {
        tree.addChild(ROOT, id);
        parents.add(id);
      }
    }

    if (!children.isEmpty()) {
      for (int i = 0; i < departments.getNumberOfRows(); i++) {
        List<Long> nodes = new ArrayList<>(parents);
        parents.clear();

        for (Long node : nodes) {
          if (children.containsKey(node)) {
            for (Long child : children.get(node)) {
              tree.addChild(node, child);
              if (children.containsKey(child)) {
                parents.add(child);
              }
            }
          }
        }

        if (parents.isEmpty()) {
          break;
        }
      }
    }

    Configuration<Long> configuration = new DefaultConfiguration<>(levelGap, nodeGap,
        Location.TOP, BeeUtils.nvl(alignmentInLevel, DEFAULT_ALIGNMENT_IN_LEVEL));

    TreeLayout<Long> treeLayout = new TreeLayout<>(tree, nodeExtentProvider, configuration);
    return treeLayout;
  }

  private void refresh() {
    lastRpcId = Queries.getData(viewNames, new RpcCallbackWithId<Collection<BeeRowSet>>() {
      @Override
      public void onSuccess(Collection<BeeRowSet> result) {
        if (getRpcId() >= lastRpcId) {
          for (BeeRowSet rowSet : result) {
            switch (rowSet.getViewName()) {
              case VIEW_DEPARTMENTS:
                departments = rowSet;
                break;

              case VIEW_DEPARTMENT_EMPLOYEES:
                employees = rowSet;
                break;

              case VIEW_DEPARTMENT_POSITIONS:
                positions = rowSet;
                break;
            }
          }

          redraw();

        } else {
          logger.debug(NameUtils.getName(CompanyStructureForm.this), "response", getRpcId(),
              "ignored, waiting for", lastRpcId);
        }
      }
    });
  }

  private void redraw() {
    final Flow panel = getPanel();
    panel.clear();

    if (!DataUtils.isEmpty(departments)) {
      final Map<Long, String> nodeIds = new HashMap<>();

      for (BeeRow department : departments) {
        IdentifiableWidget w = renderDepartment(department);
        if (w != null) {
          panel.add(w);
          nodeIds.put(department.getId(), w.getId());
        }
      }

      if (!nodeIds.isEmpty()) {
        Scheduler.get().scheduleDeferred(() -> {
          NodeExtentProvider<Long> nodeExtentProvider = createNodeExtentProvider(nodeIds);
          TreeLayout<Long> layoutTree = layoutTree(nodeExtentProvider);

          render(panel, nodeIds, layoutTree);
        });
      }
    }
  }

  private NodeExtentProvider<Long> createNodeExtentProvider(final Map<Long, String> nodeIds) {
    NodeExtentProvider<Long> nep = new NodeExtentProvider<Long>() {
      @Override
      public double getWidth(Long treeNode) {
        if (DataUtils.isId(treeNode) && nodeIds.containsKey(treeNode)) {
          if (nodeMinWidth > 0 && nodeMaxWidth == nodeMinWidth) {
            return nodeMinWidth;
          }

          Element element = getNodeElement(treeNode, nodeIds.get(treeNode));
          if (element != null) {
            return BeeUtils.clamp(element.getOffsetWidth() + DomUtils.getScrollBarWidth(),
                nodeMinWidth, nodeMaxWidth);
          }
        }
        return 0;
      }

      @Override
      public double getHeight(Long treeNode) {
        if (DataUtils.isId(treeNode) && nodeIds.containsKey(treeNode)) {
          if (nodeMinHeight > 0 && nodeMinHeight == nodeMaxHeight) {
            return nodeMinHeight;
          }

          Element element = getNodeElement(treeNode, nodeIds.get(treeNode));
          if (element != null) {
            return BeeUtils.clamp(element.getOffsetHeight(), nodeMinHeight, nodeMaxHeight);
          }
        }
        return 0;
      }
    };

    return nep;
  }

  private void render(Flow panel, Map<Long, String> nodeIds, TreeLayout<Long> treeLayout) {
    Map<Long, Rectangle2D.Double> nodeBounds = new HashMap<>(treeLayout.getNodeBounds());

    int shiftX = Math.max(marginLeft, 0);
    int shiftY = Math.max(marginTop, 0) - levelGap;

    if (shiftX != 0 || shiftY != 0) {
      for (Rectangle2D.Double rectangle : nodeBounds.values()) {
        rectangle.setRect(rectangle.getX() + shiftX, rectangle.getY() + shiftY,
            rectangle.getWidth(), rectangle.getHeight());
      }
    }

    Map<Long, Integer> nodeLevels = new HashMap<>();
    Map<Integer, Double> levelTop = new HashMap<>();

    for (Map.Entry<Long, String> entry : nodeIds.entrySet()) {
      Long node = entry.getKey();

      if (nodeBounds.containsKey(node)) {
        Element element = getNodeElement(node, entry.getValue());

        if (element != null) {
          int level = treeLayout.getTree().getLevel(node) - 1;
          Rectangle2D.Double rectangle = nodeBounds.get(node);

          if (level >= 0) {
            addLevelStyle(element, STYLE_DEPARTMENT_PREFIX, level);
            nodeLevels.put(node, level);

            double top = rectangle.getY();
            if (levelTop.containsKey(level)) {
              top = Math.min(top, levelTop.get(level));
            }

            levelTop.put(level, top);
          }

          StyleUtils.setRectangle(element,
              BeeUtils.round(rectangle.getX()), BeeUtils.round(rectangle.getY()),
              BeeUtils.round(rectangle.getWidth()), BeeUtils.round(rectangle.getHeight()));

          StyleUtils.clearProperties(element,
              StyleUtils.STYLE_MIN_WIDTH, StyleUtils.STYLE_MAX_WIDTH,
              StyleUtils.STYLE_MIN_HEIGHT, StyleUtils.STYLE_MAX_HEIGHT);
        }

      } else {
        Widget w = DomUtils.getChild(panel, entry.getValue());
        if (w != null) {
          panel.remove(w);
        }
      }
    }

    if (levelGap >= MIN_LEVEL_GAP_FOR_LINES) {
      boolean bubbles = levelGap >= MIN_LEVEL_GAP_FOR_BUBBLES;

      for (Long parent : nodeBounds.keySet()) {
        if (!ROOT.equals(parent)) {
          List<Long> children = treeLayout.getTree().getChildren(parent);

          if (!BeeUtils.isEmpty(children)) {
            Rectangle2D.Double parentBounds = nodeBounds.get(parent);
            Integer parentLevel = nodeLevels.get(parent);

            if (lineType == LineType.BROKEN) {
              List<Rectangle2D.Double> childBounds = new ArrayList<>();
              Double childTop = null;

              for (Long child : children) {
                Rectangle2D.Double b = nodeBounds.get(child);
                if (b != null) {
                  childBounds.add(b);

                  if (childTop == null && nodeLevels.containsKey(child)) {
                    childTop = levelTop.get(nodeLevels.get(child));
                  }
                }
              }

              if (parentBounds != null && !childBounds.isEmpty() && childTop != null) {
                connect(panel, parentBounds, parentLevel, childBounds, childTop, bubbles);
              }

            } else {
              for (Long child : children) {
                connectStraight(panel, parentBounds, parentLevel, nodeBounds.get(child),
                    STYLE_LINE_STRAIGHT, bubbles);
              }
            }
          }
        }
      }
    }
  }

  private static Element getNodeElement(Long depId, String elId) {
    Element element = DomUtils.getElementQuietly(elId);
    if (element == null) {
      logger.warning("element not found", depId, elId);
    }
    return element;
  }

  private void connect(Flow panel, Rectangle2D.Double parent, Integer parentLevel,
      Collection<Rectangle2D.Double> children, double childTop, boolean bubbles) {

    int middle = BeeUtils.round(childTop - levelGap / 2);

    int x = BeeUtils.round(parent.getCenterX());
    int y1 = BeeUtils.round(parent.getMaxY());

    panel.add(drawVertical(x, y1, middle, parentLevel));
    if (bubbles) {
      panel.add(drawBubble(x, y1, STYLE_BUBBLE_PARENT, parentLevel));
    }

    int x1 = x;
    int x2 = x;

    for (Rectangle2D.Double child : children) {
      x = BeeUtils.round(child.getCenterX());

      x1 = Math.min(x1, x);
      x2 = Math.max(x2, x);

      int y2 = BeeUtils.round(child.getY());
      panel.add(drawVertical(x, middle, y2, parentLevel));

      if (bubbles) {
        panel.add(drawBubble(x, y2, STYLE_BUBBLE_CHILD, parentLevel));
      }
    }

    if (x2 > x1) {
      panel.add(drawHorizontal(x1, x2, middle, parentLevel));
    }
  }

  private static Widget drawBubble(int x, int y, String styleName, Integer level) {
    CustomDiv widget = new CustomDiv(styleName);
    StyleUtils.makeAbsolute(widget);

    StyleUtils.setLeft(widget, x);
    StyleUtils.setTop(widget, y);

    if (level != null) {
      addLevelStyle(widget.getElement(), STYLE_BUBBLE_PREFIX, level);
    }

    return widget;
  }

  private static Widget drawHorizontal(int x1, int x2, int y, Integer level) {
    CustomDiv widget = new CustomDiv(STYLE_LINE_HORIZONTAL);
    StyleUtils.makeAbsolute(widget);

    StyleUtils.setTop(widget, y);
    StyleUtils.setLeft(widget, Math.min(x1, x2));
    StyleUtils.setWidth(widget, Math.abs(x2 - x1));

    if (level != null) {
      addLevelStyle(widget.getElement(), STYLE_LINE_PREFIX, level);
    }

    return widget;
  }

  private static Widget drawVertical(int x, int y1, int y2, Integer level) {
    CustomDiv widget = new CustomDiv(STYLE_LINE_VERTICAL);
    StyleUtils.makeAbsolute(widget);

    StyleUtils.setLeft(widget, x);
    StyleUtils.setTop(widget, Math.min(y1, y2));
    StyleUtils.setHeight(widget, Math.abs(y2 - y1));

    if (level != null) {
      addLevelStyle(widget.getElement(), STYLE_LINE_PREFIX, level);
    }

    return widget;
  }

  private static void connectStraight(Flow panel, Rectangle2D.Double parent, Integer level,
      Rectangle2D.Double child, String styleName, boolean bubbles) {

    if (parent != null && child != null) {
      double x1 = parent.getCenterX();
      double y1 = parent.getMaxY();

      double x2 = child.getCenterX();
      double y2 = child.getY();

      Line line = new Line(x1, y1, x2, y2, styleName);
      if (level != null) {
        addLevelStyle(line.getElement(), STYLE_LINE_PREFIX, level);
      }

      panel.add(line);

      if (bubbles) {
        panel.add(drawBubble(BeeUtils.round(x1), BeeUtils.round(y1), STYLE_BUBBLE_PARENT, level));
        panel.add(drawBubble(BeeUtils.round(x2), BeeUtils.round(y2), STYLE_BUBBLE_CHILD, level));
      }
    }
  }

  private IdentifiableWidget renderDepartment(BeeRow department) {
    final long id = department.getId();

    final Flow panel = new Flow(STYLE_DEPARTMENT_PANEL);
    StyleUtils.makeAbsolute(panel);

    if (nodeMinWidth > 0) {
      if (nodeMinWidth == nodeMaxWidth) {
        StyleUtils.setWidth(panel, nodeMinWidth);
      } else {
        StyleUtils.setMinWidth(panel, nodeMinWidth);
      }
    }
    if (nodeMaxWidth > 0 && nodeMaxWidth > nodeMinWidth) {
      StyleUtils.setMaxWidth(panel, nodeMaxWidth);
    }

    if (nodeMinHeight > 0) {
      if (nodeMinHeight == nodeMaxHeight) {
        StyleUtils.setHeight(panel, nodeMinHeight);
      } else {
        StyleUtils.setMinHeight(panel, nodeMinHeight);
      }
    }
    if (nodeMaxHeight > 0 && nodeMaxHeight > nodeMinHeight) {
      StyleUtils.setMaxHeight(panel, nodeMaxHeight);
    }

    DndDiv label = new DndDiv(STYLE_DEPARTMENT_LABEL);

    String name = DataUtils.getString(departments, department, COL_DEPARTMENT_NAME);
    label.setText(name);

    String fullName = department.getProperty(PROP_DEPARTMENT_FULL_NAME);
    if (!BeeUtils.isEmpty(fullName) && !BeeUtils.equalsTrim(name, fullName)) {
      label.setTitle(fullName);
    }

    label.addClickHandler(event -> RowEditor.open(VIEW_DEPARTMENTS, id, Opener.MODAL));

    DndHelper.makeSource(label, DATA_TYPE_DEPARTMENT, id, STYLE_DEPARTMENT_DRAG);
    panel.add(label);

    Flow content = new Flow(STYLE_DEPARTMENT_CONTENT);

    Long head = DataUtils.getLong(departments, department, COL_DEPARTMENT_HEAD);
    if (DataUtils.isId(head) && !DataUtils.isEmpty(employees)) {
      BeeRow employee = employees.getRowById(head);
      if (employee != null) {
        HtmlTable bossTable = new HtmlTable(STYLE_BOSS_TABLE);
        renderEmployee(bossTable, 0, employee, true, false);
        content.add(bossTable);
      }
    }

    List<BeeRow> depEmployees = filterEmployees(id, head);
    List<DepartmentPosition> depPositions = getDepartmentPositions(id, depEmployees);

    if (showPositions && showEmployees && !depPositions.isEmpty() && !depEmployees.isEmpty()) {
      content.add(renderPositionsAndEmployees(depPositions, depEmployees));

    } else {
      if (showPositions && !depPositions.isEmpty()) {
        content.add(renderPositions(depPositions));
      }

      if (!depEmployees.isEmpty()) {
        content.add(renderEmployees(name, depEmployees, showEmployees, true));
      }
    }

    panel.add(content);

    DndHelper.makeTarget(panel, DND_TYPES, STYLE_DEPARTMENT_DRAG_OVER,
        input -> {
          if (validateDataType(DndHelper.getDataType()) && validateDndContent(input)) {
            return isTarget(DndHelper.getDataType(), (Long) input, id);
          } else {
            return false;
          }
        },
        (t, u) -> {
          panel.removeStyleName(STYLE_DEPARTMENT_DRAG_OVER);

          if (validateDataType(DndHelper.getDataType()) && validateDndContent(u)) {
            acceptDrop(DndHelper.getDataType(), (Long) u, id);
          }
        });

    return panel;
  }

  private static boolean validateDataType(String dataType) {
    return DND_TYPES.contains(dataType);
  }

  private static boolean validateDndContent(Object content) {
    if (content instanceof Long) {
      return DataUtils.isId((Long) content);
    } else {
      return false;
    }
  }

  private BeeRow findEmployee(long depId, long persId) {
    if (DataUtils.isEmpty(employees)) {
      return null;
    }

    int departmentIndex = employees.getColumnIndex(COL_DEPARTMENT);
    int personIndex = employees.getColumnIndex(COL_COMPANY_PERSON);

    for (BeeRow row : employees) {
      if (Objects.equals(row.getLong(departmentIndex), depId)
          && Objects.equals(row.getLong(personIndex), persId)) {
        return row;
      }
    }
    return null;
  }

  private boolean hasEmployee(long depId, long persId) {
    return findEmployee(depId, persId) != null;
  }

  private Long getEmployeeRelation(long emplId, String colName) {
    if (DataUtils.isEmpty(employees)) {
      return null;
    }

    BeeRow row = employees.getRowById(emplId);
    if (row == null) {
      return null;
    } else {
      return DataUtils.getLong(employees, row, colName);
    }
  }

  private Long getDepartmentParent(long depId) {
    if (DataUtils.isEmpty(departments)) {
      return null;
    }

    BeeRow row = departments.getRowById(depId);
    if (row == null) {
      return null;
    } else {
      return DataUtils.getLong(departments, row, COL_DEPARTMENT_PARENT);
    }
  }

  private boolean isTarget(String dataType, long source, long target) {
    switch (dataType) {
      case DATA_TYPE_DEPARTMENT:
        return !Objects.equals(source, target)
            && !Objects.equals(getDepartmentParent(source), target);

      case DATA_TYPE_BOSS:
      case DATA_TYPE_EMPLOYEE:
        Long persId = getEmployeeRelation(source, COL_COMPANY_PERSON);
        return DataUtils.isId(persId) && !hasEmployee(target, persId);

      default:
        return false;
    }
  }

  private void acceptDrop(String dataType, final long source, final long target) {
    UiHelper.closeChildPopups(getFormView().asWidget());

    switch (dataType) {
      case DATA_TYPE_DEPARTMENT:
        Long unbind = null;
        if (!DataUtils.isEmpty(departments)) {
          Long parent = target;

          for (int i = 0; i < departments.getNumberOfRows(); i++) {
            Long p = getDepartmentParent(parent);

            if (p == null) {
              break;
            } else if (Objects.equals(p, source)) {
              unbind = parent;
              break;
            } else {
              parent = p;
            }
          }
        }

        final IntCallback callback = result -> {
          if (BeeUtils.isPositive(result)) {
            fireRefresh(VIEW_DEPARTMENTS);
          }
        };

        if (unbind == null) {
          updateDepartmentParent(source, target, callback);

        } else {
          updateDepartmentParent(unbind, getDepartmentParent(source), result -> {
            if (BeeUtils.isPositive(result)) {
              updateDepartmentParent(source, target, callback);
            }
          });
        }

        break;

      case DATA_TYPE_BOSS:
        Long oldDep = getEmployeeRelation(source, COL_DEPARTMENT);
        Queries.update(VIEW_DEPARTMENTS, oldDep, COL_DEPARTMENT_HEAD, LongValue.getNullValue(),
            result -> {
              if (BeeUtils.isPositive(result)) {
                fireRefresh(VIEW_DEPARTMENTS);
                updateEmployeeDepartment(source, target);
              }
            });

        break;

      case DATA_TYPE_EMPLOYEE:
        updateEmployeeDepartment(source, target);
        break;
    }
  }

  private static void updateDepartmentParent(long depId, Long parent, IntCallback callback) {
    Queries.update(VIEW_DEPARTMENTS, depId, COL_DEPARTMENT_PARENT, new LongValue(parent), callback);
  }

  private static void updateEmployeeDepartment(long emplId, long depId) {
    Queries.update(VIEW_DEPARTMENT_EMPLOYEES, emplId, COL_DEPARTMENT, new LongValue(depId),
        result -> {
          if (BeeUtils.isPositive(result)) {
            fireRefresh(VIEW_DEPARTMENT_EMPLOYEES);
          }
        });
  }

  private List<DepartmentPosition> getDepartmentPositions(Long depId, List<BeeRow> depEmployees) {
    Map<Long, DepartmentPosition> map = new HashMap<>();

    if (!DataUtils.isEmpty(positions)) {
      List<BeeRow> depPositions = DataUtils.filterRows(positions, COL_DEPARTMENT, depId);

      if (!depPositions.isEmpty()) {
        int posIndex = positions.getColumnIndex(COL_POSITION);
        int nameIndex = positions.getColumnIndex(ALS_POSITION_NAME);
        int plannedIndex = positions.getColumnIndex(COL_DEPARTMENT_POSITION_NUMBER_OF_EMPLOYEES);

        for (BeeRow row : depPositions) {
          Long posId = row.getLong(posIndex);
          DepartmentPosition dp = new DepartmentPosition(posId, row.getString(nameIndex));

          Integer planned = row.getInteger(plannedIndex);
          if (BeeUtils.isPositive(planned)) {
            dp.setPlanned(planned);
          }

          map.put(posId, dp);
        }
      }
    }

    if (!BeeUtils.isEmpty(depEmployees)) {
      int posIndex = employees.getColumnIndex(COL_POSITION);
      int nameIndex = employees.getColumnIndex(ALS_POSITION_NAME);

      int ppIndex = employees.getColumnIndex(ALS_PRIMARY_POSITION);
      int ppnIndex = employees.getColumnIndex(ALS_PRIMARY_POSITION_NAME);

      for (BeeRow row : depEmployees) {
        Long posId = row.getLong(posIndex);
        String posName = row.getString(nameIndex);

        if (!DataUtils.isId(posId)) {
          posId = row.getLong(ppIndex);
          posName = row.getString(ppnIndex);
        }

        if (DataUtils.isId(posId)) {
          if (map.containsKey(posId)) {
            map.get(posId).addStaff(row.getId());
          } else {
            DepartmentPosition dp = new DepartmentPosition(posId, posName);
            dp.addStaff(row.getId());
            map.put(posId, dp);
          }
        }
      }
    }

    List<DepartmentPosition> result = new ArrayList<>();
    if (!map.isEmpty()) {
      result.addAll(map.values());
    }

    if (result.size() > 1) {
      Collections.sort(result);
    }
    return result;
  }

  private List<BeeRow> filterEmployees(Long depId, Long exclude) {
    if (DataUtils.isEmpty(employees)) {
      return Collections.emptyList();

    } else {
      List<BeeRow> rows = DataUtils.filterRows(employees, COL_DEPARTMENT, depId);

      if (!rows.isEmpty() && DataUtils.isId(exclude)) {
        int index = BeeConst.UNDEF;
        for (int i = 0; i < rows.size(); i++) {
          if (DataUtils.idEquals(rows.get(i), exclude)) {
            index = i;
            break;
          }
        }

        if (!BeeConst.isUndef(index)) {
          rows.remove(index);
        }
      }
      return rows;
    }
  }

  private void renderEmployee(HtmlTable table, int row, BeeRow employee, boolean boss,
      boolean renderPosition) {

    final long emplId = employee.getId();

    String fullName = BeeUtils.joinWords(
        DataUtils.getString(employees, employee, COL_FIRST_NAME),
        DataUtils.getString(employees, employee, COL_LAST_NAME));

    String positionName = DataUtils.getString(employees, employee, ALS_POSITION_NAME);
    if (BeeUtils.isEmpty(positionName)) {
      positionName = DataUtils.getString(employees, employee, ALS_PRIMARY_POSITION_NAME);
    }

    String companyName = DataUtils.getString(employees, employee, ALS_COMPANY_NAME);

    String photo = DataUtils.getString(employees, employee, COL_PHOTO);
    Flow photoContainer = new Flow();
    String photoUrl = PhotoRenderer.getPhotoUrl(photo);

    Image image = new Image(photoUrl);
    image.setTitle(BeeUtils.buildLines(fullName, positionName, companyName));

    image.addClickHandler(event -> {
      Long person = getEmployeeRelation(emplId, COL_PERSON);
      RowEditor.open(VIEW_PERSONS, person, Opener.MODAL, result -> refresh());
    });

    photoContainer.add(image);
    String styleName = STYLE_PHOTO_CONTAINER;

    table.setWidgetAndStyle(row, 0, photoContainer, styleName);

    DndDiv label = new DndDiv();
    label.setText(fullName);

    label.addClickHandler(event -> {
      Long cp = getEmployeeRelation(emplId, COL_COMPANY_PERSON);
      RowEditor.open(VIEW_COMPANY_PERSONS, cp, Opener.MODAL, result -> refresh());
    });

    styleName = boss ? STYLE_BOSS_LABEL : STYLE_EMPLOYEE_LABEL;
    table.setWidgetAndStyle(row, 1, label, styleName);

    if (renderPosition && !BeeUtils.isEmpty(positionName)) {
      table.setWidgetAndStyle(row, 2, new Label(positionName), STYLE_EMPLOYEE_POSITION);
    }

    if (boss) {
      if (!BeeUtils.isEmpty(photo)) {
        DndHelper.makeSource(photoContainer, DATA_TYPE_BOSS, emplId, null);
      }
      DndHelper.makeSource(label, DATA_TYPE_BOSS, emplId, STYLE_BOSS_DRAG);

    } else {
      if (!BeeUtils.isEmpty(photo)) {
        DndHelper.makeSource(photoContainer, DATA_TYPE_EMPLOYEE, emplId, null);
      }
      DndHelper.makeSource(label, DATA_TYPE_EMPLOYEE, emplId, STYLE_EMPLOYEE_DRAG);
    }
  }

  private Widget renderPositionsAndEmployees(List<DepartmentPosition> depPositions,
      List<BeeRow> depEmployees) {

    HtmlTable table = new HtmlTable(STYLE_POSITIONS_AND_EMPLOYEES);
    int row = 0;

    int colLabel = 0;
    int colActual = 1;
    int colPlanned = 2;

    Set<Long> havePositions = new HashSet<>();
    for (DepartmentPosition dp : depPositions) {
      if (!dp.staff.isEmpty()) {
        havePositions.addAll(dp.staff);
      }
    }

    int missing = depEmployees.size() - havePositions.size();
    if (missing > 0) {
      table.setWidgetAndStyle(row, colActual, new Badge(missing), STYLE_POSITION_ACTUAL);
      row++;

      List<BeeRow> noPositions = new ArrayList<>();
      for (BeeRow employee : depEmployees) {
        if (!havePositions.contains(employee.getId())) {
          noPositions.add(employee);
        }
      }

      if (!noPositions.isEmpty()) {
        table.setWidget(row, 0, renderEmployees(null, noPositions, true, false));
        row++;
      }
    }

    for (DepartmentPosition dp : depPositions) {
      Label label = new Label(dp.name);
      table.setWidgetAndStyle(row, colLabel, label, STYLE_POSITION_LABEL);

      table.setWidgetAndStyle(row, colActual, new Badge(dp.staff.size()), STYLE_POSITION_ACTUAL);
      table.setWidgetAndStyle(row, colPlanned, new Badge(dp.planned), dp.getStyleName());
      row++;

      if (!dp.staff.isEmpty()) {
        List<BeeRow> posEmployees = DataUtils.filterRows(depEmployees, dp.staff);
        if (!posEmployees.isEmpty()) {
          table.setWidget(row, 0, renderEmployees(null, posEmployees, true, false));
          row++;
        }
      }
    }

    return table;
  }

  private Widget renderPositions(List<DepartmentPosition> depPositions) {
    HtmlTable table = new HtmlTable(STYLE_POSITION_TABLE);
    int row = 0;

    int colLabel = 0;
    int colActual = 1;
    int colPlanned = 2;

    for (DepartmentPosition dp : depPositions) {
      Label label = new Label(dp.name);

      if (!dp.staff.isEmpty()) {
        label.addStyleName(STYLE_POSITION_STAFFED);
        DomUtils.setDataProperty(label.getElement(), KEY_STAFF, DataUtils.buildIdList(dp.staff));

        label.addClickHandler(event -> {
          Element target = EventUtils.getEventTargetElement(event);
          String idList = DomUtils.getDataProperty(target, KEY_STAFF);
          List<Long> emplIds = DataUtils.parseIdList(idList);
          List<BeeRow> posEmployees = DataUtils.filterRows(employees.getRows(), emplIds);

          if (!posEmployees.isEmpty()) {
            String caption = target.getInnerText();
            Widget widget = renderEmployees(caption, posEmployees, true, false);

            Flow content = new Flow(STYLE_POPUP_CONTENT);
            content.add(widget);

            Global.showModalWidget(caption, content, target);
          }
        });
      }

      table.setWidgetAndStyle(row, colLabel, label, STYLE_POSITION_LABEL);

      table.setWidgetAndStyle(row, colActual, new Badge(dp.staff.size()), STYLE_POSITION_ACTUAL);
      table.setWidgetAndStyle(row, colPlanned, new Badge(dp.planned), dp.getStyleName());

      row++;
    }

    return table;
  }

  private Widget renderEmployees(final String caption, final List<BeeRow> depEmployees,
      boolean details, boolean renderPosition) {

    HtmlTable table = new HtmlTable(STYLE_EMPLOYEE_TABLE);
    int row = 0;

    if (details) {
      table.addStyleName(STYLE_EMPLOYEE_DETAILS);

      for (BeeRow employee : depEmployees) {
        renderEmployee(table, row, employee, false, renderPosition);
        row++;
      }

    } else {
      table.addStyleName(STYLE_EMPLOYEE_SUMMARY);

      Label label = new Label(Localized.dictionary().employees());

      label.addClickHandler(event -> {
        Widget widget = renderEmployees(caption, depEmployees, true, true);
        Flow content = new Flow(STYLE_POPUP_CONTENT);
        content.add(widget);
        Global.showModalWidget(caption, content, EventUtils.getEventTargetElement(event));
      });

      table.setWidgetAndStyle(row, 0, label, STYLE_EMPLOYEE_LABEL);
      table.setWidgetAndStyle(row, 1, new Badge(depEmployees.size()), STYLE_EMPLOYEE_COUNT);
    }

    return table;
  }

  private static void fireRefresh(String viewName) {
    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);
  }
}
