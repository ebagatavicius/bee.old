package com.butent.bee.server.modules.administration;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.IdGeneratorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.cars.CarsModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportProperty;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.cars.Bundle;
import com.butent.bee.shared.modules.cars.CarsConstants;
import com.butent.bee.shared.modules.cars.ConfInfo;
import com.butent.bee.shared.modules.cars.Configuration;
import com.butent.bee.shared.modules.cars.Option;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import lt.locator.Report;
import lt.locator.ReportProviderInterface;
import lt.locator.ReportSummarizedPeriod;
import lt.locator.TripSumRepData;

@Stateless
@LocalBean
public class ImportBean {

  private static final class PropertyData {
    private final ImportProperty property;
    private Long id;
    private String value;
    private ImportObject relation;

    private PropertyData(ImportProperty property) {
      this.property = Assert.notNull(property);
    }
  }

  private static final class ImportObject {

    private final long objectId;
    private final String viewName;
    private final Map<String, PropertyData> props = new LinkedHashMap<>();

    private ImportObject(long optId, String viewName) {
      this.objectId = optId;
      this.viewName = viewName;
    }

    public void addProperty(ImportProperty property) {
      props.put(property.getName(), new PropertyData(property));
    }

    public SqlCreate createStructure(SystemBean sb, String parentName, SqlCreate query) {
      SqlCreate create = BeeUtils.nvl(query, new SqlCreate(SqlUtils.temporaryName())
          .addInteger(COL_REC_NO, true));

      BeeView view = BeeUtils.isEmpty(getViewName()) ? null : sb.getView(getViewName());

      for (ImportProperty prop : getProperties()) {
        if (!prop.isDataProperty()) {
          continue;
        }
        ImportObject ro = getPropertyRelation(prop.getName());

        String propName = BeeUtils.join("_", parentName, prop.getName());

        if (Objects.isNull(view)) {
          if (Objects.nonNull(ro)) {
            create.addLong(propName, false);
          } else {
            create.addText(propName, false);
          }
        } else {
          create.addField(propName, view.getColumnType(prop.getName()),
              view.getColumnPrecision(prop.getName()), view.getColumnScale(prop.getName()), false);
        }
        if (Objects.nonNull(ro)) {
          ro.createStructure(sb, propName, create);
        }
      }
      return create;
    }

    public Map<Long, String> getDeepNames(String parentName) {
      Map<Long, String> propNames = new LinkedHashMap<>();

      for (ImportProperty prop : getProperties()) {
        if (!prop.isDataProperty()) {
          continue;
        }
        String propName = prop.getName();
        Long propId = getPropertyId(propName);
        ImportObject ro = getPropertyRelation(propName);

        propName = BeeUtils.join("_", parentName, propName);
        propNames.put(propId, propName);

        if (ro != null) {
          propNames.putAll(ro.getDeepNames(propName));
        }
      }
      return propNames;
    }

    public Map<String, String> getDeepProperties(String parentName) {
      Map<String, String> propNames = new LinkedHashMap<>();

      for (ImportProperty prop : getProperties()) {
        if (!prop.isDataProperty()) {
          continue;
        }
        String propName = prop.getName();
        ImportObject ro = getPropertyRelation(propName);
        String propValue = Objects.isNull(ro) ? getPropertyValue(propName) : null;

        propName = BeeUtils.join("_", parentName, propName);
        propNames.put(propName, propValue);

        if (ro != null) {
          propNames.putAll(ro.getDeepProperties(propName));
        }
      }
      return propNames;
    }

    public long getObjectId() {
      return objectId;
    }

    public Collection<ImportProperty> getProperties() {
      List<ImportProperty> properties = new ArrayList<>();

      for (PropertyData data : props.values()) {
        properties.add(data.property);
      }
      return properties;
    }

    public ImportProperty getProperty(String prop) {
      return props.containsKey(prop) ? props.get(prop).property : null;
    }

    public Long getPropertyId(String prop) {
      return props.containsKey(prop) ? props.get(prop).id : null;
    }

    public ImportObject getPropertyRelation(String prop) {
      return props.containsKey(prop) ? props.get(prop).relation : null;
    }

    public String getPropertyValue(String prop) {
      return props.containsKey(prop) ? props.get(prop).value : null;
    }

    public String getViewName() {
      return viewName;
    }

    public boolean isPropertyLocked(String prop) {
      return BeeUtils.allNotNull(getPropertyRelation(prop), getPropertyValue(prop));
    }

    public void setPropertyId(String prop, Long id) {
      if (props.containsKey(prop)) {
        props.get(prop).id = id;
      }
    }

    public void setPropertyRelation(String prop, ImportObject io) {
      if (props.containsKey(prop)) {
        props.get(prop).relation = io;
      }
    }

    public void setPropertyValue(String prop, String value) {
      if (props.containsKey(prop)) {
        props.get(prop).value = value;
      }
    }
  }

  private static final String COL_REC_NO = "_RecNo_";
  private static final String COL_REASON = "_Reason_";

  private static Function<SQLException, ResponseObject> errorHandler = ResponseObject::error;

  @EJB SystemBean sys;
  @EJB QueryServiceBean qs;
  @EJB FileStorageBean fs;
  @EJB UserServiceBean usr;
  @EJB DataEditorBean deb;
  @EJB IdGeneratorBean ig;
  @EJB CarsModuleBean cars;
  @Resource EJBContext ctx;

