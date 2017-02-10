package com.butent.bee.client.data;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.ModalForm;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.RowPresenter;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RowEditor {

  public static final String DIALOG_STYLE = BeeConst.CSS_CLASS_PREFIX + "EditRow";
  public static final String EDITABLE_RELATION_STYLE = BeeConst.CSS_CLASS_PREFIX
      + "EditableRelation";

  private static final BeeLogger logger = LogUtils.getLogger(RowEditor.class);

  private static final String HAS_DELEGATE = "*";

  private static final Set<String> hasEditorDelegates = new HashSet<>();
  private static final Map<String, FormNameProvider> formNameProviders = new HashMap<>();

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

    if (hasEditorDelegates.contains(dataInfo.getViewName())
        || formNameProviders.containsKey(dataInfo.getViewName())) {
      return HAS_DELEGATE;
    } else {
      return null;
    }
  }

  public static String getSupplierKey(String viewName, long rowId) {
    Assert.notEmpty(viewName);
    String name = BeeUtils.join(BeeConst.STRING_UNDER, viewName, rowId);
    return ViewFactory.SupplierKind.ROW_EDITOR.getKey(name);
  }

  public static void open(String viewName, IsRow row, Opener opener) {
    open(viewName, row, opener, null);
  }

  public static void open(String viewName, IsRow row, Opener opener, RowCallback rowCallback) {
    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    String formName = getFormName(null, dataInfo);
    openForm(formName, dataInfo, row, opener, rowCallback, null);
  }

  public static boolean open(String viewName, Long rowId, Opener opener) {
    return open(viewName, rowId, opener, null);
  }

  public static boolean open(String viewName, Long rowId, Opener opener, RowCallback rowCallback) {
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

    getRow(formName, dataInfo, Filter.compareId(rowId), opener, rowCallback, null);
    return true;
  }

  public static void openForm(String formName, DataInfo dataInfo, Filter filter, Opener opener) {
    openForm(formName, dataInfo, filter, opener, null, null);
  }

  public static void openForm(String formName, DataInfo dataInfo, Filter filter,
      Opener opener, RowCallback rowCallback) {
    openForm(formName, dataInfo, filter, opener, rowCallback, null);
  }

  public static void openForm(String formName, DataInfo dataInfo, Filter filter,
      Opener opener, RowCallback rowCallback, FormInterceptor formInterceptor) {
    getRow(formName, dataInfo, filter, opener, rowCallback, formInterceptor);
  }

  public static void openForm(String formName, String viewName, IsRow row, Opener opener,
      RowCallback rowCallback) {

    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    openForm(formName, dataInfo, row, opener, rowCallback, null);
  }

  public static void openForm(String formName, DataInfo dataInfo, IsRow row, Opener opener,
      RowCallback rowCallback, FormInterceptor formInterceptor) {

    Assert.notNull(dataInfo);
    Assert.notNull(row);
    Assert.notNull(opener);

    if (!RowActionEvent.fireEditRow(dataInfo.getViewName(), row, opener, formName)) {
      return;
    }

    String fn = formName;

    if (!isValidFormName(formName)) {
      FormNameProvider provider = formNameProviders.get(dataInfo.getViewName());
      if (provider != null) {
        fn = provider.getFormName(dataInfo, row);
      }

      if (!isValidFormName(fn)) {
        logger.warning(dataInfo.getViewName(), "edit form not specified");
        return;
      }
    }

    createForm(fn, dataInfo, row, opener, rowCallback, formInterceptor);
  }

  public static boolean parse(String input, final Opener opener) {
    Assert.notEmpty(input);
    Assert.notNull(opener);

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
        open(viewName, result, opener);
      }
    });

    return true;
  }

  public static void registerHasDelegate(String viewName) {
    if (!BeeUtils.isEmpty(viewName)) {
      hasEditorDelegates.add(viewName);
    }
  }

  public static void registerFormNameProvider(String viewName, FormNameProvider provider) {
    if (!BeeUtils.isEmpty(viewName)) {
      if (provider == null) {
        formNameProviders.remove(viewName);
      } else {
        formNameProviders.put(viewName, provider);
      }
    }
  }

  private static void createForm(String formName, final DataInfo dataInfo, final IsRow row,
      final Opener opener, final RowCallback rowCallback, FormInterceptor formInterceptor) {

    FormFactory.createFormView(formName, dataInfo.getViewName(), dataInfo.getColumns(), true,
        (formInterceptor == null) ? FormFactory.getFormInterceptor(formName) : formInterceptor,
        (formDescription, result) -> {
          if (result != null) {
            result.setEditing(true);
            result.start(null);
            result.observeData();

            if (!Data.isViewEditable(dataInfo.getViewName())) {
              result.setEnabled(false);
            }

            Opener formOpener;
            if (!opener.isModal() && Popup.hasEventPreview()) {
              formOpener = Opener.modal(opener.getOnOpen());
            } else {
              formOpener = opener;
            }

            launch(formDescription, result, dataInfo, row, formOpener, rowCallback);
          }
        });
  }

  private static void getRow(final String formName, final DataInfo dataInfo, Filter filter,
      final Opener opener, final RowCallback rowCallback, final FormInterceptor formInterceptor) {

    Queries.getRow(dataInfo.getViewName(), filter, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        openForm(formName, dataInfo, result, opener, rowCallback, formInterceptor);
      }
    });
  }

  private static boolean isValidFormName(String formName) {
    return !BeeUtils.isEmpty(formName) && !HAS_DELEGATE.equals(formName);
  }

  private static void launch(FormDescription formDescription, final FormView formView,
      final DataInfo dataInfo, final IsRow oldRow, final Opener opener,
      final RowCallback callback) {

    Set<Action> enabledActions = EnumSet.of(Action.SAVE, Action.PRINT, Action.CLOSE);
    Set<Action> disabledActions = new HashSet<>();

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

    if (formView.getFormInterceptor() != null) {
      formView.getFormInterceptor().afterCreatePresenter(presenter);
    }

    final ModalForm dialog = opener.isModal() ? new ModalForm(presenter, formView, false) : null;

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
        if (opener.isModal()) {
          dialog.close();
        } else {
          BeeKeeper.getScreen().closeWidget(presenter.getMainView());
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

          case BOOKMARK:
            formView.bookmark();
            break;

          default:
            logger.warning(NameUtils.getName(this), action, "not implemented");
        }
      }
    });

    final ScheduledCommand focusCommand = () -> {
      formView.focus();

      if (opener.getOnOpen() != null) {
        opener.getOnOpen().accept(formView);
      }
    };

    if (opener.isModal()) {
      if (enabledActions.contains(Action.SAVE)) {
        dialog.setOnSave(input -> {
          if (formView.checkOnSave(input)) {
            presenter.handleAction(Action.SAVE);
          }
        });
      }

      if (enabledActions.contains(Action.CLOSE)) {
        dialog.setOnEscape(input -> {
          if (formView.checkOnClose(input)) {
            presenter.handleAction(Action.CLOSE);
          }
        });
      }

      dialog.addOpenHandler(event -> formView.editRow(oldRow, focusCommand));

      if (opener.getTarget() == null) {
        dialog.center();
      } else {
        dialog.showRelativeTo(opener.getTarget());
      }

    } else {
      if (opener.getPresenterCallback() != null) {
        opener.getPresenterCallback().onCreate(presenter);
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
