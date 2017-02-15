package com.butent.bee.server.data;

import com.google.common.base.Splitter;

import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class SearchBean {

  @EJB
  SystemBean sys;
  @EJB
  ModuleHolderBean mh;
  @EJB
  UserServiceBean usr;

  private static final BeeLogger logger = LogUtils.getLogger(SearchBean.class);

  /**
   * Builds search SQL builder compatible filter by query using search operators.
   * 
   * @param viewName view name of system where to search
   * @param columns set of view columns where to search
   * @param query search phrases or operations used to search
   * 
   * @return SQL builder compatible Filter object
   * 
   *         <p>
   *         {@code query} can consist words or digits separated in spaces or search operators ("" -
   *         exact string search, or * - any string search)
   *         </p>
   *         <p>
   * 
   *         Examples: <br />
   *         {@code String viewName = "Tasks"; } <br />
   *         {@code Set<String> columns = Sets.newHashSet("Id", // ID column type of view } <br />
   *         {@code "Subject", "OwnerFirstName", "OwnerLastName" // String columns of view} <br />
   *         {@code ); } <br />
   *         </p>
   *         <p>
   *         {@code String query = "System supp" // search only matched strings  } <br />
   *         {@code Filter f = buildSearchFilter(viewName, columns, query); } <br />
   *         </p>
   *         <p>
   * 
   *         SQL builder f filter query like be: (Tasks.Subject LIKE '%System%' OR
   *         Tasks.OwnerFirstName LIKE '%System%' OR Tasks.OwnerLastName LIKE '%System%') OR
   *         (Tasks.Subject LIKE '%supp%' OR Tasks.OwnerFirstName LIKE '%supp%' OR
   *         Tasks.OwnerLastName LIKE '%supp%')
   *         </p>
   *         <p>
   *         {@code query = "System 109" // search only matched strings or numbers } <br />
   *         {@code f = buildSearchFilter(viewName, columns, query); } <br />
   *         </p>
   *         <p>
   * 
   *         SQL builder f filter query like be: (Tasks.Subject LIKE '%System%' OR
   *         Tasks.OwnerFirstName LIKE '%System%' OR Tasks.OwnerLastName LIKE '%System%') OR
   *         (Tasks.Id = 109 Tasks.Subject LIKE '%109%' OR Tasks.OwnerFirstName LIKE '%109%' OR
   *         Tasks.OwnerLastName LIKE '%109%')
   *         </p>
   *         <p>
   *         {@code query = "\"System 109"\" // search only exact string } <br />
   *         {@code f = buildSearchFilter(viewName, columns, query); } <br />
   *         </p>
   *         <p>
   * 
   *         SQL builder f filter query like be: (Tasks.Subject LIKE 'System 109' OR
   *         Tasks.OwnerFirstName LIKE 'System 109' OR Tasks.OwnerLastName LIKE 'System 109')
   * 
   *         </p>
   */
  public Filter buildSearchFilter(String viewName, Set<String> columns, String query) {

    Filter filter = null;
    DataInfo viewData = sys.getDataInfo(viewName);

    if (viewData == null) {
      logger.error(null, viewName, "view data not found");
      return filter;
    }

    List<String> phrases = parseQuery(query);
    DateOrdering dateOrdering = usr.getDateOrdering();

    for (String part : phrases) {
      if (BeeUtils.isEmpty(part)) {
        continue;
      }

      Filter sub = null;
      for (String column : columns) {
        IsColumn col = viewData.getColumn(column);

        if (col == null) {
          logger.warning(column, "column data not found");
          continue;
        }

        Operator op = Operator.MATCHES;

        switch (col.getType()) {
          case LONG:
            if (BeeUtils.isLong(part)) {
              if (BeeUtils.same(viewData.getIdColumn(), col.getId())) {
                sub = Filter.compareId(BeeUtils.toLong(part));
              } else {
                sub = Filter.or(sub, Filter.equals(column, BeeUtils.toLongOrNull(part)));
              }
            }
            break;
          case INTEGER:
            if (BeeUtils.isInt(part)) {
              sub = Filter.or(sub, Filter.equals(column, BeeUtils.toIntOrNull(part)));
            }
            break;
          case DECIMAL:
          case NUMBER:
            if (BeeUtils.isDouble(part)) {
              sub = Filter.or(sub, Filter.equals(column, part));
            }
            break;
          case DATE_TIME:
            DateTime dt = TimeUtils.parseDateTime(part, dateOrdering);
            if (dt != null) {
              sub = Filter.or(sub, Filter.equals(column, dt));
            }
            break;
          case DATE:
            JustDate date = TimeUtils.parseDate(part, dateOrdering);
            if (date != null) {
              sub = Filter.or(sub, Filter.equals(column, date));
            }
            break;
          case TEXT:
            sub = Filter.or(sub, parseWordSearch(col, part, dateOrdering));
            break;
          default:
            sub = Filter.or(sub, Filter.compareWithValue(col, op, part, dateOrdering));
        }
      }

      filter = Filter.or(filter, sub);
    }
    return filter;
  }

  public ResponseObject processQuery(String query) {
    if (BeeUtils.isEmpty(query)) {
      return ResponseObject.error("search query not specified");
    }

    List<SearchResult> results = mh.doSearch(query);
    return ResponseObject.response(results);
  }

  private static List<String> parseQuery(String query) {
    List<String> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(query)) {
      if (BeeUtils.isQuoted(query)) {
        /* if query is quoted ["Hello world again"] */

        result.add(query);
      } else if (BeeUtils.count(query, BeeConst.CHAR_QUOT) > 1) {
        /* if query has one word in quoted [Hello "world" again] */

        // send parsing first part of query until quote separator[Hello]
        result.addAll(parseQuery(BeeUtils.trim(query.substring(0, query.indexOf(
            BeeConst.CHAR_QUOT)))));

        String qt = query.substring(query.indexOf(BeeConst.CHAR_QUOT) + 1);

        // send parsed first quoted part ["world"]
        result.addAll(parseQuery(BeeConst.STRING_QUOT + qt.substring(0, qt.indexOf(
            BeeConst.CHAR_QUOT) + 1)));

        // send last part of query after quoted text [again]
        result.addAll(parseQuery(BeeUtils.trim(qt.substring(qt.indexOf(BeeConst.CHAR_QUOT) + 1))));

      } else if (query.indexOf(BeeConst.CHAR_SPACE) > 0) {
        /* if query has spaced word [Hello world again] */
        result.addAll(Splitter.on(BeeConst.CHAR_SPACE).splitToList(query));
      } else {
        /* if query has one word only [Hello] */
        result.add(query);
      }
    }

    return result;
  }

  private static Filter parseWordSearch(IsColumn col, String text, DateOrdering dateOrdering) {
    Filter filter = Filter.contains(col.getId(), text);

    if (BeeUtils.isPrefix(text, BeeConst.STRING_ASTERISK)
        && BeeUtils.isSuffix(text, BeeConst.STRING_ASTERISK)) {

      String t = text.substring(1, text.length() - 1);
      if (!BeeUtils.isEmpty(t)) {
        filter = Filter.contains(col.getId(), t);
      }

    } else if (BeeUtils.isPrefix(text, BeeConst.STRING_ASTERISK)) {
      String t = text.substring(1);

      if (!BeeUtils.isEmpty(t)) {
        filter = Filter.compareWithValue(col, Operator.ENDS, t, dateOrdering);
      }

    } else if (BeeUtils.isSuffix(text, BeeConst.STRING_ASTERISK)) {
      String t = text.substring(0, text.length() - 1);
      if (!BeeUtils.isEmpty(t)) {
        filter = Filter.compareWithValue(col, Operator.STARTS, t, dateOrdering);
      }

    } else if (BeeUtils.isQuoted(text)) {
      String t = BeeUtils.unquote(text);

      if (!BeeUtils.isEmpty(t)) {
        filter = Filter.equals(col.getId(), t);
      }
    }

    return filter;
  }
}
