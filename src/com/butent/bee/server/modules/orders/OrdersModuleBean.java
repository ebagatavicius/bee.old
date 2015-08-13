package com.butent.bee.server.modules.orders;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.Module;

import java.util.Collection;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class OrdersModuleBean implements BeeModule {

  @Override
  public List<SearchResult> doSearch(String query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Module getModule() {
    return Module.ORDERS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    // TODO Auto-generated method stub
  }

}