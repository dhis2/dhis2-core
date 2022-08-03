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
package org.hisp.dhis.trackedentitycomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityCommentServiceTest extends IntegrationTestBase
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
    void testSaveTrackedEntityComment()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        long idB = commentService.addTrackedEntityComment( commentB );
        assertNotNull( commentService.getTrackedEntityComment( idA ) );
        assertNotNull( commentService.getTrackedEntityComment( idB ) );
    }

    @Test
    void testDeleteTrackedEntityComment()
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
    void testUpdateTrackedEntityComment()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        assertNotNull( commentService.getTrackedEntityComment( idA ) );
        commentA.setCommentText( "B" );
        commentService.updateTrackedEntityComment( commentA );
        assertEquals( "B", commentService.getTrackedEntityComment( idA ).getCommentText() );
    }

    @Test
    void testGetTrackedEntityCommentById()
    {
        long idA = commentService.addTrackedEntityComment( commentA );
        long idB = commentService.addTrackedEntityComment( commentB );
        assertEquals( commentA, commentService.getTrackedEntityComment( idA ) );
        assertEquals( commentB, commentService.getTrackedEntityComment( idB ) );
    }

    @Test
    void testCommentExists()
    {
        commentService.addTrackedEntityComment( commentA );
        assertTrue( commentService.trackedEntityCommentExists( commentA.getUid() ) );
    }
}
