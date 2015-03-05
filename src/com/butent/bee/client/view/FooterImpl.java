package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.SelectionCountChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.navigation.SimplePager;
import com.butent.bee.client.view.search.SearchBox;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

/**
 * Implements styling and user command capture for data footers.
 */

public class FooterImpl extends Flow implements FooterView, HasNavigation, HasSearch {

  private static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX + "FooterContainer";

  private static final String STYLE_PAGER = BeeConst.CSS_CLASS_PREFIX + "SimplePager";
  private static final String STYLE_SEARCH = BeeConst.CSS_CLASS_PREFIX + "FooterSearch";
  private static final String STYLE_SELECTION_COUNTER = BeeConst.CSS_CLASS_PREFIX
      + "SelectionCounter";

  private static final int HEIGHT = 26;

  private Presenter viewPresenter;

  private String pagerId;
  private String searchId;
  private String selectionCounterId;

  private boolean enabled = true;

  public FooterImpl() {
    super(STYLE_CONTAINER);
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public void create(int maxRowCount, boolean addPaging, boolean showPageSize, boolean addSearch) {
    if (addPaging) {
      SimplePager pager = new SimplePager(maxRowCount, showPageSize);
      pager.addStyleName(STYLE_PAGER);
      add(pager);
      setPagerId(pager.getWidgetId());
    }

    if (addSearch) {
      SearchBox search = new SearchBox();
      search.addStyleName(STYLE_SEARCH);
      add(search);
      setSearchId(search.getWidgetId());
    }

    Label selectionCounter = new Label();
    selectionCounter.addStyleName(STYLE_SELECTION_COUNTER);
    add(selectionCounter);
    setSelectionCounterId(selectionCounter.getId());
  }

  @Override
  public String getCaption() {
    return null;
  }

  @Override
  public int getHeight() {
    return HEIGHT;
  }

  @Override
  public Collection<PagerView> getPagers() {
    return ViewHelper.getPagers(this);
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public Collection<SearchView> getSearchers() {
    return ViewHelper.getSearchers(this);
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (!BeeUtils.isEmpty(getPagerId())) {
      for (PagerView pager : getPagers()) {
        if (pager.asWidget().getElement().isOrHasChild(source)) {
          return pager.onPrint(source, target);
        }
      }
    }

    if (!BeeUtils.isEmpty(getSearchId()) && getSearchId().equals(source.getId())) {
      return !BeeUtils.isEmpty(DomUtils.getValue(source));
    }
    return true;
  }

  @Override
  public void onSelectionCountChange(SelectionCountChangeEvent event) {
    Assert.notNull(event);
    if (!BeeUtils.isEmpty(getSelectionCounterId())) {
      int cnt = event.getCount();
      String text = (cnt > 0) ? BeeUtils.toString(cnt) : BeeConst.STRING_EMPTY;
      DomUtils.setText(getSelectionCounterId(), text);
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    return false;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled == isEnabled()) {
      return;
    }
    this.enabled = enabled;
    UiHelper.enableChildren(this, enabled);
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    ReadyEvent.fire(this);
  }

  private String getPagerId() {
    return pagerId;
  }

  private String getSearchId() {
    return searchId;
  }

  private String getSelectionCounterId() {
    return selectionCounterId;
  }

  private void setPagerId(String pagerId) {
    this.pagerId = pagerId;
  }

  private void setSearchId(String searchId) {
    this.searchId = searchId;
  }

  private void setSelectionCounterId(String selectionCounterId) {
    this.selectionCounterId = selectionCounterId;
  }
}
