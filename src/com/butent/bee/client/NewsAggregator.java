package com.butent.bee.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.mail.MailKeeper;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.widget.Badge;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.news.Subscription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NewsAggregator implements HandlesAllDataEvents {

  public interface HeadlineAccessor {
    void access(Long id);

    boolean read(Long id);
  }

  private final class HeadlinePanel extends Flow {

    private static final String STYLE_HEADLINE_PREFIX = STYLE_PREFIX + "headline-";
    private static final String STYLE_NEW = STYLE_HEADLINE_PREFIX + "new";
    private static final String STYLE_UPD = STYLE_HEADLINE_PREFIX + "upd";

    private final long dataId;
    private final boolean isNew;

    private HeadlinePanel(Headline headline) {
      super(STYLE_HEADLINE_PREFIX + "panel");

      this.dataId = headline.getId();
      this.isNew = headline.isNew();

      CustomDiv typeWidget = new CustomDiv(headline.isNew() ? STYLE_NEW : STYLE_UPD);
      add(typeWidget);

      Label captionWidget = new Label(headline.getCaption());
      captionWidget.addStyleName(STYLE_HEADLINE_PREFIX + "caption");

      if (!BeeUtils.isEmpty(headline.getSubtitle())) {
        captionWidget.setTitle(headline.getSubtitle());
      }

      captionWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          readHeadline(HeadlinePanel.this);
          UiHelper.closeDialog(ScreenImpl.NOTIFICATION_CONTENT);
        }
      });

      add(captionWidget);

      Label dismiss = new Label(String.valueOf(BeeConst.CHAR_TIMES));
      dismiss.addStyleName(STYLE_HEADLINE_PREFIX + "dismiss");

      dismiss.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          dismissHeadline(HeadlinePanel.this);
        }
      });

      add(dismiss);
    }

    private long getDataId() {
      return dataId;
    }

    private Feed getFeed() {
      for (Widget parent = getParent(); parent != null; parent = parent.getParent()) {
        if (parent instanceof SubscriptionPanel) {
          return ((SubscriptionPanel) parent).getFeed();
        }
      }
      return null;
    }

    private boolean isNew() {
      return isNew;
    }
  }

  private final class NewsPanel extends Flow {

    private static final String STYLE_LOADING = STYLE_PREFIX + "loading";
    private static final String STYLE_NOT_LOADING = STYLE_PREFIX + "not-loading";

    private static final String STYLE_NOTHING_HAPPENS = STYLE_PREFIX + "nothing-happens";

    private final FaLabel disclosureWidget;
    private final FaLabel loadingWidget;

    private final Flow content;

    private boolean open;

    private NewsPanel() {
      super(STYLE_PREFIX + "panel");
      addStyleName(STYLE_APATHY);
      addStyleName(STYLE_NOTHING_HAPPENS);

      Flow header = new Flow(STYLE_PREFIX + "header");

      this.disclosureWidget = new FaLabel(FontAwesome.CARET_RIGHT);
      disclosureWidget.addStyleName(STYLE_PREFIX + "disclosure");

      disclosureWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          toggleOpen();
        }
      });

      header.add(disclosureWidget);

      FaLabel refreshWidget = new FaLabel(FontAwesome.REFRESH);
      refreshWidget.setTitle(Localized.getConstants().actionRefresh());
      refreshWidget.addStyleName(STYLE_PREFIX + "refresh");

      refreshWidget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          refresh();
        }
      });

      header.add(refreshWidget);

      this.loadingWidget = new FaLabel(FontAwesome.SPINNER);
      loadingWidget.addStyleName(STYLE_NOT_LOADING);

      header.add(loadingWidget);

      add(header);

      this.content = new Flow(STYLE_PREFIX + "content");
      add(content);
    }

    private void addSubscriptionPanel(SubscriptionPanel subscriptionPanel) {
      if (content.isEmpty()) {
        removeStyleName(STYLE_NOTHING_HAPPENS);
      }
      content.add(subscriptionPanel);
    }

    private SubscriptionPanel asSubscriptionPanel(Widget widget) {
      if (widget instanceof SubscriptionPanel) {
        return (SubscriptionPanel) widget;
      } else {
        return null;
      }
    }

    private void clearSubscriptions() {
      if (!content.isEmpty()) {
        content.clear();
        addStyleName(STYLE_NOTHING_HAPPENS);
      }
    }

    private void endRefresh() {
      loadingWidget.addStyleName(STYLE_NOT_LOADING);
      loadingWidget.removeStyleName(STYLE_LOADING);
    }

    private SubscriptionPanel findSubscriptionPanel(Feed feed) {
      for (Widget widget : content) {
        SubscriptionPanel subscriptionPanel = asSubscriptionPanel(widget);
        if (subscriptionPanel != null && subscriptionPanel.getFeed() == feed) {
          return subscriptionPanel;
        }
      }
      return null;
    }

    private Map<Feed, Boolean> getOpenness() {
      EnumMap<Feed, Boolean> result = new EnumMap<>(Feed.class);

      for (Widget widget : content) {
        SubscriptionPanel subscriptionPanel = asSubscriptionPanel(widget);
        if (subscriptionPanel != null) {
          result.put(subscriptionPanel.getFeed(), subscriptionPanel.isOpen());
        }
      }

      return result;
    }

    private boolean isOpen() {
      return open;
    }

    private boolean removeHeadline(Feed feed, long dataId) {
      SubscriptionPanel subscriptionPanel = findSubscriptionPanel(feed);

      if (subscriptionPanel == null) {
        return false;

      } else {
        boolean ok = subscriptionPanel.removeHeadline(dataId);

        if (ok && !subscriptionPanel.hasHeadlines()) {
          content.remove(subscriptionPanel);
          if (content.isEmpty()) {
            addStyleName(STYLE_NOTHING_HAPPENS);
          }
        }

        return ok;
      }
    }

    private boolean removeSubscription(Feed feed) {
      SubscriptionPanel subscriptionPanel = findSubscriptionPanel(feed);

      if (subscriptionPanel == null) {
        return false;

      } else {
        boolean ok = content.remove(subscriptionPanel);
        if (content.isEmpty()) {
          addStyleName(STYLE_NOTHING_HAPPENS);
        }

        return ok;
      }
    }

    private void setOpen(boolean open) {
      this.open = open;
    }

    private void startRefresh() {
      loadingWidget.addStyleName(STYLE_LOADING);
      loadingWidget.removeStyleName(STYLE_NOT_LOADING);
    }

    private void toggleOpen() {
      setOpen(!isOpen());
      disclosureWidget.setChar(isOpen() ? FontAwesome.CARET_DOWN : FontAwesome.CARET_RIGHT);

      for (Widget widget : content) {
        SubscriptionPanel subscriptionPanel = asSubscriptionPanel(widget);
        if (subscriptionPanel != null && subscriptionPanel.isOpen() != isOpen()) {
          subscriptionPanel.toggleOpen();
        }
      }
    }
  }

  private final class SubscriptionPanel extends Flow {

    private static final String STYLE_SUBSCRIPTION_PREFIX = STYLE_PREFIX + "subscription-";
    private static final String STYLE_CLOSED = STYLE_SUBSCRIPTION_PREFIX + "closed";

    private final Feed feed;

    private final FaLabel disclosure;
    private final Badge newSize;
    private final Badge updSize;

    private final Flow content;

    private boolean open;

    private SubscriptionPanel(Subscription subscription, boolean open) {
      super(STYLE_SUBSCRIPTION_PREFIX + "panel");
      if (!open) {
        addStyleName(STYLE_CLOSED);
      }

      this.feed = subscription.getFeed();
      this.open = open;

      Flow header = new Flow(STYLE_SUBSCRIPTION_PREFIX + "header");

      this.disclosure = new FaLabel(open ? FontAwesome.CARET_DOWN : FontAwesome.CARET_RIGHT);
      disclosure.addStyleName(STYLE_SUBSCRIPTION_PREFIX + "disclosure");

      disclosure.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          toggleOpen();
        }
      });

      header.add(disclosure);

      Label feedLabel = new Label(subscription.getLabel());
      feedLabel.addStyleName(STYLE_SUBSCRIPTION_PREFIX + "label");

      feedLabel.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          toggleOpen();
        }
      });

      header.add(feedLabel);

      this.newSize = new Badge(subscription.countNew(), STYLE_SUBSCRIPTION_PREFIX + "new-size");
      header.add(newSize);

      this.updSize = new Badge(subscription.countUpdated(), STYLE_SUBSCRIPTION_PREFIX + "upd-size");
      header.add(updSize);

      FaLabel filter = new FaLabel(FontAwesome.FILTER);
      filter.addStyleName(STYLE_SUBSCRIPTION_PREFIX + "filter");
      filter.setTitle(Localized.getConstants().actionFilter());

      filter.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          onFilter(getFeed());
        }
      });

      header.add(filter);

      add(header);

      this.content = new Flow(STYLE_SUBSCRIPTION_PREFIX + "content");

      for (Headline headline : subscription.getHeadlines()) {
        HeadlinePanel headlinePanel = new HeadlinePanel(headline);
        content.add(headlinePanel);
      }

      add(content);
    }

    private HeadlinePanel findHeadlinePanel(long dataId) {
      for (Widget widget : content) {
        if (widget instanceof HeadlinePanel && ((HeadlinePanel) widget).getDataId() == dataId) {
          return (HeadlinePanel) widget;
        }
      }
      return null;
    }

    private Feed getFeed() {
      return feed;
    }

    private boolean hasHeadlines() {
      return !content.isEmpty();
    }

    private boolean isOpen() {
      return open;
    }

    private boolean removeHeadline(long dataId) {
      HeadlinePanel headlinePanel = findHeadlinePanel(dataId);

      if (headlinePanel == null) {
        return false;

      } else {
        boolean ok = content.remove(headlinePanel);

        if (ok) {
          if (headlinePanel.isNew()) {
            newSize.decrement();
          } else {
            updSize.decrement();
          }
        }

        return ok;
      }
    }

    private void setOpen(boolean open) {
      this.open = open;
    }

    private void toggleOpen() {
      setOpen(!isOpen());

      setStyleName(STYLE_CLOSED, !isOpen());
      disclosure.setChar(isOpen() ? FontAwesome.CARET_DOWN : FontAwesome.CARET_RIGHT);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(NewsAggregator.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "News-";
  private static final String STYLE_APATHY = STYLE_PREFIX + "apathy";

  private final List<Subscription> subscriptions = new ArrayList<>();

  private final NewsPanel newsPanel = new NewsPanel();

  private Badge sizeBadge;

  private final EnumMap<Feed, BiConsumer<GridOptions, PresenterCallback>> registeredFilterHandlers =
      new EnumMap<>(Feed.class);

  private final Map<String, HeadlineAccessor> registeredAccessHandlers = new HashMap<>();

  NewsAggregator() {
  }

  public int countNews() {
    int count = 0;
    for (Subscription subscription : subscriptions) {
      count += subscription.size();
    }
    return count;
  }

  public void filterNews(String input, ViewCallback callback) {
    Assert.notEmpty(input);
    Assert.notNull(callback);

    Feed feed = EnumUtils.getEnumByName(Feed.class, input);
    if (feed == null) {
      callback.onFailure("cannot parse feed", input);

    } else {
      Subscription subscription = findSubscription(feed);
      if (subscription == null) {
        callback.onFailure("subscription not found for", feed.name());
      } else {
        doFilter(feed, subscription, ViewFactory.getPresenterCallback(callback));
      }
    }
  }

  public IdentifiableWidget getNewsPanel() {
    return newsPanel;
  }

  public boolean hasNews() {
    for (Subscription subscription : subscriptions) {
      if (!subscription.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasSubscription(Feed feed) {
    Assert.notNull(feed);
    return findSubscription(feed) != null;
  }

  public void loadSubscriptions(String serialized, boolean notify) {
    String[] arr = Codec.beeDeserializeCollection(serialized);

    if (ArrayUtils.isEmpty(arr)) {
      logger.severe("cannot deserialize subscriptions");

    } else {
      Map<Feed, Boolean> openness = newsPanel.getOpenness();

      clear(false);

      List<String> notifyMsg = new ArrayList<>();

      for (String s : arr) {
        final Subscription subscription = Subscription.restore(s);
        subscriptions.add(subscription);

        if (!subscription.isEmpty()) {
          boolean open = openness.containsKey(subscription.getFeed())
              ? openness.get(subscription.getFeed()) : newsPanel.isOpen();
          SubscriptionPanel subscriptionPanel = new SubscriptionPanel(subscription, open);

          newsPanel.addSubscriptionPanel(subscriptionPanel);

          if (notify) {
            notifyMsg.add(BeeUtils.joinWords(subscription.getLabel(),
                BeeUtils.bracket(subscription.countNew() + subscription.countUpdated())));
          }
        }
      }

      if (!notifyMsg.isEmpty()) {
        Global.showBrowserNotify(BeeUtils.buildLines(notifyMsg));
      }

      newsPanel.removeStyleName(STYLE_APATHY);

      updateHeader();
      logger.info("subscriptions", subscriptions.size(), countNews());
    }
  }

  public void onAccess(String viewName, long rowId) {
    if (registeredAccessHandlers.containsKey(viewName)) {
      registeredAccessHandlers.get(viewName).access(rowId);
    }

    String table = Data.getViewTable(viewName);

    if (NewsConstants.hasUsageTable(table)) {
      ParameterList parameters = BeeKeeper.getRpc().createParameters(Service.ACCESS);
      parameters.addQueryItem(Service.VAR_TABLE, table);
      parameters.addQueryItem(Service.VAR_ID, rowId);

      BeeKeeper.getRpc().makeRequest(parameters);
    }

    removeData(viewName, rowId);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (requiresRefresh(event)) {
      refresh();
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (requiresRefresh(event)) {
      refresh();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (event.hasView(NewsConstants.VIEW_USER_FEEDS)) {
      for (RowInfo rowInfo : event.getRows()) {
        Subscription subscription = findSubscription(rowInfo.getId());
        if (subscription != null) {
          removeSubscription(subscription);
        }
      }

    } else if (requiresRefresh(event)) {
      refresh();
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (event.hasView(NewsConstants.VIEW_USER_FEEDS)) {
      Subscription subscription = findSubscription(event.getRowId());
      if (subscription != null) {
        removeSubscription(subscription);
      }

    } else if (requiresRefresh(event)) {
      refresh();
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (requiresRefresh(event)) {
      refresh();
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (requiresRefresh(event)) {
      refresh();
    }
  }

  public void refresh() {
    newsPanel.startRefresh();

    BeeKeeper.getRpc().makeGetRequest(Service.GET_NEWS, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.hasErrors()) {
          if (response.hasResponse()) {
            loadSubscriptions(response.getResponseAsString(), true);
          } else {
            clear(true);
          }
        }

        newsPanel.endRefresh();
      }
    });
  }

  public void registerAccessHandler(String viewName, HeadlineAccessor handler) {
    Assert.notEmpty(viewName);
    Assert.notNull(handler);

    registeredAccessHandlers.put(viewName, handler);
  }

  public void registerFilterHandler(Feed feed, BiConsumer<GridOptions, PresenterCallback> handler) {
    Assert.notNull(feed);
    Assert.notNull(handler);

    registeredFilterHandlers.put(feed, handler);
  }

  public boolean removeSubscription(Subscription subscription) {
    if (subscriptions.contains(subscription)) {
      if (subscriptions.size() == 1) {
        clear(true);

      } else {
        subscriptions.remove(subscription);
        newsPanel.removeSubscription(subscription.getFeed());

        updateHeader();
      }

      logger.info("unsubscribed", subscription.getFeed());
      return true;

    } else {
      return false;
    }
  }

  private void clear(boolean clearSizeBadge) {
    if (!subscriptions.isEmpty()) {
      subscriptions.clear();
      newsPanel.addStyleName(STYLE_APATHY);
    }
    newsPanel.clearSubscriptions();

    if (clearSizeBadge && getSizeBadge() != null) {
      getSizeBadge().setValue(0);
    }
  }

  private void dismissHeadline(HeadlinePanel headlinePanel) {
    Feed feed = headlinePanel.getFeed();
    if (feed != null) {
      onAccess(feed.getHeadlineView(), headlinePanel.getDataId());
    }
  }

  private void doFilter(Feed feed, Subscription subscription, PresenterCallback callback) {
    String caption = BeeUtils.join(" - ", Domain.NEWS.getCaption(), feed.getCaption());

    Set<Long> idSet = subscription.getIdSet();
    Filter filter = idSet.isEmpty() ? Filter.isFalse() : Filter.idIn(idSet);

    GridOptions gridOptions = GridOptions.forFeed(feed, caption, filter);

    if (registeredFilterHandlers.containsKey(feed)) {
      registeredFilterHandlers.get(feed).accept(gridOptions, callback);

    } else {
      String gridName = feed.getHeadlineView();
      GridFactory.openGrid(gridName, GridFactory.getGridInterceptor(gridName), gridOptions,
          callback);
    }
  }

  private List<Subscription> filterSubscriptions(String viewName) {
    List<Subscription> result = new ArrayList<>();

    for (Subscription subscription : subscriptions) {
      if (Data.sameTable(viewName, subscription.getHeadlineView())) {
        result.add(subscription);
      }
    }

    return result;
  }

  private Subscription findSubscription(Feed feed) {
    for (Subscription subscription : subscriptions) {
      if (subscription.getFeed() == feed) {
        return subscription;
      }
    }
    return null;
  }

  private Subscription findSubscription(long rowId) {
    for (Subscription subscription : subscriptions) {
      if (subscription.getRowId() == rowId) {
        return subscription;
      }
    }
    return null;
  }

  private Badge getSizeBadge() {
    return sizeBadge;
  }

  private void onFilter(Feed feed) {
    Subscription subscription = findSubscription(feed);
    if (subscription != null && !subscription.isEmpty()) {
      doFilter(feed, subscription, ViewHelper.getPresenterCallback());
    }
  }

  private void readHeadline(HeadlinePanel headlinePanel) {
    Feed feed = headlinePanel.getFeed();

    if (feed != null && (!registeredAccessHandlers.containsKey(feed.getHeadlineView())
        || !registeredAccessHandlers.get(feed.getHeadlineView()).read(headlinePanel.getDataId()))) {

      RowEditor.open(feed.getHeadlineView(), headlinePanel.getDataId(), Opener.modeless());
    }
  }

  private void removeData(String viewName, long rowId) {
    List<Subscription> filteredSubscriptions = filterSubscriptions(viewName);
    if (!filteredSubscriptions.isEmpty()) {
      removeHeadline(filteredSubscriptions, rowId);
    }
  }

  private void removeHeadline(List<Subscription> subs, long rowId) {
    boolean changed = false;

    for (Subscription subscription : subs) {
      if (subscription.contains(rowId)) {
        subscription.remove(rowId);
        newsPanel.removeHeadline(subscription.getFeed(), rowId);

        changed = true;
      }
    }

    if (changed) {
      updateHeader();
    }
  }

  private boolean requiresRefresh(ModificationEvent<?> event) {
    if (event == null) {
      return false;

    } else if (event.hasView(NewsConstants.VIEW_USER_FEEDS)) {
      return true;

    } else if (subscriptions.isEmpty()) {
      return false;

    } else {
      boolean requires = false;
      String table = Data.getViewTable(event.getViewName());

      if (!BeeUtils.isEmpty(table)) {
        for (Subscription subscription : subscriptions) {
          if (table.equals(subscription.getTable())
              || table.equals(Data.getViewTable(subscription.getHeadlineView()))) {
            requires = true;
            break;
          }
        }
      }

      return requires;
    }
  }

  private void setSizeBadge(Badge sizeBadge) {
    this.sizeBadge = sizeBadge;
  }

  private void updateHeader() {

    MailKeeper.getUnreadCount();

    int size = countNews();
    ScreenImpl.updateNewsSize(size);

    if (getSizeBadge() == null) {
      Badge badge = new Badge(size, STYLE_PREFIX + "size");

      setSizeBadge(badge);

    } else {
      getSizeBadge().update(size);
    }
  }
}
