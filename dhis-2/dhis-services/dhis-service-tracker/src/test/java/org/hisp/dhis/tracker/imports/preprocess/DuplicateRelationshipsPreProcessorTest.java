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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
class DuplicateRelationshipsPreProcessorTest
{

    private DuplicateRelationshipsPreProcessor preProcessor;

    private TrackerPreheat preheat;

    private final String REL_TYPE_BIDIRECTIONAL_UID = CodeGenerator.generateUid();

    private final String REL_TYPE_NONBIDIRECTIONAL_UID = CodeGenerator.generateUid();

    @BeforeEach
    void setUp()
    {
        preheat = new TrackerPreheat();
        RelationshipType relationshipTypeBidirectional = new RelationshipType();
        relationshipTypeBidirectional.setUid( REL_TYPE_BIDIRECTIONAL_UID );
        relationshipTypeBidirectional.setBidirectional( true );
        RelationshipType relationshipTypeNonBidirectional = new RelationshipType();
        relationshipTypeNonBidirectional.setUid( REL_TYPE_NONBIDIRECTIONAL_UID );
        preheat.put( TrackerIdSchemeParam.UID, relationshipTypeBidirectional );
        preheat.put( TrackerIdSchemeParam.UID, relationshipTypeNonBidirectional );
        this.preProcessor = new DuplicateRelationshipsPreProcessor();
    }

    @Test
    void test_relationshipIsIgnored_on_null_relType()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 identical relationships
     *
     * - one is removed
     */
    @Test
    void test_on_identical_rels_1_is_removed()
    {
        String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 non-identical relationships
     *
     * - none is removed
     */
    @Test
    void test_on_different_rels_none_is_removed()
    {
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( REL_TYPE_NONBIDIRECTIONAL_UID ) )
            .from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( REL_TYPE_NONBIDIRECTIONAL_UID ) )
            .from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( enrollmentRelationshipItem( toTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 relationships having identical but "inverted" data
     *
     * - none is removed
     */
    @Test
    void test_on_identical_but_inverted_rels_none_is_removed()
    {
        String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).bidirectional( false )
            .from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).bidirectional( false )
            .from( trackedEntityRelationshipItem( toTeiUid ) )
            .to( trackedEntityRelationshipItem( fromTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 identical relationships having identical but "inverted" data
     *
     * - and relationship type's bidirectional property = true
     *
     * - none is removed
     */
    @Test
    void test_on_identical_rels_but_inverted_type_bi_1_is_removed()
    {
        String relType = REL_TYPE_BIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).from( trackedEntityRelationshipItem( toTeiUid ) )
            .to( trackedEntityRelationshipItem( fromTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    /*
     * Verifies that:
     *
     * - given 2 identical relationships
     *
     * - and relationship type's bidirectional property = true
     *
     * - one is removed
     */
    @Test
    void test_on_identical_rels_relType_bi_1_is_removed()
    {
        String relType = REL_TYPE_BIDIRECTIONAL_UID;
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();
        Relationship relationship1 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).bidirectional( true )
            .from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        Relationship relationship2 = Relationship.builder().relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType ) ).bidirectional( true )
            .from( trackedEntityRelationshipItem( fromTeiUid ) )
            .to( trackedEntityRelationshipItem( toTeiUid ) ).build();
        TrackerBundle bundle = TrackerBundle.builder().preheat( this.preheat )
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();
        preProcessor.process( bundle );
        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    private RelationshipItem trackedEntityRelationshipItem( String trackedEntityUid )
    {
        return RelationshipItem.builder()
            .trackedEntity( trackedEntityUid )
            .build();
    }

    private RelationshipItem enrollmentRelationshipItem( String enrollmentUid )
    {
        return RelationshipItem.builder()
            .enrollment( enrollmentUid )
            .build();
    }
}
