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
package org.hisp.dhis.cacheinvalidation.sqlobserver;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.hisp.dhis.audit.DmlEvent;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class DmlCacheInvalidationBridgeTest {

  @Mock private ETagVersionService eTagVersionService;

  private DmlCacheInvalidationBridge bridge;

  @BeforeEach
  void setUp() {
    bridge = new DmlCacheInvalidationBridge(eTagVersionService);
  }

  @Test
  void testMetadataEntityBumpsVersion() {
    DmlEvent event =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("dataelement")
            .entityClassName("org.hisp.dhis.dataelement.DataElement")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testNonMetadataEntitySkipped() {
    DmlEvent event =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.INSERT)
            .tableName("datavalue")
            .entityClassName("org.hisp.dhis.datavalue.DataValue")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testNullEntityClassNameSkipped() {
    DmlEvent event =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.DELETE)
            .tableName("sometable")
            .entityClassName(null)
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testDeduplicationWithinBatch() {
    DmlEvent event1 =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("dataelement")
            .entityClassName("org.hisp.dhis.dataelement.DataElement")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    DmlEvent event2 =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("dataelement")
            .entityClassName("org.hisp.dhis.dataelement.DataElement")
            .entityId(2L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event1, event2), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testMultipleEntityTypesInBatch() {
    DmlEvent deEvent =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("dataelement")
            .entityClassName("org.hisp.dhis.dataelement.DataElement")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    DmlEvent indicatorEvent =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.INSERT)
            .tableName("indicator")
            .entityClassName("org.hisp.dhis.indicator.Indicator")
            .entityId(2L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(deEvent, indicatorEvent), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(Indicator.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testUnresolvableClassNameSkipped() {
    DmlEvent event =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("nonexistent")
            .entityClassName("com.example.NonExistentClass")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testConfigurationEntityTracked() {
    DmlEvent event =
        DmlEvent.builder()
            .operation(DmlEvent.DmlOperation.UPDATE)
            .tableName("configuration")
            .entityClassName("org.hisp.dhis.configuration.Configuration")
            .entityId(1L)
            .timestamp(Instant.now())
            .connectionId("conn-1")
            .build();

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(Configuration.class);
    verifyNoMoreInteractions(eTagVersionService);
  }
}
