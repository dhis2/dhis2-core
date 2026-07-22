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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.junit.jupiter.api.Test;

class ETagObservedEntityTypesTest {

  @Test
  void metadataTypesAreObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(DataElement.class));
    assertTrue(ETagObservedEntityTypes.isObservedType(OrganisationUnit.class));
    assertTrue(ETagObservedEntityTypes.isObservedType(Indicator.class));
  }

  @Test
  void configurationIsObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(Configuration.class));
  }

  @Test
  void fileResourceIsObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(FileResource.class));
  }

  @Test
  void userSettingIsObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(UserSetting.class));
  }

  @Test
  void datastoreEntryIsObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(DatastoreEntry.class));
  }

  @Test
  void userDatastoreEntryIsObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(UserDatastoreEntry.class));
  }

  @Test
  void dataStatisticsTypesAreObserved() {
    assertTrue(ETagObservedEntityTypes.isObservedType(DataStatistics.class));
    assertTrue(ETagObservedEntityTypes.isObservedType(DataStatisticsEvent.class));
  }

  @Test
  void systemSettingIsObservedViaClassName() {
    assertTrue(
        ETagObservedEntityTypes.getAdditionalObservedTypeNames()
            .contains("org.hisp.dhis.setting.SystemSetting"));
  }

  @Test
  void nonMetadataDataTypesAreNotObserved() {
    // DataValue is not a MetadataObject and not in the additional list
    assertFalse(ETagObservedEntityTypes.isObservedType(org.hisp.dhis.datavalue.DataValue.class));
  }

  @Test
  void additionalObservedTypeNamesHasExpectedCount() {
    // Configuration, FileResource, UserSetting, DatastoreEntry, UserDatastoreEntry,
    // DataStatistics, DataStatisticsEvent, SystemSetting = 8
    assertEquals(8, ETagObservedEntityTypes.getAdditionalObservedTypeNames().size());
  }
}
