package org.hisp.dhis.tracker.preheat;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.TrackerIdentifierParams.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
public class TrackerPreheatIdentifiersTest extends TrackerTest
{
    @Autowired
    private TrackerPreheatService trackerPreheatService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/identifier_metadata.json" );
    }

    @Test
    public void testProgramIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "QfUVllTs6cS", "PRG1", "ProgramA" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setProgram( pair.getLeft() );

            TrackerImportParams params = buildParams( event, builder()
                .programIdScheme( pair.getRight() )
                .build() );

            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, Program.class, pair.getRight(), pair.getLeft() );
        }
    }

    @Test
    public void testOrgUnitIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "PlKwabX2xRW", "COU1", "Country" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setOrgUnit( pair.getLeft() );

            TrackerImportParams params = buildParams( event, builder()
                .orgUnitIdScheme( pair.getRight() )
                .build() );

            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, OrganisationUnit.class, pair.getRight(), pair.getLeft() );
        }
    }

    @Test
    public void testProgramStageIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "NpsdDv6kKSO", "PRGA", "ProgramA" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setProgramStage( pair.getLeft() );

            TrackerImportParams params = buildParams( event, builder()
                .programStageIdScheme( pair.getRight() )
                .build() );

            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, ProgramStage.class, pair.getRight(), pair.getLeft() );
        }
    }

    @Test
    public void testDataElementIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "DSKTW8qFP0z", "DEAGE", "DE Age" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setProgramStage( "NpsdDv6kKSO" );

            DataValue dv1 = new DataValue();
            dv1.setDataElement( pair.getLeft() );
            dv1.setValue( "val1" );
            event.setDataValues( Collections.singleton( dv1 ) );

            TrackerImportParams params = buildParams( event, builder()
                .dataElementIdScheme( pair.getRight() )
                .build() );

            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, DataElement.class, pair.getRight(), pair.getLeft() );
        }
    }

    @Test
    public void testCategoryOptionIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "XXXrKDKCefk", "COA", "COAname" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setAttributeCategoryOptions( pair.getLeft() );

            TrackerImportParams params = buildParams( event, builder()
                .categoryOptionIdScheme( pair.getRight() )
                .build() );
            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, CategoryOption.class, pair.getRight(), pair.getLeft() );
        }
    }

    @Test
    public void testCategoryOptionComboIdentifiers()
    {
        List<Pair<String, TrackerIdentifier>> data = buildDataSet( "XXXvX50cXC0", "COCA", "COCAname" );

        for ( Pair<String, TrackerIdentifier> pair : data )
        {
            Event event = new Event();
            event.setAttributeOptionCombo( pair.getLeft() );
            TrackerImportParams params = buildParams( event, builder()
                .categoryOptionComboIdScheme( pair.getRight() )
                .build() );

            TrackerPreheat preheat = trackerPreheatService.preheat( params );

            assertPreheatedObjectExists( preheat, CategoryOptionCombo.class, pair.getRight(), pair.getLeft() );
        }
    }

    private TrackerImportParams buildParams( Event event, TrackerIdentifierParams idParams )
    {
        TrackerImportParams params = TrackerImportParams.builder()
            .events( Collections.singletonList( event ) )
            .user( currentUserService.getCurrentUser() )
            .build();

        params.setIdentifiers( idParams );
        return params;
    }

    private List<Pair<String, TrackerIdentifier>> buildDataSet( String uid, String code, String name )
    {
        List<Pair<String, TrackerIdentifier>> data = new ArrayList<>();

        data.add( ImmutablePair.of( uid, TrackerIdentifier.UID ) );
        data.add( ImmutablePair.of( code, TrackerIdentifier.CODE ) );
        data.add( ImmutablePair.of( name, TrackerIdentifier.NAME ) );
        return data;
    }

    private void assertPreheatedObjectExists( TrackerPreheat preheat, Class klazz, TrackerIdentifier identifier,
        String id )
    {
        assertThat(
            "Expecting a preheated object for identifier: " + identifier.getIdScheme().name() + " with value: " + id,
            preheat.get( klazz, id ), is( notNullValue() ) );
    }
}
