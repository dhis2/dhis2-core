package org.hisp.dhis.dxf2.events.repository;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityAttributeRepositoryTest
    extends
    DhisSpringTest
{

    @Autowired
    private TrackedEntityAttributeRepository trackedEntityAttributeRepository;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ProgramService programService;

    private final static int A = 65;

    private final static int T = 85;

    private Program programB;

    @Before
    public void setUp()
    {

        Program program = createProgram( 'A' );
        programService.addProgram( program );

        TrackedEntityType trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeA.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeA );

        TrackedEntityType trackedEntityTypeB = createTrackedEntityType( 'B' );
        trackedEntityTypeB.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeB );

        // Create 20 Tracked Entity Attributes (named A .. O)
        IntStream.range( A, T ).mapToObj( i -> Character.toString( (char) i ) ).forEach( c -> attributeService
            .addTrackedEntityAttribute( createTrackedEntityAttribute( c.charAt( 0 ), ValueType.TEXT ) ) );
        
        // Transform the Tracked Entity Attributes into a List of TrackedEntityTypeAttribute
        List<TrackedEntityTypeAttribute> teatList = IntStream.range( A, T )
            .mapToObj( i -> Character.toString( (char) i ) )
            .map( s -> new TrackedEntityTypeAttribute( trackedEntityTypeA,
                attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
            .collect( Collectors.toList() );
        
        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type A
        trackedEntityTypeA.setTrackedEntityTypeAttributes( teatList.subList( 0, 10 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeA );

        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type B
        trackedEntityTypeB.setTrackedEntityTypeAttributes( teatList.subList( 10, 20 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeB );
        
        // Setup for second test
        
        programB = createProgram( 'B' );
        programService.addProgram( programB );

        List<ProgramTrackedEntityAttribute> pteaList = IntStream.range( A, T )
            .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programB,
                attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
            .collect( Collectors.toList() );

        programB.setProgramAttributes( pteaList );
        programService.updateProgram( program );

    }

    @Test
    public void verifyGetTrackedEntityAttributesByTrackedEntityTypes()
    {

        Set<TrackedEntityAttribute> trackedEntityAttributes = trackedEntityAttributeRepository
            .getTrackedEntityAttributesByTrackedEntityTypes();

        assertThat( trackedEntityAttributes, hasSize( 20 ) );
    }

    @Test
    public void verifyGetTrackedEntityAttributesByProgram()
    {

        Map<Program, Set<TrackedEntityAttribute>> trackedEntityAttributes = trackedEntityAttributeRepository
            .getTrackedEntityAttributesByProgram();

        assertThat( trackedEntityAttributes.size(), is( 1 ) );
        assertThat( trackedEntityAttributes.get( programB ), hasSize( 20 ) );
    }

}