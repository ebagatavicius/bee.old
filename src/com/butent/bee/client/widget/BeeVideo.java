package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.media.client.Video;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;

public class BeeVideo extends Video implements IdentifiableWidget {

  public BeeVideo() {
    this(Document.get().createVideoElement());
  }

  public BeeVideo(VideoElement element) {
    super(element);
    init();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "video";
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }
}
