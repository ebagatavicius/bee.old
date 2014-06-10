package com.butent.bee.server.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.primitives.Longs;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.LocalizableMessages;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.Wildcards;

import java.io.IOException;
import java.io.StringReader;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for users system, their login status, localization, user and roles cache etc.
 */

@Singleton
@Lock(LockType.READ)
public class UserServiceBean {

  private static final class UserInfo {

    private final UserData userData;
    private final String password;
    private Collection<Long> userRoles;

    private SupportedLocale userLocale = SupportedLocale.DEFAULT;
    private UserInterface userInterface;

    private DateTime blockAfter;
    private DateTime blockBefore;

    private final Set<Long> sessions = Sets.newHashSet();

    private UserInfo(UserData userData, String password) {
      this.userData = userData;
      this.password = password;
    }

    private UserInfo addSession(long session) {
      sessions.add(session);
      return this;
    }

    private DateTime getBlockAfter() {
      return blockAfter;
    }

    private DateTime getBlockBefore() {
      return blockBefore;
    }

    private Long getCompany() {
      return userData.getCompany();
    }

    private Long getCompanyPerson() {
      return userData.getCompanyPerson();
    }

    private String getLanguage() {
      return getUserLocale().getLanguage();
    }

    private String getPassword() {
      return password;
    }

    private Long getPerson() {
      return userData.getPerson();
    }

    private Collection<Long> getRoles() {
      return userRoles;
    }

    private UserData getUserData() {
      return userData;
    }

    private UserInterface getUserInterface() {
      return userInterface;
    }

    private SupportedLocale getUserLocale() {
      return userLocale;
    }

    private boolean isBlocked(long time) {
      if (getBlockAfter() != null && getBlockBefore() != null) {
        return getBlockAfter().getTime() <= time && getBlockBefore().getTime() > time;
      } else if (getBlockAfter() != null) {
        return getBlockAfter().getTime() <= time;
      } else if (getBlockBefore() != null) {
        return getBlockBefore().getTime() > time;
      } else {
        return false;
      }
    }

    private boolean isOnline() {
      return !BeeUtils.isEmpty(sessions);
    }

    private boolean removeSession(long session) {
      return sessions.remove(session);
    }

    private UserInfo setBlockAfter(DateTime after) {
      this.blockAfter = after;
      return this;
    }

    private UserInfo setBlockBefore(DateTime before) {
      this.blockBefore = before;
      return this;
    }

    private UserInfo setProperties(String properties) {
      Map<String, String> props = null;

      if (!BeeUtils.isEmpty(properties)) {
        props = Maps.newHashMap();
        Properties prp = new Properties();

        try {
          prp.load(new StringReader(properties));
        } catch (IOException e) {
          logger.error(e, properties);
        }
        for (String p : prp.stringPropertyNames()) {
          props.put(p, prp.getProperty(p));
        }
      }
      userData.setProperties(props);
      return this;
    }

    private UserInfo setRoles(Collection<Long> roles) {
      this.userRoles = roles;
      return this;
    }

    private UserInfo setUserInterface(UserInterface ui) {
      this.userInterface = ui;
      return this;
    }

    private UserInfo setUserLocale(SupportedLocale sl) {
      this.userLocale = BeeUtils.nvl(sl, SupportedLocale.DEFAULT);
      return this;
    }
  }

  private static final class IpFilter {
    private final String host;
    private final DateTime blockAfter;
    private final DateTime blockBefore;

    private IpFilter(String host, DateTime blockAfter, DateTime blockBefore) {
      this.host = host;
      this.blockAfter = blockAfter;
      this.blockBefore = blockBefore;
    }

