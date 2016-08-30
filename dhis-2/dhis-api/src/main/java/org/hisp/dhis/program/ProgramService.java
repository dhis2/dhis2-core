package org.hisp.dhis.program;

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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew
 */
public interface ProgramService
{
    String ID = ProgramService.class.getName();

    Pattern INPUT_PATTERN = Pattern.compile( "(<input.*?/>)", Pattern.DOTALL );
    Pattern DYNAMIC_ATTRIBUTE_PATTERN = Pattern.compile( "attributeid=\"(\\w+)\"" );
    Pattern PROGRAM_PATTERN = Pattern.compile( "programid=\"(\\w+)\"" );
    Pattern VALUE_TAG_PATTERN = Pattern.compile( "value=\"(.*?)\"", Pattern.DOTALL );
    Pattern TITLE_TAG_PATTERN = Pattern.compile( "title=\"(.*?)\"", Pattern.DOTALL );
    Pattern SUGGESTED_VALUE_PATTERN = Pattern.compile( "suggested=('|\")(\\w*)('|\")" );
    Pattern CLASS_PATTERN = Pattern.compile( "class=('|\")(\\w*)('|\")" );
    Pattern STYLE_PATTERN = Pattern.compile( "style=('|\")([\\w|\\d\\:\\;]+)('|\")" );

    /**
     * Adds an {@link Program}
     *
     * @param program The to Program add.
     * @return A generated unique id of the added {@link Program}.
     */
    int addProgram( Program program );

    /**
     * Updates an {@link Program}.
     *
     * @param program the Program to update.
     */
    void updateProgram( Program program );

    /**
     * Deletes a {@link Program}. All {@link ProgramStage},
     * {@link ProgramInstance} and {@link ProgramStageInstance} belong to this
     * program are removed
     *
     * @param program the Program to delete.
     */
    void deleteProgram( Program program );

    /**
     * Returns a {@link Program}.
     *
     * @param id the id of the Program to return.
     * @return the Program with the given id
     */
    Program getProgram( int id );

    /**
     * Returns a {@link Program} with a given name.
     *
     * @param name the name of the Program to return.
     * @return the Program with the given name, or null if no match.
     */
    Program getProgramByName( String name );

    /**
     * Returns all {@link Program}.
     *
     * @return a collection of all Program, or an empty collection if there are
     *         no Programs.
     */
    List<Program> getAllPrograms();

    /**
     * Get all {@link Program} belong to a orgunit
     *
     * @param organisationUnit {@link OrganisationUnit}
     * @return The program list
     */
    List<Program> getPrograms( OrganisationUnit organisationUnit );

    /**
     * Get {@link Program} by a type
     *
     * @param type The type of program. There are three types, include Multi
     *        events with registration, Single event with registration and
     *        Single event without registration
     * @return Program list by a type specified
     */
    List<Program> getPrograms( ProgramType type );

    /**
     * Get {@link Program} assigned to an {@link OrganisationUnit} by a type
     *
     * @param type The type of program. There are three types, include Multi
     *        events with registration, Single event with registration and
     *        Single event without registration
     * @param orgunit Where programs assigned
     * @return Program list by a type specified
     */
    List<Program> getPrograms( ProgramType type, OrganisationUnit orgunit );

    /**
     * Returns the {@link Program} with the given UID.
     *
     * @param uid the UID.
     * @return the Program with the given UID, or null if no match.
     */
    Program getProgram( String uid );

    /**
     * Get {@link TrackedEntity} by TrackedEntity
     *
     * @param trackedEntity {@link TrackedEntity}
     */
    List<Program> getProgramsByTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Get all Programs with the given DataEntryForm.
     *
     * @param dataEntryForm the DataEntryForm.
     * @return a list of Programs.
     */
    List<Program> getProgramsByDataEntryForm( DataEntryForm dataEntryForm );

    /**
     * Returns The number of Programs with the key searched
     *
     * @param name Keyword for searching by name
     * @return A number
     */
    Integer getProgramCountByName( String name );

    /**
     * Returns {@link Program} list with paging
     *
     * @param name Keyword for searching by name
     * @param min First result
     * @param max Maximum results
     * @return a List of all Program, or an empty collection if there are no
     *         Program.
     */
    List<Program> getProgramBetweenByName( String name, int min, int max );

    /**
     * Returns The number of all Program available
     */
    Integer getProgramCount();

    /**
     * Returns {@link Program} list with paging
     *
     * @param min First result
     * @param max Maximum results
     * @return a List of all Program, or an empty List if there are no Program.
     */
    List<Program> getProgramsBetween( int min, int max );

    /**
     * Get {@link Program} by the current user. Returns all programs if current
     * user is superuser. Returns an empty list if there is no current user.
     *
     * @return Immutable set of programs associated with the current user.
     */
    Set<Program> getUserPrograms();

    Set<Program> getUserPrograms( User user );

    /**
     * Get {@link Program} by the current user and a certain type
     *
     * @param programType The type of program. There are three types, include Multi
     *        events with registration, Single event with registration and
     *        Single event without registration.
     * @return Immutable set of programs associated with the current user.
     */
    Set<Program> getUserPrograms( ProgramType programType );

    /**
     * Sets the given merge organisation units on the given programs. Only 
     * the sub-hierarchy of the current user is modified. 
     * 
     * @param program the program.
     * @param mergeOrganisationUnits the merge organisation units.
     */
    void mergeWithCurrentUserOrganisationUnits( Program program, Collection<OrganisationUnit> mergeOrganisationUnits );
    
    /**
     * Gets or adds a program data element for the given program and data element.
     * 
     * @param programUid the program identifier.
     * @param dataElementUid the data element identifier.
     * @return a program data element.
     */
    ProgramDataElement getOrAddProgramDataElement(  String programUid, String dataElementUid  );
    
    /**
     * Creates a program data element based on the program and data element with
     * the given program and data element identifiers.
     * 
     * @param programUid the program identifier.
     * @param dataElementUid the data element identifier.
     * @return a program data element.
     */
    ProgramDataElement getProgramDataElement( String programUid, String dataElementUid );
    
    /**
     * Returns a list of generated, non-persisted program data elements for the
     * program with the given identifier.
     * 
     * @param programUid the program identifier.
     * @return a list of program data elements.
     */
    List<ProgramDataElement> getGeneratedProgramDataElements( String programUid );
}
