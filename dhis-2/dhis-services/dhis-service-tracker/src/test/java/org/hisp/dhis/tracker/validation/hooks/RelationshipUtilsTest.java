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
package org.hisp.dhis.tracker.validation.hooks;/*
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

import static org.junit.Assert.assertEquals;

import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.junit.Test;

public class RelationshipUtilsTest
{

    @Test
    public void testExtractRelationshipItemUid()
    {
        RelationshipItem itemA = new RelationshipItem();
        RelationshipItem itemB = new RelationshipItem();
        RelationshipItem itemC = new RelationshipItem();
        itemA.setTrackedEntity( "A" );
        itemB.setEnrollment( "B" );
        itemC.setEvent( "C" );

        assertEquals( "A", RelationshipUtils.extractRelationshipItemUid( itemA ) );
        assertEquals( "B", RelationshipUtils.extractRelationshipItemUid( itemB ) );
        assertEquals( "C", RelationshipUtils.extractRelationshipItemUid( itemC ) );
    }

    @Test
    public void testGenerateRelationshipKey()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntity( "A" );
        to.setTrackedEntity( "B" );

        Relationship relationship = new Relationship();
        relationship.setRelationshipType( "C" );
        relationship.setFrom( from );
        relationship.setTo( to );

        assertEquals( "C_A_B", RelationshipUtils.generateRelationshipKey( relationship ) );
    }

    @Test
    public void testGenerateRelationshipInvertedKey()
    {
        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntity( "A" );
        to.setTrackedEntity( "B" );

        Relationship relationship = new Relationship();
        relationship.setRelationshipType( "C" );
        relationship.setFrom( from );
        relationship.setTo( to );

        assertEquals( "C_B_A", RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
    }
}
