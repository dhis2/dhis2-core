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
package org.hisp.dhis.tracker;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.tracker.domain.MetadataIdentifier.ofAttribute;
import static org.hisp.dhis.tracker.domain.MetadataIdentifier.ofCode;
import static org.hisp.dhis.tracker.domain.MetadataIdentifier.ofName;
import static org.hisp.dhis.tracker.domain.MetadataIdentifier.ofUid;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackerIdentifierCollectorTest
{

    private TrackerIdentifierCollector collector;

    @BeforeEach
    void setUp()
    {

        ProgramRuleService programRuleService = mock( ProgramRuleService.class );
        collector = new TrackerIdentifierCollector( programRuleService );
    }

    @Test
    void collectTrackedEntities()
    {

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.ofAttribute( "NTVsGflP5Ix" ) )
            .orgUnitIdScheme( TrackerIdSchemeParam.NAME )
            .build();

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( uid() )
            .trackedEntityType( ofAttribute( "NTVsGflP5Ix", "sunshine" ) )
            .orgUnit( ofName( "ward" ) )
            .attributes( teAttributes( "VohJnvWfvyo", "qv9xOw8fBzy" ) )
            .build();

        TrackerImportParams params = params( idSchemes )
            .trackedEntities( singletonList( trackedEntity ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( TrackedEntity.class ), trackedEntity.getTrackedEntity() );
        assertContainsOnly( ids.get( TrackedEntityType.class ), "sunshine" );
        assertContainsOnly( ids.get( OrganisationUnit.class ), "ward" );
        assertContainsOnly( ids.get( TrackedEntityAttribute.class ), "VohJnvWfvyo", "qv9xOw8fBzy" );
    }

    @Test
    void collectEnrollments()
    {

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .orgUnitIdScheme( TrackerIdSchemeParam.NAME )
            .programIdScheme( TrackerIdSchemeParam.ofAttribute( "NTVsGflP5Ix" ) )
            .build();

        Enrollment enrollment = Enrollment.builder()
            .enrollment( uid() )
            .trackedEntity( uid() )
            .program( ofAttribute( "NTVsGflP5Ix", "sunshine" ) )
            .orgUnit( ofName( "ward" ) )
            .attributes( teAttributes( "VohJnvWfvyo", "qv9xOw8fBzy" ) )
            .build();

        TrackerImportParams params = params( idSchemes )
            .enrollments( singletonList( enrollment ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( Enrollment.class ), enrollment.getUid() );
        assertContainsOnly( ids.get( TrackedEntity.class ), enrollment.getTrackedEntity() );
        assertContainsOnly( ids.get( Program.class ), "sunshine" );
        assertContainsOnly( ids.get( OrganisationUnit.class ), "ward" );
        assertContainsOnly( ids.get( TrackedEntityAttribute.class ), "VohJnvWfvyo", "qv9xOw8fBzy" );
    }

    @Test
    void collectEvents()
    {

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .orgUnitIdScheme( TrackerIdSchemeParam.NAME )
            .programIdScheme( TrackerIdSchemeParam.ofAttribute( "NTVsGflP5Ix" ) )
            .dataElementIdScheme( TrackerIdSchemeParam.UID )
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .build();

        Event event = Event.builder()
            .event( uid() )
            .enrollment( uid() )
            .program( ofAttribute( "NTVsGflP5Ix", "sunshine" ) )
            .programStage( ofAttribute( "NTVsGflP5Ix", "flowers" ) )
            .orgUnit( ofName( "ward" ) )
            .dataValues( dataValues( "VohJnvWfvyo", "qv9xOw8fBzy" ) )
            .attributeOptionCombo( ofCode( "rgb" ) )
            .attributeCategoryOptions( Set.of( ofCode( "red" ), ofCode( "green" ), ofCode( "blue" ) ) )
            .notes( List.of( Note.builder().note( "i1vviSlidJE" ).value( "nice day!" ).build() ) )
            .build();

        TrackerImportParams params = params( idSchemes )
            .events( singletonList( event ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( Event.class ), event.getUid() );
        assertContainsOnly( ids.get( Enrollment.class ), event.getEnrollment() );
        assertContainsOnly( ids.get( Program.class ), "sunshine" );
        assertContainsOnly( ids.get( ProgramStage.class ), "flowers" );
        assertContainsOnly( ids.get( OrganisationUnit.class ), "ward" );
        assertContainsOnly( ids.get( DataElement.class ), "VohJnvWfvyo", "qv9xOw8fBzy" );
        assertContainsOnly( ids.get( CategoryOptionCombo.class ), "rgb" );
        assertContainsOnly( ids.get( CategoryOption.class ), "red", "green", "blue" );
        assertContainsOnly( ids.get( TrackedEntityComment.class ), "i1vviSlidJE" );
    }

    @Test
    void collectEventsSkipsNotesWithoutAnId()
    {
        Event event = Event.builder()
            .notes( List.of( Note.builder().value( "nice day!" ).build() ) )
            .build();

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .events( singletonList( event ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertNull( ids.get( TrackedEntityComment.class ) );
    }

    @Test
    void collectEventsSkipsNotesWithoutAValue()
    {
        Event event = Event.builder()
            .notes( List.of( Note.builder().note( "i1vviSlidJE" ).build() ) )
            .build();

        TrackerImportParams params = params( TrackerIdSchemeParams.builder().build() )
            .events( singletonList( event ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertNull( ids.get( TrackedEntityComment.class ) );
    }

    @Test
    void collectRelationships()
    {

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.ofAttribute( "NTVsGflP5Ix" ) )
            .orgUnitIdScheme( TrackerIdSchemeParam.NAME )
            .build();

        Relationship relationship = Relationship.builder()
            .relationship( uid() )
            .relationshipType( ofAttribute( "NTVsGflP5Ix", "sunshine" ) )
            .from( RelationshipItem.builder()
                .enrollment( uid() )
                .build() )
            .to( RelationshipItem.builder()
                .event( uid() )
                .build() )
            .build();

        TrackerImportParams params = params( idSchemes )
            .relationships( singletonList( relationship ) )
            .build();

        Map<Class<?>, Set<String>> ids = collector.collect( params );

        assertNotNull( ids );
        assertContainsOnly( ids.get( Relationship.class ), relationship.getRelationship() );
        assertContainsOnly( ids.get( RelationshipType.class ), "sunshine" );
        assertContainsOnly( ids.get( Enrollment.class ), relationship.getFrom().getEnrollment() );
        assertContainsOnly( ids.get( Event.class ), relationship.getTo().getEvent() );
    }

    private String uid()
    {
        return CodeGenerator.generateUid();
    }

    private TrackerImportParams.TrackerImportParamsBuilder params( TrackerIdSchemeParams idSchemes )
    {
        return TrackerImportParams.builder().idSchemes( idSchemes );
    }

    private List<Attribute> teAttributes( String... uids )
    {

        List<Attribute> result = new ArrayList<>();
        for ( String uid : uids )
        {
            result.add( teAttribute( uid ) );
        }
        return result;
    }

    private Attribute teAttribute( String uid )
    {
        return Attribute.builder()
            .attribute( ofUid( uid ) )
            .build();
    }

    private Set<DataValue> dataValues( String... dataElementUids )
    {

        Set<DataValue> result = new HashSet<>();
        for ( String uid : dataElementUids )
        {
            result.add( dataValue( uid ) );
        }
        return result;
    }

    private DataValue dataValue( String dataElementUid )
    {
        return DataValue.builder()
            .dataElement( ofUid( dataElementUid ) )
            .build();
    }
}