package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SummaryChangeEvent extends GwtEvent<SummaryChangeEvent.Handler> {

  @FunctionalInterface
  public interface Handler extends EventHandler {
    void onSummaryChange(SummaryChangeEvent event);
  }

  @FunctionalInterface
  public interface Renderer extends Function<Map<String, Value>, String> {
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Collection<HasSummaryChangeHandlers> findSources(Widget parent) {
    Collection<HasSummaryChangeHandlers> sources = new ArrayList<>();

    if (parent instanceof HasSummaryChangeHandlers) {
      if (((HasSummaryChangeHandlers) parent).summarize()) {
        sources.add((HasSummaryChangeHandlers) parent);
      }

    } else if (parent instanceof HasOneWidget) {
      sources.addAll(findSources(((HasOneWidget) parent).getWidget()));

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        sources.addAll(findSources(child));
      }
    }

    return sources;
  }

  public static void fire(HasSummaryChangeHandlers source) {
    Assert.notNull(source);
    source.fireEvent(new SummaryChangeEvent(source.getId(), source.getSummary()));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public static void maybeFire(HasSummaryChangeHandlers source) {
    if (source != null && source.summarize()) {
      fire(source);
    }
  }

  public static void maybeSummarize(Widget widget) {
    if (widget instanceof HasSummaryChangeHandlers) {
      maybeFire((HasSummaryChangeHandlers) widget);
    }
  }

  public static String renderSummary(Collection<Value> values) {
    if (BeeUtils.isEmpty(values)) {
      return BeeConst.STRING_EMPTY;
    }

    List<String> messages = new ArrayList<>();
    int size = 0;

    for (Value value : values) {
      if (value != null && !value.isEmpty()) {
        switch (value.getType()) {
          case BOOLEAN:
            if (BooleanValue.TRUE.equals(value)) {
              size++;
            }
            break;

          case INTEGER:
            size += value.getInteger();
            break;

          default:
            messages.add(value.render(Format.getDateRenderer(), Format.getDateTimeRenderer()));
        }
      }
    }

    if (size > 0) {
      if (messages.isEmpty()) {
        return BeeUtils.toString(size);
      }
      messages.add(BeeUtils.toString(size));
    }

    if (messages.isEmpty()) {
      return BeeConst.STRING_EMPTY;
    } else if (messages.size() == 1) {
      return messages.get(0);
    } else {
      return BeeUtils.joinWords(messages);
    }
  }

  private final String sourceId;
  private final Value value;

  private SummaryChangeEvent(String sourceId, Value value) {
    this.sourceId = sourceId;
    this.value = value;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getSourceId() {
    return sourceId;
  }

  public Value getValue() {
    return value;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSummaryChange(this);
  }
}
