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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.dml.DmlEvent;
import org.hisp.dhis.dml.DmlObservedEvent;
import org.hisp.dhis.dml.DmlOperation;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
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

  @Mock private ETagService eTagVersionService;

  private DmlCacheInvalidationBridge bridge;

  @BeforeEach
  void setUp() {
    bridge = new DmlCacheInvalidationBridge(eTagVersionService, null);
  }

  @Test
  void testMetadataEntityBumpsVersion() {
    DmlEvent event =
        new DmlEvent(
            DmlOperation.UPDATE,
            "dataelement",
            "org.hisp.dhis.dataelement.DataElement",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testNonMetadataEntitySkipped() {
    DmlEvent event =
        new DmlEvent(
            DmlOperation.INSERT,
            "datavalue",
            "org.hisp.dhis.datavalue.DataValue",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testNullEntityClassNameSkipped() {
    DmlEvent event = new DmlEvent(DmlOperation.DELETE, "sometable", null, Instant.now(), "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testDeduplicationWithinBatch() {
    DmlEvent event1 =
        new DmlEvent(
            DmlOperation.UPDATE,
            "dataelement",
            "org.hisp.dhis.dataelement.DataElement",
            Instant.now(),
            "conn-1");

    DmlEvent event2 =
        new DmlEvent(
            DmlOperation.UPDATE,
            "dataelement",
            "org.hisp.dhis.dataelement.DataElement",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event1, event2), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testMultipleEntityTypesInBatch() {
    DmlEvent deEvent =
        new DmlEvent(
            DmlOperation.UPDATE,
            "dataelement",
            "org.hisp.dhis.dataelement.DataElement",
            Instant.now(),
            "conn-1");

    DmlEvent indicatorEvent =
        new DmlEvent(
            DmlOperation.INSERT,
            "indicator",
            "org.hisp.dhis.indicator.Indicator",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(deEvent, indicatorEvent), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(DataElement.class);
    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(Indicator.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testUnresolvableClassNameSkipped() {
    DmlEvent event =
        new DmlEvent(
            DmlOperation.UPDATE,
            "nonexistent",
            "com.example.NonExistentClass",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verifyNoInteractions(eTagVersionService);
  }

  @Test
  void testConfigurationEntityTracked() {
    DmlEvent event =
        new DmlEvent(
            DmlOperation.UPDATE,
            "configuration",
            "org.hisp.dhis.configuration.Configuration",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(Configuration.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testFileResourceEntityTracked() {
    DmlEvent event =
        new DmlEvent(
            DmlOperation.UPDATE,
            "fileresource",
            "org.hisp.dhis.fileresource.FileResource",
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null));

    verify(eTagVersionService, times(1)).incrementEntityTypeVersion(FileResource.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testSettingsAndDatastoreEntitiesTracked() {
    DmlEvent systemSettingEvent =
        new DmlEvent(
            DmlOperation.UPDATE,
            "systemsetting",
            SystemSetting.class.getName(),
            Instant.now(),
            "conn-1");
    DmlEvent userSettingEvent =
        new DmlEvent(
            DmlOperation.UPDATE,
            "usersetting",
            UserSetting.class.getName(),
            Instant.now(),
            "conn-1");
    DmlEvent datastoreEvent =
        new DmlEvent(
            DmlOperation.UPDATE,
            "keyjsonvalue",
            DatastoreEntry.class.getName(),
            Instant.now(),
            "conn-1");
    DmlEvent userDatastoreEvent =
        new DmlEvent(
            DmlOperation.UPDATE,
            "userkeyjsonvalue",
            UserDatastoreEntry.class.getName(),
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(
        new DmlObservedEvent(
            this,
            List.of(systemSettingEvent, userSettingEvent, datastoreEvent, userDatastoreEvent),
            null));

    verify(eTagVersionService).incrementEntityTypeVersion(SystemSetting.class);
    verify(eTagVersionService).incrementEntityTypeVersion(UserSetting.class);
    verify(eTagVersionService).incrementEntityTypeVersion(DatastoreEntry.class);
    verify(eTagVersionService).incrementEntityTypeVersion(UserDatastoreEntry.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testDataStatisticsEntitiesTracked() {
    DmlEvent dataStatisticsEvent =
        new DmlEvent(
            DmlOperation.INSERT,
            "datastatistics",
            DataStatistics.class.getName(),
            Instant.now(),
            "conn-1");
    DmlEvent dataStatisticsFavoriteEvent =
        new DmlEvent(
            DmlOperation.INSERT,
            "datastatisticsevent",
            DataStatisticsEvent.class.getName(),
            Instant.now(),
            "conn-1");

    bridge.onDmlObserved(
        new DmlObservedEvent(
            this, List.of(dataStatisticsEvent, dataStatisticsFavoriteEvent), null));

    verify(eTagVersionService).incrementEntityTypeVersion(DataStatistics.class);
    verify(eTagVersionService).incrementEntityTypeVersion(DataStatisticsEvent.class);
    verifyNoMoreInteractions(eTagVersionService);
  }

  @Test
  void testExceptionInIncrementDoesNotPropagate() {
    // Verify the bridge catches exceptions (preventing @Async silent swallowing).
    doThrow(new RuntimeException("Simulated version service failure"))
        .when(eTagVersionService)
        .incrementEntityTypeVersion(DataElement.class);

    DmlEvent event =
        new DmlEvent(
            DmlOperation.UPDATE,
            "dataelement",
            "org.hisp.dhis.dataelement.DataElement",
            Instant.now(),
            "conn-1");

    assertDoesNotThrow(
        () -> bridge.onDmlObserved(new DmlObservedEvent(this, List.of(event), null)),
        "Bridge should catch exceptions to prevent @Async silent swallowing");
  }

  @Test
  void testEmptyEventListDoesNotThrow() {
    assertDoesNotThrow(
        () -> bridge.onDmlObserved(new DmlObservedEvent(this, List.of(), null)),
        "Empty event list should be handled gracefully");

    verifyNoInteractions(eTagVersionService);
  }
}
