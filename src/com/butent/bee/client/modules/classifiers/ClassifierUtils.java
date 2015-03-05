package com.butent.bee.client.modules.classifiers;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.Map;

public final class ClassifierUtils {

  private static final String STYLE_COMPANY = BeeConst.CSS_CLASS_PREFIX + "co-companyInfo";
  private static final String STYLE_COMPANY_ITEM = STYLE_COMPANY + "-item";
  private static final String STYLE_COMPANY_LABEL = STYLE_COMPANY + "-label";

  private static final String KEY_LOCALE = "locale";

  private static final String[] COMPANY_INFO_COLS = new String[] {
      COL_COMPANY_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE,
      COL_ADDRESS, COL_PHONE, COL_MOBILE, COL_FAX, COL_EMAIL_ADDRESS, COL_BANK_ACCOUNT};

  public static void createCompany(final Map<String, String> parameters,
      final NotificationListener notificationListener, final IdCallback callback) {

    Assert.notEmpty(parameters);

    ParameterList args = ClassifierKeeper.createArgs(SVC_CREATE_COMPANY);
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (!BeeUtils.anyEmpty(entry.getKey(), entry.getValue())) {
        args.addDataItem(entry.getKey(), entry.getValue());
      }
    }

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (notificationListener != null) {
          response.notify(notificationListener);
        }

        if (response.hasResponse(Long.class)) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_COMPANIES);
          if (callback != null) {
            callback.onSuccess(response.getResponseAsLong());
          }
        }
      }
    });
  }

  public static void getCompanyInfo(Long companyId, final Widget target) {
    Assert.notNull(target);
    if (!DataUtils.isId(companyId)) {
      return;
    }
    ParameterList args = ClassifierKeeper.createArgs(SVC_COMPANY_INFO);
    args.addDataItem(COL_COMPANY, companyId);
    String locale = DomUtils.getDataProperty(target.getElement(), KEY_LOCALE);

    if (BeeUtils.isEmpty(locale)) {
      locale = Localized.getConstants().languageTag();
    }
    if (!BeeUtils.isEmpty(locale)) {
      args.addDataItem(AdministrationConstants.VAR_LOCALE, locale);
    }
    BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (response.hasErrors()) {
          return;
        }
        Map<String, String> entries = Codec.deserializeMap(response.getResponseAsString());

        if (BeeUtils.isEmpty(entries)) {
          return;
        }
        Map<String, Pair<String, String>> info = new HashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
          info.put(entry.getKey(), Pair.restore(entry.getValue()));
        }
        Flow flow = new Flow(STYLE_COMPANY);

        for (String col : COMPANY_INFO_COLS) {
          Flow record = new Flow(STYLE_COMPANY + "-" + col.toLowerCase());

          switch (col) {
            case COL_ADDRESS:
              String value = BeeUtils.joinItems(info.get(COL_ADDRESS).getB(),
                  info.get(COL_POST_INDEX).getB(), info.get(COL_CITY).getB(),
                  info.get(COL_COUNTRY).getB());

              if (!BeeUtils.isEmpty(value)) {
                Widget widget = new Label(info.get(col).getA());
                widget.setStyleName(STYLE_COMPANY_LABEL);
                record.add(widget);

                widget = new Label(value);
                widget.setStyleName(STYLE_COMPANY_ITEM);
                record.add(widget);
              }
              break;

            case COL_BANK_ACCOUNT:
              value = info.get(COL_BANK_ACCOUNT).getB();

              if (!BeeUtils.isEmpty(value)) {
                Flow bank = new Flow();

                Widget widget = new Label(info.get(col).getA());
                widget.setStyleName(STYLE_COMPANY_LABEL);
                bank.add(widget);

                widget = new Label(BeeUtils.joinItems(value,
                    info.get(COL_BANK).getB(), info.get(COL_BANK_CODE).getB()));
                widget.setStyleName(STYLE_COMPANY_ITEM);
                bank.add(widget);

                if (!BeeUtils.isEmpty(info.get(COL_SWIFT_CODE).getB())) {
                  widget = new Label(info.get(COL_SWIFT_CODE).getA());
                  widget.setStyleName(STYLE_COMPANY_LABEL);
                  bank.add(widget);

                  widget = new Label(info.get(COL_SWIFT_CODE).getB());
                  widget.setStyleName(STYLE_COMPANY_ITEM);
                  bank.add(widget);
                }
                record.add(bank);
              }
              break;

            default:
              Pair<String, String> pair = info.get(col);

              if (!BeeUtils.isEmpty(pair.getB())) {
                Widget widget = new Label(pair.getA());
                widget.setStyleName(STYLE_COMPANY_LABEL);
                record.add(widget);

                widget = new Label(pair.getB());
                widget.setStyleName(STYLE_COMPANY_ITEM);
                record.add(widget);
              }
          }
          if (!record.isEmpty()) {
            flow.add(record);
          }
        }
        target.getElement().setInnerHTML(flow.getElement().getString());
      }
    });
  }

  private ClassifierUtils() {
  }
}
