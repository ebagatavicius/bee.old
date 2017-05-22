package com.butent.bee.shared.communication;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Used to transport data with messages between various layers.
 */
public class ResponseObject implements BeeSerializable {

  /**
   * Contains a list of serializable members of response object.
   */

  private enum Serial {
    MESSAGES, RESPONSE_TYPE, ARRAY_TYPE, RESPONSE, SIZE
  }

  public static <T> ResponseObject collection(Collection<T> response, Class<T> clazz) {
    return new ResponseObject().setCollection(response, clazz);
  }

  public static ResponseObject emptyResponse() {
    return new ResponseObject();
  }

  public static ResponseObject error(Object... err) {
    return new ResponseObject().addError(err);
  }

  public static ResponseObject error(Throwable err, Object... msgs) {
    return new ResponseObject().addError(err, msgs);
  }

  public static ResponseObject info(Object... obj) {
    return new ResponseObject().addInfo(obj);
  }

  public static ResponseObject parameterNotFound(String paramName) {
    return new ResponseObject().addError("parameter not found:", paramName);
  }

  public static ResponseObject parameterNotFound(String service, String paramName) {
    return new ResponseObject().addError(service, "parameter not found:", paramName);
  }

  public static ResponseObject response(Object response) {
    return new ResponseObject().setResponse(response);
  }

  public static ResponseObject response(Object response, Class<?> clazz) {
    return new ResponseObject().setResponse(response).setType(clazz);
  }

  public static ResponseObject responseWithSize(Collection<?> response) {
    return new ResponseObject().setResponse(response).setSize(BeeUtils.size(response));
  }

  public static ResponseObject restore(String s) {
    ResponseObject response = new ResponseObject();
    response.deserialize(s);
    return response;
  }

  public static ResponseObject warning(Object... obj) {
    return new ResponseObject().addWarning(obj);
  }

  private final Collection<ResponseMessage> messages = new ArrayList<>();
  private Object response;
  private String type;
  private boolean isArrayType;

  private int size;

  public ResponseObject addDebug(Object... obj) {
    messages.add(new ResponseMessage(LogLevel.DEBUG, ArrayUtils.joinWords(obj)));
    return this;
  }

  public ResponseObject addError(Object... err) {
    messages.add(new ResponseMessage(LogLevel.ERROR, ArrayUtils.joinWords(err)));
    return this;
  }

  public ResponseObject addError(Throwable err, Object... msgs) {
    Throwable cause = err;

    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    addError(BeeUtils.joinWords(ArrayUtils.joinWords(msgs), cause.toString()));
    return this;
  }

  public ResponseObject addErrorsFrom(ResponseObject other) {
    for (String err : other.getErrors()) {
      addError(err);
    }
    return this;
  }

  public ResponseObject addInfo(Object... obj) {
    messages.add(new ResponseMessage(LogLevel.INFO, ArrayUtils.joinWords(obj)));
    return this;
  }

  public ResponseObject addMessages(Collection<ResponseMessage> msgs) {
    if (!BeeUtils.isEmpty(msgs)) {
      messages.addAll(msgs);
    }
    return this;
  }

  public ResponseObject addMessagesFrom(ResponseObject other) {
    if (other.hasMessages()) {
      addMessages(other.getMessages());
    }
    return this;
  }

  public ResponseObject addWarning(Object... obj) {
    messages.add(new ResponseMessage(LogLevel.WARNING, ArrayUtils.joinWords(obj)));
    return this;
  }

  public ResponseObject clearMessages() {
    messages.clear();
    return this;
  }

