package com.butent.bee.client.render;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XPicture;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

public class PhotoRenderer extends AbstractCellRenderer {

  public static final String STYLE_EMBEDDED = BeeConst.CSS_CLASS_PREFIX + "Photo-embedded";

  private static final ImageElement imageElement;

  private static final Map<String, String> urlCache;

  static {
    imageElement = Document.get().createImageElement();
    imageElement.addClassName(STYLE_EMBEDDED);

    urlCache = new HashMap<>();
  }

  public static void addToCache(String fileName, String url) {
    Assert.notEmpty(fileName);
    Assert.notEmpty(url);

    urlCache.put(fileName, url);
  }

  public static String getUrl(String fileName) {
    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else if (urlCache.containsKey(fileName)) {
      return urlCache.get(fileName);
    } else {
      return getPath(fileName);
    }
  }

  private static String getPath(String fileName) {
    return Paths.buildPath(Paths.IMAGE_DIR, Paths.PHOTO_DIR, fileName);
  }

  public PhotoRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    String src = getSrc(row);
    if (BeeUtils.isEmpty(src) || sheet == null) {
      return null;
    }

    XPicture picture = XPicture.create(src);

    if (picture == null) {
      return null;

    } else {
      int ref = sheet.registerPicture(picture);
      XCell cell = XCell.forPicture(cellIndex, ref);
      cell.setPictureLayout(XPicture.Layout.RESIZE);

      return cell;
    }
  }

  @Override
  public String render(IsRow row) {
    String src = getSrc(row);
    if (BeeUtils.isEmpty(src)) {
      return null;
    }

    imageElement.setSrc(src);
    imageElement.setAlt("photo");

    return imageElement.getString();
  }

  private String getSrc(IsRow row) {
    String fileName = getString(row);

    if (BeeUtils.isEmpty(fileName)) {
      return null;
    } else {
      return getUrl(fileName);
    }
  }
}
