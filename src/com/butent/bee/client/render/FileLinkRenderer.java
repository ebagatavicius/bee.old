package com.butent.bee.client.render;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;

import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.utils.BeeUtils;

public class FileLinkRenderer extends AbstractCellRenderer {

  private static final AnchorElement anchorElement = Document.get().createAnchorElement();

  private final int hashIndex;
  private final Integer captionIndex;
  private final Integer nameIndex;

  public FileLinkRenderer(int hashIndex, Integer captionIndex, Integer nameIndex) {
    super(null);

    this.hashIndex = hashIndex;
    this.captionIndex = captionIndex;
    this.nameIndex = nameIndex;
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    String text = getText(row);

    if (BeeUtils.isEmpty(text)) {
      return null;
    } else {
      return new XCell(cellIndex, text, styleRef);
    }
  }

  @Override
  public VerticalAlign getDefaultVerticalAlign() {
    return VerticalAlign.MIDDLE;
  }

  @Override
  public String render(IsRow row) {
    if (row == null || BeeConst.isUndef(hashIndex)) {
      return null;
    }
    String hash = row.getString(hashIndex);

    if (BeeUtils.isEmpty(hash)) {
      return null;
    }
    String text = getText(row);

    if (BeeUtils.isEmpty(text)) {
      return null;
    }
    anchorElement.setHref(FileUtils.getUrl(hash, text));
    anchorElement.setInnerText(text);

    return anchorElement.getString();
  }

  private String getText(IsRow row) {
    if (row == null) {
      return null;
    }
    String text = null;

    if (BeeUtils.isNonNegative(captionIndex)) {
      text = row.getString(captionIndex);
    }
    if (BeeUtils.isEmpty(text) && BeeUtils.isNonNegative(nameIndex)) {
      text = row.getString(nameIndex);
    }
    if (BeeUtils.isEmpty(text) && BeeUtils.isNonNegative(hashIndex)) {
      text = row.getString(hashIndex);
    }
    return text;
  }
}
