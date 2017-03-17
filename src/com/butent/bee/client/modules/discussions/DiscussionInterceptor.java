package com.butent.bee.client.modules.discussions;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.Disclosure;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.event.logical.SelectorEvent.Handler;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.mail.Relations;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.richtext.RichTextEditor;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionStatus;
import com.butent.bee.shared.modules.discussions.DiscussionsUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.*;

class DiscussionInterceptor extends AbstractFormInterceptor {

  private static final class CommentDialog extends DialogBox {

    private static final String STYLE_DIALOG = DISCUSSIONS_STYLE_PREFIX + "commentDialog";
    private static final String STYLE_CELL = "Cell";

    private CommentDialog(String caption) {
      super(caption, STYLE_DIALOG);

      addDefaultCloseBox();

      HtmlTable container = new HtmlTable();
      container.addStyleName(STYLE_DIALOG + "-container");

      setWidget(container);
    }

    private void addSaveAction(final ScheduledCommand command) {
      String styleName = STYLE_DIALOG + "-action";

      FaLabel faSave = new FaLabel(FontAwesome.SAVE);
      faSave.addClickHandler(arg0 -> command.execute());

      HtmlTable table = getContainer();
      int row = table.getRowCount();
      int col = 0;

      faSave.setTitle(Action.SAVE.getCaption());
      StyleUtils.enableAnimation(Action.SAVE, faSave);

      insertAction(BeeConst.INT_TRUE, faSave);

      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
      table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.CENTER);

      table.getCellFormatter().setColSpan(row, col, 2);
    }

    private String addComment(boolean required) {
      String styleName = STYLE_DIALOG + "commentLabel";
      Label label = new Label(Localized.dictionary().discussComment());
      label.addStyleName(styleName);
      if (required) {
        label.addStyleName(StyleUtils.NAME_REQUIRED);
      }

      HtmlTable table = getContainer();
      int row = table.getRowCount();
      int col = 0;

      table.setWidget(row, col, label);
      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
      col++;

      Editor input = new RichTextEditor(true);
      styleName = STYLE_DIALOG + "-commentEditor";
      input.addStyleName(styleName);
      table.setWidget(row, col, (Widget) input);
      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

      return input.getId();
    }

