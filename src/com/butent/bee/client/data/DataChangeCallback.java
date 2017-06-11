package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataChangeEvent.Effect;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;

public class DataChangeCallback implements RpcCallback<RowInfoList> {

  private static final EnumSet<Effect> DEFAULT_EFFECTS = EnumSet.of(Effect.REFRESH);

  private final String viewName;
  private final EnumSet<Effect> effects;

  private final Long parentId;

  public DataChangeCallback(String viewName) {
    this(viewName, DEFAULT_EFFECTS);
  }

  public DataChangeCallback(String viewName, Long parentId) {
    this(viewName, DEFAULT_EFFECTS, parentId);
  }

  public DataChangeCallback(String viewName, EnumSet<Effect> effects) {
    this(viewName, effects, null);
  }

  public DataChangeCallback(String viewName, EnumSet<Effect> effects, Long parentId) {
    super();

    this.viewName = viewName;
    this.effects = effects;
    this.parentId = parentId;
  }

  @Override
  public void onSuccess(RowInfoList result) {
    if (!BeeUtils.isEmpty(result)) {
      DataChangeEvent.fire(BeeKeeper.getBus(), viewName, effects, parentId);
    }
  }
}
