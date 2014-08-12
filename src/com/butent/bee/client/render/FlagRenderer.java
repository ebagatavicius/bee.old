package com.butent.bee.client.render;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

import com.butent.bee.client.images.Flags;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XPicture;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.utils.BeeUtils;

public class FlagRenderer extends AbstractCellRenderer {

  private static final ImageElement imageElement = Document.get().createImageElement();

  public FlagRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    String uri = getUri(row);
    if (BeeUtils.isEmpty(uri) || sheet == null) {
      return null;
    }

    XPicture picture = XPicture.create(uri);
    if (picture == null) {
      return new XCell(cellIndex, getString(row), styleRef);
    }

    int ref = sheet.registerPicture(picture);
    return XCell.forPicture(cellIndex, ref);
  }

  @Override
  public String render(IsRow row) {
    String uri = getUri(row);
    if (BeeUtils.isEmpty(uri)) {
      return null;
    }

    imageElement.setSrc(uri);
    imageElement.setAlt(getString(row));

    return imageElement.getString();
  }

  private String getUri(IsRow row) {
    String key = getString(row);

    if (BeeUtils.isEmpty(key)) {
      return null;
    } else {
      return Flags.get(key);
    }
  }
}
