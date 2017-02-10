package com.butent.bee.client.modules.administration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_STAGE;

import com.butent.bee.client.data.Data;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Stage {

  private final class StageCondition {
    private final String field;
    private final Operator op;
    private final String value;

    private StageCondition(String field, Operator op, String value) {
      this.field = Assert.notEmpty(field);
      this.op = Assert.notNull(op);
      this.value = value;
    }

    public boolean applies(IsRow row) {
      boolean ok;
      String val = Data.getString(dataView, row, field);

      switch (op) {
        case EQ:
          ok = Objects.equals(val, value);
          break;
        case NE:
          ok = !Objects.equals(val, value);
          break;
        case IS_NULL:
          ok = BeeUtils.isEmpty(val);
          break;
        case NOT_NULL:
          ok = !BeeUtils.isEmpty(val);
          break;
        default:
          Assert.unsupported();
          ok = false;
          break;
      }
      return ok;
    }
  }

  private final String dataView;
  private final Long id;
  private final String name;
  private final String confirm;

  private final List<StageCondition> conditions = new ArrayList<>();
  private final Set<String> actions = new HashSet<>();
  private final Set<String> triggers = new HashSet<>();

  Stage(String dataView, Long id, String name, String confirm) {
    this.dataView = Assert.notEmpty(dataView);
    this.id = Assert.notNull(id);
    this.name = Assert.notEmpty(name);
    this.confirm = confirm;
  }

  public boolean active(IsRow row) {
    return Objects.equals(id, Data.getLong(dataView, row, COL_STAGE));
  }

  public void addAction(String action) {
    actions.add(action);
  }

  public void addCondition(String field, Operator op, String value) {
    conditions.add(new StageCondition(field, op, value));
  }

  public void addTrigger(String trigger) {
    triggers.add(trigger);
  }

  public boolean applies(IsRow row) {
    return active(row) || conditions.isEmpty()
        || conditions.stream().anyMatch(cond -> cond.applies(row));
  }

  public String getConfirm() {
    return confirm;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean hasAction(String action) {
    return actions.contains(action);
  }

  public boolean hasTrigger(String triger) {
    return triggers.contains(triger);
  }
}
