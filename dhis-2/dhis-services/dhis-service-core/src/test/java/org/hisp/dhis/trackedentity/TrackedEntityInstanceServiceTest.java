/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityInstanceServiceTest
    extends
    DhisSpringTest
{
    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    private ProgramStageInstance programStageInstanceA;

    private ProgramInstance programInstanceA;

    private Program programA;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceB1;

    private TrackedEntityAttribute entityInstanceAttribute;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityTypeA = createTrackedEntityType( 'A' );
    private TrackedEntityAttribute attrD = createTrackedEntityAttribute( 'D' );
    private TrackedEntityAttribute attrE = createTrackedEntityAttribute( 'E' );
    private TrackedEntityAttribute filtF = createTrackedEntityAttribute( 'F' );
    private TrackedEntityAttribute filtG = createTrackedEntityAttribute( 'G' );

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        entityInstanceAttribute = createTrackedEntityAttribute( 'A' );
        attributeService.addTrackedEntityAttribute( entityInstanceAttribute );

        entityInstanceA1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1.setUid( "UID-B1" );

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );

        programService.addProgram( programA );

        ProgramStage stageA = createProgramStage( 'A', programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        DateTime enrollmentDate = DateTime.now();
        enrollmentDate.withTimeAtStartOfDay();
        enrollmentDate = enrollmentDate.minusDays( 70 );

        DateTime incidentDate = DateTime.now();
        incidentDate.withTimeAtStartOfDay();

        programInstanceA = new ProgramInstance( enrollmentDate.toDate(), incidentDate.toDate(), entityInstanceA1,
                programA);
        programInstanceA.setUid( "UID-A" );
        programInstanceA.setOrganisationUnit( organisationUnit );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, stageA );
        programInstanceA.setUid( "UID-PSI-A" );
        programInstanceA.setOrganisationUnit( organisationUnit );

        trackedEntityTypeA.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeA );

        attributeService.addTrackedEntityAttribute( attrD );
        attributeService.addTrackedEntityAttribute( attrE );
        attributeService.addTrackedEntityAttribute( filtF );
        attributeService.addTrackedEntityAttribute( filtG );

        super.userService = this.userService;
        User user = createUser( "testUser" );
        user.setTeiSearchOrganisationUnits( Sets.newHashSet( organisationUnit ) );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        ReflectionTestUtils.setField( entityInstanceService, "currentUserService", currentUserService );
    }

    @Test
    public void testSaveTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testDeleteTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        TrackedEntityInstance teiB = entityInstanceService.getTrackedEntityInstance( idB );

        assertNotNull( teiA );
        assertNotNull( teiB );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );
    }

    @Test
    public void testDeleteTrackedEntityInstanceAndLinkedEnrollmentsAndEvents()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long psIdA = programInstanceService.addProgramInstance( programInstanceA );
        long psiIdA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programInstanceA.setProgramStageInstances( Sets.newHashSet( programStageInstanceA ) );
        entityInstanceA1.setProgramInstances( Sets.newHashSet( programInstanceA ) );

        programInstanceService.updateProgramInstance( programInstanceA );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        ProgramInstance psA = programInstanceService.getProgramInstance( psIdA );
        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( psiIdA );

        assertNotNull( teiA );
        assertNotNull( psA );
        assertNotNull( psiA );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( programInstanceService.getProgramInstance( psIdA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( psiIdA ) );

    }

    @Test
    public void testUpdateTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );

        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    public void testGetTrackedEntityInstanceById()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testGetTrackedEntityInstanceByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );

        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( "B1" ) );
    }

    @Test
    public void testStoredByColumnForTrackedEntityInstance()
    {
        entityInstanceA1.setStoredBy( "test" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( entityInstanceA1.getUid() );
        assertEquals( "test", tei.getStoredBy() );
    }

    @Test
    public void testGetFromUrl()
    {
        final TrackedEntityInstanceQueryParams queryParams = entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 8, 5 ),
            getDate( 2020, 8, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "A1234567890", "B1234567890" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );

        assertThat( queryParams.getQuery().getFilter(), is( "query-test" ) );
        assertThat( queryParams.getQuery().getOperator(), is( QueryOperator.EQ ) );

        assertThat( queryParams.getProgram(), is( programA ) );
        assertThat( queryParams.getTrackedEntityType(), is( trackedEntityTypeA ) );
        assertThat( queryParams.getOrganisationUnits(), hasSize( 1 ) );
        assertThat( queryParams.getOrganisationUnits().iterator().next(), is( organisationUnit ) );
        assertThat( queryParams.getAttributes(), hasSize( 2 ) );
        assertTrue(
            queryParams.getAttributes().stream().anyMatch( a -> a.getItem().getUid().equals( attrD.getUid() ) ) );
        assertTrue(
            queryParams.getAttributes().stream().anyMatch( a -> a.getItem().getUid().equals( attrE.getUid() ) ) );

        assertThat( queryParams.getFilters(), hasSize( 2 ) );
        assertTrue( queryParams.getFilters().stream().anyMatch( a -> a.getItem().getUid().equals( filtF.getUid() ) ) );
        assertTrue( queryParams.getFilters().stream().anyMatch( a -> a.getItem().getUid().equals( filtG.getUid() ) ) );

        assertThat( queryParams.getPageSizeWithDefault(), is( 50 ) );
        assertThat( queryParams.getPageSize(), is( 50 ) );
        assertThat( queryParams.getPage(), is( 1 ) );
        assertThat( queryParams.isTotalPages(), is( false ) );

        assertThat( queryParams.getProgramStatus(), is( ProgramStatus.ACTIVE ) );
        assertThat( queryParams.getFollowUp(), is( false ) );

        assertThat( queryParams.getLastUpdatedStartDate(), is( getDate( 2019, 1, 1 ) ) );
        assertThat( queryParams.getLastUpdatedEndDate(), is( getDate( 2020, 1, 1 ) ) );
        assertThat( queryParams.getProgramEnrollmentStartDate(), is( getDate( 2019, 5, 5 ) ) );
        assertThat( queryParams.getProgramEnrollmentEndDate(), is( DateUtils.addDays( getDate( 2020, 5, 5 ), 1 ) ) );
        assertThat( queryParams.getProgramIncidentStartDate(), is( getDate( 2019, 8, 5 ) ) );
        assertThat( queryParams.getProgramIncidentEndDate(), is( DateUtils.addDays( getDate( 2020, 8, 5 ), 1 ) ) );
        assertThat( queryParams.getEventStatus(), is( EventStatus.COMPLETED ) );
        assertThat( queryParams.getEventStartDate(), is( getDate( 2019, 7, 7 ) ) );
        assertThat( queryParams.getEventEndDate(), is( getDate( 2020, 7, 7 ) ) );
        assertThat( queryParams.getAssignedUserSelectionMode(), is( AssignedUserSelectionMode.PROVIDED ) );
        assertTrue( queryParams.getAssignedUsers().stream().anyMatch( u -> u.equals( "A1234567890" ) ) );
        assertTrue( queryParams.getAssignedUsers().stream().anyMatch( u -> u.equals( "B1234567890" ) ) );

        assertThat( queryParams.isIncludeDeleted(), is( true ) );
        assertThat( queryParams.isIncludeAllAttributes(), is( false ) );

        assertTrue( queryParams.getOrders().stream().anyMatch( o -> o.equals( "order-1" ) ) );
    }

    @Test
    public void testGetFromUrlFailOnMissingAttribute()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Attribute does not exist: missing" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid(), "missing" ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnMissingFilter()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Attribute does not exist: missing" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid(), "missing" ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnMissingProgram()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Program does not exist: " + programA.getUid() + "A" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid() + "A",
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnMissingTrackerEntityType()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Tracked entity type does not exist: " + trackedEntityTypeA.getUid() + "A" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid() + "A",
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnMissingOrgUnit()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Organisation unit does not exist: " + organisationUnit.getUid() + "A" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() + "A" ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnUserNonInOuHierarchy()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Organisation unit is not part of the search scope: " + organisationUnit.getUid() );

        // Force Current User Service to return a User without search org unit
        ReflectionTestUtils.setField( entityInstanceService, "currentUserService",
            new MockCurrentUserService( createUser( "testUser2" ) ) );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.PROVIDED,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }

    @Test
    public void testGetFromUrlFailOnNonProvidedAndAssignedUsers()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );

        entityInstanceService.getFromUrl(
            "query-test",
            newHashSet( attrD.getUid(), attrE.getUid() ),
            newHashSet( filtF.getUid(), filtG.getUid() ),
            newHashSet( organisationUnit.getUid() ),
            OrganisationUnitSelectionMode.DESCENDANTS,
            programA.getUid(),
            ProgramStatus.ACTIVE,
            false,
            getDate( 2019, 1, 1 ),
            getDate( 2020, 1, 1 ),
            "20",
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            getDate( 2019, 5, 5 ),
            getDate( 2020, 5, 5 ),
            trackedEntityTypeA.getUid(),
            null,
            EventStatus.COMPLETED,
            getDate( 2019, 7, 7 ),
            getDate( 2020, 7, 7 ),
            AssignedUserSelectionMode.CURRENT,
            newHashSet( "user-1", "user-2" ),
            true,
            1,
            50,
            false,
            false,
            true,
            false,
            newArrayList( "order-1" ) );
    }
}
