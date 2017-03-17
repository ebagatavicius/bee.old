package com.butent.bee.server.modules.ec;

import com.google.common.net.HttpHeaders;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.server.LoginServlet;
import com.butent.bee.server.ProxyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.HttpConst;
import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.IdFilter;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.Node;
import com.butent.bee.shared.html.builder.elements.Datalist;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Form;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.html.builder.elements.Input.Type;
import com.butent.bee.shared.html.builder.elements.Select;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/ec/*")
@SuppressWarnings("serial")
public class EcServlet extends LoginServlet {

  private static final String PATH_REGISTER = "/register";

  private static final String REG_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ec-registration-";

  private static final String REG_STYLE_LABEL_CELL = REG_STYLE_PREFIX + "label-cell";
  private static final String REG_STYLE_LABEL = REG_STYLE_PREFIX + "label";

  private static final String REG_STYLE_INPUT_CELL = REG_STYLE_PREFIX + "input-cell";
  private static final String REG_STYLE_INPUT = REG_STYLE_PREFIX + "input";

  private static final String REG_STYLE_REQUIRED = REG_STYLE_PREFIX + "required";

  private static final String REG_STYLE_TYPE_PREFIX = REG_STYLE_PREFIX + "type-";

  private static Node clientTypeSelector(Dictionary constants, EcClientType defaultType) {
    String name = COL_REGISTRATION_TYPE;

    Div container = div().addClass(REG_STYLE_TYPE_PREFIX + "container");

    for (EcClientType clientType : EcClientType.values()) {
      Input input = input().addClass(REG_STYLE_TYPE_PREFIX + "input").type(Type.RADIO)
          .name(name).value(clientType.ordinal()).id(clientType.name().toLowerCase())
          .onChange("onSelectType(this.value)");

      if (clientType == defaultType) {
        input.checked();
      }

      Span span = span().addClass(REG_STYLE_TYPE_PREFIX + "text")
          .text(clientType.getCaption(constants));

      container.append(label().addClass(REG_STYLE_TYPE_PREFIX + "label").append(input, span));
    }

    return tr().id(name + ID_SUFFIX_FIELD).append(
        registrationLabelCell(name + ID_SUFFIX_LABEL, constants.ecClientType(), true),
        td().addClass(REG_STYLE_INPUT_CELL).append(container));
  }

  private static Element registrationField(String label, String name, boolean required) {
    return registrationField(label, Type.TEXT, name, required);
  }

  private static Element registrationField(String label, String name, String className) {
    Element element = registrationField(label, Type.TEXT, name, false);
    element.addClassName(className);
    return element;
  }

  private static Element registrationField(String label, Type type, String name, boolean required) {
    return tr().id(name + ID_SUFFIX_FIELD).append(
        registrationLabelCell(name + ID_SUFFIX_LABEL, label, required),
        registrationInputCell(name + ID_SUFFIX_INPUT, type, name, required));
  }

  private static Node registrationInputCell(String id, Type type, String name, boolean required) {
    Input input = input().addClass(REG_STYLE_INPUT).id(id).type(type).name(name);
    if (required) {
      input.required();
    }

    return td().addClass(REG_STYLE_INPUT_CELL).append(input);
  }

  private static Node registrationLabelCell(String id, String text, boolean required) {
    Div div = div().addClass(REG_STYLE_LABEL);
    if (required) {
      div.addClass(REG_STYLE_REQUIRED);
    }
    div.id(id).text(text);

    return td().addClass(REG_STYLE_LABEL_CELL).append(div);
  }

  @EJB
  ProxyBean proxy;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    String html;
    String path = req.getPathInfo();

    if (BeeUtils.isEmpty(path)) {
      html = getInitialPage(req, UserInterface.E_COMMERCE);

    } else if (BeeUtils.same(path, PATH_REGISTER)) {
      Map<String, String> parameters = HttpUtils.getParameters(req, false);

      String language = getLanguage(req);
      Dictionary constants = Localizations.getDictionary(language);

      if (parameters.containsKey(COL_REGISTRATION_BRANCH)) {
        Map<String, String> dictionary = Localizations.getGlossary(language);
        DateTimeFormatInfo dtfInfo = SupportedLocale.parse(language).getDateTimeFormatInfo();

        html = register(req, parameters, constants, dictionary, dtfInfo);

      } else {
        html = getRegistrationForm(req.getServletContext().getContextPath(), constants);
      }

    } else {
      HttpUtils.sendError(resp, HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    HttpUtils.sendResponse(resp, html);
  }

  @Override
  protected Node getLoginExtension(HttpServletRequest req) {
    Form form = form().addClass(STYLE_PREFIX + "Command-Form-register")
        .name("register")
        .acceptCharsetUtf8()
        .methodPost()
        .action(req.getServletContext().getContextPath() + req.getServletPath() + PATH_REGISTER)
        .onSubmit("setSelectedLanguage(this)")
        .append(
            button().typeSubmit().addClass(STYLE_PREFIX + "Register").id(COMMAND_REGISTER_ID),
            input().type(Type.HIDDEN).id("register-language").name(HttpConst.PARAM_LOCALE));

    return div().addClass(STYLE_PREFIX + "Command-container").append(form);
  }

  @Override protected boolean isProtected(HttpServletRequest req) {
    return !BeeUtils.same(req.getPathInfo(), PATH_REGISTER) && super.isProtected(req);
  }

  private Node branchSelector(String label) {
    String name = COL_REGISTRATION_BRANCH;
    Select select = select().name(name).required().append(option());

    BeeRowSet branches = qs.getViewData(ClassifierConstants.VIEW_BRANCHES);
    if (!DataUtils.isEmpty(branches)) {
      int index = branches.getColumnIndex(ClassifierConstants.COL_BRANCH_NAME);

      for (BeeRow row : branches.getRows()) {
        select.append(option().value(row.getId()).text(row.getString(index)));
      }
    }

    return tr().id(name + ID_SUFFIX_FIELD).append(
        registrationLabelCell(name + ID_SUFFIX_LABEL, label, true),
        td().addClass(REG_STYLE_INPUT_CELL).append(select));
  }

  private String getRegistrationForm(String contextPath, Dictionary constants) {
    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(constants.ecRegistrationNew()),
        link().styleSheet(resource(contextPath, Paths.getStyleSheetPath("ecregistration"))),
        script().src(resource(contextPath, Paths.getScriptPath("ecregistration"))));

    Tbody fields = tbody().append(
        branchSelector(constants.branch()),
        clientTypeSelector(constants, EcClientType.COMPANY),
        registrationField(constants.ecClientCompanyName(), COL_REGISTRATION_COMPANY_NAME, true),
        registrationField(constants.ecClientCompanyCode(), COL_REGISTRATION_COMPANY_CODE, true),
        registrationField(constants.ecClientVatCode(), COL_REGISTRATION_VAT_CODE, true),
        registrationField(constants.ecClientFirstName(), COL_REGISTRATION_FIRST_NAME, true),
        registrationField(constants.ecClientLastName(), COL_REGISTRATION_LAST_NAME, true),
        registrationField(constants.ecClientPersonCode(), COL_REGISTRATION_PERSON_CODE,
            REG_STYLE_PREFIX + "hide"),
        registrationField(constants.email(), Type.EMAIL, COL_REGISTRATION_EMAIL, true),
        registrationField(constants.phone(), Type.TEL, COL_REGISTRATION_PHONE, true),
        registrationField(constants.country(), COL_REGISTRATION_COUNTRY, false),
        registrationField(constants.city(), COL_REGISTRATION_CITY, true),
        registrationField(constants.address(), COL_REGISTRATION_ADDRESS, true),
        registrationField(constants.postIndex(), COL_REGISTRATION_POST_INDEX, true),
        registrationField(constants.ecClientActivity(), COL_REGISTRATION_ACTIVITY, false));

    doc.getBody().append(
        div().addClass(REG_STYLE_PREFIX + "panel").append(
            div().addClass(REG_STYLE_PREFIX + "caption").text(constants.ecRegistrationNew()),
            form().addClass(REG_STYLE_PREFIX + "form").acceptCharsetUtf8().methodPost().append(
                table().addClass(REG_STYLE_PREFIX + "table").append(fields),
                input().type(Type.HIDDEN).name(HttpConst.PARAM_LOCALE)
                    .value(constants.languageTag()),
                button().typeSubmit().addClass(REG_STYLE_PREFIX + "submit")
                    .text(constants.ecRegister()))));

    Datalist cities = proxy.getDataList(ClassifierConstants.TBL_CITIES,
        ClassifierConstants.COL_CITY_NAME);
    if (cities != null) {
      String listId = COL_REGISTRATION_CITY + ID_SUFFIX_LIST;
      cities.id(listId);

      Element element = fields.queryId(COL_REGISTRATION_CITY + ID_SUFFIX_INPUT);
      if (element instanceof Input) {
        ((Input) element).list(listId);
        doc.getBody().append(cities);
      }
    }

    Datalist countries = proxy.getDataList(ClassifierConstants.TBL_COUNTRIES,
        ClassifierConstants.COL_COUNTRY_NAME);
    if (countries != null) {
      String listId = COL_REGISTRATION_COUNTRY + ID_SUFFIX_LIST;
      countries.id(listId);

      Element element = fields.queryId(COL_REGISTRATION_COUNTRY + ID_SUFFIX_INPUT);
      if (element instanceof Input) {
        ((Input) element).list(listId);
        doc.getBody().append(countries);
      }
    }

    return doc.buildLines();
  }

  private String register(HttpServletRequest req, Map<String, String> parameters,
      Dictionary constants, Map<String, String> dictionary, DateTimeFormatInfo dtfInfo) {

    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(constants.ecRegistrationNew()));

    SqlInsert si = new SqlInsert(TBL_REGISTRATIONS);

    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (!BeeUtils.isEmpty(entry.getValue()) && sys.hasField(TBL_REGISTRATIONS, entry.getKey())) {
        si.addConstant(entry.getKey(), entry.getValue().trim());
      }
    }

    si.addConstant(COL_REGISTRATION_DATE, TimeUtils.nowMinutes());
    si.addConstant(COL_REGISTRATION_HOST, req.getRemoteAddr());
    si.addConstant(COL_REGISTRATION_AGENT, req.getHeader(HttpHeaders.USER_AGENT));

    SupportedLocale locale = SupportedLocale.getByLanguage(constants.languageTag());
    if (locale != null) {
      si.addConstant(COL_REGISTRATION_LANGUAGE, locale.ordinal());
    }

    ResponseObject response = proxy.insert(si);
    if (response.hasErrors()) {
      for (String message : response.getErrors()) {
        doc.getBody().append(div().text(message));
      }

    } else if (response.hasResponse(Long.class)) {
      BeeRowSet data = qs.getViewData(VIEW_REGISTRATIONS,
          IdFilter.compareId((Long) response.getResponse()));
      BeeRow row = data.getRow(0);

      Tbody fields = tbody();

      for (int i = 0; i < data.getNumberOfColumns(); i++) {
        BeeColumn column = data.getColumn(i);
        String value;

        switch (column.getId()) {
          case COL_REGISTRATION_DATE:
            value = Formatter.renderDateTime(dtfInfo, row.getDateTime(i));
            break;

          case COL_REGISTRATION_BRANCH:
          case COL_REGISTRATION_HOST:
          case COL_REGISTRATION_AGENT:
            value = null;
            break;

          case COL_REGISTRATION_TYPE:
            EcClientType type = EnumUtils.getEnumByIndex(EcClientType.class, row.getInteger(i));
            value = (type == null) ? null : type.getCaption(constants);
            break;

          default:
            value = row.getString(i);
        }

        if (!BeeUtils.isEmpty(value)) {
          String label;
          if (column.getId().startsWith(COL_REGISTRATION_BRANCH)) {
            label = constants.branch();
          } else {
            label = Localized.maybeTranslate(column.getLabel(), dictionary);
          }

          fields.append(tr().append(
              td().alignRight().paddingRight(1, CssUnit.EM).text(label),
              td().text(value)));
        }
      }

      doc.getBody().append(h3().text(constants.ecRegistrationReceived()), table().append(fields));

    } else if (response.hasMessages()) {
      for (ResponseMessage message : response.getMessages()) {
        doc.getBody().append(div().text(message.getMessage()));
      }

    } else {
      doc.getBody().append(div().text(BeeUtils.joinWords("response", response.getType(),
          response.getResponse())));
    }

    return doc.buildLines();
  }
}
