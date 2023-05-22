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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class RelationshipTypeJoinGeneratorTest
{
    private final static String ALIAS = "subax";

    private final static String RELATIONSHIP_JOIN = " LEFT JOIN relationship r on r.from_relationshipitemid = ri.relationshipitemid "
        + "LEFT JOIN relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
        + "LEFT JOIN relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid ";

    private final static String TEI_JOIN_START = ALIAS
        + ".tei in (select tei.uid from trackedentityinstance tei LEFT JOIN relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid ";

    private final static String PI_JOIN_START = ALIAS
        + ".pi in (select pi.uid from programinstance pi LEFT JOIN relationshipitem ri on pi.programinstanceid = ri.programinstanceid ";

    private final static String PSI_JOIN_START = ALIAS
        + ".psi in (select psi.uid from programstageinstance psi LEFT JOIN relationshipitem ri on psi.programstageinstanceid = ri.programstageinstanceid ";

    private final static String TEI_RELTO_JOIN = "LEFT JOIN trackedentityinstance tei on tei.trackedentityinstanceid = ri2.trackedentityinstanceid";

    private final static String PI_RELTO_JOIN = "LEFT JOIN programinstance pi on pi.programinstanceid = ri2.programinstanceid";

    private final static String PSI_RELTO_JOIN = "LEFT JOIN programstageinstance psi on psi.programstageinstanceid = ri2.programstageinstanceid";

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @Test
    void verifyTeiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            TRACKED_ENTITY_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
        asserter( relationshipType, AnalyticsType.EVENT );
    }

    @Test
    void verifyPiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            PROGRAM_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyPsiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyTeiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            PROGRAM_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyTeiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( TRACKED_ENTITY_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyPiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            TRACKED_ENTITY_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyPiToPsi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_INSTANCE.getName(),
            PROGRAM_STAGE_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyPsiToTei()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
            TRACKED_ENTITY_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    @Test
    void verifyPsiToPi()
    {
        RelationshipType relationshipType = createRelationshipType( PROGRAM_STAGE_INSTANCE.getName(),
            PROGRAM_INSTANCE.getName() );
        asserter( relationshipType, AnalyticsType.EVENT );
        asserter( relationshipType, AnalyticsType.ENROLLMENT );
    }

    private RelationshipType createRelationshipType( String fromConstraint, String toConstraint )
    {
        RelationshipType relationshipType = rnd.nextObject( RelationshipType.class );
        relationshipType.getFromConstraint().setRelationshipEntity( RelationshipEntity.get( fromConstraint ) );
        relationshipType.getToConstraint().setRelationshipEntity( RelationshipEntity.get( toConstraint ) );
        return relationshipType;
    }

    private String addWhere( RelationshipType relationshipType )
    {
        return new StringSubstitutor( Map.of( "relationshipid", relationshipType.getId() ) )
            .replace( RelationshipTypeJoinGenerator.RELATIONSHIP_JOIN );
    }

    private void asserter( RelationshipType relationshipType, AnalyticsType type )
    {
        RelationshipEntity from = relationshipType.getFromConstraint().getRelationshipEntity();
        RelationshipEntity to = relationshipType.getToConstraint().getRelationshipEntity();
        String expected = " ";
        expected += getFromRelationshipEntity( from, type );
        expected += RELATIONSHIP_JOIN;
        expected += getToRelationshipEntity( to );
        expected += addWhere( relationshipType );
        expected += (to.equals( TRACKED_ENTITY_INSTANCE ) ? " AND tei.uid = ax.tei )"
            : (to.equals( PROGRAM_INSTANCE ) ? " AND pi.uid = ax.pi )" : " AND psi.uid = ax.psi )"));
        assertEquals( expected, RelationshipTypeJoinGenerator.generate( ALIAS, relationshipType, type ) );
    }

    private static String getFromRelationshipEntity( RelationshipEntity relationshipEntity,
        AnalyticsType programIndicatorType )
    {
        switch ( relationshipEntity )
        {
            case TRACKED_ENTITY_INSTANCE:
                return TEI_JOIN_START;
            case PROGRAM_STAGE_INSTANCE:
            case PROGRAM_INSTANCE:
                return (programIndicatorType.equals( AnalyticsType.EVENT ) ? PSI_JOIN_START : PI_JOIN_START);
        }
        return "";
    }

    private static String getToRelationshipEntity( RelationshipEntity relationshipEntity )
    {
        switch ( relationshipEntity )
        {
            case TRACKED_ENTITY_INSTANCE:
                return TEI_RELTO_JOIN;
            case PROGRAM_STAGE_INSTANCE:
                return PSI_RELTO_JOIN;
            case PROGRAM_INSTANCE:
                return PI_RELTO_JOIN;
        }
        return "";
    }
}
