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
package org.hisp.dhis.tracker.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageFormatterTest
{

    private TrackerIdSchemeParams idSchemes;

    @BeforeEach
    void setUp()
    {
        idSchemes = TrackerIdSchemeParams.builder().build();
    }

    @Test
    void format()
    {

        assertEquals( "User: `Snow`, has no write access to OrganisationUnit: `ward`.", MessageFormatter
            .format( idSchemes, "User: `{0}`, has no write access to OrganisationUnit: `{1}`.", "Snow", "ward" ) );
    }

    @Test
    void formatWithoutArgs()
    {

        assertEquals( "User has no write access to OrganisationUnit.",
            MessageFormatter.format( idSchemes, "User has no write access to OrganisationUnit." ) );
    }

    @Test
    void formatArgumentsShouldTurnIdentifiableObjectIntoArgument()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.UID )
            .programIdScheme( TrackerIdSchemeParam.NAME )
            .programStageIdScheme( TrackerIdSchemeParam.NAME )
            .orgUnitIdScheme( TrackerIdSchemeParam.ofAttribute( "HpSAvRWtdDR" ) )
            .dataElementIdScheme( TrackerIdSchemeParam.ofAttribute( "m0GpPuMUfFW" ) )
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.ofAttribute( "qAvXlaodIZ9" ) )
            .categoryOptionIdScheme( TrackerIdSchemeParam.ofAttribute( "y0Yxr50hAbP" ) )
            .build();
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( "WTTYiPQDqh1" );
        Program program = new Program( "friendship" );
        ProgramStage programStage = new ProgramStage( "meet", program );
        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setAttributeValues( attributeValues( "HpSAvRWtdDR", "sunshine" ) );
        DataElement dataElement = new DataElement();
        dataElement.setAttributeValues( attributeValues( "m0GpPuMUfFW", "ice" ) );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setAttributeValues( attributeValues( "qAvXlaodIZ9", "wheat" ) );
        CategoryOption co = new CategoryOption();
        co.setAttributeValues( attributeValues( "y0Yxr50hAbP", "red" ) );

        List<String> args = MessageFormatter.formatArguments( idSchemes, relationshipType, program, programStage,
            orgUnit, dataElement, coc, co );

        assertThat( args,
            contains( "RelationshipType (WTTYiPQDqh1)",
                "Program (friendship)",
                "ProgramStage (meet)",
                "OrganisationUnit (sunshine)",
                "DataElement (ice)",
                "CategoryOptionCombo (wheat)",
                "CategoryOption (red)" ) );
    }

    private Set<AttributeValue> attributeValues( String uid, String value )
    {
        return Set.of( new AttributeValue( attribute( uid ), value ) );
    }

    private Attribute attribute( String attributeUid )
    {
        Attribute att = new Attribute();
        att.setUid( attributeUid );
        return att;
    }

    @Test
    void formatArgumentsShouldTurnInstantIntoArgument()
    {
        final Instant now = Instant.now();

        List<String> args = MessageFormatter.formatArguments( idSchemes, now );

        assertThat( args.size(), is( 1 ) );
        assertThat( args.get( 0 ), is( DateUtils.getIso8601NoTz( DateUtils.fromInstant( now ) ) ) );
    }

    @Test
    void formatArgumentsShouldTurnDateIntoArgument()
    {
        final Date now = Date.from( Instant.now() );

        List<String> args = MessageFormatter.formatArguments( idSchemes, now );

        assertThat( args.size(), is( 1 ) );
        assertThat( args.get( 0 ), is( DateFormat.getInstance().format( now ) ) );
    }

    @Test
    void formatArgumentsShouldTurnStringsIntoArguments()
    {
        List<String> args = MessageFormatter.formatArguments( idSchemes, "foo", "faa" );

        assertThat( args, contains( "foo", "faa" ) );
    }

    @Test
    void formatArgumentsShouldTurnMetadataIdentifierIntoArguments()
    {
        List<String> args = MessageFormatter.formatArguments( idSchemes,
            MetadataIdentifier.ofUid( "iB8AZpf681V" ), MetadataIdentifier.ofAttribute( "zwccdzhk5zc", "GREEN" ) );

        assertThat( args, contains( "iB8AZpf681V", "GREEN" ) );
    }

    @Test
    void formatArgumentsShouldTurnTrackedEntityIntoArguments()
    {
        List<String> args = MessageFormatter.formatArguments( idSchemes,
            TrackedEntity.builder().trackedEntity( "zwccdzhk5zc" ).build() );

        assertThat( args, contains( "TrackedEntity (zwccdzhk5zc)" ) );
    }

    @Test
    void formatArgumentsShouldTurnEnrollmentIntoArguments()
    {
        List<String> args = MessageFormatter.formatArguments( idSchemes,
            Enrollment.builder().enrollment( "zwccdzhk5zc" ).build() );

        assertThat( args, contains( "Enrollment (zwccdzhk5zc)" ) );
    }

    @Test
    void formatArgumentsShouldTurnEventIntoArguments()
    {
        List<String> args = MessageFormatter.formatArguments( idSchemes,
            Event.builder().event( "zwccdzhk5zc" ).build() );

        assertThat( args, contains( "Event (zwccdzhk5zc)" ) );
    }

    @Test
    void formatArgumentsWithNumber()
    {
        assertEquals( List.of( "" ), MessageFormatter.formatArguments( idSchemes, 2 ) );
    }

    @Test
    void formatArgumentsWithoutArgument()
    {
        assertEquals( Collections.emptyList(), MessageFormatter.formatArguments( idSchemes ) );
    }
}
