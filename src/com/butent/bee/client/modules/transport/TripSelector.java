package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent.Effect;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

final class TripSelector implements Handler, ClickHandler {

  public static void select(String[] cargos, Filter tripFilter, Element target) {
    TripSelector selector = new TripSelector(cargos, tripFilter);
    selector.dialog.showOnTop(target);
  }

  final String[] cargos;
  final DialogBox dialog;
  final UnboundSelector selector;
  final Button tripButton;
  final Button expeditionTripButton;

  private TripSelector(String[] cargos, Filter tripFilter) {
    this.cargos = cargos;
    this.dialog = DialogBox.create(Localized.getConstants().trAssignTrip());
    dialog.setHideOnEscape(true);

    HtmlTable container = new HtmlTable();
    container.setBorderSpacing(5);

    container.setHtml(0, 0, Localized.getConstants().trCargoSelectTrip());

    Relation relation = Relation.create(VIEW_ACTIVE_TRIPS,
        Lists.newArrayList(COL_TRIP_NO, "VehicleNumber", "DriverFirstName", "DriverLastName",
            "ExpeditionType", "ForwarderName"));
    relation.disableNewRow();
    relation.setCaching(Relation.Caching.QUERY);
    relation.setFilter(tripFilter);

    selector = UnboundSelector.create(relation, Lists.newArrayList(COL_TRIP_NO));
    selector.addEditStopHandler(this);
    container.setWidget(0, 1, selector);

    tripButton = new Button(Localized.getConstants().trNewTrip(), this);
    container.setWidget(1, 0, tripButton);

    expeditionTripButton = new Button(Localized.getConstants().trNewExpedition(), this);
    container.setWidget(1, 1, expeditionTripButton);

    dialog.setWidget(container);
  }

  @Override
  public void onClick(ClickEvent event) {
    if (event.getSource() == tripButton) {
      createNewTrip(VIEW_TRIPS);
    } else if (event.getSource() == expeditionTripButton) {
      createNewTrip(VIEW_EXPEDITION_TRIPS);
    }
  }

  @Override
  public void onEditStop(EditStopEvent event) {
    if (event.isChanged()) {
      addTrip(BeeUtils.toLong(selector.getValue()));
    }
  }

  private void addTrip(final long tripId) {
    if (!DataUtils.isId(tripId)) {
      return;
    }
    dialog.close();

    final Holder<Integer> holder = Holder.of(0);
    List<BeeColumn> columns = Data.getColumns(VIEW_CARGO_TRIPS,
        Lists.newArrayList(COL_CARGO, COL_TRIP));

    for (String cargoId : cargos) {
      Queries.insert(VIEW_CARGO_TRIPS, columns,
          Lists.newArrayList(cargoId, BeeUtils.toString(tripId)), null, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              holder.set(holder.get() + 1);

              if (Objects.equals(holder.get(), cargos.length)) {
                Data.onTableChange(TBL_CARGO_TRIPS, EnumSet.of(Effect.REFRESH));
              }
            }
          });
    }
  }

  private void createNewTrip(final String viewName) {
    DataInfo dataInfo = Data.getDataInfo(viewName);

    RowFactory.createRow(dataInfo.getEditForm(), dataInfo.getNewRowCaption(),
        dataInfo, RowFactory.createEmptyRow(dataInfo, true), new RowCallback() {
          @Override
          public void onSuccess(BeeRow row) {
            RowInsertEvent.fire(BeeKeeper.getBus(), viewName, row, null);
            addTrip(row.getId());
          }
        });
  }
}