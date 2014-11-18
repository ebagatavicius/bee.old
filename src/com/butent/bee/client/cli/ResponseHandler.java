package com.butent.bee.client.cli;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.ExtendedPropertiesData;
import com.butent.bee.shared.data.PropertiesData;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ResponseHandler {

  private static final BeeLogger logger = LogUtils.getLogger(ResponseHandler.class);

  static ResponseCallback callback(final String caption) {
    return new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        dispatch(caption, response);
      }
    };
  }

  static void dispatch(String caption, ResponseObject response) {
    if (response == null || !response.hasResponse()) {
      logger.info(caption, "response is empty");

    } else if (response.hasArrayResponse(Property.class)) {
      List<Property> properties =
          PropertyUtils.restoreProperties(response.getResponseAsString());

      if (BeeUtils.isEmpty(properties)) {
        onEmptyResponse(caption);
      } else {
        Global.showTable(caption, new PropertiesData(properties));
      }

    } else if (response.hasArrayResponse(ExtendedProperty.class)) {
      List<ExtendedProperty> extProperties =
          PropertyUtils.restoreExtended(response.getResponseAsString());
      if (BeeUtils.isEmpty(extProperties)) {
        onEmptyResponse(caption);
      } else {
        Global.showTable(caption, new ExtendedPropertiesData(extProperties, false));
      }

    } else if (response.hasResponse(BeeRowSet.class)) {
      BeeRowSet rowSet = BeeRowSet.restore(response.getResponseAsString());

      if (DataUtils.isEmpty(rowSet)) {
        onEmptyResponse(caption);
      } else {
        Global.showTable(caption, rowSet);
      }

    } else if (response.hasResponse(String.class)) {
      if (BeeUtils.isEmpty(response.getResponseAsString())) {
        onEmptyResponse(caption);
      } else {
        logger.debug(caption, response.getResponseAsString());
      }

    } else {
      logger.warning(caption, "response type", response.getType(), "not dispatched");
    }
  }

  static void unicodeTest(String input, ResponseObject response) {
    Assert.notEmpty(input);

    Map<String, String> serverData;

    if (response != null && response.hasResponse()) {
      serverData = Codec.deserializeMap(response.getResponseAsString());
    } else {
      serverData = null;
    }

    if (BeeUtils.isEmpty(serverData)) {
      onEmptyResponse("unicode");
      return;
    }

    String respTxt = serverData.remove("input");
    if (BeeUtils.isEmpty(respTxt)) {
      logger.warning("response data key [input] not found");
      return;
    }

    int reqLen = input.length();
    int respLen = respTxt.length();

    boolean ok = reqLen == respLen && input.equals(respTxt);

    if (!ok) {
      logger.log(reqLen == respLen ? LogLevel.INFO : LogLevel.WARNING,
          "length req", reqLen, "resp", respLen);

      for (int i = 0; i < respLen && i < reqLen; i++) {
        if (input.charAt(i) != respTxt.charAt(i)) {
          logger.warning("charAt", i,
              "req", Integer.toHexString(input.charAt(i)), BeeUtils.bracket(input.charAt(i)),
              "resp", Integer.toHexString(respTxt.charAt(i)), BeeUtils.bracket(respTxt.charAt(i)));
          break;
        }
      }
      return;
    }

    if (reqLen <= 100) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < reqLen; i++) {
        if (i % 10 == 0) {
          if (sb.length() > 0) {
            logger.info(sb);
            sb.setLength(0);
          }
          sb.append(i);
        }

        sb.append(BeeConst.CHAR_SPACE);
        sb.append(Integer.toHexString(input.charAt(i)));
      }

      if (sb.length() > 0) {
        logger.info(sb);
      }
    }

    byte[] bytes = Codec.toBytes(input);

    Map<String, String> clientData = new HashMap<>();
    clientData.put("adler32", Codec.adler32(bytes));
    clientData.put("crc16", Codec.crc16(bytes));
    clientData.put("crc32", Codec.crc32(bytes));
    clientData.put("crc32d", Codec.crc32Direct(bytes));

    String z;

    for (Map.Entry<String, String> entry : serverData.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (clientData.containsKey(key)) {
        z = clientData.get(key);
      } else if (key.contains(BeeConst.STRING_POINT)) {
        z = clientData.get(BeeUtils.getPrefix(key, BeeConst.CHAR_POINT));
      } else {
        z = null;
      }

      if (value.equals(z)) {
        logger.info(key, value);
      } else {
        logger.warning(key, "client", z, "server", value);
      }
    }
  }

  private static void onEmptyResponse(String caption) {
    logger.warning(caption, "response is empty");
  }

  private ResponseHandler() {
  }
}
