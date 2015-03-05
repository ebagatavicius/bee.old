package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.WidgetCollection;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Layout.Alignment;
import com.butent.bee.client.layout.Layout.AnimationCallback;
import com.butent.bee.client.layout.Layout.Layer;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;

public class LayoutPanel extends ComplexPanel implements AnimatedLayout, RequiresResize,
    ProvidesResize, IdentifiableWidget {

  private static final CssUnit DEFAULT_UNIT = CssUnit.PX;

  private final Layout layout;
  private final LayoutCommand layoutCmd;

  public LayoutPanel() {
    super();

    setElement(Document.get().createDivElement());
    layout = new Layout(getElement());
    layoutCmd = new LayoutCommand(layout);

    DomUtils.createId(this, getIdPrefix());
  }

  public LayoutPanel(Widget widget) {
    this();
    add(widget);
  }

  @Override
  public void add(Widget widget) {
    Assert.notNull(widget);
    insert(widget, getWidgetCount());

    DomUtils.createId(getWidgetContainerElement(widget), "container");
    getWidgetContainerElement(widget)
        .setClassName(BeeConst.CSS_CLASS_PREFIX + "LayoutContainer");
  }

  public void add(Widget widget, boolean scroll) {
    add(widget);

    if (scroll) {
      getWidgetContainerElement(widget).getStyle().setOverflow(Overflow.AUTO);
    }
  }

  public String addLeftRight(Widget widget, double left, CssUnit leftUnit,
      double right, CssUnit rightUnit) {
    add(widget);
    setWidgetLeftRight(widget, left, leftUnit, right, rightUnit);

    return DomUtils.getId(widget);
  }

  public String addLeftRight(Widget widget, int left, int right) {
    return addLeftRight(widget, left, DEFAULT_UNIT, right, DEFAULT_UNIT);
  }

  public String addLeftRightTop(Widget widget, double left, CssUnit leftUnit,
      double right, CssUnit rightUnit, double top, CssUnit topUnit) {
    add(widget);
    setWidgetLeftRight(widget, left, leftUnit, right, rightUnit);

    setWidgetTopBottom(widget, top, topUnit, 0, CssUnit.PX);
    setWidgetVerticalPosition(widget, Alignment.BEGIN);

    return DomUtils.getId(widget);
  }

  public String addLeftRightTop(Widget widget, int left, int right, int top) {
    return addLeftRightTop(widget, left, DEFAULT_UNIT, right, DEFAULT_UNIT, top, DEFAULT_UNIT);
  }

  public String addLeftTop(Widget widget, double left, CssUnit leftUnit,
      double top, CssUnit topUnit) {
    add(widget);
    setWidgetLeftRight(widget, left, leftUnit, 0, CssUnit.PX);
    setWidgetHorizontalPosition(widget, Alignment.BEGIN);

    setWidgetTopBottom(widget, top, topUnit, 0, CssUnit.PX);
    setWidgetVerticalPosition(widget, Alignment.BEGIN);

    return DomUtils.getId(widget);
  }

  public String addLeftTop(Widget widget, int left, int top) {
    return addLeftTop(widget, left, DEFAULT_UNIT, top, DEFAULT_UNIT);
  }

  public String addLeftWidthTop(Widget widget, double left, CssUnit leftUnit,
      double width, CssUnit widthUnit, double top, CssUnit topUnit) {
    add(widget);
    setWidgetLeftWidth(widget, left, leftUnit, width, widthUnit);

    setWidgetTopBottom(widget, top, topUnit, 0, CssUnit.PX);
    setWidgetVerticalPosition(widget, Alignment.BEGIN);

    return DomUtils.getId(widget);
  }

  public String addLeftWidthTop(Widget widget, int left, int width, int top) {
    return addLeftWidthTop(widget, left, DEFAULT_UNIT, width, DEFAULT_UNIT, top, DEFAULT_UNIT);
  }

  public String addRightWidthTop(Widget widget, double right, CssUnit rightUnit,
      double width, CssUnit widthUnit, double top, CssUnit topUnit) {
    add(widget);
    setWidgetRightWidth(widget, right, rightUnit, width, widthUnit);
    setWidgetHorizontalPosition(widget, Alignment.END);

    setWidgetTopBottom(widget, top, topUnit, 0, CssUnit.PX);
    setWidgetVerticalPosition(widget, Alignment.BEGIN);

    return DomUtils.getId(widget);
  }

  public String addRightWidthTop(Widget widget, int right, int width, int top) {
    return addRightWidthTop(widget, right, DEFAULT_UNIT, width, DEFAULT_UNIT, top, DEFAULT_UNIT);
  }

  @Override
  public void animate(int duration) {
    animate(duration, null);
  }

  @Override
  public void animate(final int duration, final AnimationCallback callback) {
    layoutCmd.schedule(duration, callback);
  }

  @Override
  public void forceLayout() {
    layoutCmd.cancel();
    layout.layout();
    onResize();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "layout";
  }

  public Element getWidgetContainerElement(Widget child) {
    return getLayer(child).getContainerElement();
  }

  public void insert(Widget widget, int beforeIndex) {
    widget.removeFromParent();

    getChildren().insert(widget, beforeIndex);

    Layer layer = layout.attachChild(widget.getElement(), widget);
    widget.setLayoutData(layer);

    adopt(widget);

    animate(0);
  }

  @Override
  public void onResize() {
    for (Widget child : getChildren()) {
      if (child instanceof RequiresResize) {
        ((RequiresResize) child).onResize();
      }
    }
  }

  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      layout.removeChild((Layer) w.getLayoutData());
    }
    return removed;
  }

  public void setHorizontalLayout(Widget widget, Double left, CssUnit leftUnit,
      Double right, CssUnit rightUnit, Double width, CssUnit widthUnit) {
    Assert.notNull(widget);

    boolean hasLeft = left != null;
    boolean hasRight = right != null;
    boolean hasWidth = BeeUtils.isPositive(width);

    if (hasLeft && hasRight) {
      setWidgetLeftRight(widget, left, normalizeUnit(leftUnit), right, normalizeUnit(rightUnit));
    } else if (hasLeft && hasWidth) {
      setWidgetLeftWidth(widget, left, normalizeUnit(leftUnit), width, normalizeUnit(widthUnit));
    } else if (hasRight && hasWidth) {
      setWidgetRightWidth(widget, right, normalizeUnit(rightUnit), width, normalizeUnit(widthUnit));
      setWidgetHorizontalPosition(widget, Alignment.END);

    } else if (hasLeft) {
      setWidgetLeftRight(widget, left, normalizeUnit(leftUnit), 0, CssUnit.PX);
    } else if (hasRight) {
      setWidgetLeftRight(widget, 0, CssUnit.PX, right, normalizeUnit(rightUnit));
      setWidgetHorizontalPosition(widget, Alignment.END);
    } else if (hasWidth) {
      setWidgetLeftWidth(widget, 0, CssUnit.PX, width, normalizeUnit(widthUnit));
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setVerticalLayout(Widget widget, Double top, CssUnit topUnit,
      Double bottom, CssUnit bottomUnit, Double height, CssUnit heightUnit) {
    Assert.notNull(widget);

    boolean hasTop = top != null;
    boolean hasBottom = bottom != null;
    boolean hasHeight = BeeUtils.isPositive(height);

    if (hasTop && hasBottom) {
      setWidgetTopBottom(widget, top, normalizeUnit(topUnit), bottom, normalizeUnit(bottomUnit));
    } else if (hasTop && hasHeight) {
      setWidgetTopHeight(widget, top, normalizeUnit(topUnit), height, normalizeUnit(heightUnit));
    } else if (hasBottom && hasHeight) {
      setWidgetBottomHeight(widget, bottom, normalizeUnit(bottomUnit),
          height, normalizeUnit(heightUnit));
      setWidgetVerticalPosition(widget, Alignment.END);

    } else if (hasTop) {
      setWidgetTopBottom(widget, top, normalizeUnit(topUnit), 0, CssUnit.PX);
    } else if (hasBottom) {
      setWidgetTopBottom(widget, 0, CssUnit.PX, bottom, normalizeUnit(bottomUnit));
      setWidgetVerticalPosition(widget, Alignment.END);
    } else if (hasHeight) {
      setWidgetTopHeight(widget, 0, CssUnit.PX, height, normalizeUnit(heightUnit));
    }
  }

  public void setWidgetBottomHeight(Widget child, double bottom,
      CssUnit bottomUnit, double height, CssUnit heightUnit) {
    getLayer(child).setBottomHeight(bottom, bottomUnit, height, heightUnit);
    animate(0);
  }

  public void setWidgetHorizontalPosition(Widget child, Alignment position) {
    getLayer(child).setChildHorizontalPosition(position);
    animate(0);
  }

  public void setWidgetLeftRight(Widget child, double left, CssUnit leftUnit,
      double right, CssUnit rightUnit) {
    getLayer(child).setLeftRight(left, leftUnit, right, rightUnit);
    animate(0);
  }

  public void setWidgetLeftWidth(Widget child, double left, CssUnit leftUnit,
      double width, CssUnit widthUnit) {
    getLayer(child).setLeftWidth(left, leftUnit, width, widthUnit);
    animate(0);
  }

  public void setWidgetRightWidth(Widget child, double right, CssUnit rightUnit,
      double width, CssUnit widthUnit) {
    getLayer(child).setRightWidth(right, rightUnit, width, widthUnit);
    animate(0);
  }

  public void setWidgetTopBottom(Widget child, double top, CssUnit topUnit,
      double bottom, CssUnit bottomUnit) {
    getLayer(child).setTopBottom(top, topUnit, bottom, bottomUnit);
    animate(0);
  }

  public void setWidgetTopHeight(Widget child, double top, CssUnit topUnit,
      double height, CssUnit heightUnit) {
    getLayer(child).setTopHeight(top, topUnit, height, heightUnit);
    animate(0);
  }

  public void setWidgetVerticalPosition(Widget child, Alignment position) {
    getLayer(child).setChildVerticalPosition(position);
    animate(0);
  }

  public void setWidgetVisible(Widget child, boolean visible) {
    getLayer(child).setVisible(visible);
    child.setVisible(visible);
    animate(0);
  }

  @Override
  protected WidgetCollection getChildren() {
    return super.getChildren();
  }

  Layout getLayout() {
    return layout;
  }

  private static Layout.Layer getLayer(Widget child) {
    return (Layout.Layer) child.getLayoutData();
  }

  private static CssUnit normalizeUnit(CssUnit unit) {
    return (unit == null) ? DEFAULT_UNIT : unit;
  }
}
