package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class LocationMessage extends Message implements HasRecipient {

  public static LocationMessage coordinates(String from, String to,
      Double latitude, Double longitude, Double accuracy) {
    LocationMessage locationMessage = new LocationMessage(from, to);

    locationMessage.setLatitude(latitude);
    locationMessage.setLongitude(longitude);
    locationMessage.setAccuracy(accuracy);

    return locationMessage;
  }

  public static LocationMessage query(String from, String to) {
    return new LocationMessage(from, to);
  }

  public static LocationMessage response(String from, String to, String response) {
    LocationMessage locationMessage = new LocationMessage(from, to);
    locationMessage.setResponse(response);
    return locationMessage;
  }

  private String from;
  private String to;

  private Double latitude;
  private Double longitude;
  private Double accuracy;

  private String response;

  LocationMessage() {
    super(Type.LOCATION);
  }

  private LocationMessage(String from, String to) {
    this();

    this.from = from;
    this.to = to;
  }

  @Override
  public String brief() {
    return BeeUtils.joinWords(string(getLatitude()), string(getLongitude()));
  }

  public Double getAccuracy() {
    return accuracy;
  }

  public String getFrom() {
    return from;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getResponse() {
    return response;
  }

  @Override
  public String getTo() {
    return to;
  }

  public boolean hasCoordinates() {
    return getLatitude() != null && getLongitude() != null;
  }

  public boolean isQuery() {
    return getLatitude() == null && BeeUtils.isEmpty(getResponse());
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.anyEmpty(getFrom(), getTo());
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "from", getFrom(), "to", getTo(),
        "latitude", string(getLatitude()), "longitude", string(getLongitude()),
        "accuracy", string(getAccuracy()), "response", getResponse());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 6);

    int i = 0;
    setFrom(arr[i++]);
    setTo(arr[i++]);

    setLatitude(BeeUtils.toDoubleOrNull(arr[i++]));
    setLongitude(BeeUtils.toDoubleOrNull(arr[i++]));
    setAccuracy(BeeUtils.toDoubleOrNull(arr[i++]));

    setResponse(arr[i++]);
  }

  @Override
  protected String serialize() {
    List<Object> values = Lists.newArrayList();

    values.add(getFrom());
    values.add(getTo());

    values.add(getLatitude());
    values.add(getLongitude());
    values.add(getAccuracy());

    values.add(getResponse());

    return Codec.beeSerialize(values);
  }

  private void setAccuracy(Double accuracy) {
    this.accuracy = accuracy;
  }

  private void setFrom(String from) {
    this.from = from;
  }

  private void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  private void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  private void setResponse(String response) {
    this.response = response;
  }

  private void setTo(String to) {
    this.to = to;
  }
}
