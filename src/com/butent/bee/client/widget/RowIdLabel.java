package com.butent.bee.client.widget;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HandlesRendering;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

public class RowIdLabel extends Label implements HandlesRendering, HasNumberFormat {

  private NumberFormat numberFormat;
  private AbstractCellRenderer renderer;

  public RowIdLabel(boolean inline) {
    this(Format.getDefaultLongFormat(), inline);
  }

  public RowIdLabel(NumberFormat format, boolean inline) {
    super(inline);
    this.numberFormat = format;
  }

  public RowIdLabel(String pattern, boolean inline) {
    this(BeeUtils.isEmpty(pattern) ? null : Format.getNumberFormat(pattern), inline);
  }

  @Override
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  @Override
  public AbstractCellRenderer getRenderer() {
    return renderer;
  }

  @Override
  public void render(IsRow row) {
    String text;

    if (getRenderer() != null) {
      text = getRenderer().render(row);
    } else if (row == null) {
      text = BeeConst.STRING_EMPTY;
    } else if (getNumberFormat() != null) {
      text = getNumberFormat().format(row.getId());
    } else {
      text = BeeUtils.toString(row.getId());
    }

    setHtml(text);
  }

  @Override
  public void setNumberFormat(NumberFormat numberFormat) {
    this.numberFormat = numberFormat;
  }

  @Override
  public void setRenderer(AbstractCellRenderer renderer) {
    this.renderer = renderer;
  }
}
