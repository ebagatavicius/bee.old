package com.butent.bee.shared;

public class Size {

  private final int width;
  private final int height;

  public Size(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public boolean encloses(Size other) {
    Assert.notNull(other);
    return width >= other.width && height >= other.height;
  }

  public boolean encloses(Size first, Size second) {
    Assert.notNull(first);
    Assert.notNull(second);

    return width >= (first.width + second.width)
        && height >= first.height && height >= second.height
        || height >= (first.height + second.height)
        && width >= first.width && width >= second.width;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Size) {
      return width == ((Size) obj).width && height == ((Size) obj).height;
    } else {
      return false;
    }
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + height;
    result = prime * result + width;
    return result;
  }

  public boolean isValid() {
    return width > 0 && height > 0;
  }

  public Size minus(Size size) {
    Assert.notNull(size);
    return new Size(width - size.width, height - size.height);
  }

  public Size plus(Size size) {
    Assert.notNull(size);
    return new Size(width + size.width, height + size.height);
  }

  public Size rotate() {
    return new Size(height, width);
  }
}
