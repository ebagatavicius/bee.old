package com.butent.bee.client.style;

import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;

import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Handles font and other style properties of particular styles.
 */

public class StyleDescriptor extends StyleDeclaration {

  public static StyleDescriptor copyOf(StyleDeclaration src) {
    if (src == null) {
      return null;
    }
    return new StyleDescriptor(src.getClassName(), src.getInline(), src.getFontDeclaration());
  }

  public static StyleDescriptor of(SafeStyles styles) {
    if (styles == null) {
      return null;

    } else {
      StyleDescriptor sd = new StyleDescriptor(null);
      sd.setSafeStyles(styles);
      return sd;
    }
  }

  private Font font;
  private SafeStyles safeStyles;

  public StyleDescriptor(String className) {
    super(className);
  }

  public StyleDescriptor(String className, String inline, String fontDeclaration) {
    super(className, inline, fontDeclaration);
  }

  public void buildSafeStyles(SafeStylesBuilder stylesBuilder) {
    if (stylesBuilder == null) {
      return;
    }

    if (getSafeStyles() != null) {
      stylesBuilder.append(getSafeStyles());
    }
    if (getFont() != null) {
      stylesBuilder.append(getFont().buildCss());
    }
  }

  public Font getFont() {
    return font;
  }

  public SafeStyles getSafeStyles() {
    return safeStyles;
  }

  public boolean hasSafeStylesOrFont() {
    return getSafeStyles() != null || getFont() != null;
  }

  @Override
  public void setFontDeclaration(String fontDeclaration) {
    super.setFontDeclaration(fontDeclaration);
    setFont(Font.parse(fontDeclaration));
  }

  @Override
  protected void setInline(String inline) {
    super.setInline(inline);
    if (BeeUtils.isEmpty(inline)) {
      setSafeStyles(null);
    } else {
      setSafeStyles(StyleUtils.toSafeStyles(inline));
    }
  }

  private void setFont(Font font) {
    this.font = font;
  }

  private void setSafeStyles(SafeStyles safeStyles) {
    this.safeStyles = safeStyles;
  }
}
