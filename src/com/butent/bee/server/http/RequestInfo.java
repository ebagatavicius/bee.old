package com.butent.bee.server.http;

import com.google.common.net.HttpHeaders;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

/**
 * HttpServletRequest wrapper.
 */

public class RequestInfo implements HasExtendedInfo, HasOptions {

  private static int counter;

  private final HttpServletRequest request;

  private final String httpMethod;
  private final String query;

  private final Map<String, String> headers;
  private final Map<String, String> params;

  private final Map<String, String> vars;

  private final int contentLen;

  private String contentTypeHeader;

  private String content;

  private String id;

  private String service;
  private String subService;

  private String options;

  private ContentType contentType;

  public RequestInfo(HttpServletRequest req, boolean encoded) {
    super();
    counter++;

    this.request = req;

    this.httpMethod = req.getMethod();
    this.query = req.getQueryString();

    this.headers = HttpUtils.getHeaders(req, false);
    this.params = HttpUtils.getParameters(req, false);

    if (!BeeUtils.isEmpty(headers)) {
      for (Map.Entry<String, String> el : headers.entrySet()) {
        setRpcInfo(el.getKey(), el.getValue());
      }
    }

    if (!BeeUtils.isEmpty(params)) {
      for (Map.Entry<String, String> el : params.entrySet()) {
        setRpcInfo(el.getKey(), el.getValue());
      }
    }

    if (req.getContentLength() > 0) {
      this.contentTypeHeader = req.getContentType();
      if (encoded) {
        this.content = CommUtils.getContent(getContentType(),
            Codec.fromBytes(Base64.getDecoder().decode(HttpUtils.readContent(req))));
      } else {
        this.content = CommUtils.getContent(getContentType(), HttpUtils.readContent(req));
      }
    } else {
      this.contentTypeHeader = null;
      this.content = null;
    }
    this.contentLen = BeeUtils.length(this.content);

    if (isData()) {
      this.vars = Codec.deserializeLinkedHashMap(content);
    } else {
      this.vars = null;
    }
  }

  public String getContent() {
    return content;
  }

  public int getContentLen() {
    return contentLen;
  }

  public ContentType getContentType() {
    return contentType;
  }

  public String getContentTypeHeader() {
    return contentTypeHeader;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    if (request == null) {
      return null;
    }

    List<ExtendedProperty> reqInfo = new ArrayList<>();

    if (request.isAsyncStarted()) {
      PropertyUtils.appendExtended(reqInfo,
          HttpUtils.getAsyncContextInfo(request.getAsyncContext()));
    }

    PropertyUtils.appendExtended(reqInfo, HttpUtils.getAttributeInfo(request));

    PropertyUtils.addProperties(reqInfo, false,
        "Auth Type", request.getAuthType(),
        "Character Encoding", request.getCharacterEncoding(),
        "Content Length", request.getContentLength(),
        "Content Type", request.getContentType(),
        "Context Path", request.getContextPath());

    PropertyUtils.appendExtended(reqInfo, HttpUtils.getCookieInfo(request));

    DispatcherType dt = request.getDispatcherType();
    if (dt != null) {
      PropertyUtils.addExtended(reqInfo, "Dispatcher Type", null, dt.toString());
    }

    PropertyUtils.appendExtended(reqInfo, HttpUtils.getHeaderInfo(request));
    PropertyUtils.appendExtended(reqInfo, HttpUtils.getLocaleInfo(request));

    PropertyUtils.addProperties(reqInfo, false,
        "Local Addr", request.getLocalAddr(),
        "Local Name", request.getLocalName(),
        "Local Port", request.getLocalPort(),
        "Method", request.getMethod());

    PropertyUtils.appendExtended(reqInfo, HttpUtils.getParameterInfo(request));

    PropertyUtils.addProperties(reqInfo, false,
        "Path Info", request.getPathInfo(),
        "Path Translated", request.getPathTranslated(),
        "Protocol", request.getProtocol(),
        "Query String", request.getQueryString(),
        "Remote Addr", getRemoteAddr(),
        "Remote Host", getRemoteHost(),
        "Remote Port", request.getRemotePort(),
        "Remote User", getRemoteUser(),
        "Requested Session Id", request.getRequestedSessionId(),
        "Request URI", request.getRequestURI(),
        "Request URL", request.getRequestURL(),
        "Scheme", request.getScheme(),
        "Server Name", request.getServerName(),
        "Server Port", request.getServerPort(),
        "Servlet Path", request.getServletPath(),
        "User Agent", getUserAgent());

    PropertyUtils.appendExtended(reqInfo,
        HttpUtils.getServletContextInfo(request.getServletContext()));
    PropertyUtils.appendExtended(reqInfo,
        HttpUtils.getSessionInfo(request.getSession(false)));

    Principal principal = request.getUserPrincipal();
    if (principal != null) {
      PropertyUtils.addChildren(reqInfo, "User Principal",
          "Name", principal.getName(), "String", principal.toString());
    }

    PropertyUtils.addProperties(reqInfo, false,
        "is Async Started", request.isAsyncStarted(),
        "is Async Supported", request.isAsyncSupported(),
        "is Requested Session Id From Cookie", request.isRequestedSessionIdFromCookie(),
        "is Requested Session Id From URL", request.isRequestedSessionIdFromURL(),
        "is Requested Session Id Valid", request.isRequestedSessionIdValid(),
        "is Secure", request.isSecure());

    return reqInfo;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return BeeUtils.notEmpty(getSubService(), getService());
  }

