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
package org.hisp.dhis.program;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hisp.dhis.program.notification.NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE;
import static org.hisp.dhis.program.notification.NotificationTrigger.SCHEDULED_DAYS_INCIDENT_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Sets;

/**
 * @author Chau Thu Tran
 */
class ProgramInstanceStoreTest extends TransactionalIntegrationTest
{

    @Autowired
    private ProgramInstanceStore programInstanceStore;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DbmsManager dbmsManager;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    @Qualifier( "org.hisp.dhis.program.notification.ProgramNotificationStore" )
    private IdentifiableObjectStore<ProgramNotificationTemplate> programNotificationStore;

    private Date incidentDate;

    private Date enrollmentDate;

    private Program programA;

    private Program programB;

    private Program programC;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramInstance programInstanceC;

    private ProgramInstance programInstanceD;

    private TrackedEntityInstance entityInstanceA;

    private Collection<Long> orgunitIds;

    @Override
    public void setUpTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        long idA = organisationUnitService.addOrganisationUnit( organisationUnitA );
        organisationUnitB = createOrganisationUnit( 'B' );
        long idB = organisationUnitService.addOrganisationUnit( organisationUnitB );
        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programService.addProgram( programA );
        ProgramStage stageA = new ProgramStage( "StageA", programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        ProgramStage stageB = new ProgramStage( "StageB", programA );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );
        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programService.addProgram( programB );
        programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programService.addProgram( programC );
        entityInstanceA = createTrackedEntityInstance( organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );
        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidentDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();
        programInstanceA = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-A" );
        programInstanceB = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programB );
        programInstanceB.setUid( "UID-B" );
        programInstanceB.setStatus( ProgramStatus.CANCELLED );
        programInstanceC = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programC );
        programInstanceC.setUid( "UID-C" );
        programInstanceC.setStatus( ProgramStatus.COMPLETED );
        programInstanceD = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceB, programA );
        programInstanceD.setUid( "UID-D" );
    }

    @Test
    void testProgramStageInstanceExists()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        dbmsManager.flushSession();
        assertTrue( programInstanceStore.exists( programInstanceA.getUid() ) );
        assertTrue( programInstanceStore.exists( programInstanceB.getUid() ) );
        assertFalse( programInstanceStore.exists( "aaaabbbbccc" ) );
        assertFalse( programInstanceStore.exists( null ) );
    }

    @Test
    void testGetProgramInstancesByProgram()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );
        List<ProgramInstance> programInstances = programInstanceStore.get( programA );
        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceD ) );
        programInstances = programInstanceStore.get( programB );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceB ) );
    }

    @Test
    void testGetProgramInstancesByEntityInstanceProgramStatus()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceC );
        programInstanceStore.save( programInstanceD );
        List<ProgramInstance> programInstances = programInstanceStore.get( entityInstanceA, programC,
            ProgramStatus.COMPLETED );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceC ) );
        programInstances = programInstanceStore.get( entityInstanceA, programA, ProgramStatus.ACTIVE );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
    }

    @Test
    void testGetWithScheduledNotifications()
    {
        ProgramNotificationTemplate a1 = createProgramNotificationTemplate( "a1", -1, SCHEDULED_DAYS_INCIDENT_DATE,
            ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE ),
            a2 = createProgramNotificationTemplate( "a2", 1, SCHEDULED_DAYS_INCIDENT_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE ),
            a3 = createProgramNotificationTemplate( "a3", 7, SCHEDULED_DAYS_ENROLLMENT_DATE,
                ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE );
        programNotificationStore.save( a1 );
        programNotificationStore.save( a2 );
        programNotificationStore.save( a3 );
        // TEI
        TrackedEntityInstance teiX = createTrackedEntityInstance( organisationUnitA );
        TrackedEntityInstance teiY = createTrackedEntityInstance( organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( teiX );
        entityInstanceService.addTrackedEntityInstance( teiY );
        // Program
        programA.setNotificationTemplates( Sets.newHashSet( a1, a2, a3 ) );
        programService.updateProgram( programA );
        // Dates
        Calendar cal = Calendar.getInstance();
        PeriodType.clearTimeOfDay( cal );
        Date today = cal.getTime();
        cal.add( Calendar.DATE, 1 );
        Date tomorrow = cal.getTime();
        cal.add( Calendar.DATE, -2 );
        Date yesterday = cal.getTime();
        cal.add( Calendar.DATE, -6 );
        Date aWeekAgo = cal.getTime();
        // Enrollments
        ProgramInstance enrollmentA = new ProgramInstance( today, tomorrow, teiX, programA );
        programInstanceStore.save( enrollmentA );
        ProgramInstance enrollmentB = new ProgramInstance( aWeekAgo, yesterday, teiY, programA );
        programInstanceStore.save( enrollmentB );
        // Queries
        List<ProgramInstance> results;
        // A
        results = programInstanceStore.getWithScheduledNotifications( a1, today );
        assertEquals( 1, results.size() );
        assertEquals( enrollmentA, results.get( 0 ) );
        results = programInstanceStore.getWithScheduledNotifications( a2, today );
        assertEquals( 1, results.size() );
        assertEquals( enrollmentB, results.get( 0 ) );
        results = programInstanceStore.getWithScheduledNotifications( a3, today );
        assertEquals( 1, results.size() );
        assertEquals( enrollmentB, results.get( 0 ) );
        results = programInstanceStore.getWithScheduledNotifications( a3, yesterday );
        assertEquals( 0, results.size() );
    }

    @Test
    void testGetExcludeDeletedProgramInstance()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.delete( programInstanceA );
        assertEquals( 1, programInstanceStore.getAll().size() );
    }

    @Test
    void testGetByProgramAndTrackedEntityInstance()
    {
        // Create a second Program Instance with identical Program and TEI as
        // programInstanceA.
        // This should really never happen in production
        // Doing it here to test that the query can return both instances
        ProgramInstance programInstanceZ = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA,
            programA );
        programInstanceZ.setUid( "UID-Z" );
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceZ );
        List<Pair<Program, TrackedEntityInstance>> programTeiPair = new ArrayList<>();
        Pair<Program, TrackedEntityInstance> pair1 = Pair.of( programA, entityInstanceA );
        programTeiPair.add( pair1 );
        final List<ProgramInstance> programInstances = programInstanceStore
            .getByProgramAndTrackedEntityInstance( programTeiPair, ProgramStatus.ACTIVE );
        assertEquals( 2, programInstances.size() );
        assertThat( programInstances, containsInAnyOrder( Matchers.hasProperty( "uid", is( "UID-Z" ) ),
            Matchers.hasProperty( "uid", is( "UID-A" ) ) ) );
    }
}
