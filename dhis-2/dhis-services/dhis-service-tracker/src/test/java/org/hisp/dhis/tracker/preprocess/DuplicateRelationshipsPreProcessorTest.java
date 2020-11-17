package org.hisp.dhis.tracker.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class DuplicateRelationshipsPreProcessorTest
{
    private DuplicateRelationshipsPreProcessor preProcessor;

    @Before
    public void setUp()
    {
        this.preProcessor = new DuplicateRelationshipsPreProcessor();
    }

    @Test
    public void test_on_identical_rels_1_is_removed()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    @Test
    public void test_on_different_rels_none_is_removed()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( false )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        Relationship relationship2 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( false )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .enrollment( toTeiUid )
                        .build() )
                .build();

        TrackerBundle bundle = TrackerBundle.builder()
                .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    @Test
    public void test_on_identical_but_inverted_rels_none_is_removed()
    {

        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .build();

        Relationship relationship2 = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( relType )
            .bidirectional( false )
            .from( RelationshipItem.builder()
                .trackedEntity( toTeiUid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( fromTeiUid )
                .build() )
            .build();

        TrackerBundle bundle = TrackerBundle.builder()
            .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }

    @Test
    public void test_on_identical_rels_but_inverted_both_bi_1_is_removed()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( true )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        Relationship relationship2 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( true )
                .from( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .build();

        TrackerBundle bundle = TrackerBundle.builder()
                .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    @Test
    public void test_on_identical_rels_both_bi_1_is_removed()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( true )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        Relationship relationship2 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( true )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        TrackerBundle bundle = TrackerBundle.builder()
                .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 1 ) );
    }

    @Test
    public void identical_rels_one_bi_other_uni_none_is_removed()
    {
        String relType = CodeGenerator.generateUid();
        String fromTeiUid = CodeGenerator.generateUid();
        String toTeiUid = CodeGenerator.generateUid();

        Relationship relationship1 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( false )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        Relationship relationship2 = Relationship.builder()
                .relationship( CodeGenerator.generateUid() )
                .relationshipType( relType )
                .bidirectional( true )
                .from( RelationshipItem.builder()
                        .trackedEntity( fromTeiUid )
                        .build() )
                .to( RelationshipItem.builder()
                        .trackedEntity( toTeiUid )
                        .build() )
                .build();

        TrackerBundle bundle = TrackerBundle.builder()
                .relationships( Lists.newArrayList( relationship1, relationship2 ) ).build();

        preProcessor.process( bundle );

        assertThat( bundle.getRelationships(), hasSize( 2 ) );
    }
}