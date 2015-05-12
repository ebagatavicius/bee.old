package com.butent.bee.client.grid.column;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasVerticalAlign;
import com.butent.bee.client.style.HasWhiteSpace;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Is an abstract class for grid column classes.
 */

public abstract class AbstractColumn<C> implements HasValueType, HasOptions, HasWhiteSpace,
    HasTextAlign, HasVerticalAlign {

  private final AbstractCell<C> cell;

  private List<String> searchBy;
  private List<String> sortBy;

  private boolean sortable;

  private TextAlign textAlign;
  private VerticalAlign verticalAlign;

  private WhiteSpace whiteSpace;

  private String options;

  private final List<String> classes = new ArrayList<>();

  private boolean instantKarma;
  private boolean draggable;

  public AbstractColumn(AbstractCell<C> cell) {
    this.cell = cell;
  }

  public void addClass(String className) {
    if (!BeeUtils.isEmpty(className)) {
      this.classes.add(className);
    }
  }

  public XCell export(CellContext context, Integer styleRef, XSheet sheet) {
    if (context == null) {
      return null;
    }

    String text;
    ValueType type = getValueType();

    AbstractCellRenderer renderer = getOptionalRenderer();

    if (renderer != null) {
      XCell xc = renderer.export(context.getRow(), context.getColumnIndex(), styleRef, sheet);
      if (xc != null) {
        return xc;
      }

      text = renderer.render(context.getRow());
      if (renderer.getExportType() != null) {
        type = renderer.getExportType();
      }

    } else {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      render(context, sb);

      text = sb.toSafeHtml().asString();
    }

    return Exporter.createCell(text, type, context.getColumnIndex(), styleRef);
  }

  public AbstractCell<C> getCell() {
    return cell;
  }

  public List<String> getClasses() {
    return classes;
  }

  public abstract ColType getColType();

  public AbstractCellRenderer getOptionalRenderer() {
    if (this instanceof HasCellRenderer) {
      return ((HasCellRenderer) this).getRenderer();
    } else {
      return null;
    }
  }

  @Override
  public String getOptions() {
    return options;
  }

  public List<String> getSearchBy() {
    return searchBy;
  }

  public List<String> getSortBy() {
    return sortBy;
  }

  public abstract String getString(CellContext context);

  public abstract String getStyleSuffix();

  @Override
  public TextAlign getTextAlign() {
    return textAlign;
  }

  public abstract C getValue(IsRow row);

  @Override
  public VerticalAlign getVerticalAlign() {
    return verticalAlign;
  }

  @Override
  public WhiteSpace getWhiteSpace() {
    return whiteSpace;
  }

  public Integer initExport(XSheet sheet) {
    Integer styleRef = null;

    ValueType type = getValueType();
    AbstractCellRenderer renderer = getOptionalRenderer();

    if (renderer != null) {
      styleRef = renderer.initExport(sheet);
      if (renderer.getExportType() != null) {
        type = renderer.getExportType();
      }
    }

    if (styleRef == null && type != null && sheet != null) {
      TextAlign ta = getTextAlign();
      if (ta == null) {
        ta = UiHelper.getDefaultHorizontalAlignment(type);
      }

      NumberFormat numberFormat = null;
      if (ValueType.isNumeric(type)) {
        if (this instanceof HasNumberFormat) {
          numberFormat = ((HasNumberFormat) this).getNumberFormat();
        }

        if (numberFormat == null && (type == ValueType.DECIMAL || type == ValueType.NUMBER)) {
          int scale = (this instanceof HasScale) ? ((HasScale) this).getScale() : 0;
          numberFormat = Format.getDefaultNumberFormat(type, scale);
        }
      }

      if (ta != null || numberFormat != null) {
        XStyle style = new XStyle();

        if (ta != null) {
          style.setTextAlign(ta);
        }
        if (numberFormat != null) {
          style.setFormat(numberFormat.getPattern());
        }

        styleRef = sheet.registerStyle(style);
      }
    }

    return styleRef;
  }

  public boolean instantKarma(IsRow row) {
    return instantKarma && getValue(row) != null;
  }

  public boolean isDraggable() {
    return draggable;
  }

  public boolean isSortable() {
    return sortable;
  }

  public EventState onBrowserEvent(CellContext context, Element elem, IsRow row, Event event) {
    return cell.onBrowserEvent(context, elem, getValue(row), event);
  }

  public void render(CellContext context, SafeHtmlBuilder sb) {
    cell.render(context, getValue(context.getRow()), sb);
  }

  public void setDraggable(boolean draggable) {
    this.draggable = draggable;
  }

  public void setInstantKarma(boolean instantKarma) {
    this.instantKarma = instantKarma;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setSearchBy(List<String> searchBy) {
    this.searchBy = searchBy;
  }

  public void setSortable(boolean sortable) {
    this.sortable = sortable;
  }

  public void setSortBy(List<String> sortBy) {
    this.sortBy = sortBy;
  }

  @Override
  public void setTextAlign(TextAlign textAlign) {
    this.textAlign = textAlign;
  }

  @Override
  public void setVerticalAlign(VerticalAlign verticalAlign) {
    this.verticalAlign = verticalAlign;
  }

  @Override
  public void setWhiteSpace(WhiteSpace whiteSpace) {
    this.whiteSpace = whiteSpace;
  }
}
