package com.butent.bee.client.modules.discussions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.event.logical.SelectorEvent.Handler;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.richtext.RichTextEditor;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.HeaderView;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      faSave.addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent arg0) {
          command.execute();
        }
      });

      HtmlTable table = getContainer();
      int row = table.getRowCount();
      int col = 0;

      faSave.setTitle(Action.SAVE.getCaption());

      insertAction(BeeConst.INT_TRUE, faSave);

      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
      table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.CENTER);

      table.getCellFormatter().setColSpan(row, col, 2);
    }

    private String addComment(boolean required) {
      String styleName = STYLE_DIALOG + "commentLabel";
      Label label = new Label(Localized.getConstants().discussComment());
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
      Label label = new Label(Localized.getConstants().discussFiles());
      label.addStyleName(styleName);

      table.setWidget(row, col, label);
      table.getCellFormatter().addStyleName(row, col, styleName + STYLE_CELL);
      col++;

      styleName = STYLE_DIALOG + "fileCollector";
      final FileCollector collector = new FileCollector(new Image(Global.getImages().attachment()));
      collector.addStyleName(styleName);

      final Map<String, String> discussParams = DiscussionsUtils.getDiscussionsParameters(formRow);
      if (discussParams != null) {

        collector.addSelectionHandler(new SelectionHandler<FileInfo>() {
          @Override
          public void onSelection(SelectionEvent<FileInfo> event) {
            FileInfo fileInfo = event.getSelectedItem();

            if (DiscussionsUtils.isFileSizeLimitExceeded(fileInfo.getSize(),
                BeeUtils.toLongOrNull(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)))) {

              BeeKeeper.getScreen().notifyWarning(
                  Localized.getMessages().fileSizeExceeded(fileInfo.getSize(),
                      BeeUtils.toLong(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)) * 1024 * 1024),
                  "("
                      + fileInfo.getName() + ")");

              collector.clear();
              return;
            }

            if (DiscussionsUtils.isForbiddenExtention(BeeUtils.getSuffix(fileInfo.getName(),
                BeeConst.STRING_POINT), discussParams.get(PRM_FORBIDDEN_FILES_EXTENTIONS))) {

              BeeKeeper.getScreen().notifyWarning(Localized.getConstants().discussInvalidFile(),
                  fileInfo.getName());
              collector.clear();
              return;
            }
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
  private static final String STYLE_ACTIONS = "Actions";
  private static final String STYLE_MARKED = "-marked";
  private static final String STYLE_MARK = "-mark";
  private static final String STYLE_STATS = "-stats";
  private static final String STYLE_DISABLED = "-disabled";
  private static final String STYLE_LABEL = "-label";
  private static final String STYLE_REPLY = "-reply";
  private static final String STYLE_TRASH = "-trash";
  private static final String STYLE_CHATTER = "-chatter";

  private static final String WIDGET_DESCRIPTION_EDITOR = COL_DESCRIPTION + "Editor";

  private static final String STYLE_DESCRIPTION_DISABLED = DISCUSSIONS_STYLE_PREFIX
      + COL_DESCRIPTION + STYLE_DISABLED;

  private static final String STYLE_DESCRIPTION_EDITOR_DISABLED = DISCUSSIONS_STYLE_PREFIX
      + WIDGET_DESCRIPTION_EDITOR + STYLE_DISABLED;

  private static final String WIDGET_LABEL_MEMBERS = "membersLabel";
  private static final String WIDGET_LABEL_DISPLAY_IN_BOARD = "DisplayInBoard";

  private static final int INITIAL_COMMENT_ROW_PADDING_LEFT = 0;
  private static final int MAX_COMMENT_ROW_PADDING_LEFT = 5;

  private final List<String> relations = Lists.newArrayList(PROP_COMPANIES, PROP_PERSONS,
      PROP_APPOINTMENTS, PROP_TASKS, PROP_DOCUMENTS);
  private final long userId;

  DiscussionInterceptor() {
    super();
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(COL_ACCESSIBILITY, name) && widget instanceof InputBoolean) {
      final InputBoolean ac = (InputBoolean) widget;
      ac.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          boolean value = BeeUtils.toBoolean(ac.getValue());
          MultiSelector ms = getMultiSelector(getFormView(), PROP_MEMBERS);
          Label label = getLabel(getFormView(), WIDGET_LABEL_MEMBERS);

          if (ms != null) {
            ms.setEnabled(!value);
            ms.setNullable(value);
          }

          if (label != null) {
            label.setStyleName(StyleUtils.NAME_REQUIRED,
                !BeeUtils.toBoolean(ac.getValue()));
          }

          if (value && ms != null) {
            ms.clearValue();
            ms.setValue(null);
            getFormView().getActiveRow().setProperty(PROP_MEMBERS, null);
          } else {
            getFormView().getActiveRow().setValue(getFormView().getDataIndex(COL_ACCESSIBILITY),
                (Boolean) null);
          }
        }
      });
    }

    if (BeeUtils.same(COL_PERMIT_COMMENT, name) && widget instanceof InputBoolean) {
      final InputBoolean pcib = (InputBoolean) widget;
      pcib.addValueChangeHandler(new ValueChangeHandler<String>() {

        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          boolean value = BeeUtils.toBoolean(event.getValue());
          if (value) {
            doClose();
          } else {
            doActivate();
          }
        }
      });
    }

    if (BeeUtils.same(name, COL_TOPIC) && widget instanceof DataSelector) {
      final DataSelector tds = (DataSelector) widget;
      Handler selHandler = new Handler() {

        @Override
        public void onDataSelector(SelectorEvent event) {
          Label label = (Label) getFormView().getWidgetByName(WIDGET_LABEL_DISPLAY_IN_BOARD);
          if (label != null) {
            label.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.isEmpty(tds.getValue()));
          }
        }
      };

      tds.addSelectorHandler(selHandler);
    }

    if (BeeUtils.same(name, COL_DESCRIPTION) && widget != null && getFormView() != null) {
      if (getFormView().isEnabled()) {
        widget.getElement().addClassName(STYLE_DESCRIPTION_DISABLED);
      } else {
        widget.getElement().removeClassName(STYLE_DESCRIPTION_DISABLED);
      }
    }

    if (BeeUtils.same(name, WIDGET_DESCRIPTION_EDITOR) && widget != null && getFormView() != null) {
      if (getFormView().isEnabled()) {
        widget.getElement().removeClassName(STYLE_DESCRIPTION_EDITOR_DISABLED);
      } else {
        widget.getElement().addClassName(STYLE_DESCRIPTION_EDITOR_DISABLED);
      }
    }
  }

  @Override
  public void afterRefresh(final FormView form, final IsRow row) {
    final HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (row == null) {
      return;
    }

    final Integer status = row.getInteger(form.getDataIndex(COL_STATUS));
    final Long owner = row.getLong(form.getDataIndex(COL_OWNER));

    final Map<String, String> discussParameters = DiscussionsUtils.getDiscussionsParameters(row);
    header.clearCommandPanel();

    if (discussParameters == null) {
      return;
    }

    for (final DiscussionEvent event : DiscussionEvent.values()) {
      String label = event.getCommandLabel();

      if (!BeeUtils.isEmpty(label) && isEventEnabled(form, row, event, status, owner,
          discussParameters.get(PRM_DISCUSS_ADMIN))) {
        header.addCommandItem((IdentifiableWidget) createEventButton(form, row, event,
            discussParameters
                .get(PRM_DISCUSS_ADMIN)));
      }
    }

    Widget widget = form.getWidgetBySource(COL_ACCESSIBILITY);

    if (widget instanceof InputBoolean) {
      InputBoolean ac = (InputBoolean) widget;
      boolean val = BeeUtils.toBoolean(ac.getValue());

      MultiSelector wMembers = getMultiSelector(form, PROP_MEMBERS);

      if (wMembers != null) {
        wMembers.setEnabled(!val);
      }
    }

    widget = form.getWidgetByName(VIEW_DISCUSSIONS_MARK_TYPES);
    if (widget instanceof Panel) {
      createMarkPanel((Flow) widget, form, row, null, true);
    }

    widget = form.getWidgetByName(COL_PERMIT_COMMENT);
    if (widget instanceof InputBoolean) {
      InputBoolean pcib = (InputBoolean) widget;
      boolean closed =
          BeeUtils.unbox(row.getInteger(form.getDataIndex(COL_STATUS))) == DiscussionStatus.CLOSED
              .ordinal();
      pcib.setValue(BeeUtils.toString(closed));
    }

    widget = form.getWidgetBySource(COL_TOPIC);
    if (widget instanceof DataSelector) {
      DataSelector tds = (DataSelector) widget;
      Label lbl = (Label) form.getWidgetByName(WIDGET_LABEL_DISPLAY_IN_BOARD);
      if (lbl != null) {
        lbl.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.isEmpty(tds.getValue()));
      }
    }

    widget = form.getWidgetByName(COL_DESCRIPTION);

    if (widget != null) {
      widget.getElement().setInnerHTML(
          row.getString(form.getDataIndex(COL_DESCRIPTION)));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DiscussionInterceptor();
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    IsRow oldRow = event.getOldRow();
    IsRow newRow = event.getNewRow();

    if (oldRow == null || newRow == null) {
      return;
    }

    if (event.isEmpty() && DiscussionsUtils.sameMembers(oldRow, newRow)
        && getUpdatedRelations(oldRow, newRow).isEmpty()) {
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

      if (!validateDates(validFrom, validTo, event)) {
        return;
      }
    }

    Editor wDescription = (Editor) getFormView().getWidgetByName(WIDGET_DESCRIPTION_EDITOR);

    if (wDescription != null && getFormView() != null) {
      /* Don't change description data if form disabled ! */
      if (getFormView().isEnabled()) {
        newRow.setValue(getFormView().getDataIndex(COL_DESCRIPTION), wDescription.getValue());
      } else {
        newRow.setValue(getFormView().getDataIndex(COL_DESCRIPTION),
            oldRow.getValue(getFormView().getDataIndex(COL_DESCRIPTION)));
      }
    }

    ParameterList params = createParams(DiscussionEvent.MODIFY, null);

    sendRequest(params, new RpcCallback<ResponseObject>() {

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(DiscussionEvent.MODIFY.getCaption(), result, this);

        if (data != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS, data);
        }
      }
    });
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row, ScheduledCommand focusCommand) {
    final Long owner = row.getLong(form.getDataIndex(COL_OWNER));

    MultiSelector members = getMultiSelector(form, PROP_MEMBERS);
    Widget accessWidget = form.getWidgetBySource(COL_ACCESSIBILITY);
    InputBoolean accessibility = (InputBoolean) accessWidget;
    final Map<String, String> parameters = DiscussionsUtils.getDiscussionsParameters(row);

    final int discussStatus = BeeUtils.unbox(row.getInteger(form.getDataIndex(COL_STATUS)));

    if (parameters != null) {
      if (isEventEnabled(form, row, DiscussionEvent.MODIFY, discussStatus, owner, parameters
          .get(PRM_DISCUSS_ADMIN))) {
        setEnabled(form, true);
      } else {
        setEnabled(form, false);
      }
    } else {
      setEnabled(form, false);
    }

    if (accessibility != null && members != null) {
      accessibility.setEnabled(isOwner(userId, BeeUtils.unbox(owner)));

      if (!BeeUtils.isEmpty(accessibility.getValue())) {

        if (!BeeUtils.isEmpty(members.getValue())) {
          members.clearDisplay();
          members.clearValue();
          row.setProperty(PROP_MEMBERS, null);
        }
      }
    }

    if (members != null) {
      if (!BeeUtils.isEmpty(members.getValue())) {
        members.setEnabled(isMember(userId, form, row));
        row.setValue(form.getDataIndex(COL_ACCESSIBILITY), (Boolean) null);
      }
    }

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
      }

      @Override
      public void onSuccess(ResponseObject result) {
        BeeRow data = getResponseRow(DiscussionEvent.VISIT.getCaption(), result, this);
        if (data == null) {
          return;
        }

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
        SimpleRowSet markData = DiscussionsUtils.getMarkData(data);
        Map<String, String> disscussParams = DiscussionsUtils.getDiscussionsParameters(data);
        clearCommentsCache(form);

        if (!BeeUtils.isEmpty(comments)) {
          showCommentsAndMarks(form, data, BeeRowSet.restore(comments), files);
          establishAdminFormForEdit(disscussParams, form);
        } else if (markData != null) {
          if (!markData.isEmpty()) {
            establishAdminFormForEdit(disscussParams, form);
          } else {
            establishFormForEdit(disscussParams, form, row, discussStatus, owner);
          }
        } else {
          establishFormForEdit(disscussParams, form, row, discussStatus, owner);
        }

        Widget wDescription = form.getWidgetByName(COL_DESCRIPTION);

        if (wDescription != null) {
          wDescription.getElement().setInnerHTML(
              data.getString(form.getDataIndex(COL_DESCRIPTION)));
        }

        Widget wDescriptionEdit = form.getWidgetByName(WIDGET_DESCRIPTION_EDITOR);

        if (wDescriptionEdit instanceof Editor) {
          ((Editor) wDescriptionEdit).setValue(
              data.getString(form.getDataIndex(COL_DESCRIPTION)));
        }

        form.updateRow(data, true);
        form.refresh();
      }
    });
    return false;
  }

  private Widget createEventButton(final FormView form, final IsRow row,
      final DiscussionEvent event, final String adminLogin) {

    FontAwesome icon = event.getCommandIcon();
    String label = event.getCommandLabel();

    Widget cmd = null;

    if (icon != null) {
      cmd = new FaLabel(icon);
      cmd.setTitle(label);
    } else {
      cmd = new Button(label);
      cmd.setTitle(label);
    }

    if (cmd instanceof HasClickHandlers) {
      ((HasClickHandlers) cmd).addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent mouseEvent) {
          doEvent(form, row, event, adminLogin);
        }
      });
    }

    return cmd;
  }

  private void createMarkPanel(final Flow flowWidget, final FormView form, final IsRow formRow,
      final Long commentId) {
    createMarkPanel(flowWidget, form, formRow, commentId, false);
  }

  private void createMarkPanel(final Flow flowWidget, final FormView form, final IsRow formRow,
      final Long commentId, final boolean renderHeader) {
    flowWidget.clear();

    if (form == null || formRow == null) {
      return;
    }

    final Integer status = formRow.getInteger(form.getDataIndex(COL_STATUS));
    final Long owner = formRow.getLong(form.getDataIndex(COL_OWNER));

    BeeRowSet markTypes = DiscussionsUtils.getMarkTypes(formRow);

    if (markTypes != null) {
      flowWidget.clear();
      if (!markTypes.isEmpty()) {
        showDiscussionMarkData(flowWidget, form, formRow, commentId, owner, status, markTypes,
            renderHeader);
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

  private void establishAdminFormForEdit(Map<String, String> parameters, FormView form) {
    if (parameters != null) {
      if (isAdmin(parameters.get(PRM_DISCUSS_ADMIN))) {
        setEnabled(form, true);
      } else {
        setEnabled(form, false);
      }
    } else {
      setEnabled(form, false);
    }
  }

  private void establishFormForEdit(Map<String, String> parameters, FormView form, IsRow row,
      int discussStatus, Long owner) {
    if (parameters != null) {
      if (isEventEnabled(form, row, DiscussionEvent.MODIFY, discussStatus, owner, parameters
          .get(PRM_DISCUSS_ADMIN))) {
        setEnabled(form, true);
      } else {
        setEnabled(form, false);
      }
    } else {
      setEnabled(form, false);
    }
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

  private static Label getLabel(FormView form, String name) {
    Widget widget = form.getWidgetByName(name);
    return (widget instanceof Label) ? (Label) widget : null;
  }

  private static String getDiscussionMembers(FormView form, IsRow row) {
    return DataUtils.buildIdList(DiscussionsUtils.getDiscussionMembers(row, form.getDataColumns()));
  }

  private static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
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

  private static boolean isOwner(long user, long owner) {
    return user == owner;
  }

  private static boolean isPhoto(BeeRow row, BeeRowSet rowSet) {
    return !BeeUtils.isEmpty(row.getString(rowSet.getColumnIndex(COL_PHOTO)));
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
        }
      }

    };

    sendRequest(params, callback);
  }

  private void showComment(IsRow formRow, Flow panel, BeeRow commentRow, List<BeeColumn> columns,
      List<FileInfo> files, boolean renderPhoto, int paddingLeft, String allowDelOwnComments,
      String discussAdmin) {

    boolean deleted = BeeUtils.unbox(
        commentRow.getBoolean(DataUtils.getColumnIndex(COL_DELETED, columns)));

    boolean isCommentOwner = isOwner(userId, BeeUtils.unbox(
        commentRow.getLong(DataUtils.getColumnIndex(COL_PUBLISHER, columns))));

    Long ownerId = formRow.getLong(getFormView().getDataIndex(COL_OWNER));
    Integer statusId = formRow.getInteger(getFormView().getDataIndex(COL_STATUS));

    Flow container = new Flow();
    container.addStyleName(STYLE_COMMENT_ROW);
    container.addStyleName(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);

    if (paddingLeft <= MAX_COMMENT_ROW_PADDING_LEFT) {
      container.getElement().getStyle().setPaddingLeft(paddingLeft, Unit.EM);
    } else {
      container.getElement().getStyle().setPaddingLeft(MAX_COMMENT_ROW_PADDING_LEFT, Unit.EM);
    }

    Flow colPhoto = new Flow();
    colPhoto.addStyleName(STYLE_COMMENT_ROW + COL_PHOTO);

    if (paddingLeft > MAX_COMMENT_ROW_PADDING_LEFT) {
      colPhoto.addStyleName(STYLE_COMMENT_ROW + COL_PHOTO + STYLE_CHATTER);
    }

    if (renderPhoto) {
      renderPhoto(commentRow, columns, colPhoto);
    }

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
      colPublisher.add(createCommentCell(COL_PUBLISH_TIME, Format.getDefaultDateTimeFormat()
          .format(publishTime)));
    }

    container.add(colPublisher);

    String text = deleted ? commentRow.getString(DataUtils.getColumnIndex(COL_REASON, columns))
        : commentRow.getString(DataUtils.getColumnIndex(COL_COMMENT_TEXT, columns));

    if (!BeeUtils.isEmpty(text)) {
      colPublisher.add(createCommentCell(COL_COMMENT, text));
    }

    if (!files.isEmpty()) {
      renderFiles(files, colPublisher);
    }

    Flow colMarks = new Flow();
    colMarks.addStyleName(STYLE_COMMENT_COL + COL_MARK);
    createMarkPanel(colMarks, getFormView(), formRow, commentRow.getId());
    container.add(colMarks);

    Flow colActions = new Flow();
    colActions.addStyleName(STYLE_COMMENT_COL + STYLE_ACTIONS);

    if (!deleted
        && isEventEnabled(getFormView(), formRow, DiscussionEvent.REPLY, statusId, ownerId,
            false)) {
      renderReply(formRow, commentRow, colActions);
    }

    if (!deleted
        && isEventEnabled(getFormView(), formRow, DiscussionEvent.COMMENT_DELETE, statusId,
            ownerId, false, discussAdmin,
            isCommentOwner && BeeUtils.toBoolean(allowDelOwnComments))) {
      renderTrash(commentRow, colActions);
    }

    container.add(colActions);

    panel.add(container);
  }

  private void showAnsweredCommentsAndMarks(IsRow activeRow, Flow panel, List<FileInfo> files,
      Multimap<Long, Long> data, long parent, BeeRowSet rowSet, int paddingLeft,
      String allowDelOwnComments, String discussAdmin) {
    if (data.containsKey(parent)) {
      for (long id : data.get(parent)) {
        BeeRow row = rowSet.getRowById(id);
        showComment(activeRow, panel, row, rowSet.getColumns(), filterCommentFiles(files, row
            .getId()),
            isPhoto(row, rowSet), paddingLeft, allowDelOwnComments, discussAdmin);

        showAnsweredCommentsAndMarks(activeRow, panel, files, data, id, rowSet, paddingLeft + 1,
            allowDelOwnComments, discussAdmin);
      }
    }
  }

  private void setEnabled(FormView form, boolean enabled) {
    if (form == null) {
      return;
    }

    form.setEnabled(enabled);

    Widget widget = form.getWidgetByName(COL_DESCRIPTION);

    if (widget != null) {
      if (getFormView().isEnabled()) {
        widget.getElement().addClassName(STYLE_DESCRIPTION_DISABLED);
      } else {
        widget.getElement().removeClassName(STYLE_DESCRIPTION_DISABLED);
      }
    }

    widget = form.getWidgetByName(WIDGET_DESCRIPTION_EDITOR);

    if (widget != null) {
      if (getFormView().isEnabled()) {
        widget.getElement().removeClassName(STYLE_DESCRIPTION_EDITOR_DISABLED);
      } else {
        widget.getElement().addClassName(STYLE_DESCRIPTION_EDITOR_DISABLED);
      }
    }

    form.refresh();
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
      Global.showError("Error getting parrameters ");
      return;
    }

    if (discussParams.isEmpty()) {
      Global.showError("Error getting parrameters ");
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
          .getId()),
          isPhoto(row, rowSet), INITIAL_COMMENT_ROW_PADDING_LEFT, discussParams
              .get(PRM_ALLOW_DELETE_OWN_COMMENTS), discussParams.get(PRM_DISCUSS_ADMIN));

      showAnsweredCommentsAndMarks(formRow, panel, files, data, id, rowSet,
          INITIAL_COMMENT_ROW_PADDING_LEFT + 1, discussParams.get(PRM_ALLOW_DELETE_OWN_COMMENTS),
          discussParams.get(PRM_DISCUSS_ADMIN));
    }

    if (panel.getWidgetCount() > 0 && DomUtils.isVisible(form.getElement())) {
      final Widget last = panel.getWidget(panel.getWidgetCount() - 1);
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          DomUtils.scrollIntoView(last.getElement());
        }
      });
    }

  }

  private void showDiscussionMarkData(final Flow flowWidget, final FormView form,
      final IsRow formRow, final Long commentId, final Long owner, final Integer status,
      final BeeRowSet result, final boolean renderHeaderInfo) {

    final SimpleRowSet markData = DiscussionsUtils.getMarkData(formRow);

    if (markData == null) {
      return;
    }

    if (renderHeaderInfo) {
      HeaderView header = form.getViewPresenter().getHeader();
      String caption = form.getCaption();
      caption =
          BeeUtils.joinWords(caption, "[" + Localized.getConstants().discussMarked(),
              DiscussionsUtils.getDiscussMarkCountTotal(markData) + "]");
      header.setCaption(caption);
    }

    boolean enabled =
        isEventEnabled(form, formRow, DiscussionEvent.MARK, status, owner, false)
            && !DiscussionsUtils.hasOneMark(userId, commentId, markData);

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
    imgStats.setTitle(Localized.getConstants().discussMarkStats());

    imgStats.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        List<String> stats = DiscussionsUtils.getMarkStats(commentId, markData);

        if (BeeUtils.isEmpty(stats)) {
          BeeKeeper.getScreen().notifyInfo(Localized.getConstants().noData());
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
      }

    });

    flowWidget.add(imgStats);

  }

  private static void showError(String message) {
    BeeKeeper.getScreen().notifySevere(Localized.getConstants().error(), message);
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
    Global.confirm(Localized.getConstants().discussActivationQuestion(),
        new ConfirmationCallback() {

          @Override
          public void onConfirm() {
            BeeRow newRow = getNewRow(DiscussionStatus.ACTIVE);

            ParameterList params = createParams(DiscussionEvent.ACTIVATE, newRow, null);
            sendRequest(params, DiscussionEvent.ACTIVATE);
          }
        });
  }

  private void doClose() {
    Global.confirm(Localized.getConstants().discussCloseQuestion(), new ConfirmationCallback() {

      @Override
      public void onConfirm() {
        BeeRow newRow = getNewRow(DiscussionStatus.CLOSED);
        ParameterList params = createParams(DiscussionEvent.CLOSE, newRow, null);
        sendRequest(params, DiscussionEvent.CLOSE);
      }
    });
  }

  private void doComment(IsRow formRow, final Long replayedCommentId) {
    final CommentDialog dialog =
        new CommentDialog(replayedCommentId == null
            ? Localized.getConstants().discussComment() : Localized.getConstants()
                .discussActionReply());

    final String cid = dialog.addComment(true);
    final String fid = dialog.addFileCollector(formRow);

    dialog.addSaveAction(new ScheduledCommand() {

      @Override
      public void execute() {
        String comment = dialog.getComment(cid);

        if (BeeUtils.isEmpty(comment)) {
          showError(Localized.getConstants().crmEnterComment());
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

            Long commentId = BeeUtils.toLongOrNull(data.getProperty(PROP_LAST_COMMENT));
            if (DataUtils.isId(commentId) && !files.isEmpty()) {
              sendFiles(files, discussionId, commentId);
            }

            if (getFormView() != null) {
              setEnabled(getFormView(), false);
            }
          }

        });
      }
    });

    dialog.display();
  }

  private void doCommentDelete(final long commentId) {
    Global.confirm(Localized.getConstants().deleteQuestion(), new ConfirmationCallback() {

      @Override
      public void onConfirm() {
        ParameterList params = createParams(DiscussionEvent.COMMENT_DELETE, null);
        params.addDataItem(VAR_DISCUSSION_DELETED_COMMENT, commentId);
        sendRequest(params, DiscussionEvent.COMMENT_DELETE);
      }
    });
  }

  private void doEvent(FormView form, IsRow row, DiscussionEvent event, String adminLogin) {
    if (!isEventEnabled(form, row, event, getStatus(), getOwner(), adminLogin)) {
      showError(Localized.getConstants().actionNotAllowed());
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
        break;
      case REPLY:
        break;
      case VISIT:
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

    if (getFormView() != null) {
      setEnabled(getFormView(), false);
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

  private Integer getStatus() {
    return getFormView().getActiveRow().getInteger(getFormView().getDataIndex(COL_STATUS));
  }

  private List<String> getUpdatedRelations(IsRow oldRow, IsRow newRow) {
    List<String> updatedRelations = new ArrayList<>();

    if (oldRow == null || newRow == null) {
      return updatedRelations;
    }

    for (String relation : relations) {
      if (!DataUtils.sameIdSet(oldRow.getProperty(relation), newRow.getProperty(relation))) {
        updatedRelations.add(relation);
      }
    }
    return updatedRelations;
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event, Integer status,
      Long owner, String adminLogin) {
    return isEventEnabled(form, row, event, status, owner, true, adminLogin, false);
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event, Integer status,
      Long owner, boolean showInHeader) {
    return isEventEnabled(form, row, event, status, owner, showInHeader, "", false);
  }

  private boolean isEventEnabled(FormView form, IsRow row, DiscussionEvent event, Integer status,
      Long owner, boolean showInHeader, String adminLogin, boolean allowDelOwnComments) {

    if (event == null || status == null || owner == null
        || (!isMember(userId, form, row) && !isPublic(form, row) && !isAdmin(adminLogin))) {

      return false;
    }

    switch (event) {
      case ACTIVATE:
        return (DiscussionStatus.in(status, DiscussionStatus.INACTIVE) && isAdmin(adminLogin))
            || (DiscussionStatus.in(status, DiscussionStatus.CLOSED, DiscussionStatus.INACTIVE)
            && (isOwner(userId, owner) || isAdmin(adminLogin)));
      case CLOSE:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && (isOwner(userId, owner) || isAdmin(adminLogin));
      case COMMENT:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE);
      case COMMENT_DELETE:
        return !showInHeader
            && DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && (isAdmin(adminLogin) || allowDelOwnComments);
      case CREATE:
      case CREATE_MAIL:
        return false;
      case DEACTIVATE:
        return false;
      case MARK:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && !showInHeader;
      case MODIFY:
        return isOwner(userId, owner) || isAdmin(adminLogin);
      case REPLY:
        return DiscussionStatus.in(status, DiscussionStatus.ACTIVE, DiscussionStatus.INACTIVE)
            && !showInHeader;
      case VISIT:
        return false;
    }

    return false;
  }

  private static boolean isAdmin(String loginName) {
    if (BeeUtils.isEmpty(loginName)) {
      return false;
    }
    return BeeUtils.same(loginName, BeeKeeper.getUser().getLogin());
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

    String comments = data.getProperty(PROP_COMMENTS);

    if (!BeeUtils.isEmpty(comments)) {
      List<FileInfo> files = FileInfo.restoreCollection(data.getProperty(PROP_FILES));
      showCommentsAndMarks(form, form.getActiveRow(), BeeRowSet.restore(comments), files);
    }
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
        !hasImageRes ? new FaLabel(FontAwesome.THUMBS_O_UP) : new FaLabel(FontAwesome
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
      ((HasClickHandlers) imgMark).addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          doMark(commentId, markId);
        }

      });
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
    String photo =
        commentRow.getString(DataUtils.getColumnIndex(COL_PHOTO, commentColumns));
    if (!BeeUtils.isEmpty(photo)) {
      Image image = new Image(PhotoRenderer.getUrl(photo));
      image.addStyleName(STYLE_COMMENT + COL_PHOTO);
      container.add(image);
    }
  }

  private void renderReply(final IsRow formRow, IsRow commentRow, Flow container) {
    String label = DiscussionEvent.REPLY.getCommandLabel();
    FontAwesome icon = DiscussionEvent.REPLY.getCommandIcon();

    Widget widgetReply;

    if (icon != null) {
      widgetReply = new FaLabel(icon);
      widgetReply.setTitle(label);
    } else {
      widgetReply = new Button(label);
      widgetReply.setTitle(label);
    }
    widgetReply.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS);
    widgetReply.addStyleName(STYLE_COMMENT_COL + STYLE_ACTIONS + STYLE_REPLY);
    widgetReply.setTitle(Localized.getConstants().discussActionReply());

    final long commentId = commentRow.getId();

    if (widgetReply instanceof HasClickHandlers) {
      ((HasClickHandlers) widgetReply).addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          doComment(formRow, commentId);
        }

      });
    }

    container.add(widgetReply);
  }

  private void renderTrash(IsRow commentRow, Flow container) {
    String label = DiscussionEvent.COMMENT_DELETE.getCommandLabel();
    FontAwesome icon = DiscussionEvent.COMMENT_DELETE.getCommandIcon();

    Widget widgetTrash;

    if (icon != null) {
      widgetTrash = new FaLabel(icon);
      widgetTrash.setTitle(label);
    } else {
      widgetTrash = new Button(label);
      widgetTrash.setTitle(label);
    }

    widgetTrash.addStyleName(DISCUSSIONS_STYLE_PREFIX + STYLE_ACTIONS);
    widgetTrash.addStyleName(STYLE_COMMENT_COL + STYLE_ACTIONS + STYLE_TRASH);
    widgetTrash.setTitle(Localized.getConstants().actionDelete());
    final long commentId = commentRow.getId();

    if (widgetTrash instanceof HasClickHandlers) {
      ((HasClickHandlers) widgetTrash).addClickHandler(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
          doCommentDelete(commentId);
        }

      });
    }

    container.add(widgetTrash);
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
      FileUtils.uploadFile(fileInfo, new Callback<Long>() {

        @Override
        public void onSuccess(Long result) {
          List<String> values = Lists.newArrayList(BeeUtils.toString(discussionId),
              BeeUtils.toString(commentId), BeeUtils.toString(result), fileInfo.getCaption());

          Queries.insert(VIEW_DISCUSSIONS_FILES, columns, values, null, new RowCallback() {
            @Override
            public void onSuccess(BeeRow row) {
              counter.set(counter.get() + 1);
              if (counter.get() == files.size()) {
                requeryComments(discussionId);
              }
            }
          });
        }

      });
    }
  }

  private static boolean validateDates(Long from, Long to,
      final SaveChangesEvent event) {
    long now = System.currentTimeMillis();

    if (from == null && to == null) {
      event.getCallback().onFailure(
          BeeUtils.joinWords(Localized.getConstants().displayInBoard(), Localized.getConstants()
              .enterDate()));
      return false;
    }

    if (from == null && to != null) {
      if (to.longValue() >= now) {
        return true;
      }
    }

    if (from != null && to == null) {
      if (from.longValue() <= now) {
        return true;
      }
    }

    if (from != null && to != null) {
      if (from <= to) {
        return true;
      } else {
        event.getCallback().onFailure(
            BeeUtils.joinWords(Localized.getConstants().displayInBoard(),
                Localized.getConstants().crmFinishDateMustBeGreaterThanStart()));
        return false;
      }
    }

    return true;
  }
}
