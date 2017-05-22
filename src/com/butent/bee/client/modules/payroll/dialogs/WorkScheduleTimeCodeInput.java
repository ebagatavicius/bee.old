package com.butent.bee.client.modules.payroll.dialogs;

import com.google.gwt.event.dom.client.KeyCodes;

import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Set;

public class WorkScheduleTimeCodeInput extends AbstractTimeCodeInput {

  private static final String STYLE_TRC_INPUT_LABEL = STYLE_PREFIX + "trc-input-label";
  private static final String STYLE_TRC_INPUT_WIDGET = STYLE_PREFIX + "trc-input-widget";

  public WorkScheduleTimeCodeInput(String caption, BeeRowSet timeCodes,
                                   Set<Long> exclusions) {
    super(caption, timeCodes, exclusions);
  }

  @Override
  void onInputContainerCreate(Flow inputContainer) {
    Label inputLabel = new Label(Localized.dictionary().timeRangeCode());
    InputText inputWidget = new InputText();

    inputLabel.addStyleName(STYLE_TRC_INPUT_LABEL);
    inputWidget.addStyleName(STYLE_TRC_INPUT_WIDGET);
    inputWidget.setMaxLength(Data.getColumnPrecision(getTimeCodes().getViewName(), COL_TR_CODE));
    inputWidget.setUpperCase(true);
    focusOnOpen(inputWidget);

    inputContainer.add(inputLabel);
    inputContainer.add(inputWidget);

    inputWidget.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
        String value = inputWidget.getValue();
        List<Long> filterCodes = getFilteredTimeCodeIds();

        if (!BeeUtils.isEmpty(value)) {
          BeeRow found = getTimeCodes().findRow((columns, row) ->
            BeeUtils.same(getString(row.getId(), COL_TR_CODE), value)
              && filterCodes.contains(row.getId())
          );

          if (found != null) {
            event.preventDefault();
            handleTimeCodeSelection(found);
          }
        }
      }
    });
  }

  @Override
  String renderTimeCodeButtonLabel(BeeRowSet rs, BeeRow row) {
    String separator = new Span().addClass(STYLE_TRC_OPTION_SEPARATOR).build();
    String label = getCodeRange(row.getId());
    Span code = new Span()
      .addClass(STYLE_TRC_OPTION_CODE).text(getString(row.getId(), COL_TR_CODE));
    Span info = new Span()
      .addClass(STYLE_TRC_OPTION_INFO).text(label);

    return code.build() + separator + info.build();
  }

  private String getCodeRange(Long id) {
    return BeeUtils.joinWords(getString(id, COL_TR_NAME),
      TimeUtils.renderPeriod(getString(id, COL_TR_FROM), getString(id, COL_TR_UNTIL)));
  }
}
