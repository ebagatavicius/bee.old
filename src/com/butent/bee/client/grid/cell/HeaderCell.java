package com.butent.bee.client.grid.cell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements header cells rendering and behavior management.
 */

public class HeaderCell extends AbstractCell<String> implements HasCaption {

  /**
   * Specifies header cell's templates for safeHtml usage.
   */

  interface Template extends SafeHtmlTemplates {
    @Template("<div id=\"{0}\" class=\"{1}\">{2}</div>")
    SafeHtml caption(String id, String classes, SafeHtml label);

    @Template("<div id=\"{0}\" class=\"{1}\"></div>")
    SafeHtml sortable(String id, String classes);

    @Template("<div id=\"{0}\" class=\"{1}\">{2}</div>")
    SafeHtml sorted(String id, String classes, String sortInfo);
  }

  public static final int SORT_INFO_WIDTH = 15;

  private static final String STYLE_CAPTION = BeeConst.CSS_CLASS_PREFIX + "HeaderCellCaption";
  private static final String STYLE_SORT_INFO = BeeConst.CSS_CLASS_PREFIX + "HeaderCellSortInfo";
  private static final String STYLE_SORTABLE = BeeConst.CSS_CLASS_PREFIX + "HeaderCellSortable";
  private static final String STYLE_ASCENDING = BeeConst.CSS_CLASS_PREFIX + "HeaderCellAscending";
  private static final String STYLE_DESCENDING = BeeConst.CSS_CLASS_PREFIX + "HeaderCellDescending";

  private static Template template;

  private final String sortInfoId;
  private final String captionId;

  private String caption;

  public HeaderCell(String caption) {
    super(EventUtils.EVENT_TYPE_CLICK);
    this.caption = caption;

    if (template == null) {
      template = GWT.create(Template.class);
    }

    sortInfoId = DomUtils.createUniqueId("sort-info");
    captionId = DomUtils.createUniqueId("caption");
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public EventState onBrowserEvent(CellContext context, Element parent, String value, Event event) {
    EventState state = super.onBrowserEvent(context, parent, value, event);

    if (state.proceed() && EventUtils.isClick(event) && context.getGrid() != null) {
      event.preventDefault();
      int col = context.getColumnIndex();

      if (EventUtils.isTargetId(event.getEventTarget(), sortInfoId)) {
        context.getGrid().updateOrder(col, event);
      } else {
        context.getGrid().autoFitColumn(col, !EventUtils.hasModifierKey(event));
      }

      state = EventState.CONSUMED;
    }

    return state;
  }

  @Override
  public void render(CellContext context, String value, SafeHtmlBuilder sb) {
    renderHeader(context, value, sb);
  }

  public void renderHeader(CellContext context, String columnId, SafeHtmlBuilder sb) {
    if (!BeeUtils.isEmpty(caption)) {
      sb.append(template.caption(captionId, STYLE_CAPTION,
          SafeHtmlUtils.fromTrustedString(caption)));
    }

    CellGrid grid = context.getGrid();
    if (grid != null && grid.isSortable(columnId)) {

      Order sortOrder = grid.getSortOrder();
      int size = (sortOrder == null) ? 0 : sortOrder.getSize();

      int sortIndex = (size > 0) ? sortOrder.getIndex(columnId) : BeeConst.UNDEF;

      if (sortIndex >= 0) {
        boolean ascending = sortOrder.getColumns().get(sortIndex).isAscending();
        String classes = StyleUtils.buildClasses(STYLE_SORT_INFO,
            ascending ? STYLE_ASCENDING : STYLE_DESCENDING);
        String sortInfo = (size > 1) ? BeeUtils.toString(sortIndex + 1) : BeeConst.STRING_EMPTY;
        sb.append(template.sorted(sortInfoId, classes, sortInfo));
      } else {
        sb.append(template.sortable(sortInfoId,
            StyleUtils.buildClasses(STYLE_SORT_INFO, STYLE_SORTABLE)));
      }
    }
  }

  public void setCaption(String caption) {
    this.caption = caption;
    Element el = DomUtils.getElementQuietly(captionId);

    if (el != null) {
      el.setInnerHTML(caption);
    }
  }
}
