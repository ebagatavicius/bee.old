package com.butent.bee.server.modules.administration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

public final class ExchangeUtils {

  public static IsExpression exchangeField(SqlSelect query, String tbl, String amountFld,
      String currencyFld, String dateFld) {

    IsExpression date = null;

    if (!BeeUtils.isEmpty(dateFld)) {
      date = SqlUtils.field(tbl, dateFld);
    }
    return exchangeField(query,
        SqlUtils.field(tbl, amountFld), SqlUtils.field(tbl, currencyFld), date);
  }

  public static IsExpression exchangeField(SqlSelect query, IsExpression amount,
      IsExpression currency, IsExpression date) {

    Assert.notNull(amount);
    String rates = SqlUtils.uniqueName();

    addExchangeFrom(query, rates, currency, date);

    return SqlUtils.multiply(amount,
        SqlUtils.divide(SqlUtils.field(rates, COL_CURRENCY_RATE), SqlUtils.field(rates,
            COL_CURRENCY_RATE_QUANTITY)));
  }

  public static IsExpression exchangeFieldTo(SqlSelect query, String tbl, String amountFld,
      String currencyFld, String dateFld, Long currencyTo) {

    if (!DataUtils.isId(currencyTo)) {
      return exchangeField(query, tbl, amountFld, currencyFld, dateFld);
    }
    IsExpression date = null;

    if (!BeeUtils.isEmpty(dateFld)) {
      date = SqlUtils.field(tbl, dateFld);
    }
    return exchangeFieldTo(query, SqlUtils.field(tbl, amountFld), SqlUtils.field(tbl, currencyFld),
        date, SqlUtils.constant(currencyTo));
  }

  public static IsExpression exchangeFieldTo(SqlSelect query, IsExpression amount,
      IsExpression currency, IsExpression date, IsExpression currencyTo) {

    if (currencyTo == null) {
      return exchangeField(query, amount, currency, date);
    }
    String ratesTo = SqlUtils.uniqueName();

    IsExpression xpr = SqlUtils.multiply(exchangeField(query, amount, currency, date),
        SqlUtils.divide(SqlUtils.field(ratesTo, COL_CURRENCY_RATE_QUANTITY),
            SqlUtils.field(ratesTo, COL_CURRENCY_RATE)));

    addExchangeFrom(query, ratesTo, currencyTo, date);

    return xpr;
  }

  private static void addExchangeFrom(SqlSelect query, String ratesAlias, IsExpression currency,
      IsExpression date) {

    Assert.noNulls(query, currency);
    IsCondition dateClause = null;

    if (date != null) {
      dateClause = SqlUtils.or(SqlUtils.isNull(date),
          SqlUtils.lessEqual(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE, date));
    }
    // TODO: ORACLE workaround needed
    query.addFromLeft(TBL_CURRENCY_RATES, ratesAlias,
        SqlUtils.equals(ratesAlias, COL_CURRENCY, currency, COL_CURRENCY_RATE_DATE,
            new SqlSelect()
                .addMax(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE)
                .addFrom(TBL_CURRENCY_RATES)
                .setWhere(SqlUtils.and(dateClause,
                    SqlUtils.equals(TBL_CURRENCY_RATES, COL_CURRENCY, currency)))));
  }

  private ExchangeUtils() {
  }
}
