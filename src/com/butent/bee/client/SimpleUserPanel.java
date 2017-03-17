package com.butent.bee.client;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public class SimpleUserPanel extends Horizontal implements IsUserPanel {
  protected static final String STYLE_LOGOUT = BeeConst.CSS_CLASS_PREFIX + "Logout";
  protected static final String STYLE_LOGOUT_CONTAINER = STYLE_LOGOUT + "Container";
  protected static final String STYLE_USER_PHOTO = BeeConst.CSS_CLASS_PREFIX + "UserPhoto";
  protected static final String STYLE_USER_PHOTO_CONTAINER = STYLE_USER_PHOTO + "Container";

  protected static final String STYLE_USER_SIGNATURE = BeeConst.CSS_CLASS_PREFIX
      + "UserSignature";

  private static final String PANEL_NAME = "SimpleUserPanel";

  private static final String STYLE_POPUP_PRESENCE = BeeConst.CSS_CLASS_PREFIX + "PresenceChange";
  private static final String STYLE_USER_PRESENCE_ICON = BeeConst.CSS_CLASS_PREFIX
      + "UserPresenceIcon";
  private static final String STYLE_POPUP_PRESENCE_TABLE = STYLE_POPUP_PRESENCE + "Table";
  private static final String STYLE_POPUP_PRESENCE_ICON = STYLE_POPUP_PRESENCE + "Icon";

  private static final String STYLE_POPUP_PRESENCE_LABEL = STYLE_POPUP_PRESENCE + "Label";
  private static final String STYLE_USER_PRESENCE_CONTAINER = BeeConst.CSS_CLASS_PREFIX
      + "UserPresenceContainer";

  private Flow logoutContainer;
  private Flow photoContainer;
  private Flow presenceContainer;
  private Flow userSignatureContainer;

  private Image userPhotoImage;

  @Override
  public Widget asWidget() {
    return this;
  }

  @Override
  public IsWidget create() {
    createUserSignatureContainer();
    createPhotoContainer();
    createPresenceContainer();
    createExitContainer();
    return this;
  }

  @Override
  public Flow getLogoutContainer() {
    return logoutContainer;
  }

  @Override
  public String getName() {
    return PANEL_NAME;
  }

  @Override
  public Flow getUserSignatureContainer() {
    return userSignatureContainer;
  }

  @Override
  public Flow getPhotoContainer() {
    return photoContainer;
  }

  @Override
  public Image getUserPhotoImage() {
    return userPhotoImage;
  }

  @Override
  public Flow getPresenceContainer() {
    return null;
  }

  @Override
  public void updateUserData(UserData userData) {
    if (userData == null) {
      return;
    }

    createUserPhoto(userData);

    if (getUserSignatureContainer() != null) {
      getUserSignatureContainer().getElement().setInnerText(BeeUtils.trim(userData.getUserSign()));
    }
  }

  @Override
  public void updateUserPresence(Presence presence) {
    if (presenceContainer == null || presence == null) {
      return;
    }

    if (DomUtils.getDataIndexInt(presenceContainer.getElement()) != presence.ordinal()) {

      presenceContainer.clear();
      DomUtils.setDataIndex(presenceContainer.getElement(), presence.ordinal());

      FaLabel presenceWidget = new FaLabel(presence.getIcon(), presence.getStyleName());
      presenceWidget.addStyleName(STYLE_USER_PRESENCE_ICON);
      presenceWidget.setTitle(Localized.dictionary().presenceChangeTooltip());

      if (presence != Presence.IDLE) {
        presenceWidget.addClickHandler(event -> {
          HtmlTable table = new HtmlTable(STYLE_POPUP_PRESENCE_TABLE);
          int r = 0;

          for (Presence p : Presence.values()) {
            table.setWidgetAndStyle(r, 0, new FaLabel(p.getIcon(), p.getStyleName()),
                STYLE_POPUP_PRESENCE_ICON);
            table.setWidgetAndStyle(r, 1, new Label(p.getCaption()),
                STYLE_POPUP_PRESENCE_LABEL);

            DomUtils.setDataIndex(table.getRow(r), p.ordinal());
            r++;
          }

          table.addClickHandler(te -> {
            Element targetElement = EventUtils.getEventTargetElement(te);
            TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);

            int index = DomUtils.getDataIndexInt(rowElement);
            Presence newPresence = EnumUtils.getEnumByIndex(Presence.class, index);

            UiHelper.closeDialog(table);
            BeeKeeper.getUser().maybeUpdatePresence(newPresence);
          });

          Popup popup = new Popup(Popup.OutsideClick.CLOSE, STYLE_POPUP_PRESENCE);

          popup.setWidget(table);
          popup.setHideOnEscape(true);

          popup.showRelativeTo(presenceWidget.getElement());
        });
      }

      presenceContainer.add(presenceWidget);
    }
  }

  protected void createExitContainer() {
    if (!Settings.showLogout()) {
      return;
    }
    logoutContainer = new Flow();
    logoutContainer.addStyleName(STYLE_LOGOUT_CONTAINER);

    FaLabel exit = new FaLabel(FontAwesome.SIGN_OUT);
    exit.addStyleName(STYLE_LOGOUT);
    exit.setTitle(Localized.dictionary().signOut());

    exit.addClickHandler(event ->
        Global.confirm(Localized.dictionary().endSession(Settings.getAppName()),
            Icon.QUESTION, Lists.newArrayList(Localized.dictionary().questionLogout()),
            Localized.dictionary().yes(), Localized.dictionary().no(), Bee::exit));

    logoutContainer.add(exit);
    add(logoutContainer);
  }

  protected void createPhotoContainer() {
    if (!Settings.showUserPhoto()) {
      return;
    }
    photoContainer = new Flow(STYLE_USER_PHOTO_CONTAINER);
    add(photoContainer);
  }

  protected void createUserPhoto(UserData userData) {
    userPhotoImage = new Image(PhotoRenderer.getPhotoUrl(userData.getPhotoFile()));
    userPhotoImage.setAlt(userData.getLogin());
    userPhotoImage.addStyleName(STYLE_USER_PHOTO);

    if (getPhotoContainer() != null) {
      getPhotoContainer().clear();
      getPhotoContainer().add(userPhotoImage);
    }
  }

  protected void createPresenceContainer() {
    if (Settings.showUserPresence()) {
      presenceContainer = new Flow(STYLE_USER_PRESENCE_CONTAINER);
      add(presenceContainer);
    }
  }

  protected void createUserSignatureContainer() {
    userSignatureContainer = new Flow(STYLE_USER_SIGNATURE);
    add(userSignatureContainer);
  }

}
