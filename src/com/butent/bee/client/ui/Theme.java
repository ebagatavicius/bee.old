package com.butent.bee.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.json.client.JSONObject;

import com.butent.bee.client.Settings;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

public final class Theme {

  private static JSONObject values;

  public static void load(String serialized) {
    if (BeeUtils.isEmpty(serialized)) {
      setValues(Settings.getTheme());
    } else {
      BeeRowSet rowSet = BeeRowSet.restore(serialized);
      if (!DataUtils.isEmpty(rowSet)) {
        setValues(JsonUtils.toJson(rowSet.getColumns(), rowSet.getRow(0)));
      }
    }

    String css = getCss();
    if (!BeeUtils.isEmpty(css)) {
      StyleElement element = Document.get().createStyleElement();
      DomUtils.createId(element, "theme-");

      element.setInnerText(css.trim());
      DomUtils.getHead().appendChild(element);
    }
  }

  public static int getWorkspaceMarginRight() {
    return getInteger("WorkspaceMarginRight");
  }

  public static int getViewHeaderHeight() {
    return getInteger("ViewHeaderHeight");
  }

  public static int getChildViewHeaderHeight() {
    return getInteger("ChildViewHeaderHeight");
  }

  public static boolean hasViewActionCreateNew() {
    return getBoolean("ViewActionCreateNew");
  }

  public static boolean hasGridActionCreateNew() {
    return getBoolean("GridActionCreateNew");
  }

  public static boolean hasChildActionCreateNew() {
    return getBoolean("ChildActionCreateNew");
  }

  public static int getGridHeaderRowHeight() {
    return getInteger("GridHeaderRowHeight");
  }

  public static int getGridBodyRowHeight() {
    return getInteger("GridBodyRowHeight");
  }

  public static int getGridFooterRowHeight() {
    return getInteger("GridFooterRowHeight");
  }

  public static int getGridMarginLeft() {
    return getInteger("GridMarginLeft");
  }

  public static String getGridHeaderFont() {
    return getString("GridHeaderFont");
  }

  public static String getGridBodyFont() {
    return getString("GridBodyFont");
  }

  public static String getGridFooterFont() {
    return getString("GridFooterFont");
  }

  public static int getChildGridHeaderRowHeight() {
    return getInteger("ChildGridHeaderRowHeight");
  }

  public static int getChildGridBodyRowHeight() {
    return getInteger("ChildGridBodyRowHeight");
  }

  public static int getChildGridFooterRowHeight() {
    return getInteger("ChildGridFooterRowHeight");
  }

  public static int getChildGridMarginLeft() {
    return getInteger("ChildGridMarginLeft");
  }

  public static String getChildGridHeaderFont() {
    return getString("ChildGridHeaderFont");
  }

  public static String getChildGridBodyFont() {
    return getString("ChildGridBodyFont");
  }

  public static String getChildGridFooterFont() {
    return getString("ChildGridFooterFont");
  }

  public static int getApplianceHeaderHeight() {
    return getInteger("ApplianceHeaderHeight");
  }

  public static int getSubMenuLineHeight() {
    return getInteger("SubMenuLineHeight");
  }

  public static int getWorkspaceTabHeight() {
    return getInteger("WorkspaceTabHeight");
  }

  public static int getTabbedPagesTabHeight() {
    return getInteger("TabbedPagesTabHeight");
  }

  public static int getDisclosureClosedHeight() {
    return getInteger("DisclosureClosedHeight");
  }

  public static int getInputLineHeight() {
    return getInteger("InputLineHeight");
  }

  public static String getInputPadding() {
    return getString("InputPadding");
  }

  public static String getListSize1Padding() {
    return getString("ListSize1Padding");
  }

  public static int getFormCellPaddingTop() {
    return getInteger("FormCellPaddingTop");
  }

  public static JSONObject getValues() {
    return values;
  }

  private static String getCss() {
    return getString("Css");
  }

  private static boolean getBoolean(String key) {
    return BeeUtils.isTrue(JsonUtils.getBoolean(values, key));
  }

  private static int getInteger(String key) {
    Integer value = JsonUtils.getInteger(values, key);
    return (value == null) ? BeeConst.UNDEF : value;
  }

  private static String getString(String key) {
    return JsonUtils.getString(values, key);
  }

  private static void setValues(JSONObject values) {
    Theme.values = values;
  }

  private Theme() {
  }
}
