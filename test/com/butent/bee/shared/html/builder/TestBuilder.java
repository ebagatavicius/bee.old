package com.butent.bee.shared.html.builder;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.http.HttpConst;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Form;
import com.butent.bee.shared.html.builder.elements.Input.Type;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestBuilder {

  private static List<Td> renderCells(String label, String source, boolean required) {
    List<Td> cells = new ArrayList<>();

    cells.add(td().verticalAlign(VerticalAlign.TOP).paddingBottom(4, CssUnit.PX)
        .textAlign(TextAlign.RIGHT).paddingLeft(1, CssUnit.EM).paddingRight(1, CssUnit.EM)
        .append(div().text(label).addClass(required ? "bee-required" : null)));

    cells.add(td().verticalAlign(VerticalAlign.TOP).paddingBottom(4, CssUnit.PX)
        .append(input().type(Type.TEXT).name(source).required(required)));

    return cells;
  }

  private static void renderField(List<Pair<Integer, String>> html, int indent,
      String label, String source, String cellStyle, String labelCellStyle, boolean required) {

    html.add(Pair.of(indent + 1,
        "<td style=\"" + BeeUtils.joinWords(cellStyle, labelCellStyle) + "\">"));
    html.add(Pair.of(indent + 2,
        "<div" + (required ? " class=\"bee-required\"" : "") + ">" + label + "</div>"));
    html.add(Pair.of(indent + 1, "</td>"));

    html.add(Pair.of(indent + 1, "<td style=\"" + cellStyle + "\">"));
    html.add(Pair.of(indent + 2, "<input type=\"text\" name=\"" + source + "\""
        + (required ? " required" : "") + " />"));
    html.add(Pair.of(indent + 1, "</td>"));
  }

  private final String userNameLabel = "Prisijungimo vardas";

  private final String passwordLabel = "Slaptažodis";
  private final String loginLabel = "Prisijungti";

  private final boolean failed = true;
  private final String failedLabel = "Bandykite dar kartą";

  private final String userName = "user";
  private final String ui = "ec";

  private final String commandRegister = "trregister";
  private final String commandQuery = "trquery";

  private final String labelRegister = "Registruotis";

  private final String labelQuery = "Pateikti užklausą";
  private final List<String> loginHtml = new ArrayList<>();

  private final String registrationTitle = "Registration Title";

  private final String registrationCaption = "Registration Caption";

  private final List<Pair<Integer, String>> registerHtml = new ArrayList<>();

  @Before
  public void setUpLogin() {
    List<String> html = new ArrayList<>();

    html.add("<!doctype html>");
    html.add("<html>");
    html.add("<head>");
    html.add("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />");
    html.add("<title>to BEE or not to BEE</title>");

    html.add("<link rel=\"stylesheet\" href=\"" + "css/login.css" + "\" />");
    html.add("<script src=\"" + "js/login.js" + "\"></script>");

    html.add("</head>");
    html.add("<body>");

    html.add("<div class=\"bee-SignIn-Panel\">");
    html.add("<form class=\"bee-SignIn-Form\" method=\"post\">");

    if (SupportedLocale.values().length > 1) {
      html.add("<div class=\"bee-SignIn-Locale-container\">");

      for (SupportedLocale locale : SupportedLocale.values()) {
        html.add("<label><input type=\"radio\" name=\"" + HttpConst.PARAM_LOCALE
            + "\" value=\"" + locale.getLanguage() + "\" />" + locale.getCaption() + "</label>");
      }

      html.add("</div>");
    }

    html.add("<div class=\"bee-SignIn-Logo-container\">");
    html.add("<img class=\"bee-SignIn-Logo\" src=\"images/copyright.png\" />");
    html.add("</div>");

    String text = userNameLabel;
    html.add("<div class=\"bee-SignIn-Label bee-SignIn-Label-user\">" + text + "</div>");

    StringBuilder sb = new StringBuilder();
    sb.append("<input class=\"bee-SignIn-Input bee-SignIn-Input-user\" type=\"text\" name=\"");
    sb.append(HttpConst.PARAM_USER + "\" id=\"user\"");

    if (!BeeUtils.isEmpty(userName)) {
      sb.append(" value=\"" + BeeUtils.trim(userName) + "\"");
    }
    sb.append(" onkeydown=\"return goPswd(event)\" autofocus required />");
    html.add(sb.toString());

    text = passwordLabel;
    html.add("<div class=\"bee-SignIn-Label bee-SignIn-Label-password\">" + text + "</div>");

    html.add("<input class=\"bee-SignIn-Input bee-SignIn-Input-password\" type=\"password\" "
        + "name=\"" + HttpConst.PARAM_PASSWORD + "\" id=\"pswd\" required />");

    if (!BeeUtils.isEmpty(ui)) {
      html.add("<input type=\"hidden\" name=\"" + HttpConst.PARAM_UI + "\" value=\""
          + BeeUtils.trim(ui) + "\" />");
    }

    text = loginLabel;
    html.add("<input class=\"bee-SignIn-Button\" type=\"submit\" value=\"" + text + "\" />");

    if (failed) {
      text = failedLabel;
      html.add("<div class=\"bee-SignIn-Error\">" + text + "</div>");
    }
    html.add("</form>");

    if (!BeeUtils.allEmpty(commandRegister, commandQuery)) {
      String styleName = "bee-SignIn-Command-container";
      html.add("<div class=\"" + BeeUtils.joinWords(styleName,
          BeeUtils.join(BeeConst.STRING_MINUS, styleName, commandRegister, commandQuery)) + "\">");

      if (!BeeUtils.isEmpty(commandRegister)) {
        html.add("<form class=\"bee-SignIn-Command-Form-register\" method=\"post\" action=\""
            + BeeUtils.trim(commandRegister) + "\">");

        text = labelRegister;
        html.add("<input class=\"bee-SignIn-Register\" type=\"submit\" value=\"" + text + "\" />");

        html.add("</form>");
      }

      if (!BeeUtils.isEmpty(commandQuery)) {
        html.add("<form class=\"bee-SignIn-Command-Form-query\" method=\"post\" action=\""
            + BeeUtils.trim(commandQuery) + "\">");

        text = labelQuery;
        html.add("<input class=\"bee-SignIn-Query\" type=\"submit\" value=\"" + text + "\" />");

        html.add("</form>");
      }

      html.add("</div>");
    }

    html.add("<div class=\"bee-SignIn-Caption\">");
    html.add("<img class=\"bee-Copyright-logo\" src=\"images/butent_arrow.png\" />");
    html.add("<span>UAB \"Būtenta\" &copy; 2010 - " + TimeUtils.today().getYear() + "</span>");
    html.add("</div>");
    html.add("</div>");

    html.add("</body>");
    html.add("</html>");

    this.loginHtml.addAll(html);
  }

  @Before
  public void setUpRegister() {
    List<Pair<Integer, String>> html = new ArrayList<>();

    int indent = 0;

    html.add(Pair.of(indent, "<!doctype html>"));
    html.add(Pair.of(indent, "<html>"));

    indent++;
    html.add(Pair.of(indent, "<head>"));
    indent++;
    html.add(Pair.of(indent,
        "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"));

    html.add(Pair.of(indent, "<title>" + registrationTitle + "</title>"));

    html.add(Pair.of(indent, "<style>"));
    indent++;
    html.add(Pair.of(indent, ".bee-required:after {"));
    html.add(Pair.of(indent, "content: \"*\"; color: red; font-size: 13px; font-weight: bold;"));
    html.add(Pair.of(indent, "}"));
    indent--;
    html.add(Pair.of(indent, "</style>"));

    indent--;
    html.add(Pair.of(indent, "</head>"));
    html.add(Pair.of(indent, "<body>"));

    indent++;
    html.add(Pair.of(indent, "<h2>" + registrationCaption + "</h2>"));

    html.add(Pair.of(indent, "<form method=\"post\" action=\"trregister?submit\">"));
    indent++;
    html.add(Pair.of(indent,
        "<table style=\"border-collapse: collapse; margin-top: 2ex; margin-left: 1em;\">"));
    indent++;
    html.add(Pair.of(indent, "<tbody>"));

    String cellStyle = "vertical-align: top; padding-bottom: 4px;";
    String labelCellStyle = "text-align: right; padding-left: 1em; padding-right: 1em;";

    indent++;
    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "CompanyName", COL_REGISTRATION_COMPANY_NAME,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "CompanyCode", COL_REGISTRATION_COMPANY_CODE,
        cellStyle, labelCellStyle, true);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Address", COL_REGISTRATION_ADDRESS,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "VatCode", COL_REGISTRATION_VAT_CODE,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "City", COL_REGISTRATION_CITY,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "Country", COL_REGISTRATION_COUNTRY,
        cellStyle, labelCellStyle, true);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Contact", COL_REGISTRATION_CONTACT,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "Bank", COL_REGISTRATION_BANK,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Position", COL_REGISTRATION_CONTACT_POSITION,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "BankAddress", COL_REGISTRATION_BANK_ADDRESS,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Phone", COL_REGISTRATION_PHONE,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "BankAccount", COL_REGISTRATION_BANK_ACCOUNT,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Email", COL_REGISTRATION_EMAIL,
        cellStyle, labelCellStyle, true);
    renderField(html, indent, "Mobile", COL_REGISTRATION_MOBILE,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "ExchangeCode", COL_REGISTRATION_EXCHANGE_CODE,
        cellStyle, labelCellStyle, false);
    renderField(html, indent, "Fax", COL_REGISTRATION_FAX,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    html.add(Pair.of(indent, "<tr>"));
    renderField(html, indent, "Swift", COL_REGISTRATION_SWIFT,
        cellStyle, labelCellStyle, false);
    renderField(html, indent, "Notes", COL_REGISTRATION_NOTES,
        cellStyle, labelCellStyle, false);
    html.add(Pair.of(indent, "</tr>"));

    indent--;
    html.add(Pair.of(indent, "</tbody>"));
    indent--;
    html.add(Pair.of(indent, "</table>"));

    html.add(Pair.of(indent, "<input type=\"submit\" value=\"" + "Submit" + "\" />"));
    indent--;
    html.add(Pair.of(indent, "</form>"));

    indent--;
    html.add(Pair.of(indent, "</body>"));
    indent--;
    html.add(Pair.of(indent, "</html>"));

    registerHtml.addAll(html);
  }

  @Test
  public final void testLogin() {
    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text("to BEE or not to BEE"),
        link().styleSheet("css/login.css"),
        script().src("js/login.js"));

    Form form = form().addClass("bee-SignIn-Form").methodPost();

    if (SupportedLocale.values().length > 1) {
      Div div = div().addClass("bee-SignIn-Locale-container");
      for (SupportedLocale locale : SupportedLocale.values()) {
        div.append(
            label()
                .append(input().type(Type.RADIO).name(HttpConst.PARAM_LOCALE)
                    .value(locale.getLanguage()))
                .text(locale.getCaption()));
      }
      form.append(div);
    }

    form.append(
        div().addClass("bee-SignIn-Logo-container")
            .append(
                img().addClass("bee-SignIn-Logo").src("images/copyright.png")),
        div().addClass("bee-SignIn-Label").addClass("bee-SignIn-Label-user").text(userNameLabel),
        input().addClass("bee-SignIn-Input").addClass("bee-SignIn-Input-user")
            .type(Type.TEXT)
            .name(HttpConst.PARAM_USER)
            .id("user")
            .value(userName)
            .onKeyDown("return goPswd(event)")
            .autofocus()
            .required(),
        div().addClass("bee-SignIn-Label bee-SignIn-Label-password")
            .text(passwordLabel),
        input().addClass("bee-SignIn-Input bee-SignIn-Input-password")
            .type(Type.PASSWORD)
            .name(HttpConst.PARAM_PASSWORD)
            .id("pswd")
            .required());

    if (!BeeUtils.isEmpty(ui)) {
      form.append(input().type(Type.HIDDEN).name(HttpConst.PARAM_UI).value(ui.trim()));
    }

    form.append(input().addClass("bee-SignIn-Button").type(Type.SUBMIT).value(loginLabel));

    if (failed) {
      form.append(div().addClass("bee-SignIn-Error").text(failedLabel));
    }

    Div container = div().addClass("bee-SignIn-Panel").append(form);

    if (!BeeUtils.allEmpty(commandRegister, commandQuery)) {
      String pfx = "bee-SignIn-Command-";
      String styleName = pfx + "container";

      Div commandPanel = div().addClass(BeeUtils.joinWords(styleName,
          BeeUtils.join(BeeConst.STRING_MINUS, styleName, commandRegister, commandQuery)));

      if (!BeeUtils.isEmpty(commandRegister)) {
        commandPanel.append(
            form().addClass(pfx + "Form-register").methodPost().action(commandRegister).append(
                input().type(Type.SUBMIT).addClass("bee-SignIn-Register").value(labelRegister))
            );
      }

      if (!BeeUtils.isEmpty(commandQuery)) {
        commandPanel.append(
            form().addClass(pfx + "Form-query").methodPost().action(commandQuery).append(
                input().type(Type.SUBMIT).addClass("bee-SignIn-Query").value(labelQuery))
            );
      }

      container.append(commandPanel);
    }

    container.append(
        div().addClass("bee-SignIn-Caption").append(
            img().addClass("bee-Copyright-logo").src("images/butent_arrow.png"),
            span().text("UAB \"Būtenta\" &copy; 2010 - " + TimeUtils.today().getYear())));

    doc.getBody().append(container);

    Assert.assertEquals(BeeUtils.join("", loginHtml), doc.build());
    Assert.assertEquals(BeeUtils.buildLines(loginHtml), doc.build(0, 0));
  }

  @Test
  public final void testRegister() {
    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(registrationTitle),
        style()
            .text(".bee-required:after {")
            .text("content: \"*\"; color: red; font-size: 13px; font-weight: bold;")
            .text("}"));

    doc.getBody().append(
        h2().text(registrationCaption),
        form().methodPost().action("trregister?submit").append(
            table().borderCollapse().marginTop(2, CssUnit.EX).marginLeft(1, CssUnit.EM).append(
                tbody().append(
                    tr().append(renderCells("CompanyName", COL_REGISTRATION_COMPANY_NAME, true))
                        .append(renderCells("CompanyCode", COL_REGISTRATION_COMPANY_CODE, true)),
                    tr().append(renderCells("Address", COL_REGISTRATION_ADDRESS, true))
                        .append(renderCells("VatCode", COL_REGISTRATION_VAT_CODE, false)),
                    tr().append(renderCells("City", COL_REGISTRATION_CITY, true))
                        .append(renderCells("Country", COL_REGISTRATION_COUNTRY, true)),
                    tr().append(renderCells("Contact", COL_REGISTRATION_CONTACT, true))
                        .append(renderCells("Bank", COL_REGISTRATION_BANK, false)),
                    tr().append(renderCells("Position", COL_REGISTRATION_CONTACT_POSITION, true))
                        .append(renderCells("BankAddress", COL_REGISTRATION_BANK_ADDRESS, false)),
                    tr().append(renderCells("Phone", COL_REGISTRATION_PHONE, true))
                        .append(renderCells("BankAccount", COL_REGISTRATION_BANK_ACCOUNT, false)),
                    tr().append(renderCells("Email", COL_REGISTRATION_EMAIL, true))
                        .append(renderCells("Mobile", COL_REGISTRATION_MOBILE, false)),
                    tr().append(renderCells("ExchangeCode", COL_REGISTRATION_EXCHANGE_CODE, false))
                        .append(renderCells("Fax", COL_REGISTRATION_FAX, false)),
                    tr().append(renderCells("Swift", COL_REGISTRATION_SWIFT, false))
                        .append(renderCells("Notes", COL_REGISTRATION_NOTES, false))
                    )),
            input().type(Type.SUBMIT).value("Submit")));

    Assert.assertEquals(getRegistrationHtml(0), doc.build(0, 0));
    Assert.assertEquals(getRegistrationHtml(4), doc.build(0, 4));
  }

  private String getRegistrationHtml(int indent) {
    StringBuilder sb = new StringBuilder();

    for (Pair<Integer, String> line : registerHtml) {
      if (sb.length() > 0) {
        sb.append(BeeConst.CHAR_EOL);
      }

      if (line.getA() > 0 && indent > 0) {
        sb.append(BeeUtils.space(line.getA() * indent));
      }
      sb.append(line.getB());
    }

    return sb.toString();
  }
}
