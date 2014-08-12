package com.butent.bee.client.render;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;

import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.utils.BeeUtils;

public class UrlRenderer extends AbstractCellRenderer {

  private static final AnchorElement anchorElement = Document.get().createAnchorElement();

  static {
    anchorElement.setTarget(Keywords.BROWSING_CONTEXT_BLANK);
  }

  public UrlRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public String render(IsRow row) {
    String url = getString(row);

    if (BeeUtils.isEmpty(url)) {
      return null;
    } else {
      anchorElement.setHref(url);
      anchorElement.setInnerText(url);

      return anchorElement.getString();
    }
  }
}
