/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.jclouds;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JCloudsStoreTest {
  @Mock private DhisConfigurationProvider configurationProvider;
  @Mock private LocationManager locationManager;

  @Test
  void failIfUnsupportedProviderIsConfigured() {
    when(configurationProvider.getProperty(ConfigurationKey.FILESTORE_PROVIDER))
        .thenReturn("rackspace-cloudfiles-us");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JCloudsStore(configurationProvider, locationManager));

    assertContains("unsupported file store provider", exception.getMessage());
  }

  @Test
  void failIfFilesystemProviderIsConfiguredButNoExternalDirectoryIsSet() {
    when(configurationProvider.getProperty(ConfigurationKey.FILESTORE_PROVIDER))
        .thenReturn("filesystem");
    when(locationManager.externalDirectorySet()).thenReturn(false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JCloudsStore(configurationProvider, locationManager));

    assertContains("external directory is not set", exception.getMessage());
  }
}
