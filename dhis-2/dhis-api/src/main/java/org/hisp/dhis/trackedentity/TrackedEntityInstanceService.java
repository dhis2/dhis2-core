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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.events.event.EventContext;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;

/**
 * <p>
 * This interface is responsible for retrieving tracked entity instances (TEI).
 * The query methods accepts a TrackedEntityInstanceQueryParams object which
 * encapsulates all arguments.
 * </p>
 * <p/>
 * <p>
 * The TEIs are returned as a Grid object, which is a two-dimensional list with
 * headers. The TEI attribute values are returned in the same order as specified
 * in the arguments. The grid has a set of columns which are always present
 * starting at index 0, followed by attributes specified for the query. All
 * values in the grid are of type String. The order is:
 * </p>
 * <p/>
 * <ul>
 * <li>0: Tracked entity instance UID</li>
 * <li>1: Created time stamp</li>
 * <li>2: Last updated time stamp</li>
 * <li>3: Organisation unit UID</li>
 * <li>4: Tracked entity UID</li>
 * <ul>
 * <p/>
 * <p>
 * Attributes specified in the query follows on the next column indexes. Example
 * usage for retrieving TEIs with two attributes using one attribute as filter:
 * </p>
 *
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
     * @param skipAccessValidation If true, access validation is skipped. Should
     *        be set to true only for internal tasks (e.g. currently used by
     *        synchronization job)
     * @param skipSearchScopeValidation if true, search scope validation is
     *        skipped.
     * @return List of TEIs matching the params
     */
    List<TrackedEntity> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params,
        boolean skipAccessValidation, boolean skipSearchScopeValidation );

    /**
     * Returns a list tracked entity instance primary key ids based on the given
     * TrackedEntityInstanceQueryParams.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     * @param skipAccessValidation If true, access validation is skipped. Should
     *        be set to true only for internal tasks (e.g. currently used by
     *        synchronization job)
     * @param skipSearchScopeValidation if true, search scope validation is
     *        skipped.
     * @return List of TEI IDs matching the params
     */
    List<Long> getTrackedEntityInstanceIds( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation );

    /**
     * Return the count of the Tracked entity instances that meet the criteria
     * specified in params
     *
     * @param params Parameteres that specify searching criteria
     * @param skipAccessValidation If true, the access validation is skipped
     * @param skipSearchScopeValidation If true, the search scope validation is
     *        skipped
     * @return the count of the Tracked entity instances that meet the criteria
     *         specified in params
     */
    int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation );

    /**
     * Decides whether current user is authorized to perform the given query.
     * IllegalQueryException is thrown if not.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     */
    void decideAccess( TrackedEntityInstanceQueryParams params );

    /**
     * Validates scope of given TrackedEntityInstanceQueryParams. The params is
     * considered valid if no exception are thrown and the method returns
     * normally.
     *
     * @param params the TrackedEntityInstanceQueryParams.
     * @param isGridSearch specifies whether search is made for a Grid response
     * @throws IllegalQueryException if the given params is invalid.
     */
    void validateSearchScope( TrackedEntityInstanceQueryParams params, boolean isGridSearch )
        throws IllegalQueryException;

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
     * Adds an {@link TrackedEntity}
     *
     * @param entityInstance The to TrackedEntity add.
     * @return A generated unique id of the added {@link TrackedEntity}.
     */
    long addTrackedEntityInstance( TrackedEntity entityInstance );

    /**
     * Soft deletes a {@link TrackedEntity}.
     *
     * @param entityInstance the TrackedEntity to delete.
     */
    void deleteTrackedEntityInstance( TrackedEntity entityInstance );

    /**
     * Updates a {@link TrackedEntity}.
     *
     * @param entityInstance the TrackedEntity to update.
     */
    void updateTrackedEntityInstance( TrackedEntity entityInstance );

    void updateTrackedEntityInstance( TrackedEntity instance, User user );

    /**
     * Updates a last sync timestamp on specified TrackedEntityInstances
     *
     * @param trackedEntityInstanceUIDs UIDs of Tracked entity instances where
     *        the lastSynchronized flag should be updated
     * @param lastSynchronized The date of last successful sync
     */
    void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs, Date lastSynchronized );

    void updateTrackedEntityInstanceLastUpdated( Set<String> trackedEntityInstanceUIDs, Date lastUpdated );

    /**
     * Returns a {@link TrackedEntity}.
     *
     * @param id the id of the TrackedEntityInstanceAttribute to return.
     * @return the TrackedEntityInstanceAttribute with the given id
     */
    TrackedEntity getTrackedEntityInstance( long id );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntityInstanceAttribute with the given UID, or null if
     *         no match.
     */
    TrackedEntity getTrackedEntityInstance( String uid );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @param user User
     * @return the TrackedEntityInstanceAttribute with the given UID, or null if
     *         no match.
     */
    TrackedEntity getTrackedEntityInstance( String trackedEntityInstance, User user );

    /**
     * Checks for the existence of a TEI by UID. Deleted values are not taken
     * into account.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean trackedEntityInstanceExists( String uid );

    /**
     * Checks for the existence of a TEI by UID. Takes into account also the
     * deleted values.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean trackedEntityInstanceExistsIncludingDeleted( String uid );

    /**
     * Returns UIDs of existing TrackedEntityInstances (including deleted) from
     * the provided UIDs
     *
     * @param uids TEI UIDs to check
     * @return Set containing UIDs of existing TEIs (including deleted)
     */
    List<String> getTrackedEntityInstancesUidsIncludingDeleted( List<String> uids );

    /**
     * Register a new entityInstance
     *
     * @param entityInstance TrackedEntity
     * @param attributeValues Set of attribute values
     * @return The error code after registering entityInstance
     */
    long createTrackedEntityInstance( TrackedEntity entityInstance,
        Set<TrackedEntityAttributeValue> attributeValues );

    List<TrackedEntity> getTrackedEntityInstancesByUid( List<String> uids, User user );

    List<EventContext.TrackedEntityOuInfo> getTrackedEntityOuInfoByUid( List<String> uids, User user );

}
