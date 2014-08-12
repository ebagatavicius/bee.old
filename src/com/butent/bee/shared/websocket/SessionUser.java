package com.butent.bee.shared.websocket;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class SessionUser implements BeeSerializable {

  public static SessionUser restore(String s) {
    Assert.notEmpty(s);

    SessionUser sessionUser = new SessionUser();
    sessionUser.deserialize(s);
    return sessionUser;
  }

  private String sessionId;
  private long userId;

  public SessionUser(String sessionId, long userId) {
    this.sessionId = sessionId;
    this.userId = userId;
  }

  private SessionUser() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setSessionId(arr[0]);
    setUserId(BeeUtils.toLong(arr[1]));
  }

  public String getSessionId() {
    return sessionId;
  }

  public long getUserId() {
    return userId;
  }

  @Override
  public String serialize() {
    List<String> values = Lists.newArrayList(getSessionId(), BeeUtils.toString(getUserId()));
    return Codec.beeSerialize(values);
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("sessionId", getSessionId(), "userId",
        BeeUtils.toString(getUserId()));
  }

  private void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  private void setUserId(long userId) {
    this.userId = userId;
  }
}
