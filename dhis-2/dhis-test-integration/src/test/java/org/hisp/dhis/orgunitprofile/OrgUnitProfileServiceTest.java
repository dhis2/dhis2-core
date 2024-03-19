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
package org.hisp.dhis.orgunitprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.orgunitprofile.impl.DefaultOrgUnitProfileService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class OrgUnitProfileServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private OrgUnitProfileService service;

  @Autowired private UserService _userService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DatastoreService dataStore;

  private AnalyticsService mockAnalyticsService;

  @Autowired private ObjectMapper jsonMapper;

  private OrgUnitProfileService mockService;

  @Override
  public void setUpTest() {
    userService = _userService;
    createAndInjectAdminUser();
    mockAnalyticsService = Mockito.mock(AnalyticsService.class);
    mockService =
        new DefaultOrgUnitProfileService(
            dataStore,
            manager,
            mockAnalyticsService,
            organisationUnitGroupService,
            organisationUnitService,
            jsonMapper);
  }

  @Test
  void testSave() {
    OrgUnitProfile orgUnitProfile =
        createOrgUnitProfile(
            Lists.newArrayList("Attribute1", "Attribute2"),
            Lists.newArrayList("GroupSet1", "GroupSet2"),
            Lists.newArrayList("DataItem1", "DataItem2"));
    service.saveOrgUnitProfile(orgUnitProfile);

    OrgUnitProfile savedProfile = service.getOrgUnitProfile();
    assertEquals(2, savedProfile.getAttributes().size());
    assertEquals(2, savedProfile.getDataItems().size());
    assertEquals(2, savedProfile.getGroupSets().size());
    assertTrue(savedProfile.getAttributes().contains("Attribute1"));
    assertTrue(savedProfile.getDataItems().contains("DataItem2"));
    assertTrue(savedProfile.getGroupSets().contains("GroupSet1"));
  }

  @Test
  void testUpdateOrgUnitProfile() {
    OrgUnitProfile orgUnitProfile =
        createOrgUnitProfile(
            Lists.newArrayList("Attribute1", "Attribute2"),
            Lists.newArrayList("GroupSet1", "GroupSet2"),
            Lists.newArrayList("DataItem1", "DataItem2"));
    service.saveOrgUnitProfile(orgUnitProfile);

    orgUnitProfile.getGroupSets().clear();
    orgUnitProfile.getDataItems().remove("DataItem2");
    orgUnitProfile.getAttributes().remove("Attribute1");
    orgUnitProfile.getAttributes().add("Attribute3");

    service.saveOrgUnitProfile(orgUnitProfile);

    assertEquals(2, orgUnitProfile.getAttributes().size());
    assertEquals(1, orgUnitProfile.getDataItems().size());
    assertEquals(0, orgUnitProfile.getGroupSets().size());
    assertTrue(orgUnitProfile.getAttributes().contains("Attribute3"));
    assertTrue(orgUnitProfile.getDataItems().contains("DataItem1"));
  }

  @Test
  void testGetProfileDataWithoutOrgUnitProfile() {
    Attribute attribute = createAttribute('A');
    attribute.setOrganisationUnitAttribute(true);
    manager.save(attribute);

    OrganisationUnit orgUnit = createOrganisationUnit("A");
    orgUnit.getAttributeValues().add(new AttributeValue("testAttributeValue", attribute));
    manager.save(orgUnit);

    OrganisationUnitGroup group = createOrganisationUnitGroup('A');
    group.addOrganisationUnit(orgUnit);
    manager.save(group);

    OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet('A');
    groupSet.addOrganisationUnitGroup(group);
    manager.save(groupSet);

    DataElement dataElement = createDataElement('A');
    manager.save(dataElement);

    Period period = createPeriod("202106");
    manager.save(period);

    // Mock analytic query for data value
    Map<String, Object> mapDataItem = new HashMap<>();
    mapDataItem.put(dataElement.getUid(), "testDataValue");
    Mockito.when(mockAnalyticsService.getAggregatedDataValueMapping(any(DataQueryParams.class)))
        .thenReturn(mapDataItem);

    OrgUnitProfileData data =
        mockService.getOrgUnitProfileData(orgUnit.getUid(), period.getIsoDate());

    assertEquals(0, data.getAttributes().size());
    assertEquals(0, data.getDataItems().size());
    assertEquals(0, data.getGroupSets().size());
    assertEquals(orgUnit.getCode(), data.getInfo().getCode());
    assertEquals(orgUnit.getName(), data.getInfo().getName());
  }

  @Test
  void testGetProfileDataWithOrgUnitProfile() {
    Attribute attribute = createAttribute('A');
    attribute.setOrganisationUnitAttribute(true);
    manager.save(attribute);

    OrganisationUnit orgUnit = createOrganisationUnit("A");
    orgUnit.getAttributeValues().add(new AttributeValue("testAttributeValue", attribute));
    manager.save(orgUnit);

    OrganisationUnitGroup group = createOrganisationUnitGroup('A');
    group.addOrganisationUnit(orgUnit);
    manager.save(group);

    OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet('A');
    groupSet.addOrganisationUnitGroup(group);
    manager.save(groupSet);

    DataElement dataElement = createDataElement('A');
    manager.save(dataElement);

    Period period = createPeriod("202106");
    manager.save(period);

    OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
    orgUnitProfile.getAttributes().add(attribute.getUid());
    orgUnitProfile.getDataItems().add(dataElement.getUid());
    orgUnitProfile.getGroupSets().add(groupSet.getUid());
    service.saveOrgUnitProfile(orgUnitProfile);

    // Mock analytic query for data value
    Map<String, Object> mapDataItem = new HashMap<>();
    mapDataItem.put(dataElement.getUid(), "testDataValue");
    Mockito.when(mockAnalyticsService.getAggregatedDataValueMapping(any(DataQueryParams.class)))
        .thenReturn(mapDataItem);

    OrgUnitProfileData data =
        mockService.getOrgUnitProfileData(orgUnit.getUid(), period.getIsoDate());

    assertEquals("testAttributeValue", data.getAttributes().get(0).getValue());
    assertEquals("testDataValue", data.getDataItems().get(0).getValue());
    assertEquals(group.getDisplayName(), data.getGroupSets().get(0).getValue());
  }

  @Test
  void testValidator() {
    Attribute attribute = createAttribute('A');
    attribute.setOrganisationUnitAttribute(true);

    OrganisationUnit orgUnit = createOrganisationUnit("A");
    orgUnit.getAttributeValues().add(new AttributeValue("testAttributeValue", attribute));

    OrganisationUnitGroup group = createOrganisationUnitGroup('A');
    group.addOrganisationUnit(orgUnit);

    OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet('A');
    groupSet.addOrganisationUnitGroup(group);

    DataElement dataElement = createDataElement('A');

    OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
    orgUnitProfile.getAttributes().add(attribute.getUid());
    orgUnitProfile.getDataItems().add(dataElement.getUid());
    orgUnitProfile.getGroupSets().add(groupSet.getUid());
    List<ErrorReport> errors = service.validateOrgUnitProfile(orgUnitProfile);
    assertEquals(3, errors.size());
    assertTrue(
        errorContains(errors, ErrorCode.E4014, OrganisationUnitGroupSet.class, groupSet.getUid()));
    assertTrue(errorContains(errors, ErrorCode.E4014, Attribute.class, attribute.getUid()));
    assertTrue(errorContains(errors, ErrorCode.E4014, Collection.class, dataElement.getUid()));
  }

  @Test
  void testValidateNonAggregateableDataElement() {
    DataElement deA = createDataElement('A');
    deA.setValueType(ValueType.NUMBER);
    DataElement deB = createDataElement('B');
    deB.setValueType(ValueType.DATE);

    manager.save(deA);
    manager.save(deB);

    OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
    orgUnitProfile.getDataItems().add(deA.getUid());
    orgUnitProfile.getDataItems().add(deB.getUid());

    List<ErrorReport> errors = service.validateOrgUnitProfile(orgUnitProfile);
    assertEquals(1, errors.size());
    assertTrue(errorContains(errors, ErrorCode.E7115, DataElement.class, deB.getUid()));
  }

  @Test
  void testDeletionHandling() {
    OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet('A');

    manager.save(groupSet);

    OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
    orgUnitProfile.getGroupSets().add(groupSet.getUid());

    assertTrue(orgUnitProfile.getGroupSets().contains(groupSet.getUid()));

    service.saveOrgUnitProfile(orgUnitProfile);

    manager.delete(groupSet);

    orgUnitProfile = service.getOrgUnitProfile();

    assertFalse(orgUnitProfile.getGroupSets().contains(groupSet.getUid()));
  }

  private boolean errorContains(
      List<ErrorReport> errors, ErrorCode errorCode, Class<?> clazz, String uid) {
    return errors.stream()
        .filter(
            errorReport ->
                errorReport.getErrorCode() == errorCode
                    && errorReport.getMainKlass().isAssignableFrom(clazz)
                    && errorReport.getMessage().contains(uid))
        .findFirst()
        .isPresent();
  }

  private OrgUnitProfile createOrgUnitProfile(
      List<String> attributes, List<String> groupSets, List<String> dataItems) {
    OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
    orgUnitProfile.getAttributes().addAll(attributes);
    orgUnitProfile.getDataItems().addAll(dataItems);
    orgUnitProfile.getGroupSets().addAll(groupSets);
    return orgUnitProfile;
  }
}
