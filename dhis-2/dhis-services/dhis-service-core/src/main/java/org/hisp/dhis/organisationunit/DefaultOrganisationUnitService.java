/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.organisationunit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hisp.dhis.commons.util.TextUtils.joinHyphen;

import com.google.common.collect.Sets;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SortProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.hierarchy.HierarchyViolationException;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitLevelComparator;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.system.filter.OrganisationUnitPolygonCoveringCoordinateFilter;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Torgeir Lorange Ostby
 */
@Service("org.hisp.dhis.organisationunit.OrganisationUnitService")
public class DefaultOrganisationUnitService implements OrganisationUnitService {
  private static final String LEVEL_PREFIX = "Level ";
  private final OrganisationUnitStore organisationUnitStore;
  private final OrganisationUnitLevelStore organisationUnitLevelStore;
  private final ConfigurationService configurationService;

  private final Cache<Boolean> inUserOrgUnitHierarchyCache;

  public DefaultOrganisationUnitService(
      OrganisationUnitStore organisationUnitStore,
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitLevelStore organisationUnitLevelStore,
      ConfigurationService configurationService,
      CacheProvider cacheProvider) {

    checkNotNull(organisationUnitStore);
    checkNotNull(idObjectManager);
    checkNotNull(organisationUnitLevelStore);
    checkNotNull(configurationService);
    checkNotNull(cacheProvider);

    this.organisationUnitStore = organisationUnitStore;
    this.organisationUnitLevelStore = organisationUnitLevelStore;
    this.configurationService = configurationService;

    this.inUserOrgUnitHierarchyCache = cacheProvider.createInUserOrgUnitHierarchyCache();
  }

