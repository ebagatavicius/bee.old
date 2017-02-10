package com.butent.bee.shared.modules.mail;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AccountInfo {
  private final long accountId;
  private final long addressId;
  private final String address;
  private final long userId;
  private final boolean isPrivate;
  private final String description;
  private final Long signatureId;
  private final Map<SystemFolder, Long> sysFolders = new HashMap<>();
  private MailFolder rootFolder = new MailFolder();

  public AccountInfo(SimpleRow row) {
    this.accountId = row.getLong(COL_ACCOUNT);
    this.addressId = row.getLong(COL_ADDRESS);
    this.address = row.getValue(ClassifierConstants.COL_EMAIL_ADDRESS);
    this.userId = row.getLong(COL_USER);
    this.isPrivate = BeeUtils.unbox(row.getBoolean(COL_ACCOUNT_PRIVATE));
    this.description = row.getValue(COL_ACCOUNT_DESCRIPTION);
    this.signatureId = row.getLong(COL_SIGNATURE);

    for (SystemFolder sysFolder : SystemFolder.values()) {
      setSystemFolder(sysFolder, row.getLong(sysFolder.name() + COL_FOLDER));
    }
  }

  public MailFolder findFolder(Long folderId) {
    if (DataUtils.isId(folderId)) {
      return getRootFolder().findFolder(folderId);
    }
    return null;
  }

  public long getAccountId() {
    return accountId;
  }

  public String getAddress() {
    return address;
  }

  public long getAddressId() {
    return addressId;
  }

  public String getDescription() {
    return description;
  }

  public long getInboxId() {
    return getSystemFolder(SystemFolder.Inbox);
  }

  public String getFolderCaption(Long folderId) {
    Dictionary loc = Localized.dictionary();
    String cap = null;

    if (Objects.isNull(folderId)) {
      cap = getRootFolder().getName();
    } else if (isDraftsFolder(folderId)) {
      cap = loc.mailFolderDrafts();
    } else if (isInboxFolder(folderId)) {
      cap = loc.mailFolderInbox();
    } else if (isSentFolder(folderId)) {
      cap = loc.mailFolderSent();
    } else if (isTrashFolder(folderId)) {
      cap = loc.mailFolderTrash();
    } else {
      MailFolder folder = findFolder(folderId);

      if (Objects.nonNull(folder)) {
        cap = folder.getName();
      }
    }
    return cap;
  }

  public MailFolder getRootFolder() {
    return rootFolder;
  }

  public Long getSignatureId() {
    return signatureId;
  }

  public Long getSystemFolder(SystemFolder sysFolder) {
    return sysFolders.get(sysFolder);
  }

  public long getTrashId() {
    return getSystemFolder(SystemFolder.Trash);
  }

  public long getUserId() {
    return userId;
  }

  public boolean isDraftsFolder(Long folderId) {
    return Objects.equals(folderId, getSystemFolder(SystemFolder.Drafts));
  }

  public boolean isInboxFolder(Long folderId) {
    return Objects.equals(folderId, getInboxId());
  }

  public boolean isSentFolder(Long folderId) {
    return Objects.equals(folderId, getSystemFolder(SystemFolder.Sent));
  }

  public boolean isSystemFolder(Long folderId) {
    return sysFolders.containsValue(folderId);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isTrashFolder(Long folderId) {
    return Objects.equals(folderId, getTrashId());
  }

  public void setRootFolder(MailFolder folder) {
    this.rootFolder = folder;
  }

  public void setSystemFolder(SystemFolder sysFolder, Long folderId) {
    sysFolders.put(sysFolder, folderId);
  }
}