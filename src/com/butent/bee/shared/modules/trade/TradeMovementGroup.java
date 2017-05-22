package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

public enum TradeMovementGroup implements HasLocalizedCaption {

  OPERATION_TYPE("operation_type") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.trdOperationType();
    }
  },

  OPERATION("operation") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.trdOperation();
    }
  },

  WAREHOUSE("warehouse") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.warehouse();
    }
  };

  private final String code;

  TradeMovementGroup(String code) {
    this.code = code;
  }

  private static TradeMovementGroup parse(String input) {
    if (!BeeUtils.isEmpty(input)) {
      for (TradeMovementGroup group : values()) {
        if (BeeUtils.same(group.code, input)) {
          return group;
        }
      }
    }
    return null;
  }

  public static List<TradeMovementGroup> parseList(String input) {
    List<TradeMovementGroup> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(input)) {
      List<String> list = NameUtils.toList(input);

      for (String s : list) {
        TradeMovementGroup group = parse(s);

        if (group != null && !result.contains(group)) {
          result.add(group);
        }
      }
    }

    return result;
  }
}
