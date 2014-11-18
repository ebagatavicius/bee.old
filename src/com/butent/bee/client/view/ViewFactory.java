package com.butent.bee.client.view;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Search;
import com.butent.bee.client.composite.ResourceEditor;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.maps.MapUtils;
import com.butent.bee.client.modules.administration.ParametersGrid;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public final class ViewFactory {

  public enum SupplierKind {
    GRID("grid_") {
      @Override
      void create(String item, ViewCallback callback) {
        GridFactory.openGrid(item, GridFactory.getGridInterceptor(item), null,
            getPresenterCallback(callback));
      }
    },

    FORM("form_") {
      @Override
      void create(final String item, final ViewCallback callback) {
        FormFactory.getFormDescription(item, new Callback<FormDescription>() {
          @Override
          public void onSuccess(FormDescription result) {
            FormFactory.openForm(result, FormFactory.getFormInterceptor(item),
                getPresenterCallback(callback));
          }
        });
      }
    },

    ROW_EDITOR("row_") {
      @Override
      void create(String item, ViewCallback callback) {
        RowEditor.parse(item, Opener.with(getPresenterCallback(callback)));
      }
    },

    REPORT("report_") {
      @Override
      void create(String item, ViewCallback callback) {
        Report.open(item, callback);
      }
    },

    PARAMETERS("parameters_") {
      @Override
      void create(String item, ViewCallback callback) {
        ParametersGrid.open(item, callback);
      }
    },

    RESOURCE("resource_") {
      @Override
      void create(String item, final ViewCallback callback) {
        ResourceEditor.open(item, callback);
      }
    },

    CHAT("chat_") {
      @Override
      void create(String item, ViewCallback callback) {
        Long id = BeeUtils.toLongOrNull(item);
        if (id != null) {
          Global.getRooms().open(id, callback);
        }
      }
    },

    MAP("map_") {
      @Override
      void create(String item, ViewCallback callback) {
        MapUtils.open(item, callback);
      }
    },

    SEARCH("search_") {
      @Override
      void create(String item, ViewCallback callback) {
        Search.doQuery(item, callback);
      }
    },

    NEWS("news_") {
      @Override
      void create(String item, ViewCallback callback) {
        Global.getNewsAggregator().filterNews(item, callback);
      }
    },

    CALENDAR("calendar_") {
      @Override
      void create(String item, ViewCallback callback) {
        Long id = BeeUtils.toLongOrNull(item);
        if (DataUtils.isId(id)) {
          CalendarKeeper.openCalendar(id, callback);
        }
      }
    };

    private static Pair<SupplierKind, String> parse(String key) {
      for (SupplierKind kind : SupplierKind.values()) {
        if (BeeUtils.isPrefix(key, kind.prefix)) {
          return Pair.of(kind, BeeUtils.removePrefix(key, kind.prefix));
        }
      }
      return null;
    }

    private final String prefix;

    private SupplierKind(String prefix) {
      this.prefix = prefix;
    }

    public String getKey(String item) {
      if (BeeUtils.isEmpty(item)) {
        return null;
      } else {
        return prefix + BeeUtils.trim(item);
      }
    }

    abstract void create(String item, ViewCallback callback);
  }

  private static final Map<String, ViewSupplier> suppliers = new HashMap<>();

  public static void clear() {
    suppliers.clear();
  }

  public static void create(String key, ViewCallback callback) {
    if (BeeUtils.isEmpty(key) || callback == null) {
      return;
    }

    ViewSupplier supplier = suppliers.get(BeeUtils.trim(key));
    if (supplier != null) {
      supplier.create(callback);

    } else {
      Pair<SupplierKind, String> pair = SupplierKind.parse(key);

      if (pair == null) {
        callback.onFailure("view supplier not found:", key);
      } else {
        pair.getA().create(pair.getB(), callback);
      }
    }
  }

  public static void createAndShow(String key) {
    Assert.notEmpty(key);

    create(key, new ViewCallback() {
      @Override
      public void onSuccess(View result) {
        BeeKeeper.getScreen().show(result);
      }
    });
  }

  public static Collection<String> getKeys() {
    return new TreeSet<>(suppliers.keySet());
  }

  public static PresenterCallback getPresenterCallback(final ViewCallback callback) {
    return new PresenterCallback() {
      @Override
      public void onCreate(Presenter presenter) {
        callback.onSuccess(presenter.getMainView());
      }
    };
  }

  public static ViewSupplier getSupplier(String key) {
    if (BeeUtils.isEmpty(key)) {
      return null;
    } else {
      return suppliers.get(BeeUtils.trim(key));
    }
  }

  public static boolean hasSupplier(String key) {
    if (BeeUtils.isEmpty(key)) {
      return false;
    } else {
      return suppliers.containsKey(BeeUtils.trim(key));
    }
  }

  public static void registerSupplier(String key, ViewSupplier supplier) {
    if (!BeeUtils.isEmpty(key) && supplier != null && !hasSupplier(key)) {
      suppliers.put(BeeUtils.trim(key), supplier);
    }
  }

  private ViewFactory() {
  }
}
