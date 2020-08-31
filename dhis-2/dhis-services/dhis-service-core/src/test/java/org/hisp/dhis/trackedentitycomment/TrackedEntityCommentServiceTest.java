package org.hisp.dhis.trackedentitycomment;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.CodeGenerator;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityCommentServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityCommentService commentService;

    private TrackedEntityComment commentA;

    private TrackedEntityComment commentB;

    @Override
    public void setUpTest()
    {
        commentA = new TrackedEntityComment( "A", "Test" );
        commentA.setUid( CodeGenerator.generateUid() );
        commentB = new TrackedEntityComment( "B", "Test" );
        commentB.setUid( CodeGenerator.generateUid() );
    }

    @Test
    public void testSaveTrackedEntityComment()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        long idB = commentService.addTrackedEntityComment( commentB );

        assertNotNull( commentService.getTrackedEntityComment( idA ) );
        assertNotNull( commentService.getTrackedEntityComment( idB ) );
    }

    @Test
    public void testDeleteTrackedEntityComment()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        long idB = commentService.addTrackedEntityComment( commentB );

        assertNotNull( commentService.getTrackedEntityComment( idA ) );
        assertNotNull( commentService.getTrackedEntityComment( idB ) );

        commentService.deleteTrackedEntityComment( commentA );

        assertNull( commentService.getTrackedEntityComment( idA ) );
        assertNotNull( commentService.getTrackedEntityComment( idB ) );

        commentService.deleteTrackedEntityComment( commentB );

        assertNull( commentService.getTrackedEntityComment( idA ) );
        assertNull( commentService.getTrackedEntityComment( idB ) );
    }

    @Test
    public void testUpdateTrackedEntityComment()
    {
        long idA = commentService.addTrackedEntityComment( commentA );

        assertNotNull( commentService.getTrackedEntityComment( idA ) );

        commentA.setCommentText( "B" );
        commentService.updateTrackedEntityComment( commentA );

        assertEquals( "B", commentService.getTrackedEntityComment( idA ).getCommentText() );
    }

    @Test
    public void testGetTrackedEntityCommentById()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        long idB = commentService.addTrackedEntityComment( commentB );

        assertEquals( commentA, commentService.getTrackedEntityComment( idA ) );
        assertEquals( commentB, commentService.getTrackedEntityComment( idB ) );
    }
    
    @Test
    public void testCommentExists()
    {
        commentService.addTrackedEntityComment( commentA );

        assertTrue( commentService.trackedEntityCommentExists( commentA.getUid() ) );
    }

    @Test
    public void testFilterExistingNotes()
    {
        // create 15 comments
        List<String> commentUids = createComments( 15 );
        // add 3 uids to the existing comments UID
        List<String> newUids = IntStream.rangeClosed( 1, 3 ).boxed().map( x -> CodeGenerator.generateUid() )
            .collect( Collectors.toList() );
        commentUids.addAll( newUids );

        final List<String> filteredUid = commentService.filterExistingNotes( commentUids );
        assertThat(filteredUid, hasSize(3));
        for ( String uid : filteredUid )
        {
            assertTrue( newUids.contains( uid ) );
        }
    }
    
    private List<String> createComments( int size )
    {
        List<String> commentUids = new ArrayList<>( size );
        for ( int i = 0; i < size; i++ )
        {
            TrackedEntityComment comment = new TrackedEntityComment();
            comment.setUid( CodeGenerator.generateUid() );
            comment.setCreated( new Date() );
            comment.setCommentText( RandomStringUtils.randomAlphabetic( 20 ) );
            commentService.addTrackedEntityComment( comment );
            commentUids.add( comment.getUid() );

        }
        return commentUids;
    }
    
}
