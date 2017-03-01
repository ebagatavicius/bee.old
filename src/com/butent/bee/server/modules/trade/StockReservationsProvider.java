package com.butent.bee.server.modules.trade;

import com.google.common.collect.Multimap;

import com.butent.bee.shared.modules.trade.ItemQuantities;

import java.util.Collection;

public interface StockReservationsProvider {
  Multimap<Long, ItemQuantities> getStockReservations(Long warehouse, Collection<Long> items);
}
