package com.butent.bee.client.data;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import com.butent.bee.client.Settings;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataCache implements HandlesAllDataEvents {

  public interface MultiCallback {
    void onSuccess(Integer result);
  }

  private static final BeeLogger logger = LogUtils.getLogger(DataCache.class);

  private final Map<String, BeeRowSet> data = new HashMap<>();

  private final Map<String, State> states = new HashMap<>();

  private final Map<String, List<RowSetCallback>> callbacks = new HashMap<>();

  public DataCache() {
    super();
  }

  public boolean contains(String viewName, Filter filter) {
    BeeRowSet rowSet = getRowSet(viewName);
    if (rowSet == null) {
      return false;
    }

    for (BeeRow row : rowSet.getRows()) {
      if (filter.isMatch(rowSet.getColumns(), row)) {
        return true;
      }
    }
    return false;
  }

  public void ensureData(String viewName) {
    getData(viewName, null);
  }

  public Boolean getBoolean(String viewName, long rowId, String columnId) {
    BeeRow row = getRow(viewName, rowId);
    if (row == null) {
      return null;
    } else {
      return Data.getBoolean(viewName, row, columnId);
    }
  }

  public void getData(final Collection<String> viewNames, final MultiCallback multiCallback) {
    if (multiCallback == null) {
      for (String viewName : viewNames) {
        ensureData(viewName);
      }

    } else if (Settings.minimizeNumberOfConcurrentRequests()) {
      List<String> notCached = new ArrayList<>();
      for (String viewName : viewNames) {
        if (!isLoaded(viewName)) {
          notCached.add(viewName);
        }
      }

      if (notCached.isEmpty()) {
        multiCallback.onSuccess(viewNames.size());
      } else {
        Queries.getData(notCached, CachingPolicy.NONE, new Queries.DataCallback() {
          @Override
          public void onSuccess(Collection<BeeRowSet> result) {
            for (BeeRowSet rowSet : result) {
              put(rowSet.getViewName(), rowSet);
            }
            multiCallback.onSuccess(viewNames.size());
          }
        });
      }

    } else {
      RowSetCallback callback = new RowSetCallback() {
        private boolean consumed;

        @Override
        public void onSuccess(BeeRowSet result) {
          if (this.consumed) {
            return;
          }

          boolean ok = true;
          for (String viewName : viewNames) {
            if (!isLoaded(viewName)) {
              ok = false;
              break;
            }
          }

          if (ok) {
            this.consumed = true;
            multiCallback.onSuccess(viewNames.size());
          }
        }
      };

      for (String viewName : viewNames) {
        getData(viewName, callback);
      }
    }
  }

  public void getData(String viewName, RowSetCallback callback) {
    if (State.LOADED.equals(states.get(viewName))) {
      if (callback != null) {
        callback.onSuccess(data.get(viewName));
      }

    } else {
      if (callback != null) {
        List<RowSetCallback> cbs = callbacks.get(viewName);
        if (cbs == null) {
          callbacks.put(viewName, Lists.newArrayList(callback));
        } else {
          cbs.add(callback);
        }
      }
      loadData(viewName);
    }
  }

  public Integer getInteger(String viewName, long rowId, String columnId) {
    BeeRow row = getRow(viewName, rowId);
    if (row == null) {
      return null;
    } else {
      return Data.getInteger(viewName, row, columnId);
    }
  }

  public Long getLong(String viewName, long rowId, String columnId) {
    BeeRow row = getRow(viewName, rowId);
    if (row == null) {
      return null;
    } else {
      return Data.getLong(viewName, row, columnId);
    }
  }

  public BeeRow getRow(String viewName, long rowId) {
    BeeRowSet rowSet = getRowSet(viewName);
    if (rowSet == null) {
      return null;
    } else {
      return rowSet.getRowById(rowId);
    }
  }

  public BeeRowSet getRowSet(String viewName) {
    BeeRowSet rowSet = data.get(viewName);
    if (rowSet == null) {
      logger.warning(NameUtils.getName(this), viewName, "data not available");
    }
    return rowSet;
  }

  public String getString(String viewName, long rowId, String columnId) {
    BeeRow row = getRow(viewName, rowId);
    if (row == null) {
      return null;
    } else {
      return Data.getString(viewName, row, columnId);
    }
  }

  public boolean isLoaded(String viewName) {
    return State.LOADED.equals(states.get(viewName));
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event)) {
      BeeRowSet rowSet = data.get(event.getViewName());
      if (rowSet != null) {
        event.applyTo(rowSet);
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
      BeeRowSet rowSet = data.get(event.getViewName());
      if (rowSet != null) {
        for (RowInfo rowInfo : event.getRows()) {
          rowSet.removeRowById(rowInfo.getId());
        }
      }
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event)) {
      BeeRowSet rowSet = data.get(event.getViewName());
      if (rowSet != null) {
        rowSet.removeRowById(event.getRowId());
      }
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event)) {
      BeeRowSet rowSet = data.get(event.getViewName());
      if (rowSet != null && !rowSet.containsRow(event.getRowId())) {
        rowSet.addRow(event.getRow());
      }
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event)) {
      BeeRowSet rowSet = data.get(event.getViewName());
      if (rowSet != null) {
        rowSet.updateRow(event.getRow());
      }
    }
  }

  public void refresh() {
    if (data.isEmpty()) {
      return;
    }
    states.clear();

    for (String viewName : ImmutableSet.copyOf(data.keySet())) {
      loadData(viewName);
    }
  }

  private boolean isEventRelevant(ModificationEvent<?> event) {
    return event != null && event.containsAny(data.keySet());
  }

  private void loadData(final String viewName) {
    if (State.PENDING.equals(states.get(viewName))) {
      return;
    }
    states.put(viewName, State.PENDING);

    Queries.getRowSet(viewName, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        put(viewName, result.copy());

        List<RowSetCallback> cbs = callbacks.get(viewName);
        if (cbs != null) {
          for (RowSetCallback callback : cbs) {
            callback.onSuccess(result);
          }
          callbacks.remove(viewName);
        }
      }
    });
  }

  private void put(String viewName, BeeRowSet rowSet) {
    data.put(viewName, rowSet);
    states.put(viewName, State.LOADED);
  }
}
