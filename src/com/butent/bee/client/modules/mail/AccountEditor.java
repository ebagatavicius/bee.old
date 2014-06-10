package com.butent.bee.client.modules.mail;

import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.mail.MailConstants.Protocol;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Map;
import java.util.Set;

public class AccountEditor extends AbstractFormInterceptor implements SelectorEvent.Handler {
  @Override
  public void afterCreateWidget(final String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector
        && BeeUtils.inListSame(name,
            SystemFolder.Sent.name() + COL_FOLDER,
            SystemFolder.Drafts.name() + COL_FOLDER,
            SystemFolder.Trash.name() + COL_FOLDER)) {

      ((DataSelector) widget).addSelectorHandler(this);

    } else if (widget instanceof HasClickHandlers) {
      if (BeeUtils.inListSame(name, COL_STORE_PASSWORD, COL_TRANSPORT_PASSWORD)) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Global.inputString(Localized.getConstants().mailNewAccountPassword(), null,
                new StringCallback(false) {
                  @Override
                  public void onSuccess(String value) {
                    getFormView().getActiveRow().setValue(getFormView().getDataIndex(name),
                        BeeUtils.isEmpty(value) ? null : Codec.encodeBase64(value));
                  }
                }, null, BeeConst.UNDEF, null, BeeConst.DOUBLE_UNDEF, null, BeeConst.UNDEF,
                Localized.getConstants().ok(), Localized.getConstants().cancel(),
                new WidgetInitializer() {
                  @Override
                  public Widget initialize(Widget inputWidget, String widgetName) {
                    if (BeeUtils.same(widgetName, DialogConstants.WIDGET_INPUT)) {
                      inputWidget.getElement().setPropertyString("type", "password");
                    }
                    return inputWidget;
                  }
                });
          }
        });
      } else if (BeeUtils.inListSame(name, COL_STORE_PROPERTIES, COL_TRANSPORT_PROPERTIES)) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final IsRow row = getActiveRow();
            final int index = getDataIndex(name);
            if (index == BeeConst.UNDEF) {
              return;
            }
            Global.inputMap(BeeUtils.joinWords(Localized.getConstants().properties(),
                BeeUtils.parenthesize(BeeUtils.same(name, COL_TRANSPORT_PROPERTIES)
                    ? Protocol.SMTP.name() : row.getString(getDataIndex(COL_STORE_TYPE)))),
                Localized.getConstants().property(), Localized.getConstants().value(),
                Codec.deserializeMap(row.getString(index)),
                new Consumer<Map<String, String>>() {
                  @Override
                  public void accept(Map<String, String> input) {
                    row.setValue(index, Codec.beeSerialize(input));
                  }
                });
          }
        });
      }
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    Queries.getRow(getViewName(), result.getId(), new RowUpdateCallback(getViewName()));
    super.afterInsertRow(result, forced);
  }

  @Override
  public FormInterceptor getInstance() {
    return new AccountEditor();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      Set<Long> exclusions = Sets.newHashSet();

      for (SystemFolder folder : SystemFolder.values()) {
        exclusions.add(getLongValue(folder + COL_FOLDER));
      }
      event.consume();
      event.getSelector().setAdditionalFilter(Filter.equals(COL_ACCOUNT, getActiveRowId()));
      event.getSelector().getOracle().setExclusions(exclusions);
    }
  }
}
