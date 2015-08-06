package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.edit.SimpleEditorHandler;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TaskDialog extends DialogBox {

  private static final String STYLE_DIALOG = CRM_STYLE_PREFIX + "taskDialog";
  private static final String STYLE_CELL = "Cell";

  TaskDialog(String caption) {
    super(caption, STYLE_DIALOG);
    addDefaultCloseBox();

    HtmlTable container = new HtmlTable();
    container.addStyleName(STYLE_DIALOG + "-container");

    setWidget(container);
  }

  void addAction(String caption, final ScheduledCommand command) {
    String styleName = STYLE_DIALOG + "-action";

    Button button = new Button(caption, command);
    button.addStyleName(styleName);

    FaLabel faSave = new FaLabel(FontAwesome.SAVE);
    faSave.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        command.execute();
      }
    });

    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    faSave.setTitle(Action.SAVE.getCaption());

    insertAction(BeeConst.INT_TRUE, faSave);

    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.CENTER);

    table.getCellFormatter().setColSpan(row, col, 2);
  }

  String addComment(boolean required) {
    String styleName = STYLE_DIALOG + "-commentLabel";
    Label label = new Label(Localized.getConstants().crmTaskComment());
    label.addStyleName(styleName);
    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    InputArea input = new InputArea();
    styleName = STYLE_DIALOG + "-commentArea";
    input.addStyleName(styleName);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return input.getId();
  }

  String addDateTime(String caption, boolean required, DateTime def) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-dateLabel";
    Label label = new Label(caption);
    label.addStyleName(styleName);
    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-dateInput";
    InputDateTime input = new InputDateTime();
    input.addStyleName(styleName);

    if (def != null) {
      input.setDateTime(def);
    }

    SimpleEditorHandler.observe(caption, input);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return input.getId();
  }

  Map<String, String> addDuration() {
    Map<String, String> result = new HashMap<>();

    result.put(COL_DURATION, addTime(Localized.getConstants().crmSpentTime()));
    result.put(COL_DURATION_TYPE, addSelector(Localized.getConstants().crmDurationType(),
        VIEW_DURATION_TYPES, Lists.newArrayList(COL_DURATION_TYPE_NAME), false, null, null));
    result.put(COL_DURATION_DATE, addDateTime(Localized.getConstants().crmTaskFinishDate(),
        false, TimeUtils.nowMinutes()));

    return result;
  }

  String addFileCollector() {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-filesLabel";
    Label label = new Label(Localized.getConstants().files());
    label.addStyleName(styleName);

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-fileCollector";
    FileCollector collector = new FileCollector(new Image(Global.getImages().attachment()));
    collector.addStyleName(styleName);

    table.setWidget(row, col, collector);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    Widget panel = getWidget();
    if (panel instanceof DndTarget) {
      collector.bindDnd((DndTarget) panel);
    }

    return collector.getId();
  }

  String addCheckBox(boolean checked) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-observerCheckbox";
    CheckBox chkBx = new CheckBox(Localized.getConstants().crmTaskAddSenderToObservers());
    chkBx.setChecked(checked);
    chkBx.addStyleName(styleName);

    table.setWidget(row, col, chkBx);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return chkBx.getId();
  }

  String addSelector(String caption, String relView, List<String> relColumns,
      boolean required, Collection<Long> exclusions, Collection<Long> filter) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-selectorLabel";
    Label label = new Label(caption);
    label.addStyleName(styleName);
    if (required) {
      label.addStyleName(StyleUtils.NAME_REQUIRED);
    }

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-selectorInput";
    UnboundSelector selector = UnboundSelector.create(relView, relColumns);
    selector.addStyleName(styleName);

    if (!BeeUtils.isEmpty(exclusions)) {
      selector.getOracle().setExclusions(exclusions);
    }

    if (!BeeUtils.isEmpty(filter)) {
      selector.getOracle().setAdditionalFilter(Filter.idIn(filter), true);
    } else {
      selector.getOracle().setAdditionalFilter(null, true);
    }

    table.setWidget(row, col, selector);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return selector.getId();
  }

  void display() {
    focusOnOpen(getContent());
    center();
  }

  void display(String focusId) {
    focusOnOpen(getChild(focusId));
    center();
  }

  String getComment(String id) {
    Widget child = getChild(id);
    if (child instanceof InputArea) {
      return ((InputArea) child).getValue();
    } else {
      return null;
    }
  }

  DateTime getDateTime(String id) {
    Widget child = getChild(id);
    if (child instanceof InputDateTime) {
      return ((InputDateTime) child).getDateTime();
    } else {
      return null;
    }
  }

  List<FileInfo> getFiles(String id) {
    Widget child = getChild(id);
    if (child instanceof FileCollector) {
      return ((FileCollector) child).getFiles();
    } else {
      return new ArrayList<>();
    }
  }

  DataSelector getSelector(String id) {
    Widget child = getChild(id);
    if (child instanceof DataSelector) {
      return (DataSelector) child;
    } else {
      return null;
    }
  }

  String getTime(String id) {
    Widget child = getChild(id);
    if (child instanceof InputTime) {
      return ((InputTime) child).getValue();
    } else {
      return null;
    }
  }

  boolean isChecked(String id) {
    Widget child = getChild(id);
    if (child instanceof CheckBox) {
      return ((CheckBox) child).isChecked();
    } else {
      return false;
    }
  }

  private String addTime(String caption) {
    HtmlTable table = getContainer();
    int row = table.getRowCount();
    int col = 0;

    String styleName = STYLE_DIALOG + "-timeLabel";
    Label label = new Label(caption);
    label.addStyleName(styleName);

    table.setWidget(row, col, label);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
    col++;

    styleName = STYLE_DIALOG + "-timeInput";
    InputTime input = new InputTime();
    input.addStyleName(styleName);

    SimpleEditorHandler.observe(caption, input);

    table.setWidget(row, col, input);
    table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

    return input.getId();
  }

  private Widget getChild(String id) {
    return DomUtils.getChildQuietly(getContent(), id);
  }

  private HtmlTable getContainer() {
    return (HtmlTable) getContent();
  }
}
