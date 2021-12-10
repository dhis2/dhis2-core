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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link InterpretationController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class InterpretationControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private IdentifiableObjectManager manager;

    private String uid;

    private String ouId;

    @Before
    public void setUp()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        manager.save( ouA );
        Visualization vzA = createVisualization( 'A' );
        visualizationService.save( vzA );
        Interpretation ipA = new Interpretation( vzA, ouA, "Interpration of visualization A" );
        interpretationService.saveInterpretation( ipA );

        uid = ipA.getUid();
        ouId = ouA.getUid();
    }

    @Test
    public void testDeleteObject()
    {
        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/interpretations/" + uid ) );
    }

    @Test
    public void testDeleteObject_NotFound()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Interpretation does not exist: xyz",
            DELETE( "/interpretations/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testWriteVisualizationInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Visualization does not exist or is not accessible: xyz",
            POST( "/interpretations/visualization/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testWriteEventVisualizationInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "EventVisualization does not exist or is not accessible: xyz",
            POST( "/interpretations/eventVisualization/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testWriteMapInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Map does not exist or is not accessible: xyz",
            POST( "/interpretations/map/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testWriteEventReportInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Event report does not exist or is not accessible: xyz",
            POST( "/interpretations/eventReport/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testWriteEventChartInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Event chart does not exist or is not accessible: xyz",
            POST( "/interpretations/eventChart/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testWriteDataSetReportInterpretation()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Data set does not exist or is not accessible: xyz",
            POST( "/interpretations/dataSetReport/xyz?pe=2021&ou=" + ouId, "text/plain:text" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testUpdateInterpretation()
    {
        assertStatus( HttpStatus.NO_CONTENT, PUT( "/interpretations/" + uid, "text/plain:text" ) );
    }

    @Test
    public void testUpdateInterpretation_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Interpretation does not exist: xyz",
            PUT( "/interpretations/xyz", "text/plain:text" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testPostComment()
    {
        assertWebMessage( "Created", 201, "OK", "Commented created",
            POST( "/interpretations/" + uid + "/comments", "text/plain:comment" ).content( HttpStatus.CREATED ) );

        JsonObject comments = GET( "/interpretations/{uid}/comments", uid ).content( HttpStatus.OK );
        assertEquals( 1, comments.getArray( "comments" ).size() );
    }

    @Test
    public void testPostComment_NoSuchObject()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Interpretation does not exist: xyz",
            POST( "/interpretations/xyz/comments", "text/plain:comment" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testUpdateComment()
    {
        String cuid = assertStatus( HttpStatus.CREATED,
            POST( "/interpretations/" + uid + "/comments", "text/plain:comment" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            PUT( "/interpretations/" + uid + "/comments/" + cuid, "text/plain:new comment" ) );

        JsonObject comments = GET( "/interpretations/{uid}/comments/", uid ).content();
        assertEquals( "new comment", comments.getArray( "comments" ).getObject( 0 ).getString( "text" ).string() );
    }

    @Test
    public void testUpdateComment_NoSuchObject()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Interpretation does not exist: xyz",
            PUT( "/interpretations/xyz/comments/abc", "text/plain:comment" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testDeleteComment()
    {
        String cuid = assertStatus( HttpStatus.CREATED,
            POST( "/interpretations/" + uid + "/comments", "text/plain:comment" ) );

        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/interpretations/" + uid + "/comments/" + cuid ) );

        JsonObject comments = GET( "/interpretations/{uid}/comments", uid ).content( HttpStatus.OK );
        assertEquals( 0, comments.getArray( "comments" ).size() );
    }

    @Test
    public void testDeleteComment_NoSuchObject()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Interpretation does not exist: xyz",
            DELETE( "/interpretations/xyz/comments/abc" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testLike()
    {
        assertWebMessage( "Created", 201, "OK", "Like added to interpretation",
            POST( "/interpretations/" + uid + "/like" ).content( HttpStatus.CREATED ) );
    }

    @Test
    public void testLike_NoSuchObject()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Interpretation does not exist: xyz",
            POST( "/interpretations/xyz/like" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testLike_AlreadyLiked()
    {
        assertStatus( HttpStatus.CREATED, POST( "/interpretations/" + uid + "/like" ) );

        assertWebMessage( "Conflict", 409, "ERROR", "Could not add like, user had already liked interpretation",
            POST( "/interpretations/" + uid + "/like" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testUnlike()
    {
        assertStatus( HttpStatus.CREATED, POST( "/interpretations/" + uid + "/like" ) );

        assertWebMessage( "Created", 201, "OK", "Like removed from interpretation",
            DELETE( "/interpretations/" + uid + "/like" ).content( HttpStatus.CREATED ) );
    }

    @Test
    public void testUnlike_NoSuchObject()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Interpretation does not exist: xyz",
            DELETE( "/interpretations/xyz/like" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testUnlike_NotYetLiked()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "Could not remove like, user had not previously liked interpretation",
            DELETE( "/interpretations/" + uid + "/like" ).content( HttpStatus.CONFLICT ) );
    }
}
