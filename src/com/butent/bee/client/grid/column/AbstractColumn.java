package com.butent.bee.client.grid.column;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasWhiteSpace;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.HasValueType;
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
    HasTextAlign {

  private final AbstractCell<C> cell;

  private List<String> searchBy;
  private List<String> sortBy;

  private boolean isSortable;

  private TextAlign hAlign;
  private WhiteSpace whiteSpace;

  private String options;

  private final List<String> classes = new ArrayList<>();

  private boolean instantKarma;

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

    AbstractCellRenderer renderer = getOptionalRenderer();

    if (renderer != null) {
      return renderer.export(context.getRow(), context.getColumnIndex(), styleRef, sheet);

    } else {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      render(context, sb);

      String html = sb.toSafeHtml().asString();

      if (BeeUtils.isEmpty(html)) {
        return null;

      } else {
        XCell xc = new XCell(context.getColumnIndex(), html);
        if (styleRef != null) {
          xc.setStyleRef(styleRef);
        }

        return xc;
      }
    }
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
    return hAlign;
  }

  public abstract C getValue(IsRow row);

  @Override
  public WhiteSpace getWhiteSpace() {
    return whiteSpace;
  }

  public Integer initExport(XSheet sheet) {
    AbstractCellRenderer renderer = getOptionalRenderer();

    if (renderer != null) {
      return renderer.initExport(sheet);

    } else if ((getTextAlign() != null || getValueType() != null) && sheet != null) {
      TextAlign textAlign = getTextAlign();
      if (textAlign == null) {
        textAlign = UiHelper.getDefaultHorizontalAlignment(getValueType());
      }

      if (textAlign != null) {
        XStyle style = new XStyle();
        style.setTextAlign(textAlign);
        return sheet.registerStyle(style);
      }
    }
    return null;
  }

  public boolean instantKarma(IsRow row) {
    return instantKarma && getValue(row) != null;
  }

  public boolean isSortable() {
    return isSortable;
  }

  public EventState onBrowserEvent(CellContext context, Element elem, IsRow row, Event event) {
    return cell.onBrowserEvent(context, elem, getValue(row), event);
  }

  public void render(CellContext context, SafeHtmlBuilder sb) {
    cell.render(context, getValue(context.getRow()), sb);
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
    this.isSortable = sortable;
  }

  public void setSortBy(List<String> sortBy) {
    this.sortBy = sortBy;
  }

  @Override
  public void setTextAlign(TextAlign align) {
    this.hAlign = align;
  }

  @Override
  public void setWhiteSpace(WhiteSpace whiteSpace) {
    this.whiteSpace = whiteSpace;
  }
}
