package org.hisp.dhis.dataapproval;

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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.user.User;

/**
 * @author Jim Grace
 */
public interface DataApprovalLevelService
{
    String ID = DataApprovalLevelService.class.getName();

    /**
     * Constant that can be used in place of approval level
     * for data that has not been approved at any level.
     */
    int APPROVAL_LEVEL_UNAPPROVED = 999;
    
    /**
     * Constant representing the highest possible level of approval.
     */
    int APPROVAL_LEVEL_HIGHEST = 0;

    /**
     * Gets the data approval level with the given id.
     *
     * @param id the id.
     * @return a data approval level.
     */
    DataApprovalLevel getDataApprovalLevel( int id );

    /**
     * Gets the data approval level with the given uid.
     *
     * @param uid the uid.
     * @return a data approval level.
     */
    DataApprovalLevel getDataApprovalLevel( String uid );

    /**
     * Gets the data approval level with the given name.
     *
     * @param name the name.
     * @return a data approval level.
     */
    DataApprovalLevel getDataApprovalLevelByName( String name );

    /**
     * Gets the data approval level with the given level number.
     *
     * @param levelNumber number of the level to return.
     * @return a data approval level.
     */
    DataApprovalLevel getDataApprovalLevelByLevelNumber( int levelNumber );

    /**
     * Gets the highest approval at which the current user may approve the
     * organisation unit.
     *
     * @param orgUnit organisation unit to look for.
     * @return a data approval level, or null if not found.
     */
    DataApprovalLevel getHighestDataApprovalLevel( OrganisationUnit orgUnit );

    /**
     * Gets the lowest approval level for a given organisation unit and
     * (optionally) a set of attribute options. Returns the last
     * approval level matching both the orgUnit's level and (optionally)
     * having a category option group set containing one of the category
     * option groups containing one of the options.
     *
     * @param orgUnit organisation unit to look for.
     * @param attributeOptionCombo attribute option combination.
     * @return a data approval level, or null if not found.
     */
    DataApprovalLevel getLowestDataApprovalLevel( OrganisationUnit orgUnit, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Gets a list of all data approval levels, ordered by level in ascending order,
     * i.e. from 1 to n.
     *
     * @return list of all data approval levels, ordered from 1 to n.
     */
    List<DataApprovalLevel> getAllDataApprovalLevels();
    
    /**
     * Gets a mapping of all data approval levels between level number and approval
     * level.
     * 
     * @return map of all data approval levels between level number and approval
     * level.
     */
    Map<Integer, DataApprovalLevel> getDataApprovalLevelMap();

    /**
     * Gets all approval levels to which the user has access.
     * @return all approval levels to which the user has access.
     */
    List<DataApprovalLevel> getUserDataApprovalLevels();

    /**
     * Gets approval levels within a workflow to which the user has access.
     * @param workflow the workflow to look within.
     * @return all user-accessible approval levels within that workflow.
     */
    List<DataApprovalLevel> getUserDataApprovalLevels( DataApprovalWorkflow workflow );

    /**
     * Gets data approval levels by org unit level.
     * 
     * @param orgUnitLevel the org unit level.
     * @return a list of data approval levels.
     */
    List<DataApprovalLevel> getDataApprovalLevelsByOrgUnitLevel( int orgUnitLevel );
    
    /**
     * Retrieves all org unit levels which have approval levels associated.
     * 
     * @return a list of org unit levels.
     */
    Set<OrganisationUnitLevel> getOrganisationUnitApprovalLevels();

    /**
     * Tells whether a level can move down in the list (can switch places with
     * the level below.)
     *
     * @param level the level to test.
     * @return true if the level can move down, otherwise false.
     */
    boolean canDataApprovalLevelMoveDown( int level );

    /**
     * Tells whether a level can move up in the list (can switch places with
     * the level above.)
     *
     * @param level the level to test.
     * @return true if the level can move up, otherwise false.
     */
    boolean canDataApprovalLevelMoveUp( int level );

    /**
     * Moves a data approval level down in the list (switches places with the
     * level below).
     *
     * @param level the level to move down.
     */
    void moveDataApprovalLevelDown( int level );

    /**
     * Moves a data approval level up in the list (switches places with the
     * level above).
     *
     * @param level the level to move up.
     */
    void moveDataApprovalLevelUp( int level );

    /**
     * Determines whether level already exists with the same organisation
     * unit level and category option group set (but not necessarily the
     * same level number.)
     *
     * @param level Data approval level to test for existence.
     * @return true if it exists, otherwise false.
     */
    boolean dataApprovalLevelExists ( DataApprovalLevel level );

    /**
     * Reorders the existing approval levels to prepare insert of the given
     * approval level. Should be followed by saving of the approval level.
     * 
     * @param level the level to add.
     * @return true if the level can be added, false if not.
     */
    boolean prepareAddDataApproval( DataApprovalLevel level );
    
    /**
     * Adds a new data approval level. Adds the new level at the highest
     * position possible (to facilitate the use case where users add the
     * approval levels from low to high.)
     *
     * @param level the new level to add.
     * @return the identifier of the added level, or -1 if not well formed or duplicate.
     */
    int addDataApprovalLevel( DataApprovalLevel level );
    
    /**
     * Adds a new data approval level. Sets the level explicitly.
     * 
     * @param approvalLevel the new level to add.
     * @param level the level.
     * @return the identifier of the added level, or -1 if not well formed or duplicate.
     */
    int addDataApprovalLevel( DataApprovalLevel approvalLevel, int level );

    /**
     * Removes a data approval level.
     *
     * @param dataApprovalLevel the data approval level to delete.
     */
    void deleteDataApprovalLevel( DataApprovalLevel dataApprovalLevel );

    /**
     * Reorders the remaining approval levels, if necessary, after deleting
     * an approval level. Should follow the deleting of an approval level.
     */
    void postDeleteDataApprovalLevel();

    /**
     * Gets the approval level at which this user may make approval actions
     * (if the user is authorized for any) on this organisation unit.
     *
     * @param orgUnit org unit to test
     * @param user user to get approval level from.
     * @param approvalLevels list of data approval levels to choose from.
     * @return approval level
     */
    DataApprovalLevel getUserApprovalLevel( User user, OrganisationUnit orgUnit, List<DataApprovalLevel> approvalLevels );

    /**
     * By organisation unit subhierarchy, returns the lowest data approval
     * level at which the user may see data within that subhierarchy, if
     * data viewing is being restricted to approved data from lower levels.
     * <p>
     * Returns the value APPROVAL_LEVEL_UNAPPROVED for a subhierarchy if
     * the user may see unapproved data.
     * <p>
     * (Note that the "lowest" approval level means the "highest" approval
     * level number.)
     *
     * @return For each organisation unit subhierarchy available to the user,
     *         the minimum data approval level within that subhierarchy.
     */
    Map<OrganisationUnit, Integer> getUserReadApprovalLevels();
    
    /**
     * Gets a map of organisation units by the given approval level. The organisation
     * units are the data view organisation units of the current user, or if user
     * has no data view organisation units then the hierarchy root organisation units.
     * 
     * @param approvalLevel the approval level.
     * @return a mapping of organisation units and approval levels.
     */
    Map<OrganisationUnit, Integer> getUserReadApprovalLevels( DataApprovalLevel approvalLevel );
}
