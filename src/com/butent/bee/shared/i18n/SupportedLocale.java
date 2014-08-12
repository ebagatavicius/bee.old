package com.butent.bee.shared.i18n;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum SupportedLocale implements HasCaption {
  LT {
    @Override
    public String getCaption() {
      return "Lietuviškai";
    }

    @Override
    public String getIconName() {
      return "lithuanian";
    }

    @Override
    public String getLanguage() {
      return "lt";
    }
  },

  EN {
    @Override
    public String getCaption() {
      return "English";
    }

    @Override
    public String getIconName() {
      return "english";
    }

    @Override
    public String getLanguage() {
      return "en";
    }
  },

  RU {
    @Override
    public String getCaption() {
      return "Русский";
    }

    @Override
    public String getIconName() {
      return "russian";
    }

    @Override
    public String getLanguage() {
      return "ru";
    }
  },

  FI {
    @Override
    public String getCaption() {
      return "Suomi";
    }

    @Override
    public String getIconName() {
      return "finnish";
    }

    @Override
    public String getLanguage() {
      return "fi";
    }
  };

  public static final SupportedLocale DEFAULT = LT;

  public static SupportedLocale getByLanguage(String language) {
    for (SupportedLocale locale : values()) {
      if (BeeUtils.same(locale.getLanguage(), language)) {
        return locale;
      }
    }
    return null;
  }

  public static String normalizeLanguage(String language) {
    return BeeUtils.nvl(getByLanguage(language), DEFAULT).getLanguage();
  }

  public abstract String getIconName();

  public abstract String getLanguage();
}
