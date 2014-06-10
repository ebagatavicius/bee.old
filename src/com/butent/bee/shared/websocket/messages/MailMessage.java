package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

public class MailMessage extends Message {

  private enum Serial {
    FOLDER, MESSAGES, FOLDERS, FLAG, ERROR
  }

  private Long folderId;
  private boolean messagesUpdated;
  private boolean foldersUpdated;
  private MessageFlag flag;
  private String error;

  public MailMessage(Long folderId) {
    this();
    this.folderId = folderId;
  }

  MailMessage() {
    super(Type.MAIL);
  }

  @Override
  public String brief() {
    return toString();
  }

  public boolean foldersUpdated() {
    return foldersUpdated;
  }

  public String getError() {
    return error;
  }

  public MessageFlag getFlag() {
    return flag;
  }

  public Long getFolderId() {
    return folderId;
  }

  @Override
  public boolean isValid() {
    return BeeUtils.isEmpty(getError());
  }

  public boolean messagesUpdated() {
    return messagesUpdated;
  }

  public void setError(String error) {
    this.error = error;
  }

  public void setFlag(MessageFlag flag) {
    this.flag = flag;
  }

  public void setFoldersUpdated(boolean foldersUpdated) {
    this.foldersUpdated = foldersUpdated;
  }

  public void setMessagesUpdated(boolean messagesUpdated) {
    this.messagesUpdated = messagesUpdated;
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(getType(),
        DataUtils.isId(getFolderId()) ? "folder =" : null, getFolderId(), getFlag(),
        messagesUpdated() ? "MessagesUpdated" : null, foldersUpdated() ? "FoldersUpdated" : null);
  }

  @Override
  protected void deserialize(String s) {
    Serial[] members = Serial.values();
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case ERROR:
          setError(value);
          break;

        case FLAG:
          setFlag(EnumUtils.getEnumByName(MessageFlag.class, value));
          break;

        case FOLDER:
          folderId = BeeUtils.toLongOrNull(value);
          break;

        case FOLDERS:
          setFoldersUpdated(BeeUtils.toBoolean(value));
          break;

        case MESSAGES:
          setMessagesUpdated(BeeUtils.toBoolean(value));
          break;
      }
    }
  }

  @Override
  protected String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ERROR:
          arr[i++] = getError();
          break;

        case FLAG:
          arr[i++] = getFlag();
          break;

        case FOLDER:
          arr[i++] = getFolderId();
          break;

        case FOLDERS:
          arr[i++] = foldersUpdated();
          break;

        case MESSAGES:
          arr[i++] = messagesUpdated();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }
}
