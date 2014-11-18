package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DataChangeEvent extends ModificationEvent<DataChangeEvent.Handler> {

  public enum Effect {
    CANCEL, REFRESH, RESET
  }

  public interface Handler {
    void onDataChange(DataChangeEvent event);
  }

  public static final EnumSet<Effect> CANCEL_RESET_REFRESH =
      EnumSet.of(Effect.CANCEL, Effect.REFRESH, Effect.RESET);

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents em, String viewName, EnumSet<Effect> effects) {
    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.notEmpty(effects);

    em.fireModificationEvent(new DataChangeEvent(viewName, effects), Locality.ENTANGLED);
  }

  public static void fireLocal(FiresModificationEvents em, String viewName,
      EnumSet<Effect> effects) {
    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.notEmpty(effects);

    em.fireModificationEvent(new DataChangeEvent(viewName, effects), Locality.LOCAL);
  }

  public static void fireLocalRefresh(FiresModificationEvents em, String viewName) {
    fireLocal(em, viewName, EnumSet.of(Effect.REFRESH));
  }

  public static void fireLocalReset(FiresModificationEvents em, String viewName) {
    fireLocal(em, viewName, EnumSet.of(Effect.REFRESH, Effect.RESET));
  }

  public static void fireRefresh(FiresModificationEvents em, String viewName) {
    fire(em, viewName, EnumSet.of(Effect.REFRESH));
  }

  public static void fireReset(FiresModificationEvents em, String viewName) {
    fire(em, viewName, EnumSet.of(Effect.REFRESH, Effect.RESET));
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private String viewName;
  private EnumSet<Effect> effects;

  private DataChangeEvent(String viewName, EnumSet<Effect> effects) {
    this.viewName = viewName;
    this.effects = effects;
  }

  DataChangeEvent() {
  }

  public boolean contains(Effect effect) {
    return effects != null && effect != null && effects.contains(effect);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    this.viewName = arr[0];
    this.effects = EnumSet.noneOf(Effect.class);

    String[] packedEffects = Codec.beeDeserializeCollection(arr[1]);
    if (!ArrayUtils.isEmpty(packedEffects)) {
      for (String pe : packedEffects) {
        effects.add(Codec.unpack(Effect.class, pe));
      }
    }
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public Kind getKind() {
    return Kind.DATA_CHANGE;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public boolean hasCancel() {
    return contains(Effect.CANCEL);
  }

  public boolean hasRefresh() {
    return contains(Effect.REFRESH);
  }

  public boolean hasReset() {
    return contains(Effect.RESET);
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  @Override
  public String serialize() {
    List<String> packedEffects = new ArrayList<>();
    if (!BeeUtils.isEmpty(effects)) {
      for (Effect effect : effects) {
        packedEffects.add(Codec.pack(effect));
      }
    }

    Object[] arr = new Object[] {getViewName(), packedEffects};
    return Codec.beeSerialize(arr);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onDataChange(this);
  }
}
