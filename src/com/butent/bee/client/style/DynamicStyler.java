package com.butent.bee.client.style;

import com.google.common.base.Strings;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

public class DynamicStyler {

  private static final BeeLogger logger = LogUtils.getLogger(DynamicStyler.class);

  private final String elementId;

  private final CellSource cellSource;

  private final List<ConditionalStyle> conditionalStyles = new ArrayList<>();

  private List<String> defClasses;
  private SafeStyles defStyles;

  private boolean initialized;

  public DynamicStyler(String elementId, CellSource cellSource, ConditionalStyle conditionalStyle) {
    this.elementId = elementId;
    this.cellSource = cellSource;

    addConditionalStyle(conditionalStyle);
  }

  public void addConditionalStyle(ConditionalStyle conditionalStyle) {
    if (conditionalStyle != null) {
      conditionalStyles.add(conditionalStyle);
    }
  }

  public void apply(IsRow row) {
    Element element = Document.get().getElementById(elementId);
    if (element == null) {
      logger.warning(NameUtils.getName(this), elementId, "element not found");
      return;
    }

    if (!initialized) {
      defClasses = StyleUtils.splitClasses(DomUtils.getClassName(element));
      defStyles = StyleUtils.toSafeStyles(StyleUtils.getCssText(element));

      initialized = true;
    }

    Integer index = (cellSource == null) ? null : cellSource.getIndex();
    int colIndex = (index == null) ? BeeConst.UNDEF : index;

    ValueType type = (cellSource == null) ? null : cellSource.getValueType();
    String value = (cellSource == null || row == null) ? null : cellSource.getString(row);

    List<String> classesBuilder = new ArrayList<>();
    if (!BeeUtils.isEmpty(defClasses)) {
      classesBuilder.addAll(defClasses);
    }

    SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
    if (defStyles != null) {
      stylesBuilder.append(defStyles);
    }

    for (ConditionalStyle cs : conditionalStyles) {
      StyleDescriptor styleDescriptor = cs.getStyleDescriptor(row, BeeConst.UNDEF, colIndex,
          type, value);

      if (styleDescriptor != null) {
        List<String> classes = StyleUtils.splitClasses(styleDescriptor.getClassName());
        for (String name : classes) {
          if (!classesBuilder.contains(name)) {
            classesBuilder.add(name);
          }
        }

        if (styleDescriptor.hasSafeStylesOrFont()) {
          styleDescriptor.buildSafeStyles(stylesBuilder);
        }
      }
    }

    String oldClasses = DomUtils.getClassName(element);
    String newClasses = StyleUtils.buildClasses(classesBuilder);

    if (!BeeUtils.equalsTrim(oldClasses, newClasses)) {
      element.setClassName(Strings.nullToEmpty(newClasses));
    }

    SafeStyles ss = StyleUtils.toSafeStyles(StyleUtils.getCssText(element));
    String oldStyles = (ss == null) ? null : ss.asString();
    String newStyles = stylesBuilder.toSafeStyles().asString();

    if (!BeeUtils.equalsTrim(oldStyles, newStyles)) {
      StyleUtils.setCssText(element, Strings.nullToEmpty(newStyles));
    }
  }

  public String getElementId() {
    return elementId;
  }
}
