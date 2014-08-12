package com.butent.bee.client.presenter;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Callback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormAndHeader;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.Action;

import java.util.Set;

public class GridFormPresenter extends AbstractPresenter implements HasGridView, Printable,
    ParentRowCreator {

  public static final String STYLE_FORM_CONTAINER = "bee-GridFormContainer";
  public static final String STYLE_FORM_HEADER = "bee-GridFormHeader";
  public static final String STYLE_FORM_CAPTION = "bee-GridFormCaption";

  private static final String SUFFIX_EDIT = "-edit";
  private static final String SUFFIX_NEW_ROW = "-newRow";

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

    this.header = createHeader(caption, actions, edit);
    this.container = createContainer(this.header, formView, edit);

    this.container.setViewPresenter(this);

    this.editSave = editSave;
  }

  @Override
  public void createParentRow(final NotificationListener notificationListener,
      final Callback<IsRow> callback) {

    if (gridView.isAdding() && gridView.likeAMotherlessChild()) {
      gridView.ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          gridView.createParentRow(notificationListener, callback);
        }
      });

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
        gridView.formCancel();
        break;

      case CLOSE:
        getForm().onClose(new CloseCallback() {
          @Override
          public void onClose() {
            gridView.formCancel();
          }

          @Override
          public void onSave() {
            handleAction(Action.SAVE);
          }
        });
        break;

      case EDIT:
        gridView.getForm(true).setEnabled(true);
        hideAction(action);
        if (editSave) {
          showAction(Action.SAVE);
        }
        break;

      case SAVE:
        save();
        break;

      case PRINT:
        if (getForm().printHeader()) {
          Printer.print(this);
        } else {
          Printer.print(getForm());
        }
        break;

      default:
    }
    if (interceptor != null) {
      interceptor.afterAction(action, this);
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

  public void setCaption(String caption) {
    header.setCaption(caption);
  }

  public void setMessage(String message) {
    header.setMessage(message);
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

    formContainer.addTopHeightFillHorizontal(headerView.asWidget(), 0, headerView.getHeight());
    formContainer.addTopBottomFillHorizontal(formView.asWidget(), headerView.getHeight(), 0);

    return formContainer;
  }

  private static HeaderView createHeader(String caption, Set<Action> actions, boolean edit) {
    HeaderView formHeader = new HeaderImpl();
    formHeader.asWidget().addStyleName(STYLE_FORM_HEADER);
    formHeader.asWidget().addStyleName(getFormStyle(STYLE_FORM_HEADER, edit));

    formHeader.create(caption, false, false, null, null, actions, Action.NO_ACTIONS,
        Action.NO_ACTIONS);
    formHeader.addCaptionStyle(STYLE_FORM_CAPTION);
    formHeader.addCaptionStyle(getFormStyle(STYLE_FORM_CAPTION, edit));

    return formHeader;
  }

  private void save() {
    final FormView form = getForm();
    if (!form.validate(form, true)) {
      return;
    }

    if (gridView.isAdding() && gridView.likeAMotherlessChild()) {
      gridView.ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          gridView.formConfirm();
        }
      });

    } else {
      gridView.formConfirm();
    }
  }
}
