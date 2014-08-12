package com.butent.bee.server.data;

import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class SearchBean {

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  ModuleHolderBean mh;

  public ResponseObject processQuery(String query) {
    if (BeeUtils.isEmpty(query)) {
      return ResponseObject.error("search query not specified");
    }

    List<SearchResult> results = mh.doSearch(query);
    return ResponseObject.response(results);
  }
}
