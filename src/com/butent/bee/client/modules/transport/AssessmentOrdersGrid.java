package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssessmentOrdersGrid extends AbstractGridInterceptor implements ClickHandler {

  private final Button action = new Button(Localized.getConstants().trCreateTransportation(), this);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().addCommandItem(action);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new AssessmentOrdersGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = Sets.newHashSet();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(TBL_ASSESSMENT_FORWARDERS,
        Lists.newArrayList(COL_CARGO, COL_FORWARDER, COL_EXPEDITION, COL_FORWARDER + "Name",
            COL_FORWARDER + COL_VEHICLE),
        Filter.any(COL_ASSESSMENT, ids), new RowSetCallback() {
          @Override
          public void onSuccess(final BeeRowSet result) {
            int cargoCol = result.getColumnIndex(COL_CARGO);
            int idCol = result.getColumnIndex(COL_FORWARDER);
            int expCol = result.getColumnIndex(COL_EXPEDITION);
            int nameCol = result.getColumnIndex(COL_FORWARDER + "Name");
            int vehicleCol = result.getColumnIndex(COL_FORWARDER + COL_VEHICLE);

            final Multimap<String, String> cargo = HashMultimap.create();
            final Map<String, Pair<String, String>> forwarders = Maps.newLinkedHashMap();
            final Multimap<String, String> vehicles = HashMultimap.create();

            for (BeeRow row : result.getRows()) {
              String id = row.getString(idCol);
              cargo.put(id, row.getString(cargoCol));
              forwarders.put(id, Pair.of(row.getString(nameCol), row.getString(expCol)));
              vehicles.put(id, row.getString(vehicleCol));
            }
            if (forwarders.isEmpty()) {
              presenter.getGridView().notifyWarning(Localized.getConstants().noData());
            } else {
              List<String> fwd = Lists.newArrayList();

              for (Pair<String, String> forwarder : forwarders.values()) {
                fwd.add(forwarder.getA());
              }
              Global.choice(Localized.getConstants().trChooseForwarder(), null, fwd,
                  new ChoiceCallback() {
                    @Override
                    public void onSuccess(int value) {
                      final String id = forwarders.keySet().toArray(new String[0])[value];

                      Queries.insert(VIEW_EXPEDITION_TRIPS, Data.getColumns(VIEW_EXPEDITION_TRIPS,
                          Lists.newArrayList(COL_FORWARDER, COL_EXPEDITION,
                              COL_FORWARDER + COL_VEHICLE)),
                          Lists.newArrayList(id, forwarders.get(id).getB(),
                              BeeUtils.joinItems(vehicles.get(id))), null, new RowCallback() {
                            @Override
                            public void onSuccess(BeeRow trip) {
                              final Long tripId = trip.getId();
                              final Holder<Integer> holder = Holder.of(0);
                              final Collection<String> cargoIds = cargo.get(id);

                              for (String cargoId : cargoIds) {
                                Queries.insert(TBL_CARGO_TRIPS, Data.getColumns(TBL_CARGO_TRIPS,
                                    Lists.newArrayList(COL_CARGO, COL_TRIP)),
                                    Lists.newArrayList(cargoId, BeeUtils.toString(tripId)), null,
                                    new RowCallback() {
                                      @Override
                                      public void onSuccess(BeeRow res) {
                                        holder.set(holder.get() + 1);

                                        if (Objects.equal(holder.get(), cargoIds.size())) {
                                          DataChangeEvent.fire(BeeKeeper.getBus(),
                                              presenter.getViewName(),
                                              DataChangeEvent.CANCEL_RESET_REFRESH);

                                          RowEditor.openForm(FORM_ASSESSMENT_TRANSPORTATION,
                                              Data.getDataInfo(VIEW_ASSESSMENT_TRANSPORTATIONS),
                                              tripId, Opener.MODAL);
                                        }
                                      }
                                    });
                              }
                            }
                          });
                    }
                  });
            }
          }
        });
  }
}