  @Override
  public String getOptions() {
    return options;
  }

  public String getParameter(int idx) {
    return getParameter(CommUtils.rpcParamName(idx));
  }

  public String getParameter(String name) {
    Assert.notEmpty(name);
    String value = null;

    if (!BeeUtils.isEmpty(getParams())) {
      value = getParams().get(name);
      if (!BeeUtils.isEmpty(value)) {
        return value;
      }
    }

    if (!BeeUtils.isEmpty(getHeaders())) {
      value = getHeaders().get(name);
      if (!BeeUtils.isEmpty(value)) {
        return value;
      }
    }

    if (!BeeUtils.isEmpty(getVars())) {
      value = getVars().get(name);
    }

    return value;
  }

  public boolean getParameterBoolean(String name) {
    return BeeUtils.toBoolean(getParameter(name));
  }

  public DateTime getParameterDateTime(String name) {
    return DateTime.restore(getParameter(name));
  }

  public Double getParameterDouble(String name) {
    return BeeUtils.toDoubleOrNull(getParameter(name));
  }

  public <E extends Enum<?>> E getParameterEnum(String name, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getParameterInt(name));
  }

  public Integer getParameterInt(String name) {
    return BeeUtils.toIntOrNull(getParameter(name));
  }

  public Long getParameterLong(String name) {
    return BeeUtils.toLongOrNull(getParameter(name));
  }

  public Map<String, String> getParams() {
    return params;
  }

  public String getQuery() {
    return query;
  }

  public String getRemoteAddr() {
    return request.getRemoteAddr();
  }

  public String getRemoteHost() {
    return request.getRemoteHost();
  }

  public String getRemoteUser() {
    return request.getRemoteUser();
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public String getService() {
    return service;
  }

  public SubModule getSubModule() {
    String value = getParameter(Service.VAR_SUB_MODULE);
    if (BeeUtils.isEmpty(value)) {
      return null;
    } else {
      return SubModule.parse(value);
    }
  }

  public String getSubService() {
    return subService;
  }

  public String getUserAgent() {
    return request.getHeader(HttpHeaders.USER_AGENT);
  }

  public Map<String, String> getVars() {
    return vars;
  }

  public boolean hasParameter(int idx) {
    return hasParameter(CommUtils.rpcParamName(idx));
  }

  public boolean hasParameter(String name) {
    Assert.notEmpty(name);

    if (!BeeUtils.isEmpty(getParams()) && getParams().containsKey(name)) {
      return true;
    }
    if (!BeeUtils.isEmpty(getHeaders()) && getHeaders().containsKey(name)) {
      return true;
    }
    if (!BeeUtils.isEmpty(getVars()) && getVars().containsKey(name)) {
      return true;
    }

    return false;
  }

  public boolean isData() {
    return getContentLen() > 0 && CommUtils.equals(getContentType(), ContentType.BINARY);
  }

  public boolean isDebug() {
    return BeeUtils.containsSame(options, CommUtils.OPTION_DEBUG);
  }

  public void logHeaders(BeeLogger logger) {
    if (BeeUtils.isEmpty(getHeaders())) {
      logger.warning("headers not available");
      return;
    }

    int n = getHeaders().size();
    int i = 0;

    for (Map.Entry<String, String> el : getHeaders().entrySet()) {
      logger.info("Header", BeeUtils.progress(++i, n), el.getKey(), el.getValue());
    }
  }

  public void logParams(BeeLogger logger) {
    if (BeeUtils.isEmpty(getParams())) {
      logger.warning("Parameters not available");
      return;
    }

    int n = getParams().size();
    int i = 0;

    for (Map.Entry<String, String> el : getParams().entrySet()) {
      logger.info("Parameter", BeeUtils.progress(++i, n), el.getKey(), el.getValue());
    }
  }

  public void logVars(BeeLogger logger) {
    if (BeeUtils.isEmpty(getVars())) {
      if (isData()) {
        logger.warning("Vars not available");
      }
      return;
    }

    int n = getVars().size();
    int i = 0;

    for (Map.Entry<String, String> el : getVars().entrySet()) {
      logger.info("Var", BeeUtils.progress(++i, n), el.getKey(), el.getValue());
    }
  }

  public boolean parameterEquals(int idx, String value) {
    Assert.notEmpty(value);
    return BeeUtils.same(getParameter(idx), value);
  }

  public boolean parameterEquals(String name, String value) {
    Assert.notEmpty(name);
    Assert.notEmpty(value);
    return BeeUtils.same(getParameter(name), value);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  public String toString() {
    return BeeUtils.join(BeeConst.DEFAULT_ROW_SEPARATOR,
        BeeUtils.joinOptions("counter", BeeUtils.toString(counter),
            "http method", httpMethod,
            "id", id,
            "service", service,
            "sub service", subService,
            "options", options),
        headers, params);
  }

  private void setRpcInfo(String nm, String v) {
    if (BeeUtils.isEmpty(nm) || BeeUtils.isEmpty(v)) {
      return;
    }

    switch (nm) {
      case Service.RPC_VAR_QID:
        id = v;
        break;

      case Service.RPC_VAR_SVC:
        service = v;
        break;

      case Service.RPC_VAR_SUB:
        subService = v;
        break;

      case Service.RPC_VAR_OPT:
        options = v;
        break;

      case Service.RPC_VAR_CTP:
        contentType = CommUtils.getContentType(v);
        break;
    }
  }
}
