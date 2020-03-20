package org.hisp.dhis.organisationunit;

import static org.hisp.dhis.commons.util.TextUtils.joinHyphen;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.SortProperty;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.hierarchy.HierarchyViolationException;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitLevelComparator;
import org.hisp.dhis.system.filter.OrganisationUnitPolygonCoveringCoordinateFilter;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.version.VersionService;
import org.springframework.transaction.annotation.Transactional;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import com.google.common.collect.Sets;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

/**
 * @author Torgeir Lorange Ostby
 */
@Transactional
public class DefaultOrganisationUnitService
    implements OrganisationUnitService
{
    private static final String LEVEL_PREFIX = "Level ";

    private static Cache<Boolean> IN_USER_ORG_UNIT_HIERARCHY_CACHE;

    private static Cache<Boolean> IN_USER_ORG_UNIT_SEARCH_HIERARCHY_CACHE;


    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitStore organisationUnitStore;

    public void setOrganisationUnitStore( OrganisationUnitStore organisationUnitStore )
    {
        this.organisationUnitStore = organisationUnitStore;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitLevelStore organisationUnitLevelStore;

    public void setOrganisationUnitLevelStore( OrganisationUnitLevelStore organisationUnitLevelStore )
    {
        this.organisationUnitLevelStore = organisationUnitLevelStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private VersionService versionService;

    public void setVersionService( VersionService versionService )
    {
        this.versionService = versionService;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService( ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }
    
    private CacheProvider cacheProvider;

    public void setCacheProvider( CacheProvider cacheProvider )
    {
        this.cacheProvider = cacheProvider;
    }
    
    @PostConstruct
    public void init()
    {
        IN_USER_ORG_UNIT_HIERARCHY_CACHE = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "inUserOuHierarchy" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun() ? 0 : 20000 )
            .build();

        IN_USER_ORG_UNIT_SEARCH_HIERARCHY_CACHE = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "inUserSearchOuHierarchy" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun() ? 0 : 20000 )
            .build();
    }

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    @Override
    public int addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnitStore.save( organisationUnit );
        User user = currentUserService.getCurrentUser();

        if ( organisationUnit.getParent() == null && user != null )
        {
            // Adding a new root node, add this node to the current user
            user.getOrganisationUnits().add( organisationUnit );
        }

        return organisationUnit.getId();
    }

    @Override
    public void updateOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnitStore.update( organisationUnit );
    }

    @Override
    public void updateOrganisationUnitVersion()
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    @Override
    public void updateOrganisationUnit( OrganisationUnit organisationUnit, boolean updateHierarchy )
    {
        updateOrganisationUnit( organisationUnit );
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit organisationUnit )
        throws HierarchyViolationException
    {
        organisationUnit = getOrganisationUnit( organisationUnit.getId() );

        if ( !organisationUnit.getChildren().isEmpty() )
        {
            throw new HierarchyViolationException( "Cannot delete an OrganisationUnit with children" );
        }

        OrganisationUnit parent = organisationUnit.getParent();

        if ( parent != null )
        {
            parent.getChildren().remove( organisationUnit );

            organisationUnitStore.update( parent );
        }

        organisationUnitStore.delete( organisationUnit );
    }

    @Override
    public OrganisationUnit getOrganisationUnit( int id )
    {
        return organisationUnitStore.get( id );
    }

    @Override
    public List<OrganisationUnit> getAllOrganisationUnits()
    {
        return organisationUnitStore.getAll();
    }

    @Override
    public List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated( Date lastUpdated )
    {
        return organisationUnitStore.getAllOrganisationUnitsByLastUpdated( lastUpdated );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnits( Collection<Integer> identifiers )
    {
        return organisationUnitStore.getById( identifiers );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsByUid( Collection<String> uids )
    {
        return organisationUnitStore.getByUid( uids );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsByQuery( OrganisationUnitQueryParams params )
    {
        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public OrganisationUnit getOrganisationUnit( String uid )
    {
        return organisationUnitStore.getByUid( uid );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitByName( String name )
    {
        return new ArrayList<>( organisationUnitStore.getAllEqName( name ) );
    }

    @Override
    public OrganisationUnit getOrganisationUnitByCode( String code )
    {
        return organisationUnitStore.getByCode( code );
    }

    @Override
    public List<OrganisationUnit> getRootOrganisationUnits()
    {
        return organisationUnitStore.getRootOrganisationUnits();
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnits( Collection<OrganisationUnitGroup> groups, Collection<OrganisationUnit> parents )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( Sets.newHashSet( parents ) );
        params.setGroups( Sets.newHashSet( groups ) );

        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithChildren( Collection<String> parentUids )
    {
        return getOrganisationUnitsWithChildren( parentUids, null );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithChildren( Collection<String> parentUids, Integer maxLevels )
    {
        List<OrganisationUnit> units = new ArrayList<>();

        for ( String uid : parentUids )
        {
            units.addAll( getOrganisationUnitWithChildren( uid, maxLevels ) );
        }

        return units;
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitWithChildren( String uid )
    {
        return getOrganisationUnitWithChildren( uid, null );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitWithChildren( String uid, Integer maxLevels )
    {
        OrganisationUnit unit = getOrganisationUnit( uid );

        int id = unit != null ? unit.getId() : -1;

        return getOrganisationUnitWithChildren( id, maxLevels );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitWithChildren( int id )
    {
        return getOrganisationUnitWithChildren( id, null );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitWithChildren( int id, Integer maxLevels )
    {
        OrganisationUnit organisationUnit = getOrganisationUnit( id );

        if ( organisationUnit == null )
        {
            return new ArrayList<>();
        }

        if ( maxLevels != null && maxLevels <= 0 )
        {
            return new ArrayList<>();
        }

        int rootLevel = organisationUnit.getLevel();

        Integer levels = maxLevels != null ? (rootLevel + maxLevels - 1) : null;
        SortProperty orderBy = SortProperty.fromValue(
            userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY ).toString() );

        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( Sets.newHashSet( organisationUnit ) );
        params.setMaxLevels( levels );
        params.setFetchChildren( true );
        params.setOrderBy( orderBy );

        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsAtLevel( int level )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setLevels( Sets.newHashSet( level ) );

        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsAtLevel( int level, OrganisationUnit parent )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setLevels( Sets.newHashSet( level ) );

        if ( parent != null )
        {
            params.setParents( Sets.newHashSet( parent ) );
        }

        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsAtOrgUnitLevels( Collection<OrganisationUnitLevel> levels, Collection<OrganisationUnit> parents )
    {
        return getOrganisationUnitsAtLevels( levels.stream().map( l -> l.getLevel() ).collect( Collectors.toList() ), parents );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsAtLevels( Collection<Integer> levels, Collection<OrganisationUnit> parents )
    {
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setLevels( Sets.newHashSet( levels ) );
        params.setParents( Sets.newHashSet( parents ) );

        return organisationUnitStore.getOrganisationUnits( params );
    }

    @Override
    public int getNumberOfOrganisationalLevels()
    {
        return organisationUnitStore.getMaxLevel();
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return organisationUnitStore.getOrganisationUnitsWithoutGroups();
    }

    @Override
    public Long getOrganisationUnitHierarchyMemberCount( OrganisationUnit parent, Object member, String collectionName )
    {
        return organisationUnitStore.getOrganisationUnitHierarchyMemberCount( parent, member, collectionName );
    }

    @Override
    public OrganisationUnitDataSetAssociationSet getOrganisationUnitDataSetAssociationSet( Integer maxLevels )
    {
        User user = currentUserService.getCurrentUser();

        Set<OrganisationUnit> organisationUnits = user != null ? user.getOrganisationUnits() : null;
        List<DataSet> dataSets = ( user != null && user.isSuper() ) ? null : dataSetService.getUserDataWrite( user );

        Map<String, Set<String>> associationSet = organisationUnitStore.getOrganisationUnitDataSetAssocationMap( organisationUnits, dataSets );

        OrganisationUnitDataSetAssociationSet set = new OrganisationUnitDataSetAssociationSet();

        for ( Map.Entry<String, Set<String>> entry : associationSet.entrySet() )
        {
            int index = set.getDataSetAssociationSets().indexOf( entry.getValue() );

            if ( index == -1 ) // Association set does not exist, add new
            {
                index = set.getDataSetAssociationSets().size();
                set.getDataSetAssociationSets().add( entry.getValue() );
            }

            set.getOrganisationUnitAssociationSetMap().put( entry.getKey(), index );
            set.getDistinctDataSets().addAll( entry.getValue() );
        }

        return set;
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsBetweenByName( String name, int first, int max )
    {
        return organisationUnitStore.getAllLikeName( name, first, max );
    }

    @Override
    public boolean isInUserHierarchy( OrganisationUnit organisationUnit )
    {
        return isInUserHierarchy( currentUserService.getCurrentUser(), organisationUnit );
    }

    @Override
    public boolean isInUserHierarchyCached( OrganisationUnit organisationUnit )
    {
        return isInUserHierarchyCached( currentUserService.getCurrentUser(), organisationUnit );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUserHierarchyCached( User user, OrganisationUnit organisationUnit )
    {
        String cacheKey = joinHyphen( user.getUsername(), organisationUnit.getUid() );

        return IN_USER_ORG_UNIT_HIERARCHY_CACHE.get( cacheKey, ou -> isInUserHierarchy( user, organisationUnit ) ).get();
    }

    @Override
    public boolean isInUserHierarchy( User user, OrganisationUnit organisationUnit )
    {
        if ( user == null || user.getOrganisationUnits() == null || user.getOrganisationUnits().isEmpty() )
        {
            return false;
        }

        return organisationUnit.isDescendant( user.getOrganisationUnits() );
    }

    @Override
    public boolean isInUserHierarchy( String uid, Set<OrganisationUnit> organisationUnits )
    {
        OrganisationUnit organisationUnit = organisationUnitStore.getByUid( uid );

        return organisationUnit != null ? organisationUnit.isDescendant( organisationUnits ) : false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isInUserSearchHierarchy( OrganisationUnit organisationUnit )
    {
        return isInUserSearchHierarchy( currentUserService.getCurrentUser(), organisationUnit );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUserSearchHierarchyCached( OrganisationUnit organisationUnit )
    {
        return isInUserSearchHierarchyCached( currentUserService.getCurrentUser(), organisationUnit );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUserSearchHierarchyCached( User user, OrganisationUnit organisationUnit )
    {
        String cacheKey = joinHyphen( user.getUsername(), organisationUnit.getUid() );

        return IN_USER_ORG_UNIT_SEARCH_HIERARCHY_CACHE.get( cacheKey, ou -> isInUserSearchHierarchy( user, organisationUnit ) ).get();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUserSearchHierarchy( User user, OrganisationUnit organisationUnit )
    {
        if ( user == null || user.getTeiSearchOrganisationUnitsWithFallback() == null || user.getTeiSearchOrganisationUnitsWithFallback().isEmpty() )
        {
            return false;
        }

        return organisationUnit.isDescendant( user.getTeiSearchOrganisationUnitsWithFallback() );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitHierarchy
    // -------------------------------------------------------------------------

    @Override
    public OrganisationUnitHierarchy getOrganisationUnitHierarchy()
    {
        return organisationUnitStore.getOrganisationUnitHierarchy();
    }

    @Override
    public void updateOrganisationUnitParent( int organisationUnitId, int parentId )
    {
        organisationUnitStore.updateOrganisationUnitParent( organisationUnitId, parentId );
    }

    // -------------------------------------------------------------------------
    // OrganisationUnitLevel
    // -------------------------------------------------------------------------

    @Override
    public int addOrganisationUnitLevel( OrganisationUnitLevel organisationUnitLevel )
    {
        organisationUnitLevelStore.save( organisationUnitLevel );
        return organisationUnitLevel.getId();
    }

    @Override
    public void updateOrganisationUnitLevel( OrganisationUnitLevel organisationUnitLevel )
    {
        organisationUnitLevelStore.update( organisationUnitLevel );
    }

    @Override
    public void addOrUpdateOrganisationUnitLevel( OrganisationUnitLevel level )
    {
        OrganisationUnitLevel existing = getOrganisationUnitLevelByLevel( level.getLevel() );

        if ( existing == null )
        {
            addOrganisationUnitLevel( level );
        }
        else
        {
            existing.setName( level.getName() );
            existing.setOfflineLevels( level.getOfflineLevels() );

            updateOrganisationUnitLevel( existing );
        }
    }

    @Override
    public void pruneOrganisationUnitLevels( Set<Integer> currentLevels )
    {
        for ( OrganisationUnitLevel level : getOrganisationUnitLevels() )
        {
            if ( !currentLevels.contains( level.getLevel() ) )
            {
                deleteOrganisationUnitLevel( level );
            }
        }
    }

    @Override
    public OrganisationUnitLevel getOrganisationUnitLevel( int id )
    {
        return organisationUnitLevelStore.get( id );
    }

    @Override
    public OrganisationUnitLevel getOrganisationUnitLevel( String uid )
    {
        return organisationUnitLevelStore.getByUid( uid );
    }

    @Override
    public void deleteOrganisationUnitLevel( OrganisationUnitLevel organisationUnitLevel )
    {
        organisationUnitLevelStore.delete( organisationUnitLevel );
    }

    @Override
    public void deleteOrganisationUnitLevels()
    {
        organisationUnitLevelStore.deleteAll();
    }

    @Override
    public List<OrganisationUnitLevel> getOrganisationUnitLevels()
    {
        return ListUtils.sort( organisationUnitLevelStore.getAll(),
            OrganisationUnitLevelComparator.INSTANCE );
    }

    @Override
    public OrganisationUnitLevel getOrganisationUnitLevelByLevel( int level )
    {
        return organisationUnitLevelStore.getByLevel( level );
    }

    @Override
    public List<OrganisationUnitLevel> getOrganisationUnitLevelByName( String name )
    {
        return new ArrayList<>( organisationUnitLevelStore.getAllEqName( name ) );
    }

    @Override
    public List<OrganisationUnitLevel> getFilledOrganisationUnitLevels()
    {
        Map<Integer, OrganisationUnitLevel> levelMap = getOrganisationUnitLevelMap();

        List<OrganisationUnitLevel> levels = new ArrayList<>();

        int levelNo = getNumberOfOrganisationalLevels();

        for ( int i = 0; i < levelNo; i++ )
        {
            int level = i + 1;

            OrganisationUnitLevel filledLevel = ObjectUtils.firstNonNull(
                levelMap.get( level ), new OrganisationUnitLevel( level, LEVEL_PREFIX + level ) );

            levels.add( filledLevel );
        }

        return levels;
    }

    @Override
    public Map<Integer, OrganisationUnitLevel> getOrganisationUnitLevelMap()
    {
        Map<Integer, OrganisationUnitLevel> levelMap = new HashMap<>();

        List<OrganisationUnitLevel> levels = getOrganisationUnitLevels();

        for ( OrganisationUnitLevel level : levels )
        {
            levelMap.put( level.getLevel(), level );
        }

        return levelMap;
    }

    @Override
    public int getNumberOfOrganisationUnits()
    {
        return organisationUnitStore.getCount();
    }

    @Override
    public int getOfflineOrganisationUnitLevels()
    {
        // ---------------------------------------------------------------------
        // Get level from organisation unit of current user
        // ---------------------------------------------------------------------

        User user = currentUserService.getCurrentUser();

        if ( user != null && user.hasOrganisationUnit() )
        {
            OrganisationUnit organisationUnit = user.getOrganisationUnit();

            int level = organisationUnit.getLevel();

            OrganisationUnitLevel orgUnitLevel = getOrganisationUnitLevelByLevel( level );

            if ( orgUnitLevel != null && orgUnitLevel.getOfflineLevels() != null )
            {
                return orgUnitLevel.getOfflineLevels();
            }
        }

        // ---------------------------------------------------------------------
        // Get level from system configuration
        // ---------------------------------------------------------------------

        OrganisationUnitLevel level = configurationService.getConfiguration().getOfflineOrganisationUnitLevel();

        if ( level != null )
        {
            return level.getLevel();
        }

        // ---------------------------------------------------------------------
        // Get max level
        // ---------------------------------------------------------------------

        int max = getOrganisationUnitLevels().size();

        OrganisationUnitLevel maxLevel = getOrganisationUnitLevelByLevel( max );

        if ( maxLevel != null )
        {
            return maxLevel.getLevel();
        }

        // ---------------------------------------------------------------------
        // Return 1 level as fall back
        // ---------------------------------------------------------------------

        return 1;
    }

    @Override
    public void updatePaths()
    {
        organisationUnitStore.updatePaths();
    }

    @Override
    public void forceUpdatePaths()
    {
        organisationUnitStore.forceUpdatePaths();
    }

    @Override
    public Integer getOrganisationUnitLevelByLevelOrUid( String level )
    {
        if ( level.matches( ExpressionService.INT_EXPRESSION ) )
        {
            Integer orgUnitLevel = Integer.parseInt( level );

            return orgUnitLevel > 0 ? orgUnitLevel : null;
        }
        else if ( level.matches( ExpressionService.UID_EXPRESSION ) )
        {
            OrganisationUnitLevel orgUnitLevel = getOrganisationUnitLevel( level );

            return orgUnitLevel != null ? orgUnitLevel.getLevel() : null;
        }

        return null;
    }

    /**
     * Get all the Organisation Units within the distance of a coordinate.
     */
    @Override
    public List<OrganisationUnit> getOrganisationUnitWithinDistance( double longitude, double latitude,
        double distance )
    {
        List<OrganisationUnit> objects = organisationUnitStore.getWithinCoordinateArea( GeoUtils.getBoxShape(
            longitude, latitude, distance ) );

        // Go through the list and remove the ones located outside radius

        if ( objects != null && objects.size() > 0 )
        {
            Iterator<OrganisationUnit> iter = objects.iterator();

            Point2D centerPoint = new Point2D.Double( longitude, latitude );

            while ( iter.hasNext() )
            {
                OrganisationUnit orgunit = iter.next();

                double distancebetween = GeoUtils.getDistanceBetweenTwoPoints( centerPoint,
                    ValidationUtils.getCoordinatePoint2D( orgunit.getCoordinates() ) );

                if ( distancebetween > distance )
                {
                    iter.remove();
                }
            }
        }

        return objects;
    }

    /**
     * Get lowest level/target level Organisation Units that includes the coordinates.
     */
    @Override
    public List<OrganisationUnit> getOrganisationUnitByCoordinate( double longitude, double latitude,
        String topOrgUnitUid, Integer targetLevel )
    {
        List<OrganisationUnit> orgUnits = new ArrayList<>();

        if ( GeoUtils.checkGeoJsonPointValid( longitude, latitude ) )
        {
            OrganisationUnit topOrgUnit = null;

            if ( topOrgUnitUid != null && !topOrgUnitUid.isEmpty() )
            {
                topOrgUnit = getOrganisationUnit( topOrgUnitUid );
            }
            else
            {
                // Get top search point through top level org unit which contains coordinate

                List<OrganisationUnit> orgUnitsTopLevel = getTopLevelOrgUnitWithPoint( longitude, latitude, 1,
                    getNumberOfOrganisationalLevels() - 1 );

                if ( orgUnitsTopLevel.size() == 1 )
                {
                    topOrgUnit = orgUnitsTopLevel.iterator().next();
                }
            }

            // Search children org units to get the lowest level org unit that contains coordinate

            if ( topOrgUnit != null )
            {
                List<OrganisationUnit> orgUnitChildren = new ArrayList<>();

                if ( targetLevel != null )
                {
                    orgUnitChildren = getOrganisationUnitsAtLevel( targetLevel, topOrgUnit );
                }
                else
                {
                    orgUnitChildren = getOrganisationUnitWithChildren( topOrgUnit.getId() );
                }

                FilterUtils.filter( orgUnitChildren, new OrganisationUnitPolygonCoveringCoordinateFilter( longitude, latitude ) );

                // Get org units with lowest level

                int bottomLevel = topOrgUnit.getLevel();

                for ( OrganisationUnit ou : orgUnitChildren )
                {
                    if ( ou.getLevel() > bottomLevel )
                    {
                        bottomLevel = ou.getLevel();
                    }
                }

                for ( OrganisationUnit ou : orgUnitChildren )
                {
                    if ( ou.getLevel() == bottomLevel )
                    {
                        orgUnits.add( ou );
                    }
                }
            }
        }

        return orgUnits;
    }

    // -------------------------------------------------------------------------
    // Version
    // -------------------------------------------------------------------------

    @Override
    public void updateVersion()
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Searches organisation units until finding one with polygon containing point.
     */
    private List<OrganisationUnit> getTopLevelOrgUnitWithPoint( double longitude, double latitude,
        int searchLevel, int stopLevel )
    {
        for ( int i = searchLevel; i <= stopLevel; i++ )
        {
            List<OrganisationUnit> unitsAtLevel = new ArrayList<>( getOrganisationUnitsAtLevel( i ) );
            FilterUtils.filter( unitsAtLevel, new OrganisationUnitPolygonCoveringCoordinateFilter( longitude, latitude ) );

            if ( unitsAtLevel.size() > 0 )
            {
                return unitsAtLevel;
            }
        }

        return new ArrayList<>();
    }
}
