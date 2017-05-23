package com.butent.bee.client.view.grid;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Place;
import com.butent.bee.client.Storage;
import com.butent.bee.client.data.ClientDefaults;
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
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.dom.Stacking;
import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.event.logical.SortEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
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
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.render.SimpleRenderer;
import com.butent.bee.client.style.ColorStyleProvider;
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
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.validation.CellValidateEvent.Handler;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.validation.EditorValidation;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.validation.ValidationOrigin;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
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
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasPercentageTag;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
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
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.ui.CellType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.ColumnRelation;
import com.butent.bee.shared.ui.FilterSupplierType;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.HandlesFormat;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.ArrayUtils;
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
import java.util.function.Consumer;

/**
 * Creates cell grid elements, connecting view and presenter elements of them.
 */

public class GridImpl extends Absolute implements GridView, EditEndEvent.Handler,
    SortEvent.Handler, SettingsChangeEvent.Handler, RenderingEvent.Handler {

  private static final class GridForm {

    private final String name;
    private String label;

    private FormView formView;
    private String containerId;
    private ModalForm popup;

    private final Set<State> states = EnumSet.noneOf(State.class);

    private GridForm(String name) {
      this.name = name;
    }

    private void addState(State state) {
      states.add(state);
    }

    private IsRow getActiveRow() {
      return (getFormView() == null) ? null : getFormView().getActiveRow();
    }

    private String getContainerId() {
      return containerId;
    }

    private FormView getFormView() {
      return formView;
    }

    private String getLabel() {
      return label;
    }

    private String getName() {
      return name;
    }

    private ModalForm getPopup() {
      return popup;
    }

    private boolean hasState(State state) {
      return states.contains(state);
    }

    private void onUnload() {
      if (getPopup() != null) {
        getPopup().unload();
      }
    }

    private boolean removeState(State state) {
      return states.remove(state);
    }

    private void setContainerId(String containerId) {
      this.containerId = containerId;
    }

    private void setFormView(FormView formView) {
      this.formView = formView;
    }

    private void setLabel(String label) {
      this.label = label;
    }

    private void setPopup(ModalForm popup) {
      this.popup = popup;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(GridImpl.class);

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "GridView";

  private static final String STYLE_PROGRESS_CONTAINER =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressContainer";
  private static final String STYLE_PROGRESS_BAR =
      BeeConst.CSS_CLASS_PREFIX + "Grid-ProgressBar";

  private static final String KEY_COLUMN = "column";

  private static Widget createProgress() {
    Flow container = new Flow(STYLE_PROGRESS_CONTAINER);
    container.add(new CustomDiv(STYLE_PROGRESS_BAR));
    return container;
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
  private Long relId;

  private final Collection<UiOption> uiOptions;
  private final int gridMarginLeft;

  private final GridInterceptor gridInterceptor;

  private GridPresenter viewPresenter;
  private CellGrid grid;

  private Evaluator rowValidation;
  private Evaluator rowEditable;

  private final Map<String, EditableColumn> editableColumns = new LinkedHashMap<>();
  private final Map<String, ColumnRelation> columnRelations = new HashMap<>();

  private final Notification notification = new Notification();

  private final List<String> newRowDefaults = new ArrayList<>();

  private String newRowCaption;
  private boolean newRowFormGenerated;

  private final List<GridForm> newRowForms = new ArrayList<>();
  private int newRowFormIndex;

  private final List<GridForm> editForms = new ArrayList<>();
  private int editFormIndex;

  private final Set<Integer> copyColumns = new HashSet<>();

  private boolean editMode;
  private boolean editSave;

  private Evaluator editMessage;
  private boolean editShowId;
  private final Set<String> editInPlace = new HashSet<>();

  private EditStartEvent pendingEditStartEvent;

  private boolean adding;
  private String activeFormContainerId;

  private Runnable onFormOpen;

  private boolean showNewRowPopup;
  private boolean showEditPopup;

  private final Set<String> pendingResize = new HashSet<>();
  private String options;

  private final Map<String, String> properties = new HashMap<>();

  private final List<String> dynamicColumnGroups = new ArrayList<>();

  private State state;

  private boolean summarize;

  private final Feed feed;

  public GridImpl(GridDescription gridDescription, String gridKey,
      List<BeeColumn> dataColumns, String relColumn,
      Collection<UiOption> uiOptions, GridInterceptor gridInterceptor, GridOptions gridOptions) {

    super();
    addStyleName(STYLE_NAME);

    this.uiOptions = uiOptions;
    this.gridMarginLeft = UiOption.isChildOrEmbedded(uiOptions)
        ? Theme.getChildGridMarginLeft() : Theme.getGridMarginLeft();

    createGrid();

    this.gridDescription = Assert.notNull(gridDescription);
    this.gridKey = gridKey;

    this.dataInfo = BeeUtils.isEmpty(gridDescription.getViewName()) ? null
        : Data.getDataInfo(gridDescription.getViewName());

    this.dataColumns = Assert.notEmpty(dataColumns);
    this.relColumn = relColumn;

    this.gridInterceptor = gridInterceptor;

    this.feed = gridOptions == null ? null : gridOptions.getFeed();
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
  public boolean addColumn(ColumnDescription cd, String dynGroup, int beforeIndex) {
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

      ColumnRelation columnRelation = cd.getColumnRelation();
      relationEditable = columnRelation != null && columnRelation.isEditEnabled()
          && Data.isViewVisible(columnRelation.getViewName(getDataInfo(), source));
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
        label = Localized.dictionary().captionId();
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
        cellSource = CellSource.forProperty(property,
            BeeKeeper.getUser().idOrNull(cd.getUserMode()), cd.getValueType());

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
          label = Localized.dictionary().selectionColumnLabel();
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

    if (relationEditable) {
      column.addClass(RowEditor.EDITABLE_RELATION_STYLE);
      column.setInstantKarma(true);
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
      NumberFormat nf;
      if (cellSource != null && cellSource.getScale() > cd.getScale()) {
        nf = Format.getDecimalFormat(cd.getScale(), cellSource.getScale());
      } else {
        nf = Format.getDecimalFormat(cd.getScale());
      }
      ((HasNumberFormat) column).setNumberFormat(nf);
    }

    if (!BeeUtils.isEmpty(cd.getHorAlign())) {
      UiHelper.setHorizontalAlignment(column, cd.getHorAlign());
    }

    VerticalAlign verticalAlign = null;
    if (!BeeUtils.isEmpty(cd.getVertAlign())) {
      verticalAlign = StyleUtils.parseVerticalAlign(cd.getVertAlign());
    }
    if (verticalAlign == null && renderer != null) {
      verticalAlign = renderer.getDefaultVerticalAlign();
    }
    if (verticalAlign == null) {
      if (cellType == CellType.HTML || cellSource != null && cellSource.isText()) {
        verticalAlign = VerticalAlign.TOP;
      } else {
        verticalAlign = VerticalAlign.MIDDLE;
      }
    }
    column.setVerticalAlign(verticalAlign);

    if (!BeeUtils.isEmpty(cd.getWhiteSpace())) {
      UiHelper.setWhiteSpace(column, cd.getWhiteSpace());
    }

    if (BeeUtils.isTrue(cd.getDraggable())) {
      column.setDraggable(true);
    }

    if (cd.getInstantKarma() != null) {
      column.setInstantKarma(cd.getInstantKarma());
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

    if (cd.getColumnRelation() != null && relationEditable) {
      columnRelations.put(columnId, cd.getColumnRelation());
    }

    AbstractFilterSupplier filterSupplier = null;
    if (gridInterceptor != null) {
      filterSupplier = gridInterceptor.getFilterSupplier(columnId, cd);
    }

    if (filterSupplier == null && !BeeConst.STRING_MINUS.equals(cd.getFilterOptions())
        && (filterSupplierType != null
        || !BeeConst.isUndef(dataIndex) || !BeeUtils.isEmpty(column.getSearchBy()))) {

      filterSupplier = FilterSupplierFactory.getSupplier(getViewName(), dataColumns,
          gridDescription.getIdName(), gridDescription.getVersionName(), columnId, label,
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
    StyleProvider styleProvider = null;

    if (gridInterceptor != null) {
      styleProvider = gridInterceptor.getColumnStyleProvider(columnId);
    }
    if (styleProvider == null && cd.getDynStyles() != null) {
      columnStyles = ConditionalStyle.create(cd.getDynStyles(), columnId, dataColumns);
    }
    if (styleProvider == null && columnStyles == null) {
      styleProvider = ConditionalStyle.getGridColumnStyleProvider(getGridName(), columnId);
    }
    if (styleProvider == null && columnStyles == null
        && BeeUtils.anyNotEmpty(cd.getBackgroundSource(), cd.getForegroundSource())) {

      styleProvider = ColorStyleProvider.create(getDataColumns(),
          cd.getBackgroundSource(), cd.getForegroundSource());
    }

    if (columnStyles == null && styleProvider != null) {
      columnStyles = ConditionalStyle.create(styleProvider);
    }
    if (columnStyles != null) {
      columnInfo.setDynStyles(columnStyles);
    }

    getGrid().addColumn(columnInfo, !BeeUtils.isFalse(cd.getVisible()), beforeIndex);

    return true;
  }

  @Override
  public HandlerRegistration addGridFormHandler(GridFormEvent.Handler handler) {
    return addHandler(handler, GridFormEvent.getType());
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
    add(createProgress());
    add(getNotification());

    setEditMode(BeeUtils.unbox(gridDescription.getEditMode()));
    setEditSave(BeeUtils.unbox(gridDescription.getEditSave()));

    initForms();

    setShowEditPopup(BeeUtils.nvl(gridDescription.getEditPopup(), hasChildUi()));
    setShowNewRowPopup(BeeUtils.nvl(gridDescription.getNewRowPopup(), hasChildUi()));

    if (!editForms.isEmpty()) {
      if (gridDescription.getEditMessage() != null) {
        setEditMessage(Evaluator.create(gridDescription.getEditMessage(), null, dataColumns));
      }
      setEditShowId(BeeUtils.unbox(gridDescription.getEditShowId()));
    }

    String viewName = gridDescription.getViewName();
    if (newRowForms.isEmpty() && !BeeUtils.isEmpty(viewName) && !isReadOnly()
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

    final FormView form = getForm(getActiveFormKind());

    if (!form.validate(notificationListener, false)) {
      return;
    }
    if (!validateFormData(form, notificationListener, false)) {
      return;
    }

    IsRow row = form.getActiveRow();

    if (DataUtils.isNewRow(row)) {
      prepareForInsert(row, form, true, new RowCallback() {
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
    int w = containerWidth;
    if (gridMarginLeft > 0) {
      w -= gridMarginLeft;
    }

    return getGrid().estimatePageSize(w, containerHeight, true);
  }

  @Override
  public void finishNewRow(IsRow row) {
    showForm(GridFormKind.NEW_ROW, false);
    fireEvent(new GridFormEvent(GridFormKind.NEW_ROW, State.CLOSED, showNewRowPopup()));

    setAdding(false);
    getGrid().setEditing(false);

    if (row == null) {
      maybeResizeGrid();
    } else {
      getGrid().insertRow(row, true);
    }
  }

  @Override
  public void formCancel(boolean focus) {
    if (isAdding()) {
      finishNewRow(null);
    } else {
      closeEditForm();
    }

    if (focus) {
      getGrid().refocus();
    }
  }

  @Override
  public void formConfirm(final Consumer<IsRow> consumer) {
    final FormView form = getForm(getActiveFormKind());
    Assert.notNull(form, "formConfirm: active form is null");

    IsRow oldRow = form.getOldRow();
    final IsRow newRow = form.getActiveRow();
    Assert.notNull(newRow, "formConfirm: active row is null");

    if (!validateFormData(form, form, true)) {
      return;
    }

    if (isAdding()) {
      if (DataUtils.isNewRow(newRow)) {
        prepareForInsert(newRow, form, false, new RowCallback() {
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

            if (consumer != null) {
              consumer.accept(result);
            }
          }
        });

      } else {
        saveChanges(form, oldRow, newRow, new RowCallback() {
          @Override
          public void onCancel() {
            finishNewRow(null);
            getGrid().refocus();

            if (consumer != null) {
              consumer.accept(newRow);
            }
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
            getGrid().refocus();

            if (getGridInterceptor() != null) {
              getGridInterceptor().afterUpdateRow(result);
            }

            if (consumer != null) {
              consumer.accept(result);
            }
          }
        });
      }

    } else {
      saveChanges(form, oldRow, newRow, new RowCallback() {
        @Override
        public void onCancel() {
          closeEditForm();
          getGrid().refocus();

          if (consumer != null) {
            consumer.accept(newRow);
          }
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

          closeEditForm();
          getGrid().refocus();

          if (getGridInterceptor() != null) {
            getGridInterceptor().afterUpdateRow(result);
          }

          if (consumer != null) {
            consumer.accept(result);
          }
        }
      });
    }
  }

  @Override
  public FormView getActiveForm() {
    if (!BeeUtils.isEmpty(getActiveFormContainerId())) {
      FormView form = getForm(getActiveFormKind());
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
  public long getActiveRowId() {
    return DataUtils.getId(getActiveRow());
  }

  @Override
  public List<BeeColumn> getDataColumns() {
    return dataColumns;
  }

  @Override
  public DataInfo getDataInfo() {
    return dataInfo;
  }

  @Override
  public List<String> getDynamicColumnGroups() {
    return dynamicColumnGroups;
  }

  @Override
  public Set<String> getEditInPlace() {
    return editInPlace;
  }

  @Override
  public FormView getForm(GridFormKind kind) {
    if (kind != null) {
      GridForm gridForm = null;

      switch (kind) {
        case EDIT:
          gridForm = getEditForm();
          break;
        case NEW_ROW:
          gridForm = getNewRowForm();
          break;
      }
      return (gridForm == null) ? null : gridForm.getFormView();
    }
    return null;
  }

  @Override
  public int getFormCount(GridFormKind kind) {
    if (kind != null) {
      switch (kind) {
        case EDIT:
          return editForms.size();
        case NEW_ROW:
          return newRowForms.size();
      }
    }
    return BeeConst.UNDEF;
  }

  @Override
  public int getFormIndex(GridFormKind kind) {
    if (kind != null) {
      switch (kind) {
        case EDIT:
          return getEditFormIndex();
        case NEW_ROW:
          return getNewRowFormIndex();
      }
    }
    return BeeConst.UNDEF;
  }

  @Override
  public List<String> getFormLabels(GridFormKind kind) {
    List<String> result = new ArrayList<>();

    if (kind != null) {
      switch (kind) {
        case EDIT:
          for (GridForm gridForm : editForms) {
            result.add(BeeUtils.notEmpty(gridForm.getLabel(), gridForm.getName()));
          }
          break;

        case NEW_ROW:
          for (GridForm gridForm : newRowForms) {
            result.add(BeeUtils.notEmpty(gridForm.getLabel(), gridForm.getName()));
          }
          break;
      }
    }
    return result;
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
        case MERGEABLE:
          ok = rowInfo.isEditable() && rowInfo.isRemovable()
              && getGrid().containsRow(rowInfo.getId());
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
  public State getState() {
    return state;
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
  public boolean hasChildUi() {
    return UiOption.isChild(uiOptions);
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
      notificationListener.notifyWarning(Localized.dictionary().rowIsReadOnly());
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
  public void onDataReceived(DataReceivedEvent event) {
    if (getGridInterceptor() != null && event != null) {
      getGridInterceptor().onDataReceived(event);
    }
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
        EditableColumn editableColumn = (source instanceof EditableColumn)
            ? (EditableColumn) source : null;
        updateCell(editableColumn, event.getRowValue(), event.getColumn(), oldValue, newValue,
            event.isRowMode());
      }

      if (event.getKeyCode() != null) {
        int keyCode;

        switch (BeeUtils.unbox(event.getKeyCode())) {
          case KeyCodes.KEY_ENTER:
          case KeyCodes.KEY_TAB:
            keyCode = event.hasModifiers() ? KeyCodes.KEY_LEFT : KeyCodes.KEY_RIGHT;
            break;

          case KeyCodes.KEY_UP:
          case KeyCodes.KEY_DOWN:
            keyCode = event.getKeyCode();
            break;

          default:
            keyCode = BeeConst.UNDEF;
        }

        if (!BeeConst.isUndef(keyCode)) {
          getGrid().handleKeyboardNavigation(keyCode, false);
        }
      }
    }
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    Assert.notNull(event);

    if (editForms.isEmpty() || getEditForm().getFormView() != null) {
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

    FormView form = getForm(getActiveFormKind());
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

      if (!event.canceled() && !event.dataChanged()
          && BeeUtils.isTrue(getGridDescription().getAutoFlex())) {

        getGrid().estimateColumnWidths(false);
        getGrid().doFlexLayout();
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

    for (GridForm gridForm : editForms) {
      String id = gridForm.getContainerId();
      if (!BeeUtils.isEmpty(id) && !BeeUtils.same(id, resized)) {
        pendingResize.add(id);
      }
    }

    for (GridForm gridForm : newRowForms) {
      String id = gridForm.getContainerId();
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
  public void onSettingsChange(SettingsChangeEvent event) {
    GridSettings.onSettingsChange(gridKey, event);
  }

  @Override
  public void onSort(SortEvent event) {
    GridSettings.saveSortOrder(gridKey, event.getOrder());
  }

  @Override
  public boolean previewCellUpdate(CellUpdateEvent event) {
    return getGridInterceptor() == null || getGridInterceptor().previewCellUpdate(event);
  }

  @Override
  public boolean previewDataChange(DataChangeEvent event) {
    if (event != null && DataUtils.isId(event.getParentId())
        && isChild() && !Objects.equals(event.getParentId(), getRelId())) {

      return false;

    } else {
      return getGridInterceptor() == null || getGridInterceptor().previewDataChange(event);
    }
  }

  @Override
  public boolean previewMultiDelete(MultiDeleteEvent event) {
    return getGridInterceptor() == null || getGridInterceptor().previewMultiDelete(event);
  }

  @Override
  public boolean previewRowDelete(RowDeleteEvent event) {
    return getGridInterceptor() == null || getGridInterceptor().previewRowDelete(event);
  }

  @Override
  public boolean previewRowInsert(RowInsertEvent event) {
    if (!event.hasView(getViewName())) {
      return false;
    }
    if (event.getRow() == null) {
      return false;
    }

    if (getGridInterceptor() != null && !getGridInterceptor().previewRowInsert(event)) {
      return false;
    }

    if (event.hasSourceId(getId())) {
      return false;
    }

    if (getGrid().containsRow(event.getRowId())) {
      return false;
    }

    if (BeeUtils.isEmpty(event.getSourceId()) && !event.isSpookyActionAtADistance()) {
      return false;
    }

    if (getGrid().getPageSize() > 0 && getGrid().getRowCount() > getGrid().getPageSize()) {
      return false;
    }

    if (getViewPresenter() == null) {
      return false;
    }

    if (isChild()) {
      if (!DataUtils.isId(getRelId())) {
        return false;
      }

      int index = DataUtils.getColumnIndex(getRelColumn(), getDataColumns());
      if (index < 0) {
        return false;
      }

      if (!Objects.equals(getRelId(), event.getRow().getLong(index))) {
        return false;
      }

      if (BeeUtils.anyNotNull(getViewPresenter().getDataProvider().getImmutableFilter(),
          getViewPresenter().getDataProvider().getUserFilter())) {
        return false;
      }

    } else if (getViewPresenter().hasFilter()) {
      return false;
    }

    getGrid().insertRow(event.getRow(), false);

    if (event.isSpookyActionAtADistance()) {
      Set<String> sources = new HashSet<>();

      for (int index = 0; index < getDataColumns().size(); index++) {
        if (!event.getRow().isNull(index)) {
          sources.add(getDataColumns().get(index).getId());
        }
      }

      if (!sources.isEmpty()) {
        getGrid().addUpdatedSources(event.getRowId(), sources);
      }
    }

    logger.info("grid", getId(), getViewName(), "insert row", event.getRowId());
    return true;
  }

  @Override
  public boolean previewRowUpdate(RowUpdateEvent event) {
    return getGridInterceptor() == null || getGridInterceptor().previewRowUpdate(event);
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
    createGrid();

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
  public void selectForm(GridFormKind kind, int index) {
    if (kind != null) {
      switch (kind) {
        case EDIT:
          if (index != getEditFormIndex() && BeeUtils.isIndex(editForms, index)) {
            setEditFormIndex(index);
            BeeKeeper.getStorage().set(getFormStorageKey(kind), index);
          }
          break;

        case NEW_ROW:
          if (index != getNewRowFormIndex() && BeeUtils.isIndex(newRowForms, index)) {
            setNewRowFormIndex(index);
            BeeKeeper.getStorage().set(getFormStorageKey(kind), index);
          }
          break;
      }
    }
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
  public void setState(State state) {
    this.state = state;
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
    GridForm gridForm = getNewRowForm();

    if (gridForm.getFormView() != null) {
      openNewRow(copy);

    } else {
      gridForm.addState(State.PENDING);
      if (copy) {
        gridForm.addState(State.COPYING);
      }

      createNewRowForm();
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

    if (ok && getGridInterceptor() != null) {
      ok = getGridInterceptor().validateRow(newRow, notificationListener);
    }

    if (ok && getRowValidation() != null) {
      ok = ValidationHelper.validateRow(newRow, getRowValidation(), notificationListener);
    }
    return ok;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (getState() == State.INITIALIZED) {
      ReadyEvent.fire(this);
    }
  }

  @Override
  protected void onUnload() {
    if (getGridInterceptor() != null) {
      getGridInterceptor().onUnload(this);
    }

    for (GridForm gridForm : newRowForms) {
      gridForm.onUnload();
    }
    for (GridForm gridForm : editForms) {
      gridForm.onUnload();
    }

    super.onUnload();
  }

  private void closeEditForm() {
    showForm(GridFormKind.EDIT, false);
    fireEvent(new GridFormEvent(GridFormKind.EDIT, State.CLOSED, showEditPopup()));

    if (feed != null) {
      getGrid().getRowData().remove(getActiveRow());
      getGrid().refresh();
    }

    maybeResizeGrid();
  }

  private void createEditForm() {
    final GridForm gridForm = getEditForm();

    if (gridForm != null && !gridForm.hasState(State.LOADING)) {
      gridForm.addState(State.LOADING);

      FormFactory.createFormView(gridForm.getName(), getViewName(), getDataColumns(), true,
          (formDescription, result) -> {
            createFormContainer(gridForm, result, GridFormKind.EDIT, null, showEditPopup());
            gridForm.setFormView(result);

            gridForm.removeState(State.LOADING);

            if (getPendingEditStartEvent() != null) {
              openEditor(getPendingEditStartEvent());
              setPendingEditStartEvent(null);
            }
          });
    }
  }

  private void createFormContainer(GridForm gridForm, final FormView formView, GridFormKind kind,
      String caption, boolean asPopup) {

    String formCaption = BeeUtils.notEmpty(caption, formView.getCaption());

    EnumSet<Action> actions = EnumSet.of(Action.PRINT, Action.CLOSE);
    if (kind != GridFormKind.EDIT) {
      actions.add(Action.SAVE);
    } else if (!isReadOnly()) {
      if (hasEditMode()) {
        actions.add(Action.EDIT);
      }
      if (hasEditSave()) {
        actions.add(Action.SAVE);
      }
    }

    final GridFormPresenter gfp = new GridFormPresenter(this, formView, formCaption, actions,
        kind == GridFormKind.EDIT, hasEditSave());

    if (formView.getFormInterceptor() != null) {
      formView.getFormInterceptor().afterCreatePresenter(gfp);
    }

    Widget container = gfp.getMainView().asWidget();

    if (asPopup) {
      ModalForm popup = new ModalForm(gfp, formView, true);

      popup.setOnSave(input -> {
        if (gfp.isActionEnabled(Action.SAVE) && formView.checkOnSave(input)) {
          gfp.handleAction(Action.SAVE);
        }
      });

      popup.setOnEscape(input -> {
        if (formView.checkOnClose(input)) {
          gfp.handleAction(Action.CLOSE);
        }
      });

      popup.addOpenHandler(event -> {
        if (getOnFormOpen() != null) {
          getOnFormOpen().run();
        }
      });

      gridForm.setPopup(popup);

    } else {
      add(container);
      container.setVisible(false);
    }

    formView.setEditing(true);
    formView.setState(State.CLOSED);

    gridForm.setContainerId(DomUtils.getId(container));
  }

  private void createGrid() {
    this.grid = new CellGrid(uiOptions);
    if (gridMarginLeft > 0) {
      StyleUtils.setLeft(grid, gridMarginLeft);
    }
  }

  private void createNewRowForm() {
    final GridForm gridForm = getNewRowForm();

    if (gridForm != null && !gridForm.hasState(State.LOADING)) {
      gridForm.addState(State.LOADING);

      FormFactory.createFormView(gridForm.getName(), getViewName(), getDataColumns(), true,
          (formDescription, result) -> {
            embraceNewRowForm(gridForm, result);
            gridForm.removeState(State.LOADING);

            boolean pending = gridForm.removeState(State.PENDING);
            boolean copy = gridForm.removeState(State.COPYING);
            if (pending) {
              openNewRow(copy);
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

      DomUtils.setDataProperty(container.getRow(r), KEY_COLUMN, columnName);

      if (editor.getWidgetType() != null) {
        WidgetDescription widgetDescription = new WidgetDescription(editor.getWidgetType(),
            editor.getId(), columnName);

        widgetDescription.updateFrom(editableColumn, getGrid().getColumnInfo(columnName));
        callback.onSuccess(widgetDescription, editor);
      }
      r++;
    }
  }

  private void embraceNewRowForm(GridForm gridForm, FormView formView) {
    if (gridForm != null && formView != null) {
      createFormContainer(gridForm, formView, GridFormKind.NEW_ROW, getNewRowCaption(),
          showNewRowPopup());
      gridForm.setFormView(formView);
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

    form.setCaption(Localized.dictionary().newRow());

    GridForm gridForm = new GridForm(formName);
    newRowForms.add(gridForm);

    embraceNewRowForm(gridForm, form);
  }

  private GridFormKind getActiveFormKind() {
    return isAdding() ? GridFormKind.NEW_ROW : GridFormKind.EDIT;
  }

  private String getActiveFormContainerId() {
    return activeFormContainerId;
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

  private GridForm getEditForm() {
    if (BeeUtils.isIndex(editForms, getEditFormIndex())) {
      return editForms.get(getEditFormIndex());
    } else {
      return null;
    }
  }

  private int getEditFormIndex() {
    return editFormIndex;
  }

  private Evaluator getEditMessage() {
    return editMessage;
  }

  private boolean getEditShowId() {
    return editShowId;
  }

  private String getFormStorageKey(GridFormKind kind) {
    String prefix = BeeUtils.notEmpty(getGridKey(), getGridName());
    String suffix = kind.name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS);

    return Storage.getUserKey(prefix, suffix + "-form");
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

  private GridForm getNewRowForm() {
    if (BeeUtils.isIndex(newRowForms, getNewRowFormIndex())) {
      return newRowForms.get(getNewRowFormIndex());
    } else {
      return null;
    }
  }

  private int getNewRowFormIndex() {
    return newRowFormIndex;
  }

  private Notification getNotification() {
    return notification;
  }

  private EditStartEvent getPendingEditStartEvent() {
    return pendingEditStartEvent;
  }

  private String getRowCaption(IsRow row) {
    if (getGridInterceptor() == null) {
      return null;
    }
    return getGridInterceptor().getRowCaption(row);
  }

  private Evaluator getRowEditable() {
    return rowEditable;
  }

  private Evaluator getRowValidation() {
    return rowValidation;
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

  private void initForms() {
    if (!newRowForms.isEmpty()) {
      newRowForms.clear();
    }
    newRowForms.addAll(parseForms(GridFormKind.NEW_ROW));

    Integer index = BeeKeeper.getStorage().getInteger(getFormStorageKey(GridFormKind.NEW_ROW));
    if (BeeUtils.isNonNegative(index) && BeeUtils.isIndex(newRowForms, index)) {
      setNewRowFormIndex(index);
    }

    if (!editForms.isEmpty()) {
      editForms.clear();
    }
    editForms.addAll(parseForms(GridFormKind.EDIT));

    index = BeeKeeper.getStorage().getInteger(getFormStorageKey(GridFormKind.EDIT));
    if (BeeUtils.isNonNegative(index) && BeeUtils.isIndex(editForms, index)) {
      setEditFormIndex(index);
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

    getGrid().setRowEditable(input -> isRowEditable(input, null));

    List<ColumnDescription> columnDescriptions = gridDescription.getColumns();
    if (gridInterceptor != null) {
      gridInterceptor.beforeCreateColumns(dataColumns, columnDescriptions);
    }

    for (ColumnDescription columnDescription : columnDescriptions) {
      if (isColumnVisible(getGridName(), columnDescription)) {
        ColumnDescription cd = (gridInterceptor == null) ? columnDescription
            : gridInterceptor.beforeCreateColumn(this, columnDescription);

        if (cd != null) {
          if (BeeUtils.isTrue(cd.getDynamic())) {
            dynamicColumnGroups.add(cd.getId());
          } else {
            addColumn(cd, null, BeeConst.UNDEF);
          }
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
    getGrid().addDataReceivedHandler(this);
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

  private boolean maybeOpenRelatedData(final String columnId, EditableColumn editableColumn,
      IsRow row, CellSource cellSource, int charCode, boolean readOnly) {

    if (row == null) {
      return false;
    }

    Relation relation = (editableColumn == null) ? null : editableColumn.getRelation();
    ColumnRelation columnRelation = (relation == null) ? columnRelations.get(columnId) : null;

    if (relation == null && columnRelation == null) {
      return false;
    }
    if (relation != null && !relation.isEditEnabled(false)) {
      return false;
    }

    boolean ok;
    if (charCode == EditStartEvent.CLICK) {
      ok = true;

    } else {
      Integer editKey;
      if (relation != null) {
        editKey = relation.getEditKey();
      } else if (columnRelation != null) {
        editKey = columnRelation.getEditKey();
      } else {
        editKey = null;
      }

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

    String defSource = (cellSource == null) ? null : cellSource.getName();

    String editViewName;
    final String editSource;

    if (relation != null) {
      editViewName = relation.getEditViewName();

      String es = relation.getEditSource();

      if (BeeUtils.isEmpty(es)) {
        editSource = editableColumn.getColumnId();
        if (BeeUtils.isEmpty(editViewName)) {
          editViewName = relation.getViewName();
        }
      } else {
        editSource = es;
        if (BeeUtils.isEmpty(editViewName)) {
          editViewName = getDataInfo().getRelation(editSource);
        }
      }

    } else if (columnRelation != null) {
      editViewName = columnRelation.getViewName(getDataInfo(), defSource);
      editSource = columnRelation.getSourceColumn(getDataInfo(), defSource);

    } else {
      editViewName = null;
      editSource = null;
    }

    if (BeeUtils.anyEmpty(editViewName, editSource)) {
      return false;
    }

    if (!Data.isViewVisible(editViewName)) {
      return false;
    }

    final int sourceIndex = getDataInfo().getColumnIndex(editSource);
    final Long sourceValue;

    if (sourceIndex == DataUtils.ID_INDEX) {
      sourceValue = row.getId();
    } else if (sourceIndex >= 0) {
      sourceValue = row.getLong(sourceIndex);
    } else {
      sourceValue = null;
    }

    if (!DataUtils.isId(sourceValue)) {
      return false;
    }

    final DataInfo editDataInfo = Data.getDataInfo(editViewName);
    if (editDataInfo == null) {
      return false;
    }

    final String editTarget;
    if (columnRelation != null) {
      editTarget = columnRelation.getEditTarget();
    } else {
      editTarget = null;
    }

    String formName;
    if (relation != null) {
      formName = relation.getEditForm();
    } else if (columnRelation != null) {
      formName = columnRelation.getEditForm();
    } else {
      formName = null;
    }

    if (BeeUtils.isEmpty(formName)) {
      formName = RowEditor.getFormName(null, editDataInfo);
    }
    if (BeeUtils.isEmpty(formName)) {
      return false;
    }

    boolean modal;
    if (UiHelper.isModal(this)) {
      modal = true;

    } else {
      Boolean editModal;
      if (relation != null) {
        editModal = relation.isEditModal();
      } else if (columnRelation != null) {
        editModal = columnRelation.isEditModal();
      } else {
        editModal = null;
      }

      modal = BeeUtils.nvl(editModal, charCode != EditStartEvent.CLICK);
    }

    RowCallback rowCallback;
    if (modal) {
      rowCallback = new RowCallback() {
        @Override
        public void onCancel() {
          getGrid().refocus();
        }

        @Override
        public void onSuccess(BeeRow result) {
          if (BeeUtils.isEmpty(editTarget) || editDataInfo.getIdColumn().equals(editTarget)) {
            if (!RelationUtils.updateRow(getDataInfo(), editSource, row, editDataInfo, result,
                false).isEmpty()) {
              getGrid().refreshCell(row.getId(), columnId);
            }

          } else if (sourceIndex >= 0) {
            int targetIndex = editDataInfo.getColumnIndex(editTarget);
            if (targetIndex >= 0) {
              Long targetValue = row.getLong(targetIndex);

              if (DataUtils.isId(targetValue) && !Objects.equals(sourceValue, targetValue)) {
                row.setValue(sourceIndex, targetValue);
                getGrid().refreshCell(row.getId(), columnId);
              }
            }
          }

          getGrid().refocus();
        }
      };

    } else {
      rowCallback = null;
    }

    Filter filter = BeeUtils.isEmpty(editTarget)
        ? Filter.compareId(sourceValue) : Filter.equals(editTarget, sourceValue);

    Opener opener = modal ? Opener.MODAL : Opener.NEW_TAB;
    RowEditor.openForm(formName, editDataInfo, filter, opener, rowCallback);

    return true;
  }

  private void maybeResizeGrid() {
    if (pendingResize.remove(getGrid().getId())) {
      getGrid().onResize();
    }
  }

  private void openEditor(final EditStartEvent event) {
    if (getGridInterceptor() != null && isEnabled()) {
      getGridInterceptor().onEditStart(event);
      if (event.isConsumed()) {
        return;
      }
    }

    final IsRow rowValue = event.getRowValue();
    String columnId = event.getColumnId();
    final EditableColumn editableColumn = getEditableColumn(columnId, false);

    if (maybeOpenRelatedData(columnId, editableColumn, rowValue, event.getCellSource(),
        event.getCharCode(), event.isReadOnly())) {
      return;
    }

    final FormView form = useFormForEdit(columnId);
    boolean editable = isEnabled() && !isReadOnly();

    if (form != null) {
      if (editable) {
        editable = isRowEditable(rowValue, null);
      }
      if (editable) {
        editable = form.isRowEditable(rowValue, false);
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

    if (form != null) {
      GridFormPresenter presenter = (GridFormPresenter) form.getViewPresenter();

      String caption = getRowCaption(rowValue);
      if (!BeeUtils.isEmpty(caption)) {
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

      form.setEnabled(enableForm);

      final ScheduledCommand focusCommand = () -> {
        if (enableForm) {
          Widget widget = null;

          if (editableColumn != null) {
            String source = editableColumn.getColumnId();
            widget = form.getWidgetBySource(source);

            if (widget == null && getDataInfo() != null) {
              String relSource = getDataInfo().getEditableRelationSource(source);
              if (!BeeUtils.isEmpty(relSource) && !BeeUtils.same(source, relSource)) {
                widget = form.getWidgetBySource(relSource);
              }
            }
          }

          if (widget == null || !UiHelper.focus(widget)) {
            form.focus();
          }

          if (event.getOnFormFocus() != null) {
            event.getOnFormFocus().accept(form);
          }
        }

        fireEvent(new GridFormEvent(GridFormKind.EDIT, State.OPEN, showEditPopup()));
      };

      setOnFormOpen(() -> form.editRow(rowValue, focusCommand));

      fireEvent(new GridFormEvent(GridFormKind.EDIT, State.PENDING, showEditPopup()));
      showForm(GridFormKind.EDIT, true);

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
    final IsRow newRow = DataUtils.createEmptyRow(getDataColumns().size());

    List<String> defCols = new ArrayList<>();
    if (!newRowDefaults.isEmpty()) {
      defCols.addAll(newRowDefaults);
    }

    boolean isCopy = copy && oldRow != null;

    if (isCopy) {
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

    if (!isCopy) {
      for (EditableColumn editableColumn : getEditableColumns().values()) {
        if (editableColumn.hasCarry()) {
          if (oldRow == null) {
            oldRow = DataUtils.createEmptyRow(getDataColumns().size());
          }

          String carry = editableColumn.getCarryValue(oldRow);
          if (!BeeUtils.isEmpty(carry)) {
            int index = editableColumn.getColIndex();
            newRow.setValue(index, carry);

            if (editableColumn.hasRelation()
                && BeeUtils.equalsTrim(carry, oldRow.getString(index))) {
              RelationUtils.setRelatedValues(getDataInfo(), editableColumn.getColumnId(),
                  newRow, oldRow);
            }
          }
        }
      }
    }

    if (getGridInterceptor() != null
        && !getGridInterceptor().onStartNewRow(this, oldRow, newRow, isCopy)) {
      return;
    }

    getGrid().setEditing(true);
    setAdding(true);

    String caption = getRowCaption(newRow);

    final FormView form = getForm(GridFormKind.NEW_ROW);

    if (isNewRowFormGenerated()) {
      List<String> columnIds = getGrid().getColumnIds();
      List<Element> formElements =
          Selectors.getElementsWithDataProperty(form.getElement(), KEY_COLUMN);

      if (!BeeUtils.isEmpty(columnIds) && !BeeUtils.isEmpty(formElements)) {
        formElements.forEach(fe -> {
          if (BeeUtils.containsSame(columnIds, DomUtils.getDataProperty(fe, KEY_COLUMN))) {
            StyleUtils.unhideDisplay(fe);
          } else {
            StyleUtils.hideDisplay(fe);
          }
        });
      }
    }

    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().onStartNewRow(form, oldRow, newRow);
    }

    if (form.getViewPresenter() instanceof GridFormPresenter) {
      GridFormPresenter presenter = (GridFormPresenter) form.getViewPresenter();
      if (!BeeUtils.isEmpty(caption)) {
        presenter.setCaption(caption);
      }
    }

    setOnFormOpen(() -> {
      form.updateRow(newRow, true);
      form.focus();

      fireEvent(new GridFormEvent(GridFormKind.NEW_ROW, State.OPEN, showNewRowPopup()));
    });

    fireEvent(new GridFormEvent(GridFormKind.NEW_ROW, State.PENDING, showNewRowPopup()));
    showForm(GridFormKind.NEW_ROW, true);
  }

  private List<GridForm> parseForms(GridFormKind kind) {
    List<GridForm> result = new ArrayList<>();

    String input = (kind == GridFormKind.EDIT)
        ? gridDescription.getEditForm() : gridDescription.getNewRowForm();

    if (BeeUtils.isEmpty(input) && getDataInfo() != null) {
      input = (kind == GridFormKind.EDIT)
          ? getDataInfo().getEditForm() : getDataInfo().getNewRowForm();
    }

    String[] items = BeeUtils.split(input, GridDescription.FORM_ITEM_SEPARATOR);
    if (!ArrayUtils.isEmpty(items)) {
      for (String item : items) {
        String[] arr = BeeUtils.split(item, BeeConst.CHAR_COLON);

        String name = normalizeFormName(ArrayUtils.getQuietly(arr, 0));
        if (!BeeUtils.isEmpty(name)) {
          GridForm gridForm = new GridForm(name);

          String label = Localized.maybeTranslate(ArrayUtils.getQuietly(arr, 1));
          if (!BeeUtils.isEmpty(label)) {
            gridForm.setLabel(label);
          }

          result.add(gridForm);
        }
      }
    }

    return result;
  }

  private void prepareForInsert(IsRow row, FormView form, boolean forced, RowCallback callback) {
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

      } else {
        String value = row.getString(i);

        if (dataColumn.isInsertable(value)) {
          columns.add(dataColumn);
          values.add(value);
        }
      }
    }

    if (columns.isEmpty()) {
      callback.onFailure(getViewName(), Localized.dictionary().newRow(),
          Localized.dictionary().allValuesCannotBeEmpty());
      return;
    }

    AutocompleteProvider.retainValues(form);

    ReadyForInsertEvent event = new ReadyForInsertEvent(columns, values,
        form.getChildrenForInsert(), callback, getId());
    event.setForced(forced);

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

  private void setEditFormIndex(int editFormIndex) {
    this.editFormIndex = editFormIndex;
  }

  private void setEditMessage(Evaluator editMessage) {
    this.editMessage = editMessage;
  }

  private void setEditMode(boolean editMode) {
    this.editMode = editMode;
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

  private void setNewRowFormGenerated(boolean newRowFormGenerated) {
    this.newRowFormGenerated = newRowFormGenerated;
  }

  private void setNewRowFormIndex(int newRowFormIndex) {
    this.newRowFormIndex = newRowFormIndex;
  }

  private Runnable getOnFormOpen() {
    return onFormOpen;
  }

  private void setOnFormOpen(Runnable onFormOpen) {
    this.onFormOpen = onFormOpen;
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

  private boolean showEditPopup() {
    return showEditPopup;
  }

  private void showForm(GridFormKind kind, boolean show) {
    GridForm gridForm = (kind == GridFormKind.EDIT) ? getEditForm() : getNewRowForm();
    String containerId = gridForm.getContainerId();

    ModalForm popup = gridForm.getPopup();
    boolean modal = popup != null;

    FormView form = gridForm.getFormView();

    State formState = show ? State.OPEN : State.CLOSED;
    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().beforeStateChange(formState, modal);
    }

    if (show) {
      if (modal) {
        if (kind == GridFormKind.NEW_ROW && isNewRowFormGenerated()
            && !gridForm.hasState(State.INITIALIZED)) {

          Widget w = form.getRootWidget().asWidget();
          while (w != null && !DomUtils.sameId(w, popup)) {
            StyleUtils.makeRelative(w);
            StyleUtils.setTop(w, 0);

            w = w.getParent();
          }

          if (form.getViewPresenter() != null) {
            HeaderView hv = form.getViewPresenter().getHeader();
            if (hv != null) {
              StyleUtils.makeRelative(hv.asWidget());
            }
          }

          StyleUtils.clearWidth(popup);
          StyleUtils.clearHeight(popup);
        }

        if (kind == GridFormKind.NEW_ROW && hasChildUi() && isNewRowFormGenerated()) {
          int x = getAbsoluteLeft();
          int y = getAbsoluteTop();

          popup.showAt(x, y);

        } else {
          popup.center();
        }

      } else {
        showGrid(false);
        StyleUtils.unhideDisplay(containerId);
      }

      if (!gridForm.hasState(State.INITIALIZED)) {
        gridForm.addState(State.INITIALIZED);
        form.start(null);

        if (kind == GridFormKind.EDIT) {
          form.observeData();
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
      ViewHelper.getGrids(form.asWidget()).stream()
          .filter(childGrid -> childGrid.getActiveForm() != null)
          .forEach(childGrid -> childGrid.formCancel(false));

      if (modal) {
        popup.close();

      } else {
        StyleUtils.hideDisplay(containerId);
        showGrid(true);
      }

      setActiveFormContainerId(null);
    }

    form.setAdding(kind == GridFormKind.NEW_ROW && show);

    form.setState(formState);
    if (form.getFormInterceptor() != null) {
      form.getFormInterceptor().afterStateChange(formState, modal);
    }

    if (show && !modal && getOnFormOpen() != null) {
      getOnFormOpen().run();
    }
  }

  private void showGrid(boolean show) {
    getGrid().setVisible(show);
  }

  private boolean showNewRowPopup() {
    return showNewRowPopup;
  }

  private void showNote(LogLevel level, String... messages) {
    Stacking.ensureParentContext(getNotification());
    StyleUtils.setZIndex(getNotification(), getGrid().getZIndex() + 1);

    getNotification().show(level, messages);
  }

  private void updateCell(EditableColumn editableColumn, final IsRow rowValue,
      final IsColumn dataColumn, final String oldValue, final String newValue,
      final boolean rowMode) {

    String currencySource = (editableColumn == null) ? null : editableColumn.getCurrencySource();
    int currencyIndex = BeeUtils.isEmpty(currencySource)
        ? BeeConst.UNDEF : getDataIndex(currencySource);

    if (!BeeConst.isUndef(currencyIndex)) {
      Long oldCurrency = rowValue.getLong(currencyIndex);
      Long newCurrency;

      if (newValue == null) {
        newCurrency = null;
      } else if (!DataUtils.isId(oldCurrency) && DataUtils.isId(ClientDefaults.getCurrency())) {
        newCurrency = ClientDefaults.getCurrency();
      } else {
        newCurrency = oldCurrency;
      }

      if (!Objects.equals(oldCurrency, newCurrency)) {
        String v = DataUtils.isId(newCurrency) ? BeeUtils.toString(newCurrency) : null;
        rowValue.preliminaryUpdate(currencyIndex, v);
      }
    }

    String percentageTag = (editableColumn == null) ? null : editableColumn.getPercentageTag();
    int percentageTagIndex = BeeUtils.isEmpty(percentageTag)
        ? BeeConst.UNDEF : getDataIndex(percentageTag);

    if (!BeeConst.isUndef(percentageTagIndex)) {
      boolean oldPercentageTag = BeeUtils.isTrue(rowValue.getBoolean(percentageTagIndex));
      boolean newPercentageTag = HasPercentageTag.isPercentage(BeeUtils.toDoubleOrNull(newValue));

      if (oldPercentageTag != newPercentageTag) {
        String v = newPercentageTag ? BooleanValue.pack(newPercentageTag) : null;
        rowValue.preliminaryUpdate(percentageTagIndex, v);
      }
    }

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
    getGrid().preliminaryUpdate(rowValue.getId(), dataColumn.getId(), event.getNewValue());

    fireEvent(event);
  }

  private void updateEditFormMessage(GridFormPresenter presenter, IsRow row) {
    if (getEditShowId()) {
      presenter.getHeader().showRowId(row);
    }

    if (getEditMessage() != null) {
      presenter.getHeader().showRowMessage(getEditMessage(), row);
    }
  }

  private FormView useFormForEdit(String columnId) {
    if (!BeeUtils.isEmpty(columnId) && BeeUtils.containsSame(getEditInPlace(), columnId)) {
      return null;
    }

    GridForm gridForm = getEditForm();
    return (gridForm == null) ? null : gridForm.getFormView();
  }

  private boolean validateAndUpdate(EditableColumn editableColumn, IsRow row, String oldValue,
      String newValue, boolean tab) {

    Boolean ok = editableColumn.validate(oldValue, newValue, row, ValidationOrigin.CELL,
        EditorValidation.NEW_VALUE);
    if (!BeeUtils.isTrue(ok)) {
      return false;
    }

    updateCell(editableColumn, row, editableColumn.getDataColumn(), oldValue, newValue,
        editableColumn.getRowModeForUpdate());
    if (tab) {
      getGrid().handleKeyboardNavigation(KeyCodes.KEY_TAB, false);
    }
    return true;
  }
}
