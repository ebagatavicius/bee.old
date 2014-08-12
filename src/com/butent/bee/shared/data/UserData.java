package com.butent.bee.shared.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Contains core user data like login, first and last names, user id etc.
 */

public class UserData implements BeeSerializable, HasInfo {

  /**
   * Contains serializable members of user data (login, first and last names, position etc).
   */

  private enum Serial {
    LOGIN, USER_ID, FIRST_NAME, LAST_NAME, PHOTO_FILE_NAME, COMPANY_NAME,
    COMPANY_PERSON, COMPANY, PERSON, PROPERTIES, RIGHTS
  }

  private static BeeLogger logger = LogUtils.getLogger(UserData.class);

  public static UserData restore(String s) {
    UserData data = new UserData();
    data.deserialize(s);
    return data;
  }

  private String login;
  private long userId;

  private String firstName;
  private String lastName;
  private String photoFileName;

  private String companyName;

  private Long companyPerson;
  private Long company;
  private Long person;

  private Map<String, String> properties;

  private Table<RightsState, RightsObjectType, Set<String>> rights;

  public UserData(long userId, String login) {
    this.userId = userId;
    this.login = login;
  }

  public UserData(long userId, String login, String firstName, String lastName,
      String photoFileName, String companyName, Long companyPerson, Long company, Long person) {
    this.userId = userId;
    this.login = login;

    this.firstName = firstName;
    this.lastName = lastName;
    this.photoFileName = photoFileName;

    this.companyName = companyName;

    this.companyPerson = companyPerson;
    this.company = company;
    this.person = person;
  }

  private UserData() {
  }

  public boolean canCreateData(String viewName) {
    return hasDataRight(viewName, RightsState.CREATE);
  }

  public boolean canDeleteData(String viewName) {
    return hasDataRight(viewName, RightsState.DELETE);
  }

  public boolean canEditColumn(String viewName, String column) {
    return BeeUtils.anyEmpty(viewName, column) || hasFieldRight(viewName, column, RightsState.EDIT);
  }

  public boolean canEditData(String viewName) {
    return hasDataRight(viewName, RightsState.EDIT);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case LOGIN:
          this.login = value;
          break;

        case USER_ID:
          this.userId = BeeUtils.toLong(value);
          break;

        case FIRST_NAME:
          this.firstName = value;
          break;

        case LAST_NAME:
          this.lastName = value;
          break;

        case PHOTO_FILE_NAME:
          this.photoFileName = value;
          break;

        case COMPANY_NAME:
          this.companyName = value;
          break;

        case COMPANY_PERSON:
          this.companyPerson = BeeUtils.toLongOrNull(value);
          break;

        case COMPANY:
          this.company = BeeUtils.toLongOrNull(value);
          break;

        case PERSON:
          this.person = BeeUtils.toLongOrNull(value);
          break;

        case PROPERTIES:
          String[] entry = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(entry)) {
            properties = new HashMap<>();

            for (int j = 0; j < entry.length; j += 2) {
              properties.put(entry[j], entry[j + 1]);
            }
          }
          break;

        case RIGHTS:
          Map<String, String> map = Codec.deserializeMap(value);

