package com.butent.bee.server;

import com.google.common.base.Strings;

import static com.butent.bee.shared.html.builder.Factory.*;

import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.HttpConst;
import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Node;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Form;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.html.builder.elements.Input.Type;
import com.butent.bee.shared.html.builder.elements.Link.Rel;
import com.butent.bee.shared.html.builder.elements.Meta;
import com.butent.bee.shared.html.builder.elements.Script;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// CHECKSTYLE:OFF
@WebServlet(urlPatterns = {"/index.html", "/index.htm", "/index.jsp"})
// CHECKSTYLE:ON
@SuppressWarnings("serial")
public class LoginServlet extends HttpServlet {

  protected static final String COMMAND_REGISTER_ID = "command-register";
  protected static final String COMMAND_QUERY_ID = "command-query";

  protected static final String ID_SUFFIX_FIELD = "-field";
  protected static final String ID_SUFFIX_LABEL = "-label";
  protected static final String ID_SUFFIX_INPUT = "-input";
  protected static final String ID_SUFFIX_LIST = "-list";

  protected static final String FORM_NAME = "login";

  protected static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "SignIn-";
  private static final String FAV_ICON = "favicon.ico";
  private static final String LOGO = "bs-logo.png";
  private static final String USER_NAME_LABEL_ID = "user-name-label";
  private static final String PASSWORD_LABEL_ID = "password-label";
  private static final String ERROR_MESSAGE_ID = "error";
  private static final String SUBMIT_BUTTON_ID = "submit";
  private static final String INFO_LABEL_ID = "info-label";
  private static final String INFO_HELP_ID = "info-help";
  private static final String USER_NAME_INPUT_ID = "user";
  private static final String PASSWORD_INPUT_ID = "pswd";
  private static BeeLogger logger = LogUtils.getLogger(LoginServlet.class);
  @EJB
  UserServiceBean usr;

  protected static String event(String func, String param) {
    return func + BeeConst.STRING_LEFT_PARENTHESIS + BeeConst.STRING_QUOT + param
        + BeeConst.STRING_QUOT + BeeConst.STRING_RIGHT_PARENTHESIS;
  }

  protected static String getLanguage(HttpServletRequest req) {
    String language = req.getParameter(HttpConst.PARAM_LOCALE);
    if (BeeUtils.isEmpty(language)) {
      language = SupportedLocale.normalizeLanguage(HttpUtils.getLanguage(req));
    }

    return language;
  }

  protected static String resource(String contextPath, String path) {
    File file = new File(path);
    if (!file.exists()) {
      file = new File(Config.WAR_DIR, path);
      if (!file.exists()) {
        return path;
      }
    }

    String requestPath;
    if (BeeUtils.isEmpty(contextPath) || Paths.isAbsolute(path)) {
      requestPath = path;
    } else {
      requestPath = Paths.buildPath(contextPath, path);
    }

    long time = file.lastModified();
    if (time > 0) {
      requestPath = CommUtils.addTimeStamp(requestPath, new DateTime(time));
    }
    return requestPath;
  }

  private static String generateDictionary(SupportedLocale locale) {
    String language = locale.getLanguage();
    LocalizableConstants constants = Localizations.getPreferredConstants(language);

    JsonObject dictionary = Json.createObjectBuilder()
        .add(USER_NAME_LABEL_ID, constants.loginUserName())
        .add(PASSWORD_LABEL_ID, constants.loginPassword())
        .add(ERROR_MESSAGE_ID, constants.loginFailed())
        .add(SUBMIT_BUTTON_ID, constants.loginSubmit())
        .add(COMMAND_REGISTER_ID, constants.loginCommandRegister())
        .add(COMMAND_QUERY_ID, constants.loginCommandQuery())
        .add(INFO_LABEL_ID, constants.loginInfoLabel())
        .add(INFO_HELP_ID, constants.loginInfoHelp())
        .add(USER_NAME_INPUT_ID, constants.loginUserName())
        .add(PASSWORD_INPUT_ID, constants.loginPassword())
        .build();

    StringWriter strWriter = new StringWriter();
    JsonWriter jsonWriter = Json.createWriter(strWriter);
    jsonWriter.writeObject(dictionary);
    jsonWriter.close();

    return strWriter.toString();
  }

