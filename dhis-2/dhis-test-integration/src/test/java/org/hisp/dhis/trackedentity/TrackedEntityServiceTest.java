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
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
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
class TrackedEntityServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private TrackedEntityService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EnrollmentService enrollmentService;

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

    private Enrollment enrollment;

    private Program program;

    private TrackedEntity entityInstanceA1;

    private TrackedEntity entityInstanceB1;

    private TrackedEntity entityInstanceC1;

    private TrackedEntity entityInstanceD1;

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
        entityInstanceA1 = createTrackedEntity( organisationUnit );
        entityInstanceB1 = createTrackedEntity( organisationUnit );
        entityInstanceC1 = createTrackedEntity( organisationUnit );
        entityInstanceD1 = createTrackedEntity( organisationUnit );
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
        enrollment = new Enrollment( enrollmentDate.toDate(), incidentDate.toDate(), entityInstanceA1,
            program );
        enrollment.setUid( "UID-A" );
        enrollment.setOrganisationUnit( organisationUnit );
        event = new Event( enrollment, stageA );
        enrollment.setUid( "UID-PSI-A" );
        enrollment.setOrganisationUnit( organisationUnit );

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
    void testSaveTrackedEntity()
    {
        long idA = entityInstanceService.addTrackedEntity( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntity( entityInstanceB1 );
        assertNotNull( entityInstanceService.getTrackedEntity( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntity( idB ) );
    }

    @Test
    void testDeleteTrackedEntity()
    {
        long idA = entityInstanceService.addTrackedEntity( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntity( entityInstanceB1 );
        TrackedEntity teiA = entityInstanceService.getTrackedEntity( idA );
        TrackedEntity teiB = entityInstanceService.getTrackedEntity( idB );
        assertNotNull( teiA );
        assertNotNull( teiB );
        entityInstanceService.deleteTrackedEntity( entityInstanceA1 );
        assertNull( entityInstanceService.getTrackedEntity( teiA.getUid() ) );
        assertNotNull( entityInstanceService.getTrackedEntity( teiB.getUid() ) );
        entityInstanceService.deleteTrackedEntity( entityInstanceB1 );
        assertNull( entityInstanceService.getTrackedEntity( teiA.getUid() ) );
        assertNull( entityInstanceService.getTrackedEntity( teiB.getUid() ) );
    }

    @Test
    void testDeleteTrackedEntityAndLinkedEnrollmentsAndEvents()
    {
        long idA = entityInstanceService.addTrackedEntity( entityInstanceA1 );
        long psIdA = enrollmentService.addEnrollment( enrollment );
        long eventIdA = eventService.addEvent( event );
        enrollment.setEvents( Set.of( event ) );
        entityInstanceA1.setEnrollments( Set.of( enrollment ) );
        enrollmentService.updateEnrollment( enrollment );
        entityInstanceService.updateTrackedEntity( entityInstanceA1 );
        TrackedEntity teiA = entityInstanceService.getTrackedEntity( idA );
        Enrollment psA = enrollmentService.getEnrollment( psIdA );
        Event eventA = eventService.getEvent( eventIdA );
        assertNotNull( teiA );
        assertNotNull( psA );
        assertNotNull( eventA );
        entityInstanceService.deleteTrackedEntity( entityInstanceA1 );
        assertNull( entityInstanceService.getTrackedEntity( teiA.getUid() ) );
        assertNull( enrollmentService.getEnrollment( psIdA ) );
        assertNull( eventService.getEvent( eventIdA ) );
    }

    @Test
    void testUpdateTrackedEntity()
    {
        long idA = entityInstanceService.addTrackedEntity( entityInstanceA1 );
        assertNotNull( entityInstanceService.getTrackedEntity( idA ) );
        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntity( entityInstanceA1 );
        assertEquals( "B", entityInstanceService.getTrackedEntity( idA ).getName() );
    }

    @Test
    void testGetTrackedEntityById()
    {
        long idA = entityInstanceService.addTrackedEntity( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntity( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntity( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntity( idB ) );
    }

    @Test
    void testGetTrackedEntityByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );
        entityInstanceService.addTrackedEntity( entityInstanceA1 );
        entityInstanceService.addTrackedEntity( entityInstanceB1 );
        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntity( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntity( "B1" ) );
    }

    @Test
    void testStoredByColumnForTrackedEntity()
    {
        entityInstanceA1.setStoredBy( "test" );
        entityInstanceService.addTrackedEntity( entityInstanceA1 );
        TrackedEntity tei = entityInstanceService.getTrackedEntity( entityInstanceA1.getUid() );
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
        entityInstanceService.addTrackedEntity( entityInstanceA1 );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();

        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setTrackedEntity( entityInstanceA1 );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntitiesGrid( params );

        assertEquals( 1, grid.getHeight() );
    }

    @Test
    void testTrackedEntityGridWithNoFilterableAttributes()
    {
        injectSecurityContext( superUser );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntitiesGrid( params ) );
    }

    @Test
    void testTrackedEntityGridWithNoDisplayAttributes()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( false );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );

        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        assertThrows( IllegalArgumentException.class,
            () -> entityInstanceService.getTrackedEntitiesGrid( params ) );
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

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "createdAt", SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

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

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAt", SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceB1.getId(), entityInstanceC1.getId(),
            entityInstanceD1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByIdWhenParamTrackedEntitySupplied()
    {
        injectSecurityContext( superUser );

        addEntityInstances();

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "trackedEntity", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

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

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "updatedAtClient", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceD1.getId(), entityInstanceA1.getId(), entityInstanceB1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldOrderEntitiesByEnrollmentDateWhenParamEnrolledAtSupplied()
    {
        injectSecurityContext( superUser );

        addEntityInstances();
        enrollmentService.addEnrollment( enrollment );
        addEnrollment( entityInstanceB1, DateTime.now().plusDays( 2 ).toDate(), 'B' );
        addEnrollment( entityInstanceC1, DateTime.now().minusDays( 2 ).toDate(), 'C' );
        addEnrollment( entityInstanceD1, DateTime.now().plusDays( 1 ).toDate(), 'D' );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "enrolledAt", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

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

        enrollmentService.addEnrollment( enrollment );
        addEnrollment( entityInstanceB1, DateTime.now().plusDays( 2 ).toDate(), 'B' );
        addEnrollment( entityInstanceC1, DateTime.now().minusDays( 2 ).toDate(), 'C' );
        addEnrollment( entityInstanceD1, DateTime.now().plusDays( 1 ).toDate(), 'D' );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( "inactive", SortDirection.DESC ),
            new OrderParam( "enrolledAt", SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByIdWhenNoOrderParamProvided()
    {
        injectSecurityContext( superUser );
        addEntityInstances();

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

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

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

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

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( trackedEntityAttribute.getUid(), SortDirection.DESC ),
            new OrderParam( "inactive", SortDirection.ASC ) ) );
        params.setAttributes( List.of( new QueryItem( trackedEntityAttribute ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceA1.getId(), entityInstanceD1.getId(),
            entityInstanceC1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeDescendingWhenAttributeDescendingProvided()
    {
        injectSecurityContext( superUser );

        TrackedEntityAttribute tea = createTrackedEntityAttribute();

        addEntityInstances();

        createTrackedEntityAttribute( entityInstanceA1, tea, "A" );
        createTrackedEntityAttribute( entityInstanceB1, tea, "D" );
        createTrackedEntityAttribute( entityInstanceC1, tea, "C" );
        createTrackedEntityAttribute( entityInstanceD1, tea, "B" );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( tea.getUid(), SortDirection.DESC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceB1.getId(), entityInstanceC1.getId(), entityInstanceD1.getId(),
            entityInstanceA1.getId() ), teiIdList );
    }

    @Test
    void shouldSortEntitiesByAttributeAscendingWhenAttributeAscendingProvided()
    {
        injectSecurityContext( superUser );

        TrackedEntityAttribute tea = createTrackedEntityAttribute();

        addEntityInstances();

        createTrackedEntityAttribute( entityInstanceA1, tea, "A" );
        createTrackedEntityAttribute( entityInstanceB1, tea, "D" );
        createTrackedEntityAttribute( entityInstanceC1, tea, "C" );
        createTrackedEntityAttribute( entityInstanceD1, tea, "B" );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();

        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setOrders( List.of( new OrderParam( tea.getUid(), SortDirection.ASC ) ) );

        List<Long> teiIdList = entityInstanceService.getTrackedEntityIds( params, true, true );

        assertEquals( List.of( entityInstanceA1.getId(), entityInstanceD1.getId(), entityInstanceC1.getId(),
            entityInstanceB1.getId() ), teiIdList );
    }

    @Test
    void shouldCountOneEntityWhenOnePresent()
    {
        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntity( entityInstanceA1 );

        int counter = entityInstanceService.getTrackedEntityCount( new TrackedEntityQueryParams(), true,
            true );

        assertEquals( 1, counter );
    }

    @Test
    void shouldCountZeroEntitiesWhenNonePresent()
    {
        int trackedEntitiesCounter = entityInstanceService
            .getTrackedEntityCount( new TrackedEntityQueryParams(), true, true );

        assertEquals( 0, trackedEntitiesCounter );
    }

    @Test
    void shouldSortGridByTrackedEntityIdAscendingWhenParamCreatedAscendingProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        initializeEntityInstance( entityInstanceA1 );
        initializeEntityInstance( entityInstanceB1 );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );
        params.setOrders( List.of( new OrderParam( "created", SortDirection.ASC ) ) );
        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntitiesGrid( params );

        assertEquals( 2, grid.getRows().size(),
            "Expected to find 2 rows in the grid, but found " + grid.getRows().size() + " instead" );
        assertEquals( "UID-A1", grid.getRows().get( 0 ).get( 0 ) );
        assertEquals( "UID-B1", grid.getRows().get( 1 ).get( 0 ) );
    }

    @Test
    void shouldSortGridByTrackedEntityIdDescendingWhenParamCreatedDescendingProvided()
    {
        injectSecurityContext( superUser );
        trackedEntityAttribute.setDisplayInListNoProgram( true );
        attributeService.addTrackedEntityAttribute( trackedEntityAttribute );

        User user = createAndAddUser( false, "attributeFilterUser", Set.of( organisationUnit ),
            Set.of( organisationUnit ) );
        injectSecurityContext( user );

        initializeEntityInstance( entityInstanceA1 );
        initializeEntityInstance( entityInstanceB1 );

        TrackedEntityQueryParams params = new TrackedEntityQueryParams();
        params.setOrganisationUnits( Set.of( organisationUnit ) );
        params.setTrackedEntityType( trackedEntityType );
        params.setOrders( List.of( new OrderParam( "created", SortDirection.DESC ) ) );
        params.setQuery( new QueryFilter( QueryOperator.LIKE, ATTRIBUTE_VALUE ) );

        Grid grid = entityInstanceService.getTrackedEntitiesGrid( params );

        assertEquals( 2, grid.getRows().size(),
            "Expected to find 2 rows in the grid, but found " + grid.getRows().size() + " instead" );
        assertEquals( "UID-B1", grid.getRows().get( 0 ).get( 0 ) );
        assertEquals( "UID-A1", grid.getRows().get( 1 ).get( 0 ) );
    }

    private void initializeEntityInstance( TrackedEntity entityInstance )
    {
        entityInstance.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntity( entityInstance );
        attributeValueService.addTrackedEntityAttributeValue( createTrackedEntityAttributeValue( entityInstance ) );
    }

    private TrackedEntityAttributeValue createTrackedEntityAttributeValue( TrackedEntity trackedEntity )
    {
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setTrackedEntity( trackedEntity );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        return trackedEntityAttributeValue;
    }

    private void addEnrollment( TrackedEntity entityInstance, Date enrollmentDate, char programStage )
    {
        ProgramStage stage = createProgramStage( programStage, program );
        stage.setSortOrder( 1 );
        programStageService.saveProgramStage( stage );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stage );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        enrollment = new Enrollment( enrollmentDate, DateTime.now().toDate(), entityInstance, program );
        enrollment.setUid( "UID-" + programStage );
        enrollment.setOrganisationUnit( organisationUnit );
        event = new Event( enrollment, stage );
        enrollment.setUid( "UID-PSI-" + programStage );
        enrollment.setOrganisationUnit( organisationUnit );

        enrollmentService.addEnrollment( enrollment );
    }

    private void addEntityInstances()
    {
        entityInstanceA1.setTrackedEntityType( trackedEntityType );
        entityInstanceB1.setTrackedEntityType( trackedEntityType );
        entityInstanceC1.setTrackedEntityType( trackedEntityType );
        entityInstanceD1.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntity( entityInstanceA1 );
        entityInstanceService.addTrackedEntity( entityInstanceB1 );
        entityInstanceService.addTrackedEntity( entityInstanceC1 );
        entityInstanceService.addTrackedEntity( entityInstanceD1 );
    }

    private void setUpEntityAndAttributeValue( TrackedEntity entityInstance, String attributeValue )
    {
        entityInstance.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntity( entityInstance );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setTrackedEntity( entityInstance );
        trackedEntityAttributeValue.setValue( attributeValue );
        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );
    }

    private TrackedEntityAttribute createTrackedEntityAttribute()
    {
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'X' );
        attributeService.addTrackedEntityAttribute( tea );

        return tea;
    }

    private void createTrackedEntityAttribute( TrackedEntity trackedEntity,
        TrackedEntityAttribute attribute, String value )
    {
        TrackedEntityAttributeValue trackedEntityAttributeValueA1 = new TrackedEntityAttributeValue();

        trackedEntityAttributeValueA1.setAttribute( attribute );
        trackedEntityAttributeValueA1.setTrackedEntity( trackedEntity );
        trackedEntityAttributeValueA1.setValue( value );

        attributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValueA1 );
    }
}
