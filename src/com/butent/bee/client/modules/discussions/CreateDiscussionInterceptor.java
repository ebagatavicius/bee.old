package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.SelectorEvent.Handler;
import com.butent.bee.client.modules.mail.Relations;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionStatus;
import com.butent.bee.shared.modules.discussions.DiscussionsUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

class CreateDiscussionInterceptor extends AbstractFormInterceptor {

  private static final String WIDGET_ACCESSIBILITY = "Accessibility";
  private static final String WIDGET_DESCRIPTION = "Description";
  private static final String WIDGET_SUMMARY = "Summary";
  private static final String WIDGET_LABEL_MEMBERS = "membersLabel";
  private static final String WIDGET_LABEL_DISPLAY_IN_BOARD = "DisplayInBoard";

  private enum FileStoreMode {
    NEW,
    RENAME,
    DELETE
  }

  private class FileUploadConsumer implements Consumer<FileStoreMode> {

    private int newCount;
    private int renameCount;
    private int deleteCount;
    private int okCount;
    private Callback<Integer> callback;

    @Override
    public void accept(FileStoreMode input) {
      Assert.notNull(input);

      switch (input) {
        case NEW:
          Assert.nonNegative(newCount--);
          break;
        case RENAME:
          Assert.nonNegative(renameCount--);
          break;
        case DELETE:
          Assert.nonNegative(deleteCount--);
          break;
      }

      okCount++;

      if ((newCount + renameCount + deleteCount) == 0 && getCallback() != null) {
        getCallback().onSuccess(okCount);
      }

    }

    FileUploadConsumer(int newCount, int renameCount, int deleteCount, Callback<Integer> callback) {
      Assert.nonNegative(newCount);
      Assert.nonNegative(renameCount);
      Assert.nonNegative(deleteCount);

      this.newCount = newCount;
      this.renameCount = renameCount;
      this.deleteCount = deleteCount;
      this.okCount = 0;
      this.callback = callback;
    }

    private Callback<Integer> getCallback() {
      return callback;
    }
  }

  private final Map<Long, List<FileInfo>> oldFiles = new HashMap<>();

  private HasCheckedness mailToggle;
  HasCheckedness wPermitComment;
  private Editor summaryEditor;
  private Editor descriptionEditor;
  private Relations relations;
  private FileCollector fileCollector;

