package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Visibility;

import com.butent.bee.client.Callback;
import com.butent.bee.client.images.Flags;
import com.butent.bee.shared.utils.BeeUtils;

public class Flag extends CustomWidget {

  public static final String ATTR_COUNTRY = "country";

  public Flag() {
    super(Document.get().createImageElement());
    addStyleName("bee-Flag");
  }

  public Flag(String country) {
    this();
    if (!BeeUtils.isEmpty(country)) {
      render(country);
    }
  }

  public void clear() {
    getElement().getStyle().setVisibility(Visibility.HIDDEN);
  }

  @Override
  public String getIdPrefix() {
    return "flag";
  }

  public void render(String country) {
    if (BeeUtils.isEmpty(country)) {
      clear();
      return;
    }

    Flags.get(country, new Callback<String>() {
      @Override
      public void onFailure(String... reason) {
        clear();
      }

      @Override
      public void onSuccess(String result) {
        if (BeeUtils.isEmpty(result)) {
          clear();
        } else {
          getImageElement().setSrc(result);
          getElement().getStyle().clearVisibility();
        }
      }
    });
  }

  private ImageElement getImageElement() {
    return ImageElement.as(getElement());
  }
}
