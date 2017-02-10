package com.butent.bee.client.modules.transport;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class TransportUtils {

  public static void copyOrderWithCargos(Long orderId, Filter cargoFilter,
      BiConsumer<Long, RowInfoList> consumer) {
    Queries.getRow(VIEW_ORDERS, orderId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow copyOrderRow) {
        DataInfo orderInfo = Data.getDataInfo(VIEW_ORDERS);
        BeeRow order = RowFactory.createEmptyRow(orderInfo, true);

        for (String col : new String[] {
            COL_CUSTOMER, COL_CUSTOMER_NAME, ALS_CUSTOMER_TYPE_NAME,
            COL_PAYER, COL_PAYER_NAME, ALS_PAYER_TYPE_NAME,
            "CustomerPerson", "PersonFirstName", "PersonLastName"}) {

          int idx = orderInfo.getColumnIndex(col);

          if (!BeeConst.isUndef(idx)) {
            order.setValue(idx, copyOrderRow.getString(idx));
          }
        }
        RowFactory.createRow(orderInfo, order, Modality.ENABLED, new RowCallback() {
          @Override
          public void onSuccess(BeeRow newOrder) {
            getCargos(cargoFilter, cargos -> {
              BeeRowSet newCargos = DataUtils.createRowSetForInsert(cargos);

              if (newCargos.isEmpty()) {
                consumer.accept(newOrder.getId(), null);

              } else {
                int orderIdx = newCargos.getColumnIndex(COL_ORDER);

                for (BeeRow cargo : newCargos) {
                  cargo.setValue(orderIdx, newOrder.getId());
                }
                Queries.insertRows(newCargos, new RpcCallback<RowInfoList>() {
                  @Override
                  public void onSuccess(RowInfoList newIds) {
                    Assert.isTrue(cargos.getNumberOfRows() == newIds.size());

                    String[] gridList = new String[] {TBL_CARGO_LOADING, TBL_CARGO_UNLOADING};

                    Runnable onCloneChildren = new Runnable() {
                      int copiedGrids;

                      @Override
                      public void run() {
                        if (Objects.equals(gridList.length, ++copiedGrids) && consumer != null) {
                          consumer.accept(newOrder.getId(), newIds);
                        }
                      }
                    };

                    for (String view : gridList) {
                      BeeRowSet newPlaces = Data.createRowSet(view);
                      int cargoIdx = newPlaces.getColumnIndex(COL_CARGO);

                      for (int i = 0; i < cargos.getNumberOfRows(); i++) {
                        for (BeeRow row : BeeRowSet.restore(cargos.getRow(i).getProperty(view))) {
                          BeeRow clonned = newPlaces.addEmptyRow();
                          clonned.setValues(row.getValues());
                          clonned.setValue(cargoIdx, newIds.get(i).getId());
                        }
                      }
                      if (!newPlaces.isEmpty()) {
                        newPlaces = DataUtils.createRowSetForInsert(newPlaces);
                        Queries.insertRows(newPlaces, new RpcCallback<RowInfoList>() {
                          @Override
                          public void onSuccess(RowInfoList result) {
                            onCloneChildren.run();
                          }
                        });
                      } else {
                        onCloneChildren.run();
                      }
                    }
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  static void getCargoPlaces(Filter filter, BiConsumer<BeeRowSet, BeeRowSet> places) {
    Queries.getData(Arrays.asList(TBL_CARGO_LOADING, TBL_CARGO_UNLOADING),
        ImmutableMap.of(TBL_CARGO_LOADING, filter, TBL_CARGO_UNLOADING, filter), null,
        new Queries.DataCallback() {
          @Override
          public void onSuccess(Collection<BeeRowSet> data) {
            BeeRowSet loading = null;
            BeeRowSet unloading = null;

            for (BeeRowSet rowSet : data) {
              if (Objects.isNull(loading)) {
                loading = rowSet;
              } else {
                unloading = rowSet;
              }
            }
            places.accept(loading, unloading);
          }
        });
  }

  static void getCargos(Filter cargoFilter, Consumer<BeeRowSet> cargoConsumer) {
    Queries.getRowSet(VIEW_ORDER_CARGO, null, cargoFilter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet cargo) {
        getCargoPlaces(Filter.any(COL_CARGO, cargo.getRowIds()), (loading, unloading) -> {
          for (BeeRowSet places : new BeeRowSet[] {loading, unloading}) {
            for (BeeRow cargoRow : cargo) {
              BeeRowSet current = new BeeRowSet(places.getViewName(), places.getColumns());

              for (BeeRow place : DataUtils.filterRows(places, COL_CARGO, cargoRow.getId())) {
                BeeRow cloned = DataUtils.cloneRow(place);

                Stream.of(COL_PLACE_CITY, COL_PLACE_COUNTRY).forEach(key -> {
                  int idx = current.getColumnIndex(key + "Name");

                  if (!BeeConst.isUndef(idx) && BeeUtils.isEmpty(cloned.getString(idx))) {
                    cloned.setValue(idx,
                        cloned.getString(current.getColumnIndex(key + VAR_UNBOUND)));
                  }
                });
                current.addRow(cloned);
              }
              cargoRow.setProperty(places.getViewName(), current.serialize());
            }
          }
          cargoConsumer.accept(cargo);
        });
      }
    });
  }

  private TransportUtils() {
  }
}
