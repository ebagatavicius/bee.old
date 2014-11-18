package com.butent.bee.client.modules.tasks;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

class ModeFilterSupplier extends AbstractFilterSupplier {

  private enum Mode {
    NEW {
      @Override
      String getLabel() {
        return Localized.getConstants().crmTaskFilterNew();
      }

      @Override
      String getValue() {
        return "0";
      }
    },

    UPDATED {
      @Override
      String getLabel() {
        return Localized.getConstants().crmTaskFilterUpdated();
      }

      @Override
      String getValue() {
        return "1";
      }
    },

    NEW_OR_UPDATED {
      @Override
      String getLabel() {
        return Localized.getConstants().crmTaskFilterNewOrUpdated();
      }

      @Override
      String getValue() {
        return "2";
      }
    };

    private static Mode parseValue(String value) {
      for (Mode mode : Mode.values()) {
        if (BeeUtils.same(value, mode.getValue())) {
          return mode;
        }
      }
      return null;
    }

    abstract String getLabel();

    abstract String getValue();
  }

  private static Widget createMode(String styleName) {
    return new CustomDiv(styleName);
  }

  private static Filter getNewFilter() {
    return Filter.custom(FILTER_TASKS_NEW);
  }

  private static Filter getUpdFilter() {
    return Filter.custom(FILTER_TASKS_UPDATED);
  }

  private Mode mode;

  ModeFilterSupplier(String options) {
    super(VIEW_TASKS, null, null, options);
  }

  @Override
  public String getComponentLabel(String ownerLabel) {
    return getLabel();
  }

  @Override
  public FilterValue getFilterValue() {
    return (getMode() == null) ? null : FilterValue.of(getMode().getValue());
  }

  @Override
  public String getLabel() {
    return (getMode() == null) ? null : getMode().getLabel();
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    openDialog(target, createWidget(), onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input != null && BeeUtils.isDigit(input.getValue())) {
      switch (BeeUtils.toInt(input.getValue())) {
        case 0:
          return getNewFilter();
        case 1:
          return getUpdFilter();
        case 2:
          return Filter.or(getNewFilter(), getUpdFilter());
      }
    }

    return null;
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    setMode((filterValue == null) ? null : Mode.parseValue(filterValue.getValue()));
  }

  @Override
  protected String getStylePrefix() {
    return BeeConst.CSS_CLASS_PREFIX + "crm-FilterSupplier-Mode-";
  }

  private Widget createWidget() {
    HtmlTable container = new HtmlTable();
    container.addStyleName(getStylePrefix() + "container");

    int row = 0;

    Button bNew = new Button(Localized.getConstants().crmTaskFilterNew());
    bNew.addStyleName(getStylePrefix() + "new");

    bNew.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = !Mode.NEW.equals(getMode());
        setMode(Mode.NEW);
        update(changed);
      }
    });

    container.setWidget(row, 0, bNew);

    Widget modeNew = createMode(ModeRenderer.STYLE_MODE_NEW);
    container.setWidget(row, 1, modeNew);

    row++;

    Button bUpd = new Button(Localized.getConstants().crmTaskFilterUpdated());
    bUpd.addStyleName(getStylePrefix() + "upd");

    bUpd.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = !Mode.UPDATED.equals(getMode());
        setMode(Mode.UPDATED);
        update(changed);
      }
    });

    container.setWidget(row, 0, bUpd);

    Widget modeUpd = createMode(ModeRenderer.STYLE_MODE_UPD);
    container.setWidget(row, 1, modeUpd);

    row++;

    Button both = new Button(Localized.getConstants().crmTaskFilterNewOrUpdated());
    both.addStyleName(getStylePrefix() + "both");

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = !Mode.NEW_OR_UPDATED.equals(getMode());
        setMode(Mode.NEW_OR_UPDATED);
        update(changed);
      }
    });

    container.setWidget(row, 0, both);

    Flow modeBoth = new Flow();
    modeBoth.add(createMode(ModeRenderer.STYLE_MODE_NEW));
    modeBoth.add(createMode(ModeRenderer.STYLE_MODE_UPD));

    container.setWidget(row, 1, modeBoth);

    row++;

    Button all = new Button(Localized.getConstants().crmTaskFilterAll());
    all.addStyleName(getStylePrefix() + "all");

    all.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = getMode() != null;
        setMode(null);
        update(changed);
      }
    });

    container.setWidget(row, 0, all);

    Button cancel = new Button(Localized.getConstants().cancel());
    cancel.addStyleName(getStylePrefix() + "cancel");

    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        closeDialog();
      }
    });

    container.setWidget(row, 1, cancel);

    return container;
  }

  private Mode getMode() {
    return mode;
  }

  private void setMode(Mode mode) {
    this.mode = mode;
  }
}
