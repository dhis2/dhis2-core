package org.hisp.dhis.tracker.converter;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * @author Enrico Colasante
 */
public class RelationshipTrackerConverterServiceTest
    extends DhisSpringTest
{

    private final static String MOTHER_TO_CHILD_RELATIONSHIP_TYPE = "dDrh5UyCyvQ";

    private final static String CHILD_TO_MOTHER_RELATIONSHIP_TYPE = "tBeOL0DL026";

    private final static String MOTHER = "Ea0rRdBPAIp";

    private final static String CHILD = "G1afLIEKt8A";

    @Autowired
    @Qualifier( "relationshipTrackerConverterService" )
    private TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverterService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private TrackerBundle trackerBundle;

    @Override
    protected void setUpTest()
        throws IOException
    {
        preCreateInjectAdminUserWithoutPersistence();

        renderService = _renderService;
        userService = _userService;

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        TrackedEntityInstance trackedEntityInstanceA = createTrackedEntityInstance( 'A', organisationUnit,
            trackedEntityAttribute );
        trackedEntityInstanceA.setUid( MOTHER );
        TrackedEntityInstance trackedEntityInstanceB = createTrackedEntityInstance( 'B', organisationUnit,
            trackedEntityAttribute );
        trackedEntityInstanceB.setUid( CHILD );

        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceA );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceB );

        RelationshipType relationshipTypeA = createPersonToPersonRelationshipType( 'A', null, trackedEntityType,
            false );
        relationshipTypeA.setUid( MOTHER_TO_CHILD_RELATIONSHIP_TYPE );
        RelationshipType relationshipTypeB = createPersonToPersonRelationshipType( 'B', null, trackedEntityType,
            false );
        relationshipTypeB.setUid( CHILD_TO_MOTHER_RELATIONSHIP_TYPE );
        relationshipTypeService.addRelationshipType( relationshipTypeA );
        relationshipTypeService.addRelationshipType( relationshipTypeB );

        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/relationships.json" ).getInputStream(),
                TrackerBundleParams.class );

        User adminUser = createAndInjectAdminUser();

        TrackerImportParams trackerImportParams =
            TrackerImportParams
                .builder()
                .relationships( trackerBundleParams.getRelationships() )
                .user( adminUser )
                .build();

        trackerBundle = trackerBundleService.create( trackerImportParams.toTrackerBundleParams() ).get( 0 );
    }

    @Test
    public void testConverterFromRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService
            .from( trackerBundle.getPreheat(), trackerBundle.getRelationships() );

        assertNotNull( from );
        assertEquals( 2, from.size() );

        org.hisp.dhis.relationship.Relationship relationship1 = from.get( 0 );
        assertNotNull( relationship1 );
        assertNotNull( relationship1.getFrom() );
        assertNotNull( relationship1.getTo() );
        assertEquals( MOTHER_TO_CHILD_RELATIONSHIP_TYPE, relationship1.getRelationshipType().getUid() );
        assertEquals( MOTHER, relationship1.getFrom().getTrackedEntityInstance().getUid() );
        assertEquals( CHILD, relationship1.getTo().getTrackedEntityInstance().getUid() );

        org.hisp.dhis.relationship.Relationship relationship2 = from.get( 1 );
        assertNotNull( relationship2 );
        assertNotNull( relationship2.getFrom() );
        assertNotNull( relationship2.getTo() );
        assertEquals( CHILD_TO_MOTHER_RELATIONSHIP_TYPE, relationship2.getRelationshipType().getUid() );
        assertEquals( CHILD, relationship2.getFrom().getTrackedEntityInstance().getUid() );
        assertEquals( MOTHER, relationship2.getTo().getTrackedEntityInstance().getUid() );
    }

    @Test
    public void testConverterToRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService
            .from( trackerBundle.getPreheat(), trackerBundle.getRelationships() );

        List<Relationship> to = relationshipConverterService.to( from );

        assertNotNull( to );
        assertEquals( 2, to.size() );

        Relationship relationship1 = to.get( 0 );
        assertNotNull( relationship1 );
        assertNotNull( relationship1.getFrom() );
        assertNotNull( relationship1.getTo() );
        assertEquals( MOTHER_TO_CHILD_RELATIONSHIP_TYPE, relationship1.getRelationshipType() );
        assertEquals( MOTHER, relationship1.getFrom().getTrackedEntity() );
        assertEquals( CHILD, relationship1.getTo().getTrackedEntity() );

        Relationship relationship2 = to.get( 1 );
        assertNotNull( relationship2 );
        assertNotNull( relationship2.getFrom() );
        assertNotNull( relationship2.getTo() );
        assertEquals( CHILD_TO_MOTHER_RELATIONSHIP_TYPE, relationship2.getRelationshipType() );
        assertEquals( CHILD, relationship2.getFrom().getTrackedEntity() );
        assertEquals( MOTHER, relationship2.getTo().getTrackedEntity() );
    }
}
