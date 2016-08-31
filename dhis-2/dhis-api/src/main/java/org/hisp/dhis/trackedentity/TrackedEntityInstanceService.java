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

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.validation.ValidationCriteria;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * <p>This interface is responsible for retrieving tracked entity instances (TEI).
 * The query methods accepts a TrackedEntityInstanceQueryParams object which
 * encapsulates all arguments.</p>
 * <p/>
 * <p>The TEIs are returned as a Grid object, which is a two-dimensional list with
 * headers. The TEI attribute values are returned in the same order as specified
 * in the arguments. The grid has a set of columns which are always present
 * starting at index 0, followed by attributes specified for the query. All
 * values in the grid are of type String. The order is:</p>
 * <p/>
 * <ul>
 * <li>0: Tracked entity instance UID</li>
 * <li>1: Created time stamp</li>
 * <li>2: Last updated time stamp</li>
 * <li>3: Organisation unit UID</li>
 * <li>4: Tracked entity UID</li>
 * <ul>
 * <p/>
 * <p>Attributes specified in the query follows on the next column indexes.
 * Example usage for retrieving TEIs with two attributes using one attribute as
 * filter:</p>
 * <p>
 * <pre>
 * <code>
 * TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
 *
 * params.addAttribute( new QueryItem( gender, QueryOperator.EQ, "Male", false ) );
 * params.addAttribute( new QueryItem( age, QueryOperator.LT, "5", true ) );
 * params.addFilter( new QueryItem( weight, QueryOperator.GT, "2500", true ) );
 * params.addOrganistionUnit( unit );
 *
 * Grid instances = teiService.getTrackedEntityInstancesGrid( params );
 *
 * for ( List&lt;Object&gt; row : instances.getRows() )
 * {
 *     String tei = row.get( 0 );
 *     String ou = row.get( 3 );
 *     String gender = row.get( 5 );
 *     String age = row.get( 6 );
 * }
 * </code>
 * </pre>
 *
 * @author Abyot Asalefew Gizaw
 * @author Lars Helge Overland
 */
public interface TrackedEntityInstanceService
{
    String ID = TrackedEntityInstanceService.class.getName();

    int ERROR_NONE = 0;
    int ERROR_DUPLICATE_IDENTIFIER = 1;
    int ERROR_ENROLLMENT = 2;

    String SEPARATOR = "_";

    String F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS = "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS";

    /**
     * Returns a grid with tracked entity instance values based on the given
     * TrackedEntityInstanceQueryParams.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     * @return a grid.
     */
    Grid getTrackedEntityInstancesGrid( TrackedEntityInstanceQueryParams params );

