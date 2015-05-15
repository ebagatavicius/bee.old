package com.butent.bee.client.render;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XPicture;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.utils.BeeUtils;

public class FileIconRenderer extends AbstractCellRenderer {

  private static final ImageElement imageElement = Document.get().createImageElement();

  public FileIconRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    String url = getUrl(row);
    if (BeeUtils.isEmpty(url) || sheet == null) {
      return null;
    }

    XPicture picture = XPicture.create(url);
    if (picture == null) {
      return new XCell(cellIndex, getString(row), styleRef);
    }

    int ref = sheet.registerPicture(picture);
    return XCell.forPicture(cellIndex, ref);
  }

  @Override
  public VerticalAlign getDefaultVerticalAlign() {
    return VerticalAlign.MIDDLE;
  }

  @Override
  public String render(IsRow row) {
    String url = getUrl(row);
    if (BeeUtils.isEmpty(url)) {
      return null;
    }

    imageElement.setSrc(url);
    imageElement.setAlt(getString(row));

    return imageElement.getString();
  }

  private String getUrl(IsRow row) {
    if (row == null) {
      return null;
    }

    String icon = getString(row);
    if (BeeUtils.isEmpty(icon)) {
      return null;
    } else {
      return FileInfo.getIconUrl(icon);
    }
  }
}
