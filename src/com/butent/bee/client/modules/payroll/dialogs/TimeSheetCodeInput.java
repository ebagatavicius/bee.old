package com.butent.bee.client.modules.payroll.dialogs;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;

import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.InputTimeOfDay;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TimeSheetCodeInput extends AbstractTimeCodeInput {
  private static final String STYLE_TRC_INPUT_LABEL = STYLE_PREFIX + "trc-input-label";
  private static final String STYLE_TRC_INPUT_WIDGET = STYLE_PREFIX + "trc-input-widget";

  private UnboundSelector inputCode;
  private InputTimeOfDay inputDuration;

  public TimeSheetCodeInput(String caption, BeeRowSet timeCodes) {
    super(caption, timeCodes, getExclusions(timeCodes));
  }

  @Override
  void onInputContainerCreate(Flow inputContainer) {
    Label inputLabel = new Label(Localized.dictionary().timeCardCode());
    Label durationLabel = new Label(Localized.dictionary().duration());
    Relation rel = Relation.create(VIEW_TIME_CARD_CODES, Collections.singletonList(COL_TC_CODE));
    rel.setChoiceColumns(Arrays.asList(COL_TC_CODE, COL_TC_NAME));

    inputCode = UnboundSelector.create(rel);
    inputDuration = new InputTimeOfDay();

    inputLabel.addStyleName(STYLE_TRC_INPUT_LABEL);
    durationLabel.addStyleName(STYLE_TRC_INPUT_LABEL);

    inputCode.addStyleName(STYLE_TRC_INPUT_WIDGET);
    inputCode.setMaxLength(Data.getColumnPrecision(getTimeCodes().getViewName(), COL_TC_CODE));
    inputCode.setUpperCase(true);
    inputCode.addSelectorHandler(e -> onInputHandleEvent(e, null));
    inputCode.setAdditionalFilter(Filter.idIn(getTimeCodes().getRowIds()), true);

    inputDuration.addStyleName(STYLE_TRC_INPUT_WIDGET);
    inputDuration.addKeyDownHandler(e -> onInputHandleEvent(null, e));

    inputContainer.add(inputLabel);
    inputContainer.add(inputCode);
    inputContainer.add(durationLabel);
    inputContainer.add(inputDuration);
  }

  @Override
  String renderTimeCodeButtonLabel(BeeRowSet rs, BeeRow row) {
    String separator = new Span().addClass(STYLE_TRC_OPTION_SEPARATOR).build();
    String label = getCodeDurationKindCaption(row.getId());
    Span code = new Span()
      .addClass(STYLE_TRC_OPTION_CODE).text(getString(row.getId(), COL_TC_CODE));
    Span info = new Span()
      .addClass(STYLE_TRC_OPTION_INFO).text(label);

    return code.build() + separator + info.build();
  }

  private static Set<Long> getExclusions(BeeRowSet timeCodes) {
    Set<Long> exclusions = new HashSet<>();

    if (timeCodes != null) {
      timeCodes.forEach(row -> {
        TcDurationType dt = EnumUtils.getEnumByIndex(TcDurationType.class,
          timeCodes.getStringByRowId(row.getId(), COL_TC_DURATION_TYPE));

        if (dt != null && dt == TcDurationType.PART_TIME) {
          exclusions.add(row.getId());
        }
      });
    }
    return exclusions;
  }

  private String getCodeDurationKindCaption(Long id) {
    TcDurationType dt = getCodeDurationKind(id);

    return dt != null ? dt.getCaption() : BeeConst.STRING_EMPTY;
  }

  private TcDurationType getCodeDurationKind(Long id) {
    return EnumUtils.getEnumByIndex(TcDurationType.class,
      getString(id, COL_TC_DURATION_TYPE));
  }

  private void onInputHandleEvent(SelectorEvent selectorEvent, KeyDownEvent keyDownEvent) {
    if ((selectorEvent != null && selectorEvent.isChanged())
      || keyDownEvent != null && keyDownEvent.getNativeKeyCode() ==  KeyCodes.KEY_ENTER) {
      Long code = inputCode  != null ? inputCode.getRelatedId() : null;
      String duration = inputDuration !=  null ? inputDuration.getNormalizedValue() : null;

      if (!DataUtils.isId(code)) {
        if (!BeeUtils.isEmpty(duration)) {
          BeeRow row = new BeeRow(BeeConst.UNDEF, BeeConst.UNDEF);

          row.setProperty(PROP_TC_DURATION, duration);

          if (keyDownEvent != null) {
            keyDownEvent.preventDefault();
          } else if (selectorEvent != null) {
            selectorEvent.consume();
          }
          handleTimeCodeSelection(row);
        }
      } else {
        BeeRow found = getTimeCodes().getRowById(code);

        if (found != null && DataUtils.isId(found.getId())) {
          TcDurationType dt = getCodeDurationKind(found.getId());

          if (dt != null && dt == TcDurationType.PART_TIME && !BeeUtils.isEmpty(duration)) {
            BeeRow row = DataUtils.cloneRow(found);
            row.setProperty(PROP_TC_DURATION, duration);

            if (keyDownEvent != null) {
              keyDownEvent.preventDefault();
            } else if (selectorEvent != null) {
              selectorEvent.consume();
            }
            handleTimeCodeSelection(row);
          } else if (dt != null && dt == TcDurationType.PART_TIME) {
            if (selectorEvent == null) {
              BeeKeeper.getScreen().notifySevere(Localized.dictionary().enterTime());
            }
          } else {
            handleTimeCodeSelection(found);
          }
        }
      }
    }
  }
}
