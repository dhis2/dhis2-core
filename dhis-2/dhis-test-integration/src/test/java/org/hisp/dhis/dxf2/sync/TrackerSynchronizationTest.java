/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.sync;

import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.hisp.dhis.security.acl.AccessStringHelper.FULL;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceEnrollmentParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
class TrackerSynchronizationTest extends SingleSetupIntegrationTestBase {
  // We need to pick a future date as lastUpdated is automatically set to now and cannot be changed
  private static final Date TOMORROW = DateUtils.getDateForTomorrow(0);

  private static final String EVENT_DATE =
      DateUtils.getIso8601NoTz(DateUtils.nowMinusDuration("1d"));

  private static final String TEI_NOT_IN_SYNC_UID = "ABCDEFGHI01";

  private static final String SYNCHRONIZED_TEI_UID = "ABCDEFGHI02";

  private static final String SKIP_ATT_VALUE = "ATT: Skip Sync";

  private static final String ATT_VALUE = "ATT: Value";

  private static final String SKIP_DATA_VALUE = "DV: Skip Sync";

  private static final String DATA_VALUE = "DV: Value";

  @Autowired private UserService _userService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityInstanceService subject;

  @Autowired private EventService eventService;

  @Autowired private TrackerSynchronization trackerSync;

  private TrackedEntityInstanceQueryParams queryParams;

  private TrackedEntityInstanceParams params;

  private void prepareDataForTest(User user) {
    TrackedEntityAttribute teaA = createTrackedEntityAttribute('a');
    TrackedEntityAttribute teaB = createTrackedEntityAttribute('b');
    teaB.setSkipSynchronization(true);
    manager.save(teaA);
    manager.save(teaB);
    TrackedEntityType tet = createTrackedEntityType('a');
    TrackedEntityTypeAttribute tetaA = new TrackedEntityTypeAttribute(tet, teaA, true, false);
    TrackedEntityTypeAttribute tetaB = new TrackedEntityTypeAttribute(tet, teaB, true, false);
    tet.getTrackedEntityTypeAttributes().add(tetaA);
    tet.getTrackedEntityTypeAttributes().add(tetaB);
    manager.save(tet);
    OrganisationUnit ou = createOrganisationUnit('a');
    manager.save(ou);
    Program program = createProgram('a', null, Set.of(teaA, teaB), Set.of(ou), null);
    program.setTrackedEntityType(tet);
    program.setSharing(Sharing.builder().publicAccess(FULL).build());
    manager.save(program);
    ProgramStage programStage = createProgramStage('a', program);
    program.getProgramStages().add(programStage);
    manager.save(programStage);
    manager.update(program);
    DataElement deA = createDataElement('A');
    deA.setValueType(ValueType.TEXT);
    deA.setDomainType(DataElementDomain.TRACKER);
    DataElement deB = createDataElement('B');
    deB.setValueType(ValueType.TEXT);
    deB.setDomainType(DataElementDomain.TRACKER);
    manager.save(deA);
    manager.save(deB);
    ProgramStageDataElement psdeA = createProgramStageDataElement(programStage, deA, 1);
    ProgramStageDataElement psdeB = createProgramStageDataElement(programStage, deB, 2);
    psdeA.setSkipSynchronization(true);
    manager.save(psdeA);
    manager.save(psdeB);
    programStage.getProgramStageDataElements().addAll(List.of(psdeA, psdeB));
    manager.update(programStage);
    TrackedEntityInstance teiToSync = createTrackedEntityInstance('a', ou, teaA);
    teiToSync.setTrackedEntityType(tet);
    teiToSync.setUid(TEI_NOT_IN_SYNC_UID);
    TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue('a', teiToSync, teaA);
    teavA.setValue(ATT_VALUE);
    TrackedEntityAttributeValue teavB = createTrackedEntityAttributeValue('b', teiToSync, teaB);
    teavB.setValue(SKIP_ATT_VALUE);
    manager.save(teiToSync);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(teavA);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(teavB);
    ProgramInstance enrA = createProgramInstance(program, teiToSync, ou);
    enrA.enrollTrackedEntityInstance(teiToSync, program);
    enrA.setUser(user);
    manager.save(enrA);
    TrackedEntityInstance alreadySynchronizedTei = createTrackedEntityInstance('b', ou);
    alreadySynchronizedTei.setTrackedEntityType(tet);
    alreadySynchronizedTei.setLastSynchronized(TOMORROW);
    alreadySynchronizedTei.setUid(SYNCHRONIZED_TEI_UID);
    manager.save(alreadySynchronizedTei);
    User superUser = createAndAddAdminUser(AUTHORITY_ALL);
    injectSecurityContext(superUser);
    org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei =
        subject.getTrackedEntityInstance(teiToSync, TrackedEntityInstanceParams.TRUE);
    DataValue dataValueA = createDataValue(deA.getUid(), SKIP_DATA_VALUE, true);
    DataValue dataValueB = createDataValue(deB.getUid(), DATA_VALUE, false);
    Set<DataValue> dataValues = Set.of(dataValueA, dataValueB);
    Event event =
        createEvent(
            program,
            programStage,
            ou,
            enrA,
            tei.getTrackedEntityInstance(),
            dataValues);
    eventService.addEvent(event, null, false);
  }

