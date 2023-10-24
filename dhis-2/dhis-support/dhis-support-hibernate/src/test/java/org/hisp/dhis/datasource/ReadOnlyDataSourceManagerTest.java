/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
  @Mock private DhisConfigurationProvider config;

  @Test
  void testGetReadOnlyDataSourceConfigs() {
    Properties props = new Properties();

    props.put("read1.connection.url", "jdbc:postgresql:dev_read_1");
    props.put("read1.connection.username", "dhis1");
    props.put("read1.connection.password", "pw1");

    when(config.getProperties()).thenReturn(props);

    when(config.getProperty(CONNECTION_USERNAME)).thenReturn(CONNECTION_USERNAME.getDefaultValue());
    when(config.getProperty(CONNECTION_PASSWORD)).thenReturn(CONNECTION_PASSWORD.getDefaultValue());

    ReadOnlyDataSourceManager manager = new ReadOnlyDataSourceManager();
    List<ReadOnlyDataSourceConfig> dataSourceConfigs = manager.getReadOnlyDataSourceConfigs(config);
    assertEquals(1, dataSourceConfigs.size());
    ReadOnlyDataSourceConfig dataSourceConfig = dataSourceConfigs.get(0);
    assertEquals("jdbc:postgresql:dev_read_1", dataSourceConfig.getUrl());
    assertEquals("dhis1", dataSourceConfig.getUsername());
    assertEquals("pw1", dataSourceConfig.getPassword());
  }
}
