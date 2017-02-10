package com.butent.bee.server.modules.ec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.AsynchronousRunnable;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.IdGeneratorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ec.TecDocRemote.TcdData;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.IsSql;
import com.butent.bee.server.sql.SqlBuilder;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.webservice.ButentWS;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.imageio.ImageIO;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import pl.motonet.ArrayOfString;
import pl.motonet.WSMotoOferta;
import pl.motonet.WSMotoOfertaSoap;

@Stateless
@LocalBean
public class TecDocBean implements HasTimerService {

  static {
    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
      private final HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
      private final String host = WSMotoOferta.WSMOTOOFERTA_WSDL_LOCATION.getHost();

      @Override
      public boolean verify(String hostname, SSLSession session) {
        if (BeeUtils.same(hostname, host)) {
          return true;
        }
        return verifier.verify(hostname, session);
      }
    });
  }

  private static final class RemoteItem {
    private final String supplierId;
    private final Double cost;
    private final String brand;
    private final String articleNr;
    private final String name;
    private final String descr;
    private Map<String, Double> prices;

    private RemoteItem(String supplierId, String brand, String articleNr, Double cost,
        String name, String descr) {
      this.supplierId = supplierId;
      this.brand = brand;
      this.articleNr = articleNr;
      this.cost = cost;
      this.name = name;
      this.descr = descr;
    }

    private void addPrice(String priceList, Double price) {
      if (prices == null) {
        prices = new HashMap<>();
      }
      prices.put(priceList, price);
    }
  }

  private static final class RemoteRemainder {
    private final String supplierId;
    private final String warehouse;
    private final Double remainder;

    private RemoteRemainder(String supplierId, String warehouse, Double remainder) {
      this.supplierId = supplierId;
      this.warehouse = warehouse;
      this.remainder = remainder;
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(TecDocBean.class);

  private static Boolean supportsJPEG2000;
  private static final String JPEG = "JPG";
  private static final String JPEG2000 = "JP2";

  static final String TCD_SCHEMA = "TecDoc";
  private static final String TCD_GRAPHICS_RESOURCES = "TcdResources";

  private static final String TCD_TECDOC_ID = "TecDocID";
  private static final String TCD_ARTICLE_ID = "ArticleID";
  private static final String TCD_CATEGORY_ID = "CategoryID";
  private static final String TCD_MODEL_ID = "ModelID";
  private static final String TCD_TYPE_ID = "TypeID";
  private static final String TCD_GRAPHICS_ID = "GraphicsID";
  private static final String TCD_GRAPHICS_RESOURCE_ID = "ResourceID";
  private static final String TCD_GRAPHICS_RESOURCE_NO = "ResourceNo";
  private static final String TCD_CRITERIA_ID = "CriteriaID";
  private static final String TCD_PARENT_ID = "ParentID";

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  TecDocRemote tcd;
  @EJB
  IdGeneratorBean ig;
  @EJB
  ParamHolderBean prm;
  @EJB
  ConcurrencyBean cb;

  @Resource
  TimerService timerService;

  public void adoptOrphans(String progressId, SimpleRowSet orphans) {
    BeeRowSet rs = tcd.getCrossai(orphans);

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addField(TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR, COL_TCD_ARTICLE)
        .addField(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME, COL_TCD_BRAND)
        .addFields(TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR)
        .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
        .addEmptyLong(TCD_ARTICLE_ID)
        .addFrom(TBL_TCD_ORPHANS)
        .addFrom(TBL_TCD_BRANDS)
        .setWhere(SqlUtils.sqlFalse()));

    boolean isDebugEnabled = QueryServiceBean.debugOff();

    SqlInsert insert = new SqlInsert(tmp)
        .addFields(COL_TCD_ARTICLE, COL_TCD_BRAND, COL_TCD_ARTICLE_NR, COL_TCD_BRAND_NAME);
    int c = 0;

    for (BeeRow row : rs) {
      insert.addValues(row.getValues().toArray());

      if (++c % 1e4 == 0) {
        qs.insertData(insert);
        insert.resetValues();
        logger.debug(tmp, "Inserted", c, "records");
      }
    }
    if (c % 1e4 > 0) {
      qs.insertData(insert);
      logger.debug(tmp, "Inserted", c, "records");
    }
    QueryServiceBean.debugOn(isDebugEnabled);

    String tcdArticles = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLES);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(TCD_ARTICLE_ID, SqlUtils.field(tcdArticles, TCD_ARTICLE_ID))
        .setFrom(tcdArticles,
            SqlUtils.joinUsing(tmp, tcdArticles, COL_TCD_ARTICLE_NR, COL_TCD_BRAND_NAME)));

    String zz = qs.sqlCreateTemp(new SqlSelect()
        .addFields(TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR, COL_TCD_BRAND)
        .addMax(tmp, TCD_ARTICLE_ID)
        .addFrom(tmp)
        .addFromInner(TBL_TCD_ORPHANS,
            SqlUtils.join(tmp, COL_TCD_ARTICLE, TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR))
        .addFromInner(TBL_TCD_BRANDS, SqlUtils.and(
            sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ORPHANS, COL_TCD_BRAND),
            SqlUtils.join(tmp, COL_TCD_BRAND, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)))
        .setWhere(SqlUtils.notNull(tmp, TCD_ARTICLE_ID))
        .addGroup(TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR, COL_TCD_BRAND));

    qs.sqlDropTemp(tmp);
    tmp = zz;

    importArticles(qs.sqlCreateTemp(new SqlSelect()
        .addFields(tmp, TCD_ARTICLE_ID)
        .addEmptyLong(COL_TCD_ARTICLE)
        .addFrom(tmp)
        .addFromLeft(TBL_TCD_ARTICLES,
            SqlUtils.join(tmp, TCD_ARTICLE_ID, TBL_TCD_ARTICLES, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES)))
        .addGroup(tmp, TCD_ARTICLE_ID)));

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(tmp, COL_TCD_ARTICLE_NR, COL_TCD_BRAND)
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(tmp)
        .addFromInner(TBL_TCD_ARTICLES,
            SqlUtils.join(tmp, TCD_ARTICLE_ID, TBL_TCD_ARTICLES, TCD_TECDOC_ID)));

    qs.sqlDropTemp(tmp);

    if (!data.isEmpty()) {
      c = 0;

      for (SimpleRow row : data) {
        cloneItem(row.getLong(COL_TCD_ARTICLE), row.getValue(COL_TCD_ARTICLE_NR),
            row.getLong(COL_TCD_BRAND));

        if (!BeeUtils.isEmpty(progressId)
            && !Endpoint.updateProgress(progressId, ++c / (double) data.getNumberOfRows())) {
          break;
        }
      }
      String orphanIdName = sys.getIdName(TBL_TCD_ORPHANS);

      tmp = qs.sqlCreateTemp(new SqlSelect()
          .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
          .addFields(TBL_TCD_ORPHANS, COL_TCD_SUPPLIER, COL_TCD_SUPPLIER_ID, orphanIdName)
          .addFrom(TBL_TCD_ORPHANS)
          .addFromInner(TBL_TCD_ARTICLES, SqlUtils.joinUsing(TBL_TCD_ORPHANS, TBL_TCD_ARTICLES,
              COL_TCD_ARTICLE_NR, COL_TCD_BRAND)));

      qs.loadData(TBL_TCD_ARTICLE_SUPPLIERS, new SqlSelect()
          .addFields(tmp, COL_TCD_ARTICLE, COL_TCD_SUPPLIER, COL_TCD_SUPPLIER_ID)
          .addFrom(tmp));

      qs.updateData(new SqlDelete(TBL_TCD_ORPHANS)
          .setWhere(SqlUtils.in(TBL_TCD_ORPHANS, orphanIdName, tmp, orphanIdName)));

      qs.sqlDropTemp(tmp);
    }
  }

  public Long cloneItem(Long art, String artNr, Long brand) {
    if (!DataUtils.isId(art)) {
      return null;
    }
    SqlSelect query = new SqlSelect();

    String[] cols = new String[] {
        COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_DESCRIPTION,
        COL_TCD_ARTICLE_UNIT, COL_TCD_ARTICLE_WEIGHT, COL_TCD_ARTICLE_VISIBLE};

    SimpleRow analog = qs.getRow(query.addFields(TBL_TCD_ARTICLES, cols)
        .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR, COL_TCD_BRAND)
        .addFrom(TBL_TCD_ARTICLES)
        .setWhere(sys.idEquals(TBL_TCD_ARTICLES, art)));

    if (analog == null) {
      return null;
    }
    SqlInsert insert = new SqlInsert(TBL_TCD_ARTICLES)
        .addConstant(COL_TCD_ARTICLE_NR, artNr)
        .addConstant(COL_TCD_BRAND, brand);

    for (String col : cols) {
      insert.addConstant(col, analog.getValue(col));
    }
    long newArt = qs.insertData(insert);

    Map<String, String[]> sources = ImmutableMap.of(
        TBL_TCD_ARTICLE_CRITERIA, new String[] {COL_TCD_CRITERIA, COL_TCD_CRITERIA_VALUE},
        TBL_TCD_ARTICLE_CATEGORIES, new String[] {COL_TCD_CATEGORY},
        TBL_TCD_TYPE_ARTICLES, new String[] {COL_TCD_TYPE},
        TBL_TCD_ARTICLE_CODES,
        new String[] {COL_TCD_SEARCH_NR, COL_TCD_CODE_NR, COL_TCD_BRAND, COL_TCD_OE_CODE});

    for (String source : sources.keySet()) {
      qs.loadData(source, new SqlSelect()
          .addConstant(newArt, COL_TCD_ARTICLE)
          .addFields(source, sources.get(source))
          .addFrom(source)
          .setWhere(SqlUtils.equals(source, COL_TCD_ARTICLE, art)));
    }
    qs.loadData(TBL_TCD_ARTICLE_CODES, new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_CODES, COL_TCD_ARTICLE)
        .addConstant(EcUtils.normalizeCode(artNr), COL_TCD_SEARCH_NR)
        .addConstant(artNr, COL_TCD_CODE_NR)
        .addConstant(brand, COL_TCD_BRAND)
        .addFrom(TBL_TCD_ARTICLE_CODES)
        .setWhere(SqlUtils.equals(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR,
            analog.getValue(COL_TCD_ARTICLE_NR), COL_TCD_BRAND, analog.getLong(COL_TCD_BRAND))));

    return newArt;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (ConcurrencyBean.isParameterTimer(timer, PRM_BUTENT_INTERVAL)) {
      suckButent(true);
    } else if (ConcurrencyBean.isParameterTimer(timer, PRM_MOTONET_HOURS)) {
      suckMotonet(true);
    }
  }

  public Collection<BeeParameter> getDefaultParameters() {
    return Lists.newArrayList(
        BeeParameter.createMap(Module.ECOMMERCE.getName(), PRM_BUTENT_PRICES),
        BeeParameter.createNumber(Module.ECOMMERCE.getName(), PRM_BUTENT_INTERVAL),
        BeeParameter.createText(Module.ECOMMERCE.getName(), PRM_MOTONET_HOURS));
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  public void initTimers() {
    cb.createIntervalTimer(this.getClass(), PRM_BUTENT_INTERVAL);
    cb.createCalendarTimer(this.getClass(), PRM_MOTONET_HOURS);
  }

  public void suckButent(boolean async) {
    if (async) {
      cb.asynchronousCall(new AsynchronousRunnable() {
        @Override
        public String getId() {
          return BeeUtils.join("-", TecDocBean.class.getName(), PRM_BUTENT_INTERVAL);
        }

        @Override
        public void run() {
          TecDocBean bean = Assert.notNull(Invocation.locateRemoteBean(TecDocBean.class));
          bean.suckButent(false);
        }
      });
      return;
    }
    EcSupplier supplier = EcSupplier.EOLTAS;
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    try {
      Map<String, String> fields = new LinkedHashMap<>();
      fields.put("pr", "preke");
      fields.put("sv", "savikaina");
      fields.put("ga", "gam_art");
      fields.put("gam", "gamintojas");
      fields.put("pav", "pavad");
      fields.put("apr", "aprasymas");

      Map<String, String> prices = prm.getMap(PRM_BUTENT_PRICES);

      if (!BeeUtils.isEmpty(prices)) {
        int i = 1;
        for (Entry<String, String> entry : prices.entrySet()) {
          String fld = "k" + i++;
          fields.put(fld, entry.getValue());
          prices.put(entry.getKey(), fld);
        }
      }
      StringBuilder fldList = new StringBuilder();

      for (Entry<String, String> entry : fields.entrySet()) {
        if (fldList.length() > 0) {
          fldList.append(", ");
        }
        fldList.append(entry.getValue()).append(" AS ").append(entry.getKey());
      }
      String itemsFilter = "prekes.gam_art IS NOT NULL AND prekes.gam_art != ''"
          + " AND prekes.gamintojas IS NOT NULL AND prekes.gamintojas != ''";

      SimpleRowSet rows = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData("SELECT " + fldList.toString()
                  + " FROM prekes"
                  + " WHERE " + itemsFilter,
              fields.keySet().toArray(new String[0]));

      if (rows.getNumberOfRows() > 0) {
        List<RemoteItem> data = new ArrayList<>(rows.getNumberOfRows());

        for (SimpleRow row : rows) {
          RemoteItem item = new RemoteItem(row.getValue("pr"), row.getValue("gam"),
              row.getValue("ga"), row.getDouble("sv"), row.getValue("pav"), row.getValue("apr"));

          if (!BeeUtils.isEmpty(prices)) {
            for (Entry<String, String> entry : prices.entrySet()) {
              item.addPrice(entry.getKey(), row.getDouble(entry.getValue()));
            }
          }
          data.add(item);
        }
        importItems(supplier, data);
      }
      rows = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData("SELECT likuciai.sandelis AS sn, likuciai.preke AS pr,"
                  + " sum(likuciai.kiekis) AS lk"
                  + " FROM likuciai INNER JOIN sand"
                  + " ON likuciai.sandelis = sand.sandelis AND sand.sand_mode LIKE '%e%'"
                  + " INNER JOIN prekes ON likuciai.preke = prekes.preke AND " + itemsFilter
                  + " GROUP by likuciai.sandelis, likuciai.preke HAVING lk > 0",
              "sn", "pr", "lk");

      if (rows.getNumberOfRows() > 0) {
        List<RemoteRemainder> data = Lists.newArrayListWithCapacity(rows.getNumberOfRows());

        for (SimpleRow row : rows) {
          data.add(new RemoteRemainder(row.getValue("pr"), row.getValue("sn"),
              row.getDouble("lk")));
        }
        importRemainders(supplier, data);
      }
    } catch (BeeException e) {
      logger.error(e);
    }
  }

  public void suckMotonet(boolean async) {
    if (async) {
      cb.asynchronousCall(new AsynchronousRunnable() {
        @Override
        public String getId() {
          return BeeUtils.join("-", TecDocBean.class.getName(), PRM_MOTONET_HOURS);
        }

        @Override
        public void run() {
          TecDocBean bean = Assert.notNull(Invocation.locateRemoteBean(TecDocBean.class));
          bean.suckMotonet(false);
        }
      });
      return;
    }
    EcSupplier supplier = EcSupplier.MOTOPROFIL;
    logger.info(supplier, "Waiting for data...");

    ArrayOfString info;
    try {
      WSMotoOfertaSoap port = new WSMotoOferta().getWSMotoOfertaSoap();
      info = port.zwrocCennikDetalOffline("10431", "6492", "BEE");
    } catch (Exception e) {
      info = new ArrayOfString();
      info.getString().add(e.getMessage());
    }
    if (info != null && info.getString().size() > 1) {
      int size = info.getString().size();
      List<RemoteItem> items = new ArrayList<>(size);
      List<RemoteRemainder> remainders = new ArrayList<>(size);

      logger.info(supplier, "Received", size, "records. Updating data...");

      SimpleRowSet rs = qs.getData(new SqlSelect()
          .addFields(TBL_TCD_BRANDS_MAPPING, COL_TCD_SUPPLIER_BRAND)
          .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
          .addFrom(TBL_TCD_BRANDS_MAPPING)
          .addFromLeft(TBL_TCD_BRANDS,
              sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_BRANDS_MAPPING, COL_TCD_BRAND))
          .setWhere(SqlUtils.equals(TBL_TCD_BRANDS_MAPPING, COL_TCD_SUPPLIER, supplier.ordinal())));

      Map<String, String> mappings = new HashMap<>();

      for (SimpleRow row : rs) {
        mappings.put(row.getValue(COL_TCD_SUPPLIER_BRAND), row.getValue(COL_TCD_BRAND_NAME));
      }
      for (String item : info.getString()) {
        String[] values = item.split("[|]", 8);

        if (values.length == 8) {
          String supplierBrand = values[0];

          if (mappings.containsKey(supplierBrand)) {
            String brand = mappings.get(supplierBrand);

            if (!BeeUtils.isEmpty(brand)) {
              String supplierId = supplierBrand + values[1];

              items.add(new RemoteItem(supplierId, brand, values[1],
                  BeeUtils.toDoubleOrNull(values[7].replace(',', '.')), null, null));

              remainders.add(new RemoteRemainder(supplierId, "MotoNet",
                  BeeUtils.toDoubleOrNull(values[3])));
            }
          } else {
            qs.insertData(new SqlInsert(TBL_TCD_BRANDS_MAPPING)
                .addConstant(COL_TCD_SUPPLIER, supplier.ordinal())
                .addConstant(COL_TCD_SUPPLIER_BRAND, supplierBrand));

            mappings.put(supplierBrand, null);
          }
        }
      }
      importItems(supplier, items);
      importRemainders(supplier, remainders);

    } else {
      if (info != null && info.getString().size() > 0) {
        logger.info(supplier, info.getString().get(0));
      } else {
        logger.info(supplier, "webService returned no data");
      }
    }
  }

  @Asynchronous
  public void suckTecdoc() {
    List<IsSql> init = new ArrayList<>();

    String cds = "_country_designations";
    String des = "_designations";

    init.add(new SqlCreate(cds, false)
        .setDataSource(new SqlSelect()
            .addFields("tof_country_designations", "cds_id")
            .addFields("tof_des_texts", "tex_text")
            .addFrom("tof_country_designations")
            .addFromInner("tof_des_texts", SqlUtils.join("tof_country_designations",
                "cds_tex_id", "tof_des_texts", "tex_id"))
            .addFromInner("tof_languages", SqlUtils.and(
                SqlUtils.join("tof_country_designations", "cds_lng_id", "tof_languages", "lng_id"),
                SqlUtils.equals("tof_languages", "lng_iso2", "lt")))
            .setWhere(SqlUtils.equals(SqlUtils.expression("SUBSTRING(",
                SqlUtils.field("tof_country_designations", "cds_ctm"), ", LOCATE('1', (",
                new SqlSelect()
                    .addFields("tof_countries", "cou_ctm")
                    .addFrom("tof_countries")
                    .setWhere(SqlUtils.equals("tof_countries", "cou_iso2", "LT")), ")), 1)"),
                "1"))));

    init.add(SqlUtils.createPrimaryKey(cds, SqlUtils.uniqueName(),
        Collections.singletonList("cds_id")));

    init.add(new SqlCreate(des, false)
        .setDataSource(new SqlSelect().setUnionAllMode(true)
            .addFields("tof_designations", "des_id")
            .addFields("tof_des_texts", "tex_text")
            .addFrom("tof_designations")
            .addFromInner("tof_des_texts",
                SqlUtils.join("tof_designations", "des_tex_id", "tof_des_texts", "tex_id"))
            .addFromInner("tof_languages", SqlUtils.and(
                SqlUtils.join("tof_designations", "des_lng_id", "tof_languages", "lng_id"),
                SqlUtils.equals("tof_languages", "lng_iso2", "lt")))
            .addUnion(new SqlSelect()
                .addFields("tof_designations", "des_id")
                .addFields("tof_des_texts", "tex_text")
                .addFrom("tof_designations")
                .addFromInner("tof_des_texts", SqlUtils.and(
                    SqlUtils.join("tof_designations", "des_tex_id", "tof_des_texts", "tex_id"),
                    SqlUtils.equals("tof_designations", "des_lng_id", 255)))
                .setWhere(SqlUtils.not(SqlUtils.in("tof_designations", "des_id",
                    new SqlSelect()
                        .addFields("tof_designations", "des_id")
                        .addFrom("tof_designations")
                        .addFromInner("tof_languages", SqlUtils.and(
                            SqlUtils.join("tof_designations", "des_lng_id", "tof_languages",
                                "lng_id"),
                            SqlUtils.equals("tof_languages", "lng_iso2", "lt")))
                ))))));

    init.add(SqlUtils.createPrimaryKey(des, SqlUtils.uniqueName(),
        Collections.singletonList("des_id")));

    List<TcdData> builds = new ArrayList<>();

    TcdData data = new TcdData(new SqlCreate(TBL_TCD_MODELS, false)
        .addInteger(TCD_MODEL_ID, true)
        .addString(COL_TCD_MODEL_NAME, 1000, true)
        .addString(COL_TCD_MANUFACTURER_NAME, 60, true),
        new SqlSelect()
            .addField("tof_models", "mod_id", TCD_MODEL_ID)
            .addField(cds, "tex_text", COL_TCD_MODEL_NAME)
            .addField("tof_manufacturers", "mfa_brand", COL_TCD_MANUFACTURER_NAME)
            .addFrom("tof_models")
            .addFromInner("tof_manufacturers",
                SqlUtils.join("tof_models", "mod_mfa_id", "tof_manufacturers", "mfa_id"))
            .addFromInner(cds, SqlUtils.join("tof_models", "mod_cds_id", cds, "cds_id")));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_TYPES, false)
        .addInteger(TCD_TYPE_ID, true)
        .addInteger(TCD_MODEL_ID, true)
        .addString(COL_TCD_TYPE_NAME, 1000, true)
        .addInteger(COL_TCD_PRODUCED_FROM, false)
        .addInteger(COL_TCD_PRODUCED_TO, false)
        .addInteger(COL_TCD_CCM, false)
        .addInteger(COL_TCD_KW_FROM, false)
        .addInteger(COL_TCD_KW_TO, false)
        .addDecimal(COL_TCD_CYLINDERS, 2, 0, false)
        .addDecimal(COL_TCD_MAX_WEIGHT, 6, 2, false)
        .addString(COL_TCD_ENGINE, 1000, false)
        .addString(COL_TCD_FUEL, 1000, false)
        .addString(COL_TCD_BODY, 1000, false)
        .addString(COL_TCD_AXLE, 1000, false),
        new SqlSelect()
            .addField("tof_types", "typ_id", TCD_TYPE_ID)
            .addField("tof_types", "typ_mod_id", TCD_MODEL_ID)
            .addField(cds, "tex_text", COL_TCD_TYPE_NAME)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_pcon_start"), 0, null,
                SqlUtils.name("typ_pcon_start")), COL_TCD_PRODUCED_FROM)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_pcon_end"), 0, null,
                SqlUtils.name("typ_pcon_end")), COL_TCD_PRODUCED_TO)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_ccm"), 0, null,
                SqlUtils.name("typ_ccm")), COL_TCD_CCM)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_kw_from"), 0, null,
                SqlUtils.name("typ_kw_from")), COL_TCD_KW_FROM)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_kw_upto"), 0, null,
                SqlUtils.name("typ_kw_upto")), COL_TCD_KW_TO)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_cylinders"), 0, null,
                SqlUtils.name("typ_cylinders")), COL_TCD_CYLINDERS)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("typ_max_weight"), 0, null,
                SqlUtils.name("typ_max_weight")), COL_TCD_MAX_WEIGHT)
            .addField("_engines", "tex_text", COL_TCD_ENGINE)
            .addField("_fuels", "tex_text", COL_TCD_FUEL)
            .addExpr(SqlUtils.nvl(SqlUtils.field("_bodies", "tex_text"),
                SqlUtils.field("_models", "tex_text")), COL_TCD_BODY)
            .addField("_axles", "tex_text", COL_TCD_AXLE)
            .addFrom("tof_types")
            .addFromInner(cds, SqlUtils.join("tof_types", "typ_cds_id", cds, "cds_id"))
            .addFromLeft(des, "_engines",
                SqlUtils.join("tof_types", "typ_kv_engine_des_id", "_engines", "des_id"))
            .addFromLeft(des, "_fuels",
                SqlUtils.join("tof_types", "typ_kv_fuel_des_id", "_fuels", "des_id"))
            .addFromLeft(des, "_bodies",
                SqlUtils.join("tof_types", "typ_kv_body_des_id", "_bodies", "des_id"))
            .addFromLeft(des, "_models",
                SqlUtils.join("tof_types", "typ_kv_model_des_id", "_models", "des_id"))
            .addFromLeft(des, "_axles",
                SqlUtils.join("tof_types", "typ_kv_axle_des_id", "_axles", "des_id")));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_ARTICLES, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addString(COL_TCD_ARTICLE_NR, 60, true)
        .addString(COL_TCD_SEARCH_NR, 50, true)
        .addString(COL_TCD_ARTICLE_NAME, 1000, true)
        .addString(COL_TCD_BRAND_NAME, 60, true),
        new SqlSelect().setLimit(100000)
            .addField("_articles", "art_id", TCD_ARTICLE_ID)
            .addField("_articles", "art_article_nr", COL_TCD_ARTICLE_NR)
            .addField("_articles", "arl_search_number", COL_TCD_SEARCH_NR)
            .addField("_articles", "tex_text", COL_TCD_ARTICLE_NAME)
            .addField("_articles", "sup_brand", COL_TCD_BRAND_NAME)
            .addFrom("_articles"));

    data.addBaseIndexes(TCD_ARTICLE_ID);
    data.addBaseIndexes(COL_TCD_SEARCH_NR, COL_TCD_BRAND_NAME);

    data.addPreparation(new SqlCreate("_articles", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_articles", "art_id", "art_article_nr")
            .addFields("tof_art_lookup", "arl_search_number")
            .addFields(des, "tex_text")
            .addFields("tof_suppliers", "sup_brand")
            .addFrom("tof_articles")
            .addFromInner("tof_art_lookup", SqlUtils.and(
                SqlUtils.join("tof_articles", "art_id", "tof_art_lookup", "arl_art_id"),
                SqlUtils.equals("tof_art_lookup", "arl_kind", "1"),
                SqlUtils.notNull("tof_art_lookup", "arl_search_number")))
            .addFromInner("tof_suppliers",
                SqlUtils.join("tof_articles", "art_sup_id", "tof_suppliers", "sup_id"))
            .addFromInner(des,
                SqlUtils.join("tof_articles", "art_complete_des_id", des, "des_id"))));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_CRITERIA, false)
        .addInteger(TCD_CRITERIA_ID, true)
        .addString(COL_TCD_CRITERIA_NAME, 1000, true)
        .addString("ShortName", 1000, false)
        .addString("UnitName", 1000, false),
        new SqlSelect()
            .addField("tof_criteria", "cri_id", TCD_CRITERIA_ID)
            .addField(des, "tex_text", COL_TCD_CRITERIA_NAME)
            .addField("short", "tex_text", "ShortName")
            .addField("unit", "tex_text", "UnitName")
            .addFrom("tof_criteria")
            .addFromInner(des, SqlUtils.join("tof_criteria", "cri_des_id", des, "des_id"))
            .addFromLeft(des, "short",
                SqlUtils.join("tof_criteria", "cri_short_des_id", "short", "des_id"))
            .addFromLeft(des, "unit",
                SqlUtils.join("tof_criteria", "cri_unit_des_id", "unit", "des_id")));

    data.addBaseIndexes(TCD_CRITERIA_ID);
    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_ARTICLE_CRITERIA, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addInteger(TCD_CRITERIA_ID, true)
        .addString(COL_TCD_CRITERIA_VALUE, 60, false),
        new SqlSelect().setLimit(500000)
            .addField("_article_criteria", "acr_art_id", TCD_ARTICLE_ID)
            .addField("_article_criteria", "acr_cri_id", TCD_CRITERIA_ID)
            .addFields("_article_criteria", COL_TCD_CRITERIA_VALUE)
            .addFrom("_article_criteria"));

    data.addBaseIndexes(TCD_ARTICLE_ID);

    data.addPreparation(new SqlCreate("_article_criteria", false)
        .setDataSource(new SqlSelect().setDistinctMode(true)
            .addFields("tof_article_criteria", "acr_art_id", "acr_cri_id")
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("acr_value"),
                SqlUtils.constant("NULL"), SqlUtils.field(des, "tex_text"),
                SqlUtils.name("acr_value")), COL_TCD_CRITERIA_VALUE)
            .addFrom("tof_article_criteria")
            .addFromLeft(des,
                SqlUtils.join("tof_article_criteria", "acr_kv_des_id", des, "des_id"))));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_ARTICLE_CODES, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addString(COL_TCD_SEARCH_NR, 50, true)
        .addString(COL_TCD_CODE_NR, 50, true)
        .addDecimal(COL_TCD_OE_CODE, 1, 0, true)
        .addString(COL_TCD_BRAND_NAME, 60, false)
        .addInteger(TCD_TECDOC_ID, true),
        new SqlSelect().setLimit(500000)
            .addField("_analogs", "arl_art_id", TCD_ARTICLE_ID)
            .addField("_analogs", "arl_search_number", COL_TCD_SEARCH_NR)
            .addField("_analogs", "arl_display_nr", COL_TCD_CODE_NR)
            .addField("_analogs", "arl_kind", COL_TCD_OE_CODE)
            .addField("_analogs", "bra_brand", COL_TCD_BRAND_NAME)
            .addField("_analogs", "id", TCD_TECDOC_ID)
            .addFrom("_analogs"));

    data.addBaseIndexes(TCD_ARTICLE_ID);

    data.addPreparation(new SqlCreate("_analogs", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_art_lookup", "arl_art_id", "arl_search_number")
            .addExpr(SqlUtils.expression("CAST(arl_kind AS DECIMAL(1))"), "arl_kind")
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("arl_kind"), "1",
                SqlUtils.field("tof_articles", "art_article_nr"),
                SqlUtils.field("tof_art_lookup", "arl_display_nr")), "arl_display_nr")
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("arl_kind"), "1",
                SqlUtils.field("tof_suppliers", "sup_brand"),
                SqlUtils.field("tof_brands", "bra_brand")), "bra_brand")
            .addFrom("tof_art_lookup")
            .addFromInner("tof_articles",
                SqlUtils.join("tof_art_lookup", "arl_art_id", "tof_articles", "art_id"))
            .addFromLeft("tof_suppliers",
                SqlUtils.join("tof_articles", "art_sup_id", "tof_suppliers", "sup_id"))
            .addFromLeft("tof_brands",
                SqlUtils.join("tof_art_lookup", "arl_bra_id", "tof_brands", "bra_id"))
            .setWhere(SqlUtils.notNull("tof_art_lookup", "arl_search_number"))));

    data.addPreparation(new IsSql() {
      @Override
      public String getSqlString(SqlBuilder builder) {
        return "ALTER TABLE _analogs ADD id INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST";
      }
    });

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_TYPE_ARTICLES, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addInteger(TCD_TYPE_ID, true),
        new SqlSelect().setLimit(1000000)
            .addField("_articles_to_types", "la_art_id", TCD_ARTICLE_ID)
            .addField("_articles_to_types", "lat_typ_id", TCD_TYPE_ID)
            .addFrom("_articles_to_types"));

    data.addBaseIndexes(TCD_ARTICLE_ID);

    data.addPreparation(new SqlCreate("_art", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_link_art", "la_id", "la_art_id")
            .addFrom("tof_link_art")));

    data.addPreparation(SqlUtils.createIndex("_art", SqlUtils.uniqueName(),
        Lists.newArrayList("la_id"), false));

    data.addPreparation(new SqlCreate("_la_typ", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_link_la_typ", "lat_la_id", "lat_typ_id")
            .addFrom("tof_link_la_typ")));

    data.addPreparation(SqlUtils.createIndex("_la_typ", SqlUtils.uniqueName(),
        Lists.newArrayList("lat_la_id"), false));

    data.addPreparation(new SqlCreate("_articles_to_types", false)
        .setDataSource(new SqlSelect().setDistinctMode(true)
            .addFields("_art", "la_art_id")
            .addFields("_la_typ", "lat_typ_id")
            .addFrom("_art")
            .addFromInner("_la_typ",
                SqlUtils.join("_art", "la_id", "_la_typ", "lat_la_id"))));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_CATEGORIES, false)
        .addInteger(TCD_CATEGORY_ID, true)
        .addInteger(TCD_PARENT_ID, false)
        .addString(COL_TCD_CATEGORY_NAME, 1000, true),
        new SqlSelect()
            .addField("tof_search_tree", "str_id", TCD_CATEGORY_ID)
            .addExpr(SqlUtils.sqlCase(SqlUtils.name("str_id_parent"), 0, null,
                SqlUtils.name("str_id_parent")), TCD_PARENT_ID)
            .addField(des, "tex_text", COL_TCD_CATEGORY_NAME)
            .addFrom("tof_search_tree")
            .addFromInner(des, SqlUtils.join("tof_search_tree", "str_des_id", des, "des_id")));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_ARTICLE_CATEGORIES, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addInteger(TCD_CATEGORY_ID, true),
        new SqlSelect().setLimit(1000000)
            .addField("_articles_to_categories", "lag_art_id", TCD_ARTICLE_ID)
            .addField("_articles_to_categories", "lgs_str_id", TCD_CATEGORY_ID)
            .addFrom("_articles_to_categories"));

    data.addBaseIndexes(TCD_ARTICLE_ID);

    data.addPreparation(new SqlCreate("_art_ga", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_link_art_ga", "lag_ga_id", "lag_art_id")
            .addFrom("tof_link_art_ga")));

    data.addPreparation(SqlUtils.createIndex("_art_ga", SqlUtils.uniqueName(),
        Lists.newArrayList("lag_ga_id"), false));

    data.addPreparation(new SqlCreate("_ga_str", false)
        .setDataSource(new SqlSelect()
            .addFields("tof_link_ga_str", "lgs_ga_id", "lgs_str_id")
            .addFrom("tof_link_ga_str")));

    data.addPreparation(SqlUtils.createIndex("_ga_str", SqlUtils.uniqueName(),
        Lists.newArrayList("lgs_ga_id"), false));

    data.addPreparation(new SqlCreate("_articles_to_categories", false)
        .setDataSource(new SqlSelect().setDistinctMode(true)
            .addFields("_art_ga", "lag_art_id")
            .addFields("_ga_str", "lgs_str_id")
            .addFrom("_art_ga")
            .addFromInner("_ga_str",
                SqlUtils.join("_art_ga", "lag_ga_id", "_ga_str", "lgs_ga_id"))));

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_GRAPHICS, false)
        .addLong(TCD_GRAPHICS_ID, true)
        .addString(COL_TCD_GRAPHICS_TYPE, 3, true)
        .addInteger(TCD_GRAPHICS_RESOURCE_ID, true)
        .addString(TCD_GRAPHICS_RESOURCE_NO, 2, true),
        new SqlSelect().setLimit(1000000)
            .addField("tof_graphics", "gra_id", TCD_GRAPHICS_ID)
            .addField("tof_doc_types", "doc_extension", COL_TCD_GRAPHICS_TYPE)
            .addField("tof_graphics", "gra_grd_id", TCD_GRAPHICS_RESOURCE_ID)
            .addField("tof_graphics", "gra_tab_nr", TCD_GRAPHICS_RESOURCE_NO)
            .addFrom("tof_graphics")
            .addFromLeft("tof_doc_types",
                SqlUtils.join("tof_graphics", "gra_doc_type", "tof_doc_types", "doc_type"))
            .setWhere(SqlUtils.and(SqlUtils.equals("tof_graphics", "gra_lng_id", 255),
                SqlUtils.notEqual("tof_graphics", "gra_grd_id", 0))));

    data.addBaseIndexes(TCD_GRAPHICS_ID);

    builds.add(data);

    data = new TcdData(new SqlCreate(TBL_TCD_ARTICLE_GRAPHICS, false)
        .addInteger(TCD_ARTICLE_ID, true)
        .addLong(TCD_GRAPHICS_ID, true)
        .addDecimal(COL_TCD_SORT, 2, 0, true),
        new SqlSelect().setLimit(1000000)
            .addField("tof_link_gra_art", "lga_art_id", TCD_ARTICLE_ID)
            .addField("tof_link_gra_art", "lga_gra_id", TCD_GRAPHICS_ID)
            .addField("tof_link_gra_art", "lga_sort", COL_TCD_SORT)
            .addFrom("tof_link_gra_art"));

    data.addBaseIndexes(TCD_ARTICLE_ID);

    builds.add(data);

    tcd.init(init);
    tcd.importTcd(builds);
    tcd.cleanup(init);

    builds.clear();

    for (String resource : qs.getColumn(new SqlSelect().setDistinctMode(true)
        .addFields(SqlUtils.table(TCD_SCHEMA, TBL_TCD_GRAPHICS), TCD_GRAPHICS_RESOURCE_NO)
        .addFrom(SqlUtils.table(TCD_SCHEMA, TBL_TCD_GRAPHICS))
        .addOrder(SqlUtils.table(TCD_SCHEMA, TBL_TCD_GRAPHICS), TCD_GRAPHICS_RESOURCE_NO))) {

      String table = "tof_gra_data_" + resource;

      data = new TcdData(new SqlCreate(TCD_GRAPHICS_RESOURCES + resource, false)
          .addInteger(TCD_GRAPHICS_RESOURCE_ID, true)
          .addText(COL_TCD_GRAPHICS_RESOURCE, true),
          new SqlSelect().setLimit(500)
              .addField(table, "grd_id", TCD_GRAPHICS_RESOURCE_ID)
              .addField(table, "grd_graphic", COL_TCD_GRAPHICS_RESOURCE)
              .addFrom(table));

      data.addBaseIndexes(TCD_GRAPHICS_RESOURCE_ID);
      builds.add(data);
    }
    tcd.importTcd(builds);
  }

  private void analyzeQuery(IsQuery query) {
    if (SqlEngine.POSTGRESQL != SqlBuilderFactory.getBuilder().getEngine()) {
      return;
    }
    BeeRowSet data = (BeeRowSet) qs.doSql("EXPLAIN " + query.getQuery());

    for (BeeRow row : data.getRows()) {
      logger.debug(row.getValues());
    }
  }

  private void importArticles(String art) {
    // ---------------- TcdArticles
    String tcdArticles = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLES);

    importBrands(new SqlSelect()
        .setDistinctMode(true)
        .addFields(tcdArticles, COL_TCD_BRAND_NAME)
        .addFrom(art)
        .addFromInner(tcdArticles, SqlUtils.joinUsing(art, tcdArticles, TCD_ARTICLE_ID)));

    int c = qs.loadData(TBL_TCD_ARTICLES, new SqlSelect().setLimit(100000)
        .addFields(tcdArticles, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_NR)
        .addField(TBL_TCD_BRANDS, sys.getIdName(TBL_TCD_BRANDS), COL_TCD_BRAND)
        .addField(tcdArticles, TCD_ARTICLE_ID, TCD_TECDOC_ID)
        .addConstant(true, COL_TCD_ARTICLE_VISIBLE)
        .addFrom(art)
        .addFromInner(tcdArticles, SqlUtils.joinUsing(art, tcdArticles, TCD_ARTICLE_ID))
        .addFromInner(TBL_TCD_BRANDS,
            SqlUtils.joinUsing(tcdArticles, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME))
        .addOrder(tcdArticles, TCD_ARTICLE_ID));

    if (c == 0) {
      qs.sqlDropTemp(art);
      return;
    }
    qs.updateData(new SqlUpdate(art)
        .addExpression(COL_TCD_ARTICLE,
            SqlUtils.field(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES)))
        .setFrom(TBL_TCD_ARTICLES,
            SqlUtils.join(art, TCD_ARTICLE_ID, TBL_TCD_ARTICLES, TCD_TECDOC_ID)));

    // ---------------- TcdArticleCriteria, TcdCriteria...
    String tcdArticleCriteria = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLE_CRITERIA);

    String artCrit = qs.sqlCreateTemp(new SqlSelect()
        .addFields(art, COL_TCD_ARTICLE)
        .addFields(tcdArticleCriteria, COL_TCD_CRITERIA_VALUE, TCD_CRITERIA_ID)
        .addFrom(art)
        .addFromInner(tcdArticleCriteria,
            SqlUtils.joinUsing(art, tcdArticleCriteria, TCD_ARTICLE_ID)));

    qs.sqlIndex(artCrit, TCD_CRITERIA_ID);
    String tcdCriteria = SqlUtils.table(TCD_SCHEMA, TBL_TCD_CRITERIA);
    String subq = SqlUtils.uniqueName();
    String als = SqlUtils.uniqueName();

    qs.loadData(TBL_TCD_CRITERIA, new SqlSelect()
        .addFields(tcdCriteria, COL_TCD_CRITERIA_NAME)
        .addField(tcdCriteria, TCD_CRITERIA_ID, TCD_TECDOC_ID)
        .addFrom(tcdCriteria)
        .addFromInner(new SqlSelect().setDistinctMode(true)
            .addFields(artCrit, TCD_CRITERIA_ID)
            .addFrom(artCrit), subq, SqlUtils.joinUsing(tcdCriteria, subq, TCD_CRITERIA_ID))
        .addFromLeft(TBL_TCD_CRITERIA, als,
            SqlUtils.join(tcdCriteria, TCD_CRITERIA_ID, als, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(als, sys.getIdName(TBL_TCD_CRITERIA))));

    qs.loadData(TBL_TCD_ARTICLE_CRITERIA, new SqlSelect().setLimit(500000)
        .addFields(artCrit, COL_TCD_ARTICLE, COL_TCD_CRITERIA_VALUE)
        .addField(TBL_TCD_CRITERIA, sys.getIdName(TBL_TCD_CRITERIA), COL_TCD_CRITERIA)
        .addFrom(artCrit)
        .addFromInner(TBL_TCD_CRITERIA,
            SqlUtils.join(artCrit, TCD_CRITERIA_ID, TBL_TCD_CRITERIA, TCD_TECDOC_ID))
        .addOrder(artCrit, COL_TCD_ARTICLE)
        .addOrder(TBL_TCD_CRITERIA, sys.getIdName(TBL_TCD_CRITERIA))
        .addOrder(artCrit, COL_TCD_CRITERIA_VALUE));

    qs.sqlDropTemp(artCrit);

    // ---------------- TcdArticleCategories, TcdCategories...
    String tcdArticleCategories = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLE_CATEGORIES);

    String artCateg = qs.sqlCreateTemp(new SqlSelect()
        .addFields(art, COL_TCD_ARTICLE)
        .addFields(tcdArticleCategories, TCD_CATEGORY_ID)
        .addFrom(art)
        .addFromInner(tcdArticleCategories,
            SqlUtils.joinUsing(art, tcdArticleCategories, TCD_ARTICLE_ID)));

    qs.sqlIndex(artCateg, TCD_CATEGORY_ID);
    String tcdCategories = SqlUtils.table(TCD_SCHEMA, TBL_TCD_CATEGORIES);

    String categ = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tcdCategories, TCD_CATEGORY_ID, COL_TCD_CATEGORY_NAME, TCD_PARENT_ID)
        .addEmptyLong(COL_TCD_CATEGORY_PARENT)
        .addFrom(tcdCategories)
        .addFromInner(new SqlSelect().setDistinctMode(true)
            .addFields(artCateg, TCD_CATEGORY_ID)
            .addFrom(artCateg), subq, SqlUtils.joinUsing(tcdCategories, subq, TCD_CATEGORY_ID))
        .addFromLeft(TBL_TCD_TECDOC_CATEGORIES, SqlUtils.join(tcdCategories, TCD_CATEGORY_ID,
            TBL_TCD_TECDOC_CATEGORIES, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(TBL_TCD_TECDOC_CATEGORIES, TCD_TECDOC_ID)));

    qs.sqlIndex(categ, TCD_CATEGORY_ID);
    qs.sqlIndex(categ, TCD_PARENT_ID);
    boolean isDebugEnabled = QueryServiceBean.debugOff();

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(categ, COL_TCD_CATEGORY_NAME, TCD_CATEGORY_ID)
        .addFrom(categ))) {

      qs.insertData(new SqlInsert(TBL_TCD_TECDOC_CATEGORIES)
          .addConstant(COL_TCD_CATEGORY, qs.insertData(new SqlInsert(TBL_TCD_CATEGORIES)
              .addConstant(COL_TCD_CATEGORY_NAME, row.getValue(COL_TCD_CATEGORY_NAME))))
          .addConstant(TCD_TECDOC_ID, row.getLong(TCD_CATEGORY_ID)));
    }
    QueryServiceBean.debugOn(isDebugEnabled);

    qs.updateData(new SqlUpdate(categ)
        .addExpression(COL_TCD_CATEGORY_PARENT,
            SqlUtils.field(TBL_TCD_TECDOC_CATEGORIES, COL_TCD_CATEGORY))
        .setFrom(TBL_TCD_TECDOC_CATEGORIES,
            SqlUtils.join(categ, TCD_PARENT_ID, TBL_TCD_TECDOC_CATEGORIES, TCD_TECDOC_ID)));

    qs.updateData(new SqlUpdate(TBL_TCD_CATEGORIES)
        .addExpression(COL_TCD_CATEGORY_PARENT, SqlUtils.field(als, COL_TCD_CATEGORY_PARENT))
        .setFrom(new SqlSelect()
                .addFields(categ, COL_TCD_CATEGORY_PARENT)
                .addFields(TBL_TCD_TECDOC_CATEGORIES, COL_TCD_CATEGORY)
                .addFrom(categ)
                .addFromInner(TBL_TCD_TECDOC_CATEGORIES, SqlUtils.join(categ, TCD_CATEGORY_ID,
                    TBL_TCD_TECDOC_CATEGORIES, TCD_TECDOC_ID)),
            als, sys.joinTables(TBL_TCD_CATEGORIES, als, COL_TCD_CATEGORY)));

    qs.sqlDropTemp(categ);

    qs.loadData(TBL_TCD_ARTICLE_CATEGORIES, new SqlSelect().setDistinctMode(true).setLimit(500000)
        .addFields(artCateg, COL_TCD_ARTICLE)
        .addFields(TBL_TCD_TECDOC_CATEGORIES, COL_TCD_CATEGORY)
        .addFrom(artCateg)
        .addFromInner(TBL_TCD_TECDOC_CATEGORIES,
            SqlUtils.join(artCateg, TCD_CATEGORY_ID, TBL_TCD_TECDOC_CATEGORIES, TCD_TECDOC_ID))
        .addOrder(artCateg, COL_TCD_ARTICLE)
        .addOrder(TBL_TCD_TECDOC_CATEGORIES, COL_TCD_CATEGORY));

    qs.sqlDropTemp(artCateg);

    // ---------------- TcdTypeArticles, TcdTypes, TcdModels, TcdManufacturers...
    String tcdTypeArticles = SqlUtils.table(TCD_SCHEMA, TBL_TCD_TYPE_ARTICLES);

    SqlSelect query = new SqlSelect()
        .addFields(art, COL_TCD_ARTICLE)
        .addFields(tcdTypeArticles, TCD_TYPE_ID)
        .addFrom(art)
        .addFromInner(tcdTypeArticles,
            SqlUtils.joinUsing(art, tcdTypeArticles, TCD_ARTICLE_ID));

    qs.tweakSql(true);
    analyzeQuery(query);
    String typArt = qs.sqlCreateTemp(query);
    qs.tweakSql(false);

    qs.sqlIndex(typArt, TCD_TYPE_ID);
    String tcdTypes = SqlUtils.table(TCD_SCHEMA, TBL_TCD_TYPES);

    String typ = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tcdTypes, TCD_TYPE_ID, COL_TCD_TYPE_NAME, TCD_MODEL_ID,
            COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO, COL_TCD_CCM, COL_TCD_KW_FROM,
            COL_TCD_KW_TO, COL_TCD_CYLINDERS, COL_TCD_MAX_WEIGHT, COL_TCD_ENGINE, COL_TCD_FUEL,
            COL_TCD_BODY, COL_TCD_AXLE)
        .addFrom(tcdTypes)
        .addFromInner(new SqlSelect().setDistinctMode(true)
            .addFields(typArt, TCD_TYPE_ID)
            .addFrom(typArt), subq, SqlUtils.joinUsing(tcdTypes, subq, TCD_TYPE_ID))
        .addFromLeft(TBL_TCD_TYPES, als,
            SqlUtils.join(tcdTypes, TCD_TYPE_ID, als, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(als, sys.getIdName(TBL_TCD_TYPES))));

    qs.sqlIndex(typ, TCD_MODEL_ID);
    String tcdModels = SqlUtils.table(TCD_SCHEMA, TBL_TCD_MODELS);

    String mod = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tcdModels, TCD_MODEL_ID, COL_TCD_MODEL_NAME, COL_TCD_MANUFACTURER_NAME)
        .addFrom(tcdModels)
        .addFromInner(new SqlSelect().setDistinctMode(true)
            .addFields(typ, TCD_MODEL_ID)
            .addFrom(typ), subq, SqlUtils.joinUsing(tcdModels, subq, TCD_MODEL_ID))
        .addFromLeft(TBL_TCD_MODELS, als,
            SqlUtils.join(tcdModels, TCD_MODEL_ID, als, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(als, sys.getIdName(TBL_TCD_MODELS))));

    qs.sqlIndex(mod, COL_TCD_MANUFACTURER_NAME);

    qs.loadData(TBL_TCD_MANUFACTURERS, new SqlSelect().setDistinctMode(true)
        .addFields(mod, COL_TCD_MANUFACTURER_NAME)
        .addConstant(true, COL_TCD_MF_VISIBLE)
        .addFrom(mod)
        .addFromLeft(TBL_TCD_MANUFACTURERS,
            SqlUtils.joinUsing(mod, TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME))
        .setWhere(SqlUtils.isNull(TBL_TCD_MANUFACTURERS, sys.getIdName(TBL_TCD_MANUFACTURERS))));

    qs.loadData(TBL_TCD_MODELS, new SqlSelect()
        .addField(TBL_TCD_MANUFACTURERS, sys.getIdName(TBL_TCD_MANUFACTURERS),
            COL_TCD_MANUFACTURER)
        .addFields(mod, COL_TCD_MODEL_NAME)
        .addField(mod, TCD_MODEL_ID, TCD_TECDOC_ID)
        .addConstant(true, COL_TCD_MODEL_VISIBLE)
        .addFrom(mod)
        .addFromInner(TBL_TCD_MANUFACTURERS,
            SqlUtils.joinUsing(mod, TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)));

    qs.sqlDropTemp(mod);

    qs.loadData(TBL_TCD_TYPES, new SqlSelect()
        .addField(TBL_TCD_MODELS, sys.getIdName(TBL_TCD_MODELS), COL_TCD_MODEL)
        .addFields(typ, COL_TCD_TYPE_NAME, COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO, COL_TCD_CCM,
            COL_TCD_KW_FROM, COL_TCD_KW_TO, COL_TCD_CYLINDERS, COL_TCD_MAX_WEIGHT, COL_TCD_ENGINE,
            COL_TCD_FUEL, COL_TCD_BODY, COL_TCD_AXLE)
        .addField(typ, TCD_TYPE_ID, TCD_TECDOC_ID)
        .addConstant(true, COL_TCD_TYPE_VISIBLE)
        .addFrom(typ)
        .addFromInner(TBL_TCD_MODELS,
            SqlUtils.join(typ, TCD_MODEL_ID, TBL_TCD_MODELS, TCD_TECDOC_ID)));

    qs.sqlDropTemp(typ);

    qs.loadData(TBL_TCD_TYPE_ARTICLES, new SqlSelect().setLimit(500000)
        .addField(TBL_TCD_TYPES, sys.getIdName(TBL_TCD_TYPES), COL_TCD_TYPE)
        .addFields(typArt, COL_TCD_ARTICLE)
        .addFrom(typArt)
        .addFromInner(TBL_TCD_TYPES,
            SqlUtils.join(typArt, TCD_TYPE_ID, TBL_TCD_TYPES, TCD_TECDOC_ID))
        .addOrder(TBL_TCD_TYPES, sys.getIdName(TBL_TCD_TYPES))
        .addOrder(typArt, COL_TCD_ARTICLE));

    qs.sqlDropTemp(typArt);

    // ---------------- TcdArticleCodes
    String tcdArticleCodes = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLE_CODES);

    tcdArticleCodes = qs.sqlCreateTemp(new SqlSelect()
        .addFields(art, COL_TCD_ARTICLE)
        .addFields(tcdArticleCodes,
            COL_TCD_SEARCH_NR, COL_TCD_CODE_NR, COL_TCD_BRAND_NAME, TCD_TECDOC_ID)
        .addExpr(SqlUtils.sqlCase(SqlUtils.field(tcdArticleCodes, COL_TCD_OE_CODE), 3,
            SqlUtils.constant(true), null), COL_TCD_OE_CODE)
        .addFrom(art)
        .addFromInner(tcdArticleCodes, SqlUtils.joinUsing(art, tcdArticleCodes, TCD_ARTICLE_ID)));

    importBrands(new SqlSelect()
        .setDistinctMode(true)
        .addFields(tcdArticleCodes, COL_TCD_BRAND_NAME)
        .addFrom(tcdArticleCodes));

    qs.loadData(TBL_TCD_ARTICLE_CODES, new SqlSelect().setLimit(100000)
        .addFields(tcdArticleCodes,
            COL_TCD_ARTICLE, COL_TCD_SEARCH_NR, COL_TCD_CODE_NR, COL_TCD_OE_CODE)
        .addField(TBL_TCD_BRANDS, sys.getIdName(TBL_TCD_BRANDS), COL_TCD_BRAND)
        .addFrom(tcdArticleCodes)
        .addFromInner(TBL_TCD_BRANDS,
            SqlUtils.joinUsing(tcdArticleCodes, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME))
        .addOrder(tcdArticleCodes, TCD_TECDOC_ID));

    qs.sqlDropTemp(tcdArticleCodes);

    // ---------------- TcdArticleGraphics, TcdGraphics, TcdResources...
    String tcdArticleGraphics = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLE_GRAPHICS);

    String artGraph = qs.sqlCreateTemp(new SqlSelect()
        .addFields(art, COL_TCD_ARTICLE)
        .addFields(tcdArticleGraphics, TCD_GRAPHICS_ID, COL_TCD_SORT)
        .addFrom(art)
        .addFromInner(tcdArticleGraphics,
            SqlUtils.joinUsing(art, tcdArticleGraphics, TCD_ARTICLE_ID)));

    qs.sqlIndex(artGraph, TCD_GRAPHICS_ID);
    String tcdGraphics = SqlUtils.table(TCD_SCHEMA, TBL_TCD_GRAPHICS);

    String graph = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tcdGraphics, COL_TCD_GRAPHICS_TYPE,
            TCD_GRAPHICS_RESOURCE_NO, TCD_GRAPHICS_RESOURCE_ID)
        .addField(tcdGraphics, TCD_GRAPHICS_ID, TCD_TECDOC_ID)
        .addFrom(tcdGraphics)
        .addFromInner(new SqlSelect().setDistinctMode(true)
            .addFields(artGraph, TCD_GRAPHICS_ID)
            .addFrom(artGraph), subq, SqlUtils.joinUsing(tcdGraphics, subq, TCD_GRAPHICS_ID))
        .addFromLeft(TBL_TCD_GRAPHICS, als,
            SqlUtils.join(tcdGraphics, TCD_GRAPHICS_ID, als, TCD_TECDOC_ID))
        .setWhere(SqlUtils.isNull(als, sys.getIdName(TBL_TCD_GRAPHICS))));

    qs.sqlIndex(graph, TCD_GRAPHICS_RESOURCE_NO, TCD_GRAPHICS_RESOURCE_ID);

    for (String part : qs.getColumn(new SqlSelect().setDistinctMode(true)
        .addFields(graph, TCD_GRAPHICS_RESOURCE_NO)
        .addFrom(graph)
        .addOrder(graph, TCD_GRAPHICS_RESOURCE_NO))) {

      String resource = SqlUtils.table(TCD_SCHEMA, TCD_GRAPHICS_RESOURCES + part);

      SqlSelect sql = new SqlSelect()
          .addFields(graph, COL_TCD_GRAPHICS_TYPE, TCD_TECDOC_ID)
          .addFields(resource, COL_TCD_GRAPHICS_RESOURCE)
          .addFrom(resource)
          .addFromInner(graph, SqlUtils.and(SqlUtils.equals(graph,
              TCD_GRAPHICS_RESOURCE_NO, part),
              SqlUtils.joinUsing(resource, graph, TCD_GRAPHICS_RESOURCE_ID)))
          .addOrder(resource, TCD_GRAPHICS_RESOURCE_ID);

      int tot = 0;
      int chunk = 100;
      int offset = 0;
      SimpleRowSet data = null;
      sql.setLimit(chunk);
      SqlInsert insert = new SqlInsert(TBL_TCD_GRAPHICS)
          .addFields(COL_TCD_GRAPHICS_TYPE, COL_TCD_GRAPHICS_RESOURCE, TCD_TECDOC_ID,
              sys.getIdName(TBL_TCD_GRAPHICS));

      do {
        data = qs.getData(sql.setOffset(offset));
        QueryServiceBean.debugOff();

        for (SimpleRow row : data) {
          String type = row.getValue(COL_TCD_GRAPHICS_TYPE);
          String image = row.getValue(COL_TCD_GRAPHICS_RESOURCE);

          if (JPEG2000.equals(type)) {
            if (supportsJPEG2000 == null) {
              ImageIO.scanForPlugins();
              supportsJPEG2000 = ArrayUtils.containsSame(ImageIO.getReaderFileSuffixes(), JPEG2000);
            }
            if (supportsJPEG2000) {
              try {
                ByteArrayInputStream in = new ByteArrayInputStream(Codec.fromBase64(image));
                BufferedImage img = ImageIO.read(in);

                if (img != null) {
                  ByteArrayOutputStream out = new ByteArrayOutputStream();

                  if (ImageIO.write(img, JPEG, out)) {
                    image = Codec.toBase64(out.toByteArray());
                    type = JPEG;
                  }
                  out.close();
                }
                in.close();
              } catch (IOException e) {
                logger.error(e);
              }
            }
          }
          insert.addValues(type, image, row.getLong(TCD_TECDOC_ID), ig.getId(TBL_TCD_GRAPHICS));

          if (++tot % chunk == 0) {
            qs.insertData(insert);
            insert.resetValues();
            logger.debug("Inserted", tot, "records into table", TBL_TCD_GRAPHICS);
          }
        }
        if (tot % chunk > 0) {
          qs.insertData(insert);
          logger.debug("Inserted", tot, "records into table", TBL_TCD_GRAPHICS);
        }
        QueryServiceBean.debugOn(isDebugEnabled);
        offset += chunk;
      } while (data.getNumberOfRows() == chunk);
    }
    qs.sqlDropTemp(graph);

    qs.loadData(TBL_TCD_ARTICLE_GRAPHICS, new SqlSelect().setLimit(500000)
        .addFields(artGraph, COL_TCD_ARTICLE, COL_TCD_SORT)
        .addField(TBL_TCD_GRAPHICS, sys.getIdName(TBL_TCD_GRAPHICS), COL_TCD_GRAPHICS)
        .addFrom(artGraph)
        .addFromInner(TBL_TCD_GRAPHICS,
            SqlUtils.join(artGraph, TCD_GRAPHICS_ID, TBL_TCD_GRAPHICS, TCD_TECDOC_ID))
        .addOrder(artGraph, COL_TCD_ARTICLE)
        .addOrder(TBL_TCD_GRAPHICS, sys.getIdName(TBL_TCD_GRAPHICS)));

    qs.sqlDropTemp(artGraph);
    qs.sqlDropTemp(art);
  }

  private void importBrands(SqlSelect brandsQuery) {
    String brandsAlias = SqlUtils.uniqueName();

    String[] brands = qs.getColumn(new SqlSelect()
        .setDistinctMode(true)
        .addFields(brandsAlias, COL_TCD_BRAND_NAME)
        .addFrom(brandsQuery, brandsAlias)
        .addFromLeft(TBL_TCD_BRANDS,
            SqlUtils.joinUsing(brandsAlias, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME))
        .setWhere(SqlUtils.and(SqlUtils.notNull(brandsAlias, COL_TCD_BRAND_NAME),
            SqlUtils.isNull(TBL_TCD_BRANDS, sys.getIdName(TBL_TCD_BRANDS)))));

    for (String brand : brands) {
      qs.insertData(new SqlInsert(TBL_TCD_BRANDS)
          .addConstant(COL_TCD_BRAND_NAME, brand));
    }
  }

  private void importItems(EcSupplier supplier, List<RemoteItem> data) {
    String log = supplier + " " + TBL_TCD_ARTICLE_SUPPLIERS + ":";

    if (BeeUtils.isEmpty(data)) {
      return;
    }
    String idName = SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addField(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR, COL_TCD_SEARCH_NR)
        .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR)
        .addFields(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER_ID, COL_TCD_COST)
        .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_DESCRIPTION)
        .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
        .addEmptyLong(idName)
        .addFrom(TBL_TCD_ARTICLES)
        .addFrom(TBL_TCD_BRANDS)
        .addFrom(TBL_TCD_ARTICLE_SUPPLIERS)
        .setWhere(SqlUtils.sqlFalse());

    Map<String, Object> fields = new LinkedHashMap<>();
    Map<String, String> priceLists = new LinkedHashMap<>();

    if (!BeeUtils.isEmpty(data.get(0).prices)) {
      SimpleRowSet rs = qs.getData(new SqlSelect()
          .addFields(TBL_TCD_PRICELISTS, COL_TCD_PRICELIST_NAME)
          .addField(TBL_TCD_PRICELISTS, sys.getIdName(TBL_TCD_PRICELISTS), COL_TCD_PRICELIST)
          .addFrom(TBL_TCD_PRICELISTS));

      for (String priceList : data.get(0).prices.keySet()) {
        Long id = BeeUtils.toLongOrNull(rs.getValueByKey(COL_TCD_PRICELIST_NAME, priceList,
            COL_TCD_PRICELIST));

        if (!DataUtils.isId(id)) {
          id = qs.insertData(new SqlInsert(TBL_TCD_PRICELISTS)
              .addConstant(COL_TCD_PRICELIST_NAME, priceList));
        }
        String fld = "prc" + id;
        priceLists.put(priceList, fld);
        query.addEmptyDouble(fld);
        fields.put(fld, null);
      }
    }
    String tmp = qs.sqlCreateTemp(query);

    for (String fld : new String[] {
        COL_TCD_SEARCH_NR, COL_TCD_ARTICLE_NR, COL_TCD_BRAND_NAME,
        COL_TCD_COST, COL_TCD_SUPPLIER_ID, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_DESCRIPTION}) {
      fields.put(fld, null);
    }
    SqlInsert insert = new SqlInsert(tmp).addFields(fields.keySet().toArray(new String[0]));
    int tot = 0;
    boolean isDebugEnabled = QueryServiceBean.debugOff();

    for (RemoteItem info : data) {
      String searchNr = EcUtils.normalizeCode(info.articleNr);

      if (!BeeUtils.isEmpty(searchNr)) {
        fields.put(COL_TCD_SEARCH_NR, searchNr);
        fields.put(COL_TCD_ARTICLE_NR, info.articleNr);
        fields.put(COL_TCD_BRAND_NAME, info.brand);
        fields.put(COL_TCD_COST, info.cost);
        fields.put(COL_TCD_SUPPLIER_ID, info.supplierId);
        fields.put(COL_TCD_ARTICLE_NAME,
            sys.clampValue(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, info.name));
        fields.put(COL_TCD_ARTICLE_DESCRIPTION, info.descr);

        for (Entry<String, String> entry : priceLists.entrySet()) {
          fields.put(entry.getValue(), info.prices.get(entry.getKey()));
        }
        insert.addValues(fields.values().toArray());
      }
      if (++tot % 1e4 == 0) {
        qs.insertData(insert);
        insert.resetValues();
        logger.debug(log, "Processed", tot, "records");
      }
    }
    if (tot % 1e4 > 0) {
      if (!insert.isEmpty()) {
        qs.insertData(insert);
      }
      logger.debug(log, "Processed", tot, "records");
    }
    QueryServiceBean.debugOn(isDebugEnabled);

    IsCondition join = SqlUtils.and(SqlUtils.joinUsing(tmp, TBL_TCD_ARTICLE_SUPPLIERS,
        COL_TCD_SUPPLIER_ID), SqlUtils.equals(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER,
        supplier.ordinal()));

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(idName, SqlUtils.field(TBL_TCD_ARTICLE_SUPPLIERS,
            sys.getIdName(TBL_TCD_ARTICLE_SUPPLIERS)))
        .setFrom(TBL_TCD_ARTICLE_SUPPLIERS, join));

    long time = System.currentTimeMillis();

    tot = qs.updateData(new SqlUpdate(TBL_TCD_ARTICLE_SUPPLIERS)
        .addExpression(COL_TCD_UPDATED_COST, SqlUtils.field(tmp, COL_TCD_COST))
        .addConstant(COL_TCD_UPDATE_TIME, time)
        .setFrom(tmp, join)
        .setWhere(SqlUtils.notEqual(
            SqlUtils.nvl(SqlUtils.field(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_UPDATED_COST), 0),
            SqlUtils.nvl(SqlUtils.field(tmp, COL_TCD_COST), 0))));

    logger.info(log, "Updated", tot, "records");

    String tmpPrices = null;

    if (!BeeUtils.isEmpty(priceLists)) {
      tmpPrices = qs.sqlCreateTemp(new SqlSelect()
          .addFields(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_ARTICLE)
          .addFields(tmp, priceLists.values().toArray(new String[0]))
          .addFrom(tmp)
          .addFromInner(TBL_TCD_ARTICLE_SUPPLIERS,
              sys.joinTables(TBL_TCD_ARTICLE_SUPPLIERS, tmp, idName)));
    }
    qs.updateData(new SqlDelete(tmp)
        .setWhere(SqlUtils.notNull(tmp, idName)));

    String tcdArticles = SqlUtils.table(TCD_SCHEMA, TBL_TCD_ARTICLES);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(idName, SqlUtils.field(tcdArticles, TCD_ARTICLE_ID))
        .setFrom(tcdArticles,
            SqlUtils.joinUsing(tmp, tcdArticles, COL_TCD_SEARCH_NR, COL_TCD_BRAND_NAME)));

    importArticles(qs.sqlCreateTemp(new SqlSelect()
        .addField(tmp, idName, TCD_ARTICLE_ID)
        .addEmptyLong(COL_TCD_ARTICLE)
        .addFrom(tmp)
        .addFromLeft(TBL_TCD_ARTICLES, SqlUtils.join(tmp, idName, TBL_TCD_ARTICLES, TCD_TECDOC_ID))
        .setWhere(SqlUtils.and(SqlUtils.notNull(tmp, idName),
            SqlUtils.isNull(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES))))
        .addGroup(tmp, idName)));

    String zz = qs.sqlCreateTemp(new SqlSelect()
        .addAllFields(tmp)
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(tmp)
        .addFromLeft(TBL_TCD_BRANDS, SqlUtils.joinUsing(tmp, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME))
        .addFromLeft(TBL_TCD_ARTICLES, SqlUtils.and(
            sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ARTICLES, COL_TCD_BRAND),
            SqlUtils.joinUsing(tmp, TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR))));

    qs.sqlDropTemp(tmp);
    tmp = zz;

    qs.loadData(TBL_TCD_ARTICLE_SUPPLIERS, new SqlSelect().setLimit(100000)
        .addFields(tmp, COL_TCD_ARTICLE, COL_TCD_COST, COL_TCD_SUPPLIER_ID)
        .addConstant(supplier.ordinal(), COL_TCD_SUPPLIER)
        .addField(tmp, COL_TCD_COST, COL_TCD_UPDATED_COST)
        .addConstant(time, COL_TCD_UPDATE_TIME)
        .addFrom(tmp)
        .setWhere(SqlUtils.notNull(tmp, COL_TCD_ARTICLE))
        .addOrder(tmp, COL_TCD_SUPPLIER_ID));

    if (!BeeUtils.isEmpty(priceLists)) {
      List<String> flds = new ArrayList<>();
      flds.add(COL_TCD_ARTICLE);
      flds.addAll(priceLists.values());

      qs.insertData(new SqlInsert(tmpPrices)
          .addFields(flds.toArray(new String[0]))
          .setDataSource(new SqlSelect()
              .addFields(tmp, flds.toArray(new String[0]))
              .addFrom(tmp)
              .setWhere(SqlUtils.notNull(tmp, COL_TCD_ARTICLE))));

      for (Entry<String, String> entry : priceLists.entrySet()) {
        String col = entry.getValue();
        Long id = Assert.notNull(BeeUtils.toLongOrNull(BeeUtils.removePrefix(col, "prc")));

        String tmpPr = qs.sqlCreateTemp(new SqlSelect()
            .addFields(tmpPrices, COL_TCD_ARTICLE)
            .addField(TBL_TCD_ARTICLE_PRICES, sys.getIdName(TBL_TCD_ARTICLE_PRICES),
                COL_TCD_PRICELIST)
            .addMax(tmpPrices, col, COL_TCD_PRICE)
            .addFrom(tmpPrices)
            .addFromLeft(TBL_TCD_ARTICLE_PRICES, SqlUtils.and(SqlUtils.joinUsing(tmpPrices,
                TBL_TCD_ARTICLE_PRICES, COL_TCD_ARTICLE),
                SqlUtils.equals(TBL_TCD_ARTICLE_PRICES, COL_TCD_PRICELIST, id)))
            .addGroup(tmpPrices, COL_TCD_ARTICLE)
            .addGroup(TBL_TCD_ARTICLE_PRICES, sys.getIdName(TBL_TCD_ARTICLE_PRICES)));

        int c = qs.updateData(new SqlUpdate(TBL_TCD_ARTICLE_PRICES)
            .addExpression(COL_TCD_PRICE, SqlUtils.field(tmpPr, COL_TCD_PRICE))
            .setFrom(tmpPr, sys.joinTables(TBL_TCD_ARTICLE_PRICES, tmpPr, COL_TCD_PRICELIST)));

        logger.info(entry.getKey(), "- Updated", c, "records");

        qs.loadData(TBL_TCD_ARTICLE_PRICES, new SqlSelect().setLimit(100000)
            .addFields(tmpPr, COL_TCD_ARTICLE, COL_TCD_PRICE)
            .addConstant(id, COL_TCD_PRICELIST)
            .addFrom(tmpPr)
            .setWhere(SqlUtils.and(SqlUtils.isNull(tmpPr, COL_TCD_PRICELIST),
                SqlUtils.notNull(tmpPr, COL_TCD_PRICE)))
            .addOrder(tmpPr, COL_TCD_ARTICLE));

        qs.sqlDropTemp(tmpPr);
      }
      qs.sqlDropTemp(tmpPrices);
    }
    qs.updateData(new SqlDelete(tmp)
        .setWhere(SqlUtils.notNull(tmp, COL_TCD_ARTICLE)));

    qs.updateData(new SqlDelete(TBL_TCD_ORPHANS)
        .setWhere(SqlUtils.and(SqlUtils.not(SqlUtils.in(TBL_TCD_ORPHANS, COL_TCD_SUPPLIER_ID, tmp,
            COL_TCD_SUPPLIER_ID)),
            SqlUtils.equals(TBL_TCD_ORPHANS, COL_TCD_SUPPLIER, supplier.ordinal()))));

    importBrands(new SqlSelect()
        .setDistinctMode(true)
        .addFields(tmp, COL_TCD_BRAND_NAME)
        .addFrom(tmp));

    qs.loadData(TBL_TCD_ORPHANS, new SqlSelect()
        .addFields(tmp, COL_TCD_ARTICLE_NR, COL_TCD_SUPPLIER_ID, COL_TCD_ARTICLE_NAME,
            COL_TCD_ARTICLE_DESCRIPTION)
        .addField(TBL_TCD_BRANDS, sys.getIdName(TBL_TCD_BRANDS), COL_TCD_BRAND)
        .addConstant(supplier.ordinal(), COL_TCD_SUPPLIER)
        .addFrom(tmp)
        .addFromLeft(TBL_TCD_BRANDS, SqlUtils.joinUsing(tmp, TBL_TCD_BRANDS, COL_TCD_BRAND_NAME))
        .addFromLeft(TBL_TCD_ORPHANS,
            SqlUtils.and(SqlUtils.joinUsing(tmp, TBL_TCD_ORPHANS, COL_TCD_SUPPLIER_ID),
                SqlUtils.equals(TBL_TCD_ORPHANS, COL_TCD_SUPPLIER, supplier.ordinal())))
        .setWhere(SqlUtils.isNull(TBL_TCD_ORPHANS, sys.getIdName(TBL_TCD_ORPHANS))));

    qs.sqlDropTemp(tmp);
  }

  private void importRemainders(EcSupplier supplier, List<RemoteRemainder> data) {
    String log = supplier + " " + TBL_TCD_REMAINDERS + ":";

    String idName = sys.getIdName(TBL_TCD_REMAINDERS);
    String supplierWarehouse = COL_WAREHOUSE_SUPPLIER_CODE;

    String rem = qs.sqlCreateTemp(new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER_ID)
        .addFields(TBL_TCD_REMAINDERS, idName)
        .addFields(TBL_WAREHOUSES, supplierWarehouse)
        .addFrom(TBL_TCD_REMAINDERS)
        .addFromInner(TBL_TCD_ARTICLE_SUPPLIERS,
            SqlUtils.and(SqlUtils.equals(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER,
                supplier.ordinal()),
                sys.joinTables(TBL_TCD_ARTICLE_SUPPLIERS, TBL_TCD_REMAINDERS,
                    COL_TCD_ARTICLE_SUPPLIER)))
        .addFromInner(TBL_WAREHOUSES,
            SqlUtils.and(sys.joinTables(TBL_WAREHOUSES, TBL_TCD_REMAINDERS, COL_WAREHOUSE),
                SqlUtils.notNull(TBL_WAREHOUSES, supplierWarehouse))));

    qs.updateData(new SqlUpdate(TBL_TCD_REMAINDERS)
        .addConstant(COL_TCD_REMAINDER, null)
        .setFrom(rem, sys.joinTables(TBL_TCD_REMAINDERS, rem, idName)));

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER_ID)
        .addFields(TBL_TCD_REMAINDERS, COL_TCD_REMAINDER, idName)
        .addFields(TBL_WAREHOUSES, supplierWarehouse)
        .addFrom(TBL_TCD_ARTICLE_SUPPLIERS)
        .addFrom(TBL_TCD_REMAINDERS)
        .addFrom(TBL_WAREHOUSES)
        .setWhere(SqlUtils.sqlFalse()));

    boolean isDebugEnabled = QueryServiceBean.debugOff();

    SqlInsert insert = new SqlInsert(tmp)
        .addFields(COL_TCD_SUPPLIER_ID, supplierWarehouse, COL_TCD_REMAINDER);
    int tot = 0;

    for (RemoteRemainder info : data) {
      insert.addValues(info.supplierId, info.warehouse, info.remainder);

      if (++tot % 1e4 == 0) {
        qs.insertData(insert);
        insert.resetValues();
        logger.debug(log, "Processed", tot, "records");
      }
    }
    if (tot % 1e4 > 0) {
      if (!insert.isEmpty()) {
        qs.insertData(insert);
      }
      logger.debug(log, "Processed", tot, "records");
    }
    QueryServiceBean.debugOn(isDebugEnabled);

    qs.sqlIndex(rem, COL_TCD_SUPPLIER_ID, supplierWarehouse);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(idName, SqlUtils.field(rem, idName))
        .setFrom(rem, SqlUtils.joinUsing(tmp, rem, COL_TCD_SUPPLIER_ID, supplierWarehouse)));

    qs.sqlIndex(tmp, idName);

    tot = qs.updateData(new SqlUpdate(TBL_TCD_REMAINDERS)
        .addExpression(COL_TCD_REMAINDER, SqlUtils.field(tmp, COL_TCD_REMAINDER))
        .setFrom(tmp, sys.joinTables(TBL_TCD_REMAINDERS, tmp, idName)));

    qs.sqlDropTemp(rem);

    logger.info(log, "Updated", tot, "records");

    String zz = qs.sqlCreateTemp(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.isNull(tmp, idName)));

    qs.sqlDropTemp(tmp);
    tmp = zz;

    for (String warehouse : qs.getColumn(new SqlSelect().setDistinctMode(true)
        .addFields(tmp, supplierWarehouse)
        .addFrom(tmp)
        .setWhere(SqlUtils.not(SqlUtils.in(tmp, supplierWarehouse, TBL_WAREHOUSES,
            supplierWarehouse, SqlUtils.notNull(TBL_WAREHOUSES,
                supplierWarehouse)))))) {

      qs.insertData(new SqlInsert(TBL_WAREHOUSES)
          .addConstant(COL_WAREHOUSE_CODE, warehouse)
          .addConstant(supplierWarehouse, warehouse));
    }

    qs.sqlIndex(tmp, COL_TCD_SUPPLIER_ID, supplierWarehouse);

    qs.loadData(TBL_TCD_REMAINDERS, new SqlSelect().setLimit(500000)
        .addField(TBL_TCD_ARTICLE_SUPPLIERS, sys.getIdName(TBL_TCD_ARTICLE_SUPPLIERS),
            COL_TCD_ARTICLE_SUPPLIER)
        .addField(TBL_WAREHOUSES, sys.getIdName(TBL_WAREHOUSES),
            COL_WAREHOUSE)
        .addFields(tmp, COL_TCD_REMAINDER)
        .addFrom(tmp)
        .addFromInner(TBL_TCD_ARTICLE_SUPPLIERS,
            SqlUtils.and(SqlUtils.joinUsing(tmp, TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER_ID),
                SqlUtils.equals(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_SUPPLIER, supplier.ordinal())))
        .addFromInner(TBL_WAREHOUSES, SqlUtils.joinUsing(tmp, TBL_WAREHOUSES, supplierWarehouse))
        .addOrder(TBL_TCD_ARTICLE_SUPPLIERS, sys.getIdName(TBL_TCD_ARTICLE_SUPPLIERS))
        .addOrder(tmp, supplierWarehouse));

    qs.sqlDropTemp(tmp);
  }
}
