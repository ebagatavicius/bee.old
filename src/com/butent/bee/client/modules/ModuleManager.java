package com.butent.bee.client.modules;

import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.modules.discussions.DiscussionsKeeper;
import com.butent.bee.client.modules.documents.DocumentsHandler;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.mail.MailKeeper;
import com.butent.bee.client.modules.service.ServiceKeeper;
import com.butent.bee.client.modules.tasks.TasksKeeper;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.utils.Command;
import com.butent.bee.shared.rights.Module;

public final class ModuleManager {

  public static void maybeInitialize(final Command command) {
    CalendarKeeper.ensureData(command);
  }

  public static void onLoad() {
    AdministrationKeeper.register();
    ClassifierKeeper.register();

    if (Module.TRANSPORT.isEnabled()) {
      TransportHandler.register();
    }

    if (Module.TASKS.isEnabled()) {
      TasksKeeper.register();
    }

    if (Module.DOCUMENTS.isEnabled()) {
      DocumentsHandler.register();
    }

    if (Module.CALENDAR.isEnabled()) {
      CalendarKeeper.register();
    }

    if (Module.MAIL.isEnabled()) {
      MailKeeper.register();
    }

    if (Module.TRADE.isEnabled()) {
      TradeKeeper.register();
    }

    if (Module.ECOMMERCE.isEnabled()) {
      EcKeeper.register();
    }

    if (Module.DISCUSSIONS.isEnabled()) {
      DiscussionsKeeper.register();
    }

    if (Module.SERVICE.isEnabled()) {
      ServiceKeeper.register();
    }
  }

  private ModuleManager() {
  }
}
