package com.butent.bee.client.presenter;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormAndHeader;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridFormKind;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GridFormPresenter extends AbstractPresenter implements HasGridView, Printable,
    ParentRowCreator {

  public static final String STYLE_FORM_CONTAINER = BeeConst.CSS_CLASS_PREFIX
      + "GridFormContainer";
  public static final String STYLE_FORM_HEADER = BeeConst.CSS_CLASS_PREFIX + "GridFormHeader";
  public static final String STYLE_FORM_CAPTION = BeeConst.CSS_CLASS_PREFIX + "GridFormCaption";

  private static final String SUFFIX_EDIT = "-edit";
  private static final String SUFFIX_NEW_ROW = "-newRow";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.EDITOR);

  public static String getFormStyle(String base, boolean edit) {
    return base + (edit ? SUFFIX_EDIT : SUFFIX_NEW_ROW);
  }

  private final GridView gridView;

  private final HeaderView header;
  private final FormAndHeader container;

  private final boolean editSave;

  public GridFormPresenter(GridView gridView, FormView formView, String caption,
      Set<Action> actions, boolean edit, boolean editSave) {

    this.gridView = gridView;

    Set<Action> enabledActions = new HashSet<>();
    if (!BeeUtils.isEmpty(actions)) {
      enabledActions.addAll(actions);
    }

    if (edit && !BeeUtils.isEmpty(formView.getFavorite())) {
      enabledActions.add(Action.BOOKMARK);
    }

    this.header = createHeader(caption, enabledActions, edit);
    this.container = createContainer(this.header, formView, edit);

    this.container.setViewPresenter(this);

    this.editSave = editSave;
  }

  @Override
  public void createParentRow(final NotificationListener notificationListener,
      final Callback<IsRow> callback) {

    if (gridView.isAdding() && gridView.likeAMotherlessChild()) {
      gridView.ensureRelId(result -> gridView.createParentRow(notificationListener, callback));
    } else {
      gridView.createParentRow(notificationListener, callback);
    }
  }

  @Override
  public String getCaption() {
    return header.getCaption();
  }

  public FormView getForm() {
    for (Widget child : container) {
      if (child instanceof FormView) {
        return (FormView) child;
      }
    }
    return null;
  }

  public GridInterceptor getGridInterceptor() {
    return gridView.getGridInterceptor();
  }

  @Override
  public GridView getGridView() {
    return gridView;
  }

  @Override
  public HeaderView getHeader() {
    return header;
  }

  @Override
  public View getMainView() {
    return container;
  }

  @Override
  public Element getPrintElement() {
    return getMainView().getElement();
  }

  @Override
  public String getViewKey() {
    return getForm().getSupplierKey();
  }

  @Override
  public void handleAction(Action action) {
    if (action == null) {
      return;
    }

    FormInterceptor interceptor = getForm().getFormInterceptor();
    if (interceptor != null && !interceptor.beforeAction(action, this)) {
      return;
    }

    switch (action) {
      case CANCEL:
        gridView.formCancel(true);
        break;

      case CLOSE:
        getForm().onClose(new CloseCallback() {
          @Override
          public void onClose() {
            gridView.formCancel(true);
          }

          @Override
          public void onSave() {
            handleAction(Action.SAVE);
          }
        });
        break;

      case EDIT:
        gridView.getForm(GridFormKind.EDIT).setEnabled(true);
        hideAction(action);
        if (editSave) {
          showAction(Action.SAVE);
        }
        break;

      case SAVE:
        save(null);
        break;

      case PRINT:
        if (gridView.isAdding() && interceptor != null && interceptor.saveOnPrintNewRow()) {
          maybeSaveAndPrint();
        } else {
          print();
        }
        break;

      case BOOKMARK:
        getForm().bookmark();
        break;

      default:
    }
  }

  public boolean hasAction(Action action) {
    return header.hasAction(action);
  }

  public void hideAction(Action action) {
    header.showAction(action, false);
  }

  public boolean isActionEnabled(Action action) {
    return header.isActionEnabled(action);
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;

    if (container.getId().equals(source.getId())) {
      ElementSize.copyWithAdjustment(source, target, getForm().getPrintElement());
      ok = true;
    } else if (header.asWidget().getElement().isOrHasChild(source)) {
      ok = getForm().printHeader() && header.onPrint(source, target);
    } else {
      ok = true;
    }

    return ok;
  }

  public void save(Consumer<IsRow> consumer) {
    final FormView form = getForm();
    if (!form.validate(form, true)) {
      return;
    }

    if (gridView.isAdding() && gridView.likeAMotherlessChild()) {
      gridView.ensureRelId(result -> gridView.formConfirm(consumer));
    } else {
      gridView.formConfirm(consumer);
    }
  }

  public void setCaption(String caption) {
    header.setCaption(caption);
  }

  public void showAction(Action action) {
    header.showAction(action, true);
  }

  public void showAction(Action action, boolean visible) {
    header.showAction(action, visible);
  }

  public void updateStyle(boolean edit) {
    container.removeStyleName(getFormStyle(STYLE_FORM_CONTAINER, !edit));
    container.addStyleName(getFormStyle(STYLE_FORM_CONTAINER, edit));

    header.removeCaptionStyle(getFormStyle(STYLE_FORM_CAPTION, !edit));
    header.addCaptionStyle(getFormStyle(STYLE_FORM_CAPTION, edit));
  }

  private static FormAndHeader createContainer(HeaderView headerView, FormView formView,
      boolean edit) {

    FormAndHeader formContainer = new FormAndHeader();
    formContainer.addStyleName(STYLE_FORM_CONTAINER);
    formContainer.addStyleName(getFormStyle(STYLE_FORM_CONTAINER, edit));
    formContainer.addStyleName(UiOption.getStyleName(uiOptions));
    formContainer.addStyleName(formView.getContainerStyleName());

    formContainer.addTopHeightFillHorizontal(headerView.asWidget(), 0, headerView.getHeight());
    formContainer.addTopBottomFillHorizontal(formView.asWidget(), headerView.getHeight(), 0);

    return formContainer;
  }

  private static HeaderView createHeader(String caption, Set<Action> actions, boolean edit) {
    HeaderView formHeader = new HeaderImpl();
    formHeader.asWidget().addStyleName(STYLE_FORM_HEADER);
    formHeader.asWidget().addStyleName(getFormStyle(STYLE_FORM_HEADER, edit));

    formHeader.create(caption, false, false, null, uiOptions, actions, Action.NO_ACTIONS,
        Action.NO_ACTIONS);
    formHeader.addCaptionStyle(STYLE_FORM_CAPTION);
    formHeader.addCaptionStyle(getFormStyle(STYLE_FORM_CAPTION, edit));

    return formHeader;
  }

  private void maybeSaveAndPrint() {
    Global.confirm(getCaption(), Icon.QUESTION,
        Collections.singletonList(Localized.dictionary().saveAndPrintQuestion()),
        Localized.dictionary().saveAndPrintAction(), Localized.dictionary().cancel(),
        this::saveAndPrint);
  }

  private void print() {
    if (getForm().printHeader()) {
      Printer.print(this);
    } else {
      Printer.print(getForm());
    }
  }

  private void saveAndPrint() {
    FormInterceptor interceptor = getForm().getFormInterceptor();
    if (interceptor != null && !interceptor.beforeAction(Action.SAVE, this)) {
      return;
    }

    save(row -> {
      if (DomUtils.isVisible(gridView.getGrid())
          && DataUtils.sameId(row, gridView.getActiveRow())) {

        EditStartEvent event = new EditStartEvent(row, gridView.isReadOnly());
        event.setOnFormFocus(form -> form.getViewPresenter().handleAction(Action.PRINT));

        gridView.onEditStart(event);
      }
    });
  }
}
