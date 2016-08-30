package org.hisp.dhis.organisationunit;

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

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.hierarchy.HierarchyViolationException;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines methods for working with OrganisationUnits.
 *
 * @author Torgeir Lorange Ostby
 */
public interface OrganisationUnitService
{
    String ID = OrganisationUnitService.class.getName();

    int MAX_LIMIT = 500;

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    /**
     * Adds an OrganisationUnit to the hierarchy.
     *
     * @param organisationUnit the OrganisationUnit to add.
     * @return a generated unique id of the added OrganisationUnit.
     */
    int addOrganisationUnit( OrganisationUnit organisationUnit );

    /**
     * Updates an OrganisationUnit.
     *
     * @param organisationUnit the OrganisationUnit to update.
     */
    void updateOrganisationUnit( OrganisationUnit organisationUnit );

    /**
     * Updates the version of the organisation unit hierarchy.
     */
    void updateOrganisationUnitVersion();

    /**
     * Updates an OrganisationUnit.
     *
     * @param organisationUnit the organisationUnit to update.
     * @param updateHierarchy  indicate whether the OrganisationUnit hierarchy
     *                         has been updated.
     */
    void updateOrganisationUnit( OrganisationUnit organisationUnit, boolean updateHierarchy );

    /**
     * Deletes an OrganisationUnit. OrganisationUnits with children cannot be
     * deleted.
     *
     * @param organisationUnit the OrganisationUnit to delete.
     * @throws HierarchyViolationException if the OrganisationUnit has children.
     */
    void deleteOrganisationUnit( OrganisationUnit organisationUnit )
        throws HierarchyViolationException;

    /**
     * Returns an OrganisationUnit.
     *
     * @param id the id of the OrganisationUnit to return.
     * @return the OrganisationUnit with the given id, or null if no match.
     */
    OrganisationUnit getOrganisationUnit( int id );

    /**
     * Returns the OrganisationUnit with the given UID.
     *
     * @param uid the UID of the OrganisationUnit to return.
     * @return the OrganisationUnit with the given UID, or null if no match.
     */
    OrganisationUnit getOrganisationUnit( String uid );

    /**
     * Returns the OrganisationUnit with the given code.
     *
     * @param code the code of the OrganisationUnit to return.
     * @return the OrganisationUnit with the given code, or null if no match.
     */
    OrganisationUnit getOrganisationUnitByCode( String code );

    /**
     * Returns all OrganisationUnits.
     *
     * @return a list of all OrganisationUnits, or an empty list if
     * there are no OrganisationUnits.
     */
    List<OrganisationUnit> getAllOrganisationUnits();

    /**
     * Returns all OrganisationUnits by lastUpdated.
     *
     * @param lastUpdated OrganisationUnits from this date
     * @return a list of all OrganisationUnits, or an empty list if
     * there are no OrganisationUnits.
     */
    List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated( Date lastUpdated );

    /**
     * Returns all OrganisationUnits with corresponding identifiers.
     *
     * @param identifiers the collection of identifiers.
     * @return a list of OrganisationUnits.
     */
    List<OrganisationUnit> getOrganisationUnits( Collection<Integer> identifiers );

    /**
     * Returns all OrganisationUnits with corresponding identifiers.
     *
     * @param uids the collection of uids.
     * @return a list of OrganisationUnits.
     */
    List<OrganisationUnit> getOrganisationUnitsByUid( Collection<String> uids );
    
    /**
     * Returns a list of OrganisationUnits based on the given params.
     * 
     * @param params the params.
     * @return a list of OrganisationUnits.
     */
    List<OrganisationUnit> getOrganisationUnitsByQuery( OrganisationUnitQueryParams params );

    /**
     * Returns an OrganisationUnit with a given name.
     *
     * @param name the name of the OrganisationUnit to return.
     * @return the OrganisationUnit with the given name, or null if not match.
     */
    List<OrganisationUnit> getOrganisationUnitByName( String name );

    /**
     * Returns an OrganisationUnit with a given name. Case is ignored.
     *
     * @param name the name of the OrganisationUnit to return.
     * @return the OrganisationUnit with the given name, or null if not match.
     */
    List<OrganisationUnit> getOrganisationUnitByNameIgnoreCase( String name );

    /**
     * Returns all root OrganisationUnits. A root OrganisationUnit is an
     * OrganisationUnit with no parent/the parent set to null.
     *
     * @return a list containing all root OrganisationUnits, or an empty
     * list if there are no OrganisationUnits.
     */
    List<OrganisationUnit> getRootOrganisationUnits();

