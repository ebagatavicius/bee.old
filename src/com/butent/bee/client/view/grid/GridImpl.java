package com.butent.bee.client.view.grid;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Place;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ModalForm;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer.PreviewConsumer;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.event.logical.SortEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.cell.ActionCell;
import com.butent.bee.client.grid.cell.CalculatedCell;
import com.butent.bee.client.grid.cell.SelectionHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.grid.column.ActionColumn;
import com.butent.bee.client.grid.column.CalculatedColumn;
import com.butent.bee.client.grid.column.RowIdColumn;
import com.butent.bee.client.grid.column.RowVersionColumn;
import com.butent.bee.client.grid.column.SelectionColumn;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.render.SimpleRenderer;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.style.StyleProvider;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.validation.CellValidateEvent.Handler;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.validation.EditorValidation;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.validation.ValidationOrigin;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.AddEndEvent;
import com.butent.bee.client.view.add.AddStartEvent;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditFormEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormImpl;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.view.search.FilterSupplierFactory;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.ui.CellType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.FilterSupplierType;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.HandlesFormat;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Wildcards;
import com.butent.bee.shared.utils.Wildcards.Pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Creates cell grid elements, connecting view and presenter elements of them.
 */

public class GridImpl extends Absolute implements GridView, EditEndEvent.Handler,
    SortEvent.Handler, SettingsChangeEvent.Handler, RenderingEvent.Handler {

  private class SaveChangesCallback extends RowCallback {
    @Override
    public void onCancel() {
      closeEditForm();
    }

    @Override
    public void onFailure(String... reason) {
      getEditForm().notifySevere(reason);
    }

    @Override
    public void onSuccess(BeeRow result) {
      FormView form = getForm(true);

      if (form.getFormInterceptor() != null) {
        form.getFormInterceptor().afterUpdateRow(result);
      }

      closeEditForm();

      if (getGridInterceptor() != null) {
        getGridInterceptor().afterUpdateRow(result);
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(GridImpl.class);

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "GridView";

  private static void amendGeneratedSize(final ModalForm popup, final FormView form) {
    popup.attachAmendDetach(new ScheduledCommand() {
      @Override
      public void execute() {
        int width = DomUtils.getOuterWidth(form.getRootWidget().asWidget().getElement());
        int height = DomUtils.getOuterHeight(form.getRootWidget().asWidget().getElement())
            + form.getViewPresenter().getHeader().getHeight() + 1;

        if (width > BeeUtils.toInt(form.getWidthValue())) {
          StyleUtils.setWidth(popup, width + 10);
        }
        StyleUtils.setHeight(popup, height);
      }
    });
  }

  private static boolean isColumnReadOnly(String viewName, String source,
      ColumnDescription columnDescription) {

    if (columnDescription.getColType().isReadOnly()) {
      return true;

    } else if (BeeUtils.isTrue(columnDescription.getReadOnly())) {
      return true;

    } else if (BeeUtils.allNotEmpty(viewName, source)) {
      return !BeeKeeper.getUser().canEditColumn(viewName, source);

    } else {
      return false;
    }
  }

  private static boolean isColumnVisible(String gridName, ColumnDescription columnDescription) {
    if (BeeUtils.isTrue(columnDescription.getVisible())) {
      return true;
    } else {
      return !GridFactory.isHidden(gridName, columnDescription.getId());
    }
  }

  private static String normalizeFormName(String formName) {
    if (BeeUtils.isEmpty(formName) || formName.trim().equals(BeeConst.STRING_MINUS)) {
      return null;
    } else {
      return formName.trim();
    }
  }

  private final GridDescription gridDescription;

  private final String gridKey;
  private final DataInfo dataInfo;
  private final List<BeeColumn> dataColumns;

  private final String relColumn;

  private final GridInterceptor gridInterceptor;

  private GridPresenter viewPresenter;

  private CellGrid grid = new CellGrid();
  private Evaluator rowValidation;

  private Evaluator rowEditable;

  private final Map<String, EditableColumn> editableColumns = new LinkedHashMap<>();

  private final Notification notification = new Notification();

  private Long relId;
  private final List<String> newRowDefaults = new ArrayList<>();

  private String newRowCaption;
  private FormView newRowForm;
  private String newRowFormName;

  private String newRowFormContainerId;
  private boolean newRowFormGenerated;

  private final Set<State> newRowFormState = EnumSet.noneOf(State.class);

  private final Set<Integer> copyColumns = new HashSet<>();
  private FormView editForm;
  private String editFormName;
  private boolean editMode;
  private boolean editSave;

  private Evaluator editMessage;
  private boolean editShowId;
  private final Set<String> editInPlace = new HashSet<>();

  private String editFormContainerId;

  private final Set<State> editFormState = EnumSet.noneOf(State.class);

  private EditStartEvent pendingEditStartEvent;

  private boolean singleForm;

  private boolean adding;
  private String activeFormContainerId;
  private boolean showNewRowPopup;

  private boolean showEditPopup;
  private ModalForm newRowPopup;

  private ModalForm editPopup;

  private SaveChangesCallback saveChangesCallback;

  private final Set<String> pendingResize = new HashSet<>();
  private String options;

  private final Map<String, String> properties = new HashMap<>();

  private final List<String> dynamicColumnGroups = new ArrayList<>();

  private final List<com.google.web.bindery.event.shared.HandlerRegistration> registry =
      new ArrayList<>();

  private State state;

  private boolean summarize;

  public GridImpl(GridDescription gridDescription, String gridKey,
      List<BeeColumn> dataColumns, String relColumn, GridInterceptor gridInterceptor) {

    super();
    addStyleName(STYLE_NAME);

    this.gridDescription = Assert.notNull(gridDescription);
    this.gridKey = gridKey;

    this.dataInfo = BeeUtils.isEmpty(gridDescription.getViewName()) ? null
        : Data.getDataInfo(gridDescription.getViewName());

    this.dataColumns = Assert.notEmpty(dataColumns);
    this.relColumn = relColumn;

    this.gridInterceptor = gridInterceptor;
  }

  @Override
  public HandlerRegistration addAddEndHandler(AddEndEvent.Handler handler) {
    return addHandler(handler, AddEndEvent.getType());
  }

  @Override
  public HandlerRegistration addAddStartHandler(AddStartEvent.Handler handler) {
    return addHandler(handler, AddStartEvent.getType());
  }

  @Override
  public HandlerRegistration addCellValidationHandler(String columnId, Handler handler) {
    EditableColumn editableColumn = getEditableColumn(columnId, true);
    if (editableColumn == null) {
      return null;
    } else {
      return editableColumn.addCellValidationHandler(handler);
    }
  }

  @Override
  public boolean addColumn(ColumnDescription columnDescription, String dynGroup, int beforeIndex) {
    ColumnDescription cd = (gridInterceptor == null) ? columnDescription
        : gridInterceptor.beforeCreateColumn(this, columnDescription);
    if (cd == null) {
      return false;
    }

    String columnId = cd.getId();

    ColType colType = cd.getColType();
    if (BeeUtils.isEmpty(columnId) || colType == null) {
      return false;
    }

    List<String> renderColumns = cd.getRenderColumns();
    if (BeeUtils.isEmpty(renderColumns) && !BeeUtils.isEmpty(cd.getRenderTokens())) {
      if (renderColumns == null) {
        renderColumns = new ArrayList<>();
      }
      for (RenderableToken renderableToken : cd.getRenderTokens()) {
        if (DataUtils.contains(dataColumns, renderableToken.getSource())) {
          renderColumns.add(renderableToken.getSource());
        }
      }
    }

    String source = cd.getSource();
    String originalSource;
    boolean relationEditable;

    if (colType == ColType.RELATED && cd.getRelation() != null) {
      if (!cd.isRelationInitialized()) {
        Holder<String> sourceHolder = Holder.of(source);
        Holder<List<String>> listHolder = Holder.of(renderColumns);

        cd.getRelation().initialize(Data.getDataInfoProvider(), getViewName(),
            sourceHolder, listHolder, Relation.RenderMode.TARGET, BeeKeeper.getUser().getUserId());

        source = sourceHolder.get();
        renderColumns = listHolder.get();

        cd.setSource(source);
        cd.setRenderColumns(renderColumns);
        cd.setRelationInitialized(true);
      }

      if (gridInterceptor != null) {
        gridInterceptor.configureRelation(columnId, cd.getRelation());
      }

      originalSource = cd.getRelation().getOriginalTarget();
      relationEditable = cd.getRelation().isEditEnabled(false)
          && Data.isViewVisible(cd.getRelation().getViewName());

    } else {
      originalSource = null;
      relationEditable = false;
    }

    int dataIndex = BeeConst.UNDEF;
    if (!BeeUtils.isEmpty(source)) {
      String normalized = DataUtils.getColumnName(source, dataColumns, gridDescription.getIdName(),
          gridDescription.getVersionName());

      if (BeeUtils.isEmpty(normalized)) {
        logger.warning("column:", columnId, "source:", source, "not found");
        return false;
      } else {
        if (!source.equals(normalized)) {
          source = normalized;
        }
        dataIndex = DataUtils.getColumnIndex(source, dataColumns);
      }
    }

    String label = Localized.maybeTranslate(cd.getLabel());
    if (BeeUtils.isEmpty(label)) {
      int index;
      if (!BeeUtils.isEmpty(originalSource) && !originalSource.equals(source)) {
        index = DataUtils.getColumnIndex(originalSource, dataColumns);
      } else {
        index = dataIndex;
      }

      if (!BeeConst.isUndef(index)) {
        label = Localized.getLabel(dataColumns.get(index));
      } else if (colType == ColType.ID
          || !BeeUtils.isEmpty(source) && BeeUtils.same(source, gridDescription.getIdName())) {
        label = Localized.getConstants().captionId();
      }
    }

    String caption = Localized.maybeTranslate(cd.getCaption());
    if (BeeUtils.isEmpty(caption)) {
      caption = label;
    } else if (BeeUtils.isEmpty(label)) {
      if (Captions.isCaption(caption)) {
        label = caption;
      } else {
        label = columnId;
      }
    }

    String enumKey = cd.getEnumKey();

    CellSource cellSource;

    switch (colType) {
      case ID:
        source = gridDescription.getIdName();
        cellSource = CellSource.forRowId(source);
        break;

      case VERSION:
        source = gridDescription.getVersionName();
        cellSource = CellSource.forRowVersion(source);
        break;

      case PROPERTY:
        String property = BeeUtils.notEmpty(cd.getProperty(), columnId);
        cellSource = CellSource.forProperty(property, cd.getValueType());

        if (cd.getPrecision() != null) {
          cellSource.setPrecision(cd.getPrecision());
        }
        if (cd.getScale() != null) {
          cellSource.setScale(cd.getScale());
        }
        break;

      default:
        if (dataIndex >= 0) {
          BeeColumn dataColumn = dataColumns.get(dataIndex);
          cellSource = CellSource.forColumn(dataColumn, dataIndex);
          if (BeeUtils.isEmpty(enumKey)) {
            enumKey = dataColumn.getEnumKey();
          }
        } else {
          cellSource = null;
        }
    }

    AbstractCellRenderer renderer = null;
    if (gridInterceptor != null) {
      renderer = gridInterceptor.getRenderer(columnId, dataColumns, cd, cellSource);
    }
    if (renderer == null) {
      renderer = RendererFactory.getGridColumnRenderer(getGridName(), columnId, dataColumns, cd,
          cellSource);
    }
    if (renderer == null) {
      renderer = RendererFactory.getRenderer(cd.getRendererDescription(), cd.getRender(),
          cd.getRenderTokens(), enumKey, renderColumns, dataColumns, cellSource, cd.getRelation());
    }

    FilterSupplierType filterSupplierType = cd.getFilterSupplierType();

    CellType cellType = cd.getCellType();

    AbstractColumn<?> column = null;

    switch (colType) {
      case ID:
        column = new RowIdColumn();
        if (filterSupplierType == null) {
          filterSupplierType = FilterSupplierType.ID;
        }
        break;

      case VERSION:
        column = new RowVersionColumn();
        if (filterSupplierType == null) {
          filterSupplierType = FilterSupplierType.VERSION;
        }
        break;

      case DATA:
      case RELATED:
      case AUTO:
        if (dataIndex >= 0) {
          column = GridFactory.createColumn(cellSource, cellType, renderer);

          if (relationEditable) {
            column.addClass(RowEditor.EDITABLE_RELATION_STYLE);
            column.setInstantKarma(true);
          }
        }
        break;

      case CALCULATED:
        AbstractCell<String> cell =
            (cellType == null) ? new CalculatedCell() : GridFactory.createCell(cellType);
        CalculatedColumn calcColumn = new CalculatedColumn(cell, cd.getValueType(), renderer);

        if (cd.getPrecision() != null) {
          calcColumn.setPrecision(cd.getPrecision());
        }
        if (cd.getScale() != null) {
          calcColumn.setScale(cd.getScale());
        }
        column = calcColumn;
        break;

      case SELECTION:
        column = new SelectionColumn(getGrid());
        source = null;
        if (BeeUtils.isEmpty(label)) {
          label = Localized.getConstants().selectionColumnLabel();
        }
        break;

      case ACTION:
        if (renderer == null && cellSource != null) {
          renderer = new SimpleRenderer(cellSource);
        }
        if (renderer != null) {
          column = new ActionColumn(ActionCell.create(getViewName(), cd), renderer);
        }
        break;

      case PROPERTY:
        column = GridFactory.createColumn(cellSource, cellType, renderer);
        break;

      case RIGHTS:
        break;
    }

    if (column == null) {
      logger.warning("cannot create column:", columnId, colType);
      return false;
    }

    if (!BeeUtils.isEmpty(cd.getSortBy())) {
      column.setSortBy(NameUtils.toList(cd.getSortBy()));
    } else if (!BeeUtils.isEmpty(renderColumns)) {
      column.setSortBy(new ArrayList<>(renderColumns));
    } else if (!BeeUtils.isEmpty(source)) {
      column.setSortBy(Lists.newArrayList(source));
    }

    if (!BeeUtils.isEmpty(cd.getSearchBy())) {
      column.setSearchBy(NameUtils.toList(cd.getSearchBy()));
    } else if (!BeeUtils.isEmpty(column.getSortBy())) {
      column.setSearchBy(Lists.newArrayList(column.getSortBy()));
    } else if (!BeeUtils.isEmpty(source)) {
      column.setSearchBy(Lists.newArrayList(source));
    }

    if (BeeUtils.isTrue(cd.getSortable()) && !BeeUtils.isEmpty(column.getSortBy())) {
      column.setSortable(true);
    }

    if (!BeeUtils.isEmpty(cd.getFormat())) {
      if (column instanceof HandlesFormat) {
        ((HandlesFormat) column).setFormat(cd.getFormat());
      } else {
        Format.setFormat(column, column.getValueType(), cd.getFormat());
      }
    } else if (BeeUtils.isNonNegative(cd.getScale()) && (column instanceof HasNumberFormat)) {
      ((HasNumberFormat) column).setNumberFormat(Format.getDecimalFormat(cd.getScale()));
    }

    if (!BeeUtils.isEmpty(cd.getHorAlign())) {
      UiHelper.setHorizontalAlignment(column, cd.getHorAlign());
    }
    if (!BeeUtils.isEmpty(cd.getWhiteSpace())) {
      UiHelper.setWhiteSpace(column, cd.getWhiteSpace());
    }

    if (BeeUtils.isTrue(cd.getDraggable())) {
      column.setDraggable(true);
    }

    if (!BeeUtils.isEmpty(cd.getOptions())) {
      column.setOptions(cd.getOptions());
    }

    ColumnHeader header = null;
    if (gridDescription.hasColumnHeaders()) {
      String headerCaption;
      if (BeeConst.STRING_MINUS.equals(caption)) {
        headerCaption = null;
      } else {
        headerCaption = BeeUtils.notEmpty(caption, columnId);
      }

      if (gridInterceptor != null) {
        header = gridInterceptor.getHeader(columnId, headerCaption);
      }

      if (header == null) {
        if (colType == ColType.SELECTION) {
          header = new ColumnHeader(columnId, new SelectionHeader(), BeeConst.STRING_CHECK_MARK);
        } else {
          header = new ColumnHeader(columnId, headerCaption, headerCaption);
        }
      }
    }

    ColumnFooter footer = null;
    if (gridDescription.hasFooters()) {
      if (gridInterceptor != null) {
        footer = gridInterceptor.getFooter(columnId, cd);
      }
      if (footer == null && cd.getFooterDescription() != null) {
        footer = new ColumnFooter(cellSource, column, cd, dataColumns);
      }
    }

    EditableColumn editableColumn = colType.isReadOnly() ? null
        : new EditableColumn(getViewName(), dataColumns, dataIndex, column, label, cd, enumKey);

    if (gridInterceptor != null && !gridInterceptor.afterCreateColumn(columnId, dataColumns,
        column, header, footer, editableColumn)) {
      return false;
    }

    if (editableColumn != null) {
      editableColumn.setNotificationListener(this);
      getEditableColumns().put(BeeUtils.normalize(columnId), editableColumn);

      if (BeeUtils.isTrue(cd.getEditInPlace())) {
        getEditInPlace().add(columnId);
      }
    }

    AbstractFilterSupplier filterSupplier = null;
    if (gridInterceptor != null) {
      filterSupplier = gridInterceptor.getFilterSupplier(columnId, cd);
    }

    if (filterSupplier == null && !BeeConst.STRING_MINUS.equals(cd.getFilterOptions())
        && (filterSupplierType != null
            || !BeeConst.isUndef(dataIndex) || !BeeUtils.isEmpty(column.getSearchBy()))) {

      filterSupplier = FilterSupplierFactory.getSupplier(getViewName(), dataColumns,
          gridDescription.getIdName(), gridDescription.getVersionName(), dataIndex, label,
          column.getSearchBy(), cd.getValueType(), filterSupplierType, renderColumns,
          column.getSortBy(), enumKey, cd.getRelation(), cd.getFilterOptions());
    }

    ColumnInfo columnInfo = new ColumnInfo(columnId, label, cellSource, column, header, footer,
        filterSupplier, dynGroup);

    if (BeeUtils.isTrue(cd.getVisible())) {
      columnInfo.setHidable(false);
    }
    if (isColumnReadOnly(getViewName(), source, cd)) {
      columnInfo.setColReadOnly(true);
    }

    columnInfo.initProperties(cd, gridDescription);

    ConditionalStyle columnStyles = null;

    if (gridInterceptor != null) {
      StyleProvider styleProvider = gridInterceptor.getColumnStyleProvider(columnId);
      if (styleProvider != null) {
        columnStyles = ConditionalStyle.create(styleProvider);
      }
    }
    if (columnStyles == null && cd.getDynStyles() != null) {
      columnStyles = ConditionalStyle.create(cd.getDynStyles(), columnId, dataColumns);
    }
    if (columnStyles == null) {
      StyleProvider styleProvider = ConditionalStyle.getGridColumnStyleProvider(getGridName(),
          columnId);
      if (styleProvider != null) {
        columnStyles = ConditionalStyle.create(styleProvider);
      }
    }

    if (columnStyles != null) {
      columnInfo.setDynStyles(columnStyles);
    }

    getGrid().addColumn(columnInfo, !BeeUtils.isFalse(cd.getVisible()), beforeIndex);

    return true;
  }

  @Override
  public HandlerRegistration addEditFormHandler(EditFormEvent.Handler handler) {
    return addHandler(handler, EditFormEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyForInsertHandler(ReadyForInsertEvent.Handler handler) {
    return addHandler(handler, ReadyForInsertEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyForUpdateHandler(ReadyForUpdateEvent.Handler handler) {
    return addHandler(handler, ReadyForUpdateEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public HandlerRegistration addSaveChangesHandler(SaveChangesEvent.Handler handler) {
    return addHandler(handler, SaveChangesEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public void clearNotifications() {
    if (getNotification() != null) {
      getNotification().clear();
    }
  }

  @Override
  public void create(Order order) {
    if (gridInterceptor != null) {
      gridInterceptor.beforeCreate(dataColumns, gridDescription);
    }

    if (gridDescription.getStyleSheets() != null) {
      Global.addStyleSheets(gridDescription.getStyleSheets());
    }

    initGrid();

    if (gridDescription.getRowEditable() != null) {
      setRowEditable(Evaluator.create(gridDescription.getRowEditable(), null, dataColumns));
    }

    if (gridDescription.getRowValidation() != null) {
      setRowValidation(Evaluator.create(gridDescription.getRowValidation(), null, dataColumns));
    }

    if (!BeeUtils.isEmpty(gridDescription.getOptions())) {
      setOptions(gridDescription.getOptions());
    }
    if (!BeeUtils.isEmpty(gridDescription.getProperties())) {
      setProperties(gridDescription.getProperties());
    }

    initNewRowDefaults(gridDescription.getNewRowDefaults());
    initCopyColumns(gridDescription.getEnableCopy());

    setNewRowCaption(BeeUtils.notEmpty(gridDescription.getNewRowCaption(),
        (getDataInfo() == null) ? null : getDataInfo().getNewRowCaption()));

    initOrder(order);

    add(getGrid());
    add(getNotification());

    setEditMode(BeeUtils.unbox(gridDescription.getEditMode()));
    setEditSave(BeeUtils.unbox(gridDescription.getEditSave()));

    setEditFormName(normalizeFormName(BeeUtils.notEmpty(gridDescription.getEditForm(),
        (getDataInfo() == null) ? null : getDataInfo().getEditForm())));
    setNewRowFormName(normalizeFormName(BeeUtils.notEmpty(gridDescription.getNewRowForm(),
        (getDataInfo() == null) ? null : getDataInfo().getNewRowForm())));

    setShowEditPopup(BeeUtils.nvl(gridDescription.getEditPopup(), isChild()));
    setShowNewRowPopup(BeeUtils.nvl(gridDescription.getNewRowPopup(), isChild()));

    setSingleForm(!BeeUtils.isEmpty(getEditFormName())
        && BeeUtils.same(getNewRowFormName(), getEditFormName()));

    if (!BeeUtils.isEmpty(getEditFormName())) {
      if (gridDescription.getEditMessage() != null) {
        setEditMessage(Evaluator.create(gridDescription.getEditMessage(), null, dataColumns));
      }
      setEditShowId(BeeUtils.unbox(gridDescription.getEditShowId()));
    }

    if (BeeUtils.isTrue(gridDescription.getEditFormImmediate())) {
      createEditForm();
    }
    if (BeeUtils.isTrue(gridDescription.getNewRowFormImmediate())) {
      createNewRowForm();
    }

    String viewName = gridDescription.getViewName();
    if (BeeUtils.isEmpty(getNewRowFormName()) && !BeeUtils.isEmpty(viewName) && !isReadOnly()
        && BeeKeeper.getUser().canCreateData(viewName)) {
      generateNewRowForm();
      setNewRowFormGenerated(true);
    }

    if (gridInterceptor != null) {
      gridInterceptor.afterCreate(this);
    }
  }

  @Override
  public void createParentRow(final NotificationListener notificationListener,
      final Callback<IsRow> callback) {

    final FormView form = getForm(!isAdding());

    if (!form.validate(notificationListener, false)) {
      return;
    }
    if (!validateFormData(form, notificationListener, false)) {
      return;
    }

    IsRow row = form.getActiveRow();

    if (DataUtils.isNewRow(row)) {
      prepareForInsert(row, form, new RowCallback() {
        @Override
        public void onFailure(String... reason) {
          if (callback != null) {
            callback.onFailure(reason);
          }
        }

        @Override
        public void onSuccess(BeeRow result) {
          if (form.getFormInterceptor() != null) {
            form.getFormInterceptor().afterInsertRow(result, true);
          }
          form.observeData();
          form.updateRow(result, true);

          IsRow copy = DataUtils.cloneRow(result);
          getGrid().insertRow(copy, false);

          if (callback != null) {
            callback.onSuccess(result);
          }

          if (getGridInterceptor() != null) {
            getGridInterceptor().afterInsertRow(result);
          }
        }
      });
    } else if (callback != null) {
      callback.onSuccess(row);
    }
  }

  @Override
  public void ensureRelId(final IdCallback callback) {
    Assert.notNull(callback);

    if (DataUtils.isId(getRelId())) {
      callback.onSuccess(getRelId());
      return;
    }

    if (!isChild()) {
      callback.onFailure(getViewName(), "not a child");
      return;
    }

    FormView parentForm = ViewHelper.getForm(this);
    if (parentForm == null) {
      callback.onFailure(getViewName(), "parent form not found");
      return;
    }

    if (parentForm.getViewPresenter() instanceof ParentRowCreator) {
      ((ParentRowCreator) parentForm.getViewPresenter()).createParentRow(this,
          new Callback<IsRow>() {
            @Override
            public void onFailure(String... reason) {
              callback.onFailure(reason);
            }

            @Override
            public void onSuccess(IsRow result) {
              if (DataUtils.isId(getRelId())) {
                callback.onSuccess(getRelId());

              } else {
                if (getGridInterceptor() == null
                    || !getGridInterceptor().ensureRelId(callback)) {

                  callback.onFailure(getViewName(), "parent row not created");
                }
              }
            }
          });

    } else {
      callback.onFailure(getViewName(), "parent row creator not available");
    }
  }

  @Override
  public void ensureRow(IsRow row, boolean focus) {
    Assert.isTrue(DataUtils.hasId(row));

    if (!getGrid().containsRow(row.getId())) {
      getGrid().insertRow(row, focus);
    }
  }

  @Override
  public int estimatePageSize(int containerWidth, int containerHeight) {
    return getGrid().estimatePageSize(containerWidth, containerHeight, true);
  }

  @Override
  public void finishNewRow(IsRow row) {
    showForm(false, false);

    fireEvent(new AddEndEvent(showNewRowPopup()));
    setAdding(false);

    getGrid().setEditing(false);

    if (row == null) {
      maybeResizeGrid();
      getGrid().refocus();
    } else {
      getGrid().insertRow(row, true);
    }
  }

  @Override
  public void formCancel() {
    if (isAdding()) {
      finishNewRow(null);
    } else {
      closeEditForm();
    }
  }

  @Override
  public void formConfirm() {
    final FormView form = getForm(!isAdding());
    Assert.notNull(form, "formConfirm: active form is null");

    IsRow oldRow = form.getOldRow();
    IsRow newRow = form.getActiveRow();
    Assert.notNull(newRow, "formConfirm: active row is null");

    if (!validateFormData(form, form, true)) {
      return;
    }

    if (isAdding()) {
      if (DataUtils.isNewRow(newRow)) {
        prepareForInsert(newRow, form, new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            form.notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
            if (form.getFormInterceptor() != null) {
              form.getFormInterceptor().afterInsertRow(result, false);
            }

            finishNewRow(result);

            if (getGridInterceptor() != null) {
              getGridInterceptor().afterInsertRow(result);
            }
          }
        });

      } else {
        saveChanges(form, oldRow, newRow, new RowCallback() {
          @Override
          public void onCancel() {
            finishNewRow(null);
          }

          @Override
          public void onFailure(String... reason) {
            form.notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
            if (form.getFormInterceptor() != null) {
              form.getFormInterceptor().afterUpdateRow(result);
            }

            finishNewRow(null);

            if (getGridInterceptor() != null) {
              getGridInterceptor().afterUpdateRow(result);
            }
          }
        });
      }

    } else {
      saveChanges(form, oldRow, newRow, getSaveChangesCallback());
    }
  }

  @Override
  public FormView getActiveForm() {
    if (!BeeUtils.isEmpty(getActiveFormContainerId())) {
      FormView form = getForm(!isAdding());
      if (form != null && form.getState() == State.OPEN) {
        return form;
      }
    }
    return null;
  }

  @Override
  public IsRow getActiveRow() {
    if (isAdding() && getNewRowForm() != null) {
      return getNewRowForm().getActiveRow();
    } else {
      return getGrid().getActiveRow();
    }
  }

  @Override
  public List<BeeColumn> getDataColumns() {
    return dataColumns;
  }

  @Override
  public int getDataIndex(String source) {
    int index = DataUtils.getColumnIndex(source, getDataColumns());
    if (BeeConst.isUndef(index)) {
      logger.warning(getGridName(), source, "not found");
    }
    return index;
  }

  @Override
  public List<String> getDynamicColumnGroups() {
    return dynamicColumnGroups;
  }

  @Override
  public String getEditFormName() {
    return editFormName;
  }

  @Override
  public Set<String> getEditInPlace() {
    return editInPlace;
  }

  @Override
  public FormView getForm(boolean edit) {
    if (edit || isSingleFormInstance()) {
      return getEditForm();
    } else {
      return getNewRowForm();
    }
  }

  @Override
  public CellGrid getGrid() {
    return grid;
  }

  @Override
  public GridDescription getGridDescription() {
    return gridDescription;
  }

  @Override
  public GridInterceptor getGridInterceptor() {
    return gridInterceptor;
  }

  @Override
  public String getGridKey() {
    return gridKey;
  }

  @Override
  public String getGridName() {
    return gridDescription.getName();
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public String getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public String getRelColumn() {
    return relColumn;
  }

  public Long getRelId() {
    return relId;
  }

  @Override
  public List<? extends IsRow> getRowData() {
    return getGrid().getRowData();
  }

  @Override
  public Collection<RowInfo> getSelectedRows(SelectedRows mode) {
    if (getGrid().getSelectedRows().isEmpty() || mode == null || mode == SelectedRows.ALL) {
      return getGrid().getSelectedRows().values();
    }

    Collection<RowInfo> result = new ArrayList<>();
    boolean ok = false;

    for (RowInfo rowInfo : getGrid().getSelectedRows().values()) {
      switch (mode) {
        case EDITABLE:
          ok = rowInfo.isEditable();
          break;
        case REMOVABLE:
          ok = rowInfo.isEditable() && rowInfo.isRemovable();
          break;
        case ALL:
          ok = true;
          break;
      }

      if (ok) {
        result.add(rowInfo);
      }
    }
    return result;
  }

  @Override
  public Value getSummary() {
    return new IntegerValue(Math.max(getGrid().getRowCount(), 0));
  }

  @Override
  public String getViewName() {
    return gridDescription.getViewName();
  }

  @Override
  public GridPresenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean hasNotifications() {
    return getNotification() != null && getNotification().isActive();
  }

  @Override
  public void initData(int rowCount, BeeRowSet rowSet) {
    getGrid().setRowCount(rowCount, false);
    if (rowSet != null && !rowSet.isEmpty()) {
      getGrid().setRowData(rowSet.getRows(), false);
    }
  }

  @Override
  public boolean isAdding() {
    return adding;
  }

  @Override
  public boolean isChild() {
    return !BeeUtils.isEmpty(getRelColumn());
  }

  @Override
  public boolean isEmpty() {
    return getRowData().isEmpty();
  }

  @Override
  public boolean isEnabled() {
    return getGrid().isEnabled();
  }

  @Override
  public boolean isFlushable() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return getGrid().isReadOnly();
  }

  @Override
  public boolean isRowEditable(IsRow rowValue, NotificationListener notificationListener) {
    if (rowValue == null) {
      return false;
    }

    boolean ok = rowValue.isEditable();

    if (ok && getGridInterceptor() != null) {
      ok = getGridInterceptor().isRowEditable(rowValue);
    }

    if (ok && getRowEditable() != null) {
      getRowEditable().update(rowValue);
      ok = BeeUtils.toBoolean(getRowEditable().evaluate());
    }

    if (!ok && notificationListener != null) {
      notificationListener.notifyWarning(Localized.getConstants().rowIsReadOnly());
    }
    return ok;
  }

  @Override
  public boolean isRowSelected(long rowId) {
    return getGrid().isRowSelected(rowId);
  }

  @Override
  public boolean likeAMotherlessChild() {
    return isChild() && !DataUtils.isId(getRelId());
  }

  @Override
  public void notifyInfo(String... messages) {
    showNote(LogLevel.INFO, messages);
  }

  @Override
  public void notifySevere(String... messages) {
    showNote(LogLevel.ERROR, messages);
  }

  @Override
  public void notifyWarning(String... messages) {
    showNote(LogLevel.WARNING, messages);
  }

  @Override
  public void onEditEnd(EditEndEvent event, Object source) {
    Assert.notNull(event);
    getGrid().setEditing(false);
    getGrid().refocus();

    if (getGridInterceptor() != null) {
      getGridInterceptor().onEditEnd(event, source);
    }

    if (!event.isConsumed()) {
      String oldValue = event.getOldValue();
      String newValue = event.getNewValue();

      if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
        updateCell(event.getRowValue(), event.getColumn(), oldValue, newValue, event.isRowMode());
      }

      if (event.getKeyCode() != null) {
        int keyCode = BeeUtils.unbox(event.getKeyCode());
        if (BeeUtils.inList(keyCode, KeyCodes.KEY_TAB, KeyCodes.KEY_UP, KeyCodes.KEY_DOWN)) {
          getGrid().handleKeyboardNavigation(keyCode, event.hasModifiers());
        }
      }
    }
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    Assert.notNull(event);

    if (getEditForm() != null || BeeUtils.isEmpty(getEditFormName())) {
      openEditor(event);
    } else {
      setPendingEditStartEvent(event);
      createEditForm();
    }
  }

  @Override
  public boolean onHistory(Place place, boolean forward) {
    if (BeeUtils.isEmpty(getActiveFormContainerId())) {
      return false;
    }

    FormView form = getForm(!isAdding());
    if (form == null || !form.asWidget().isVisible()) {
      return false;
    }

    if (form.getViewPresenter() != null) {
      form.getViewPresenter().handleAction(Action.CLOSE);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onRender(RenderingEvent event) {
    Assert.notNull(event);

    if (getGridInterceptor() != null) {
      if (event.isBefore()) {
        getGridInterceptor().beforeRender(this, event);
      } else if (event.isAfter()) {
        getGridInterceptor().afterRender(this, event);
      }
    }

    if (event.isBefore() && !event.isConsumed() && !BeeUtils.isEmpty(getRowData())) {
      if (!getDynamicColumnGroups().isEmpty()) {
        for (String dynGroup : getDynamicColumnGroups()) {
          if (!event.canceled()) {
            DynamicColumnFactory.checkDynamicColumns(this, event, dynGroup);
          }
        }
      }

      if (!event.canceled() && getViewPresenter() != null) {
        DynamicColumnFactory.checkRightsColumns(getViewPresenter(), this, event);
      }

    } else if (event.isAfter() && getState() == null) {
      setState(State.INITIALIZED);

      if (isAttached()) {
        ReadyEvent.fire(this);
      }
    }
  }

  @Override
  public void onResize() {
    pendingResize.clear();

    String resized = null;

    if (getGrid().isVisible()) {
      getGrid().onResize();

    } else {
      pendingResize.add(getGrid().getId());

      String activeId = getActiveFormContainerId();
      if (UiHelper.hasImmediateChild(this, activeId) && UiHelper.maybeResize(this, activeId)) {
        resized = activeId;
      }
    }

    String id = getEditFormContainerId();
    if (!BeeUtils.isEmpty(id) && !BeeUtils.same(id, resized)) {
      pendingResize.add(id);
    }

    if (!isSingleFormInstance()) {
      id = getNewRowFormContainerId();
      if (!BeeUtils.isEmpty(id) && !BeeUtils.same(id, resized)) {
        pendingResize.add(id);
      }
    }
  }

  @Override
  public void onRowCountChange(RowCountChangeEvent event) {
    if (getGridInterceptor() != null && !getGridInterceptor().onRowCountChange(this, event)) {
      return;
    }

    if (summarize()) {
      SummaryChangeEvent.fire(this);
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (!event.hasView(getViewName())) {
      return;
    }
    if (event.getRow() == null) {
      return;
    }

    if (event.hasSourceId(getId())) {
      return;
    }
    if (getGrid().containsRow(event.getRowId())) {
      return;
    }

    if (getGridInterceptor() != null && !getGridInterceptor().onRowInsert(event)) {
      return;
    }

    if (BeeUtils.isEmpty(event.getSourceId()) && !event.isSpookyActionAtADistance()) {
      return;
    }

    if (getGrid().getPageSize() > 0 && getGrid().getRowCount() > getGrid().getPageSize()) {
      return;
    }

    if (isChild()) {
      if (!DataUtils.isId(getRelId())) {
        return;
      }

      int index = DataUtils.getColumnIndex(getRelColumn(), getDataColumns());
      if (index < 0) {
        return;
      }

      if (!Objects.equals(getRelId(), event.getRow().getLong(index))) {
        return;
      }

    } else if (getViewPresenter() == null || getViewPresenter().hasFilter()) {
      return;
    }

    getGrid().insertRow(event.getRow(), false);
    logger.info("grid", getId(), getViewName(), "insert row", event.getRowId());
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (getGridInterceptor() != null && event.hasView(getViewName())) {
      getGridInterceptor().onRowUpdate(event);
    }
  }

  @Override
  public void onSettingsChange(SettingsChangeEvent event) {
    GridSettings.onSettingsChange(gridKey, event);
  }

  @Override
  public void onSort(SortEvent event) {
    GridSettings.saveSortOrder(gridKey, event.getOrder());
  }

  @Override
  public boolean reactsTo(Action action) {
    GridPresenter presenter = getViewPresenter();
    return presenter != null && presenter.isActionEnabled(action);
  }

  @Override
  public void refresh(boolean refreshChildren, boolean focus) {
    getGrid().refresh();
  }

  @Override
  public int refreshBySource(String source) {
    IsRow row = getGrid().getActiveRow();
    return (row == null) ? 0 : refreshCell(row.getId(), source);
  }

  @Override
  public int refreshCell(long rowId, String columnSource) {
    return getGrid().refreshCell(rowId, columnSource);
  }

  @Override
  public void reset(GridDescription gd) {
    Assert.notNull(gd);
    if (getGridInterceptor() != null && !getGridInterceptor().initDescription(gd)) {
      return;
    }

    List<? extends IsRow> rowData = getRowData();
    int rowCount = getGrid().getRowCount();

    int pageStart = getGrid().getPageStart();
    int pageSize = getGrid().getPageSize();

    Order order = getGrid().getSortOrder();

    gridDescription.deserialize(gd.serialize());

    remove(getGrid());
    this.grid = new CellGrid();

    getEditableColumns().clear();
    getDynamicColumnGroups().clear();
    getEditInPlace().clear();

    initGrid();
    initOrder(order);

    insert(getGrid(), 0);

    getGrid().setPageStart(pageStart, false, false, NavigationOrigin.SYSTEM);
    getGrid().setPageSize(pageSize, false);

    getGrid().setRowCount(rowCount, false);
    getGrid().setRowData(rowData, true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    getGrid().setEnabled(enabled);
  }

  @Override
  public void setRelId(Long relId) {
    this.relId = relId;
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setViewPresenter(Presenter presenter) {
    if (presenter instanceof GridPresenter) {
      this.viewPresenter = (GridPresenter) presenter;
    } else if (presenter == null) {
      this.viewPresenter = null;
    }
  }

  @Override
  public void startNewRow(boolean copy) {
    if (getForm(false) != null) {
      openNewRow(copy);

    } else {
      newRowFormState.add(State.PENDING);
      if (copy) {
        newRowFormState.add(State.COPYING);
      }

      if (isSingleForm()) {
        createEditForm();
      } else {
        createNewRowForm();
      }
    }
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public boolean validateFormData(FormView form, NotificationListener notificationListener,
      boolean focusOnError) {

    boolean ok = true;
    if (isReadOnly()) {
      return ok;
    }

    IsRow oldRow = getGrid().getActiveRow();
    IsRow newRow = form.getActiveRow();

    String oldValue = null;
    String newValue;
    int index;

    for (Map.Entry<String, EditableColumn> entry : getEditableColumns().entrySet()) {
      if (getGrid().isColumnReadOnly(entry.getKey())) {
        continue;
      }

      EditableColumn ec = entry.getValue();
      if (!ec.isWritable()) {
        continue;
      }

      index = ec.getColIndex();
      if (oldRow != null) {
        oldValue = oldRow.getString(index);
      }
      newValue = newRow.getString(index);

      CellValidation cv = new CellValidation(oldValue, newValue, ec.getEditor(),
          EditorValidation.NEW_VALUE, ec.getValidation(), newRow, ec.getDataColumn(), index,
          ec.getDataType(), ec.isNullable(), ec.getCaption(), notificationListener);

      ok = BeeUtils.isTrue(ValidationHelper.validateCell(cv, ec, ValidationOrigin.GRID));
      if (!ok) {
        if (focusOnError) {
          form.focus(ec.getColumnId());
        }
        break;
      }
    }

    if (ok && getRowValidation() != null) {
      ok = ValidationHelper.validateRow(newRow, getRowValidation(), notificationListener);
    }
    return ok;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    registry.add(BeeKeeper.getBus().registerRowInsertHandler(this, false));
    registry.add(BeeKeeper.getBus().registerRowUpdateHandler(this, false));

    if (getState() == State.INITIALIZED) {
      ReadyEvent.fire(this);
    }
  }

  @Override
  protected void onUnload() {
    if (getGridInterceptor() != null) {
      getGridInterceptor().onUnload(this);
    }

    EventUtils.clearRegistry(registry);

    if (getNewRowPopup() != null) {
      getNewRowPopup().unload();
    }
    if (getEditPopup() != null) {
      getEditPopup().unload();
    }

    super.onUnload();
  }

  private void closeEditForm() {
    showForm(true, false);
    fireEvent(new EditFormEvent(State.CLOSED, showEditPopup()));

    maybeResizeGrid();
    getGrid().refocus();
  }

  private void createEditForm() {
    if (!editFormState.contains(State.LOADING) && !BeeUtils.isEmpty(getEditFormName())) {
      editFormState.add(State.LOADING);
      if (isSingleForm()) {
        newRowFormState.add(State.LOADING);
      }

      FormFactory.createFormView(getEditFormName(), getViewName(), getDataColumns(), true,
          new FormFactory.FormViewCallback() {
            @Override
            public void onSuccess(FormDescription formDescription, FormView result) {
              String containerId = createFormContainer(result, true, null, showEditPopup());
              setEditFormContainerId(containerId);
              setEditForm(result);

              if (isSingleFormInstance()) {
                setNewRowFormContainerId(containerId);

              } else if (isSingleForm()) {
                FormView newRowFormView = new FormImpl(getNewRowFormName());
                newRowFormView.create(formDescription, getViewName(), getDataColumns(), true,
                    FormFactory.getFormInterceptor(getNewRowFormName()));
                embraceNewRowForm(newRowFormView);
              }

              editFormState.remove(State.LOADING);

              boolean editPending = getPendingEditStartEvent() != null;
              boolean newRowPending = false;
              boolean newRowCopy = false;

              if (isSingleForm()) {
                newRowFormState.remove(State.LOADING);
                newRowPending = newRowFormState.remove(State.PENDING);
                newRowCopy = newRowFormState.remove(State.COPYING);
              }

              if (editPending) {
                openEditor(getPendingEditStartEvent());
                setPendingEditStartEvent(null);
              } else if (newRowPending) {
                openNewRow(newRowCopy);
              }
            }
          });
    }
  }

  private String createFormContainer(final FormView formView, boolean edit, String caption,
      boolean asPopup) {
    String formCaption = BeeUtils.notEmpty(caption, formView.getCaption());

    EnumSet<Action> actions = EnumSet.of(Action.PRINT, Action.CLOSE);
    if (!edit) {
      actions.add(Action.SAVE);
    } else if (!isReadOnly()) {
      if (hasEditMode()) {
        actions.add(Action.EDIT);
      }
      if (hasEditSave() || isSingleFormInstance()) {
        actions.add(Action.SAVE);
      }
    }

    final GridFormPresenter gfp = new GridFormPresenter(this, formView, formCaption, actions, edit,
        hasEditSave());
    Widget container = gfp.getMainView().asWidget();

    if (asPopup) {
      ModalForm popup = new ModalForm(gfp, formView, true);

      popup.setOnSave(new PreviewConsumer() {
        @Override
        public void accept(NativePreviewEvent input) {
          if (gfp.isActionEnabled(Action.SAVE) && formView.checkOnSave(input)) {
            gfp.handleAction(Action.SAVE);
          }
        }
      });

      popup.setOnEscape(new PreviewConsumer() {
        @Override
        public void accept(NativePreviewEvent input) {
          if (formView.checkOnClose(input)) {
            gfp.handleAction(Action.CLOSE);
          }
        }
      });

      if (edit) {
        setEditPopup(popup);
      } else {
        setNewRowPopup(popup);
      }

    } else {
      add(container);
      container.setVisible(false);
    }

    formView.setEditing(true);

    formView.setState(State.CLOSED);

    return DomUtils.getId(container);
  }

  private void createNewRowForm() {
    if (!newRowFormState.contains(State.LOADING) && !BeeUtils.isEmpty(getNewRowFormName())
        && !isSingleForm()) {
      newRowFormState.add(State.LOADING);

      FormFactory.createFormView(getNewRowFormName(), getViewName(), getDataColumns(), true,
          new FormFactory.FormViewCallback() {
            @Override
            public void onSuccess(FormDescription formDescription, FormView result) {
              embraceNewRowForm(result);
              newRowFormState.remove(State.LOADING);

              boolean pending = newRowFormState.remove(State.PENDING);
              boolean copy = newRowFormState.remove(State.COPYING);
              if (pending) {
                openNewRow(copy);
              }
            }
          });
    }
  }

  private void createNewRowWidgets(HtmlTable container, List<String> columnNames,
      WidgetDescriptionCallback callback) {

    int r = 0;

    for (String columnName : columnNames) {
      EditableColumn editableColumn = getEditableColumn(columnName, true);
      if (editableColumn == null) {
        continue;
      }

      Label label = new Label(editableColumn.getCaption());
      label.addStyleName(RowFactory.STYLE_NEW_ROW_LABEL);

      if (editableColumn.hasDefaults()) {
        label.addStyleName(StyleUtils.NAME_HAS_DEFAULTS);
      } else if (!editableColumn.isNullable()) {
        label.addStyleName(StyleUtils.NAME_REQUIRED);
      }

      container.setWidget(r, 0, label);
      container.getCellFormatter().setStyleName(r, 0, RowFactory.STYLE_NEW_ROW_LABEL_CELL);

      Editor editor = editableColumn.createEditor(true, getGridInterceptor());
      editor.asWidget().addStyleName(RowFactory.STYLE_NEW_ROW_INPUT);
      editor.asWidget().addStyleName(BeeUtils.join(BeeConst.STRING_MINUS,
          RowFactory.STYLE_NEW_ROW_INPUT, getGridName(), columnName));

      if (editableColumn.getEditorDescription() != null) {
        Dimensions defaultDimensions =
            EditorAssistant.getDefaultDimensions(editableColumn.getEditorDescription());
        if (defaultDimensions != null) {
          defaultDimensions.applyTo(editor.asWidget());
        }
      }

      container.setWidget(r, 1, editor.asWidget());
      container.getCellFormatter().setStyleName(r, 1, RowFactory.STYLE_NEW_ROW_INPUT_CELL);

      if (editor.getWidgetType() != null) {
        WidgetDescription widgetDescription = new WidgetDescription(editor.getWidgetType(),
            editor.getId(), columnName);

        widgetDescription.updateFrom(editableColumn);
        callback.onSuccess(widgetDescription, editor);
      }
      r++;
    }
  }

  private void embraceNewRowForm(FormView formView) {
    if (formView != null) {
      String id = createFormContainer(formView, false, getNewRowCaption(), showNewRowPopup());

      setNewRowFormContainerId(id);
      setNewRowForm(formView);
    }
  }

  private void generateNewRowForm() {
    String newRowColumns = BeeUtils.notEmpty(gridDescription.getNewRowColumns(),
        (getDataInfo() == null) ? null : getDataInfo().getNewRowColumns());

    if (BeeConst.STRING_MINUS.equals(newRowColumns)) {
      return;
    }

    final List<String> columnNames = getNewRowColumnNames(newRowColumns);

    if (columnNames.isEmpty()) {
      logger.severe("grid", gridDescription.getName(), "new row columns not available");
      return;
    }

    String formName = "grid-" + gridDescription.getName() + "-new-row";
    final String rootName = "root";

    FormDescription formDescription = FormFactory.createFormDescription(formName,
        ImmutableMap.of(UiConstants.ATTR_VIEW_NAME, gridDescription.getViewName()),
        FormWidget.TABLE, ImmutableMap.of(UiConstants.ATTR_NAME, rootName));

    FormView form = new FormImpl(formName);
    form.create(formDescription, gridDescription.getViewName(), getDataColumns(), true,
        new AbstractFormInterceptor() {
          @Override
          public void afterCreateEditableWidget(EditableWidget editableWidget,
              IdentifiableWidget widget) {
            EditableColumn ec = getEditableColumn(editableWidget.getWidgetName(), true);
            editableWidget.setValidationDelegate(ec);
          }

          @Override
          public void afterCreateWidget(String name, IdentifiableWidget widget,
              WidgetDescriptionCallback wdc) {
            if (BeeUtils.same(name, rootName) && widget instanceof HtmlTable) {
              widget.asWidget().addStyleName(RowFactory.STYLE_NEW_ROW_TABLE);
              createNewRowWidgets((HtmlTable) widget, columnNames, wdc);
            }
          }

          @Override
          public FormInterceptor getInstance() {
            return null;
          }

          @Override
          public AbstractCellRenderer getRenderer(WidgetDescription widgetDescription) {
            EditableColumn ec = getEditableColumn(widgetDescription.getWidgetName(), true);
            AbstractColumn<?> uiColumn = (ec == null) ? null : ec.getUiColumn();

            if (uiColumn instanceof HasCellRenderer) {
              return ((HasCellRenderer) uiColumn).getRenderer();
            } else {
              return null;
            }
          }
        });

    form.setCaption(Localized.getConstants().actionNew());

    double initialWidth = RowFactory.GENERATED_FORM_WIDTH;
    double initialHeight = RowFactory.GENERATED_HEADER_HEIGHT + RowFactory.GENERATED_HEIGHT_MARGIN
        + columnNames.size() * RowFactory.GENERATED_ROW_HEIGHT;

    form.setWidthValue(initialWidth);
    form.setHeightValue(initialHeight);

    embraceNewRowForm(form);
  }

  private String getActiveFormContainerId() {
    return activeFormContainerId;
  }

  private DataInfo getDataInfo() {
    return dataInfo;
  }

  private EditableColumn getEditableColumn(String columnId, boolean warn) {
    if (BeeUtils.isEmpty(columnId)) {
      if (warn) {
        logger.warning("editable column id not specified");
      }
      return null;
    }

    EditableColumn editableColumn = getEditableColumns().get(BeeUtils.normalize(columnId));
    if (editableColumn == null && warn) {
      logger.warning("editable column not found:", columnId);
    }
    return editableColumn;
  }

  private Map<String, EditableColumn> getEditableColumns() {
    return editableColumns;
  }

  private FormView getEditForm() {
    return editForm;
  }

  private String getEditFormContainerId() {
    return editFormContainerId;
  }

  private Evaluator getEditMessage() {
    return editMessage;
  }

  private ModalForm getEditPopup() {
    return editPopup;
  }

  private boolean getEditShowId() {
    return editShowId;
  }

  private String getNewRowCaption() {
    return newRowCaption;
  }

  private List<String> getNewRowColumnNames(String columnNames) {
    List<String> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(columnNames)) {
      for (String colName : NameUtils.NAME_SPLITTER.split(columnNames)) {
        if (BeeUtils.isEmpty(colName)) {
          continue;
        }

        String name = BeeUtils.normalize(colName);
        if (!getEditableColumns().containsKey(name)) {
          logger.warning("newRowColumn", colName, "is not editable");
          continue;
        }

        if (!result.contains(name)) {
          result.add(colName);
        }
      }
    }

    if (result.isEmpty()) {
      for (ColumnInfo columnInfo : getGrid().getColumns()) {
        String id = columnInfo.getColumnId();
        EditableColumn ec = getEditableColumn(id, false);

        if (ec != null && !result.contains(id)) {
          if (columnInfo.isColReadOnly()) {
            BeeColumn dataColumn = ec.getDataColumn();
            if (dataColumn.isEditable() && !dataColumn.isNullable() && !dataColumn.hasDefaults()) {
              result.add(id);
            }

          } else {
            result.add(id);
          }
        }
      }
    }
    return result;
  }

  private FormView getNewRowForm() {
    return newRowForm;
  }

  private String getNewRowFormContainerId() {
    return newRowFormContainerId;
  }

  private String getNewRowFormName() {
    return newRowFormName;
  }

  private ModalForm getNewRowPopup() {
    return newRowPopup;
  }

  private Notification getNotification() {
    return notification;
  }

  private EditStartEvent getPendingEditStartEvent() {
    return pendingEditStartEvent;
  }

  private String getRowCaption(IsRow row, boolean edit) {
    if (getGridInterceptor() == null) {
      return null;
    }
    return getGridInterceptor().getRowCaption(row, edit);
  }

  private Evaluator getRowEditable() {
    return rowEditable;
  }

  private Evaluator getRowValidation() {
    return rowValidation;
  }

  private SaveChangesCallback getSaveChangesCallback() {
    if (saveChangesCallback == null) {
      saveChangesCallback = new SaveChangesCallback();
    }
    return saveChangesCallback;
  }

  private State getState() {
    return state;
  }

  private boolean hasEditMode() {
    return editMode;
  }

  private boolean hasEditSave() {
    return editSave;
  }

  private void initCopyColumns(String input) {
    if (!copyColumns.isEmpty()) {
      copyColumns.clear();
    }

    if (BeeUtils.isEmpty(input) || Wildcards.isDefaultAny(input)
        || BeeUtils.containsOnly(input, BeeConst.CHAR_MINUS) || BeeUtils.isEmpty(dataColumns)) {
      return;
    }

    boolean exclude = BeeUtils.isPrefixOrSuffix(input, BeeConst.CHAR_MINUS);

    Set<Pattern> patterns = new HashSet<>();
    for (String s : NameUtils.NAME_SPLITTER.split(BeeUtils.remove(input, BeeConst.CHAR_MINUS))) {
      patterns.add(Wildcards.getDefaultPattern(s, false));
    }

    for (int i = 0; i < dataColumns.size(); i++) {
      if (Wildcards.contains(patterns, dataColumns.get(i).getId()) != exclude) {
        copyColumns.add(i);
      }
    }
  }

  private void initNewRowDefaults(String input) {
    if (!newRowDefaults.isEmpty()) {
      newRowDefaults.clear();
    }

    if (BeeUtils.same(input, BeeConst.STRING_MINUS) || BeeUtils.isEmpty(dataColumns)) {
      return;
    }

    if (BeeUtils.isEmpty(input) || Wildcards.isDefaultAny(input)) {
      for (BeeColumn column : dataColumns) {
        if (column.hasDefaults()) {
          newRowDefaults.add(column.getId());
        }
      }
      return;
    }

    Set<Pattern> patterns = new HashSet<>();
    for (String s : NameUtils.NAME_SPLITTER.split(input)) {
      patterns.add(Wildcards.getDefaultPattern(s, false));
    }

    for (BeeColumn column : dataColumns) {
      if (column.hasDefaults() && Wildcards.contains(patterns, column.getId())) {
        newRowDefaults.add(column.getId());
      }
    }
  }

  private void initGrid() {
    if (gridDescription.getHeader() != null && gridDescription.hasColumnHeaders()) {
      getGrid().setHeaderComponent(gridDescription.getHeader());
    }
    if (gridDescription.getBody() != null) {
      getGrid().setBodyComponent(gridDescription.getBody());
    }
    if (gridDescription.getFooter() != null && gridDescription.hasFooters()) {
      getGrid().setFooterComponent(gridDescription.getFooter());
    }

    if (BeeUtils.isTrue(gridDescription.isReadOnly())
        || !BeeUtils.isEmpty(getViewName()) && !Data.isViewEditable(getViewName())) {
      getGrid().setReadOnly(true);
    }

    ConditionalStyle rowStyles = null;

    if (gridInterceptor != null) {
      StyleProvider styleProvider = gridInterceptor.getRowStyleProvider();
      if (styleProvider != null) {
        rowStyles = ConditionalStyle.create(styleProvider);
      }
    }
    if (rowStyles == null && gridDescription.getRowStyles() != null) {
      rowStyles = ConditionalStyle.create(gridDescription.getRowStyles(), null, dataColumns);
    }
    if (rowStyles == null) {
      StyleProvider styleProvider = ConditionalStyle.getGridRowStyleProvider(getGridName());
      if (styleProvider != null) {
        rowStyles = ConditionalStyle.create(styleProvider);
      }
    }

    if (rowStyles != null) {
      getGrid().setRowStyles(rowStyles);
    }

    getGrid().setRowEditable(new Predicate<IsRow>() {
      @Override
      public boolean apply(IsRow input) {
        return isRowEditable(input, null);
      }
    });

    List<ColumnDescription> columnDescriptions = gridDescription.getColumns();
    if (gridInterceptor != null) {
      gridInterceptor.beforeCreateColumns(dataColumns, columnDescriptions);
    }

    for (ColumnDescription columnDescription : columnDescriptions) {
      if (isColumnVisible(getGridName(), columnDescription)) {
        if (BeeUtils.isTrue(columnDescription.getDynamic())) {
          dynamicColumnGroups.add(columnDescription.getId());
        } else {
          addColumn(columnDescription, null, BeeConst.UNDEF);
        }
      }
    }

    if (gridInterceptor != null) {
      gridInterceptor.afterCreateColumns(this);
    }

    getGrid().initRenderMode(gridDescription.getRenderMode());

    getGrid().setRowChangeSensitivityMillis(gridDescription.getRowChangeSensitivityMillis());

    getGrid().estimateHeaderWidths();

    getGrid().setDefaultFlexibility(gridDescription.getFlexibility());

    getGrid().addEditStartHandler(this);

    getGrid().addSortHandler(this);
    getGrid().addSettingsChangeHandler(this);
    getGrid().addRenderingHandler(this);
    getGrid().addRowCountChangeHandler(this);
  }

  private void initOrder(Order viewOrder) {
    if (viewOrder == null) {
      return;
    }

    Order gridOrder = getGrid().getSortOrder();
    if (!gridOrder.isEmpty()) {
      gridOrder.clear();
    }
    if (viewOrder.isEmpty()) {
      return;
    }

    List<ColumnInfo> columns = getGrid().getColumns();

    for (Order.Column oc : viewOrder.getColumns()) {
      for (ColumnInfo columnInfo : columns) {
        List<String> sortBy = columnInfo.getSortBy();

        if (!BeeUtils.isEmpty(sortBy) && sortBy.equals(oc.getSources())) {
          gridOrder.add(columnInfo.getColumnId(), sortBy, oc.isAscending());
          break;
        }
      }
    }
  }

  private boolean isNewRowFormGenerated() {
    return newRowFormGenerated;
  }

  private boolean isSingleForm() {
    return singleForm;
  }

  private boolean isSingleFormInstance() {
    return isSingleForm() && !showNewRowPopup() && !showEditPopup();
  }

  private boolean maybeOpenRelatedData(final EditableColumn editableColumn, final IsRow row,
      int charCode, boolean readOnly) {

    if (row == null || editableColumn == null || !editableColumn.hasRelation()
        || !editableColumn.getRelation().isEditEnabled(false)) {
      return false;
    }

    boolean ok;
    if (charCode == EditStartEvent.CLICK) {
      ok = true;
    } else {
      Integer editKey = editableColumn.getRelation().getEditKey();
      if (editKey == null) {
        ok = (!isEnabled() || readOnly) && EditStartEvent.isEnter(charCode);
      } else if (EditStartEvent.isEnter(editKey)) {
        ok = EditStartEvent.isEnter(charCode);
      } else {
        ok = editKey == charCode;
      }
    }
    if (!ok) {
      return false;
    }

    final String editSource;
    String editViewName = editableColumn.getRelation().getEditViewName();

    String es = editableColumn.getRelation().getEditSource();

    if (BeeUtils.isEmpty(es)) {
      editSource = editableColumn.getColumnId();
      if (BeeUtils.isEmpty(editViewName)) {
        editViewName = editableColumn.getRelation().getViewName();
      }
    } else {
      editSource = es;
      if (BeeUtils.isEmpty(editViewName)) {
        editViewName = getDataInfo().getRelation(editSource);
      }
    }
    if (BeeUtils.isEmpty(editViewName)) {
      return false;
    }

    if (!Data.isViewVisible(editViewName)) {
      return false;
    }

    Long id = row.getLong(getDataInfo().getColumnIndex(editSource));
    if (!DataUtils.isId(id)) {
      return false;
    }

    final DataInfo editDataInfo = Data.getDataInfo(editViewName);
    if (editDataInfo == null) {
      return false;
    }

    String formName = RowEditor.getFormName(editableColumn.getRelation().getEditForm(),
        editDataInfo);
    if (BeeUtils.isEmpty(formName)) {
      return false;
    }

    boolean modal = BeeUtils.nvl(editableColumn.getRelation().isEditModal(),
        charCode != EditStartEvent.CLICK) || UiHelper.isModal(this);
    RowCallback rowCallback;

    if (modal) {
      rowCallback = new RowCallback() {
        @Override
        public void onCancel() {
          getGrid().refocus();
        }

        @Override
        public void onSuccess(BeeRow result) {
          if (!RelationUtils.updateRow(getDataInfo(), editSource, row, editDataInfo, result,
              false).isEmpty()) {
            getGrid().refreshCell(row.getId(), editableColumn.getColumnId());
          }

          getGrid().refocus();
        }
      };

    } else {
      rowCallback = null;
    }

    Opener opener = modal ? Opener.MODAL : Opener.NEW_TAB;
    RowEditor.openForm(formName, editDataInfo, id, opener, rowCallback);
    return true;
  }

  private void maybeResizeGrid() {
    if (pendingResize.remove(getGrid().getId())) {
      getGrid().onResize();
    }
  }

  private void openEditor(EditStartEvent event) {
    if (getGridInterceptor() != null && isEnabled()) {
      getGridInterceptor().onEditStart(event);
      if (event.isConsumed()) {
        return;
      }
    }

    IsRow rowValue = event.getRowValue();
    String columnId = event.getColumnId();
    final EditableColumn editableColumn = getEditableColumn(columnId, false);

    if (maybeOpenRelatedData(editableColumn, rowValue, event.getCharCode(), event.isReadOnly())) {
      return;
    }

    boolean useForm = useFormForEdit(columnId);
    boolean editable = isEnabled() && !isReadOnly();

    if (useForm) {
      if (editable) {
        editable = isRowEditable(rowValue, BeeKeeper.getScreen());
      }
      if (editable && getEditForm() != null) {
        editable = getEditForm().isRowEditable(rowValue, false);
      }

    } else {
      if (!editable || event.isReadOnly()) {
        return;
      }
      if (!isRowEditable(rowValue, BeeKeeper.getScreen())) {
        return;
      }
      if (editableColumn == null) {
        return;
      }
      if (!editableColumn.isCellEditable(rowValue, true)) {
        return;
      }
    }

    if (useForm) {
      fireEvent(new EditFormEvent(State.OPEN, showEditPopup()));
      showForm(true, true);

      GridFormPresenter presenter = (GridFormPresenter) getEditForm().getViewPresenter();

      String caption = getRowCaption(rowValue, true);
      if (isSingleForm()) {
        presenter.setCaption(BeeUtils.notEmpty(caption, getEditForm().getCaption()));
        presenter.updateStyle(true);
      } else if (!BeeUtils.isEmpty(caption)) {
        presenter.setCaption(caption);
      }
      updateEditFormMessage(presenter, rowValue);

      final boolean enableForm;

      if (presenter.hasAction(Action.EDIT)) {
        presenter.showAction(Action.EDIT, editable && hasEditMode());
        if (presenter.hasAction(Action.SAVE)) {
          presenter.hideAction(Action.SAVE);
        }
        enableForm = false;

      } else {
        if (presenter.hasAction(Action.SAVE)) {
          presenter.showAction(Action.SAVE, editable && hasEditSave());
        }
        enableForm = editable;
      }

      getEditForm().setEnabled(enableForm);

      ScheduledCommand focusCommand = new ScheduledCommand() {
        @Override
        public void execute() {
          if (enableForm) {
            Widget widget = null;

            if (editableColumn != null) {
              String source = editableColumn.getColumnId();
              widget = getEditForm().getWidgetBySource(source);

              if (widget == null && getDataInfo() != null) {
                String relSource = getDataInfo().getEditableRelationSource(source);
                if (!BeeUtils.isEmpty(relSource) && !BeeUtils.same(source, relSource)) {
                  widget = getEditForm().getWidgetBySource(relSource);
                }
              }
            }

            if (widget == null || !UiHelper.focus(widget)) {
              UiHelper.focus(getEditForm().asWidget());
            }
          }
        }
      };

      getEditForm().editRow(rowValue, focusCommand);
      return;
    }

    if (event.getCharCode() == EditStartEvent.DELETE) {
      if (!editableColumn.isNullable()) {
        return;
      }

      String oldValue = editableColumn.getOldValue(rowValue);
      if (BeeUtils.isEmpty(oldValue)) {
        return;
      }

      validateAndUpdate(editableColumn, rowValue, oldValue, null, false);
      return;
    }

    if (ValueType.BOOLEAN.equals(editableColumn.getDataType())) {
      if (EditStartEvent.isClickOrEnter(event.getCharCode())) {
        String oldValue = editableColumn.getOldValue(rowValue);
        Boolean b = !BeeUtils.toBoolean(oldValue);
        if (!b && editableColumn.isNullable()) {
          b = null;
        }
        String newValue = BooleanValue.pack(b);

        validateAndUpdate(editableColumn, rowValue, oldValue, newValue, true);
      }
      return;
    }

    getGrid().setEditing(true);
    if (event.getSourceElement() != null) {
      event.getSourceElement().blur();
    }

    editableColumn.openEditor(this, getGridInterceptor(), event.getSourceElement(),
        getGrid().getElement(), getGrid().getZIndex() + 1, rowValue,
        BeeUtils.toChar(event.getCharCode()), this);
  }

  private void openNewRow(boolean copy) {
    if (!isEnabled() || isReadOnly()) {
      return;
    }

    IsRow oldRow = getGrid().getActiveRow();
    IsRow newRow = DataUtils.createEmptyRow(getDataColumns().size());

    List<String> defCols = new ArrayList<>();
    if (!newRowDefaults.isEmpty()) {
      defCols.addAll(newRowDefaults);
    }

    if (copy && oldRow != null) {
      if (copyColumns.isEmpty()) {
        for (int i = 0; i < getDataColumns().size(); i++) {
          if (!oldRow.isNull(i)) {
            newRow.setValue(i, oldRow.getString(i));
          }
        }

      } else {
        for (int i : copyColumns) {
          if (!oldRow.isNull(i)) {
            newRow.setValue(i, oldRow.getString(i));

            if (!defCols.isEmpty()) {
              String colId = getDataColumns().get(i).getId();
              if (defCols.contains(colId)) {
                defCols.remove(colId);
              }
            }
          }
        }
      }
    }

    if (!defCols.isEmpty()) {
      DataUtils.setDefaults(newRow, defCols, getDataColumns(), Global.getDefaults());
      RelationUtils.setDefaults(getDataInfo(), newRow, defCols, getDataColumns(),
          BeeKeeper.getUser().getUserData());
    }

    for (EditableColumn editableColumn : getEditableColumns().values()) {
      if (!editableColumn.hasCarry()) {
        continue;
      }
      if (oldRow == null) {
        oldRow = DataUtils.createEmptyRow(getDataColumns().size());
      }

      String carry = editableColumn.getCarryValue(oldRow);
      if (!BeeUtils.isEmpty(carry)) {
        int index = editableColumn.getColIndex();
        newRow.setValue(index, carry);

        if (editableColumn.hasRelation() && BeeUtils.equalsTrim(carry, oldRow.getString(index))) {
          RelationUtils.setRelatedValues(getDataInfo(), editableColumn.getColumnId(),
              newRow, oldRow);
        }
      }
    }

    if (getGridInterceptor() != null && !getGridInterceptor().onStartNewRow(this, oldRow, newRow)) {
      return;
    }

    getGrid().setEditing(true);

    fireEvent(new AddStartEvent(null, showNewRowPopup()));

    setAdding(true);

    String caption = getRowCaption(newRow, false);

    showForm(false, true);
    FormView form = getForm(false);
    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().onStartNewRow(form, oldRow, newRow);
    }

    if (form.getViewPresenter() instanceof GridFormPresenter) {
      GridFormPresenter presenter = (GridFormPresenter) form.getViewPresenter();

      if (isSingleForm()) {
        presenter.setCaption(BeeUtils.notEmpty(caption, getNewRowCaption()));
        presenter.setMessage(null);
        presenter.updateStyle(false);

        if (presenter.hasAction(Action.EDIT)) {
          presenter.hideAction(Action.EDIT);
        }
        if (presenter.hasAction(Action.SAVE)) {
          presenter.showAction(Action.SAVE);
        }
        form.setEnabled(true);

      } else if (!BeeUtils.isEmpty(caption)) {
        presenter.setCaption(caption);
      }
    }

    form.updateRow(newRow, true);
    UiHelper.focus(form.asWidget());
  }

  private void prepareForInsert(IsRow row, FormView form, RowCallback callback) {
    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (int i = 0; i < getDataColumns().size(); i++) {
      BeeColumn dataColumn = getDataColumns().get(i);
      if (!BeeUtils.isEmpty(getRelColumn()) && BeeUtils.same(getRelColumn(), dataColumn.getId())) {
        if (!DataUtils.isId(getRelId())) {
          callback.onFailure(BeeUtils.joinWords(getViewName(), getRelColumn(), "invalid rel id"));
          return;
        }

        columns.add(dataColumn);
        values.add(BeeUtils.toString(getRelId()));
        continue;
      }

      String value = row.getString(i);
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (dataColumn.isEditable()) {
        columns.add(dataColumn);
        values.add(value);
      }
    }

    if (columns.isEmpty()) {
      callback.onFailure(getViewName(), "New Row", "all columns cannot be empty");
      return;
    }

    AutocompleteProvider.retainValues(form);

    ReadyForInsertEvent event = new ReadyForInsertEvent(columns, values,
        form.getChildrenForInsert(), callback, getId());

    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().onReadyForInsert(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    if (getGridInterceptor() != null) {
      getGridInterceptor().onReadyForInsert(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    fireEvent(event);
  }

  private void saveChanges(FormView form, IsRow oldRow, IsRow newRow, RowCallback callback) {
    Collection<RowChildren> children = (form == null) ? null : form.getChildrenForUpdate();
    SaveChangesEvent event = SaveChangesEvent.create(oldRow, newRow, getDataColumns(), children,
        callback);

    if (form != null && !event.isEmpty()) {
      AutocompleteProvider.retainValues(form);
    }

    if (form != null && form.getFormInterceptor() != null) {
      form.getFormInterceptor().onSaveChanges(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    if (getGridInterceptor() != null) {
      getGridInterceptor().onSaveChanges(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    if (event.isEmpty()) {
      callback.onCancel();
    } else {
      fireEvent(event);
    }
  }

  private void setActiveFormContainerId(String activeFormContainerId) {
    this.activeFormContainerId = activeFormContainerId;
  }

  private void setAdding(boolean adding) {
    this.adding = adding;
  }

  private void setEditForm(FormView editForm) {
    this.editForm = editForm;
  }

  private void setEditFormContainerId(String editFormContainerId) {
    this.editFormContainerId = editFormContainerId;
  }

  private void setEditFormName(String editFormName) {
    this.editFormName = editFormName;
  }

  private void setEditMessage(Evaluator editMessage) {
    this.editMessage = editMessage;
  }

  private void setEditMode(boolean editMode) {
    this.editMode = editMode;
  }

  private void setEditPopup(ModalForm editPopup) {
    this.editPopup = editPopup;
  }

  private void setEditSave(boolean editSave) {
    this.editSave = editSave;
  }

  private void setEditShowId(boolean editShowId) {
    this.editShowId = editShowId;
  }

  private void setNewRowCaption(String newRowCaption) {
    this.newRowCaption = newRowCaption;
  }

  private void setNewRowForm(FormView newRowForm) {
    this.newRowForm = newRowForm;
  }

  private void setNewRowFormContainerId(String newRowFormContainerId) {
    this.newRowFormContainerId = newRowFormContainerId;
  }

  private void setNewRowFormGenerated(boolean newRowFormGenerated) {
    this.newRowFormGenerated = newRowFormGenerated;
  }

  private void setNewRowFormName(String newRowFormName) {
    this.newRowFormName = newRowFormName;
  }

  private void setNewRowPopup(ModalForm newRowPopup) {
    this.newRowPopup = newRowPopup;
  }

  private void setOptions(String options) {
    this.options = options;
  }

  private void setPendingEditStartEvent(EditStartEvent pendingEditStartEvent) {
    this.pendingEditStartEvent = pendingEditStartEvent;
  }

  private void setProperties(Map<String, String> properties) {
    BeeUtils.overwrite(this.properties, properties);
  }

  private void setRowEditable(Evaluator rowEditable) {
    this.rowEditable = rowEditable;
  }

  private void setRowValidation(Evaluator rowValidation) {
    this.rowValidation = rowValidation;
  }

  private void setShowEditPopup(boolean showEditPopup) {
    this.showEditPopup = showEditPopup;
  }

  private void setShowNewRowPopup(boolean showNewRowPopup) {
    this.showNewRowPopup = showNewRowPopup;
  }

  private void setSingleForm(boolean singleForm) {
    this.singleForm = singleForm;
  }

  private void setState(State state) {
    this.state = state;
  }

  private boolean showEditPopup() {
    return showEditPopup;
  }

  private void showForm(boolean edit, boolean show) {
    String containerId = edit ? getEditFormContainerId() : getNewRowFormContainerId();

    ModalForm popup = edit ? getEditPopup() : getNewRowPopup();
    boolean modal = popup != null;

    FormView form = getForm(edit);

    State formState = show ? State.OPEN : State.CLOSED;
    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().beforeStateChange(formState, modal);
    }

    if (show) {
      if (modal) {
        if (isChild() && isNewRowFormGenerated()) {
          if (!newRowFormState.contains(State.INITIALIZED)) {
            amendGeneratedSize(popup, form);
          }
          popup.showAt(getAbsoluteLeft(), getAbsoluteTop());
        } else {
          popup.center();
        }

      } else {
        showGrid(false);
        StyleUtils.unhideDisplay(containerId);
      }

      if (edit) {
        if (!editFormState.contains(State.INITIALIZED)) {
          editFormState.add(State.INITIALIZED);
          if (isSingleFormInstance()) {
            newRowFormState.add(State.INITIALIZED);
          }

          form.start(null);
          form.observeData();
        }

      } else {
        if (!newRowFormState.contains(State.INITIALIZED)) {
          if (isSingleFormInstance()) {
            editFormState.add(State.INITIALIZED);
          }
          newRowFormState.add(State.INITIALIZED);
          form.start(null);
        }
      }

      setActiveFormContainerId(containerId);

      if (pendingResize.remove(containerId)) {
        if (modal) {
          popup.onResize();
        } else {
          UiHelper.maybeResize(this, containerId);
        }
      }

    } else {
      if (modal) {
        popup.close();
      } else {
        StyleUtils.hideDisplay(containerId);
        showGrid(true);
      }

      setActiveFormContainerId(null);
    }

    form.setState(formState);
    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().afterStateChange(formState, modal);
    }
  }

  private void showGrid(boolean show) {
    getGrid().setVisible(show);
  }

  private boolean showNewRowPopup() {
    return showNewRowPopup;
  }

  private void showNote(LogLevel level, String... messages) {
    StyleUtils.setZIndex(getNotification(), getGrid().getZIndex() + 1);
    getNotification().show(level, messages);
  }

  private void updateCell(final IsRow rowValue, final IsColumn dataColumn,
      final String oldValue, final String newValue, final boolean rowMode) {

    getGrid().preliminaryUpdate(rowValue.getId(), dataColumn.getId(), newValue);

    RowCallback callback = new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        refreshCell(rowValue.getId(), dataColumn.getId());
        notifySevere(reason);
      }

      @Override
      public void onSuccess(BeeRow result) {
        if (getGridInterceptor() != null) {
          getGridInterceptor().afterUpdateCell(dataColumn, oldValue, newValue, result, rowMode);
        }
      }
    };

    ReadyForUpdateEvent event =
        new ReadyForUpdateEvent(rowValue, dataColumn, oldValue, newValue, rowMode, callback);

    if (getGridInterceptor() != null) {
      getGridInterceptor().onReadyForUpdate(this, event);
      if (event.isConsumed()) {
        return;
      }
    }

    fireEvent(event);
  }

  private void updateEditFormMessage(GridFormPresenter presenter, IsRow row) {
    if (getEditMessage() == null && !getEditShowId()) {
      return;
    }

    String message = null;
    if (getEditMessage() != null) {
      getEditMessage().update(row);
      message = getEditMessage().evaluate();
    }
    if (getEditShowId() && row != null) {
      message = BeeUtils.joinWords(message, BeeUtils.bracket(row.getId()));
    }

    presenter.setMessage(message);
  }

  private boolean useFormForEdit(String columnId) {
    if (getEditForm() == null) {
      return false;
    }

    if (BeeUtils.isEmpty(columnId) || getEditInPlace().isEmpty()) {
      return true;
    }
    return !BeeUtils.containsSame(getEditInPlace(), columnId);
  }

  private boolean validateAndUpdate(EditableColumn editableColumn, IsRow row, String oldValue,
      String newValue, boolean tab) {
    Boolean ok = editableColumn.validate(oldValue, newValue, row, ValidationOrigin.CELL,
        EditorValidation.NEW_VALUE);
    if (!BeeUtils.isTrue(ok)) {
      return false;
    }

    updateCell(row, editableColumn.getDataColumn(), oldValue, newValue,
        editableColumn.getRowModeForUpdate());
    if (tab) {
      getGrid().handleKeyboardNavigation(KeyCodes.KEY_TAB, false);
    }
    return true;
  }
}
