package com.butent.bee.client.view.form.interceptor;

import com.google.common.collect.Lists;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class PrintFormInterceptor extends AbstractFormInterceptor {

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      if (DataUtils.isNewRow(getActiveRow())) {
        return saveOnPrintNewRow();
      }
      String[] reports = BeeUtils.split(getFormView().getProperty("reports"), BeeConst.CHAR_COMMA);

      if (!ArrayUtils.isEmpty(reports)) {
        final List<FormDescription> forms = Lists.newArrayListWithCapacity(reports.length);
        for (int i = 0; i < reports.length; i++) {
          forms.add(null);
        }
        final ChoiceCallback choice = value -> {
          if (BeeUtils.isIndex(forms, value)) {
            FormDescription form = forms.get(value);

            String viewName = form.getViewName();
            IsRow row = getFormView().getActiveRow();

            if (BeeUtils.isEmpty(viewName)
                || BeeUtils.same(viewName, getFormView().getViewName())) {

              RowEditor.openForm(form.getName(), Data.getDataInfo(getFormView().getViewName()),
                  row, Opener.MODAL, null, getPrintFormInterceptor());
            } else {
              RowEditor.openForm(form.getName(), Data.getDataInfo(viewName),
                  Filter.compareId(row.getId()), Opener.MODAL, null, getPrintFormInterceptor());
            }
          } else {
            printJasperReport();
          }
        };
        final Holder<Integer> counter = Holder.of(0);

        for (int i = 0; i < reports.length; i++) {
          final int idx = i;
          FormFactory.getFormDescription(reports[i], new Callback<FormDescription>() {
            @Override
            public void onFailure(String... reason) {
              Callback.super.onFailure(reason);
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
                if (!ArrayUtils.isEmpty(getReports())) {
                  captions.add(Localized.dictionary().otherInfo() + "...");
                }
                BeeUtils.overwrite(forms, descriptions);

                if (captions.size() > 1) {
                  Global.choice(null,
                      Localized.dictionary().choosePrintingForm(), captions, choice);

                } else if (captions.size() == 1) {
                  choice.onSuccess(0);
                }
              }
            }
          });
        }
        return false;

      } else if (printJasperReport()) {
        return false;
      }
    }
    return super.beforeAction(action, presenter);
  }

  public FormInterceptor getPrintFormInterceptor() {
    return null;
  }

  protected ReportUtils.ReportCallback getReportCallback() {
    return null;
  }

  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    dataConsumer.accept(null);
  }

  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, String> params = new HashMap<>();

    for (BeeColumn column : getFormView().getDataColumns()) {
      String value = getStringValue(column.getId());

      if (!BeeUtils.isEmpty(value)) {
        params.put(column.getId(), value);
      }
    }
    if (Objects.nonNull(getActiveRow())) {
      params.put("ID", BeeUtils.toString(getActiveRowId()));

      if (!BeeUtils.isEmpty(getActiveRow().getProperties())) {
        getActiveRow().getProperties().forEach((key, value) -> {
          if (!BeeUtils.isEmpty(value)) {
            params.put(key, value);
          }
        });
      }
    }
    parametersConsumer.accept(params);
  }

  protected String[] getReports() {
    return BeeUtils.split(getFormView().getProperty("jasperReports"), BeeConst.CHAR_COMMA);
  }

  protected void print(String report) {
    getReportParameters(parameters ->
        getReportData(data ->
            ReportUtils.showReport(report, getReportCallback(), parameters, data)));
  }

  private boolean printJasperReport() {
    String[] reports = getReports();

    if (ArrayUtils.isEmpty(reports)) {
      return false;
    }
    List<String> reps = new ArrayList<>();
    List<String> caps = new ArrayList<>();

    for (String report : reports) {
      String[] arr = BeeUtils.split(report, BeeConst.CHAR_COLON);
      String rep = arr[0];
      String cap = ArrayUtils.getQuietly(arr, 1);

      if (BeeUtils.isEmpty(cap)) {
        cap = BeeUtils.notEmpty(Localized.translate("report" + rep), rep);
      } else {
        cap = Localized.maybeTranslate(cap);
      }
      reps.add(rep);
      caps.add(cap);
    }
    Consumer<String> consumer = report -> {
      if (BeeUtils.isEmpty(Localized.extractLanguage(report))) {
        Map<String, String> locales = new LinkedHashMap<>();
        Arrays.stream(SupportedLocale.values()).filter(SupportedLocale::isActive)
            .forEach(locale -> locales.put(locale.getLanguage(),
                BeeUtils.notEmpty(locale.getCaption(), locale.getLanguage())));

        if (locales.size() > 1) {
          Global.choice(Localized.dictionary().chooseLanguage(), null,
              new ArrayList<>(locales.values()), idx ->
                  print(Localized.setLanguage(report, new ArrayList<>(locales.keySet()).get(idx))));
        } else {
          print(report);
        }
      } else {
        print(report);
      }
    };
    if (reps.size() > 1) {
      Global.choice(Localized.dictionary().choosePrintingForm(), null, caps,
          idx -> consumer.accept(reps.get(idx)));
    } else {
      consumer.accept(reps.get(0));
    }
    return true;
  }
}
