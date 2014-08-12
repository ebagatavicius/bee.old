package com.butent.bee.server.http;

import com.google.common.net.MediaType;

import com.butent.bee.server.concurrency.Counter;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Contains utility HTTP request related functions like getting header information or execute binary
 * read of requests.
 */

public final class HttpUtils {

  private static BeeLogger logger = LogUtils.getLogger(HttpUtils.class);

  public static void badRequest(HttpServletResponse resp, String... messages) {
    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, ArrayUtils.joinWords(messages));
  }

  public static String counterInfo(String name, Object obj) {
    if (obj instanceof Counter) {
      return NameUtils.addName(name, ((Counter) obj).toString());
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public static Collection<ExtendedProperty> getAsyncContextInfo(AsyncContext ac) {
    if (ac == null) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Async Context";

    PropertyUtils.addProperties(info, true, root, "Timeout", ac.getTimeout(),
        root, "Has Original Request And Response", ac.hasOriginalRequestAndResponse());

    return info;
  }

  public static Collection<ExtendedProperty> getAttributeInfo(HttpServletRequest req) {
    if (req == null) {
      return null;
    }

    Enumeration<String> lst = req.getAttributeNames();
    if (lst == null) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Attribute";
    String nm;

    while (lst.hasMoreElements()) {
      nm = lst.nextElement();
      PropertyUtils.addExtended(info, root, nm, req.getAttribute(nm));
    }

    return info;
  }

  public static Collection<ExtendedProperty> getCookieInfo(HttpServletRequest req) {
    if (req == null) {
      return null;
    }

    Cookie[] arr = req.getCookies();
    if (arr == null) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    PropertyUtils.addExtended(info, "Cookies", "cnt", arr.length);

    String nm;

    for (Cookie coo : arr) {
      nm = coo.getName();
      if (BeeUtils.isEmpty(nm)) {
        continue;
      }

      PropertyUtils.addProperties(info, true, nm, "Value", coo.getValue(),
          nm, "Comment", coo.getComment(), nm, "Domain", coo.getDomain(),
          nm, "Max Age", coo.getMaxAge(), nm, "Path", coo.getPath(),
          nm, "Secure", coo.getSecure(), nm, "Version", coo.getVersion(),
          nm, "Http Only", coo.isHttpOnly());
    }
    return info;
  }

  public static Collection<ExtendedProperty> getHeaderInfo(HttpServletRequest req) {
    if (req == null) {
      return null;
    }

    Map<String, String> lst = HttpUtils.getHeaders(req, false);
    if (BeeUtils.isEmpty(lst)) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Header";
    String nm;
    String v;

    for (Map.Entry<String, String> el : lst.entrySet()) {
      nm = el.getKey();
      if (BeeUtils.isEmpty(nm)) {
        continue;
      }

      v = el.getValue();
      if (BeeUtils.isEmpty(v)) {
        continue;
      }

      PropertyUtils.addExtended(info, root, nm, v);
    }
    return info;
  }

  public static Map<String, String> getHeaders(HttpServletRequest req, boolean decode) {
    Map<String, String> headers = new HashMap<>();
    if (req == null) {
      return headers;
    }

    Enumeration<String> lst = req.getHeaderNames();
    if (lst == null) {
      return headers;
    }

    String nm;
    String v;

    while (lst.hasMoreElements()) {
      nm = lst.nextElement();
      if (BeeUtils.isEmpty(nm)) {
        continue;
      }

      Enumeration<String> values = req.getHeaders(nm);
      if (values != null && values.hasMoreElements()) {
        v = BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, Collections.list(values));
      } else {
        v = req.getHeader(nm);
      }

      if (!BeeUtils.isEmpty(v)) {
        headers.put(nm, decode ? Codec.decodeBase64(v) : v);
      }
    }
    return headers;
  }

  public static String getLanguage(HttpServletRequest req) {
    if (req == null) {
      return null;
    } else {
      Locale locale = req.getLocale();
      return (locale == null) ? null : locale.getLanguage();
    }
  }

  public static Collection<ExtendedProperty> getLocaleInfo(HttpServletRequest req) {
    if (req == null) {
      return null;
    }

    Locale loc;
    List<Locale> lst = new ArrayList<>();

    Enumeration<Locale> z = req.getLocales();
    if (z == null) {
      loc = req.getLocale();
      if (loc != null) {
        lst.add(loc);
      }
    } else {
      while (z.hasMoreElements()) {
        lst.add(z.nextElement());
      }
    }

    if (lst.isEmpty()) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root;

    for (int i = 0; i < lst.size(); i++) {
      loc = lst.get(i);
      root = "Locale " + i;

      PropertyUtils.addProperties(info, true, root, "Country", loc.getCountry(),
          root, "Display Country", loc.getDisplayCountry(),
          root, "Display Country loc", loc.getDisplayCountry(loc),
          root, "Display Language", loc.getDisplayLanguage(),
          root, "Display Language loc", loc.getDisplayLanguage(loc),
          root, "Display Name", loc.getDisplayName(),
          root, "Display Name loc", loc.getDisplayName(loc),
          root, "Display Variant", loc.getDisplayVariant(),
          root, "Display Variant loc", loc.getDisplayVariant(loc),
          root, "ISO3 Country", loc.getISO3Country(),
          root, "ISO3 Language", loc.getISO3Language(),
          root, "Language", loc.getLanguage(),
          root, "Variant", loc.getVariant());
    }
    return info;
  }

  public static Collection<ExtendedProperty> getParameterInfo(HttpServletRequest req) {
    if (req == null) {
      return null;
    }

    Map<String, String> lst = HttpUtils.getParameters(req, false);
    if (BeeUtils.isEmpty(lst)) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Parameter";
    String nm;
    String v;

    for (Map.Entry<String, String> el : lst.entrySet()) {
      nm = el.getKey();
      v = el.getValue();
      if (BeeUtils.isEmpty(nm) || BeeUtils.isEmpty(v)) {
        continue;
      }

      PropertyUtils.addExtended(info, root, nm, v);
    }
    return info;
  }

  public static Map<String, String> getParameters(HttpServletRequest req, boolean decode) {
    Map<String, String> params = new HashMap<>();
    if (req == null) {
      return params;
    }

    Map<String, String[]> lst = req.getParameterMap();
    String nm;
    String[] v;

    if (BeeUtils.isEmpty(lst)) {
      Enumeration<String> z = req.getParameterNames();
      if (z == null) {
        return params;
      }

      lst = new HashMap<>();

      while (z.hasMoreElements()) {
        nm = z.nextElement();
        if (BeeUtils.isEmpty(nm)) {
          continue;
        }

        v = req.getParameterValues(nm);
        if (ArrayUtils.isEmpty(v)) {
          v = new String[] {req.getParameter(nm)};
        }
        lst.put(nm, v);
      }
    }

    if (BeeUtils.isEmpty(lst)) {
      return params;
    }

    for (Map.Entry<String, String[]> el : lst.entrySet()) {
      nm = el.getKey();
      v = el.getValue();
      if (BeeUtils.isEmpty(nm) || ArrayUtils.isEmpty(v)) {
        continue;
      }

      for (int i = 0; i < v.length; i++) {
        params.put(nm, decode ? Codec.decodeBase64(v[i]) : v[i]);
      }
    }
    return params;
  }

  public static Collection<ExtendedProperty> getServletContextInfo(ServletContext sc) {
    if (sc == null) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Servlet Context";
    String nm;
    String v;
    int c;
    String[] arr;

    Enumeration<String> lst = sc.getAttributeNames();
    if (lst != null) {
      while (lst.hasMoreElements()) {
        nm = lst.nextElement();

        Object attribute = sc.getAttribute(nm);
        if (attribute == null) {
          continue;
        }
        v = BeeUtils.trim(attribute.toString());
        if (BeeUtils.isEmpty(v)) {
          continue;
        }

        if (v.length() > 100) {
          arr = splitValue(v);
          if (arr == null) {
            c = 0;
          } else {
            c = arr.length;
          }

          if (c > 1) {
            for (int i = 0; i < c; i++) {
              PropertyUtils.addExtended(info, root + " attribute " + nm,
                  BeeUtils.toString(i), arr[i]);
            }
            continue;
          }
        }
        PropertyUtils.addExtended(info, root + " attribute " + nm, null, v);
      }
    }

    PropertyUtils.addExtended(info, root, "Context Path", sc.getContextPath());

    Set<SessionTrackingMode> trackMd = sc.getDefaultSessionTrackingModes();
    if (!BeeUtils.isEmpty(trackMd)) {
      for (SessionTrackingMode md : trackMd) {
        PropertyUtils.addExtended(info, root, "Default Session Tracking Mode", md.name());
      }
    }

    trackMd = sc.getEffectiveSessionTrackingModes();
    if (!BeeUtils.isEmpty(trackMd)) {
      for (SessionTrackingMode md : trackMd) {
        PropertyUtils.addExtended(info, root, "Effective Session Tracking Mode", md.name());
      }
    }

    PropertyUtils.addProperties(info, true,
        root, "Effective Major Version", sc.getEffectiveMajorVersion(),
        root, "Effective Minor Version", sc.getEffectiveMinorVersion(),
        root, "Major Version", sc.getMajorVersion(),
        root, "Minor Version", sc.getMinorVersion(),
        root, "Server Info", sc.getServerInfo(),
        root, "Servlet Context Name", sc.getServletContextName());

    lst = sc.getInitParameterNames();
    if (lst != null) {
      while (lst.hasMoreElements()) {
        nm = lst.nextElement();
        PropertyUtils.addExtended(info, root + " init parameter", nm, sc.getInitParameter(nm));
      }
    }

    return info;
  }

  public static Collection<ExtendedProperty> getSessionInfo(HttpSession hs) {
    if (hs == null) {
      return null;
    }

    Collection<ExtendedProperty> info = new ArrayList<>();
    String root = "Session";
    String nm;

    Enumeration<String> lst = hs.getAttributeNames();
    if (lst != null) {
      while (lst.hasMoreElements()) {
        nm = lst.nextElement();
        PropertyUtils.addExtended(info, root + " attribute", nm, hs.getAttribute(nm));
      }
    }

    PropertyUtils.addProperties(info, true, root, "Id", hs.getId(),
        root, "Creation Time", hs.getCreationTime(),
        root, "Last Accessed Time", hs.getLastAccessedTime(),
        root, "Max Inactive Interval", hs.getMaxInactiveInterval(),
        root, "Is New", hs.isNew());

    return info;
  }

  public static void incrCounter(Object obj) {
    if (obj instanceof Counter) {
      ((Counter) obj).incCounter();
    }
  }

  public static String readBinary(HttpServletRequest req, int len) {
    Assert.notNull(req);
    Assert.isPositive(len);
    Assert.isEven(len);

    byte[] arr = new byte[len];
    boolean ok = true;

    try {
      ServletInputStream stream = req.getInputStream();
      int tot = stream.read(arr, 0, len);
      Assert.isPositive(tot);

      while (tot < len) {
        int cnt = stream.read(arr, tot, len - tot);
        Assert.isPositive(cnt);
        tot += cnt;
      }
      stream.close();
    } catch (IOException ex) {
      logger.error(ex);
      ok = false;
    }

    if (ok) {
      return Codec.fromBytes(arr);
    } else {
      return null;
    }
  }

  public static String readContent(HttpServletRequest req) {
    Assert.notNull(req);

    StringBuffer sb = new StringBuffer();
    int cbSize = 2048;
    char[] cbuf = new char[cbSize];
    int len;

    try {
      BufferedReader reader = req.getReader();

      do {
        len = reader.read(cbuf, 0, cbSize);
        if (len > 0) {
          sb.append(cbuf, 0, len);
        } else {
          break;
        }
      } while (len > 0);

      reader.close();
    } catch (IOException ex) {
      logger.error(ex);
    }

    return sb.toString();
  }

  public static String readPart(HttpServletRequest req, String name) {
    Assert.notNull(req);
    Assert.notEmpty(name);

    String content = null;
    try {
      Part part = req.getPart(name);
      if (part != null) {
        content = FileUtils.streamToString(part.getInputStream());
      }
    } catch (IOException ex) {
      logger.error(ex);
    } catch (ServletException ex) {
      logger.error(ex);
    }
    return content;
  }

  public static void sendError(HttpServletResponse resp, int errorCode, String err) {
    try {
      logger.severe(err);
      resp.sendError(errorCode, err);
    } catch (IOException e) {
      logger.error(e);
    }
  }

  public static void sendResponse(HttpServletResponse resp, String html) {
    resp.setContentType(MediaType.HTML_UTF_8.toString());
    PrintWriter writer;

    try {
      writer = resp.getWriter();
      writer.print(html);
      writer.flush();
    } catch (IOException ex) {
      logger.error(ex);
    }
  }

  private static String[] splitValue(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else if (s.contains(", ")) {
      return s.split(", ");
    } else if (s.contains(";")) {
      return s.split(";");
    } else {
      return null;
    }
  }

  private HttpUtils() {
  }
}