  private static String render(String contextPath, UserInterface ui, SupportedLocale locale) {
    Document doc = new Document();

    doc.getHead().append(meta().encodingDeclarationUtf8());
    if (locale != null) {
      doc.getHead().append(meta().name("gwt:property").content("locale=" + locale.getLanguage()));
    }

    List<Meta> meta = ui.getMeta();
    if (!BeeUtils.isEmpty(meta)) {
      doc.getHead().append(meta);
    }

    doc.getHead().append(
        title().text(ui.getTitle()),
        link().rel(Rel.SHORTCUT_ICON)
            .href(resource(contextPath, Paths.getImagePath(LoginServlet.FAV_ICON))),
        link().rel(Rel.STYLE_SHEET).href(
            "//fonts.googleapis.com/css?family=Open+Sans:700,300,800,400"),
        base().targetBlank());

    for (String styleSheet : ui.getStyleSheets()) {
      doc.getHead().append(link()
          .styleSheet(resource(contextPath, Paths.getStyleSheetPath(styleSheet))));
    }

    for (String script : ui.getScripts()) {
      String src = script.contains("://") ? script
          : resource(contextPath, Paths.getScriptPath(script));
      doc.getHead().append(script().src(src));
    }

    doc.getHead().append(script().src(
        CommUtils.addTimeStamp(Paths.buildPath(contextPath, "bee/bee.nocache.js"),
            new DateTime())));

    return doc.buildLines();
  }

  private static String verboten(String contextPath) {
    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text("Verboten"));

    doc.getBody().append(img().src(resource(contextPath, Paths.getImagePath("answer.jpg")))
        .alt("respect my authoritah"));

