package com.butent.bee.client.widget;

import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.media.client.Audio;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;

public class BeeAudio extends Audio implements IdentifiableWidget {

  public BeeAudio() {
    this(Document.get().createAudioElement());
  }

  public BeeAudio(AudioElement element) {
    super(element);
    init();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "audio";
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }
}
