package com.butent.bee.client.communication;

import com.google.gwt.http.client.RequestBuilder;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.RpcParameter.Section;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains and manages RPC parameter lists for individual requests.
 */
@SuppressWarnings("serial")
public class ParameterList extends ArrayList<RpcParameter> {

  private static final BeeLogger logger = LogUtils.getLogger(ParameterList.class);

  private final String service;

  private boolean ready;
  private List<RpcParameter> dataItems;
  private List<RpcParameter> headerItems;
  private List<RpcParameter> queryItems;

  private String summary;

  public ParameterList(String svc) {
    super();
    this.service = svc;

    addQueryItem(Service.RPC_VAR_SVC, svc);

    String opt = BeeKeeper.getRpc().getOptions();
    if (!BeeUtils.isEmpty(opt)) {
      addHeaderItem(Service.RPC_VAR_OPT, opt);
    }
  }

  public ParameterList(String svc, Section section, Collection<Property> items) {
    this(svc);
    Assert.notNull(section);

    switch (section) {
      case DATA:
        addDataItems(items);
        break;
      case HEADER:
        addHeaderItems(items);
        break;
      case QUERY:
        addQueryItems(items);
        break;
    }
  }

  public void addDataItem(String name, int value) {
    addDataItem(name, BeeUtils.toString(value));
  }

  public void addDataItem(String name, long value) {
    addDataItem(name, BeeUtils.toString(value));
  }

  public void addDataItem(String name, double value) {
    addDataItem(name, BeeUtils.toString(value));
  }

  public void addDataItem(String name, boolean value) {
    addDataItem(name, BeeUtils.toString(value));
  }

  public void addDataItem(String name, String value) {
    addItem(new RpcParameter(Section.DATA, name, value));
  }

  public void addDataItems(Collection<Property> items) {
    if (items != null) {
      for (Property p : items) {
        addDataItem(p.getName(), p.getValue());
      }
    }
  }

  public void addHeaderItem(String name, String value) {
    addItem(new RpcParameter(Section.HEADER, name, value));
  }

  public void addHeaderItems(Collection<Property> items) {
    if (items != null) {
      for (Property p : items) {
        addHeaderItem(p.getName(), p.getValue());
      }
    }
  }

  public void addNotEmptyData(String name, String value) {
    if (!BeeUtils.anyEmpty(name, value)) {
      addDataItem(name, value);
    }
  }

  public void addNotEmptyQuery(String name, String value) {
    if (!BeeUtils.anyEmpty(name, value)) {
      addQueryItem(name, value);
    }
  }

  public void addNotNullData(String name, Integer value) {
    if (!BeeUtils.isEmpty(name) && value != null) {
      addDataItem(name, value);
    }
  }

  public void addPositionalData(String first, String... rest) {
    addPositionalItem(Section.DATA, first);
    if (rest != null) {
      for (String value : rest) {
        addPositionalItem(Section.DATA, value);
      }
    }
  }

  public void addPositionalHeader(String first, String... rest) {
    addPositionalItem(Section.HEADER, first);
    if (rest != null) {
      for (String value : rest) {
        addPositionalItem(Section.HEADER, value);
      }
    }
  }

  public void addPositionalQuery(String first, String... rest) {
    addPositionalItem(Section.QUERY, first);
    if (rest != null) {
      for (String value : rest) {
        addPositionalItem(Section.QUERY, value);
      }
    }
  }

  public void addQueryItem(String name, int value) {
    addQueryItem(name, BeeUtils.toString(value));
  }

  public void addQueryItem(String name, long value) {
    addQueryItem(name, BeeUtils.toString(value));
  }

  public void addQueryItem(String name, String value) {
    addItem(new RpcParameter(Section.QUERY, name, value));
  }

  public void addQueryItems(Collection<Property> items) {
    if (items != null) {
      for (Property p : items) {
        addQueryItem(p.getName(), p.getValue());
      }
    }
  }

  public ContentType getContentType() {
    ContentType ctp = CommUtils.getContentType(getParameter(Service.RPC_VAR_CTP));

    if (ctp == null) {
      prepare();
      ctp = BeeUtils.isEmpty(dataItems) ? null : ContentType.BINARY;
    }
    return ctp;
  }

  public String getData() {
    prepare();
    if (BeeUtils.isEmpty(dataItems)) {
      return null;
    }
    Map<String, String> data = new LinkedHashMap<>();

    for (RpcParameter item : dataItems) {
      data.put(item.getName(), item.getValue());
    }
    return Codec.beeSerialize(data);
  }

  public void getHeadersExcept(RequestBuilder bld, String... ignore) {
    Assert.notNull(bld);
    prepare();
    if (BeeUtils.isEmpty(headerItems)) {
      return;
    }

    int n = (ignore == null) ? 0 : ignore.length;

    for (RpcParameter item : headerItems) {
      if (item.isReady()) {
        if (n > 0 && ArrayUtils.containsSame(ignore, item.getName())) {
          continue;
        }
        bld.setHeader(item.getName(), item.getValue());
      }
    }
  }

  public String getParameter(String name) {
    Assert.notEmpty(name);
    String value = null;

    for (RpcParameter item : this) {
      if (BeeUtils.same(item.getName(), name)) {
        value = item.getValue();
        break;
      }
    }
    return value;
  }

  public String getQuery() {
    prepare();
    if (BeeUtils.isEmpty(queryItems)) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();

    for (RpcParameter item : queryItems) {
      if (item.isReady()) {
        if (sb.length() > 0) {
          sb.append(CommUtils.QUERY_STRING_PAIR_SEPARATOR);
        }

        sb.append(item.getName().trim());
        sb.append(CommUtils.QUERY_STRING_VALUE_SEPARATOR);
        sb.append(item.getValue().trim());
      }
    }
    return sb.toString();
  }

  public String getService() {
    return service;
  }

  public String getSubService() {
    return getParameter(Service.RPC_VAR_SUB);
  }

  public String getSummary() {
    return summary;
  }

  public boolean hasData() {
    for (RpcParameter item : this) {
      if (item.getSection() == Section.DATA) {
        return true;
      }
    }
    return false;
  }

  public boolean hasParameter(String name) {
    Assert.notEmpty(name);
    boolean ok = false;

    for (RpcParameter item : this) {
      if (BeeUtils.same(item.getName(), name)) {
        ok = true;
        break;
      }
    }
    return ok;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public void setSummary(Object first, Object second, Object... rest) {
    setSummary(BeeUtils.joinWords(first, second, rest));
  }

  private void addItem(RpcParameter item) {
    if (item.isValid()) {
      add(item);
    } else {
      logger.severe("Invalid rpc parameter:", item);
    }
  }

  private void addPositionalItem(Section section, String value) {
    addItem(new RpcParameter(section, null, value));
  }

  private void prepare() {
    if (ready) {
      return;
    }

    dataItems = new ArrayList<>();
    headerItems = new ArrayList<>();
    queryItems = new ArrayList<>();
    ready = true;

    if (isEmpty()) {
      return;
    }

    int n = 0;
    for (RpcParameter item : this) {
      if (!item.isValid()) {
        continue;
      }
      if (!item.isNamed()) {
        item.setName(CommUtils.rpcParamName(n));
        n++;
      }

      switch (item.getSection()) {
        case DATA:
          dataItems.add(item);
          break;
        case HEADER:
          headerItems.add(item);
          break;
        case QUERY:
          queryItems.add(item);
          break;
      }
    }
    if (n > 0) {
      queryItems.add(new RpcParameter(Section.QUERY, Service.RPC_VAR_PRM_CNT,
          BeeUtils.toString(n)));
    }
  }
}
