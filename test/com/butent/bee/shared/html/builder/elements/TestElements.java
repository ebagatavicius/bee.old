package com.butent.bee.shared.html.builder.elements;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("static-method")
public class TestElements {

  @Test
  public final void testDiv() {
    Div div = new Div();

    assertTrue("<div></div>".equals(div.toString()));
  }
}
