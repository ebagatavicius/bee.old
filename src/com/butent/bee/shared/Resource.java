package com.butent.bee.shared;

import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class Resource implements BeeSerializable {

  public static Resource restore(String s) {
    Resource resource = new Resource();
    resource.deserialize(s);
    return resource;
  }

  private String uri;
  private boolean readOnly;

  private String content;
  private ContentType type;

  private Resource() {
  }

  public Resource(String uri, ContentType type) {
    this(uri, null, type, false);
  }

  public Resource(String uri, String content) {
    this(uri, content, null, false);
  }

  public Resource(String uri, String content, boolean readOnly) {
    this(uri, content, null, readOnly);
  }

  public Resource(String uri, String content, ContentType type) {
    this(uri, content, type, false);
  }

  public Resource(String uri, String content, ContentType type, boolean readOnly) {
    this.uri = uri;
    this.content = content;
    this.type = type;
    this.readOnly = readOnly;
  }

  /**
   * Deserializes the provided argument {@code src}, and sets the deserialized values to the
   * resource.
   *
   * @param src the String to deserialize
   */
  @Override
  public void deserialize(String src) {
    Assert.notNull(src);

    Pair<Integer, Integer> scan;
    int len = 0;
    int start = 0;

    for (int i = 0; i < 4; i++) {
      scan = Codec.deserializeLength(src, start);
      len = scan.getA();
      start += scan.getB();

      if (len <= 0) {
        continue;
      }
      String v = src.substring(start, start + len);

      switch (i) {
        case 0:
          setUri(v);
          break;
        case 1:
          setType(CommUtils.getContentType(v));
          break;
        case 2:
          setReadOnly(BeeUtils.toBoolean(v));
          break;
        case 3:
          setContent(v);
          break;
      }

      start += len;
    }
  }

  public String getContent() {
    return content;
  }

  public ContentType getType() {
    return type;
  }

  public String getUri() {
    return uri;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * Serializes resources {@code (name, uri, type, readOnly, content)} in this and sequence and
   * returns a serialized String.
   *
   * @return a serializes String for deserialization.
   */
  @Override
  public String serialize() {
    int[] arr = new int[] {
        BeeUtils.length(uri),
        BeeUtils.length(BeeUtils.toString(type)),
        BeeUtils.length(BeeUtils.toString(readOnly)),
        BeeUtils.length(content)};

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < arr.length; i++) {
      sb.append(Codec.serializeLength(arr[i]));
      if (arr[i] <= 0) {
        continue;
      }

      switch (i) {
        case 0:
          sb.append(uri);
          break;
        case 1:
          sb.append(BeeUtils.toString(type));
          break;
        case 2:
          sb.append(BeeUtils.toString(readOnly));
          break;
        case 3:
          sb.append(content);
          break;
      }
    }
    return sb.toString();
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setType(ContentType type) {
    this.type = type;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
