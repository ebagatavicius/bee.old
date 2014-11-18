package com.butent.bee.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;

import com.butent.bee.client.communication.AsyncCallback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcInfo;
import com.butent.bee.client.communication.RpcList;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * enables to generate and manage remote procedure calls, GET and POST statements.
 */

public class RpcFactory {

  private static final BeeLogger logger = LogUtils.getLogger(RpcFactory.class);

  private final String rpcUrl;

  private final RpcList rpcList = new RpcList();
  private final AsyncCallback reqCallBack = new AsyncCallback();

  public RpcFactory() {
    this.rpcUrl = GWT.getHostPageBaseURL() + GWT.getModuleName();
  }

  public boolean cancelRequest(int id) {
    RpcInfo info = getRpcInfo(id);
    if (info == null) {
      return false;
    }

    Set<State> states = info.getStates();
    boolean ok = info.cancel();

    if (ok) {
      logger.info("request", id, "canceled");
      logger.addSeparator();
    } else {
      logger.warning("request", id, "is not pendind");
      if (states != null) {
        logger.debug("States:", states);
      }
    }

    return ok;
  }

  public ParameterList createParameters(String svc) {
    Assert.notEmpty(svc);
    return new ParameterList(svc);
  }

  public ParameterList createParameters(Module module, String method) {
    Assert.notNull(module);
    Assert.notEmpty(method);

    ParameterList params = createParameters(module.getName());
    params.addQueryItem(Service.VAR_METHOD, method);

    return params;
  }

  public ParameterList createParameters(Module module, SubModule subModule, String method) {
    ParameterList params = createParameters(module, method);

    if (subModule != null) {
      params.addQueryItem(Service.VAR_SUB_MODULE, subModule.getName());
    }

    return params;
  }

