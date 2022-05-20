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
package org.hisp.dhis.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelationshipUtilsTest
{
    private static final String TEIA_UID = "TEIA_UID";

    private static final String TEIB_UID = "TEIB_UID";

    private static final String PI_UID = "PI_UID";

    private static final String PSI_UID = "PSI_UID";

    private static final String RELATIONSHIP_TYPE_UID = "RELATIONSHIP_TYPE_UID";

    private TrackedEntityInstance teiA, teiB;

    private ProgramInstance piA;

    private ProgramStageInstance psiA;

    private RelationshipType relationshipType;

    @BeforeEach
    void setup()
    {
        teiA = new TrackedEntityInstance();
        teiA.setUid( TEIA_UID );
        teiB = new TrackedEntityInstance();
        teiB.setUid( TEIB_UID );
        piA = new ProgramInstance();
        piA.setUid( PI_UID );
        psiA = new ProgramStageInstance();
        psiA.setUid( PSI_UID );
        relationshipType = new RelationshipType();
        relationshipType.setUid( RELATIONSHIP_TYPE_UID );
    }

    @Test
    void testExtractRelationshipItemUid()
    {
        RelationshipItem itemA = new RelationshipItem();
        RelationshipItem itemB = new RelationshipItem();
        RelationshipItem itemC = new RelationshipItem();
        itemA.setTrackedEntityInstance( teiA );
        itemB.setProgramInstance( piA );
        itemC.setProgramStageInstance( psiA );
        assertEquals( teiA.getUid(), RelationshipUtils.extractRelationshipItemUid( itemA ) );
        assertEquals( piA.getUid(), RelationshipUtils.extractRelationshipItemUid( itemB ) );
        assertEquals( psiA.getUid(), RelationshipUtils.extractRelationshipItemUid( itemC ) );
    }

    @Test
    void testGenerateRelationshipKeyForTeiToTei()
    {
        Relationship relationship = teiAToTeiBRelationship();
        String key = relationshipType.getUid() + "_" + teiA.getUid() + "_" + teiB.getUid();
        assertEquals( key, RelationshipUtils.generateRelationshipKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipInvertedKeyForTeiToTei()
    {
        Relationship relationship = teiAToTeiBRelationship();
        String invertedKey = relationshipType.getUid() + "_" + teiB.getUid() + "_" + teiA.getUid();
        assertEquals( invertedKey, RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipKeyForTeiToEnrollemnt()
    {
        Relationship relationship = teiToEnrollmentRelationship();
        String key = relationshipType.getUid() + "_" + teiA.getUid() + "_" + piA.getUid();
        assertEquals( key, RelationshipUtils.generateRelationshipKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipInvertedKeyForTeiToEnrollment()
    {
        Relationship relationship = teiToEnrollmentRelationship();
        String invertedKey = relationshipType.getUid() + "_" + piA.getUid() + "_" + teiA.getUid();
        assertEquals( invertedKey, RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipKeyForTeiToEvent()
    {
        Relationship relationship = teiToEventRelationship();
        String key = relationshipType.getUid() + "_" + teiA.getUid() + "_" + psiA.getUid();
        assertEquals( key, RelationshipUtils.generateRelationshipKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipInvertedKeyForTeiToEvent()
    {
        Relationship relationship = teiToEventRelationship();
        String invertedKey = relationshipType.getUid() + "_" + psiA.getUid() + "_" + teiA.getUid();
        assertEquals( invertedKey, RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipKeyForEnrollmentToEvent()
    {
        Relationship relationship = enrollmentToEventRelationship();
        String key = relationshipType.getUid() + "_" + piA.getUid() + "_" + psiA.getUid();
        assertEquals( key, RelationshipUtils.generateRelationshipKey( relationship ) );
    }

    @Test
    void testGenerateRelationshipInvertedKeyForEnrollmentToEvent()
    {
        Relationship relationship = enrollmentToEventRelationship();
        String invertedKey = relationshipType.getUid() + "_" + psiA.getUid() + "_" + piA.getUid();
        assertEquals( invertedKey, RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
    }

    private Relationship teiAToTeiBRelationship()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntityInstance( teiA );
        to.setTrackedEntityInstance( teiB );

        return relationship( from, to );
    }

    private Relationship teiToEnrollmentRelationship()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntityInstance( teiA );
        to.setProgramInstance( piA );

        return relationship( from, to );
    }

    private Relationship teiToEventRelationship()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntityInstance( teiA );
        to.setProgramStageInstance( psiA );

        return relationship( from, to );
    }

    private Relationship enrollmentToEventRelationship()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setProgramInstance( piA );
        to.setProgramStageInstance( psiA );

        return relationship( from, to );
    }

    private Relationship relationship( RelationshipItem from, RelationshipItem to )
    {
        Relationship relationship = new Relationship();
        relationship.setRelationshipType( relationshipType );
        relationship.setFrom( from );
        relationship.setTo( to );

        return relationship;
    }
}
