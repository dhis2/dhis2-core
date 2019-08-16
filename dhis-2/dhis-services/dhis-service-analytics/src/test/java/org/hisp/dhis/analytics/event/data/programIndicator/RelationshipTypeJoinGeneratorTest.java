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

package org.hisp.dhis.analytics.event.data.programIndicator;

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.analytics.event.data.programIndicator.RelationshipTypeJoinGenerator;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.junit.Before;
import org.junit.Test;

import static org.hisp.dhis.relationship.RelationshipEntity.*;
import static org.junit.Assert.*;

/**
 * @author Luciano Fiandesio
 */
public class RelationshipTypeJoinGeneratorTest
{
    private BeanRandomizer beanRandomizer;

    @Before
    public void setUp()
    {
        beanRandomizer = new BeanRandomizer();
    }

    private final static String TEI_JOIN = " ax.tei in (select tei.uid from trackedentityinstance tei LEFT JOIN relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid ";

    private final static String PI_JOIN = " ax.pi in (select pi.uid from programinstance pi LEFT JOIN relationshipitem ri on pi.programinstanceid = ri.programinstanceid ";

    private final static String PSI_JOIN = " ax.pi in (select pi.uid from programstageinstance psi LEFT JOIN relationshipitem ri on psi.programstageinstance = ri.programinstanceid ";

    @Test
    public void verifyTeiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            TRACKED_ENTITY_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( TEI_JOIN + addCommonJoin( relationshipType ), sql );
    }

    @Test
    public void verifyPiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            PROGRAM_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( PI_JOIN + addCommonJoin( relationshipType ), sql );
    }

    @Test
    public void verifyPsiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( PSI_JOIN + addCommonJoin( relationshipType ), sql );
    }

    @Test
    public void verifyTeiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            PROGRAM_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + TEI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + PI_JOIN
            + addCommonJoin( relationshipType ) + ") )", sql );
    }

    @Test
    public void verifyTeiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + TEI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + PSI_JOIN
            + addCommonJoin( relationshipType ) + ") )", sql );
    }

    @Test
    public void verifyPiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            TRACKED_ENTITY_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + PI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + TEI_JOIN
            + addCommonJoin( relationshipType ) + ") )", sql );
    }

    @Test
    public void verifyPiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + PI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + PSI_JOIN
            + addCommonJoin( relationshipType ) + ") )", sql );
    }

    @Test
    public void verifyPsiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
                TRACKED_ENTITY_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + PSI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + TEI_JOIN
                + addCommonJoin( relationshipType ) + ") )", sql );
    }

    @Test
    public void verifyPsiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
                PROGRAM_INSTANCE.getName() );

        String sql = RelationshipTypeJoinGenerator.generate( relationshipType );
        assertEquals( " ( (" + PSI_JOIN + addCommonJoin( relationshipType ) + ") OR (" + PI_JOIN
                + addCommonJoin( relationshipType ) + ") )", sql );
    }

    private RelationshipType createRelationshipType( String fromConstraint, String toConstraint )
    {
        RelationshipType relationshipType = beanRandomizer.randomObject( RelationshipType.class );
        relationshipType.getFromConstraint().setRelationshipEntity( RelationshipEntity.get( fromConstraint ) );
        relationshipType.getToConstraint().setRelationshipEntity( RelationshipEntity.get( toConstraint ) );

        return relationshipType;
    }

    private String addCommonJoin( RelationshipType relationshipType )
    {
        return new org.apache.commons.text.StrSubstitutor(
            ImmutableMap.<String, Long> builder().put( "relationshipid", relationshipType.getId() ).build() )
                .replace( RelationshipTypeJoinGenerator.RELATIONSHIP_JOIN );
    }
}