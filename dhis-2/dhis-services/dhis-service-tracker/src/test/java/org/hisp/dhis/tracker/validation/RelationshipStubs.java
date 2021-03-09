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
package org.hisp.dhis.tracker.validation;

import java.util.List;

import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;

import com.google.common.collect.Lists;

public class RelationshipStubs
{

    private final static String TEI_TO_TEI_RELATIONSHIP_TYPE = "TEI_TO_TEI";

    private final static String TEI_TO_EVENT_RELATIONSHIP_TYPE = "TEI_TO_EVENT";

    private final static String EVENT_TO_TEI_RELATIONSHIP_TYPE = "EVENT_TO_TEI";

    private final static String TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE = "TEI_TO_ENROLLMENT";

    private final static String ENROLLMENT_TO_EVENT_RELATIONSHIP_TYPE = "ENROLLMENT_TO_EVENT";

    private final static String VALID_RELATIONSHIP_UID = "valid uid";

    private final static String VALID_DUPLICATED_RELATIONSHIP_UID = "valid duplicated uid";

    public static List<RelationshipType> getRelationshipTypes()
    {
        RelationshipConstraint teiRelationshipConstraint = new RelationshipConstraint();
        teiRelationshipConstraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );

        RelationshipConstraint eventRelationshipConstraint = new RelationshipConstraint();
        eventRelationshipConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );

        RelationshipConstraint enrollmentRelationshipConstraint = new RelationshipConstraint();
        enrollmentRelationshipConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );

        RelationshipType relationshipTypeA = new RelationshipType();
        relationshipTypeA.setUid( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        relationshipTypeA.setFromConstraint( teiRelationshipConstraint );
        relationshipTypeA.setToConstraint( eventRelationshipConstraint );

        RelationshipType relationshipTypeB = new RelationshipType();
        relationshipTypeB.setUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE );
        relationshipTypeB.setFromConstraint( teiRelationshipConstraint );
        relationshipTypeB.setToConstraint( enrollmentRelationshipConstraint );

        RelationshipType relationshipTypeC = new RelationshipType();
        relationshipTypeC.setUid( ENROLLMENT_TO_EVENT_RELATIONSHIP_TYPE );
        relationshipTypeC.setFromConstraint( enrollmentRelationshipConstraint );
        relationshipTypeC.setToConstraint( eventRelationshipConstraint );

        RelationshipType relationshipTypeD = new RelationshipType();
        relationshipTypeD.setUid( TEI_TO_TEI_RELATIONSHIP_TYPE );
        relationshipTypeD.setFromConstraint( teiRelationshipConstraint );
        relationshipTypeD.setToConstraint( teiRelationshipConstraint );

        RelationshipType relationshipTypeE = new RelationshipType();
        relationshipTypeE.setUid( EVENT_TO_TEI_RELATIONSHIP_TYPE );
        relationshipTypeE.setFromConstraint( eventRelationshipConstraint );
        relationshipTypeE.setToConstraint( teiRelationshipConstraint );

        return Lists.newArrayList( relationshipTypeA, relationshipTypeB, relationshipTypeC, relationshipTypeD,
            relationshipTypeE );
    }

    public static Relationship getBadTEIRelationshipItemRelationship()
    {
        RelationshipItem notValidTEIRelationshipItem = new RelationshipItem();
        notValidTEIRelationshipItem.setEvent( "eventG" );
        notValidTEIRelationshipItem.setEnrollment( "enrollmentH" );
        RelationshipItem validEventRelationshipItem = new RelationshipItem();
        validEventRelationshipItem.setEvent( "eventB" );

        Relationship notValidRelationship = new Relationship();
        notValidRelationship.setRelationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        notValidRelationship.setFrom( notValidTEIRelationshipItem );
        notValidRelationship.setTo( validEventRelationshipItem );

        return notValidRelationship;
    }

    public static Relationship getBadEnrollmentRelationshipItemRelationship()
    {
        RelationshipItem notValidEnrollmentRelationshipItem = new RelationshipItem();
        notValidEnrollmentRelationshipItem.setTrackedEntity( "trackedEntityE" );
        notValidEnrollmentRelationshipItem.setEvent( "eventF" );
        RelationshipItem validEventRelationshipItem = new RelationshipItem();
        validEventRelationshipItem.setEvent( "eventB" );

        Relationship notValidRelationship = new Relationship();
        notValidRelationship.setRelationshipType( ENROLLMENT_TO_EVENT_RELATIONSHIP_TYPE );
        notValidRelationship.setFrom( notValidEnrollmentRelationshipItem );
        notValidRelationship.setTo( validEventRelationshipItem );

        return notValidRelationship;
    }

    public static Relationship getBadEventRelationshipItemRelationship()
    {
        RelationshipItem validEnrollmentRelationshipItem = new RelationshipItem();
        validEnrollmentRelationshipItem.setEnrollment( "enrollmentH" );
        RelationshipItem notValidEventRelationshipItem = new RelationshipItem();
        notValidEventRelationshipItem.setTrackedEntity( "trackedEntityC" );
        notValidEventRelationshipItem.setEnrollment( "enrollmentD" );

        Relationship notValidRelationship = new Relationship();
        notValidRelationship.setRelationshipType( ENROLLMENT_TO_EVENT_RELATIONSHIP_TYPE );
        notValidRelationship.setFrom( validEnrollmentRelationshipItem );
        notValidRelationship.setTo( notValidEventRelationshipItem );

        return notValidRelationship;
    }

    public static Relationship getMissingToRelationshipItemRelationship()
    {
        Relationship validRelationship = getValidRelationship();
        validRelationship.setTo( null );
        return validRelationship;
    }

    public static Relationship getMissingFromRelationshipItemRelationship()
    {
        Relationship validRelationship = getValidRelationship();
        validRelationship.setFrom( null );
        return validRelationship;
    }

    public static Relationship getAutoRelationship()
    {
        RelationshipItem validTEIRelationshipItem = new RelationshipItem();
        validTEIRelationshipItem.setTrackedEntity( "trackedEntityA" );

        Relationship autoRelationship = new Relationship();
        autoRelationship.setRelationshipType( TEI_TO_TEI_RELATIONSHIP_TYPE );
        autoRelationship.setFrom( validTEIRelationshipItem );
        autoRelationship.setTo( validTEIRelationshipItem );

        return autoRelationship;
    }

    public static List<Relationship> getDuplicatedAndValidRelationships()
    {
        List<Relationship> duplicatedRelationships = getDuplicatedRelationships();
        duplicatedRelationships.add( getValidRelationship() );
        return duplicatedRelationships;
    }

    public static List<Relationship> getBidirectionalDuplicatedAndValidRelationships()
    {
        List<Relationship> bidirectionalDuplicatedRelationships = getBidirectionalDuplicatedRelationships();
        bidirectionalDuplicatedRelationships.add( getValidRelationship() );
        return bidirectionalDuplicatedRelationships;
    }

    public static List<Relationship> getDuplicatedRelationships()
    {
        RelationshipItem validTEIRelationshipItem = new RelationshipItem();
        validTEIRelationshipItem.setTrackedEntity( "trackedEntityZ" );
        RelationshipItem validEventRelationshipItem = new RelationshipItem();
        validEventRelationshipItem.setEvent( "eventY" );

        Relationship validRelationship = new Relationship();

        validRelationship.setRelationship( VALID_RELATIONSHIP_UID );
        validRelationship.setBidirectional( false );
        validRelationship.setRelationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        validRelationship.setFrom( validTEIRelationshipItem );
        validRelationship.setTo( validEventRelationshipItem );

        Relationship validDuplicatedRelationship = new Relationship();

        validDuplicatedRelationship.setRelationship( VALID_DUPLICATED_RELATIONSHIP_UID );
        validDuplicatedRelationship.setBidirectional( false );
        validDuplicatedRelationship.setRelationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        validDuplicatedRelationship.setFrom( validTEIRelationshipItem );
        validDuplicatedRelationship.setTo( validEventRelationshipItem );

        return Lists.newArrayList( validRelationship, validDuplicatedRelationship );
    }

    public static List<Relationship> getBidirectionalDuplicatedRelationships()
    {
        RelationshipItem validTEIRelationshipItem = new RelationshipItem();
        validTEIRelationshipItem.setTrackedEntity( "trackedEntityZ" );
        RelationshipItem validEventRelationshipItem = new RelationshipItem();
        validEventRelationshipItem.setEvent( "eventY" );

        Relationship validRelationship = new Relationship();

        validRelationship.setRelationship( VALID_RELATIONSHIP_UID );
        validRelationship.setBidirectional( true );
        validRelationship.setRelationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        validRelationship.setFrom( validTEIRelationshipItem );
        validRelationship.setTo( validEventRelationshipItem );

        Relationship validDuplicatedRelationship = new Relationship();

        validDuplicatedRelationship.setRelationship( VALID_DUPLICATED_RELATIONSHIP_UID );
        validDuplicatedRelationship.setBidirectional( true );
        validDuplicatedRelationship.setRelationshipType( EVENT_TO_TEI_RELATIONSHIP_TYPE );
        validDuplicatedRelationship.setFrom( validEventRelationshipItem );
        validDuplicatedRelationship.setTo( validTEIRelationshipItem );

        return Lists.newArrayList( validRelationship, validDuplicatedRelationship );
    }

    public static List<Relationship> getOnlyOneBidirectionalDuplicatedRelationships()
    {
        Relationship validRelationship = getValidRelationship();

        Relationship validDuplicatedRelationship = getValidRelationship();
        validDuplicatedRelationship.setBidirectional( true );

        return Lists.newArrayList( validRelationship, validDuplicatedRelationship );
    }

    public static Relationship getValidRelationship()
    {
        return getValidRelationships().get( 0 );
    }

    public static List<Relationship> getValidRelationships()
    {
        RelationshipItem validTEIRelationshipItem = new RelationshipItem();
        validTEIRelationshipItem.setTrackedEntity( "validTEIA" );
        RelationshipItem validEventRelationshipItem = new RelationshipItem();
        validEventRelationshipItem.setEvent( "validEventB" );

        Relationship validRelationship = new Relationship();
        validRelationship.setRelationshipType( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        validRelationship.setFrom( validTEIRelationshipItem );
        validRelationship.setTo( validEventRelationshipItem );

        return Lists.newArrayList( validRelationship );
    }
}
