package com.butent.bee.client.modules.cars;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_CUSTOMER;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.Objects;

public class CarJobProgressGrid extends AbstractGridInterceptor {

  String parentView;
  IsRow parentRow;

  @Override
  public GridInterceptor getInstance() {
    return new CarJobProgressGrid();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (event.isReadOnly()) {
      IsRow row = event.getRowValue();
      Long eventId = row.getLong(getDataIndex(COL_SERVICE_EVENT));

      if (DataUtils.isId(eventId)) {
        RowEditor.open(TBL_SERVICE_EVENTS, eventId, Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            Queries.getRow(getViewName(), row.getId(), new RowUpdateCallback(getViewName()));
          }
        });
      } else {
        DataInfo eventInfo = Data.getDataInfo(TBL_SERVICE_EVENTS);
        BeeRow eventRow = RowFactory.createEmptyRow(eventInfo);
        eventRow.setProperty(COL_DURATION, Data.getString(parentView, parentRow, COL_DURATION));
        FormView parentForm = ViewHelper.getForm(getGridView());

        if (Objects.nonNull(parentForm)) {
          DataInfo sourceInfo = Data.getDataInfo(parentForm.getViewName());
          IsRow sourceRow = parentForm.getActiveRow();

          RelationUtils.updateRow(eventInfo, COL_SERVICE_ORDER, eventRow, sourceInfo, sourceRow,
              true);

          ImmutableMap.of(COL_TRADE_CUSTOMER, COL_COMPANY, COL_TRADE_CUSTOMER + COL_PERSON,
              COL_COMPANY_PERSON, COL_CAR, COL_CAR).forEach((s, t) ->
              RelationUtils.copyWithDescendants(sourceInfo, s, sourceRow, eventInfo, t, eventRow));
        }
        Queries.getRowSet(TBL_ATTENDEES, Collections.singletonList(COL_ATTENDEE_NAME),
            Filter.equals(COL_COMPANY_PERSON, row.getLong(getDataIndex(COL_COMPANY_PERSON))),
            new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet rowSet) {
                eventRow.setProperty(TBL_ATTENDEES, DataUtils.buildIdList(rowSet));

                RowFactory.createRow(eventInfo, eventRow, null, new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    Queries.updateAndFire(getViewName(), row.getId(), row.getVersion(),
                        COL_SERVICE_EVENT, null, BeeUtils.toString(result.getId()),
                        ModificationEvent.Kind.UPDATE_ROW);
                  }
                });
              }
            });
      }
    }
    super.onEditStart(event);
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    parentView = event.getViewName();
    parentRow = event.getRow();
    super.onParentRow(event);
  }
}