    private boolean isBlocked(String addr, DateTime dt) {
      return Wildcards.isLike(addr, host)
          && TimeUtils.isBetweenExclusiveNotRequired(dt, blockAfter, blockBefore);
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(UserServiceBean.class);

  private static String key(String value) {
    return (value == null) ? null : value.toLowerCase();
  }

  @Resource
  EJBContext ctx;
  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;

  private final Map<Long, String> roleCache = Maps.newHashMap();
  private final BiMap<Long, String> userCache = HashBiMap.create();
  private Map<String, UserInfo> infoCache = Maps.newHashMap();

  private final List<IpFilter> ipFilters = Lists.newArrayList();

  private final Table<RightsObjectType, String, Multimap<RightsState, Long>> rightsCache =
      HashBasedTable.create();

  @Lock(LockType.WRITE)
  public boolean authenticateUser(String name, String password) {
    if (BeeUtils.isEmpty(userCache)) {
      long company = qs.insertData(new SqlInsert(TBL_COMPANIES)
          .addConstant(COL_COMPANY_NAME, name));

      long person = qs.insertData(new SqlInsert(TBL_PERSONS)
          .addConstant(COL_FIRST_NAME, name));

      long companyPerson = qs.insertData(new SqlInsert(TBL_COMPANY_PERSONS)
          .addConstant(COL_COMPANY, company)
          .addConstant(COL_PERSON, person));

      qs.insertData(new SqlInsert(TBL_USERS)
          .addConstant(COL_LOGIN, name)
          .addConstant(COL_PASSWORD, password)
          .addConstant(COL_COMPANY_PERSON, companyPerson));
    }
    UserInfo info = getUserInfo(getUserId(name));
    return info != null && Objects.equals(password, info.getPassword());
  }

  public boolean canEditColumn(String viewName, String column) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().canEditColumn(viewName, column);
  }

  public List<UserData> getAllUserData() {
    List<UserData> data = Lists.newArrayList();
    for (UserInfo userInfo : infoCache.values()) {
      data.add(userInfo.getUserData());
    }
    return data;
  }

  public Long getCompanyPerson(Long userId) {
    UserInfo userInfo = getUserInfo(userId);
    return (userInfo == null) ? null : userInfo.getCompanyPerson();
  }

  public String getCurrentUser() {
    Principal p = ctx.getCallerPrincipal();
    Assert.notNull(p);
    return p.getName().toLowerCase();
  }

  public UserData getCurrentUserData() {
    UserInfo userInfo = getCurrentUserInfo();
    return (userInfo == null) ? null : userInfo.getUserData();
  }

  public Filter getCurrentUserFilter(String column) {
    return Filter.equals(column, getCurrentUserId());
  }

  public Long getCurrentUserId() {
    return getUserId(getCurrentUser());
  }

  public Long getEmailId(Long userId, boolean checkCompany) {
    if (userId == null) {
      return null;
    }

    UserInfo userInfo = getUserInfo(userId);
    if (userInfo == null) {
      return null;
    }

    if (DataUtils.isId(userInfo.getCompanyPerson())) {
      Long id =
          qs.getLong(new SqlSelect().addFields(TBL_CONTACTS, COL_EMAIL)
              .addFrom(TBL_CONTACTS)
              .addFromLeft(TBL_COMPANY_PERSONS,
                  sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
              .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, userInfo.getCompanyPerson())));

      if (DataUtils.isId(id)) {
        return id;
      }
    }

    if (DataUtils.isId(userInfo.getPerson())) {
      Long id = qs.getLong(new SqlSelect().addFields(TBL_CONTACTS, COL_EMAIL)
          .addFrom(TBL_CONTACTS)
          .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_CONTACTS, TBL_PERSONS, COL_CONTACT))
          .setWhere(sys.idEquals(TBL_PERSONS, userInfo.getPerson())));

