package com.butent.bee.server.modules.ec;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.BeeTable.BeeForeignKey;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.ExtendedUsageQueryProvider;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.ui.UiHolderBean;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.SelectableValue;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.Text;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.html.builder.elements.Tr;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.ec.ArticleCriteria;
import com.butent.bee.shared.modules.ec.ArticleSupplier;
import com.butent.bee.shared.modules.ec.Cart;
import com.butent.bee.shared.modules.ec.CartItem;
import com.butent.bee.shared.modules.ec.DeliveryMethod;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcCarModel;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcCriterion;
import com.butent.bee.shared.modules.ec.EcFinInfo;
import com.butent.bee.shared.modules.ec.EcGroup;
import com.butent.bee.shared.modules.ec.EcGroupFilters;
import com.butent.bee.shared.modules.ec.EcInvoice;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcItemInfo;
import com.butent.bee.shared.modules.ec.EcOrder;
import com.butent.bee.shared.modules.ec.EcOrderEvent;
import com.butent.bee.shared.modules.ec.EcOrderItem;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.webservice.ButentWS;
import com.butent.webservice.WSDocument;
import com.butent.webservice.WSDocument.WSDocumentItem;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class EcModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(EcModuleBean.class);

  private static IsCondition articleCondition =
      SqlUtils.notNull(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_VISIBLE);

  private static IsCondition oeNumberCondition =
      SqlUtils.notNull(TBL_TCD_ARTICLE_CODES, COL_TCD_OE_CODE);

  private static IsCondition getCodeCondition(String code, Operator defOperator) {
    if (BeeUtils.isEmpty(code)) {
      return null;
    }
    Operator operator;
    String value;

    if (code.contains(Operator.CHAR_ANY) || code.contains(Operator.CHAR_ONE)) {
      operator = Operator.MATCHES;
      value = code.trim().toUpperCase();

    } else if (BeeUtils.isPrefixOrSuffix(code, BeeConst.CHAR_EQ)) {
      operator = Operator.EQ;
      value = BeeUtils.removePrefixAndSuffix(code, BeeConst.CHAR_EQ).trim().toUpperCase();

    } else {
      operator = BeeUtils.nvl(defOperator, Operator.CONTAINS);
      value = EcUtils.normalizeCode(code);

      if (operator == Operator.STARTS) {
        value += Operator.CHAR_ANY;
        operator = Operator.MATCHES;
      }
      if (BeeUtils.length(value) < MIN_SEARCH_QUERY_LENGTH) {
        return null;
      }
    }

    return SqlUtils.compare(TBL_TCD_ARTICLE_CODES, COL_TCD_SEARCH_NR, operator, value);
  }

  private static Double getMarginPercent(Collection<Long> categories, Map<Long, Long> parents,
      Map<Long, Long> roots, Map<Long, Double> margins) {

    if (categories.isEmpty() || margins.isEmpty()) {
      return null;
    }

    Map<Long, Double> marginByRoot = new HashMap<>();
    Set<Long> traversed = new HashSet<>();

    for (Long category : categories) {
      Double margin = null;

      for (Long id = category; id != null && !traversed.contains(id); id = parents.get(id)) {
        traversed.add(id);
        if (margin == null) {
          margin = margins.get(id);
        }
      }

      if (margin != null) {
        marginByRoot.put(roots.get(category), margin);
      }
    }

    if (marginByRoot.isEmpty()) {
      return null;
    } else {
      Double result = null;
      for (double margin : marginByRoot.values()) {
        if (result == null || margin < result) {
          result = margin;
        }
      }
      return result;
    }
  }

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;
  @EJB
  ParamHolderBean prm;
  @EJB
  TecDocBean tcd;

  @EJB
  MailModuleBean mail;
  @EJB
  NewsBean news;
  @EJB
  UiHolderBean ui;

  @Asynchronous
  public void adoptOrphans(String progressId, IsCondition clause) {
    tcd.adoptOrphans(progressId, qs.getData(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TCD_ORPHANS, COL_TCD_ARTICLE_NR)
        .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
        .addFrom(TBL_TCD_ORPHANS)
        .addFromInner(TBL_TCD_BRANDS,
            sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ORPHANS, COL_TCD_BRAND))
        .setWhere(clause)));

    EcSupplier supplier = EcSupplier.EOLTAS;
    String orphanId = sys.getIdName(TBL_TCD_ORPHANS);
    SimpleRowSet rows = null;

    SimpleRowSet orphans = qs.getData(new SqlSelect()
        .addFields(TBL_TCD_ORPHANS, orphanId, COL_TCD_SUPPLIER_ID, COL_TCD_BRAND,
            COL_TCD_ARTICLE_NR, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_DESCRIPTION)
        .addFrom(TBL_TCD_ORPHANS)
        .setWhere(SqlUtils.and(clause,
            SqlUtils.equals(TBL_TCD_ORPHANS, COL_TCD_SUPPLIER, supplier.ordinal()))));

    boolean ok = !orphans.isEmpty() && (BeeUtils.isEmpty(progressId)
        || Endpoint.updateProgress(progressId, 1));

    if (ok) {
      String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
      String remoteLogin = prm.getText(PRM_ERP_LOGIN);
      String remotePassword = prm.getText(PRM_ERP_PASSWORD);

      try {
        rows = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
            .getSQLData("SELECT preke AS pr, prekes.grupe AS gr, savikaina AS sv"
                    + " FROM prekes"
                    + " INNER JOIN grupes"
                    + " ON prekes.grupe = grupes.grupe"
                    + " WHERE prekes.gam_art IS NOT NULL AND prekes.gam_art != ''"
                    + " AND prekes.gamintojas IS NOT NULL AND prekes.gamintojas != ''"
                    + " AND grupes.pos_mode = 'E'",
                "pr", "gr", "sv");

        ok = !rows.isEmpty();

      } catch (BeeException e) {
        logger.error(e);
        ok = false;
      }
    }
    if (ok) {
      Map<String, Long> categories = new HashMap<>();
      int c = 0;

      for (SimpleRow orphan : orphans) {
        String supplierId = orphan.getValue(COL_TCD_SUPPLIER_ID);
        String categoryName = rows.getValueByKey("pr", supplierId, "gr");

        if (!BeeUtils.isEmpty(categoryName)) {
          if (!categories.containsKey(categoryName)) {
            Long[] category = qs.getLongColumn(new SqlSelect()
                .addField(TBL_TCD_CATEGORIES, sys.getIdName(TBL_TCD_CATEGORIES), COL_TCD_CATEGORY)
                .addFrom(TBL_TCD_CATEGORIES)
                .setWhere(SqlUtils.equals(TBL_TCD_CATEGORIES, COL_TCD_CATEGORY_NAME, categoryName))
                .addOrder(TBL_TCD_CATEGORIES, COL_TCD_CATEGORY_PARENT));

            if (category.length > 0) {
              categories.put(categoryName, category[0]);
            } else {
              categories.put(categoryName, qs.insertData(new SqlInsert(TBL_TCD_CATEGORIES)
                  .addConstant(COL_TCD_CATEGORY_NAME, categoryName)));
            }
          }
          String artNr = orphan.getValue(COL_TCD_ARTICLE_NR);
          String brand = orphan.getValue(COL_TCD_BRAND);

          long newArt = qs.insertData(new SqlInsert(TBL_TCD_ARTICLES)
              .addConstant(COL_TCD_ARTICLE_NR, artNr)
              .addConstant(COL_TCD_BRAND, brand)
              .addConstant(COL_TCD_ARTICLE_NAME, orphan.getValue(COL_TCD_ARTICLE_NAME))
              .addConstant(COL_TCD_ARTICLE_DESCRIPTION,
                  orphan.getValue(COL_TCD_ARTICLE_DESCRIPTION))
              .addConstant(COL_TCD_ARTICLE_VISIBLE, true));

          qs.insertData(new SqlInsert(TBL_TCD_ARTICLE_SUPPLIERS)
              .addConstant(COL_TCD_ARTICLE, newArt)
              .addConstant(COL_TCD_SUPPLIER, supplier.ordinal())
              .addConstant(COL_TCD_SUPPLIER_ID, supplierId)
              .addConstant(COL_TCD_COST, rows.getValueByKey("pr", supplierId, "sv")));

          qs.insertData(new SqlInsert(TBL_TCD_ARTICLE_CATEGORIES)
              .addConstant(COL_TCD_ARTICLE, newArt)
              .addConstant(COL_TCD_CATEGORY, categories.get(categoryName)));

          qs.insertData(new SqlInsert(TBL_TCD_ARTICLE_CODES)
              .addConstant(COL_TCD_ARTICLE, newArt)
              .addConstant(COL_TCD_SEARCH_NR, EcUtils.normalizeCode(artNr))
              .addConstant(COL_TCD_CODE_NR, artNr)
              .addConstant(COL_TCD_BRAND, brand));

          qs.updateData(new SqlDelete(TBL_TCD_ORPHANS)
              .setWhere(sys.idEquals(TBL_TCD_ORPHANS, orphan.getLong(orphanId))));
        }
        if (!BeeUtils.isEmpty(progressId)
            && !Endpoint.updateProgress(progressId,
            1 - (++c / (double) orphans.getNumberOfRows()))) {
          break;
        }
      }
    }
    if (!BeeUtils.isEmpty(progressId)) {
      Endpoint.closeProgress(progressId);
    }
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    long startMillis = System.currentTimeMillis();

    ResponseObject response = null;

    String query = null;
    Long article = null;

    boolean log = false;

    if (BeeUtils.same(svc, SVC_GET_PROMO)) {
      response = getPromo(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CATEGORIES)) {
      response = getCategories();

    } else if (BeeUtils.same(svc, SVC_GLOBAL_SEARCH)) {
      query = reqInfo.getParameter(VAR_QUERY);
      response = doGlobalSearch(query);
      log = true;

    } else if (BeeUtils.same(svc, SVC_SEARCH_BY_ITEM_CODE)) {
      query = reqInfo.getParameter(VAR_QUERY);
      response = searchByItemCode(query, null, Operator.STARTS);
      log = true;

    } else if (BeeUtils.same(svc, SVC_SEARCH_BY_OE_NUMBER)) {
      query = reqInfo.getParameter(VAR_QUERY);
      response = searchByOeNumber(query);
      log = true;

    } else if (BeeUtils.same(svc, SVC_GET_CAR_MANUFACTURERS)) {
      response = getCarManufacturers();

    } else if (BeeUtils.same(svc, SVC_GET_CAR_MODELS)) {
      query = reqInfo.getParameter(VAR_MANUFACTURER);
      response = getCarModels(query);

    } else if (BeeUtils.same(svc, SVC_GET_CAR_TYPES)) {
      query = reqInfo.getParameter(VAR_MODEL);
      response = getCarTypes(BeeUtils.toLongOrNull(query));

    } else if (BeeUtils.same(svc, SVC_GET_ITEMS_BY_CAR_TYPE)) {
      query = reqInfo.getParameter(VAR_TYPE);
      response = getItemsByCarType(BeeUtils.toLongOrNull(query));
      log = true;

    } else if (BeeUtils.same(svc, SVC_GET_CAR_TYPE_HISTORY)) {
      response = getCarTypeHistory();

    } else if (BeeUtils.same(svc, SVC_GET_ITEM_BRANDS)) {
      response = getItemBrands();

    } else if (BeeUtils.same(svc, SVC_GET_ITEMS_BY_BRAND)) {
      query = reqInfo.getParameter(COL_TCD_BRAND);
      response = getItemsByBrand(BeeUtils.toLongOrNull(query));
      log = true;

    } else if (BeeUtils.same(svc, SVC_GET_DELIVERY_METHODS)) {
      response = getDeliveryMethods();

    } else if (BeeUtils.same(svc, SVC_SUBMIT_ORDER)) {
      response = submitOrder(reqInfo);

    } else if (BeeUtils.same(svc, SVC_SEND_TO_ERP)) {
      response = sendToERP(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CONFIGURATION)) {
      response = getConfiguration();
    } else if (BeeUtils.same(svc, SVC_SAVE_CONFIGURATION)) {
      response = saveConfiguration(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CLEAR_CONFIGURATION)) {
      response = clearConfiguration(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_ITEM_ANALOGS)) {
      response = getItemAnalogs(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_ITEM_INFO)) {
      article = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_ARTICLE));
      response = getItemInfo(article);
      log = true;

    } else if (BeeUtils.same(svc, SVC_GET_PICTURES)) {
      Set<Long> articles = DataUtils.parseIdSet(reqInfo.getParameter(COL_TCD_ARTICLE));
      response = getPictures(articles);

    } else if (BeeUtils.same(svc, SVC_UPDATE_COSTS)) {
      Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(COL_TCD_ARTICLE));
      response = updateCosts(ids);

    } else if (BeeUtils.same(svc, SVC_MERGE_CATEGORY)) {
      response = mergeCategory(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_CATEGORY)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_CATEGORY_PARENT)));

    } else if (BeeUtils.same(svc, SVC_GET_SHOPPING_CARTS)) {
      response = getShoppingCarts();
    } else if (BeeUtils.same(svc, SVC_UPDATE_SHOPPING_CART)) {
      response = updateShoppingCart(reqInfo);

    } else if (BeeUtils.same(svc, SVC_FINANCIAL_INFORMATION)) {
      response = getFinancialInformation(getCurrentClientId());

    } else if (BeeUtils.same(svc, SVC_GET_ITEM_GROUPS)) {
      boolean moto = reqInfo.hasParameter(COL_GROUP_MOTO);
      response = getItemGroups(moto);

    } else if (BeeUtils.same(svc, SVC_GET_GROUP_FILTERS)) {
      query = reqInfo.getParameter(COL_GROUP);
      response = getGroupFilters(BeeUtils.toLongOrNull(query));

    } else if (BeeUtils.same(svc, SVC_GET_GROUP_ITEMS)) {
      query = reqInfo.getParameter(COL_GROUP);
      response = getGroupItems(reqInfo, BeeUtils.toLongOrNull(query));
      log = true;

    } else if (BeeUtils.same(svc, SVC_GET_CLIENT_INFO)) {
      response = getClientInfo();
    } else if (BeeUtils.same(svc, SVC_GET_CLIENT_STOCK_LABELS)) {
      response = getClientStockLabels();

    } else if (BeeUtils.same(svc, SVC_ADD_TO_UNSUPPLIED_ITEMS)) {
      Long orderId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_ORDER));
      response = addToUnsuppliedItems(orderId);

    } else if (BeeUtils.same(svc, SVC_MAIL_ORDER)) {
      Long orderId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_ORDER));
      response = mailOrder(orderId, false);

    } else if (BeeUtils.same(svc, SVC_REGISTER_ORDER_EVENT)) {
      response = registerOrderEvent(reqInfo);

    } else if (BeeUtils.same(svc, SVC_UPLOAD_GRAPHICS)) {
      response = uploadGraphics(reqInfo);
    } else if (BeeUtils.same(svc, SVC_UPLOAD_BANNERS)) {
      response = uploadBanners(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_CLIENT)) {
      response = createClient(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_ITEM)) {
      response = createItem(reqInfo);

    } else if (BeeUtils.same(svc, SVC_ADOPT_ORPHANS)) {
      IsCondition clause = null;
      Filter filter = Filter.restore(reqInfo.getParameter("filter"));

      if (filter != null) {
        clause = SqlUtils.in(TBL_TCD_ORPHANS, sys.getIdName(TBL_TCD_ORPHANS),
            sys.getView(TBL_TCD_ORPHANS).getQuery(usr.getCurrentUserId(), filter)
                .resetFields()
                .addFields(TBL_TCD_ORPHANS, sys.getIdName(TBL_TCD_ORPHANS)));
      }
      Invocation.locateRemoteBean(EcModuleBean.class)
          .adoptOrphans(reqInfo.getParameter(Service.VAR_PROGRESS), clause);

      response = ResponseObject.emptyResponse();

    } else if (BeeUtils.same(svc, SVC_ADD_ARTICLE_CAR_TYPES)) {
      response = addArticleCarTypes(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("e-commerce service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    if (log && response != null && !response.hasErrors()) {
      logHistory(svc, query, article, response.getSize(),
          System.currentTimeMillis() - startMillis);
    }

    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> parameters = Lists.newArrayList(
        BeeParameter.createBoolean(module, PRM_PROMO_FEATURED, false, true),
        BeeParameter.createBoolean(module, PRM_PROMO_NOVELTY, false, true));

    for (EcSupplier supplier : EcSupplier.values()) {
      String name = supplier.getBasePriceListParameterName();
      if (!BeeUtils.isEmpty(name)) {
        parameters.add(BeeParameter.createRelation(module, name,
            TBL_TCD_PRICELISTS, COL_TCD_PRICELIST_NAME));
      }

      name = supplier.getClientPriceListParameterName();
      if (!BeeUtils.isEmpty(name)) {
        parameters.add(BeeParameter.createRelation(module, name,
            TBL_TCD_PRICELISTS, COL_TCD_PRICELIST_NAME));
      }
    }

    parameters.addAll(tcd.getDefaultParameters());

    return parameters;
  }

  @Override
  public Module getModule() {
    return Module.ECOMMERCE;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    tcd.initTimers();

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void orderCategories(ViewQueryEvent event) {
        if (event.isAfter(VIEW_CATEGORIES)) {
          BeeRowSet rowSet = event.getRowset();

          if (rowSet.getNumberOfRows() > 1) {
            final int nameIndex = rowSet.getColumnIndex(COL_TCD_CATEGORY_NAME);
            final int parentIndex = rowSet.getColumnIndex(COL_TCD_CATEGORY_PARENT);
            final int fullNameIndex = rowSet.getColumnIndex(COL_TCD_CATEGORY_FULL_NAME);

            if (BeeConst.isUndef(nameIndex) || BeeConst.isUndef(parentIndex)
                || BeeConst.isUndef(fullNameIndex)) {
              return;
            }

            Map<Long, Long> parents = new HashMap<>();
            Map<Long, String> names = new HashMap<>();

            for (BeeRow row : rowSet.getRows()) {
              Long parent = row.getLong(parentIndex);
              if (parent != null) {
                parents.put(row.getId(), parent);
              }

              names.put(row.getId(), row.getString(nameIndex));
            }

            for (BeeRow row : rowSet.getRows()) {
              Long parent = row.getLong(parentIndex);

              if (parent == null) {
                row.setValue(fullNameIndex, row.getString(nameIndex));

              } else {
                List<String> fullName = Lists.newArrayList(row.getString(nameIndex),
                    row.getString(fullNameIndex));

                while (parent != null && parents.containsKey(parent)) {
                  parent = parents.get(parent);
                  fullName.add(names.get(parent));
                }

                StringBuilder sb = new StringBuilder();
                for (int i = fullName.size() - 1; i >= 0; i--) {
                  sb.append(fullName.get(i));
                  if (i > 0) {
                    sb.append(CATEGORY_NAME_SEPARATOR);
                  }
                }
                row.setValue(fullNameIndex, sb.toString());
              }
            }

            final Collator collator = Collator.getInstance(usr.getLocale());
            collator.setStrength(Collator.IDENTICAL);

            Collections.sort(rowSet.getRows(), new Comparator<BeeRow>() {
              @Override
              public int compare(BeeRow row1, BeeRow row2) {
                String name1 = row1.getString(fullNameIndex);
                String name2 = row2.getString(fullNameIndex);

                int result;
                if (name1 == null) {
                  result = (name2 == null) ? BeeConst.COMPARE_EQUAL : BeeConst.COMPARE_LESS;
                } else if (name2 == null) {
                  result = BeeConst.COMPARE_MORE;
                } else {
                  result = collator.compare(name1.toLowerCase(), name2.toLowerCase());
                }

                if (result == BeeConst.COMPARE_EQUAL) {
                  result = Long.compare(row1.getId(), row2.getId());
                }

                return result;
              }
            });
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setSuppliersAndRemainders(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ARTICLES, VIEW_ORDER_ITEMS) && event.hasData()) {
          Set<Long> articleIds = new HashSet<>();

          BeeRowSet rowSet = event.getRowset();

          int index;
          if (event.isTarget(VIEW_ARTICLES)) {
            index = DataUtils.ID_INDEX;
          } else if (event.isTarget(VIEW_ORDER_ITEMS)) {
            index = rowSet.getColumnIndex(COL_ORDER_ITEM_ARTICLE);
          } else {
            index = BeeConst.UNDEF;
          }

          if (BeeConst.isUndef(index)) {
            return;
          }

          for (BeeRow row : rowSet.getRows()) {
            if (index == DataUtils.ID_INDEX) {
              articleIds.add(row.getId());
            } else {
              Long article = row.getLong(index);
              if (article != null) {
                articleIds.add(article);
              }
            }
          }

          if (articleIds.isEmpty()) {
            return;
          }

          Multimap<Long, ArticleSupplier> articleSuppliers =
              getArticleSuppliers(null, articleIds, null, null);

          if (articleSuppliers.isEmpty()) {
            return;
          }

          for (BeeRow row : rowSet.getRows()) {
            Long article = (index == DataUtils.ID_INDEX) ? (Long) row.getId() : row.getLong(index);

            if (article != null && articleSuppliers.containsKey(article)) {
              for (ArticleSupplier articleSupplier : articleSuppliers.get(article)) {
                String supplierSuffix = articleSupplier.getSupplier().getShortName();

                String supplierId = articleSupplier.getSupplierId();
                if (!BeeUtils.isEmpty(supplierId)) {
                  row.setProperty(COL_TCD_SUPPLIER_ID + supplierSuffix, supplierId);
                }

                double cost = articleSupplier.getRealCost();
                if (BeeUtils.isPositive(cost)) {
                  row.setProperty(COL_TCD_COST + supplierSuffix, BeeUtils.toString(cost));
                }

                Map<String, String> remainders = articleSupplier.getRemainders();
                if (!BeeUtils.isEmpty(remainders)) {
                  for (Map.Entry<String, String> entry : remainders.entrySet()) {
                    row.setProperty(COL_TCD_REMAINDER + entry.getKey(), entry.getValue());
                  }
                }
              }
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.EC_CLIENTS_MY, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.equals(TBL_MANAGERS, COL_MANAGER_USER, userId));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_CLIENTS, news.joinUsage(TBL_CLIENTS),
            TBL_MANAGERS, sys.joinTables(TBL_MANAGERS, TBL_CLIENTS, COL_CLIENT_MANAGER));
      }
    });

    news.registerUsageQueryProvider(Feed.EC_ORDERS_MY, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.equals(TBL_MANAGERS, COL_MANAGER_USER, userId));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_ORDERS, news.joinUsage(TBL_ORDERS),
            TBL_MANAGERS, sys.joinTables(TBL_MANAGERS, TBL_ORDERS, COL_ORDER_MANAGER));
      }
    });

    MenuService.ENSURE_CATEGORIES_AND_OPEN_GRID.setDataNameProvider(ui.getGridDataNameProvider());
  }

  private ResponseObject addArticleCarTypes(RequestInfo reqInfo) {
    Long article = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_ARTICLE));
    if (!DataUtils.isId(article)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TCD_ARTICLE);
    }

    List<Long> types = DataUtils.parseIdList(reqInfo.getParameter(COL_TCD_TYPE));
    if (BeeUtils.isEmpty(types)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TCD_TYPE);
    }

    for (Long type : types) {
      ResponseObject response = qs.insertDataWithResponse(new SqlInsert(TBL_TCD_TYPE_ARTICLES)
          .addConstant(COL_TCD_TYPE, type)
          .addConstant(COL_TCD_ARTICLE, article));

      if (response.hasErrors()) {
        return response;
      }
    }

    return ResponseObject.response(types.size());
  }

  private ResponseObject addToUnsuppliedItems(Long orderId) {
    if (!DataUtils.isId(orderId)) {
      return ResponseObject.parameterNotFound(SVC_ADD_TO_UNSUPPLIED_ITEMS, VAR_ORDER);
    }

    int itemsAdded = 0;

    SqlSelect orderQuery =
        new SqlSelect()
            .addFields(TBL_ORDERS, COL_ORDER_DATE, COL_ORDER_CLIENT)
            .addFields(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ARTICLE, COL_ORDER_ITEM_QUANTITY_ORDERED,
                COL_ORDER_ITEM_QUANTITY_SUBMIT, COL_ORDER_ITEM_PRICE, COL_ORDER_ITEM_NOTE)
            .addFrom(TBL_ORDER_ITEMS)
            .addFromInner(TBL_ORDERS,
                sys.joinTables(TBL_ORDERS, TBL_ORDER_ITEMS, COL_ORDER_ITEM_ORDER))
            .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ORDER, orderId),
                SqlUtils.positive(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_ORDERED),
                SqlUtils.or(SqlUtils.isNull(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_SUBMIT),
                    SqlUtils.more(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_ORDERED,
                        SqlUtils.field(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_SUBMIT)))));

    SimpleRowSet orderData = qs.getData(orderQuery);

    if (!DataUtils.isEmpty(orderData)) {
      for (SimpleRow row : orderData) {
        int quantity = BeeUtils.unbox(row.getInt(COL_ORDER_ITEM_QUANTITY_ORDERED))
            - BeeUtils.unbox(row.getInt(COL_ORDER_ITEM_QUANTITY_SUBMIT));
        if (quantity > 0) {
          SqlInsert insItem = new SqlInsert(TBL_UNSUPPLIED_ITEMS);

          insItem.addConstant(COL_UNSUPPLIED_ITEM_CLIENT, row.getLong(COL_ORDER_CLIENT));
          insItem.addConstant(COL_UNSUPPLIED_ITEM_DATE, row.getLong(COL_ORDER_DATE));
          insItem.addConstant(COL_UNSUPPLIED_ITEM_ORDER, orderId);

          insItem.addConstant(COL_UNSUPPLIED_ITEM_ARTICLE, row.getLong(COL_ORDER_ITEM_ARTICLE));
          insItem.addConstant(COL_UNSUPPLIED_ITEM_QUANTITY, quantity);
          insItem.addConstant(COL_UNSUPPLIED_ITEM_PRICE, row.getDouble(COL_ORDER_ITEM_PRICE));

          String note = row.getValue(COL_ORDER_ITEM_NOTE);
          if (!BeeUtils.isEmpty(note)) {
            insItem.addConstant(COL_UNSUPPLIED_ITEM_NOTE, note);
          }

          ResponseObject itemResponse = qs.insertDataWithResponse(insItem);
          if (itemResponse.hasErrors()) {
            return itemResponse;
          }

          itemsAdded++;
        }
      }
    }

    return ResponseObject.response(itemsAdded).setSize(itemsAdded);
  }

  private ResponseObject clearConfiguration(RequestInfo reqInfo) {
    String column = reqInfo.getParameter(Service.VAR_COLUMN);
    if (BeeUtils.isEmpty(column)) {
      return ResponseObject.parameterNotFound(SVC_CLEAR_CONFIGURATION, Service.VAR_COLUMN);
    }

    if (updateConfiguration(column, null)) {
      return ResponseObject.response(column);
    } else {
      String message = BeeUtils.joinWords(SVC_CLEAR_CONFIGURATION, column,
          "cannot clear configuration");
      logger.severe(message);
      return ResponseObject.error(message);
    }
  }

  private Map<Long, Integer> countAnalogs(String tempArticleIds) {
    Map<Long, Integer> result = new HashMap<>();

    String countName = "cnt" + SqlUtils.uniqueName();

    String articlesAlias = "art" + SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addFields(tempArticleIds, COL_TCD_ARTICLE)
        .addCountDistinct(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), countName)
        .addFrom(tempArticleIds)
        .addFromInner(TBL_TCD_ARTICLE_CODES,
            SqlUtils.joinUsing(tempArticleIds, TBL_TCD_ARTICLE_CODES, COL_TCD_ARTICLE))
        .addFromInner(TBL_TCD_ARTICLES,
            SqlUtils.and(SqlUtils.join(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR, TBL_TCD_ARTICLES,
                COL_TCD_ARTICLE_NR),
                SqlUtils.joinUsing(TBL_TCD_ARTICLE_CODES, TBL_TCD_ARTICLES, COL_TCD_BRAND),
                SqlUtils.notNull(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_VISIBLE)))
        .addFromInner(TBL_TCD_ARTICLES, articlesAlias,
            sys.joinTables(TBL_TCD_ARTICLES, articlesAlias, tempArticleIds, COL_TCD_ARTICLE))
        .setWhere(SqlUtils.or(SqlUtils.joinNotEqual(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR,
            articlesAlias, COL_TCD_ARTICLE_NR),
            SqlUtils.joinNotEqual(TBL_TCD_ARTICLES, COL_TCD_BRAND, articlesAlias, COL_TCD_BRAND)))
        .addGroup(tempArticleIds, COL_TCD_ARTICLE);

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_TCD_ARTICLE), row.getInt(countName));
      }
    }

    return result;
  }

  private ResponseObject createClient(RequestInfo reqInfo) {
    List<BeeColumn> columns = sys.getView(VIEW_CLIENTS).getRowSetColumns();
    BeeRow row = DataUtils.createEmptyRow(columns.size());

    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);
      if (column.isEditable()) {
        String value = reqInfo.getParameter(column.getId());

        if (!BeeUtils.isEmpty(value)) {
          row.setValue(i, value);
        } else if (!column.isNullable() && !column.hasDefaults()) {
          return ResponseObject.parameterNotFound(SVC_CREATE_CLIENT, column.getId());
        }
      }
    }

    int managerIndex = DataUtils.getColumnIndex(COL_CLIENT_MANAGER, columns);
    if (!DataUtils.isId(row.getLong(managerIndex))) {
      Long manager = qs.getId(TBL_MANAGERS, COL_MANAGER_USER, usr.getCurrentUserId());
      if (manager != null) {
        row.setValue(managerIndex, manager);
      }
    }

    BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_CLIENTS, columns, row);

    ResponseObject response = deb.commitRow(rowSet);
    if (response.hasErrors()) {
      return response;
    }

    response.clearMessages();

    if (reqInfo.hasParameter(VAR_MAIL)) {
      row = (BeeRow) response.getResponse();

      Long account = getSenderAccountId(row.getLong(managerIndex));

      String recipient = DataUtils.getString(columns, row, COL_EMAIL_ADDRESS);

      if (BeeUtils.isEmpty(recipient)) {
        Long userId = DataUtils.getLong(columns, row, COL_CLIENT_USER);
        recipient = usr.getUserEmail(userId, true);
      }

      String login = DataUtils.getString(columns, row, COL_LOGIN);
      String password = reqInfo.getParameter(COL_PASSWORD);

      SupportedLocale locale = EnumUtils.getEnumByIndex(SupportedLocale.class,
          BeeUtils.toIntOrNull(reqInfo.getParameter(COL_USER_LOCALE)));

      if (DataUtils.isId(account) && !BeeUtils.isEmpty(recipient)
          && !BeeUtils.anyEmpty(login, password)) {
        response.addMessagesFrom(mailRegistration(account, recipient, login, password, locale));
      }
    }

    return response;
  }

  private ResponseObject createItem(RequestInfo reqInfo) {
    Long newArt = tcd.cloneItem(reqInfo.getParameterLong(COL_TCD_ARTICLE),
        reqInfo.getParameter(COL_TCD_ARTICLE_NR), reqInfo.getParameterLong(COL_TCD_BRAND));

    if (!DataUtils.isId(newArt)) {
      return ResponseObject.error(Localized.dictionary()
          .dataNotAvailable(COL_TCD_ARTICLE + "=" + reqInfo.getParameterLong(COL_TCD_ARTICLE)));
    }
    qs.insertData(new SqlInsert(TBL_TCD_ARTICLE_SUPPLIERS)
        .addConstant(COL_TCD_ARTICLE, newArt)
        .addConstant(COL_TCD_SUPPLIER, reqInfo.getParameter(COL_TCD_SUPPLIER))
        .addConstant(COL_TCD_SUPPLIER_ID, reqInfo.getParameter(COL_TCD_SUPPLIER_ID)));

    return ResponseObject.response(newArt);
  }

  private String createTempArticleIds(SqlSelect query) {
    String tmp = qs.sqlCreateTemp(query);
    qs.sqlIndex(tmp, COL_TCD_ARTICLE);
    return tmp;
  }

  private ResponseObject didNotMatch(String query) {
    return ResponseObject.warning(usr.getDictionary().ecSearchDidNotMatch(query));
  }

  private ResponseObject doGlobalSearch(String query) {
    if (BeeUtils.isEmpty(query)) {
      return ResponseObject.parameterNotFound(SVC_GLOBAL_SEARCH, VAR_QUERY);
    }

    IsCondition condition;
    String code;

    if (BeeUtils.isEmpty(BeeUtils.parseDigits(query))) {
      condition = SqlUtils.contains(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, query);
      code = null;
    } else {
      condition = SqlUtils.or(SqlUtils.contains(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, query),
          SqlUtils.contains(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NR, query));
      code = query;
    }

    SqlSelect articleIdQuery = new SqlSelect()
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLES)
        .setWhere(SqlUtils.and(condition, articleCondition));

    List<EcItem> items = getItems(articleIdQuery, code);
    if (items.isEmpty()) {
      return didNotMatch(query);
    } else {
      return ResponseObject.response(items).setSize(items.size());
    }
  }

  private Map<Long, String> getArticleCategories(String tempArticleIds) {
    Map<Long, String> result = new HashMap<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE, COL_TCD_CATEGORY)
        .addFrom(TBL_TCD_ARTICLE_CATEGORIES)
        .addFromInner(tempArticleIds, SqlUtils.joinUsing(tempArticleIds,
            TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE))
        .addOrder(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE);

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      Map<Long, Long> parents = getCategoryParents();

      long lastArt = 0;
      Set<Long> categories = new HashSet<>();

      for (SimpleRow row : data) {
        long art = row.getLong(COL_TCD_ARTICLE);
        long cat = row.getLong(COL_TCD_CATEGORY);

        if (art != lastArt) {
          if (!categories.isEmpty()) {
            result.put(lastArt, EcItem.joinCategories(categories));
            categories.clear();
          }

          lastArt = art;
        }

        for (Long id = cat; id != null; id = parents.get(id)) {
          if (!categories.add(id)) {
            break;
          }
        }
      }

      if (!categories.isEmpty()) {
        result.put(lastArt, EcItem.joinCategories(categories));
      }
    }

    return result;
  }

  private ListMultimap<Long, ArticleCriteria> getArticleCriteria(String tempArticleIds) {
    ListMultimap<Long, ArticleCriteria> criteria = ArrayListMultimap.create();

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_ARTICLE, COL_TCD_CRITERIA_VALUE)
        .addFields(TBL_TCD_CRITERIA, COL_TCD_CRITERIA_NAME)
        .addFrom(TBL_TCD_ARTICLE_CRITERIA)
        .addFromInner(tempArticleIds,
            SqlUtils.joinUsing(tempArticleIds, TBL_TCD_ARTICLE_CRITERIA, COL_TCD_ARTICLE))
        .addFromInner(TBL_TCD_CRITERIA,
            sys.joinTables(TBL_TCD_CRITERIA, TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA))
        .setWhere(SqlUtils.notNull(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA_VALUE))
        .addOrder(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_ARTICLE)
        .addOrder(TBL_TCD_CRITERIA, COL_TCD_CRITERIA_NAME)
        .addOrder(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA_VALUE));

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        criteria.put(row.getLong(COL_TCD_ARTICLE),
            new ArticleCriteria(row.getValue(COL_TCD_CRITERIA_NAME),
                row.getValue(COL_TCD_CRITERIA_VALUE)));
      }
    }

    return criteria;
  }

  private Multimap<Long, ArticleSupplier> getArticleSuppliers(String tempArticleIds,
      Collection<Long> articleIds, Map<EcSupplier, Long> basePriceLists,
      Map<EcSupplier, Long> clientPriceLists) {

    String idName = sys.getIdName(TBL_TCD_ARTICLE_SUPPLIERS);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_SUPPLIERS, idName, COL_TCD_ARTICLE, COL_TCD_SUPPLIER,
            COL_TCD_SUPPLIER_ID, COL_TCD_COST)
        .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
        .addFields(TBL_TCD_REMAINDERS, COL_TCD_REMAINDER)
        .addFrom(TBL_TCD_ARTICLE_SUPPLIERS);

    if (!BeeUtils.isEmpty(tempArticleIds)) {
      query.addFromInner(tempArticleIds,
          SqlUtils.joinUsing(TBL_TCD_ARTICLE_SUPPLIERS, tempArticleIds, COL_TCD_ARTICLE));
    }

    if (!BeeUtils.isEmpty(basePriceLists)) {
      for (Map.Entry<EcSupplier, Long> entry : basePriceLists.entrySet()) {
        String priceListAlias = "Bpl_" + SqlUtils.uniqueName();

        String priceAlias = entry.getKey().getBasePriceListColumnName();
        query.addField(priceListAlias, COL_TCD_PRICE, priceAlias);

        query.addFromLeft(TBL_TCD_ARTICLE_PRICES, priceListAlias,
            SqlUtils.and(
                SqlUtils.joinUsing(TBL_TCD_ARTICLE_SUPPLIERS, priceListAlias, COL_TCD_ARTICLE),
                SqlUtils.equals(priceListAlias, COL_TCD_PRICELIST, entry.getValue())));

      }
    }

    if (!BeeUtils.isEmpty(clientPriceLists)) {
      for (Map.Entry<EcSupplier, Long> entry : clientPriceLists.entrySet()) {
        String priceListAlias = "Cpl_" + SqlUtils.uniqueName();

        String priceAlias = entry.getKey().getClientPriceListColumnName();
        query.addField(priceListAlias, COL_TCD_PRICE, priceAlias);

        query.addFromLeft(TBL_TCD_ARTICLE_PRICES, priceListAlias,
            SqlUtils.and(
                SqlUtils.joinUsing(TBL_TCD_ARTICLE_SUPPLIERS, priceListAlias, COL_TCD_ARTICLE),
                SqlUtils.equals(priceListAlias, COL_TCD_PRICELIST, entry.getValue())));

      }
    }

    query.addFromLeft(TBL_TCD_REMAINDERS,
        sys.joinTables(TBL_TCD_ARTICLE_SUPPLIERS, TBL_TCD_REMAINDERS, COL_TCD_ARTICLE_SUPPLIER));
    query.addFromLeft(TBL_WAREHOUSES,
        sys.joinTables(TBL_WAREHOUSES, TBL_TCD_REMAINDERS, COL_WAREHOUSE));

    if (!BeeUtils.isEmpty(articleIds)) {
      query.setWhere(SqlUtils.inList(TBL_TCD_ARTICLE_SUPPLIERS, COL_TCD_ARTICLE, articleIds));
    }

    query.addOrder(TBL_TCD_ARTICLE_SUPPLIERS, idName);

    SimpleRowSet data = qs.getData(query);

    Multimap<Long, ArticleSupplier> suppliers = HashMultimap.create();

    String lastId = null;
    ArticleSupplier supplier = null;

    Double listPrice;
    Double price;

    for (SimpleRow row : data) {
      String id = row.getValue(idName);

      if (!BeeUtils.same(id, lastId)) {
        EcSupplier ecs = EnumUtils.getEnumByIndex(EcSupplier.class, row.getInt(COL_TCD_SUPPLIER));

        if (basePriceLists != null && basePriceLists.containsKey(ecs)) {
          listPrice = row.getDouble(ecs.getBasePriceListColumnName());
        } else {
          listPrice = null;
        }

        if (clientPriceLists != null && clientPriceLists.containsKey(ecs)) {
          price = row.getDouble(ecs.getClientPriceListColumnName());
        } else {
          price = null;
        }

        supplier = new ArticleSupplier(ecs, row.getValue(COL_TCD_SUPPLIER_ID),
            row.getDouble(COL_TCD_COST), listPrice, price);

        suppliers.put(row.getLong(COL_TCD_ARTICLE), supplier);
        lastId = id;
      }

      supplier.addRemainder(row.getValue(COL_WAREHOUSE_CODE), row.getDouble(COL_TCD_REMAINDER));
    }
    return suppliers;
  }

  private BeeRowSet getBanners(List<RowInfo> cachedBanners) {
    DateTimeValue now = new DateTimeValue(TimeUtils.nowMinutes());

    Filter filter = Filter.and(
        Filter.or(Filter.isNull(COL_BANNER_SHOW_AFTER),
            Filter.isLessEqual(COL_BANNER_SHOW_AFTER, now)),
        Filter.or(Filter.isNull(COL_BANNER_SHOW_BEFORE),
            Filter.isMore(COL_BANNER_SHOW_BEFORE, now)));

    BeeRowSet rowSet = qs.getViewData(VIEW_BANNERS, filter);
    boolean changed;

    if (DataUtils.isEmpty(rowSet)) {
      changed = !cachedBanners.isEmpty();

    } else if (cachedBanners.size() != rowSet.getNumberOfRows()) {
      changed = true;

    } else {
      changed = false;

      for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
        RowInfo rowInfo = cachedBanners.get(i);
        BeeRow row = rowSet.getRow(i);

        if (rowInfo.getId() != row.getId() || rowInfo.getVersion() != row.getVersion()) {
          changed = true;
          break;
        }
      }
    }

    return changed ? rowSet : null;
  }

  private String getBranchLabel(Long branch) {
    if (branch == null) {
      return null;
    }

    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_BRANCHES, COL_BRANCH_NAME,
            COL_BRANCH_CODE)
        .addFrom(TBL_BRANCHES)
        .setWhere(sys.idEquals(TBL_BRANCHES, branch)));

    if (row == null) {
      return null;
    }

    String label = row.getValue(COL_BRANCH_CODE);
    if (!BeeUtils.isEmpty(label)) {
      return label;
    }
    return row.getValue(COL_BRANCH_NAME);
  }

  private List<String> getBranchWarehouses(Long branch) {
    List<String> result = new ArrayList<>();

    if (branch != null) {
      String[] arr = qs.getColumn(new SqlSelect()
          .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
          .addFrom(TBL_WAREHOUSES)
          .setWhere(SqlUtils.equals(TBL_WAREHOUSES,
              COL_WAREHOUSE_BRANCH, branch))
          .addOrder(TBL_WAREHOUSES, COL_WAREHOUSE_CODE));

      if (arr != null) {
        for (String s : arr) {
          result.add(s);
        }
      }
    }

    return result;
  }

  private ResponseObject getCarManufacturers() {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addFrom(TBL_TCD_MANUFACTURERS)
        .setWhere(SqlUtils.notNull(TBL_TCD_MANUFACTURERS, COL_TCD_MF_VISIBLE))
        .addOrder(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME);

    String[] manufacturers = qs.getColumn(query);
    return ResponseObject.response(manufacturers).setSize(ArrayUtils.length(manufacturers));
  }

  private ResponseObject getCarModels(String manufacturer) {
    if (BeeUtils.isEmpty(manufacturer)) {
      return ResponseObject.parameterNotFound(SVC_GET_CAR_MODELS, VAR_MANUFACTURER);
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_TYPES, COL_TCD_MODEL)
        .addFields(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addFields(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addMin(TBL_TCD_TYPES, COL_TCD_PRODUCED_FROM)
        .addMax(TBL_TCD_TYPES, COL_TCD_PRODUCED_TO)
        .addFrom(TBL_TCD_MODELS)
        .addFromInner(TBL_TCD_MANUFACTURERS,
            sys.joinTables(TBL_TCD_MANUFACTURERS, TBL_TCD_MODELS, COL_TCD_MANUFACTURER))
        .addFromInner(TBL_TCD_TYPES, sys.joinTables(TBL_TCD_MODELS, TBL_TCD_TYPES, COL_TCD_MODEL))
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME, manufacturer),
            SqlUtils.notNull(TBL_TCD_MODELS, COL_TCD_MODEL_VISIBLE),
            SqlUtils.notNull(TBL_TCD_TYPES, COL_TCD_TYPE_VISIBLE)))
        .addGroup(TBL_TCD_TYPES, COL_TCD_MODEL)
        .addGroup(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addGroup(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addOrder(TBL_TCD_MODELS, COL_TCD_MODEL_NAME);

    SimpleRowSet rowSet = qs.getData(query);
    if (DataUtils.isEmpty(rowSet)) {
      return didNotMatch(manufacturer);
    }

    List<EcCarModel> carModels = new ArrayList<>();
    for (SimpleRow row : rowSet) {
      carModels.add(new EcCarModel(row));
    }

    return ResponseObject.response(carModels).setSize(carModels.size());
  }

  private ResponseObject getCarTypeHistory() {
    Long user = usr.getCurrentUserId();
    if (!DataUtils.isId(user)) {
      return ResponseObject.emptyResponse();
    }

    Integer maxSize = qs.getInt(new SqlSelect().addFrom(TBL_CLIENTS)
        .addFields(TBL_CLIENTS, COL_CLIENT_CAR_TYPE_HISTORY_SIZE)
        .setWhere(SqlUtils.equals(TBL_CLIENTS, COL_CLIENT_USER, user)));
    if (BeeUtils.isNegative(maxSize)) {
      return ResponseObject.response(maxSize);
    }

    if (!BeeUtils.isPositive(maxSize)) {
      maxSize = DEFAULT_CAR_TYPE_HISTORY_SIZE;
    }

    IsCondition historyWhere = SqlUtils.and(SqlUtils.equals(TBL_HISTORY, COL_HISTORY_USER, user),
        SqlUtils.equals(TBL_HISTORY, COL_HISTORY_SERVICE, SVC_GET_ITEMS_BY_CAR_TYPE),
        SqlUtils.notNull(TBL_HISTORY, COL_HISTORY_QUERY),
        SqlUtils.more(TBL_HISTORY, COL_HISTORY_COUNT, 0));

    String dateAlias = "Date_" + SqlUtils.uniqueName();

    SqlSelect historyQuery = new SqlSelect()
        .addFields(TBL_HISTORY, COL_HISTORY_QUERY)
        .addFrom(TBL_HISTORY)
        .addMax(TBL_HISTORY, COL_HISTORY_DATE, dateAlias)
        .setWhere(historyWhere)
        .addGroup(TBL_HISTORY, COL_HISTORY_QUERY)
        .addOrderDesc(null, dateAlias)
        .setLimit(maxSize);

    SimpleRowSet historyData = qs.getData(historyQuery);
    if (DataUtils.isEmpty(historyData)) {
      return ResponseObject.emptyResponse();
    }

    List<EcCarType> carTypes = new ArrayList<>();

    String idName = sys.getIdName(TBL_TCD_TYPES);

    SqlSelect carTypeQuery = new SqlSelect()
        .addFields(TBL_TCD_TYPES, COL_TCD_MODEL)
        .addFields(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addFields(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addField(TBL_TCD_TYPES, idName, COL_TCD_TYPE)
        .addFields(TBL_TCD_TYPES, COL_TCD_TYPE_NAME,
            COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO, COL_TCD_CCM,
            COL_TCD_KW_FROM, COL_TCD_KW_TO, COL_TCD_CYLINDERS, COL_TCD_MAX_WEIGHT,
            COL_TCD_ENGINE, COL_TCD_FUEL, COL_TCD_BODY, COL_TCD_AXLE)
        .addFrom(TBL_TCD_TYPES)
        .addFromInner(TBL_TCD_MODELS,
            sys.joinTables(TBL_TCD_MODELS, TBL_TCD_TYPES, COL_TCD_MODEL))
        .addFromInner(TBL_TCD_MANUFACTURERS,
            sys.joinTables(TBL_TCD_MANUFACTURERS, TBL_TCD_MODELS, COL_TCD_MANUFACTURER));

    IsCondition visibilityCondition =
        SqlUtils.and(
            SqlUtils.notNull(TBL_TCD_TYPES, COL_TCD_TYPE_VISIBLE),
            SqlUtils.notNull(TBL_TCD_MODELS, COL_TCD_MODEL_VISIBLE),
            SqlUtils.notNull(TBL_TCD_MANUFACTURERS, COL_TCD_MF_VISIBLE));

    for (SimpleRow historyRow : historyData) {
      Long typeId = historyRow.getLong(COL_HISTORY_QUERY);

      if (DataUtils.isId(typeId)) {
        carTypeQuery.setWhere(
            SqlUtils.and(
                SqlUtils.equals(TBL_TCD_TYPES, idName, typeId),
                visibilityCondition));

        SimpleRow carTypeRow = qs.getRow(carTypeQuery);

        if (carTypeRow != null) {
          carTypes.add(new EcCarType(carTypeRow));
        }
      }
    }

    if (carTypes.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(carTypes).setSize(carTypes.size());
    }
  }

  private ResponseObject getCarTypes(Long modelId) {
    if (!DataUtils.isId(modelId)) {
      return ResponseObject.parameterNotFound(SVC_GET_CAR_TYPES, VAR_MODEL);
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_TYPES, COL_TCD_MODEL)
        .addFields(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addFields(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addField(TBL_TCD_TYPES, sys.getIdName(TBL_TCD_TYPES), COL_TCD_TYPE)
        .addFields(TBL_TCD_TYPES, COL_TCD_TYPE_NAME,
            COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO, COL_TCD_CCM,
            COL_TCD_KW_FROM, COL_TCD_KW_TO, COL_TCD_CYLINDERS, COL_TCD_MAX_WEIGHT,
            COL_TCD_ENGINE, COL_TCD_FUEL, COL_TCD_BODY, COL_TCD_AXLE)
        .addFrom(TBL_TCD_MODELS)
        .addFromInner(TBL_TCD_MANUFACTURERS,
            sys.joinTables(TBL_TCD_MANUFACTURERS, TBL_TCD_MODELS, COL_TCD_MANUFACTURER))
        .addFromInner(TBL_TCD_TYPES, sys.joinTables(TBL_TCD_MODELS, TBL_TCD_TYPES, COL_TCD_MODEL))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TCD_TYPES, COL_TCD_MODEL, modelId),
            SqlUtils.notNull(TBL_TCD_TYPES, COL_TCD_TYPE_VISIBLE)))
        .addOrder(TBL_TCD_TYPES, COL_TCD_TYPE_NAME, COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO,
            COL_TCD_KW_FROM, COL_TCD_KW_TO);

    SimpleRowSet rowSet = qs.getData(query);
    if (DataUtils.isEmpty(rowSet)) {
      return didNotMatch(BeeUtils.toString(modelId));
    }

    List<EcCarType> carTypes = new ArrayList<>();
    for (SimpleRow row : rowSet) {
      carTypes.add(new EcCarType(row));
    }

    return ResponseObject.response(carTypes).setSize(carTypes.size());
  }

  private ResponseObject getCategories() {
    String idName = sys.getIdName(TBL_TCD_CATEGORIES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TCD_CATEGORIES, idName, COL_TCD_CATEGORY_PARENT,
            COL_TCD_CATEGORY_NAME)
        .addFrom(TBL_TCD_CATEGORIES)
        .addOrder(TBL_TCD_CATEGORIES, idName);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      String msg = TBL_TCD_CATEGORIES + ": data not available";
      logger.warning(msg);
      return ResponseObject.error(msg);
    }

    int rc = data.getNumberOfRows();
    int cc = data.getNumberOfColumns();

    String[] arr = new String[rc * cc];
    int i = 0;

    for (String[] row : data.getRows()) {
      for (int j = 0; j < cc; j++) {
        arr[i * cc + j] = row[j];
      }
      i++;
    }

    return ResponseObject.response(arr).setSize(rc);
  }

  private Map<Long, Long> getCategoryParents() {
    Map<Long, Long> result = new HashMap<>();

    String colCategoryId = sys.getIdName(TBL_TCD_CATEGORIES);

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_TCD_CATEGORIES, colCategoryId, COL_TCD_CATEGORY_PARENT);
    query.addFrom(TBL_TCD_CATEGORIES);
    query.setWhere(SqlUtils.notNull(TBL_TCD_CATEGORIES, COL_TCD_CATEGORY_PARENT));

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(colCategoryId), row.getLong(COL_TCD_CATEGORY_PARENT));
      }
    }

    return result;
  }

  private EcClientDiscounts getClientDiscounts() {
    List<SimpleRowSet> discounts = new ArrayList<>();

    String colClientId = sys.getIdName(TBL_CLIENTS);
    SimpleRow currentClientInfo = getCurrentClientInfo(colClientId, COL_CLIENT_DISCOUNT_PERCENT,
        COL_CLIENT_DISCOUNT_PARENT);
    if (currentClientInfo == null) {
      return new EcClientDiscounts(null, discounts);
    }

    Long client = currentClientInfo.getLong(colClientId);

    Double percent = currentClientInfo.getDouble(COL_CLIENT_DISCOUNT_PERCENT);
    Long parent = currentClientInfo.getLong(COL_CLIENT_DISCOUNT_PARENT);

    SqlSelect discountQuery = new SqlSelect();
    discountQuery.addFields(EcConstants.TBL_DISCOUNTS,
        EcConstants.COL_DISCOUNT_DATE_FROM, EcConstants.COL_DISCOUNT_DATE_TO,
        EcConstants.COL_DISCOUNT_CATEGORY, COL_DISCOUNT_BRAND, COL_DISCOUNT_SUPPLIER,
        COL_DISCOUNT_ARTICLE, EcConstants.COL_DISCOUNT_PERCENT, EcConstants.COL_DISCOUNT_PRICE);
    discountQuery.addFrom(EcConstants.TBL_DISCOUNTS);

    discountQuery.setWhere(SqlUtils.equals(EcConstants.TBL_DISCOUNTS, COL_DISCOUNT_CLIENT, client));

    SimpleRowSet discountData = qs.getData(discountQuery);
    if (!DataUtils.isEmpty(discountData)) {
      discounts.add(discountData);
    }

    if (DataUtils.isId(parent) && !Objects.equals(client, parent)) {
      Set<Long> traversed = Sets.newHashSet(client);

      SqlSelect clientQuery = new SqlSelect();
      clientQuery.addFields(TBL_CLIENTS, COL_CLIENT_DISCOUNT_PERCENT, COL_CLIENT_DISCOUNT_PARENT);
      clientQuery.addFrom(TBL_CLIENTS);

      while (DataUtils.isId(parent) && !traversed.contains(parent)) {
        discountQuery.setWhere(SqlUtils.equals(EcConstants.TBL_DISCOUNTS, COL_DISCOUNT_CLIENT,
            parent));

        discountData = qs.getData(discountQuery);
        if (!DataUtils.isEmpty(discountData)) {
          discounts.add(discountData);
        }

        clientQuery.setWhere(SqlUtils.equals(TBL_CLIENTS, colClientId, parent));

        SimpleRow clientRow = qs.getRow(clientQuery);
        if (percent == null) {
          percent = clientRow.getDouble(COL_CLIENT_DISCOUNT_PERCENT);
        }

        traversed.add(parent);
        parent = clientRow.getLong(COL_CLIENT_DISCOUNT_PARENT);
      }
    }

    return new EcClientDiscounts(percent, discounts);
  }

  private ResponseObject getClientInfo() {
    BeeRowSet rowSet = qs.getViewData(VIEW_CLIENTS,
        Filter.equals(COL_CLIENT_USER, usr.getCurrentUserId()));

    if (DataUtils.isEmpty(rowSet)) {
      String msg = BeeUtils.joinWords("client not available for user", usr.getCurrentUser());
      logger.severe(msg);
      return ResponseObject.error(msg);
    }

    Map<String, String> result = new HashMap<>();

    BeeRow row = rowSet.getRow(0);
    for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
      if (!row.isNull(i)) {
        result.put(rowSet.getColumnId(i), row.getString(i));
      }
    }

    return ResponseObject.response(result);
  }

  private Pair<Map<EcSupplier, Long>, Map<EcSupplier, Long>> getClientPriceLists() {
    Map<EcSupplier, Long> basePriceLists = new EnumMap<>(EcSupplier.class);
    Map<EcSupplier, Long> clientPriceLists = new EnumMap<>(EcSupplier.class);

    SimpleRow row = getCurrentClientRow();

    Long priceList;
    String name;

    for (EcSupplier supplier : EcSupplier.values()) {
      name = supplier.getBasePriceListColumnName();
      if (row != null && row.getRowSet().hasColumn(name)) {
        priceList = row.getLong(name);
      } else {
        priceList = null;
      }

      if (!DataUtils.isId(priceList)) {
        name = supplier.getBasePriceListParameterName();
        if (!BeeUtils.isEmpty(name)) {
          priceList = prm.getRelation(name);
        }
      }

      if (DataUtils.isId(priceList)) {
        basePriceLists.put(supplier, priceList);
      }

      name = supplier.getClientPriceListColumnName();
      if (row != null && row.getRowSet().hasColumn(name)) {
        priceList = row.getLong(name);
      } else {
        priceList = null;
      }

      if (!DataUtils.isId(priceList)) {
        name = supplier.getClientPriceListParameterName();
        if (!BeeUtils.isEmpty(name)) {
          priceList = prm.getRelation(name);
        }
      }

      if (DataUtils.isId(priceList)) {
        clientPriceLists.put(supplier, priceList);
      }
    }

    return Pair.of(basePriceLists, clientPriceLists);
  }

  private ResponseObject getClientStockLabels() {
    List<String> result = new ArrayList<>();

    String colClientId = sys.getIdName(TBL_CLIENTS);
    SimpleRow clientInfo = getCurrentClientInfo(colClientId, COL_CLIENT_PRIMARY_BRANCH,
        COL_CLIENT_SECONDARY_BRANCH);
    if (clientInfo == null) {
      result.add(BeeConst.STRING_EMPTY);
      result.add(BeeConst.STRING_EMPTY);

      return ResponseObject.response(result);
    }

    Long client = clientInfo.getLong(colClientId);

    Long branch = clientInfo.getLong(COL_CLIENT_PRIMARY_BRANCH);
    String primaryLabel;
    if (branch == null) {
      primaryLabel = BeeUtils.joinItems(getClientWarehouses(client, TBL_PRIMARY_WAREHOUSES));
    } else {
      primaryLabel = getBranchLabel(branch);
    }

    branch = clientInfo.getLong(COL_CLIENT_SECONDARY_BRANCH);
    String secondaryLabel;
    if (branch == null) {
      secondaryLabel = BeeUtils.joinItems(getClientWarehouses(client, TBL_SECONDARY_WAREHOUSES));
    } else {
      secondaryLabel = getBranchLabel(branch);
    }

    result.add(primaryLabel);
    result.add(secondaryLabel);

    return ResponseObject.response(result);
  }

  private Pair<Set<String>, Set<String>> getClientWarehouses() {
    Set<String> primary = new HashSet<>();
    Set<String> secondary = new HashSet<>();

    Pair<Set<String>, Set<String>> result = Pair.of(primary, secondary);

    String colClientId = sys.getIdName(TBL_CLIENTS);
    SimpleRow clientInfo = getCurrentClientInfo(colClientId, COL_CLIENT_PRIMARY_BRANCH,
        COL_CLIENT_SECONDARY_BRANCH);
    if (clientInfo == null) {
      return result;
    }

    Long branch = clientInfo.getLong(COL_CLIENT_PRIMARY_BRANCH);
    if (branch != null) {
      primary.addAll(getBranchWarehouses(branch));
    }

    branch = clientInfo.getLong(COL_CLIENT_SECONDARY_BRANCH);
    if (branch != null) {
      secondary.addAll(getBranchWarehouses(branch));
    }

    Long client = clientInfo.getLong(colClientId);
    if (client != null) {
      primary.addAll(getClientWarehouses(client, TBL_PRIMARY_WAREHOUSES));
      secondary.addAll(getClientWarehouses(client, TBL_SECONDARY_WAREHOUSES));
    }

    if (BeeUtils.intersects(primary, secondary)) {
      if (primary.containsAll(secondary)) {
        secondary.removeAll(primary);
      } else {
        primary.removeAll(secondary);
      }
    }

    return result;
  }

  private List<String> getClientWarehouses(Long client, String table) {
    List<String> result = new ArrayList<>();

    if (client != null) {
      String[] arr = qs.getColumn(new SqlSelect()
          .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_CODE)
          .addFrom(table)
          .addFromInner(TBL_WAREHOUSES,
              sys.joinTables(TBL_WAREHOUSES, table, COL_CW_WAREHOUSE))
          .setWhere(SqlUtils.equals(table, COL_CW_CLIENT, client))
          .addOrder(TBL_WAREHOUSES, COL_WAREHOUSE_CODE));

      if (arr != null) {
        for (String s : arr) {
          result.add(s);
        }
      }
    }

    return result;
  }

  private ResponseObject getConfiguration() {
    BeeRowSet rowSet = qs.getViewData(VIEW_CONFIGURATION);
    if (rowSet == null) {
      return ResponseObject.error("cannot read", VIEW_CONFIGURATION);
    }

    Map<String, String> result = new HashMap<>();
    if (rowSet.isEmpty()) {
      for (BeeColumn column : rowSet.getColumns()) {
        result.put(column.getId(), null);
      }
    } else {
      BeeRow row = rowSet.getRow(0);
      for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
        result.put(rowSet.getColumnId(i), row.getString(i));
      }
    }

    return ResponseObject.response(result);
  }

  private Long getCurrentClientId() {
    Long id = qs.getLong(new SqlSelect().addFrom(TBL_CLIENTS)
        .addFields(TBL_CLIENTS, sys.getIdName(TBL_CLIENTS))
        .setWhere(SqlUtils.equals(TBL_CLIENTS, COL_CLIENT_USER, usr.getCurrentUserId())));

    if (!DataUtils.isId(id)) {
      logger.severe("client not available for user", usr.getCurrentUser());
    }
    return id;
  }

  private SimpleRow getCurrentClientInfo(String... fields) {
    return qs.getRow(new SqlSelect().addFrom(TBL_CLIENTS).addFields(TBL_CLIENTS, fields)
        .setWhere(SqlUtils.equals(TBL_CLIENTS, COL_CLIENT_USER, usr.getCurrentUserId())));
  }

  private SimpleRow getCurrentClientRow() {
    return qs.getRow(new SqlSelect().addFrom(TBL_CLIENTS).addAllFields(TBL_CLIENTS)
        .setWhere(SqlUtils.equals(TBL_CLIENTS, COL_CLIENT_USER, usr.getCurrentUserId())));
  }

  private ResponseObject getDeliveryMethods() {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_DELIVERY_METHODS)
        .addFields(TBL_DELIVERY_METHODS, COL_DELIVERY_METHOD_ID, COL_DELIVERY_METHOD_NAME,
            COL_DELIVERY_METHOD_NOTES)
        .addOrder(TBL_DELIVERY_METHODS, COL_DELIVERY_METHOD_NAME);

    SimpleRowSet rowSet = qs.getData(query);
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.warning(usr.getDictionary().dataNotAvailable(
          usr.getDictionary().ecDeliveryMethods()));
    }

    List<DeliveryMethod> deliveryMethods = new ArrayList<>();
    for (SimpleRow row : rowSet) {
      deliveryMethods.add(new DeliveryMethod(row.getLong(COL_DELIVERY_METHOD_ID),
          row.getValue(COL_DELIVERY_METHOD_NAME), row.getValue(COL_DELIVERY_METHOD_NOTES)));
    }

    return ResponseObject.response(deliveryMethods);
  }

  private ResponseObject getFinancialInformation(Long client) {
    EcFinInfo finInfo = new EcFinInfo();
    if (!DataUtils.isId(client)) {
      return ResponseObject.response(finInfo);
    }
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    if (!BeeUtils.same(remoteAddress, BeeConst.STRING_MINUS)) {
      SimpleRow companyInfo = qs.getRow(new SqlSelect()
          .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CODE)
          .addFrom(TBL_CLIENTS)
          .addFromInner(TBL_USERS, sys.joinTables(TBL_USERS, TBL_CLIENTS, COL_CLIENT_USER))
          .addFromInner(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
          .addFromInner(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
          .setWhere(sys.idEquals(TBL_CLIENTS, client)));

      String company = null;
      String wh =
          "LOWER(klientas) = '" + companyInfo.getValue(COL_COMPANY_NAME).toLowerCase() + "'";

      try {
        SimpleRow row = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
            .getSQLData("SELECT klientas, max_skola, dienos"
                    + " FROM klientai"
                    + " WHERE " + wh + " OR kodas = '" + companyInfo.getValue(COL_COMPANY_CODE)
                    + "'"
                    + " ORDER BY " + wh + " DESC",
                "klientas", "max_skola", "dienos").getRow(0);

        if (row != null) {
          finInfo.setCreditLimit(row.getDouble("max_skola"));
          finInfo.setDaysForPayment(row.getInt("dienos"));
          company = row.getValue("klientas");
        }
      } catch (BeeException e) {
        logger.error(e);
      }
      if (!BeeUtils.isEmpty(company)) {
        try {
          SimpleRow row = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
              .getSQLData("SELECT SUM(kiekis * kaina) AS suma"
                      + " FROM likuciai"
                      + " INNER JOIN sand ON likuciai.sandelis = sand.sandelis"
                      + "   AND sand.konsign IS NOT NULL"
                      + " WHERE likuciai.kiekis > 0 AND likuciai.gavejas = '" + company + "'",
                  "suma").getRow(0);

          if (row != null) {
            finInfo.setTotalTaken(row.getDouble("suma"));
          }
        } catch (BeeException e) {
          logger.error(e);
        }
        try {
          double totalDebt = 0;
          double timedOutDebt = 0;

          SimpleRowSet data = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
              .getSQLData("SELECT data, dokumentas, dok_serija, kitas_dok, viso, skola_w, terminas"
                      + " FROM apyvarta"
                      + " INNER JOIN operac ON apyvarta.operacija = operac.operacija"
                      + "   AND operac.oper_apm IS NOT NULL AND operac.oper_pirk IS NOT NULL"
                      + " INNER JOIN klientai ON apyvarta.gavejas = klientai.klientas"
                      + " WHERE apyvarta.pajamos = 0 AND apyvarta.ivestas IS NOT NULL"
                      + "   AND apyvarta.skola_w > 0"
                      + "   AND (klientai.klientas = '" + company + "'"
                      + "     OR klientai.moketojas = '" + company + "')"
                      + " ORDER BY data",
                  "data", "dokumentas", "dok_serija", "kitas_dok", "viso", "skola_w", "terminas");

          for (SimpleRow row : data) {
            DateTime date = TimeUtils.parseDateTime(row.getValue("data"));
            DateTime term = TimeUtils.parseDateTime(row.getValue("terminas"));

            if (term == null) {
              term = TimeUtils.nextDay(date, BeeUtils.unbox(finInfo.getDaysForPayment()))
                  .getDateTime();
            }
            double debt = BeeUtils.unbox(row.getDouble("skola_w"));
            totalDebt += debt;

            if (TimeUtils.dayDiff(term, TimeUtils.today()) > 0) {
              timedOutDebt += debt;
            }
            EcInvoice invoice = new EcInvoice();
            invoice.setDate(date);
            invoice.setNumber(BeeUtils.notEmpty(BeeUtils.joinWords(row.getValue("dok_serija"),
                row.getValue("kitas_dok")), row.getValue("dokumentas")));
            invoice.setAmount(row.getDouble("viso"));
            invoice.setDebt(debt);
            invoice.setTerm(term);
            finInfo.getInvoices().add(invoice);
          }
          finInfo.setDebt(totalDebt);
          finInfo.setMaxedOut(timedOutDebt);

        } catch (BeeException e) {
          logger.error(e);
        }
      }
    }
    BeeRowSet orderData = qs.getViewData(VIEW_ORDERS,
        Filter.equals(COL_ORDER_CLIENT, client),
        new Order(COL_ORDER_DATE, false));

    if (!DataUtils.isEmpty(orderData)) {
      int dateIndex = orderData.getColumnIndex(COL_ORDER_DATE);
      int statusIndex = orderData.getColumnIndex(COL_ORDER_STATUS);

      int mfIndex = orderData.getColumnIndex(ALS_ORDER_MANAGER_FIRST_NAME);
      int mlIndex = orderData.getColumnIndex(ALS_ORDER_MANAGER_LAST_NAME);

      int daIndex = orderData.getColumnIndex(COL_ORDER_DELIVERY_ADDRESS);
      int dmIndex = orderData.getColumnIndex(ALS_ORDER_DELIVERY_METHOD_NAME);

      int commentIndex = orderData.getColumnIndex(COL_ORDER_CLIENT_COMMENT);
      int rrIndex = orderData.getColumnIndex(ALS_ORDER_REJECTION_REASON_NAME);

      SqlSelect itemQuery = new SqlSelect()
          .addFields(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ARTICLE, COL_ORDER_ITEM_QUANTITY_ORDERED,
              COL_ORDER_ITEM_QUANTITY_SUBMIT, COL_ORDER_ITEM_PRICE)
          .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_NR,
              COL_TCD_ARTICLE_WEIGHT)
          .addFields(TBL_UNITS, COL_UNIT_NAME)
          .addFrom(TBL_ORDER_ITEMS)
          .addFromInner(TBL_TCD_ARTICLES,
              sys.joinTables(TBL_TCD_ARTICLES, TBL_ORDER_ITEMS, COL_ORDER_ITEM_ARTICLE))
          .addFromLeft(TBL_UNITS,
              sys.joinTables(TBL_UNITS, TBL_TCD_ARTICLES, COL_TCD_ARTICLE_UNIT))
          .addOrder(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS));

      for (BeeRow orderRow : orderData.getRows()) {
        EcOrder order = new EcOrder();

        order.setOrderId(orderRow.getId());
        order.setDate(orderRow.getDateTime(dateIndex));
        order.setStatus(orderRow.getInteger(statusIndex));

        order.setManager(BeeUtils.joinWords(orderRow.getString(mfIndex),
            orderRow.getString(mlIndex)));

        order.setDeliveryAddress(orderRow.getString(daIndex));
        order.setDeliveryMethod(orderRow.getString(dmIndex));

        order.setComment(orderRow.getString(commentIndex));
        order.setRejectionReason(orderRow.getString(rrIndex));

        itemQuery.setWhere(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ORDER,
            orderRow.getId()));
        SimpleRowSet itemData = qs.getData(itemQuery);

        if (!DataUtils.isEmpty(itemData)) {
          for (SimpleRow itemRow : itemData) {
            EcOrderItem item = new EcOrderItem();

            item.setArticleId(itemRow.getLong(COL_ORDER_ITEM_ARTICLE));

            item.setName(itemRow.getValue(COL_TCD_ARTICLE_NAME));
            item.setCode(itemRow.getValue(COL_TCD_ARTICLE_NR));

            item.setQuantityOrdered(itemRow.getInt(COL_ORDER_ITEM_QUANTITY_ORDERED));
            item.setQuantitySubmit(itemRow.getInt(COL_ORDER_ITEM_QUANTITY_SUBMIT));

            item.setPrice(itemRow.getDouble(COL_ORDER_ITEM_PRICE));

            item.setUnit(itemRow.getValue(COL_UNIT_NAME));
            item.setWeight(itemRow.getDouble(COL_TCD_ARTICLE_WEIGHT));

            order.getItems().add(item);
          }
        }

        BeeRowSet eventData = qs.getViewData(VIEW_ORDER_EVENTS,
            Filter.equals(COL_ORDER_EVENT_ORDER, orderRow.getId()),
            new Order(COL_ORDER_EVENT_DATE, true));

        if (!DataUtils.isEmpty(eventData)) {
          for (BeeRow eventRow : eventData.getRows()) {
            DateTime date = DataUtils.getDateTime(eventData, eventRow, COL_ORDER_EVENT_DATE);
            Integer status = DataUtils.getInteger(eventData, eventRow, COL_ORDER_EVENT_STATUS);

            String firstName = DataUtils.getString(eventData, eventRow, COL_FIRST_NAME);
            String lastName = DataUtils.getString(eventData, eventRow, COL_LAST_NAME);

            EcOrderEvent event = new EcOrderEvent(date, status,
                BeeUtils.joinWords(firstName, lastName));
            order.getEvents().add(event);
          }
        }

        finInfo.getOrders().add(order);
      }
    }

    int size = finInfo.getOrders().size() + finInfo.getInvoices().size();

    BeeRowSet unsuppliedItems = qs.getViewData(VIEW_UNSUPPLIED_ITEMS,
        Filter.equals(COL_UNSUPPLIED_ITEM_CLIENT, client));
    if (!DataUtils.isEmpty(unsuppliedItems)) {
      finInfo.setUnsuppliedItems(unsuppliedItems);
      size += unsuppliedItems.getNumberOfRows();
    }

    return ResponseObject.response(finInfo).setSize(size);
  }

  private Set<Long> getGroupCategories(Long groupId) {
    Set<Long> categories = new HashSet<>();

    Long[] arr = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_GROUP_CATEGORIES, COL_GROUP_CATEGORY)
        .addFrom(TBL_GROUP_CATEGORIES)
        .setWhere(SqlUtils.equals(TBL_GROUP_CATEGORIES, COL_GROUP, groupId)));

    if (arr != null) {
      for (Long category : arr) {
        categories.add(category);
      }
    }

    return categories;
  }

  private ResponseObject getGroupFilters(Long groupId) {
    if (!DataUtils.isId(groupId)) {
      return ResponseObject.parameterNotFound(SVC_GET_GROUP_FILTERS, COL_GROUP);
    }

    EcGroupFilters groupFilters = new EcGroupFilters();

    Set<Long> categories = getGroupCategories(groupId);
    if (categories.isEmpty()) {
      return ResponseObject.response(groupFilters);
    }

    IsCondition catWhere = SqlUtils.inList(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_CATEGORY,
        categories);

    Boolean needsBrands = qs.getBoolean(new SqlSelect()
        .addFields(TBL_GROUPS, COL_GROUP_BRAND_SELECTION)
        .addFrom(TBL_GROUPS)
        .setWhere(SqlUtils.equals(TBL_GROUPS, sys.getIdName(TBL_GROUPS), groupId)));

    if (BeeUtils.isTrue(needsBrands)) {
      String colBrandId = sys.getIdName(TBL_TCD_BRANDS);

      SimpleRowSet brandData = qs.getData(new SqlSelect().setDistinctMode(true)
          .addFields(TBL_TCD_BRANDS, colBrandId, COL_TCD_BRAND_NAME)
          .addFrom(TBL_TCD_ARTICLES)
          .addFromInner(TBL_TCD_BRANDS,
              sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ARTICLES, COL_TCD_BRAND))
          .addFromInner(TBL_TCD_ARTICLE_CATEGORIES,
              sys.joinTables(TBL_TCD_ARTICLES, TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE))
          .setWhere(SqlUtils.and(catWhere, articleCondition))
          .addOrder(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME, colBrandId));

      if (!DataUtils.isEmpty(brandData)) {
        for (SimpleRow row : brandData) {
          EcBrand brand = new EcBrand(row.getLong(colBrandId), row.getValue(COL_TCD_BRAND_NAME));
          groupFilters.getBrands().add(brand);
        }
      }
    }

    BeeRowSet criteria = qs.getViewData(VIEW_GROUP_CRITERIA, Filter.equals(COL_GROUP, groupId));

    if (!DataUtils.isEmpty(criteria)) {
      int idIndex = criteria.getColumnIndex(COL_GROUP_CRITERIA);
      int nameIndex = criteria.getColumnIndex(COL_TCD_CRITERIA_NAME);

      SqlSelect criterionQuery = new SqlSelect().setDistinctMode(true)
          .addFields(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA_VALUE)
          .addFrom(TBL_TCD_ARTICLE_CRITERIA)
          .addFromInner(TBL_TCD_ARTICLE_CATEGORIES, SqlUtils.join(TBL_TCD_ARTICLE_CATEGORIES,
              COL_TCD_ARTICLE, TBL_TCD_ARTICLE_CRITERIA, COL_TCD_ARTICLE))
          .addOrder(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA_VALUE);

      for (int i = 0; i < criteria.getNumberOfRows(); i++) {
        long criterionId = criteria.getLong(i, idIndex);

        criterionQuery.setWhere(SqlUtils.and(catWhere,
            SqlUtils.equals(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA, criterionId)));
        String[] values = qs.getColumn(criterionQuery);

        if (!ArrayUtils.isEmpty(values)) {
          EcCriterion criterion = new EcCriterion(criterionId, criteria.getString(i, nameIndex));
          for (String value : values) {
            criterion.getValues().add(new SelectableValue(value));
          }

          groupFilters.getCriteria().add(criterion);
        }
      }
    }

    return ResponseObject.response(groupFilters).setSize(groupFilters.getSize());
  }

  private ResponseObject getGroupItems(RequestInfo reqInfo, Long groupId) {
    if (!DataUtils.isId(groupId)) {
      return ResponseObject.parameterNotFound(SVC_GET_GROUP_ITEMS, COL_GROUP);
    }

    Set<Long> categories = getGroupCategories(groupId);
    if (categories.isEmpty()) {
      String message = BeeUtils.joinWords(SVC_GET_GROUP_ITEMS, COL_GROUP, groupId,
          "categories not available");
      logger.severe(message);
      return ResponseObject.error(message);
    }

    EcGroupFilters filters = null;
    if (reqInfo.hasParameter(VAR_FILTER)) {
      filters = EcGroupFilters.restore(reqInfo.getParameter(VAR_FILTER));
    }

    SqlSelect articleIdQuery = new SqlSelect().setDistinctMode(true)
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLES)
        .addFromInner(TBL_TCD_ARTICLE_CATEGORIES,
            sys.joinTables(TBL_TCD_ARTICLES, TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE));

    HasConditions where = SqlUtils.and(articleCondition,
        SqlUtils.inList(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_CATEGORY, categories));

    if (filters != null && !filters.isEmpty()) {
      Set<Long> selectedBrands = filters.getSelectedBrands();
      if (!selectedBrands.isEmpty()) {
        where.add(SqlUtils.inList(TBL_TCD_ARTICLES, COL_TCD_BRAND, selectedBrands));
      }

      Multimap<Long, String> selectedCriteria = filters.getSelectedCriteria();
      if (!selectedCriteria.isEmpty()) {

        for (Long criterion : selectedCriteria.keySet()) {
          String alias = SqlUtils.uniqueName();

          HasConditions conditions = SqlUtils.or();
          for (String value : selectedCriteria.get(criterion)) {
            conditions.add(SqlUtils.equals(alias, COL_TCD_CRITERIA_VALUE, value));
          }

          if (!conditions.isEmpty()) {
            articleIdQuery.addFromInner(TBL_TCD_ARTICLE_CRITERIA, alias,
                sys.joinTables(TBL_TCD_ARTICLES, alias, COL_TCD_ARTICLE));

            where.add(SqlUtils.and(SqlUtils.equals(alias, COL_TCD_CRITERIA, criterion),
                conditions));
          }
        }
      }
    }

    articleIdQuery.setWhere(where);

    List<EcItem> items = getItems(articleIdQuery, null);

    if (items.isEmpty()) {
      return didNotMatch(qs.getValue(new SqlSelect()
          .addFields(TBL_GROUPS, COL_GROUP_NAME)
          .addFrom(TBL_GROUPS)
          .setWhere(SqlUtils.equals(TBL_GROUPS, sys.getIdName(TBL_GROUPS), groupId))));
    } else {
      return ResponseObject.response(items).setSize(items.size());
    }
  }

  private String getIncomingEmail(Long manager) {
    if (DataUtils.isId(manager)) {
      String email = qs.getValue(new SqlSelect()
          .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
          .addFrom(TBL_MANAGERS)
          .addFromInner(TBL_EMAILS,
              sys.joinTables(TBL_EMAILS, TBL_MANAGERS, COL_MANAGER_INCOMING_MAIL))
          .setWhere(sys.idEquals(TBL_MANAGERS, manager)));

      if (!BeeUtils.isEmpty(email)) {
        return email;
      }
    }
    return qs.getValue(new SqlSelect()
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFrom(TBL_CONFIGURATION)
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_CONFIGURATION, COL_CONFIG_INCOMING_MAIL)));
  }

  private ResponseObject getItemAnalogs(RequestInfo reqInfo) {
    Long id = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_ARTICLE));
    String code = reqInfo.getParameter(COL_TCD_ARTICLE_NR);
    Long brand = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_BRAND));

    SqlSelect articleIdQuery = new SqlSelect().setDistinctMode(true)
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLE_CODES)
        .addFromInner(TBL_TCD_ARTICLES,
            SqlUtils.and(SqlUtils.join(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR, TBL_TCD_ARTICLES,
                COL_TCD_ARTICLE_NR),
                SqlUtils.joinUsing(TBL_TCD_ARTICLE_CODES, TBL_TCD_ARTICLES, COL_TCD_BRAND),
                SqlUtils.notNull(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_VISIBLE)))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TCD_ARTICLE_CODES, COL_TCD_ARTICLE, id),
            SqlUtils.or(SqlUtils.notEqual(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR, code),
                SqlUtils.notEqual(TBL_TCD_ARTICLE_CODES, COL_TCD_BRAND, brand))));

    return ResponseObject.response(getItems(articleIdQuery, null));
  }

  private ResponseObject getItemBrands() {
    String colBrandId = sys.getIdName(TBL_TCD_BRANDS);

    SimpleRowSet data = qs.getData(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TCD_BRANDS, colBrandId, COL_TCD_BRAND_NAME)
        .addFrom(TBL_TCD_ARTICLES)
        .addFromInner(TBL_TCD_BRANDS,
            sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ARTICLES, COL_TCD_BRAND))
        .setWhere(articleCondition)
        .addOrder(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME, colBrandId));

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.warning(usr.getDictionary().dataNotAvailable(TBL_TCD_BRANDS));
    }

    List<EcBrand> brands = new ArrayList<>();
    for (SimpleRow row : data) {
      brands.add(new EcBrand(row.getLong(colBrandId), row.getValue(COL_TCD_BRAND_NAME)));
    }

    return ResponseObject.response(brands);
  }

  private ResponseObject getItemGroups(boolean moto) {
    String colGroupId = sys.getIdName(TBL_GROUPS);
    IsCondition where = moto ? SqlUtils.notNull(TBL_GROUPS, COL_GROUP_MOTO)
        : SqlUtils.isNull(TBL_GROUPS, COL_GROUP_MOTO);

    SimpleRowSet groupData = qs.getData(new SqlSelect()
        .addFields(TBL_GROUPS, colGroupId, COL_GROUP_NAME, COL_GROUP_BRAND_SELECTION,
            COL_GROUP_ORDINAL)
        .addFrom(TBL_GROUPS)
        .setWhere(where)
        .addOrder(TBL_GROUPS, COL_GROUP_ORDINAL, COL_GROUP_NAME));

    if (DataUtils.isEmpty(groupData)) {
      return ResponseObject.emptyResponse();
    }

    List<EcGroup> groups = new ArrayList<>();

    for (SimpleRow groupRow : groupData) {
      Long id = groupRow.getLong(colGroupId);

      if (qs.sqlExists(TBL_GROUP_CATEGORIES,
          SqlUtils.equals(TBL_GROUP_CATEGORIES, COL_GROUP, id))) {

        EcGroup group = new EcGroup(id, groupRow.getValue(COL_GROUP_NAME));
        if (BeeUtils.isTrue(groupRow.getBoolean(COL_GROUP_BRAND_SELECTION))) {
          group.setBrandSelection(true);
        }

        BeeRowSet criteria = qs.getViewData(VIEW_GROUP_CRITERIA, Filter.equals(COL_GROUP, id));
        if (!DataUtils.isEmpty(criteria)) {
          int colIndex = criteria.getColumnIndex(COL_GROUP_CRITERIA);
          for (int i = 0; i < criteria.getNumberOfRows(); i++) {
            group.getCriteria().add(criteria.getLong(i, colIndex));
          }
        }

        groups.add(group);
      }
    }

    return ResponseObject.response(groups).setSize(groups.size());
  }

  private ResponseObject getItemInfo(Long articleId) {
    if (!DataUtils.isId(articleId)) {
      return ResponseObject.parameterNotFound(SVC_GET_ITEM_INFO, COL_TCD_ARTICLE);
    }
    EcItemInfo ecItemInfo = new EcItemInfo();

    SimpleRowSet criteriaData = qs.getData(new SqlSelect()
        .addFields(TBL_TCD_CRITERIA, COL_TCD_CRITERIA_NAME)
        .addFields(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA_VALUE)
        .addFrom(TBL_TCD_ARTICLE_CRITERIA)
        .addFromInner(TBL_TCD_CRITERIA,
            sys.joinTables(TBL_TCD_CRITERIA, TBL_TCD_ARTICLE_CRITERIA, COL_TCD_CRITERIA))
        .setWhere(SqlUtils.equals(TBL_TCD_ARTICLE_CRITERIA, COL_TCD_ARTICLE, articleId))
        .addOrder(TBL_TCD_CRITERIA, COL_TCD_CRITERIA_NAME));

    if (!DataUtils.isEmpty(criteriaData)) {
      for (SimpleRow row : criteriaData) {
        ecItemInfo.addCriteria(new ArticleCriteria(row.getValue(COL_TCD_CRITERIA_NAME),
            row.getValue(COL_TCD_CRITERIA_VALUE)));
      }
    }

    SqlSelect carTypeQuery = new SqlSelect()
        .addFields(TBL_TCD_TYPES, COL_TCD_MODEL)
        .addFields(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addFields(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addFields(TBL_TCD_TYPES, COL_TCD_TYPE_NAME,
            COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO, COL_TCD_CCM,
            COL_TCD_KW_FROM, COL_TCD_KW_TO, COL_TCD_CYLINDERS, COL_TCD_MAX_WEIGHT,
            COL_TCD_ENGINE, COL_TCD_FUEL, COL_TCD_BODY, COL_TCD_AXLE)
        .addFields(TBL_TCD_TYPE_ARTICLES, COL_TCD_TYPE)
        .addFrom(TBL_TCD_TYPE_ARTICLES)
        .addFromInner(TBL_TCD_TYPES,
            sys.joinTables(TBL_TCD_TYPES, TBL_TCD_TYPE_ARTICLES, COL_TCD_TYPE))
        .addFromInner(TBL_TCD_MODELS, sys.joinTables(TBL_TCD_MODELS, TBL_TCD_TYPES, COL_TCD_MODEL))
        .addFromInner(TBL_TCD_MANUFACTURERS,
            sys.joinTables(TBL_TCD_MANUFACTURERS, TBL_TCD_MODELS, COL_TCD_MANUFACTURER))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TCD_TYPE_ARTICLES, COL_TCD_ARTICLE, articleId),
            SqlUtils.notNull(TBL_TCD_TYPES, COL_TCD_TYPE_VISIBLE),
            SqlUtils.notNull(TBL_TCD_MODELS, COL_TCD_MODEL_VISIBLE),
            SqlUtils.notNull(TBL_TCD_MANUFACTURERS, COL_TCD_MF_VISIBLE)))
        .addOrder(TBL_TCD_MANUFACTURERS, COL_TCD_MANUFACTURER_NAME)
        .addOrder(TBL_TCD_MODELS, COL_TCD_MODEL_NAME)
        .addOrder(TBL_TCD_TYPES, COL_TCD_TYPE_NAME, COL_TCD_PRODUCED_FROM, COL_TCD_PRODUCED_TO,
            COL_TCD_KW_FROM, COL_TCD_KW_TO);

    SimpleRowSet carTypeData = qs.getData(carTypeQuery);
    if (!DataUtils.isEmpty(carTypeData)) {
      for (SimpleRow row : carTypeData) {
        ecItemInfo.addCarType(new EcCarType(row));
      }
    }

    SqlSelect oeNumberQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR)
        .addFrom(TBL_TCD_ARTICLE_CODES)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TCD_ARTICLE_CODES, COL_TCD_ARTICLE, articleId),
            oeNumberCondition))
        .addOrder(TBL_TCD_ARTICLE_CODES, COL_TCD_CODE_NR);

    SimpleRowSet oeNumberData = qs.getData(oeNumberQuery);
    if (!DataUtils.isEmpty(oeNumberData)) {
      for (SimpleRow row : oeNumberData) {
        ecItemInfo.addOeNumber(row.getValue(COL_TCD_CODE_NR));
      }
    }

    return ResponseObject.response(ecItemInfo);
  }

  private List<EcItem> getItems(SqlSelect query, String code) {
    List<EcItem> items = new ArrayList<>();
    Map<Long, Double> featuredDiscountPercents = new HashMap<>();

    String tempArticleIds = createTempArticleIds(query);
    String unitName = "UnitName";

    SqlSelect articleQuery = new SqlSelect()
        .addFields(tempArticleIds, COL_TCD_ARTICLE)
        .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_NR, COL_TCD_BRAND,
            COL_TCD_ARTICLE_DESCRIPTION, COL_TCD_ARTICLE_NOVELTY, COL_TCD_ARTICLE_FEATURED,
            COL_TCD_ARTICLE_FEATURED_PRICE, COL_TCD_ARTICLE_FEATURED_PERCENT)
        .addField(TBL_UNITS, COL_UNIT_NAME, unitName)
        .addFrom(tempArticleIds)
        .addFromInner(TBL_TCD_ARTICLES,
            sys.joinTables(TBL_TCD_ARTICLES, tempArticleIds, COL_TCD_ARTICLE))
        .addFromLeft(TBL_UNITS,
            sys.joinTables(TBL_UNITS, TBL_TCD_ARTICLES, COL_TCD_ARTICLE_UNIT))
        .setWhere(articleCondition);

    SimpleRowSet articleData = qs.getData(articleQuery);
    if (!DataUtils.isEmpty(articleData)) {
      long time = System.currentTimeMillis();

      for (SimpleRow row : articleData) {
        EcItem item = new EcItem(row.getLong(COL_TCD_ARTICLE));

        item.setBrand(row.getLong(COL_TCD_BRAND));
        item.setCode(row.getValue(COL_TCD_ARTICLE_NR));
        item.setName(row.getValue(COL_TCD_ARTICLE_NAME));

        item.setDescription(row.getValue(COL_TCD_ARTICLE_DESCRIPTION));

        Long until = row.getLong(COL_TCD_ARTICLE_NOVELTY);
        if (until != null && until > time) {
          item.setNovelty(true);
        }

        until = row.getLong(COL_TCD_ARTICLE_FEATURED);
        if (until != null && until > time) {
          item.setFeatured(true);

          Double featuredPrice = row.getDouble(COL_TCD_ARTICLE_FEATURED_PRICE);
          if (BeeUtils.isPositive(featuredPrice)) {
            item.setFeaturedPrice(featuredPrice);
          } else {
            Double featuredPercent = row.getDouble(COL_TCD_ARTICLE_FEATURED_PERCENT);
            if (BeeUtils.isPositive(featuredPercent)) {
              featuredDiscountPercents.put(item.getArticleId(), featuredPercent);
            }
          }
        }

        item.setUnit(row.getValue(unitName));
        items.add(item);
      }
    }

    if (!items.isEmpty()) {
      Map<Long, String> articleCategories = getArticleCategories(tempArticleIds);
      ListMultimap<Long, ArticleCriteria> articleCriteria = getArticleCriteria(tempArticleIds);

      Pair<Map<EcSupplier, Long>, Map<EcSupplier, Long>> priceLists = getClientPriceLists();
      Multimap<Long, ArticleSupplier> articleSuppliers =
          getArticleSuppliers(tempArticleIds, null, priceLists.getA(), priceLists.getB());

      Pair<Set<String>, Set<String>> clientWarehouses = getClientWarehouses();
      Set<String> primaryWarehouses = clientWarehouses.getA();
      Set<String> secondaryWarehouses = clientWarehouses.getB();

      Map<Long, Integer> analogCount = countAnalogs(tempArticleIds);

      for (EcItem item : items) {
        long articleId = item.getArticleId();

        item.setCategories(articleCategories.get(articleId));
        item.setCriteria(articleCriteria.get(articleId));

        Collection<ArticleSupplier> itemSuppliers = articleSuppliers.get(articleId);

        if (!itemSuppliers.isEmpty()) {
          item.setSuppliers(itemSuppliers);

          double primaryStock = 0;
          double secondaryStock = 0;

          for (ArticleSupplier itemSupplier : itemSuppliers) {
            if (!primaryWarehouses.isEmpty()) {
              primaryStock += itemSupplier.getStock(primaryWarehouses);
            }
            if (!secondaryWarehouses.isEmpty()) {
              secondaryStock += itemSupplier.getStock(secondaryWarehouses);
            }
          }

          item.setPrimaryStock(primaryStock);
          item.setSecondaryStock(secondaryStock);
        }

        Integer ac = analogCount.get(articleId);
        if (ac != null) {
          item.setAnalogCount(ac);
        }
      }

      setSupplierPrices(items);
      setClientDiscounts(items);

      if (!featuredDiscountPercents.isEmpty()) {
        for (EcItem item : items) {
          Double percent = featuredDiscountPercents.get(item.getArticleId());
          if (percent != null && item.getClientPrice() > 0) {
            item.setFeaturedPrice(BeeUtils.minusPercent(item.getRealClientPrice(), percent));
          }
        }
      }

      if (items.size() > 1) {
        Collections.sort(items, new EcItemComparator(usr.getLocale(), code));
      }
    }

    qs.sqlDropTemp(tempArticleIds);

    return items;
  }

  private ResponseObject getItemsByBrand(Long brand) {
    if (!DataUtils.isId(brand)) {
      return ResponseObject.parameterNotFound(SVC_GET_ITEMS_BY_BRAND, COL_TCD_BRAND);
    }

    SqlSelect articleIdQuery = new SqlSelect()
        .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLES)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TCD_ARTICLES, COL_TCD_BRAND, brand),
            articleCondition));

    List<EcItem> items = getItems(articleIdQuery, null);

    if (items.isEmpty()) {
      return didNotMatch(BeeUtils.toString(brand));
    } else {
      return ResponseObject.response(items).setSize(items.size());
    }
  }

  private ResponseObject getItemsByCarType(Long typeId) {
    if (!DataUtils.isId(typeId)) {
      return ResponseObject.parameterNotFound(SVC_GET_ITEMS_BY_CAR_TYPE, VAR_TYPE);
    }

    SqlSelect articleIdQuery = new SqlSelect()
        .addFields(TBL_TCD_TYPE_ARTICLES, COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_TYPE_ARTICLES)
        .setWhere(SqlUtils.equals(TBL_TCD_TYPE_ARTICLES, COL_TCD_TYPE, typeId));

    List<EcItem> items = getItems(articleIdQuery, null);

    if (items.isEmpty()) {
      return didNotMatch(BeeUtils.toString(typeId));
    } else {
      return ResponseObject.response(items).setSize(items.size());
    }
  }

  private ResponseObject getPictures(Set<Long> articles) {
    if (BeeUtils.isEmpty(articles)) {
      return ResponseObject.parameterNotFound(SVC_GET_PICTURES, COL_TCD_ARTICLE);
    }

    SqlSelect graphicsQuery = new SqlSelect()
        .addFields(TBL_TCD_ARTICLE_GRAPHICS, COL_TCD_ARTICLE)
        .addFields(TBL_TCD_GRAPHICS, COL_TCD_GRAPHICS_TYPE, COL_TCD_GRAPHICS_RESOURCE)
        .addFrom(TBL_TCD_ARTICLE_GRAPHICS)
        .addFromInner(TBL_TCD_GRAPHICS,
            sys.joinTables(TBL_TCD_GRAPHICS, TBL_TCD_ARTICLE_GRAPHICS, COL_TCD_GRAPHICS))
        .setWhere(SqlUtils.inList(TBL_TCD_ARTICLE_GRAPHICS, COL_TCD_ARTICLE, articles))
        .addOrder(TBL_TCD_ARTICLE_GRAPHICS, COL_TCD_ARTICLE, COL_TCD_SORT,
            sys.getIdName(TBL_TCD_ARTICLE_GRAPHICS));

    SimpleRowSet graphicsData = qs.getData(graphicsQuery);

    if (DataUtils.isEmpty(graphicsData)) {
      return ResponseObject.emptyResponse();
    }

    List<String> pictures = Lists.newArrayListWithExpectedSize(graphicsData.getNumberOfRows() * 2);
    for (SimpleRow row : graphicsData) {
      pictures.add(row.getValue(COL_TCD_ARTICLE));
      pictures.add(EcUtils.picture(row.getValue(COL_TCD_GRAPHICS_TYPE),
          row.getValue(COL_TCD_GRAPHICS_RESOURCE)));
    }

    return ResponseObject.response(pictures).setSize(pictures.size() / 2);
  }

  private ResponseObject getPromo(RequestInfo reqInfo) {
    List<RowInfo> cachedBanners = new ArrayList<>();

    String param = reqInfo.getParameter(VAR_BANNERS);
    if (!BeeUtils.isEmpty(param)) {
      String[] arr = Codec.beeDeserializeCollection(param);
      if (arr != null) {
        for (int i = 0; i < arr.length; i++) {
          cachedBanners.add(RowInfo.restore(arr[i]));
        }
      }
    }

    BeeRowSet banners = getBanners(cachedBanners);

    boolean featured = prm.getBoolean(PRM_PROMO_FEATURED);
    boolean novelty = prm.getBoolean(PRM_PROMO_NOVELTY);

    List<EcItem> items;

    if (featured || novelty) {
      long time = System.currentTimeMillis();

      HasConditions where = SqlUtils.or();
      if (featured) {
        where.add(SqlUtils.more(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_FEATURED, time));
      }
      if (novelty) {
        where.add(SqlUtils.more(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NOVELTY, time));
      }

      SqlSelect articleIdQuery = new SqlSelect()
          .addField(TBL_TCD_ARTICLES, sys.getIdName(TBL_TCD_ARTICLES), COL_TCD_ARTICLE)
          .addFrom(TBL_TCD_ARTICLES)
          .setWhere(where);

      items = getItems(articleIdQuery, null);

    } else {
      items = null;
    }

    String a = (banners == null) ? null : Codec.beeSerialize(banners);
    String b = BeeUtils.isEmpty(items) ? null : Codec.beeSerialize(items);

    return ResponseObject.response(Pair.of(a, b));
  }

  private Long getSenderAccountId(Long manager) {
    Long accountId = null;

    if (DataUtils.isId(manager)) {
      accountId = qs.getLong(new SqlSelect()
          .addFields(MailConstants.TBL_ACCOUNTS, sys.getIdName(MailConstants.TBL_ACCOUNTS))
          .addFrom(MailConstants.TBL_ACCOUNTS)
          .addFromInner(TBL_MANAGERS, sys.joinTables(MailConstants.TBL_ACCOUNTS, TBL_MANAGERS,
              COL_MANAGER_MAIL_ACCOUNT))
          .setWhere(sys.idEquals(TBL_MANAGERS, manager)));
    }
    if (!DataUtils.isId(accountId)) {
      accountId = qs.getLong(new SqlSelect()
          .addFields(MailConstants.TBL_ACCOUNTS, sys.getIdName(MailConstants.TBL_ACCOUNTS))
          .addFrom(MailConstants.TBL_ACCOUNTS)
          .addFromInner(TBL_CONFIGURATION, sys.joinTables(MailConstants.TBL_ACCOUNTS,
              TBL_CONFIGURATION, COL_CONFIG_MAIL_ACCOUNT)));
    }
    return accountId;
  }

  private ResponseObject getShoppingCarts() {
    Long client = getCurrentClientId();
    if (client == null) {
      return ResponseObject.emptyResponse();
    }

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_SHOPPING_CARTS, COL_SHOPPING_CART_TYPE, COL_SHOPPING_CART_CREATED,
            COL_SHOPPING_CART_ARTICLE, COL_SHOPPING_CART_QUANTITY)
        .addFrom(TBL_SHOPPING_CARTS)
        .setWhere(SqlUtils.equals(TBL_SHOPPING_CARTS, COL_SHOPPING_CART_CLIENT, client))
        .addOrder(TBL_SHOPPING_CARTS, COL_SHOPPING_CART_TYPE, COL_SHOPPING_CART_CREATED));

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    }

    Set<Long> articles = Sets.newHashSet(data.getLongColumn(COL_SHOPPING_CART_ARTICLE));

    String idName = sys.getIdName(TBL_TCD_ARTICLES);
    SqlSelect articleIdQuery = new SqlSelect()
        .addField(TBL_TCD_ARTICLES, idName, COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLES)
        .setWhere(SqlUtils.inList(TBL_TCD_ARTICLES, idName, articles));

    List<EcItem> ecItems = getItems(articleIdQuery, null);
    if (ecItems.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    List<CartItem> result = new ArrayList<>();

    for (SimpleRow row : data) {
      Long article = row.getLong(COL_SHOPPING_CART_ARTICLE);

      for (EcItem ecItem : ecItems) {
        if (Objects.equals(article, ecItem.getArticleId())) {
          CartItem cartItem = new CartItem(ecItem, row.getInt(COL_SHOPPING_CART_QUANTITY));
          cartItem.setNote(row.getValue(COL_SHOPPING_CART_TYPE));
          result.add(cartItem);
          break;
        }
      }
    }

    return ResponseObject.response(result);
  }

  private ResponseObject isValidFinancialInfo(Long orderId) {
    SimpleRow orderRow = qs.getRow(new SqlSelect()
        .addFields(TBL_ORDERS, COL_CLIENT, COL_ORDER_MANAGER, COL_ORDER_MANAGER_COMMENT)
        .addFrom(TBL_ORDERS)
        .setWhere(SqlUtils.equals(TBL_ORDERS, sys.getIdName(TBL_ORDERS), orderId)));

    ResponseObject finInfoResp = getFinancialInformation(orderRow.getLong(COL_CLIENT));
    if (finInfoResp.hasErrors()) {
      return finInfoResp;
    }

    if (finInfoResp.hasResponse(EcFinInfo.class)) {
      EcFinInfo ecFinInfo = (EcFinInfo) finInfoResp.getResponse();

      double orderTotal = BeeConst.DOUBLE_ZERO;

      for (EcOrder order : ecFinInfo.getOrders()) {
        if (EcOrderStatus.in(order.getStatus(), EcOrderStatus.NEW, EcOrderStatus.ACTIVE)
            && Objects.equals(order.getOrderId(), orderId)) {
          orderTotal += order.getAmount();
        }
      }

      double totalTaken = BeeUtils.unbox(ecFinInfo.getTotalTaken());
      double dept = BeeUtils.unbox(ecFinInfo.getDebt());
      double maxedOut = BeeUtils.unbox(ecFinInfo.getMaxedOut());
      double creditLimit = BeeUtils.unbox(ecFinInfo.getCreditLimit());

      double totalDept = creditLimit - (totalTaken + dept + maxedOut + orderTotal);

      if (BeeUtils.isNegative(totalDept)) {
        String comment = orderRow.getValue(COL_ORDER_MANAGER_COMMENT);
        if (BeeUtils.isEmpty(comment) && !DataUtils.isId(orderRow.getLong(COL_ORDER_MANAGER))) {
          return ResponseObject.error(usr.getDictionary().ecExceededCreditLimit());
        } else {
          return ResponseObject.info(comment);
        }

      } else {
        return ResponseObject.emptyResponse();
      }
    }
    return ResponseObject.error(usr.getDictionary().actionCanNotBeExecuted());
  }

  private void logHistory(String service, String query, Long article, int count, long duration) {
    SqlInsert ins = new SqlInsert(TBL_HISTORY);
    ins.addConstant(COL_HISTORY_DATE, System.currentTimeMillis());
    ins.addConstant(COL_HISTORY_USER, usr.getCurrentUserId());

    ins.addConstant(COL_HISTORY_SERVICE, sys.clampValue(TBL_HISTORY, COL_HISTORY_SERVICE, service));
    if (!BeeUtils.isEmpty(query)) {
      ins.addConstant(COL_HISTORY_QUERY, sys.clampValue(TBL_HISTORY, COL_HISTORY_QUERY, query));
    }
    if (article != null) {
      ins.addConstant(COL_HISTORY_ARTICLE, article);
    }

    ins.addConstant(COL_HISTORY_COUNT, count);
    ins.addConstant(COL_HISTORY_DURATION, duration);

    qs.insertData(ins);
  }

  private ResponseObject mailOrder(Long orderId, boolean isClient) {
    if (!DataUtils.isId(orderId)) {
      return ResponseObject.parameterNotFound(SVC_MAIL_ORDER, VAR_ORDER);
    }

    BeeRowSet orderData = qs.getViewData(VIEW_ORDERS, Filter.compareId(orderId));
    if (DataUtils.isEmpty(orderData)) {
      String msg = BeeUtils.joinWords(SVC_MAIL_ORDER, "order not found:", orderId);
      logger.severe(msg);
      return ResponseObject.error(msg);
    }

    BeeRow orderRow = orderData.getRow(0);

    EcOrderStatus status = EcOrderStatus.get(DataUtils.getInteger(orderData, orderRow,
        COL_ORDER_STATUS));
    Assert.notNull(status);

    Long clientUser = DataUtils.getLong(orderData, orderRow, ALS_ORDER_CLIENT_USER);
    Assert.notNull(clientUser);

    Long manager = DataUtils.getLong(orderData, orderRow, COL_ORDER_MANAGER);

    ResponseObject response = ResponseObject.emptyResponse();

    Set<String> recipients = new HashSet<>();

    String clientEmail = null;
    if (!isClient
        || BeeUtils.isTrue(DataUtils.getBoolean(orderData, orderRow, COL_ORDER_COPY_BY_MAIL))) {
      clientEmail = usr.getUserEmail(clientUser, true);

      if (!BeeUtils.isEmpty(clientEmail)) {
        recipients.add(clientEmail);
      } else {
        response.addWarning(usr.getDictionary().ecMailClientAddressNotFound());
      }
    }

    String incomingEmail = null;
    if (status == EcOrderStatus.NEW) {
      incomingEmail = getIncomingEmail(manager);
      if (incomingEmail != null && incomingEmail.equals(clientEmail)) {
        incomingEmail = null;
      }

      if (!BeeUtils.isEmpty(incomingEmail)) {
        recipients.add(incomingEmail);
      }
    }

    if (recipients.isEmpty()) {
      return response;
    }

    Long account = getSenderAccountId(manager);
    if (!DataUtils.isId(account)) {
      return ResponseObject.warning(usr.getDictionary().ecMailAccountNotFound());
    }

    Dictionary constants = usr.getDictionary(clientUser);
    Assert.notNull(constants);

    Document document = orderToHtml(orderData.getColumns(), orderRow, constants);
    String content = document.buildLines();

    ResponseObject mailResponse = mail.sendMail(account, recipients.toArray(new String[0]), null,
        null, status.getSubject(constants), content, null, false);

    if (mailResponse.hasErrors()) {
      if (isClient) {
        return ResponseObject.warning(usr.getDictionary().ecMailFailed());
      } else {
        return mailResponse;
      }
    }

    logger.info(SVC_MAIL_ORDER, orderId, "sent to", clientEmail, incomingEmail);

    if (isClient && BeeUtils.isEmpty(clientEmail)) {
      return response;
    }

    response.addInfo(usr.getDictionary().ecMailSent());
    if (!BeeUtils.isEmpty(clientEmail)) {
      response.addInfo(clientEmail);
    }
    if (!isClient && !BeeUtils.isEmpty(incomingEmail)) {
      response.addInfo(incomingEmail);
    }

    return response;
  }

  private ResponseObject mailRegistration(Long account, String recipient, String login,
      String password, SupportedLocale locale) {

    String companyName = BeeUtils.trim(prm.getRelationInfo(PRM_COMPANY).getB());
    String url = BeeUtils.trim(prm.getText(PRM_URL));

    Dictionary messages = Localizations.getDictionary(locale);

    String subject = BeeUtils.trim(messages.ecRegistrationMailSubject(companyName));
    String content = BeeUtils.trim(messages.ecRegistrationMailContent(login, password, url));

    ResponseObject response = mail.sendMail(account, recipient, subject, content);
    if (response.hasErrors()) {
      return response;
    }

    response.addInfo(usr.getDictionary().ecMailSent());
    response.addInfo(recipient);

    return response;
  }

  private ResponseObject mergeCategory(Long categoryId, Long parentCategoryId) {
    Assert.noNulls(categoryId, parentCategoryId);

    qs.updateData(new SqlDelete(TBL_TCD_ARTICLE_CATEGORIES)
        .setWhere(SqlUtils
            .and(SqlUtils.equals(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_CATEGORY, parentCategoryId),
                SqlUtils.in(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE,
                    TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_ARTICLE,
                    SqlUtils.equals(TBL_TCD_ARTICLE_CATEGORIES, COL_TCD_CATEGORY, categoryId)))));

    for (String tblName : sys.getTableNames()) {
      for (BeeForeignKey fKey : sys.getTable(tblName).getForeignKeys()) {
        if (BeeUtils.same(fKey.getRefTable(), TBL_TCD_CATEGORIES)
            && fKey.getFields().size() == 1) {

          String tbl = fKey.getTable();
          String fld = fKey.getFields().get(0);

          qs.updateData(new SqlUpdate(tbl)
              .addConstant(fld, parentCategoryId)
              .setWhere(SqlUtils.equals(tbl, fld, categoryId)));
        }
      }
    }
    return qs.updateDataWithResponse(new SqlDelete(TBL_TCD_CATEGORIES)
        .setWhere(sys.idEquals(TBL_TCD_CATEGORIES, categoryId)));
  }

  private Document orderToHtml(List<BeeColumn> orderColumns, BeeRow orderRow,
      Dictionary constants) {

    String clientFirstName = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_CLIENT_FIRST_NAME, orderColumns));
    String clientLastName = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_CLIENT_LAST_NAME, orderColumns));
    String clientCompanyName = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_CLIENT_COMPANY_NAME, orderColumns));

    DateTime date = orderRow.getDateTime(DataUtils.getColumnIndex(COL_ORDER_DATE, orderColumns));
    EcOrderStatus status = EcOrderStatus.get(orderRow.getInteger(DataUtils.getColumnIndex(
        COL_ORDER_STATUS, orderColumns)));

    String managerFirstName = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_MANAGER_FIRST_NAME, orderColumns));
    String managerLastName = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_MANAGER_LAST_NAME, orderColumns));

    String deliveryAddress = orderRow.getString(DataUtils.getColumnIndex(
        COL_ORDER_DELIVERY_ADDRESS, orderColumns));
    String deliveryMethod = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_DELIVERY_METHOD_NAME, orderColumns));

    String clientComment = orderRow.getString(DataUtils.getColumnIndex(
        COL_ORDER_CLIENT_COMMENT, orderColumns));
    String rejectionReason = orderRow.getString(DataUtils.getColumnIndex(
        ALS_ORDER_REJECTION_REASON_NAME, orderColumns));

    SqlSelect itemQuery = new SqlSelect()
        .addFields(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ARTICLE, COL_ORDER_ITEM_QUANTITY_ORDERED,
            COL_ORDER_ITEM_QUANTITY_SUBMIT, COL_ORDER_ITEM_PRICE, COL_ORDER_ITEM_NOTE)
        .addFields(TBL_TCD_ARTICLES, COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_NR,
            COL_TCD_ARTICLE_WEIGHT)
        .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
        .addFields(TBL_UNITS, COL_UNIT_NAME)
        .addFrom(TBL_ORDER_ITEMS)
        .addFromInner(TBL_TCD_ARTICLES,
            sys.joinTables(TBL_TCD_ARTICLES, TBL_ORDER_ITEMS, COL_ORDER_ITEM_ARTICLE))
        .addFromLeft(TBL_TCD_BRANDS,
            sys.joinTables(TBL_TCD_BRANDS, TBL_TCD_ARTICLES, COL_TCD_BRAND))
        .addFromLeft(TBL_UNITS,
            sys.joinTables(TBL_UNITS, TBL_TCD_ARTICLES, COL_TCD_ARTICLE_UNIT))
        .setWhere(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ORDER, orderRow.getId()))
        .addOrder(TBL_ORDER_ITEMS, sys.getIdName(TBL_ORDER_ITEMS));

    SimpleRowSet itemData = qs.getData(itemQuery);

    double totalAmount = BeeConst.DOUBLE_ZERO;
    double totalWeight = BeeConst.DOUBLE_ZERO;

    if (!DataUtils.isEmpty(itemData)) {
      for (SimpleRow itemRow : itemData) {
        Integer quantity = itemRow.getInt(COL_ORDER_ITEM_QUANTITY_SUBMIT);
        Double price = itemRow.getDouble(COL_ORDER_ITEM_PRICE);
        Double weight = itemRow.getDouble(COL_TCD_ARTICLE_WEIGHT);

        totalAmount += BeeUtils.unbox(quantity) * BeeUtils.unbox(price);
        totalWeight += BeeUtils.unbox(quantity) * BeeUtils.unbox(weight);
      }
    }

    BeeRowSet eventData = qs.getViewData(VIEW_ORDER_EVENTS,
        Filter.equals(COL_ORDER_EVENT_ORDER, orderRow.getId()),
        new Order(COL_ORDER_EVENT_DATE, true));

    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(constants.ecOrder()));

    Div panel = div().backgroundColor(Colors.WHITESMOKE);
    doc.getBody().append(panel);

    String customer = EcUtils.formatPerson(clientFirstName, clientLastName, clientCompanyName);
    panel.append(h3().text(customer));

    Tbody fields = tbody().append(
        tr().append(
            td().text(constants.ecOrderSubmissionDate()),
            td().text(TimeUtils.renderCompact(date)),
            td().text(constants.ecOrderNumber()),
            td().text(BeeUtils.toString(orderRow.getId()))));

    Tr tr = tr().append(
        td().text(constants.ecOrderStatus()),
        td().text((status == null) ? null : status.getCaption(constants)));
    if (EcOrderStatus.REJECTED == status) {
      tr.append(td().text(constants.ecRejectionReason()),
          td().text(rejectionReason));
    }
    fields.append(tr);

    if (!DataUtils.isEmpty(eventData)) {
      for (BeeRow eventRow : eventData.getRows()) {
        EcOrderStatus eventStatus = EcOrderStatus.get(DataUtils.getInteger(eventData, eventRow,
            COL_ORDER_EVENT_STATUS));
        DateTime eventDate = DataUtils.getDateTime(eventData, eventRow, COL_ORDER_EVENT_DATE);

        if (eventStatus != null && eventDate != null) {
          fields.append(tr().append(
              td().text(eventStatus.getCaption(constants)),
              td().text(TimeUtils.renderCompact(eventDate))));
        }
      }
    }

    fields.append(
        tr().append(
            td().text(constants.ecDeliveryMethod()),
            td().text(deliveryMethod),
            td().text(constants.ecDeliveryAddress()),
            td().text(deliveryAddress)),
        tr().append(
            td().text(constants.ecManager()),
            td().text(BeeUtils.joinWords(managerFirstName, managerLastName)),
            td().text(constants.comment()),
            td().text(clientComment)),
        tr().append(
            td().text(BeeUtils.joinWords(constants.ecOrderAmount(), CURRENCY)),
            td().text(EcUtils.formatCents(EcUtils.toCents(totalAmount))).alignRight()
                .fontWeight(FontWeight.BOLD).fontSize(FontSize.LARGER),
            td().text(BeeUtils.joinWords(constants.weight(), WEIGHT_UNIT)),
            td().text(BeeUtils.toString(totalWeight, WEIGHT_SCALE)).alignRight()));

    List<Element> cells = fields.queryTag(Tags.TD);
    for (Element cell : cells) {
      cell.setPaddingLeft(10, CssUnit.PX);
      cell.setPaddingRight(10, CssUnit.PX);

      cell.setPaddingTop(3, CssUnit.PX);
      cell.setPaddingBottom(3, CssUnit.PX);

      int index = cell.index();
      if (index % 2 == 0) {
        cell.setTextAlign(TextAlign.RIGHT);

      } else {
        cell.setMinWidth(120, CssUnit.PX);
        cell.setMaxWidth(200, CssUnit.PX);

        cell.setBorderWidth(1, CssUnit.PX);
        cell.setBorderStyle(BorderStyle.SOLID);
        cell.setBorderColor("#ccc");

        cell.setWhiteSpace(WhiteSpace.PRE_LINE);
        cell.setBackground(Colors.WHITE);

        Td td = (Td) cell;
        if (td.size() == 1 && td.hasText()) {
          Text textNode = (Text) td.getFirstChild();
          if (textNode.isEmpty()) {
            textNode.setText(BeeConst.STRING_MINUS);
          }
        }
      }
    }

    panel.append(table().append(fields));

    if (!DataUtils.isEmpty(itemData)) {
      panel.append(div()
          .fontWeight(FontWeight.BOLDER)
          .marginTop(2, CssUnit.EX)
          .marginBottom(1, CssUnit.EX)
          .text(BeeUtils.joinWords(constants.ecOrderItems(),
              BeeUtils.bracket(itemData.getNumberOfRows()))));

      Tbody items = tbody().append(tr().textAlign(TextAlign.CENTER).fontWeight(FontWeight.BOLDER)
          .append(
              td().text(constants.ecItemName()),
              td().text(constants.ecItemBrand()),
              td().text(constants.ecItemCode()),
              td().text(constants.ecItemWeight()),
              td().text(constants.ecItemQuantityOrdered()),
              td().text(constants.ecItemQuantity()),
              td().text(constants.ecItemPrice()),
              td().text(constants.total()),
              td().text(constants.ecItemNote())));

      int i = 0;
      for (SimpleRow itemRow : itemData) {
        String rowBackground = (i % 2 == 1) ? "#f5f5f5" : "#ebebeb";

        int ordered = BeeUtils.unbox(itemRow.getInt(COL_ORDER_ITEM_QUANTITY_ORDERED));
        int quantity = BeeUtils.unbox(itemRow.getInt(COL_ORDER_ITEM_QUANTITY_SUBMIT));
        double price = BeeUtils.unbox(itemRow.getDouble(COL_ORDER_ITEM_PRICE));

        String quantityColor;
        if (quantity > ordered) {
          quantityColor = Colors.GREEN;
        } else if (quantity < ordered) {
          quantityColor = Colors.RED;
        } else {
          quantityColor = null;
        }

        items.append(tr().backgroundColor(rowBackground).append(
            td().text(itemRow.getValue(COL_TCD_ARTICLE_NAME)),
            td().text(itemRow.getValue(COL_TCD_BRAND_NAME)),
            td().text(itemRow.getValue(COL_TCD_ARTICLE_NR)),
            td().text(itemRow.getValue(COL_TCD_ARTICLE_WEIGHT)).alignRight(),
            td().text(EcUtils.format(ordered)).alignRight(),
            td().text(EcUtils.format(quantity)).alignRight()
                .fontWeight(FontWeight.BOLDER).color(quantityColor),
            td().text(EcUtils.formatCents(EcUtils.toCents(price))).alignRight(),
            td().text(EcUtils.formatCents(EcUtils.toCents(quantity * price))).alignRight(),
            td().text(itemRow.getValue(COL_ORDER_ITEM_NOTE))));

        i++;
      }

      cells = items.queryTag(Tags.TD);
      for (Element cell : cells) {
        cell.setPaddingLeft(10, CssUnit.PX);
        cell.setPaddingRight(10, CssUnit.PX);

        cell.setPaddingTop(3, CssUnit.PX);
        cell.setPaddingBottom(3, CssUnit.PX);

        cell.setBorderWidth(1, CssUnit.PX);
        cell.setBorderStyle(BorderStyle.SOLID);
        cell.setBorderColor("#ddd");
      }

      panel.append(table().borderCollapse().marginLeft(1, CssUnit.EM).append(items));
    }

    return doc;
  }

  private ResponseObject registerOrderEvent(Long orderId, EcOrderStatus status) {
    return qs.insertDataWithResponse(new SqlInsert(TBL_ORDER_EVENTS)
        .addConstant(COL_ORDER_EVENT_ORDER, orderId)
        .addConstant(COL_ORDER_EVENT_DATE, TimeUtils.nowMinutes().getTime())
        .addConstant(COL_ORDER_EVENT_STATUS, status.ordinal())
        .addConstant(COL_ORDER_EVENT_USER, usr.getCurrentUserId()));
  }

  private ResponseObject registerOrderEvent(RequestInfo reqInfo) {
    Long orderId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_ORDER));
    EcOrderStatus status =
        EcOrderStatus.get(BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_STATUS)));

    if (!DataUtils.isId(orderId)) {
      return ResponseObject.parameterNotFound(SVC_REGISTER_ORDER_EVENT, VAR_ORDER);
    }
    if (status == null) {
      return ResponseObject.parameterNotFound(SVC_REGISTER_ORDER_EVENT, VAR_STATUS);
    }

    return registerOrderEvent(orderId, status);
  }

  private ResponseObject saveConfiguration(RequestInfo reqInfo) {
    String column = reqInfo.getParameter(Service.VAR_COLUMN);
    if (BeeUtils.isEmpty(column)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_CONFIGURATION, Service.VAR_COLUMN);
    }

    String value = reqInfo.getParameter(Service.VAR_VALUE);
    if (BeeUtils.isEmpty(value)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_CONFIGURATION, Service.VAR_VALUE);
    }

    if (updateConfiguration(column, value)) {
      return ResponseObject.response(column);
    } else {
      String message = BeeUtils.joinWords(SVC_SAVE_CONFIGURATION, column,
          "cannot save configuration");
      logger.severe(message);
      return ResponseObject.error(message);
    }
  }

  private ResponseObject searchByItemCode(String code, IsCondition clause, Operator defOperator) {
    if (BeeUtils.isEmpty(code)) {
      return ResponseObject.parameterNotFound(SVC_SEARCH_BY_ITEM_CODE, VAR_QUERY);
    }

    IsCondition codeCondition = getCodeCondition(code, defOperator);
    if (codeCondition == null) {
      return ResponseObject.error(EcUtils.normalizeCode(code),
          usr.getDictionary().searchQueryRestriction(MIN_SEARCH_QUERY_LENGTH));
    }

    SqlSelect articleIdQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TCD_ARTICLE_CODES, COL_TCD_ARTICLE)
        .addFrom(TBL_TCD_ARTICLE_CODES)
        .setWhere((clause == null) ? codeCondition : SqlUtils.and(codeCondition, clause));

    List<EcItem> items = getItems(articleIdQuery, code);
    if (items.isEmpty()) {
      return didNotMatch(code);
    } else {
      return ResponseObject.response(items).setSize(items.size());
    }
  }

  private ResponseObject searchByOeNumber(String code) {
    if (BeeUtils.isEmpty(code)) {
      return ResponseObject.parameterNotFound(SVC_SEARCH_BY_OE_NUMBER, VAR_QUERY);
    }
    return searchByItemCode(code, oeNumberCondition, Operator.CONTAINS);
  }

  private ResponseObject sendToERP(RequestInfo reqInfo) {
    Long orderId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_ORDER));
    if (!DataUtils.isId(orderId)) {
      return ResponseObject.parameterNotFound(SVC_SEND_TO_ERP, VAR_ORDER);
    }

    boolean sendMail = reqInfo.hasParameter(VAR_MAIL);

    ResponseObject finResp = isValidFinancialInfo(orderId);
    if (finResp.hasErrors()) {
      return finResp;
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_ORDER_ITEMS, COL_TCD_ARTICLE)
        .addMax(TBL_ORDER_ITEMS, COL_ORDER_ITEM_PRICE)
        .addSum(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_SUBMIT)
        .addFrom(TBL_ORDER_ITEMS)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_ITEMS, COL_ORDER_ITEM_ORDER, orderId),
            SqlUtils.positive(TBL_ORDER_ITEMS, COL_ORDER_ITEM_QUANTITY_SUBMIT)))
        .addGroup(TBL_ORDER_ITEMS, COL_TCD_ARTICLE);

    List<EcItem> items = getItems(query, null);
    if (items.isEmpty()) {
      return ResponseObject.error(usr.getDictionary().ecNothingToOrder());
    }
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    ResponseObject response = ResponseObject.emptyResponse();

    if (!BeeUtils.same(remoteAddress, BeeConst.STRING_MINUS)) {
      SimpleRow order = qs.getRow(new SqlSelect()
          .addFields(TBL_ORDERS, COL_ORDER_NUMBER)
          .addFields(TBL_WAREHOUSES, COL_WAREHOUSE_SUPPLIER_CODE)
          .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY)
          .addFields(TBL_COMPANIES, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE,
              sys.getIdName(TBL_COMPANIES))
          .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX)
          .addField(TBL_CITIES, COL_CITY_NAME, COL_CITY)
          .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COUNTRY)
          .addFrom(TBL_ORDERS)
          .addFromLeft(TBL_CLIENTS, sys.joinTables(TBL_CLIENTS, TBL_ORDERS, COL_ORDER_CLIENT))
          .addFromLeft(TBL_BRANCHES,
              sys.joinTables(TBL_BRANCHES, TBL_CLIENTS, COL_CLIENT_PRIMARY_BRANCH))
          .addFromLeft(TBL_WAREHOUSES,
              sys.joinTables(TBL_WAREHOUSES, TBL_BRANCHES, COL_BRANCH_PRIMARY_WAREHOUSE))
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_CLIENTS, COL_CLIENT_USER))
          .addFromLeft(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
          .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
          .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
          .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
          .setWhere(SqlUtils.equals(TBL_ORDERS, sys.getIdName(TBL_ORDERS), orderId)));

      String company = null;

      try {
        company = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
            .importClient(order.getValue(sys.getIdName(TBL_COMPANIES)), order.getValue(COL_COMPANY),
                order.getValue(COL_COMPANY_CODE), order.getValue(COL_COMPANY_VAT_CODE),
                order.getValue(COL_ADDRESS), order.getValue(COL_POST_INDEX),
                order.getValue(COL_CITY), order.getValue(COL_COUNTRY));
      } catch (BeeException e) {
        response = response.addError(e);
      }
      if (!response.hasErrors()) {
        String warehouse = BeeUtils.notEmpty(order.getValue(COL_WAREHOUSE_SUPPLIER_CODE),
            prm.getText("ERPWarehouse"));

        WSDocument doc = new WSDocument(BeeUtils.toString(orderId), TimeUtils.nowSeconds(),
            prm.getText("ERPOperation"), company, warehouse);

        SimpleRowSet data = qs.getData(query);

        for (EcItem item : items) {
          String id = null;

          for (ArticleSupplier supplier : item.getSuppliers()) {
            if (EcSupplier.EOLTAS == supplier.getSupplier()) {
              id = supplier.getSupplierId();
              break;
            }
          }
          if (BeeUtils.isEmpty(id)) {
            String brandName = qs.getValue(new SqlSelect()
                .addFields(TBL_TCD_BRANDS, COL_TCD_BRAND_NAME)
                .addFrom(TBL_TCD_BRANDS)
                .setWhere(sys.idEquals(TBL_TCD_BRANDS, item.getBrand())));
            try {
              id = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
                  .importItem(item.getName(), brandName, item.getCode());
            } catch (BeeException e) {
              response.addError(e);
              break;
            }
            if (!BeeUtils.isEmpty(id)) {
              double cost = BeeConst.DOUBLE_ZERO;

              for (ArticleSupplier supplier : item.getSuppliers()) {
                cost = Math.max(cost, supplier.getRealCost());
              }
              qs.insertData(new SqlInsert(TBL_TCD_ARTICLE_SUPPLIERS)
                  .addConstant(COL_TCD_ARTICLE, item.getArticleId())
                  .addConstant(COL_TCD_COST, cost)
                  .addConstant(COL_TCD_SUPPLIER, EcSupplier.EOLTAS.ordinal())
                  .addConstant(COL_TCD_SUPPLIER_ID, id));
            }
          }
          if (!BeeUtils.isEmpty(id)) {
            String article = BeeUtils.toString(item.getArticleId());

            WSDocumentItem docItem = doc.addItem(id,
                data.getValueByKey(COL_TCD_ARTICLE, article, COL_ORDER_ITEM_QUANTITY_SUBMIT));

            docItem.setPrice(data.getValueByKey(COL_TCD_ARTICLE, article, COL_ORDER_ITEM_PRICE));
            docItem.setVat(prm.getValue(PRM_VAT_PERCENT), true, false);
          }
        }
        if (!response.hasErrors()) {
          try {
            ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
                .importDoc(doc);
          } catch (BeeException e) {
            response.addError(e);
          }
        }
      }
    }
    if (response.hasErrors()) {
      response.log(logger);
    } else {
      if (finResp.hasNotifications()) {
        response.addInfo(usr.getDictionary().ecExceededCreditLimitSend());
        response.addMessagesFrom(finResp);
      }
      qs.updateData(new SqlUpdate(TBL_ORDERS)
          .addConstant(COL_ORDER_STATUS, EcOrderStatus.ACTIVE.ordinal())
          .setWhere(SqlUtils.equals(TBL_ORDERS, sys.getIdName(TBL_ORDERS), orderId)));

      registerOrderEvent(orderId, EcOrderStatus.ACTIVE);
      addToUnsuppliedItems(orderId);

      if (sendMail) {
        ResponseObject mailResponse = mailOrder(orderId, false);
        response.addMessagesFrom(mailResponse);
      }
    }
    return response;
  }

  private void setClientDiscounts(List<EcItem> items) {
    long start = System.currentTimeMillis();
    EcClientDiscounts clientDiscounts = getClientDiscounts();

    long watch = System.currentTimeMillis();

    if (clientDiscounts != null && !clientDiscounts.isEmpty()) {
      Map<Long, Long> categoryParents;
      if (clientDiscounts.hasCategories()) {
        categoryParents = getCategoryParents();
      } else {
        categoryParents = new HashMap<>();
      }

      for (EcItem item : items) {
        clientDiscounts.applyTo(item, categoryParents);
      }
    }

    long end = System.currentTimeMillis();
    logger.debug("price", watch - start, "+", end - watch, "=", end - start);
  }

  private void setSupplierPrices(List<EcItem> items) {
    long start = System.currentTimeMillis();

    SqlSelect defQuery = new SqlSelect();
    defQuery.addFrom(TBL_CONFIGURATION);
    defQuery.addFields(TBL_CONFIGURATION, COL_CONFIG_MARGIN_DEFAULT_PERCENT);

    Double defMargin = qs.getDouble(defQuery);

    Map<Long, Long> catParents = new HashMap<>();
    Map<Long, Long> catRoots = new HashMap<>();
    Map<Long, Double> catMargins = new HashMap<>();

    String colCategoryId = sys.getIdName(TBL_TCD_CATEGORIES);

    SqlSelect catQuery = new SqlSelect();
    catQuery.addFrom(TBL_TCD_CATEGORIES);
    catQuery.addFields(TBL_TCD_CATEGORIES, colCategoryId, COL_TCD_CATEGORY_PARENT,
        COL_TCD_CATEGORY_MARGIN_PERCENT);

    SimpleRowSet catData = qs.getData(catQuery);
    if (!DataUtils.isEmpty(catData)) {
      for (SimpleRow row : catData) {
        Long id = row.getLong(colCategoryId);

        Long parent = row.getLong(COL_TCD_CATEGORY_PARENT);
        if (parent == null) {
          catRoots.put(id, id);
        } else {
          catParents.put(id, parent);
        }

        Double percent = row.getDouble(COL_TCD_CATEGORY_MARGIN_PERCENT);
        if (percent != null) {
          catMargins.put(id, percent);
        }
      }

      ImmutableSet<Long> roots = ImmutableSet.copyOf(catRoots.values());
      ImmutableSet<Long> categories = ImmutableSet.copyOf(catParents.keySet());

      for (Long category : categories) {
        Long parent = catParents.get(category);

        while (!roots.contains(parent)) {
          parent = catParents.get(parent);
        }

        catRoots.put(category, parent);
      }
    }

    SimpleRow clientInfo = getCurrentClientInfo(COL_CLIENT_DISPLAYED_PRICE);

    EcDisplayedPrice displayedPrice;
    if (clientInfo == null) {
      displayedPrice = null;
    } else {
      displayedPrice = EnumUtils.getEnumByIndex(EcDisplayedPrice.class,
          clientInfo.getInt(COL_CLIENT_DISPLAYED_PRICE));
    }

    for (EcItem item : items) {
      Double marginPercent = getMarginPercent(item.getCategorySet(), catParents, catRoots,
          catMargins);
      if (marginPercent == null) {
        marginPercent = defMargin;
      }

      Pair<EcSupplier, Integer> supplierListPrice =
          item.getSupplierPrice(displayedPrice, true, marginPercent);
      Pair<EcSupplier, Integer> supplierClientPrice =
          item.getSupplierPrice(displayedPrice, false, marginPercent);

      int listPrice = (supplierListPrice == null) ? 0 : supplierListPrice.getB();

      EcSupplier priceSupplier = (supplierClientPrice == null) ? null : supplierClientPrice.getA();
      int clientPrice = (supplierClientPrice == null) ? 0 : supplierClientPrice.getB();

      if (listPrice <= 0) {
        listPrice = clientPrice;

      } else if (clientPrice <= 0 && supplierListPrice != null) {
        priceSupplier = supplierListPrice.getA();
        clientPrice = listPrice;
      }

      item.setListPrice(listPrice);

      item.setPriceSupplier(priceSupplier);
      item.setClientPrice(clientPrice);
    }

    logger.debug("supplier prices", items.size(), catMargins.size(),
        TimeUtils.elapsedMillis(start));
  }

  private ResponseObject submitOrder(RequestInfo reqInfo) {
    String serializedCart = reqInfo.getParameter(VAR_CART);
    if (BeeUtils.isEmpty(serializedCart)) {
      return ResponseObject.parameterNotFound(SVC_SUBMIT_ORDER, VAR_CART);
    }

    boolean copyByMail = reqInfo.hasParameter(VAR_MAIL);

    Cart cart = Cart.restore(serializedCart);
    if (cart == null || cart.isEmpty()) {
      String message = BeeUtils.joinWords(SVC_SUBMIT_ORDER, "cart deserialization failed");
      logger.severe(message);
      return ResponseObject.error(message);
    }

    String colClientId = sys.getIdName(TBL_CLIENTS);
    SimpleRow clientInfo = getCurrentClientInfo(colClientId, COL_CLIENT_MANAGER);
    if (clientInfo == null) {
      String message = BeeUtils.joinWords(SVC_SUBMIT_ORDER, "client not found for user",
          usr.getCurrentUserId());
      logger.severe(message);
      return ResponseObject.error(message);
    }

    SqlInsert insOrder = new SqlInsert(TBL_ORDERS);

    insOrder.addConstant(COL_ORDER_DATE, TimeUtils.nowMinutes().getTime());
    insOrder.addConstant(COL_ORDER_STATUS, EcOrderStatus.NEW.ordinal());

    insOrder.addConstant(COL_ORDER_CLIENT, clientInfo.getLong(colClientId));
    Long manager = clientInfo.getLong(COL_CLIENT_MANAGER);
    if (manager != null) {
      insOrder.addConstant(COL_ORDER_MANAGER, manager);
    }

    insOrder.addConstant(COL_ORDER_DELIVERY_METHOD, cart.getDeliveryMethod());
    if (!BeeUtils.isEmpty(cart.getDeliveryAddress())) {
      insOrder.addConstant(COL_ORDER_DELIVERY_ADDRESS, cart.getDeliveryAddress());
    }

    if (copyByMail) {
      insOrder.addConstant(COL_ORDER_COPY_BY_MAIL, copyByMail);
    }

    if (!BeeUtils.isEmpty(cart.getComment())) {
      insOrder.addConstant(COL_ORDER_CLIENT_COMMENT, cart.getComment());
    }

    ResponseObject response = qs.insertDataWithResponse(insOrder);
    if (response.hasErrors() || !response.hasResponse(Long.class)) {
      return response;
    }

    Long orderId = (Long) response.getResponse();

    for (CartItem cartItem : cart.getItems()) {
      SqlInsert insItem = new SqlInsert(TBL_ORDER_ITEMS);

      insItem.addConstant(COL_ORDER_ITEM_ORDER, orderId);
      insItem.addConstant(COL_ORDER_ITEM_ARTICLE, cartItem.getEcItem().getArticleId());

      insItem.addConstant(COL_ORDER_ITEM_QUANTITY_ORDERED, cartItem.getQuantity());
      insItem.addConstant(COL_ORDER_ITEM_QUANTITY_SUBMIT, cartItem.getQuantity());

      insItem.addConstant(COL_ORDER_ITEM_PRICE, cartItem.getEcItem().getRealPrice());

      ResponseObject itemResponse = qs.insertDataWithResponse(insItem);
      if (itemResponse.hasErrors()) {
        return itemResponse;
      }
    }

    Integer cartType = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_SHOPPING_CART_TYPE));
    if (cartType != null) {
      qs.updateData(new SqlDelete(TBL_SHOPPING_CARTS)
          .setWhere(SqlUtils.equals(TBL_SHOPPING_CARTS,
              COL_SHOPPING_CART_CLIENT, clientInfo.getLong(colClientId),
              COL_SHOPPING_CART_TYPE, cartType)));
    }

    ResponseObject mailResponse = mailOrder(orderId, true);
    response.addMessagesFrom(mailResponse);

    return response;
  }

  private boolean updateConfiguration(String column, String value) {
    BeeRowSet rowSet = qs.getViewData(VIEW_CONFIGURATION);

    if (DataUtils.isEmpty(rowSet)) {
      if (BeeUtils.isEmpty(value)) {
        return true;
      } else {
        SqlInsert ins = new SqlInsert(TBL_CONFIGURATION).addConstant(column, value);

        ResponseObject response = qs.insertDataWithResponse(ins);
        return !response.hasErrors();
      }

    } else {
      String oldValue = rowSet.getString(0, column);
      if (BeeUtils.equalsTrimRight(value, oldValue)) {
        return true;
      } else {
        SqlUpdate upd = new SqlUpdate(TBL_CONFIGURATION).addConstant(column, value);
        upd.setWhere(SqlUtils.equals(TBL_CONFIGURATION, COL_CONFIG_ID, rowSet.getRow(0).getId()));

        ResponseObject response = qs.updateDataWithResponse(upd);
        return !response.hasErrors();
      }
    }
  }

  private ResponseObject updateCosts(Set<Long> ids) {
    int c = 0;

    if (!BeeUtils.isEmpty(ids)) {
      c = qs.updateData(new SqlUpdate(TBL_TCD_ARTICLE_SUPPLIERS)
          .addExpression(COL_TCD_COST, SqlUtils.name(COL_TCD_UPDATED_COST))
          .setWhere(SqlUtils.inList(TBL_TCD_ARTICLE_SUPPLIERS,
              sys.getIdName(TBL_TCD_ARTICLE_SUPPLIERS), ids)));
    }
    return ResponseObject.info(usr.getDictionary().rowsUpdated(c));
  }

  private ResponseObject updateShoppingCart(RequestInfo reqInfo) {
    Integer cartType = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_SHOPPING_CART_TYPE));
    if (cartType == null) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_SHOPPING_CART, COL_SHOPPING_CART_TYPE);
    }

    Long article = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SHOPPING_CART_ARTICLE));
    if (!DataUtils.isId(article)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_SHOPPING_CART, COL_SHOPPING_CART_ARTICLE);
    }

    Integer quantity = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_SHOPPING_CART_QUANTITY));
    if (quantity == null) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_SHOPPING_CART, COL_SHOPPING_CART_QUANTITY);
    }

    Long client = getCurrentClientId();
    if (!DataUtils.isId(client)) {
      return ResponseObject.emptyResponse();
    }

    IsCondition where = SqlUtils.equals(TBL_SHOPPING_CARTS, COL_SHOPPING_CART_CLIENT, client,
        COL_SHOPPING_CART_TYPE, cartType, COL_SHOPPING_CART_ARTICLE, article);

    if (BeeUtils.isPositive(quantity)) {
      if (qs.sqlExists(TBL_SHOPPING_CARTS, where)) {
        qs.updateData(new SqlUpdate(TBL_SHOPPING_CARTS)
            .addConstant(COL_SHOPPING_CART_QUANTITY, quantity)
            .setWhere(where));
      } else {
        qs.insertData(new SqlInsert(TBL_SHOPPING_CARTS)
            .addConstant(COL_SHOPPING_CART_CREATED, System.currentTimeMillis())
            .addConstant(COL_SHOPPING_CART_CLIENT, client)
            .addConstant(COL_SHOPPING_CART_TYPE, cartType)
            .addConstant(COL_SHOPPING_CART_ARTICLE, article)
            .addConstant(COL_SHOPPING_CART_QUANTITY, quantity));
      }

    } else {
      qs.updateData(new SqlDelete(TBL_SHOPPING_CARTS).setWhere(where));
    }

    return ResponseObject.response(article);
  }

  private ResponseObject uploadBanners(RequestInfo reqInfo) {
    String picture = reqInfo.getParameter(COL_BANNER_PICTURE);
    if (BeeUtils.isEmpty(picture)) {
      return ResponseObject.parameterNotFound(SVC_UPLOAD_BANNERS, COL_BANNER_PICTURE);
    }

    return qs.insertDataWithResponse(new SqlInsert(TBL_BANNERS)
        .addConstant(COL_BANNER_PICTURE, picture));
  }

  private ResponseObject uploadGraphics(RequestInfo reqInfo) {
    Long article = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TCD_ARTICLE));
    Integer sort = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_TCD_SORT));

    String resource = reqInfo.getParameter(COL_TCD_GRAPHICS_RESOURCE);

    if (!DataUtils.isId(article)) {
      return ResponseObject.parameterNotFound(SVC_UPLOAD_GRAPHICS, COL_TCD_ARTICLE);
    }
    if (sort == null) {
      return ResponseObject.parameterNotFound(SVC_UPLOAD_GRAPHICS, COL_TCD_SORT);
    }
    if (BeeUtils.isEmpty(resource)) {
      return ResponseObject.parameterNotFound(SVC_UPLOAD_GRAPHICS, COL_TCD_GRAPHICS_RESOURCE);
    }

    ResponseObject response = qs.insertDataWithResponse(new SqlInsert(TBL_TCD_GRAPHICS)
        .addConstant(COL_TCD_GRAPHICS_RESOURCE, resource));
    if (response.hasErrors()) {
      return response;
    }

    return qs.insertDataWithResponse(new SqlInsert(TBL_TCD_ARTICLE_GRAPHICS)
        .addConstant(COL_TCD_ARTICLE, article)
        .addConstant(COL_TCD_SORT, sort)
        .addConstant(COL_TCD_GRAPHICS, response.getResponse()));
  }
}
