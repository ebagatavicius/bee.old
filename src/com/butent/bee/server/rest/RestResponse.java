package com.butent.bee.server.rest;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;

public final class RestResponse {

  public static final String JSON_TYPE = "application/json;charset=UTF-8";

  static final String LAST_SYNC_TIME = "LastSyncTime";
  private static final String SUCCESS = "Success";
  private static final String ERROR_CODE = "ErrorCode";
  private static final String ERROR_MESSAGE = "ErrorMessage";

  private Object result;
  private String error;
  private Long lastSync;

  private String hash;

  private RestResponse() {
  }

  public static RestResponse empty() {
    return ok(null);
  }

  public static RestResponse forbidden() {
    return error(Localized.dictionary().actionNotAllowed());
  }

  public static RestResponse error(Throwable ex) {
    return error(BeeUtils.notEmpty(ex.getLocalizedMessage(), ex.toString()));
  }

  public static RestResponse error(String... errorMessages) {
    return new RestResponse().setError(ArrayUtils.joinWords(errorMessages));
  }

  public static RestResponse ok(Object result) {
    return new RestResponse().setResult(result);
  }

  @XmlAttribute(name = "Hash")
  public String getHash() {
    return hash;
  }

  @XmlAttribute(name = "Result")
  public Object getResult() {
    return BeeUtils.nvl(result, new Object[0]);
  }

  @XmlAttribute(name = "Status")
  public Map<String, Object> getStatus() {
    Map<String, Object> status = new HashMap<>();

    if (BeeUtils.isEmpty(error)) {
      status.put(SUCCESS, 1);
    } else {
      status.put(SUCCESS, 0);
      status.put(ERROR_CODE, 1);
      status.put(ERROR_MESSAGE, error);
    }
    if (Objects.nonNull(lastSync)) {
      status.put(LAST_SYNC_TIME, lastSync);
    }
    return status;
  }

  public boolean hasError() {
    return !BeeUtils.toBoolean(getStatus().get(SUCCESS).toString());
  }

  public RestResponse setError(String errorMessage) {
    this.error = errorMessage;
    return this;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public RestResponse setLastSync(Long last) {
    this.lastSync = last;
    return this;
  }

  public RestResponse setResult(Object obj) {
    this.result = obj;
    return this;
  }
}
