package com.butent.bee.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OnlineUsers extends Flow {
  public static final String STYLE_USERS_COUNT = BeeConst.CSS_CLASS_PREFIX + "OnlineUsersCount";
  public static final String STYLE_USERS_ICON = BeeConst.CSS_CLASS_PREFIX + "OnlineUsersIcon";
  public static final String STYLE_POPUP_USERS = BeeConst.CSS_CLASS_PREFIX + "PopupUsers";

  private static final String STYLE_POPUP_USERS_PHOTO = STYLE_POPUP_USERS + "Photo";
  private static final String STYLE_POPUP_USERS_SEARCH = STYLE_POPUP_USERS + "Search";
  private static final String STYLE_POPUP_USERS_SEARCH_ICON = STYLE_POPUP_USERS_SEARCH + "-Icon";
  private static final String STYLE_POPUP_USERS_SEARCH_INPUT = STYLE_POPUP_USERS_SEARCH + "-Input";
  private static final String STYLE_POPUP_USERS_SEARCH_NOT_FOUND = STYLE_POPUP_USERS_SEARCH
      + "-NotFound";

  private static final String STYLE_POPUP_USERS_LABEL = STYLE_POPUP_USERS + "Label";
  private static final String STYLE_POPUP_USERS_POSITION = STYLE_POPUP_USERS + "Position";
  private static final String STYLE_POPUP_USERS_SIGN_FLOW = STYLE_POPUP_USERS + "SignFlow";
  private static final String STYLE_POPUP_USERS_CONTENT = STYLE_POPUP_USERS + "Content";
  private static final String STYLE_POPUP_USERS_PRESENCE = STYLE_POPUP_USERS + "Presence";
  private static final String STYLE_POPUP_USERS_CHAT = STYLE_POPUP_USERS + "Chat";

  private static final int MAX_SEARCH_INPUT_SIZE = 255;

  private final Map<String, Element> searchIndex = new HashMap<>();

  private CustomDiv userCountDiv = new CustomDiv(STYLE_USERS_COUNT);
  private FaLabel icon = new FaLabel(FontAwesome.USER, STYLE_USERS_ICON);
  private Popup onlineUsersPopup = new Popup(Popup.OutsideClick.CLOSE, STYLE_POPUP_USERS);

  /**
   * Create a widget where shows online (in session) users.
   *
   * @return Html div element where on click shows online users.
   */
  public static Flow createWidget(String style) {
    OnlineUsers instance = new OnlineUsers(style);
    instance.create();
    return instance;
  }

  private static String generateKey(UserData user) {
    String key = BeeUtils.toLowerCase(user.getLogin());

    key += BeeUtils.toLowerCase(user.getFirstName());
    key += BeeUtils.toLowerCase(user.getLastName());
    key += BeeUtils.toLowerCase(user.getCompanyPersonPositionName());

    return key;
  }

  private static Image renderUserPhoto(String photoFile) {
    Image photo = new Image(PhotoRenderer.getPhotoUrl(photoFile));
    photo.addStyleName(STYLE_POPUP_USERS_PHOTO);
    return photo;
  }

  private static FaLabel renderUserPresence(String session) {
    FaLabel widget = null;
    Presence presence = Global.getUsers().getPresenceBySession(session);

    if (presence == null) {
      return widget;
    }

    widget = new FaLabel(presence.getIcon(), presence.getStyleName());
    widget.addStyleName(STYLE_POPUP_USERS_PRESENCE);
    widget.setTitle(presence.getCaption());

    return widget;
  }

  private static Flow renderUserSign(UserData userData) {
    Label userSign = new Label(userData.getUserSign());
    Label userPosition = new Label(userData.getCompanyPersonPositionName());
    Flow userSignFlow = new Flow(STYLE_POPUP_USERS_SIGN_FLOW);

    userSign.addStyleName(STYLE_POPUP_USERS_LABEL);
    userPosition.addStyleName(STYLE_POPUP_USERS_POSITION);

    userSignFlow.add(userSign);
    userSignFlow.add(userPosition);

    return userSignFlow;
  }

  private int addRow(HtmlTable table, int rowId, UserData userData, Widget... widgets) {
    int colId = 0;

    for (Widget widget : widgets) {
      if (widget != null) {
        table.setWidget(rowId, colId, widget);

        if (widget instanceof InputText) {
          table.getCellFormatter().setColSpan(rowId, colId, 3);
        }
      }

      colId++;
    }

    if (userData != null) {
      String key = generateKey(userData);
      DomUtils.setDataIndex(table.getRow(rowId), userData.getPerson());
      searchIndex.put(key, table.getRow(rowId));
    }

    return rowId + 1;
  }

  private void create() {
    StyleUtils.setEmptiness(userCountDiv, true);

    add(userCountDiv);

    StyleUtils.setEmptiness(icon, true);
    icon.addClickHandler(this::showOnlineUsers);

    add(icon);
  }

  /* SearchInput method call on Input event. */
  private void doSearch(InputText inputText) {
    String input = BeeUtils.toLowerCase(inputText.getText());

    if (BeeUtils.isEmpty(input)) {
      setUsersVisible(true);
      inputText.removeStyleName(STYLE_POPUP_USERS_SEARCH_NOT_FOUND);
    } else {
      String[] words = BeeUtils.split(input, BeeConst.CHAR_SPACE);
      boolean found = false;

      for (String key : searchIndex.keySet()) {
        boolean visible = false;

        for (String word : words) {
          visible |= BeeUtils.containsSame(key, word);
        }

        found |= visible;
        StyleUtils.setVisible(searchIndex.get(key), visible);
      }

      inputText.setStyleName(STYLE_POPUP_USERS_SEARCH_NOT_FOUND, !found);
    }

  }

  /* ChatButton method call onClick event. */
  private void openChat(Long userId) {
    UiHelper.closeDialog(onlineUsersPopup.getWidget());
    Global.getChatManager().chatWithUser(userId);
  }

  /* UsersTable method call onClick event. */
  private void openPersonForm(ClickEvent event) {
    Element targetElement = EventUtils.getEventTargetElement(event);
    TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
    Long id = DomUtils.getDataIndexLong(rowElement);

    if (!DataUtils.isId(id)) {
      return;
    }

    if (!BeeKeeper.getUser().isDataVisible(ClassifierConstants.VIEW_PERSONS)) {
      return;
    }

    UiHelper.closeDialog(onlineUsersPopup.getWidget());
    RowEditor.open(ClassifierConstants.VIEW_PERSONS, id, Opener.NEW_TAB);
  }

  private FaLabel renderChatButton(Long userId) {
    FaLabel button = null;

    if (!Global.getChatManager().isEnabled()) {
      return button;
    }

    button = new FaLabel(FontAwesome.COMMENT_O, STYLE_POPUP_USERS_CHAT);
    button.setTitle(Localized.dictionary().chat());

    button.addClickHandler(event -> openChat(userId));

    return button;
  }

  private int renderSearch(HtmlTable table, int rowId) {
    FaLabel search = new FaLabel(FontAwesome.SEARCH);
    InputText searchText = new InputText();

    search.addStyleName(STYLE_POPUP_USERS_SEARCH_ICON);
    searchText.addStyleName(STYLE_POPUP_USERS_SEARCH_INPUT);
    searchText.setMaxLength(MAX_SEARCH_INPUT_SIZE);
    searchText.addInputHandler(keyPressEvent -> doSearch(searchText));

    DomUtils.setPlaceholder(searchText.getElement(), Localized.dictionary().search() + "...");

    return addRow(table, rowId, null, search, searchText);
  }

  private void setUsersVisible(boolean visible) {
    for (Element el : searchIndex.values()) {
      StyleUtils.setVisible(el, visible);
    }
  }

  /* Icon method call onClick event. */
  private void showOnlineUsers(ClickEvent event) {
    Set<String> sessions = Global.getUsers().getAllSessions();

    if (BeeUtils.isEmpty(sessions)) {
      return;
    }

    int rowId = 0;
    searchIndex.clear();

    HtmlTable usersTable = new HtmlTable(STYLE_POPUP_USERS_CONTENT);
    rowId = renderSearch(usersTable, rowId);

    for (String session : sessions) {
      UserData userData = Global.getUsers().getUserData(
          Global.getUsers().getUserIdBySession(session));
      Image userPhoto = renderUserPhoto(userData.getPhotoFile());
      FaLabel userPresence = renderUserPresence(session);
      FaLabel chatButton = renderChatButton(userData.getUserId());
      Flow userSign = renderUserSign(userData);

      rowId = addRow(usersTable, rowId, userData, userPhoto, userPresence, userSign, chatButton);
    }

    onlineUsersPopup.setWidget(usersTable);
    onlineUsersPopup.setHideOnEscape(true);
    onlineUsersPopup.showRelativeTo(icon.getElement());

    if (rowId == 1) {
      return;
    }

    usersTable.addClickHandler(this::openPersonForm);
  }

  private OnlineUsers(String style) {
    super(style);
  }
}
