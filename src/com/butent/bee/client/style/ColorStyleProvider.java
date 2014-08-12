package com.butent.bee.client.style;

import com.google.gwt.safecss.shared.SafeStyles;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class ColorStyleProvider implements StyleProvider {

  private static final BeeLogger logger = LogUtils.getLogger(ColorStyleProvider.class);

  public static ColorStyleProvider create(List<? extends IsColumn> columns,
      String bgName, String fgName) {

    if (BeeUtils.isEmpty(columns) || BeeUtils.allEmpty(bgName, fgName)) {
      return null;
    }

    CellSource bgSource = null;
    CellSource fgSource = null;

    for (int i = 0; i < columns.size(); i++) {
      IsColumn column = columns.get(i);

      if (BeeUtils.same(column.getId(), bgName)) {
        bgSource = CellSource.forColumn(column, i);
        if (fgSource != null) {
          break;
        }

      } else if (BeeUtils.same(column.getId(), fgName)) {
        fgSource = CellSource.forColumn(column, i);
        if (bgSource != null) {
          break;
        }
      }
    }

    boolean ok = true;

    if (bgSource == null && !BeeUtils.isEmpty(bgName)) {
      logger.severe("bg column not found:", bgName);
      ok = false;
    }
    if (fgSource == null && !BeeUtils.isEmpty(fgName)) {
      logger.severe("fg column not found:", fgName);
      ok = false;
    }

    if (ok) {
      return new ColorStyleProvider(bgSource, fgSource);
    } else {
      return null;
    }
  }

  public static ColorStyleProvider create(String viewName, String bgName, String fgName) {
    return create(Data.getColumns(viewName), bgName, fgName);
  }

  public static ColorStyleProvider createDefault(String viewName) {
    return create(viewName, COL_BACKGROUND, COL_FOREGROUND);
  }

  public static ColorStyleProvider forBackground(String viewName, String bgName) {
    return create(Data.getColumns(viewName), bgName, null);
  }

  private final CellSource bgSource;
  private final CellSource fgSource;

  private ColorStyleProvider(CellSource bgSource, CellSource fgSource) {
    this.bgSource = bgSource;
    this.fgSource = fgSource;
  }

  @Override
  public Integer getExportStyleRef(IsRow row, XSheet sheet) {
    if (row == null || sheet == null) {
      return null;
    }

    String bgValue = getBg(row);
    String fgValue = getFg(row);

    if (BeeUtils.allEmpty(bgValue, fgValue)) {
      return null;
    }

    XStyle style = new XStyle();
    if (!BeeUtils.isEmpty(bgValue)) {
      style.setColor(bgValue);
    }

    if (!BeeUtils.isEmpty(fgValue)) {
      XFont font = new XFont();
      font.setColor(fgValue);
      style.setFontRef(sheet.registerFont(font));
    }

    return sheet.registerStyle(style);
  }

  @Override
  public StyleDescriptor getStyleDescriptor(IsRow row) {
    if (row == null) {
      return null;
    }

    String bgValue = getBg(row);
    String fgValue = getFg(row);

    SafeStyles styles;
    if (BeeUtils.isEmpty(bgValue)) {
      styles = BeeUtils.isEmpty(fgValue)
          ? null : StyleUtils.buildStyle(CssProperties.COLOR, fgValue);
    } else if (BeeUtils.isEmpty(fgValue)) {
      styles = StyleUtils.buildStyle(CssProperties.BACKGROUND_COLOR, bgValue);
    } else {
      styles = StyleUtils.buildStyle(CssProperties.BACKGROUND_COLOR, bgValue,
          CssProperties.COLOR, fgValue);
    }

    return (styles == null) ? null : StyleDescriptor.of(styles);
  }

  private String getBg(IsRow row) {
    return (bgSource == null) ? null : bgSource.getString(row);
  }

  private String getFg(IsRow row) {
    return (fgSource == null) ? null : fgSource.getString(row);
  }
}
