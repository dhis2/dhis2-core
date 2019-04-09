package org.hisp.dhis.programstagefilter;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Category( IntegrationTest.class )
public class ProgramStageInstanceFilterTest extends IntegrationTestBase
{

    @Autowired
    private ProgramStageInstanceFilterService psiFilterService;

    @Autowired
    private ProgramStageInstanceFilterStore psiFilterStore;

    @Autowired
    private ProgramService programService;

    @Autowired
    private UserService userService;

    private CurrentUserService currentUserService;

    private Program programA;

    private Program programB;

    @Override
    public void setUpTest()
    {
        super.userService = this.userService;
        User user = createUser( "testUser" );
        currentUserService = new MockCurrentUserService( user );
        setDependency( psiFilterStore, "currentUserService", currentUserService );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        programService.addProgram( programA );
        programService.addProgram( programB );

    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testAddEventFilter()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        long id = psiFilterService.add( psiFilter );

        assertNotNull( id );
        assertEquals( psiFilter, psiFilterService.get( id ) );
    }

    @Test
    public void testGetEventFilterByUid()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        long id = psiFilterService.add( psiFilter );

        assertNotNull( id );
        assertEquals( psiFilter, psiFilterService.get( psiFilter.getUid() ) );
    }

    @Test
    public void testGetAllEventFilters()
    {
        ProgramStageInstanceFilter psif1 = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        ProgramStageInstanceFilter psif2 = createProgramStageInstanceFilter( '2', programB.getUid(), null );
        ProgramStageInstanceFilter psif3 = createProgramStageInstanceFilter( '3', programA.getUid(), null );

        psiFilterService.add( psif1 );
        psiFilterService.add( psif2 );
        psiFilterService.add( psif3 );

        List<ProgramStageInstanceFilter> list = psiFilterService.getAll( null );

        assertEquals( 3, list.size() );
        assertTrue( list.contains( psif1 ) );
        assertTrue( list.contains( psif2 ) );
        assertTrue( list.contains( psif3 ) );
    }

    @Test
    public void testGetAllEventFiltersByProgram()
    {
        ProgramStageInstanceFilter psif1 = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        ProgramStageInstanceFilter psif2 = createProgramStageInstanceFilter( '2', programB.getUid(), null );
        ProgramStageInstanceFilter psif3 = createProgramStageInstanceFilter( '3', programA.getUid(), null );

        psiFilterService.add( psif1 );
        psiFilterService.add( psif2 );
        psiFilterService.add( psif3 );

        List<ProgramStageInstanceFilter> list = psiFilterService.getAll( programA.getUid() );

        assertEquals( 2, list.size() );
        assertTrue( list.contains( psif1 ) );
        assertTrue( list.contains( psif3 ) );
    }

    @Test
    public void testUpdateEventFilter()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        long id = psiFilterService.add( psiFilter );

        assertNotNull( id );
        assertEquals( psiFilter, psiFilterService.get( id ) );
        assertEquals( programA.getUid(), psiFilterService.get( psiFilter.getUid() ).getProgram() );

        psiFilter.setProgram( programB.getUid() );

        psiFilterService.update( psiFilter );
        assertEquals( programB.getUid(), psiFilterService.get( psiFilter.getUid() ).getProgram() );
    }

    @Test
    public void testDeleteEventFilter()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        long id = psiFilterService.add( psiFilter );

        assertNotNull( id );
        assertEquals( psiFilter, psiFilterService.get( psiFilter.getUid() ) );

        psiFilterService.delete( psiFilter );
        assertNull( psiFilterService.get( psiFilter.getUid() ) );
    }

    @Test( expected = IllegalQueryException.class )
    public void testAddInvalidEventFilterWithMissingProgram()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', null, null );
        psiFilterService.add( psiFilter );
    }

    @Test( expected = IllegalQueryException.class )
    public void testAddInvalidEventFilterWithInvalidProgram()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', "ABCDEF12345", null );
        psiFilterService.add( psiFilter );
    }

    private static ProgramStageInstanceFilter createProgramStageInstanceFilter( char uniqueCharacter, String program, String programStage )
    {
        ProgramStageInstanceFilter psiFilter = new ProgramStageInstanceFilter();
        psiFilter.setAutoFields();

        psiFilter.setName( "eventFilterName" + uniqueCharacter );
        psiFilter.setCode( "eventFilterCode" + uniqueCharacter );
        psiFilter.setDescription( "eventFilterDescription" + uniqueCharacter );

        if ( program != null )
        {
            psiFilter.setProgram( program );
        }

        if ( programStage != null )
        {
            psiFilter.setProgramStage( programStage );
        }
        return psiFilter;
    }

}
