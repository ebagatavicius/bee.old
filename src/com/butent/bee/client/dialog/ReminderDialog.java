package com.butent.bee.client.dialog;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReminderDialog extends DialogBox {
  private static final String STYLE_DIALOG = BeeConst.CSS_CLASS_PREFIX + "reminder-dialog";
  private static final String STYLE_DIALOG_INFO_PANEL = STYLE_DIALOG + "-infoPanel";
  private static final String STYLE_DIALOG_INFO_TEXT = STYLE_DIALOG + "-infoText";
  private static final String STYLE_DIALOG_DATE_TEXT = STYLE_DIALOG + "-dateText";
  private static final String STYLE_DIALOG_PANEL = STYLE_DIALOG + "-panel";
  private static final String STYLE_DIALOG_SUSPEND_BUTTON = STYLE_DIALOG + "-suspendButton";
  private static final String STYLE_DIALOG_REMIND_BUTTON = STYLE_DIALOG + "-remindButton";
  private static final String STYLE_ELEMENT_NOT_VISIBLE = STYLE_DIALOG + "-not-visible";
  private static final String STYLE_DIALOG_TEXT = STYLE_DIALOG + "-text";
  private static final String STYLE_DIALOG_COMPONENT = STYLE_DIALOG + "-component";
  private static final String REMINDER_ACTIVE = "bee-reminder-dialog-active";

  private BeeRow reminderDataRow;
  private Map<Integer, DateTime> datesByField;
  private TextLabel dateTextLabel;
  private InputDateTime dateTimeInput;
  private Filter flt;
  private Module module;
  private long objectId;
  private FaLabel reminderLabel;
  private long userId;
  private UnboundSelector selector;

  public ReminderDialog(Module module, long remindForId, long userId) {
    super(Localized.dictionary().userReminder(), null);

    this.module = module;
    this.objectId = remindForId;
    this.userId = userId;

    flt = Filter.and(Filter.equals(COL_USER_REMINDER_OBJECT, remindForId),
        Filter.equals(COL_USER_REMINDER_USER, userId),
        Filter.equals(COL_USER_REMINDER_OBJECT_MODULE, module.ordinal()));

    addDefaultCloseBox();

    reminderLabel = generateReminderLabel();

    super.addCloseHandler(event -> generateReminderLabel());
  }

  public FaLabel getReminderLabel() {
    return reminderLabel;
  }

  public FaLabel generateReminderLabel() {
    if (reminderLabel == null) {
      reminderLabel = new FaLabel(FontAwesome.BELL_O);
      reminderLabel.setTitle(Localized.dictionary().userReminder());
    }

    Queries.getRowSet(VIEW_USER_REMINDERS, null, flt, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          return;
        }

        BeeRow dataRow = result.getRow(0);
        if (dataRow != null && isActive(dataRow) && !isTimeout(dataRow)) {
          reminderLabel.addStyleName(REMINDER_ACTIVE);
        } else {
          reminderLabel.removeStyleName(REMINDER_ACTIVE);
        }
      }
    });

    return reminderLabel;
  }

  public void showDialog(Map<Integer, DateTime> datesByFieldType) {
    this.datesByField = datesByFieldType;

    Queries.getRowSet(VIEW_USER_REMINDERS, null, flt, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          reminderDataRow = null;
        } else {
          reminderDataRow = result.getRow(0);
        }
        setWidget(generateReminderWidget());
        display();
      }
    });

  }

  private Flow generateReminderWidget() {
    Flow mainPanel = new Flow();

    Flow selectionPanel = new Flow(STYLE_DIALOG_PANEL);
    selectionPanel.add(createTextLabelWidget(Localized.dictionary().userReminderSendRemind(),
        STYLE_DIALOG_TEXT));

    Long dataTypeId = reminderDataRow == null ? null : reminderDataRow.getLong(
        Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TYPE));

    createSelectorWidget(dataTypeId);
    selectionPanel.add(selector);

    createDateTimeWidget(dataTypeId);
    selectionPanel.add(dateTimeInput);

    mainPanel.add(selectionPanel);

    Flow infoPanel = new Flow(STYLE_DIALOG_INFO_PANEL);
    infoPanel.add(createTextLabelWidget(Localized.dictionary().userReminderDataLabel(),
        STYLE_DIALOG_INFO_TEXT));

    dateTextLabel = new TextLabel(true);
    dateTextLabel.addStyleName(STYLE_DIALOG_DATE_TEXT);
    infoPanel.add(dateTextLabel);

    mainPanel.add(infoPanel);

    calculateReminderTime(selector.getRelatedRow(),
        reminderDataRow != null ? reminderDataRow.getDateTime(
            Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TIME)) : null);

    Flow buttonsPanel = new Flow(STYLE_DIALOG_PANEL);

    if (reminderDataRow == null) {
      buttonsPanel.add(createReminderButton());
    } else {
      buttonsPanel.add(createUpdateButton());
    }

    boolean active = isActive(reminderDataRow);

    if (active) {
      buttonsPanel.add(createSuspendReminderButton());
    }

    if (active && !isTimeout(reminderDataRow)) {
      reminderLabel.addStyleName(REMINDER_ACTIVE);
    } else {
      reminderLabel.removeStyleName(REMINDER_ACTIVE);
    }

    buttonsPanel.add(createCancelButton());

    mainPanel.add(buttonsPanel);
    return mainPanel;
  }

  private static boolean isActive(BeeRow dataRow) {
    if (dataRow != null) {
      return BeeUtils.toBoolean(dataRow.getString(
          Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE)));

    }
    return false;
  }

  private static boolean isTimeout(BeeRow dataRow) {
    if (dataRow != null) {
      return BeeUtils.toBoolean(dataRow.getString(
          Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TIMEOUT)));

    }
    return false;
  }

  private DateTime calculateDateBySelectorChoice(BeeRow selectorRow) {

    if (selectorRow != null) {

      Integer dataField = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_DATA_FIELD));
      Integer dataIndicator = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_DATA_INDICATOR));
      Integer dataHours = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_HOURS));
      Integer dataMinutes = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_MINUTES));

      if (dataField != null) {
        DateTime calculatedDate = new DateTime();
        calculatedDate.setTime(datesByField.get(dataField).getTime());

        if (calculatedDate != null && dataIndicator != null) {
          if (BeeUtils.same(dataIndicator.toString(),
              BeeUtils.toString(ReminderDateIndicator.AFTER.ordinal()))) {
            if (dataHours != null) {
              TimeUtils.addHour(calculatedDate, dataHours);
            }
            if (dataMinutes != null) {
              TimeUtils.addMinute(calculatedDate, dataMinutes);
            }

          } else if (BeeUtils.same(dataIndicator.toString(),
              BeeUtils.toString(ReminderDateIndicator.BEFORE.ordinal()))) {
            if (dataHours != null) {
              TimeUtils.addHour(calculatedDate, dataHours * -1);
            }
            if (dataMinutes != null) {
              TimeUtils.addMinute(calculatedDate, dataMinutes * -1);
            }
          }

        }
        return calculatedDate;
      }
    }
    return null;
  }

  private void calculateReminderTime(BeeRow selectorRow, DateTime dateTime) {
    DateTime calculatedDate = dateTime;
    if (selectorRow != null && DataUtils.isId(selectorRow.getId())) {
      Integer dataField = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_DATA_FIELD));

      if (datesByField.containsKey(dataField)) {
        calculatedDate = calculateDateBySelectorChoice(selectorRow);
        if (calculatedDate != null) {
          dateTimeInput.setDateTime(calculatedDate);
          dateTimeInput.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
          dateTextLabel.setText(formatDate(calculatedDate));
        }
      }

    } else if (dateTime != null) {
      dateTextLabel.setText(formatDate(dateTime));
    }

    if (calculatedDate != null && calculatedDate.getTime() > System.currentTimeMillis()) {
      dateTextLabel.getParent().removeStyleName(STYLE_ELEMENT_NOT_VISIBLE);
    } else {
      dateTextLabel.getParent().addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
    }
  }

  private Button createCancelButton() {
    return new Button(Localized.dictionary().userReminderCancel(), event -> {
      close();
    });
  }

  private void createDateTimeWidget(Long dataTypeId) {
    DateTime dataValue = reminderDataRow == null ? null : reminderDataRow.getDateTime(
        Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TIME));

    dateTimeInput = new InputDateTime();
    dateTimeInput.addStyleName(STYLE_DIALOG_COMPONENT);
    dateTimeInput.setDateTimeFormat(Format.getPredefinedFormat(PredefinedFormat.DATE_TIME_SHORT));

    if (dataValue != null) {
      dateTimeInput.setDateTime(dataValue);
    }

    if (reminderDataRow == null || dataTypeId != null) {
      dateTimeInput.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
    }

    dateTimeInput.addEditStopHandler(valueChangeEvent ->
        setTime());
    dateTimeInput.addInputHandler(valueChangeEvent ->
        calculateReminderTime(null, dateTimeInput.getDateTime()));
  }

  private Button createReminderButton() {
    final Button reminderButton =
        new Button(Localized.dictionary().userRemind(), event -> {
          Long time =
              dateTimeInput.getDateTime() == null ? null : dateTimeInput.getDateTime().getTime();

          if (time != null && System.currentTimeMillis() < time) {
            final List<BeeColumn> columns = Data.getColumns(VIEW_USER_REMINDERS,
                Lists.newArrayList(COL_USER_REMINDER_OBJECT, COL_USER_REMINDER_OBJECT_MODULE,
                    COL_USER_REMINDER_TYPE, COL_USER_REMINDER_USER,
                    COL_USER_REMINDER_TIME, COL_USER_REMINDER_ACTIVE));

            Long selectorId = selector.getRelatedId();
            List<String> values = Lists.newArrayList(BeeUtils.toString(objectId),
                BeeUtils.toString(module.ordinal()),
                DataUtils.isId(selectorId) ? BeeUtils.toString(selector.getRelatedId()) : null,
                BeeUtils.toString(userId),
                !DataUtils.isId(selectorId) ? BeeUtils.toString(time) : null, BeeConst.STRING_TRUE);

            Queries.insert(VIEW_USER_REMINDERS, columns, values, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow row) {
                close();
                BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderCreated());
              }
            });
          } else {
            showDateError();
          }
        });

    reminderButton.addStyleName(STYLE_DIALOG_REMIND_BUTTON);
    reminderButton.addStyleName(STYLE_DIALOG_COMPONENT);
    return reminderButton;
  }

  private void createSelectorWidget(Long dataTypeId) {
    Relation relation = Relation.create(VIEW_REMINDER_TYPES, Lists.newArrayList(COL_REMINDER_NAME));
    relation.disableNewRow();

    selector = UnboundSelector.create(relation);
    selector.addStyleName(STYLE_DIALOG_COMPONENT);

    if (reminderDataRow != null && dataTypeId == null) {
      selector.setSelection(createOtherTimeSelectionRow(), null, false);
      selector.normalizeDisplay(Localized.dictionary().userReminderDisabled());
    }

    if (dataTypeId != null) {
      selector.setValue(dataTypeId, true);
    }

    selector.getOracle().setAdditionalFilter(
        Filter.equals(COL_REMINDER_MODULE, module.ordinal()), true);

    selector.addSelectorHandler(event -> {
      if (event.isChanged()) {
        if (event != null) {
          calculateReminderTime(event.getRelatedRow(), null);
        }
        if (!DataUtils.hasId(event.getRelatedRow())) {
          dateTimeInput.clearValue();
          dateTimeInput.removeStyleName(STYLE_ELEMENT_NOT_VISIBLE);
          dateTextLabel.getParent().addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
        }
      }
    });

    selector.getOracle().addDataReceivedHandler(rowSet -> {
      List<BeeRow> selectionRows = new ArrayList<>();
      for (BeeRow row : rowSet.getRows()) {
        if (DataUtils.isId(row.getId())) {
          DateTime calculatedDate = calculateDateBySelectorChoice(row);
          if (calculatedDate != null && calculatedDate.getTime() > System.currentTimeMillis()) {
            selectionRows.add(row);
          }
        }
      }

      selectionRows.add(createOtherTimeSelectionRow());

      rowSet.setRows(selectionRows);
    });
  }

  private static BeeRow createOtherTimeSelectionRow() {
    DataInfo data = Data.getDataInfo(VIEW_REMINDER_TYPES);
    BeeRow emptyRow = RowFactory.createEmptyRow(data, true);
    emptyRow.setValue(Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_NAME),
        Localized.dictionary().userReminderOtherTime());
    return emptyRow;
  }

  private Button createSuspendReminderButton() {
    final Button suspendReminderButton =
        new Button(Localized.dictionary().userReminderSuspend(), event -> {

          Queries.update(VIEW_USER_REMINDERS, flt, COL_USER_REMINDER_ACTIVE,
              BeeConst.STRING_FALSE, new Queries.IntCallback() {
                @Override
                public void onSuccess(Integer result) {
                  close();
                  BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderDisabled());
                }
              });
        });
    suspendReminderButton.addStyleName(STYLE_DIALOG_SUSPEND_BUTTON);
    suspendReminderButton.addStyleName(STYLE_DIALOG_COMPONENT);
    return suspendReminderButton;
  }

  private static TextLabel createTextLabelWidget(String text, String style) {
    TextLabel textLabel = new TextLabel(true);
    textLabel.setText(text);
    textLabel.setStyleName(style);
    return textLabel;
  }

  private Button createUpdateButton() {
    final Button updateReminderButton =
        new Button(Localized.dictionary().userReminderUpdate(), event -> {
          Long time = dateTimeInput.getDateTime() == null ? null
              : dateTimeInput.getDateTime().getTime();

          if (time != null && System.currentTimeMillis() < time) {
            List<String> columns = Lists.newArrayList(COL_USER_REMINDER_TYPE,
                COL_USER_REMINDER_TIME, COL_USER_REMINDER_ACTIVE, COL_USER_REMINDER_TIMEOUT);

            List<String> values;
            Long selectorId = selector.getRelatedId();
            if (DataUtils.isId(selectorId)) {
              values = Lists.newArrayList(
                  BeeUtils.toString(selectorId), null, BeeConst.STRING_TRUE, BeeConst.STRING_FALSE);
            } else {
              values = Lists.newArrayList(null, BeeUtils.toString(time), BeeConst.STRING_TRUE,
                  BeeConst.STRING_FALSE);
            }

            Queries
                .update(VIEW_USER_REMINDERS, flt, columns, values, new Queries.IntCallback() {
                  @Override
                  public void onSuccess(Integer result) {
                    close();
                    BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderCreated());
                  }
                });
          } else {
            showDateError();
          }
        });
    updateReminderButton.addStyleName(STYLE_DIALOG_REMIND_BUTTON);
    updateReminderButton.addStyleName(STYLE_DIALOG_COMPONENT);
    return updateReminderButton;
  }

  private void display() {
    focusOnOpen(getContent());
    center();
  }

  private static String formatDate(DateTime dateTime) {
    return BeeUtils.joinItems(" ",
        Format.render(PredefinedFormat.DATE_FULL, dateTime),
        Format.render(PredefinedFormat.TIME_SHORT, dateTime));
  }

  private void setTime() {
    setTimeToDateTimeInput(Global.getParameterTime(TimeUtils.isToday(dateTimeInput.getDateTime())
        ? TaskConstants.PRM_END_OF_WORK_DAY : TaskConstants.PRM_START_OF_WORK_DAY));
  }

  private void setTimeToDateTimeInput(Long time) {
    if (Objects.nonNull(time)) {
      DateTime dateTime = TimeUtils.toDateTimeOrNull(time);

      if (dateTime != null) {
        int hour = dateTime.getUtcHour();
        int minute = dateTime.getUtcMinute();
        DateTime value = dateTimeInput.getDateTime();
        value.setHour(hour);
        value.setMinute(minute);
        dateTimeInput.setDateTime(value);
      }
    }

    calculateReminderTime(null, dateTimeInput.getDateTime());
  }

  private static void showDateError() {
    Global.showError(Localized.dictionary().error(), Collections.singletonList(
        Localized.dictionary().userReminderSendRemindDateError()));
  }

}
