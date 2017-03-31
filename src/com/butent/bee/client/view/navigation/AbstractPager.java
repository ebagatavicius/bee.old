package com.butent.bee.client.view.navigation;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.NavigationOrigin;

import java.util.Objects;

/**
 * Is an abstract class with default pager implementation behavior.
 */

public abstract class AbstractPager extends Composite implements PagerView {

  private HasPaging display;

  private HandlerRegistration changeHandler;

  private Presenter viewPresenter;

  @Override
  public String getCaption() {
    return null;
  }

  public HasPaging getDisplay() {
    return display;
  }

  @Override
  public String getId() {
    return getIdentifiableWidget().getId();
  }

  @Override
  public String getIdPrefix() {
    return getIdentifiableWidget().getIdPrefix();
  }

  public int getPageSize() {
    return (getDisplay() == null) ? BeeConst.UNDEF : getDisplay().getPageSize();
  }

  public int getPageStart() {
    return (getDisplay() == null) ? BeeConst.UNDEF : getDisplay().getPageStart();
  }

  @Override
  public Element getPrintElement() {
    return getWidget().getElement();
  }

  public int getRowCount() {
    return (getDisplay() == null) ? BeeConst.UNDEF : getDisplay().getRowCount();
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return DomUtils.getId(getWidget());
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean reactsTo(Action action) {
    return false;
  }

  @Override
  public void setDisplay(HasPaging display) {
    if (changeHandler != null) {
      changeHandler.removeHandler();
      changeHandler = null;
    }

    this.display = display;

    if (display != null) {
      changeHandler = display.addScopeChangeHandler(this);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
  }

  @Override
  public void setId(String id) {
    getIdentifiableWidget().setId(id);
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  public void start(HasPaging displ) {
    if (!Objects.equals(displ, getDisplay())) {
      setDisplay(displ);
    }
  }

  protected abstract NavigationOrigin getNavigationOrigin();

  protected int getPage() {
    int pageSize = getPageSize();
    if (pageSize <= 0) {
      return BeeConst.UNDEF;
    }
    return (getPageStart() + pageSize - 1) / pageSize;
  }

  protected int getPageCount() {
    int pageSize = getPageSize();
    if (pageSize <= 0) {
      return BeeConst.UNDEF;
    }
    return (getRowCount() + pageSize - 1) / pageSize;
  }

  protected boolean hasNextPage() {
    if (getPageSize() <= 0) {
      return false;
    }
    return getPageStart() + getPageSize() < getRowCount();
  }

  protected boolean hasPreviousPage() {
    return getPageStart() > 0 && getRowCount() > 0;
  }

  @Override
  protected void initWidget(Widget widget) {
    if (widget instanceof IdentifiableWidget) {
      super.initWidget(widget);
    } else {
      Assert.unsupported("only IdentifiableWidget can be used as Pager");
    }
  }

  protected void nextPage() {
    if (getPageSize() > 0) {
      setPageStart(getPageStart() + getPageSize());
    }
  }

  protected void previousPage() {
    if (getPageSize() > 0 && getPageStart() > 0) {
      setPageStart(getPageStart() - getPageSize());
    }
  }

  protected void setPageStart(int index) {
    if (getRowCount() > 0) {
      int start;
      if (getPageSize() > 0) {
        start = Math.min(index, getRowCount() - getPageSize());
      } else {
        start = Math.min(index, getRowCount() - 1);
      }

      if (start != getPageStart()) {
        getDisplay().setPageStart(Math.max(0, start), true, true, getNavigationOrigin());
      }
    }
  }

  private IdentifiableWidget getIdentifiableWidget() {
    return (getWidget() == null) ? null : (IdentifiableWidget) getWidget();
  }
}
