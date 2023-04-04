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
package org.hisp.dhis.trackedentity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
public interface TrackedEntityAttributeService
{
    String ID = TrackedEntityAttributeService.class.getName();

    /**
     * The max length of a value. This is also naturally constrained by the
     * database table, due to the data type: varchar(1200).
     */
    int TEA_VALUE_MAX_LENGTH = 1200;

    /**
     * Adds an {@link TrackedEntityAttribute}
     *
     * @param attribute The to TrackedEntityAttribute add.
     * @return A generated unique id of the added {@link TrackedEntityAttribute}
     *         .
     */
    long addTrackedEntityAttribute( TrackedEntityAttribute attribute );

    /**
     * Deletes a {@link TrackedEntityAttribute}.
     *
     * @param attribute the TrackedEntityAttribute to delete.
     */
    void deleteTrackedEntityAttribute( TrackedEntityAttribute attribute );

    /**
     * Updates an {@link TrackedEntityAttribute}.
     *
     * @param attribute the TrackedEntityAttribute to update.
     */
    void updateTrackedEntityAttribute( TrackedEntityAttribute attribute );

    /**
     * Returns a {@link TrackedEntityAttribute}.
     *
     * @param id the id of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given id
     */
    TrackedEntityAttribute getTrackedEntityAttribute( long id );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntityAttribute with the given UID, or null if no
     *         match.
     */
    TrackedEntityAttribute getTrackedEntityAttribute( String uid );

    /**
     * Returns the {@link TrackedEntityAttribute}s with the given UIDs.
     *
     * @param uids list of UIDs.
     * @return all the TrackedEntityAttribute with the given UIDs.
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributes( @Nonnull List<String> uids );

    /**
     * Returns the {@link TrackedEntityAttribute}s with the given UIDs.
     *
     * @param ids list of primary key ids.
     * @return all the TrackedEntityAttribute with the given ids.
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesById( List<Long> ids );

    /**
     * Returns a {@link TrackedEntityAttribute} with a given name.
     *
     * @param name the name of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given name, or null if no
     *         match.
     */
    TrackedEntityAttribute getTrackedEntityAttributeByName( String name );

    /**
     * Returns all {@link TrackedEntityAttribute}
     *
     * @return a list of all TrackedEntityAttribute, or an empty List if there
     *         are no TrackedEntityAttributes.
     */
    List<TrackedEntityAttribute> getAllTrackedEntityAttributes();

    Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes( User user );

    Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes( User user, List<Program> programs,
        List<TrackedEntityType> trackedEntityTypes );

    ProgramTrackedEntityAttribute getProgramTrackedEntityAttribute( Program program,
        TrackedEntityAttribute trackedEntityAttribute );

    /**
     * Returns all {@link TrackedEntityAttribute} that are candidates for
     * creating trigram indexes.
     *
     * @return a set of all TrackedEntityAttribute, or an empty List if there
     *         are no TrackedEntityAttributes that are indexable
     */
    Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes();

    /**
     * Returns all {@link TrackedEntityAttribute}
     *
     * @return a List of all system wide uniqe TrackedEntityAttribute, or an
     *         empty List if there are no TrackedEntityAttributes.
     */
    List<TrackedEntityAttribute> getAllSystemWideUniqueTrackedEntityAttributes();

    /**
     * Get attributes which are displayed in visit schedule
     *
     * @param displayOnVisitSchedule True/False value
     * @return a list of attributes
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
        boolean displayOnVisitSchedule );

    /**
     * Get attributes which are displayed in visit schedule
     *
     * @return a list of attributes
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesDisplayInListNoProgram();

    /**
     * Get all attributes that user is allowed to read (through program and
     * tracked entity type)
     *
     * @return a list of attributes
     */
    Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes();

    /**
     * Validate uniqueness of the tracked entity attribute value within its
     * scope. Will return non-empty error message if attribute is non-unique.
     *
     * @param trackedEntityAttribute TrackedEntityAttribute
     * @param value Value
     * @param trackedEntityInstance TrackedEntityInstance - required if updating
     *        TEI
     * @param organisationUnit OrganisationUnit - only required if org unit
     *        scoped
     * @return null if valid, a message if not
     */
    String validateAttributeUniquenessWithinScope( TrackedEntityAttribute trackedEntityAttribute,
        String value, TrackedEntityInstance trackedEntityInstance, OrganisationUnit organisationUnit );

    /**
     * Validate value against tracked entity attribute value type.
     *
     * @param trackedEntityAttribute TrackedEntityAttribute
     * @param value Value
     * @return null if valid, a message if not
     */
    String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value );

    @Transactional( readOnly = true )
    List<TrackedEntityAttribute> getAllUniqueTrackedEntityAttributes();

    /**
     * Get all {@link TrackedEntityAttribute} linked to all
     * {@link TrackedEntityType} present in the system
     *
     * @return a Set of {@link TrackedEntityAttribute}
     */
    Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes();

    /**
     * Get all {@link TrackedEntityAttribute} grouped by {@link Program}
     *
     * @return a Map, where the key is the {@link Program} and the values is a
     *         Set of {@link TrackedEntityAttribute} associated to the
     *         {@link Program} in the key
     */
    Map<Program, Set<TrackedEntityAttribute>> getTrackedEntityAttributesByProgram();
}
