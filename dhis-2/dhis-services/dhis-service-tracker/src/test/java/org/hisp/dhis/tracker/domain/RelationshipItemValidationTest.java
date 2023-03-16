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
package org.hisp.dhis.tracker.domain;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.validation.Error;
import org.hisp.dhis.tracker.validation.Validation;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.Test;

class RelationshipItemValidationTest
{

    @Test
    void shouldPassGivenItemHasEntitySetAccordingToConstraint()
    {
        RelationshipItem item = new RelationshipItem( "", "b", "" );

        Optional<Validation> validation = validate( item, RelationshipEntity.PROGRAM_INSTANCE );

        assertFalse( validation.isPresent() );
    }

    @Test
    void shouldFailIfGivenItemDoesNotHaveAnyEntitySet()
    {
        RelationshipItem item = new RelationshipItem( "", "", "" );

        Optional<Validation> validation = validate( item, RelationshipEntity.PROGRAM_INSTANCE );

        assertTrue( validation.isPresent() );
        Validation actual = validation.get();
        assertContains( "requires only enrollment to be set", actual.getMessage() );
        assertContains( "but none was found", actual.getMessage() );
    }

    @Test
    void shouldFailIfGivenItemDoesNotHaveEntitySetAccordingToConstraint()
    {
        RelationshipItem item = new RelationshipItem( "a", "", "" );

        Optional<Validation> validation = validate( item, RelationshipEntity.PROGRAM_INSTANCE );

        assertTrue( validation.isPresent() );
        Validation actual = validation.get();
        assertContains( "requires only enrollment to be set", actual.getMessage() );
        assertContains( "but trackedEntity was found", actual.getMessage() );
    }

    @Test
    void shouldFailIfMoreThenOneEntityIsSet()
    {
        RelationshipItem item = new RelationshipItem( "a", "b", "c" );

        Optional<Validation> validation = validate( item, RelationshipEntity.PROGRAM_INSTANCE );

        assertTrue( validation.isPresent() );
        Validation actual = validation.get();
        assertContains( "trackedEntity", actual.getMessage() );
        assertContains( "enrollment", actual.getMessage() );
        assertContains( "event", actual.getMessage() );
    }

    private static Optional<Validation> validate( RelationshipItem item, RelationshipEntity entity )
    {
        // Populate a map with an entry for each of the fields only if the field contains a value
        // This would be super easy if we were validating the JSON, so if we were operating on a map
        // maybe there is a more compact way of writing this. I just quickly tried to create a map of field names to
        // getters using lambdas but failed. At least this is super obvious and everyone will understand.
        Map<String, String> fields = new HashMap<>();
        if ( StringUtils.isNotBlank( item.getTrackedEntity() ) )
        {
            fields.put( "trackedEntity", item.getTrackedEntity() );
        }
        if ( StringUtils.isNotBlank( item.getEnrollment() ) )
        {
            fields.put( "enrollment", item.getEnrollment() );
        }
        if ( StringUtils.isNotBlank( item.getEvent() ) )
        {
            fields.put( "event", item.getEvent() );
        }

        // NOTE: I assume that the field name and the RelationshipEntity.name are the same.
        // I had to change RelationshipEntity.TRACKED_ENTITY_INSTANCE. If we cannot, we would need to translate between the two
        // as the string literal field names here are also useful for the error message to let the user know which fields were set
        if ( !fields.containsKey( entity.getName() ) || fields.size() > 1 )
        {
            String fieldNames = !fields.isEmpty() ? String.join( ", ", fields.keySet() ) : "none";
            return Optional.of( new Error( MessageFormat.format(
                "Relationship Type `some type` constraint requires only {0} to be set but {1} was found.",
                entity.getName(), fieldNames ), ValidationCode.E4010, TrackerType.RELATIONSHIP, "uid" ) );
        }

        return Optional.empty();
    }
}