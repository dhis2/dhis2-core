/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DefaultConfigurationServiceTest {

  private static final String TRUSTED = "https://trusted.example.org";

  @Mock private GenericStore<Configuration> configurationStore;

  @Mock private CacheProvider cacheProvider;

  private DefaultConfigurationService service;

  @BeforeEach
  void setUp() {
    when(cacheProvider.createCorsWhitelistCache()).thenReturn(new NoOpCache<>());
    service = new DefaultConfigurationService(configurationStore, cacheProvider);
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", "*.*", "*://*", "http*://*", "https://*", "https://*.org", "null"})
  void storedInvalidEntryDoesNotTrustForeignOrigins(String invalid) {
    givenStoredAllowlist(invalid, TRUSTED);

    assertFalse(service.isCorsWhitelisted("https://foreign.example.com"));
    assertFalse(service.isCorsWhitelisted("null"));
    assertTrue(service.isCorsWhitelisted(TRUSTED));
  }

  @Test
  void storedInvalidEntryIsReturnedAsStoredButLeftOutOfTrustedOrigins() {
    givenStoredAllowlist("*", TRUSTED + "/");

    assertEquals(Set.of("*", TRUSTED + "/"), service.getCorsWhitelist());
    assertEquals(List.of(TRUSTED), service.getCorsAllowlistOrigins());
  }

  @Test
  void setCorsWhitelistRejectsInvalidEntryAndStoresNothing() {
    ConflictException ex =
        assertThrows(
            ConflictException.class,
            () -> service.setCorsWhitelist(Set.of("https://new.example.org", "*")));

    assertEquals(ErrorCode.E1136, ex.getCode());
    assertTrue(ex.getMessage().contains("`*`"), ex.getMessage());
    verify(configurationStore, never()).update(any());
    verify(configurationStore, never()).save(any());
  }

  @Test
  void setCorsWhitelistStoresNormalisedOrigins() throws ConflictException {
    Configuration configuration = givenStoredAllowlist();

    service.setCorsWhitelist(
        Set.of("HTTPS://Trusted.Example.org/", "https://*.example.org", "http://localhost:3000"));

    ArgumentCaptor<Configuration> saved = ArgumentCaptor.forClass(Configuration.class);
    verify(configurationStore).save(saved.capture());
    assertSame(configuration, saved.getValue());
    assertEquals(
        Set.of(TRUSTED, "https://*.example.org", "http://localhost:3000"),
        saved.getValue().getCorsWhitelist());
  }

  private Configuration givenStoredAllowlist(String... entries) {
    Configuration configuration = new Configuration();
    configuration.setCorsWhitelist(new HashSet<>(List.of(entries)));
    when(configurationStore.getAll()).thenReturn(List.of(configuration));
    return configuration;
  }
}
