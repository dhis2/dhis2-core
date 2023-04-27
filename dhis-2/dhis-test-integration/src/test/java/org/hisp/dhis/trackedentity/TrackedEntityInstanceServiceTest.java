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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityInstanceServiceTest
    extends IntegrationTestBase
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
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    private Event event;

    private ProgramInstance programInstance;

    private Program program;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceB1;

    private TrackedEntityInstance entityInstanceC1;

    private TrackedEntityInstance entityInstanceD1;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityType;

    private TrackedEntityAttribute trackedEntityAttribute;

    private final static String ATTRIBUTE_VALUE = "Value";

    private User superUser;

    @Override
    public void setUpTest()
    {
        super.userService = _userService;

        this.superUser = preCreateInjectAdminUser();

        trackedEntityType = createTrackedEntityType( 'A' );
        TrackedEntityAttribute attrD = createTrackedEntityAttribute( 'D' );
        TrackedEntityAttribute attrE = createTrackedEntityAttribute( 'E' );
        TrackedEntityAttribute filtF = createTrackedEntityAttribute( 'F' );
        TrackedEntityAttribute filtG = createTrackedEntityAttribute( 'G' );
        trackedEntityAttribute = createTrackedEntityAttribute( 'H' );

        trackedEntityAttributeService.addTrackedEntityAttribute( attrD );
        trackedEntityAttributeService.addTrackedEntityAttribute( attrE );
        trackedEntityAttributeService.addTrackedEntityAttribute( filtF );
        trackedEntityAttributeService.addTrackedEntityAttribute( filtG );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );
        TrackedEntityAttribute entityInstanceAttribute = createTrackedEntityAttribute( 'A' );
        attributeService.addTrackedEntityAttribute( entityInstanceAttribute );
        entityInstanceA1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceC1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceD1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceA1.setUid( "UID-A1" );
        entityInstanceB1.setUid( "UID-B1" );
        entityInstanceC1.setUid( "UID-C1" );
        entityInstanceD1.setUid( "UID-D1" );
        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );
        ProgramStage stageA = createProgramStage( 'A', program );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        program.setProgramStages( programStages );
        programService.updateProgram( program );
        DateTime enrollmentDate = DateTime.now();
        enrollmentDate.withTimeAtStartOfDay();
        enrollmentDate = enrollmentDate.minusDays( 70 );
        DateTime incidentDate = DateTime.now();
        incidentDate.withTimeAtStartOfDay();
        programInstance = new ProgramInstance( enrollmentDate.toDate(), incidentDate.toDate(), entityInstanceA1,
            program );
        programInstance.setUid( "UID-A" );
        programInstance.setOrganisationUnit( organisationUnit );
        event = new Event( programInstance, stageA );
        programInstance.setUid( "UID-PSI-A" );
        programInstance.setOrganisationUnit( organisationUnit );

        trackedEntityType.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        attributeService.addTrackedEntityAttribute( attrD );
        attributeService.addTrackedEntityAttribute( attrE );
        attributeService.addTrackedEntityAttribute( filtF );
        attributeService.addTrackedEntityAttribute( filtG );

        User user = createUserWithAuth( "testUser" );
        user.setTeiSearchOrganisationUnits( Set.of( organisationUnit ) );
        injectSecurityContext( user );
    }

    @Test
    void testSaveTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    void testDeleteTrackedEntityInstance()
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
    void testDeleteTrackedEntityInstanceAndLinkedEnrollmentsAndEvents()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long psIdA = programInstanceService.addProgramInstance( programInstance );
        long psiIdA = programStageInstanceService.addProgramStageInstance( event );
        programInstance.setEvents( Set.of( event ) );
        entityInstanceA1.setProgramInstances( Set.of( programInstance ) );
        programInstanceService.updateProgramInstance( programInstance );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );
        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        ProgramInstance psA = programInstanceService.getProgramInstance( psIdA );
        Event psiA = programStageInstanceService.getProgramStageInstance( psiIdA );
        assertNotNull( teiA );
        assertNotNull( psA );
        assertNotNull( psiA );
        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( programInstanceService.getProgramInstance( psIdA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( psiIdA ) );
    }

    @Test
    void testUpdateTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );
        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    void testGetTrackedEntityInstanceById()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    void testGetTrackedEntityInstanceByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( "B1" ) );
    }

    @Test
    void testStoredByColumnForTrackedEntityInstance()
    {
        entityInstanceA1.setStoredBy( "test" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( entityInstanceA1.getUid() );
        assertEquals( "test", tei.getStoredBy() );
    }

    @Test
    void testTrackedEntityAttributeFilter()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();

        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( entityInstanceA1 );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntityInstancesGrid( params );

        assertEquals( 1, grid.getHeight() );
    }

    @Test
    void testTrackedEntityInstanceGridWithNoFilterableAttributes()
    {
        injectSecurityContext( superUser );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntityInstancesGrid( params ) );
    }

    @Test
    void testTrackedEntityInstanceGridWithNoDisplayAttributes()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( false );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntityInstancesGrid( params ) );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamCreatedAtSupplied()
    {
        injectSecurityContext( superUser );

        entityInstanceA1.setCreated( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setCreated( DateTime.now().toDate() );
        entityInstanceC1.setCreated( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setCreated( DateTime.now().plusDays( 2 ).toDate() );
        addEntityInstances();

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "createdAt", SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamUpdatedAtSupplied()
    {
        injectSecurityContext( superUser );

        entityInstanceA1.setLastUpdated( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setLastUpdated( DateTime.now().toDate() );
        entityInstanceC1.setLastUpdated( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setLastUpdated( DateTime.now().plusDays( 2 ).toDate() );

        addEntityInstances();

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAt", SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamTrackedEntitySupplied()
    {
        injectSecurityContext( superUser );

        addEntityInstances();

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "trackedEntity", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceD1.getId(), entityInstanceC1.getId(), entityInstanceB1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByLastUpdatedAtClientWhenParamUpdatedAtClientSupplied()
    {
        injectSecurityContext( superUser );

        entityInstanceA1.setLastUpdatedAtClient( DateTime.now().plusDays( 1 ).toDate() );
        entityInstanceB1.setLastUpdatedAtClient( DateTime.now().toDate() );
        entityInstanceC1.setLastUpdatedAtClient( DateTime.now().minusDays( 1 ).toDate() );
        entityInstanceD1.setLastUpdatedAtClient( DateTime.now().plusDays( 2 ).toDate() );
        addEntityInstances();

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAtClient", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceD1.getId(), entityInstanceA1.getId(), entityInstanceB1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByEnrollmentDateWhenParamEnrolledAtSupplied()
    {
        injectSecurityContext( superUser );

        addEntityInstances();
        programInstanceService.addProgramInstance( programInstance );
        addEnrollment( entityInstanceB1, DateTime.now().plusDays( 2 ).toDate(), 'B' );
        addEnrollment( entityInstanceC1, DateTime.now().minusDays( 2 ).toDate(), 'C' );
        addEnrollment( entityInstanceD1, DateTime.now().plusDays( 1 ).toDate(), 'D' );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "enrolledAt", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceD1.getId(), entityInstanceC1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesAndKeepOrderOfParamsWhenMultipleStaticFieldsSupplied()
    {
        injectSecurityContext( superUser );
        entityInstanceA1.setInactive( true );
        entityInstanceB1.setInactive( true );
        entityInstanceC1.setInactive( false );
        entityInstanceD1.setInactive( false );
        addEntityInstances();

        programInstanceService.addProgramInstance( programInstance );
        addEnrollment( entityInstanceB1, DateTime.now().plusDays( 2 ).toDate(), 'B' );
        addEnrollment( entityInstanceC1, DateTime.now().minusDays( 2 ).toDate(), 'C' );
        addEnrollment( entityInstanceD1, DateTime.now().plusDays( 1 ).toDate(), 'D' );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "inactive", SortDirection.DESC ),
            new OrderParam( "enrolledAt", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByIdWhenNoOrderParamProvided()
    {
        injectSecurityContext( superUser );
        addEntityInstances();

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderByNonStaticFieldWhenNonStaticFieldProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        setUpEntityAndAttributeValue( entityInstanceA1, "3-Attribute Value A1" );
        setUpEntityAndAttributeValue( entityInstanceB1, "1-Attribute Value B1" );
        setUpEntityAndAttributeValue( entityInstanceC1, "4-Attribute Value C1" );
        setUpEntityAndAttributeValue( entityInstanceD1, "2-Attribute Value D1" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceD1.getId(), entityInstanceA1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesAndKeepOrderOfParamsWhenStaticAndNonStaticFieldsSupplied()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        entityInstanceA1.setInactive( true );
        entityInstanceB1.setInactive( false );
        entityInstanceC1.setInactive( true );
        entityInstanceD1.setInactive( false );

        setUpEntityAndAttributeValue( entityInstanceA1, "2-Attribute Value" );
        setUpEntityAndAttributeValue( entityInstanceB1, "2-Attribute Value" );
        setUpEntityAndAttributeValue( entityInstanceC1, "1-Attribute Value" );
        setUpEntityAndAttributeValue( entityInstanceD1, "1-Attribute Value" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), SortDirection.DESC ),
            new OrderParam( "inactive", SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeDescendingWhenAttributeDescendingProvided()
    {
        injectSecurityContext( superUser );

        TrackedEntityAttribute tea = createTrackedEntityAttribute();

        addEntityInstances();

        createTrackedEntityInstanceAttribute( entityInstanceA1, tea, "A" );
        createTrackedEntityInstanceAttribute( entityInstanceB1, tea, "D" );
        createTrackedEntityInstanceAttribute( entityInstanceC1, tea, "C" );
        createTrackedEntityInstanceAttribute( entityInstanceD1, tea, "B" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( tea.getUid(), SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceC1.getId(), entityInstanceD1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeAscendingWhenAttributeAscendingProvided()
    {
        injectSecurityContext( superUser );

        TrackedEntityAttribute tea = createTrackedEntityAttribute();

        addEntityInstances();

        createTrackedEntityInstanceAttribute( entityInstanceA1, tea, "A" );
        createTrackedEntityInstanceAttribute( entityInstanceB1, tea, "D" );
        createTrackedEntityInstanceAttribute( entityInstanceC1, tea, "C" );
        createTrackedEntityInstanceAttribute( entityInstanceD1, tea, "B" );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( tea.getUid(), SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityInstanceIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceD1.getId(), entityInstanceC1.getId(),
            entityInstanceB1.getId() ), teiIdList );
    }

    @Test
    void shouldCountOneEntityWhenOnePresent()
    {
        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        int counter = entityInstanceService.getTrackedEntityInstanceCount( new TrackedEntityInstanceQueryParams(), true,
            true );

        assertEquals( 1, counter );
    }

    @Test
    void shouldCountZeroEntitiesWhenNonePresent()
    {
        int trackedEntitiesCounter = entityInstanceService
            .getTrackedEntityInstanceCount( new TrackedEntityInstanceQueryParams(), true, true );

        assertEquals( 0, trackedEntitiesCounter );
    }

    @Test
    void shouldSortGridByTrackedEntityInstanceIdAscendingWhenParamCreatedAscendingProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        initializeEntityInstance( entityInstanceA1 );
        initializeEntityInstance( entityInstanceB1 );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );
        params.setOrders( List.of( new OrderParam( "created", SortDirection.ASC ) ) );
        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntityInstancesGrid( params );

        assertEquals( 2, grid.getRows().size(),
            "Expected to find 2 rows in the grid, but found " + grid.getRows().size() + " instead" );
        assertEquals( "UID-A1", grid.getRows().get( 0 ).get( 0 ) );
        assertEquals( "UID-B1", grid.getRows().get( 1 ).get( 0 ) );
    }

    @Test
    void shouldSortGridByTrackedEntityInstanceIdDescendingWhenParamCreatedDescendingProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        initializeEntityInstance( entityInstanceA1 );
        initializeEntityInstance( entityInstanceB1 );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );
        params.setOrders( List.of( new OrderParam( "created", SortDirection.DESC ) ) );
        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntityInstancesGrid( params );

        assertEquals( 2, grid.getRows().size(),
            "Expected to find 2 rows in the grid, but found " + grid.getRows().size() + " instead" );
        assertEquals( "UID-B1", grid.getRows().get( 0 ).get( 0 ) );
        assertEquals( "UID-A1", grid.getRows().get( 1 ).get( 0 ) );
    }

    private void initializeEntityInstance( TrackedEntityInstance entityInstance )
    {
        entityInstance.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstance );
        attributeValueService.addTrackedEntityAttributeValue( createTrackedEntityAttributeValue( entityInstance ) );
    }

    private TrackedEntityAttributeValue createTrackedEntityAttributeValue( TrackedEntityInstance trackedEntityInstance )
    {
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        return trackedEntityAttributeValue;
    }

    private void addEnrollment( TrackedEntityInstance entityInstance, Date enrollmentDate, char programStage )
    {
        ProgramStage stage = createProgramStage( programStage, program );
        stage.setSortOrder( 1 );
        programStageService.saveProgramStage( stage );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stage );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        programInstance = new ProgramInstance( enrollmentDate, DateTime.now().toDate(), entityInstance, program );
        programInstance.setUid( "UID-" + programStage );
        programInstance.setOrganisationUnit( organisationUnit );
        event = new Event( programInstance, stage );
        programInstance.setUid( "UID-PSI-" + programStage );
        programInstance.setOrganisationUnit( organisationUnit );

        programInstanceService.addProgramInstance( programInstance );
    }

    private void addEntityInstances()
    {
        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceB1.setTrackedEntityType( trackedEntityType );
        entityInstanceC1.setTrackedEntityType( trackedEntityType );
        entityInstanceD1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceC1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceD1 );
    }

    private void setUpEntityAndAttributeValue( TrackedEntityInstance entityInstance, String attributeValue )
    {
        entityInstance.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( entityInstance );
        trackedEntityAttributeValue.setValue( attributeValue );
        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );
    }

    private TrackedEntityAttribute createTrackedEntityAttribute()
    {
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'X' );
        attributeService.addTrackedEntityAttribute( tea );

        return tea;
    }

    private void createTrackedEntityInstanceAttribute( TrackedEntityInstance trackedEntityInstance,
        TrackedEntityAttribute attribute, String value )
    {
        TrackedEntityAttributeValue trackedEntityAttributeValueA1 = new TrackedEntityAttributeValue();

        trackedEntityAttributeValueA1.setAttribute( attribute );
        trackedEntityAttributeValueA1.setEntityInstance( trackedEntityInstance );
        trackedEntityAttributeValueA1.setValue( value );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValueA1 );
    }
}
