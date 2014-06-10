package com.butent.bee.client.modules.discussions;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.modules.discussions.DiscussionsList.ListType;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AttachmentRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.FaLabel;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.PROP_STAR;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

class DiscussionsGridHandler extends AbstractGridInterceptor {

  private static final int DEFAULT_STAR_COUNT = 3;
  private static final String NAME_STAR = "Star";

  private final ListType type;
  private final Long userId;

  public DiscussionsGridHandler(ListType type) {
    this.type = type;
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    return DeleteMode.SINGLE;
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
    gridDescription.setFilter(type.getFilter(new LongValue(userId)));
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

  private void updateStar(final EditStartEvent event, final CellSource source,
      final Integer value) {
    final long rowId = event.getRowValue().getId();

    Filter filter = Filter.and(Filter.equals(COL_DISCUSSION, rowId),
        Filter.equals(AdministrationConstants.COL_USER, userId));

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
