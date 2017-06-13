package org.hisp.dhis.organisationunit;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.GenericNameableObjectStore;

/**
 * Defines methods for persisting OrganisationUnits.
 *
 * @author Kristian Nordal
 * @version $Id: OrganisationUnitStore.java 5645 2008-09-04 10:01:02Z larshelg $
 */
public interface OrganisationUnitStore
    extends GenericNameableObjectStore<OrganisationUnit>
{
    String ID = OrganisationUnitStore.class.getName();

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    /**
     * Returns all OrganisationUnits by lastUpdated.
     *
     * @param lastUpdated OrganisationUnits from this date
     * @return a list of all OrganisationUnits, or an empty list if
     *         there are no OrganisationUnits.
     */
    List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated( Date lastUpdated );

    /**
     * Returns all root OrganisationUnits. A root OrganisationUnit is an
     * OrganisationUnit with no parent/has the parent set to null.
     *
     * @return a list containing all root OrganisationUnits, or an empty
     *         list if there are no OrganisationUnits.
     */
    List<OrganisationUnit> getRootOrganisationUnits();

    /**
     * Returns all OrganisationUnits which are not a member of any OrganisationUnitGroups.
     *
     * @return all OrganisationUnits which are not a member of any OrganisationUnitGroups.
     */
    List<OrganisationUnit> getOrganisationUnitsWithoutGroups();
    
    /**
     * Returns the count of OrganisationUnits which are part of the
     * sub-hierarchy of the given parent OrganisationUnit and members of 
     * the given object based on the collection of the given collection name.
     * 
     * @param parent the parent OrganisationUnit.
     * @param member the member object.
     * @param collectionName the name of the collection.
     * @return the count of member OrganisationUnits.
     */
    Long getOrganisationUnitHierarchyMemberCount( OrganisationUnit parent, Object member, String collectionName );

    /**
     * Returns a list of OrganisationUnits based on the given params.
     * 
     * @param params the params.
     * @return a list of OrganisationUnits.
     */
    List<OrganisationUnit> getOrganisationUnits( OrganisationUnitQueryParams params );

    /**
     * Creates a mapping between organisation unit UID and set of data set UIDs
     * being assigned to the organisation unit.
     * 
     * @return a map of sets.
     */
    Map<String, Set<String>> getOrganisationUnitDataSetAssocationMap();

    /**
     * Retrieves the objects where its coordinate is within the 4 area points.
     * 4 area points are
     * Index 0: Maximum latitude (north edge of box shape)
     * Index 1: Maxium longitude (east edge of box shape)
     * Index 2: Minimum latitude (south edge of box shape)
     * Index 3: Minumum longitude (west edge of box shape)
     *
     * @param box      the 4 area points.
     * @return a list of objects.
     */
    List<OrganisationUnit> getWithinCoordinateArea( double[] box );
    
    // -------------------------------------------------------------------------
    // OrganisationUnitHierarchy
    // -------------------------------------------------------------------------

    /**
     * Get the OrganisationUnit hierarchy.
     *
     * @return a  with OrganisationUnitRelationship entries.
     */
    OrganisationUnitHierarchy getOrganisationUnitHierarchy();

    /**
     * Updates the parent id of the organisation unit with the given id.
     *
     * @param organisationUnitId the child organisation unit identifier.
     * @param parentId           the parent organisation unit identifier.
     */
    void updateOrganisationUnitParent( int organisationUnitId, int parentId );

    void updatePaths();

    void forceUpdatePaths();
    
    /**
     * Returns the number of organsiation unit levels in the database based on
     * the organisation unit hierarchy.
     * 
     * @return number of levels, 0 if no organisation units are present.
     */
    int getMaxLevel();
}
