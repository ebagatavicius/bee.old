package com.butent.bee.client.modules.ec;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.event.logical.CatchEvent.CatchHandler;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeContainer;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.i18n.Localized;

import java.util.List;

class EcCategoriesForm extends AbstractFormInterceptor implements CatchHandler<IsRow> {

  private TreeContainer treeView;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeContainer) {
      treeView = (TreeContainer) widget;
      treeView.getTreePresenter().removeCatchHandler();
      treeView.addCatchHandler(this);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new EcCategoriesForm();
  }

  @Override
  public void onCatch(final CatchEvent<IsRow> event) {
    if (treeView == null) {
      return;
    }
    final TreePresenter presenter = treeView.getTreePresenter();
    final IsRow source = event.getPacket();
    final IsRow destination = event.getDestination();
    final boolean isConsumable = !event.isConsumed();

    if (isConsumable) {
      event.consume();
    }
    String prompt;
    List<String> actions = Lists.newArrayList(Localized.dictionary().ecCategoryMove());

    if (destination != null) {
      actions.add(Localized.dictionary().ecCategoryMerge());
      prompt = Localized.dictionary().ecCategoryMigrate(presenter.format(source),
          presenter.format(destination));
    } else {
      prompt = Localized.dictionary().ecCategoryMigrate(presenter.format(source),
          Localized.maybeTranslate(presenter.getCaption()));
    }
    Global.choice(presenter.getCaption(), prompt, actions, new ChoiceCallback() {
      @Override
      public void onSuccess(int value) {
        switch (value) {
          case 0:
            if (isConsumable) {
              event.executeScheduled();
            }
            presenter.onCatch(event);
            break;

          case 1:
            ParameterList args = EcKeeper.createArgs(SVC_MERGE_CATEGORY);
            args.addDataItem(COL_TCD_CATEGORY, source.getId());
            args.addDataItem(COL_TCD_CATEGORY_PARENT, destination.getId());

            BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                response.notify(getFormView());

                if (response.hasErrors()) {
                  return;
                }
                treeView.removeItem(source);
                RowDeleteEvent.fire(BeeKeeper.getBus(), presenter.getViewName(), source.getId());
              }
            });
            break;
        }
      }
    });
  }
}
