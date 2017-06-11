package com.butent.bee.client.presenter;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormImpl;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.data.AbstractRow;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFormatter;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TreePresenter extends AbstractPresenter
    implements CatchEvent.CatchHandler<IsRow>, HasViewName, Launchable {

  private final class CommitCallback implements RowCallback {
    private final boolean createMode;

    private CommitCallback(boolean createMode) {
      this.createMode = createMode;
    }

    @Override
    public void onSuccess(BeeRow result) {
      String text = format(result);

      if (createMode) {
        Long parentId =
            result.getLong(DataUtils.getColumnIndex(parentColumnName, getDataColumns()));
        getView().addItem(parentId, text, result, true);
        RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), result, treeView.getId());
      } else {
        getView().updateItem(text, result);
        RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);
      }
    }
  }

  public static TreePresenter create(String treeName, TreeView treeView, String viewName,
      Map<String, String> attributes, Calculation calculation, Element editForm) {

    if (treeView == null) {
      logger.severe(Localized.dictionary().parameterNotFound(NameUtils.getName(TreeView.class)));
      return null;
    }

    if (BeeUtils.isEmpty(viewName)) {
      logger.severe(Localized.dictionary().parameterNotFound(UiConstants.ATTR_VIEW_NAME));
      return null;
    }
    if (BeeUtils.isEmpty(attributes)) {
      logger.severe(Localized.dictionary().parameterNotFound("attributes"));
      return null;
    }

    Filter filter = null;
    String treeFilter = attributes.get(UiConstants.ATTR_FILTER);

    if (!BeeUtils.isEmpty(treeFilter)) {
      DataInfo dataInfo = Data.getDataInfo(viewName);
      if (dataInfo != null) {
        filter = dataInfo.parseFilter(treeFilter, BeeKeeper.getUser().getUserId());
      }
    }

    RowFormatter rowFormatter = RendererFactory.getTreeFormatter(treeName);

    return new TreePresenter(treeView, viewName, filter, attributes.get("parentColumn"),
        attributes.get("orderColumn"), attributes.get("relationColumn"),
        rowFormatter, calculation, editForm);
  }

  private static final BeeLogger logger = LogUtils.getLogger(TreePresenter.class);

  private final TreeView treeView;
  private final String viewName;
  private final String parentColumnName;
  @SuppressWarnings("unused")
  private final String orderColumnName; // TODO implement order column
  private final String relationColumnName;
  private Long relationId;
  private final Calculation calculation;
  private List<BeeColumn> dataColumns;
  private CustomProperties properties;
  private Evaluator evaluator;
  private final Element editor;
  private FormInterceptor editorInterceptor;

  private final HandlerRegistration catchHandler;

  private Filter filter;
  private RowFormatter rowFormatter;

  private TreePresenter(TreeView view, String viewName, Filter filter,
      String parentColumnName, String orderColumnName, String relationColumnName,
      RowFormatter rowFormatter, Calculation calculation, Element editorForm) {

    Assert.notNull(view);
    Assert.notEmpty(viewName);

    this.treeView = view;

    this.viewName = viewName;
    this.filter = filter;

    this.parentColumnName = parentColumnName;
    this.orderColumnName = orderColumnName;
    this.relationColumnName = relationColumnName;

    this.calculation = calculation;

    if (rowFormatter == null && calculation == null) {
      this.rowFormatter =
          row -> BeeUtils.joinOptions(Localized.dictionary().captionId(), row.getId());
    } else {
      this.rowFormatter = rowFormatter;
    }

    this.editor = editorForm;

    this.catchHandler = getView().addCatchHandler(this);
  }

  public String format(IsRow row) {
    String value;

    if (row == null) {
      value = null;

    } else if (rowFormatter != null) {
      value = rowFormatter.apply(row);

    } else if (evaluator != null) {
      evaluator.update(row);
      value = evaluator.evaluate();

    } else {
      value = null;
    }

    return value;
  }

  @Override
  public String getCaption() {
    return treeView.getCaption();
  }

  public List<BeeColumn> getDataColumns() {
    return dataColumns;
  }

  @Override
  public HeaderView getHeader() {
    return null;
  }

  @Override
  public View getMainView() {
    return getView();
  }

  public CustomProperties getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    Assert.notEmpty(key);

    if (properties == null) {
      return null;
    }
    return properties.get(key);
  }

  @Override
  public String getViewKey() {
    return "tree_" + getViewName();
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public void handleAction(Action action) {
    Assert.notNull(action);

    switch (action) {
      case ADD:
        if (getView().isEnabled()) {
          addItem();
        }
        break;

      case BOOKMARK:
        createBookmark();
        break;

      case DELETE:
        if (getView().isEnabled()) {
          removeItem();
        }
        break;

      case EDIT:
        if (getView().isEnabled()) {
          editItem();
        }
        break;

      case REFRESH:
        requery();
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public void launch() {
    if (BeeUtils.isEmpty(relationColumnName)) {
      requery();
    }
  }

  @Override
  public void onCatch(CatchEvent<IsRow> event) {
    IsRow item = event.getPacket();
    int parentIndex = DataUtils.getColumnIndex(parentColumnName, getDataColumns());
    IsRow destination = event.getDestination();

    Queries.update(getViewName(), item.getId(), item.getVersion(),
        Lists.newArrayList(getDataColumns().get(parentIndex)),
        Lists.newArrayList(item.getString(parentIndex)),
        Lists.newArrayList(destination == null ? null : BeeUtils.toString(destination.getId())),
        null, new CommitCallback(false));
  }

  @Override
  public void onViewUnload() {
    getView().setViewPresenter(null);
    super.onViewUnload();
  }

  public void removeCatchHandler() {
    catchHandler.removeHandler();
  }

  public void setEditorInterceptor(FormInterceptor editorInterceptor) {
    this.editorInterceptor = editorInterceptor;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public void setRowFormatter(RowFormatter rowFormatter) {
    this.rowFormatter = rowFormatter;
  }

  public void updateRelation(Long parentId) {
    this.relationId = parentId;
    requery();
  }

  private void addBranch(Long parentId, Map<Long, List<Long>> hierarchy, Map<Long, IsRow> items) {
    List<Long> branch = hierarchy.get(parentId);

    if (branch != null) {
      for (Long leaf : branch) {
        IsRow item = items.get(leaf);
        getView().addItem(parentId, format(item), item, false);
        addBranch(item.getId(), hierarchy, items);
      }
    }
  }

  private void addItem() {
    if (canModify()) {
      edit(null);
    }
  }

  private boolean canModify() {
    boolean ok = true;

    if (!BeeUtils.isEmpty(relationColumnName) && relationId == null) {
      ok = false;
    }
    if (ok && editor == null) {
      BeeKeeper.getScreen().notifySevere("Editor form not found");
      ok = false;
    }
    return ok;
  }

  private void createBookmark() {
    IsRow activeRow = getView().getSelectedItem();
    Global.getFavorites().bookmark(getViewName(), activeRow, getDataColumns(),
        treeView.getFavorite(), BeeConst.STRING_GT);
  }

  private void edit(final IsRow item) {
    final boolean addMode = item == null;
    final IsRow row;

    if (addMode) {
      row = DataUtils.createEmptyRow(getDataColumns().size());
    } else {
      row = DataUtils.cloneRow(item);
    }

    final FormView formView = new FormImpl(FormDescription.getName(editor));
    formView.create(new FormDescription(editor), getViewName(), getDataColumns(), false,
        editorInterceptor);
    formView.setEditing(true);
    formView.start(null);
    formView.setCaption(getCaption());

    String caption;
    if (addMode) {
      caption = format(getView().getSelectedItem());
    } else {
      caption = format(getView().getParentItem(item));
    }

    String styleName = (addMode ? RowFactory.DIALOG_STYLE : RowEditor.DIALOG_STYLE) + "-tree";

    Global.inputWidget(caption, formView, new InputCallback() {
      final List<BeeColumn> columns = new ArrayList<>();
      final List<String> oldValues = new ArrayList<>();
      final List<String> values = new ArrayList<>();

      @Override
      public String getErrorMessage() {
        if (!formView.validate(formView, true)) {
          return InputBoxes.SILENT_ERROR;
        }

        columns.clear();
        oldValues.clear();
        values.clear();

        for (int i = 0; i < getDataColumns().size(); i++) {
          BeeColumn column = getDataColumns().get(i);

          if (column.isEditable()) {
            String value = row.getString(i);
            if (addMode) {
              if (!BeeUtils.isEmpty(value)) {
                columns.add(column);
                values.add(value);
              }

            } else {
              if (!Objects.equals(item.getString(i), value)) {
                columns.add(column);
                oldValues.add(item.getString(i));
                values.add(value);
              }
            }
          }
        }

        if (addMode && BeeUtils.isEmpty(columns)) {
          return Localized.dictionary().allValuesCannotBeEmpty();
        }
        return null;
      }

      @Override
      public void onClose(CloseCallback closeCallback) {
        formView.onClose(closeCallback);
      }

      @Override
      public void onSuccess() {
        if (addMode) {
          if (getView().getSelectedItem() != null) {
            columns.add(new BeeColumn(parentColumnName));
            values.add(BeeUtils.toString(getView().getSelectedItem().getId()));
          }
          if (!BeeUtils.isEmpty(relationColumnName)) {
            columns.add(new BeeColumn(relationColumnName));
            values.add(BeeUtils.toString(relationId));
          }
          Queries.insert(getViewName(), columns, values, formView.getChildrenForInsert(),
              new CommitCallback(true));

        } else if (!columns.isEmpty()) {
          Queries.update(getViewName(), row.getId(), row.getVersion(), columns, oldValues, values,
              formView.getChildrenForUpdate(), new CommitCallback(false));
        }
      }
    }, styleName, null, Action.NO_ACTIONS, (widget, name) -> {
      if (DialogConstants.WIDGET_DIALOG.equals(name) && widget instanceof Popup) {
        ((Popup) widget).addOpenHandler(event -> formView.updateRow(row, true));
      }
      return widget;
    });
  }

  private void editItem() {
    if (canModify() && getView().getSelectedItem() != null) {
      edit(getView().getSelectedItem());
    }
  }

  private TreeView getView() {
    return treeView;
  }

  private void removeItem() {
    final IsRow data = getView().getSelectedItem();

    if (data != null) {
      String message = BeeUtils.joinWords(Localized.dictionary().delete(),
          BeeUtils.bracket(format(data)), "?");

      Global.confirmDelete(getCaption(), Icon.WARNING, Lists.newArrayList(message), () ->
          Queries.deleteRow(getViewName(), data.getId(), data.getVersion(),
              result -> {
                getView().removeItem(data);
                RowDeleteEvent.fire(BeeKeeper.getBus(), getViewName(), data.getId());
              }));
    }
  }

  private void requery() {
    Filter flt;

    if (BeeUtils.isEmpty(relationColumnName)) {
      flt = filter;
    } else {
      flt = Filter.and(filter, Filter.equals(relationColumnName, relationId));
    }

    Queries.getRowSet(getViewName(), null, flt, null, result -> {
      dataColumns = result.getColumns();
      properties = result.getTableProperties();

      if (evaluator == null && calculation != null) {
        evaluator = Evaluator.create(calculation, null, getDataColumns());
      }

      getView().removeItems();

      if (result.isEmpty()) {
        getView().afterRequery();
        return;
      }

      int parentIndex = result.getColumnIndex(parentColumnName);

      if (BeeConst.isUndef(parentIndex)) {
        BeeKeeper.getScreen().notifySevere("Parent column not found", parentColumnName);
        getView().afterRequery();
        return;
      }

      Map<Long, IsRow> items = result.getRows().stream()
          .collect(Collectors.toMap(AbstractRow::getId, Function.identity()));

      Map<Long, List<Long>> hierarchy = new LinkedHashMap<>();

      if (filter == null) {
        for (IsRow row : result) {
          Long parent = row.getLong(parentIndex);
          hierarchy.computeIfAbsent(parent, k -> new ArrayList<>()).add(row.getId());
        }

      } else {
        Map<Long, Long> parents = result.getRows().stream()
            .filter(row -> !Objects.equals(row.getId(), row.getLong(parentIndex)))
            .collect(Collectors.toMap(AbstractRow::getId, row -> row.getLong(parentIndex)));

        for (IsRow row : result) {
          long id = row.getId();
          Long parent = row.getLong(parentIndex);

          boolean ok = !Objects.equals(id, parent);

          if (ok && parent != null) {
            for (Long p = parent; p != null; p = parents.get(p)) {
              if (!parents.containsKey(p)) {
                ok = false;
                break;
              }
            }
          }

          if (ok) {
            hierarchy.computeIfAbsent(parent, k -> new ArrayList<>()).add(id);
          }
        }
      }

      for (Long parent : hierarchy.keySet()) {
        if (!items.containsKey(parent)) {
          addBranch(parent, hierarchy, items);
        }
      }

      getView().afterRequery();
    });
  }
}
