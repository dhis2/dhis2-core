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
package org.hisp.dhis.tracker.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class RelationshipTrackerConverterServiceTest extends DhisConvenienceTest
{

    private final static String RELATIONSHIP_A_UID = "Nva3Xj2j75W";

    private final static String RELATIONSHIP_B_UID = "HiXiipNGsxT";

    private final static String TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE = "xLmPUYJX8Ks";

    private final static String TEI_TO_EVENT_RELATIONSHIP_TYPE = "TV9oB9LT3sh";

    private final static String TEI = "IOR1AXXl24H";

    private final static String ENROLLMENT = "TvctPPhpD8u";

    private final static String EVENT = "D9PbzJY8bJO";

    private final static boolean IS_BIDIRECTIONAL = false;

    private Program program;

    private ProgramStage programStage;

    private TrackedEntityType trackedEntityType;

    private OrganisationUnit organisationUnit;

    private TrackedEntityInstance trackedEntityInstance;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private RelationshipType teiToEnrollmentRelationshipType;

    private RelationshipType teiToEventRelationshipType;

    @Mock
    private TrackerPreheat preheat;

    private RelationshipTrackerConverterService relationshipConverterService = new RelationshipTrackerConverterService();

    @BeforeEach
    void setUp()
    {
        program = createProgram( 'A' );
        programStage = createProgramStage( 'A', program );
        trackedEntityType = createTrackedEntityType( 'A' );
        organisationUnit = createOrganisationUnit( 'A' );
        trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.setUid( TEI );
        programInstance = createProgramInstance( program, trackedEntityInstance, organisationUnit );
        programInstance.setUid( ENROLLMENT );
        programStageInstance = createProgramStageInstance( programStage, programInstance, organisationUnit );
        programStageInstance.setUid( EVENT );

        teiToEnrollmentRelationshipType = createTeiToEnrollmentRelationshipType( 'A', program, trackedEntityType,
            IS_BIDIRECTIONAL );
        teiToEnrollmentRelationshipType.setUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE );

        teiToEventRelationshipType = createTeiToEventRelationshipType( 'A', program, trackedEntityType,
            IS_BIDIRECTIONAL );
        teiToEventRelationshipType.setUid( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        setUpMocks();
    }

    @Test
    void testConverterFromRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService.from( preheat,
            relationships() );
        assertNotNull( from );
        assertEquals( 2, from.size() );
        from.forEach( relationship -> {
            if ( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( ENROLLMENT, relationship.getTo().getProgramInstance().getUid() );
            }
            else if ( TEI_TO_EVENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( EVENT, relationship.getTo().getProgramStageInstance().getUid() );
            }
            else
            {
                fail( "Unexpected relationshipType found." );
            }
            assertNotNull( relationship.getFrom() );
            assertNotNull( relationship.getTo() );
        } );
    }

    @Test
    void testConverterToRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService.from( preheat,
            relationships() );
        List<Relationship> to = relationshipConverterService.to( from );
        assertNotNull( to );
        assertEquals( 2, to.size() );
        from.forEach( relationship -> {
            if ( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( ENROLLMENT, relationship.getTo().getProgramInstance().getUid() );
            }
            else if ( TEI_TO_EVENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( EVENT, relationship.getTo().getProgramStageInstance().getUid() );
            }
            else
            {
                fail( "Unexpected relationshipType found." );
            }
            assertNotNull( relationship.getFrom() );
            assertNotNull( relationship.getTo() );
        } );
    }

    private Relationship relationshipA()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_A_UID )
            .from( RelationshipItem.builder()
                .trackedEntity( RelationshipItem.TrackedEntity.builder().trackedEntity( TEI ).build() ).build() )
            .to( RelationshipItem.builder()
                .enrollment( RelationshipItem.Enrollment.builder().enrollment( ENROLLMENT ).build() ).build() )
            .relationshipType( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE )
            .build();
    }

    private Relationship relationshipB()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_B_UID )
            .from( RelationshipItem.builder()
                .trackedEntity( RelationshipItem.TrackedEntity.builder().trackedEntity( TEI ).build() ).build() )
            .to( RelationshipItem.builder().event( RelationshipItem.Event.builder().event( EVENT ).build() ).build() )
            .relationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE )
            .build();
    }

    private org.hisp.dhis.relationship.Relationship relationshipAFromDB()
    {
        return createTeiToEnrollmentRelationship( trackedEntityInstance, programInstance,
            teiToEnrollmentRelationshipType );
    }

    private org.hisp.dhis.relationship.Relationship relationshipBFromDB()
    {
        return createTeiToEventRelationship( trackedEntityInstance, programStageInstance, teiToEventRelationshipType );
    }

    private List<Relationship> relationships()
    {
        return List.of( relationshipA(), relationshipB() );
    }

    private void setUpMocks()
    {
        when( preheat.getRelationship( relationshipA() ) ).thenReturn( relationshipAFromDB() );
        when( preheat.getRelationship( relationshipB() ) ).thenReturn( relationshipBFromDB() );
        when( preheat.get( RelationshipType.class, TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE ) )
            .thenReturn( teiToEnrollmentRelationshipType );
        when( preheat.get( RelationshipType.class, TEI_TO_EVENT_RELATIONSHIP_TYPE ) )
            .thenReturn( teiToEventRelationshipType );
        when( preheat.getTrackedEntity( TEI ) ).thenReturn( trackedEntityInstance );
        when( preheat.getEnrollment( ENROLLMENT ) ).thenReturn( programInstance );
        when( preheat.getEvent( EVENT ) ).thenReturn( programStageInstance );
    }
}
