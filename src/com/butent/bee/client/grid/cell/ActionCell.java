package com.butent.bee.client.grid.cell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public class ActionCell extends AbstractCell<String> implements HasOptions, HasViewName {

  public enum Type {
    LINK("Link"), BUTTON("Button");

    private final String styleSuffix;

    private Type(String styleSuffix) {
      this.styleSuffix = styleSuffix;
    }

    private String getStyleName() {
      return BeeConst.CSS_CLASS_PREFIX + "ActionCell" + styleSuffix;
    }

    private SafeHtml render(String value) {
      switch (this) {
        case LINK:
          return TEMPLATE.link(getStyleName(), value);
        case BUTTON:
          return TEMPLATE.button(getStyleName(), value);
        default:
          Assert.untouchable();
          return null;
      }
    }
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<button class=\"{0}\">{1}</button>")
    SafeHtml button(String styleName, String option);

    @Template("<div class=\"{0}\">{1}</div>")
    SafeHtml link(String styleName, String option);
  }

  private static final Template TEMPLATE = GWT.create(Template.class);

  private static final Type DEFAULT_TYPE = Type.LINK;

  public static ActionCell create(String viewName, ColumnDescription columnDescription) {
    ActionCell cell = new ActionCell(EnumUtils.getEnumByName(Type.class,
        columnDescription.getElementType()));

    cell.setViewName(viewName);
    cell.setOptions(columnDescription.getOptions());

    return cell;
  }

  private final Type type;

  private String viewName;

  private String options;

  public ActionCell() {
    this(DEFAULT_TYPE);
  }

  public ActionCell(Type type) {
    super(EventUtils.EVENT_TYPE_CLICK);
    this.type = (type == null) ? DEFAULT_TYPE : type;
  }

  public ActionCell copy() {
    ActionCell copy = new ActionCell(type);

    copy.setViewName(getViewName());
    copy.setOptions(getOptions());

    return copy;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public EventState onBrowserEvent(CellContext context, Element parent, String value, Event event) {
    EventState state = super.onBrowserEvent(context, parent, value, event);

    if (state.proceed() && EventUtils.isClick(event)) {
      RowActionEvent.fireCellClick(getViewName(), context.getRow(), getOptions());
      state = EventState.CONSUMED;
    }

    return state;
  }

  @Override
  public void render(CellContext context, String value, SafeHtmlBuilder sb) {
    if (!BeeUtils.isEmpty(value)) {
      sb.append(type.render(value));
    }
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }
}
