package com.butent.bee.server.modules.trade;

import com.google.common.collect.Multimap;

import com.butent.bee.shared.modules.trade.ItemQuantities;
import com.butent.bee.shared.time.DateTime;

import java.util.Collection;
import java.util.Map;

public interface StockReservationsProvider {

  Map<String, Double> getItemReservationsInfo(Long warehouse, Long item, DateTime dateTo);

  Multimap<Long, ItemQuantities> getStockReservations(Long warehouse, Collection<Long> items);
}
