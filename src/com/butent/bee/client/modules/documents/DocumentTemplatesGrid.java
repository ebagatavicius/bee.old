package com.butent.bee.client.modules.documents;

import com.google.common.collect.Lists;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class DocumentTemplatesGrid extends AbstractGridInterceptor implements
    SelectionHandler<IsRow> {

  private static final String FILTER_KEY = "f1";
  private TreeView categoryTree;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeView) {
      categoryTree = (TreeView) widget;
      categoryTree.addSelectionHandler(this);
    }
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (action == Action.COPY) {
      LocalizableConstants loc = Localized.getConstants();
      final GridView grid = getGridView();
      final IsRow row = grid.getActiveRow();

      if (row != null) {
        Global.inputString(loc.newDocumentTemplate(), loc.documentTemplateName(),
            new StringCallback() {
              @Override
              public void onSuccess(final String value) {
                DocumentsHandler.copyDocumentData(row.getLong(grid.getDataIndex(COL_DOCUMENT_DATA)),
                    new IdCallback() {
                      @Override
                      public void onSuccess(Long dataId) {
                        Queries.insert(VIEW_DOCUMENT_TEMPLATES,
                            Data.getColumns(VIEW_DOCUMENT_TEMPLATES,
                                Lists.newArrayList(COL_DOCUMENT_CATEGORY,
                                    COL_DOCUMENT_TEMPLATE_NAME, COL_DOCUMENT_DATA)),
                            Lists.newArrayList(
                                row.getString(grid.getDataIndex(COL_DOCUMENT_CATEGORY)), value,
                                DataUtils.isId(dataId) ? BeeUtils.toString(dataId) : null),
                            null, new RowInsertCallback(VIEW_DOCUMENT_TEMPLATES, grid.getId()) {
                              @Override
                              public void onSuccess(BeeRow result) {
                                super.onSuccess(result);
                                grid.getGrid().insertRow(result, true);
                              }
                            });
                      }
                    });
              }
            }, row.getString(grid.getDataIndex(COL_DOCUMENT_TEMPLATE_NAME)));
      }
      return false;
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return Data.createRowSet(gridDescription.getViewName());
  }

  @Override
  public DocumentTemplatesGrid getInstance() {
    return new DocumentTemplatesGrid();
  }

  @Override
  public List<String> getParentLabels() {
    if (categoryTree == null || categoryTree.getSelectedItem() == null) {
      return super.getParentLabels();
    } else {
      return categoryTree.getPathLabels(categoryTree.getSelectedItem().getId(), ALS_CATEGORY_NAME);
    }
  }
  
  @Override
  public void onSelection(SelectionEvent<IsRow> event) {
    if (event != null) {
      Long category = (event.getSelectedItem() == null) ? null : event.getSelectedItem().getId();
      Filter flt;

      if (category != null) {
        flt = Filter.equals(COL_DOCUMENT_CATEGORY, category);
      } else {
        flt = Filter.isFalse();
      }
      getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, flt);
      getGridPresenter().refresh(true);
    }
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    int categoryIdx = gridView.getDataIndex(COL_DOCUMENT_CATEGORY);
    int nameIdx = gridView.getDataIndex(ALS_CATEGORY_NAME);

    if (oldRow != null) {
      newRow.setValue(categoryIdx, oldRow.getString(categoryIdx));
      newRow.setValue(nameIdx, oldRow.getString(nameIdx));

    } else if (categoryTree != null) {
      IsRow category = categoryTree.getSelectedItem();

      if (category != null) {
        newRow.setValue(categoryIdx, category.getId());
        newRow.setValue(nameIdx,
            category.getString(DataUtils.getColumnIndex(ALS_CATEGORY_NAME,
                categoryTree.getTreePresenter().getDataColumns())));
      }
    }
    return true;
  }
}
