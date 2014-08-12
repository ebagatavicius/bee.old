package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;

public final class DiscussionsKeeper {

  public static void register() {
    /* Form interceptors */
    FormFactory.registerFormInterceptor(FORM_NEW_DISCUSSION, new CreateDiscussionInterceptor());
    FormFactory.registerFormInterceptor(FORM_DISCUSSION, new DiscussionInterceptor());
    FormFactory.registerFormInterceptor(FORM_ANNOUNCEMENTS_BOARD,
        new AnnouncementsBoardInterceptor());

    for (DiscussionsListType type : DiscussionsListType.values()) {
      GridFactory.registerGridSupplier(type.getSupplierKey(), GRID_DISCUSSIONS,
          new DiscussionsGridHandler(type));
    }

    /* Grid handlers */

    GridFactory.registerGridInterceptor(GRID_DISCUSSION_FILES, new DiscussionFilesGrid());

    /* Menu */
    MenuService.DISCUSS_LIST.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        DiscussionsListType type = DiscussionsListType.getByPrefix(parameters);

        if (type == null) {
          Global.showError(Lists.newArrayList(GRID_DISCUSSIONS, "Type not recognized:",
              parameters));
        } else {
          ViewFactory.createAndShow(type.getSupplierKey());
        }
      }
    });

    /* Row handlers */
    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);

    Global.getNewsAggregator().registerFilterHandler(Feed.ANNOUNCEMENTS,
        getAnnouncementsFilterHandler());
  }

  static ParameterList createArgs(String method) {
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.DISCUSSIONS.getName());
    args.addQueryItem(AdministrationConstants.METHOD, method);
    return args;
  }

  static ParameterList createDiscussionRpcParameters(DiscussionEvent event) {
    return createArgs(DISCUSSIONS_PREFIX + event.name());
  }

  static BiConsumer<GridOptions, PresenterCallback> getAnnouncementsFilterHandler() {
    BiConsumer<GridOptions, PresenterCallback> consumer =
        new BiConsumer<GridFactory.GridOptions, PresenterCallback>() {
          @Override
          public void accept(GridOptions gridOptions, PresenterCallback callback) {
            GridFactory.openGrid(GRID_DISCUSSIONS,
                new DiscussionsGridHandler(DiscussionsListType.ALL),
                gridOptions, callback);
          }
        };

    return consumer;
  }

  private DiscussionsKeeper() {
  }
}
