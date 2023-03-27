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
package org.hisp.dhis.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

class PrefixedDimensionsTest
{

    @Test
    void testOfItemsWithProgram()
    {
        Program program = buildBaseIdentifiableObject( "programUid", Program.class );

        DataElement dataElement = buildBaseIdentifiableObject( "dataElement", DataElement.class );
        ProgramStageDataElement programStageDataElement = buildBaseIdentifiableObject( "programStageDataElement",
            ProgramStageDataElement.class );
        TrackedEntityAttribute trackedEntityAttribute = buildBaseIdentifiableObject( "trackedEntityAttribute",
            TrackedEntityAttribute.class );

        Collection<PrefixedDimension> prefixedDimensions = PrefixedDimensions.ofItemsWithProgram( program,
            ImmutableList.of(
                dataElement,
                programStageDataElement,
                trackedEntityAttribute ) );

        assertThat( prefixedDimensions, hasSize( 3 ) );

        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getPrefix )
            .collect( Collectors.toList() ),
            containsInAnyOrder( "programUid", "programUid", "programUid" ) );

        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() ),
            containsInAnyOrder( dataElement, programStageDataElement, trackedEntityAttribute ) );
    }

    @Test
    void testOfProgramIndicators()
    {

        ProgramIndicator programIndicator = buildBaseIdentifiableObject( "programIndicator", ProgramIndicator.class );
        Program program = buildBaseIdentifiableObject( "programUid", Program.class );
        programIndicator.setProgram( program );

        Collection<PrefixedDimension> prefixedDimensions = PrefixedDimensions.ofProgramIndicators(
            ImmutableSet.of( programIndicator ) );

        assertThat( prefixedDimensions, hasSize( 1 ) );
        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getPrefix )
            .collect( Collectors.toList() ),
            containsInAnyOrder( "programUid" ) );
        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() ),
            containsInAnyOrder( programIndicator ) );
        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getProgram )
            .collect( Collectors.toList() ),
            containsInAnyOrder( program ) );
    }

    @Test
    void testOfDataElements()
    {

        ProgramStage programStage = buildBaseIdentifiableObject( "programStageUid", ProgramStage.class );
        Program program = buildBaseIdentifiableObject( "programUid", Program.class );

        DataElement dataElement1 = buildBaseIdentifiableObject( "de1", DataElement.class );
        DataElement dataElement2 = buildBaseIdentifiableObject( "de2", DataElement.class );

        programStage.setProgram( program );
        programStage.setProgramStageDataElements(
            ImmutableSet.of(
                new ProgramStageDataElement( programStage, dataElement1 ),
                new ProgramStageDataElement( programStage, dataElement2 ) ) );

        Collection<PrefixedDimension> prefixedDimensions = PrefixedDimensions.ofDataElements( programStage );

        assertThat( prefixedDimensions, hasSize( 2 ) );
        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getPrefix )
            .collect( Collectors.toList() ),
            containsInAnyOrder( "programUid.programStageUid", "programUid.programStageUid" ) );

        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() ),
            containsInAnyOrder( dataElement1, dataElement2 ) );
    }

    @Test
    void testOfProgramStageDataElements()
    {

        ProgramStage programStage1 = buildBaseIdentifiableObject( "programStageUid1", ProgramStage.class );
        ProgramStage programStage2 = buildBaseIdentifiableObject( "programStageUid2", ProgramStage.class );

        Program program = buildBaseIdentifiableObject( "programUid", Program.class );

        DataElement dataElement1 = buildBaseIdentifiableObject( "de1", DataElement.class );
        DataElement dataElement2 = buildBaseIdentifiableObject( "de2", DataElement.class );

        programStage1.setProgram( program );
        programStage2.setProgram( program );

        ProgramStageDataElement ps1 = new ProgramStageDataElement( programStage1, dataElement1 );
        ProgramStageDataElement ps2 = new ProgramStageDataElement( programStage1, dataElement2 );
        ProgramStageDataElement ps3 = new ProgramStageDataElement( programStage2, dataElement1 );
        ProgramStageDataElement ps4 = new ProgramStageDataElement( programStage2, dataElement2 );

        Collection<PrefixedDimension> prefixedDimensions = PrefixedDimensions.ofProgramStageDataElements(
            ImmutableSet.of( ps1, ps2, ps3, ps4 ) );

        assertThat( prefixedDimensions, hasSize( 4 ) );
        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getPrefix )
            .collect( Collectors.toList() ),
            containsInAnyOrder( "programUid.programStageUid1", "programUid.programStageUid2",
                "programUid.programStageUid1", "programUid.programStageUid2" ) );

        assertThat( prefixedDimensions.stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() ),
            containsInAnyOrder( ps1, ps2, ps3, ps4 ) );
    }

    @SneakyThrows
    private <T extends BaseIdentifiableObject> T buildBaseIdentifiableObject( String uid, Class<T> tClass )
    {
        T t = tClass.getDeclaredConstructor().newInstance();
        t.setUid( uid );
        return t;
    }
}
