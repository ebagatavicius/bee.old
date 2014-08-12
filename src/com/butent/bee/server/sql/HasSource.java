package com.butent.bee.server.sql;

import java.util.Collection;

/**
 * Ensures that all implementing classes of this interface would have {@code getSources} method.
 */

public interface HasSource {

  Collection<String> getSources();
}
