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
package org.hisp.dhis.interpretation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Disabled
class InterpretationServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private UserService _userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private IdentifiableObjectManager manager;

    private User userA;

    private User userB;

    private User userC;

    private Visualization visualizationA;

    private Interpretation interpretationA;

    private Interpretation interpretationB;

    private Interpretation interpretationC;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
    }

    @BeforeEach
    void beforeTest()
    {
        userA = makeUser( "A" );
        userB = makeUser( "B" );
        userC = createUserWithAuth( "C.D-E_F" );
        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        injectSecurityContext( userA );
        visualizationA = createVisualization( 'A' );
        visualizationService.save( visualizationA );
        interpretationA = new Interpretation( visualizationA, null, "Interpration of chart A" );
        interpretationB = new Interpretation( visualizationA, null, "Interpration of chart B" );
        interpretationC = new Interpretation( visualizationA, null, "Interpration of chart C" );
    }

    @Test
    void testConstruct()
    {
        // Given
        final Visualization aVisualizationA = createVisualization( 'A' );
        // When
        final Interpretation anInterpretationA = new Interpretation( aVisualizationA, null, "InterpretationA" );
        final Interpretation anInterpretationB = new Interpretation( aVisualizationA, null, "InterpretationB" );
        // Then
        assertEquals( aVisualizationA, anInterpretationA.getVisualization() );
        assertEquals( aVisualizationA, anInterpretationB.getVisualization() );
        assertTrue( aVisualizationA.getInterpretations().contains( anInterpretationA ) );
        assertTrue( aVisualizationA.getInterpretations().contains( anInterpretationB ) );
    }

    @Test
    void testSaveGet()
    {
        long idA = interpretationService.saveInterpretation( interpretationA );
        long idB = interpretationService.saveInterpretation( interpretationB );
        long idC = interpretationService.saveInterpretation( interpretationC );
        assertEquals( interpretationA, interpretationService.getInterpretation( idA ) );
        assertEquals( interpretationB, interpretationService.getInterpretation( idB ) );
        assertEquals( interpretationC, interpretationService.getInterpretation( idC ) );
    }

    @Test
    void testDelete()
    {
        long idA = interpretationService.saveInterpretation( interpretationA );
        long idB = interpretationService.saveInterpretation( interpretationB );
        long idC = interpretationService.saveInterpretation( interpretationC );
        assertNotNull( interpretationService.getInterpretation( idA ) );
        assertNotNull( interpretationService.getInterpretation( idB ) );
        assertNotNull( interpretationService.getInterpretation( idC ) );
        interpretationService.deleteInterpretation( interpretationB );
        assertNotNull( interpretationService.getInterpretation( idA ) );
        assertNull( interpretationService.getInterpretation( idB ) );
        assertNotNull( interpretationService.getInterpretation( idC ) );
        interpretationService.deleteInterpretation( interpretationA );
        assertNull( interpretationService.getInterpretation( idA ) );
        assertNull( interpretationService.getInterpretation( idB ) );
        assertNotNull( interpretationService.getInterpretation( idC ) );
        interpretationService.deleteInterpretation( interpretationC );
        assertNull( interpretationService.getInterpretation( idA ) );
        assertNull( interpretationService.getInterpretation( idB ) );
        assertNull( interpretationService.getInterpretation( idC ) );
    }

    @Test
    void testGetLast()
    {
        interpretationService.saveInterpretation( interpretationA );
        interpretationService.saveInterpretation( interpretationB );
        interpretationService.saveInterpretation( interpretationC );
        List<Interpretation> interpretations = interpretationService.getInterpretations( 0, 50 );
        assertEquals( 3, interpretations.size() );
        assertTrue( interpretations.contains( interpretationA ) );
        assertTrue( interpretations.contains( interpretationB ) );
        assertTrue( interpretations.contains( interpretationC ) );
    }

    @Test
    void testAddComment()
    {
        interpretationService.saveInterpretation( interpretationA );
        String uid = interpretationA.getUid();
        assertNotNull( uid );
        interpretationService.addInterpretationComment( uid, "This interpretation is good" );
        interpretationService.addInterpretationComment( uid, "This interpretation is bad" );
        interpretationA = interpretationService.getInterpretation( uid );
        assertNotNull( interpretationA.getComments() );
        assertEquals( 2, interpretationA.getComments().size() );
    }

    @Test
    void testUpdateComment()
    {
        interpretationService.saveInterpretation( interpretationA );
        String uid = interpretationA.getUid();
        assertNotNull( uid );
        InterpretationComment comment = interpretationService.addInterpretationComment( uid,
            "This interpretation is good" );
        comment.setText( "Comment with Mentions @" + userA.getUsername() + " @" + userB.getUsername() );
        interpretationService.updateComment( interpretationA, comment );
        assertEquals( 2, comment.getMentions().size() );
    }

    @Test
    void testMentions()
    {
        String text;
        // Testing with mentions
        interpretationA = new Interpretation( visualizationA, null,
            "Interpration of chart A with Mentions @" + userA.getUsername() );
        interpretationService.saveInterpretation( interpretationA );
        String uid = interpretationA.getUid();
        assertNotNull( uid );
        assertNotNull( interpretationA.getMentions() );
        assertEquals( 1, interpretationA.getMentions().size() );
        text = "Interpretation of chart A with Mentions @" + userA.getUsername() + " @" + userB.getUsername();
        interpretationService.updateInterpretationText( interpretationA, text );
        uid = interpretationA.getUid();
        assertNotNull( uid );
        assertNotNull( interpretationA.getMentions() );
        assertEquals( 2, interpretationA.getMentions().size() );
        InterpretationComment interpretationComment = interpretationService.addInterpretationComment( uid,
            "This interpretation is good @" + userA.getUsername() + " @" + userB.getUsername() );
        assertNotNull( interpretationComment.getMentions() );
        assertEquals( 2, interpretationComment.getMentions().size() );
        interpretationA = interpretationService.getInterpretation( uid );
        assertNotNull( interpretationA.getComments() );
        assertEquals( 1, interpretationA.getComments().size() );
        assertNotNull( interpretationA.getMentions() );
        assertEquals( 2, interpretationA.getMentions().size() );
        assertNotNull( interpretationComment.getMentions() );
        assertEquals( 2, interpretationComment.getMentions().size() );
        InterpretationComment interpretationComment2 = interpretationService.addInterpretationComment( uid,
            "This interpretation is bad @" + userA.getUsername() + " @" + userB.getUsername() + " @"
                + userC.getUsername() );
        assertNotNull( interpretationComment2.getMentions() );
        assertEquals( 3, interpretationComment2.getMentions().size() );
        interpretationA = interpretationService.getInterpretation( uid );
        assertNotNull( interpretationA.getComments() );
        assertEquals( 2, interpretationA.getComments().size() );
        // Testing with no mention or non real mentions
        interpretationB = new Interpretation( visualizationA, null, "Interpration of chart B with no mentions" );
        interpretationService.saveInterpretation( interpretationB );
        uid = interpretationB.getUid();
        assertNotNull( uid );
        assertNotNull( interpretationB.getMentions() );
        assertEquals( 0, interpretationB.getMentions().size() );
        text = "Interpration of chart B with fake mention @thisisnotauser";
        interpretationService.updateInterpretationText( interpretationB, text );
        uid = interpretationB.getUid();
        assertNotNull( uid );
        assertNotNull( interpretationB.getMentions() );
        assertEquals( 0, interpretationB.getMentions().size() );
        text = "Interpration of chart B with 3 mentions @" + userA.getUsername() + " @" + userB.getUsername() + " @"
            + userC.getUsername();
        interpretationService.updateInterpretationText( interpretationB, text );
        uid = interpretationB.getUid();
        assertNotNull( uid );
        assertNotNull( interpretationB.getMentions() );
        assertEquals( 3, interpretationB.getMentions().size() );
        InterpretationComment interpretationComment3 = interpretationService.addInterpretationComment( uid,
            "This interpretation has no mentions" );
        assertNotNull( interpretationComment3.getMentions() );
        assertEquals( 0, interpretationComment3.getMentions().size() );
        interpretationComment3 = interpretationService.addInterpretationComment( uid,
            "This interpretation has a fake mention @thisisnotauser" );
        assertNotNull( interpretationComment3.getMentions() );
        assertEquals( 0, interpretationComment3.getMentions().size() );
        interpretationA = interpretationService.getInterpretation( uid );
        assertNotNull( interpretationA.getComments() );
        assertEquals( 2, interpretationA.getComments().size() );
        assertNotNull( interpretationA.getMentions() );
        assertEquals( 3, interpretationA.getMentions().size() );
        assertNotNull( interpretationComment3.getMentions() );
        assertEquals( 0, interpretationComment3.getMentions().size() );
    }

    @Test
    void testGetNewCount()
    {
        interpretationService.saveInterpretation( interpretationA );
        interpretationService.saveInterpretation( interpretationB );
        interpretationService.saveInterpretation( interpretationC );
        long count = interpretationService.getNewInterpretationCount();
        assertEquals( 3, count );
    }

    @Test
    void testLikeInterpretation()
    {
        long idA = interpretationService.saveInterpretation( interpretationA );
        interpretationService.saveInterpretation( interpretationB );
        assertEquals( 0, interpretationA.getLikes() );
        assertEquals( 0, interpretationA.getLikedBy().size() );
        interpretationService.likeInterpretation( idA );
        assertEquals( 1, interpretationA.getLikes() );
        assertEquals( 1, interpretationA.getLikedBy().size() );
        interpretationService.unlikeInterpretation( idA );
        assertEquals( 0, interpretationA.getLikes() );
        assertEquals( 0, interpretationA.getLikedBy().size() );
    }

    // TODO enable
    @Test
    @Disabled
    void testCreateChartAndInterpretationSyncSharing()
        throws IOException
    {
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( userA, userB ) );
        userGroupService.addUserGroup( userGroup );
        Visualization visualization = createVisualization( 'A' );
        manager.save( visualization );
        visualization.setPublicAccess( AccessStringHelper.READ_WRITE );
        visualization.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, AccessStringHelper.READ ) );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        manager.update( visualization );
        assertEquals( AccessStringHelper.READ_WRITE, visualization.getPublicAccess() );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        Interpretation interpretation = new Interpretation( visualization, null, "test" );
        interpretationService.saveInterpretation( interpretation );
        interpretationService.updateInterpretation( interpretation );
        assertEquals( AccessStringHelper.READ_WRITE, interpretation.getPublicAccess() );
        assertEquals( interpretation.getUserGroupAccesses().size(), visualization.getUserGroupAccesses().size() );
    }
}
