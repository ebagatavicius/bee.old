package com.butent.bee.server.data;

import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.shared.data.Defaults;
import com.butent.bee.shared.modules.administration.AdministrationConstants;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class ServerDefaults extends Defaults {

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  ParamHolderBean prm;
  @EJB
  SystemBean sys;

  @Override
  public Object getValue(DefaultExpression defExpr, Object defValue) {
    return getValue(null, null, defExpr, defValue);
  }

  public Object getValue(String tblName, String fldName, DefaultExpression defExpr,
      Object defValue) {
    Object value = null;

    if (defExpr == null) {
      value = defValue;
    } else {
      switch (defExpr) {
        case CURRENT_USER:
          value = usr.getCurrentUserId();
          break;

        case NEXT_NUMBER:
          String prefix = (defValue == null) ? null : defValue.toString().trim();
          value = sys.clampValue(tblName, fldName,
              qs.getNextNumber(tblName, fldName, prefix, null));
          break;

        case MAIN_CURRENCY:
          value = prm.getRelation(AdministrationConstants.PRM_CURRENCY);
          break;

        default:
          value = super.getValue(defExpr, defValue);
          break;
      }
    }
    return value;
  }
}
