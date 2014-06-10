package com.butent.bee.client.rights;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumer;
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
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Roles implements HandlesAllDataEvents {

  private static BeeLogger logger = LogUtils.getLogger(Roles.class);
  
  private static final Roles INSTANCE = new Roles();

  public static void ensureData(final Consumer<Boolean> consumer) {
    Assert.notNull(consumer);
    
    if (INSTANCE.data.isEmpty()) {
      getData(new Consumer<Map<Long, String>>() {
        @Override
        public void accept(Map<Long, String> input) {
          consumer.accept(!input.isEmpty());
        }
      });
      
    } else {
      consumer.accept(true);
    }
  }
  
  public static void getData(Consumer<Map<Long, String>> consumer) {
    Assert.notNull(consumer);

    if (INSTANCE.data.isEmpty()) {
      INSTANCE.load(consumer);
    } else {
      consumer.accept(INSTANCE.data);
    }
  }

  public static void getName(final Long id, final Consumer<String> consumer) {
    Assert.notNull(consumer);
    
    if (!DataUtils.isId(id)) {
      consumer.accept(null);
    
    } else if (INSTANCE.data.containsKey(id)) {
      consumer.accept(INSTANCE.data.get(id));

    } else {
      getData(new Consumer<Map<Long, String>>() {
        @Override
        public void accept(Map<Long, String> input) {
          consumer.accept(input.get(id));
        }
      });
    }
  }
  
  public static void getNames(final Consumer<List<String>> consumer) {
    Assert.notNull(consumer);
    
    getData(new Consumer<Map<Long, String>>() {
      @Override
      public void accept(Map<Long, String> input) {
        List<String> names = new ArrayList<>(input.values());
        if (names.size() > 1) {
          Collections.sort(names, Collator.DEFAULT);
        }
        
        consumer.accept(names);
      }
    });
  }

  private final Map<Long, String> data = new HashMap<>();

  private Roles() {
    BeeKeeper.getBus().registerDataHandler(this, false);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event)) {
      if (data.containsKey(event.getRowId()) && event.hasSource(COL_ROLE_NAME)
          && !BeeUtils.isEmpty(event.getValue())) {
        data.put(event.getRowId(), event.getValue());
      } else {
        refresh();
      }
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isEventRelevant(event)) {
      boolean refresh = false;

      for (RowInfo rowInfo : event.getRows()) {
        if (data.containsKey(rowInfo.getId())) {
          data.remove(rowInfo.getId());
        } else {
          refresh = true;
        }
      }

      if (refresh) {
        refresh();
      }
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event)) {
      if (data.containsKey(event.getRowId())) {
        data.remove(event.getRowId());
      } else {
        refresh();
      }
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event)) {
      String name = Data.getString(VIEW_ROLES, event.getRow(), COL_ROLE_NAME);

      if (!data.containsKey(event.getRowId()) && !BeeUtils.isEmpty(name)) {
        data.put(event.getRowId(), name);
      } else {
        refresh();
      }
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event)) {
      String name = Data.getString(VIEW_ROLES, event.getRow(), COL_ROLE_NAME);

      if (data.containsKey(event.getRowId()) && !BeeUtils.isEmpty(name)) {
        data.put(event.getRowId(), name);
      } else {
        refresh();
      }
    }
  }
  
  private boolean isEventRelevant(DataEvent event) {
    return !data.isEmpty() && event != null && event.hasView(VIEW_ROLES);
  }
  
  private void load(final Consumer<Map<Long, String>> consumer) {
    Queries.getRowSet(VIEW_ROLES, Lists.newArrayList(COL_ROLE_NAME), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (!data.isEmpty()) {
          data.clear();
        }

        if (DataUtils.isEmpty(result)) {
          logger.severe("roles not available");

        } else {
          int index = result.getColumnIndex(COL_ROLE_NAME);
          for (BeeRow row : result) {
            data.put(row.getId(), row.getString(index));
          }
          
          logger.info(result.getViewName(), data.size());
        }

        if (consumer != null) {
          consumer.accept(data);
        }
      }
    });
  }

  private void refresh() {
    load(null);
  }
}