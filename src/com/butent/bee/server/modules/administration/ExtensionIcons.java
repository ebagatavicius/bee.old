package com.butent.bee.server.modules.administration;

import com.butent.bee.server.Config;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ExtensionIcons {

  private static BeeLogger logger = LogUtils.getLogger(ExtensionIcons.class);

  private static final Map<String, String> icons = new HashMap<>();
  private static boolean initialized;

  public static String getIcon(String filename) {
    String extension = FileNameUtils.getExtension(filename);
    if (BeeUtils.isEmpty(extension)) {
      return null;
    }

    ensureIcons();
    return icons.get(BeeUtils.normalize(extension));
  }

  public static void setIcons(BeeRowSet rowSet, String fileNameColumn, String property) {
    if (!DataUtils.isEmpty(rowSet)) {
      int index = rowSet.getColumnIndex(fileNameColumn);

      if (!BeeConst.isUndef(index)) {
        for (BeeRow row : rowSet.getRows()) {
          String icon = getIcon(row.getString(index));
          if (!BeeUtils.isEmpty(icon)) {
            row.setProperty(property, icon);
          }
        }
      }
    }
  }

  private static synchronized void ensureIcons() {
    if (initialized) {
      return;
    }

    File dir = new File(Config.IMAGE_DIR, Paths.FILE_ICON_DIR);
    File[] files = dir.listFiles();

    if (files != null) {
      for (File file : files) {
        String name = file.getName();
        String key = FileNameUtils.removeExtension(name);

        if (!BeeUtils.same(key, name)) {
          icons.put(BeeUtils.normalize(key), BeeUtils.trim(name));
        }
      }
    }

    initialized = true;

    logger.info("loaded", icons.size(), "extension icons from", dir.getPath());
  }

  private ExtensionIcons() {
  }
}
