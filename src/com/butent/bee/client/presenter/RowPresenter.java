package com.butent.bee.client.presenter;

import com.google.gwt.dom.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormAndHeader;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class RowPresenter extends AbstractPresenter implements Printable, SaveChangesEvent.Handler {

  private static final class Container extends FormAndHeader implements HasCaption,
      HasWidgetSupplier {

    private final DataInfo dataInfo;
    private final long rowId;
    private final String initialCaption;

    private Container(DataInfo dataInfo, long rowId, String initialCaption) {
      super();
      this.dataInfo = dataInfo;
      this.rowId = rowId;
      this.initialCaption = initialCaption;
    }

    @Override
    public String getCaption() {
      String caption = DataUtils.getRowCaption(dataInfo, getForm().getActiveRow(),
          Format.getDateRenderer(), Format.getDateTimeRenderer());

      if (BeeUtils.isEmpty(caption)) {
        return BeeUtils.notEmpty(initialCaption, getHeader().getCaption());
      } else {
        return caption;
      }
    }

    @Override
    public String getIdPrefix() {
      return "row-editor";
    }

    @Override
    public String getSupplierKey() {
      return RowEditor.getSupplierKey(dataInfo.getViewName(), rowId);
    }
  }

  public static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX + "RowContainer";
  public static final String STYLE_HEADER = BeeConst.CSS_CLASS_PREFIX + "RowHeader";
  public static final String STYLE_CAPTION = BeeConst.CSS_CLASS_PREFIX + "RowCaption";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.EDITOR);

  private static HeaderView createHeader(String caption, Set<Action> enabledActions,
      Set<Action> disabledActions) {

    HeaderView formHeader = new HeaderImpl();
    formHeader.asWidget().addStyleName(STYLE_HEADER);

    formHeader.create(caption, false, false, null, uiOptions, enabledActions, disabledActions,
        Action.NO_ACTIONS);
    formHeader.addCaptionStyle(STYLE_CAPTION);

    return formHeader;
  }

  private final DataInfo dataInfo;
  private final FormView formView;

  private final Container container;

  private HandlesActions actionDelegate;

  public RowPresenter(FormView formView, DataInfo dataInfo, long rowId, String initialCaption,
      Set<Action> enabledActions, Set<Action> disabledActions) {

    this.formView = formView;
    this.dataInfo = dataInfo;

    Set<Action> ea = new HashSet<>();
    if (!BeeUtils.isEmpty(enabledActions)) {
      ea.addAll(enabledActions);
    }

    if (!BeeUtils.isEmpty(formView.getFavorite())
        && !Global.getFavorites().isBookmarked(formView.getViewName(), rowId)) {
      ea.add(Action.BOOKMARK);
    }

    HeaderView headerView = createHeader(formView.getCaption(), ea, disabledActions);

    this.container = new Container(dataInfo, rowId, initialCaption);
    container.addStyleName(STYLE_CONTAINER);
    container.addStyleName(UiOption.getStyleName(uiOptions));
    container.addStyleName(formView.getContainerStyleName());

    container.addTopHeightFillHorizontal(headerView.asWidget(), 0, headerView.getHeight());
    container.addTopBottomFillHorizontal(formView.asWidget(), headerView.getHeight(), 0);

    container.setViewPresenter(this);

    formView.addSaveChangesHandler(this);
  }

  @Override
  public String getCaption() {
    return container.getCaption();
  }

  @Override
  public HeaderView getHeader() {
    return container.getHeader();
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
    return formView.getSupplierKey();
  }

  @Override
  public void handleAction(Action action) {
    if (getActionDelegate() != null) {
      getActionDelegate().handleAction(action);
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;

    if (container.getId().equals(source.getId())) {
      ElementSize.copyWithAdjustment(source, target, container.getForm().getPrintElement());
      ok = true;
    } else if (getHeader().asWidget().getElement().isOrHasChild(source)) {
      ok = container.getForm().printHeader() && getHeader().onPrint(source, target);
    } else {
      ok = true;
    }

    return ok;
  }

  @Override
  public void onSaveChanges(final SaveChangesEvent event) {
    BeeRowSet updated = DataUtils.getUpdated(dataInfo.getViewName(), event.getOldRow().getId(),
        event.getOldRow().getVersion(), event.getColumns(), event.getOldValues(),
        event.getNewValues(), event.getChildren());

    if (DataUtils.isEmpty(updated) && BeeUtils.isEmpty(event.getChildren())) {
      return;
    }

    RowCallback updateCallback = new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        formView.notifySevere(reason);
      }

      @Override
      public void onSuccess(BeeRow result) {
        RowUpdateEvent.fire(BeeKeeper.getBus(), dataInfo.getViewName(), result);

        if (formView.getFormInterceptor() != null) {
          formView.getFormInterceptor().afterUpdateRow(result);
        }

        if (event.getCallback() != null) {
          event.getCallback().onSuccess(result);
        }
      }
    };

    if (DataUtils.isEmpty(updated)) {
      Queries.updateChildren(dataInfo.getViewName(), event.getOldRow().getId(),
          event.getChildren(),
          updateCallback);
    } else {
      Queries.updateRow(updated, updateCallback);
    }
  }

  public void setActionDelegate(HandlesActions actionDelegate) {
    this.actionDelegate = actionDelegate;
  }

  private HandlesActions getActionDelegate() {
    return actionDelegate;
  }
}
