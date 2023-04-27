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
package org.hisp.dhis.tracker.imports.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
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

    private final static String TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE = "xLmPUYJX8Ks";

    private final static String TEI_TO_EVENT_RELATIONSHIP_TYPE = "TV9oB9LT3sh";

    private final static String TEI = "TEI_UID";

    private final static String ENROLLMENT = "ENROLLMENT_UID";

    private final static String EVENT = "EVENT_UID";

    private final static String RELATIONSHIP_A = "RELATIONSHIP_A_UID";

    private final static String RELATIONSHIP_B = "RELATIONSHIP_B_UID";

    private RelationshipType teiToEnrollment;

    private RelationshipType teiToEvent;

    private TrackedEntityInstance tei;

    private ProgramInstance pi;

    private Event event;

    private TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverterService;

    @Mock
    public TrackerPreheat preheat;

    @BeforeEach
    protected void setupTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        Program program = createProgram( 'A' );
        TrackedEntityType teiType = createTrackedEntityType( 'A' );

        teiToEnrollment = createTeiToEnrollmentRelationshipType( 'A', program, teiType, false );
        teiToEnrollment.setUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE );

        teiToEvent = createTeiToEventRelationshipType( 'B', program, teiType, false );
        teiToEvent.setUid( TEI_TO_EVENT_RELATIONSHIP_TYPE );

        tei = createTrackedEntityInstance( organisationUnit );
        tei.setTrackedEntityType( teiType );
        tei.setUid( TEI );
        pi = createProgramInstance( program, tei, organisationUnit );
        pi.setUid( ENROLLMENT );
        event = createProgramStageInstance( createProgramStage( 'A', program ), pi, organisationUnit );
        event.setUid( EVENT );

        relationshipConverterService = new RelationshipTrackerConverterService();
    }

    @Test
    void testConverterFromRelationships()
    {
        when( preheat.getRelationship( RELATIONSHIP_A ) ).thenReturn( relationshipAFromDB() );
        when( preheat.getRelationship( RELATIONSHIP_B ) ).thenReturn( relationshipBFromDB() );
        when( preheat.getRelationshipType( MetadataIdentifier.ofUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE ) ) )
            .thenReturn( teiToEnrollment );
        when( preheat.getRelationshipType( MetadataIdentifier.ofUid( TEI_TO_EVENT_RELATIONSHIP_TYPE ) ) )
            .thenReturn( teiToEvent );
        when( preheat.getTrackedEntity( TEI ) ).thenReturn( tei );
        when( preheat.getEnrollment( ENROLLMENT ) ).thenReturn( pi );
        when( preheat.getEvent( EVENT ) ).thenReturn( event );

        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService
            .from( preheat, List.of( relationshipA(), relationshipB() ) );
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
                assertEquals( EVENT, relationship.getTo().getEvent().getUid() );
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
        List<Relationship> to = relationshipConverterService
            .to( List.of( relationshipAFromDB(), relationshipBFromDB() ) );
        assertNotNull( to );
        assertEquals( 2, to.size() );
        to.forEach( relationship -> {
            if ( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getIdentifier() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntity() );
                assertEquals( ENROLLMENT, relationship.getTo().getEnrollment() );
            }
            else if ( TEI_TO_EVENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getIdentifier() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntity() );
                assertEquals( EVENT, relationship.getTo().getEvent() );
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
            .relationship( RELATIONSHIP_A )
            .relationshipType( MetadataIdentifier.ofUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE ) )
            .from( RelationshipItem.builder().trackedEntity( TEI ).build() )
            .to( RelationshipItem.builder().enrollment( ENROLLMENT ).build() )
            .build();
    }

    private Relationship relationshipB()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_B )
            .relationshipType( MetadataIdentifier.ofUid( TEI_TO_EVENT_RELATIONSHIP_TYPE ) )
            .from( RelationshipItem.builder().trackedEntity( TEI ).build() )
            .to( RelationshipItem.builder().event( EVENT ).build() )
            .build();
    }

    private org.hisp.dhis.relationship.Relationship relationshipAFromDB()
    {
        return createTeiToProgramInstanceRelationship( tei, pi, teiToEnrollment );
    }

    private org.hisp.dhis.relationship.Relationship relationshipBFromDB()
    {
        return createTeiToProgramStageInstanceRelationship( tei, event, teiToEvent );
    }
}
