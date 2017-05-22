package com.butent.bee.client.output;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportSettings {

  private static final class Item implements HasCaption {
    private final long id;
    private final Report report;

    private String caption;
    private final ReportParameters parameters;

    private Item(long id, Report report, String caption, ReportParameters parameters) {
      this.id = id;
      this.report = report;
      this.caption = caption;
      this.parameters = parameters;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    private long getId() {
      return id;
    }

    private ReportParameters getParameters() {
      return parameters;
    }

    private Report getReport() {
      return report;
    }

    private void open() {
      report.open(parameters);
    }

    private void setCaption(String caption) {
      this.caption = caption;
    }
  }

  private static final class SettingsWidget extends Flow {
    private final Item item;

    private SettingsWidget(Item item) {
      super(STYLE_ITEM);
      this.item = item;

      CustomDiv label = new CustomDiv(STYLE_LABEL);
      label.setText(item.getCaption());

      if (Global.isDebug()) {
        List<String> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : item.getParameters().entrySet()) {
          params.add(BeeUtils.joinWords(entry.getKey(), entry.getValue()));
        }
        if (!params.isEmpty()) {
          label.setTitle(BeeUtils.buildLines(params));
        }
      }

      label.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          getItem().open();
        }
      });

      add(label);

      FaLabel edit = new FaLabel(FontAwesome.EDIT, STYLE_EDIT);
      edit.setTitle(Localized.dictionary().actionRename());

      edit.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          int maxLength = Data.getColumnPrecision(VIEW_REPORT_SETTINGS, COL_RS_CAPTION);

          Global.inputString(Localized.dictionary().bookmarkName(), null, new StringCallback() {
            @Override
            public void onSuccess(String value) {
              setCaption(value);
            }
          }, null, getItem().getCaption(), maxLength, getElement(),
              CAPTION_INPUT_WIDTH, CAPTION_INPUT_WIDTH_UNIT);
        }
      });

      add(edit);

      FaLabel delete = new FaLabel(FontAwesome.TRASH_O, STYLE_DELETE);
      delete.setTitle(Localized.dictionary().actionRemove());

      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Global.confirmRemove(Domain.REPORTS.getCaption(), getItem().getCaption(),
              new ConfirmationCallback() {
                @Override
                public void onConfirm() {
                  Queries.deleteRow(VIEW_REPORT_SETTINGS, getItem().getId());
                  removeFromParent();
                }
              }, getElement());
        }
      });

      add(delete);
    }

    private Item getItem() {
      return item;
    }

    private void setCaption(String caption) {
      if (!BeeUtils.isEmpty(caption) && !BeeUtils.equalsTrim(caption, item.getCaption())) {
        Queries.update(VIEW_REPORT_SETTINGS, item.getId(), COL_RS_CAPTION,
            new TextValue(caption.trim()));
        getItem().setCaption(caption.trim());

        for (Widget widget : this) {
          if (widget.getElement().hasClassName(STYLE_LABEL)) {
            widget.getElement().setInnerText(caption.trim());
            break;
          }
        }
      }
    }
  }

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "report-settings-";

  private static final String STYLE_ITEM = STYLE_PREFIX + "item";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_EDIT = STYLE_PREFIX + "edit";
  private static final String STYLE_DELETE = STYLE_PREFIX + "delete";

  private static final double CAPTION_INPUT_WIDTH = 320;
  private static final CssUnit CAPTION_INPUT_WIDTH_UNIT = CssUnit.PX;

  private final Flow panel = new Flow(STYLE_PREFIX + "panel");

  public ReportSettings() {
    super();
  }

  public void bookmark(final Report report, String caption, final ReportParameters parameters) {
    Assert.notNull(report);
    Assert.notNull(parameters);

    if (!DataUtils.isId(BeeKeeper.getUser().getUserId())) {
      return;
    }

    final SettingsWidget settingsWidget = find(report, parameters);

    String defValue = (settingsWidget == null) ? caption : settingsWidget.getItem().getCaption();
    int maxLength = Data.getColumnPrecision(VIEW_REPORT_SETTINGS, COL_RS_CAPTION);

    Global.inputString(Localized.dictionary().bookmarkName(), null, new StringCallback() {
      @Override
      public void onSuccess(String value) {
        if (settingsWidget == null) {
          addItem(report, value, parameters);

        } else {
          settingsWidget.setCaption(value);
          activate();
        }
      }
    }, null, defValue, maxLength, null, CAPTION_INPUT_WIDTH, CAPTION_INPUT_WIDTH_UNIT);
  }

  public IdentifiableWidget getPanel() {
    return panel;
  }

  public boolean isEmpty() {
    return panel.isEmpty();
  }

  public void load(String serialized) {
    if (!panel.isEmpty()) {
      panel.clear();
    }

    if (BeeUtils.isEmpty(serialized)) {
      return;
    }

    BeeRowSet rowSet = BeeRowSet.restore(serialized);
    if (DataUtils.isEmpty(rowSet)) {
      return;
    }

    int reportIndex = rowSet.getColumnIndex(COL_RS_REPORT);
    Assert.nonNegative(reportIndex, COL_RS_REPORT);

    int captionIndex = rowSet.getColumnIndex(COL_RS_CAPTION);
    Assert.nonNegative(captionIndex, COL_RS_CAPTION);

    int paramIndex = rowSet.getColumnIndex(COL_RS_PARAMETERS);
    Assert.nonNegative(paramIndex, COL_RS_PARAMETERS);

    for (BeeRow row : rowSet) {
      Report report = Report.parse(row.getString(reportIndex));

      if (report != null) {
        Item item = new Item(row.getId(), report, row.getString(captionIndex),
            ReportParameters.restore(row.getString(paramIndex)));
        add(item);
      }
    }
  }

  private void activate() {
    if (!BeeKeeper.getScreen().containsDomainEntry(Domain.REPORTS, null)) {
      BeeKeeper.getScreen().addDomainEntry(Domain.REPORTS, getPanel(), null, null);
    }
    BeeKeeper.getScreen().activateDomainEntry(Domain.REPORTS, null);
  }

  private void add(Item item) {
    panel.add(new SettingsWidget(item));
  }

  private void addItem(final Report report, final String caption,
      final ReportParameters parameters) {

    List<BeeColumn> columns = Data.getColumns(VIEW_REPORT_SETTINGS,
        Lists.newArrayList(COL_RS_USER, COL_RS_REPORT, COL_RS_CAPTION, COL_RS_PARAMETERS));
    List<String> values = Lists.newArrayList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
        report.getReportName(), BeeUtils.trim(caption), Codec.beeSerialize(parameters));

    Queries.insert(VIEW_REPORT_SETTINGS, columns, values, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow row) {
        Item item = new Item(row.getId(), report, BeeUtils.trim(caption), parameters);
        add(item);

        activate();

        if (panel.getWidgetCount() > 1) {
          DomUtils.scrollToBottom(panel);
        }
      }
    });
  }

  private SettingsWidget find(Report report, ReportParameters parameters) {
    if (panel.isEmpty()) {
      return null;
    }

    for (Widget widget : panel) {
      if (widget instanceof SettingsWidget) {
        Item item = ((SettingsWidget) widget).getItem();

        if (item.getReport() == report && item.getParameters().equals(parameters)) {
          return (SettingsWidget) widget;
        }
      }
    }
    return null;
  }
}
