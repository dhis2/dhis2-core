package org.hisp.dhis.appmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppManagerTest {

  @Test
  @DisplayName("filter by plugin type should not thrown a null pointer exception")
  void filterByPluginTypeTest() {
    // given 2 apps, 1 with a plugin type and 1 with none
    App app1 = new App();
    app1.setName("app 1");
    app1.setVersion("v1");
    app1.setPluginType("plugin1");

    App app2 = new App();
    app2.setName("app 2");
    app2.setVersion("v2");

    // when filtering by plugin type
    List<App> filteredApps =
        assertDoesNotThrow(() -> AppManager.filterAppsByPluginType("plugin1", List.of(app1, app2)));

    // then 1 app is returned and no error is thrown
    assertEquals(1, filteredApps.size());
    assertEquals("app 1", filteredApps.get(0).getName());
  }
}