  // -------------------------------------------------------------------------
  // OrganisationUnit
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnitStore.save(organisationUnit);
    // TODO: MAS: This is only used in tests and should be moved to a test util class
    return organisationUnit.getId();
  }

  @Override
  @Transactional
  public void updateOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnitStore.update(organisationUnit);
  }

  @Override
  @Transactional
  public void updateOrganisationUnit(OrganisationUnit organisationUnit, boolean updateHierarchy) {
    updateOrganisationUnit(organisationUnit);
  }

  @Override
  @Transactional
  public void deleteOrganisationUnit(OrganisationUnit organisationUnit)
      throws HierarchyViolationException {
    organisationUnitStore.delete(organisationUnit);
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnit getOrganisationUnit(long id) {
    return organisationUnitStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getAllOrganisationUnits() {
    return organisationUnitStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated(Date lastUpdated) {
    return organisationUnitStore.getAllOrganisationUnitsByLastUpdated(lastUpdated);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnits(Collection<Long> identifiers) {
    return organisationUnitStore.getById(identifiers);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsByUid(@Nonnull Collection<String> uids) {
    return organisationUnitStore.getByUid(new HashSet<>(uids));
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnit getOrganisationUnit(String uid) {
    return organisationUnitStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitByName(String name) {
    return new ArrayList<>(organisationUnitStore.getAllEqName(name));
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnit getOrganisationUnitByCode(String code) {
    return organisationUnitStore.getByCode(code);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getRootOrganisationUnits() {
    return organisationUnitStore.getRootOrganisationUnits();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnits(
      Collection<OrganisationUnitGroup> groups, Collection<OrganisationUnit> parents) {
    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
    params.setParents(Sets.newHashSet(parents));
    params.setGroups(Sets.newHashSet(groups));

    return organisationUnitStore.getOrganisationUnits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsWithChildren(Collection<String> parentUids) {
    return getOrganisationUnitsWithChildren(parentUids, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsWithChildren(
      Collection<String> parentUids, Integer maxLevels) {
    List<OrganisationUnit> units = new ArrayList<>();

    for (String uid : parentUids) {
      units.addAll(getOrganisationUnitWithChildren(uid, maxLevels));
    }

    return units;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitWithChildren(String uid) {
    return getOrganisationUnitWithChildren(uid, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitWithChildren(String uid, Integer maxLevels) {
    OrganisationUnit unit = getOrganisationUnit(uid);

    long id = unit != null ? unit.getId() : -1;

    return getOrganisationUnitWithChildren(id, maxLevels);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitWithChildren(long id) {
    return getOrganisationUnitWithChildren(id, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitWithChildren(long id, Integer maxLevels) {
    OrganisationUnit organisationUnit = getOrganisationUnit(id);

    if (organisationUnit == null) {
      return new ArrayList<>();
    }

    if (maxLevels != null && maxLevels <= 0) {
      return new ArrayList<>();
    }

    int rootLevel = organisationUnit.getLevel();
    Integer levels = maxLevels != null ? (rootLevel + maxLevels - 1) : null;

    SortProperty orderBy =
        SortProperty.fromValue(
            UserSettings.getCurrentSettings().getUserAnalysisDisplayProperty().toString());

    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
    params.setParents(Sets.newHashSet(organisationUnit));
    params.setMaxLevels(levels);
    params.setFetchChildren(true);
    params.setOrderBy(orderBy);

    return organisationUnitStore.getOrganisationUnits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getDataSetOrganisationUnits(@Nonnull String dataSetUid) {
    return organisationUnitStore.getOrganisationUnitsByDataSet(dataSetUid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getProgramOrganisationUnits(@Nonnull String programUid) {
    return organisationUnitStore.getOrganisationUnitsByProgram(programUid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsAtLevel(int level) {
    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
    params.setLevels(Sets.newHashSet(level));

    return organisationUnitStore.getOrganisationUnits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsAtLevel(int level, OrganisationUnit parent) {
    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
    params.setLevels(Sets.newHashSet(level));

    if (parent != null) {
      params.setParents(Sets.newHashSet(parent));
    }

    return organisationUnitStore.getOrganisationUnits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsAtOrgUnitLevels(
      Collection<OrganisationUnitLevel> levels, Collection<OrganisationUnit> parents) {
    return getOrganisationUnitsAtLevels(
        levels.stream().map(OrganisationUnitLevel::getLevel).collect(Collectors.toList()), parents);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsAtLevels(
      Collection<Integer> levels, Collection<OrganisationUnit> parents) {
    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
    params.setLevels(Sets.newHashSet(levels));
    params.setParents(Sets.newHashSet(parents));

    return organisationUnitStore.getOrganisationUnits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public int getNumberOfOrganisationalLevels() {
    return organisationUnitStore.getMaxLevel();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsWithoutGroups() {
    return organisationUnitStore.getOrganisationUnitsWithoutGroups();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<OrganisationUnit> getOrganisationUnitsWithCyclicReferences() {
    return organisationUnitStore.getOrganisationUnitsWithCyclicReferences();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrphanedOrganisationUnits() {
    return organisationUnitStore.getOrphanedOrganisationUnits();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitsViolatingExclusiveGroupSets() {
    return organisationUnitStore.getOrganisationUnitsViolatingExclusiveGroupSets();
  }

  @Override
  @Transactional(readOnly = true)
  public Long getOrganisationUnitHierarchyMemberCount(
      OrganisationUnit parent, Object member, String collectionName) throws BadRequestException {
    if (!collectionName.matches("[a-zA-Z0-9_]{1,30}"))
      throw new BadRequestException("Not a valid property name: " + collectionName);
    return organisationUnitStore.getOrganisationUnitHierarchyMemberCount(
        parent, member, collectionName);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isInUserHierarchyCached(User user, OrganisationUnit organisationUnit) {
    String cacheKey = joinHyphen(user.getUsername(), organisationUnit.getUid());
    return inUserOrgUnitHierarchyCache.get(
        cacheKey, ou -> isInUserHierarchy(user, organisationUnit));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isInUserHierarchy(User user, OrganisationUnit organisationUnit) {
    if (isEmpty(user.getOrganisationUnits())) {
      return false;
    }

    OrganisationUnit unit = organisationUnitStore.getByUid(organisationUnit.getUid());

    if (unit == null) {
      return false;
    }

    return unit.isDescendant(user.getOrganisationUnits());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isInUserDataViewHierarchy(User user, OrganisationUnit organisationUnit) {
    if (isEmpty(user.getDataViewOrganisationUnitsWithFallback())) {
      return false;
    }

    return organisationUnit.isDescendant(user.getDataViewOrganisationUnitsWithFallback());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isInUserSearchHierarchy(User user, OrganisationUnit organisationUnit) {
    if (isEmpty(user.getTeiSearchOrganisationUnitsWithFallback())) {
      return false;
    }

    return organisationUnit.isDescendant(user.getTeiSearchOrganisationUnitsWithFallback());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isInUserHierarchy(String uid, Set<OrganisationUnit> organisationUnits) {
    OrganisationUnit organisationUnit = organisationUnitStore.getByUid(uid);

    return organisationUnit != null && organisationUnit.isDescendant(organisationUnits);
  }

  // -------------------------------------------------------------------------
  // OrganisationUnitLevel
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addOrganisationUnitLevel(OrganisationUnitLevel organisationUnitLevel) {
    organisationUnitLevelStore.save(organisationUnitLevel);
    return organisationUnitLevel.getId();
  }

  @Override
  @Transactional
  public void updateOrganisationUnitLevel(OrganisationUnitLevel organisationUnitLevel) {
    organisationUnitLevelStore.update(organisationUnitLevel);
  }

  @Override
  @Transactional
  public void addOrUpdateOrganisationUnitLevel(OrganisationUnitLevel level) {
    OrganisationUnitLevel existing = getOrganisationUnitLevelByLevel(level.getLevel());

    if (existing == null) {
      addOrganisationUnitLevel(level);
    } else {
      existing.setName(level.getName());
      existing.setOfflineLevels(level.getOfflineLevels());

      updateOrganisationUnitLevel(existing);
    }
  }

  @Override
  @Transactional
  public void pruneOrganisationUnitLevels(Set<Integer> currentLevels) {
    for (OrganisationUnitLevel level : getOrganisationUnitLevels()) {
      if (!currentLevels.contains(level.getLevel())) {
        deleteOrganisationUnitLevel(level);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnitLevel getOrganisationUnitLevel(long id) {
    return organisationUnitLevelStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnitLevel getOrganisationUnitLevel(String uid) {
    return organisationUnitLevelStore.getByUid(uid);
  }

  @Override
  @Transactional
  public void deleteOrganisationUnitLevel(OrganisationUnitLevel organisationUnitLevel) {
    organisationUnitLevelStore.delete(organisationUnitLevel);
  }

  @Override
  @Transactional
  public void deleteOrganisationUnitLevels() {
    organisationUnitLevelStore.deleteAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnitLevel> getOrganisationUnitLevels() {
    return ListUtils.sort(
        organisationUnitLevelStore.getAll(), OrganisationUnitLevelComparator.INSTANCE);
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationUnitLevel getOrganisationUnitLevelByLevel(int level) {
    return organisationUnitLevelStore.getByLevel(level);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnitLevel> getOrganisationUnitLevelByName(String name) {
    return new ArrayList<>(organisationUnitLevelStore.getAllEqName(name));
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnitLevel> getFilledOrganisationUnitLevels() {
    Map<Integer, OrganisationUnitLevel> levelMap = getOrganisationUnitLevelMap();

    List<OrganisationUnitLevel> levels = new ArrayList<>();

    int levelNo = getNumberOfOrganisationalLevels();

    for (int i = 0; i < levelNo; i++) {
      int level = i + 1;

      OrganisationUnitLevel filledLevel =
          ObjectUtils.firstNonNull(
              levelMap.get(level), new OrganisationUnitLevel(level, LEVEL_PREFIX + level));

      levels.add(filledLevel);
    }

    return levels;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Integer, OrganisationUnitLevel> getOrganisationUnitLevelMap() {
    Map<Integer, OrganisationUnitLevel> levelMap = new HashMap<>();

    List<OrganisationUnitLevel> levels = getOrganisationUnitLevels();

    for (OrganisationUnitLevel level : levels) {
      levelMap.put(level.getLevel(), level);
    }

    return levelMap;
  }

  @Override
  @Transactional(readOnly = true)
  public int getNumberOfOrganisationUnits() {
    return organisationUnitStore.getCount();
  }

  @Override
  @Transactional(readOnly = true)
  public int getOfflineOrganisationUnitLevels(User user) {
    // ---------------------------------------------------------------------
    // Get level from organisation unit of current user
    // ---------------------------------------------------------------------

    if (user != null && user.hasOrganisationUnit()) {
      OrganisationUnit organisationUnit = user.getOrganisationUnit();

      int level = organisationUnit.getLevel();

      OrganisationUnitLevel orgUnitLevel = getOrganisationUnitLevelByLevel(level);

      if (orgUnitLevel != null && orgUnitLevel.getOfflineLevels() != null) {
        return orgUnitLevel.getOfflineLevels();
      }
    }

    // ---------------------------------------------------------------------
    // Get level from system configuration
    // ---------------------------------------------------------------------

    OrganisationUnitLevel level =
        configurationService.getConfiguration().getOfflineOrganisationUnitLevel();

    if (level != null) {
      return level.getLevel();
    }

    // ---------------------------------------------------------------------
    // Get max level
    // ---------------------------------------------------------------------

    int max = getOrganisationUnitLevels().size();

    OrganisationUnitLevel maxLevel = getOrganisationUnitLevelByLevel(max);

    if (maxLevel != null) {
      return maxLevel.getLevel();
    }

    // ---------------------------------------------------------------------
    // Return 1 level as fall back
    // ---------------------------------------------------------------------

    return 1;
  }

  @Override
  @Transactional
  public void updatePaths() {
    organisationUnitStore.updatePaths();
  }

  @Override
  @Transactional
  public void forceUpdatePaths() {
    organisationUnitStore.forceUpdatePaths();
  }

  @Override
  public List<String> getOrganisationUnitsUidsByUser(String username) {
    return organisationUnitStore.getOrganisationUnitsUidsByUser(username);
  }

  @Override
  public List<String> getDataViewOrganisationUnitsUidsByUser(String username) {
    return organisationUnitStore.getDataViewOrganisationUnitsUidsByUser(username);
  }

  @Override
  public List<String> getSearchOrganisationUnitsUidsByUser(String username) {
    return organisationUnitStore.getSearchOrganisationUnitsUidsByUser(username);
  }

  @Override
  public List<OrganisationUnit> getByCategoryOption(Collection<UID> categoryOptions) {
    return organisationUnitStore.getByCategoryOption(UID.toValueList(categoryOptions));
  }

  @Override
  @Transactional(readOnly = true)
  public Integer getOrganisationUnitLevelByLevelOrUid(String level) {
    if (level.matches(ExpressionService.INT_EXPRESSION)) {
      int orgUnitLevel = Integer.parseInt(level);

      return orgUnitLevel > 0 ? orgUnitLevel : null;
    } else if (level.matches(ExpressionService.UID_EXPRESSION)) {
      OrganisationUnitLevel orgUnitLevel = getOrganisationUnitLevel(level);

      return orgUnitLevel != null ? orgUnitLevel.getLevel() : null;
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitWithinDistance(
      double longitude, double latitude, double distance) {
    List<OrganisationUnit> objects =
        organisationUnitStore.getWithinCoordinateArea(
            GeoUtils.getBoxShape(longitude, latitude, distance));

    // Go through the list and remove the ones located outside radius

    if (objects != null && !objects.isEmpty()) {
      Iterator<OrganisationUnit> iter = objects.iterator();

      Point2D centerPoint = new Point2D.Double(longitude, latitude);

      while (iter.hasNext()) {
        OrganisationUnit orgunit = iter.next();

        double distancebetween =
            GeoUtils.getDistanceBetweenTwoPoints(
                centerPoint,
                ValidationUtils.getCoordinatePoint2D(
                    GeoUtils.getCoordinatesFromGeometry(orgunit.getGeometry())));

        if (distancebetween > distance) {
          iter.remove();
        }
      }
    }

    return objects;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganisationUnit> getOrganisationUnitByCoordinate(
      double longitude, double latitude, String topOrgUnitUid, Integer targetLevel) {
    List<OrganisationUnit> orgUnits = new ArrayList<>();

    if (GeoUtils.checkGeoJsonPointValid(longitude, latitude)) {
      OrganisationUnit topOrgUnit = null;

      if (topOrgUnitUid != null && !topOrgUnitUid.isEmpty()) {
        topOrgUnit = getOrganisationUnit(topOrgUnitUid);
      } else {
        // Get top search point through top level org unit which
        // contains coordinate

        List<OrganisationUnit> orgUnitsTopLevel =
            getTopLevelOrgUnitWithPoint(
                longitude, latitude, 1, getNumberOfOrganisationalLevels() - 1);

        if (orgUnitsTopLevel.size() == 1) {
          topOrgUnit = orgUnitsTopLevel.iterator().next();
        }
      }

      // Search child org units to get lowest level org unit with coordinate

      if (topOrgUnit != null) {
        List<OrganisationUnit> orgUnitChildren;

        if (targetLevel != null) {
          orgUnitChildren = getOrganisationUnitsAtLevel(targetLevel, topOrgUnit);
        } else {
          orgUnitChildren = getOrganisationUnitWithChildren(topOrgUnit.getId());
        }

        FilterUtils.filter(
            orgUnitChildren,
            new OrganisationUnitPolygonCoveringCoordinateFilter(longitude, latitude));

        // Get org units with lowest level

        int bottomLevel = topOrgUnit.getLevel();

        for (OrganisationUnit ou : orgUnitChildren) {
          if (ou.getLevel() > bottomLevel) {
            bottomLevel = ou.getLevel();
          }
        }

        for (OrganisationUnit ou : orgUnitChildren) {
          if (ou.getLevel() == bottomLevel) {
            orgUnits.add(ou);
          }
        }
      }
    }

    return orgUnits;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Searches organisation units until finding one with polygon containing point.
   *
   * @param longitude the longitude.
   * @param latitude the latitude.
   * @param searchLevel the search level.
   * @param stopLevel the stop level.
   */
  private List<OrganisationUnit> getTopLevelOrgUnitWithPoint(
      double longitude, double latitude, int searchLevel, int stopLevel) {
    for (int i = searchLevel; i <= stopLevel; i++) {
      List<OrganisationUnit> unitsAtLevel = new ArrayList<>(getOrganisationUnitsAtLevel(i));
      FilterUtils.filter(
          unitsAtLevel, new OrganisationUnitPolygonCoveringCoordinateFilter(longitude, latitude));

      if (!unitsAtLevel.isEmpty()) {
        return unitsAtLevel;
      }
    }

    return new ArrayList<>();
  }
}