    /**
     * Returns the intersection of the members of the given OrganisationUnitGroups
     * and the OrganisationUnits which are children of the given collection of
     * parents in the hierarchy. If the given parent collection is null or empty,
     * the members of the group are returned.
     *
     * @param groups  the collection of OrganisationUnitGroups.
     * @param parents the collection of OrganisationUnit parents in the hierarchy.
     * @return A list of OrganisationUnits.
     */
    List<OrganisationUnit> getOrganisationUnits( Collection<OrganisationUnitGroup> groups, Collection<OrganisationUnit> parents );

    /**
     * Returns organisation unit UIDs for the given parameters.
     * 
     * @param parents the set of boundary parent organisation units.
     * @param ouMode the organisation unit selection mode.
     * @return a set of strings.
     */
    Set<String> getOrganisationUnitUids( Set<String> parents, OrganisationUnitSelectionMode ouMode );

    /**
     * Returns an OrganisationUnit and all its children.
     *
     * @param uid the uid of the parent OrganisationUnit in the subtree.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitWithChildren( String uid );

    /**
     * Returns an OrganisationUnit and all its children.
     *
     * @param uid       the uid of the parent OrganisationUnit in the subtree.
     * @param maxLevels the max number of levels to return relative to
     *                  the given root, inclusive.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitWithChildren( String uid, Integer maxLevels );

    /**
     * Returns an OrganisationUnit and all its children.
     *
     * @param id the id of the parent OrganisationUnit in the subtree.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitWithChildren( int id );

    /**
     * Returns an OrganisationUnit and all its children.
     *
     * @param id        the id of the parent OrganisationUnit in the subtree.
     * @param maxLevels the max number of levels to return relative to
     *                  the given root, inclusive.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitWithChildren( int id, Integer maxLevels );

    /**
     * Returns the OrganisationUnits and all their children.
     *
     * @param uids the uids of the parent OrganisationUnits.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitsWithChildren( Collection<String> uids );

    /**
     * Returns the OrganisationUnits and all their children.
     *
     * @param uids      the uids of the parent OrganisationUnits.
     * @param maxLevels the max number of levels to return relative to
     *                  the given root, inclusive.
     * @return a list containing the OrganisationUnit with the given id
     * and all its children, or an empty list if no
     * OrganisationUnits match.
     */
    List<OrganisationUnit> getOrganisationUnitsWithChildren( Collection<String> uids, Integer maxLevels );

    /**
     * Returns all OrganisationUnits at a given hierarchical level. The root
     * OrganisationUnits are at level 1.
     *
     * @param level the hierarchical level.
     * @return a list of all OrganisationUnits at a given hierarchical
     * level, or an empty list if the level is empty.
     * @throws IllegalArgumentException if the level is zero or negative.
     */
    List<OrganisationUnit> getOrganisationUnitsAtLevel( int level );

    /**
     * Returns all OrganisationUnits which are children of the given unit and are
     * at the given hierarchical level. The root OrganisationUnits are at level 1.
     * If parent is null, then all OrganisationUnits at the given level are returned.
     *
     * @param level  the hierarchical level.
     * @param parent the parent unit.
     * @return all OrganisationUnits which are children of the given unit and are
     * at the given hierarchical level.
     * @throws IllegalArgumentException if the level is illegal.
     */
    List<OrganisationUnit> getOrganisationUnitsAtLevel( int level, OrganisationUnit parent );

    /**
     * Returns all OrganisationUnits which are children of the given unit and are
     * at the given hierarchical levels. The root OrganisationUnits are at level 1.
     * If parent is null, then all OrganisationUnits at the given level are returned.
     *
     * @param levels the hierarchical levels.
     * @param parent the parent unit.
     * @return all OrganisationUnits which are children of the given unit and are
     * at the given hierarchical level.
     * @throws IllegalArgumentException if the level is illegal.
     */
    List<OrganisationUnit> getOrganisationUnitsAtLevels( Collection<Integer> levels, Collection<OrganisationUnit> parents );

    /**
     * Returns the number of levels in the OrganisationUnit hierarchy.
     *
     * @return the number of hierarchical levels.
     */
    int getNumberOfOrganisationalLevels();

    /**
     * Returns all OrganisationUnits which are not a member of any OrganisationUnitGroups.
     *
     * @return all OrganisationUnits which are not a member of any OrganisationUnitGroups.
     */
    List<OrganisationUnit> getOrganisationUnitsWithoutGroups();
    
    /**
     * Returns all OrganisationUnits with at least one CategoryOption.
     * 
     * @return all OrganisationUnits with at least one CategoryOption.
     */
    List<OrganisationUnit> getOrganisationUnitsWithCategoryOptions();

