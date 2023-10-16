package org.hisp.dhis.datasource;

import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_PASSWORD;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Properties;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.ReadOnlyDataSourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadOnlyDataSourceManagerTest {
  @Mock
  private DhisConfigurationProvider config;
      
  @Test
  void testGetReadOnlyDataSourceConfigs()
  {
    Properties props = new Properties();
    
    props.put("read1.connection.url", "jdbc:postgresql:dev_read_1");
    props.put("read1.connection.username", "dhis1");
    props.put("read1.connection.password", "pw1");
    
    when(config.getProperties()).thenReturn(props);

    when(config.getProperty(CONNECTION_USERNAME))
        .thenReturn(CONNECTION_USERNAME.getDefaultValue());
    when(config.getProperty(CONNECTION_PASSWORD))
        .thenReturn(CONNECTION_PASSWORD.getDefaultValue());
    
    ReadOnlyDataSourceManager manager = new ReadOnlyDataSourceManager();
    List<ReadOnlyDataSourceConfig> dataSourceConfigs = manager.getReadOnlyDataSourceConfigs(config);
    assertEquals(1,dataSourceConfigs.size());
    ReadOnlyDataSourceConfig dataSourceConfig = dataSourceConfigs.get(0);
    assertEquals("jdbc:postgresql:dev_read_1", dataSourceConfig.getUrl());
    assertEquals("dhis1", dataSourceConfig.getUsername());
    assertEquals("pw1", dataSourceConfig.getPassword());
  }
}
