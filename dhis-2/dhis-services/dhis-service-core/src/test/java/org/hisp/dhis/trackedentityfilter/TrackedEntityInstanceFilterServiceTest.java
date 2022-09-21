/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.trackedentityfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class TrackedEntityInstanceFilterServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackedEntityInstanceFilterService trackedEntityInstanceFilterService;

    @Autowired
    private UserService _userService;

    private Program programA;

    private Program programB;

    @Before
    public void init()
    {
        userService = _userService;
    }

    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A', null, null );
        programB = createProgram( 'B', null, null );

        programService.addProgram( programA );
        programService.addProgram( programB );
    }

    @Test
    public void testAddGet()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );

        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        long idB = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );

        assertEquals( idA, trackedEntityInstanceFilterA.getId() );
        assertEquals( idB, trackedEntityInstanceFilterB.getId() );
        assertEquals( trackedEntityInstanceFilterA, trackedEntityInstanceFilterService.get( idA ) );
        assertEquals( trackedEntityInstanceFilterB, trackedEntityInstanceFilterService.get( idB ) );
    }

    @Test
    public void testGetAll()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );

        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );

        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService.getAll();

        assertEquals( trackedEntityInstanceFilters.size(), 2 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterB ) );
    }

    @Test
    public void testGetByProgram()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterC = createTrackedEntityInstanceFilter( 'C', programA );

        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );
        trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterC );

        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService
            .get( programA );

        assertEquals( trackedEntityInstanceFilters.size(), 2 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterC ) );
        assertFalse( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterB ) );

    }

    @Test
    public void testUpdate()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );

        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );

        trackedEntityInstanceFilterA.setProgram( programB );

        trackedEntityInstanceFilterService.update( trackedEntityInstanceFilterA );

        assertEquals( trackedEntityInstanceFilterA, trackedEntityInstanceFilterService.get( idA ) );

        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService
            .get( programB );

        assertEquals( trackedEntityInstanceFilters.size(), 1 );
        assertTrue( trackedEntityInstanceFilters.contains( trackedEntityInstanceFilterA ) );

        trackedEntityInstanceFilters = trackedEntityInstanceFilterService.get( programA );

        assertEquals( trackedEntityInstanceFilters.size(), 0 );
    }

    @Test
    public void testDelete()
    {
        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );
        TrackedEntityInstanceFilter trackedEntityInstanceFilterB = createTrackedEntityInstanceFilter( 'B', programB );

        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );
        long idB = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterB );

        List<TrackedEntityInstanceFilter> trackedEntityInstanceFilters = trackedEntityInstanceFilterService.getAll();

        assertEquals( trackedEntityInstanceFilters.size(), 2 );

        trackedEntityInstanceFilterService.delete( trackedEntityInstanceFilterService.get( idA ) );

        assertNull( trackedEntityInstanceFilterService.get( idA ) );
        assertNotNull( trackedEntityInstanceFilterService.get( idB ) );
    }

    @Test
    public void testSaveWithoutAuthority()
    {
        createUserAndInjectSecurityContext( false );

        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );

        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );

        assertNotNull( trackedEntityInstanceFilterService.get( idA ) );
    }

    @Test
    public void testSaveWithAuthority()
    {
        createUserAndInjectSecurityContext( false, "F_PROGRAMSTAGE_ADD" );

        TrackedEntityInstanceFilter trackedEntityInstanceFilterA = createTrackedEntityInstanceFilter( 'A', programA );

        long idA = trackedEntityInstanceFilterService.add( trackedEntityInstanceFilterA );

        assertNotNull( trackedEntityInstanceFilterService.get( idA ) );
    }

}