  @Override
  public void deserialize(String s) {
    Serial[] members = Serial.values();
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case MESSAGES:
          messages.clear();
          String[] cArr = Codec.beeDeserializeCollection(value);

          if (cArr != null) {
            for (String msg : cArr) {
              messages.add(new ResponseMessage(msg, true));
            }
          }
          break;

        case RESPONSE_TYPE:
          this.type = value;
          break;

        case ARRAY_TYPE:
          this.isArrayType = BeeUtils.toBoolean(value);
          break;

        case RESPONSE:
          this.response = value;
          break;

        case SIZE:
          this.size = BeeUtils.toInt(value);
          break;
      }
    }
  }

  public String[] getErrors() {
    return getMessageArray(LogLevel.ERROR);
  }

  public Collection<ResponseMessage> getMessages() {
    return messages;
  }

  public String[] getNotifications() {
    return getMessageArray(LogLevel.INFO);
  }

  public Object getResponse() {
    return response;
  }

  @SuppressWarnings("unchecked")
  public <T> T getResponse(T def, BeeLogger logger) {
    Assert.notNull(def);
    T res = def;

    if (!hasErrors()) {
      if (hasResponse(def.getClass())) {
        res = (T) getResponse();

      } else if (logger != null) {
        logger.warning("Requested response type:", BeeUtils.bracket(def.getClass().toString()),
            "got:", BeeUtils.bracket(getResponse().getClass().toString()));
      }
    }
    log(logger);

    return res;
  }

  public Integer getResponseAsInt() {
    if (getResponse() instanceof Integer) {
      return (Integer) getResponse();
    } else if (getResponse() instanceof String) {
      return BeeUtils.toIntOrNull(getResponseAsString());
    } else {
      return null;
    }
  }

  public Long getResponseAsLong() {
    if (getResponse() instanceof Long) {
      return (Long) getResponse();
    } else if (getResponse() instanceof String) {
      return BeeUtils.toLongOrNull(getResponseAsString());
    } else {
      return null;
    }
  }

  public String getResponseAsString() {
    return (String) getResponse();
  }

  @SuppressWarnings("unchecked")
  public Collection<String> getResponseAsStringCollection() {
    return (Collection<String>) getResponse();
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getResponseAsStringMap() {
    return (Map<String, String>) getResponse();
  }

  public int getSize() {
    return size;
  }

  public String getType() {
    return type;
  }

  public String[] getWarnings() {
    return getMessageArray(LogLevel.WARNING);
  }

  public boolean hasArrayResponse(Class<?> clazz) {
    return hasResponse(clazz, true);
  }

  public boolean hasErrors() {
    return hasMessages(LogLevel.ERROR);
  }

  public boolean hasMessages() {
    return !messages.isEmpty();
  }

  public boolean hasNotifications() {
    return hasMessages(LogLevel.INFO);
  }

  public boolean hasResponse() {
    return response != null;
  }

  public boolean hasResponse(Class<?> clazz) {
    return hasResponse(clazz, false);
  }

  public boolean hasWarnings() {
    return hasMessages(LogLevel.WARNING);
  }

  public boolean is(String value) {
    return hasResponse(String.class) && BeeUtils.equalsTrim(getResponseAsString(), value);
  }

  public boolean isArrayType() {
    return isArrayType;
  }

  public boolean isEmpty() {
    return !hasMessages(null) && !hasResponse();
  }

  public void log(BeeLogger logger) {
    if (logger != null && hasMessages()) {
      for (ResponseMessage message : getMessages()) {
        switch (message.getLevel()) {
          case DEBUG:
            logger.debug(message.getMessage());
            break;
          case ERROR:
            logger.severe(message.getMessage());
            break;
          case INFO:
            logger.info(message.getMessage());
            break;
          case WARNING:
            logger.warning(message.getMessage());
            break;
        }
      }
    }
  }

  public void notify(NotificationListener notificator) {
    if (notificator != null && hasMessages()) {
      LogLevel level = null;
      List<String> list = new ArrayList<>();

      for (ResponseMessage message : getMessages()) {
        if (!BeeUtils.isEmpty(message.getMessage())) {
          level = BeeUtils.max(level, message.getLevel());
          list.add(message.getMessage());
        }
      }

      if (!list.isEmpty()) {
        if (level == LogLevel.ERROR) {
          notificator.notifySevere(ArrayUtils.toArray(list));
        } else if (level == LogLevel.WARNING) {
          notificator.notifyWarning(ArrayUtils.toArray(list));
        } else {
          notificator.notifyInfo(ArrayUtils.toArray(list));
        }
      }
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case MESSAGES:
          arr[i++] = messages;
          break;

        case RESPONSE_TYPE:
          arr[i++] = type;
          break;

        case ARRAY_TYPE:
          arr[i++] = isArrayType;
          break;

        case RESPONSE:
          arr[i++] = response;
          break;

        case SIZE:
          arr[i++] = size;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public <T> ResponseObject setCollection(Collection<T> rsp, Class<T> clazz) {
    Assert.notNull(clazz);

    this.response = rsp;
    this.type = NameUtils.getClassName(clazz);
    this.isArrayType = true;

    return this;
  }

  public ResponseObject setResponse(Object rsp) {
    if (rsp != null) {
      setType(rsp.getClass());
    }
    this.response = rsp;
    return this;
  }

  public ResponseObject setSize(int z) {
    this.size = z;
    return this;
  }

  private String[] getMessageArray(LogLevel lvl) {
    List<String> msgs = new ArrayList<>();

    for (ResponseMessage message : messages) {
      if (Objects.equals(message.getLevel(), lvl)) {
        msgs.add(message.getMessage());
      }
    }
    return ArrayUtils.toArray(msgs);
  }

  private boolean hasMessages(LogLevel lvl) {
    for (ResponseMessage message : messages) {
      if (lvl == null || Objects.equals(message.getLevel(), lvl)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasResponse(Class<?> clazz, boolean isArray) {
    boolean ok = response != null;

    if (ok && clazz != null) {
      ok = (isArrayType() == isArray) && BeeUtils.same(getType(), NameUtils.getClassName(clazz));
    }
    return ok;
  }

  private ResponseObject setType(Class<?> clazz) {
    Assert.notNull(clazz);

    this.isArrayType = clazz.isArray();
    Class<?> cls;

    if (isArrayType) {
      cls = clazz.getComponentType();
    } else {
      cls = clazz;
    }

    this.type = NameUtils.getClassName(cls);
    return this;
  }
}
