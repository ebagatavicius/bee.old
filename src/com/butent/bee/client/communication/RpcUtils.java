package com.butent.bee.client.communication;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;

import elemental.client.Browser;
import elemental.xml.XMLHttpRequest;

/**
 * Contains utility functions for working with remote procedure calls.
 */

public final class RpcUtils {

  private static final BeeLogger logger = LogUtils.getLogger(RpcUtils.class);

  public static void addSessionId(XMLHttpRequest xhr) {
    Assert.notNull(xhr);
    String sid = BeeKeeper.getUser().getSessionId();
    if (!BeeUtils.isEmpty(sid)) {
      xhr.setRequestHeader(Service.RPC_VAR_SID, sid);
    }
  }

  public static XMLHttpRequest createXhr() {
    return Browser.getWindow().newXMLHttpRequest();
  }

  public static void dispatchMessages(ResponseObject responseObject) {
    if (responseObject != null && responseObject.hasMessages()) {
      dispatchMessages(responseObject.getMessages());
    }
  }

  public static void dispatchMessages(Collection<ResponseMessage> messages) {
    if (!BeeUtils.isEmpty(messages)) {
      for (ResponseMessage message : messages) {
        LogLevel level = message.getLevel();

        DateTime date = message.getDate();
        String msg;

        if (date == null) {
          msg = message.getMessage();
        } else {
          msg = BeeUtils.joinWords(date.toTimeString(), message.getMessage());
        }
        if (level == null) {
          logger.info(msg);
        } else {
          switch (level) {
            case DEBUG:
              logger.debug(msg);
              break;
            case ERROR:
              logger.severe(msg);
              break;
            case INFO:
              logger.info(msg);
              break;
            case WARNING:
              logger.warning(msg);
              break;
          }
        }
      }
    }
  }

  public static Collection<Property> requestInfo(RequestBuilder rb) {
    Assert.notNull(rb);
    Collection<Property> prp = new ArrayList<>();

    PropertyUtils.addProperties(prp, "Url", rb.getUrl(),
        "Http Method", rb.getHTTPMethod(),
        "Request Data", rb.getRequestData(),
        "Password", rb.getPassword(),
        "User", rb.getUser(),
        "Timeout", rb.getTimeoutMillis());
    return prp;
  }

  public static Collection<ExtendedProperty> responseInfo(Response resp) {
    Assert.notNull(resp);

    Collection<ExtendedProperty> prp = new ArrayList<>();

    PropertyUtils.addExtended(prp, "Status",
        NameUtils.addName("Code", resp.getStatusCode()), resp.getStatusText());

    Header[] h = resp.getHeaders();
    int c = h.length;

    if (c > 0) {
      PropertyUtils.addExtended(prp, "Headers", NameUtils.addName("Cnt", c),
          NameUtils.addName("Length", resp.getHeadersAsString().length()));

      for (int i = 0; i < c; i++) {
        PropertyUtils.addExtended(prp, "Header", h[i].getName(), h[i].getValue());
      }
    }
    return prp;
  }

  private RpcUtils() {
  }
}
