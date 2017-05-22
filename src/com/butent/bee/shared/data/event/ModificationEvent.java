package com.butent.bee.shared.data.event;

import com.google.common.collect.Lists;
import com.google.web.bindery.event.shared.Event;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.List;

public abstract class ModificationEvent<H> extends Event<H> implements DataEvent, BeeSerializable {

  public enum Kind {
    INSERT("ins") {
      @Override
      ModificationEvent<?> createEvent() {
        return new RowInsertEvent();
      }
    },
    UPDATE_CELL("cell") {
      @Override
      ModificationEvent<?> createEvent() {
        return new CellUpdateEvent();
      }
    },
    UPDATE_ROW("row") {
      @Override
      ModificationEvent<?> createEvent() {
        return new RowUpdateEvent();
      }
    },
    DELETE_ROW("del") {
      @Override
      ModificationEvent<?> createEvent() {
        return new RowDeleteEvent();
      }
    },
    DELETE_MULTI("mu") {
      @Override
      ModificationEvent<?> createEvent() {
        return new MultiDeleteEvent();
      }
    },
    DATA_CHANGE("ch") {
      @Override
      ModificationEvent<?> createEvent() {
        return new DataChangeEvent();
      }
    };

    private final String brief;

    Kind(String brief) {
      this.brief = brief;
    }

    abstract ModificationEvent<?> createEvent();
  }

  private static BeeLogger logger = LogUtils.getLogger(ModificationEvent.class);

  public static ModificationEvent<?> decode(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr == null || arr.length != 2) {
      logger.severe("cannot decode modification event", s);
      return null;
    }

    Kind kind = Codec.unpack(Kind.class, arr[0]);
    if (kind == null) {
      logger.severe("cannot decode modification event kind", arr[0]);
      return null;
    }

    ModificationEvent<?> event = kind.createEvent();
    event.deserialize(arr[1]);

    return event;
  }

  private transient Locality locality;

  public String brief() {
    if (getKind() == null) {
      return BeeConst.UNKNOWN;
    } else {
      return BeeUtils.joinWords(getKind().brief, getViewNames());
    }
  }

  public boolean containsAny(Collection<String> viewNames) {
    return BeeUtils.intersects(viewNames, getViewNames());
  }

  public String encode() {
    List<String> data = Lists.newArrayList(Codec.pack(getKind()), serialize());
    return Codec.beeSerialize(data);
  }

  public abstract Kind getKind();

  public Locality getLocality() {
    return locality;
  }

  public abstract Collection<String> getViewNames();

  public boolean isSpookyActionAtADistance() {
    return getLocality() == Locality.ENTANGLED;
  }

  public void setLocality(Locality locality) {
    this.locality = locality;
  }

  @Override
  public abstract String toString();
}
