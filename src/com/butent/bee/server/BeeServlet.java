package com.butent.bee.server;

import com.google.common.net.HttpHeaders;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Sends client side HTTP service requests to server, extends Java standard <code>HttpServlet</code>
 * functionality.
 */

@WebServlet(urlPatterns = "/bee")
@SuppressWarnings("serial")
public class BeeServlet extends LoginServlet {

  private static BeeLogger logger = LogUtils.getLogger(BeeServlet.class);

  @EJB
  DispatcherBean dispatcher;

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    long start = System.currentTimeMillis();

    HttpSession session = req.getSession();
    String sessionId = session.getId();

    RequestInfo reqInfo = new RequestInfo(req);

    String meth = reqInfo.getMethod();
    String rid = reqInfo.getId();
    String svc = reqInfo.getService();

    boolean debug = reqInfo.isDebug();

    logger.info("<", rid, meth, svc);
    if (debug) {
      reqInfo.logParams(logger);
      reqInfo.logVars(logger);
      reqInfo.logHeaders(logger);
    }

    if (reqInfo.getContentLen() > 0) {
      logger.info("<", rid, reqInfo.getContentTypeHeader(), reqInfo.getContentType(),
          BeeUtils.bracket(reqInfo.getContentLen()));
      if (debug) {
        logger.info(reqInfo.getContent());
      }
    }

    String reqSid = reqInfo.getParameter(Service.RPC_VAR_SID);

    boolean doLogin = false;
    boolean doLogout = false;

    if (BeeUtils.same(svc, Service.LOGIN)) {
      doLogin = session.getAttribute(Service.VAR_USER) == null;
      doLogout = !doLogin;

    } else if (BeeUtils.same(svc, Service.LOGOUT)) {
      doLogout = true;

    } else if (BeeUtils.isEmpty(reqSid)) {
      doLogout = session.getAttribute(Service.VAR_USER) != null;

    } else if (!BeeUtils.same(reqSid, sessionId)) {
      doLogout = true;
      logger.severe("session id:", "request =", reqSid, "current =", sessionId);
    }

    ResponseObject response;

    if (doLogin) {
      try {
        response = dispatcher.doLogin(reqInfo);
      } catch (EJBException ex) {
        response = ResponseObject.error(ex);
      }

      if (response.hasErrors()) {
        response.log(logger);
        logout(req, session);

      } else {
        Object userData = ((Map<?, ?>) response.getResponse()).get(Service.VAR_USER);

        if (userData instanceof UserData) {
          session.setAttribute(Service.VAR_USER, ((UserData) userData).getUserId());
          session.setAttribute(Service.VAR_FILE_ID,
              BeeUtils.toLong(((UserData) userData).getProperty(Service.VAR_FILE_ID)));
        }
        resp.setHeader(Service.RPC_VAR_SID, sessionId);
        resp.setHeader(Service.RPC_VAR_QID, rid);

        logger.debug("session id:", sessionId);
      }

    } else if (doLogout) {
      dispatcher.beforeLogout(reqInfo);
      logout(req, session);

      try {
        req.getRequestDispatcher(req.getRequestURI()).forward(req, resp);
      } catch (ServletException | IOException ex) {
        logger.error(ex);
      }
      return;

    } else {
      try {
        response = dispatcher.doService(svc, reqInfo);
      } catch (EJBException ex) {
        response = ResponseObject.error(ex);
      }

      resp.setHeader(Service.RPC_VAR_QID, rid);
    }

    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    resp.setHeader(HttpHeaders.PRAGMA, "no-cache");
    resp.setHeader(HttpHeaders.EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");

    ContentType ctp = CommUtils.DEFAULT_RESPONSE_CONTENT_TYPE;
    
    resp.setContentType(CommUtils.getMediaType(ctp));
    resp.setCharacterEncoding(CommUtils.getCharacterEncoding(ctp));
    resp.setHeader(Service.RPC_VAR_CTP, ctp.name());

    String s;
    if (response != null) {
      s = CommUtils.prepareContent(ctp, Codec.beeSerialize(response));
    } else {
      s = BeeConst.STRING_EMPTY;
    }

    logger.info(">", rid, TimeUtils.elapsedSeconds(start), ctp, s.length());

    try {
      PrintWriter out = resp.getWriter();
      out.print(s);
      out.flush();
    } catch (IOException ex) {
      logger.error(ex);
    }
  }

  private static void logout(HttpServletRequest req, HttpSession session) {
    try {
      req.logout();
      session.invalidate();
      logger.debug("logout successful");
    } catch (ServletException ex) {
      logger.warning("logout", ex);
    }
  }
}
