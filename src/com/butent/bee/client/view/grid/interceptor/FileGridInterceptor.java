package com.butent.bee.client.view.grid.interceptor;

import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FileLinkRenderer;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

public class FileGridInterceptor extends AbstractGridInterceptor {

  private final String parentColumn;
  private final String fileColumn;
  private final String captionColumn;
  private final String nameColumn;

  private FileCollector collector;

  public FileGridInterceptor(String parentColumn, String fileColumn, String captionColumn,
      String nameColumn) {
    this.parentColumn = parentColumn;
    this.fileColumn = fileColumn;
    this.captionColumn = captionColumn;
    this.nameColumn = nameColumn;
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (Action.ADD.equals(action)) {
      if (collector == null || getGridView() == null) {
        return false;
      }
      if (getGridView().likeAMotherlessChild() && !presenter.validateParent()) {
        return false;
      }

      collector.clickInput();
      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new FileGridInterceptor(parentColumn, fileColumn, captionColumn, nameColumn);
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    if (BeeUtils.same(columnName, fileColumn) && !BeeUtils.isEmpty(captionColumn)) {
      return new FileLinkRenderer(DataUtils.getColumnIndex(columnName, dataColumns),
          DataUtils.getColumnIndex(captionColumn, dataColumns),
          DataUtils.getColumnIndex(nameColumn, dataColumns));

    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  @Override
  public void onLoad(final GridView gridView) {
    if (collector == null) {
      collector = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
        @Override
        public void accept(final Collection<? extends FileInfo> input) {
          if (!BeeUtils.isEmpty(input)) {
            gridView.ensureRelId(new IdCallback() {
              @Override
              public void onSuccess(Long result) {
                FileUtils.commitFiles(input, gridView.getViewName(), parentColumn, result,
                    fileColumn, captionColumn);
              }
            });
          }
        }
      });

      gridView.add(collector);

      FormView form = ViewHelper.getForm(gridView.asWidget());
      if (form != null) {
        collector.bindDnd(form);
      }
    }
  }
}
