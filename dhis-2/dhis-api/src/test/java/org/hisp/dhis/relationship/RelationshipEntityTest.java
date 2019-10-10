package org.hisp.dhis.relationship;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class RelationshipEntityTest
{
    @Test
    public void testGetRelationshipEntityByName()
    {
        RelationshipEntity relationshipEntity = RelationshipEntity.get( "tracked_entity" );
        RelationshipEntity relationshipEntityNull = RelationshipEntity.get( "I_DONT_EXIST" );

        assertThat( relationshipEntity, is( RelationshipEntity.TRACKED_ENTITY_INSTANCE ) );
        assertNull( relationshipEntityNull );
    }
}