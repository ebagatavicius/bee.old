package com.butent.bee.client.ui;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;
import java.util.Set;

/**
 * Manages xml descriptions of forms.
 */

public class FormDescription implements HasViewName {
  
  public static final String ATTR_ENABLED_ACTIONS = "enabledActions";
  public static final String ATTR_DISABLED_ACTIONS = "disabledActions";

  private static final String TAG_ROW_MESSAGE = "rowMessage";
  private static final String TAG_ROW_EDITABLE = "rowEditable";
  private static final String TAG_ROW_VALIDATION = "rowValidation";

  private static final String ATTR_SHOW_ROW_ID = "showRowId";

  private static final String ATTR_PRINT_HEADER = "printHeader";
  private static final String ATTR_PRINT_FOOTER = "printFooter";
  
  public static String getName(Element element) {
    Assert.notNull(element);
    return element.getAttribute(UiConstants.ATTR_NAME);
  }
  
  private final Element formElement;
  
  public FormDescription(Element formElement) {
    Assert.notNull(formElement);
    this.formElement = formElement;
  }

  public boolean cacheDescription() {
    return BeeUtils.isTrue(XmlUtils.getAttributeBoolean(getFormElement(),
        UiConstants.ATTR_CACHE_DESCRIPTION));
  }
  
  public String getCaption() {
    String caption = getFormElement().getAttribute(UiConstants.ATTR_CAPTION);
    return Localized.maybeTranslate(BeeUtils.trim(caption));
  }

  public Dimensions getDimensions() {
    return XmlUtils.getDimensions(getFormElement());
  }

  public Set<Action> getDisabledActions() {
    String actions = getFormElement().getAttribute(ATTR_DISABLED_ACTIONS);
    return Action.parse(actions);
  }
  
  public Set<Action> getEnabledActions() {
    String actions = getFormElement().getAttribute(ATTR_ENABLED_ACTIONS);
    return Action.parse(actions);
  }

  public String getName() {
    return getFormElement().getAttribute(UiConstants.ATTR_NAME);
  }
  
  public String getOptions() {
    return getFormElement().getAttribute(HasOptions.ATTR_OPTIONS);
  }
  
  public Map<String, String> getProperties() {
    return XmlUtils.getChildAttributes(getFormElement(), CustomProperties.TAG_PROPERTIES);
  }

  public Calculation getRowEditable() {
    return XmlUtils.getCalculation(getFormElement(), TAG_ROW_EDITABLE);
  }

  public Calculation getRowMessage() {
    return XmlUtils.getCalculation(getFormElement(), TAG_ROW_MESSAGE);
  }

  public Calculation getRowValidation() {
    return XmlUtils.getCalculation(getFormElement(), TAG_ROW_VALIDATION);
  }

  @Override
  public String getViewName() {
    return getFormElement().getAttribute(UiConstants.ATTR_VIEW_NAME);
  }

  public boolean isReadOnly() {
    Boolean readOnly = XmlUtils.getAttributeBoolean(getFormElement(), UiConstants.ATTR_READ_ONLY);
    if (readOnly == null) {
      return false;
    }
    return readOnly;
  }

  public boolean printFooter() {
    return BeeUtils.isTrue(XmlUtils.getAttributeBoolean(getFormElement(), ATTR_PRINT_FOOTER));
  }

  public boolean printHeader() {
    return BeeUtils.isTrue(XmlUtils.getAttributeBoolean(getFormElement(), ATTR_PRINT_HEADER));
  }
  
  public boolean showRowId() {
    return BeeUtils.isTrue(XmlUtils.getAttributeBoolean(getFormElement(), ATTR_SHOW_ROW_ID));
  }
  
  Element getFormElement() {
    return formElement;
  }
}
