package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.html.builder.elements.Meta;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public enum UserInterface implements HasCaption {
  DESKTOP {
    @Override
    public Collection<Component> getComponents() {
      return EnumSet.allOf(Component.class);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList();
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("settings", "js/tinymce/js/tinymce/tinymce.min.js");
    }

    @Override
    public String getShortName() {
      return "desktop";
    }

    @Override
    public List<String> getStyleSheets() {
      return Lists.newArrayList(MAIN_STYLE_SHEET, CalendarConstants.STYLE_SHEET,
          AdministrationConstants.STYLE_SHEET, TaskConstants.STYLE_SHEET, EcConstants.STYLE_SHEET,
          MailConstants.STYLE_SHEET, TradeConstants.STYLE_SHEET, TransportConstants.STYLE_SHEET,
          DiscussionsConstants.STYLE_SHEET, ServiceConstants.STYLE_SHEET,
          DocumentConstants.STYLE_SHEET);
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  TABLET {
    @Override
    public Collection<Component> getComponents() {
      return EnumSet.allOf(Component.class);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(
          new Meta().name("gwt:property").content("screen=tablet"),
          new Meta().name("apple-mobile-web-app-capable").content("yes"),
          new Meta().name("viewport").content("width=device-width, user-scalable=yes"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("tabletsettings");
    }

    @Override
    public String getShortName() {
      return "tablet";
    }

    @Override
    public List<String> getStyleSheets() {
      return Lists.newArrayList(MAIN_STYLE_SHEET, CalendarConstants.STYLE_SHEET,
          AdministrationConstants.STYLE_SHEET, TaskConstants.STYLE_SHEET, EcConstants.STYLE_SHEET,
          MailConstants.STYLE_SHEET, TradeConstants.STYLE_SHEET, TransportConstants.STYLE_SHEET,
          ServiceConstants.STYLE_SHEET);
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  MOBILE {
    @Override
    public Collection<Component> getComponents() {
      return EnumSet.allOf(Component.class);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(
          new Meta().name("gwt:property").content("screen=mobile"),
          new Meta().name("apple-mobile-web-app-capable").content("yes"),
          new Meta().name("viewport").content("width=device-width, user-scalable=yes"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("mobilesettings");
    }

    @Override
    public String getShortName() {
      return "mobile";
    }

    @Override
    public List<String> getStyleSheets() {
      return Lists.newArrayList(MAIN_STYLE_SHEET, CalendarConstants.STYLE_SHEET,
          AdministrationConstants.STYLE_SHEET, TaskConstants.STYLE_SHEET, EcConstants.STYLE_SHEET,
          MailConstants.STYLE_SHEET, TradeConstants.STYLE_SHEET, TransportConstants.STYLE_SHEET,
          ServiceConstants.STYLE_SHEET);
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  E_COMMERCE {
    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.DATA_INFO, Component.AUTOCOMPLETE, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(new Meta().name("gwt:property").content("screen=ec"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("ecsettings");
    }

    @Override
    public String getShortName() {
      return "ec";
    }

    @Override
    public List<String> getStyleSheets() {
      return Lists.newArrayList(MAIN_STYLE_SHEET, EcConstants.CLIENT_STYLE_SHEET);
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  },

  SELF_SERVICE {
    @Override
    public Collection<Component> getComponents() {
      return EnumSet.of(Component.DATA_INFO, Component.DICTIONARY, Component.FILTERS,
          Component.DECORATORS, Component.GRIDS, Component.AUTOCOMPLETE, Component.USERS);
    }

    @Override
    public List<Meta> getMeta() {
      return Lists.newArrayList(new Meta().name("gwt:property").content("screen=trss"));
    }

    @Override
    public List<String> getScripts() {
      return Lists.newArrayList("settings");
    }

    @Override
    public String getShortName() {
      return "trss";
    }

    @Override
    public List<String> getStyleSheets() {
      return Lists.newArrayList(MAIN_STYLE_SHEET, TransportConstants.STYLE_SHEET);
    }

    @Override
    public String getTitle() {
      return TITLE;
    }
  };

  public enum Component {
    AUTOCOMPLETE(false),
    DATA_INFO(false),
    DECORATORS(false),
    DICTIONARY(false),
    FAVORITES(false),
    FILTERS(false),
    GRIDS(false),
    MENU(false),
    NEWS(false),
    REPORTS(false),
    USERS(true),
    WORKSPACES(false);

    private final boolean required;

    private Component(boolean required) {
      this.required = required;
    }

    public boolean isRequired() {
      return required;
    }

    public String key() {
      return name().toLowerCase();
    }
  }

  public static final UserInterface DEFAULT = DESKTOP;

  public static final String MAIN_STYLE_SHEET = "bee";

  public static final String TITLE = "B-NOVO";

  public static UserInterface getByShortName(String input) {
    for (UserInterface ui : values()) {
      if (BeeUtils.same(ui.getShortName(), input)) {
        return ui;
      }
    }

    for (UserInterface ui : values()) {
      if (BeeUtils.containsSame(input, ui.getShortName())) {
        return ui;
      }
    }

    return EnumUtils.getEnumByName(UserInterface.class, input);
  }

  public static Collection<Component> getRequiredComponents() {
    EnumSet<Component> components = EnumSet.noneOf(Component.class);

    for (Component component : Component.values()) {
      if (component.isRequired()) {
        components.add(component);
      }
    }

    return components;
  }

  public static UserInterface normalize(UserInterface ui) {
    return (ui == null) ? DEFAULT : ui;
  }

  @Override
  public String getCaption() {
    return name();
  }

  public abstract Collection<Component> getComponents();

  public abstract List<Meta> getMeta();

  public abstract List<String> getScripts();

  public abstract String getShortName();

  public abstract List<String> getStyleSheets();

  public abstract String getTitle();
  
  public boolean hasMenu() {
    return getComponents().contains(Component.MENU);
  }
}
