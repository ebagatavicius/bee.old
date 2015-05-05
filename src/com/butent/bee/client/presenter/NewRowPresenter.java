package com.butent.bee.client.presenter;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormAndHeader;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class NewRowPresenter extends AbstractPresenter implements ParentRowCreator {

  public static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX + "NewRowContainer";
  public static final String STYLE_HEADER = BeeConst.CSS_CLASS_PREFIX + "NewRowHeader";
  public static final String STYLE_CAPTION = BeeConst.CSS_CLASS_PREFIX + "NewRowCaption";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.EDITOR);

  private static HeaderView createHeader(String caption) {
    HeaderView formHeader = new HeaderImpl();
    formHeader.asWidget().addStyleName(STYLE_HEADER);

    formHeader.create(caption, false, false, null, uiOptions,
        EnumSet.of(Action.SAVE, Action.CLOSE), Action.NO_ACTIONS, Action.NO_ACTIONS);
    formHeader.addCaptionStyle(STYLE_CAPTION);

    return formHeader;
  }

  private final DataInfo dataInfo;

  private final FormAndHeader container;
  private final FormView formView;

  private HandlesActions actionDelegate;

  public NewRowPresenter(FormView formView, DataInfo dataInfo, String caption) {
    this.formView = formView;
    this.dataInfo = dataInfo;

    HeaderView header = createHeader(caption);
    this.container = createContainer(header);

    container.setViewPresenter(this);
  }

  @Override
  public void createParentRow(NotificationListener notificationListener,
      final Callback<IsRow> callback) {

    if (!formView.validate(notificationListener, false)) {
      return;
    }

    IsRow row = formView.getActiveRow();

    if (DataUtils.isNewRow(row)) {
      insert(row, true, new RowCallback() {
        @Override
        public void onFailure(String... reason) {
          if (callback != null) {
            callback.onFailure(reason);
          }
        }

        @Override
        public void onSuccess(BeeRow result) {
          formView.observeData();
          formView.updateRow(result, true);
          if (callback != null) {
            callback.onSuccess(result);
          }
        }
      });
    } else if (callback != null) {
      callback.onSuccess(row);
    }
  }

  @Override
  public String getCaption() {
    for (Widget child : container) {
      if (child instanceof HasCaption) {
        return ((HasCaption) child).getCaption();
      }
    }
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return ViewHelper.getHeader(getMainView().asWidget());
  }

  @Override
  public View getMainView() {
    return container;
  }

  @Override
  public void handleAction(Action action) {
    if (getActionDelegate() != null) {
      getActionDelegate().handleAction(action);
    }
  }

  public void save(final RowCallback callback) {
    if (!formView.validate(formView, true)) {
      return;
    }
    IsRow row = formView.getActiveRow();

    if (DataUtils.isNewRow(row)) {
      insert(row, false, new RowCallback() {
        @Override
        public void onFailure(String... reason) {
          formView.notifySevere(reason);
        }

        @Override
        public void onSuccess(BeeRow result) {
          if (callback != null) {
            callback.onSuccess(result);
          }
        }
      });

    } else {
      int upd = Queries.update(dataInfo.getViewName(), dataInfo.getColumns(), formView.getOldRow(),
          row, formView.getChildrenForUpdate(), new RowCallback() {
            @Override
            public void onFailure(String... reason) {
              formView.notifySevere(reason);
            }

            @Override
            public void onSuccess(BeeRow result) {
              RowUpdateEvent.fire(BeeKeeper.getBus(), dataInfo.getViewName(), result);
              if (callback != null) {
                callback.onSuccess(result);
              }
            }
          });

      if (upd == 0 && callback != null && row instanceof BeeRow) {
        callback.onSuccess((BeeRow) row);
      }
    }
  }

  public void setActionDelegate(HandlesActions actionDelegate) {
    this.actionDelegate = actionDelegate;
  }

  private FormAndHeader createContainer(HeaderView headerView) {
    FormAndHeader formContainer = new FormAndHeader();
    formContainer.addStyleName(STYLE_CONTAINER);
    formContainer.addStyleName(UiOption.getStyleName(uiOptions));

    formContainer.addTopHeightFillHorizontal(headerView.asWidget(), 0, headerView.getHeight());
    formContainer.addTopBottomFillHorizontal(formView.asWidget(), headerView.getHeight(), 0);

    return formContainer;
  }

  private void doInsert(final ReadyForInsertEvent event, final boolean forced) {
    Queries.insert(dataInfo.getViewName(), event.getColumns(), event.getValues(),
        event.getChildren(), new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            if (event.getCallback() == null) {
              formView.notifySevere(reason);
            } else {
              event.getCallback().onFailure(reason);
            }
          }

          @Override
          public void onSuccess(BeeRow result) {
            RowInsertEvent.fire(BeeKeeper.getBus(), dataInfo.getViewName(), result,
                event.getSourceId());

            if (formView.getFormInterceptor() != null) {
              formView.getFormInterceptor().afterInsertRow(result, forced);
            }

            if (event.getCallback() != null) {
              event.getCallback().onSuccess(result);
            }
          }
        });
  }

  private HandlesActions getActionDelegate() {
    return actionDelegate;
  }

  private void insert(IsRow row, boolean forced, RowCallback callback) {
    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (int i = 0; i < dataInfo.getColumnCount(); i++) {
      BeeColumn column = dataInfo.getColumns().get(i);

      String value = row.getString(i);
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (column.isEditable()) {
        columns.add(column);
        values.add(value);
      }
    }

    if (columns.isEmpty()) {
      callback.onFailure("New Row", "all columns cannot be empty");
      return;
    }

    AutocompleteProvider.retainValues(formView);

    Collection<RowChildren> children = formView.getChildrenForInsert();
    ReadyForInsertEvent event = new ReadyForInsertEvent(columns, values, children, callback,
        formView.getId());

    if (formView.getFormInterceptor() != null) {
      formView.getFormInterceptor().onReadyForInsert(formView, event);
      if (event.isConsumed()) {
        return;
      }
    }
    doInsert(event, forced);
  }
}
