package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AttachmentRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

class DiscussionsGridHandler extends AbstractGridInterceptor {

  private static final int DEFAULT_STAR_COUNT = 3;
  private static final String NAME_STAR = "Star";

  private final DiscussionsListType type;
  private final UserInfo currentUser;
  private String discussionAdminLogin;

  public DiscussionsGridHandler(DiscussionsListType type) {
    this.type = type;
    this.currentUser = BeeKeeper.getUser();
    this.discussionAdminLogin = "";
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    super.beforeRender(gridView, event);

    Global.getParameter(PRM_DISCUSS_ADMIN, getDiscussAdminParameterConsumer());
    finishCompletedDiscussions(gridView);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {

    if (type == DiscussionsListType.ANNOUNCEMENTSBOARDLIST) {
      DataInfo data = Data.getDataInfo(VIEW_DISCUSSIONS);
      BeeRow emptyRow = RowFactory.createEmptyRow(data, true);
      final BeeColumn beeCol = data.getColumn(COL_TOPIC);
      if (beeCol != null) {
        beeCol.setNullable(false);
      }
      RowFactory.createRow(FORM_NEW_DISCUSSION, Localized.getConstants().announcementNew(),
          data, emptyRow, presenter.getMainView().asWidget(),
          new CreateDiscussionInterceptor(), new RowCallback() {

            @Override
            public void onCancel() {
              if (beeCol != null) {
                beeCol.setNullable(true);
              }
            }

            @Override
            public void onSuccess(BeeRow result) {
              if (beeCol != null) {
                beeCol.setNullable(true);
              }
            }
          });
    } else {
      return super.beforeAddRow(presenter, copy);
    }
    return false;
  }

  @Override
  public String getCaption() {
    if (type.getCaption() == Localized.getConstants().announcements()) {
      return type.getCaption();
    } else {
      return BeeUtils.joinWords(Localized.getConstants().discussions(),
          BeeUtils.parenthesize(type.getCaption()));
    }
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    if (presenter == null && activeRow == null && currentUser == null) {
      return DeleteMode.CANCEL;
    }

    GridView gridView = presenter.getGridView();

    if (gridView == null) {
      return DeleteMode.CANCEL;
    }

    long discussOwner = BeeUtils.unbox(activeRow.getLong(gridView.getDataIndex(COL_OWNER)));
    boolean isAdmin = isDiscussAdmin(currentUser.getLogin(), getDiscussionAdminLogin());
    boolean isOwner = currentUser.getUserId().longValue() == discussOwner;

    if (!isAdmin && !isOwner) {
      gridView.notifyWarning(BeeUtils.joinWords(Localized.getConstants().discussion(),
          activeRow.getId(), Localized.getConstants().discussDeleteCanOwnerOrAdmin()));
      return DeleteMode.CANCEL;
    }

    return DeleteMode.SINGLE;
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    GridView gridView = getGridView();
    if (row == null && gridView == null) {
      return super.getDeleteRowMessage(row);
    }

    String m1 = BeeUtils.joinWords(Localized.getConstants().discussion(),
        row.getValue(gridView.getDataIndex(COL_SUBJECT)));
    String m2 = Localized.getConstants().discussDeleteQuestion();

    return Lists.newArrayList(m1, m2);
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {
    if (BeeUtils.same(columnName, NAME_STAR)) {
      return new DiscussStarFilterSupplier(columnDescription.getFilterOptions());
    } else {
      return super.getFilterSupplier(columnName, columnDescription);
    }
  }

  @Override
  public ColumnHeader getHeader(String columnName, String caption) {
    if (PROP_STAR.equals(columnName)) {
      return new ColumnHeader(columnName, Stars.getDefaultHeader(), BeeConst.STRING_ASTERISK);

    } else if (PROP_FILES_COUNT.equals(columnName)) {
      return new ColumnHeader(columnName,
          Images.asString(Images.get(AttachmentRenderer.IMAGE_ATTACHMENT)), null);

    } else if (PROP_RELATIONS_COUNT.equals(columnName)) {
      return new ColumnHeader(columnName, Images.asString(Images.get("link")), null);

    } else if (PROP_ANNOUNCMENT.equals(columnName)) {
      FaLabel fl = new FaLabel(FontAwesome.BULLHORN);
      StyleUtils.setFontSize(fl, 16);
      return new ColumnHeader(columnName, fl.toString(), null);

    } else {
      return super.getHeader(columnName, caption);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new DiscussionsGridHandler(type);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    if (currentUser != null) {
      gridDescription.setFilter(type.getFilter(new LongValue(currentUser.getUserId())));
    }
    return true;
  }

  @Override
  public void onEditStart(final EditStartEvent event) {
    IsRow row = event.getRowValue();

    if (row == null) {
      return;
    }

    if (PROP_STAR.equals(event.getColumnId())) {
      if (row.getProperty(PROP_USER) != null) {

        final CellSource source = CellSource.forProperty(PROP_STAR, ValueType.INTEGER);
        EditorAssistant.editStarCell(DEFAULT_STAR_COUNT, event, source, new Consumer<Integer>() {
          @Override
          public void accept(Integer parameter) {
            updateStar(event, source, parameter);
          }
        });
      }
    }
  }

  private static void finishCompletedDiscussions(GridView grid) {
    final String viewName = grid.getViewName();
    Filter timeFilter = Filter.isLess(COL_VISIBLE_TO, Value.getValue(new DateTime()));
    Filter activeFilter = Filter.equals(COL_STATUS, DiscussionStatus.ACTIVE.ordinal());
    Filter filter = Filter.and(activeFilter, timeFilter);
    Queries.getRowSet(viewName, Lists.newArrayList(COL_STATUS, COL_VISIBLE_TO), filter,
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            for (BeeRow rs : result.getRows()) {
              Queries.update(viewName, rs.getId(), COL_STATUS, new TextValue("1"));
            }
          }
        });
  }

  private static boolean isDiscussAdmin(String currentUserLogin, String adminLoginName) {
    if (BeeUtils.isEmpty(currentUserLogin)) {
      return false;
    }

    if (BeeUtils.isEmpty(adminLoginName)) {
      return false;
    }

    return BeeUtils.equalsTrim(currentUserLogin, adminLoginName);
  }

  private String getDiscussionAdminLogin() {
    return this.discussionAdminLogin;
  }

  private Consumer<String> getDiscussAdminParameterConsumer() {
    return new Consumer<String>() {

      @Override
      public void accept(String input) {
        setDiscussionAdminLogin(input);
      }
    };
  }

  private void setDiscussionAdminLogin(String loginName) {
    this.discussionAdminLogin = loginName;
  }

  private void updateStar(final EditStartEvent event, final CellSource source,
      final Integer value) {
    final long rowId = event.getRowValue().getId();

    if (currentUser == null) {
      return;
    }

    Filter filter = Filter.and(Filter.equals(COL_DISCUSSION, rowId),
        Filter.equals(AdministrationConstants.COL_USER, currentUser.getUserId()));

    Queries.update(VIEW_DISCUSSIONS_USERS, filter, COL_STAR, new IntegerValue(value),
        new Queries.IntCallback() {

          @Override
          public void onSuccess(Integer result) {
            CellUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS, rowId,
                event.getRowValue().getVersion(), source,
                (value == null) ? null : BeeUtils.toString(value));
          }
        });
  }
}
