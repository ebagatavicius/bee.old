package com.butent.bee.server.modules.mail;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

public class MailAccount {

  private static class MailStore {
    final Store store;
    final long start;
    int cnt;

    public MailStore(Store store) {
      this.store = Assert.notNull(store);
      this.start = System.currentTimeMillis();
    }

    public void enter() {
      cnt++;
    }

    public boolean expired() {
      return BeeUtils.isMore(System.currentTimeMillis() - start, TimeUtils.MILLIS_PER_MINUTE * 10L);
    }

    public Store getStore() {
      return store;
    }

    public boolean idle() {
      return cnt <= 0;
    }

    public void leave() {
      cnt--;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(MailAccount.class);

  private static final Map<Long, MailStore> stores = new HashMap<>();
  private static final ReentrantLock storesLock = new ReentrantLock();

  private static boolean checkNewFolderName(Folder newFolder, String name, boolean acceptExisting)
      throws MessagingException {
    if (name.indexOf(newFolder.getSeparator()) >= 0) {
      throw new MessagingException("Invalid folder name: " + name);
    }
    logger.debug("Checking, if folder exists:", name);

    if (newFolder.exists()) {
      if (acceptExisting) {
        return false;
      }
      throw new MessagingException("Folder with new name already exists: " + name);
    }
    return true;
  }

  private static void fillTree(MailFolder parent, Multimap<Long, SimpleRow> folders) {
    for (SimpleRow row : folders.get(parent.getId())) {
      MailFolder folder = new MailFolder(parent, row.getLong(COL_FOLDER),
          row.getValue(COL_FOLDER_NAME), row.getLong(COL_FOLDER_UID));

      folder.setUnread(BeeUtils.unbox(row.getInt(COL_MESSAGE)));

      fillTree(folder, folders);
      parent.addSubFolder(folder);
    }
  }

  private static List<Message> getMessageReferences(Folder remoteSource, long[] uids)
      throws MessagingException {

    logger.debug("Getting messages from folder", remoteSource.getName(), "by UIDs:", uids);
    Message[] msgs = ((UIDFolder) remoteSource).getMessagesByUID(uids);

    for (Message message : msgs) {
      if (message == null) {
        throw new MessagingException("Not all messages where returned by UIDs. "
            + "Folder resynchronization required.");
      }
    }
    return Lists.newArrayList(msgs);
  }

  private String error;

  private final Protocol storeProtocol;
  private final String storeHost;
  private final Integer storePort;
  private final String storeLogin;
  private final String storePassword;
  private final boolean storeSSL;
  private final Map<String, String> storeProperties;

  private final Protocol transportProtocol = Protocol.SMTP;
  private final String transportHost;
  private final Integer transportPort;
  private final String transportLogin;
  private final String transportPassword;
  private final boolean transportSSL;
  private final Map<String, String> transportProperties;

  private final AccountInfo accountInfo;

  MailAccount(SimpleRow data) {
    Assert.notNull(data);

    storeProtocol = EnumUtils.getEnumByName(Protocol.class, data.getValue(COL_STORE_TYPE));
    storeHost = data.getValue(COL_STORE_SERVER);
    storePort = data.getInt(COL_STORE_SPORT);
    storeLogin = BeeUtils.notEmpty(data.getValue(COL_STORE_LOGIN),
        data.getValue(ClassifierConstants.COL_EMAIL_ADDRESS));
    storePassword = BeeUtils.isEmpty(data.getValue(COL_STORE_PASSWORD))
        ? null : Codec.decodeBase64(data.getValue(COL_STORE_PASSWORD));
    storeSSL = BeeUtils.isTrue(data.getBoolean(COL_STORE_SSL));
    storeProperties = Codec.deserializeMap(data.getValue(COL_STORE_PROPERTIES));

    transportHost = data.getValue(COL_TRANSPORT_SERVER);
    transportPort = data.getInt(COL_TRANSPORT_PORT);
    transportLogin = BeeUtils.notEmpty(data.getValue(COL_TRANSPORT_LOGIN),
        data.getValue(ClassifierConstants.COL_EMAIL_ADDRESS));
    transportPassword = BeeUtils.isEmpty(data.getValue(COL_TRANSPORT_PASSWORD))
        ? null : Codec.decodeBase64(data.getValue(COL_TRANSPORT_PASSWORD));
    transportSSL = BeeUtils.isTrue(data.getBoolean(COL_TRANSPORT_SSL));
    transportProperties = Codec.deserializeMap(data.getValue(COL_TRANSPORT_PROPERTIES));

    accountInfo = new AccountInfo(data);
  }

  public Long getAccountId() {
    return accountInfo.getAccountId();
  }

  public String getAddress() {
    return accountInfo.getAddress();
  }

  public Long getAddressId() {
    return accountInfo.getAddressId();
  }

  public Long getSignatureId() {
    return accountInfo.getSignatureId();
  }

  public String getStoreErrorMessage() {
    String err = error;

    if (BeeUtils.isEmpty(err)) {
      if (storeProtocol == null) {
        err = "Unknown store protocol";

      } else if (BeeUtils.isEmpty(storeHost)) {
        err = "Unknown store host";

      } else if (BeeUtils.isEmpty(storeLogin)) {
        err = "Unknown store login";
      }
    }
    return err;
  }

  public String getStoreHost() {
    return storeHost;
  }

  public String getStoreLogin() {
    return storeLogin;
  }

  public String getStorePassword() {
    return storePassword;
  }

  public int getStorePort() {
    return BeeUtils.isPositive(storePort) ? storePort : -1;
  }

  public Map<String, String> getStoreProperties() {
    return storeProperties;
  }

  public Protocol getStoreProtocol() {
    return storeProtocol;
  }

  public String getTransportErrorMessage() {
    String err = error;

    if (BeeUtils.isEmpty(err)) {
      if (transportProtocol == null) {
        err = "Unknown transport protocol";

      } else if (BeeUtils.isEmpty(transportHost)) {
        err = "Unknown transport host";
      }
    }
    return err;
  }

  public String getTransportHost() {
    return transportHost;
  }

  public String getTransportLogin() {
    return transportLogin;
  }

  public String getTransportPassword() {
    return transportPassword;
  }

  public Integer getTransportPort() {
    return BeeUtils.isPositive(transportPort) ? transportPort : -1;
  }

  public Map<String, String> getTransportProperties() {
    return transportProperties;
  }

  public Protocol getTransportProtocol() {
    return transportProtocol;
  }

  public Long getUserId() {
    return accountInfo.getUserId();
  }

  public boolean isStoredRemotedly(MailFolder folder) {
    Assert.notNull(folder);
    return (getStoreProtocol() == Protocol.IMAP) && folder.isConnected();
  }

  public boolean isStoreSSL() {
    return storeSSL;
  }

  public boolean isTransportSSL() {
    return transportSSL;
  }

  public boolean isValidStoreAccount() {
    return BeeUtils.isEmpty(getStoreErrorMessage());
  }

  public boolean isValidTransportAccount() {
    return BeeUtils.isEmpty(getTransportErrorMessage());
  }

  boolean addMessageToRemoteFolder(MimeMessage message, MailFolder localFolder)
      throws MessagingException {

    if (!isStoredRemotedly(localFolder)) {
      return false;
    }
    Store store = null;
    Folder folder = null;

    try {
      store = connectToStore();
      folder = getRemoteFolder(store, localFolder);
      folder.appendMessages(new MimeMessage[] {message});

    } finally {
      disconnectFromStore(store);
    }
    return true;
  }

  Store connectToStore() throws MessagingException {
    if (!isValidStoreAccount()) {
      throw new MessagingException(getStoreErrorMessage());
    }
    storesLock.lock();
    MailStore mailStore = stores.get(getAccountId());

    if (mailStore != null) {
      if (mailStore.expired()) {
        logger.debug("Removing expired store...");
        stores.remove(getAccountId());
        storesLock.unlock();
        return connectToStore();

      } else if (mailStore.idle()) {
        logger.debug("Waiting for connected store...");
        storesLock.unlock();

        try {
          Thread.sleep(TimeUtils.MILLIS_PER_SECOND);
        } catch (InterruptedException e) {
          logger.warning(e.getMessage());
        }
        return connectToStore();
      }
      logger.debug("Entering connected store...");
      mailStore.enter();
      storesLock.unlock();
    } else {
      logger.debug("Connecting to store...");

      String protocol = getStoreProtocol().name().toLowerCase();
      Properties props = new Properties();
      String pfx = "mail." + protocol + ".";

      if (isStoreSSL()) {
        props.put(pfx + "ssl.enable", "true");
      }
      for (Entry<String, String> prop : getStoreProperties().entrySet()) {
        String key = prop.getKey();
        props.put(BeeUtils.isPrefix(key, "mail.") ? key : pfx + key, prop.getValue());
      }
      Session session = Session.getInstance(props, null);
      mailStore = new MailStore(session.getStore(protocol));

      stores.put(getAccountId(), mailStore);
      storesLock.unlock();

      try {
        mailStore.getStore().connect(getStoreHost(), getStorePort(), getStoreLogin(),
            getStorePassword());
      } catch (MessagingException ex) {
        storesLock.lock();
        stores.remove(getAccountId());
        storesLock.unlock();
        throw ex;
      }
      mailStore.enter();
    }
    return mailStore.getStore();
  }

  Transport connectToTransport() throws MessagingException {
    if (!isValidTransportAccount()) {
      throw new MessagingException(getTransportErrorMessage());
    }
    logger.debug("Connecting to transport...");

    String protocol = getTransportProtocol().name().toLowerCase();
    Properties props = new Properties();
    String pfx = "mail." + protocol + ".";

    if (!BeeUtils.isEmpty(getTransportPassword())) {
      props.put(pfx + "auth", "true");

      if (isTransportSSL()) {
        props.put(pfx + "ssl.enable", "true");
      } else {
        props.put(pfx + "starttls.enable", "true");
      }
    }
    for (Entry<String, String> prop : getTransportProperties().entrySet()) {
      String key = prop.getKey();
      props.put(BeeUtils.isPrefix(key, "mail.") ? key : pfx + key, prop.getValue());
    }
    Session session = Session.getInstance(props, null);
    Transport transport = session.getTransport(protocol);
    transport.connect(getTransportHost(), getTransportPort(), getTransportLogin(),
        getTransportPassword());
    return transport;
  }

  boolean createRemoteFolder(MailFolder parent, String name, boolean acceptExisting)
      throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(parent)) {
      return ok;
    }
    Store store = null;
    Folder folder = null;

