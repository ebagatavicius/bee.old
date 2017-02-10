package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.Cursor;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AnnouncementsBoardInterceptor extends AbstractFormInterceptor implements
    RowInsertEvent.Handler, HandlesUpdateEvents, DataChangeEvent.Handler,
    HandlesDeleteEvents {

  private class AnnouncementTopicWidget extends Flow {
    private static final String STYLE_BORDER_LEFT_BLUE = STYLE_PREFIX + "LeftBorder";
    private static final String STYLE_BORDER_LEFT_NONE = STYLE_PREFIX + "leftBorder-none";
    private static final String STYLE_BORDER_LEFT_ORANGE = STYLE_PREFIX + "leftBorder-red";
    private static final String STYLE_RIGHT_FLOW = STYLE_PREFIX + "right";
    private static final String STYLE_AUTH_PHOTO = STYLE_PREFIX + "auth-photo";
    private static final String STYLE_NAME_SUR = STYLE_PREFIX + "name-sur";

    private static final String STYLE_IMPORTANT = STYLE_PREFIX + "important";
    private static final String STYLE_TOPIC_FLOW = STYLE_PREFIX + "topic-flow";
    private static final String STYLE_TIME_CREATED = STYLE_PREFIX + "time-created";
    private static final String STYLE_COMMENTS_COUNT = STYLE_PREFIX + "comments-count";
    private static final String STYLE_PAPER_CLIP = STYLE_PREFIX + "paper-clip";
    private static final String STYLE_TOPIC = STYLE_PREFIX + "topic";
    private static final String STYLE_SUBJECT = STYLE_PREFIX + "subject";
    private static final String STYLE_FILE_GROUP = STYLE_PREFIX + "file-group";
    private static final String STYLE_COMMENT_LINE = STYLE_PREFIX + "comment-line";
    private static final String STYLE_USER_PHOTO = STYLE_PREFIX + "user-photo";
    private static final String STYLE_COMENT_INPUT = STYLE_PREFIX + "coment-input";
    private static final String STYLE_FA = STYLE_PREFIX + "fa";
    private static final String STYLE_FA_SEND = STYLE_PREFIX + "fa-send";
    private static final String STYLE_FILE_COLLECTOR = STYLE_PREFIX + "file-collector";
    private static final String STYLE_MAIN_CONTAINER = STYLE_PREFIX + "conatiner";

    private final Flow eventContainer = new Flow(StyleUtils.NAME_FLEX_BOX_VERTICAL);
    private final Flow userContainer = new Flow(STYLE_RIGHT_FLOW);
    private final Image userPhoto = new Image();
    private final TextLabel userName = new TextLabel(true);
    private final FaLabel important = new FaLabel(FontAwesome.EXCLAMATION);
    private final TextLabel topicWidget = new TextLabel(true);
    private final TextLabel timeCreated = new TextLabel(true);
    private final Flow numbersContainer = new Flow();
    private final TextLabel commentCount = new TextLabel(true);
    private final FaLabel attachmentLabel = new FaLabel(FontAwesome.PAPERCLIP);
    private final Flow containerSubject = new Flow(StyleUtils.NAME_FLEX_BOX_VERTICAL);
    private final TextLabel subject = new TextLabel(true);
    private final TextLabel summaryWidget = new TextLabel(true);
    private final InputArea commentInput = new InputArea();
    private final FaLabel sendLabel = new FaLabel(FontAwesome.SEND, STYLE_PREFIX + COL_SEND);
    private final FaLabel attachLabel = new FaLabel(FontAwesome.PAPERCLIP, STYLE_PREFIX
        + COL_ATTACH);
    private final FileCollector att = new FileCollector(attachLabel);
    private final Flow commentLineFlow = new Flow();
    private FileGroup files = new FileGroup();

    AnnouncementTopicWidget(final Long discussId) {
      super(STYLE_MAIN_CONTAINER);
      openOnClick(VIEW_DISCUSSIONS, discussId);
      add(eventContainer);

      Flow container2 = new Flow();
      container2.addStyleName(StyleUtils.NAME_FLEX_BOX_VERTICAL);
      eventContainer.add(container2);

      container2.add(userContainer);
      userPhoto.addStyleName(STYLE_AUTH_PHOTO);
      userContainer.add(userPhoto);

      userName.setStyleName(STYLE_NAME_SUR);
      userContainer.add(userName);

      important.addStyleName(STYLE_IMPORTANT);
      important.setVisible(false);
      userContainer.add(important);

      topicWidget.setStyleName(STYLE_TOPIC_FLOW);
      userContainer.add(topicWidget);

      attachmentLabel.setStyleName(STYLE_PAPER_CLIP);
      attachmentLabel.addClickHandler(arg0 -> downloadFile(discussId));
      userContainer.add(attachmentLabel);

      timeCreated.setStyleName(STYLE_TIME_CREATED);
      userContainer.add(timeCreated);

      commentCount.setStyleName(STYLE_COMMENTS_COUNT);
      numbersContainer.add(commentCount);
      userContainer.add(numbersContainer);
      eventContainer.add(containerSubject);

      subject.setStyleName(STYLE_SUBJECT);
      containerSubject.add(subject);

      Flow container4 = new Flow();
      container4.addStyleName(StyleUtils.NAME_FLEX_BOX_VERTICAL);

      eventContainer.add(container4);

      summaryWidget.setStyleName(STYLE_TOPIC);
      container4.add(summaryWidget);

      Flow container5 = new Flow();
      container5.addStyleName(StyleUtils.NAME_FLEX_BOX_VERTICAL);
      eventContainer.add(container5);

      files.addStyleName(STYLE_FILE_GROUP);

      container5.add(files);

      Flow container6 = new Flow();
      container6.addStyleName(StyleUtils.NAME_FLEX_BOX_VERTICAL);
      add(container6);

      commentLineFlow.addStyleName(STYLE_COMMENT_LINE);

      container6.add(commentLineFlow);

      Long photoFile = BeeKeeper.getUser().getUserData().getPhotoFile();

      Image currUserPhoto = new Image();
      if (DataUtils.isId(photoFile)) {
        currUserPhoto.setUrl(PhotoRenderer.getUrl(photoFile));
      } else {
        currUserPhoto.setUrl(DEFAULT_PHOTO_IMAGE);
      }
      currUserPhoto.addStyleName(STYLE_USER_PHOTO);
      commentLineFlow.add(currUserPhoto);

      commentInput.addStyleName(STYLE_COMENT_INPUT);

      commentLineFlow.add(commentInput);

      sendLabel.setTitle(Localized.dictionary().crmActionComment());
      attachLabel.addStyleName(STYLE_FA);

      sendLabel.addClickHandler(event -> {
        String viewName = VIEW_DISCUSSIONS_COMMENTS;

        if (BeeUtils.isEmpty(commentInput.getValue()) || !DataUtils.isId(discussId)) {
          return;
        }

        Queries.insert(viewName, Data.getColumns(VIEW_DISCUSSIONS_COMMENTS, Lists.newArrayList(
            COL_DISCUSSION, COL_PUBLISHER, COL_PUBLISH_TIME, COL_COMMENT_TEXT)), Lists
            .newArrayList(
                BeeUtils.toString(discussId), BeeUtils.toString(BeeKeeper.getUser()
                    .getUserId()), BeeUtils.toString(new DateTime().getTime()), commentInput
                    .getValue()),
            null,
            new RowInsertCallback(viewName) {
              @Override
              public void onSuccess(BeeRow result) {
                final String commentId = Long.toString(result.getId());
                RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS_COMMENTS, result);

                final List<BeeColumn> columns =
                    Data.getColumns(VIEW_DISCUSSIONS_FILES, Lists.newArrayList(COL_DISCUSSION,
                        COL_COMMENT,
                        AdministrationConstants.COL_FILE, COL_CAPTION));
                commentInput.setValue("");

                for (final FileInfo f : att.getFiles()) {
                  FileUtils.uploadFile(f, result1 -> {
                    att.clear();
                    commentInput.setValue("");
                    List<String> values = Lists.newArrayList(BeeUtils.toString(discussId),
                        commentId, BeeUtils.toString(result1), f
                            .getCaption());

                    Queries.insert(VIEW_DISCUSSIONS_FILES, columns, values, null,
                        new RowCallback() {
                          @Override
                          public void onSuccess(BeeRow row) {
                            att.clear();
                            renderContent(getFormView(), false);
                          }
                        });
                  });
                }
              }
            });
      });

      sendLabel.addStyleName(STYLE_FA_SEND);
      commentLineFlow.add(sendLabel);

      att.addStyleName(STYLE_FILE_COLLECTOR);
      commentLineFlow.add(att);

      Flow line = new Flow();
      line.addStyleName(STYLE_SEPARATOR_LINE);
      container6.add(line);

      Flow container8 = new Flow();
      container8.addStyleName(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);

      final TextLabel comValue = new TextLabel(true);

      container8.add(comValue);
      add(container8);
    }

    void openOnClick(final String viewName, final Long id) {

      if (DataUtils.isId(id) && BeeKeeper.getUser().isDataVisible(viewName)) {

        ClickHandler handler = arg0 -> {
          RowEditor.open(viewName, id, Opener.NEW_TAB);
          markAsRead();
        };
        if (commentCount != null) {
          commentCount.addClickHandler(handler);
        }

        if (containerSubject != null) {
          containerSubject.addClickHandler(handler);
          containerSubject.getElement().getStyle().setProperty(StyleUtils.STYLE_CURSOR,
              Cursor.POINTER.getCssName());
        }
        if (topicWidget != null) {
          topicWidget.addClickHandler(handler);
          topicWidget.getElement().getStyle().setProperty(StyleUtils.STYLE_CURSOR,
              Cursor.POINTER.getCssName());
        }
      }
    }

    void markAsNew() {
      eventContainer.addStyleName(STYLE_BORDER_LEFT_BLUE);
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_NONE);
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_ORANGE);
    }

    void markAsRead() {
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_ORANGE);
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_BLUE);
      eventContainer.setStyleName(STYLE_BORDER_LEFT_NONE);
    }

    void markAsModify() {
      eventContainer.addStyleName(STYLE_BORDER_LEFT_ORANGE);
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_NONE);
      eventContainer.removeStyleName(STYLE_BORDER_LEFT_BLUE);
    }

    void showAttachments(boolean show, List<FileInfo>  filesList) {
      attachmentLabel.setVisible(show);

      files.clear();
      if (!BeeUtils.isEmpty(filesList)) {
        files.addFiles(filesList);
      }
      files.setVisible(show);
    }

    void setCreateTime(String time) {
      if (!BeeUtils.isLong(time)) {
        timeCreated.setText(BeeConst.STRING_EMPTY);
      } else {
        Long timeInMillis = BeeUtils.toLong(time);
        timeCreated.setText(DiscussionHelper.renderDateTime(new DateTime(timeInMillis)));
      }
    }

    void setCommentCount(String count) {
      commentCount.setText(count);
    }

    void setEnableCommenting(boolean enabled) {

      if (enabled) {
        commentInput.setEnabled(true);
        DomUtils.setPlaceholder(commentInput, Localized.dictionary().discussCommentPlaceholder());
        sendLabel.setEnabled(true);
        att.setVisible(true);
      } else {
        commentInput.setEnabled(false);
        DomUtils.setPlaceholder(commentInput, Localized.dictionary().discussCommentPermit());
        sendLabel.setEnabled(false);
        att.setVisible(false);
      }
    }

    void setImportant(boolean important) {
      this.important.setVisible(important);
    }

    void setFullName(String name) {
      userName.setText(name);
    }

    void setPhoto(String photo) {
      userPhoto.setUrl(photo);
    }

    void setSubject(String subject) {
      this.subject.setText(subject);
    }

    void setSummary(String summary) {
      summaryWidget.setValue(summary);
    }

    void setTopicName(String topicName) {
      topicWidget.setText(topicName);
    }

    void setTopicColor(String bg, String fg) {
      StyleUtils.setBackgroundColor(topicWidget.getElement(), bg);
      StyleUtils.setColor(topicWidget.getElement(), fg);
    }

    void setVisibleCommentLine(boolean visible) {
      commentLineFlow.setVisible(visible);
    }

    private void downloadFile(Long discussionId) {
      if (!DataUtils.isId(discussionId)) {
        return;
      }

      Filter filter = Filter.and(Filter.isEqual(COL_DISCUSSION, Value.getValue(discussionId)),
          Filter.or(Filter.isNull(COL_COMMENT),
              Filter.in(COL_COMMENT, VIEW_DISCUSSIONS_COMMENTS, COL_DISCUSSION_COMMENT_ID,
                  Filter.or(Filter.isNull(COL_DELETED),
                      Filter.isEqual(COL_DELETED, BooleanValue.FALSE)))));
      // filter = Filter.and(filter, Filter.isNull(COL_COMMENT));
      GridPanel grid = new GridPanel(GRID_DISCUSSION_FILES, GridOptions.forFilter(filter), false);
      grid.setGridInterceptor(new DiscussionFilesGrid());
      StyleUtils.setSize(grid, 600, 400);

      DialogBox dialog = DialogBox.create(null);
      dialog.setWidget(grid);
      dialog.setAnimationEnabled(true);
      dialog.setHideOnEscape(true);
      dialog.center();
    }
  }

  private static final String WIDGET_ADS_CONTENT = "AdsContent";
  private static final String WIDGET_ADS_FLOW = "Flowing";

  public static final String COL_ATTACH = "Attach";
  public static final String COL_SEND = "Send";

  private static final String STYLE_MAIN = BeeConst.CSS_CLASS_PREFIX
      + "discuss-adsFormContent";
  private static final String STYLE_PREFIX = STYLE_MAIN + "-";
  private static final String STYLE_HAPPY_DAY = STYLE_PREFIX + "DescriptionBirthday-happyDay";
  private static final String STYLE_BIRTH_LIST = STYLE_PREFIX + "DescriptionBirthday-birthList";
  private static final String STYLE_HB_DATE = STYLE_PREFIX + "hb-date";
  private static final String STYLE_HB_PHOTO = STYLE_PREFIX + "hb-photo";
  private static final String STYLE_NAME_SUR_HB = STYLE_PREFIX + "name-sur-hb";
  private static final String STYLE_TOPIC_HB = STYLE_PREFIX + "topic-hb";
  private static final String STYLE_SEPARATOR_LINE = STYLE_PREFIX + "line";

  private static final String LOCALE_NAME_LT = "lt";

  private static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";

  private static final String DAY = Localized.dictionary().unitDayShort().toLowerCase();

  private final Collection<HandlerRegistration> registry = new ArrayList<>();
  private final Map<Long, AnnouncementTopicWidget> cachedContainers = new HashMap<>();
  private boolean emptyForm;
  private AnnouncementTopicWidget lastTopicPosition;
  private Flow cachedBirthdays;

  @Override
  public FormInterceptor getInstance() {
    return new AnnouncementsBoardInterceptor();
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action.compareTo(Action.REFRESH) == 0) {
      setVisited();
      renderContent(getFormView(), true);
      return false;
    }

    if (Action.CLOSE.equals(action)) {
      setVisited();
    }

    if (action.compareTo(Action.ADD) == 0) {

      DataInfo data = Data.getDataInfo(VIEW_DISCUSSIONS);
      BeeRow emptyRow = RowFactory.createEmptyRow(data, true);
      RowFactory.createRow(FORM_NEW_ANNOUNCEMENT, Localized.dictionary().announcementNew(),
          data, emptyRow, Modality.ENABLED, presenter.getMainView().asWidget(),
          new CreateDiscussionInterceptor(), null, null);

      return false;
    }

    return super.beforeAction(action, presenter);
  }

  @Override
  public void onLoad(FormView form) {
    registry.add(BeeKeeper.getBus().registerRowInsertHandler(this, false));
    registry.addAll(BeeKeeper.getBus().registerUpdateHandler(this, false));
    registry.add(BeeKeeper.getBus().registerDataChangeHandler(this, false));
    registry.addAll(BeeKeeper.getBus().registerDeleteHandler(this, false));
    super.onLoad(form);
  }

  @Override
  public void onStart(FormView form) {
    renderContent(form, true);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS) || event.hasView(VIEW_DISCUSSIONS_COMMENTS)
        || event.hasView(VIEW_DISCUSSIONS_FILES)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS) || event.hasView(VIEW_DISCUSSIONS_COMMENTS)) {
      renderContent(getFormView(), false);
    }
  }

  @Override
  public void onUnload(FormView form) {
    EventUtils.clearRegistry(registry);
    super.onUnload(form);
  }

  protected void renderAnnouncementsSection(final SimpleRow rsRow,
      final SimpleRowSet rs, Flow mainFlow, List<FileInfo>  filesList) {
    final Long rowId = rsRow.getLong(COL_DISCUSSION);

    AnnouncementTopicWidget tWidget = null;

    if (cachedContainers.containsKey(rowId)) {
      tWidget = cachedContainers.get(rowId);
      setLastTopicPosition(cachedContainers.get(rowId));
    }

    if (tWidget == null) {
      tWidget = new AnnouncementTopicWidget(rowId);
      mainFlow.insert(tWidget, mainFlow.getWidgetIndex(getLastTopicPosition()) + 1);
      cachedContainers.put(rowId, tWidget);
      setLastTopicPosition(tWidget);
    }

    if (rs.hasColumn(ALS_NEW_ANNOUCEMENT)) {
      boolean isNew = BeeUtils.unbox(rsRow.getBoolean(ALS_NEW_ANNOUCEMENT));
      if (isNew) {
        tWidget.markAsNew();
      } else {
        if (!isNew) {
          tWidget.markAsRead();
        }
        if (BeeUtils.unbox(rsRow.getLong(ALS_MAX_PUBLISH_TIME)) > BeeUtils.unbox(rsRow.getLong(
            NewsConstants.COL_USAGE_ACCESS))) {
          tWidget.markAsModify();
        }

      }

    }

    Long photoId = rsRow.getLong(COL_PHOTO);

    if (DataUtils.isId(photoId)) {
      tWidget.setPhoto(PhotoRenderer.getUrl(photoId));
    } else {
      tWidget.setPhoto(DEFAULT_PHOTO_IMAGE);
    }

    String fullName = BeeUtils.joinWords(rsRow.getValue(COL_FIRST_NAME), rsRow.getValue(
        COL_LAST_NAME));

    if (!BeeUtils.isEmpty(fullName)) {
      tWidget.setFullName(fullName);
    }

    if (!BeeUtils.isEmpty(rsRow.getValue(COL_IMPORTANT))) {
      tWidget.setImportant(true);
    } else {
      tWidget.setImportant(false);
    }

    tWidget.setTopicName(rsRow.getValue(ALS_TOPIC_NAME));
    tWidget.setTopicColor(rsRow.getValue(COL_BACKGROUND_COLOR), rsRow.getValue(COL_TEXT_COLOR));
    tWidget.setCreateTime(rsRow.getValue(COL_CREATED));

    if (rs.hasColumn(AdministrationConstants.COL_FILE)) {
      if (!BeeUtils.isEmpty(rsRow.getValue(AdministrationConstants.COL_FILE))) {
        int fileCount = BeeUtils.unbox(rsRow.getInt(AdministrationConstants.COL_FILE));

        boolean hasFiles = BeeUtils.isPositive(fileCount);
        tWidget.showAttachments(hasFiles, filesList);

      } else {
        tWidget.showAttachments(false, filesList);
      }
    } else {
      tWidget.showAttachments(false, filesList);
    }

    if (DataUtils.isId(rsRow.getLong(COL_DISCUSSION_COMMENT_ID))) {
      tWidget.setCommentCount(BeeUtils.joinWords(rsRow.getValue(COL_DISCUSSION_COMMENT_ID),
          Localized.dictionary().discussCommentCount()));
    } else {
      tWidget.setCommentCount(BeeConst.STRING_EMPTY);
    }
    tWidget.setSubject(rsRow.getValue(COL_SUBJECT));
    tWidget.setSummary(rsRow.getValue(COL_SUMMARY));
    tWidget.setVisibleCommentLine(true);
    tWidget.setEnableCommenting(DiscussionStatus.CLOSED.ordinal() != BeeUtils.unbox(rsRow.getInt(
        COL_STATUS)));

    if (mainFlow != null) {
      setEmptyForm(false);
      mainFlow.add(tWidget);
    }

  }

  protected void renderWelcomeSection(Flow adsTable) {

    AnnouncementTopicWidget welcome = new AnnouncementTopicWidget(null);
    welcome.setEnableCommenting(false);
    welcome.setPhoto(DEFAULT_PHOTO_IMAGE);
    welcome.setSummary(Localized.dictionary().welcomeMessage());
    welcome.showAttachments(false, null);
    welcome.setVisibleCommentLine(false);

    adsTable.clear();
    adsTable.add(welcome);
  }

  protected void renderBirthdaySection(SimpleRow rsRow,
      Flow mainFlow, boolean forceRefresh) {

    HtmlTable birthTable = new HtmlTable(STYLE_MAIN);
    int row = birthTable.getRowCount();

    Flow birthdayContent;

    if (forceRefresh || getCachedBirthdays() == null) {
      birthdayContent = new Flow();
      setCatchedBirthdays(birthdayContent);
    } else {
      birthdayContent = getCachedBirthdays();
    }

    birthdayContent.clear();
    Flow caption = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);
    caption.add(new Label(Localized.dictionary().birthdaysParties()));

    Flow topicName = new Flow();
    caption.add(topicName);
    birthTable.setWidget(row, 0, caption, STYLE_PREFIX + COL_SUBJECT);
    birthTable.getCellFormatter().setColSpan(row, 0, 2);

    birthTable.getRow(row).addClassName(STYLE_PREFIX + ALS_BIRTHDAY);
    TextLabel topicN = new TextLabel(true);
    topicN.setValue(rsRow.getValue(ALS_TOPIC_NAME));
    topicN.setStyleName(STYLE_PREFIX + ALS_TOPIC_NAME);
    topicN.setStyleName(STYLE_PREFIX + ALS_TOPIC_NAME + ALS_BIRTHDAY);
    topicN.setStyleName(STYLE_TOPIC_HB);
    StyleUtils.setBackgroundColor(topicN.getElement(), rsRow.getValue(COL_BACKGROUND_COLOR));
    StyleUtils.setColor(topicN.getElement(), rsRow.getValue(COL_TEXT_COLOR));
    topicName.add(topicN);

    row++;
    int contentRow = row;
    birthTable.setHtml(row, 0, BeeConst.STRING_EMPTY, STYLE_PREFIX + COL_DESCRIPTION);
    birthTable.getCellFormatter().setColSpan(row, 0, 2);

    birthTable.getRow(row).addClassName(STYLE_PREFIX + ALS_BIRTHDAY);

    renderBirthdaysList(contentRow, birthTable, rsRow.getValue(ALS_BIRTHDAY));

    birthdayContent.add(birthTable);
    Flow line = new Flow();
    line.addStyleName(STYLE_SEPARATOR_LINE);
    birthdayContent.add(line);

    if (mainFlow != null) {
      mainFlow.add(birthdayContent);
    }

  }

  protected void setVisited() {
    Set<Long> k = cachedContainers.keySet();

    for (long id : k) {
      Global.getNewsAggregator().onAccess(VIEW_DISCUSSIONS, id);

      AnnouncementTopicWidget w = cachedContainers.get(id);

      if (w != null) {
        w.markAsRead();
      }
    }
  }

  private void clearCache(Flow panel) {
    cachedContainers.clear();
    panel.clear();
  }

  private void clearUnused(Flow panel, SimpleRowSet data) {
    // List<Long> queue = new ArrayList<>();
    Map<Long, AnnouncementTopicWidget> newCache = new HashMap<>();
    for (Long id : cachedContainers.keySet()) {
      if (!DataUtils.isId(data.getValueByKey(COL_DISCUSSION, BeeUtils.toString(id),
          COL_DISCUSSION))) {
        panel.remove(cachedContainers.get(id));
        // queue.add(id);
      } else {
        newCache.put(id, cachedContainers.get(id));
      }
    }

    cachedContainers.clear();
    cachedContainers.putAll(newCache);
  }

  private Flow getCachedBirthdays() {
    return cachedBirthdays;
  }

  private AnnouncementTopicWidget getLastTopicPosition() {
    return lastTopicPosition;
  }

  private void renderBirthdaysList(final int contentRow, final HtmlTable birthContent,
      String birthData) {

    if (BeeUtils.isEmpty(birthData)) {
      return;
    }

    SimpleRowSet rs = SimpleRowSet.restore(birthData);

    if (rs == null || rs.isEmpty()) {
      birthContent.clear();
      return;
    } else {
      setEmptyForm(false);
    }

    HtmlTable listTbl = new HtmlTable();
    int row = listTbl.getRowCount();

    for (String[] birthListData : rs.getRows()) {

      String photo1 = birthListData[rs.getColumnIndex(COL_PHOTO)];
      String photoUrl1;
      if (DataUtils.isId(photo1)) {
        photoUrl1 =
            PhotoRenderer.getUrl(BeeUtils.toLongOrNull(photo1));
      } else {
        photoUrl1 = DEFAULT_PHOTO_IMAGE;
      }

      Image img = new Image(photoUrl1);
      img.addStyleName(STYLE_HB_PHOTO);
      listTbl.setWidget(row, 0, img);

      listTbl.setHtml(row, 1, birthListData[rs.getColumnIndex(COL_NAME)]);
      listTbl.addStyleName(STYLE_NAME_SUR_HB);
      String locale =
          LocaleInfo.getCurrentLocale().getLocaleName();
      if (locale.equals(LOCALE_NAME_LT)) {
        Flow happyBirthdayDate = new Flow();
        listTbl.setWidget(row, 2, happyBirthdayDate);
        TextLabel date = new TextLabel(true);
        date.setValue(Format.getPredefinedFormat(PredefinedFormat.MONTH_DAY)
            .format(new JustDate(BeeUtils.toLong(birthListData[rs
                .getColumnIndex(COL_DATE_OF_BIRTH)])))
            + " " + DAY);
        happyBirthdayDate.add(date);
        happyBirthdayDate.setStyleName(STYLE_HB_DATE);
      } else {
        listTbl.setHtml(row, 2, Format.getPredefinedFormat(PredefinedFormat.MONTH_DAY)
            .format(new JustDate(BeeUtils.toLong(birthListData[rs
                .getColumnIndex(COL_DATE_OF_BIRTH)]))));
      }

      listTbl.getRow(row).addClassName(STYLE_BIRTH_LIST);

      JustDate now = new JustDate();

      if (now.getDoy() == (new JustDate(BeeUtils.toLong(birthListData[rs
          .getColumnIndex(COL_DATE_OF_BIRTH)]))).getDoy()) {
        listTbl.getRow(row).addClassName(STYLE_HAPPY_DAY);
      }
      row++;
    }

    birthContent.setWidget(contentRow, 0, listTbl);
  }

  private void renderContent(final FormView form, final boolean forceRefresh) {
    Widget w = Assert.notNull(form.getWidgetByName(WIDGET_ADS_CONTENT));
    Widget f = Assert.notNull(form.getWidgetByName(WIDGET_ADS_FLOW));

    setEmptyForm(true);

    if (!(w instanceof HtmlTable)) {
      Assert.notImplemented();
    }

    if (!(f instanceof Flow)) {
      Assert.notImplemented();
    }

    final HtmlTable adsTable = (HtmlTable) w;
    final Flow adsFlow = (Flow) f;
    adsTable.clear();

    if (forceRefresh) {
      clearCache(adsFlow);
    }
    ParameterList params = DiscussionsKeeper.createArgs(SVC_GET_ANNOUNCEMENTS_DATA);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);
        adsTable.clear();
        adsTable.setVisible(false);
        if (forceRefresh) {
          clearCache(adsFlow);
        }
        if (response.isEmpty()) {
          renderWelcomeSection(adsFlow);
          return;
        }

        if (!response.hasResponse(Pair.class)) {
          renderWelcomeSection(adsFlow);
          return;
        }

        setEmptyForm(true);

        Pair<String, String> results = Pair.restore(response.getResponseAsString());
        String serializedAnnouncements = results.getA();
        if (BeeUtils.isEmpty(serializedAnnouncements)) {
          return;
        }
        SimpleRowSet rs = SimpleRowSet.restore(serializedAnnouncements);

        String serializedAnnouncementsFiles = results.getB();
        Map<Long, List<FileInfo>> discussionFilesMap = new HashMap<>();
        if (!BeeUtils.isEmpty(serializedAnnouncementsFiles)) {
          SimpleRowSet rsFiles = SimpleRowSet.restore(results.getB());
          discussionFilesMap = generateDiscussionFilesMap(rsFiles);
        }

        clearUnused(adsFlow, rs);
        setLastTopicPosition(null);

        for (SimpleRow rsRow : rs) {
          if (rs.hasColumn(ALS_BIRTHDAY) && !BeeUtils.isEmpty(rsRow.getValue(ALS_BIRTHDAY))) {
            renderBirthdaySection(rsRow, adsFlow, forceRefresh);
          } else {
            renderAnnouncementsSection(rsRow, rs, adsFlow,
                discussionFilesMap.get(rsRow.getLong(COL_DISCUSSION)));
          }
        }

        if (isEmptyForm()) {
          // adsTable.setVisible(true);
          renderWelcomeSection(adsFlow);
        } else {
          adsTable.setVisible(false);
        }

      }
    });
  }

  private Map<Long, List<FileInfo>> generateDiscussionFilesMap(SimpleRowSet rsFiles) {
    Map<Long, List<FileInfo>> discussionFilesMap = new HashMap<>();
    if (DataUtils.isEmpty(rsFiles)) {
      return new HashMap<>();
    }

    for (SimpleRow row : rsFiles) {
      FileInfo file = new FileInfo(row.getLong(COL_FILE),
          row.getValue(COL_FILE_NAME),
          row.getLong(COL_FILE_SIZE),
          row.getValue(COL_FILE_TYPE));

      file.setCaption(row.getValue(COL_FILE_CAPTION));
      file.setIcon(FileNameUtils.getExtension(file.getName()) + ".png");
      Long discussionsId = row.getLong(COL_DISCUSSION);
      if (discussionFilesMap.containsKey(discussionsId)) {
        discussionFilesMap.get(discussionsId).add(file);
      } else {
        List<FileInfo> fileList = new ArrayList<>();
        fileList.add(file);
        discussionFilesMap.put(discussionsId, fileList);
      }
    }

    return discussionFilesMap;
  }

  private boolean isEmptyForm() {
    return emptyForm;
  }

  private void setEmptyForm(boolean emptyForm) {
    this.emptyForm = emptyForm;
  }

  private void setCatchedBirthdays(Flow cahedBirthdays) {
    this.cachedBirthdays = cahedBirthdays;
  }

  private void setLastTopicPosition(AnnouncementTopicWidget lastTopicPosition) {
    this.lastTopicPosition = lastTopicPosition;
  }
}
