package com.butent.bee.shared.modules.mail;

import com.butent.bee.shared.utils.BeeUtils;

public class MailConstants {

  public enum SystemFolder {
    Inbox("INBOX"), Sent("Sent Messages"), Drafts("Drafts"), Trash("Deleted Messages");

    private final String name;

    SystemFolder(String name) {
      this.name = name;
    }

    public String getFullName() {
      return name;
    }
  }

  public enum AddressType {
    TO, CC, BCC
  }

  public enum Protocol {
    POP3, IMAP, SMTP
  }

  public enum MessageFlag {
    ANSWERED(1), DELETED(2), FLAGGED(4), SEEN(8), USER(16);

    public static boolean isFlagged(Integer bits) {
      return (BeeUtils.unbox(bits) & FLAGGED.getMask()) != 0;
    }

    public static boolean isSeen(Integer bits) {
      return (BeeUtils.unbox(bits) & SEEN.getMask()) != 0;
    }

    final int mask;

    private MessageFlag(int mask) {
      this.mask = mask;
    }

    public int getMask() {
      return mask;
    }
  }

  public static final String MAIL_MODULE = "Mail";
  public static final String MAIL_METHOD = MAIL_MODULE + "Method";

  public static final String SVC_RESTART_PROXY = "restart_proxy";
  public static final String SVC_GET_ACCOUNTS = "get_accounts";
  public static final String SVC_GET_FOLDERS = "get_folders";
  public static final String SVC_CREATE_FOLDER = "create_folder";
  public static final String SVC_RENAME_FOLDER = "rename_folder";
  public static final String SVC_DROP_FOLDER = "drop_folder";
  public static final String SVC_GET_MESSAGE = "get_message";
  public static final String SVC_FLAG_MESSAGE = "flag_message";
  public static final String SVC_CHECK_MAIL = "check_mail";
  public static final String SVC_SEND_MAIL = "send_mail";
  public static final String SVC_REMOVE_MESSAGES = "remove_messages";

  public static final String TBL_ACCOUNTS = "Accounts";
  public static final String TBL_FOLDERS = "Folders";

  public static final String TBL_MESSAGES = "Messages";
  public static final String TBL_HEADERS = "Headers";
  public static final String TBL_PARTS = "Parts";
  public static final String TBL_ATTACHMENTS = "Attachments";

  public static final String TBL_RECIPIENTS = "Recipients";
  public static final String TBL_PLACES = "Places";

  public static final String COL_MESSAGE = "Message";
  public static final String COL_HEADER = "Header";
  public static final String COL_FILE = "File";
  public static final String COL_ATTACHMENT_NAME = "FileName";
  public static final String COL_ADDRESS = "Address";
  public static final String COL_ADDRESS_TYPE = "Type";
  public static final String COL_USER = "User";
  public static final String COL_UNIQUE_ID = "UniqueId";
  public static final String COL_DATE = "Date";
  public static final String COL_SENDER = "Sender";
  public static final String COL_SUBJECT = "Subject";
  public static final String COL_CONTENT = "Content";
  public static final String COL_HTML_CONTENT = "HtmlContent";
  public static final String COL_CONTENT_TYPE = "ContentType";

  public static final String COL_ACCOUNT_DESCRIPTION = "Description";
  public static final String COL_ACCOUNT_DEFAULT = "Main";
  public static final String COL_STORE_STYPE = "StoreType";
  public static final String COL_STORE_SERVER = "StoreServer";
  public static final String COL_STORE_SPORT = "StorePort";
  public static final String COL_STORE_LOGIN = "StoreLogin";
  public static final String COL_STORE_PASSWORD = "StorePassword";
  public static final String COL_TRANSPORT_SERVER = "TransportServer";
  public static final String COL_TRANSPORT_PORT = "TransportPort";

  public static final String COL_ACCOUNT = "Account";
  public static final String COL_FOLDER_PARENT = "Parent";
  public static final String COL_FOLDER_NAME = "Name";
  public static final String COL_FOLDER_UID = "UIDValidity";

  public static final String COL_PLACE = "Place";
  public static final String COL_FOLDER = "Folder";
  public static final String COL_FLAGS = "Flags";
  public static final String COL_MESSAGE_UID = "MessageUID";

  private MailConstants() {
  }
}
