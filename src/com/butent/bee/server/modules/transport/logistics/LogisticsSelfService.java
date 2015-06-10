package com.butent.bee.server.modules.transport.logistics;

import com.butent.bee.server.ProxyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.modules.transport.TransportSelfService;
import com.butent.bee.shared.ui.UserInterface;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/logistics/*")
@SuppressWarnings("serial")
public class LogisticsSelfService extends TransportSelfService {

  @EJB
  ProxyBean proxy;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  @Override
  protected UserInterface getInitialUserInterface() {
    return UserInterface.SELF_SERVICE_LOG;
  }
}
