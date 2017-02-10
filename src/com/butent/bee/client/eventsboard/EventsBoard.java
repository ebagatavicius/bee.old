package com.butent.bee.client.eventsboard;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.Position;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class EventsBoard extends Flow implements Presenter, RowInsertEvent.Handler,
    HandlesUpdateEvents, DataChangeEvent.Handler {

  public static class EventFilesFilter {
    String filesViewName;
    String eventColName;
    String fileColName;
    String fileNameColName;
    String fileSizeColName;
    String fileTypeColName;
    String fileCaptionColName;

    public EventFilesFilter(String filesViewName, String eventColName,
        String fileColName) {
      this(filesViewName, eventColName, fileColName, null, null, null, null);
    }

    public EventFilesFilter(String filesViewName, String eventColName,
        String fileColName,
        String fileNameColName, String fileSizeColName, String fileTypeColName,
        String fileCaptionColName) {
      Assert.notEmpty(filesViewName);
      Assert.notEmpty(eventColName);
      Assert.notEmpty(fileColName);

      this.filesViewName = filesViewName;
      this.eventColName = eventColName;
      this.fileColName = fileColName;
      this.fileNameColName = fileNameColName;
      this.fileSizeColName = fileSizeColName;
      this.fileTypeColName = fileTypeColName;
      this.fileCaptionColName = fileCaptionColName;
    }

    public List<String> getColumnsList() {
      List<String> cols = Lists.newArrayList(eventColName, fileColName);

      if (!BeeUtils.isEmpty(fileNameColName)) {
        cols.add(fileNameColName);
      }

      if (!BeeUtils.isEmpty(fileSizeColName)) {
        cols.add(fileSizeColName);
      }

      if (!BeeUtils.isEmpty(fileTypeColName)) {
        cols.add(fileTypeColName);
      }

      if (!BeeUtils.isEmpty(fileCaptionColName)) {
        cols.add(fileCaptionColName);
      }

      return cols;
    }
  }

  protected static final String CELL_EVENT_TYPE = "EventType";
  protected static final String CELL_EVENT_PUBLISH = "EventPublished";
  protected static final String CELL_EVENT_PUBLISHER = "EventPublisher";
  protected static final String CELL_EVENT_NOTE = "EventNote";

  private static final Set<Action> HIDDEN_ACTIONS = Sets.newHashSet(Action.DELETE);

  private static final String STYLE_HEADER = "header";

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "EventsBoard-";
  private static final String STYLE_CONTENT = "content";
  private static final String STYLE_CONTENT_ROW = STYLE_CONTENT + "-row";
  private static final String STYLE_CONTENT_COL = STYLE_CONTENT + "-col";
  private static final String STYLE_CONTENT_PHOTO = STYLE_CONTENT + "-Photo";
  private static final String STYLE_HAS_CHILD_LIMIT = "-HasChildLimit";

  private static final String CONTENT_COL_PHOTO = "PublisherPhoto";
  private static final String CONTENT_COL_PUBLISHER_INFO = "PublisherInfo";
  private static final String CONTENT_COL_EVENT_NOTE = "EventNote";
  private static final String CONTENT_COL_EVENT_FILES = "EventFiles";
  private static final String CONTENT_COL_PUBLISHER_CONTENT = "EventContent";

  private static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";

  private static final int MAX_PADDING_LEFT = 5;

  private final BeeLogger logger = LogUtils.getLogger(EventsBoard.class);
  private final Collection<HandlerRegistration> registry = Lists.newArrayList();

  private HeaderView headerView;
  private long relatedId = BeeConst.LONG_UNDEF;
  private Flow content = new Flow(STYLE_PREFIX + STYLE_CONTENT);
  private List<FileInfo> files;
  private BeeRowSet oldData;
  private EventFilesFilter fileFilter;

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh(true);
        break;
      case ADD:
        add();
        break;
      default:
        break;
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (event.hasView(getEventsDataViewName())) {
      refresh(false);
    }
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public View getMainView() {
    return null;
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (event.hasView(getEventsDataViewName())) {
      refresh(false);
      return;
    }

    if (fileFilter == null) {
      return;
    }

    if (event.hasView(fileFilter.filesViewName)) {
      refresh(true);
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (event.hasView(getEventsDataViewName())) {
      refresh(false);
      return;
    }

    if (fileFilter == null) {
      return;
    }

    if (event.hasView(fileFilter.filesViewName)) {
      refresh(true);
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (event.hasView(getEventsDataViewName())) {
      refresh(true);
    }
  }

  @Override
  protected void onUnload() {
    logger.debug("Unloading registry");
    EventUtils.clearRegistry(registry);
    super.onUnload();
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  public void add() {
    DataInfo data = Data.getDataInfo(getEventsDataViewName());

    BeeRow row = RowFactory.createEmptyRow(data, true);
    RowFactory.createRow(data.getNewRowForm(), data.getCaption(), data, row, null, null,
        getNewEventFormInterceptor(), null, null);
  }

  public void create(HasWidgets widget, long relId) {
    create(widget, relId, null);
  }

  public void create(HasWidgets widget, long relId, EventFilesFilter filesFilter) {
    Assert.notNull(widget);
    this.relatedId = relId;
    this.fileFilter = filesFilter;
    clear();
    createHeaderView();
    createContent();
    refresh(true);
    loadHandlerRegistry();
    widget.add(this);
  }

  protected Widget createCellWidgetItem(String name, Widget item) {
    Assert.notNull(name);
    FlowPanel widget = new FlowPanel();
    widget.addStyleName(STYLE_PREFIX + STYLE_CONTENT + BeeConst.STRING_MINUS + name);
    if (!BeeUtils.isEmpty(getStylePrefix())) {
      widget.addStyleName(getStylePrefix() + STYLE_CONTENT + BeeConst.STRING_MINUS + name);
    }

    if (item != null) {
      widget.add(item);
    }

    return widget;
  }

  public long getRelatedId() {
    return relatedId;
  }

  public EventFilesFilter getFilesFilter() {
    return fileFilter;
  }

  public Set<Action> getDisabledActions() {
    return Action.NO_ACTIONS;
  }

  public void refresh(boolean cleanCache) {
    if (content == null) {
      return;
    }

    getData(content, cleanCache);
  }

  protected void setAdditionalStyleToWidget(String name, BeeRowSet rs, BeeRow row, Widget widget) {
  }

  public void setEventFilesFilterData(EventFilesFilter filter) {
    this.fileFilter = filter;
  }

  protected abstract IdentifiableWidget getAddEventActionWidget();

  protected abstract String getAddEventFromName();

  protected abstract Set<Action> getEnabledActions();

  protected abstract Order getEventsDataOrder();

  protected abstract String getEventsDataViewName();

  protected abstract String getEventNoteColumnName();

  protected abstract String getEventTypeColumnName();

  protected abstract AbstractFormInterceptor getNewEventFormInterceptor();

  protected abstract String getPublisherPhotoColumnName();

  protected abstract String getPublisherFirstNameColumnName();

  protected abstract String getPublisherLastNameColumnName();

  protected abstract String getPublishTimeColumnName();

  protected abstract String getRelatedColumnName();

  @SuppressWarnings("unused")
  protected void afterCreateCellContent(BeeRowSet rs, BeeRow row, Flow cell) {
  }

  @SuppressWarnings("unused")
  protected void afterCreateEventRow(BeeRowSet rs, BeeRow row, Flow eventRow) {
  }

  @SuppressWarnings("unused")
  protected void afterCreateEventRows() {
  }

  @SuppressWarnings("unused")
  protected void afterCreateEventCell(Flow eventRow, Flow eventCell, String name) {
  }

  @SuppressWarnings("unused")
  protected void afterCreateEventNoteCell(BeeRowSet rs, BeeRow row, Flow widget, Flow cell) {
  }

  @SuppressWarnings("unused")
  protected void afterCreateEventFilesCell(BeeRowSet rs, BeeRow row, Flow widget, Simple cell) {
  }

  @SuppressWarnings("unused")
  protected void beforeCreateEventNoteCell(BeeRowSet rs, BeeRow row, Flow widget) {
  }

  @SuppressWarnings("unused")
  protected void beforeCreateEventFilesCell(BeeRowSet rs, BeeRow row, Flow widget) {
  }

  @SuppressWarnings("unused")
  protected void beforeCreateEventRow(BeeRowSet rs, BeeRow row, HasWidgets widget, int rowLevel) {
  }

  @SuppressWarnings("unused")
  protected void beforeCreateEventCell(Flow eventRow, String name) {
  }

  protected Flow createEventRowCell(Flow eventRow, String name, String styleName) {
    return createEventRowCell(eventRow, name, styleName, true);
  }

  protected Flow createEventRowCell(Flow eventRow, String name, String styleName,
      boolean addContentStyle) {
    beforeCreateEventCell(eventRow, name);

    Flow eventCell = new Flow();
    if (addContentStyle) {
      eventCell.addStyleName(STYLE_PREFIX + STYLE_CONTENT_COL);
    }

    if (!BeeUtils.isEmpty(name)) {
      eventCell.addStyleName(STYLE_PREFIX + STYLE_CONTENT_COL + BeeConst.STRING_MINUS + name);
    }
    if (!BeeUtils.isEmpty(styleName)) {
      eventCell.addStyleName(styleName);
    }

    if (!BeeUtils.isEmpty(getStylePrefix()) && !BeeUtils.isEmpty(name)) {
      eventCell.addStyleName(getStylePrefix() + name);
    }

    eventRow.add(eventCell);
    return eventCell;
  }

  protected Widget createCellHtmlItem(String name, String html) {
    Assert.notNull(name);
    Widget widget = new CustomDiv(STYLE_PREFIX + STYLE_CONTENT + BeeConst.STRING_MINUS + name);

    if (!BeeUtils.isEmpty(getStylePrefix())) {
      widget.addStyleName(getStylePrefix() + STYLE_CONTENT + BeeConst.STRING_MINUS + name);
    }

    if (!BeeUtils.isEmpty(html)) {
      widget.getElement().setInnerHTML(html);
    }

    return widget;
  }

  protected Widget createCellHtmlItem(String name, String html, BeeRowSet rs, BeeRow row) {
    Widget cellWidget = createCellHtmlItem(name, html);
    setAdditionalStyleToWidget(name, rs, row, cellWidget);
    return cellWidget;
  }

  protected int getMaxChildRowLevel() {
    return MAX_PADDING_LEFT;
  }

  protected BeeRowSet getOldData() {
    return oldData;
  }

  protected String getStylePrefix() {
    return null;
  }

  protected String getParentEventColumnName() {
    return null;
  }

  protected void setOldData(BeeRowSet oldData) {
    this.oldData = oldData;
  }

  protected void setFiles(List<FileInfo> files) {
    this.files = files;
  }

  private void createChildEventRows(BeeRowSet rs, Multimap<Long, Long> data,
      long parent, HasWidgets widget, int rowLevel) {

    if (data.containsKey(parent)) {
      for (long id : data.get(parent)) {
        BeeRow row = rs.getRowById(id);
        createEventRow(rs, row, widget, rowLevel);
        createChildEventRows(rs, data, parent + 1, widget, rowLevel);
      }
    }
  }

  private void createContent() {
    if (!BeeUtils.isEmpty(getStylePrefix())) {
      content.addStyleName(getStylePrefix() + STYLE_CONTENT);
    }

    StyleUtils.setProperty(content, StyleUtils.STYLE_POSITION, Position.ABSOLUTE);

    if (headerView != null) {
      StyleUtils.setTop(content, headerView.getHeight());
    }

    add(content);
  }

  private void createEventRow(BeeRowSet rs, BeeRow row, HasWidgets widget, int rowLevel) {
    beforeCreateEventRow(rs, row, widget, rowLevel);

    Flow contentRow = new Flow();
    contentRow.addStyleName(STYLE_PREFIX + STYLE_CONTENT_ROW);

    if (!BeeUtils.isEmpty(getStylePrefix())) {
      contentRow.addStyleName(getStylePrefix() + STYLE_CONTENT_ROW);
    }

    if (rowLevel <= getMaxChildRowLevel()) {
      contentRow.getElement().getStyle().setPaddingLeft(rowLevel, Unit.EM);
    } else {
      contentRow.getElement().getStyle().setPaddingLeft(getMaxChildRowLevel(), Unit.EM);
      contentRow.addStyleName(STYLE_PREFIX + STYLE_CONTENT_ROW + STYLE_HAS_CHILD_LIMIT);

      if (!BeeUtils.isEmpty(getStylePrefix())) {
        contentRow.addStyleName(getStylePrefix() + STYLE_CONTENT_ROW + STYLE_HAS_CHILD_LIMIT);
      }
    }

    if (!BeeUtils.isEmpty(getPublisherPhotoColumnName())) {
      createPhotoCell(rs, row, contentRow);
    }

    createCellContent(rs, row, contentRow);


    afterCreateEventRow(rs, row, contentRow);

    widget.add(contentRow);
  }

  private void createCellContent(BeeRowSet rs, BeeRow row, Flow contentRow) {
    Flow cell = createEventRowCell(contentRow, CONTENT_COL_PUBLISHER_CONTENT, null);
    cell.add(createPublisherInfoCell(rs, row, contentRow));

    cell.add(createEventNoteCell(rs, row, contentRow));

    afterCreateCellContent(rs, row, cell);
  }

  private void createEventFilesCell(BeeRowSet rs, BeeRow row, Flow widget) {
    List<FileInfo> fileList = filterEventFiles(row.getId());

    if (fileList.isEmpty()) {
      return;
    }

    beforeCreateEventFilesCell(rs, row, widget);
    Simple fileContainer = new Simple();
    fileContainer.addStyleName(STYLE_PREFIX + STYLE_CONTENT_COL + BeeConst.STRING_MINUS
        + CONTENT_COL_EVENT_FILES);

    if (!BeeUtils.isEmpty(getStylePrefix())) {
      fileContainer.addStyleName(getStylePrefix() + STYLE_CONTENT_COL + BeeConst.STRING_MINUS
          + CONTENT_COL_EVENT_FILES);
    }

    FileGroup fileGroup = new FileGroup();

    for (FileInfo file : fileList) {
      if (file.getRelatedId() != null) {
        file.setIcon(FileNameUtils.getExtension(file.getName()) + ".png");
      }
    }

    fileGroup.addFiles(fileList);

    fileContainer.setWidget(fileGroup);
    afterCreateEventFilesCell(rs, row, widget, fileContainer);

    widget.add(fileContainer);
  }

  private Flow createEventNoteCell(BeeRowSet rs, BeeRow row, Flow widget) {
    beforeCreateEventNoteCell(rs, row, widget);
    Flow cell = createEventRowCell(widget, CONTENT_COL_EVENT_NOTE, null, false);

    int idxNote = rs.getColumnIndex(getEventNoteColumnName());

    if (BeeUtils.isNegative(idxNote)) {
      logger.warning("column ", getEventNoteColumnName(), "not found in view ",
          getEventsDataViewName());
      return null;
    }

    if (!BeeUtils.isEmpty(row.getString(idxNote))) {
      cell.add(createCellHtmlItem(CELL_EVENT_NOTE, row.getString(idxNote), rs, row));
    }

    if (files != null) {
      createEventFilesCell(rs, row, cell);
    }

    afterCreateEventNoteCell(rs, row, widget, cell);
    return cell;
  }

  public void createHeaderView() {
    headerView = new HeaderImpl();
    headerView.setViewPresenter(this);
    IdentifiableWidget add = null;

    if (getEnabledActions().contains(Action.ADD) && getAddEventActionWidget() != null) {
      add = getAddEventActionWidget();
      if (add instanceof HasClickHandlers) {
        HIDDEN_ACTIONS.add(Action.ADD);

        ((HasClickHandlers) add).addClickHandler(new ClickHandler() {

          @Override
          public void onClick(ClickEvent arg0) {
            handleAction(Action.ADD);
          }
        });
      }
    }

    headerView.create(getCaption(), true, false, getEventsDataViewName(), null,
        getEnabledActions(), getDisabledActions(), HIDDEN_ACTIONS);

    if (!BeeUtils.isEmpty(getStylePrefix())) {
      headerView.addStyleName(getStylePrefix() + STYLE_HEADER);
    }

    headerView.clearCommandPanel();

    if (add != null) {
      headerView.addCommandItem(add);
    }
    add(headerView);
  }

  private void createPhotoCell(BeeRowSet rs, BeeRow row, Flow widget) {
    Flow cell = createEventRowCell(widget, CONTENT_COL_PHOTO, null);

    int idxPhoto = rs.getColumnIndex(getPublisherPhotoColumnName());

    if (BeeUtils.isNegative(idxPhoto)) {
      logger.warning("column ", getPublisherPhotoColumnName(), "not found in view ",
          getEventsDataViewName());
      return;
    }

    Long photo = row.getLong(idxPhoto);

    String photoUrl;

    if (!DataUtils.isId(photo)) {
      photoUrl = DEFAULT_PHOTO_IMAGE;
    } else {
      photoUrl = PhotoRenderer.getUrl(photo);
    }

    Image image = new Image(photoUrl);
    image.addStyleName(STYLE_PREFIX + STYLE_CONTENT_PHOTO);

    if (BeeUtils.isEmpty(getStylePrefix())) {
      image.addStyleName(getStylePrefix() + STYLE_CONTENT_PHOTO);
    }

    cell.add(image);
  }

  private Flow createPublisherInfoCell(BeeRowSet rs, BeeRow row, Flow widget) {
    Flow cell = new Flow();
    cell.addStyleName(STYLE_PREFIX + STYLE_CONTENT_COL + BeeConst.STRING_MINUS
        + CONTENT_COL_PUBLISHER_INFO);

    if (!BeeUtils.isEmpty(getPublisherFirstNameColumnName())
        || !BeeUtils.isEmpty(getPublisherFirstNameColumnName())) {

      int idxFirst = rs.getColumnIndex(getPublisherFirstNameColumnName());
      int idxLast = rs.getColumnIndex(getPublisherLastNameColumnName());
      String fullName = BeeConst.STRING_EMPTY;

      if (!BeeUtils.isNegative(idxFirst)) {
        fullName = row.getString(idxFirst);
      }

      if (!BeeUtils.isNegative(idxLast)) {
        fullName = BeeUtils.joinWords(fullName, row.getString(idxLast));
      }

      cell.add(createCellHtmlItem(CELL_EVENT_PUBLISHER, fullName, rs, row));
    }

    if (!BeeUtils.isEmpty(getPublishTimeColumnName())) {
      int idxCol = rs.getColumnIndex(getPublishTimeColumnName());

      if (!BeeUtils.isNegative(idxCol)) {
        DateTime publishTime = row.getDateTime(idxCol);
        if (publishTime != null) {
          cell.add(createCellHtmlItem(CELL_EVENT_PUBLISH, Format.renderDateTime(publishTime), rs,
              row));
        }

      } else {
        logger.warning("column", getPublishTimeColumnName(), "not found in view",
            getEventsDataViewName());
      }
    }

    if (!BeeUtils.isEmpty(getEventTypeColumnName())) {
      int idxEvent = rs.getColumnIndex(getEventTypeColumnName());

      if (!BeeUtils.isNegative(idxEvent)) {
        BeeColumn column = rs.getColumn(idxEvent);
        String text;
        if (column.getType().equals(ValueType.TEXT)) {
          text = row.getString(idxEvent);
        } else {
          text = EnumUtils.getCaption(column.getEnumKey(), row.getInteger(idxEvent));
        }

        cell.add(createCellHtmlItem(CELL_EVENT_TYPE, text, rs, row));

      } else {
        logger.warning("column", getEventTypeColumnName(), "not found in view",
            getEventsDataViewName());
      }
    }
    return cell;
  }

  private RowSetCallback getDataCallback(final HasWidgets cont, final boolean clearCache) {
    return new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        logger.debug("parse event data from", getEventsDataViewName());
        if (!clearCache && getOldData() != null && result != null) {
          if (getOldData().getNumberOfRows() == result.getNumberOfRows()
              && getOldData().getRow(getOldData().getNumberOfRows() - 1).getId() == result.getRow(
                  result.getNumberOfRows() - 1).getId()) {
            // TODO: create some methods validate that data is same;
            return;
          }
        }
        cont.clear();
        setOldData(result);

        if (result.isEmpty()) {
          return;
        }

        EventFilesFilter flt = getFilesFilter();

        if (flt == null) {
          prepareCascadedStructure(cont, result);
          return;
        }

        List<String> fileCols = flt.getColumnsList();
        Filter filter = Filter.and(Filter.notNull(flt.eventColName),
            Filter.any(flt.eventColName, result.getRowIds()));

        Queries
            .getRowSet(flt.filesViewName, fileCols, filter, getFilesRowSetCallback(cont, result));
      }
    };
  }

  private void getData(final HasWidgets cont, final boolean clearCache) {
    Assert.isTrue(DataUtils.isId(getRelatedId()));
    Assert.notEmpty(getRelatedColumnName());
    Assert.notEmpty(getEventsDataViewName());

    Filter filter = Filter.equals(getRelatedColumnName(), getRelatedId());

    DataInfo info = Data.getDataInfo(getEventsDataViewName());

    Queries.getRowSet(getEventsDataViewName(), info.getColumnNames(false), filter,
        getEventsDataOrder(), getDataCallback(cont, clearCache));
  }

  private List<FileInfo> filterEventFiles(long eventId) {
    if (files.isEmpty()) {
      return files;
    }

    List<FileInfo> result = Lists.newArrayList();

    for (FileInfo file : files) {
      Long id = file.getRelatedId();

      if (id != null && id == eventId) {
        result.add(file);
      }
    }

    return result;
  }

  private RowSetCallback getFilesRowSetCallback(final HasWidgets eventPanel,
      final BeeRowSet events) {
    return new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        EventFilesFilter filter = getFilesFilter();

        if (filter == null) {
          logger.warning("file filter was missed");
          prepareCascadedStructure(eventPanel, events);
          return;
        }

        List<FileInfo> filesResult = Lists.newArrayList();

        int idxFileId = result.getColumnIndex(filter.fileColName);
        int idxEventId = result.getColumnIndex(filter.eventColName);

        if (BeeUtils.isNegative(idxFileId) || BeeUtils.isNegative(idxEventId)) {
          logger.warning("cannot access in column", filter.fileColName, filter.eventColName,
              " in view", filter.filesViewName);
          prepareCascadedStructure(eventPanel, events);
          return;
        }

        int idxFileName =
            BeeUtils.isEmpty(filter.fileNameColName) ? BeeConst.UNDEF : result
                .getColumnIndex(filter.fileNameColName);

        int idxFileSize =
            BeeUtils.isEmpty(filter.fileNameColName) ? BeeConst.UNDEF : result
                .getColumnIndex(filter.fileSizeColName);
        int idxFileType =
            BeeUtils.isEmpty(filter.fileTypeColName) ? BeeConst.UNDEF : result
                .getColumnIndex(filter.fileTypeColName);
        int idxFileCaption =
            BeeUtils.isEmpty(filter.fileCaptionColName) ? BeeConst.UNDEF : result
                .getColumnIndex(filter.fileCaptionColName);

        for (BeeRow fileRow : result) {
          Long fileId = fileRow.getLong(idxFileId);

          String fileName = null;
          Long fileSize = null;
          String fileType = null;
          String fileCaption = null;

          if (!BeeUtils.isNegative(idxFileName)) {
            fileName = fileRow.getString(idxFileName);
          }

          if (!BeeUtils.isNegative(idxFileSize)) {
            fileSize = fileRow.getLong(idxFileSize);
          }

          if (!BeeUtils.isNegative(idxFileType)) {
            fileType = fileRow.getString(idxFileType);
          }

          if (!BeeUtils.isNegative(idxFileCaption)) {
            fileCaption = fileRow.getString(idxFileCaption);
          }

          FileInfo fi = new FileInfo(fileId, fileName, fileSize, fileType);

          fi.setRelatedId(fileRow.getLong(idxEventId));

          if (!BeeUtils.isEmpty(fileCaption)) {
            fi.setCaption(fileCaption);
          }

          filesResult.add(fi);
        }
        setFiles(filesResult);

        prepareCascadedStructure(eventPanel, events);
      }
    };
  }

  private void loadHandlerRegistry() {
    logger.debug("load register of", getEventsDataViewName());
    registry.add(BeeKeeper.getBus().registerRowInsertHandler(this, false));
    registry.addAll(BeeKeeper.getBus().registerUpdateHandler(this, false));
    registry.add(BeeKeeper.getBus().registerDataChangeHandler(this, false));
  }

  private void prepareCascadedStructure(HasWidgets widget, BeeRowSet rs) {
    String parentColumnName = getParentEventColumnName();

    List<Long> roots = Lists.newArrayList();
    Multimap<Long, Long> data = HashMultimap.create();

    if (BeeUtils.isEmpty(parentColumnName)) {
      roots = rs.getRowIds();
    } else {
      int idxParent = rs.getColumnIndex(parentColumnName);

      if (BeeUtils.isNegative(idxParent)) {
        logger.error(null, "Parent column", parentColumnName, "not found in view",
            getEventsDataViewName());
        return;
      }

      for (BeeRow row : rs.getRows()) {
        Long parent = row.getLong(idxParent);

        if (parent == null) {
          roots.add(row.getId());
        } else {
          data.put(parent, row.getId());
        }
      }
    }

    for (long id : roots) {
      BeeRow row = rs.getRowById(id);
      createEventRow(rs, row, widget, 0);
      createChildEventRows(rs, data, id, widget, 1);
    }

    afterCreateEventRows();

    if (widget instanceof ComplexPanel) {
      ComplexPanel panel = (ComplexPanel) widget;

      if (panel.getWidgetCount() > 0 && DomUtils.isVisible(panel.getParent())) {
        final Widget last = panel.getWidget(panel.getWidgetCount() - 1);
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          @Override
          public void execute() {
            DomUtils.scrollIntoView(last.getElement());
          }
        });
      }
    }
  }
}
