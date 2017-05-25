package org.hisp.dhis.dataapproval;

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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jim Grace
 */
@Transactional
public class DefaultDataApprovalLevelService
    implements DataApprovalLevelService
{
    private static final Log log = LogFactory.getLog( DefaultDataApprovalLevelService.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataApprovalLevelStore dataApprovalLevelStore;

    public void setDataApprovalLevelStore( DataApprovalLevelStore dataApprovalLevelStore )
    {
        this.dataApprovalLevelStore = dataApprovalLevelStore;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private SecurityService securityService;

    public void setSecurityService( SecurityService securityService )
    {
        this.securityService = securityService;
    }

    // -------------------------------------------------------------------------
    // DataApprovalLevel
    // -------------------------------------------------------------------------

    @Override
    public DataApprovalLevel getDataApprovalLevel( int id )
    {
        return dataApprovalLevelStore.get( id );
    }

    @Override
    public DataApprovalLevel getDataApprovalLevel( String uid )
    {
        return dataApprovalLevelStore.getByUid( uid );
    }

    @Override
    public DataApprovalLevel getDataApprovalLevelByName( String name )
    {
        return dataApprovalLevelStore.getByName( name );
    }

    @Override
    public DataApprovalLevel getDataApprovalLevelByLevelNumber( int levelNumber )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        if ( levelNumber < 1 || levelNumber > dataApprovalLevels.size() )
        {
            return null;
        }
        else
        {
            return dataApprovalLevels.get( levelNumber - 1 );
        }
    }

    @Override
    public DataApprovalLevel getHighestDataApprovalLevel( OrganisationUnit orgUnit )
    {
        int orgUnitLevel = orgUnit.getLevel();

        DataApprovalLevel levelAbove = null;

        int levelAboveOrgUnitLevel = 0;

        List<DataApprovalLevel> userApprovalLevels = getUserDataApprovalLevels();
        
        for ( DataApprovalLevel level : userApprovalLevels )
        {
            log.debug( "Get highest data approval level: " + level.getName() );

            if ( level.getOrgUnitLevel() == orgUnitLevel )
            {
                return level; // Exact match on org unit level.
            }
            else if ( level.getOrgUnitLevel() > levelAboveOrgUnitLevel )
            {
                levelAbove = level; // Must be first matching approval level for this org unit level.

                levelAboveOrgUnitLevel = level.getOrgUnitLevel();
            }
        }

        return levelAbove; // Closest ancestor above, or null if none.
    }

    @Override
    public DataApprovalLevel getLowestDataApprovalLevel( OrganisationUnit orgUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        Set<CategoryOptionGroupSet> cogSets = null;

        if ( attributeOptionCombo != null && attributeOptionCombo != categoryService.getDefaultDataElementCategoryOptionCombo()  )
        {
            cogSets = new HashSet<>();

            for ( DataElementCategoryOption option : attributeOptionCombo.getCategoryOptions() )
            {
                if ( option.getGroupSets() != null )
                {
                    cogSets.addAll( option.getGroupSets() );
                }
            }
        }

        int orgUnitLevel = orgUnit.getLevel();

        List<DataApprovalLevel> approvalLevels = getDataApprovalLevelsByOrgUnitLevel( orgUnitLevel );
        
        for ( DataApprovalLevel level : Lists.reverse( approvalLevels ) )
        {
            if ( level.getCategoryOptionGroupSet() == null )
            {
                if ( cogSets == null )
                {
                    return level;
                }
            }
            else if ( cogSets != null && cogSets.contains( level.getCategoryOptionGroupSet() ) )
            {
                return level;
            }
        }

        return null;
    }

    @Override
    public List<DataApprovalLevel> getAllDataApprovalLevels()
    {
        List<DataApprovalLevel> dataApprovalLevels = dataApprovalLevelStore.getAllDataApprovalLevels();

        for ( DataApprovalLevel dataApprovalLevel : dataApprovalLevels )
        {
            int ouLevelNumber = dataApprovalLevel.getOrgUnitLevel();

            OrganisationUnitLevel ouLevel = organisationUnitService.getOrganisationUnitLevelByLevel( ouLevelNumber );

            String ouLevelName = ouLevel != null ? ouLevel.getName() : "Organisation unit level " + ouLevelNumber;

            dataApprovalLevel.setOrgUnitLevelName( ouLevelName );
        }

        return dataApprovalLevels;
    }

    @Override
    public Map<Integer, DataApprovalLevel> getDataApprovalLevelMap()
    {
        List<DataApprovalLevel> levels = dataApprovalLevelStore.getAllDataApprovalLevels();
        
        return Maps.uniqueIndex( levels, new Function<DataApprovalLevel, Integer>()
        {
            @Override
            public Integer apply( DataApprovalLevel level )
            {
                return level.getLevel();
            }            
        } );
    }

    @Override
    public List<DataApprovalLevel> getUserDataApprovalLevels()
    {
        return subsetUserDataApprovalLevels( getAllDataApprovalLevels() );
    }

    @Override
    public List<DataApprovalLevel> getUserDataApprovalLevels( DataApprovalWorkflow workflow )
    {
        return subsetUserDataApprovalLevels( workflow.getSortedLevels() );
    }

    @Override
    public List<DataApprovalLevel> getDataApprovalLevelsByOrgUnitLevel( int orgUnitLevel )
    {
        return dataApprovalLevelStore.getDataApprovalLevelsByOrgUnitLevel( orgUnitLevel );
    }

    @Override
    public Set<OrganisationUnitLevel> getOrganisationUnitApprovalLevels()
    {
        Set<OrganisationUnitLevel> orgUnitLevels = new HashSet<>();
        
        List<DataApprovalLevel> dataApprovalLevels = dataApprovalLevelStore.getAllDataApprovalLevels();
        
        for ( DataApprovalLevel level : dataApprovalLevels )
        {
            OrganisationUnitLevel orgUnitLevel = organisationUnitService.getOrganisationUnitLevelByLevel( level.getOrgUnitLevel() );
            
            if ( orgUnitLevel != null )
            {
                orgUnitLevels.add( orgUnitLevel );
            }
        }
        
        return orgUnitLevels;
    }

    @Override
    public boolean canDataApprovalLevelMoveDown( int level )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        int index = level - 1;

        if ( index < 0 || index + 1 >= dataApprovalLevels.size() )
        {
            return false;
        }

        DataApprovalLevel test = dataApprovalLevels.get( index );
        DataApprovalLevel next = dataApprovalLevels.get( index + 1 );

        return test.getOrgUnitLevel() == next.getOrgUnitLevel()
            && test.getCategoryOptionGroupSet() != null;
    }

    @Override
    public boolean canDataApprovalLevelMoveUp( int level )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        int index = level - 1;

        if ( index <= 0 || index >= dataApprovalLevels.size() )
        {
            return false;
        }

        DataApprovalLevel test = dataApprovalLevels.get( index );
        DataApprovalLevel previous = dataApprovalLevels.get( index - 1 );

        return test.getOrgUnitLevel() == previous.getOrgUnitLevel()
            && previous.getCategoryOptionGroupSet() != null;
    }

    @Override
    public void moveDataApprovalLevelDown( int level )
    {
        if ( canDataApprovalLevelMoveDown( level ) )
        {
            swapWithNextLevel( level );
        }
    }

    @Override
    public void moveDataApprovalLevelUp( int level )
    {
        if ( canDataApprovalLevelMoveUp( level ) )
        {
            swapWithNextLevel( level - 1 );
        }
    }

    @Override
    public boolean dataApprovalLevelExists( DataApprovalLevel level )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        for ( DataApprovalLevel dataApprovalLevel : dataApprovalLevels )
        {
            if ( level.levelEquals( dataApprovalLevel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean prepareAddDataApproval( DataApprovalLevel level )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        int index = getInsertIndex( dataApprovalLevels, level );

        if ( index < 0 )
        {
            return false;
        }

        dataApprovalLevels.add( index, level );

        // Move down from end to here, to avoid duplicate level in database.

        for ( int i = dataApprovalLevels.size() - 1; i > index; i-- )
        {
            update( dataApprovalLevels.get( i ), i );
        }

        level.setLevel( index + 1 );
        
        return true;
    }
    
    @Override
    public int addDataApprovalLevel( DataApprovalLevel level )
    {
        if ( !prepareAddDataApproval( level ) )
        {
            return -1;
        }

        dataApprovalLevelStore.save( level );

        return level.getId();
    }

    @Override
    public int addDataApprovalLevel( DataApprovalLevel approvalLevel, int level )
    {
        approvalLevel.setLevel( level );
        
        dataApprovalLevelStore.save( approvalLevel );

        return approvalLevel.getId();
    }
    
    @Override
    public void deleteDataApprovalLevel( DataApprovalLevel dataApprovalLevel )
    {
        dataApprovalLevelStore.delete( dataApprovalLevel );

        postDeleteDataApprovalLevel();
    }

    @Override
    public void postDeleteDataApprovalLevel()
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        for ( int i = 0; i < dataApprovalLevels.size(); i++ )
        {
            if ( dataApprovalLevels.get( i ).getLevel() != i + 1 )
            {
                update( dataApprovalLevels.get( i ), i );
            }
        }
    }

    @Override
    public DataApprovalLevel getUserApprovalLevel( User user, OrganisationUnit orgUnit, List<DataApprovalLevel> approvalLevels )
    {
        if ( user == null || orgUnit == null )
        {
            return null;
        }

        OrganisationUnit organisationUnit = null;
        
        for ( OrganisationUnit unit : user.getOrganisationUnits() )
        {
            if ( orgUnit.isDescendant( unit ) )
            {
                organisationUnit = unit;
                break;
            }
        }

        return organisationUnit != null ? getUserApprovalLevel( organisationUnit, user, approvalLevels ) : null;
    }

    @Override
    public Map<OrganisationUnit, Integer> getUserReadApprovalLevels()
    {
        Map<OrganisationUnit, Integer> map = new HashMap<>();

        User user = currentUserService.getCurrentUser();

        List<DataApprovalLevel> approvalLevels = getAllDataApprovalLevels();
        
        // ---------------------------------------------------------------------
        // Add user organisation units if authorized to approve at lower levels
        // ---------------------------------------------------------------------

        if ( user.getUserCredentials().isAuthorized( DataApproval.AUTH_APPROVE_LOWER_LEVELS ) )
        {
            for ( OrganisationUnit orgUnit : user.getOrganisationUnits() )
            {
                map.put( orgUnit, APPROVAL_LEVEL_UNAPPROVED );
            }
        }
        else
        {
            for ( OrganisationUnit orgUnit : user.getOrganisationUnits() )
            {
                int level = requiredApprovalLevel( orgUnit, user, approvalLevels );
                
                map.put( orgUnit, level );
            }
        }

        // ---------------------------------------------------------------------
        // Add data view organisation units with approval levels
        // ---------------------------------------------------------------------

        Collection<OrganisationUnit> dataViewOrgUnits = user.getDataViewOrganisationUnits();

        if ( dataViewOrgUnits == null || dataViewOrgUnits.isEmpty() )
        {
            dataViewOrgUnits = organisationUnitService.getRootOrganisationUnits();
        }

        for ( OrganisationUnit orgUnit : dataViewOrgUnits )
        {
            if ( !map.containsKey( orgUnit ) )
            {
                int level = requiredApprovalLevel( orgUnit, user, approvalLevels );
                
                map.put( orgUnit, level );
            }
        }

        return map;
    }
    
    @Override
    public Map<OrganisationUnit, Integer> getUserReadApprovalLevels( DataApprovalLevel approvalLevel )
    {
        Map<OrganisationUnit, Integer> map = new HashMap<>();
        
        User user = currentUserService.getCurrentUser();

        Collection<OrganisationUnit> orgUnits = user.getDataViewOrganisationUnits();

        if ( orgUnits == null || orgUnits.isEmpty() )
        {
            orgUnits = organisationUnitService.getRootOrganisationUnits();
        }

        for ( OrganisationUnit orgUnit : orgUnits )
        {
            map.put( orgUnit, approvalLevel.getLevel() );
        }

        return map;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Finds the lowest number (highest level) organisaiton unit level
     * from the organisations assigned to the current user.
     */
    private int getCurrentUsersLowestNumberOrgUnitLevel()
    {
        int level = APPROVAL_LEVEL_UNAPPROVED;

        Set<OrganisationUnit> userOrgUnits = currentUserService.getCurrentUser().getOrganisationUnits();

        for ( OrganisationUnit orgUnit : userOrgUnits )
        {
            if ( orgUnit.getLevel() < level )
            {
                level = orgUnit.getLevel();
            }
        }
        return level;
    }

    /**
     * Swaps a data approval level with the next higher level.
     *
     * @param level lower level to swap.
     */
    private void swapWithNextLevel( int level )
    {
        List<DataApprovalLevel> dataApprovalLevels = getAllDataApprovalLevels();

        int index = level - 1;

        DataApprovalLevel d2 = dataApprovalLevels.get( index );
        DataApprovalLevel d1  = dataApprovalLevels.get( index + 1 );

        dataApprovalLevels.set( index, d1 );
        dataApprovalLevels.set( index + 1, d2 );

        update( d1, index );
        update( d2, index + 1 );
    }

    /**
     * Updates a data approval level object by setting the level to
     * correspond with the list index, setting the updated date to now,
     * and updating the object on disk.
     *
     * @param dataApprovalLevel data approval level to update
     * @param index index of the object (used to set the level.)
     */
    private void update( DataApprovalLevel dataApprovalLevel, int index )
    {
        dataApprovalLevel.setLevel( index + 1 );

        dataApprovalLevelStore.update( dataApprovalLevel );
    }

    /**
     * Finds the right index at which to insert a new data approval level.
     * Returns -1 if the new data approval level is a duplicate.
     *
     * @param dataApprovalLevels list of all levels.
     * @param newLevel new level to find the insertion point for.
     * @return index where the new approval level should be inserted,
     * or -1 if the new level is a duplicate.
     */
    private int getInsertIndex( List<DataApprovalLevel> dataApprovalLevels, DataApprovalLevel newLevel )
    {
        int i = dataApprovalLevels.size() - 1;

        while ( i >= 0 )
        {
            DataApprovalLevel test = dataApprovalLevels.get( i );

            int orgLevelDifference = newLevel.getOrgUnitLevel() - test.getOrgUnitLevel();

            if ( orgLevelDifference > 0 )
            {
                break;
            }

            if ( orgLevelDifference == 0 )
            {
                if ( newLevel.levelEquals( test ) )
                {
                    return -1;
                }

                if ( test.getCategoryOptionGroupSet() == null )
                {
                    break;
                }
            }

            i--;
        }
        
        return i + 1;
    }

    /**
     * Get the approval level for an organisation unit that is required
     * in order for the user to see the data, assuming user is limited to seeing
     * approved data only from lower approval levels.
     *
     * @param orgUnit organisation unit to test.
     * @param user the user.
     * @param approvalLevels all data approval levels.
     * @return required approval level for user to see the data.
     */
    private int requiredApprovalLevel( OrganisationUnit orgUnit, User user, List<DataApprovalLevel> approvalLevels )
    {
        DataApprovalLevel userLevel = getUserApprovalLevel( orgUnit, user, approvalLevels );

        int totalLevels = approvalLevels.size();
        
        return userLevel == null ? 0 : 
            userLevel.getLevel() == totalLevels ? APPROVAL_LEVEL_UNAPPROVED :
            userLevel.getLevel() + 1;
    }

    /**
     * Get the approval level for a user for a given organisation unit. It is
     * assumed that the user has access to the organisation unit (must be
     * checked elsewhere, it is not checked here.) If the organisation unit is
     * above all approval levels, returns null (no approval levels apply.)
     * <p>
     * If users are restricted to viewing approved data only, users may
     * see data from lower levels *only* if it is approved *below* this approval
     * level (higher number approval level). Or, if this method returns the
     * lowest (highest number) approval level, users may see unapproved data.
     * <p>
     * If users have approve/unapprove authority (checked elsewhere, not here),
     * the returned level is the level at which users may approve/unapprove.
     * If users have authority to approve at lower levels, they may approve
     * at levels below the returned level.
     * <p>
     * If users have accept/unaccept authority (checked elsewhere, not here),
     * users may accept/unaccept at the level just *below* this level.
     *
     * @param orgUnit organisation unit to test.
     * @param user the user.
     * @param approvalLevels app data approval levels.
     * @return approval level for user.
     */
    private DataApprovalLevel getUserApprovalLevel( OrganisationUnit orgUnit, User user, List<DataApprovalLevel> approvalLevels )
    {
        int userOrgUnitLevel = orgUnit.getLevel();

        DataApprovalLevel userLevel = null;

        for ( DataApprovalLevel level : approvalLevels )
        {
            if ( level.getOrgUnitLevel() >= userOrgUnitLevel &&
                securityService.canRead( level ) &&
                canReadCOGS( user, level.getCategoryOptionGroupSet() ) )
            {
                userLevel = level;
                break;
            }
        }

        return userLevel;
    }

    /**
     * Can the user read from this CategoryOptionGroupSet (COGS)?
     * <p>
     * If the COGS is null, then the user must have no dimension constraints.
     * (In other words, the user must be able to read across all category
     * option groups.)
     * <p>
     * If the COGS is not null, then the user must be able to read at least
     * one category option group from the category option group set.
     *
     * @param cogs The category option group set to test
     * @return true if user can read at least one category option group.
     */
    private boolean canReadCOGS( User user, CategoryOptionGroupSet cogs )
    {
        if ( cogs == null )
        {
            UserCredentials userCredentials = user.getUserCredentials();

            return CollectionUtils.isEmpty( userCredentials.getCogsDimensionConstraints() )
                && CollectionUtils.isEmpty( userCredentials.getCatDimensionConstraints() );
        }

        return !CollectionUtils.isEmpty( categoryService.getCategoryOptionGroups( cogs ) );
    }

    /**
     * Returns the subset of approval levels that the user is allowed to access.
     *
     * @param approvalLevels the approval levels to test.
     * @return the subset of approval levels to which the user has access.
     */
    private List<DataApprovalLevel> subsetUserDataApprovalLevels( List<DataApprovalLevel> approvalLevels )
    {
        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();

        int lowestNumberOrgUnitLevel = getCurrentUsersLowestNumberOrgUnitLevel();

        boolean canSeeAllDimensions = CollectionUtils.isEmpty( categoryService.getCoDimensionConstraints( userCredentials ) )
            && CollectionUtils.isEmpty( categoryService.getCogDimensionConstraints( userCredentials ) );

        List<DataApprovalLevel> userDataApprovalLevels = new ArrayList<>();

        boolean addLevel = false;

        for ( DataApprovalLevel approvalLevel : approvalLevels )
        {
            if ( !addLevel && approvalLevel.getOrgUnitLevel() >= lowestNumberOrgUnitLevel )
            {
                CategoryOptionGroupSet cogs = approvalLevel.getCategoryOptionGroupSet();

                addLevel = securityService.canRead( approvalLevel ) &&
                    cogs == null ? canSeeAllDimensions :
                    ( securityService.canRead( cogs ) && !CollectionUtils.isEmpty( categoryService.getCategoryOptionGroups( cogs ) ) );
            }

            if ( addLevel )
            {
                userDataApprovalLevels.add( approvalLevel );
            }
        }

        return userDataApprovalLevels;
    }
}
