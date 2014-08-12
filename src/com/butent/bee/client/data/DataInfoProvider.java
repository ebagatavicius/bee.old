package com.butent.bee.client.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.view.ColumnNamesProvider;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DataInfoProvider implements DataInfo.Provider, ColumnNamesProvider {

  private static final BeeLogger logger = LogUtils.getLogger(DataInfoProvider.class);

  private final Map<String, DataInfo> views = Maps.newHashMap();

  public DataInfoProvider() {
    super();
  }

  @Override
  public ImmutableList<String> getColumnNames(String viewName) {
    DataInfo dataInfo = getDataInfo(viewName, true);
    return (dataInfo == null) ? null : ImmutableList.copyOf(dataInfo.getColumnNames(false));
  }

  @Override
  public DataInfo getDataInfo(String viewName, boolean warn) {
    DataInfo dataInfo = views.get(BeeUtils.normalize(viewName));
    if (dataInfo == null && warn) {
      logger.severe("view", viewName, "data info not found");
    }
    return dataInfo;
  }

  public Collection<String> getViewNames(String tableName) {
    Assert.notEmpty(tableName);

    Set<String> viewNames = Sets.newHashSet();
    for (DataInfo dataInfo : views.values()) {
      if (BeeUtils.same(dataInfo.getTableName(), tableName)) {
        viewNames.add(dataInfo.getViewName());
      }
    }

    return viewNames;
  }

  public Collection<DataInfo> getViews() {
    return views.values();
  }

  public void load() {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_DATA_INFO, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);
        restore((String) response.getResponse());
      }
    });
  }

  public void restore(String serialized) {
    String[] arr = Codec.beeDeserializeCollection(serialized);

    if (arr == null) {
      logger.severe("cannot restore data info");

    } else {
      views.clear();

      for (String s : arr) {
        DataInfo dataInfo = DataInfo.restore(s);
        if (dataInfo != null) {
          views.put(BeeUtils.normalize(dataInfo.getViewName()), dataInfo);
        }
      }

      logger.info("data info", views.size());
    }
  }
}
