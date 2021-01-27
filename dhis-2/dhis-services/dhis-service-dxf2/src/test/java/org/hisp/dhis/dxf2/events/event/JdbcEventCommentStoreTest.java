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
package org.hisp.dhis.dxf2.events.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.ImmutableList;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
public class JdbcEventCommentStoreTest
{

    private JdbcEventCommentStore jdbcEventCommentStore;

    @Before
    public void setUp()
    {
        JdbcTemplate jdbcTemplate = mock( JdbcTemplate.class );
        JdbcEventCommentStore jdbcEventCommentStore = new JdbcEventCommentStore( jdbcTemplate );
        this.jdbcEventCommentStore = Mockito.spy( jdbcEventCommentStore );
        doReturn( 1L ).when( this.jdbcEventCommentStore ).saveComment( any() );
        doNothing().when( this.jdbcEventCommentStore ).saveCommentToEvent( anyLong(), anyLong(), anyInt() );
    }

    @Test
    public void verifyPSITableIsNotQueriedWhenNoComments()
    {
        List<ProgramStageInstance> programStageInstanceList = getProgramStageList( false );
        jdbcEventCommentStore.saveAllComments( programStageInstanceList );
        verify( jdbcEventCommentStore, never() ).getInitialSortOrder( any() );
    }

    @Test
    public void verifyPSITableIsNotQueriedWhenCommentsTextEmpty()
    {
        List<ProgramStageInstance> programStageInstanceList = getProgramStageList( true, true );
        jdbcEventCommentStore.saveAllComments( programStageInstanceList );
        verify( jdbcEventCommentStore, never() ).getInitialSortOrder( any() );
    }

    @Test
    public void verifyPSITableIsQueriedWhenComments()
    {
        List<ProgramStageInstance> programStageInstanceList = getProgramStageList( true );
        jdbcEventCommentStore.saveAllComments( programStageInstanceList );
        verify( jdbcEventCommentStore ).getInitialSortOrder( any() );
    }

    private List<ProgramStageInstance> getProgramStageList( boolean withComments )
    {
        return getProgramStageList( withComments, false );
    }

    private List<ProgramStageInstance> getProgramStageList( boolean withComments, boolean emptyComment )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        if ( withComments )
        {
            programStageInstance.setComments( ImmutableList.of( getComment( emptyComment ? "" : "Some comment" ) ) );
        }
        return ImmutableList.of( programStageInstance );
    }

    private TrackedEntityComment getComment( String commentText )
    {
        return new TrackedEntityComment( commentText, "Some author" );
    }
}
