package com.butent.bee.client.data;

import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.ModalForm;
import com.butent.bee.client.dialog.NotificationListener;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.StyleUtils.ScrollBars;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.RowPresenter;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowActionEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Set;

public class RowEditor {

  public static final String DIALOG_STYLE = "bee-EditRow";
  public static final String EDITABLE_RELATION_STYLE = "bee-EditableRelation";
  
  private static final BeeLogger logger = LogUtils.getLogger(RowEditor.class);

  private static final String HAS_DELEGATE = "*";
  
  private static final Set<String> hasEditorDelegates = Sets.newHashSet();
  
  public static String getFormName(String formName, DataInfo dataInfo) {
    if (!BeeUtils.isEmpty(formName)) {
      return formName;
    }
    if (dataInfo == null) {
      return formName;
    }

    if (!BeeUtils.isEmpty(dataInfo.getEditForm())) {
      return dataInfo.getEditForm();
    }
    
    if (hasEditorDelegates.contains(dataInfo.getViewName())) {
      return HAS_DELEGATE;
    } else {
      return null;
    }
  }

  public static boolean openRelatedRow(Relation relation, Long rowId, final boolean modal,
      final RowCallback rowCallback) {
    if (relation == null || !relation.isEditEnabled() || !DataUtils.isId(rowId)) {
      return false;
    }
    
    final String viewName = relation.getViewName();
    final DataInfo dataInfo = Data.getDataInfo(viewName);

    final String formName = getFormName(relation.getEditForm(), dataInfo);
    if (BeeUtils.isEmpty(formName)) {
      return false;
    }
    
    Queries.getRow(viewName, rowId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        openRow(formName, dataInfo, result, modal, null, rowCallback);
      }
    });
    return true;
  }

  public static void openRow(String viewName, BeeRow row, boolean modal) {
    openRow(viewName, row, modal, null, null);
  }

  public static void openRow(String viewName, BeeRow row, boolean modal, RowCallback rowCallback) {
    openRow(viewName, row, modal, null, rowCallback);
  }

  public static void openRow(String viewName, BeeRow row, boolean modal, UIObject target,
      RowCallback rowCallback) {
    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    String formName = getFormName(null, dataInfo);
    openRow(formName, dataInfo, row, modal, target, rowCallback);
  }

  public static void openRow(final String formName, final DataInfo dataInfo, long rowId,
      final boolean modal, final UIObject target, final RowCallback rowCallback) {
    Assert.notNull(dataInfo);
    
    Queries.getRow(dataInfo.getViewName(), rowId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        openRow(formName, dataInfo, result, modal, target, rowCallback);
      }
    });
  }
  
  public static void openRow(String formName, DataInfo dataInfo, BeeRow row,
      boolean modal, UIObject target, RowCallback rowCallback) {
    Assert.notNull(dataInfo);
    Assert.notNull(row);
    
    RowActionEvent event = new RowActionEvent(dataInfo.getViewName(), row, Service.EDIT_ROW);
    BeeKeeper.getBus().fireEvent(event);
    if (event.isConsumed()) {
      return;
    }
    
    if (BeeUtils.isEmpty(formName) || HAS_DELEGATE.equals(formName)) {
      logger.warning(dataInfo.getViewName(), "edit form not specified");
      return;
    }
    
    createForm(formName, dataInfo, row, modal, target, rowCallback);
  }
  
  public static void registerHasDelegate(String viewName) {
    hasEditorDelegates.add(viewName);
  }
  
  private static void createForm(String formName, final DataInfo dataInfo, final BeeRow row,
      final boolean modal, final UIObject target, final RowCallback rowCallback) {

    FormFactory.createFormView(formName, dataInfo.getViewName(), dataInfo.getColumns(), true,
        new FormFactory.FormViewCallback() {
          @Override
          public void onSuccess(FormDescription formDescription, FormView result) {
            if (result != null) {
              result.setEditing(true);
              result.start(null);

              openForm(result, dataInfo, row, modal || Popup.isActive(), target, rowCallback);
            }
          }
        });
  }

  private static void openForm(final FormView formView, final DataInfo dataInfo,
      final IsRow oldRow, final boolean modal, UIObject target, final RowCallback callback) {

    final RowPresenter presenter = new RowPresenter(formView, dataInfo,
        DataUtils.getRowCaption(dataInfo, oldRow));
    final ModalForm dialog =
        modal ? new ModalForm(presenter.getWidget().asWidget(), formView, false, true) : null;

    final CloseCallback close = new CloseCallback() {
      @Override
      public void onClose() {
        closeForm();
        if (callback != null) {
          callback.onCancel();
        }
      }

      @Override
      public void onSave() {
        if (validate(formView)) {
          int upd = update(dataInfo, oldRow, formView.getActiveRow(), formView, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              closeForm();
              if (callback != null) {
                callback.onSuccess(result);
              }
            }
          });

          if (upd == 0) {
            onClose();
          }
        }
      }

      private void closeForm() {
        if (modal) {
          dialog.hide();
        } else {
          BeeKeeper.getScreen().closeWidget(presenter.getWidget());
        }
      }
    };

    presenter.setActionDelegate(new HandlesActions() {
      @Override
      public void handleAction(Action action) {
        if (Action.CLOSE.equals(action)) {
          formView.onClose(close);

        } else if (Action.SAVE.equals(action)) {
          close.onSave();

        } else if (Action.PRINT.equals(action)) {
          Printer.print(presenter);
        }
      }
    });

    if (modal) {
      dialog.setOnSave(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          if (formView.checkOnSave()) {
            presenter.handleAction(Action.SAVE);
          }
        }
      });

      dialog.setOnEscape(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          if (formView.checkOnClose()) {
            presenter.handleAction(Action.CLOSE);
          }
        }
      });

      dialog.addAttachHandler(new AttachEvent.Handler() {
        @Override
        public void onAttachOrDetach(AttachEvent event) {
          if (event.isAttached()) {
            formView.updateRow(DataUtils.cloneRow(oldRow), true);
          }
        }
      });

      if (target == null) {
        dialog.center();
      } else {
        dialog.showRelativeTo(target);
      }

    } else {
      BeeKeeper.getScreen().showWidget(presenter.getWidget(), ScrollBars.NONE, true);
      formView.updateRow(DataUtils.cloneRow(oldRow), true);
    }

    UiHelper.focus(formView.getRootWidget().asWidget());
  }

  private static int update(final DataInfo dataInfo, IsRow oldRow, IsRow newRow,
      final NotificationListener notificationListener, final RowCallback callback) {

    return Queries.update(dataInfo.getViewName(), dataInfo.getColumns(), oldRow, newRow,
        new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            notificationListener.notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
            BeeKeeper.getBus().fireEvent(new RowUpdateEvent(dataInfo.getViewName(), result));
            if (callback != null) {
              callback.onSuccess(result);
            }
          }
        });
  }

  private static boolean validate(FormView formView) {
    return formView.validate(formView, true);
  }

  private RowEditor() {
  }
}