    /**
     * Returns a list with tracked entity instance values based on the given
     * TrackedEntityInstanceQueryParams.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     * @return List of TEIs matching the params
     */
    List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params );

    int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params );

    /**
     * Returns a TrackedEntityInstanceQueryParams based on the given input.
     *
     * @param query                             the query string.
     * @param attribute                         the set of attributes.
     * @param filter                            the set of filters.
     * @param ou                                the set of organisation unit identifiers.
     * @param ouMode                            the OrganisationUnitSelectionMode.
     * @param program                           the Program identifier.
     * @param programStatus                     the ProgramStatus in the given program.
     * @param followUp                          indicates follow up status in the given Program.
     * @param programEnrollmentStartDate        the start date for enrollment in the given
     *                                          Program.
     * @param programEnrollmentEndDate          the end date for enrollment in the given Program.
     * @param programIncidentStartDate          the start date for incident in the given Program.
     * @param programIncidentEndDate            the end date for enrollment in the given Program.
     * @param trackedEntity                     the TrackedEntity uid.
     * @param eventStatus                       the event status for the given Program.
     * @param eventStartDate                    the event start date for the given Program.
     * @param eventEndDate                      the event end date for the given Program.
     * @param skipMeta                          indicates whether to include meta data in the response.
     * @param page                              the page number.
     * @param pageSize                          the page size.
     * @param totalPages                        indicates whether to include the total number of pages.
     * @param skipPaging                        whether to skip paging.
     * @return a TrackedEntityInstanceQueryParams.
     */
    TrackedEntityInstanceQueryParams getFromUrl( String query, Set<String> attribute, Set<String> filter,
        Set<String> ou, OrganisationUnitSelectionMode ouMode, String program, ProgramStatus programStatus,
        Boolean followUp, Date programEnrollmentStartDate, Date programEnrollmentEndDate, Date programIncidentStartDate, Date programIncidentEndDate, String trackedEntity, EventStatus eventStatus,
        Date eventStartDate, Date eventEndDate, boolean skipMeta, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging );

    /**
     * Decides whether current user is authorized to perform the given query.
     * IllegalQueryException is thrown if not.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     */
    void decideAccess( TrackedEntityInstanceQueryParams params );

    /**
     * Validates the given TrackedEntityInstanceQueryParams. The params is
     * considered valid if no exception are thrown and the method returns
     * normally.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     * @throws IllegalQueryException if the given params is invalid.
     */
    void validate( TrackedEntityInstanceQueryParams params )
        throws IllegalQueryException;

    /**
     * Adds an {@link TrackedEntityInstance}
     *
     * @param entityInstance The to TrackedEntityInstance add.
     * @return A generated unique id of the added {@link TrackedEntityInstance}.
     */
    int addTrackedEntityInstance( TrackedEntityInstance entityInstance );

    /**
     * Deletes a {@link TrackedEntityInstance}.
     *
     * @param entityInstance the TrackedEntityInstance to delete.
     */
    void deleteTrackedEntityInstance( TrackedEntityInstance entityInstance );

    /**
     * Updates a {@link TrackedEntityInstance}.
     *
     * @param entityInstance the TrackedEntityInstance to update.
     */
    void updateTrackedEntityInstance( TrackedEntityInstance entityInstance );

    /**
     * Returns a {@link TrackedEntityInstance}.
     *
     * @param id the id of the TrackedEntityInstanceAttribute to return.
     * @return the TrackedEntityInstanceAttribute with the given id
     */
    TrackedEntityInstance getTrackedEntityInstance( int id );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntityInstanceAttribute with the given UID, or null if
     * no match.
     */
    TrackedEntityInstance getTrackedEntityInstance( String uid );

    /**
     * Checks for the existence of a TEI by UID
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean trackedEntityInstanceExists( String uid );

    /**
     * Register a new entityInstance
     *
     * @param entityInstance     TrackedEntityInstance
     * @param representativeId   The uid of entityInstance who is representative
     * @param relationshipTypeId The id of relationship type defined
     * @param attributeValues    Set of attribute values
     * @return The error code after registering entityInstance
     */
    int createTrackedEntityInstance( TrackedEntityInstance entityInstance, String representativeId,
        Integer relationshipTypeId, Set<TrackedEntityAttributeValue> attributeValues );

    /**
     * Update information of an entityInstance existed
     *
     * @param entityInstance     TrackedEntityInstance
     * @param representativeId   The id of representative of this entityInstance
     * @param relationshipTypeId The id of relationship type of this person
     * @param valuesForSave      The entityInstance attribute values for adding
     * @param valuesForUpdate    The entityInstance attribute values for updating
     * @param valuesForDelete    The entityInstance attribute values for deleting
     */
    void updateTrackedEntityInstance( TrackedEntityInstance entityInstance, String representativeId,
        Integer relationshipTypeId, List<TrackedEntityAttributeValue> valuesForSave,
        List<TrackedEntityAttributeValue> valuesForUpdate, Collection<TrackedEntityAttributeValue> valuesForDelete );

    /**
     * Validate entityInstance attributes and validation criteria by program
     * before registering or updating information
     *
     * @param entityInstance TrackedEntityInstance object
     * @param program        Program which person needs to enroll. If this parameter is
     *                       null, the system check unique attribute values of the
     *                       entityInstance
     * @return Error code 0 : Validation is OK 1_<duplicate-value> : The
     * attribute value is duplicated 2_<validation-criteria-id> :
     * Violate validation criteria of the program
     */
    String validateTrackedEntityInstance( TrackedEntityInstance entityInstance, Program program );

    /**
     * Validate tracked entity instance enrollment
     *
     * @param entityInstance TrackedEntityInstance object
     * @param program        Program which person needs to enroll. If this parameter is
     *                       null, the system check identifiers of the tracked entity instance
     * @return ValidationCriteria object which is violated
     */
    ValidationCriteria validateEnrollment( TrackedEntityInstance entityInstance, Program program );
}
