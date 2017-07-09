package com.butent.bee.client.modules.transport;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.view.search.BooleanFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssessmentOrdersGrid extends AbstractGridInterceptor implements ClickHandler {

  private final Button action = new Button(Localized.dictionary().trCreateTransportation(), this);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().addCommandItem(action);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {

    if (BeeUtils.inList(columnName, PROP_INCOMES_COMMITED, PROP_EXPENSES_COMMITED)) {
      return new BooleanFilterSupplier(null, null, null, columnDescription.getFilterOptions()) {
        @Override
        public Filter parse(FilterValue input) {
          Boolean b = getBoolean(input);

          if (b == null) {
            return null;
          } else {
            return Filter.custom(getOptions(), columnName, BooleanValue.pack(b));
          }
        }
      };
    }
    return super.getFilterSupplier(columnName, columnDescription);
  }

  @Override
  public GridInterceptor getInstance() {
    return new AssessmentOrdersGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(TBL_ASSESSMENT_FORWARDERS,
        Lists.newArrayList(COL_CARGO, COL_FORWARDER, COL_EXPEDITION, COL_FORWARDER + "Name",
            COL_FORWARDER + COL_VEHICLE, COL_CARGO_TRIP),
        Filter.any(COL_ASSESSMENT, ids), new RowSetCallback() {
          @Override
          public void onSuccess(final BeeRowSet result) {
            int cargoCol = result.getColumnIndex(COL_CARGO);
            int idCol = result.getColumnIndex(COL_FORWARDER);
            int expCol = result.getColumnIndex(COL_EXPEDITION);
            int nameCol = result.getColumnIndex(COL_FORWARDER + "Name");
            int vehicleCol = result.getColumnIndex(COL_FORWARDER + COL_VEHICLE);
            int cargoTripCol = result.getColumnIndex(COL_CARGO_TRIP);

            final Map<String, Pair<String, String>> forwarders = new LinkedHashMap<>();
            final Multimap<String, String> vehicles = HashMultimap.create();
            final Map<String, Multimap<String, Long>> cargoMap = new HashMap<>();

            for (BeeRow row : result.getRows()) {
              String id = row.getString(idCol);
              forwarders.put(id, Pair.of(row.getString(nameCol), row.getString(expCol)));
              vehicles.put(id, row.getString(vehicleCol));

              Multimap<String, Long> multimap = cargoMap.getOrDefault(id, HashMultimap.create());
              multimap.put(row.getString(cargoCol), row.getLong(cargoTripCol));
              cargoMap.put(id, multimap);
            }
            if (forwarders.isEmpty()) {
              presenter.getGridView().notifyWarning(Localized.dictionary().noData());
            } else {
              List<String> fwd = new ArrayList<>();

              for (Pair<String, String> forwarder : forwarders.values()) {
                fwd.add(forwarder.getA());
              }
              Global.choice(Localized.dictionary().trChooseForwarder(), null, fwd,
                  value -> {
                    final String id = forwarders.keySet().toArray(new String[0])[value];

                    Queries.insert(VIEW_EXPEDITION_TRIPS, Data.getColumns(VIEW_EXPEDITION_TRIPS,
                        Lists.newArrayList(COL_FORWARDER, COL_EXPEDITION,
                            COL_FORWARDER + COL_VEHICLE)),
                        Lists.newArrayList(id, forwarders.get(id).getB(),
                            BeeUtils.joinItems(vehicles.get(id))), null, new RowCallback() {
                          @Override
                          public void onSuccess(BeeRow trip) {
                            copyCargoPlaces(trip.getId(), cargoMap.get(id));
                          }
                        });
                  });
            }
          }
        });
  }

  private static void copyCargoPlaces(Long tripId, Multimap<String, Long> cargoMap) {
    Queries.getRowSet(VIEW_TRIP_CARGO, null, Filter.idIn(cargoMap.values()), new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        Multimap<String, SimpleRowSet> placesMap = HashMultimap.create();
        for (BeeRow row : result) {
          SimpleRowSet loading = SimpleRowSet.restore(row.getProperty(VAR_LOADING));
          if (loading != null) {
            placesMap.put(TBL_CARGO_LOADING, loading);
          }

          SimpleRowSet unloading = SimpleRowSet.restore(row.getProperty(VAR_UNLOADING));
          if (unloading != null) {
            placesMap.put(TBL_CARGO_UNLOADING, unloading);
          }
        }

        final Latch latch = new Latch(cargoMap.keySet().size());

        for (String cargoId : cargoMap.keySet()) {
          Queries.insert(TBL_CARGO_TRIPS, Data.getColumns(TBL_CARGO_TRIPS,
              Lists.newArrayList(COL_CARGO, COL_TRIP)),
              Lists.newArrayList(cargoId, BeeUtils.toString(tripId)), null, new RowCallback() {
                @Override
                public void onSuccess(BeeRow res) {
                  for (String key : new String[] {TBL_CARGO_LOADING, TBL_CARGO_UNLOADING}) {
                    BeeRowSet newPlaces = Data.createRowSet(key);
                    int cargoTripIdx = newPlaces.getColumnIndex(COL_CARGO_TRIP);

                    for (SimpleRowSet places : placesMap.get(key)) {
                      for (SimpleRowSet.SimpleRow place : places) {
                        if (cargoMap.get(cargoId).contains(place.getLong(COL_CARGO_TRIP))) {
                          BeeRow clonned = newPlaces.addEmptyRow();

                          for (String col : place.getColumnNames()) {
                            if (!BeeUtils.isEmpty(place.getValue(col))
                                && newPlaces.containsColumn(col)) {
                              int idx = newPlaces.getColumnIndex(col);
                              clonned.setValue(idx, place.getValue(col));
                            }
                          }

                          clonned.setValue(cargoTripIdx, res.getId());
                        }
                      }
                    }

                    if (!newPlaces.isEmpty()) {
                      newPlaces = DataUtils.createRowSetForInsert(newPlaces);
                      Queries.insertRows(newPlaces);
                    }

                    latch.decrement();

                    if (latch.isOpen()) {
                      RowEditor.openForm(FORM_ASSESSMENT_TRANSPORTATION,
                          Data.getDataInfo(VIEW_ASSESSMENT_TRANSPORTATIONS),
                          Filter.compareId(tripId), Opener.MODAL);
                    }
                  }
                }
              });
        }
      }
    });
  }
}

