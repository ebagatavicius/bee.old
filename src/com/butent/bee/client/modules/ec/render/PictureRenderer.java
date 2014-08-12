package com.butent.bee.client.modules.ec.render;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.ec.EcUtils;

public class PictureRenderer extends AbstractCellRenderer {

  private static final ImageElement imageElement;

  static {
    imageElement = Document.get().createImageElement();
    imageElement.setAlt("picture");

    imageElement.setClassName(EcStyles.name("Picture"));
  }

  private final int typeIdx;
  private final int resourceIdx;

  public PictureRenderer(int resourceIdx) {
    this(BeeConst.UNDEF, resourceIdx);
  }

  public PictureRenderer(int typeIdx, int resourceIdx) {
    super(null);

    this.typeIdx = typeIdx;
    this.resourceIdx = resourceIdx;
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    String type = BeeConst.isUndef(typeIdx) ? null : row.getString(typeIdx);
    String picture = EcUtils.picture(type, row.getString(resourceIdx));

    if (picture == null) {
      return null;
    }
    imageElement.setSrc(picture);
    return imageElement.getString();
  }
}
