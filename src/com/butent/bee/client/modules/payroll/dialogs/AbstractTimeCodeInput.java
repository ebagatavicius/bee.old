package com.butent.bee.client.modules.payroll.dialogs;


import com.google.gwt.event.dom.client.ClickEvent;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.CloseButton;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.payroll.PayrollKeeper;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractTimeCodeInput extends DialogBox {
  public interface TimeCodeSelectEvent {
    void onTimeCodeSelected(BeeRow selectedTimeCode);
  }

  protected static final String STYLE_PREFIX = PayrollKeeper.STYLE_PREFIX + "ws-";
  protected static final String STYLE_TRC_DIALOG = STYLE_PREFIX + "trc-dialog";
  protected static final String STYLE_TRC_PANEL = STYLE_PREFIX + "trc-panel";
  protected static final String STYLE_TRC_HEADER = STYLE_PREFIX + "trc-header";
  protected static final String STYLE_TRC_INPUT_CONTAINER = STYLE_PREFIX + "trc-input-container";

  static final String STYLE_TRC_OPTION_SEPARATOR = STYLE_PREFIX + "trc-option-separator";
  static final String STYLE_TRC_OPTIONS_TABLE = STYLE_PREFIX + "trc-options-table";
  static final String STYLE_TRC_OPTION_WIDGET = STYLE_PREFIX + "trc-option-widget";
  static final String STYLE_TRC_OPTION_CODE = STYLE_PREFIX + "trc-option-code";
  static final String STYLE_TRC_OPTION_INFO = STYLE_PREFIX + "trc-option-info";

  private static final String STYLE_TRC_OPTIONS_CONTAINER = STYLE_PREFIX + "trc-options-container";
  private static final String STYLE_TRC_CONTROLS = STYLE_PREFIX + "trc-controls";
  private static final String STYLE_TRC_CANCEL = STYLE_PREFIX + "trc-cancel";

  private final Flow panel = new Flow(STYLE_TRC_PANEL);
  private final Label header = new Label();
  private final Flow inputContainer = new Flow(STYLE_TRC_INPUT_CONTAINER);

  private final BeeRowSet timeCodes;
  private final Set<Long> exclusions = new HashSet<>();

  private TimeCodeSelectEvent timeCodeSelectEvent;
  private Flow contentPanel;

  public AbstractTimeCodeInput(String caption, BeeRowSet timeCodes, Set<Long> exclusions) {
    super(caption, STYLE_TRC_DIALOG);
    this.timeCodes = timeCodes;

    if (!BeeUtils.isEmpty(exclusions)) {
      this.exclusions.addAll(exclusions);
    }
    addDefaultCloseBox();
    createUI();
  }

  public void setContentPanel(Flow contentPanel) {
    this.contentPanel = contentPanel;
  }

  public void setHeaderDate(JustDate date) {
    header.setHtml(Format.renderDateFull(date));
  }

  public void show() {
    if (contentPanel == null) {
      center();
    } else {
      showRelativeTo(contentPanel.getElement());
    }
  }

  public void setTimeCodeSelectEvent(TimeCodeSelectEvent timeCodeSelectEvent) {
    this.timeCodeSelectEvent = timeCodeSelectEvent;
  }

  protected void handleTimeCodeSelection(BeeRow row) {
    if (timeCodeSelectEvent != null) {
      timeCodeSelectEvent.onTimeCodeSelected(row);
    }
    close();
  }

  List<Long> getFilteredTimeCodeIds() {
    List<Long> codes = new ArrayList<>();
    codes.addAll(timeCodes.getRowIds());

    if (!BeeUtils.isEmpty(exclusions)) {
      codes.removeAll(exclusions);
    }
    return codes;
  }

  String getString(long id, String col) {
    if (!BeeUtils.isEmpty(col) && timeCodes.containsColumn(col)) {
      return timeCodes.getStringByRowId(id, col);
    }
    return BeeConst.STRING_EMPTY;
  }

  BeeRowSet getTimeCodes() {
    return timeCodes;
  }

  void onOptionButtonClick(ClickEvent event) {
    Long trId = DomUtils.getDataIndexLong(EventUtils.getSourceElement(event));
    handleTimeCodeSelection(timeCodes.getRowById(trId));
  }

  abstract void onInputContainerCreate(Flow inputContainerWidget);

  abstract String renderTimeCodeButtonLabel(BeeRowSet rs, BeeRow row);

  private void createUI() {
    header.addStyleName(STYLE_TRC_HEADER);
    panel.add(header);
    panel.add(inputContainer);

    onInputContainerCreate(inputContainer);
    createOptionTable();
    createControls();

    setWidget(panel);
    setAnimationEnabled(true);
    setHideOnEscape(true);
  }

  private void createControls() {
    Button cancel = new CloseButton(Localized.dictionary().cancel());
    cancel.addStyleName(STYLE_TRC_CANCEL);

    Flow controls = new Flow(STYLE_TRC_CONTROLS);
    controls.add(cancel);
    panel.add(controls);
  }

  private void createOptionTable() {
    List<Long> codes = getFilteredTimeCodeIds();
    HtmlTable options = new HtmlTable(STYLE_TRC_OPTIONS_TABLE);
    Flow optionsContainer = new Flow(STYLE_TRC_OPTIONS_CONTAINER);

    int width = BeeKeeper.getScreen().getWidth();
    int minCols = BeeUtils.resize(width, 300, 1920, 2, 4);
    int maxCols = Math.max(BeeUtils.resize(width, 300, 1920, 3, 8), minCols + 1);
    int size = codes.size();
    int cols = UiHelper.getLayoutColumns(size, minCols, maxCols);

    for (int i = 0; i < size; i++) {
      int r = i / cols;
      int c = i % cols;
      long codeId = codes.get(i);

      Button option = new Button(renderTimeCodeButtonLabel(timeCodes, timeCodes.createRow(codeId)));

      DomUtils.setDataIndex(option.getElement(), codeId);
      option.addClickHandler(this::onOptionButtonClick);
      options.setWidgetAndStyle(r, c, option, STYLE_TRC_OPTION_WIDGET);
    }

    optionsContainer.add(options);
    panel.add(optionsContainer);
  }
}