package com.butent.bee.client.view.grid.interceptor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_FILE_HASH;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FileLinkRenderer;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
  public void afterCreate(final GridView gridView) {
    gridView.addDragStartHandler(new DragStartHandler() {
      @Override
      public void onDragStart(DragStartEvent event) {
        Long fileId = null;
        Element target = EventUtils.getEventTargetElement(event);

        while (target != null) {
          Integer idx = BeeUtils.toIntOrNull(DomUtils.getDataRow(target));
          IsRow row = idx != null ? BeeUtils.getQuietly(gridView.getRowData(), idx) : null;

          if (row != null) {
            if (BeeUtils.same(gridView.getGrid().getColumnId(DomUtils.getDataColumnInt(target)),
                fileColumn)) {
              fileId = row.getLong(gridView.getDataIndex(fileColumn));
            }
            break;
          }
          target = target.getParentElement();
        }
        if (DataUtils.isId(fileId)) {
          for (FileInfo fileInfo : collector.getFiles()) {
            if (Objects.equals(fileInfo.getId(), fileId)) {
              EventUtils.allowCopyMove(event);
              DndHelper.fillContent(NameUtils.getClassName(FileInfo.class), null, null, fileInfo);
              return;
            }
          }
        }
      }
    });
    gridView.addDragEndHandler(new DragEndHandler() {
      @Override
      public void onDragEnd(DragEndEvent dragEndEvent) {
        DndHelper.reset();
      }
    });
    super.afterCreate(gridView);
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
      return new FileLinkRenderer(DataUtils.getColumnIndex(COL_FILE_HASH, dataColumns),
          DataUtils.getColumnIndex(captionColumn, dataColumns),
          DataUtils.getColumnIndex(nameColumn, dataColumns));

    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    if (collector == null) {
      init(gridView);
    }
    collector.clear();
    final Map<Long, String> fileIds = new HashMap<>();
    int fileIdx = gridView.getDataIndex(fileColumn);
    int capIdx = gridView.getDataIndex(captionColumn);

    for (IsRow row : gridView.getRowData()) {
      fileIds.put(row.getLong(fileIdx), row.getString(capIdx));
    }
    if (!BeeUtils.isEmpty(fileIds)) {
      ParameterList args = new ParameterList(Service.GET_FILES);
      args.addDataItem(Service.VAR_FILES, DataUtils.buildIdList(fileIds.keySet()));

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          collector.clear();
          List<FileInfo> files = FileInfo.restoreCollection(response.getResponseAsString());

          for (FileInfo fileInfo : files) {
            fileInfo.setCaption(fileIds.get(fileInfo.getId()));
            collector.getFiles().add(fileInfo);
          }
        }
      });
    }
    super.beforeRender(gridView, event);
  }

  private void init(final GridView gridView) {
    if (collector == null) {
      collector = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
        @Override
        public void accept(Collection<? extends FileInfo> input) {
          if (!gridView.isEnabled()) {
            return;
          }
          final Collection<FileInfo> files = new HashSet<>();

          if (!BeeUtils.isEmpty(input)) {
            for (FileInfo fileInfo : input) {
              if (!collector.contains(fileInfo)) {
                files.add(fileInfo);
              }
            }
          }
          if (!BeeUtils.isEmpty(files)) {
            gridView.ensureRelId(new IdCallback() {
              @Override
              public void onSuccess(Long result) {
                FileUtils.commitFiles(files, gridView.getViewName(), parentColumn, result,
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
