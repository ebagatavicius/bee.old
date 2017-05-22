package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataChangeEvent extends ModificationEvent<DataChangeEvent.Handler> {

  public enum Effect {
    CANCEL, REFRESH, RESET
  }

  public interface Handler {
    void onDataChange(DataChangeEvent event);
  }

  public static final EnumSet<Effect> RESET_REFRESH = EnumSet.of(Effect.RESET, Effect.REFRESH);
  public static final EnumSet<Effect> CANCEL_RESET_REFRESH = EnumSet.allOf(Effect.class);

  private static final EnumSet<Effect> JUST_REFRESH = EnumSet.of(Effect.REFRESH);

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(FiresModificationEvents em, String viewName, EnumSet<Effect> effects,
      Long parentId) {

    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.notEmpty(effects);

    em.fireModificationEvent(new DataChangeEvent(viewName, effects, parentId), Locality.ENTANGLED);
  }

  public static void fire(FiresModificationEvents em, Collection<String> viewNames,
      EnumSet<Effect> effects) {

    Assert.notNull(em);
    Assert.notEmpty(viewNames);
    Assert.notEmpty(effects);

    em.fireModificationEvent(new DataChangeEvent(viewNames, effects), Locality.ENTANGLED);
  }

  public static void fireLocal(FiresModificationEvents em, String viewName,
      EnumSet<Effect> effects, Long parentId) {

    Assert.notNull(em);
    Assert.notEmpty(viewName);
    Assert.notEmpty(effects);

    em.fireModificationEvent(new DataChangeEvent(viewName, effects, parentId), Locality.LOCAL);
  }

  public static void fireLocalRefresh(FiresModificationEvents em, String viewName) {
    fireLocalRefresh(em, viewName, null);
  }

  public static void fireLocalRefresh(FiresModificationEvents em, String viewName, Long parentId) {
    fireLocal(em, viewName, JUST_REFRESH, parentId);
  }

  public static void fireLocalReset(FiresModificationEvents em, String viewName, Long parentId) {
    fireLocal(em, viewName, RESET_REFRESH, parentId);
  }

  public static void fireRefresh(FiresModificationEvents em, String viewName) {
    fireRefresh(em, viewName, null);
  }

  public static void fireRefresh(FiresModificationEvents em, String viewName, Long parentId) {
    fire(em, viewName, JUST_REFRESH, parentId);
  }

  public static void fireRefresh(FiresModificationEvents em, Collection<String> viewNames) {
    fire(em, viewNames, JUST_REFRESH);
  }

  public static void fireReset(FiresModificationEvents em, String viewName, Long parentId) {
    fire(em, viewName, RESET_REFRESH, parentId);
  }

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private final Set<String> viewNames = new HashSet<>();
  private EnumSet<Effect> effects;

  private Long parentId;

  private DataChangeEvent(String viewName, EnumSet<Effect> effects, Long parentId) {
    viewNames.add(viewName);
    this.effects = effects;
    this.parentId = parentId;
  }

  private DataChangeEvent(Collection<String> views, EnumSet<Effect> effects) {
    viewNames.addAll(views);
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
    Assert.lengthEquals(arr, 3);

    viewNames.clear();

    String[] packedViews = Codec.beeDeserializeCollection(arr[0]);
    if (!ArrayUtils.isEmpty(packedViews)) {
      Collections.addAll(viewNames, packedViews);
    }

    this.effects = EnumSet.noneOf(Effect.class);

    String[] packedEffects = Codec.beeDeserializeCollection(arr[1]);
    if (!ArrayUtils.isEmpty(packedEffects)) {
      for (String pe : packedEffects) {
        effects.add(Codec.unpack(Effect.class, pe));
      }
    }

    this.parentId = BeeUtils.toLongOrNull(arr[2]);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public Kind getKind() {
    return Kind.DATA_CHANGE;
  }

  public Long getParentId() {
    return parentId;
  }

  @Override
  public Collection<String> getViewNames() {
    return viewNames;
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
    return BeeUtils.containsSame(viewNames, view);
  }

  @Override
  public String serialize() {
    List<String> packedEffects = new ArrayList<>();
    if (!BeeUtils.isEmpty(effects)) {
      for (Effect effect : effects) {
        packedEffects.add(Codec.pack(effect));
      }
    }

    Object[] arr = new Object[] {getViewNames(), packedEffects, getParentId()};
    return Codec.beeSerialize(arr);
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(getKind(), getViewNames(), effects);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onDataChange(this);
  }
}
