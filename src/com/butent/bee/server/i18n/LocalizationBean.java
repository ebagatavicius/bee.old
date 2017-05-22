package com.butent.bee.server.i18n;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
@Lock(LockType.READ)
public class LocalizationBean {

  private static BeeLogger logger = LogUtils.getLogger(LocalizationBean.class);

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;

  @Lock(LockType.WRITE)
  public Collection<SupportedLocale> customizeGlossaries() {
    EnumSet<SupportedLocale> customized = EnumSet.noneOf(SupportedLocale.class);

    for (SupportedLocale supportedLocale : SupportedLocale.values()) {
      Map<String, String> glossary =
          getDictionaryData(supportedLocale.getDictionaryCustomColumnName());

      Localizations.setCustomGlossary(supportedLocale, glossary);
      if (!glossary.isEmpty()) {
        customized.add(supportedLocale);
      }
    }

    return customized;
  }

  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(reqInfo.getService());

    switch (svc) {
      case Service.PREPARE_DICTIONARY:
        response = prepareDictionary();
        break;

      case Service.CUSTOMIZE_DICTIONARY:
        response = customizeDictionary(reqInfo);
        break;

      default:
        String msg = BeeUtils.joinWords("localization service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  private ResponseObject customizeDictionary(RequestInfo reqInfo) {
    SupportedLocale locale = SupportedLocale.getByLanguage(reqInfo.getParameter(VAR_LOCALE));
    if (locale == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_LOCALE);
    }

    Localizations.setCustomGlossary(locale,
        getDictionaryData(locale.getDictionaryCustomColumnName()));

    if (locale.isUserDefault() || locale == SupportedLocale.DICTIONARY_DEFAULT) {
      Localized.setGlossary(Localizations.getGlossary(SupportedLocale.getUserDefault()));
    }

    SupportedLocale userLocale = usr.getSupportedLocale();
    if (userLocale != null
        && (locale == userLocale || locale == SupportedLocale.DICTIONARY_DEFAULT)) {

      return ResponseObject.response(Localizations.getGlossary(userLocale));

    } else {
      return ResponseObject.emptyResponse();
    }
  }

  private Map<String, String> getDictionaryData(String column) {
    Map<String, String> result = new HashMap<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_DICTIONARY, COL_DICTIONARY_KEY, column)
        .addFrom(TBL_DICTIONARY)
        .setWhere(SqlUtils.notNull(TBL_DICTIONARY, column));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        String key = BeeUtils.trim(row.getValue(0));
        String value = BeeUtils.trim(Codec.unescapePropertyValue(row.getValue(1)));

        if (!key.isEmpty() && !value.isEmpty()) {
          result.put(key, value);
        }
      }
    }

    return result;
  }

  @Lock(LockType.WRITE)
  private ResponseObject prepareDictionary() {
    ResponseObject response;

    Map<SupportedLocale, Map<String, String>> glossaries = Localizations.getDefaultGlossaries();
    if (BeeUtils.isEmpty(glossaries)) {
      return ResponseObject.error(Service.PREPARE_DICTIONARY, "default glossaries not available");
    }

    Map<String, String> mainGlossary = glossaries.get(SupportedLocale.DICTIONARY_DEFAULT);
    if (BeeUtils.isEmpty(mainGlossary)) {
      return ResponseObject.error(Service.PREPARE_DICTIONARY, SupportedLocale.DICTIONARY_DEFAULT,
          "glossary is empty");
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_DICTIONARY, COL_DICTIONARY_KEY)
        .addFrom(TBL_DICTIONARY);

    Set<String> oldKeys = qs.getValueSet(query);
    Set<String> newKeys = new HashSet<>(mainGlossary.keySet());

    Set<String> removeKeys = BeeUtils.difference(oldKeys, newKeys);
    if (!BeeUtils.isEmpty(removeKeys)) {
      SqlDelete delete = new SqlDelete(TBL_DICTIONARY);

      for (String key : removeKeys) {
        delete.setWhere(SqlUtils.equals(TBL_DICTIONARY, COL_DICTIONARY_KEY, key));

        response = qs.updateDataWithResponse(delete);
        if (response.hasErrors()) {
          return response;
        }
      }

      logger.info(Service.PREPARE_DICTIONARY, "removed", removeKeys.size(), "keys");
    }

    Set<String> intersection = BeeUtils.intersection(oldKeys, newKeys);
    if (!BeeUtils.isEmpty(intersection)) {
      for (SupportedLocale supportedLocale : SupportedLocale.values()) {
        String columnName = supportedLocale.getDictionaryDefaultColumnName();

        Map<String, String> newGlossary = glossaries.get(supportedLocale);

        if (BeeUtils.isEmpty(newGlossary)) {
          logger.warning(Service.PREPARE_DICTIONARY, "default glossary", supportedLocale,
              "is empty");

          SqlUpdate update = new SqlUpdate(TBL_DICTIONARY)
              .addConstant(columnName, null)
              .setWhere(SqlUtils.notNull(TBL_DICTIONARY, columnName));

          response = qs.updateDataWithResponse(update);
          if (response.hasErrors()) {
            return response;
          }

        } else {
          Map<String, String> oldGlossary = getDictionaryData(columnName);
          int updateCount = 0;

          for (String key : intersection) {
            String newValue = newGlossary.get(key);

            if (!BeeUtils.equalsTrimRight(newValue, oldGlossary.get(key))) {
              SqlUpdate update = new SqlUpdate(TBL_DICTIONARY)
                  .addConstant(columnName, Codec.escapePropertyValue(newValue))
                  .setWhere(SqlUtils.equals(TBL_DICTIONARY, COL_DICTIONARY_KEY, key));

              response = qs.updateDataWithResponse(update);
              if (response.hasErrors()) {
                return response;
              }

              updateCount++;
            }
          }

          if (updateCount > 0) {
            logger.info(Service.PREPARE_DICTIONARY, columnName, "updated", updateCount, "keys");
          }
        }
      }
    }

    Set<String> addKeys = BeeUtils.difference(newKeys, oldKeys);
    if (!BeeUtils.isEmpty(addKeys)) {
      SqlInsert insert = new SqlInsert(TBL_DICTIONARY);

      for (String key : addKeys) {
        insert.reset();
        insert.addConstant(COL_DICTIONARY_KEY, key);

        glossaries.forEach((supportedLocale, glossary) -> {
          String value = glossary.get(key);
          if (value != null) {
            insert.addConstant(supportedLocale.getDictionaryDefaultColumnName(),
                Codec.escapePropertyValue(value));
          }
        });

        response = qs.insertDataWithResponse(insert);
        if (response.hasErrors()) {
          return response;
        }
      }

      logger.info(Service.PREPARE_DICTIONARY, "added", addKeys.size(), "keys");
    }

    response = ResponseObject.response(newKeys.size());
    return response;
  }
}
