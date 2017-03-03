package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;

import java.util.function.BiConsumer;

public final class DiscussionsKeeper {

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_DISCUSSIONS_FILES)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_DISCUSSIONS_FILES), event.getRow(),
            Lists.newArrayList(COL_CAPTION, AdministrationConstants.ALS_FILE_TYPE,
                COL_COMMENT_TEXT),
            BeeConst.STRING_SPACE, Format.getDateRenderer(), Format.getDateTimeRenderer()));
      }
    }
  }

  public static void register() {
    /* Form interceptors */
    FormFactory.registerFormInterceptor(FORM_NEW_DISCUSSION, new CreateDiscussionInterceptor());
    FormFactory.registerFormInterceptor(FORM_NEW_ANNOUNCEMENT, new CreateDiscussionInterceptor());
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
    MenuService.DISCUSS_LIST.setHandler(parameters -> {
      DiscussionsListType type = DiscussionsListType.getByPrefix(parameters);

      if (type == null) {
        Global.showError(Lists.newArrayList(GRID_DISCUSSIONS, "Type not recognized:",
            parameters));
      } else {
        ViewFactory.createAndShow(type.getSupplierKey());
      }
    });

    /* Row handlers */
    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());

    Global.getNewsAggregator().registerFilterHandler(Feed.ANNOUNCEMENTS,
        getAnnouncementsFilterHandler());

    BeeKeeper.getBus().registerRowActionHandler(new RowActionEvent.Handler() {
      @Override
      public void onRowAction(RowActionEvent event) {
        if (event.isEditRow() && event.hasView(VIEW_DISCUSSIONS_FILES)) {
          event.consume();

          if (event.hasRow() && event.getOpener() != null) {
            Long discussionId = Data.getLong(event.getViewName(), event.getRow(), COL_DISCUSSION);
            RowEditor.open(VIEW_DISCUSSIONS, discussionId, event.getOpener());
          }
        }
      }
    });
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.DISCUSSIONS, method);
  }

  static ParameterList createDiscussionRpcParameters(DiscussionEvent event) {
    return createArgs(DISCUSSIONS_PREFIX + event.name());
  }

  static BiConsumer<GridOptions, PresenterCallback> getAnnouncementsFilterHandler() {
    return (gridOptions, callback) -> GridFactory.openGrid(GRID_DISCUSSIONS,
                new DiscussionsGridHandler(DiscussionsListType.ALL),
                gridOptions, callback);

  }

  private DiscussionsKeeper() {
  }
}
