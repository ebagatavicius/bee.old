package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataChangeEvent.Effect;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;

public class DataChangeCallback extends IntCallback {

  private final String viewName;
  private final EnumSet<Effect> effects;

  public DataChangeCallback(String viewName) {
    this(viewName, EnumSet.of(Effect.REFRESH));
  }

  public DataChangeCallback(String viewName, EnumSet<Effect> effects) {
    super();

    this.viewName = viewName;
    this.effects = effects;
  }

  @Override
  public void onSuccess(Integer result) {
    if (BeeUtils.isPositive(result)) {
      DataChangeEvent.fire(BeeKeeper.getBus(), viewName, effects);
    }
  }
}
