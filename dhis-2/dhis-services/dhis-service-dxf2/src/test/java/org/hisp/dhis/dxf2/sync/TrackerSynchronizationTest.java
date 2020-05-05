package org.hisp.dhis.dxf2.sync;

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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
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
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
public class TrackerSynchronizationTest extends DhisSpringTest
{
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
        TrackedEntityInstance tei = createTrackedEntityInstance( 'a', ou, teaA );
        tei.setTrackedEntityType( tet );

        TrackedEntityAttributeValue teavB = createTrackedEntityAttributeValue( 'b', tei, teaB );
        tei.getTrackedEntityAttributeValues().add( teavB );

        TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue( 'a', tei, teaA );
        tei.getTrackedEntityAttributeValues().add( teavA );

        currentSession.save( ou );
        currentSession.save( tei );
        currentSession.save( teavA );
        currentSession.save( teavB );
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
            _relationshipService, relationshipService, trackedEntityAttributeValueService, manager, _userService,
            dbmsManager, enrollmentService, programInstanceService, currentUserService, schemaService, queryService,
            reservedValueService, trackerAccessManager, fileResourceService, trackerOwnershipAccessManager,
            trackedEntityInstanceAggregate, trackedEntityAttributeStore, notifier, jsonMapper, xmlMapper );

        prepareSyncParams();
        prepareDataForTest();
    }

    private void prepareSyncParams()
    {
        queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setIncludeDeleted( true );
        queryParams.setSynchronizationQuery( true );

        params = new TrackedEntityInstanceParams();
        params.setDataSynchronizationQuery( true );
        params.setIncludeDeleted( true );
    }

    @Test
    public void testSkipSyncFunctionality()
    {
        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis =
            subject.getTrackedEntityInstances( queryParams, params, true );

        assertEquals( 1, fetchedTeis.get( 0 ).getAttributes().size() );
    }
}