  CreateDiscussionInterceptor() {
    super();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    final FormView form = getFormView();

    if (widget instanceof FileCollector) {
      fileCollector = (FileCollector) widget;
      fileCollector.bindDnd(getFormView());

      final Map<String, String> discussParams =
          DiscussionsUtils.getDiscussionsParameters(form.getActiveRow());

      if (discussParams == null || discussParams.isEmpty()) {
        return;
      }

      fileCollector.addSelectionHandler(event -> {
        FileInfo fileInfo = event.getSelectedItem();

        if (DiscussionsUtils.isFileSizeLimitExceeded(fileInfo.getSize(),
            BeeUtils.toLongOrNull(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)))) {

          BeeKeeper.getScreen().notifyWarning(
              Localized.dictionary().fileSizeExceeded(fileInfo.getSize(),
                  BeeUtils.toLong(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)) * 1024 * 1024),
              "("
                  + fileInfo.getName() + ")");

          fileCollector.clear();
          return;
        }

        if (DiscussionsUtils.isForbiddenExtention(BeeUtils.getSuffix(fileInfo.getName(),
            BeeConst.STRING_POINT), discussParams.get(PRM_FORBIDDEN_FILES_EXTENTIONS))) {

          BeeKeeper.getScreen().notifyWarning(Localized.dictionary().discussInvalidFile(),
              fileInfo.getName());
          fileCollector.clear();
        }
      });
    }

    if (BeeUtils.same(name, WIDGET_ACCESSIBILITY) && widget instanceof InputBoolean) {
      final InputBoolean ac = (InputBoolean) widget;
      ac.setValue(BeeConst.STRING_TRUE);

      ac.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          MultiSelector ms = getMultiSelector(form, PROP_MEMBERS);
          MultiSelector mg = getMultiSelector(form, PROP_MEMBER_GROUP);

          Label lbl = getLabel(form, WIDGET_LABEL_MEMBERS);

          if (lbl != null) {
            lbl.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.toBoolean(ac.getValue()));
          }

          setSelectorState(ms);
          setSelectorState(mg);

        }

        void setSelectorState(MultiSelector ms) {
          boolean checked = BeeUtils.toBoolean(ac.getValue());

          if (ms != null) {
            ms.setEnabled(!checked);
            if (checked) {
              ms.clearValue();
              form.getActiveRow().removeProperty(PROP_MEMBERS);
              form.getActiveRow().removeProperty(PROP_MEMBER_GROUP);
            } else {
              form.getActiveRow().setValue(form.getDataIndex(COL_ACCESSIBILITY), (Boolean) null);
              form.refreshBySource(COL_ACCESSIBILITY);
            }
          }
        }
      });
    }

    if (BeeUtils.same(name, COL_TOPIC) && widget instanceof DataSelector) {
      final DataSelector tds = (DataSelector) widget;
      Handler selHandler = event -> {
        Label label = (Label) getFormView().getWidgetByName(WIDGET_LABEL_DISPLAY_IN_BOARD);
        if (label != null) {
          label.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.isEmpty(tds.getValue()));
        }
      };

      tds.addSelectorHandler(selHandler);
    }

    if (BeeUtils.same(name, PROP_MAIL) && (widget instanceof HasCheckedness)) {
      mailToggle = (HasCheckedness) widget;
    }

    if (BeeUtils.same(name, WIDGET_SUMMARY) && widget instanceof Editor) {
      summaryEditor = (Editor) widget;
    }

    if (BeeUtils.same(name, WIDGET_DESCRIPTION) && widget instanceof Editor) {
      descriptionEditor = (Editor) widget;
    }

    if (BeeUtils.same(name, AdministrationConstants.TBL_RELATIONS) && widget instanceof Relations) {
      relations = (Relations) widget;
    }

    if (BeeUtils.same(name, COL_PERMIT_COMMENT) && widget instanceof HasCheckedness) {
      wPermitComment = (HasCheckedness) widget;
    }

  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    boolean isPublic = true;
    GridView parentGrid = getGridView();

    if (parentGrid != null) {
      String gridKey = parentGrid.getGridKey();

      if (Objects.equals(gridKey, DiscussionsListType.OBSERVED.getSupplierKey())) {
        isPublic = false;
      }
    }

    row.setValue(form.getDataIndex(COL_ACCESSIBILITY), isPublic);
    form.getOldRow().setValue(form.getDataIndex(COL_ACCESSIBILITY), isPublic);
    form.refreshBySource(COL_ACCESSIBILITY);
    MultiSelector ms = getMultiSelector(getFormView(), PROP_MEMBERS);
    MultiSelector mg = getMultiSelector(getFormView(), PROP_MEMBER_GROUP);

    if (ms != null) {
      ms.setEnabled(!isPublic);
    }

    if (mg != null) {
      mg.setEnabled(!isPublic);
    }

  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    FormView form = getFormView();
    IsRow row = getActiveRow();
    if (form != null && row != null) {
      if (descriptionEditor != null) {
        row.setValue(form.getDataIndex(COL_DESCRIPTION),
            descriptionEditor.getValue());
      }

      if (summaryEditor != null) {
        row.setValue(form.getDataIndex(COL_SUMMARY),
            summaryEditor.getValue());
      }
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public void beforeStateChange(State state, boolean modal) {
    if (state == State.OPEN && mailToggle != null && mailToggle.isChecked()) {
      mailToggle.setChecked(false);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CreateDiscussionInterceptor();
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();
    FormView form = getFormView();
    IsRow activeRow = form.getActiveRow();

    BeeRow newRow = fillRowData(form, activeRow, event.getCallback());

    if (newRow == null) {
      return;
    }

    final BeeRowSet rowSet =
        DataUtils.createRowSetForInsert(VIEW_DISCUSSIONS, getFormView().getDataColumns(), newRow,
            null, true);
    ParameterList args = DiscussionsKeeper.createDiscussionRpcParameters(DiscussionEvent.CREATE);
    args.addDataItem(VAR_DISCUSSION_DATA, Codec.beeSerialize(rowSet));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);

        if (response.hasErrors()) {
          event.getCallback().onFailure(response.getErrors());
        } else if (response.hasResponse(BeeRow.class)) {
          BeeRow createdDiscussion = BeeRow.restore(response.getResponseAsString());

          if (createdDiscussion == null) {
            event.getCallback().onFailure(Localized.dictionary().discussNotCreated());
            return;
          }

          createFiles(createdDiscussion.getId(), null);
          final Collection<RowChildren> relData = new ArrayList<>();

          if (relations != null) {
            BeeUtils.overwrite(relData, relations.getRowChildren(true));
          }

          if (!BeeUtils.isEmpty(relData)) {
            Queries.updateChildren(VIEW_DISCUSSIONS, createdDiscussion.getId(), relData,
                new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                event.getCallback().onSuccess(result);
              }
            });
          } else {
            event.getCallback().onSuccess(createdDiscussion);
          }

          String message;

          if (BeeUtils.isEmpty(createdDiscussion.getString(form.getDataIndex(ALS_TOPIC_NAME)))) {
            message = Localized.dictionary().discussCreatedNewDiscussion();
          } else {
            message = Localized.dictionary().discussCreatedNewAnnouncement();
          }

          BeeKeeper.getScreen().notifyInfo(message);

          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);

          ParameterList mailArgs =
              DiscussionsKeeper.createDiscussionRpcParameters(DiscussionEvent.CREATE_MAIL);
          mailArgs.addDataItem(VAR_DISCUSSION_DATA, Codec.beeSerialize(rowSet));
          mailArgs.addDataItem(VAR_DISCUSSION_ID, createdDiscussion.getId());
          BeeKeeper.getRpc().makePostRequest(mailArgs, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject emptyResp) {

            }
          });

        } else {
          event.getCallback().onFailure("Unknown response");
        }
      }
    });
  }

  @Override
  public void onSaveChanges(HasHandlers listener, final SaveChangesEvent event) {
    event.consume();
    FormView form = getFormView();
    IsRow activeRow = form.getActiveRow();

    final BeeRow newRow = fillRowData(form, activeRow, event.getCallback());

    if (newRow == null) {
      return;
    }

    BeeRowSet rowSet =
        DataUtils.getUpdated(form.getViewName(), form.getDataColumns(), event.getOldRow(), newRow,
            form.getChildrenForUpdate());

    if (rowSet == null) {
      createFiles(newRow.getId(), result -> event.getCallback().onSuccess(newRow));
      return;
    }

    Queries.updateRow(rowSet, new RowCallback() {

      @Override
      public void onSuccess(BeeRow result) {
        createFiles(result.getId(), fileCount -> event.getCallback().onSuccess(result));
      }
    });
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    form.updateRow(row, true);

    if (summaryEditor != null) {
      summaryEditor.setValue(row.getString(form.getDataIndex(COL_SUMMARY)));
    }

    if (descriptionEditor != null) {
      descriptionEditor.setValue(form.getStringValue(COL_DESCRIPTION));
    }

    if (fileCollector != null) {
      List<FileInfo> files = FileInfo.restoreCollection(row.getProperty(PROP_FILES));
      fileCollector.addFiles(files);
      List<FileInfo> oldFileList = new ArrayList<>();
      BeeUtils.overwrite(oldFileList, files);
      oldFiles.put(row.getId(), oldFileList);
    }

    if (focusCommand != null) {
      focusCommand.execute();
    }

    return false;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    if (summaryEditor != null) {
      summaryEditor.clearValue();
      summaryEditor.setValue(BeeConst.STRING_EMPTY);
    }

    if (descriptionEditor != null) {
      descriptionEditor.clearValue();
      descriptionEditor.setValue(BeeConst.STRING_EMPTY);
    }
  }

  private static Label getLabel(FormView form, String name) {
    Widget widget = form.getWidgetByName(name);
    return (widget instanceof Label) ? (Label) widget : null;
  }

  private static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
  }

  private void createFiles(final Long discussionId, Callback<Integer> countCallback) {
    List<FileInfo> newFileList = Lists.newArrayList(fileCollector.getFiles());
    List<FileInfo> oldFileList = new ArrayList<>();

    List<FileInfo> uploadList = Lists.newArrayList();
    List<FileInfo> updateList = Lists.newArrayList();
    List<FileInfo> deleteList = Lists.newArrayList();

    BeeUtils.overwrite(oldFileList, oldFiles.get(discussionId));

    for (final FileInfo fileInfo : newFileList) {
      if (DataUtils.isId(fileInfo.getId()) && oldFileList.contains(fileInfo)) {
        oldFileList.remove(fileInfo);
      } else if (DataUtils.isId(fileInfo.getId())) {
        updateList.add(fileInfo);
      } else {
        uploadList.add(fileInfo);
      }
    }

    BeeUtils.overwrite(deleteList, oldFileList);

    if ((uploadList.size() + updateList.size() + deleteList.size()) == 0 && countCallback != null) {
      countCallback.onSuccess(0);
    }

    final FileUploadConsumer consumer =
        new FileUploadConsumer(uploadList.size(), updateList.size(), deleteList.size(),
            countCallback);

    final List<BeeColumn> columns =
        Data.getColumns(VIEW_DISCUSSIONS_FILES, Lists.newArrayList(COL_DISCUSSION,
            AdministrationConstants.COL_FILE, COL_CAPTION));

    for (FileInfo file : uploadList) {

      FileUtils.uploadFile(file, result -> {

        List<String> values =
            Lists.newArrayList(BeeUtils.toString(discussionId), BeeUtils.toString(result),
                file.getCaption());
        Queries.insert(VIEW_DISCUSSIONS_FILES, columns, values, null, new RowCallback() {

          @Override
          public void onSuccess(BeeRow discussFile) {
            consumer.accept(FileStoreMode.NEW);
            RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_DISCUSSIONS_FILES, discussFile);
          }
        });

      });
    }

    for (FileInfo file : updateList) {

      Queries.update(VIEW_DISCUSSIONS_FILES, Filter.and(Filter.equals(COL_DISCUSSION,
          discussionId), Filter.equals(AdministrationConstants.COL_FILE, file.getId())),
          COL_CAPTION, file.getCaption(), new IntCallback() {

            @Override
            public void onSuccess(Integer result) {
              consumer.accept(FileStoreMode.RENAME);
            }
          });
    }

    for (FileInfo file : deleteList) {

      Queries.delete(VIEW_DISCUSSIONS_FILES, Filter.and(
          Filter.equals(COL_DISCUSSION, discussionId), Filter.equals(
              AdministrationConstants.COL_FILE, file.getId())), new IntCallback() {

        @Override
        public void onSuccess(Integer result) {
          consumer.accept(FileStoreMode.DELETE);

        }
      });
    }

    fileCollector.clear();

  }

  private BeeRow fillRowData(FormView form, IsRow activeRow, RowCallback callback) {
    boolean discussPublic =
        BeeUtils.unbox(activeRow.getBoolean(form.getDataIndex(COL_ACCESSIBILITY)));
    boolean discussClosed = false;
    boolean isTopic = false;
    String description = "";
    String summary = "";

    DataSelector wTopic = (DataSelector) getFormView().getWidgetBySource(COL_TOPIC);

    if (wPermitComment != null) {
      discussClosed = wPermitComment.isChecked();
    }

    if (wTopic != null) {
      isTopic = DataUtils.isId(BeeUtils.toLongOrNull(wTopic.getValue()));
    }

    if (isTopic) {
      InputDateTime wVisibleFrom = (InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_FROM);

      InputDateTime wVisibleTo = (InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_TO);

      String validFromVal = null;
      String validToVal = null;

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

      if (!DiscussionHelper.validateDates(validFrom, validTo, callback)) {
        return null;
      }
    }

    if (descriptionEditor != null) {
      description = descriptionEditor.getValue();
    }

    if (summaryEditor != null) {
      summary = summaryEditor.getValue();
    }

    if (!discussPublic && BeeUtils.isEmpty(activeRow.getProperty(PROP_MEMBERS))
        && BeeUtils.isEmpty(activeRow.getProperty(PROP_MEMBER_GROUP))) {
      callback.onFailure(Localized.dictionary().discussSelectMembers());
      return null;
    }

    BeeRow newRow = DataUtils.cloneRow(activeRow);

    if (!BeeUtils.isEmpty(description)) {
      Data.setValue(VIEW_DISCUSSIONS, newRow, COL_DESCRIPTION, description);
    }

    if (!BeeUtils.isEmpty(summary)) {
      Data.setValue(VIEW_DISCUSSIONS, newRow, COL_SUMMARY, summary);
    }

    if (discussPublic) {
      newRow.removeProperty(PROP_MEMBERS);
      newRow.removeProperty(PROP_MEMBER_GROUP);
    }

    newRow.setValue(getFormView().getDataIndex(COL_ACCESSIBILITY), discussPublic);

    if (discussClosed) {
      newRow.setValue(getFormView().getDataIndex(COL_STATUS), DiscussionStatus.CLOSED.ordinal());
    }

    if (mailToggle != null && mailToggle.isChecked()) {
      newRow.setProperty(PROP_MAIL, BooleanValue.S_TRUE);
    }

    return newRow;
  }
}
