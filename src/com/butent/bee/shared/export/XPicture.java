package com.butent.bee.shared.export;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class XPicture implements BeeSerializable {

  private static BeeLogger logger = LogUtils.getLogger(XPicture.class);

  public enum Layout {
    RESIZE, REPAEAT
  }

  public enum Type {
    DIB("dib"),
    EMF("emf"),
    PICT("pict"),
    PNG("png"),
    JPEG("jpg", "jpeg"),
    WMF("wmf");

    private static Type find(String ext) {
      if (BeeUtils.isEmpty(ext)) {
        return null;
      }

      for (Type type : values()) {
        if (type.extensions.contains(ext.toLowerCase())) {
          return type;
        }
      }
      return null;
    }

    private final Set<String> extensions = new HashSet<>();

    private Type(String... ext) {
      for (String x : ext) {
        extensions.add(x);
      }
    }
  }

  private enum Serial {
    TYPE, SRC, IS_DATA_URI
  }

  private static final String DATA_PREFIX = "data:image/";
  private static final String DATA_BASE64 = ";base64,";

  public static XPicture create(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }

    String src;
    boolean isData;

    String ext;

    if (url.startsWith(DATA_PREFIX)) {
      isData = true;

      int p = url.indexOf(DATA_BASE64);
      if (p > 0) {
        src = url.substring(p + DATA_BASE64.length());
        ext = url.substring(DATA_PREFIX.length(), p);
      } else {
        src = null;
        ext = null;
      }

    } else {
      src = url.trim();
      isData = false;

      ext = FileNameUtils.getExtension(url);
    }

    if (BeeUtils.anyEmpty(src, ext)) {
      logger.warning("picture type not available", url);
      return null;
    }

    Type type = Type.find(ext);
    if (type == null) {
      logger.warning("picture type", ext, "not supported");
      return null;

    } else {
      XPicture picture = new XPicture();

      picture.setType(type);
      picture.setSrc(src);
      picture.setDataUri(isData);

      return picture;
    }
  }

  public static XPicture restore(String s) {
    Assert.notEmpty(s);
    XPicture picture = new XPicture();
    picture.deserialize(s);
    return picture;
  }

  private Type type;

  private String src;
  private boolean isDataUri;

  private XPicture() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (members[i]) {
        case IS_DATA_URI:
          setDataUri(Codec.unpack(value));
          break;
        case SRC:
          setSrc(value);
          break;
        case TYPE:
          setType(Codec.unpack(Type.class, value));
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (getClass() != obj.getClass()) {
      return false;
    }

    XPicture other = (XPicture) obj;
    return type == other.type && Objects.equals(src, other.src) && isDataUri == other.isDataUri;
  }

  public String getSrc() {
    return src;
  }

  public Type getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, src, isDataUri);
  }

  public boolean isDataUri() {
    return isDataUri;
  }

  @Override
  public String serialize() {
    List<String> values = new ArrayList<>();

    for (Serial member : Serial.values()) {
      switch (member) {
        case IS_DATA_URI:
          values.add(Codec.pack(isDataUri()));
          break;
        case SRC:
          values.add(getSrc());
          break;
        case TYPE:
          values.add(Codec.pack(getType()));
          break;
      }
    }

    return Codec.beeSerialize(values);
  }

  private void setDataUri(boolean isData) {
    this.isDataUri = isData;
  }

  private void setSrc(String src) {
    this.src = src;
  }

  private void setType(Type type) {
    this.type = type;
  }
}
