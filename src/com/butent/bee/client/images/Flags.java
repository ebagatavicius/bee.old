package com.butent.bee.client.images;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Flags {

  private static final BeeLogger logger = LogUtils.getLogger(Flags.class);

  private static final Map<String, String> map = new HashMap<>();

  private static State state = State.NEW;

  private static final List<Pair<String, Callback<String>>> pendingRequests = new ArrayList<>();

  public static String get(String countryCode) {
    if (BeeUtils.isEmpty(countryCode)) {
      return null;
    }

    final String uri;

    switch (state) {
      case NEW:
        load();
        uri = getPath(countryCode);
        break;

      case LOADED:
        uri = map.get(BeeUtils.normalize(countryCode));
        break;

      default:
        uri = getPath(countryCode);
    }

    return uri;
  }

  public static void get(String countryCode, Callback<String> callback) {
    Assert.notNull(callback);
    if (BeeUtils.isEmpty(countryCode)) {
      callback.onFailure("flags: country code is empty");
      return;
    }

    switch (state) {
      case NEW:
        pendingRequests.add(Pair.of(countryCode, callback));
        load();
        break;

      case PENDING:
        pendingRequests.add(Pair.of(countryCode, callback));
        break;

      case LOADED:
        getUri(countryCode, callback);
        break;

      case ERROR:
        callback.onFailure("flags not loaded");
        break;

      default:
        Assert.untouchable();
    }
  }

  public static Map<String, String> getFlags() {
    return map;
  }

  public static String getPath(String countryCode) {
    Assert.notEmpty(countryCode);
    return Paths.getFlagPath(countryCode.trim().toLowerCase());
  }

  public static boolean isEmpty() {
    return map.isEmpty();
  }

  public static void load(final Callback<Integer> callback) {
    state = State.PENDING;

    BeeKeeper.getRpc().makeGetRequest(Service.GET_FLAGS, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          state = State.ERROR;

        } else {
          String[] arr = Codec.beeDeserializeCollection((String) response.getResponse());
          int c = ArrayUtils.length(arr);
          Assert.isEven(c);

          for (int i = 0; i < c; i += 2) {
            map.put(arr[i], arr[i + 1]);
          }

          state = State.LOADED;

          for (Pair<String, Callback<String>> request : pendingRequests) {
            getUri(request.getA(), request.getB());
          }
          pendingRequests.clear();

          if (callback != null) {
            callback.onSuccess(c / 2);
          }
        }
      }
    });
  }

  private static void getUri(String countryCode, Callback<String> callback) {
    String uri = map.get(BeeUtils.normalize(countryCode));

    if (BeeUtils.isEmpty(uri)) {
      logger.warning("no flag for country:", countryCode);
      callback.onFailure("flag not found");

    } else {
      callback.onSuccess(uri);
    }
  }

  private static void load() {
    load(new Callback<Integer>() {
      @Override
      public void onSuccess(Integer result) {
        logger.info("loaded", result, "flags");
      }
    });
  }

  private Flags() {
  }
}
