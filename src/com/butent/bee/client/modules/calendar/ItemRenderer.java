package com.butent.bee.client.modules.calendar;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.modules.calendar.CalendarConstants.CalendarVisibility;
import com.butent.bee.shared.modules.calendar.CalendarHelper;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

final class ItemRenderer {

  private static final Splitter TEMPLATE_SPLITTER =
      Splitter.on(CharMatcher.inRange('\u0000', '\u001f'));

  private static final String HTML_LINE_SEPARATOR = "<br/>";
  private static final String TEXT_LINE_SEPARATOR = String.valueOf('\n');

  private static final String SIMPLE_HTML_SEPARATOR = HTML_LINE_SEPARATOR;
  private static final String MULTI_HTML_SEPARATOR = " ";
  private static final String COMPACT_HTML_SEPARATOR = " ";

  private static final String STRING_SEPARATOR = ", ";

  private ItemRenderer() {
  }

  static void render(long calendarId, ItemWidget itemWidget, String headerTemplate,
      String bodyTemplate, String titleTemplate, boolean multi) {

    CalendarItem item = itemWidget.getItem();

    if (item.isVisible(BeeKeeper.getUser().getUserId())) {
      Map<String, String> substitutes = getSubstitutes(calendarId, item, false);
      String separator = multi ? MULTI_HTML_SEPARATOR : SIMPLE_HTML_SEPARATOR;

      String template = BeeUtils.notEmpty(headerTemplate, multi ? item.getMultiHeaderTemplate()
          : item.isPartial() ? item.getPartialHeaderTemplate() : item.getSimpleHeaderTemplate());
      String header = parseTemplate(template, substitutes, separator);
      itemWidget.setHeaderHtml(header);

      template = BeeUtils.notEmpty(bodyTemplate, multi ? item.getMultiBodyTemplate()
          : item.isPartial() ? item.getPartialBodyTemplate() : item.getSimpleBodyTemplate());
      String body = parseTemplate(template, substitutes, separator);
      if (BeeUtils.allEmpty(header, body) || !multi && BeeUtils.isEmpty(body)) {
        body = renderEmpty(item);
      }
      itemWidget.setBodyHtml(body);

      template = BeeUtils.notEmpty(titleTemplate, item.getTitleTemplate());
      String title = parseTemplate(template, getSubstitutes(calendarId, item, true),
          TEXT_LINE_SEPARATOR);
      itemWidget.setTitleText(title);

    } else {
      itemWidget.setBodyHtml(renderPrivate());
    }
  }

  static void renderCompact(long calendarId, CalendarItem item, String compactTemplate,
      Widget htmlWidget, String titleTemplate, Widget titleWidget) {

    if (item.isVisible(BeeKeeper.getUser().getUserId())) {
      Map<String, String> substitutes = getSubstitutes(calendarId, item, false);

      String template = BeeUtils.notEmpty(compactTemplate, item.getCompactTemplate());
      String html = parseTemplate(template, substitutes, COMPACT_HTML_SEPARATOR);
      if (BeeUtils.isEmpty(html)) {
        html = renderEmpty(item);
      }
      if (!BeeUtils.isEmpty(html) && htmlWidget != null) {
        htmlWidget.getElement().setInnerHTML(BeeUtils.trim(html));
      }

      template = BeeUtils.notEmpty(titleTemplate, item.getTitleTemplate());
      String title = parseTemplate(template, getSubstitutes(calendarId, item, true),
          TEXT_LINE_SEPARATOR);
      if (!BeeUtils.isEmpty(title) && titleWidget != null) {
        titleWidget.setTitle(BeeUtils.trim(title));
      }

    } else if (htmlWidget != null) {
      htmlWidget.getElement().setInnerHTML(renderPrivate());
    }
  }

  static void renderMulti(long calendarId, ItemWidget itemWidget) {
    CalendarItem item = itemWidget.getItem();
    render(calendarId, itemWidget, item.getMultiHeaderTemplate(),
        item.getMultiBodyTemplate(), item.getTitleTemplate(), true);
  }

  static void renderSimple(long calendarId, ItemWidget itemWidget) {
    CalendarItem item = itemWidget.getItem();

    render(calendarId, itemWidget,
        item.isPartial() ? item.getPartialHeaderTemplate() : item.getSimpleHeaderTemplate(),
        item.isPartial() ? item.getPartialBodyTemplate() : item.getSimpleBodyTemplate(),
        item.getTitleTemplate(), false);
  }

  static String renderString(long calendarId, CalendarItem item) {
    return parseTemplate(item.getStringTemplate(), getSubstitutes(calendarId, item, false),
        STRING_SEPARATOR);
  }

  private static Map<String, String> getSubstitutes(long calendarId, CalendarItem item,
      boolean addLabels) {

    return item.getSubstitutes(calendarId, Global.getUsers().getUserData(), addLabels,
        Format.getDateTimeRenderer(), Format.getPeriodRenderer());
  }

  private static String parseLine(String line, Map<String, String> substitutes) {
    if (!CalendarHelper.hasSubstitutes(line) || substitutes.isEmpty()) {
      return line;
    }

    String result = line;
    for (Map.Entry<String, String> entry : substitutes.entrySet()) {
      if (entry.getValue() != null) {
        result = result.replace(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private static String parseTemplate(String template, Map<String, String> substitutes,
      String separator) {
    if (BeeUtils.isEmpty(template)) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (String line : TEMPLATE_SPLITTER.split(template.trim())) {
      String s = parseLine(line, substitutes);
      if (!BeeUtils.isEmpty(s)) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(s);
      }
    }
    return sb.toString();
  }

  private static String renderEmpty(CalendarItem item) {
    return Format.renderPeriod(item.getStartTime(), item.getEndTime());
  }

  private static String renderPrivate() {
    return CalendarVisibility.PRIVATE.getCaption();
  }
}
