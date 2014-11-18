package com.butent.bee.client.modules.tasks;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.utils.BeeUtils;

class ModeRenderer extends AbstractCellRenderer {

  private enum Mode {
    NEW, UPD
  }

  static final String STYLE_MODE_NEW = BeeConst.CSS_CLASS_PREFIX + "crm-Mode-new";
  static final String STYLE_MODE_UPD = BeeConst.CSS_CLASS_PREFIX + "crm-Mode-upd";

  private static Mode getMode(IsRow row) {
    if (row == null) {
      return null;
    }
    if (row.getProperty(PROP_USER) == null) {
      return null;
    }

    Long access = BeeUtils.toLongOrNull(row.getProperty(PROP_LAST_ACCESS));
    if (access == null) {
      return Mode.NEW;
    }

    Long publish = BeeUtils.toLongOrNull(row.getProperty(PROP_LAST_PUBLISH));
    if (publish != null && access < publish) {
      return Mode.UPD;
    } else {
      return null;
    }
  }

  private static String renderMode(String styleName) {
    return "<div class=\"" + styleName + "\"></div>";
  }

  ModeRenderer() {
    super(null);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    Mode mode = getMode(row);
    if (mode == null) {
      return null;
    }

    XStyle style = new XStyle();

    switch (mode) {
      case NEW:
        style.setColor(Colors.LIGHTGREEN);
        break;
      case UPD:
        style.setColor(Colors.YELLOW);
        break;
    }

    return XCell.forStyle(cellIndex, sheet.registerStyle(style));
  }

  @Override
  public String render(IsRow row) {
    Mode mode = getMode(row);

    if (mode == null) {
      return BeeConst.STRING_EMPTY;
    }

    switch (mode) {
      case NEW:
        return renderMode(STYLE_MODE_NEW);
      case UPD:
        return renderMode(STYLE_MODE_UPD);
    }

    Assert.untouchable();
    return null;
  }
}
