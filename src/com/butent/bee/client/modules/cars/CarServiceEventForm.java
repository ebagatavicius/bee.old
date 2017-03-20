package com.butent.bee.client.modules.cars;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_CUSTOMER;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.ChildSelector;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarPanel;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;

public class CarServiceEventForm extends AbstractFormInterceptor implements ClickHandler,
    SelectorEvent.Handler {

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (Objects.equals(editableWidget.getColumnId(), COL_CAR) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(this);
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (Objects.equals(name, COL_SERVICE_ORDER) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(this);
    }
    if (Objects.equals(name, TBL_SERVICE_SYMPTOMS) && widget instanceof ChildSelector) {
      ((ChildSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          Filter filter = Filter.isNull(COL_MODEL);

          if (DataUtils.isId(getLongValue(COL_MODEL))) {
            filter = Filter.or(filter, Filter.equals(COL_MODEL, getLongValue(COL_MODEL)));
          }
          event.getSelector().setAdditionalFilter(filter);
        }
      });
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    commitAttendees(getActiveRow().getProperty(TBL_ATTENDEES), result, true);
    super.afterInsertRow(result, forced);
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    commitAttendees(getActiveRow().getProperty(TBL_ATTENDEES), result, false);
    super.afterUpdateRow(result);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (Objects.equals(action, Action.SAVE) && !validateDates()) {
      return false;
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CarServiceEventForm();
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    DataInfo eventInfo = Data.getDataInfo(getViewName());
    IsRow eventRow = getActiveRow();
    DataInfo orderInfo = Data.getDataInfo(TBL_SERVICE_ORDERS);
    BeeRow orderRow = RowFactory.createEmptyRow(orderInfo);

    orderRow.setValue(orderInfo.getColumnIndex(COL_NOTES),
        eventRow.getString(eventInfo.getColumnIndex(CalendarConstants.COL_DESCRIPTION)));

    orderRow.setProperty(TBL_SERVICE_SYMPTOMS, eventRow.getProperty(TBL_SERVICE_SYMPTOMS));

    ImmutableMap.of(COL_COMPANY, COL_TRADE_CUSTOMER, COL_COMPANY_PERSON,
        COL_TRADE_CUSTOMER + COL_PERSON, COL_CAR, COL_CAR).forEach((s, t) ->
        RelationUtils.copyWithDescendants(eventInfo, s, eventRow, orderInfo, t, orderRow));

    RowFactory.createRow(orderInfo, orderRow, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        RelationUtils.updateRow(eventInfo, COL_SERVICE_ORDER, eventRow, orderInfo, result, true);
        getFormView().refresh();
      }
    });
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    DataInfo eventInfo = Data.getDataInfo(getViewName());
    DataInfo carInfo = Data.getDataInfo(event.getRelatedViewName());
    Long owner = getLongValue(COL_COMPANY);

    if (event.isNewRow()) {
      RelationUtils.copyWithDescendants(eventInfo, COL_COMPANY, getActiveRow(),
          carInfo, COL_OWNER, event.getNewRow());

    } else if (event.isOpened()) {
      event.getSelector().setAdditionalFilter(Objects.isNull(owner) ? null
          : Filter.equals(COL_OWNER, owner));

    } else if (event.isChanged() && Objects.isNull(owner)) {
      RelationUtils.copyWithDescendants(carInfo, COL_OWNER, event.getRelatedRow(),
          eventInfo, COL_COMPANY, getActiveRow());
      getFormView().refresh();
    }
  }

  @Override
  public void onLoad(FormView form) {
    FaLabel cal = new FaLabel(FontAwesome.CALENDAR);
    cal.setTitle(Localized.dictionary().startingTime());

    cal.addClickHandler(clickEvent -> BeeKeeper.getRpc()
        .makeGetRequest(CarsKeeper.createSvcArgs(SVC_GET_CALENDAR), new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            CalendarKeeper.openCalendar(response.getResponseAsLong(), result -> {
              if (result instanceof CalendarPanel) {
                CalendarPanel panel = (CalendarPanel) result;

                panel.setTimePickerCallback((startTime, attendee) -> {
                  if (Objects.nonNull(attendee)) {
                    getActiveRow().setProperty(TBL_ATTENDEES, attendee);
                  }
                  Long startMillis = getLongValue(COL_START_DATE_TIME);
                  Long endMillis = getLongValue(COL_END_DATE_TIME);
                  getActiveRow().setValue(getDataIndex(COL_START_DATE_TIME), startTime);

                  if (BeeUtils.allNotNull(startMillis, endMillis)) {
                    getActiveRow().setValue(getDataIndex(COL_END_DATE_TIME),
                        endMillis + (startTime.getTime() - startMillis));
                  }
                  form.refresh();
                  UiHelper.closeDialog(panel);
                });
                StyleUtils.setWidth(panel, BeeKeeper.getScreen().getWidth() * 0.7, CssUnit.PX);
                StyleUtils.setHeight(panel, BeeKeeper.getScreen().getHeight() * 0.9, CssUnit.PX);

                Global.showModalWidget(cal.getTitle(), panel);
              }
            });
          }
        }));
    getHeaderView().addCommandItem(cal);
    super.onLoad(form);
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (event.isEmpty()) {
      afterUpdateRow(getActiveRow());
    }
    super.onSaveChanges(listener, event);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    if (newRow.isEmpty(getDataIndex(COL_END_DATE_TIME))
        && BeeUtils.isEmpty(newRow.getProperty(COL_DURATION))) {
      newRow.setProperty(COL_DURATION, TimeUtils.renderTime(TimeUtils.MILLIS_PER_HOUR, true));
    }
    super.onStartNewRow(form, oldRow, newRow);
  }

  private void commitAttendees(String attendees, IsRow eventRow, boolean isNew) {
    Long appointmentId = eventRow.getLong(getDataIndex(COL_APPOINTMENT));

    BiConsumer<BeeRow, State> upd = (result, state) -> {
      AppointmentEvent.fire(Appointment.create(result), state);
      Queries.getRow(getViewName(), eventRow.getId(), new RowUpdateCallback(getViewName()));
    };

    RowCallback callback;
    if (isNew) {
      callback = new RowInsertCallback(TBL_APPOINTMENTS) {
        @Override
        public void onSuccess(BeeRow result) {
          upd.accept(result, State.CREATED);
          super.onSuccess(result);
        }
      };

    } else {
      callback = new RowUpdateCallback(TBL_APPOINTMENTS) {
        @Override
        public void onSuccess(BeeRow result) {
          upd.accept(result, State.CHANGED);
          super.onSuccess(result);
        }
      };
    }

    Queries.updateChildren(TBL_APPOINTMENTS, appointmentId,
        Collections.singleton(RowChildren.create(TBL_APPOINTMENT_ATTENDEES,
            COL_APPOINTMENT, appointmentId, COL_ATTENDEE, attendees)), callback);
  }

  private boolean validateDates() {
    DateTime start = getDateTimeValue(COL_START_DATE_TIME);
    DateTime end = getDateTimeValue(COL_END_DATE_TIME);

    if (Objects.isNull(end)) {
      Long duration = TimeUtils.parseTime(getActiveRow().getProperty(COL_DURATION));

      if (BeeUtils.allNotNull(duration, start)) {
        end = new DateTime(start.getTime() + duration);
        getActiveRow().setValue(getDataIndex(COL_END_DATE_TIME), end);
        getFormView().refreshBySource(COL_END_DATE_TIME);
      }
    }
    boolean ok = TimeUtils.isMore(end, start);

    if (!ok) {
      getFormView().focus(COL_END_DATE_TIME);
      getFormView().notifySevere(Localized.dictionary().crmFinishDateMustBeGreaterThanStart());
    }
    return ok;
  }
}
