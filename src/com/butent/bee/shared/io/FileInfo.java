package com.butent.bee.shared.io;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

public class FileInfo implements HasInfo, HasCaption, BeeSerializable {

  private enum Serial {
    ID, NAME, SIZE, TYPE, ICON, DATE, VERSION, CAPTION, DESCRIPTION, RELATED
  }

  public static String getIconUrl(String icon) {
    Assert.notEmpty(icon);
    return Paths.buildPath(Paths.IMAGE_DIR, Paths.FILE_ICON_DIR, icon);
  }

  public static FileInfo restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    FileInfo result = new FileInfo();
    result.deserialize(s);

    return result;
  }

  public static List<FileInfo> restoreCollection(String s) {
    List<FileInfo> result = Lists.newArrayList();
    if (BeeUtils.isEmpty(s)) {
      return result;
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr == null) {
      return result;
    }

    for (String item : arr) {
      FileInfo storedFile = restore(item);
      if (storedFile != null) {
        result.add(storedFile);
      }
    }
    return result;
  }

  private Long fileId;

  private String name;
  private Long size;

  private String type;
  private String icon;

  private DateTime fileDate;
  private String fileVersion;

  private String caption;
  private String description;

  private Long relatedId;

  private String path;
  private boolean temporary;

  public FileInfo(Long fileId, String name, Long size, String type) {
    setFileId(fileId);
    setName(name);
    setSize(size);
    setType(type);
  }

  private FileInfo() {
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
      Serial member = members[i];

      switch (member) {
        case ID:
          setFileId(BeeUtils.toLongOrNull(value));
          break;

        case NAME:
          setName(value);
          break;

        case SIZE:
          setSize(BeeUtils.toLongOrNull(value));
          break;

        case TYPE:
          setType(value);
          break;

        case ICON:
          setIcon(value);
          break;

        case DATE:
          setFileDate(DateTime.restore(value));
          break;

        case VERSION:
          setFileVersion(value);
          break;

        case CAPTION:
          setCaption(value);
          break;

        case DESCRIPTION:
          setDescription(value);
          break;

        case RELATED:
          setRelatedId(BeeUtils.toLong(value));
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof FileInfo)) {
      return false;
    }
    FileInfo other = (FileInfo) obj;
    if (fileId == null) {
      if (other.fileId != null) {
        return false;
      }
    } else if (fileId.equals(other.fileId)) {
      return true;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (size == null) {
      if (other.size != null) {
        return false;
      }
    } else if (!size.equals(other.size)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public String getDescription() {
    return description;
  }

  public DateTime getFileDate() {
    return fileDate;
  }

  public Long getId() {
    return fileId;
  }

  public String getFileVersion() {
    return fileVersion;
  }

  public String getIcon() {
    return icon;
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties("Id", getId(),
        "Name", getName(),
        "Size", getSize(),
        "Type", getType(),
        "Icon", getIcon(),
        "File Date", getFileDate(),
        "File Version", getFileVersion(),
        "Caption", getCaption(),
        "Description", getDescription(),
        "Related Id", getRelatedId());
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public Long getRelatedId() {
    return relatedId;
  }

  public Long getSize() {
    return size;
  }

  public String getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((size == null) ? 0 : size.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  public boolean isTemporary() {
    return temporary;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ID:
          arr[i++] = getId();
          break;

        case NAME:
          arr[i++] = getName();
          break;

        case SIZE:
          arr[i++] = getSize();
          break;

        case TYPE:
          arr[i++] = getType();
          break;

        case ICON:
          arr[i++] = getIcon();
          break;

        case DATE:
          arr[i++] = getFileDate();
          break;

        case VERSION:
          arr[i++] = getFileVersion();
          break;

        case CAPTION:
          arr[i++] = getCaption();
          break;

        case DESCRIPTION:
          arr[i++] = getDescription();
          break;

        case RELATED:
          arr[i++] = getRelatedId();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setFileDate(DateTime fileDate) {
    this.fileDate = fileDate;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

  public void setFileVersion(String fileVersion) {
    this.fileVersion = fileVersion;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setRelatedId(Long relatedId) {
    this.relatedId = relatedId;
  }

  public void setTemporary(boolean temporary) {
    this.temporary = temporary;
  }

  public void setType(String type) {
    this.type = type;
  }

  private void setName(String name) {
    this.name = name;
  }

  private void setSize(Long size) {
    this.size = size;
  }
}
