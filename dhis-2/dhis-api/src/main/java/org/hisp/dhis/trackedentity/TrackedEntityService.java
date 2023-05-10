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
 * This interface is responsible for retrieving tracked entities (TEI). The
 * query methods accepts a TrackedEntityQueryParams object which encapsulates
 * all arguments.
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
 * <li>0: Tracked entity UID</li>
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
 * TrackedEntityQueryParams params = new TrackedEntityQueryParams();
 *
 * params.addAttribute( new QueryItem( gender, QueryOperator.EQ, "Male", false ) );
 * params.addAttribute( new QueryItem( age, QueryOperator.LT, "5", true ) );
 * params.addFilter( new QueryItem( weight, QueryOperator.GT, "2500", true ) );
 * params.addOrganistionUnit( unit );
 *
 * Grid instances = teiService.getTrackedEntityGrid( params );
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
public interface TrackedEntityService
{
    String ID = TrackedEntityService.class.getName();

    int ERROR_NONE = 0;

    int ERROR_DUPLICATE_IDENTIFIER = 1;

    int ERROR_ENROLLMENT = 2;

    String SEPARATOR = "_";

    /**
     * Returns a grid with tracked entity values based on the given
     * TrackedEntityQueryParams.
     *
     * @param params the TrackedEntityQueryParams.
     * @return a grid.
     */
    Grid getTrackedEntitiesGrid( TrackedEntityQueryParams params );

    /**
     * Returns a list with tracked entity values based on the given
     * TrackedEntityQueryParams.
     *
     * @param params the TrackedEntityQueryParams.
     * @param skipAccessValidation If true, access validation is skipped. Should
     *        be set to true only for internal tasks (e.g. currently used by
     *        synchronization job)
     * @param skipSearchScopeValidation if true, search scope validation is
     *        skipped.
     * @return List of TEIs matching the params
     */
    List<TrackedEntity> getTrackedEntities( TrackedEntityQueryParams params,
        boolean skipAccessValidation, boolean skipSearchScopeValidation );

    /**
     * Returns a list tracked entity primary key ids based on the given
     * TrackedEntityQueryParams.
     *
     * @param params the TrackedEntityQueryParams.
     * @param skipAccessValidation If true, access validation is skipped. Should
     *        be set to true only for internal tasks (e.g. currently used by
     *        synchronization job)
     * @param skipSearchScopeValidation if true, search scope validation is
     *        skipped.
     * @return List of TEI IDs matching the params
     */
    List<Long> getTrackedEntityIds( TrackedEntityQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation );

    /**
     * Return the count of the Tracked entity that meet the criteria specified
     * in params
     *
     * @param params Parameteres that specify searching criteria
     * @param skipAccessValidation If true, the access validation is skipped
     * @param skipSearchScopeValidation If true, the search scope validation is
     *        skipped
     * @return the count of the tracked entities that meet the criteria
     *         specified in params
     */
    int getTrackedEntityCount( TrackedEntityQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation );

    /**
     * Decides whether current user is authorized to perform the given query.
     * IllegalQueryException is thrown if not.
     *
     * @param params the TrackedEntityQueryParams.
     */
    void decideAccess( TrackedEntityQueryParams params );

    /**
     * Validates scope of given TrackedEntityQueryParams. The params is
     * considered valid if no exception are thrown and the method returns
     * normally.
     *
     * @param params the TrackedEntityQueryParams.
     * @param isGridSearch specifies whether search is made for a Grid response
     * @throws IllegalQueryException if the given params is invalid.
     */
    void validateSearchScope( TrackedEntityQueryParams params, boolean isGridSearch )
        throws IllegalQueryException;

    /**
     * Validates the given TrackedEntityQueryParams. The params is considered
     * valid if no exception are thrown and the method returns normally.
     *
     * @param params the TrackedEntityQueryParams.
     * @throws IllegalQueryException if the given params is invalid.
     */
    void validate( TrackedEntityQueryParams params )
        throws IllegalQueryException;

    /**
     * Adds an {@link TrackedEntity}
     *
     * @param trackedEntity The to TrackedEntity add.
     * @return A generated unique id of the added {@link TrackedEntity}.
     */
    long addTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Soft deletes a {@link TrackedEntity}.
     *
     * @param trackedEntity the TrackedEntity to delete.
     */
    void deleteTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Updates a {@link TrackedEntity}.
     *
     * @param trackedEntity the TrackedEntity to update.
     */
    void updateTrackedEntity( TrackedEntity trackedEntity );

    void updateTrackedEntity( TrackedEntity trackedEntity, User user );

    /**
     * Updates a last sync timestamp on specified TrackedEntity
     *
     * @param trackedEntityUIDs UIDs of tracked entities where the
     *        lastSynchronized flag should be updated
     * @param lastSynchronized The date of last successful sync
     */
    void updateTrackedEntitySyncTimestamp( List<String> trackedEntityUIDs, Date lastSynchronized );

    void updateTrackedEntityLastUpdated( Set<String> trackedEntityUIDs, Date lastUpdated );

    /**
     * Returns a {@link TrackedEntity}.
     *
     * @param id the id of the TrackedEntity to return.
     * @return the TrackedEntity with the given id
     */
    TrackedEntity getTrackedEntity( long id );

    /**
     * Returns the {@link TrackedEntity} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntity with the given UID, or null if no match.
     */
    TrackedEntity getTrackedEntity( String uid );

    /**
     * Returns the {@link TrackedEntity} with the given UID.
     *
     * @param uid the UID.
     * @param user User
     * @return the TrackedEntity with the given UID, or null if no match.
     */
    TrackedEntity getTrackedEntity( String uid, User user );

    /**
     * Checks for the existence of a TEI by UID. Deleted values are not taken
     * into account.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean trackedEntityExists( String uid );

    /**
     * Checks for the existence of a TEI by UID. Takes into account also the
     * deleted values.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean trackedEntityExistsIncludingDeleted( String uid );

    /**
     * Returns UIDs of existing tracked entities (including deleted) from the
     * provided UIDs
     *
     * @param uids TEI UIDs to check
     * @return Set containing UIDs of existing TEIs (including deleted)
     */
    List<String> getTrackedEntitiesUidsIncludingDeleted( List<String> uids );

    /**
     * Register a new trackedEntity
     *
     * @param trackedEntity TrackedEntity
     * @param attributeValues Set of attribute values
     * @return The error code after registering trackedEntity
     */
    long createTrackedEntity( TrackedEntity trackedEntity, Set<TrackedEntityAttributeValue> attributeValues );

    List<TrackedEntity> getTrackedEntitiesByUid( List<String> uids, User user );

    List<EventContext.TrackedEntityOuInfo> getTrackedEntityOuInfoByUid( List<String> uids, User user );

}
