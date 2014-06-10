package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.event.logical.SelectorEvent.Handler;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
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
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.modules.discussions.DiscussionsUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

class CreateDiscussionInterceptor extends AbstractFormInterceptor {

  private static final String WIDGET_ACCESSIBILITY = "Accessibility";
  private static final String WIDGET_DESCRIPTION = "Description";
  private static final String WIDGET_FILES = "Files";
  private static final String WIDGET_LABEL_MEMBERS = "membersLabel";
  private static final String WIDGET_LABEL_DISPLAY_IN_BOARD = "DisplayInBoard";

  private HasCheckedness mailToggle;

  CreateDiscussionInterceptor() {
    super();
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    Widget widget = getFormView().getWidgetByName(WIDGET_ACCESSIBILITY);
    if (widget instanceof InputBoolean) {
      InputBoolean ac = (InputBoolean) widget;
      getMultiSelector(getFormView(), PROP_MEMBERS).setEnabled(!BeeUtils.toBoolean(ac.getValue()));
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    final FormView form = getFormView();

    if (widget instanceof FileCollector) {
      final FileCollector fc = (FileCollector) widget;
      fc.bindDnd(getFormView());

      final Map<String, String> discussParams =
          DiscussionsUtils.getDiscussionsParameters(form.getActiveRow());

      if (discussParams == null || discussParams.isEmpty()) {
        return;
      }

      fc.addSelectionHandler(new SelectionHandler<NewFileInfo>() {

        @Override
        public void onSelection(SelectionEvent<NewFileInfo> event) {
          NewFileInfo fileInfo = event.getSelectedItem();

          if (DiscussionsUtils.isFileSizeLimitExceeded(fileInfo.getSize(),
              BeeUtils.toLongOrNull(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)))) {

            BeeKeeper.getScreen().notifyWarning(
                Localized.getMessages().fileSizeExceeded(fileInfo.getSize(),
                    BeeUtils.toLong(discussParams.get(PRM_MAX_UPLOAD_FILE_SIZE)) * 1024 * 1024),
                "("
                    + fileInfo.getName() + ")");

            fc.clear();
            return;
          }

          if (DiscussionsUtils.isForbiddenExtention(BeeUtils.getSuffix(fileInfo.getName(),
              BeeConst.STRING_POINT), discussParams.get(PRM_FORBIDDEN_FILES_EXTENTIONS))) {

            BeeKeeper.getScreen().notifyWarning(Localized.getConstants().discussInvalidFile(),
                fileInfo.getName());
            fc.clear();
            return;
          }
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
          Label lbl = getLabel(form, WIDGET_LABEL_MEMBERS);
          boolean checked = BeeUtils.toBoolean(ac.getValue());

          if (lbl != null) {
            lbl.setStyleName(StyleUtils.NAME_REQUIRED, !BeeUtils.toBoolean(ac.getValue()));
          }

          if (ms != null) {
            ms.setEnabled(!checked);
            ms.setNullable(checked);
            
            if (checked) {
              ms.clearValue();
              form.getActiveRow().setProperty(PROP_MEMBERS, null);
            } else {
              form.getActiveRow().setValue(form.getDataIndex(COL_ACCESSIBILITY), (Boolean) null);
            }
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

    if (BeeUtils.same(name, PROP_MAIL) && (widget instanceof HasCheckedness)) {
      mailToggle = (HasCheckedness) widget;
    }
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
    IsRow activeRow = getFormView().getActiveRow();

    boolean discussPublic =
        ((HasCheckedness) getFormView().getWidgetByName(WIDGET_ACCESSIBILITY)).isChecked();

    boolean discussClosed =
        ((HasCheckedness) getFormView().getWidgetByName(COL_PERMIT_COMMENT))
            .isChecked();
    
    boolean isTopic =
        DataUtils.isId(BeeUtils.toLongOrNull(((DataSelector) getFormView().getWidgetBySource(
            COL_TOPIC)).getValue()));

    if (isTopic) {

      String validFromVal = ((InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_FROM)).getValue();

      String validToVal = ((InputDateTime) getFormView().getWidgetBySource(
          COL_VISIBLE_TO)).getValue();

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

    String description = ((Editor) getFormView().getWidgetByName(WIDGET_DESCRIPTION))
        .getValue();

    if (!discussPublic && BeeUtils.isEmpty(activeRow.getProperty(PROP_MEMBERS))) {
      event.getCallback().onFailure(Localized.getConstants().discussSelectMembers());
      return;
    }

    BeeRow newRow = DataUtils.cloneRow(activeRow);

    if (!BeeUtils.isEmpty(description)) {
      Data.setValue(VIEW_DISCUSSIONS, newRow, COL_DESCRIPTION, description);
    }

    if (discussPublic) {
      newRow.setProperty(PROP_MEMBERS, null);
    }

    newRow.setValue(getFormView().getDataIndex(COL_ACCESSIBILITY), discussPublic);

    if (discussClosed) {
      newRow.setValue(getFormView().getDataIndex(COL_STATUS), Integer
          .valueOf(DiscussionStatus.CLOSED.ordinal()));
    }

    if (mailToggle != null && mailToggle.isChecked()) {
      newRow.setProperty(PROP_MAIL, BooleanValue.S_TRUE);
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
        } else if (response.hasResponse(String.class)) {
          List<Long> discussions = DataUtils.parseIdList((String) response.getResponse());

          if (discussions.isEmpty()) {
            event.getCallback().onFailure(Localized.getConstants().discussNotCreated());
            return;
          }

          createFiles(discussions);

          event.getCallback().onSuccess(null);

          String message = Localized.getConstants().discussCreatedNewDiscussion();

          BeeKeeper.getScreen().notifyInfo(message);

          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_DISCUSSIONS);

          for (long discussionId : discussions) {
          ParameterList mailArgs =
                DiscussionsKeeper.createDiscussionRpcParameters(DiscussionEvent.CREATE_MAIL);
            mailArgs.addDataItem(VAR_DISCUSSION_DATA, Codec.beeSerialize(rowSet));
            mailArgs.addDataItem(VAR_DISCUSSION_ID, discussionId);
            BeeKeeper.getRpc().makePostRequest(mailArgs, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject emptyResp) {

              }
            });
          }
        } else {
          event.getCallback().onFailure("Unknown response");
        }
      }
    });
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    Widget widget = getFormView().getWidgetByName(WIDGET_DESCRIPTION);
    if (widget instanceof Editor) {
      Editor editor = (Editor) widget;
      editor.clearValue();
      editor.setValue(BeeConst.STRING_EMPTY);
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

  private void createFiles(final List<Long> discussions) {

    Widget widget = getFormView().getWidgetByName(WIDGET_FILES);

    if (widget instanceof FileCollector && !((FileCollector) widget).isEmpty()) {
      List<NewFileInfo> files = Lists.newArrayList(((FileCollector) widget).getFiles());

      final List<BeeColumn> columns =
          Data.getColumns(VIEW_DISCUSSIONS_FILES, Lists.newArrayList(COL_DISCUSSION,
              AdministrationConstants.COL_FILE, COL_CAPTION));

      for (final NewFileInfo fileInfo : files) {
        FileUtils.uploadFile(fileInfo, new Callback<Long>() {

          @Override
          public void onSuccess(Long result) {
            for (long discussionId : discussions) {
              List<String> values =
                  Lists.newArrayList(BeeUtils.toString(discussionId), BeeUtils.toString(result),
                      fileInfo.getCaption());
              Queries.insert(VIEW_DISCUSSIONS_FILES, columns, values);
            }
          }

        });
      }

      ((FileCollector) widget).clear();
    }
  }

  private static boolean validateDates(Long from, Long to,
      final ReadyForInsertEvent event) {
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
        // } else {
        // event.getCallback().onFailure(
        // BeeUtils.joinWords(Localized.getConstants().displayInBoard(), Localized.getConstants()
        // .invalidDate(), Localized.getConstants().dateToShort()));
        // return false;
      }
    }

    if (from != null && to == null) {
      if (from.longValue() <= now) {
        return true;
        // } else {
        // event.getCallback().onFailure(
        // BeeUtils.joinWords(Localized.getConstants().displayInBoard(), Localized.getConstants()
        // .invalidDate(), Localized.getConstants().dateFromShort()));
        // return false;
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
