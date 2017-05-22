package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public class TextConstantForm extends AbstractFormInterceptor {

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_TEXT_CONSTANT)) {
      ListBox list = (ListBox) widget;

      list.addChangeHandler((e) -> {
        TextConstant constant = EnumUtils.getEnumByIndex(TextConstant.class,
            list.getSelectedIndex());

        getFormView().updateCell(COL_TEXT_CONTENT, constant.getDefaultContent(
            Localized.dictionary()));
        getFormView().refreshBySource(COL_TEXT_CONTENT);
      });
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TextConstantForm();
  }
}
