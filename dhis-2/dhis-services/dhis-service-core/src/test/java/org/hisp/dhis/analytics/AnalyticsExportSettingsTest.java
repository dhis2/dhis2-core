package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportSettingsTest {

  @Mock private DhisConfigurationProvider config;

  @Mock private SystemSettingManager systemSettings;

  private AnalyticsExportSettings settings;

  @BeforeEach
  public void before() {
    settings = new AnalyticsExportSettings(config, systemSettings);
  }
  
  @Test
  void testSkipIndexCategoryColumns() {
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_DATA_ELEMENT_GROUP_SET))
        .thenReturn(true);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY)).thenReturn(true);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY_OPTION_GROUP_SET))
        .thenReturn(false);
    when(config.isEnabled(ConfigurationKey.ANALYTICS_TABLE_INDEX_ORG_UNIT_GROUP_SET))
        .thenReturn(false);

    assertFalse(settings.skipIndexDataElementGroupSetColumns());
    assertFalse(settings.skipIndexCategoryColumns());
    assertTrue(settings.skipIndexCategoryOptionGroupSetColumns());
    assertTrue(settings.skipIndexOrgUnitGroupSetColumns());
  }
}
