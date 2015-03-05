package com.butent.bee.client.modules.administration;

import com.google.gwt.dom.client.OptionElement;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;

public class UserFeedsInterceptor extends AbstractGridInterceptor {

  private static final class FeedCaptionRenderer extends AbstractCellRenderer {

    private FeedCaptionRenderer(CellSource cellSource) {
      super(cellSource);
    }

    @Override
    public String render(IsRow row) {
      Feed feed = EnumUtils.getEnumByName(Feed.class, getString(row));
      return (feed == null) ? null : feed.getCaption();
    }
  }

  private static final int MAX_VISIBLE_ITEM_COUNT = 30;
  private static final int VISIBLE_ITEM_COUNT_RESERVE = 3;

  private static void subscribe(final long user, final List<Feed> feeds) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.SUBSCRIBE_TO_FEEDS);
    params.addDataItem(Service.VAR_USER, user);
    params.addDataItem(Service.VAR_FEED, Feed.join(feeds));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!response.hasErrors()) {
          if (!Endpoint.isOpen()) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), NewsConstants.VIEW_USER_FEEDS);
          } else if (!BeeKeeper.getUser().is(user)) {
            DataChangeEvent.fireLocalRefresh(BeeKeeper.getBus(), NewsConstants.VIEW_USER_FEEDS);
          }
        }
      }
    });
  }

  private final Long userId;

  public UserFeedsInterceptor(Long userId) {
    this.userId = userId;
  }

  UserFeedsInterceptor() {
    this(null);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    List<? extends IsRow> data = presenter.getGridView().getRowData();
    int dataIndex = getDataIndex(NewsConstants.COL_UF_FEED);

    List<Feed> feeds = new ArrayList<>();

    for (Feed feed : Feed.values()) {
      if (BeeKeeper.getUser().isModuleVisible(feed.getModuleAndSub())) {
        boolean used = false;

        if (!BeeUtils.isEmpty(data)) {
          for (IsRow row : data) {
            if (BeeUtils.same(feed.name(), row.getString(dataIndex))) {
              used = true;
              break;
            }
          }
        }

        if (!used) {
          feeds.add(feed);
        }
      }
    }

    if (feeds.isEmpty()) {
      getGridView().notifyWarning("user is subscribed to all feeds");
      return false;
    }

    final ListBox listBox = new ListBox(true);
    for (Feed feed : feeds) {
      listBox.addItem(feed.getCaption(), BeeUtils.toString(feed.ordinal()));
    }

    if (feeds.size() <= MAX_VISIBLE_ITEM_COUNT + VISIBLE_ITEM_COUNT_RESERVE) {
      listBox.setAllVisible();
    } else {
      listBox.setVisibleItemCount(MAX_VISIBLE_ITEM_COUNT);
    }

    Global.inputWidget(Localized.getConstants().feedNew(), listBox, new InputCallback() {
      @Override
      public void onSuccess() {
        List<Feed> selection = new ArrayList<>();

        for (int i = 0; i < listBox.getItemCount(); i++) {
          OptionElement optionElement = listBox.getOptionElement(i);
          if (optionElement.isSelected()) {
            Feed feed = EnumUtils.getEnumByIndex(Feed.class, optionElement.getValue());
            if (feed != null) {
              selection.add(feed);
            }
          }
        }

        if (!selection.isEmpty()) {
          subscribe(selection);
        }
      }
    }, BeeConst.CSS_CLASS_PREFIX + "UserFeeds-add", presenter.getHeader().getElement());

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new UserFeedsInterceptor(userId);
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {
    if (BeeUtils.same(columnName, NewsConstants.COL_UF_FEED)) {
      return new FeedCaptionRenderer(cellSource);
    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  private void subscribe(final List<Feed> feeds) {
    if (DataUtils.isId(userId)) {
      subscribe(userId, feeds);

    } else {
      getGridView().ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(final Long result) {
          if (DataUtils.isId(result)) {
            subscribe(result, feeds);
          }
        }
      });
    }
  }
}
