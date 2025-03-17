package org.hisp.dhis.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.system.SystemInfo.SystemInfoForAppCacheFilter;
import org.hisp.dhis.system.SystemInfo.SystemInfoForDataStats;
import org.hisp.dhis.system.SystemInfo.SystemInfoForMetadataExport;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SystemServiceTest extends PostgresIntegrationTestBase {

  @Autowired private SystemService systemService;
  @Autowired private ConfigurationService configurationService;

  @Test
  @DisplayName("System info for metadata export has expected values")
  void systemInfoForMetadataTest() {
    // given
    Configuration config = new Configuration();
    config.setSystemId("system-id-123");
    configurationService.setConfiguration(config);

    // when
    SystemInfoForMetadataExport info = systemService.getSystemInfoForMetadataExport();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertEquals("system-id-123", info.id());
    assertNotNull(info.serverDate().toString());
  }

  @Test
  @DisplayName("System info for data stats has expected values")
  void systemInfoForDataStatsTest() {
    // given
    Configuration config = new Configuration();
    config.setSystemId("system-id-123");
    configurationService.setConfiguration(config);

    // when
    SystemInfoForDataStats info = systemService.getSystemInfoForDataStats();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertEquals("system-id-123", info.id());
    assertNotNull(info.serverDate());
    assertNotNull(info.buildTime());
  }

  @Test
  @DisplayName("System info for app cache filter has expected values")
  void systemInfoForAppCacheFilterTest() {
    // given
    Configuration config = new Configuration();
    config.setSystemId("system-id-123");
    configurationService.setConfiguration(config);

    // when
    SystemInfoForAppCacheFilter info = systemService.getSystemInfoForAppCacheFilter();

    // then
    assertEquals("123", info.version());
    assertEquals("abc1234", info.revision());
    assertEquals("iso8601", info.calendar());
  }

  @Test
  @DisplayName("System info version has expected value")
  void systemInfoVersionTest() {
    String infoVersion = systemService.getSystemInfoVersion();
    assertEquals("123", infoVersion);
  }
}
