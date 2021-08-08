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
package org.hisp.dhis.tracker.preprocess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

@RunWith( MockitoJUnitRunner.class )
public class BidirectionalRelationshipsPreProcessorTest
    extends DhisConvenienceTest
{

    private static final String RELATIONSHIP_TYPE_UID = "RELATIONSHIP_TYPE";

    private static final String BIDIRECTIONAL_RELATIONSHIP_TYPE_UID = "BIDIRECTIONAL_RELATIONSHIP_TYPE";

    private BidirectionalRelationshipsPreProcessor preProcessorToTest = new BidirectionalRelationshipsPreProcessor();

    @Test
    public void testPreprocessorPopulateRelationshipBidirectionalFieldCorrectly()
    {
        Relationship uniDirectionalRelationship = new Relationship();
        uniDirectionalRelationship.setRelationshipType( RELATIONSHIP_TYPE_UID );
        uniDirectionalRelationship.setBidirectional( true );
        Relationship biDirectionalRelationship = new Relationship();
        biDirectionalRelationship.setRelationshipType( BIDIRECTIONAL_RELATIONSHIP_TYPE_UID );
        biDirectionalRelationship.setBidirectional( false );

        TrackerBundle bundle = TrackerBundle.builder()
            .relationships( Lists.newArrayList( uniDirectionalRelationship, biDirectionalRelationship ) )
            .preheat( getPreheat() )
            .build();

        assertTrue( uniDirectionalRelationship.isBidirectional() );
        assertFalse( biDirectionalRelationship.isBidirectional() );

        preProcessorToTest.process( bundle );

        assertFalse( uniDirectionalRelationship.isBidirectional() );
        assertTrue( biDirectionalRelationship.isBidirectional() );
    }

    private TrackerPreheat getPreheat()
    {
        RelationshipType uniDirectionalRelationshipType = createRelationshipType( 'A' );
        uniDirectionalRelationshipType.setUid( RELATIONSHIP_TYPE_UID );
        uniDirectionalRelationshipType.setBidirectional( false );
        RelationshipType biDirectionalRelationshipType = createRelationshipType( 'B' );
        biDirectionalRelationshipType.setUid( BIDIRECTIONAL_RELATIONSHIP_TYPE_UID );
        biDirectionalRelationshipType.setBidirectional( true );

        TrackerPreheat trackerPreheat = new TrackerPreheat();
        trackerPreheat.put( TrackerIdentifier.UID,
            Lists.newArrayList( biDirectionalRelationshipType, uniDirectionalRelationshipType ) );

        return trackerPreheat;
    }

}