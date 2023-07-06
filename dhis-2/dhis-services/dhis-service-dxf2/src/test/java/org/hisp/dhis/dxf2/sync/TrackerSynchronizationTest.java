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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceEnrollmentParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.trackedentity.JacksonTrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
class TrackerSynchronizationTest extends IntegrationTestBase
{
    // We need to pick a future date as lastUpdated is automatically set to now
    // and cannot be changed
    private static final Date TOMORROW = DateUtils.getDateForTomorrow( 0 );

    private static final String TEI_NOT_IN_SYNC_UID = "ABCDEFGHI01";

    private static final String SYNCHRONIZED_TEI_UID = "ABCDEFGHI02";

    @Autowired
    private UserService _userService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private RelationshipService _relationshipService;

    @Autowired
    private org.hisp.dhis.dxf2.events.relationship.RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DbmsManager dbmsManager;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private ReservedValueService reservedValueService;

    @Autowired
    private TrackerAccessManager trackerAccessManager;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipAccessManager;

    @Autowired
    private TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    @Autowired
    private TrackedEntityAttributeStore trackedEntityAttributeStore;

    @Autowired
    @Qualifier( "xmlMapper" )
    private ObjectMapper xmlMapper;

    private TrackedEntityInstanceService subject;

    private TrackedEntityInstanceQueryParams queryParams;

    private TrackedEntityInstanceParams params;

    private Session currentSession;

    private void prepareDataForTest()
    {
        TrackedEntityAttribute teaA = createTrackedEntityAttribute( 'a' );
        TrackedEntityAttribute teaB = createTrackedEntityAttribute( 'b' );
        teaB.setSkipSynchronization( true );
        currentSession.save( teaA );
        currentSession.save( teaB );
        TrackedEntityType tet = createTrackedEntityType( 'a' );
        TrackedEntityTypeAttribute tetaA = new TrackedEntityTypeAttribute( tet, teaA, true, false );
        TrackedEntityTypeAttribute tetaB = new TrackedEntityTypeAttribute( tet, teaB, true, false );
        tet.getTrackedEntityTypeAttributes().add( tetaA );
        tet.getTrackedEntityTypeAttributes().add( tetaB );
        currentSession.save( tet );
        OrganisationUnit ou = createOrganisationUnit( 'a' );
        manager.save( ou );
        TrackedEntityInstance teiToSync = createTrackedEntityInstance( 'a', ou, teaA );
        teiToSync.setTrackedEntityType( tet );
        teiToSync.setUid( TEI_NOT_IN_SYNC_UID );
        TrackedEntityAttributeValue teavB = createTrackedEntityAttributeValue( 'b', teiToSync, teaB );
        TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue( 'a', teiToSync, teaA );
        manager.save( teiToSync );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teavA );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teavB );
        teiToSync.getTrackedEntityAttributeValues().addAll( List.of( teavA, teavB ) );
        manager.update( teiToSync );
        TrackedEntityInstance alreadySynchronizedTei = createTrackedEntityInstance( 'b', ou );
        alreadySynchronizedTei.setTrackedEntityType( tet );
        alreadySynchronizedTei.setLastSynchronized( TOMORROW );
        alreadySynchronizedTei.setUid( SYNCHRONIZED_TEI_UID );
        manager.save( alreadySynchronizedTei );
    }

    @Override
    public void setUpTest()
    {
        userService = _userService;
        currentSession = sessionFactory.getCurrentSession();
        User user = createUser( "userUID0001" );
        currentSession.save( user );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        subject = new JacksonTrackedEntityInstanceService( teiService, trackedEntityAttributeService,
            _relationshipService, relationshipService, relationshipTypeService, trackedEntityAttributeValueService,
            manager, _userService, dbmsManager, enrollmentService, programInstanceService, currentUserService,
            schemaService, queryService, reservedValueService, trackerAccessManager, fileResourceService,
            trackerOwnershipAccessManager, trackedEntityInstanceAggregate, trackedEntityAttributeStore,
            trackedEntityInstanceAuditService, trackedEntityTypeService, notifier, jsonMapper, xmlMapper );
        prepareSyncParams();
        prepareDataForTest();
    }

    private void prepareSyncParams()
    {
        queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setIncludeDeleted( true );
        params = new TrackedEntityInstanceParams( false, TrackedEntityInstanceEnrollmentParams.FALSE, false, false,
            true, true );
    }

    @Test
    void shouldReturnAllTeisWhenNotSyncQuery()
    {
        queryParams.setSynchronizationQuery( false );
        queryParams.setSkipChangedBefore( null );

        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis = subject
            .getTrackedEntityInstances( queryParams, params, true, true );

        assertContainsOnly(
            fetchedTeis.stream().map( t -> t.getTrackedEntityInstance() ).collect( Collectors.toList() ),
            TEI_NOT_IN_SYNC_UID, SYNCHRONIZED_TEI_UID );
        assertEquals( 1, getTeiByUid( fetchedTeis, TEI_NOT_IN_SYNC_UID ).getAttributes().size() );
    }

    @Test
    void shouldNotSynchronizeTeiUpdatedBeforeLastSync()
    {
        queryParams.setSynchronizationQuery( true );
        queryParams.setSkipChangedBefore( null );

        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis = subject
            .getTrackedEntityInstances( queryParams, params, true, true );

        assertContainsOnly(
            fetchedTeis.stream().map( t -> t.getTrackedEntityInstance() ).collect( Collectors.toList() ),
            TEI_NOT_IN_SYNC_UID );
        assertEquals( 1, getTeiByUid( fetchedTeis, TEI_NOT_IN_SYNC_UID ).getAttributes().size() );
    }

    @Test
    void shouldNotSynchronizeTeiUpdatedBeforeSkipChangedBeforeDate()
    {
        queryParams.setSynchronizationQuery( true );
        queryParams.setSkipChangedBefore( TOMORROW );

        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis = subject
            .getTrackedEntityInstances( queryParams, params, true, true );

        assertIsEmpty( fetchedTeis );
    }

    private org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance getTeiByUid(
        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> teis, String teiUid )
    {
        return teis.stream().filter( t -> Objects.equals( t.getTrackedEntityInstance(), teiUid ) ).findAny().get();
    }
}
