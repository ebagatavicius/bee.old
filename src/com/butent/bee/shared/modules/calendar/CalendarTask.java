package com.butent.bee.shared.modules.calendar;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarHelper.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskPriority;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CalendarTask extends CalendarItem implements BeeSerializable {

  private enum Serial {
    TYPE, ID, START, END, SUMMARY, DESCRIPTION, COMPANY_NAME, PRIORITY, STATUS, OWNER, EXECUTOR,
    OBSERVERS, BACKGRUOND, FOREGROUND, STYLE
  }

  private static final String SIMPLE_HEADER_TEMPLATE;
  private static final String SIMPLE_BODY_TEMPLATE;

  private static final String PARTIAL_HEADER_TEMPLATE;
  private static final String PARTIAL_BODY_TEMPLATE;

  private static final String MULTI_HEADER_TEMPLATE;
  private static final String MULTI_BODY_TEMPLATE;

  private static final String COMPACT_TEMPLATE;
  private static final String TITLE_TEMPLATE;

  private static final String STRING_TEMPLATE;

  static {
    SIMPLE_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    SIMPLE_BODY_TEMPLATE = BeeUtils.buildLines(wrap(COL_STATUS), wrap(ALS_COMPANY_NAME),
        wrap(TaskConstants.COL_OWNER), wrap(TaskConstants.COL_EXECUTOR),
        wrap(TaskConstants.PROP_OBSERVERS));

    PARTIAL_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    PARTIAL_BODY_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        wrap(ALS_COMPANY_NAME), wrap(TaskConstants.COL_OWNER), wrap(TaskConstants.COL_EXECUTOR),
        wrap(TaskConstants.PROP_OBSERVERS));

    MULTI_HEADER_TEMPLATE = BeeUtils.joinWords(wrap(KEY_PERIOD), wrap(COL_SUMMARY));
    MULTI_BODY_TEMPLATE = BeeUtils.joinWords(wrap(COL_STATUS), wrap(ALS_COMPANY_NAME),
        wrap(TaskConstants.COL_OWNER), wrap(TaskConstants.COL_EXECUTOR),
        wrap(TaskConstants.PROP_OBSERVERS));

    COMPACT_TEMPLATE = BeeUtils.joinWords(wrap(COL_SUMMARY), wrap(TaskConstants.COL_EXECUTOR));

    TITLE_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_SUMMARY), wrap(COL_STATUS),
        wrap(ALS_COMPANY_NAME), wrap(TaskConstants.COL_OWNER), wrap(TaskConstants.COL_EXECUTOR),
        wrap(TaskConstants.PROP_OBSERVERS), wrap(COL_DESCRIPTION));

    STRING_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_SUMMARY), wrap(COL_STATUS),
        wrap(ALS_COMPANY_NAME), wrap(TaskConstants.COL_OWNER), wrap(TaskConstants.COL_EXECUTOR));
  }

  public static CalendarTask restore(String s) {
    CalendarTask ct = new CalendarTask();
    ct.deserialize(s);
    return ct;
  }

  private static String formatUser(Long user, Map<Long, UserData> users) {
    if (user != null && users.containsKey(user)) {
      return users.get(user).getUserSign();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private TaskType type;
  private long id;

  private DateTime start;
  private DateTime end;

  private String summary;
  private String description;

  private String companyName;

  private TaskPriority priority;
  private TaskStatus status;

  private Long owner;
  private Long executor;

  private Collection<Long> observers;

  private String background;
  private String foreground;

  private Long style;

  public CalendarTask(TaskType type, long id, SimpleRow row) {
    this.type = type;
    this.id = id;

    this.start = row.getDateTime(TaskConstants.COL_START_TIME);
    this.end = row.getDateTime(TaskConstants.COL_FINISH_TIME);

    this.summary = row.getValue(TaskConstants.COL_SUMMARY);
    this.description = row.getValue(TaskConstants.COL_DESCRIPTION);

    this.companyName = row.getValue(ALS_COMPANY_NAME);

    this.priority = EnumUtils.getEnumByIndex(TaskPriority.class,
        row.getInt(TaskConstants.COL_PRIORITY));
    this.status = EnumUtils.getEnumByIndex(TaskStatus.class,
        row.getInt(TaskConstants.COL_STATUS));

    this.owner = row.getLong(TaskConstants.COL_OWNER);
    this.executor = row.getLong(TaskConstants.COL_EXECUTOR);
  }

  private CalendarTask() {
    super();
  }

  @Override
  public CalendarItem copy() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (members[i]) {
        case TYPE:
          setType(Codec.unpack(TaskType.class, value));
          break;
        case ID:
          setId(BeeUtils.toLong(value));
          break;

        case START:
          setStart(DateTime.restore(value));
          break;
        case END:
          setEnd(DateTime.restore(value));
          break;

        case SUMMARY:
          setSummary(value);
          break;
        case DESCRIPTION:
          setDescription(value);
          break;

        case COMPANY_NAME:
          setCompanyName(value);
          break;

        case PRIORITY:
          setPriority(Codec.unpack(TaskPriority.class, value));
          break;
        case STATUS:
          setStatus(Codec.unpack(TaskStatus.class, value));
          break;

        case OWNER:
          setOwner(BeeUtils.toLongOrNull(value));
          break;
        case EXECUTOR:
          setExecutor(BeeUtils.toLongOrNull(value));
          break;

        case OBSERVERS:
          setObservers(Codec.deserializeIdList(value));
          break;

        case BACKGRUOND:
          setBackground(value);
          break;
        case FOREGROUND:
          setForeground(value);
          break;

        case STYLE:
          setStyle(BeeUtils.toLongOrNull(value));
          break;
      }
    }
  }

  @Override
  public String getBackground() {
    return background;
  }

  @Override
  public String getCompactTemplate() {
    return COMPACT_TEMPLATE;
  }

  @Override
  public String getCompanyName() {
    return companyName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public DateTime getEnd() {
    return end;
  }

  public Long getExecutor() {
    return executor;
  }

  @Override
  public String getForeground() {
    return foreground;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public ItemType getItemType() {
    return ItemType.TASK;
  }

  @Override
  public String getMultiBodyTemplate() {
    return MULTI_BODY_TEMPLATE;
  }

  @Override
  public String getMultiHeaderTemplate() {
    return MULTI_HEADER_TEMPLATE;
  }

  public Collection<Long> getObservers() {
    return observers;
  }

  public Long getOwner() {
    return owner;
  }

  @Override
  public String getPartialBodyTemplate() {
    return PARTIAL_BODY_TEMPLATE;
  }

  @Override
  public String getPartialHeaderTemplate() {
    return PARTIAL_HEADER_TEMPLATE;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  @Override
  public Long getSeparatedAttendee() {
    return null;
  }

  @Override
  public String getSimpleBodyTemplate() {
    return SIMPLE_BODY_TEMPLATE;
  }

  @Override
  public String getSimpleHeaderTemplate() {
    return SIMPLE_HEADER_TEMPLATE;
  }

  @Override
  public DateTime getStart() {
    return start;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getStringTemplate() {
    return STRING_TEMPLATE;
  }

  @Override
  public Long getStyle() {
    return style;
  }

  @Override
  public Map<String, String> getSubstitutes(long calendarId, Map<Long, UserData> users,
      boolean addLabels, Function<HasDateValue, String> dateTimeRenderer,
      BiFunction<HasDateValue, HasDateValue, String> periodRenderer) {

    Map<String, String> result = new HashMap<>();

    result.put(wrap(TaskConstants.COL_TASK_ID), build(Localized.dictionary().captionId(),
        BeeUtils.toString(getId()), addLabels));

    result.put(wrap(COL_START_DATE_TIME), build(Localized.dictionary().crmStartDate(),
        dateTimeRenderer.apply(getStart()), addLabels));
    result.put(wrap(COL_END_DATE_TIME), build(Localized.dictionary().crmFinishDate(),
        dateTimeRenderer.apply(getEnd()), addLabels));

    result.put(wrap(COL_SUMMARY), build(Localized.dictionary().crmTaskSubject(), getSummary(),
        addLabels));
    result.put(wrap(COL_DESCRIPTION), build(Localized.dictionary().crmTaskDescription(),
        getDescription(), addLabels));

    result.put(wrap(ALS_COMPANY_NAME), build(Localized.dictionary().company(), getCompanyName(),
        addLabels));

    result.put(wrap(TaskConstants.COL_PRIORITY), build(Localized.dictionary().crmTaskPriority(),
        (getPriority() == null) ? BeeConst.STRING_EMPTY : getPriority().getCaption(), addLabels));
    result.put(wrap(COL_STATUS), build(Localized.dictionary().crmTaskStatus(),
        (getStatus() == null) ? BeeConst.STRING_EMPTY : getStatus().getCaption(), addLabels));

    result.put(wrap(TaskConstants.COL_OWNER), build(Localized.dictionary().crmTaskManager(),
        formatUser(getOwner(), users), addLabels));
    result.put(wrap(TaskConstants.COL_EXECUTOR), build(Localized.dictionary().crmTaskExecutor(),
        formatUser(getExecutor(), users), addLabels));

    if (BeeUtils.isEmpty(getObservers())) {
      result.put(wrap(TaskConstants.PROP_OBSERVERS), BeeConst.STRING_EMPTY);
    } else {
      List<String> names = new ArrayList<>();
      for (Long observer : getObservers()) {
        names.add(formatUser(observer, users));
      }
      result.put(wrap(TaskConstants.PROP_OBSERVERS),
          build(Localized.dictionary().crmTaskObservers(), joinChildren(names), addLabels));
    }

    result.put(wrap(KEY_PERIOD), build(Localized.dictionary().period(),
        periodRenderer.apply(getStart(), getEnd()), addLabels));

    return result;
  }

  @Override
  public String getSummary() {
    return summary;
  }

  @Override
  public String getTitleTemplate() {
    return TITLE_TEMPLATE;
  }

  public TaskType getType() {
    return type;
  }

  @Override
  public boolean isEditable(Long userId) {
    return isOwner(userId);
  }

  @Override
  public boolean isMovable(Long userId) {
    return isWhole() && isOwner(userId);
  }

  @Override
  public boolean isRemovable(Long userId) {
    return false;
  }

  @Override
  public boolean isResizable(Long userId) {
    return isWhole() && isOwner(userId) && getStatus() != null
        && TaskStatus.in(getStatus().ordinal(), TaskStatus.NOT_VISITED, TaskStatus.VISITED,
            TaskStatus.ACTIVE);
  }

  @Override
  public boolean isVisible(Long userId) {
    return userId != null;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case TYPE:
          arr[i++] = Codec.pack(getType());
          break;
        case ID:
          arr[i++] = getId();
          break;

        case START:
          arr[i++] = getStart();
          break;
        case END:
          arr[i++] = getEnd();
          break;

        case SUMMARY:
          arr[i++] = getSummary();
          break;
        case DESCRIPTION:
          arr[i++] = getDescription();
          break;

        case COMPANY_NAME:
          arr[i++] = getCompanyName();
          break;

        case PRIORITY:
          arr[i++] = Codec.pack(getPriority());
          break;
        case STATUS:
          arr[i++] = Codec.pack(getStatus());
          break;

        case OWNER:
          arr[i++] = getOwner();
          break;
        case EXECUTOR:
          arr[i++] = getExecutor();
          break;

        case OBSERVERS:
          arr[i++] = getObservers();
          break;

        case FOREGROUND:
          arr[i++] = getForeground();
          break;
        case BACKGRUOND:
          arr[i++] = getBackground();
          break;

        case STYLE:
          arr[i++] = getStyle();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public void setBackground(String background) {
    this.background = background;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setEnd(DateTime end) {
    this.end = end;
  }

  public void setExecutor(Long executor) {
    this.executor = executor;
  }

  public void setForeground(String foreground) {
    this.foreground = foreground;
  }

  public void setObservers(Collection<Long> observers) {
    this.observers = observers;
  }

  public void setOwner(Long owner) {
    this.owner = owner;
  }

  public void setPriority(TaskPriority priority) {
    this.priority = priority;
  }

  public void setStart(DateTime start) {
    this.start = start;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public void setStyle(Long style) {
    this.style = style;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  private boolean isOwner(Long userId) {
    return userId != null && Objects.equals(getOwner(), userId);
  }

  private void setId(long id) {
    this.id = id;
  }

  private void setType(TaskType type) {
    this.type = type;
  }
}
