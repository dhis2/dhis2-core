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
package org.hisp.dhis.dxf2.events.relationship;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class JacksonRelationshipServiceTest
{
    @Mock
    protected DbmsManager dbmsManager;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SchemaService schemaService;

    @Mock
    private QueryService queryService;

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @Mock
    private org.hisp.dhis.relationship.RelationshipService relationshipService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EventService eventService;

    @Mock
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiDaoService;

    @Mock
    private UserService userService;

    @Mock
    private ObjectMapper jsonMapper;

    @Mock
    private ObjectMapper xmlMapper;

    @InjectMocks
    private JacksonRelationshipService subject;

    private Relationship relationship;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    private static final String RELATIONSHIP_UID = "relationship uid";

    @BeforeEach
    public void setUp()
        throws IllegalAccessException
    {
        RelationshipType relationshipType = createRelationshipTypeWithTeiConstraint();
        relationship = createTei2TeiRelationship( relationshipType );
        relationship.setRelationship( RELATIONSHIP_UID );

        initFakeCaches( relationship, relationshipType );
    }

    @Test
    void verifyRelationshipIsImportedIfDoesNotExist()
    {
        when(
            relationshipService.getRelationshipByRelationship( any( org.hisp.dhis.relationship.Relationship.class ) ) )
                .thenReturn( Optional.empty() );

        ImportSummary importSummary = subject.addRelationship( relationship, rnd.nextObject( ImportOptions.class ) );

        assertThat( importSummary.getStatus(), is( ImportStatus.SUCCESS ) );
        assertThat( importSummary.getImportCount().getImported(), is( 1 ) );
    }

    @Test
    void verifyRelationshipIsNotImportedWhenDoesExist()
    {
        org.hisp.dhis.relationship.Relationship daoRelationship = new org.hisp.dhis.relationship.Relationship();
        daoRelationship.setUid( "12345" );

        when(
            relationshipService.getRelationshipByRelationship( any( org.hisp.dhis.relationship.Relationship.class ) ) )
                .thenReturn( Optional.of( daoRelationship ) );

        ImportSummary importSummary = subject.addRelationship( relationship, rnd.nextObject( ImportOptions.class ) );

        assertThat( importSummary.getStatus(), is( ImportStatus.ERROR ) );
        assertThat( importSummary.getImportCount().getImported(), is( 0 ) );
        assertThat( importSummary.getReference(), is( daoRelationship.getUid() ) );
        assertThat( importSummary.getDescription(),
            is( "Relationship " + daoRelationship.getUid() + " already exists" ) );
    }

    @Test
    void verifySoftRelationshipIsNotUpdated()
    {
        org.hisp.dhis.relationship.Relationship daoRelationship = new org.hisp.dhis.relationship.Relationship();
        daoRelationship.setUid( RELATIONSHIP_UID );
        daoRelationship.setDeleted( true );

        when( relationshipService.getRelationshipIncludeDeleted( RELATIONSHIP_UID ) ).thenReturn( daoRelationship );

        ImportSummary importSummary = subject.updateRelationship( relationship, rnd.nextObject( ImportOptions.class ) );

        assertThat( importSummary.getStatus(), is( ImportStatus.ERROR ) );
        assertThat( importSummary.getImportCount().getImported(), is( 0 ) );
        assertThat( importSummary.getReference(), is( RELATIONSHIP_UID ) );
        assertThat( importSummary.getDescription(),
            is( "Relationship '" + RELATIONSHIP_UID + "' is already deleted and cannot be modified." ) );
    }

    private void initFakeCaches( Relationship relationship, RelationshipType relationshipType )
        throws IllegalAccessException
    {
        // init relationship type cache
        HashMap<String, RelationshipType> relationshipTypeCache = new HashMap<>();
        relationshipTypeCache.put( relationship.getRelationshipType(), relationshipType );

        // init tracked entity instance cache
        HashMap<String, TrackedEntityInstance> trackedEntityInstanceCache = new HashMap<>();
        trackedEntityInstanceCache.put( relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance(),
            new TrackedEntityInstance()
            {
                {
                    setTrackedEntityType( relationshipType.getFromConstraint().getTrackedEntityType() );
                }
            } );
        trackedEntityInstanceCache.put( relationship.getTo().getTrackedEntityInstance().getTrackedEntityInstance(),
            new TrackedEntityInstance()
            {
                {
                    setTrackedEntityType( relationshipType.getToConstraint().getTrackedEntityType() );
                }
            } );

        FieldUtils.writeField( subject, "relationshipTypeCache", relationshipTypeCache, true );
        FieldUtils.writeField( subject, "trackedEntityInstanceCache", trackedEntityInstanceCache, true );

    }

    private Relationship createTei2TeiRelationship( RelationshipType relationshipType )
    {
        Relationship relationship = new Relationship();

        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance(
            rnd.nextObject( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance.class ) );

        RelationshipItem to = new RelationshipItem();
        to.setTrackedEntityInstance(
            rnd.nextObject( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance.class ) );

        relationship.setFrom( from );
        relationship.setTo( to );
        relationship.setRelationshipType( relationshipType.getUid() );
        return relationship;
    }

    private RelationshipType createRelationshipTypeWithTeiConstraint()
    {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( CodeGenerator.generateUid() );

        RelationshipConstraint from = new RelationshipConstraint();
        from.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        from.setTrackedEntityType( new TrackedEntityType( "a", "b" ) );
        RelationshipConstraint to = rnd.nextObject( RelationshipConstraint.class );
        to.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        to.setTrackedEntityType( new TrackedEntityType( "b", "c" ) );
        relationshipType.setFromConstraint( from );
        relationshipType.setToConstraint( to );

        return relationshipType;
    }
}