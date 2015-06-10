package com.butent.bee.client.modules.transport.logistics;

import com.butent.bee.client.modules.transport.SelfServiceScreen;
import com.butent.bee.shared.ui.UserInterface;

public class LogSelfServiceScreen extends SelfServiceScreen {

  public LogSelfServiceScreen() {
    super();
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.SELF_SERVICE_LOG;
  }
}