          if (!BeeUtils.isEmpty(map)) {
            rights = HashBasedTable.create();

            for (String stateIdx : map.keySet()) {
              RightsState state = EnumUtils.getEnumByIndex(RightsState.class,
                  BeeUtils.toInt(stateIdx));
              Map<String, String> row = Codec.deserializeMap(map.get(stateIdx));

              for (String typeIdx : row.keySet()) {
                rights.put(state,
                    EnumUtils.getEnumByIndex(RightsObjectType.class, BeeUtils.toInt(typeIdx)),
                    Sets.newHashSet(Codec.beeDeserializeCollection(row.get(typeIdx))));
              }
            }
          }
          break;
      }
    }
  }

  public Long getCompany() {
    return company;
  }

  public String getCompanyName() {
    return companyName;
  }

  public Long getCompanyPerson() {
    return companyPerson;
  }

  public String getFirstName() {
    return firstName;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("Login", getLogin(),
        "User Id", getUserId(),
        "First Name", getFirstName(),
        "Last Name", getLastName(),
        "Photo File Name", getPhotoFileName(),
        "Company Name", getCompanyName(),
        "Company Person ID", getCompanyPerson(),
        "Company ID", getCompany(),
        "Person ID", getPerson());

    if (!BeeUtils.isEmpty(properties)) {
      info.add(new Property("Properties", BeeUtils.bracket(properties.size())));
      info.addAll(PropertyUtils.createProperties(properties));
    }

    if (rights != null) {
      info.add(new Property("Rights", BeeUtils.bracket(rights.size())));
      for (Map.Entry<RightsState, Map<RightsObjectType, Set<String>>> entry : rights.rowMap()
          .entrySet()) {
        info.add(new Property(entry.getKey().toString(), entry.getValue().toString()));
      }
    }

    return info;
  }

  public String getLastName() {
    return lastName;
  }

  public String getLogin() {
    return login;
  }

  public Long getPerson() {
    return person;
  }

  public String getPhotoFileName() {
    return photoFileName;
  }

  public Map<String, String> getProperties() {
    return ImmutableMap.copyOf(properties);
  }

  public String getProperty(String name) {
    if (properties != null) {
      return this.properties.get(name);
    }
    return null;
  }

  public long getUserId() {
    return userId;
  }

  public String getUserSign() {
    return BeeUtils.notEmpty(BeeUtils.joinWords(getFirstName(), getLastName()), getLogin());
  }

  public boolean hasDataRight(String viewName, RightsState state) {
    return isAnyModuleVisible(RightsUtils.getViewModules(viewName))
        && hasRight(RightsObjectType.DATA, viewName, state);
  }

  public boolean isAnyModuleVisible(String input) {
    if (BeeUtils.isEmpty(input)) {
      return true;
    } else {
      List<ModuleAndSub> list = ModuleAndSub.parseList(input);
      for (ModuleAndSub ms : list) {
        if (isModuleVisible(ms)) {
          return true;
        }
      }
      return false;
    }
  }

  public boolean isColumnVisible(String viewName, String column) {
    return BeeUtils.anyEmpty(viewName, column) || hasFieldRight(viewName, column, RightsState.VIEW);
  }

  public boolean isDataVisible(String viewName) {
    return hasDataRight(viewName, RightsState.VIEW);
  }

  public boolean isMenuVisible(String object) {
    return hasRight(RightsObjectType.MENU, object, RightsState.VIEW);
  }

  public boolean isModuleVisible(ModuleAndSub moduleAndSub) {
    if (moduleAndSub == null) {
      return true;
    } else {
      return moduleAndSub.isEnabled()
          && hasRight(RightsObjectType.MODULE, moduleAndSub.getName(), RightsState.VIEW);
    }
  }

  public boolean isWidgetVisible(RegulatedWidget widget) {
    if (widget == null) {
      return true;
    } else {
      return isWidgetVisible(widget.getName()) && isModuleVisible(widget.getModuleAndSub());
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case LOGIN:
          arr[i++] = login;
          break;
        case USER_ID:
          arr[i++] = userId;
          break;
        case FIRST_NAME:
          arr[i++] = firstName;
          break;
        case LAST_NAME:
          arr[i++] = lastName;
          break;
        case PHOTO_FILE_NAME:
          arr[i++] = photoFileName;
          break;
        case COMPANY_NAME:
          arr[i++] = companyName;
          break;
        case COMPANY_PERSON:
          arr[i++] = companyPerson;
          break;
        case COMPANY:
          arr[i++] = company;
          break;
        case PERSON:
          arr[i++] = person;
          break;
        case PROPERTIES:
          arr[i++] = properties;
          break;
        case RIGHTS:
          Map<Integer, Map<Integer, Set<String>>> map = null;

          if (rights != null) {
            map = new HashMap<>();

            for (RightsState state : rights.rowKeySet()) {
              Map<Integer, Set<String>> row = new HashMap<>();

              for (Entry<RightsObjectType, Set<String>> entry : rights.row(state).entrySet()) {
                row.put(entry.getKey().ordinal(), entry.getValue());
              }
              map.put(state.ordinal(), row);
            }
          }
          arr[i++] = map;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setPerson(Long person) {
    this.person = person;
  }

  public void setPhotoFileName(String photoFileName) {
    this.photoFileName = photoFileName;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public UserData setProperty(String name, String value) {
    if (this.properties == null) {
      this.properties = new HashMap<>();
    }
    this.properties.put(name, value);
    return this;
  }

  public void setRights(Table<RightsState, RightsObjectType, Set<String>> rights) {
    this.rights = rights;
  }

  private boolean hasFieldRight(String viewName, String column, RightsState state) {
    return hasDataRight(viewName, state)
        && hasRight(RightsObjectType.FIELD, RightsUtils.buildName(viewName, column), state);
  }

  private boolean hasRight(RightsObjectType type, String object, RightsState state) {
    Assert.notNull(state);

    if (!BeeUtils.contains(type.getRegisteredStates(), state)) {
      logger.severe("State", BeeUtils.bracket(state.name()),
          "is not registered for type", BeeUtils.bracket(type.name()));
      return false;
    }
    if (BeeUtils.isEmpty(object)) {
      return true;
    }
    boolean checked;

    if (rights != null && rights.contains(state, type)) {
      String obj = null;
      checked = true;
      Collection<String> objects = rights.get(state, type);

      for (String part : RightsUtils.NAME_SPLITTER.split(object)) {
        obj = RightsUtils.NAME_JOINER.join(obj, part);

        if (objects.contains(obj) == state.isChecked()) {
          checked = false;
          break;
        }
      }
    } else {
      checked = state.isChecked();
    }
    return checked;
  }

  private boolean isWidgetVisible(String object) {
    return hasRight(RightsObjectType.WIDGET, object, RightsState.VIEW);
  }
}
