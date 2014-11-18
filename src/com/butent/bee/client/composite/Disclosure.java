package com.butent.bee.client.composite;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Disclosure extends Flow {

  private final class Header extends Flow implements ClickHandler, SummaryChangeEvent.Handler {

    private final Map<String, Value> summaryValues = new LinkedHashMap<>();

    private Header(boolean open, Widget widget) {
      super(STYLE_HEADER);

      Toggle toggle =
          new Toggle(FontAwesome.CARET_RIGHT, FontAwesome.CARET_DOWN, STYLE_TOGGLE, open);
      add(toggle);

      toggle.addClickHandler(this);

      if (widget != null) {
        add(widget);
      }

      CustomDiv summaryWidget = new CustomDiv(STYLE_SUMMARY);
      add(summaryWidget);
    }

    @Override
    public void onClick(ClickEvent event) {
      setOpen(!isOpen());
    }

    @Override
    public void onSummaryChange(SummaryChangeEvent event) {
      Value oldValue = summaryValues.get(event.getSourceId());

      if (!Objects.equals(event.getValue(), oldValue)) {
        summaryValues.put(event.getSourceId(), event.getValue());

        Widget summaryWidget = UiHelper.getChildByStyleName(this, STYLE_SUMMARY);
        if (summaryWidget != null) {
          String summary = SummaryChangeEvent.renderSummary(summaryValues.values());
          summaryWidget.getElement().setInnerText(summary);
        }
      }
    }

    private Widget getWidget() {
      return (getWidgetCount() > 2) ? getWidget(1) : null;
    }

    private void setWidget(Widget widget) {
      if (getWidget() != null) {
        remove(getWidget());
      }

      if (widget != null) {
        widget.addStyleName(STYLE_LABEL);
        insert(widget, 1);

        if (widget instanceof HasClickHandlers) {
          ((HasClickHandlers) widget).addClickHandler(this);
        }
      }
    }

    private void refresh() {
      Toggle toggle = UiHelper.getChild(this, Toggle.class);
      if (toggle != null) {
        toggle.setChecked(isOpen());
      }
    }
  }

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "Disclosure";
  private static final String STYLE_OPEN = STYLE_NAME + "-open";
  private static final String STYLE_CLOSED = STYLE_NAME + "-closed";

  private static final String STYLE_HEADER = STYLE_NAME + "-header";
  private static final String STYLE_TOGGLE = STYLE_NAME + "-toggle";
  private static final String STYLE_LABEL = STYLE_NAME + "-label";
  private static final String STYLE_SUMMARY = STYLE_NAME + "-summary";
  private static final String STYLE_CONTENT = STYLE_NAME + "-content";

  private final Header header;

  private boolean open;

  public Disclosure(boolean open) {
    this(open, null);
  }

  public Disclosure(boolean open, Widget headerWidget) {
    super(STYLE_NAME);

    this.open = open;

    this.header = new Header(open, headerWidget);
    super.add(header);
  }

  @Override
  public void add(Widget w) {
    Assert.notNull(w);

    if (header.getWidget() == null) {
      header.setWidget(w);

    } else if (getWidgetCount() == 1) {
      Widget content = prepareContent(w);
      super.add(content);
      refresh();

    } else {
      Assert.state(false, "Disclosure already contains header and content");
    }
  }

  @Override
  public String getIdPrefix() {
    return "disclosure";
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    if (isOpen() != open) {
      this.open = open;
      refresh();
    }
  }

  private Widget prepareContent(Widget widget) {
    Simple wrapper = new Simple(widget);
    wrapper.addStyleName(STYLE_CONTENT);

    Collection<HasSummaryChangeHandlers> summarySources = SummaryChangeEvent.findSources(widget);
    if (!BeeUtils.isEmpty(summarySources)) {
      for (HasSummaryChangeHandlers summarySource : summarySources) {
        summarySource.addSummaryChangeHandler(header);
      }
    }

    return wrapper;
  }

  private void refresh() {
    if (isOpen()) {
      removeStyleName(STYLE_CLOSED);
      addStyleName(STYLE_OPEN);
    } else {
      removeStyleName(STYLE_OPEN);
      addStyleName(STYLE_CLOSED);
    }

    header.refresh();
  }
}