    private String addFileCollector(IsRow formRow) {
      HtmlTable table = getContainer();
      int row = table.getRowCount();
      int col = 0;

      String styleName = STYLE_DIALOG + "-filesLabel";
      Label label = new Label(Localized.dictionary().discussFiles());
      label.addStyleName(styleName);

      table.setWidget(row, col, label);
      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
      col++;

      styleName = STYLE_DIALOG + "fileCollector";
      final FileCollector collector = new FileCollector(new Image(Global.getImages().attachment()));
      collector.addStyleName(styleName);

      final Map<String, String> discussParams = DiscussionsUtils.getDiscussionsParameters(formRow);
      if (discussParams != null) {

        collector.addSelectionHandler(event -> {
          FileInfo fileInfo = event.getSelectedItem();

          if (DiscussionsUtils.isFileSizeLimitExceeded(fileInfo.getSize(),
              BeeUtils.toLongOrNull(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)))) {

            BeeKeeper.getScreen().notifyWarning(
                Localized.dictionary().fileSizeExceeded(fileInfo.getSize(),
                    BeeUtils.toLong(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)) * 1024 * 1024),
                "("
                    + fileInfo.getName() + ")");

            collector.clear();
            return;
          }

          if (DiscussionsUtils.isForbiddenExtention(BeeUtils.getSuffix(fileInfo.getName(),
              BeeConst.STRING_POINT), discussParams.get(PRM_FORBIDDEN_FILES_EXTENTIONS))) {

            BeeKeeper.getScreen().notifyWarning(Localized.dictionary().discussInvalidFile(),
                fileInfo.getName());
            collector.clear();
          }
        });
      }
      table.setWidget(row, col, collector);
      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);

      Widget panel = getWidget();
      if (panel instanceof DndTarget) {
        collector.bindDnd((DndTarget) panel);
      }

      return collector.getId();
    }

    private void display() {
      focusOnOpen(getContent());
      center();
    }

    private Widget getChild(String id) {
      return DomUtils.getChildQuietly(getContent(), id);
    }

    private String getComment(String id) {
      Widget child = getChild(id);
      if (child instanceof InputArea) {
        return ((InputArea) child).getValue();
      } else if (child instanceof Editor) {
        return ((Editor) child).getValue();
      } else {
        return null;
      }
    }

    private HtmlTable getContainer() {
      return (HtmlTable) getContent();
    }

    private List<FileInfo> getFiles(String id) {
      Widget child = getChild(id);
      if (child instanceof FileCollector) {
        return ((FileCollector) child).getFiles();
      } else {
        return new ArrayList<>();
      }
    }
  }

  private static final String STYLE_COMMENT = DISCUSSIONS_STYLE_PREFIX + "comment-";
  private static final String STYLE_COMMENT_ROW = STYLE_COMMENT + "row";
  private static final String STYLE_COMMENT_COL = STYLE_COMMENT + "col-";
  private static final String STYLE_COMMENT_FILES = STYLE_COMMENT + "files";
  private static final String STYLE_MARK_TYPES = DISCUSSIONS_STYLE_PREFIX + "markTypes";
  private static final String STYLE_HAS_MARKS = STYLE_MARK_TYPES + "-hasMarks";
  private static final String STYLE_ACTIONS = "Actions";
  private static final String STYLE_MARKED = "-marked";
  private static final String STYLE_MARK = "-mark";
  private static final String STYLE_STATS = "-stats";
  private static final String STYLE_DISABLED = "-disabled";
  private static final String STYLE_LABEL = "-label";
  private static final String STYLE_REPLY = "-reply";
  private static final String STYLE_TRASH = "-trash";
  private static final String STYLE_CHATTER = "-chatter";
  private static final String STYLE_PHOTO = "Photo";

  private static final String WIDGET_RELATIONS_DISCLOSURE = "RelDisclosure";
  private static final String WIDGET_DESCRIPTION_DISCLOSURE = "DescriptionDisclosure";

  private static final String WIDGET_LABEL_MEMBERS = "membersLabel";
  private static final String WIDGET_LABEL_DISPLAY_IN_BOARD = "DisplayInBoard";
  private static final String WIDGET_FALABEL_REPLY = "replyFa";

  private static final int INITIAL_COMMENT_ROW_PADDING_LEFT = 0;
  private static final int COMMENT_ROW_PADDING_FACTOR = 2;
  private static final int MAX_COMMENT_ROW_PADDING_LEFT = COMMENT_ROW_PADDING_FACTOR * 5;
  private static final DiscussionEvent[] FIRE_GLOBAL_EVENTS  = {
          DiscussionEvent.CREATE, DiscussionEvent.ACTIVATE, DiscussionEvent.DEACTIVATE,
          DiscussionEvent.CLOSE, DiscussionEvent.COMMENT,
          DiscussionEvent.REPLY, DiscussionEvent.MARK, DiscussionEvent.MODIFY
  };

  private final long userId;
  private Image discussOwnerPhoto;
  private Flow markPanel;
  private Relations relations;
  private InputBoolean isPublic;
  private InputBoolean isPermitComment;
  private MultiSelector membersSelector;
  private Label membersLabel;
  private Label displayInBoardLabel;
  private Disclosure relationsDisclosure;
  private Disclosure descriptionDisclosure;
  private DiscussModeRenderer modeRenderer = new DiscussModeRenderer();
  private final Map<Long, Widget> lastComment = new HashMap<>();
  private final Map<Long, Long> lastAccess = new HashMap<>();

  DiscussionInterceptor() {
    super();
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_ACCESSIBILITY)
        && widget instanceof InputBoolean) {
      isPublic = (InputBoolean) widget;
      isPublic.addValueChangeHandler(event -> {
        boolean value = BeeUtils.toBoolean(event.getValue());
        FormView form = getFormView();
        IsRow row = null;

        if (form != null) {
          row = form.getActiveRow();
        }

        if (membersSelector != null) {
          membersSelector.setEnabled(!value);
          membersSelector.setNullable(value);
        }

        if (membersLabel != null) {
          membersLabel.setStyleName(StyleUtils.NAME_REQUIRED,
              !BeeUtils.toBoolean(event.getValue()));
        }

        if (form != null && row != null && value) {
          row.removeProperty(PROP_MEMBERS);
          form.refreshBySource(PROP_MEMBERS);
        } else if (form != null && row != null) {
          row.setValue(form.getDataIndex(COL_ACCESSIBILITY), (Boolean) null);
          form.refreshBySource(COL_ACCESSIBILITY);
        }
      });
    }

    if (BeeUtils.same(editableWidget.getRowPropertyName(), PROP_MEMBERS)
        && widget instanceof MultiSelector) {
      membersSelector = (MultiSelector) widget;
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(COL_PERMIT_COMMENT, name) && widget instanceof InputBoolean) {
      isPermitComment = (InputBoolean) widget;
      isPermitComment.addValueChangeHandler(event -> {
        boolean value = BeeUtils.toBoolean(event.getValue());
        if (value) {
          doClose();
        } else {
          doActivate();
        }
      });
    }

    if (BeeUtils.same(name, COL_TOPIC) && widget instanceof DataSelector) {
      DataSelector tds = (DataSelector) widget;
      Handler selHandler = event -> {
        if (displayInBoardLabel != null) {
          displayInBoardLabel.setStyleName(StyleUtils.NAME_REQUIRED, DataUtils.isId(event
              .getValue()));
        }
      };

      tds.addSelectorHandler(selHandler);
    }

    if (BeeUtils.same(name, WIDGET_FALABEL_REPLY) && widget instanceof FaLabel) {
      FaLabel replyf = (FaLabel) widget;
      replyf.addClickHandler(event -> doComment(getActiveRow(), null));
    }

    if (BeeUtils.same(name, ALS_OWNER_PHOTO) && widget instanceof Image) {
      discussOwnerPhoto = (Image) widget;
      discussOwnerPhoto.setUrl(PhotoRenderer.DEFAULT_PHOTO_IMAGE);
    }

    if (BeeUtils.same(name, VIEW_DISCUSSIONS_MARK_TYPES) && widget instanceof Flow) {
      markPanel = (Flow) widget;
    }

    if (BeeUtils.same(name, AdministrationConstants.TBL_RELATIONS) && widget instanceof Relations) {
      relations = (Relations) widget;
      relations.setSelectorHandler(event -> {
        FormView form = getFormView();

        if (form == null) {
          return;
        }

        IsRow row = form.getActiveRow();

        if (row == null) {
          return;
        }

        final Map<String, String> discussParameters =
            DiscussionsUtils.getDiscussionsParameters(row);

        if (discussParameters == null) {
          setEnabled(form, row, false);
          return;
        }

        final Long owner = row.getLong(form.getDataIndex(COL_OWNER));

        setEnabled(form, row, isEventEnabled(form, row, DiscussionEvent.MODIFY,
                DiscussionHelper.getStatus(row), owner, discussParameters.get(PRM_DISCUSS_ADMIN)));
      });
    }

    if (BeeUtils.same(name, WIDGET_LABEL_MEMBERS) && widget instanceof Label) {
      membersLabel = (Label) widget;
    }

    if (BeeUtils.same(name, WIDGET_LABEL_DISPLAY_IN_BOARD) && widget instanceof Label) {
      displayInBoardLabel = (Label) widget;
    }

    if (BeeUtils.same(name, WIDGET_RELATIONS_DISCLOSURE) && widget instanceof Disclosure) {
      relationsDisclosure = (Disclosure) widget;
    }

    if (BeeUtils.same(name, WIDGET_DESCRIPTION_DISCLOSURE) && widget instanceof Disclosure) {
      descriptionDisclosure = (Disclosure) widget;
    }
  }

  @Override
  public void afterRefresh(final FormView form, final IsRow row) {
    final HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (row == null) {
      return;
    }

    final Long owner = row.getLong(form.getDataIndex(COL_OWNER));
    DiscussionHelper.setFormCaption(form, row);

    final Map<String, String> discussParameters = DiscussionsUtils.getDiscussionsParameters(row);
    header.clearCommandPanel();

    if (discussParameters == null) {
      setEnabled(form, row, false);
      return;
    }

    boolean modifyEnabled = false;
    String adminLogin = discussParameters.get(PRM_DISCUSS_ADMIN);

    for (final DiscussionEvent event : DiscussionEvent.values()) {
      String label = event.getCommandLabel();

      boolean enabled = isEventEnabled(form, row, event,
              DiscussionHelper.getStatus(row), owner, adminLogin);

      if (event == DiscussionEvent.MODIFY) {
        modifyEnabled = enabled;
      }

      if (!BeeUtils.isEmpty(label) && enabled) {
        header.addCommandItem((IdentifiableWidget)
                DiscussionHelper.renderAction(event, null,
                        mouseEvent -> doEvent(form, row, event, adminLogin)));
      }
    }

    setEnabled(form, row, modifyEnabled);

    if (membersSelector != null) {
      membersSelector.setEnabled(!BeeUtils.unbox(row.getBoolean(form.getDataIndex(
          COL_ACCESSIBILITY))));
    }

    if (markPanel != null) {
      createMarkPanel(markPanel, form, row, null);
    }

    if (isPermitComment != null) {
      boolean closed =
          BeeUtils.unbox(row.getInteger(form.getDataIndex(COL_STATUS))) == DiscussionStatus.CLOSED
              .ordinal();
      isPermitComment.setValue(BeeUtils.toString(closed));
    }

    Long topicId = form.getLongValue(COL_TOPIC);
    if (displayInBoardLabel != null) {
      displayInBoardLabel.setStyleName(StyleUtils.NAME_REQUIRED, DataUtils.isId(topicId));
    }

    if (discussOwnerPhoto != null) {
      discussOwnerPhoto.setUrl(PhotoRenderer.getPhotoUrl(form.getStringValue(ALS_OWNER_PHOTO)));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DiscussionInterceptor();
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    lastComment.remove(newRow.getId());
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }

    final Collection<RowChildren> relData = new ArrayList<>();

    if (relations != null) {
      BeeUtils.overwrite(relData, relations.getRowChildren(true));
    }

    if (event.isEmpty() && DiscussionsUtils.sameMembers(oldRow, newRow)
        && relData.isEmpty()) {
      return;
    }

    event.consume();

    boolean isTopic = false;

    DataSelector wTopic = (DataSelector) getFormView().getWidgetBySource(
        COL_TOPIC);

    if (wTopic != null) {
      isTopic =
          DataUtils.isId(BeeUtils.toLongOrNull(wTopic.getValue()));
    }

    if (isTopic) {
      String validFromVal = null;
      String validToVal = null;

      InputDateTime wVisibleFrom = (InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_FROM);
      InputDateTime wVisibleTo = (InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_TO);

      if (wVisibleFrom != null) {
        validFromVal = wVisibleFrom.getValue();
      }

      if (wVisibleTo != null) {
        validToVal = wVisibleTo.getValue();
      }

      Long validFrom = null;
      if (!BeeUtils.isEmpty(validFromVal)) {
        validFrom = TimeUtils.parseTime(validFromVal);
      }

      Long validTo = null;
      if (!BeeUtils.isEmpty(validToVal)) {
        validTo = TimeUtils.parseTime(validToVal);
      }

      if (!DiscussionHelper.validateDates(validFrom, validTo, event.getCallback())) {
        return;
      }
    }

    ParameterList params = createParams(DiscussionEvent.MODIFY, null);

    sendRequest(params, new RpcCallback<ResponseObject>() {

      @Override
      public void onSuccess(ResponseObject result) {
        final BeeRow data = getResponseRow(DiscussionEvent.MODIFY.getCaption(), result, this);

        if (data == null) {
          event.getCallback().onFailure(BeeUtils.joinWords(DiscussionEvent.MODIFY.getCaption(),
              Localized.dictionary().noData()));
          return;
        }

        event.getCallback().onSuccess(data);

        if (!relData.isEmpty()) {
          Queries.updateChildren(VIEW_DISCUSSIONS, data.getId(), relData, new RowCallback() {

            @Override
            public void onSuccess(BeeRow relResult) {
              event.getCallback().onSuccess(data);
              RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS, data);
              // DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);
            }
          });
        } else {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS, data);
        }
      }
    });
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    boolean maybeAdmin = false;
    Map<String, String> parameters = DiscussionsUtils.getDiscussionsParameters(row);
    if (!BeeUtils.isEmpty(parameters)) {
      maybeAdmin = DiscussionHelper.isDiscussionAdmin(parameters.get(PRM_DISCUSS_ADMIN));
    }

    if (!DiscussionHelper.isOwner(row) && !isMember(userId, getFormView(), row) && !maybeAdmin
        && !isPublic(getFormView(), row)) {
      getFormView().getViewPresenter().handleAction(Action.CLOSE);
      BeeKeeper.getScreen().notifySevere(Localized.dictionary().discussPrivateDiscussion());
    }

    if (!DataUtils.isNewRow(row) && relations != null) {
      relations.requery(null, row.getId());
      relations.refresh();
    }
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row, ScheduledCommand focusCommand) {
    setLastAccess(row.getId(), modeRenderer.getLastAccess(row, userId));
    ensureMembersSelector(form, row);
    BeeRow visitedRow = DataUtils.cloneRow(row);

    BeeRowSet rowSet = new BeeRowSet(form.getViewName(), form.getDataColumns());
    rowSet.addRow(visitedRow);

    ParameterList params = DiscussionsKeeper.createDiscussionRpcParameters(DiscussionEvent.VISIT);
    params.addDataItem(VAR_DISCUSSION_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_DISCUSSION_USERS, getDiscussionMembers(form, row));

    sendRequest(params, new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        form.updateRow(row, true);
        form.notifySevere(reason);
        if (focusCommand != null) {
          focusCommand.execute();
        }
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(DiscussionEvent.VISIT.getCaption(), result, this);
        if (data == null) {
          return;
        }

        DataChangeEvent.fireLocalRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);

        Widget fileWidget = form.getWidgetByName(PROP_FILES);
        if (fileWidget instanceof FileGroup) {
          ((FileGroup) fileWidget).clear();
        }

        List<FileInfo> files = FileInfo.restoreCollection(data.getProperty(PROP_FILES));
        if (!files.isEmpty()) {
          if (fileWidget instanceof FileGroup) {
            for (FileInfo file : files) {
              if (file.getRelatedId() == null) {
                ((FileGroup) fileWidget).addFile(file);
              }
            }
          }
        }
        String comments = data.getProperty(PROP_COMMENTS);
        clearCommentsCache(form);

        if (!BeeUtils.isEmpty(comments)) {
          showCommentsAndMarks(form, data, BeeRowSet.restore(comments), files);
        }

        form.updateRow(data, true);
        if (focusCommand != null) {
          focusCommand.execute();
        }
      }
    });
    return false;
  }

  private void createMarkPanel(final Flow flowWidget, final FormView form, final IsRow formRow,
      final Long commentId) {
    flowWidget.clear();

    if (form == null || formRow == null) {
      return;
    }

    final Long owner = formRow.getLong(form.getDataIndex(COL_OWNER));

    BeeRowSet markTypes = DiscussionsUtils.getMarkTypes(formRow);

    if (markTypes != null) {
      flowWidget.clear();
      if (!markTypes.isEmpty()) {
        showDiscussionMarkData(flowWidget, form, formRow, commentId, owner, markTypes);
      }
    }
  }

  private static Widget createCommentCell(String colName, String value) {
    Widget widget = new CustomDiv(STYLE_COMMENT + colName);
    if (!BeeUtils.isEmpty(value)) {
      widget.getElement().setInnerHTML(value);
    }

    return widget;
  }

  private static List<FileInfo> filterCommentFiles(List<FileInfo> input, long commentId) {
    if (input.isEmpty()) {
      return input;
    }

    List<FileInfo> result = new ArrayList<>();

    for (FileInfo file : input) {
      Long id = file.getRelatedId();
      if (id != null && id == commentId) {
        result.add(file);
      }
    }
    return result;
  }

  private static String getDiscussionMembers(FormView form, IsRow row) {
    return DataUtils.buildIdList(DiscussionsUtils.getDiscussionMembers(row, form.getDataColumns()));
  }

  private static BeeRow getResponseRow(String caption, ResponseObject ro, RpcCallback<?> callback) {
    if (!Queries.checkResponse(caption, BeeConst.UNDEF, VIEW_DISCUSSIONS, ro, BeeRow.class,
        callback)) {
      return null;
    }

    BeeRow row = BeeRow.restore((String) ro.getResponse());
    if (row == null && callback != null) {
      callback.onFailure(caption, VIEW_DISCUSSIONS, "cannot restore row");
    }

    return row;
  }

  private static void sendRequest(ParameterList params,
      final RpcCallback<ResponseObject> callback) {

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          if (callback != null) {
            callback.onFailure(response.getErrors());
          }
        } else {
          if (callback != null) {
            callback.onSuccess(response);
          }
        }
      }
    });
  }

  private Widget getLastComment(long discussId) {
    return lastComment.get(discussId) != null ? lastComment.get(discussId) : null;
  }

  private void setLastComment(long discussId, Widget w) {
    lastComment.put(discussId, w);
  }

  private Long getLastAccess(long discussId) {
    return lastAccess.get(discussId) != null ? lastAccess.get(discussId) : null;
  }

  private void setLastAccess(long discussId, Long access) {
    lastAccess.put(discussId, BeeUtils.max(access, getLastAccess(discussId)));
  }

  private void sendRequest(ParameterList params, final DiscussionEvent event) {
    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {

      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(event.getCaption(), result, this);
        if (data != null) {
          onResponse(data);

          if (DiscussionEvent.in(event.ordinal(), FIRE_GLOBAL_EVENTS)) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);
          } else {
            DataChangeEvent.fireLocalRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);
          }
        }
      }

    };

    sendRequest(params, callback);
  }

  private void showComment(IsRow formRow, Flow panel, BeeRow commentRow, List<BeeColumn> columns,
      List<FileInfo> files, int paddingLeft, String allowDelOwnComments,
      String discussAdmin) {

    boolean deleted = BeeUtils.unbox(
        commentRow.getBoolean(DataUtils.getColumnIndex(COL_DELETED, columns)));

    boolean isCommentOwner = userId == BeeUtils.unbox(
        commentRow.getLong(DataUtils.getColumnIndex(COL_PUBLISHER, columns)));

    Long ownerId = formRow.getLong(getFormView().getDataIndex(COL_OWNER));

    Flow container = new Flow();
    container.addStyleName(STYLE_COMMENT_ROW);
    container.addStyleName(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);

    Long lastCommentId = formRow.getPropertyLong(PROP_LAST_COMMENT);

    if (DataUtils.isId(lastCommentId) && lastCommentId == commentRow.getId()) {
      setLastComment(formRow.getId(), container);
    }

    if (paddingLeft * COMMENT_ROW_PADDING_FACTOR <= MAX_COMMENT_ROW_PADDING_LEFT) {
      container.getElement().getStyle().setPaddingLeft(paddingLeft * COMMENT_ROW_PADDING_FACTOR,
          Unit.EM);
    } else {
      container.getElement().getStyle().setPaddingLeft(MAX_COMMENT_ROW_PADDING_LEFT
          * COMMENT_ROW_PADDING_FACTOR, Unit.EM);
    }

    Flow colPhoto = new Flow();
    colPhoto.addStyleName(STYLE_COMMENT_ROW + STYLE_PHOTO);

    if (paddingLeft * COMMENT_ROW_PADDING_FACTOR > MAX_COMMENT_ROW_PADDING_LEFT) {
      colPhoto.addStyleName(STYLE_COMMENT_ROW + STYLE_PHOTO + STYLE_CHATTER);
    }

    renderPhoto(commentRow, columns, colPhoto);

    container.add(colPhoto);

    Flow colPublisher = new Flow();
    colPublisher.addStyleName(STYLE_COMMENT_COL + COL_PUBLISHER);

    String publisher = BeeUtils.joinWords(
        commentRow.getString(DataUtils.getColumnIndex(COL_PUBLISHER_FIRST_NAME, columns)),
        commentRow.getString(DataUtils.getColumnIndex(COL_PUBLISHER_LAST_NAME, columns)));

    if (!BeeUtils.isEmpty(publisher)) {
      colPublisher.add(createCommentCell(COL_PUBLISHER, publisher));
    }

    DateTime publishTime =
        commentRow.getDateTime(DataUtils.getColumnIndex(COL_PUBLISH_TIME, columns));

    if (publishTime != null) {
      colPublisher.add(createCommentCell(COL_PUBLISH_TIME,
              DiscussionHelper.renderDateTime(publishTime)));

      if (BeeUtils.isMore(publishTime.getTime(), getLastAccess(formRow.getId()))) {
        container.addStyleName(STYLE_COMMENT_ROW + PROP_LAST_COMMENT);
      } else {
        container.removeStyleName(STYLE_COMMENT_ROW + PROP_LAST_COMMENT);
      }
    } else {
      container.removeStyleName(STYLE_COMMENT_ROW + PROP_LAST_COMMENT);
    }

    container.add(colPublisher);

    String text = deleted ? commentRow.getString(DataUtils.getColumnIndex(COL_REASON, columns))
        : commentRow.getString(DataUtils.getColumnIndex(COL_COMMENT_TEXT, columns));

    if (!BeeUtils.isEmpty(text)) {
      colPublisher.add(createCommentCell(COL_COMMENT, text));
    }

    if (!files.isEmpty() && !deleted) {
      renderFiles(files, colPublisher);
    }

    Flow colMarks = new Flow();
    colMarks.addStyleName(STYLE_COMMENT_COL + COL_MARK);

    if (!deleted) {
      createMarkPanel(colMarks, getFormView(), formRow, commentRow.getId());
    }

    colPublisher.add(colMarks);

    Flow colActions = new Flow();
    colActions.addStyleName(STYLE_COMMENT_COL + STYLE_ACTIONS);

    if (!deleted
        && isEventEnabled(getFormView(), formRow, DiscussionEvent.REPLY,
            DiscussionHelper.getStatus(formRow), ownerId, false)) {
      renderReply(formRow, commentRow, colActions);
    }

    if (!deleted
        && isEventEnabled(getFormView(), formRow, DiscussionEvent.COMMENT_DELETE,
            DiscussionHelper.getStatus(formRow), ownerId, false, discussAdmin,
            isCommentOwner && BeeUtils.toBoolean(allowDelOwnComments))) {
      renderTrash(commentRow, colActions);
    }

    colMarks.add(colActions);

    panel.add(container);
  }

  private void showAnsweredCommentsAndMarks(IsRow activeRow, Flow panel, List<FileInfo> files,
      Multimap<Long, Long> data, long parent, BeeRowSet rowSet, int paddingLeft,
      String allowDelOwnComments, String discussAdmin) {
    if (data.containsKey(parent)) {
      for (long id : data.get(parent)) {
        BeeRow row = rowSet.getRowById(id);
        showComment(activeRow, panel, row, rowSet.getColumns(), filterCommentFiles(files, row
            .getId()), paddingLeft, allowDelOwnComments, discussAdmin);

        showAnsweredCommentsAndMarks(activeRow, panel, files, data, id, rowSet, paddingLeft + 1,
            allowDelOwnComments, discussAdmin);
      }
    }
  }

  private void setEnabled(FormView form, IsRow row, boolean enabled) {

    boolean hasRelData = false;
    if (form == null) {
      return;
    }

    form.setEnabled(enabled);

    if (relations != null) {
      relations.setEnabled(enabled);

      for (RowChildren children : relations.getRowChildren(true)) {
        if (!BeeUtils.isEmpty(children.getChildrenIds())) {
          hasRelData = true;
          break;
        }
      }
    }

    if (!enabled) {
      if (membersSelector != null) {
        membersSelector.setEnabled(isMember(userId, form, row));
      }

      if (isPublic != null) {
        isPublic.setEnabled(DiscussionHelper.isOwner(row));
        isPublic.setVisible(DiscussionHelper.isOwner(row));

      }
    } else {
      ensureMembersSelector(form, row);
    }

    if (relationsDisclosure != null) {
      relationsDisclosure.setVisible(enabled
          || (hasRelData || !BeeUtils.isEmpty(DataUtils.parseIdList(row
              .getProperty(PROP_MEMBERS))) || DiscussionHelper.isOwner(row)));
    }

    if (descriptionDisclosure != null) {
      descriptionDisclosure.setVisible(enabled || (!BeeUtils.isEmpty(row.getString(form
          .getDataIndex(COL_DESCRIPTION)))));
    }

  }

  private void showCommentsAndMarks(final FormView form, final IsRow formRow,
      final BeeRowSet rowSet,
      final List<FileInfo> files) {
    Widget widget = form.getWidgetByName(VIEW_DISCUSSIONS_COMMENTS);

    if (!(widget instanceof Flow) || DataUtils.isEmpty(rowSet)) {
      return;
    }

    final Flow panel = (Flow) widget;

    Map<String, String> discussParams = DiscussionsUtils.getDiscussionsParameters(formRow);

    panel.clear();

    if (discussParams == null) {
      Global.showError("Error getting parameters ");
      return;
    }

    if (discussParams.isEmpty()) {
      Global.showError("Error getting parameters ");
      return;
    }

    Set<Long> roots = new HashSet<>();
    Multimap<Long, Long> data = HashMultimap.create();

    for (BeeRow row : rowSet.getRows()) {
      Long parent = row.getLong(rowSet.getColumnIndex(COL_PARENT_COMMENT));

      if (parent == null) {
        roots.add(row.getId());
      } else {
        data.put(parent, row.getId());
      }
    }

    for (long id : roots) {
      BeeRow row = rowSet.getRowById(id);
      showComment(formRow, panel, row, rowSet.getColumns(), filterCommentFiles(files, row
          .getId()), INITIAL_COMMENT_ROW_PADDING_LEFT, discussParams
          .get(PRM_ALLOW_DELETE_OWN_COMMENTS), discussParams.get(PRM_DISCUSS_ADMIN));

      showAnsweredCommentsAndMarks(formRow, panel, files, data, id, rowSet,
          INITIAL_COMMENT_ROW_PADDING_LEFT + 1, discussParams.get(PRM_ALLOW_DELETE_OWN_COMMENTS),
          discussParams.get(PRM_DISCUSS_ADMIN));
    }

    if (panel.getWidgetCount() > 0 && DomUtils.isVisible(form.getElement())) {
      final Widget last = getLastComment(formRow.getId()) != null ? getLastComment(formRow.getId())
              : null;
      if (last != null) {
        Scheduler.get().scheduleDeferred(() -> DomUtils.scrollIntoView(last.getElement()));
      }
    }

  }

  private void showDiscussionMarkData(final Flow flowWidget, final FormView form,
      final IsRow formRow, final Long commentId, final Long owner, final BeeRowSet result) {

    final SimpleRowSet markData = DiscussionsUtils.getMarkData(formRow);

    if (markData == null) {
      return;
    }

    if (!BeeUtils.isEmpty(DiscussionsUtils.getMarkStats(commentId, markData))) {
      flowWidget.addStyleName(STYLE_HAS_MARKS);
    } else {
      flowWidget.removeStyleName(STYLE_HAS_MARKS);
    }

    boolean enabled =
        isEventEnabled(form, formRow, DiscussionEvent.MARK, DiscussionHelper.getStatus(formRow),
                owner, false) && !DiscussionsUtils.hasOneMark(userId, commentId, markData);

    int i = 0;
    for (IsRow markRow : result.getRows()) {
      boolean marked =
          DiscussionsUtils.isMarked(markRow.getId(), userId, commentId, markData);
      int markCount =
          DiscussionsUtils.getMarkCount(markRow.getId(), commentId, markData);

      renderMark(flowWidget, result, markRow, commentId, enabled,
          marked, markCount, i++);
    }

    FaLabel imgStats = new FaLabel(FontAwesome.BAR_CHART);
    imgStats.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS);
    imgStats.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_STATS);
    imgStats.setTitle(Localized.dictionary().discussMarkStats());
    imgStats.setVisible(!BeeUtils.isEmpty(DiscussionsUtils.getMarkStats(commentId, markData)));

    imgStats.addClickHandler(event -> {
      List<String> stats = DiscussionsUtils.getMarkStats(commentId, markData);

      if (BeeUtils.isEmpty(stats)) {
        BeeKeeper.getScreen().notifyInfo(Localized.dictionary().noData());
        return;
      }

      Widget label = new CustomDiv();
      label.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS);
      label.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_LABEL + STYLE_STATS);
      label.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_LABEL + STYLE_STATS
          + BeeUtils.unbox(commentId));

      label.getElement().setInnerHTML(
          BeeUtils.join(Document.get().createBRElement().getString(), stats));

      Global.showInfo(label.toString());
    });

    flowWidget.add(imgStats);

  }

  private static void showError(String message) {
    BeeKeeper.getScreen().notifySevere(Localized.dictionary().error(), message);
  }

  private static void clearCommentsCache(FormView form) {
    Widget widget = form.getWidgetByName(VIEW_DISCUSSIONS_COMMENTS);

    if (!(widget instanceof Flow)) {
      return;
    }

    Flow panel = (Flow) widget;
    panel.clear();
  }

  private ParameterList createParams(DiscussionEvent event, BeeRow newRow, String comment) {
    FormView form = getFormView();
    String viewName = form.getViewName();

    IsRow oldRow = form.getOldRow();

    BeeRowSet updated = DataUtils.getUpdated(viewName, form.getDataColumns(), oldRow, newRow,
        form.getChildrenForUpdate());

    if (!DataUtils.isEmpty(updated)) {
      BeeRow updRow = updated.getRow(0);

      for (int i = 0; i < updated.getNumberOfColumns(); i++) {
        int index = form.getDataIndex(updated.getColumnId(i));

        newRow.setValue(index, oldRow.getString(index));
        newRow.preliminaryUpdate(index, updRow.getString(i));
      }
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, form.getDataColumns());
    rowSet.addRow(newRow);

    ParameterList params = DiscussionsKeeper.createDiscussionRpcParameters(event);
    params.addDataItem(VAR_DISCUSSION_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_DISCUSSION_USERS, getDiscussionMembers(form, oldRow));

    if (!BeeUtils.isEmpty(comment)) {
      params.addDataItem(VAR_DISCUSSION_COMMENT, comment);
    }

    return params;
  }

  private ParameterList createParams(DiscussionEvent event, String comment) {
    return createParams(event, getNewRow(), comment);
  }

  private void doActivate() {
    Global.confirm(Localized.dictionary().discussActivationQuestion(),
            () -> {
              BeeRow newRow = getNewRow(DiscussionStatus.ACTIVE);

              ParameterList params = createParams(DiscussionEvent.ACTIVATE, newRow, null);
              sendRequest(params, DiscussionEvent.ACTIVATE);
            });
  }

  private void doClose() {
    Global.confirm(Localized.dictionary().discussCloseQuestion(), () -> {
      BeeRow newRow = getNewRow(DiscussionStatus.CLOSED);
      ParameterList params = createParams(DiscussionEvent.CLOSE, newRow, null);
      sendRequest(params, DiscussionEvent.CLOSE);
    });
  }

  private void doComment(IsRow formRow, final Long replayedCommentId) {
    final CommentDialog dialog =
        new CommentDialog(replayedCommentId == null
            ? Localized.dictionary().discussComment() : Localized.dictionary()
                .discussActionReply());

    final String cid = dialog.addComment(true);
    final String fid = dialog.addFileCollector(formRow);

    dialog.addSaveAction(new ScheduledCommand() {

      @Override
      public void execute() {
        String comment = dialog.getComment(cid);

        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.dictionary().crmEnterComment());
          return;
        }

        final long discussionId = getDiscussionId();

        BeeRow newRow = getNewRow(DiscussionStatus.ACTIVE);

        ParameterList params = createParams(DiscussionEvent.COMMENT, newRow, comment);

        if (replayedCommentId != null) {
          params.addDataItem(VAR_DISCUSSION_PARENT_COMMENT, replayedCommentId);
        }

        final List<FileInfo> files = dialog.getFiles(fid);

        dialog.close();

        sendRequest(params, new RpcCallback<ResponseObject>() {
          @Override
          public void onFailure(String... reason) {
            getFormView().notifySevere(reason);
          }

          @Override
          public void onSuccess(ResponseObject result) {
            BeeRow data = getResponseRow(DiscussionEvent.COMMENT.getCaption(), result, this);
            if (data == null) {
              return;
            }

            onResponse(data);
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS_COMMENTS);
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);

            Long commentId = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_COMMENT));
            if (DataUtils.isId(commentId) && !files.isEmpty()) {
              sendFiles(files, discussionId, commentId);
            }
          }

        });
      }
    });

    dialog.display();
  }

  private void doCommentDelete(final long commentId) {
    Global.confirm(Localized.dictionary().deleteQuestion(), () -> {
      ParameterList params = createParams(DiscussionEvent.COMMENT_DELETE, null);
      params.addDataItem(VAR_DISCUSSION_DELETED_COMMENT, commentId);
      sendRequest(params, DiscussionEvent.COMMENT_DELETE);
    });
  }

  private void doEdit(FormView form, IsRow row) {
    String formName =
        DiscussionHelper.isAnnouncement(form, row) ? FORM_NEW_ANNOUNCEMENT : FORM_NEW_DISCUSSION;

    RowEditor.openForm(formName, form.getViewName(), row, Opener.MODAL,
        new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            if (result != null) {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);
              form.updateRow(result, true);
              onStartEdit(form, result, null);
            }
          }
        });
  }

  private void doEvent(FormView form, IsRow row, DiscussionEvent event, String adminLogin) {
    if (!isEventEnabled(form, row, event, DiscussionHelper.getStatus(row), getOwner(),
            adminLogin)) {
      showError(Localized.dictionary().actionNotAllowed());
    }

    switch (event) {
      case ACTIVATE:
        doActivate();
        break;
      case CLOSE:
        doClose();
        break;
      case COMMENT:
        doComment(row, null);
        break;
      case CREATE:
        break;
      case DEACTIVATE:
        break;
      case MARK:
        // doMark(null, null);
        break;
      case MODIFY:
        doEdit(form, row);
        break;
      case REPLY:
        break;
      case VISIT:
        break;
      case REFRESH:
        onStartEdit(form, row, null);
        break;
      default:
        break;
    }
  }

  private void doMark(Long commentId, Long markId) {
    if (markId == null) {
      return;
    }

    ParameterList params = createParams(DiscussionEvent.MARK, null);
    params.addDataItem(VAR_DISCUSSION_MARK, markId);

    if (DataUtils.isId(commentId)) {
      params.addDataItem(VAR_DISCUSSION_MARKED_COMMENT, commentId);
    }

    sendRequest(params, DiscussionEvent.MARK);
  }

  private void ensureMembersSelector(FormView form, IsRow row) {
    Boolean publicValue = row.getBoolean(form.getDataIndex(COL_ACCESSIBILITY));

    if (membersSelector != null) {
      membersSelector.setEnabled(!BeeUtils.unbox(publicValue));
    }

    if (isPublic != null) {
      isPublic.setEnabled(BeeUtils.unbox(publicValue) || DiscussionHelper.isOwner(row));
    }

    if (BeeUtils.isTrue(publicValue)) {
      row.removeProperty(PROP_MEMBERS);
      form.refreshBySource(PROP_MEMBERS);
    }

    if (!BeeUtils.isEmpty(DataUtils.parseIdList(row.getProperty(PROP_MEMBERS)))) {
      row.setValue(form.getDataIndex(COL_ACCESSIBILITY), (Boolean) null);
      form.refreshBySource(COL_ACCESSIBILITY);
    }
  }

  private long getDiscussionId() {
    return getFormView().getActiveRow().getId();
  }

  private Long getLong(String colName) {
    return getFormView().getActiveRow().getLong(getFormView().getDataIndex(colName));
  }

  private BeeRow getNewRow() {
    return DataUtils.cloneRow(getFormView().getActiveRow());
  }

  private BeeRow getNewRow(DiscussionStatus status) {
    BeeRow row = getNewRow();
    row.setValue(getFormView().getDataIndex(COL_STATUS), status.ordinal());
    return row;
  }

  private Long getOwner() {
    return getLong(COL_OWNER);
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event,
                                 DiscussionStatus status,  Long owner, String adminLogin) {
    return isEventEnabled(form, row, event, status, owner, true, adminLogin, false);
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event,
                                 DiscussionStatus status,  Long owner, boolean showInHeader) {
    return isEventEnabled(form, row, event, status, owner, showInHeader, "", false);
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event,
                                 DiscussionStatus status, Long owner, boolean showInHeader,
                                 String adminLogin, boolean allowDelOwnComments) {

    if (event == null || status == null || owner == null
        || (!isMember(userId, form, row) && !isPublic(form, row)
            && !DiscussionHelper.isDiscussionAdmin(adminLogin))) {

      return false;
    }

    switch (event) {
      case ACTIVATE:
        return (DiscussionStatus.in(status, DiscussionStatus.INACTIVE)
                && DiscussionHelper.isDiscussionAdmin(adminLogin))
            || (DiscussionStatus.in(status, DiscussionStatus.CLOSED, DiscussionStatus.INACTIVE)
            && (DiscussionHelper.isOwner(row) || DiscussionHelper.isDiscussionAdmin(adminLogin)));
      case CLOSE:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && (DiscussionHelper.isOwner(row) || DiscussionHelper.isDiscussionAdmin(adminLogin));
      case COMMENT:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE);
      case COMMENT_DELETE:
        return !showInHeader
            && DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && (DiscussionHelper.isDiscussionAdmin(adminLogin) || allowDelOwnComments);
      case CREATE:
      case CREATE_MAIL:
        return false;
      case DEACTIVATE:
        return false;
      case MARK:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && !showInHeader;
      case MODIFY:
        boolean hasComments =
            BeeUtils.isPositive(row.getPropertyInteger(PROP_COMMENT_COUNT));
        boolean hasMarks = BeeUtils.isPositiveInt(row.getProperty(PROP_MARK_COUNT));
        return (DiscussionHelper.isOwner(row) || DiscussionHelper.isDiscussionAdmin(adminLogin))
                && !(hasComments || hasMarks);
      case REPLY:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && !showInHeader;
      case VISIT:
        return false;
      case REFRESH:
        return true;
    }

    return false;
  }

  private static boolean isMember(Long user, FormView form, IsRow row) {
    if (!DataUtils.isId(user)) {
      return false;
    }

    List<Long> members =
        DiscussionsUtils.getDiscussionMembers(row, form.getDataColumns());

    if (BeeUtils.isEmpty(members)) {
      return false;
    }
    return members.contains(user);
  }

  private static boolean isPublic(FormView form, IsRow row) {
    return BeeUtils.unbox(row.getBoolean(form.getDataIndex(COL_ACCESSIBILITY)));
  }

  private void onResponse(BeeRow data) {
    // BeeKeeper.getBus().fireEvent(new RowUpdateEvent(VIEW_DISCUSSIONS, data));\
    RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS, data);

    FormView form = getFormView();
    setLastAccess(data.getId(), modeRenderer.getLastAccess(data, userId));

    String comments = data.getProperty(PROP_COMMENTS);

    if (!BeeUtils.isEmpty(comments)) {
      List<FileInfo> files = FileInfo.restoreCollection(data.getProperty(PROP_FILES));
      showCommentsAndMarks(form, form.getActiveRow(), BeeRowSet.restore(comments), files);
    }

    form.updateRow(data, true);
  }

  private static void renderFiles(List<FileInfo> files, Flow container) {
    Simple fileContainer = new Simple();
    fileContainer.addStyleName(STYLE_COMMENT_FILES);

    FileGroup fileGroup = new FileGroup();
    fileGroup.addFiles(files);

    fileContainer.setWidget(fileGroup);
    container.add(fileContainer);
  }

  private void renderMark(Flow container, BeeRowSet markTypeRowData, IsRow markTypeRow,
      final Long commentId, final boolean enabled, final boolean marked, final int markCount,
      final int seek) {

    String markName = markTypeRow.getString(markTypeRowData.getColumnIndex(COL_MARK_NAME));
    String markRes = markTypeRow.getString(markTypeRowData.getColumnIndex(COL_MARK_RESOURCE));
    final Long markId = markTypeRow.getId();

    boolean hasImageRes = false;
    if (!BeeUtils.isEmpty(markRes)) {
      if (FontAwesome.parse(markRes) != null) {
        hasImageRes = true;
      }
    }

    Widget imgMark =
        !hasImageRes ? new Button(markName) : new FaLabel(FontAwesome
            .parse(markRes));

    imgMark.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS);

    if (hasImageRes) {
      imgMark.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK);
    }

    if (!enabled) {
      imgMark.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK + STYLE_DISABLED);
      imgMark.addStyleName(StyleUtils.NAME_DISABLED);
    }

    if (marked) {
      imgMark.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK + STYLE_MARKED);
    }

    imgMark.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK + seek);
    imgMark.setTitle(Localized.maybeTranslate(markName));

    if (enabled && imgMark instanceof HasClickHandlers) {
      ((HasClickHandlers) imgMark).addClickHandler(event -> doMark(commentId, markId));
    }

    container.add(imgMark);

    Widget label = new CustomDiv();
    label.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK + STYLE_LABEL);
    label.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS + STYLE_MARK + STYLE_LABEL + seek);
    label.getElement().setInnerHTML(BeeUtils.toString(markCount));
    container.add(label);

  }

  private static void renderPhoto(IsRow commentRow, List<BeeColumn> commentColumns,
      Flow container) {
    Image image = new Image(PhotoRenderer.getPhotoUrl(DataUtils.getString(commentColumns,
        commentRow, COL_PHOTO)));
    image.addStyleName(STYLE_COMMENT + STYLE_PHOTO);
    container.add(image);

  }

  private void renderReply(final IsRow formRow, IsRow commentRow, Flow container) {

    final long commentId = commentRow.getId();

    DiscussionHelper.renderAction(DiscussionEvent.REPLY, container,
            event -> doComment(formRow, commentId), DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS,
            STYLE_COMMENT_COL + STYLE_ACTIONS + STYLE_REPLY);


  }

  private void renderTrash(IsRow commentRow, Flow container) {

    final long commentId = commentRow.getId();

    DiscussionHelper.renderAction(DiscussionEvent.COMMENT_DELETE, container,
            event -> doCommentDelete(commentId), DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS,
            STYLE_COMMENT_COL + STYLE_ACTIONS + STYLE_TRASH);
  }

  private void requeryComments(final long discussionId) {
    ParameterList params = DiscussionsKeeper.createArgs(SVC_GET_DISCUSSION_DATA);
    params.addDataItem(VAR_DISCUSSION_ID, discussionId);

    RpcCallback<ResponseObject> callback = new RpcCallback<ResponseObject>() {
      @Override
      public void onFailure(String... reason) {
        getFormView().notifySevere(reason);
      }

      @Override
      public void onSuccess(ResponseObject result) {
        if (getFormView().getActiveRow().getId() != discussionId) {
          return;
        }

        BeeRow data = getResponseRow(SVC_GET_DISCUSSION_DATA, result, this);
        if (data != null) {
          onResponse(data);
        }
      }
    };

    sendRequest(params, callback);
  }

  private void sendFiles(final List<FileInfo> files, final long discussionId,
      final long commentId) {

    final Holder<Integer> counter = Holder.of(0);

    final List<BeeColumn> columns =
        Data.getColumns(VIEW_DISCUSSIONS_FILES, Lists.newArrayList(COL_DISCUSSION, COL_COMMENT,
            AdministrationConstants.COL_FILE, COL_CAPTION));

    for (final FileInfo fileInfo : files) {
      FileUtils.uploadFile(fileInfo, result -> {
        List<String> values = Lists.newArrayList(BeeUtils.toString(discussionId),
            BeeUtils.toString(commentId), BeeUtils.toString(result.getId()), fileInfo.getCaption());

        Queries.insert(VIEW_DISCUSSIONS_FILES, columns, values, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow row) {
            counter.set(counter.get() + 1);
            if (counter.get() == files.size()) {
              requeryComments(discussionId);
            }
          }
        });
      });
    }
  }
}
