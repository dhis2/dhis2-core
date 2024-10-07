package org.hisp.dhis.helpers.extensions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class LiveCheckExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    ExtensionContext rootContext = context.getRoot();
    ExtensionContext.Store store = rootContext.getStore(ExtensionContext.Namespace.GLOBAL);
    boolean pingSuccess = store.getOrComputeIfAbsent("ping", key -> {

      return false;
    }, Boolean.class);
    // /api/ping
    Assertions.assertTrue(pingSuccess, "Could not ping");
  }

}
