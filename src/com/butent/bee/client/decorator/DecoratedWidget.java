package com.butent.bee.client.decorator;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsFunction;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class DecoratedWidget extends Panel implements IdentifiableWidget {

  private final Widget widget;

  private final JsFunction onInserted;
  private final JsFunction onRemoved;

  private int insertCounter;
  private int removeCounter;

  public DecoratedWidget(Widget widget, Element element, JsFunction onInserted,
      JsFunction onRemoved) {
    this.widget = Assert.notNull(widget);
    setElement(Assert.notNull(element));
    this.onInserted = onInserted;
    this.onRemoved = onRemoved;

    widget.removeFromParent();
    adopt(widget);

    if (BeeUtils.isEmpty(element.getId())) {
      DomUtils.createId(element, getIdPrefix());
    }
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "decorator";
  }

  public Widget getWidget() {
    return widget;
  }

  @Override
  public Iterator<Widget> iterator() {
    return new Iterator<Widget>() {
      int counter;

      @Override
      public boolean hasNext() {
        return counter == 0;
      }

      @Override
      public Widget next() {
        if (counter > 0) {
          throw new NoSuchElementException();
        }
        counter++;
        return widget;
      }

      @Override
      public void remove() {
        if (counter == 1) {
          DecoratedWidget.this.remove(widget);
        }
      }
    };
  }

  @Override
  public boolean remove(Widget child) {
    Assert.unsupported();
    return false;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    insertCounter++;
    if (onInserted != null && insertCounter <= 1) {
      onInserted.call(getElement());
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();

    removeCounter++;
    if (onRemoved != null && removeCounter <= 1) {
      onRemoved.call(getElement());
    }
  }
}
