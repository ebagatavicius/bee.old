package com.butent.bee.client.modules.administration;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

public class NewRoleForm extends AbstractFormInterceptor {

  private UnboundSelector baseRole;

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (baseRole != null) {
      Long baseId = BeeUtils.toLongOrNull(baseRole.getNormalizedValue());

      if (DataUtils.isId(baseId)) {
        ParameterList args = AdministrationKeeper.createArgs(SVC_COPY_RIGHTS);
        args.addDataItem(COL_ROLE, result.getId());
        args.addDataItem(VAR_BASE_ROLE, baseId);
        BeeKeeper.getRpc().makePostRequest(args, (ResponseCallback) null);
      }
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_ROLE) && widget instanceof UnboundSelector) {
      baseRole = (UnboundSelector) widget;
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewRoleForm();
  }

  @Override
  public void onStartNewRow(final FormView form, IsRow oldRow, IsRow newRow) {
    if (baseRole != null) {
      Queries.getRowCount(form.getViewName(), null, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          boolean defaultRequired = BeeUtils.isPositive(result);
          baseRole.setNullable(!defaultRequired);

          Widget w = form.getWidgetByName("RoleCaption");

          if (w != null) {
            w.setStyleName(StyleUtils.NAME_REQUIRED, defaultRequired);
          }
        }
      });
    }
    super.onStartNewRow(form, oldRow, newRow);
  }
}
