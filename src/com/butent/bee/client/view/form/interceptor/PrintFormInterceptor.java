package com.butent.bee.client.view.form.interceptor;

import com.google.common.collect.Lists;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class PrintFormInterceptor extends AbstractFormInterceptor {

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      if (DataUtils.isNewRow(getActiveRow())) {
        return false;
      }
      String print = getFormView().getProperty("reports");

      if (!BeeUtils.isEmpty(print)) {
        String[] reports = BeeUtils.split(print, BeeConst.CHAR_COMMA);
        final List<FormDescription> forms = Lists.newArrayListWithCapacity(reports.length);
        for (int i = 0; i < reports.length; i++) {
          forms.add(null);
        }
        final ChoiceCallback choice = new ChoiceCallback() {
          @Override
          public void onSuccess(int value) {
            FormDescription form = forms.get(value);
            String viewName = form.getViewName();
            IsRow row = getFormView().getActiveRow();

            if (BeeUtils.isEmpty(viewName)
                || BeeUtils.same(viewName, getFormView().getViewName())) {

              RowEditor.openForm(form.getName(), Data.getDataInfo(getFormView().getViewName()),
                  row, Opener.MODAL, null, getPrintFormInterceptor());
            } else {
              RowEditor.openForm(form.getName(), Data.getDataInfo(viewName), row.getId(),
                  Opener.MODAL, null, getPrintFormInterceptor());
            }
          }
        };
        final Holder<Integer> counter = Holder.of(0);

        for (int i = 0; i < reports.length; i++) {
          final int idx = i;
          FormFactory.getFormDescription(reports[i], new Callback<FormDescription>() {
            @Override
            public void onFailure(String... reason) {
              super.onFailure(reason);
              process();
            }

            @Override
            public void onSuccess(FormDescription formDescription) {
              forms.set(idx, formDescription);
              process();
            }

            private void process() {
              counter.set(counter.get() + 1);

              if (counter.get() == forms.size()) {
                List<String> captions = new ArrayList<>();
                List<FormDescription> descriptions = new ArrayList<>();

                for (FormDescription dscr : forms) {
                  if (dscr != null) {
                    captions.add(BeeUtils.notEmpty(Localized.maybeTranslate(dscr.getCaption()),
                        dscr.getName()));
                    descriptions.add(dscr);
                  }
                }
                BeeUtils.overwrite(forms, descriptions);

                if (captions.size() > 1) {
                  Global.choice(null,
                      Localized.getConstants().choosePrintingForm(), captions, choice);

                } else if (captions.size() == 1) {
                  choice.onSuccess(0);
                }
              }
            }
          });
        }
        return false;
      }
    }
    return super.beforeAction(action, presenter);
  }

  public abstract FormInterceptor getPrintFormInterceptor();
}
