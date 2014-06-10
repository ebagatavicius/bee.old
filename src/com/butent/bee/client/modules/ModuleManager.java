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

public final class ModuleManager {

  public static void maybeInitialize(final Command command) {
    CalendarKeeper.ensureData(command);
  }

  public static void onLoad() {
    AdministrationKeeper.register();
    ClassifierKeeper.register();
    TransportHandler.register();

    TasksKeeper.register();
    DocumentsHandler.register();

    CalendarKeeper.register();
    MailKeeper.register();
    TradeKeeper.register();

    EcKeeper.register();
    DiscussionsKeeper.register();
    ServiceKeeper.register();
  }

  private ModuleManager() {
  }
}
