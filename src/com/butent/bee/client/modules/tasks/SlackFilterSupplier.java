package com.butent.bee.client.modules.tasks;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

class SlackFilterSupplier extends AbstractFilterSupplier {

  private enum Slack {
    LATE {
      @Override
      Filter getFilter() {
        return Filter.and(Filter.isLess(COL_STATUS, IntegerValue.of(TaskStatus.COMPLETED)),
            Filter.isLess(COL_FINISH_TIME, new DateTimeValue(TimeUtils.nowMinutes())));
      }

      @Override
      String getLabel() {
        return Localized.getConstants().crmTaskLabelLate();
      }

      @Override
      String getValue() {
        return "late";
      }
    },

    SCHEDULED {
      @Override
      Filter getFilter() {
        return Filter.and(Filter.isLess(COL_STATUS, IntegerValue.of(TaskStatus.COMPLETED)),
            Filter.isMoreEqual(COL_START_TIME,
                new DateTimeValue(TimeUtils.today(1).getDateTime())));
      }

      @Override
      String getLabel() {
        return Localized.getConstants().crmTaskLabelScheduled();
      }

      @Override
      String getValue() {
        return "scheduled";
      }
    };

    private static Slack parseValue(String value) {
      for (Slack slack : Slack.values()) {
        if (BeeUtils.same(value, slack.getValue())) {
          return slack;
        }
      }
      return null;
    }

    abstract Filter getFilter();

    abstract String getLabel();

    abstract String getValue();
  }

  private Slack slack;

  SlackFilterSupplier(String options) {
    super(VIEW_TASKS, null, null, options);
  }

  @Override
  public String getComponentLabel(String ownerLabel) {
    return getLabel();
  }

  @Override
  public FilterValue getFilterValue() {
    return (getSlack() == null) ? null : FilterValue.of(getSlack().getValue());
  }

  @Override
  public String getLabel() {
    return (getSlack() == null) ? null : getSlack().getLabel();
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    openDialog(target, createWidget(), onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    Slack s = (input == null) ? null : Slack.parseValue(input.getValue());
    return (s == null) ? null : s.getFilter();
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    setSlack((filterValue == null) ? null : Slack.parseValue(filterValue.getValue()));
  }

  @Override
  protected String getStylePrefix() {
    return "bee-crm-FilterSupplier-Slack-";
  }

  private Widget createWidget() {
    Flow container = new Flow();
    container.addStyleName(getStylePrefix() + "container");

    Button late = new Button(Localized.getConstants().crmTaskFilterLate());
    late.addStyleName(getStylePrefix() + "late");

    late.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = !Slack.LATE.equals(getSlack());
        setSlack(Slack.LATE);
        update(changed);
      }
    });

    container.add(late);

    Button scheduled = new Button(Localized.getConstants().crmTaskFilterScheduled());
    scheduled.addStyleName(getStylePrefix() + "scheduled");

    scheduled.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = !Slack.SCHEDULED.equals(getSlack());
        setSlack(Slack.SCHEDULED);
        update(changed);
      }
    });

    container.add(scheduled);

    Button all = new Button(Localized.getConstants().crmTaskFilterAll());
    all.addStyleName(getStylePrefix() + "all");

    all.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean changed = getSlack() != null;
        setSlack(null);
        update(changed);
      }
    });

    container.add(all);

    Button cancel = new Button(Localized.getConstants().cancel());
    cancel.addStyleName(getStylePrefix() + "cancel");

    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        closeDialog();
      }
    });

    container.add(cancel);

    return container;
  }

  private Slack getSlack() {
    return slack;
  }

  private void setSlack(Slack slack) {
    this.slack = slack;
  }
}
