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
package org.hisp.dhis.orgunitprofile.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.orgunitprofile.OrgUnitInfo;
import org.hisp.dhis.orgunitprofile.OrgUnitProfile;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileData;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileService;
import org.hisp.dhis.orgunitprofile.ProfileItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DefaultOrgUnitProfileService implements OrgUnitProfileService {
  private static final String ORG_UNIT_PROFILE_NAMESPACE = "ORG_UNIT_PROFILE";

  private static final String ORG_UNIT_PROFILE_KEY = "ORG_UNIT_PROFILE";

  private static final String ORG_UNIT_PROFILE_AUTHORITY = "F_ORG_UNIT_PROFILE_ADD";

  private static final List<Class<? extends IdentifiableObject>> DATA_ITEM_CLASSES =
      ImmutableList.<Class<? extends IdentifiableObject>>builder()
          .add(DataElement.class)
          .add(Indicator.class)
          .add(DataSet.class)
          .add(ProgramIndicator.class)
          .build();

  private DatastoreService dataStore;

  private IdentifiableObjectManager idObjectManager;

  private AnalyticsService analyticsService;

  private OrganisationUnitGroupService groupService;

  private OrganisationUnitService organisationUnitService;

  private ObjectMapper jsonMapper;

  public DefaultOrgUnitProfileService(
      DatastoreService dataStore,
      IdentifiableObjectManager idObjectManager,
      AnalyticsService analyticsService,
      OrganisationUnitGroupService groupService,
      OrganisationUnitService organisationUnitService,
      ObjectMapper jsonMapper) {
    this.dataStore = dataStore;
    this.idObjectManager = idObjectManager;
    this.analyticsService = analyticsService;
    this.groupService = groupService;
    this.organisationUnitService = organisationUnitService;
    this.jsonMapper = jsonMapper;

    this.dataStore.addProtection(
        new DatastoreNamespaceProtection(
            ORG_UNIT_PROFILE_NAMESPACE,
            DatastoreNamespaceProtection.ProtectionType.NONE,
            DatastoreNamespaceProtection.ProtectionType.RESTRICTED,
            false,
            "ALL",
            ORG_UNIT_PROFILE_AUTHORITY));
  }

  @Override
  @Transactional
  public void saveOrgUnitProfile(OrgUnitProfile profile) {
    DatastoreEntry entry = new DatastoreEntry(ORG_UNIT_PROFILE_KEY, ORG_UNIT_PROFILE_NAMESPACE);

    try {
      entry.setValue(jsonMapper.writeValueAsString(profile));
      dataStore.saveOrUpdateEntry(entry);
    } catch (JsonProcessingException e) {
      log.error(DebugUtils.getStackTrace(e));
      throw new IllegalArgumentException(
          String.format("Can't serialize OrgUnitProfile: %s", profile));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateOrgUnitProfile(OrgUnitProfile profile) {
    List<ErrorReport> errorReports = new ArrayList<>();
    errorReports.addAll(validateAttributes(profile.getAttributes()));
    errorReports.addAll(validateDataItems(profile.getDataItems()));
    errorReports.addAll(validateGroupSets(profile.getGroupSets()));

    return errorReports;
  }

  @Override
  @Transactional(readOnly = true)
  public OrgUnitProfile getOrgUnitProfile() {
    DatastoreEntry value = dataStore.getEntry(ORG_UNIT_PROFILE_NAMESPACE, ORG_UNIT_PROFILE_KEY);

    if (value == null) {
      return new OrgUnitProfile();
    }

    try {
      return jsonMapper.readValue(value.getValue(), OrgUnitProfile.class);
    } catch (JsonProcessingException ex) {
      log.error(DebugUtils.getStackTrace(ex));
      throw new IllegalArgumentException("Cannot deserialize org unit profile", ex);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public OrgUnitProfileData getOrgUnitProfileData(String orgUnit, @Nullable String isoPeriod) {
    // If profile is empty, only fixed info will be included

    OrgUnitProfile profile = getOrgUnitProfile();

    OrganisationUnit unit = getOrgUnit(orgUnit);

    Period period = getPeriod(isoPeriod);

    OrgUnitProfileData data = new OrgUnitProfileData();

    data.setInfo(getOrgUnitInfo(unit));
    data.setAttributes(getAttributes(profile, unit));
    data.setGroupSets(getGroupSets(profile, unit));
    data.setDataItems(getDataItems(profile, unit, period));

    return data;
  }

  /** Retrieves basic profile info of given {@link OrganisationUnit}. */
  private OrgUnitInfo getOrgUnitInfo(OrganisationUnit orgUnit) {
    OrgUnitInfo info = new OrgUnitInfo();

    info.setId(orgUnit.getUid());
    info.setCode(orgUnit.getCode());
    info.setName(orgUnit.getDisplayName());
    info.setShortName(orgUnit.getDisplayShortName());
    info.setDescription(orgUnit.getDisplayDescription());
    info.setParentName(orgUnit.isRoot() ? null : orgUnit.getParent().getDisplayName());
    info.setLevel(orgUnit.getLevel());
    info.setLevelName(getOrgUnitLevelName(orgUnit.getLevel()));
    info.setOpeningDate(orgUnit.getOpeningDate());
    info.setClosedDate(orgUnit.getClosedDate());
    info.setComment(orgUnit.getComment());
    info.setUrl(orgUnit.getUrl());
    info.setContactPerson(orgUnit.getContactPerson());
    info.setAddress(orgUnit.getAddress());
    info.setEmail(orgUnit.getEmail());
    info.setPhoneNumber(orgUnit.getPhoneNumber());
    info.setFeatureType(orgUnit.getFeatureType());

    if (orgUnit.getGeometry() != null) {
      if (orgUnit.getGeometry().getGeometryType().equals(FeatureType.POINT.value())) {
        info.setLongitude(orgUnit.getGeometry().getCoordinate().x);
        info.setLatitude(orgUnit.getGeometry().getCoordinate().y);
      }
    }

    if (orgUnit.getImage() != null) {
      info.setImageId(orgUnit.getImage().getUid());
    }

    return info;
  }

  /**
   * Retrieves a list of attribute data items for the given org unit profile and org unit.
   *
   * @param profile the {@link OrganisationUnitProfile}.
   * @param orgUnit the {@link OrganisationUnit}.
   * @return a list of {@link ProfileItem}.
   */
  private List<ProfileItem> getAttributes(OrgUnitProfile profile, OrganisationUnit orgUnit) {
    if (CollectionUtils.isEmpty(profile.getAttributes())) {
      return ImmutableList.of();
    }

    List<Attribute> attributes = idObjectManager.getByUid(Attribute.class, profile.getAttributes());

    if (CollectionUtils.isEmpty(attributes)) {
      return ImmutableList.of();
    }

    List<ProfileItem> items = new ArrayList<>();

    for (Attribute attribute : attributes) {
      AttributeValue attributeValue = orgUnit.getAttributeValue(attribute);

      if (attributeValue != null) {
        items.add(
            new ProfileItem(
                attribute.getUid(), attribute.getDisplayName(), attributeValue.getValue()));
      }
    }

    return items;
  }

  /**
   * Retrieves a list of org unit group set data items for the given org unit profile and org unit.
   *
   * @param profile the {@link OrganisationUnitProfile}.
   * @param orgUnit the {@link OrganisationUnit}.
   * @return a list of {@link ProfileItem}.
   */
  private List<ProfileItem> getGroupSets(OrgUnitProfile profile, OrganisationUnit orgUnit) {
    if (CollectionUtils.isEmpty(profile.getGroupSets())) {
      return ImmutableList.of();
    }

    List<OrganisationUnitGroupSet> groupSets =
        idObjectManager.getByUid(OrganisationUnitGroupSet.class, profile.getGroupSets());

    if (CollectionUtils.isEmpty(groupSets)) {
      return ImmutableList.of();
    }

    List<ProfileItem> items = new ArrayList<>();

    Set<OrganisationUnitGroup> groups = orgUnit.getGroups();

    if (CollectionUtils.isEmpty(groups)) {
      return ImmutableList.of();
    }

    for (OrganisationUnitGroupSet groupSet : groupSets) {
      OrganisationUnitGroup group = groupService.getOrgUnitGroupInGroupSet(groups, groupSet);

      if (group != null) {
        items.add(
            new ProfileItem(groupSet.getUid(), groupSet.getDisplayName(), group.getDisplayName()));
      }
    }

    return items;
  }

  /**
   * Retrieves a list of data items for the given org unit profile and org unit. A data item can be
   * of type data element, indicator, data set and program indicator. Data element can be of type
   * aggregate and tracker.
   *
   * @param profile the {@link OrganisationUnitProfile}.
   * @param orgUnit the {@link OrganisationUnit}.
   * @param period the {@link Period}.
   * @return a list of {@link ProfileItem}.
   */
  private List<ProfileItem> getDataItems(
      OrgUnitProfile profile, OrganisationUnit orgUnit, Period period) {
    if (CollectionUtils.isEmpty(profile.getDataItems())) {
      return ImmutableList.of();
    }

    List<DimensionalItemObject> dataItems =
        idObjectManager.getByUid(DATA_ITEM_CLASSES, profile.getDataItems());

    if (CollectionUtils.isEmpty(dataItems)) {
      return ImmutableList.of();
    }

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(dataItems)
            .withFilterOrganisationUnit(orgUnit)
            .withFilterPeriod(period)
            .build();

    Map<String, Object> values = analyticsService.getAggregatedDataValueMapping(params);

    if (MapUtils.isEmpty(values)) {
      return ImmutableList.of();
    }

    List<ProfileItem> items = new ArrayList<>();

    for (DimensionalItemObject dataItem : dataItems) {
      Object value = values.get(dataItem.getUid());

      if (value != null) {
        items.add(new ProfileItem(dataItem.getUid(), dataItem.getDisplayName(), value));
      }
    }

    return items;
  }

  /**
   * Retrieves the org unit with the given identifier.
   *
   * @return an {@link OrganisationUnit}.
   * @throws IllegalQueryException if not found.
   */
  private OrganisationUnit getOrgUnit(String orgUnit) {
    OrganisationUnit unit = idObjectManager.get(OrganisationUnit.class, orgUnit);

    if (unit == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1102, orgUnit));
    }

    return unit;
  }

  /**
   * Returns the a period based on the given ISO period string. If the ISO period is not defined or
   * invalid, the current year is used as fall back.
   *
   * @param isoPeriod the ISO period string, can be null.
   * @return a {@link Period}.
   */
  private Period getPeriod(String isoPeriod) {
    Period period = PeriodType.getPeriodFromIsoString(isoPeriod);

    if (period != null) {
      return period;
    } else {
      return RelativePeriods.getRelativePeriodsFromEnum(RelativePeriodEnum.THIS_YEAR, new Date())
          .get(0);
    }
  }

  /**
   * Returns the name of the org unit level corresponding to the numeric level.
   *
   * @param level the numeric org unit level.
   * @return the org unit level name, or null if not exists.
   */
  private String getOrgUnitLevelName(Integer level) {
    OrganisationUnitLevel orgUnitLevel =
        organisationUnitService.getOrganisationUnitLevelByLevel(level);

    return orgUnitLevel != null ? orgUnitLevel.getDisplayName() : null;
  }

  /**
   * Validates the list of org unit group set identifiers.
   *
   * @param groupSets the list of org unit group set identifiers.
   * @return a list of {@link ErrorReport}.
   */
  private List<ErrorReport> validateGroupSets(List<String> groupSets) {
    if (CollectionUtils.isEmpty(groupSets)) {
      return ImmutableList.of();
    }

    List<ErrorReport> errorReports = new ArrayList<>();

    for (String groupSetId : groupSets) {
      if (idObjectManager.get(OrganisationUnitGroupSet.class, groupSetId) == null) {
        errorReports.add(
            new ErrorReport(
                OrganisationUnitGroupSet.class, ErrorCode.E4014, groupSetId, "groupSets"));
      }
    }

    return errorReports;
  }

  /**
   * Validates the list of data item identifiers.
   *
   * @param dataItems the list of data item identifiers.
   * @return a list of {@link ErrorReport}.
   */
  private List<ErrorReport> validateDataItems(List<String> dataItems) {
    if (CollectionUtils.isEmpty(dataItems)) {
      return ImmutableList.of();
    }

    List<ErrorReport> errorReports = new ArrayList<>();

    for (String dataItemId : dataItems) {
      IdentifiableObject dataItem = idObjectManager.get(DATA_ITEM_CLASSES, dataItemId);

      if (dataItem == null) {
        errorReports.add(
            new ErrorReport(Collection.class, ErrorCode.E4014, dataItemId, "dataItems"));
      }

      if (dataItem != null
          && DataElement.class.isAssignableFrom(dataItem.getClass())
          && !((DataElement) dataItem).getValueType().isAggregatable()) {
        errorReports.add(new ErrorReport(DataElement.class, ErrorCode.E7115, dataItemId));
      }
    }

    return errorReports;
  }

  /**
   * Validates the list of attribute identifiers.
   *
   * @param attributes the list ofattributes identifiers.
   * @return a list of {@link ErrorReport}.
   */
  private List<ErrorReport> validateAttributes(List<String> attributes) {
    if (CollectionUtils.isEmpty(attributes)) {
      return ImmutableList.of();
    }

    List<ErrorReport> errorReports = new ArrayList<>();

    for (String attributeId : attributes) {
      if (idObjectManager.get(Attribute.class, attributeId) == null) {
        errorReports.add(
            new ErrorReport(Attribute.class, ErrorCode.E4014, attributeId, "attributes"));
      }
    }

    return errorReports;
  }
}
