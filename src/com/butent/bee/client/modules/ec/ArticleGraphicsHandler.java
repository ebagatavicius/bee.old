package com.butent.bee.client.modules.ec;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.modules.ec.render.PictureRenderer;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

class ArticleGraphicsHandler extends AbstractGridInterceptor {

  private FileCollector collector;

  ArticleGraphicsHandler() {
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, COL_TCD_GRAPHICS_RESOURCE)) {
      EcKeeper.addPictureCellHandlers(column.getCell(), TBL_TCD_GRAPHICS);
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public boolean beforeAction(Action action, final GridPresenter presenter) {
    if (action == Action.ADD) {
      ensureCollector().clickInput();
      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new ArticleGraphicsHandler();
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_TCD_GRAPHICS_RESOURCE)) {
      return new PictureRenderer(DataUtils.getColumnIndex(COL_TCD_GRAPHICS_TYPE, dataColumns),
          DataUtils.getColumnIndex(COL_TCD_GRAPHICS_RESOURCE, dataColumns));
    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    if (!Features.supportsFileApi() && gridDescription != null) {
      gridDescription.getDisabledActions().add(Action.ADD);
    }

    return super.initDescription(gridDescription);
  }

  private FileCollector ensureCollector() {
    if (collector == null) {
      collector = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
        @Override
        public void accept(Collection<? extends FileInfo> input) {
          final Collection<FileInfo> files = Images.sanitizeInput(input, getGridView());
          if (!files.isEmpty()) {
            getGridView().ensureRelId(new IdCallback() {
              @Override
              public void onSuccess(Long result) {
                uploadGraphics(result, files);
              }
            });
          }
        }
      });

      collector.setAccept(Keywords.ACCEPT_IMAGE);

      getGridView().add(collector);
    }
    return collector;
  }

  private int getMaxSort() {
    int result = 0;

    List<? extends IsRow> data = getGridView().getRowData();
    if (!BeeUtils.isEmpty(data)) {
      int index = getDataIndex(COL_TCD_SORT);
      for (IsRow row : data) {
        result = Math.max(result, BeeUtils.unbox(row.getInteger(index)));
      }
    }

    return result;
  }

  private void uploadGraphics(final Long articleId, Collection<FileInfo> files) {
    final Holder<Integer> latch = Holder.of(files.size());
    final Holder<Integer> sort = Holder.of(getMaxSort());

    for (FileInfo fileInfo : files) {
      if (fileInfo instanceof NewFileInfo) {
        FileUtils.readAsDataURL(((NewFileInfo) fileInfo).getFile(), new Consumer<String>() {

          @Override
          public void accept(String input) {
            sort.set(sort.get() + 1);

            ParameterList params = EcKeeper.createArgs(SVC_UPLOAD_GRAPHICS);
            params.addQueryItem(COL_TCD_ARTICLE, articleId);
            params.addQueryItem(COL_TCD_SORT, sort.get());

            params.addDataItem(COL_TCD_GRAPHICS_RESOURCE, input);

            BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                latch.set(latch.get() - 1);
                if (!BeeUtils.isPositive(latch.get())) {
                  DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName());
                }
              }
            });
          }
        });
      }
    }
  }
}