  @Override
  public void setUpTest() {
    userService = _userService;
    User user = createUserWithAuth("userUID0001");
    manager.save(user);
    prepareSyncParams();
    prepareDataForTest(user);
  }

  private void prepareSyncParams() {
    queryParams = new TrackedEntityInstanceQueryParams();
    queryParams.setIncludeDeleted(true);
    params =
        new TrackedEntityInstanceParams(
            false, TrackedEntityInstanceEnrollmentParams.FALSE, false, false, true, true);
  }

  @Test
  void shouldReturnAllTeisWhenNotSyncQuery() {
    queryParams.setSynchronizationQuery(false);
    queryParams.setSkipChangedBefore(null);

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis =
        subject.getTrackedEntityInstances(queryParams, params, true, true);

    assertContainsOnly(
        List.of(TEI_NOT_IN_SYNC_UID, SYNCHRONIZED_TEI_UID),
        fetchedTeis.stream().map(t -> t.getTrackedEntityInstance()).collect(Collectors.toList()));
    assertEquals(1, getTeiByUid(fetchedTeis, TEI_NOT_IN_SYNC_UID).getAttributes().size());
  }

  @Test
  void shouldNotSynchronizeTeiUpdatedBeforeLastSync() {
    queryParams.setSynchronizationQuery(true);
    queryParams.setSkipChangedBefore(null);

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis =
        subject.getTrackedEntityInstances(queryParams, params, true, true);

    assertContainsOnly(
        List.of(TEI_NOT_IN_SYNC_UID),
        fetchedTeis.stream().map(t -> t.getTrackedEntityInstance()).collect(Collectors.toList()));
    assertEquals(1, getTeiByUid(fetchedTeis, TEI_NOT_IN_SYNC_UID).getAttributes().size());
  }

  @Test
  void shouldNotSynchronizeTeiUpdatedBeforeSkipChangedBeforeDate() {
    queryParams.setSynchronizationQuery(true);
    queryParams.setSkipChangedBefore(TOMORROW);

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis =
        subject.getTrackedEntityInstances(queryParams, params, true, true);

    assertIsEmpty(fetchedTeis);
  }

  @Test
  void shouldNotSynchronizeDataWithSkipSynchronizationFlag() throws Exception {
    queryParams.setSynchronizationQuery(true);
    queryParams.setSkipChangedBefore(null);

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis =
        subject.getTrackedEntityInstances(
            queryParams, TrackedEntityInstanceParams.DATA_SYNCHRONIZATION, true, true);

    final Map<String, Set<String>> psdeSkipMap =
        programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();

    TrackedEntityInstances teis = new TrackedEntityInstances();
    teis.setTrackedEntityInstances(fetchedTeis);

    Method method =
        TrackerSynchronization.class.getDeclaredMethod(
            "filterDataWithSkipSynchronizationFlag", TrackedEntityInstances.class, Map.class);
    method.setAccessible(true);
    method.invoke(trackerSync, teis, psdeSkipMap);

    List<String> dataValues = new ArrayList<>();
    List<String> enrollmentAttValues = new ArrayList<>();
    List<String> teiAttValues = new ArrayList<>();
    teis.getTrackedEntityInstances().forEach(
        tei -> {
          tei.getAttributes().forEach(
              att -> {
                teiAttValues.add(att.getValue());
              });
          tei.getEnrollments().forEach(
              enr -> {
                enr.getAttributes().forEach(
                    att -> {
                      enrollmentAttValues.add(att.getValue());
                    });
                enr.getEvents().forEach(
                    ev -> {
                      ev.getDataValues().forEach(
                          dv -> {
                            dataValues.add(dv.getValue());
                          });
                    });
              });
        }
    );

    assertContainsOnly(List.of(DATA_VALUE), dataValues);
    assertContainsOnly(List.of(ATT_VALUE), enrollmentAttValues);
    assertContainsOnly(List.of(ATT_VALUE), teiAttValues);
  }

  private org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance getTeiByUid(
      List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> teis, String teiUid) {
    return teis.stream()
        .filter(t -> Objects.equals(t.getTrackedEntityInstance(), teiUid))
        .findAny()
        .get();
  }

  private DataValue createDataValue(String dataElementUid, String value, Boolean skipSync) {
    DataValue dataValue = new DataValue(dataElementUid, value);
    dataValue.setSkipSynchronization(skipSync);
    return dataValue;
  }

  private Event createEvent(
          Program program,
          ProgramStage programStage,
          OrganisationUnit orgUnit,
          ProgramInstance pi,
          String teiUid,
          Set<DataValue> dataValues) {
    Event event = new Event();
    event.setStatus(EventStatus.ACTIVE);
    event.setProgram(program.getUid());
    event.setProgramStage(programStage.getUid());
    event.setTrackedEntityInstance(teiUid);
    event.setOrgUnit(orgUnit.getUid());
    event.setEnrollment(pi.getUid());
    event.setEventDate(EVENT_DATE);
    event.setDeleted(false);
    event.setDataValues(dataValues);
    return event;
  }
}