      if (DataUtils.isId(id)) {
        return id;
      }
    }

    if (checkCompany && DataUtils.isId(userInfo.getCompany())) {
      Long id = qs.getLong(new SqlSelect().addFields(TBL_CONTACTS, COL_EMAIL)
          .addFrom(TBL_CONTACTS)
          .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
          .setWhere(sys.idEquals(TBL_COMPANIES, userInfo.getCompany())));

      if (DataUtils.isId(id)) {
        return id;
      }
    }

    return null;
  }

  public String getLanguage() {
    return getLanguage(getCurrentUserId());
  }

  public String getLanguage(Long userId) {
    UserInfo userInfo = (userId == null) ? null : getUserInfo(userId);
    return (userInfo == null) ? SupportedLocale.DEFAULT.getLanguage() : userInfo.getLanguage();
  }

  public Locale getLocale() {
    return BeeUtils.nvl(I18nUtils.toLocale(getLanguage(getCurrentUserId())),
        Localizations.getDefaultLocale());
  }

  public LocalizableConstants getLocalizableConstants() {
    return getLocalizableConstants(getCurrentUserId());
  }

  public LocalizableConstants getLocalizableConstants(Long userId) {
    return Localizations.getPreferredConstants(getLanguage(userId));
  }

  public Map<String, String> getLocalizableDictionary() {
    return getLocalizableDictionary(getCurrentUserId());
  }

  public Map<String, String> getLocalizableDictionary(Long userId) {
    return Localizations.getPreferredDictionary(getLanguage(userId));
  }

  public LocalizableMessages getLocalizableMesssages() {
    return getLocalizableMesssages(getCurrentUserId());
  }

  public LocalizableMessages getLocalizableMesssages(Long userId) {
    return Localizations.getPreferredMessages(getLanguage(userId));
  }

  public String getRoleName(Long roleId) {
    Assert.contains(roleCache, roleId);
    return roleCache.get(roleId);
  }

  public ResponseObject getRoleRights(RightsObjectType type, Long roleId) {
    Assert.notNull(type);
    Assert.notNull(roleId);

    Multimap<String, RightsState> objectStates = HashMultimap.create();

    if (rightsCache.containsRow(type)) {
      for (String object : rightsCache.row(type).keySet()) {
        Multimap<RightsState, Long> stateRoles = rightsCache.get(type, object);

        if (stateRoles.containsValue(roleId)) {
          for (RightsState state : stateRoles.keySet()) {
            if (stateRoles.containsEntry(state, roleId)) {
              objectStates.put(object, state);
            }
          }
        }
      }
    }

    if (objectStates.isEmpty()) {
      return ResponseObject.emptyResponse();

    } else {
      Map<String, String> result = Maps.newHashMap();

      for (String object : objectStates.keySet()) {
        result.put(object, EnumUtils.buildIndexList(objectStates.get(object)));
      }

      return ResponseObject.response(result);
    }
  }

  public Set<Long> getRoles() {
    return roleCache.keySet();
  }

  public ResponseObject getStateRights(RightsObjectType type, RightsState state) {
    Assert.notNull(type);
    Assert.notNull(state);

    Multimap<String, Long> objectRoles = HashMultimap.create();

    if (rightsCache.containsRow(type)) {
      for (String object : rightsCache.row(type).keySet()) {
        Multimap<RightsState, Long> states = rightsCache.get(type, object);

        if (states.containsKey(state)) {
          objectRoles.putAll(object, states.get(state));
        }
      }
    }

    if (objectRoles.isEmpty()) {
      return ResponseObject.emptyResponse();

    } else {
      Map<String, String> result = Maps.newHashMap();

      for (String object : objectRoles.keySet()) {
        result.put(object, DataUtils.buildIdList(objectRoles.get(object)));
      }

      return ResponseObject.response(result);
    }
  }

  public Long getUserId(String user) {
    if (userCache.inverse().containsKey(key(user))) {
      return userCache.inverse().get(key(user));
    } else {
      return null;
    }
  }

  public UserInterface getUserInterface(String user) {
    UserInfo userInfo = getUserInfo(getUserId(user));
    return (userInfo == null) ? null : userInfo.getUserInterface();
  }

  public SupportedLocale getUserLocale(String user) {
    UserInfo userInfo = getUserInfo(getUserId(user));
    return (userInfo == null) ? null : userInfo.getUserLocale();
  }

  public String getUserName(Long userId) {
    return userCache.get(userId);
  }

  public long[] getUserRoles() {
    return getUserRoles(getCurrentUserId());
  }

  public long[] getUserRoles(Long userId) {
    UserInfo userInfo = getUserInfo(userId);

    if (userInfo != null) {
      return Longs.toArray(getUserInfo(userId).getRoles());
    }
    return new long[0];
  }

  public Set<Long> getUsers() {
    return userCache.keySet();
  }

  public String getUserSign(Long userId) {
    UserInfo userInfo = getUserInfo(userId);

    if (userInfo != null) {
      return userInfo.getUserData().getUserSign();
    }
    return null;
  }

  public boolean hasDataRight(String object, RightsState state) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().hasDataRight(object, state);
  }

  @Lock(LockType.WRITE)
  public void initIpFilters() {
    List<IpFilter> filters = Lists.newArrayList();

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_IP_FILTERS, COL_IP_FILTER_HOST,
            COL_IP_FILTER_BLOCK_AFTER,
            COL_IP_FILTER_BLOCK_BEFORE)
        .addFrom(TBL_IP_FILTERS));

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        filters.add(new IpFilter(row.getValue(COL_IP_FILTER_HOST),
            row.getDateTime(COL_IP_FILTER_BLOCK_AFTER),
            row.getDateTime(COL_IP_FILTER_BLOCK_BEFORE)));
      }
    }
    ipFilters.clear();

    if (!filters.isEmpty()) {
      ipFilters.addAll(filters);
      logger.info("Loaded", filters.size(), "ip filters");
    }
  }

  @Lock(LockType.WRITE)
  public void initRights() {
    rightsCache.clear();

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_OBJECTS, COL_OBJECT_NAME)
        .addFields(TBL_RIGHTS, COL_ROLE, COL_STATE)
        .addFrom(TBL_OBJECTS)
        .addFromInner(TBL_RIGHTS, sys.joinTables(TBL_OBJECTS, TBL_RIGHTS, COL_OBJECT));

    for (RightsObjectType type : RightsObjectType.values()) {
      if (BeeUtils.isEmpty(type.getRegisteredStates())) {
        continue;
      }
      List<Integer> stateIds = Lists.newArrayList();

      for (RightsState state : type.getRegisteredStates()) {
        stateIds.add(state.ordinal());
      }
      SimpleRowSet res = qs.getData(ss.setWhere(SqlUtils.and(SqlUtils
          .equals(TBL_OBJECTS, COL_OBJECT_TYPE, type.ordinal()),
          SqlUtils.inList(TBL_RIGHTS, COL_STATE, stateIds))));

      if (res.getNumberOfRows() > 0) {
        for (SimpleRow row : res) {
          String object = row.getValue(COL_OBJECT_NAME);
          Multimap<RightsState, Long> states = rightsCache.get(type, object);

          if (states == null) {
            states = HashMultimap.create();
            rightsCache.put(type, object, states);
          }
          states.put(EnumUtils.getEnumByIndex(RightsState.class, row.getInt(COL_STATE)),
              row.getLong(COL_ROLE));
        }
      }
    }
  }

  @Lock(LockType.WRITE)
  public void initUsers() {
    roleCache.clear();
    userCache.clear();
    Map<String, UserInfo> expiredCache = infoCache;
    infoCache = Maps.newHashMap();

    String userIdName = sys.getIdName(TBL_USERS);
    String roleIdName = sys.getIdName(TBL_ROLES);

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_ROLES, roleIdName, COL_ROLE_NAME)
        .addFrom(TBL_ROLES));

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      roleCache.put(rs.getLong(i, roleIdName), rs.getValue(i, COL_ROLE_NAME));
    }

    rs = qs.getData(new SqlSelect()
        .addFields(TBL_USER_ROLES, COL_USER, COL_ROLE)
        .addFrom(TBL_USER_ROLES));

    Multimap<Long, Long> userRoles = HashMultimap.create();

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      userRoles.put(rs.getLong(i, COL_USER), rs.getLong(i, COL_ROLE));
    }

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_USERS, userIdName, COL_LOGIN, COL_PASSWORD, COL_COMPANY_PERSON,
            COL_USER_PROPERTIES, COL_USER_LOCALE, COL_USER_INTERFACE, COL_USER_BLOCK_AFTER,
            COL_USER_BLOCK_BEFORE)
        .addFields(TBL_COMPANY_PERSONS, COL_COMPANY, COL_PERSON)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME, COL_PHOTO)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addFrom(TBL_USERS)
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS,
            COL_COMPANY));

    for (SimpleRow row : qs.getData(ss)) {
      long userId = row.getLong(userIdName);
      String login = key(row.getValue(COL_LOGIN));

      userCache.put(userId, login);

      UserData userData = new UserData(userId, login, row.getValue(COL_FIRST_NAME),
          row.getValue(COL_LAST_NAME), row.getValue(COL_PHOTO), row.getValue(ALS_COMPANY_NAME),
          row.getLong(COL_COMPANY_PERSON), row.getLong(COL_COMPANY), row.getLong(COL_PERSON));

      UserInfo user = new UserInfo(userData, row.getValue(COL_PASSWORD))
          .setRoles(userRoles.get(userId))
          .setProperties(row.getValue(COL_USER_PROPERTIES))
          .setUserLocale(EnumUtils.getEnumByIndex(SupportedLocale.class,
              row.getInt(COL_USER_LOCALE)))
          .setUserInterface(EnumUtils.getEnumByIndex(UserInterface.class,
              row.getInt(COL_USER_INTERFACE)))
          .setBlockAfter(TimeUtils.toDateTimeOrNull(row.getLong(COL_USER_BLOCK_AFTER)))
          .setBlockBefore(TimeUtils.toDateTimeOrNull(row.getLong(COL_USER_BLOCK_BEFORE)));

      UserInfo oldInfo = expiredCache.get(login);
      if (oldInfo != null && oldInfo.isOnline()) {
        user.sessions.addAll(oldInfo.sessions);
      }
      infoCache.put(login, user);
      userData.setRights(getUserRights(userId));
    }
  }

  public boolean isAdministrator() {
    return isModuleVisible(ModuleAndSub.of(Module.ADMINISTRATION));
  }

  public Boolean isBlocked(String user) {
    UserInfo userInfo = getUserInfo(getUserId(user));
    return (userInfo == null) ? null : userInfo.isBlocked(System.currentTimeMillis());
  }

  public boolean isColumnVisible(BeeView view, String column) {
    UserInfo info = getCurrentUserInfo();

    if (info == null) {
      return false;

    } else if (view == null || BeeUtils.isEmpty(column)) {
      return true;

    } else if (!info.getUserData().isColumnVisible(view.getName(), column)) {
      return false;

    } else {
      String root = view.getRootField(column);

      if (!BeeUtils.isEmpty(root) && !BeeUtils.same(column, root)) {
        return info.getUserData().isColumnVisible(view.getName(), root);
      } else {
        return true;
      }
    }
  }

  public boolean isDataVisible(String object) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().isDataVisible(object);
  }

  public boolean isMenuVisible(String object) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().isMenuVisible(object);
  }

  public boolean isModuleVisible(ModuleAndSub moduleAndSub) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().isModuleVisible(moduleAndSub);
  }

  public boolean isModuleVisible(String module) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().isModuleVisible(module);
  }

  public boolean isRoleTable(String tblName) {
    return BeeUtils.inList(tblName, TBL_ROLES, TBL_USER_ROLES);
  }

  public boolean isUser(String user) {
    return !BeeUtils.isEmpty(user) && userCache.inverse().containsKey(key(user));
  }

  public boolean isUserTable(String tblName) {
    return BeeUtils.inList(tblName, TBL_USERS, TBL_COMPANY_PERSONS, TBL_PERSONS);
  }

  public boolean isWidgetVisible(RegulatedWidget widget) {
    UserInfo info = getCurrentUserInfo();
    return (info == null) ? false : info.getUserData().isWidgetVisible(widget);
  }

  @Lock(LockType.WRITE)
  public ResponseObject login(String host, String agent) {
    ResponseObject response = new ResponseObject();
    String user = getCurrentUser();

    if (isUser(user)) {
      Long userId = getUserId(user);

      Long historyId = qs.insertData(new SqlInsert(TBL_USER_HISTORY)
          .addConstant(COL_USER, userId)
          .addConstant(COL_LOGGED_IN, System.currentTimeMillis())
          .addConstant(COL_REMOTE_HOST, host)
          .addConstant(COL_USER_AGENT, agent));

      UserInfo info = getUserInfo(userId);
      info.addSession(historyId);

      UserData data = info.getUserData()
          .setProperty(Service.VAR_FILE_ID, BeeUtils.toString(historyId));

      response.setResponse(data);
      logger.info("User logged in:", user, BeeUtils.parenthesize(data.getUserSign()));

    } else if (BeeUtils.isEmpty(getUsers())) {
      response.setResponse(new UserData(-1, user));
      logger.warning("Anonymous user logged in:", user);

    } else {
      response.addError("Login attempt by an unauthorized user:", user);
      response.log(logger);
    }
    return response;
  }

  @Lock(LockType.WRITE)
  public void logout(long userId, long historyId) {
    UserInfo info = getUserInfo(userId);

    if (info != null) {
      qs.updateData(new SqlUpdate(TBL_USER_HISTORY)
          .addConstant(COL_LOGGED_OUT, System.currentTimeMillis())
          .setWhere(sys.idEquals(TBL_USER_HISTORY, historyId)));

      String sign = info.getUserData().getLogin() + " "
          + BeeUtils.parenthesize(info.getUserData().getUserSign());

      if (info.removeSession(historyId)) {
        logger.info("User logged out:", sign);
      } else {
        logger.warning("User was not logged in:", sign);
      }
    } else if (BeeUtils.isEmpty(getUsers())) {
      logger.warning("Anonymous user logged out:", getCurrentUser());

    } else {
      logger.severe("Logout attempt by an unauthorized user:", getCurrentUser());
    }
  }

  @Lock(LockType.WRITE)
  public ResponseObject setRoleRights(RightsObjectType type, Long role,
      Map<String, String> changes) {

    Assert.notNull(type);
    Assert.state(DataUtils.isId(role));

    Multimap<String, Integer> plus = HashMultimap.create();
    Multimap<String, Integer> minus = HashMultimap.create();

    if (!BeeUtils.isEmpty(changes)) {
      for (String object : changes.keySet()) {
        Multimap<RightsState, Long> obInfo = rightsCache.get(type, object);

        if (obInfo == null) {
          obInfo = HashMultimap.create();
          rightsCache.put(type, object, obInfo);
        }
        for (RightsState state : EnumUtils.parseIndexSet(RightsState.class, changes.get(object))) {
          if (obInfo.containsEntry(state, role)) {
            minus.put(object, state.ordinal());
            obInfo.remove(state, role);
          } else {
            plus.put(object, state.ordinal());
            obInfo.put(state, role);
          }
        }
      }
    }
    return saveRights(type, COL_ROLE, role, COL_STATE, plus, minus);
  }

  @Lock(LockType.WRITE)
  public ResponseObject setStateRights(RightsObjectType type, RightsState state,
      Map<String, String> changes) {

    Assert.noNulls(type, state);

    Multimap<String, Long> plus = HashMultimap.create();
    Multimap<String, Long> minus = HashMultimap.create();

    if (!BeeUtils.isEmpty(changes)) {
      for (String object : changes.keySet()) {
        Multimap<RightsState, Long> obInfo = rightsCache.get(type, object);

        if (obInfo == null) {
          obInfo = HashMultimap.create();
          rightsCache.put(type, object, obInfo);
        }
        for (Long role : DataUtils.parseIdSet(changes.get(object))) {
          if (obInfo.containsEntry(state, role)) {
            minus.put(object, role);
            obInfo.remove(state, role);
          } else {
            plus.put(object, role);
            obInfo.put(state, role);
          }
        }
      }
    }
    return saveRights(type, COL_STATE, state.ordinal(), COL_ROLE, plus, minus);
  }

  @Lock(LockType.WRITE)
  public boolean updateUserLocale(String user, SupportedLocale locale) {
    if (!isUser(user) || locale == null) {
      return false;
    }

    Long userId = getUserId(user);
    if (!DataUtils.isId(userId)) {
      return false;
    }

    UserInfo userInfo = getUserInfo(userId);
    if (userInfo == null || userInfo.getUserLocale() == locale) {
      return false;
    }

    userInfo.setUserLocale(locale);

    qs.updateData(new SqlUpdate(TBL_USERS)
        .addConstant(COL_USER_LOCALE, locale.ordinal())
        .setWhere(sys.idEquals(TBL_USERS, userId)));

    logger.info("user", user, "updated locale:", locale.getLanguage());
    return true;
  }

  public boolean validateHost(HttpServletRequest request) {
    if (ipFilters.isEmpty()) {
      return true;
    }
    Assert.notNull(request);
    String addr = request.getRemoteAddr();
    DateTime now = TimeUtils.nowMinutes();

    for (IpFilter ipFilter : ipFilters) {
      if (ipFilter.isBlocked(addr, now)) {
        logger.warning("remote address", addr, "blocked", BeeUtils.bracket(ipFilter.host));
        return false;
      }
    }
    return true;
  }

  private UserInfo getCurrentUserInfo() {
    return getUserInfo(getCurrentUserId());
  }

  private UserInfo getUserInfo(Long userId) {
    return infoCache.get(getUserName(userId));
  }

  private Table<RightsState, RightsObjectType, Set<String>> getUserRights(long userId) {
    Table<RightsState, RightsObjectType, Set<String>> rights = HashBasedTable.create();
    long[] userRoles = getUserRoles(userId);

    for (RightsObjectType type : rightsCache.rowKeySet()) {
      Map<String, Multimap<RightsState, Long>> row = rightsCache.row(type);

      for (String object : row.keySet()) {
        Multimap<RightsState, Long> states = row.get(object);

        for (RightsState state : states.keySet()) {
          boolean checked = state.isChecked();

          for (Long role : userRoles) {
            if (states.containsEntry(state, role) != checked) {
              checked = !checked;
              break;
            }
          }
          if (checked) {
            Set<String> objects = rights.get(state, type);

            if (objects == null) {
              objects = Sets.newHashSet();
              rights.put(state, type, objects);
            }
            objects.add(object);
          }
        }
      }
    }
    return rights;
  }

  private <K, V> ResponseObject saveRights(RightsObjectType type, String keyName, K key,
      String valueName, Multimap<String, V> plus, Multimap<String, V> minus) {

    int cnt = 0;

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_OBJECTS, COL_OBJECT_NAME)
        .addField(TBL_OBJECTS, sys.getIdName(TBL_OBJECTS), COL_OBJECT)
        .addFrom(TBL_OBJECTS)
        .setWhere(SqlUtils.equals(TBL_OBJECTS, COL_OBJECT_TYPE, type.ordinal())));

    HasConditions wh = SqlUtils.or();

    for (String object : minus.keySet()) {
      Long id = BeeUtils.toLongOrNull(rs.getValueByKey(COL_OBJECT_NAME, object, COL_OBJECT));

      if (DataUtils.isId(id)) {
        wh.add(SqlUtils.and(SqlUtils.equals(TBL_RIGHTS,
            COL_OBJECT, id, keyName, key),
            SqlUtils.inList(TBL_RIGHTS, valueName, minus.get(object))));
      }
    }
    if (!wh.isEmpty()) {
      cnt += qs.updateData(new SqlDelete(TBL_RIGHTS)
          .setWhere(wh));
    }
    for (String object : plus.keySet()) {
      Long id = BeeUtils.toLongOrNull(rs.getValueByKey(COL_OBJECT_NAME, object, COL_OBJECT));

      if (!DataUtils.isId(id)) {
        id = qs.insertData(new SqlInsert(TBL_OBJECTS)
            .addConstant(COL_OBJECT_TYPE, type.ordinal())
            .addConstant(COL_OBJECT_NAME, object));
      }
      for (V value : plus.get(object)) {
        qs.insertData(new SqlInsert(TBL_RIGHTS)
            .addConstant(COL_OBJECT, id)
            .addConstant(keyName, key)
            .addConstant(valueName, value));
        cnt++;
      }
    }
    if (cnt > 0) {
      for (Entry<String, UserInfo> entry : infoCache.entrySet()) {
        entry.getValue().getUserData()
            .setRights(getUserRights(userCache.inverse().get(entry.getKey())));
      }
      Endpoint.updateUserData(getAllUserData());
    }
    return ResponseObject.response(cnt);
  }
}
