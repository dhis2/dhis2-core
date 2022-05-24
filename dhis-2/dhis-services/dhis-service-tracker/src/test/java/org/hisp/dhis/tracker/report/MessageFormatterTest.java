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

import java.text.DateFormat;
import java.time.Instant;
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
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageFormatterTest
{

    private TrackerIdSchemeParams params;

    @BeforeEach
    void setUp()
    {
        params = TrackerIdSchemeParams.builder().build();
    }

    @Test
    void buildArgumentListShouldTurnIdentifiableObjectIntoArgument()
    {
        TrackerIdSchemeParams params = TrackerIdSchemeParams.builder()
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

        List<String> args = MessageFormatter.buildArgumentList( params, relationshipType, program, programStage,
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
    void buildArgumentListShouldTurnInstantIntoArgument()
    {
        final Instant now = Instant.now();

        List<String> args = MessageFormatter.buildArgumentList( params, Instant.now() );

        assertThat( args.size(), is( 1 ) );
        assertThat( args.get( 0 ), is( DateUtils.getIso8601NoTz( DateUtils.fromInstant( now ) ) ) );
    }

    @Test
    void buildArgumentListShouldTurnDateIntoArgument()
    {
        final Date now = Date.from( Instant.now() );

        List<String> args = MessageFormatter.buildArgumentList( params, now );

        assertThat( args.size(), is( 1 ) );
        assertThat( args.get( 0 ), is( DateFormat.getInstance().format( now ) ) );
    }

    @Test
    void buildArgumentListShouldTurnStringsIntoArguments()
    {
        List<String> args = MessageFormatter.buildArgumentList( params, "foo", "faa" );

        assertThat( args, contains( "foo", "faa" ) );
    }

    @Test
    void buildArgumentListShouldTurnMetadataIdentifierIntoArguments()
    {
        List<String> args = MessageFormatter.buildArgumentList( params,
            MetadataIdentifier.ofUid( "iB8AZpf681V" ), MetadataIdentifier.ofAttribute( "zwccdzhk5zc", "GREEN" ) );

        assertThat( args, contains( "iB8AZpf681V", "GREEN" ) );
    }
}