    OrganisationUnitDataSetAssociationSet getOrganisationUnitDataSetAssociationSet( Integer maxlevels );

    List<OrganisationUnit> getOrganisationUnitsBetween( int first, int max );

    List<OrganisationUnit> getOrganisationUnitsBetweenByName( String name, int first, int max );

    List<OrganisationUnit> getOrganisationUnitsBetweenByLastUpdated( Date lastUpdated, int first, int max );

    /**
     * Retrieves all the org units within the distance from center location.
     *
     * @param longitude The longitude of the center location.
     * @param latitude  The latitude of the center location.
     * @param distance  The distance from center location.
     * @return a list of objects.
     */
    List<OrganisationUnit> getOrganisationUnitWithinDistance( double longitude, double latitude, double distance );

    /**
     * Retrieves the orgunit(s) by coordinate.
     *
     * @param longitude     The longitude of the location.
     * @param latitude      The latitude of the location.
     * @param topOrgUnitUid Optional. Uid of the search top level org unit (ex.
     *                      Country level orgunit)
     * @param targetLevel   Optional. The level being searched.
     * @return list of objects.
     */
    List<OrganisationUnit> getOrganisationUnitByCoordinate( double longitude, double latitude, String topOrgUnitUid,
        Integer targetLevel );

    /**
     * Indicates whether the given organisation unit is part of the hierarchy
     * of the organisation units of the current user.
     *
     * @param organisationUnit the organisation unit.
     * @return true if the given organisation unit is part of the hierarchy.
     */
    boolean isInUserHierarchy( OrganisationUnit organisationUnit );

    /**
     * Indicates whether the given organisation unit is part of the hierarchy
     * of the given user organisation units.
     *
     * @param uid               the uid of the organisation unit.
     * @param organisationUnits the set of organisation units associated with a
     *                          user.
     * @return true if the organisation unit with the given uid is part of the hierarchy.
     */
    boolean isInUserHierarchy( String uid, Set<OrganisationUnit> organisationUnits );
    
    // -------------------------------------------------------------------------
    // OrganisationUnitHierarchy
    // -------------------------------------------------------------------------

    /**
     * Get the OrganisationUnit hierarchy.
     *
     * @return a Collection with OrganisationUnitRelationship entries.
     */
    OrganisationUnitHierarchy getOrganisationUnitHierarchy();

    /**
     * Updates the parent id of the organisation unit with the given id.
     *
     * @param organisationUnitId the child organisation unit identifier.
     * @param parentId           the parent organisation unit identifier.
     */
    void updateOrganisationUnitParent( int organisationUnitId, int parentId );

    // -------------------------------------------------------------------------
    // OrganisationUnitLevel
    // -------------------------------------------------------------------------

    int addOrganisationUnitLevel( OrganisationUnitLevel level );

    void updateOrganisationUnitLevel( OrganisationUnitLevel level );

    void addOrUpdateOrganisationUnitLevel( OrganisationUnitLevel level );

    void pruneOrganisationUnitLevels( Set<Integer> currentLevels );

    OrganisationUnitLevel getOrganisationUnitLevel( int id );

    OrganisationUnitLevel getOrganisationUnitLevel( String uid );

    void deleteOrganisationUnitLevel( OrganisationUnitLevel level );

    void deleteOrganisationUnitLevels();

    List<OrganisationUnitLevel> getOrganisationUnitLevels();

    OrganisationUnitLevel getOrganisationUnitLevelByLevel( int level );

    List<OrganisationUnitLevel> getOrganisationUnitLevelByName( String name );

    List<OrganisationUnitLevel> getFilledOrganisationUnitLevels();

    Map<Integer, OrganisationUnitLevel> getOrganisationUnitLevelMap();

    int getNumberOfOrganisationUnits();

    /**
     * Return the number of organisation unit levels to cache offline, e.g. for
     * organisation unit tree. Looks for level to return in the following order:
     * <p/>
     * <ul>
     * <li>Get level of organisation unit of the current user.</li>
     * <li>Get level from system configuration.</li>
     * <li>Get max level.</li>
     * <li>Return 1 as fall back.</li>
     * </ul>
     */
    int getOfflineOrganisationUnitLevels();

    /**
     * Update all OUs where paths is null.
     */
    void updatePaths();

    /**
     * Update all OUs (thus forcing update of path).
     */
    void forceUpdatePaths();

    // -------------------------------------------------------------------------
    // Version
    // -------------------------------------------------------------------------

    void updateVersion();
}
