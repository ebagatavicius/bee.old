package com.butent.bee.client.utils;

import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import elemental.html.File;

public class NewFileInfo extends FileInfo {

  private final File file;

  public NewFileInfo(File file) {
    super(null, file.getName(), BeeUtils.toLong(file.getSize()), file.getType());
    this.file = file;

    double millis = FileUtils.getLastModifiedMillis(file);
    setFileDate((millis > 0) ? new DateTime(BeeUtils.toLong(millis)) : null);
  }

  public File getFile() {
    return file;
  }
}
