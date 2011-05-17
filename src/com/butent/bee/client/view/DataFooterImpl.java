package com.butent.bee.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.view.navigation.PageResizer;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.navigation.SimplePager;
import com.butent.bee.client.view.search.SearchBox;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.event.SelectionCountChangeEvent;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

/**
 * Implements styling and user command capture for data footers.
 */

public class DataFooterImpl extends Absolute implements DataFooterView, HasNavigation, HasSearch {

  /**
   * Specifies which styling resources to use for a data footer implementation.
   */
  public interface Resources extends ClientBundle {
    @Source("DataFooterImpl.css")
    Style footerStyle();
  }

  /**
   * Specifies which styling aspects have to be implemented on data footer implementations.
   */
  public interface Style extends CssResource {
    String container();

    int horizontalMargin();

    String pageResizer();

    int resizerWidth();

    String search();

    String selectionCounter();

    int selectionCounterWidth();

    String simplePager();

    int spacing();

    int top();
  }

  private static Resources defaultResources = null;
  private static Style defaultStyle = null;

  private static Resources getDefaultResources() {
    if (defaultResources == null) {
      defaultResources = GWT.create(Resources.class);
    }
    return defaultResources;
  }

  private static Style getDefaultStyle() {
    if (defaultStyle == null) {
      defaultStyle = getDefaultResources().footerStyle();
      defaultStyle.ensureInjected();
    }
    return defaultStyle;
  }

  private Presenter viewPresenter = null;

  private String pagerId = null;
  private String resizerId = null;
  private String searchId = null;
  private String selectionCounterId = null;

  private boolean adjusted = false;

  public DataFooterImpl() {
    super();
    addStyleName(StyleUtils.WINDOW_FOOTER);
  }

  public void create(int rowCount, int pageSize, boolean addPaging, boolean addSearch) {
    Style style = getDefaultStyle();
    addStyleName(style.container());

    int top = style.top();
    int spacing = style.spacing();
    int margin = style.horizontalMargin();

    int pagerWidth = 256;
    int resizerWidth = style.resizerWidth();
    int selectionCounterWidth = style.selectionCounterWidth();

    int left = margin;

    if (addPaging) {
      SimplePager pager = new SimplePager(rowCount);
      pager.addStyleName(style.simplePager());
      add(pager, left, top);
      left += pagerWidth + spacing;
      pagerId = pager.getWidgetId();

      PageResizer pageResizer = new PageResizer(pageSize);
      pageResizer.addStyleName(style.pageResizer());
      add(pageResizer, left, top);
      left += resizerWidth + spacing;
      resizerId = pageResizer.getWidgetId();
    }

    if (addSearch) {
      SearchBox search = new SearchBox();
      search.addStyleName(style.search());
      add(search, left, top);
      int right = (selectionCounterWidth > 0) ? spacing + selectionCounterWidth + margin : margin;
      StyleUtils.setRight(search, right);
      searchId = search.getWidgetId();
    }
    
    if (selectionCounterWidth > 0) {
      BeeLabel selectionCounter = new BeeLabel();
      selectionCounter.addStyleName(style.selectionCounter());
      add(selectionCounter);
      selectionCounterId = selectionCounter.getId();
    }
  }

  public Collection<PagerView> getPagers() {
    return ViewHelper.getPagers(this);
  }

  public Collection<SearchView> getSearchers() {
    return ViewHelper.getSearchers(this);
  }

  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  public String getWidgetId() {
    return getId();
  }

  public void onSelectionCountChange(SelectionCountChangeEvent event) {
    Assert.notNull(event);
    if (selectionCounterId != null) {
      int cnt = event.getCount();
      String text = (cnt > 0) ? BeeUtils.toString(cnt) : BeeConst.STRING_EMPTY;
      DomUtils.setText(selectionCounterId, text);
    }
  }

  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    if (!adjusted) {
      adjusted = true;
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          adjust();
        }
      });
    }
  }

  private void adjust() {
    if (BeeUtils.isEmpty(pagerId)) {
      return;
    }
    int pagerWidth = DomUtils.getChildOffsetWidth(this, pagerId);
    if (pagerWidth <= 0) {
      return;
    }

    Style style = getDefaultStyle();
    int spacing = style.spacing();
    int resizerWidth = style.resizerWidth();
    int margin = style.horizontalMargin();

    int left = margin + pagerWidth + spacing;

    if (!BeeUtils.isEmpty(resizerId)) {
      StyleUtils.setLeft(DomUtils.getChild(this, resizerId), left);
      left += resizerWidth + spacing;
    }
    if (!BeeUtils.isEmpty(searchId)) {
      StyleUtils.setLeft(DomUtils.getChild(this, searchId), left);
    }
  }
}
