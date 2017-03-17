package com.butent.bee.client.render;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

public class PhotoRenderer extends AbstractCellRenderer {

  public static final String STYLE_EMBEDDED = BeeConst.CSS_CLASS_PREFIX + "Photo-embedded";

  public static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";

  private static final ImageElement imageElement;

  static {
    imageElement = Document.get().createImageElement();
    imageElement.addClassName(STYLE_EMBEDDED);
  }

  public static String getPhotoUrl(String hash) {
    return BeeUtils.isEmpty(hash) ? DEFAULT_PHOTO_IMAGE : FileUtils.getUrl(hash);
  }

  public PhotoRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public String render(IsRow row) {
    imageElement.setSrc(getPhotoUrl(getString(row)));
    imageElement.setAlt("photo");

    return imageElement.getString();
  }
}