  public ResponseObject createDataImportTemplates() {
    final Map<String, String> glossary = usr.getGlossary();
    final String[] tblImportPropertiesHeader = new String[] {
      sys.getIdName(TBL_IMPORT_PROPERTIES), COL_IMPORT_OPTION, COL_IMPORT_PROPERTY,
      COL_IMPORT_VALUE
    };

    Stream.of(
      TBL_BANKS, VIEW_CARS, TBL_CITIES, VIEW_COLORS, TBL_COMPANIES, TBL_COMPANY_PERSONS,
      TBL_DRIVERS, TBL_EXPEDITION_TYPES, TBL_ITEMS, TBL_PERSONS, TBL_RIGHTS, TBL_SERVICES,
      TBL_SHIPPING_TERMS
    ).forEach(viewName -> {
      final DataInfo viewInfo = sys.getDataInfo(viewName);

      if (!usr.isAnyModuleVisible(viewInfo.getModule())) {
        return;
      }
      SqlInsert optionInsertQuery = createImportOptionsInsertionQuery(viewName);
      long optionId = qs.insertData(optionInsertQuery.addConstant(COL_IMPORT_DESCRIPTION,
        BeeUtils.notEmpty(Localized.maybeTranslate(viewInfo.getCaption(), glossary), viewName)));
      long subOptionId;
      long subOptionIdL2;

      switch (viewName) {
        case TBL_BANKS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_BANK_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_BANK_CODE, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_SWIFT_CODE, "=C")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_PHONE, "=D")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_MOBILE, "=E")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ADDRESS, "=G")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_POST_INDEX, "=H")
          );
          insertEmailOptions(optionId, "=F");
          insertCityAndCountryOptions(optionId, "=I", "=J");
          insertCountryOptions(optionId, "=J");
          break;
        case VIEW_CARS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_VEHICLE_NUMBER, "=A")
          );
          // Cars->Model
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_VEHICLE_MODELS));
          insertSubOption(optionId, subOptionId, COL_MODEL);
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_VEHICLE_MODEL_NAME, "=B")
          );
          // Cars->Type
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_VEHICLE_TYPES));
          insertSubOption(optionId, subOptionId, COL_TYPE);
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_VEHICLE_TYPE_NAME, "=D")
          );
          break;
        case TBL_CITIES:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_CITY_NAME, "=A")
          );
          // Cities->Countries
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_COUNTRIES));
          insertSubOption(optionId, subOptionId, COL_COUNTRY);
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_COUNTRY_NAME, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_COUNTRY_CODE, "=C")
          );
          break;
        case VIEW_COLORS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_COLOR_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_BACKGROUND, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_FOREGROUND, "=C")
          );
          break;
        case TBL_COMPANIES:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "6")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_COMPANY_NAME, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_COMPANY_CODE, "=C")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_COMPANY_VAT_CODE, "=D")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_PHONE, "=F")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_MOBILE, "=G")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_FAX, "=H")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_WEBSITE, "=J")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ADDRESS, "=K")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_POST_INDEX, "=N")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_NOTES, "=S")
          );
          // Companies->CompanyTypes
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_COMPANY_TYPES));
          insertSubOption(optionId, subOptionId, COL_COMPANY_TYPE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_COMPANY_TYPE_NAME, "=A")
          );
          // Companies->CompanyPriorities
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(VIEW_COMPANY_PRIORITIES));
          insertSubOption(optionId, subOptionId, COL_COMPANY_PRIORITY);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "Name", "=E")
          );
          // Companies->Emails
          insertEmailOptions(optionId, "=I");
          // Companies->Cities
          insertCityAndCountryOptions(optionId, "=L", "=M");
          insertCountryOptions(optionId, "=M");
          // Companies->CompanySizes
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(VIEW_COMPANY_SIZES));
          insertSubOption(optionId, subOptionId, COL_COMPANY_SIZE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "SizeName", "=O")
          );
          // Companies->InformationSources
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(VIEW_INFORMATION_SOURCES));
          insertSubOption(optionId, subOptionId, COL_COMPANY_INFORMATION_SOURCE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "Name", "=P")
          );
          // Companies->Turnovers
          subOptionId = qs.insertData(createImportOptionsInsertionQuery("Turnovers"));
          insertSubOption(optionId, subOptionId, "Turnover");

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "Name", "=Q")
          );
          // Companies->FinancialStates
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(VIEW_FINANCIAL_STATES));
          insertSubOption(optionId, subOptionId, COL_COMPANY_FINANCIAL_STATE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "Name", "=R")
          );
          break;
        case TBL_COMPANY_PERSONS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "8")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_PHONE, "=G")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_MOBILE, "=H")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_FAX, "=I")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ADDRESS, "=K")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_POST_INDEX, "=L")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_WEBSITE, "=M")
          );
          // CompanyPersons->Companies
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_COMPANIES));
          insertSubOption(optionId, subOptionId, COL_COMPANY, true);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_COMPANY_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_COMPANY_CODE, "=B")
          );
          // CompanyPersons->Persons
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_PERSONS));
          insertSubOption(optionId, subOptionId, COL_PERSON);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_FIRST_NAME, "=C")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_LAST_NAME, "=D")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_PHONE, "=G")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_MOBILE, "=H")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_FAX, "=I")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_ADDRESS, "=K")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_POST_INDEX, "=L")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_WEBSITE, "=M")
          );
          // CompanyPersons->Persons->Emails
          insertEmailOptions(subOptionId, "=J");
          // CompanyPersons->Persons->Cities
          insertCityAndCountryOptions(subOptionId, "=N", "=O");
          // CompanyPersons->CompanyDepartments
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(VIEW_COMPANY_DEPARTMENTS));
          insertSubOption(optionId, subOptionId, COL_DEPARTMENT);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, "Name", "=E")
          );
          // CompanyPersons->CompanyDepartments->Company
          subOptionIdL2 = qs.insertData(createImportOptionsInsertionQuery(TBL_COMPANIES));
          insertSubOption(subOptionId, subOptionIdL2, COL_COMPANY, true);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_COMPANY_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_COMPANY_CODE, "=B")
          );
          // CompanyPersons->Positions
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_POSITIONS));
          insertSubOption(optionId, subOptionId, COL_POSITION);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_POSITION_NAME, "=F")
          );
          // CompanyPersons->Emails
          insertEmailOptions(optionId, "=J");
          insertCityAndCountryOptions(optionId, "=N", "=O");
          insertCountryOptions(optionId, "=O");
          break;
        case TBL_DRIVERS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "6")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_DRIVER_EXPERIENCE, "=C")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_NOTES, "=D")
          );
          // Drivers->CompanyPerson
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_COMPANY_PERSONS));
          insertSubOption(optionId, subOptionId, COL_COMPANY_PERSON);
          // Drivers->CompanyPerson->Person
          subOptionIdL2 = qs.insertData(createImportOptionsInsertionQuery(TBL_PERSONS));
          insertSubOption(subOptionId, subOptionIdL2, COL_PERSON);
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_FIRST_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_LAST_NAME, "=B")
          );
          // Drivers->CompanyPerson->Company
          subOptionIdL2 = qs.insertData(createImportOptionsInsertionQuery(TBL_COMPANIES));
          insertSubOption(subOptionId, subOptionIdL2, COL_COMPANY);
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_COMPANY_NAME, "=E")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionIdL2, COL_COMPANY_CODE, "=F")
          );
          break;
        case TBL_EXPEDITION_TYPES:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_EXPEDITION_TYPE_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_SELF_SERVICE, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_EXPEDITION_LOGISTICS, "=C")
          );
          break;
        case TBL_ITEMS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_NAME, "=C")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_ARTICLE, "=D")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_BARCODE, "=E")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_PRICE, "=G")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_VAT, "=I")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_VAT_PERCENT, "=J")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_IS_SERVICE, "=K")
          );
          //Items->Type
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_ITEM_CATEGORY_TREE));
          insertSubOption(optionId, subOptionId, COL_ITEM_TYPE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_CATEGORY_NAME, "=A")
          );
          //Items->Group
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_ITEM_CATEGORY_TREE));
          insertSubOption(optionId, subOptionId, COL_ITEM_GROUP);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_CATEGORY_NAME, "=B")
          );
          //Items->Unit
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_UNITS));
          insertSubOption(optionId, subOptionId, COL_UNIT);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_UNIT_NAME, "=F")
          );
          //Items->Currency
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_CURRENCIES));
          insertSubOption(optionId, subOptionId, COL_CURRENCY, true);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_CURRENCY_NAME, "=H")
          );
          break;
        case TBL_PERSONS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
              .addFields(tblImportPropertiesHeader)
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "6")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_FIRST_NAME, "=A")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_LAST_NAME, "=B")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_PHONE, "=C")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_MOBILE, "=D")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_FAX, "=E")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ADDRESS, "=G")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_POST_INDEX, "=H")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_WEBSITE, "=I")
              // .addValues(ig.getId(tblImpPropName), optionId, COL_DATE_OF_BIRTH, "")
              .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_NOTES, "=L")
          );
          //Persons->Emails
          insertEmailOptions(optionId, "=F");
          //Persons->Cities
          insertCityAndCountryOptions(optionId, "=K", "=J");
          insertCountryOptions(optionId, "=J");
          break;
        case TBL_RIGHTS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "7")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_STATE, "=C")
          );
          //Rights->Object
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_OBJECTS));
          insertSubOption(optionId, subOptionId, AdministrationConstants.COL_OBJECT, true);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_OBJECT_TYPE, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_OBJECT_NAME, "=B")
          );
          //Rights->Role
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_ROLES));
          insertSubOption(optionId, subOptionId, COL_ROLE);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_ROLE_NAME, "=D")
          );
          break;
        case TBL_SERVICES:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_SERVICE_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_TRANSPORTATION, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_ITEM_VAT_PERCENT, "=C")
          );
          //Services->Item
          subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_ITEMS));
          insertSubOption(optionId, subOptionId, COL_ITEM, true);

          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_ITEM_NAME, "=D")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), subOptionId, COL_ITEM_ARTICLE, "=E")
          );
          break;
        case TBL_SHIPPING_TERMS:
          qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
            .addFields(tblImportPropertiesHeader)
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, VAR_IMPORT_START_ROW, "5")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_SHIPPING_TERM_NAME, "=A")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId,
              TransportConstants.COL_DESCRIPTION, "=B")
            .addValues(ig.getId(TBL_IMPORT_PROPERTIES), optionId, COL_SELF_SERVICE, "=C")
          );
          break;
        default:
          break;
      }
    });
    return ResponseObject.emptyResponse();
  }

  public ResponseObject doImport(RequestInfo reqInfo) {
    ResponseObject response;

    ImportType type = EnumUtils.getEnumByIndex(ImportType.class,
        BeeUtils.toIntOrNull(reqInfo.getParameter(COL_IMPORT_TYPE)));

    if (type != null) {
      switch (type) {
        case COSTS:
          response = importCosts(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_IMPORT_FILE)),
              reqInfo.getParameter(Service.VAR_PROGRESS));
          break;

        case TRACKING:
          response = importTracking(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_DATE_LOW)),
              BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_DATE_HIGH)),
              reqInfo.getParameter(Service.VAR_PROGRESS));
          break;

        case CONFIGURATION:
          response = importConfiguration(reqInfo.getParameterLong(COL_IMPORT_OPTION),
              reqInfo.getParameterLong(VAR_IMPORT_FILE), reqInfo.getParameter(Service.VAR_PROGRESS),
              reqInfo.getParameterLong(COL_BRANCH));
          break;

        default:
          response = importData(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_OPTION)),
              BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_IMPORT_FILE)),
              reqInfo.getParameter(Service.VAR_PROGRESS));
          break;
      }
      if (response.hasErrors() || BeeUtils.toBoolean(reqInfo.getParameter(VAR_IMPORT_TEST))) {
        ctx.setRollbackOnly();
      }
    } else {
      response = ResponseObject.error("Import type not recognized");
    }
    return response;
  }

  private static SqlInsert createImportOptionsInsertionQuery(String viewName) {
    Assert.notEmpty(viewName);

    return new SqlInsert(TBL_IMPORT_OPTIONS)
      .addConstant(COL_IMPORT_TYPE, ImportType.DATA)
      .addConstant(COL_IMPORT_DATA, viewName);
  }

  private void applyMappings(ImportObject io, String data, String parentName) {
    String idName = sys.getIdName(TBL_IMPORT_MAPPINGS);
    Table<Long, Long, Map<Long, String>> table = HashBasedTable.create();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_IMPORT_MAPPINGS, idName, COL_IMPORT_MAPPING)
        .addFields(TBL_IMPORT_CONDITIONS, COL_IMPORT_PROPERTY, COL_IMPORT_VALUE)
        .addFrom(TBL_IMPORT_MAPPINGS)
        .addFromInner(io.getViewName(),
            sys.joinTables(io.getViewName(), TBL_IMPORT_MAPPINGS, COL_IMPORT_MAPPING))
        .addFromInner(TBL_IMPORT_CONDITIONS,
            sys.joinTables(TBL_IMPORT_MAPPINGS, TBL_IMPORT_CONDITIONS, COL_IMPORT_MAPPING))
        .setWhere(SqlUtils.equals(TBL_IMPORT_MAPPINGS, COL_IMPORT_OPTION, io.getObjectId()));

    for (SimpleRow row : qs.getData(query)) {
      Long id = row.getLong(idName);
      Long mapping = row.getLong(COL_IMPORT_MAPPING);

      if (!table.contains(id, mapping)) {
        table.put(id, mapping, new HashMap<>());
      }
      table.get(id, mapping).put(row.getLong(COL_IMPORT_PROPERTY), row.getValue(COL_IMPORT_VALUE));
    }
    Map<Long, String> names = io.getDeepNames(parentName);

    for (Long mapping : table.columnKeySet()) {
      HasConditions condition = SqlUtils.or();

      for (Map<Long, String> conds : table.column(mapping).values()) {
        HasConditions clause = SqlUtils.and();

        for (Entry<Long, String> entry : conds.entrySet()) {
          String name = names.get(entry.getKey());

          if (!BeeUtils.isEmpty(name)) {
            clause.add(SqlUtils.equals(data, name, entry.getValue()));
          }
        }
        condition.add(clause);
      }
      qs.updateData(new SqlUpdate(data)
          .addConstant(parentName, mapping)
          .setWhere(SqlUtils.and(SqlUtils.isNull(data, parentName), condition)));
    }
  }

  private String commitData(ImportObject io, String data, String parentName, String parentCap,
      String progress, Map<String, Pair<Pair<Integer, Integer>, BeeRowSet>> status,
      boolean readOnly) {

    Map<String, String> dict = usr.getGlossary();
    String idName = SqlUtils.uniqueName();
    HasConditions clause = SqlUtils.or();
    HasConditions updateClause = SqlUtils.and();

    // PREPARE VIEW DATA
    SqlSelect query = new SqlSelect()
        .addMax(data, COL_REC_NO)
        .addEmptyText(COL_REASON)
        .addEmptyLong(idName)
        .addFrom(data)
        .setWhere(clause);

    if (!BeeUtils.isEmpty(parentName)) {
      query.setWhere(SqlUtils.and(SqlUtils.isNull(data, parentName), query.getWhere()));
      updateClause.add(SqlUtils.isNull(data, parentName));
      applyMappings(io, data, parentName);
    }
    BeeView view = sys.getView(io.getViewName());
    Set<String> cols = new LinkedHashSet<>();
    String tmp = SqlUtils.temporaryName();
    String progressCap = BeeUtils.join("->", parentCap,
        Localized.maybeTranslate(view.getCaption(), dict));

    for (ImportProperty prop : io.getProperties()) {
      String col = prop.getName();

      if (!prop.isDataProperty()) {
        continue;
      }
      String realCol = BeeUtils.join("_", parentName, col);

      if (BeeUtils.same(view.getSourceName(), TBL_EMAILS)
          && BeeUtils.same(view.getColumnField(col), COL_EMAIL_ADDRESS)) {

        qs.updateData(new SqlUpdate(data)
            .addExpression(realCol,
                SqlUtils.expression("LTRIM(RTRIM(LOWER(", SqlUtils.name(realCol), ")))"))
            .setWhere(SqlUtils.notNull(data, realCol)));
      }
      query.addField(data, realCol, col)
          .addGroup(data, realCol);

      clause.add(SqlUtils.notNull(data, realCol));

      ImportObject ro = io.getPropertyRelation(col);

      if (ro != null) {
        String error = commitData(ro, data, realCol, progressCap, progress, status,
            readOnly || io.isPropertyLocked(col) || !usr.canCreateData(ro.getViewName()));

        if (!BeeUtils.isEmpty(error)) {
          return error;
        }
      }
      updateClause.add(SqlUtils.or(SqlUtils.and(SqlUtils.isNull(tmp, col),
          SqlUtils.isNull(data, realCol)), SqlUtils.and(SqlUtils.notNull(tmp, col),
          SqlUtils.notNull(data, realCol), SqlUtils.join(tmp, col, data, realCol))));

      cols.add(col);
    }
    if (BeeUtils.isEmpty(cols)) {
      return null;
    }
    qs.updateData(new SqlCreate(tmp).setDataSource(query));

    if (qs.sqlCount(tmp, null) == 0) {
      qs.sqlDropTemp(tmp);
      return null;
    }
    // GET UNIQUE KEYS
    Set<Set<String>> uniqueKeys = new LinkedHashSet<>();

    for (Set<String> uniqueness : sys.getTable(view.getSourceName()).getUniqueness()) {
      boolean ok = true;

      for (String fld : uniqueness) {
        boolean found = false;

        for (String col : cols) {
          if (BeeUtils.isEmpty(view.getColumnLocale(col))
              && BeeUtils.same(view.getColumnField(col), fld)) {
            found = true;
            break;
          }
        }
        if (!found) {
          ok = false;
          break;
        }
      }
      if (ok) {
        uniqueKeys.add(uniqueness);
      }
    }
    Dictionary consts = usr.getDictionary();
    IsCondition validClause = SqlUtils.and(SqlUtils.isNull(tmp, idName),
        SqlUtils.isNull(tmp, COL_REASON));

    // CHECK REQUIRED FIELDS
    if (!readOnly) {
      for (String col : cols) {
        if (!view.isColNullable(col)) {
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(COL_REASON,
                  consts.valueEmpty(BeeUtils.join("_", parentName, col)) + "\n")
              .setWhere(SqlUtils.isNull(tmp, col)));
        }
      }
    }
    // FIND MATCHING ROWS FROM DATABASE
    String vw = SqlUtils.temporaryName();
    qs.updateData(new SqlCreate(vw)
        .setDataSource(view.getQuery(usr.getCurrentUserId(), null, null, cols)));

    SqlUpdate update = new SqlUpdate(tmp)
        .addExpression(idName, SqlUtils.field(vw, view.getSourceIdName()))
        .setFrom(vw, validClause);

    for (Set<String> keys : uniqueKeys) {
      HasConditions condition = SqlUtils.and();

      for (String col : keys) {
        condition.add(SqlUtils.notNull(tmp, col),
            view.isColNullable(col) ? SqlUtils.notNull(vw, col) : null,
            SqlUtils.joinUsing(tmp, vw, col));
      }
      qs.updateData(update.setWhere(condition));
    }
    if (qs.sqlExists(tmp, validClause)) {
      clause = SqlUtils.and();

      for (String col : cols) {
        IsCondition condition = SqlUtils.and(SqlUtils.notNull(tmp, col),
            SqlUtils.joinUsing(tmp, vw, col));

        if (view.isColNullable(col)) {
          condition = SqlUtils.and(SqlUtils.notNull(vw, col), condition);
          clause.add(SqlUtils.or(SqlUtils.and(SqlUtils.isNull(tmp, col), SqlUtils.isNull(vw, col)),
              condition));
        } else {
          clause.add(condition);
        }
      }
      qs.updateData(update.setWhere(clause));
    }
    // UPDATE
    if (!readOnly) {
      List<String> updCols = new ArrayList<>();
      clause = SqlUtils.or();

      SqlSelect upd = new SqlSelect()
          .addFields(vw, view.getSourceIdName(), view.getSourceVersionName())
          .addFrom(vw)
          .addFromInner(tmp, SqlUtils.join(vw, view.getSourceIdName(), tmp, idName))
          .setWhere(clause);

      for (String col : cols) {
        boolean ok = usr.canEditColumn(view.getName(), col);

        if (ok) {
          for (Set<String> keys : uniqueKeys) {
            if (keys.contains(col)) {
              ok = false;
              break;
            }
          }
        }
        if (ok) {
          updCols.add(col);
          upd.addFields(vw, col)
              .addField(tmp, col, col + tmp);

          IsCondition wh = SqlUtils.compare(SqlUtils.field(tmp, col), Operator.NE,
              SqlUtils.field(vw, col));

          if (view.isColNullable(col)) {
            clause.add(SqlUtils.and(SqlUtils.notNull(tmp, col), SqlUtils.notNull(vw, col), wh),
                SqlUtils.and(SqlUtils.isNull(tmp, col), SqlUtils.notNull(vw, col)),
                SqlUtils.and(SqlUtils.notNull(tmp, col), SqlUtils.isNull(vw, col)));
          } else {
            clause.add(wh);
          }
        }
      }
      if (!BeeUtils.isEmpty(updCols)) {
        SimpleRowSet updRows = qs.getData(upd);
        int c = 0;
        Pair<Integer, Integer> counters = null;

        if (!BeeUtils.isEmpty(progress)) {
          Endpoint.updateProgress(progress, BeeUtils.joinWords(progressCap,
              BeeUtils.parenthesize(consts.changedValues())), 0);
        }
        for (SimpleRow row : updRows) {
          if (!BeeUtils.isEmpty(progress)
              && !Endpoint.updateProgress(progress, ++c / (double) updRows.getNumberOfRows())) {

            qs.sqlDropTemp(vw);
            qs.sqlDropTemp(tmp);
            return consts.canceled();
          }
          List<BeeColumn> columns = new ArrayList<>();
          List<String> oldValues = new ArrayList<>();
          List<String> newValues = new ArrayList<>();

          for (String col : updCols) {
            String oldValue = row.getValue(col);
            String newValue = row.getValue(col + tmp);

            if (!Objects.equals(oldValue, newValue)) {
              BeeColumn column = view.getBeeColumn(col);

              switch (column.getType()) {
                case BOOLEAN:
                  if (!BeeUtils.isEmpty(oldValue)) {
                    oldValue = BooleanValue.pack(row.getBoolean(col));
                  }
                  break;
                case DATE:
                  if (!BeeUtils.isEmpty(oldValue)) {
                    oldValue = row.getDate(col).serialize();
                  }
                  if (!BeeUtils.isEmpty(newValue)) {
                    newValue = row.getDateTime(col).getDate().serialize();
                  }
                  break;
                default:
                  break;
              }
              columns.add(column);
              oldValues.add(oldValue);
              newValues.add(newValue);
            }
          }
          BeeRowSet rowSet = DataUtils.getUpdated(view.getName(),
              row.getLong(view.getSourceIdName()), row.getLong(view.getSourceVersionName()),
              columns, oldValues, newValues, null);

          ResponseObject response = deb.commitRow(rowSet, 0, RowInfo.class, errorHandler);

          if (!response.hasErrors()) {
            if (counters == null) {
              String viewName = view.getName();

              if (!status.containsKey(viewName)) {
                status.put(viewName, Pair.of(Pair.of(0, 0), null));
              }
              counters = status.get(viewName).getA();
            }
            counters.setB(counters.getB() + 1);
          } else {
            List<String> messages = new ArrayList<>();

            for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
              messages.add(Localized.maybeTranslate(rowSet.getColumnLabel(i), dict) + "="
                  + rowSet.getValue(0, i));
            }
            throw new BeeRuntimeException(BeeUtils.join("\n",
                response.getErrors(), Localized.maybeTranslate(view.getCaption(), dict),
                BeeUtils.parenthesize("ID=" + rowSet.getRow(0).getId()), messages));
          }
        }
      }
    }
    qs.sqlDropTemp(vw);

    // INSERT
    if (!readOnly) {
      String ins = SqlUtils.temporaryName();

      qs.updateData(new SqlCreate(ins)
          .setDataSource(new SqlSelect()
              .addAllFields(tmp)
              .addFrom(tmp)
              .setWhere(validClause)));

      // CHECK UNIQUE FIELDS
      for (Set<String> key : uniqueKeys) {
        HasConditions condition = SqlUtils.and();
        Set<String> flds = new HashSet<>();

        for (String col : key) {
          condition.add(SqlUtils.notNull(ins, col));
          flds.add(BeeUtils.join("_", parentName, col));
        }
        SimpleRowSet rs = qs.getData(new SqlSelect()
            .addFields(ins, key.toArray(new String[0]))
            .addFrom(ins)
            .setWhere(condition)
            .addGroup(ins, key.toArray(new String[0]))
            .setHaving(SqlUtils.more(SqlUtils.aggregate(SqlFunction.COUNT, null), 1)));

        if (!rs.isEmpty()) {
          condition = SqlUtils.or();

          for (SimpleRow row : rs) {
            HasConditions wh = SqlUtils.and();

            for (String col : key) {
              wh.add(SqlUtils.equals(ins, col, row.getValue(col)));
            }
            condition.add(wh);
          }
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(COL_REASON, consts.valueNotUnique(BeeUtils.join("+", flds)) + "\n")
              .setFrom(ins, SqlUtils.joinUsing(tmp, ins, COL_REC_NO))
              .setWhere(condition));

          qs.updateData(new SqlDelete(ins)
              .setWhere(condition));
        }
      }
      SimpleRowSet newRows = qs.getData(new SqlSelect()
          .addAllFields(ins)
          .addFrom(ins));
      int c = 0;
      Pair<Integer, Integer> counters = null;

      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress, BeeUtils.joinWords(progressCap,
            BeeUtils.parenthesize(consts.newValues())), 0);
      }
      for (SimpleRow row : newRows) {
        if (!BeeUtils.isEmpty(progress)
            && !Endpoint.updateProgress(progress, ++c / (double) newRows.getNumberOfRows())) {

          qs.sqlDropTemp(ins);
          qs.sqlDropTemp(tmp);
          return consts.canceled();
        }
        List<BeeColumn> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (String col : cols) {
          BeeColumn column = view.getBeeColumn(col);
          String value = row.getValue(col);

          if (column.getType() == ValueType.DATE && !BeeUtils.isEmpty(value)) {
            value = TimeUtils.toDateTimeOrNull(value).getDate().serialize();
          }
          columns.add(column);
          values.add(value);
        }
        BeeRowSet rowSet = DataUtils.createRowSetForInsert(view.getName(), columns, values);

        ResponseObject response = deb.commitRow(rowSet, 0, RowInfo.class, errorHandler);

        if (!response.hasErrors()) {
          qs.updateData(new SqlUpdate(tmp)
              .addConstant(idName, ((RowInfo) response.getResponse()).getId())
              .setWhere(SqlUtils.equals(tmp, COL_REC_NO, row.getLong(COL_REC_NO))));

          if (counters == null) {
            String viewName = view.getName();

            if (!status.containsKey(viewName)) {
              status.put(viewName, Pair.of(Pair.of(0, 0), null));
            }
            counters = status.get(viewName).getA();
          }
          counters.setA(counters.getA() + 1);
        } else {
          List<String> messages = new ArrayList<>();

          for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
            messages.add(Localized.maybeTranslate(rowSet.getColumnLabel(i), dict) + "="
                + rowSet.getValue(0, i));
          }
          throw new BeeRuntimeException(BeeUtils.join("\n",
              response.getErrors(), Localized.maybeTranslate(view.getCaption(), dict), messages));
        }
      }
      qs.sqlDropTemp(ins);
    }
    // PROCESS UNRECOGNIZED RECORDS
    if (qs.sqlExists(tmp, validClause)) {
      qs.updateData(new SqlUpdate(tmp)
          .addConstant(COL_REASON, consts.actionNotAllowed() + ": " + consts.actionCreate())
          .setWhere(validClause));
    }
    int c = qs.updateData(new SqlUpdate(tmp)
        .addConstant(idName, BeeConst.UNDEF)
        .setWhere(SqlUtils.isNull(tmp, idName)));

    if (c > 0) {
      BeeRowSet newRs = (BeeRowSet) qs.doSql(new SqlSelect()
          .setDistinctMode(true)
          .addFields(tmp, COL_REASON)
          .addFields(data, io.getDeepProperties(parentName).keySet().toArray(new String[0]))
          .addFrom(data)
          .addFromInner(tmp, updateClause)
          .setWhere(SqlUtils.equals(tmp, idName, BeeConst.UNDEF))
          .addOrder(tmp, COL_REASON)
          .getQuery());

      String viewName = view.getName();

      if (!status.containsKey(viewName)) {
        status.put(viewName, Pair.of(Pair.of(0, 0), null));
      }
      if (status.get(viewName).getB() != null) {
        status.get(viewName).getB().addRows(newRs.getRows());
      } else {
        for (BeeColumn column : newRs.getColumns()) {
          column.setLabel("<b>" + (Objects.equals(column.getId(), COL_REASON)
              ? consts.reason() : column.getLabel()) + "</b>");
        }
        status.get(viewName).setB(newRs);
      }
    }
    // UPDATE PARENT RELATION
    if (!BeeUtils.isEmpty(parentName)) {
      qs.updateData(new SqlUpdate(data)
          .addExpression(parentName, SqlUtils.field(tmp, idName))
          .setFrom(tmp, updateClause));

      if (c > 0) {
        qs.updateData(new SqlDelete(data)
            .setWhere(SqlUtils.equals(data, parentName, BeeConst.UNDEF)));
      }
    }
    qs.sqlDropTemp(tmp);
    return null;
  }

  private static ImportObject getSubObject(ImportObject io, String prefix) {
    ImportObject subObject = new ImportObject(io.getObjectId(), io.getViewName());

    for (ImportProperty property : io.getProperties()) {
      String name = property.getName();

      if (BeeUtils.isPrefix(name, prefix)) {
        subObject.addProperty(property);
        subObject.setPropertyId(name, io.getPropertyId(name));
        subObject.setPropertyValue(name, io.getPropertyValue(name));
        subObject.setPropertyRelation(name, io.getPropertyRelation(name));
      }
    }
    return subObject;
  }

  private ResponseObject importConfiguration(Long optId, Long fileId, String progress,
      Long branchId) {
    Dictionary loc = usr.getDictionary();
    ImportObject io = initImport(optId, usr.getGlossary());
    File file;

    try {
      file = fs.getFile(fileId).getFile();
    } catch (IOException ex) {
      return ResponseObject.error(ex);
    }
    Map<String, Pair<Pair<Integer, Integer>, BeeRowSet>> status = new LinkedHashMap<>();

    // Options
    ImportObject op = io.getPropertyRelation(TBL_CONF_OPTIONS);

    if (Objects.nonNull(op)) {
      try {
        File tmp = File.createTempFile("bee_", null);
        tmp.deleteOnExit();
        FileOutputStream os = new FileOutputStream(tmp);
        Files.copy(file.toPath(), os);
        os.close();
        file = tmp;
      } catch (IOException ex) {
        file.delete();
        return ResponseObject.error(ex);
      }
      ResponseObject response = importData(op.getObjectId(), fileId, progress);

      if (response.hasErrors()) {
        file.delete();
        return response;
      }
      status = response.getResponse(status, null);
    }
    SqlSelect query = new SqlSelect()
        .addField(TBL_CONF_OPTIONS, sys.getIdName(TBL_CONF_OPTIONS), COL_OPTION)
        .addFields(TBL_CONF_OPTIONS, CarsConstants.COL_GROUP, COL_OPTION_NAME, COL_CODE)
        .addFields(TBL_CONF_GROUPS, CarsConstants.COL_GROUP_NAME)
        .addFrom(TBL_CONF_OPTIONS)
        .addFromInner(TBL_CONF_GROUPS,
            sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, CarsConstants.COL_GROUP))
        .setWhere(SqlUtils.notNull(TBL_CONF_OPTIONS, COL_CODE));

    Long type = qs.getLongById(TBL_CONF_PRICELIST, branchId, COL_TYPE);

    if (DataUtils.isId(type)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.equals(TBL_CONF_GROUPS, COL_TYPE, type)));
    }
    Map<String, Option> options = new HashMap<>();

    for (SimpleRowSet.SimpleRow row : qs.getData(query)) {
      Option opt = new Option(row);
      String code = opt.getCode();

      if (options.containsKey(code)) {
        opt = null;
      }
      options.put(code, opt);
    }
    Map<String, Map<String, Integer>> errors = new LinkedHashMap<>();

    BiConsumer<String, String> errorProcessor = (err, cause) -> {
      if (!errors.containsKey(err)) {
        errors.put(err, new TreeMap<>());
      }
      errors.get(err).merge(BeeUtils.nvl(cause, ""), 1, Integer::sum);
    };
    BiFunction<String, ImportObject, Integer> critDescriptor = (prefix, importObject) -> {
      String[] critNames = BeeUtils.split(importObject.getPropertyValue(prefix + COL_CRITERIA
          + "Names"), BeeConst.CHAR_COMMA);
      String[] critValues = BeeUtils.split(importObject.getPropertyValue(prefix + COL_CRITERIA
          + "Values"), BeeConst.CHAR_COMMA);

      int critCnt = ArrayUtils.length(critNames);

      if (!Objects.equals(ArrayUtils.length(critValues), critCnt)) {
        critCnt = 0;
        errorProcessor.accept(loc.configuration() + ": " + loc.criteria(),
            loc.length() + BeeUtils.notEmpty(ArrayUtils.toString(critNames), "[]") + "<>"
                + loc.length() + BeeUtils.notEmpty(ArrayUtils.toString(critValues), "[]"));
      }
      for (int i = 0; i < critCnt; i++) {
        ImportProperty property = new ImportProperty(prefix + COL_CRITERIA + "Name" + i, null,
            true);
        importObject.addProperty(property);
        importObject.setPropertyValue(property.getName(), critNames[i]);
        property = new ImportProperty(prefix + COL_CRITERIA + "Value" + i, null, true);
        importObject.addProperty(property);
        importObject.setPropertyValue(property.getName(), critValues[i]);
      }
      return critCnt;
    };
    BiFunction<Pair<String, Integer>, SimpleRow, Map<String, String>> critBuilder = (pair, r) -> {
      Map<String, String> criteria = new LinkedHashMap<>();

      for (int i = 0; i < pair.getB(); i++) {
        String cap = r.getValue(pair.getA() + COL_CRITERIA + "Name" + i);
        String val = r.getValue(pair.getA() + COL_CRITERIA + "Value" + i);

        if (BeeUtils.allNotEmpty(cap, val)) {
          criteria.put(cap, val);
        }
      }
      return criteria;
    };
    Configuration configuration = new Configuration();

    // Branch bundles
    if (!BeeUtils.isEmpty(progress)) {
      Endpoint.updateProgress(progress, loc.configuration(), 0);
    }
    String prfx = TBL_CONF_BRANCH_BUNDLES;
    ImportObject bb = getSubObject(io, prfx);

    String[] opts = BeeUtils.split(bb.getPropertyValue(prfx + TBL_CONF_OPTIONS),
        BeeConst.CHAR_COMMA);

    int confOptCnt = ArrayUtils.length(opts);

    for (int i = 0; i < confOptCnt; i++) {
      ImportProperty property = new ImportProperty(prfx + COL_OPTION + i, null, true);
      bb.addProperty(property);
      bb.setPropertyValue(property.getName(), opts[i]);
    }
    int critCnt = critDescriptor.apply(prfx, bb);

    SqlCreate create = bb.createStructure(sys, null, null);
    String tmp = create.getTarget();
    qs.updateData(create);

    String error = loadXLSData(bb, file, tmp, progress, null,
        bb.getPropertyValue(prfx + VAR_IMPORT_SHEET),
        BeeUtils.toInt(bb.getPropertyValue(prfx + VAR_IMPORT_START_ROW)),
        BeeUtils.toInt(bb.getPropertyValue(prfx + VAR_IMPORT_END_ROW)));

    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      file.delete();
      return ResponseObject.error(error);
    }
    for (SimpleRow row : qs.getData(new SqlSelect().addAllFields(tmp).addFrom(tmp))) {
      String price = row.getValue(prfx + COL_PRICE);
      boolean ok = BeeUtils.isNonNegativeInt(price);

      if (!ok) {
        errorProcessor.accept(loc.configuration() + ": " + loc.price(), price);
      }
      List<Option> bundleOptions = new ArrayList<>();

      for (int i = 0; i < confOptCnt; i++) {
        String code = row.getValue(prfx + COL_OPTION + i);

        if (!BeeUtils.isEmpty(code)) {
          Option option = options.get(code);

          if (Objects.isNull(option)) {
            errorProcessor.accept(loc.configuration() + ": " + loc.code(), code);
            ok = false;
          } else {
            bundleOptions.add(option);
          }
        }
      }
      if (ok && !BeeUtils.isEmpty(bundleOptions)) {
        configuration.setBundleInfo(new Bundle(bundleOptions),
            ConfInfo.of(price, row.getValue(prfx + CarsConstants.COL_DESCRIPTION))
                .setCriteria(critBuilder.apply(Pair.of(prfx, critCnt), row)), false);
      }
    }
    qs.sqlDropTemp(tmp);

    // Branch options
    if (!BeeUtils.isEmpty(progress)) {
      Endpoint.updateProgress(progress, loc.options(), 0);
    }
    prfx = TBL_CONF_BRANCH_OPTIONS;
    ImportObject bo = getSubObject(io, prfx);

    critCnt = critDescriptor.apply(prfx, bo);

    create = bo.createStructure(sys, null, null);
    tmp = create.getTarget();
    qs.updateData(create);

    error = loadXLSData(bo, file, tmp, progress, null, bo.getPropertyValue(prfx + VAR_IMPORT_SHEET),
        BeeUtils.toInt(bo.getPropertyValue(prfx + VAR_IMPORT_START_ROW)),
        BeeUtils.toInt(bo.getPropertyValue(prfx + VAR_IMPORT_END_ROW)));

    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      file.delete();
      return ResponseObject.error(error);
    }
    for (SimpleRow row : qs.getData(new SqlSelect().addAllFields(tmp).addFrom(tmp))) {
      String price = row.getValue(prfx + COL_PRICE);
      boolean ok = BeeUtils.isEmpty(price) || BeeUtils.isNonNegativeInt(price);

      if (!ok) {
        errorProcessor.accept(loc.options() + ": " + loc.price(), price);
      }
      String code = row.getValue(prfx + COL_CODE);
      Option option = options.get(code);

      if (Objects.isNull(option)) {
        errorProcessor.accept(loc.options() + ": " + loc.code(), code);
      } else if (ok) {
        configuration.setOptionInfo(option,
            ConfInfo.of(price, row.getValue(prfx + CarsConstants.COL_DESCRIPTION))
                .setCriteria(critBuilder.apply(Pair.of(prfx, critCnt), row)));
      }
    }
    qs.sqlDropTemp(tmp);

    // Relations
    if (!BeeUtils.isEmpty(progress)) {
      Endpoint.updateProgress(progress, loc.relations(), 0);
    }
    prfx = TBL_CONF_RELATIONS;
    ImportObject rl = getSubObject(io, prfx);

    opts = BeeUtils.split(rl.getPropertyValue(prfx + TBL_CONF_OPTIONS), BeeConst.CHAR_COMMA);
    confOptCnt = ArrayUtils.length(opts);

    for (int i = 0; i < confOptCnt; i++) {
      ImportProperty property = new ImportProperty(prfx + COL_OPTION + i, null, true);
      rl.addProperty(property);
      rl.setPropertyValue(property.getName(), opts[i]);
    }
    critCnt = critDescriptor.apply(prfx, rl);

    create = rl.createStructure(sys, null, null);
    tmp = create.getTarget();
    qs.updateData(create);

    error = loadXLSData(rl, file, tmp, progress, null,
        rl.getPropertyValue(prfx + VAR_IMPORT_SHEET),
        BeeUtils.toInt(rl.getPropertyValue(prfx + VAR_IMPORT_START_ROW)),
        BeeUtils.toInt(rl.getPropertyValue(prfx + VAR_IMPORT_END_ROW)));

    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      file.delete();
      return ResponseObject.error(error);
    }
    String priceDefault = rl.getPropertyValue(prfx + VAR_PRICE_DEFAULT);
    String priceOptional = rl.getPropertyValue(prfx + VAR_PRICE_OPTIONAL);

    for (SimpleRow row : qs.getData(new SqlSelect().addAllFields(tmp).addFrom(tmp))) {
      String price = row.getValue(prfx + COL_PRICE);
      boolean ok = true;

      if (!BeeUtils.isEmpty(priceDefault) && Objects.equals(price, priceDefault)) {
        price = Configuration.DEFAULT_PRICE;
      } else if (!BeeUtils.isEmpty(priceOptional) && Objects.equals(price, priceOptional)) {
        price = null;
      } else if (!BeeUtils.isNonNegativeInt(price)) {
        errorProcessor.accept(loc.relations() + ": " + loc.price(), price);
        ok = false;
      }
      List<Option> bundleOptions = new ArrayList<>();

      for (int i = 0; i < confOptCnt; i++) {
        String code = row.getValue(prfx + COL_OPTION + i);

        if (!BeeUtils.isEmpty(code)) {
          Option option = options.get(code);

          if (Objects.isNull(option)) {
            errorProcessor.accept(loc.relations() + ": " + loc.code(), code);
            ok = false;
          } else {
            bundleOptions.add(option);
          }
        }
      }
      String code = row.getValue(prfx + COL_CODE);
      Option option = options.get(code);

      if (Objects.isNull(option)) {
        errorProcessor.accept(loc.relations() + ": " + loc.code(), code);

      } else if (ok && !BeeUtils.isEmpty(bundleOptions)) {
        Bundle bundle = new Bundle(bundleOptions);

        if (!configuration.getAllBundles().contains(bundle)) {
          errorProcessor.accept(loc.relations() + ": " + loc.configuration(), bundle.toString());
          ok = false;
        }
        if (!configuration.getOptions().contains(option)) {
          errorProcessor.accept(loc.relations() + ": " + loc.option(), option.toString());
          ok = false;
        }
        Set<Long> packet = new HashSet<>();

        for (String optCode : Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults()
            .splitToList(Strings.nullToEmpty(row.getValue(prfx + CarsConstants.COL_PACKET)))) {
          Option opt = options.get(optCode);

          if (Objects.isNull(opt)) {
            errorProcessor.accept(loc.relations() + ": " + loc.packet(), optCode);
            ok = false;
          } else {
            packet.add(opt.getId());
          }
        }
        if (ok) {
          configuration.setRelationInfo(option, bundle,
              ConfInfo.of(price, row.getValue(prfx + CarsConstants.COL_DESCRIPTION))
                  .setCriteria(critBuilder.apply(Pair.of(prfx, critCnt), row)),
              DataUtils.buildIdList(packet));
        }
      }
    }
    qs.sqlDropTemp(tmp);

    // Restrictions
    if (!BeeUtils.isEmpty(progress)) {
      Endpoint.updateProgress(progress, loc.restrictions(), 0);
    }
    prfx = TBL_CONF_RESTRICTIONS;
    ImportObject rs = getSubObject(io, prfx);

    create = rs.createStructure(sys, null, null);
    tmp = create.getTarget();
    qs.updateData(create);

    error = loadXLSData(rs, file, tmp, progress, null, rs.getPropertyValue(prfx + VAR_IMPORT_SHEET),
        BeeUtils.toInt(rs.getPropertyValue(prfx + VAR_IMPORT_START_ROW)),
        BeeUtils.toInt(rs.getPropertyValue(prfx + VAR_IMPORT_END_ROW)));

    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      file.delete();
      return ResponseObject.error(error);
    }
    String relRequired = rs.getPropertyValue(prfx + VAR_REL_REQUIRED);
    String relDenied = rs.getPropertyValue(prfx + VAR_REL_DENIED);

    for (SimpleRow row : qs.getData(new SqlSelect().addAllFields(tmp).addFrom(tmp))) {
      String code = row.getValue(prfx + COL_CODE + 1);
      Option option1 = options.get(code);
      boolean ok = true;

      if (Objects.isNull(option1)) {
        errorProcessor.accept(loc.restrictions() + ": " + loc.code(), code);
        ok = false;
      } else if (!configuration.getOptions().contains(option1)) {
        errorProcessor.accept(loc.restrictions() + ": " + loc.option(), option1.toString());
        ok = false;
      }
      code = row.getValue(prfx + COL_CODE + 2);
      Option option2 = options.get(code);

      if (Objects.isNull(option2)) {
        errorProcessor.accept(loc.restrictions() + ": " + loc.code(), code);
        ok = false;
      } else if (!configuration.getOptions().contains(option2)) {
        errorProcessor.accept(loc.restrictions() + ": " + loc.option(), option2.toString());
        ok = false;
      }
      String relStatus = row.getValue(prfx + COL_DENIED);
      boolean denied = false;

      if (!BeeUtils.isEmpty(relRequired) && Objects.equals(relStatus, relRequired)) {
        denied = false;
      } else if (!BeeUtils.isEmpty(relDenied) && Objects.equals(relStatus, relDenied)) {
        denied = true;
      } else {
        errorProcessor.accept(loc.restrictions() + ": " + loc.status(), relStatus);
        ok = false;
      }
      if (ok) {
        configuration.setRestriction(option1, option2, denied);
      }
    }
    qs.sqlDropTemp(tmp);
    file.delete();

    if (!BeeUtils.isEmpty(errors)) {
      for (String err : errors.keySet()) {
        BeeRowSet rSet = new BeeRowSet(new BeeColumn(loc.value()), new BeeColumn(loc.quantity()));

        errors.get(err).forEach((value, count) ->
            rSet.addRow(0, new String[] {value, BeeUtils.toString(count)}));

        status.put(err, Pair.of(null, rSet));
      }
      ctx.setRollbackOnly();
    } else if (configuration.isEmpty()) {
      return ResponseObject.error(loc.noData());
    } else {
      qs.updateData(new SqlDelete(TBL_CONF_BRANCH_BUNDLES)
          .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH, branchId)));
      qs.updateData(new SqlDelete(TBL_CONF_BRANCH_OPTIONS)
          .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH, branchId)));

      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress, loc.configuration(), 0);
      }
      double size = configuration.getBundles().size();
      int c = 0;
      status.put(loc.configuration(), Pair.of(Pair.of(Double.valueOf(size).intValue(), 0), null));

      for (Bundle bundle : configuration.getBundles()) {
        if (!BeeUtils.isEmpty(progress) && !Endpoint.updateProgress(progress, c++ / size)) {
          return ResponseObject.error(loc.canceled());
        }
        cars.setBundle(branchId, bundle,
            ConfInfo.of(configuration.getBundlePrice(bundle),
                configuration.getBundleDescription(bundle))
                .setCriteria(configuration.getBundleCriteria(bundle)), false);
      }
      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress, loc.options(), 0);
      }
      size = configuration.getOptions().size();
      c = 0;
      status.put(loc.options(), Pair.of(Pair.of(Double.valueOf(size).intValue(), 0), null));

      for (Option option : configuration.getOptions()) {
        if (!BeeUtils.isEmpty(progress) && !Endpoint.updateProgress(progress, c++ / size)) {
          return ResponseObject.error(loc.canceled());
        }
        cars.setOption(branchId, option.getId(),
            ConfInfo.of(configuration.getOptionPrice(option),
                configuration.getOptionDescription(option))
                .setCriteria(configuration.getOptionCriteria(option)));
      }
      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress, loc.relations(), 0);
      }
      size = configuration.getOptions().size() * configuration.getBundles().size();
      c = 0;
      int cnt = 0;
      Map<Long, Map<Long, Boolean>> restrictions = new HashMap<>();

      for (Option option : configuration.getOptions()) {
        for (Bundle bundle : configuration.getBundles()) {
          if (!BeeUtils.isEmpty(progress) && !Endpoint.updateProgress(progress, c++ / size)) {
            return ResponseObject.error(loc.canceled());
          }
          if (configuration.hasRelation(option, bundle)) {
            cars.setRelation(branchId, bundle.getKey(), option.getId(),
                ConfInfo.of(configuration.getRelationPrice(option, bundle),
                    configuration.getRelationDescription(option, bundle))
                    .setCriteria(configuration.getRelationCriteria(option, bundle)),
                configuration.getRelationPackets(option, bundle));
            cnt++;
          }
        }
        configuration.getRestrictions(option).forEach((relatedOption, denied) -> {
          if (!restrictions.containsKey(option.getId())) {
            restrictions.put(option.getId(), new HashMap<>());
          }
          restrictions.get(option.getId()).put(relatedOption.getId(), denied);
        });
      }
      status.put(loc.relations(), Pair.of(Pair.of(cnt, 0), null));
      cnt = restrictions.values().stream().mapToInt(Map::size).sum();

      if (cnt > 0) {
        cars.setRestrictions(branchId, restrictions);
      }
      status.put(loc.restrictions(), Pair.of(Pair.of(cnt, 0), null));
    }
    return ResponseObject.response(status);
  }

  private ResponseObject importCosts(Long optId, Long fileId, String progress) {
    Map<String, String> dict = usr.getGlossary();
    ImportObject io = initImport(optId, dict);
    final DateFormat dtf;

    if (!BeeUtils.isEmpty(io.getPropertyValue(VAR_IMPORT_DATE_FORMAT))) {
      dtf = new SimpleDateFormat(io.getPropertyValue(VAR_IMPORT_DATE_FORMAT));
    } else {
      dtf = null;
    }
    String prfx = "_";

    SqlCreate create = io.createStructure(sys, null, null)
        .addBoolean(prfx, false)
        .addBoolean(COL_FUEL, false)
        .addLong(COL_TRIP, false);

    String tmp = create.getTarget();
    qs.updateData(create);

    String error;

    try (FileInfo fileInfo = fs.getFile(fileId)) {
      error = loadXLSData(io, fileInfo.getFile(), tmp, progress, values -> {
            double qty = toDouble(values.get(COL_COSTS_QUANTITY));
            double prc = toDouble(values.get(COL_COSTS_PRICE));

            if (!BeeUtils.isPositive(qty)) {
              return false;
            }
            if (BeeUtils.isZero(prc)) {
              prc = BeeUtils.round(toDouble(values.get(COL_AMOUNT)) / qty, 5);
            }
            if (!BeeUtils.isZero(prc)) {
              values.put(COL_COSTS_PRICE, BeeUtils.toString(prc));
            } else {
              return false;
            }
            String value = values.get(COL_COSTS_DATE);

            if (!BeeUtils.isEmpty(value)) {
              DateTime date;

              if (BeeUtils.isLong(value)) {
                date = TimeUtils.toDateTimeOrNull(value);
              } else if (dtf != null) {
                try {
                  date = new DateTime(dtf.parse(value));
                } catch (ParseException e) {
                  date = null;
                }
              } else {
                date = TimeUtils.parseDateTime(value, DateOrdering.DEFAULT);
              }
              if (Objects.nonNull(date)) {
                JustDate dt = date.getDate();
                dt.increment();
                values.put(COL_COSTS_DATE, BeeUtils.toString(dt.getTime() - 1));
              } else {
                values.put(COL_COSTS_DATE, null);
              }
            }
            value = values.get(COL_COSTS_EXTERNAL_ID);

            if (BeeUtils.isEmpty(value)) {
              StringBuilder sb = new StringBuilder();

              for (String rel : new String[] {
                  COL_VEHICLE, COL_COSTS_ITEM, COL_COSTS_SUPPLIER,
                  COL_COSTS_DATE, COL_NUMBER, COL_COSTS_NOTE}) {
                ImportObject ro = io.getPropertyRelation(rel);

                if (ro != null) {
                  for (ImportProperty prop : ro.getProperties()) {
                    if (prop.isDataProperty()) {
                      sb.append(values.get(rel + "_" + prop.getName()));
                    }
                  }
                } else {
                  sb.append(values.get(rel));
                }
              }
              values.put(COL_COSTS_EXTERNAL_ID, Codec.md5(sb.toString()));
            }
            return true;
          }, io.getPropertyValue(VAR_IMPORT_SHEET),
          BeeUtils.toInt(io.getPropertyValue(VAR_IMPORT_START_ROW)),
          BeeUtils.toInt(io.getPropertyValue(VAR_IMPORT_END_ROW)));
    } catch (IOException e) {
      error = e.toString();
    }
    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.error(error);
    }
    Map<String, Pair<Pair<Integer, Integer>, BeeRowSet>> status = new LinkedHashMap<>();

    for (ImportProperty prop : io.getProperties()) {
      if (prop.isDataProperty()) {
        ImportObject ro = io.getPropertyRelation(prop.getName());

        if (ro != null) {
          error = commitData(ro, tmp, prop.getName(), null, progress, status,
              io.isPropertyLocked(prop.getName()) || !usr.canCreateData(ro.getViewName()));

          if (!BeeUtils.isEmpty(error)) {
            qs.sqlDropTemp(tmp);
            return ResponseObject.error(error);
          }
        }
      }
    }
    Dictionary consts = usr.getDictionary();

    SqlSelect select = new SqlSelect().addAllFields(tmp).addFrom(tmp);

    for (String col : new String[] {COL_COSTS_ITEM, COL_COSTS_CURRENCY}) {
      IsCondition wh = SqlUtils.isNull(tmp, col);
      BeeRowSet rs = (BeeRowSet) qs.doSql(select.setWhere(wh).getQuery());

      if (rs.getNumberOfRows() > 0) {
        qs.updateData(new SqlDelete(tmp).setWhere(wh));
        status.put(consts.valueEmpty(col), Pair.of(null, rs));
      }
    }
    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_FUEL, SqlUtils.constant(true))
        .setFrom(TBL_FUEL_TYPES,
            SqlUtils.join(tmp, COL_COSTS_ITEM, TBL_FUEL_TYPES, COL_ITEM)));

    for (String tbl : new String[] {TBL_TRIP_COSTS, TBL_TRIP_FUEL_COSTS}) {
      HasConditions wh = SqlUtils.and(BeeUtils.same(tbl, TBL_TRIP_FUEL_COSTS)
              ? SqlUtils.notNull(tmp, COL_FUEL) : SqlUtils.isNull(tmp, COL_FUEL),
          SqlUtils.notNull(tbl, COL_COSTS_EXTERNAL_ID));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(prfx, SqlUtils.constant(true))
          .setFrom(tbl, SqlUtils.and(wh, SqlUtils.joinUsing(tmp, tbl, COL_COSTS_EXTERNAL_ID))));

      if (!BeeUtils.isEmpty(io.getPropertyValue(COL_NUMBER))) {
        SqlSelect query = new SqlSelect()
            .addEmptyBoolean(prfx)
            .addFields(tbl, sys.getIdName(tbl))
            .addFields(tmp, COL_NUMBER)
            .addFields(TBL_TRIPS, COL_TRIP_NO)
            .addFrom(tmp)
            .addFromInner(tbl, SqlUtils.joinUsing(tmp, tbl, COL_COSTS_EXTERNAL_ID))
            .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, tbl, COL_TRIP))
            .setWhere(wh);

        wh = SqlUtils.and();
        String[] flds = new String[] {COL_COSTS_QUANTITY, COL_COSTS_PRICE, COL_COSTS_VAT};

        for (String fld : flds) {
          query.addExpr(SqlUtils.nvl(SqlUtils.field(tbl, fld), 0), fld)
              .addExpr(SqlUtils.nvl(SqlUtils.cast(SqlUtils.field(tmp, fld),
                  SqlDataType.DECIMAL, 15, 5), 0), prfx + fld);

          wh.add(SqlUtils.equals(SqlUtils.name(fld), SqlUtils.name(prfx + fld)));
        }
        String diff = qs.sqlCreateTemp(query);

        qs.updateData(new SqlUpdate(diff)
            .addExpression(prfx, SqlUtils.constant(true))
            .setWhere(wh));

        qs.updateData(new SqlUpdate(tbl)
            .addExpression(COL_NUMBER, SqlUtils.field(diff, COL_NUMBER))
            .setFrom(diff, SqlUtils.joinUsing(tbl, diff, sys.getIdName(tbl)))
            .setWhere(SqlUtils.notNull(diff, prfx)));

        String key = BeeUtils.joinWords(consts.changedValues(),
            Localized.maybeTranslate(sys.getView(tbl).getCaption(), dict));

        for (String fld : flds) {
          BeeRowSet rs = (BeeRowSet) qs.doSql(new SqlSelect()
              .addFields(diff, COL_TRIP_NO)
              .addConstant(io.getProperty(fld).getCaption(), "Value")
              .addField(diff, fld, "Old")
              .addField(diff, prfx + fld, "New")
              .addFrom(diff)
              .setWhere(SqlUtils.notEqual(SqlUtils.name(fld), SqlUtils.name(prfx + fld)))
              .getQuery());

          if (rs.getNumberOfRows() > 0) {
            if (!status.containsKey(key)) {
              status.put(key, Pair.of(null, rs));
            } else {
              status.get(key).getB().addRows(rs.getRows());
            }
          }
        }
        qs.sqlDropTemp(diff);
      }
    }
    IsExpression dt = SqlUtils.cast(SqlUtils.field(tmp, COL_COSTS_DATE), SqlDataType.DATE, 0, 0);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_TRIP, SqlUtils.field(TBL_TRIPS, sys.getIdName(TBL_TRIPS)))
        .setFrom(TBL_TRIPS,
            SqlUtils.and(SqlUtils.join(tmp, COL_VEHICLE, TBL_TRIPS, COL_VEHICLE),
                SqlUtils.moreEqual(dt, SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
                    SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE))),
                SqlUtils.less(dt, SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE), Long.MAX_VALUE)),
                SqlUtils.isNull(tmp, prfx), SqlUtils.notNull(tmp, COL_VEHICLE))));

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_COSTS_NOTE, SqlUtils.field(TBL_VEHICLES, COL_VEHICLE_NUMBER))
        .setFrom(TBL_VEHICLES, sys.joinTables(TBL_VEHICLES, tmp, COL_VEHICLE))
        .setWhere(SqlUtils.isNull(tmp, COL_TRIP)));

    for (String tbl : new String[] {TBL_TRIP_COSTS, TBL_TRIP_FUEL_COSTS}) {
      if (!BeeUtils.isEmpty(progress)) {
        Endpoint.updateProgress(progress,
            Localized.maybeTranslate(sys.getView(tbl).getCaption(), dict), 0);
      }
      SqlSelect query = new SqlSelect()
          .addFields(tmp, COL_COSTS_DATE, COL_COSTS_ITEM, COL_COSTS_QUANTITY, COL_COSTS_PRICE,
              COL_TRADE_VAT_PLUS, COL_COSTS_VAT, COL_TRADE_VAT_PERC,
              COL_NUMBER, COL_COSTS_NOTE, COL_TRIP, COL_COSTS_EXTERNAL_ID,
              COL_COSTS_CURRENCY, COL_COSTS_COUNTRY, COL_COSTS_SUPPLIER)
          .addFrom(tmp)
          .setWhere(SqlUtils.and(SqlUtils.isNull(tmp, prfx), BeeUtils.same(tbl, TBL_TRIP_COSTS)
              ? SqlUtils.isNull(tmp, COL_FUEL) : SqlUtils.notNull(tmp, COL_FUEL)));

      SimpleRowSet rs = qs.getData(query);

      for (int i = 0; i < rs.getNumberOfRows(); i++) {
        if (!BeeUtils.isEmpty(progress)
            && !Endpoint.updateProgress(progress, i / (double) rs.getNumberOfRows())) {

          qs.sqlDropTemp(tmp);
          return ResponseObject.error(consts.canceled());
        }
        qs.insertData(new SqlInsert(tbl)
            .addFields(rs.getColumnNames())
            .addValues((Object[]) rs.getValues(i)));
      }
      status.put(tbl, Pair.of(Pair.of(rs.getNumberOfRows(), 0), null));
    }
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(status);
  }

  private ResponseObject importData(Long optionId, Long fileId, String progress) {
    ImportObject io = initImport(optionId, usr.getGlossary());

    SqlCreate create = io.createStructure(sys, null, null);
    String tmp = create.getTarget();
    qs.updateData(create);

    String error;

    try (FileInfo fileInfo = fs.getFile(fileId)) {
      error = loadXLSData(io, fileInfo.getFile(), tmp, progress, values -> {
            BeeView view = sys.getView(io.getViewName());

            for (Entry<String, String> entry : values.entrySet()) {
              if (view.hasColumn(entry.getKey())
                  && view.getColumnType(entry.getKey()) == SqlDataType.DATE
                  && !BeeUtils.isEmpty(entry.getValue())) {

                entry.setValue(BeeUtils.toString(TimeUtils.toDateTimeOrNull(entry.getValue())
                    .getDate().getTime()));
              }
            }
            return true;
          }, io.getPropertyValue(VAR_IMPORT_SHEET),
          BeeUtils.toInt(io.getPropertyValue(VAR_IMPORT_START_ROW)),
          BeeUtils.toInt(io.getPropertyValue(VAR_IMPORT_END_ROW)));
    } catch (IOException e) {
      error = e.toString();
    }
    if (!BeeUtils.isEmpty(error)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.error(error);
    }
    Map<String, Pair<Pair<Integer, Integer>, BeeRowSet>> status = new LinkedHashMap<>();

    error = commitData(io, tmp, null, null, progress, status, !usr.canCreateData(io.getViewName()));

    qs.sqlDropTemp(tmp);

    if (!BeeUtils.isEmpty(error)) {
      return ResponseObject.error(error);
    }
    return ResponseObject.response(status);
  }

  private ResponseObject importTracking(Long optionId, Integer from, Integer to, String progress) {
    ImportObject io = initImport(optionId, usr.getGlossary());

    SimpleRowSet objects = qs.getData(new SqlSelect()
        .addField(TBL_VEHICLES, sys.getIdName(TBL_VEHICLES), COL_VEHICLE)
        .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
        .addFrom(TBL_VEHICLES)
        .setWhere(SqlUtils.notNull(TBL_VEHICLES, COL_VEHICLE_NUMBER)));

    if (DataUtils.isEmpty(objects)) {
      return ResponseObject.error(usr.getDictionary().noData());
    }
    SqlCreate create = io.createStructure(sys, null, null);
    String tmp = create.getTarget();
    qs.updateData(create);

    String country = null;
    ImportObject co = io.getPropertyRelation(COL_COUNTRY);

    if (co != null) {
      Map<Long, String> names = io.getDeepNames(null);
      country = names.get(co.getPropertyId(COL_COUNTRY_CODE));

      if (BeeUtils.isEmpty(country)) {
        country = names.get(co.getPropertyId(COL_COUNTRY_NAME));
      }
    }
    int recNo = 0;
    String login = io.getPropertyValue(VAR_IMPORT_LOGIN);
    String pwd = io.getPropertyValue(VAR_IMPORT_PASSWORD);

    JustDate dateFrom = new JustDate(from);
    JustDate dateTo = new JustDate(to);

    ReportProviderInterface port = new Report().getReportProviderImplPort();
    int c = 0;

    for (SimpleRow row : objects) {
      if (!BeeUtils.isEmpty(progress)
          && !Endpoint.updateProgress(progress, ++c / (double) objects.getNumberOfRows())) {

        qs.sqlDropTemp(tmp);
        return ResponseObject.error(usr.getDictionary().canceled());
      }
      boolean exists = true;

      for (int i = 0; i < TimeUtils.dayDiff(dateFrom, dateTo); i++) {
        DateTime dt = TimeUtils.nextDay(dateFrom, i).getDateTime();
        TimeUtils.addMinute(dt, TimeUtils.MINUTES_PER_DAY - 1);

        ReportSummarizedPeriod report = port.getSummarizedReport(login, pwd,
            TimeUtils.startOfDay(dt).toString(), dt.toString(), row.getValue(COL_VEHICLE_NUMBER),
            null, false, !BeeUtils.isEmpty(country), null, null);

        switch (report.getErrorCode()) {
          case 0:
            break;

          case -6:
            exists = false;
            break;

          default:
            qs.sqlDropTemp(tmp);
            return ResponseObject.error(report.getErrorMessage(),
                BeeUtils.parenthesize(report.getErrorCode()));
        }
        if (!exists) {
          break;
        }
        List<TripSumRepData> data = !BeeUtils.isEmpty(country)
            ? report.getTripSummaryByCountriesGrouped()
            : Collections.singletonList(report.getTripSummary());

        if (!BeeUtils.isEmpty(data)) {
          for (TripSumRepData tripData : data) {
            SqlInsert insert = new SqlInsert(tmp)
                .addConstant(COL_REC_NO, ++recNo)
                .addConstant(COL_VEHICLE, row.getLong(COL_VEHICLE))
                .addConstant(COL_DATE, dt.getDate().getTime());

            if (!BeeUtils.isEmpty(country)) {
              insert.addConstant(country, tripData.getName());
            }
            if (io.getPropertyId("DriveTimeInHours") != null) {
              insert.addConstant("DriveTimeInHours", tripData.getDriveTimeInHours());
            }
            if (io.getPropertyId("StopTimeInHours") != null) {
              insert.addConstant("StopTimeInHours", tripData.getStopTimeInHours());
            }
            if (io.getPropertyId("GpsDistanceInKm") != null) {
              insert.addConstant("GpsDistanceInKm", tripData.getGpsDistanceInKm());
            }
            if (io.getPropertyId("GpsAverageFuelUsage") != null) {
              insert.addConstant("GpsAverageFuelUsage", tripData.getGpsAverageFuelUsage());
            }
            if (io.getPropertyId("CounterFuelUsedInLiters") != null) {
              insert.addConstant("CounterFuelUsedInLiters", tripData.getCounterFuelUsedInLiters());
            }
            if (io.getPropertyId("CanDistanceInKm") != null) {
              insert.addConstant("CanDistanceInKm", tripData.getCanDistanceInKm());
            }
            if (io.getPropertyId("CanFuelUsedInLiters") != null) {
              insert.addConstant("CanFuelUsedInLiters", tripData.getCanFuelUsedInLiters());
            }
            if (io.getPropertyId("CanAverageFuelUsage") != null) {
              insert.addConstant("CanAverageFuelUsage", tripData.getCanAverageFuelUsage());
            }
            if (io.getPropertyId("CanOdometerValueStartInKm") != null) {
              insert.addConstant("CanOdometerValueStartInKm",
                  tripData.getCanOdometerValueStartInKm());
            }
            if (io.getPropertyId("CanOdometerValueEndInKm") != null) {
              insert.addConstant("CanOdometerValueEndInKm", tripData.getCanOdometerValueEndInKm());
            }
            qs.insertData(insert);
          }
        }
      }
    }
    Map<String, Pair<Pair<Integer, Integer>, BeeRowSet>> status = new LinkedHashMap<>();

    String error = commitData(io, tmp, null, null, progress, status,
        !usr.canCreateData(io.getViewName()));

    qs.sqlDropTemp(tmp);

    if (!BeeUtils.isEmpty(error)) {
      return ResponseObject.error(error);
    }
    return ResponseObject.response(status);
  }

  private ImportObject initImport(Long optId, Map<String, String> dict) {
    Assert.state(DataUtils.isId(optId));

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_IMPORT_OPTIONS, COL_IMPORT_TYPE, COL_IMPORT_DATA)
        .addFields(TBL_IMPORT_PROPERTIES, sys.getIdName(TBL_IMPORT_PROPERTIES),
            COL_IMPORT_PROPERTY, COL_IMPORT_VALUE, COL_IMPORT_RELATION_OPTION)
        .addFrom(TBL_IMPORT_OPTIONS)
        .addFromLeft(TBL_IMPORT_PROPERTIES,
            sys.joinTables(TBL_IMPORT_OPTIONS, TBL_IMPORT_PROPERTIES, COL_IMPORT_OPTION))
        .setWhere(sys.idEquals(TBL_IMPORT_OPTIONS, optId)));

    Assert.state(!rs.isEmpty());

    ImportObject io = new ImportObject(optId, rs.getValue(0, COL_IMPORT_DATA));

    ImportType type = EnumUtils.getEnumByIndex(ImportType.class, rs.getInt(0, COL_IMPORT_TYPE));

    for (ImportProperty prop : type.getProperties()) {
      String name = prop.getName();
      io.addProperty(prop);
      SimpleRow row = rs.getRowByKey(COL_IMPORT_PROPERTY, name);

      if (row != null) {
        io.setPropertyId(name, row.getLong(sys.getIdName(TBL_IMPORT_PROPERTIES)));
        io.setPropertyValue(name, row.getValue(COL_IMPORT_VALUE));
      }
    }
    if (!BeeUtils.isEmpty(io.getViewName())) {
      BeeView view = sys.getView(io.getViewName());

      for (ViewColumn col : view.getViewColumns()) {
        if (col.isHidden() || col.isReadOnly() || !col.isEditable()
            && BeeUtils.isPositive(col.getLevel())) {
          continue;
        }
        String name = col.getName();
        SimpleRow row = rs.getRowByKey(COL_IMPORT_PROPERTY, name);

        if (Objects.isNull(row) && view.isColNullable(name)) {
          continue;
        }
        ImportProperty property = new ImportProperty(name,
            Localized.maybeTranslate(view.getColumnLabel(name), dict), true);
        property.setRelation(col.getRelation());
        io.addProperty(property);

        if (Objects.nonNull(row)) {
          io.setPropertyId(name, row.getLong(sys.getIdName(TBL_IMPORT_PROPERTIES)));
          io.setPropertyValue(name, row.getValue(COL_IMPORT_VALUE));
        }
      }
    }
    for (ImportProperty prop : io.getProperties()) {
      if (!BeeUtils.isEmpty(prop.getRelation())) {
        String name = prop.getName();
        Long id = BeeUtils.toLongOrNull(rs.getValueByKey(COL_IMPORT_PROPERTY, name,
            COL_IMPORT_RELATION_OPTION));

        if (DataUtils.isId(id)) {
          io.setPropertyRelation(name, initImport(id, dict));
        }
      }
    }
    return io;
  }

  private String loadXLSData(ImportObject io, File file, String target, String progress,
      Function<Map<String, String>, Boolean> rowValidator, String sheet, int start, int end) {

    Sheet shit;

    try {
      Workbook wb = WorkbookFactory.create(file);
      wb.setMissingCellPolicy(Row.RETURN_BLANK_AS_NULL);

      shit = wb.getSheetAt(BeeUtils.isEmpty(sheet) ? 0 : wb.getSheetIndex(sheet));
    } catch (Exception e) {
      return e.getMessage();
    }
    Map<String, Integer> indexes = new HashMap<>();
    Map<String, String> props = io.getDeepProperties(null);

    for (Entry<String, String> entry : props.entrySet()) {
      String colValue = entry.getValue();

      if (BeeUtils.isPrefix(colValue, '=')) {
        indexes.put(entry.getKey(), CellReference.convertColStringToIndex(colValue.substring(1)));
      }
    }
    Map<String, String> values = new HashMap<>();
    int startRow = BeeUtils.max(start - 1, shit.getFirstRowNum());
    int endRow = shit.getLastRowNum();

    if (BeeUtils.isPositive(end)) {
      endRow = BeeUtils.min(end - 1, endRow);
    }
    endRow = BeeUtils.min(BeeUtils.isPositive(end) ? end - 1 : endRow, endRow);

    for (int i = startRow; i <= endRow; i++) {
      if (!BeeUtils.isEmpty(progress) && !Endpoint.updateProgress(progress,
          (i - startRow + 1) / (double) (endRow - startRow + 1))) {
        return usr.getDictionary().canceled();
      }
      Row row = shit.getRow(i);
      if (row == null) {
        continue;
      }
      values.clear();

      for (Entry<String, String> entry : props.entrySet()) {
        String prop = entry.getKey();
        String value = entry.getValue();
        Integer col = indexes.get(prop);

        if (BeeUtils.isNonNegative(col)) {
          value = null;
          Cell cell = row.getCell(col);

          if (cell != null) {
            switch (cell.getCellType()) {
              case Cell.CELL_TYPE_STRING:
                value = cell.getRichStringCellValue().getString();
                break;
              case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                  Date date = cell.getDateCellValue();

                  if (date != null) {
                    value = BeeUtils.toString(date.getTime());
                  }
                } else {
                  value = BeeUtils.toString(cell.getNumericCellValue());
                }
                break;
              case Cell.CELL_TYPE_BOOLEAN:
                value = BeeUtils.toString(cell.getBooleanCellValue());
                break;
              case Cell.CELL_TYPE_FORMULA:
                value = cell.getCellFormula();
                break;
            }
          }
        }
        values.put(prop, value != null ? value.trim() : value);
      }
      if (rowValidator != null && BeeUtils.isFalse(rowValidator.apply(values))) {
        continue;
      }
      SqlInsert insert = new SqlInsert(target);

      for (String name : values.keySet()) {
        insert.addNotEmpty(name, values.get(name));
      }
      if (!insert.isEmpty()) {
        ResponseObject response = qs.updateDataWithResponse(insert.addConstant(COL_REC_NO, i + 1),
            errorHandler);

        if (response.hasErrors()) {
          throw new BeeRuntimeException(BeeUtils.join("\n", response.getErrors(),
              insert.getQuery()));
        }
      }
    }
    return null;
  }

  private void insertCityAndCountryOptions(Long optionId, String cityName, String countryName) {
    Long subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_CITIES));
    insertSubOption(optionId, subOptionId, COL_CITY);

    if (!BeeUtils.isEmpty(cityName)) {
      qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
        .addConstant(COL_IMPORT_OPTION, subOptionId)
        .addConstant(COL_IMPORT_PROPERTY, COL_CITY_NAME)
        .addConstant(COL_IMPORT_VALUE, cityName));
    }
    insertCountryOptions(subOptionId, countryName);
  }

  private void insertCountryOptions(Long optionId, String country) {
    Long subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_COUNTRIES));
    insertSubOption(optionId, subOptionId, COL_COUNTRY);

    if (!BeeUtils.isEmpty(country)) {
      qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
        .addConstant(COL_IMPORT_OPTION, subOptionId)
        .addConstant(COL_IMPORT_PROPERTY, COL_COUNTRY_NAME)
        .addConstant(COL_IMPORT_VALUE, country));
    }
  }

  private void insertEmailOptions(Long optionId, String email) {
    Long subOptionId = qs.insertData(createImportOptionsInsertionQuery(TBL_EMAILS));
    insertSubOption(optionId, subOptionId, ALS_EMAIL_ID);

    if (!BeeUtils.isEmpty(email)) {
      qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
        .addConstant(COL_IMPORT_OPTION, subOptionId)
        .addConstant(COL_IMPORT_PROPERTY, COL_EMAIL)
        .addConstant(COL_IMPORT_VALUE, email));
    }
  }

  private void insertSubOption(long optionId, long subOptionId, String column) {
    insertSubOption(optionId, subOptionId, column, false);
  }

  private void insertSubOption(long optionId, long subOptionId, String column, boolean lock) {
    qs.insertData(new SqlInsert(TBL_IMPORT_PROPERTIES)
      .addConstant(COL_IMPORT_OPTION, optionId)
      .addConstant(COL_IMPORT_PROPERTY, column)
      .addConstant(COL_IMPORT_RELATION_OPTION, subOptionId)
      .addConstant(COL_IMPORT_VALUE, lock)
    );
  }

  private static double toDouble(String number) {
    return BeeUtils.toDouble(number != null
        ? number.replace(BeeConst.CHAR_COMMA, BeeConst.CHAR_POINT) : number);
  }
}
