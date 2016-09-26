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

import java.util.Collection;
import java.util.List;

/**
 * Defines methods for working with OrganisationUnitGroups and
 * OrganisationUnitGroupSets.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: OrganisationUnitGroupService.java 3286 2007-05-07 18:05:21Z larshelg $
 */
public interface OrganisationUnitGroupService
{
    String ID = OrganisationUnitGroupService.class.getName();

    // -------------------------------------------------------------------------
    // OrganisationUnitGroup
    // -------------------------------------------------------------------------

    /**
     * Adds an OrganisationUnitGroup.
     * 
     * @param organisationUnitGroup the OrganisationUnitGroup to add.
     * @return a generated unique id of the added OrganisationUnitGroup.
     */
    int addOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup );

    /**
     * Updates an OrganisationUnitGroup.
     * 
     * @param organisationUnitGroup the OrganisationUnitGroup to update.
     */
    void updateOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup );

    /**
     * Deletes an OrganisationUnitGroup.
     * 
     * @param organisationUnitGroup the OrganisationUnitGroup to delete.
     */
    void deleteOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup );

    /**
     * Returns an OrganisationUnitGroup.
     * 
     * @param id the id of the OrganisationUnitGroup.
     * @return the OrganisationGroup with the given id, or null if no match.
     */
    OrganisationUnitGroup getOrganisationUnitGroup( int id );

    /**
     * Returns the OrganisationUnitGroups with the given UIDs.
     * 
     * @param uids the collection of UIDs.
     * @return the OrganisationUnitGroups with the given UIDs.
     */
    List<OrganisationUnitGroup> getOrganisationUnitGroupsByUid( Collection<String> uids );
    
    /**
     * Returns the OrganisationUnitGroup with the given UID.
     * 
     * @param uid the UID of the OrganisationUnitGroup.
     * @return the OrganisationGroup with the given UID, or null if no match.
     */
    OrganisationUnitGroup getOrganisationUnitGroup( String uid );

    /**
     * Returns an OrganisationUnitGroup with a given name.
     * 
     *
     * @param name the name of the OrganisationUnitGroup.
     * @return the OrganisationUnitGroup with the given name, or null if no
     *         match.
     */
    List<OrganisationUnitGroup> getOrganisationUnitGroupByName( String name );

    /**
     * Returns the OrganisationUnitGroup with the given code.
     *
     * @param code the code.
     * @return the OrganisationUnitGroup with the given code, or null if no match.
     */
    OrganisationUnitGroup getOrganisationUnitGroupByCode( String code );

    /**
     * Returns the OrganisationUnitGroup with the given short name.
     *
     * @param short name the short name.
     * @return the OrganisationUnitGroup with the given short name, or null if no match.
     */
    OrganisationUnitGroup getOrganisationUnitGroupByShortName( String shortName );

    
    /**
     * Returns all OrganisationUnitGroups.
     * 
     * @return a list of all the OrganisationUnitGroups, or an empty
     *         list if no OrganisationUnitGroup exists.
     */
    List<OrganisationUnitGroup> getAllOrganisationUnitGroups();
    
    /**
     * Returns all OrganisationUnitGroups which have a OrganisationUnitGroupSet.
     * 
     * @return a collection of OrganisationUnitGroups.
     */
    List<OrganisationUnitGroup> getOrganisationUnitGroupsWithGroupSets();

    // -------------------------------------------------------------------------
    // OrganisationUnitGroupSet
    // -------------------------------------------------------------------------

    /**
     * Adds an OrganisationUnitGroupSet.
     * 
     * @param organisationUnitGroupSet the OrganisationUnitGroupSet to add.
     * @return the generated unique id of the added OrganisationUnitGroupSet.
     */
    int addOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet );

    /**
     * Updates an OrganisationUnitGroupSet.
     * 
     * @param organisationUnitGroupSet the OrganisationUnitGroupSet to update.
     */
    void updateOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet );

    /**
     * Deletes an OrganisationUnitGroupSet.
     * 
     * @param organisationUnitGroupSet the OrganisationUnitGroupSet to delete.
     */
    void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet );

    /**
     * Returns an OrganisationUnitGroupSet.
     * 
     * @param id the id of the OrganisationUnitGroupSet to return.
     * @return the OrganisationUnitGroupSet with the given id, or null if no
     *         match.
     */
    OrganisationUnitGroupSet getOrganisationUnitGroupSet( int id );

    /**
     * Returns an OrganisationUnitGroupSet.
     * 
     * @param uid the id of the OrganisationUnitGroupSet to return.
     * @return the OrganisationUnitGroupSet with the given uid, or null if no
     *         match.
     */
    OrganisationUnitGroupSet getOrganisationUnitGroupSet( String uid );

    /**
     * Returns the OrganisationUnitGroupSets with the given uids.
     * 
     * @param uids the collection of uids.
     * @return the OrganisationUnitGroupSets with the given uids.
     */
    List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsByUid( Collection<String> uids );
    
    /**
     * Returns an OrganisationUnitGroupSet with a given name.
     * 
     *
     * @param name the name of the OrganisationUnitGroupSet to return.
     * @return the OrganisationUnitGroupSet with the given name, or null if no
     *         match.
     */
    List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetByName( String name );

    /**
     * Returns all OrganisationUnitGroupSets.
     * 
     * @return a list of all OrganisationUnitGroupSets, or an empty
     *         collection if no OrganisationUnitGroupSet exists.
     */
    List<OrganisationUnitGroupSet> getAllOrganisationUnitGroupSets();

    /**
     * Returns all compulsory OrganisationUnitGroupSets.
     * 
     * @return a list of all compulsory OrganisationUnitGroupSets, or an
     *         empty collection if there are no compulsory
     *         OrganisationUnitGroupSets.
     */
    List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSets();

    /**
     * Returns all compulsory OrganisationUnitGroupSets which have one ore more
     * members.
     * 
     * @return a list of all OrganisationUnitGroupSets, or an
     *         empty collection if there are no compulsory
     *         OrganisationUnitGroupSets.
     */
    List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSetsWithMembers();

    /**
     * Returns a Collection of compulsory OrganisationUnitGroupSets which groups
     * the given OrganisationUnit is not a member of.
     * 
     * @param organisationUnit the OrganisationUnit.
     * @return a Collection of OrganisationUnitGroupSets.
     */
    List<OrganisationUnitGroupSet> getCompulsoryOrganisationUnitGroupSetsNotAssignedTo( OrganisationUnit organisationUnit );

    List<OrganisationUnitGroup> getOrganisationUnitGroupsBetween( int first, int max );
    
    List<OrganisationUnitGroup> getOrganisationUnitGroupsBetweenByName( String name, int first, int max );
    
    int getOrganisationUnitGroupCount();
    
    int getOrganisationUnitGroupCountByName( String name );

    List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsBetween( int first, int max );
    
    List<OrganisationUnitGroupSet> getOrganisationUnitGroupSetsBetweenByName( String name, int first, int max );
    
    int getOrganisationUnitGroupSetCount();
    
    int getOrganisationUnitGroupSetCountByName( String name );

    void mergeWithCurrentUserOrganisationUnits( OrganisationUnitGroup organisationUnitGroup, Collection<OrganisationUnit> mergeOrganisationUnits );
}
