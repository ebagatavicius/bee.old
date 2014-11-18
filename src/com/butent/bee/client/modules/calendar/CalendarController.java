package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InputColor;
import com.butent.bee.client.widget.SimpleCheckBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class CalendarController extends Flow implements HandlesStateChange, HasCaption, HasDomain {

  private enum UcaColumn {
    COLOR("color") {
      @Override
      Widget create(BeeRow row) {
        CustomDiv widget = new CustomDiv();

        String bg = BeeUtils.notEmpty(row.getString(bgIndex), row.getString(attBgIndex));
        if (!BeeUtils.isEmpty(bg)) {
          widget.getElement().getStyle().setBackgroundColor(bg.trim());
        }

        return widget;
      }
    },

    CAPTION("caption") {
      @Override
      Widget create(BeeRow row) {
        return new Label(BeeUtils.notEmpty(row.getString(captionIndex),
            row.getString(nameIndex)));
      }
    },

    UP("up") {
      @Override
      Widget create(BeeRow row) {
        if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)) {
          return new FaLabel(FontAwesome.ARROW_UP);
        } else {
          return null;
        }
      }
    },

    DOWN("down") {
      @Override
      Widget create(BeeRow row) {
        if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)) {
          return new FaLabel(FontAwesome.ARROW_DOWN);
        } else {
          return null;
        }
      }
    },

    ENABLE("enable") {
      @Override
      Widget create(BeeRow row) {
        if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)) {
          return new SimpleCheckBox();
        } else {
          return null;
        }
      }
    },

    REMOVE("remove") {
      @Override
      Widget create(BeeRow row) {
        if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)
            && BeeKeeper.getUser().canDeleteData(VIEW_USER_CAL_ATTENDEES)) {
          FaLabel widget = new FaLabel(FontAwesome.TRASH_O);
          widget.setTitle(Localized.getConstants().actionRemove());
          return widget;

        } else {
          return null;
        }
      }
    };

    private final String label;

    private UcaColumn(String label) {
      this.label = label;
    }

    abstract Widget create(BeeRow row);

    private String getLabel() {
      return label;
    }
  }

  private static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX + "cal-Controller";
  private static final String STYLE_PREFIX = STYLE_CONTAINER + "-";

  private static final String STYLE_DISCLOSURE = STYLE_PREFIX + "disclosure";
  private static final String STYLE_DATE_PICKER = STYLE_PREFIX + "datePicker";

  private static final String STYLE_TABLE_PANEL = STYLE_PREFIX + "tablePanel";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_ROW = STYLE_PREFIX + "row";

  private static final String STYLE_SELECTOR = STYLE_PREFIX + "selector";

  private static final String STYLE_COLOR_PICKER = STYLE_PREFIX + "colorPicker";

  private static final String STYLE_SUFFIX_ENABLED = "-enabled";
  private static final String STYLE_SUFFIX_DISABLED = "-disabled";

  private static final String STYLE_SUFFIX_OPEN = "-open";
  private static final String STYLE_SUFFIX_CLOSED = "-closed";

  private static int ucIndex;
  private static int attIndex;

  private static int nameIndex;
  private static int ordinalIndex;
  private static int captionIndex;
  private static int enabledIndex;

  private static int attBgIndex;
  private static int bgIndex;

  private static boolean initialized;

  private static void initialize(BeeRowSet rowSet) {
    ucIndex = rowSet.getColumnIndex(COL_USER_CALENDAR);
    attIndex = rowSet.getColumnIndex(COL_ATTENDEE);

    nameIndex = rowSet.getColumnIndex(ALS_ATTENDEE_NAME);
    ordinalIndex = rowSet.getColumnIndex(COL_ORDINAL);
    captionIndex = rowSet.getColumnIndex(COL_CAPTION);
    enabledIndex = rowSet.getColumnIndex(COL_ENABLED);

    attBgIndex = rowSet.getColumnIndex(ALS_ATTENDEE_BACKGROUND);
    bgIndex = rowSet.getColumnIndex(AdministrationConstants.COL_BACKGROUND);

    initialized = true;
  }

  private final long calendarId;
  private final long ucId;

  private final String caption;

  private final BeeRowSet ucAttendees;

  private final Image disclosureOpen = new Image(Global.getImages().disclosureOpen());
  private final Image disclosureClosed = new Image(Global.getImages().disclosureClosed());

  private final DatePicker datePicker = new DatePicker(TimeUtils.today(), MIN_DATE, MAX_DATE);

  private final HtmlTable table = new HtmlTable();
  private final List<Long> ucaIds = new ArrayList<>();

  private final InputColor colorPicker = new InputColor();

  private final DataSelector attSelector;

  private Long activeRowId;

  CalendarController(long calendarId, long ucId, String caption, BeeRowSet ucAttendees) {
    super();

    this.calendarId = calendarId;
    this.ucId = ucId;
    this.caption = caption;
    this.ucAttendees = Assert.notNull(ucAttendees, "CalendarController: attendees is null");

    addStyleName(STYLE_CONTAINER);

    if (!initialized) {
      initialize(ucAttendees);
    }

    this.attSelector = createSelector();

    createUi();
    setDatePickerOpen(true);

    setExclusions();
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public Domain getDomain() {
    return Domain.CALENDAR;
  }

  @Override
  public String getIdPrefix() {
    return "cc";
  }

  @Override
  public void onStateChange(State state) {
    if (State.ACTIVATED.equals(state)) {
      CalendarKeeper.onActivateController(calendarId);
    } else if (State.REMOVED.equals(state)) {
      CalendarKeeper.onRemoveController(calendarId);
    }
  }

  String getAttendeeCaption(long attId) {
    BeeRow row = getAttendeeRow(attId);
    return (row == null) ? null : getCaption(row);
  }

  Map<Long, String> getAttendeeColors() {
    Map<Long, String> colors = new HashMap<>();
    for (BeeRow row : ucAttendees.getRows()) {
      String color = getColor(row);
      if (!BeeUtils.isEmpty(color)) {
        colors.put(row.getLong(attIndex), color);
      }
    }
    return colors;
  }

  List<Long> getAttendees() {
    List<Long> result = new ArrayList<>();
    if (ucAttendees.isEmpty()) {
      return result;
    }

    for (BeeRow row : ucAttendees.getRows()) {
      if (isEnabled(row)) {
        result.add(row.getLong(attIndex));
      }
    }

    if (result.isEmpty()) {
      result.add(ucAttendees.getRow(0).getLong(attIndex));
    }
    return result;
  }

  void setDate(JustDate date) {
    datePicker.setDate(date, false);
  }

  private void addAttendee(BeeRow attRow) {
    BeeRow newRow = DataUtils.createEmptyRow(ucAttendees.getNumberOfColumns());

    newRow.setValue(ucIndex, ucId);
    newRow.setValue(attIndex, attRow.getId());

    newRow.setValue(enabledIndex, true);

    int ord = -1;
    for (BeeRow row : ucAttendees.getRows()) {
      ord = Math.max(ord, row.getInteger(ordinalIndex));
    }
    newRow.setValue(ordinalIndex, ord + 1);

    Queries.insert(ucAttendees.getViewName(), ucAttendees.getColumns(), newRow, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        ucAttendees.addRow(result);
        addRow(result);

        setExclusions();
        postUpdate();
      }
    });
  }

  private void addRow(BeeRow row) {
    int r = table.getRowCount();
    table.insertRow(r);
    table.getRowFormatter().addStyleName(r, STYLE_PREFIX + STYLE_ROW);

    boolean enabled = isEnabled(row);
    table.getRowFormatter().addStyleName(r, STYLE_CONTAINER
        + (enabled ? STYLE_SUFFIX_ENABLED : STYLE_SUFFIX_DISABLED));

    final long rowId = row.getId();
    ucaIds.add(rowId);

    int c = 0;
    for (UcaColumn column : UcaColumn.values()) {
      Widget widget = column.create(row);

      if (widget != null) {
        switch (column) {
          case COLOR:
            if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)) {
              Binder.addClickHandler(widget, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  setActiveRowId(rowId);
                  colorPicker.setColor(getColor(getRow(rowId)));
                  colorPicker.click();
                }
              });
            }
            break;

          case CAPTION:
            if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)) {
              ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  changeCaption(rowId);
                }
              });
            }
            break;

          case UP:
            ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                move(rowId, true);
              }
            });
            break;

          case DOWN:
            ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                move(rowId, false);
              }
            });
            break;

          case ENABLE:
            if (widget instanceof SimpleCheckBox) {
              final SimpleCheckBox cb = (SimpleCheckBox) widget;
              if (enabled) {
                cb.setValue(true);
              }

              cb.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  setEnabled(rowId, cb.getValue());
                }
              });
            }
            break;

          case REMOVE:
            ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                delete(rowId);
              }
            });
            break;
        }

        table.setWidgetAndStyle(r, c, widget, STYLE_PREFIX + column.getLabel());
      }

      c++;
    }
  }

  private void changeCaption(final long rowId) {
    final BeeRow row = getRow(rowId);

    Global.inputString(Localized.getConstants().calName(), null, new StringCallback(false) {
      @Override
      public void onSuccess(String value) {
        String name = row.getString(nameIndex);
        String oldCaption = row.getString(captionIndex);

        if (BeeUtils.equalsTrim(value, oldCaption)) {
          return;
        }
        if (BeeUtils.isEmpty(oldCaption) && BeeUtils.equalsTrim(value, name)) {
          return;
        }

        int index = ucaIds.indexOf(rowId);
        if (index >= 0) {
          updateCell(rowId, COL_CAPTION, new TextValue(value));

          row.setValue(captionIndex, value);
          Widget widget = table.getWidget(index, UcaColumn.CAPTION.ordinal());
          if (widget instanceof HasHtml) {
            ((HasHtml) widget).setHtml(BeeUtils.trim(value));
          }

          postUpdate();
        }
      }
    }, getCaption(row));
  }

  private static DataSelector createSelector() {
    if (Data.isViewEditable(VIEW_USER_CAL_ATTENDEES)
        && BeeKeeper.getUser().canCreateData(VIEW_USER_CAL_ATTENDEES)) {

      Relation relation = Relation.create(VIEW_ATTENDEES,
          Lists.newArrayList(COL_ATTENDEE_NAME, ALS_ATTENDEE_TYPE_NAME));

      relation.disableEdit();
      relation.disableNewRow();

      DataSelector dataSelector = new DataSelector(relation, true);
      dataSelector.setEditing(true);
      DomUtils.setPlaceholder(dataSelector, Localized.getConstants().actionAppend());

      return dataSelector;

    } else {
      return null;
    }
  }

  private void createUi() {
    disclosureOpen.addStyleName(STYLE_DISCLOSURE);
    disclosureOpen.addStyleName(STYLE_DISCLOSURE + STYLE_SUFFIX_OPEN);
    disclosureOpen.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setDatePickerOpen(false);
      }
    });
    add(disclosureOpen);

    disclosureClosed.addStyleName(STYLE_DISCLOSURE);
    disclosureClosed.addStyleName(STYLE_DISCLOSURE + STYLE_SUFFIX_CLOSED);
    disclosureClosed.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setDatePickerOpen(true);
      }
    });
    add(disclosureClosed);

    datePicker.addStyleName(STYLE_DATE_PICKER);
    datePicker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      @Override
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        CalendarKeeper.synchronizeDate(calendarId, event.getValue(), true);
      }
    });
    add(datePicker);

    table.addStyleName(STYLE_TABLE);
    for (BeeRow row : ucAttendees.getRows()) {
      addRow(row);
    }

    Flow panel = new Flow();
    panel.addStyleName(STYLE_TABLE_PANEL);
    panel.add(table);
    add(panel);

    if (attSelector != null) {
      attSelector.addStyleName(STYLE_SELECTOR);
      attSelector.addEditStopHandler(new EditStopEvent.Handler() {
        @Override
        public void onEditStop(EditStopEvent event) {
          if (event.isChanged() && attSelector.getRelatedRow() != null) {
            addAttendee(attSelector.getRelatedRow());
            attSelector.clearValue();
          }
        }
      });
      add(attSelector);
    }

    colorPicker.addStyleName(STYLE_COLOR_PICKER);
    colorPicker.addColorChangeHandler(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        updateColor(getActiveRowId(), colorPicker.getValue());
      }
    });
    add(colorPicker);
  }

  private void delete(final long rowId) {
    final BeeRow row = getRow(rowId);
    if (row == null) {
      return;
    }

    Global.confirmRemove(getCaption(), getCaption(row), new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        int index = ucaIds.indexOf(rowId);

        if (index >= 0) {
          Queries.deleteRow(VIEW_USER_CAL_ATTENDEES, rowId, row.getVersion());

          ucAttendees.removeRowById(rowId);
          ucaIds.remove(index);
          table.removeRow(index);

          setExclusions();

          postUpdate();
        }
      }
    });
  }

  private Long getActiveRowId() {
    return activeRowId;
  }

  private BeeRow getAttendeeRow(long attId) {
    for (BeeRow row : ucAttendees.getRows()) {
      Long value = row.getLong(attIndex);
      if (value != null && value == attId) {
        return row;
      }
    }
    return null;
  }

  private static String getCaption(BeeRow row) {
    return BeeUtils.notEmpty(row.getString(captionIndex), row.getString(nameIndex));
  }

  private static String getColor(BeeRow row) {
    if (row == null) {
      return null;
    } else {
      return BeeUtils.notEmpty(row.getString(bgIndex), row.getString(attBgIndex));
    }
  }

  private BeeRow getRow(long rowId) {
    return ucAttendees.getRowById(rowId);
  }

  private static boolean isEnabled(BeeRow row) {
    return BeeUtils.isTrue(row.getBoolean(enabledIndex));
  }

  private void move(long rowId, boolean up) {
    int srcIdx = ucaIds.indexOf(rowId);
    if (srcIdx < 0) {
      return;
    }

    if (up && srcIdx == 0 || !up && srcIdx >= ucAttendees.getNumberOfRows() - 1) {
      return;
    }

    int dstIdx = srcIdx + (up ? -1 : 1);

    List<BeeRow> rows = new ArrayList<>(ucAttendees.getRows());

    int srcOrd = rows.get(srcIdx).getInteger(ordinalIndex);
    int dstOrd = rows.get(dstIdx).getInteger(ordinalIndex);

    rows.get(srcIdx).setValue(ordinalIndex, dstOrd);
    rows.get(dstIdx).setValue(ordinalIndex, srcOrd);

    updateCell(rows.get(srcIdx).getId(), COL_ORDINAL, new IntegerValue(dstOrd));
    updateCell(rows.get(dstIdx).getId(), COL_ORDINAL, new IntegerValue(srcOrd));

    ucAttendees.clearRows();
    for (int i = 0; i < Math.min(srcIdx, dstIdx); i++) {
      ucAttendees.addRow(rows.get(i));
    }

    ucAttendees.addRow(rows.get(Math.max(srcIdx, dstIdx)));
    ucAttendees.addRow(rows.get(Math.min(srcIdx, dstIdx)));

    for (int i = Math.max(srcIdx, dstIdx) + 1; i < rows.size(); i++) {
      ucAttendees.addRow(rows.get(i));
    }

    ucaIds.clear();

    table.clear();
    for (BeeRow row : ucAttendees.getRows()) {
      addRow(row);
    }

    postUpdate();
  }

  private void postUpdate() {
    CalendarKeeper.updatePanels(calendarId, ucAttendees);
  }

  private void setActiveRowId(Long activeRowId) {
    this.activeRowId = activeRowId;
  }

  private void setDatePickerOpen(boolean open) {
    setStyleName(STYLE_CONTAINER + STYLE_SUFFIX_OPEN, open);
    setStyleName(STYLE_CONTAINER + STYLE_SUFFIX_CLOSED, !open);
  }

  private void setEnabled(long rowId, Boolean enabled) {
    int index = ucaIds.indexOf(rowId);

    if (index >= 0) {
      updateCell(rowId, COL_ENABLED, BooleanValue.of(enabled));
      getRow(rowId).setValue(enabledIndex, enabled);

      table.getRowFormatter().removeStyleName(index, STYLE_CONTAINER
          + (enabled ? STYLE_SUFFIX_DISABLED : STYLE_SUFFIX_ENABLED));
      table.getRowFormatter().addStyleName(index, STYLE_CONTAINER
          + (enabled ? STYLE_SUFFIX_ENABLED : STYLE_SUFFIX_DISABLED));

      postUpdate();
    }
  }

  private void setExclusions() {
    if (attSelector != null) {
      Set<Long> attIds = new HashSet<>();
      for (BeeRow row : ucAttendees.getRows()) {
        attIds.add(row.getLong(attIndex));
      }

      attSelector.getOracle().setExclusions(attIds);
    }
  }

  private static void updateCell(long rowId, String columnId, Value value) {
    Queries.update(VIEW_USER_CAL_ATTENDEES, Filter.compareId(rowId), columnId, value, null);
  }

  private void updateColor(Long rowId, String value) {
    if (rowId == null || BeeUtils.isEmpty(value)) {
      return;
    }

    BeeRow row = getRow(rowId);
    if (row == null) {
      return;
    }

    int index = ucaIds.indexOf(rowId);
    if (index < 0) {
      return;
    }

    updateCell(rowId, AdministrationConstants.COL_BACKGROUND, new TextValue(value));
    row.setValue(bgIndex, value);

    Widget widget = table.getWidget(index, UcaColumn.COLOR.ordinal());
    if (widget != null) {
      widget.getElement().getStyle().setBackgroundColor(value);
    }

    postUpdate();
  }
}
