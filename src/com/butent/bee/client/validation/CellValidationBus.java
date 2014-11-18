package com.butent.bee.client.validation;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class CellValidationBus implements HasCellValidationHandlers {

  private List<CellValidateEvent.Handler> handlers;

  public CellValidationBus() {
    super();
  }

  @Override
  public HandlerRegistration addCellValidationHandler(final CellValidateEvent.Handler handler) {
    Assert.notNull(handler);
    if (handlers == null) {
      handlers = new ArrayList<>();
    }

    if (!handlers.contains(handler)) {
      handlers.add(handler);
    }

    return new HandlerRegistration() {
      @Override
      public void removeHandler() {
        handlers.remove(handler);
      }
    };
  }

  @Override
  public Boolean fireCellValidation(CellValidateEvent event) {
    Assert.notNull(event);
    Boolean ok = true;

    if (!event.isCanceled() && handlers != null) {
      for (CellValidateEvent.Handler handler : handlers) {
        ok = handler.validateCell(event);

        if (BeeUtils.isTrue(ok)) {
          if (event.isCanceled()) {
            break;
          }
        } else {
          event.cancel();
          break;
        }
      }
    }
    return ok;
  }
}
