package com.butent.bee.client.timeboard;

import com.google.common.collect.Range;

import com.butent.bee.shared.time.JustDate;

public interface HasVisibleRange {
  
  Range<JustDate> getMaxRange();
  
  int getMaxSize();

  Range<JustDate> getVisibleRange();
  
  boolean setVisibleRange(JustDate start, JustDate end);
}
