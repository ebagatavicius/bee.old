package com.butent.bee.shared.modules.mail;

public interface LocalizableMailMessages {

  String mailCancelFolderSynchronizationQuestion(String folderName);

  String mailCopiedMessagesToFolder(String count);

  String mailDeletedMessages(String count);

  String mailDeleteFolderQuestion(String folderName);

  String mailMovedMessagesToFolder(String count);

  String mailMovedMessagesToTrash(String count);

  String mailOnlyInFolder(String folderName);

  String mailRenameFolder(String folderName);

  String mailSelectedMessages(int count);

}
