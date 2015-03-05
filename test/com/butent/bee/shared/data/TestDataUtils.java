package com.butent.bee.shared.data;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("static-method")
public class TestDataUtils {

  @Test
  public final void testParseColumns() {
    List<String> names = Lists.newArrayList(
        "CustomerPerson", "CustomerFirstName", "CustomerLastName",
        "CustomerCompany", "CustomerCompanyName",
        "LoadingCompany", "LoadingCompanyName", "LoadingContact", "LoadingCountry",
        "UnloadingCompany", "UnloadingCompanyName", "UnloadingContact", "UnloadingCountry",
        "CargoDescription", "Quantity", "Value", "ValueCurrency", "ValueCurrencyName",
        "Manager", "ManagerPerson", "ManagerFirstName", "ManagerLastName");

    List<BeeColumn> columns = createColumns(names);

    assertTrue(DataUtils.parseColumns("*", columns).size() == names.size());
    assertTrue(DataUtils.parseColumns("Man", columns).isEmpty());
    assertTrue(DataUtils.parseColumns("Manager", columns).size() == 1);
    assertTrue(DataUtils.parseColumns("Man*", columns).size() == 4);
    assertTrue(DataUtils.parseColumns("*first*", columns).size() == 2);
    assertTrue(DataUtils.parseColumns("*first*, *last*", columns).size() == 4);

    assertTrue(DataUtils.parseColumns("-customer* loading*-",
        columns).size() == names.size() - 5 - 4);
    assertTrue(DataUtils.parseColumns("-customer* *loading*-",
        columns).size() == names.size() - 5 - 4 - 4);
    assertTrue(DataUtils.parseColumns("*loading*, -unl*", columns).size() == 8 - 4);
  }

  private static List<BeeColumn> createColumns(List<String> names) {
    List<BeeColumn> columns = new ArrayList<>();
    for (String name : names) {
      columns.add(new BeeColumn(name));
    }
    return columns;
  }
}
