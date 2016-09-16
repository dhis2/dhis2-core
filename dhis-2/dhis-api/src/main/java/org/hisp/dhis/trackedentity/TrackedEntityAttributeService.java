package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;

import java.util.List;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface TrackedEntityAttributeService
{
    String ID = TrackedEntityAttributeService.class.getName();

    /**
     * Adds an {@link TrackedEntityAttribute}
     *
     * @param attribute The to TrackedEntityAttribute add.
     * @return A generated unique id of the added {@link TrackedEntityAttribute}
     * .
     */
    int addTrackedEntityAttribute( TrackedEntityAttribute attribute );

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
    TrackedEntityAttribute getTrackedEntityAttribute( int id );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntityAttribute with the given UID, or null if no
     * match.
     */
    TrackedEntityAttribute getTrackedEntityAttribute( String uid );

    /**
     * Returns a {@link TrackedEntityAttribute} with a given name.
     *
     * @param name the name of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given name, or null if no
     * match.
     */
    TrackedEntityAttribute getTrackedEntityAttributeByName( String name );

    /**
     * Returns a {@link TrackedEntityAttribute} with a given short name.
     *
     * @param name the short name of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given short name, or null if no
     * match.
     */
    TrackedEntityAttribute getTrackedEntityAttributeByShortName( String name );

    /**
     * Returns a {@link TrackedEntityAttribute} with a given code.
     *
     * @param code The code of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given code, or null if no match.
     */
    TrackedEntityAttribute getTrackedEntityAttributeByCode( String code );

    /**
     * Returns all {@link TrackedEntityAttribute}
     *
     * @return a List of all TrackedEntityAttribute, or an empty
     * List if there are no TrackedEntityAttributes.
     */
    List<TrackedEntityAttribute> getAllTrackedEntityAttributes();

    /**
     * Get attributes without groups
     *
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getOptionalAttributesWithoutGroup();

    /**
     * Get attributes without groups
     *
     * @return List of attributes without group
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesWithoutGroup();

    /**
     * Get attributes which are displayed in visit schedule
     *
     * @param displayOnVisitSchedule True/False value
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
        boolean displayOnVisitSchedule );

    /**
     * Get attributes which are displayed in visit schedule
     *
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesWithoutProgram();

    /**
     * Get attributes which are displayed in visit schedule
     *
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getTrackedEntityAttributesDisplayInList();

    /**
     * Validate scope of tracked entity attribute. Will return true if attribute is non-unique.
     *
     * @param trackedEntityAttribute TrackedEntityAttribute
     * @param value                  Value
     * @param trackedEntityInstance  TrackedEntityInstance - required if updating TEI
     * @param organisationUnit       OrganisationUnit - only required if org unit scoped
     * @param program                Program - only required if program scoped
     * @return null if valid, a message if not
     */
    String validateScope( TrackedEntityAttribute trackedEntityAttribute,
        String value, TrackedEntityInstance trackedEntityInstance, OrganisationUnit organisationUnit, Program program );

    /**
     * Validate value against tracked entity attribute value type.
     *
     * @param trackedEntityAttribute TrackedEntityAttribute
     * @param value                  Value
     * @return null if valid, a message if not
     */
    String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value );

    /**
     * Gets or adds a program tracked entity attribute for the given program and 
     * attribute.
     * 
     * @param programUid the program identifier.
     * @param attributeUid the tracked entity attribute identifier.
     * @return a program tracked entity attribute.
     */
    ProgramTrackedEntityAttribute getOrAddProgramTrackedEntityAttribute( String programUid, String attributeUid );
    
    /**
     * Returns a program tracked entity attribute. The program tracked entity
     * attribute itself will be transient and the associated program and tracked
     * entity attribute will be persistent.
     * 
     * @param programUid the program identifier.
     * @param attributeUid the tracked entity attribute identifier.
     * @return a tracked entity attribute.
     */
    ProgramTrackedEntityAttribute getProgramTrackedEntityAttribute( String programUid, String attributeUid );
}
