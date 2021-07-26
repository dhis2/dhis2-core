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
package org.hisp.dhis.deduplication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class DeduplicationServiceTest
{
    @InjectMocks
    private DefaultDeduplicationService deduplicationService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Mock
    private TrackedEntityInstance trackedEntityInstanceA;

    @Mock
    private TrackedEntityInstance trackedEntityInstanceB;

    @Mock
    private RelationshipItem relationshipItemAFrom;

    @Mock
    private RelationshipItem relationshipItemBFrom;

    @Mock
    private RelationshipItem relationshipItemATo;

    @Mock
    private RelationshipItem relationshipItemBTo;

    @Mock
    private Relationship relationshipA;

    @Mock
    private Relationship relationshipB;

    private PotentialDuplicate potentialDuplicate;

    private static final String teiA = "trackedentA";

    private static final String teiB = "trackedentB";

    private static final String sexUid = "sexAttributUid";

    private static final String sexName = "sex";

    private static final String firstNameUid = "nameAttributUid";

    private static final String firstName = "firstName";

    private static final String teavSex = "Male";

    private static final String teavSexFirstName = "John";

    @Before
    public void setUp()
    {
        potentialDuplicate = new PotentialDuplicate( teiA, teiB );

        when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) ).thenReturn( trackedEntityInstanceA );
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiB ) ).thenReturn( trackedEntityInstanceB );

        String uidPerson = "uidPerson";

        TrackedEntityType trackedEntityPerson = new TrackedEntityType();
        trackedEntityPerson.setName( "Person" );
        trackedEntityPerson.setUid( uidPerson );

        when( trackedEntityInstanceA.getTrackedEntityType() ).thenReturn( trackedEntityPerson );
        when( trackedEntityInstanceB.getTrackedEntityType() ).thenReturn( trackedEntityPerson );

        setAttributeValues();

        setRelationShipTeiA();

        setRelationShipTeiB();
    }

    private void setAttributeValues()
    {
        TrackedEntityAttributeValue sexAttributeValueA = getTrackedEntityAttributeValue( sexUid, sexName,
            trackedEntityInstanceA );
        sexAttributeValueA.setValue( teavSex );
        TrackedEntityAttributeValue nameAttributeValueA = getTrackedEntityAttributeValue( firstNameUid, firstName,
            trackedEntityInstanceA );
        nameAttributeValueA.setValue( teavSexFirstName );

        TrackedEntityAttributeValue sexAttributeValueB = getTrackedEntityAttributeValue( sexUid, sexName,
            trackedEntityInstanceB );
        sexAttributeValueB.setValue( teavSex );
        TrackedEntityAttributeValue nameAttributeValueB = getTrackedEntityAttributeValue( firstNameUid, firstName,
            trackedEntityInstanceB );
        nameAttributeValueB.setValue( teavSexFirstName );

        when( trackedEntityInstanceA.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>( Arrays.asList( sexAttributeValueA, nameAttributeValueA ) ) );

        when( trackedEntityInstanceB.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>( Arrays.asList( sexAttributeValueB, nameAttributeValueB ) ) );
    }

    private void setRelationShipTeiB()
    {
        when( relationshipItemBFrom.getRelationship() ).thenReturn( relationshipB );
        when( relationshipItemBTo.getRelationship() ).thenReturn( relationshipB );

        lenient().when( relationshipItemBFrom.getTrackedEntityInstance() ).thenReturn( trackedEntityInstanceB );

        when( relationshipB.getTo() ).thenReturn( relationshipItemBTo );

        when( trackedEntityInstanceB.getRelationshipItems() )
            .thenReturn( new HashSet<>( Arrays.asList( relationshipItemBFrom, relationshipItemBTo ) ) );
    }

    private void setRelationShipTeiA()
    {
        when( relationshipItemAFrom.getRelationship() ).thenReturn( relationshipA );
        when( relationshipItemATo.getRelationship() ).thenReturn( relationshipA );

        lenient().when( relationshipItemAFrom.getTrackedEntityInstance() ).thenReturn( trackedEntityInstanceA );

        when( relationshipA.getTo() ).thenReturn( relationshipItemATo );

        when( trackedEntityInstanceA.getRelationshipItems() )
            .thenReturn( new HashSet<>( Arrays.asList( relationshipItemAFrom, relationshipItemATo ) ) );
    }

    @Test
    public void shouldBeAutoMergeable()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( "uid" );

        lenient().when( relationshipItemATo.getProgramInstance() ).thenReturn( programInstance );
        lenient().when( relationshipItemBTo.getProgramInstance() ).thenReturn( programInstance );

        assertTrue( deduplicationService.isAutoMergeable( potentialDuplicate ) );
    }

    @Test
    public void shouldNotBeAutoMergeableDifferentTrackedEntityType()
    {
        String uidOther = "uidOther";

        TrackedEntityType trackedEntityOther = new TrackedEntityType();
        trackedEntityOther.setName( "Other" );
        trackedEntityOther.setUid( uidOther );

        when( trackedEntityInstanceB.getTrackedEntityType() ).thenReturn( trackedEntityOther );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );
    }

    @Test
    public void shouldNotBeAutoMergeableDeletedTrackedEntityInstance()
    {
        when( trackedEntityInstanceA.isDeleted() ).thenReturn( true );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );

        when( trackedEntityInstanceA.isDeleted() ).thenReturn( false );
        when( trackedEntityInstanceB.isDeleted() ).thenReturn( true );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );
    }

    @Test
    public void shouldNotBeAutoMergeableRelationShipBetweenEntities()
    {
        when( relationshipItemBTo.getTrackedEntityInstance() ).thenReturn( trackedEntityInstanceA );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );

        lenient().when( relationshipItemBTo.getTrackedEntityInstance() ).thenReturn( null );

        when( relationshipItemATo.getTrackedEntityInstance() ).thenReturn( trackedEntityInstanceB );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );
    }

    @Test
    public void shouldNotBeAutoMergeableDifferentAttributeValues()
    {
        TrackedEntityAttributeValue sexAttributeValueB = getTrackedEntityAttributeValue( sexUid, sexName,
            trackedEntityInstanceB );
        sexAttributeValueB.setValue( teavSex );
        TrackedEntityAttributeValue nameAttributeValueB = getTrackedEntityAttributeValue( firstNameUid, firstName,
            trackedEntityInstanceB );
        nameAttributeValueB.setValue( "Jimmy" );

        when( trackedEntityInstanceB.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>( Arrays.asList( sexAttributeValueB, nameAttributeValueB ) ) );

        assertFalse( deduplicationService.isAutoMergeable( potentialDuplicate ) );
    }

    @Test( expected = PotentialDuplicateException.class )
    public void shouldThrowMissingTeiA()
    {
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) ).thenReturn( null );

        deduplicationService.isAutoMergeable( potentialDuplicate );
    }

    @Test( expected = PotentialDuplicateException.class )
    public void shouldThrowMissingTeiAB()
    {
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiB ) ).thenReturn( null );

        deduplicationService.isAutoMergeable( potentialDuplicate );
    }

    private TrackedEntityAttributeValue getTrackedEntityAttributeValue( String uid, String name,
        TrackedEntityInstance trackedEntityInstance )
    {
        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
        attributeValue.setEntityInstance( trackedEntityInstance );
        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( uid );
        trackedEntityAttribute.setName( name );

        attributeValue.setAttribute( trackedEntityAttribute );

        return attributeValue;
    }
}
