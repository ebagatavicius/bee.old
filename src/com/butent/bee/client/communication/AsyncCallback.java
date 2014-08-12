package com.butent.bee.client.communication;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.Bee;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.utils.Duration;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;

/**
 * Manages responses to RPC calls on the client side.
 */

public class AsyncCallback implements RequestCallback {

  private static final BeeLogger logger = LogUtils.getLogger(AsyncCallback.class);

  public AsyncCallback() {
  }

  @Override
  public void onError(Request req, Throwable ex) {
    String msg = (ex instanceof RequestTimeoutException) ? "request timeout" : "request failure";
    logger.severe(msg, ex);
  }

  @Override
  public void onResponseReceived(Request req, Response resp) {
    String qid = resp.getHeader(Service.RPC_VAR_QID);
    if (qid == null) {
      BeeKeeper.getBus().removeExitHandler();
      Window.Location.reload();
      return;
    }

    int id = BeeUtils.toInt(qid);
    RpcInfo info = BeeKeeper.getRpc().getRpcInfo(id);

    if (!Bee.isEnabled()) {
      if (info != null) {
        info.done();
        info.setState(State.CLOSED);
      }
      return;
    }

    if (info == null) {
      logger.warning("Rpc info not available");
    } else if (info.isCanceled()) {
      info.done();
      logger.debug("<", qid, "canceled");
      finalizeResponse();
      return;
    }

    String svc = (info == null) ? BeeConst.STRING_EMPTY : info.getService();
    if (BeeUtils.isEmpty(svc)) {
      logger.warning("Rpc service", BeeUtils.bracket(Service.RPC_VAR_SVC), "not available");
    }

    int statusCode = resp.getStatusCode();
    if (statusCode != Response.SC_OK) {
      String msg = BeeUtils.joinWords(NameUtils.addName(Service.RPC_VAR_QID, id),
          NameUtils.addName(Service.RPC_VAR_SVC, svc));
      if (!BeeUtils.isEmpty(msg)) {
        logger.severe(msg);
      }

      msg = BeeUtils.joinWords(BeeUtils.bracket(statusCode), resp.getStatusText());
      logger.severe("response status", msg);

      if (info != null) {
        info.endError(msg);
      }
      finalizeResponse();
      return;
    }

    String sid = resp.getHeader(Service.RPC_VAR_SID);
    if (!BeeUtils.isEmpty(sid)) {
      BeeKeeper.getUser().setSessionId(sid);
    }

    ContentType ctp = CommUtils.getContentType(resp.getHeader(Service.RPC_VAR_CTP));

    String txt = CommUtils.getContent(ctp, resp.getText());
    int len = BeeUtils.length(txt);

    if (Global.isDebug()) {
      logger.info("response", NameUtils.addName(Service.RPC_VAR_QID, id),
          NameUtils.addName(Service.RPC_VAR_SVC, svc));
      logger.info(NameUtils.addName(Service.RPC_VAR_CTP, BeeUtils.toString(ctp)),
          NameUtils.addName("len", len));

      Header[] headers = resp.getHeaders();
      for (int i = 0; i < headers.length; i++) {
        logger.info("Header", i + 1, headers[i].getName(), headers[i].getValue());
      }

      if (info != null) {
        info.setRespInfo(RpcUtils.responseInfo(resp));
      }
    }

    if (len <= 0) {
      String msg = "response is empty";
      if (info != null) {
        info.endError(msg);
      }

      logger.warning(svc, "msg");
      finalizeResponse();
      return;
    }

    ResponseObject response = ResponseObject.restore(txt);

    Collection<ResponseMessage> messages = response.getMessages();
    if (info != null) {
      info.end(ctp, txt, len, messages);
    }

    Duration duration = new Duration();

    RpcUtils.dispatchMessages(response);

    if (info != null) {
      ResponseCallback callback = info.getRespCallback();
      if (callback != null) {
        callback.onResponse(response);
      }
    }

    duration.finish();

    logger.info("<", id, len, (info == null) ? null : BeeUtils.bracket(info.getCompletedTime()),
        BeeUtils.bracket(duration.getCompletedTime()));
    finalizeResponse();
  }

  private static void finalizeResponse() {
    logger.addSeparator();
  }
}
