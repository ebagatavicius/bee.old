package com.butent.bee.shared.communication;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

public class ChatItem implements BeeSerializable, Comparable<ChatItem> {

  public static ChatItem restore(String s) {
    ChatItem chatItem = new ChatItem();
    chatItem.deserialize(s);
    return chatItem;
  }

  private long userId;
  private long time;

  private String text;
  private List<FileInfo> files;

  Map<String, String> linkData;

  public ChatItem(long userId, String text) {
    this(userId, System.currentTimeMillis(), text, null);
  }

  public ChatItem(long userId, List<FileInfo> files) {
    this(userId, System.currentTimeMillis(), null, files);
  }

  public ChatItem(long userId, String text, Map<String, String> linkData) {
    this.userId = userId;
    this.time = System.currentTimeMillis();

    this.text = text;
    this.linkData = linkData;
  }

  public ChatItem(long userId, long time, String text, List<FileInfo> files) {
    this.userId = userId;
    this.time = time;

    this.text = text;
    this.files = files;
  }

  private ChatItem() {
  }

  @Override
  public int compareTo(ChatItem o) {
    return Long.compare(time, o.time);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 5);

    int i = 0;

    setUserId(BeeUtils.toLong(arr[i++]));
    setTime(BeeUtils.toLong(arr[i++]));
    setText(arr[i++]);

    String fs = arr[i++];
    if (BeeUtils.isEmpty(fs)) {
      setFiles(null);
    } else {
      setFiles(FileInfo.restoreCollection(fs));
    }

    setLinkData(Codec.deserializeLinkedHashMap(arr[i++]));

  }

  public List<FileInfo> getFiles() {
    return files;
  }

  public Map<String, String>  getLinkData() {
    return linkData;
  }

  public String getText() {
    return text;
  }

  public long getTime() {
    return time;
  }

  public long getUserId() {
    return userId;
  }

  public boolean hasFiles() {
    return !BeeUtils.isEmpty(getFiles());
  }

  public boolean hasText() {
    return !BeeUtils.isEmpty(getText());
  }

  public boolean isValid() {
    return DataUtils.isId(getUserId()) && getTime() > 0 && (hasText() || hasFiles());
  }

  @Override
  public String serialize() {
    List<Object> values = Lists.newArrayList(getUserId(), getTime(), getText(), getFiles(),
                                                                                    getLinkData());
    return Codec.beeSerialize(values);
  }

  public void setTime(long time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("userId", getUserId(),
        "time", getTime(),
        "text", BeeUtils.clip(getText(), 50),
        "files", BeeUtils.isEmpty(getFiles()) ? null : BeeUtils.bracket(BeeUtils.size(getFiles())),
        "linkData", Codec.beeSerialize(getLinkData()));
  }

  private void setFiles(List<FileInfo> files) {
    this.files = files;
  }

  private void setLinkData(Map<String, String> linkData) {
    this.linkData = linkData;
  }

  private void setText(String text) {
    this.text = text;
  }

  private void setUserId(long userId) {
    this.userId = userId;
  }
}