    return doc.buildLines();
  }

  public String getLoginForm(HttpServletRequest request, String userName) {

    Input user = new Input();
    Input pass = new Input();
    String contextPath = request.getServletContext().getContextPath();
    String requestLanguage = SupportedLocale.normalizeLanguage(HttpUtils.getLanguage(request));

    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(UserInterface.TITLE),
        link().rel(Rel.SHORTCUT_ICON).href(resource(contextPath, Paths.getImagePath(FAV_ICON))),
        link().styleSheet(resource(contextPath, Paths.getStyleSheetPath("login"))),
        script().src(resource(contextPath, Paths.getScriptPath("login"))));

    String scriptName = getLoginScriptName();
    if (!BeeUtils.isEmpty(scriptName)) {
      doc.getHead().append(script().src(resource(contextPath, Paths.getScriptPath(scriptName))));
    }

    Div panel = div().addClass(STYLE_PREFIX + "Panel").id("login-panel");
    doc.getBody().onLoad(event("onload", requestLanguage)).append(panel);

    Form form = form().addClass(STYLE_PREFIX + "Form").name(FORM_NAME).acceptCharsetUtf8()
        .methodPost();

    form.append(
        img().addClass(STYLE_PREFIX + "Logo").src(resource(contextPath,
            Paths.getImagePath(LOGO))).alt("logo"));

    Div localeContainer = div().addClass(STYLE_PREFIX + "Locale-container");
    Script dictionaries = script();

    for (SupportedLocale locale : SupportedLocale.values()) {
      String language = locale.getLanguage();

      localeContainer.append(
          label().addClass(STYLE_PREFIX + "Locale-label").id(language).text(locale.getCaption())
              .append(
                  input().addClass(STYLE_PREFIX + "Locale-input").type(Type.RADIO)
                      .id(language).name(HttpConst.PARAM_LOCALE).value(language)
                      .onChange("onSelectLanguage(this.id)")));

      String dictionary = generateDictionary(locale);
      dictionaries.text("var dictionary" + language + " = " + dictionary + ";");
    }

    panel.append(label().addClass(STYLE_PREFIX + "infoLabel").id(INFO_LABEL_ID));

    form.append(
        user.addClass(
            STYLE_PREFIX + "Input").addClass(STYLE_PREFIX + "Input-user")
            .name(HttpConst.PARAM_USER).id("user").value(Strings.emptyToNull(userName))
            .maxLength(100).onKeyDown("return goPswd(event)").autofocus().required(),
        pass.addClass(STYLE_PREFIX + "Input").addClass(STYLE_PREFIX + "Input-password")
            .type(Type.PASSWORD).name(HttpConst.PARAM_PASSWORD).id("pswd")
            .maxLength(UiConstants.MAX_PASSWORD_LENGTH).required()
        );

    if (!BeeUtils.isEmpty(userName)) {
      user.addClass(STYLE_PREFIX + "Input-user" + "-Invalid");
      pass.addClass(STYLE_PREFIX + "Input-password" + "-Invalid");
    }
    form.append(button().typeSubmit().addClass(STYLE_PREFIX + "Button").id(SUBMIT_BUTTON_ID));

    form.append(localeContainer);
    form.append(a().href(UiConstants.helpURL()).targetBlank().append(
        label().addClass(STYLE_PREFIX + "Help").id(INFO_HELP_ID)));
    doc.getHead().append(dictionaries);

    panel.append(form);

    Node extension = getLoginExtension(request);
    if (extension != null) {
      panel.append(extension);
    }

    String wtfplUrl = UiConstants.wtfplUrl();

    doc.getBody().append(
        div().addClass(STYLE_PREFIX + "Copyright").title(wtfplUrl)
            .onClick(event("window.open", wtfplUrl))
            .append(
                img().addClass(STYLE_PREFIX + "Copyright-logo")
                    .src(resource(contextPath, UiConstants.wtfplLogo())).alt("wtfpl"),
                span().addClass(STYLE_PREFIX + "Copyright-label").text(UiConstants.wtfplLabel())));

    return doc.buildLines();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    authenticate(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    authenticate(req, resp);
  }

  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    HttpUtils.sendResponse(resp, getInitialPage(req, null));
  }

  protected String getInitialPage(HttpServletRequest req, UserInterface userInterface) {
    String remoteUser = req.getRemoteUser();
    String contextPath = req.getServletContext().getContextPath();
    final String html;

    if (isBlocked(remoteUser)) {
      try {
        req.logout();
      } catch (ServletException e) {
        logger.error(e);
      }
      html = verboten(contextPath);

    } else {
      String language = BeeUtils.trim(req.getParameter(HttpConst.PARAM_LOCALE));
      SupportedLocale userLocale = getUserLocale(remoteUser);

      if (!BeeUtils.isEmpty(language)) {
        SupportedLocale loginLocale = SupportedLocale.getByLanguage(language);

        if (loginLocale != null && loginLocale != userLocale) {
          usr.updateUserLocale(remoteUser, loginLocale);
          userLocale = loginLocale;
        }
      }

      UserInterface ui = userInterface;
      if (ui == null) {
        ui = usr.getUserInterface(remoteUser);
      }
      if (ui == null) {
        ui = UserInterface.DEFAULT;
      }

      html = render(contextPath, ui, userLocale);
    }
    return html;
  }

  @SuppressWarnings("unused")
  protected Node getLoginExtension(HttpServletRequest req) {
    return null;
  }

  protected String getLoginScriptName() {
    return null;
  }

  /**
   * @param req
   */
  protected boolean isProtected(HttpServletRequest req) {
    return true;
  }

  private void authenticate(HttpServletRequest req, HttpServletResponse resp) {
    try {
      req.setCharacterEncoding(BeeConst.CHARSET_UTF8);
    } catch (UnsupportedEncodingException e) {
      logger.error(e);
    }
    HttpSession session = req.getSession();
    boolean ok = !isProtected(req) || req.getUserPrincipal() != null;
    String userName = null;

    if (!ok) {
      userName = BeeUtils.trim(req.getParameter(HttpConst.PARAM_USER));
      String password = Codec.encodePassword(req.getParameter(HttpConst.PARAM_PASSWORD));
      ok = BeeUtils.allNotEmpty(userName, password);

      if (ok) {
        try {
          req.login(userName, userName);
        } catch (ServletException e1) {
          try {
            logger.info(userName, "login failed, trying with password...");
            req.login(userName, password);
          } catch (ServletException e2) {
            logger.error(e2);
            ok = false;
          }
        }
        if (ok) {
          if (!usr.validateHost(req) || !usr.authenticateUser(userName, password)) {
            try {
              req.logout();
              session.invalidate();
            } catch (ServletException e) {
              logger.error(e);
            }
            ok = false;
          }
        }
      }
    }
    if (ok) {
      doService(req, resp);
    } else {
      HttpUtils.sendResponse(resp, getLoginForm(req, userName));
    }
  }

  private SupportedLocale getUserLocale(String userName) {
    return usr.isUser(userName) ? usr.getUserLocale(userName) : null;
  }

  private boolean isBlocked(String userName) {
    return !BeeUtils.isEmpty(userName) && usr.isUser(userName)
        && BeeUtils.isTrue(usr.isBlocked(userName));
  }
}