    try {
      store = connectToStore();
      folder = getRemoteFolder(store, parent);
      Folder newFolder = folder.getFolder(name);

      if (checkNewFolderName(newFolder, name, acceptExisting)) {
        logger.debug("Creating folder:", name);
        ok = newFolder.create(Folder.HOLDS_MESSAGES);
      }
      if (ok) {
        newFolder.setSubscribed(true);
      }
    } finally {
      disconnectFromStore(store);
    }
    return ok;
  }

  void disconnectFromStore(Store store) {
    if (store != null) {
      storesLock.lock();
      MailStore mailStore = stores.get(getAccountId());
      boolean disconnect = mailStore == null || mailStore.getStore() != store;

      if (!disconnect) {
        mailStore.leave();
        disconnect = mailStore.idle();

        if (disconnect) {
          stores.remove(getAccountId());
        }
      }
      if (disconnect) {
        try {
          logger.debug("Disconnecting from store...");
          store.close();
        } catch (MessagingException e) {
          logger.warning(e);
        }
      } else {
        logger.debug("Leaving connected store...");
      }
      storesLock.unlock();
    }
  }

  boolean dropRemoteFolder(MailFolder source) throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(source)) {
      return ok;
    }
    Store store = null;
    Folder folder = null;

    try {
      store = connectToStore();
      folder = getRemoteFolder(store, source);

      logger.debug("Removing folder:", folder.getName());
      ok = folder.delete(true) || !folder.exists();

    } finally {
      disconnectFromStore(store);
    }
    return ok;
  }

  MailFolder findFolder(Long folderId) {
    return accountInfo.findFolder(folderId);
  }

  Folder getRemoteFolder(Store remoteStore, MailFolder localFolder) throws MessagingException {
    Assert.noNulls(remoteStore, localFolder);

    if (localFolder.getParent() == null) {
      logger.debug("Looking for root folder");
      return remoteStore.getDefaultFolder();
    }
    Folder remoteParent = getRemoteFolder(remoteStore, localFolder.getParent());

    String name = localFolder.getName();
    logger.debug("Looking for remote folder", BeeUtils.join(" in ", name, remoteParent.getName()));

    Folder remote = remoteParent.getFolder(name);

    if (!remote.exists()) {
      if (isInbox(localFolder) || !isSystemFolder(localFolder)
          || !createRemoteFolder(localFolder.getParent(), name, false)) {

        throw new MessagingException("Remote folder does not exist: " + name);
      }
    }
    return remote;
  }

  MailFolder getDraftsFolder() {
    return findFolder(accountInfo.getSystemFolder(SystemFolder.Drafts));
  }

  MailFolder getInboxFolder() {
    return findFolder(accountInfo.getSystemFolder(SystemFolder.Inbox));
  }

  MailFolder getSentFolder() {
    return findFolder(accountInfo.getSystemFolder(SystemFolder.Sent));
  }

  MailFolder getTrashFolder() {
    return findFolder(accountInfo.getSystemFolder(SystemFolder.Trash));
  }

  MailFolder getRootFolder() {
    return accountInfo.getRootFolder();
  }

  boolean holdsFolders(Folder remoteFolder) throws MessagingException {
    return (remoteFolder.getType() & Folder.HOLDS_FOLDERS) != 0;
  }

  boolean holdsMessages(Folder remoteFolder) throws MessagingException {
    return (remoteFolder.getType() & Folder.HOLDS_MESSAGES) != 0;
  }

  boolean isInbox(MailFolder folder) {
    return accountInfo.isInboxFolder(folder.getId());
  }

  boolean isSystemFolder(MailFolder folder) {
    return accountInfo.isSystemFolder(folder.getId());
  }

  boolean processMessages(long[] uids, MailFolder source, MailFolder target, boolean move)
      throws MessagingException {

    if (!isStoredRemotedly(source)) {
      return false;
    }
    boolean isTarget = target != null && target.isConnected();

    if (!move && !isTarget || ArrayUtils.length(uids) == 0) {
      return true;
    }
    Store store = null;
    Folder remoteSource = null;

    try {
      store = connectToStore();
      remoteSource = getRemoteFolder(store, source);

      logger.debug("Checking folder", remoteSource.getName(), "UIDValidity with",
          source.getUidValidity());

      if (!Objects.equals(((UIDFolder) remoteSource).getUIDValidity(), source.getUidValidity())) {
        throw new MessagingException("Folder out of sync: " + source.getName());
      }
      logger.debug("Opening folder:", remoteSource.getName());
      remoteSource.open(Folder.READ_WRITE);

      List<Message> messages = getMessageReferences(remoteSource, uids);

      if (isTarget) {
        Folder remoteTarget = getRemoteFolder(store, target);

        if (!holdsMessages(remoteTarget)) {
          throw new MessagingException(BeeUtils.joinWords("Folder",
              BeeUtils.bracket(target.getName()), "cannot hold messages"));
        }
        logger.debug("Copying messages to folder:", target.getName());
        remoteSource.copyMessages(messages.toArray(new Message[0]), remoteTarget);
      }
      if (move) {
        for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext();) {
          Message message = iterator.next();

          if (message.isExpunged()) {
            iterator.remove();
          }
        }
        if (!BeeUtils.isEmpty(messages)) {
          logger.debug("Deleting selected messages from folder:", remoteSource.getName());
          remoteSource.setFlags(messages.toArray(new Message[0]), new Flags(Flag.DELETED), true);
        } else {
          logger.debug("All messages already expunged, no delete required");
        }
      }
      logger.debug("Closing folder:", remoteSource.getName());
      remoteSource.close(move);

    } finally {
      if (remoteSource != null && remoteSource.isOpen()) {
        try {
          remoteSource.close(false);
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
      disconnectFromStore(store);
    }
    return true;
  }

  boolean renameRemoteFolder(MailFolder source, String name) throws MessagingException {
    boolean ok = true;

    if (!isStoredRemotedly(source)) {
      return ok;
    }
    Store store = null;
    Folder folder = null;

    try {
      store = connectToStore();
      folder = getRemoteFolder(store, source);
      Folder newFolder = folder.getParent().getFolder(name);

      checkNewFolderName(newFolder, name, false);

      logger.debug("Renamng folder:", folder.getName(), "->", name);
      ok = folder.renameTo(newFolder);

    } finally {
      disconnectFromStore(store);
    }
    return ok;
  }

  boolean setFlag(MailFolder source, long[] uids, Flag flag, boolean on) throws MessagingException {
    if (!isStoredRemotedly(source)) {
      return false;
    }
    Store store = null;
    Folder folder = null;

    try {
      store = connectToStore();
      folder = getRemoteFolder(store, source);

      logger.debug("Checking folder", folder.getName(), "UIDValidity with",
          source.getUidValidity());

      if (!Objects.equals(((UIDFolder) folder).getUIDValidity(), source.getUidValidity())) {
        throw new MessagingException("Folder out of sync: " + source.getName());
      }
      logger.debug("Opening folder:", folder.getName());
      folder.open(Folder.READ_WRITE);

      List<Message> messages = getMessageReferences(folder, uids);

      logger.debug(on ? "Setting" : "Clearing", "flag for selected messages");
      folder.setFlags(messages.toArray(new Message[0]), new Flags(flag), on);

    } finally {
      if (folder != null && folder.isOpen()) {
        try {
          logger.debug("Closing folder:", folder.getName());
          folder.close(false);
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
      disconnectFromStore(store);
    }
    return true;
  }

  void setFolders(Multimap<Long, SimpleRow> folders) {
    getRootFolder().getSubFolders().clear();
    fillTree(getRootFolder(), folders);
  }
}
