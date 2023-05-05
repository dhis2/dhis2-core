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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.audit.UserInfoTestHelper;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
class TrackedEntityDataValueAuditStoreTest extends SingleSetupIntegrationTestBase
{
    private static final String USER_A = "userA";

    private static final UserInfoSnapshot USER_SNAP_A = UserInfoTestHelper.testUserInfo( USER_A );

    @Autowired
    private TrackedEntityDataValueAuditStore auditStore;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EventService eventService;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    private OrganisationUnit ouE;

    private Program pA;

    private ProgramStage psA;

    private ProgramStage psB;

    private DataElement deA;

    private DataElement deB;

    private Event eventA;

    private Event eventB;

    private Event eventC;

    private Event eventD;

    private Event eventE;

    private EventDataValue dvA;

    private EventDataValue dvB;

    private EventDataValue dvC;

    private EventDataValue dvD;

    private EventDataValue dvE;

    @Override
    public void setUpTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );
        ouD = createOrganisationUnit( 'D', ouB );
        ouE = createOrganisationUnit( 'E', ouD );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        pA = createProgram( 'A', new HashSet<>(), ouA );
        programService.addProgram( pA );

        psA = new ProgramStage( "StageA", pA );
        psA.setSortOrder( 1 );
        programStageService.saveProgramStage( psA );
        psB = new ProgramStage( "StageB", pA );
        psB.setSortOrder( 2 );
        programStageService.saveProgramStage( psB );
        pA.setProgramStages( Set.of( psA, psB ) );
        programService.updateProgram( pA );

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );

        TrackedEntity teiA = createTrackedEntityInstance( ouA );
        entityInstanceService.addTrackedEntityInstance( teiA );

        Enrollment enrollmentA = enrollmentService.enrollTrackedEntityInstance(
            teiA, pA, new Date(), new Date(), ouA );

        dvA = new EventDataValue( deA.getUid(), "A", USER_SNAP_A );
        dvB = new EventDataValue( deB.getUid(), "B", USER_SNAP_A );
        dvC = new EventDataValue( deA.getUid(), "C", USER_SNAP_A );
        dvD = new EventDataValue( deB.getUid(), "D", USER_SNAP_A );
        dvE = new EventDataValue( deB.getUid(), "E", USER_SNAP_A );

        eventA = createEvent( enrollmentA, psA, ouA, Set.of( dvA, dvB ) );
        eventB = createEvent( enrollmentA, psB, ouB, Set.of( dvC, dvD ) );
        eventC = createEvent( enrollmentA, psA, ouC, Set.of( dvA, dvB ) );
        eventD = createEvent( enrollmentA, psB, ouD, Set.of( dvC, dvD ) );
        eventE = createEvent( enrollmentA, psA, ouE, Set.of( dvA, dvE ) );
        eventService.addEvent( eventA );
        eventService.addEvent( eventB );
        eventService.addEvent( eventC );
        eventService.addEvent( eventD );
        eventService.addEvent( eventE );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByDataElement()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventA,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventB,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.UPDATE );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setDataElements( List.of( deA, deB ) )
            .setEvents( List.of( eventA ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaA, dvaB ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 2, auditStore.countTrackedEntityDataValueAudits( params ) );

        params = new TrackedEntityDataValueAuditQueryParams()
            .setDataElements( List.of( deA ) )
            .setEvents( List.of( eventA ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaA ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 1, auditStore.countTrackedEntityDataValueAudits( params ) );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByOrgUnitSelected()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventA,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventB,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.UPDATE );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setOrgUnits( List.of( ouA ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaA, dvaB ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 2, auditStore.countTrackedEntityDataValueAudits( params ) );

        params = new TrackedEntityDataValueAuditQueryParams()
            .setOrgUnits( List.of( ouB ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaC ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 1, auditStore.countTrackedEntityDataValueAudits( params ) );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByOrgUnitDescendants()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventB,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventC,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaD = new TrackedEntityDataValueAudit( deB, eventD,
            dvD.getAuditValue(), USER_A, dvD.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaE = new TrackedEntityDataValueAudit( deA, eventE,
            dvE.getAuditValue(), USER_A, dvE.getProvidedElsewhere(), AuditType.UPDATE );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );
        auditStore.addTrackedEntityDataValueAudit( dvaD );
        auditStore.addTrackedEntityDataValueAudit( dvaE );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setOrgUnits( List.of( ouB ) )
            .setOuMode( OrganisationUnitSelectionMode.DESCENDANTS )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaB, dvaD, dvaE ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 3, auditStore.countTrackedEntityDataValueAudits( params ) );

        params = new TrackedEntityDataValueAuditQueryParams()
            .setOrgUnits( List.of( ouA ) )
            .setOuMode( OrganisationUnitSelectionMode.DESCENDANTS )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaA, dvaB, dvaC, dvaD, dvaE ),
            auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 5, auditStore.countTrackedEntityDataValueAudits( params ) );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByProgramStage()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventA,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventB,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.UPDATE );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setProgramStages( List.of( psA ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaA, dvaB ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 2, auditStore.countTrackedEntityDataValueAudits( params ) );

        params = new TrackedEntityDataValueAuditQueryParams()
            .setProgramStages( List.of( psB ) )
            .setAuditTypes( List.of( AuditType.UPDATE ) );
        assertContainsOnly( List.of( dvaC ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 1, auditStore.countTrackedEntityDataValueAudits( params ) );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByStartEndDate()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.UPDATE );
        dvaA.setCreated( getDate( 2021, 6, 1 ) );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventA,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        dvaB.setCreated( getDate( 2021, 7, 1 ) );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventB,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.UPDATE );
        dvaC.setCreated( getDate( 2021, 8, 1 ) );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setDataElements( List.of( deA, deB ) )
            .setStartDate( getDate( 2021, 6, 15 ) )
            .setEndDate( getDate( 2021, 8, 15 ) );
        assertContainsOnly( List.of( dvaB, dvaC ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 2, auditStore.countTrackedEntityDataValueAudits( params ) );

        params = new TrackedEntityDataValueAuditQueryParams()
            .setDataElements( List.of( deA, deB ) )
            .setStartDate( getDate( 2021, 6, 15 ) )
            .setEndDate( getDate( 2021, 7, 15 ) );
        assertContainsOnly( List.of( dvaB ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 1, auditStore.countTrackedEntityDataValueAudits( params ) );
    }

    @Test
    void testGetTrackedEntityDataValueAuditsByAuditType()
    {
        TrackedEntityDataValueAudit dvaA = new TrackedEntityDataValueAudit( deA, eventA,
            dvA.getAuditValue(), USER_A, dvA.getProvidedElsewhere(), AuditType.CREATE );
        TrackedEntityDataValueAudit dvaB = new TrackedEntityDataValueAudit( deB, eventA,
            dvB.getAuditValue(), USER_A, dvB.getProvidedElsewhere(), AuditType.UPDATE );
        TrackedEntityDataValueAudit dvaC = new TrackedEntityDataValueAudit( deA, eventB,
            dvC.getAuditValue(), USER_A, dvC.getProvidedElsewhere(), AuditType.DELETE );
        auditStore.addTrackedEntityDataValueAudit( dvaA );
        auditStore.addTrackedEntityDataValueAudit( dvaB );
        auditStore.addTrackedEntityDataValueAudit( dvaC );

        TrackedEntityDataValueAuditQueryParams params = new TrackedEntityDataValueAuditQueryParams()
            .setAuditTypes( List.of( AuditType.UPDATE, AuditType.DELETE ) );
        assertContainsOnly( List.of( dvaB, dvaC ), auditStore.getTrackedEntityDataValueAudits( params ) );
        assertEquals( 2, auditStore.countTrackedEntityDataValueAudits( params ) );
    }
}
