package com.butent.bee.client.data;

import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.dialog.ModalForm;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.Previewer.PreviewConsumer;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.presenter.RowPresenter;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetFactory;
import com.butent.bee.client.ui.WidgetSupplier;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.EnumSet;
import java.util.Set;

public final class RowEditor {

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

  public static String getSupplierKey(String viewName, long rowId) {
    Assert.notEmpty(viewName);
    String name = BeeUtils.join(BeeConst.STRING_UNDER, viewName, rowId);
    return WidgetFactory.SupplierKind.ROW_EDITOR.getKey(name);
  }
  
  public static boolean open(String input, final PresenterCallback presenterCallback) {
    Assert.notEmpty(input);
    
    final String viewName = BeeUtils.getPrefix(input, BeeConst.CHAR_UNDER);
    if (BeeUtils.isEmpty(viewName)) {
      return false;
    }
    
    Long id = BeeUtils.toLongOrNull(BeeUtils.getSuffix(input, BeeConst.CHAR_UNDER));
    if (!DataUtils.isId(id)) {
      return false;
    }
    
    Queries.getRow(viewName, id, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        openRow(viewName, result, false, null, null, presenterCallback);
      }
    });
    
    return true;
  }

  public static void openRow(String viewName, IsRow row, boolean modal) {
    openRow(viewName, row, modal, null);
  }

  public static void openRow(String viewName, IsRow row, boolean modal, RowCallback rowCallback) {
    openRow(viewName, row, modal, null, rowCallback,
        modal ? null : PresenterCallback.SHOW_IN_NEW_TAB);
  }

  public static void openRow(String viewName, IsRow row, boolean modal, UIObject target,
      RowCallback rowCallback, PresenterCallback presenterCallback) {
    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    String formName = getFormName(null, dataInfo);
    openRow(formName, dataInfo, row, modal, target, rowCallback, presenterCallback, null);
  }

  public static void openRow(String formName, DataInfo dataInfo, IsRow row, boolean modal,
      UIObject target, RowCallback rowCallback, PresenterCallback presenterCallback,
      FormInterceptor formInteceptor) {

    Assert.notNull(dataInfo);
    Assert.notNull(row);

    if (!RowActionEvent.fireEditRow(dataInfo.getViewName(), row)) {
      return;
    }

    if (BeeUtils.isEmpty(formName) || HAS_DELEGATE.equals(formName)) {
      logger.warning(dataInfo.getViewName(), "edit form not specified");
      return;
    }

    createForm(formName, dataInfo, row, modal, target, rowCallback, presenterCallback,
        formInteceptor);
  }

  public static void openRow(String formName, DataInfo dataInfo, long rowId) {
    openRow(formName, dataInfo, rowId, false, null, null, null);
  }

  public static void openRow(String formName, DataInfo dataInfo, long rowId,
      boolean modal, UIObject target, RowCallback rowCallback, FormInterceptor formInteceptor) {
    Assert.notNull(dataInfo);
    getRow(formName, dataInfo, rowId, modal, target, rowCallback, formInteceptor);
  }

  public static boolean openRow(String viewName, Long rowId, boolean modal,
      RowCallback rowCallback) {
    if (BeeUtils.isEmpty(viewName) || !DataUtils.isId(rowId)) {
      return false;
    }

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return false;
    }

    String formName = getFormName(null, dataInfo);
    if (BeeUtils.isEmpty(formName)) {
      return false;
    }

    getRow(formName, dataInfo, rowId, modal, null, rowCallback, null);
    return true;
  }

  public static void openRow(String formName, String viewName, IsRow row, boolean modal,
      RowCallback rowCallback) {
    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    openRow(formName, dataInfo, row, modal, null, rowCallback, PresenterCallback.SHOW_IN_NEW_TAB,
        null);
  }

  public static void registerHasDelegate(String viewName) {
    hasEditorDelegates.add(viewName);
  }

  private static void createForm(String formName, final DataInfo dataInfo, final IsRow row,
      final boolean modal, final UIObject target, final RowCallback rowCallback,
      final PresenterCallback presenterCallback, FormInterceptor formInterceptor) {

    FormFactory.createFormView(formName, dataInfo.getViewName(), dataInfo.getColumns(), true,
        (formInterceptor == null) ? FormFactory.getFormInterceptor(formName) : formInterceptor,
        new FormFactory.FormViewCallback() {
          @Override
          public void onSuccess(FormDescription formDescription, FormView result) {
            if (result != null) {
              result.setEditing(true);
              result.start(null);
              result.observeData();

              if (!Data.isViewEditable(dataInfo.getViewName())) {
                result.setEnabled(false);
              }

              openForm(formDescription, result, dataInfo, row,
                  modal || Popup.getActivePopup() != null, target, rowCallback, presenterCallback);
            }
          }
        });
  }

  private static void getRow(final String formName, final DataInfo dataInfo, final long rowId,
      final boolean modal, final UIObject target, final RowCallback rowCallback,
      final FormInterceptor formInteceptor) {

    String supplierKey = getSupplierKey(dataInfo.getViewName(), rowId);

    if (!modal && !WidgetFactory.hasSupplier(supplierKey)) {
      WidgetSupplier supplier = new WidgetSupplier() {
        @Override
        public void create(final Callback<IdentifiableWidget> callback) {
          getRow(formName, dataInfo, rowId, modal, null, null, new PresenterCallback() {
            @Override
            public void onCreate(Presenter presenter) {
              callback.onSuccess(presenter.getWidget());
            }
          }, formInteceptor);
        }
      };

      WidgetFactory.registerSupplier(supplierKey, supplier);
    }

    getRow(formName, dataInfo, rowId, modal, target, rowCallback,
        PresenterCallback.SHOW_IN_NEW_TAB, formInteceptor);
  }

  private static void getRow(final String formName, final DataInfo dataInfo, long rowId,
      final boolean modal, final UIObject target, final RowCallback rowCallback,
      final PresenterCallback presenterCallback, final FormInterceptor formInteceptor) {

    Queries.getRow(dataInfo.getViewName(), rowId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        openRow(formName, dataInfo, result, modal, target, rowCallback, presenterCallback,
            formInteceptor);
      }
    });
  }

  private static void openForm(FormDescription formDescription, final FormView formView,
      final DataInfo dataInfo, final IsRow oldRow, final boolean modal, UIObject target,
      final RowCallback callback, PresenterCallback presenterCallback) {

    Set<Action> enabledActions = EnumSet.of(Action.SAVE, Action.PRINT, Action.CLOSE);
    Set<Action> disabledActions = Sets.newHashSet();

    if (formDescription != null) {
      Set<Action> actions = formDescription.getEnabledActions();
      if (!BeeUtils.isEmpty(actions)) {
        disabledActions.addAll(enabledActions);
        disabledActions.removeAll(actions);
        enabledActions.retainAll(actions);
      }

      actions = formDescription.getDisabledActions();
      if (!BeeUtils.isEmpty(actions)) {
        enabledActions.removeAll(actions);
        disabledActions.addAll(actions);
      }
    }
    
    if (!formView.isRowEditable(oldRow, false)) {
      enabledActions.remove(Action.SAVE);
      disabledActions.add(Action.SAVE);
    }

    final RowPresenter presenter = new RowPresenter(formView, dataInfo, oldRow.getId(),
        DataUtils.getRowCaption(dataInfo, oldRow), enabledActions, disabledActions);
    final ModalForm dialog = modal ? new ModalForm(presenter, formView, false) : null;

    final RowCallback closer = new RowCallback() {
      @Override
      public void onCancel() {
        closeForm();
        if (callback != null) {
          callback.onCancel();
        }
      }

      @Override
      public void onSuccess(BeeRow result) {
        closeForm();
        if (callback != null) {
          callback.onSuccess(result);
        }
      }

      private void closeForm() {
        if (modal) {
          dialog.close();
        } else {
          BeeKeeper.getScreen().closeWidget(presenter.getWidget());
        }
      }
    };

    presenter.setActionDelegate(new HandlesActions() {
      @Override
      public void handleAction(Action action) {
        FormInterceptor interceptor = formView.getFormInterceptor();
        if (interceptor != null && !interceptor.beforeAction(action, presenter)) {
          return;
        }

        switch (action) {
          case CANCEL:
            closer.onCancel();
            break;

          case CLOSE:
            formView.onClose(new CloseCallback() {
              @Override
              public void onClose() {
                closer.onCancel();
              }

              @Override
              public void onSave() {
                handleAction(Action.SAVE);
              }
            });
            break;

          case SAVE:
            if (validate(formView)) {
              update(dataInfo, formView.getOldRow(), formView.getActiveRow(), formView, closer);
            }
            break;

          case PRINT:
            if (formView.printHeader()) {
              Printer.print(presenter);
            } else {
              Printer.print(formView);
            }
            break;

          default:
            logger.warning(NameUtils.getName(this), action, "not implemented");
        }

        if (interceptor != null) {
          interceptor.afterAction(action, presenter);
        }
      }
    });

    final ScheduledCommand focusCommand = new ScheduledCommand() {
      @Override
      public void execute() {
        UiHelper.focus(formView.asWidget());
      }
    };

    if (modal) {
      if (enabledActions.contains(Action.SAVE)) {
        dialog.setOnSave(new PreviewConsumer() {
          @Override
          public void accept(NativePreviewEvent input) {
            if (formView.checkOnSave(input)) {
              presenter.handleAction(Action.SAVE);
            }
          }
        });
      }

      if (enabledActions.contains(Action.CLOSE)) {
        dialog.setOnEscape(new PreviewConsumer() {
          @Override
          public void accept(NativePreviewEvent input) {
            if (formView.checkOnClose(input)) {
              presenter.handleAction(Action.CLOSE);
            }
          }
        });
      }

      dialog.addOpenHandler(new OpenEvent.Handler() {
        @Override
        public void onOpen(OpenEvent event) {
          formView.editRow(oldRow, focusCommand);
        }
      });

      if (target == null) {
        dialog.center();
      } else {
        dialog.showRelativeTo(target.getElement());
      }

    } else {
      if (presenterCallback != null) {
        presenterCallback.onCreate(presenter);
      }
      formView.editRow(oldRow, focusCommand);
    }
  }

  private static void update(final DataInfo dataInfo, IsRow oldRow, IsRow newRow,
      final FormView formView, final RowCallback callback) {

    SaveChangesEvent event = SaveChangesEvent.create(oldRow, newRow, dataInfo.getColumns(),
        formView.getChildrenForUpdate(), callback);

    if (!event.isEmpty()) {
      AutocompleteProvider.retainValues(formView);
    }

    if (formView.getFormInterceptor() != null) {
      formView.getFormInterceptor().onSaveChanges(formView, event);
      if (event.isConsumed()) {
        return;
      }
    }

    if (event.isEmpty()) {
      callback.onCancel();
    } else {
      formView.fireEvent(event);
    }
  }

  private static boolean validate(FormView formView) {
    return formView.validate(formView, true);
  }

  private RowEditor() {
  }
}
