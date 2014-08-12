package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UsersMessage extends Message {

  private final List<UserData> data = Lists.newArrayList();

  public UsersMessage(Collection<UserData> data) {
    this();
    this.data.addAll(data);
  }

  UsersMessage() {
    super(Type.USERS);
  }

  @Override
  public String brief() {
    return BeeUtils.toString(getData().size());
  }

  public List<UserData> getData() {
    return data;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.isEmpty(getData());
  }

  @Override
  public String toString() {
    List<Long> ids = Lists.newArrayList();
    for (UserData userData : data) {
      ids.add(userData.getUserId());
    }

    if (ids.size() > 1) {
      Collections.sort(ids);
    }

    return BeeUtils.joinOptions("type", string(getType()), "size", BeeUtils.toString(data.size()),
        "users", ids.isEmpty() ? null : ids.toString());
  }

  @Override
  protected void deserialize(String s) {
    if (!data.isEmpty()) {
      data.clear();
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr != null) {
      for (String ud : arr) {
        data.add(UserData.restore(ud));
      }
    }
  }

  @Override
  protected String serialize() {
    return Codec.beeSerialize(getData());
  }
}
