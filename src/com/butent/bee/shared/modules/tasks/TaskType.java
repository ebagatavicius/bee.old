package com.butent.bee.shared.modules.tasks;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum TaskType implements HasCaption {
  ASSIGNED(Localized.getConstants().crmTasksAssignedTasks(), Feed.TASKS_ASSIGNED) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return Filter.isEqual(COL_EXECUTOR, userValue);
    }
  },

  DELEGATED(Localized.getConstants().crmTasksDelegatedTasks(), Feed.TASKS_DELEGATED) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return Filter.and(Filter.isEqual(COL_OWNER, userValue),
          Filter.isNotEqual(COL_EXECUTOR, userValue));
    }
  },

  OBSERVED(Localized.getConstants().crmTasksObservedTasks(), Feed.TASKS_OBSERVED) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return Filter.and(Filter.isNotEqual(COL_OWNER, userValue),
          Filter.isNotEqual(COL_EXECUTOR, userValue),
          Filter.in(COL_TASK_ID, VIEW_TASK_USERS, COL_TASK,
              Filter.isEqual(AdministrationConstants.COL_USER, userValue)));
    }
  },

  RELATED(Localized.getConstants().crmTasksRelated(), null) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return null;
    }
  },

  ALL(Localized.getConstants().crmTasksAll(), Feed.TASKS_ALL) {
    @Override
    public Filter getFilter(LongValue userValue) {
      return null;
    }
  };

  public static TaskType getByFeed(Feed input) {
    if (input == null) {
      return null;
    }

    for (TaskType type : values()) {
      if (type.feed == input) {
        return type;
      }
    }
    return null;
  }

  public static TaskType getByPrefix(String input) {
    for (TaskType type : values()) {
      if (BeeUtils.startsSame(type.name(), input)) {
        return type;
      }
    }
    return null;
  }

  private final String caption;
  private final Feed feed;

  private TaskType(String caption, Feed feed) {
    this.caption = caption;
    this.feed = feed;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public abstract Filter getFilter(LongValue userValue);
  
  public String getSupplierKey() {
    return GRID_TASKS + BeeConst.STRING_UNDER + name().toLowerCase();
  }
}