  public String getOptions() {
    if (Global.isDebug()) {
      return CommUtils.OPTION_DEBUG;
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public List<RpcInfo> getPendingRequests() {
    List<RpcInfo> result = new ArrayList<>();

    for (RpcInfo info : rpcList.values()) {
      if (info != null && info.isPending()) {
        result.add(info);
      }
    }
    return result;
  }

  public AsyncCallback getReqCallBack() {
    return reqCallBack;
  }

  public RpcInfo getRpcInfo(int id) {
    return rpcList.get(id);
  }

  public RpcList getRpcList() {
    return rpcList;
  }

  public boolean hasPendingRequests() {
    for (RpcInfo info : rpcList.values()) {
      if (info != null && info.isPending()) {
        return true;
      }
    }
    return false;
  }

  public int invoke(String method, ResponseCallback callback) {
    return invoke(method, null, null, callback);
  }

  public int invoke(String method, ContentType ctp, String data, ResponseCallback callback) {
    Assert.notEmpty(method);

    ParameterList params = createParameters(Service.INVOKE);
    params.addQueryItem(Service.VAR_METHOD, method);

    if (data == null) {
      return makeGetRequest(params, callback);
    } else {
      return makePostRequest(params, ctp, data, callback);
    }
  }

  public int invoke(String method, String data, ResponseCallback callback) {
    return invoke(method, ContentType.TEXT, data, callback);
  }

  public int makeGetRequest(ParameterList params) {
    return makeRequest(RequestBuilder.GET, params, null, null, null, BeeConst.UNDEF);
  }

  public int makeGetRequest(ParameterList params, ResponseCallback callback) {
    return makeRequest(RequestBuilder.GET, params, null, null, callback, BeeConst.UNDEF);
  }

  public int makeGetRequest(ParameterList params, ResponseCallback callback, int timeout) {
    return makeRequest(RequestBuilder.GET, params, null, null, callback, timeout);
  }

  public int makeGetRequest(String svc) {
    return makeRequest(RequestBuilder.GET, createParameters(svc), null, null, null,
        BeeConst.UNDEF);
  }

  public int makeGetRequest(String svc, ResponseCallback callback) {
    return makeRequest(RequestBuilder.GET, createParameters(svc), null, null,
        callback, BeeConst.UNDEF);
  }

  public int makeGetRequest(String svc, ResponseCallback callback, int timeout) {
    return makeRequest(RequestBuilder.GET, createParameters(svc), null, null, callback, timeout);
  }

  public int makePostRequest(ParameterList params, ContentType ctp, String data) {
    return makeRequest(RequestBuilder.POST, params, ctp, data, null, BeeConst.UNDEF);
  }

  public int makePostRequest(ParameterList params, ContentType ctp,
      String data, ResponseCallback callback) {
    return makeRequest(RequestBuilder.POST, params, ctp, data, callback, BeeConst.UNDEF);
  }

  public int makePostRequest(ParameterList params, ContentType ctp,
      String data, ResponseCallback callback, int timeout) {
    return makeRequest(RequestBuilder.POST, params, ctp, data, callback, timeout);
  }

  public int makePostRequest(ParameterList params, ResponseCallback callback) {
    return makeRequest(RequestBuilder.POST, params, null, null, callback, BeeConst.UNDEF);
  }

  public int makePostRequest(ParameterList params, String data) {
    return makeRequest(RequestBuilder.POST, params, null, data, null, BeeConst.UNDEF);
  }

  public int makePostRequest(ParameterList params, String data, ResponseCallback callback) {
    return makeRequest(RequestBuilder.POST, params, null, data, callback, BeeConst.UNDEF);
  }

  public int makePostRequest(ParameterList params, String data, ResponseCallback callback,
      int timeout) {
    return makeRequest(RequestBuilder.POST, params, null, data, callback, timeout);
  }

  public int makePostRequest(String svc, ContentType ctp, String data) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), ctp, data,
        null, BeeConst.UNDEF);
  }

  public int makePostRequest(String svc, ContentType ctp, String data, ResponseCallback callback) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), ctp, data,
        callback, BeeConst.UNDEF);
  }

  public int makePostRequest(String svc, ContentType ctp, String data,
      ResponseCallback callback, int timeout) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), ctp, data, callback, timeout);
  }

  public int makePostRequest(String svc, String data) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), null, data,
        null, BeeConst.UNDEF);
  }

  public int makePostRequest(String svc, String data, ResponseCallback callback) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), null, data,
        callback, BeeConst.UNDEF);
  }

  public int makePostRequest(String svc, String data, ResponseCallback callback, int timeout) {
    return makeRequest(RequestBuilder.POST, createParameters(svc), null, data, callback, timeout);
  }

  public int makeRequest(ParameterList params) {
    return makeRequest(params, null);
  }

  public int makeRequest(ParameterList params, ResponseCallback callback) {
    RequestBuilder.Method meth = params.hasData() ? RequestBuilder.POST : RequestBuilder.GET;
    return makeRequest(meth, params, null, null, callback, BeeConst.UNDEF);
  }

  public int sendText(ParameterList params, String data, ResponseCallback callback) {
    return makePostRequest(params, ContentType.TEXT, data, callback);
  }

  public int sendText(String svc, String data) {
    return makePostRequest(svc, ContentType.TEXT, data);
  }

  public int sendText(String svc, String data, ResponseCallback callback) {
    return makePostRequest(svc, ContentType.TEXT, data, callback);
  }

  private int makeRequest(RequestBuilder.Method meth, ParameterList params,
      ContentType type, String reqData, ResponseCallback callback, int timeout) {
    Assert.notNull(meth);
    Assert.notNull(params);

    String svc = params.getService();
    Assert.notEmpty(svc);

    boolean debug = Global.isDebug();

    ContentType ctp = type;
    String data;
    if (BeeUtils.isEmpty(reqData)) {
      data = params.getData();
    } else {
      data = reqData;
    }

    if (BeeUtils.isEmpty(data)) {
      data = null;
      ctp = null;
    } else if (ctp == null) {
      ctp = CommUtils.normalizeRequest(params.getContentType());
    }

    RpcInfo info = new RpcInfo(meth, svc, params, ctp, data, callback);
    int id = info.getId();

    String qs = params.getQuery();
    String url = CommUtils.addQueryString(rpcUrl, qs);

    RequestBuilder bld = new RequestBuilder(meth, url);
    if (timeout > 0) {
      bld.setTimeoutMillis(timeout);
      info.setTimeout(timeout);
    }

    String sid = BeeKeeper.getUser().getSessionId();
    if (!BeeUtils.isEmpty(sid)) {
      bld.setHeader(Service.RPC_VAR_SID, sid);
    }
    bld.setHeader(Service.RPC_VAR_QID, BeeUtils.toString(id));
    String cth = null;

    if (ctp != null) {
      bld.setHeader(Service.RPC_VAR_CTP, ctp.name());

      String z = params.getParameter(CommUtils.CONTENT_TYPE_HEADER);
      if (BeeUtils.isEmpty(z)) {
        cth = CommUtils.buildContentType(CommUtils.getMediaType(ctp),
            CommUtils.getCharacterEncoding(ctp));
      } else {
        cth = z;
      }
      bld.setHeader(CommUtils.CONTENT_TYPE_HEADER, cth);
    }

    params.getHeadersExcept(bld, Service.RPC_VAR_QID,
        Service.RPC_VAR_CTP, CommUtils.CONTENT_TYPE_HEADER);

    if (debug) {
      logger.info("request", id, meth.toString(), url);
    } else {
      logger.info(">", id, svc);
    }

    String content = null;

    if (data != null) {
      content = CommUtils.prepareContent(ctp, data);
      int size = content.length();
      info.setReqSize(size);

      if (debug) {
        logger.info("sending", ctp, cth, BeeUtils.bracket(size));
        logger.info(BeeUtils.clip(data, 1024));
      }
      if (meth.equals(RequestBuilder.GET)) {
        logger.severe(meth, "data is ignored");
        if (!debug) {
          logger.debug(BeeUtils.clip(data, 1024));
        }
      }
    }

    try {
      Request request = bld.sendRequest(content, reqCallBack);
      info.setRequest(request);
      info.setState(State.OPEN);
    } catch (RequestException ex) {
      info.endError(ex);
      logger.severe("send request error", id, ex);
    }

    rpcList.put(id, info);
    return id;
  }
}
