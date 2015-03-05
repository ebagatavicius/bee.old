package com.butent.bee.shared.modules.mail;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MailFolder implements BeeSerializable {

  private static final long DISCONNECTED_MODE = -1;

  public static MailFolder restore(String s) {
    MailFolder folder = new MailFolder();
    folder.deserialize(s);
    return folder;
  }

  private enum Serial {
    ID, NAME, UID, UNREAD, CHILDS
  }

  private MailFolder parent;
  private Long id;
  private String name;
  private Long uidValidity;
  private int unread;

  private final Map<String, MailFolder> childs = new LinkedHashMap<>();

  public MailFolder() {
    this(null, null, null, null);
  }

  public MailFolder(MailFolder parent, Long id, String name, Long uidValidity) {
    this.parent = parent;
    this.id = id;
    this.name = name;
    this.uidValidity = uidValidity;
  }

  public void addSubFolder(MailFolder subFolder) {
    childs.put(BeeUtils.normalize(subFolder.getName()), subFolder);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case CHILDS:
          String[] data = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(data)) {
            for (int j = 0; j < data.length; j += 2) {
              MailFolder child = restore(data[j + 1]);
              child.parent = this;
              childs.put(data[j], child);
            }
          }
          break;
        case ID:
          id = BeeUtils.toLongOrNull(value);
          break;
        case NAME:
          name = value;
          break;
        case UID:
          uidValidity = BeeUtils.toLongOrNull(value);
          break;
        case UNREAD:
          unread = BeeUtils.toInt(value);
          break;
      }
    }
  }

  public void disconnect() {
    setUidValidity(DISCONNECTED_MODE);
  }

  public MailFolder findFolder(Long folderId) {
    if (Objects.equals(getId(), folderId)) {
      return this;
    }
    for (MailFolder sub : getSubFolders()) {
      MailFolder subFolder = sub.findFolder(folderId);

      if (subFolder != null) {
        return subFolder;
      }
    }
    return null;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public MailFolder getParent() {
    return parent;
  }

  public Collection<MailFolder> getSubFolders() {
    return childs.values();
  }

  public Long getUidValidity() {
    return uidValidity;
  }

  public int getUnread() {
    return unread;
  }

  public boolean isConnected() {
    return !Objects.equals(uidValidity, DISCONNECTED_MODE);
  }

  public MailFolder removeSubFolder(String subFolderName) {
    return childs.remove(BeeUtils.normalize(subFolderName));
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case CHILDS:
          arr[i++] = childs;
          break;
        case ID:
          arr[i++] = id;
          break;
        case NAME:
          arr[i++] = name;
          break;
        case UID:
          arr[i++] = uidValidity;
          break;
        case UNREAD:
          arr[i++] = unread;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setUidValidity(Long uidValidity) {
    this.uidValidity = uidValidity;
  }

  public void setUnread(int unread) {
    this.unread = unread;
  }
}
