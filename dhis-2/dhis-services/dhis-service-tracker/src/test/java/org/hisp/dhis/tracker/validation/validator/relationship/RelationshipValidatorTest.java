/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.validation.validator.relationship;

import static org.hisp.dhis.tracker.validation.ValidationCode.E1048;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1124;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4000;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4001;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4012;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class RelationshipValidatorTest
{

    private RelationshipValidator validator;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackerBundle bundle;

    private Reporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new RelationshipValidator();

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );

        when( bundle.getStrategy( any( Relationship.class ) ) ).thenReturn( TrackerImportStrategy.CREATE );
    }

    @Test
    void shouldFailWhenRelationshipUidDoesNotHaveCorrectFormat()
    {
        Relationship relationship = new Relationship();
        relationship.setRelationship( "wrong uid" );
        when( bundle.getRelationships() ).thenReturn( List.of( relationship ) );

        validator.validate( reporter, bundle, bundle );

        assertEquals( 1, reporter.getErrors().size(),
            "Expected just one error when uid not correct, but " + reporter.getErrors().size() + " found" );
        assertHasError( reporter, relationship, E1048, "uid: `wrong uid`, has an invalid uid format." );
    }

    @Test
    void shouldFailWhenRelationshipFromNotSet()
    {
        Relationship relationship = new Relationship();
        relationship.setRelationship( CodeGenerator.generateUid() );
        relationship.setTo( new RelationshipItem( "entity", "", "" ) );
        relationship.setRelationshipType( MetadataIdentifier.ofUid( "uid" ) );
        when( bundle.getRelationships() ).thenReturn( List.of( relationship ) );
        when( bundle.getPreheat() ).thenReturn( preheat );

        validator.validate( reporter, bundle, bundle );

        assertEquals( 1, reporter.getErrors().size(),
            "Expected just one error when from constraint not set, but " + reporter.getErrors().size() + " found" );
        assertHasError( reporter, relationship, E1124, "Missing required relationship property: `from`." );
    }

    @Test
    void shouldFailWhenRelationshipFromItemTypeNotSet()
    {
        String uid = CodeGenerator.generateUid();
        Relationship relationship = createProgramRelationship( "", uid );
        when( bundle.getRelationships() ).thenReturn( List.of( relationship ) );
        when( preheat.getRelationshipType( relationship.getRelationshipType() ) )
            .thenReturn( createTrackedEntityRelationship() );
        when( bundle.getPreheat() ).thenReturn( preheat );

        validator.validate( reporter, bundle, bundle );

        assertEquals( 1, reporter.getErrors().size(),
            "Expected just one error when from item type not set, but " + reporter.getErrors().size() + " found" );
        assertHasError( reporter, relationship, E4001, "Relationship Item `from` for Relationship `" + uid
            + "` is invalid: an Item can link only one Tracker entity." );
    }

    @Test
    void shouldFailWhenRelationshipLinksToItself()
    {
        String uid = CodeGenerator.generateUid();
        Relationship relationship = createProgramRelationship( "enrollment", uid );
        when( bundle.getRelationships() ).thenReturn( List.of( relationship ) );
        when( preheat.getRelationshipType( relationship.getRelationshipType() ) )
            .thenReturn( createTrackedEntityRelationship() );
        when( bundle.getPreheat() ).thenReturn( preheat );

        validator.validate( reporter, bundle, bundle );

        assertEquals( 1, reporter.getErrors().size(), "Expected just one error when relationship links to itself, but "
            + reporter.getErrors().size() + " found" );
        assertHasError( reporter, relationship, E4000, "Relationship: `" + uid + "` cannot link to itself" );
    }

    @Test
    void shouldFailWhenRelationshipLinksProgramsAndRelationshipTypeIsNotSet()
    {
        String uid = CodeGenerator.generateUid();
        Relationship relationship = createProgramRelationship( "first enrollment", uid );
        when( bundle.getRelationships() ).thenReturn( List.of( relationship ) );
        when( preheat.getRelationshipType( relationship.getRelationshipType() ) )
            .thenReturn( createTrackedEntityRelationship() );
        when( bundle.getPreheat() ).thenReturn( preheat );

        validator.validate( reporter, bundle, bundle );

        assertEquals( 2, reporter.getErrors().size(),
            "Expected two errors when relationship type is not set, but " + reporter.getErrors().size() + " found" );
        assertHasError( reporter, relationship, E4012,
            "Could not find `enrollment`: `first enrollment`, linked to Relationship." );
    }

    private Relationship createProgramRelationship( String enrollment, String uid )
    {
        Relationship relationship = new Relationship();
        relationship.setRelationship( uid );
        relationship.setFrom( new RelationshipItem( "", enrollment, "" ) );
        relationship.setTo( new RelationshipItem( "", "enrollment", "" ) );
        relationship.setRelationshipType( MetadataIdentifier.ofUid( "uid" ) );

        return relationship;
    }

    private RelationshipType createTrackedEntityRelationship()
    {
        RelationshipConstraint fromConstraint = new RelationshipConstraint();
        fromConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setFromConstraint( fromConstraint );
        relationshipType.setToConstraint( fromConstraint );

        return relationshipType;
    }
}
