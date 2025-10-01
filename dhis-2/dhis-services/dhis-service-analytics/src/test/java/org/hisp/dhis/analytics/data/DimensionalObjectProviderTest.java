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
package org.hisp.dhis.analytics.data;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.hisp.dhis.analytics.AnalyticsConstants.KEY_DATASET;
import static org.hisp.dhis.analytics.AnalyticsConstants.KEY_PROGRAM;
import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_APRIL;
import static org.hisp.dhis.analytics.DataQueryParams.DYNAMIC_DIM_CLASSES;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DisplayProperty.SHORTNAME;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.feedback.ErrorCode.E7124;
import static org.hisp.dhis.test.TestBase.createCategory;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createIndicator;
import static org.hisp.dhis.test.TestBase.createIndicatorType;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createOrganisationUnitGroup;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.processing.MetadataDimensionsHandler;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DimensionalObjectProvider}.
 *
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class DimensionalObjectProviderTest {

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private DimensionService dimensionService;

  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private SystemSettings settings;

  @Mock private AclService aclService;

  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  @Mock private I18nFormat i18nFormat;

  @InjectMocks private DimensionalObjectProvider target;

  @BeforeEach
  public void setUp() {
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(settings);
  }

  @Test
  void testGetDimensionForIndicator() {
    Indicator indicator = createIndicator('A', createIndicatorType('A'));
    String indicatorGroupUid = "oehv9EO3vP7";
    IndicatorGroup indicatorGroup = new IndicatorGroup("dummy");
    indicatorGroup.setUid(indicatorGroupUid);
    indicatorGroup.setCode("CODE_10");
    indicatorGroup.setMembers(Set.of(indicator));
    when(idObjectManager.getObject(IndicatorGroup.class, UID, indicatorGroupUid))
        .thenReturn(indicatorGroup);

    List<String> itemsUid = List.of("IN_GROUP-" + indicatorGroupUid, "IN_GROUP-Behv9EO3hR1");

    DimensionalObject dimensionalObject = target.getDimension(itemsUid, UID);

    assertEquals("dx", dimensionalObject.getDimension());
    assertEquals("dx", dimensionalObject.getUid());
    assertEquals(DATA_X, dimensionalObject.getDimensionType());
    assertEquals("Data", dimensionalObject.getDimensionDisplayName());

    Indicator refIndicator = (Indicator) dimensionalObject.getItems().get(0);
    assertBaseDimensionalObjects(indicator, refIndicator);

    DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
    Keyword refKeyword = refDimensionKeywords.getKeywords().get(0);
    assertEquals(refKeyword.getKey(), indicatorGroup.getUid());
    assertEquals(refKeyword.getMetadataItem().getName(), indicatorGroup.getName());
    assertEquals(refKeyword.getMetadataItem().getCode(), indicatorGroup.getCode());
    assertEquals(refKeyword.getMetadataItem().getUid(), indicatorGroup.getUid());
  }

  @Test
  void testGetDimensionForDataElement() {
    DataElement dataElement = createDataElement('A');
    String deGroupUid = "oehv9EO3vP7";
    DataElementGroup dataElementGroup = new DataElementGroup("dummy");
    dataElementGroup.setUid(deGroupUid);
    dataElementGroup.setCode("CODE_10");
    dataElementGroup.setMembers(Set.of(dataElement, new DataElement()));

    when(idObjectManager.getObject(DataElementGroup.class, NAME, deGroupUid))
        .thenReturn(dataElementGroup);

    List<String> itemsUid = List.of("DE_GROUP-" + deGroupUid, "DE_GROUP-Behv9EO3hR1");

    DimensionalObject dimensionalObject = target.getDimension(itemsUid, NAME);

    assertEquals("dx", dimensionalObject.getDimension());
    assertEquals("dx", dimensionalObject.getUid());
    assertEquals(DATA_X, dimensionalObject.getDimensionType());
    assertEquals("Data", dimensionalObject.getDimensionDisplayName());

    DataElement refDataElement =
        (DataElement)
            dimensionalObject.getItems().stream().filter(de -> de.getUid() != null).toList().get(0);

    assertBaseDimensionalObjects(dataElement, refDataElement);

    DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
    assertKeywordForDimensionalObject(refDimensionKeywords.getKeywords().get(0), dataElementGroup);
  }

  @Test
  void testGetDimensionWhenDataDimensionsAreNotFound() {
    List<String> nonExistingItemsUid = List.of("DE_GROUP-Achv9EO3hR1", "IN_GROUP-Behv9EO3hR1");

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class, () -> target.getDimension(nonExistingItemsUid, UID));
    assertEquals(E7124, ex.getErrorCode());
  }

  @Test
  void testGetOrgUnitDimensionWithNoLevelsNoGroup() {
    OrganisationUnit level2Ou1 = createOrganisationUnit('A');
    OrganisationUnit level2Ou2 = createOrganisationUnit('B');
    OrganisationUnit ou1 = createOrganisationUnit('A');
    OrganisationUnit ou2 = createOrganisationUnit('B');
    List<OrganisationUnit> organisationUnits =
        new ArrayList<>(asList(level2Ou1, level2Ou2, ou1, ou2));

    when(organisationUnitService.getOrganisationUnitsAtLevels(anyList(), anyList()))
        .thenReturn(new ArrayList<>(asList(ou1, ou2)));

    List<String> itemsUid =
        List.of(
            "USER_ORGUNIT",
            "USER_ORGUNIT_CHILDREN",
            "USER_ORGUNIT_GRANDCHILDREN",
            "LEVEL-" + ou1.getUid(),
            "OU_GROUP-" + ou2.getUid());

    DimensionalObject dimensionalObject =
        target.getOrgUnitDimension(itemsUid, SHORTNAME, organisationUnits, UID);

    assertEquals("ou", dimensionalObject.getDimension());
    assertEquals("ou", dimensionalObject.getUid());
    assertEquals(ORGANISATION_UNIT, dimensionalObject.getDimensionType());
    assertEquals("Organisation unit", dimensionalObject.getDimensionDisplayName());
    assertTrue(dimensionalObject.getDimensionItemKeywords().isEmpty());

    sort(dimensionalObject.getItems());
    OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get(0);
    OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get(1);
    assertOrgUnits(ou1, refOu1);
    assertOrgUnits(ou2, refOu2);
  }

  @Test
  void testGetOrgUnitDimensionWithWithLevelAndGroup() {
    OrganisationUnitGroup organisationUnitGroup = createOrganisationUnitGroup('A');
    OrganisationUnit level2Ou1 = createOrganisationUnit('A');
    OrganisationUnit level2Ou2 = createOrganisationUnit('B');
    OrganisationUnit ou1 = createOrganisationUnit('C');
    OrganisationUnit ou2 = createOrganisationUnit('D');
    List<OrganisationUnit> organisationUnits =
        new ArrayList<>(asList(level2Ou1, level2Ou2, ou1, ou2));

    when(organisationUnitService.getOrganisationUnitsAtLevels(anyList(), anyList()))
        .thenReturn(new ArrayList<>(asList(level2Ou1, level2Ou2)));
    when(organisationUnitService.getOrganisationUnitLevelByLevelOrUid(level2Ou1.getName()))
        .thenReturn(1);
    when(idObjectManager.getObject(OrganisationUnitGroup.class, UID, level2Ou2.getUid()))
        .thenReturn(organisationUnitGroup);

    List<String> itemsUid =
        List.of(
            "USER_ORGUNIT",
            "USER_ORGUNIT_CHILDREN",
            "USER_ORGUNIT_GRANDCHILDREN",
            "LEVEL-" + level2Ou1.getName(),
            "OU_GROUP-" + level2Ou2.getUid());

    DimensionalObject dimensionalObject =
        target.getOrgUnitDimension(itemsUid, DisplayProperty.NAME, organisationUnits, UID);

    assertEquals("ou", dimensionalObject.getDimension());
    assertEquals("ou", dimensionalObject.getUid());
    assertEquals(ORGANISATION_UNIT, dimensionalObject.getDimensionType());
    assertEquals("Organisation unit", dimensionalObject.getDimensionDisplayName());

    sort(dimensionalObject.getItems());
    OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get(0);
    OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get(1);
    assertOrgUnits(level2Ou1, refOu1);
    assertOrgUnits(level2Ou2, refOu2);

    DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
    assertKeywordForDimensionalObject(
        refDimensionKeywords.getKeywords().get(0), organisationUnitGroup);
    assertKeywordForDimensionalObject(refDimensionKeywords.getKeywords().get(1), level2Ou1);
    assertKeywordForDimensionalObject(refDimensionKeywords.getKeywords().get(2), level2Ou2);
    assertKeywordForDimensionalObject(refDimensionKeywords.getKeywords().get(3), ou1);
    assertKeywordForDimensionalObject(refDimensionKeywords.getKeywords().get(4), ou2);
  }

  @Test
  void testGetOrgUnitDimensionWithWithDataSet() {
    // Given
    OrganisationUnit ou1 = createOrganisationUnit('A');
    OrganisationUnit ou2 = createOrganisationUnit('B');
    List<OrganisationUnit> organisationUnits = new ArrayList<>(asList(ou1, ou2));
    List<String> itemsUid = List.of("DS-lyLU2wR22tC");

    // When
    when(organisationUnitService.getDataSetOrganisationUnits(
            substringAfter("DS-lyLU2wR22tC", KEY_DATASET)))
        .thenReturn(new ArrayList<>(asList(ou1, ou2)));

    DimensionalObject dimensionalObject =
        target.getOrgUnitDimension(itemsUid, DisplayProperty.NAME, organisationUnits, UID);

    // Then
    assertEquals("ou", dimensionalObject.getDimension());
    assertEquals("ou", dimensionalObject.getUid());
    assertEquals(ORGANISATION_UNIT, dimensionalObject.getDimensionType());
    assertEquals("Organisation unit", dimensionalObject.getDimensionDisplayName());

    sort(dimensionalObject.getItems());
    OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get(0);
    OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get(1);
    assertOrgUnits(ou1, refOu1);
    assertOrgUnits(ou2, refOu2);
  }

  @Test
  void testGetOrgUnitDimensionWithWithProgram() {
    // Given
    OrganisationUnit ou1 = createOrganisationUnit('A');
    OrganisationUnit ou2 = createOrganisationUnit('B');
    List<OrganisationUnit> organisationUnits = new ArrayList<>(asList(ou1, ou2));
    List<String> itemsUid = List.of("PR-lxAQ7Zs9VYR");

    // When
    when(organisationUnitService.getProgramOrganisationUnits(
            substringAfter("PR-lxAQ7Zs9VYR", KEY_PROGRAM)))
        .thenReturn(new ArrayList<>(asList(ou1, ou2)));

    DimensionalObject dimensionalObject =
        target.getOrgUnitDimension(itemsUid, DisplayProperty.NAME, organisationUnits, UID);

    // Then
    assertEquals("ou", dimensionalObject.getDimension());
    assertEquals("ou", dimensionalObject.getUid());
    assertEquals(ORGANISATION_UNIT, dimensionalObject.getDimensionType());
    assertEquals("Organisation unit", dimensionalObject.getDimensionDisplayName());

    sort(dimensionalObject.getItems());
    OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get(0);
    OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get(1);
    assertOrgUnits(ou1, refOu1);
    assertOrgUnits(ou2, refOu2);
  }

  @Test
  void testGetDimensionWhenOrgUnitsAreNotFound() {
    List<String> nonExistingItemsUid = List.of("LEVEL-Achv9EO3hR1", "OU_GROUP-Blhv9EO3hR1");

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class, () -> target.getDimension(nonExistingItemsUid, UID));
    assertEquals(E7124, ex.getErrorCode());
  }

  @Test
  void testGetOrgUnitGroupDimension() {
    OrganisationUnitGroup organisationUnitGroup1 = createOrganisationUnitGroup('A');
    OrganisationUnitGroup organisationUnitGroup2 = createOrganisationUnitGroup('B');

    when(idObjectManager.getObject(OrganisationUnitGroup.class, UID, "Achv9EO3hR1"))
        .thenReturn(organisationUnitGroup1);
    when(idObjectManager.getObject(OrganisationUnitGroup.class, UID, "Blhv9EO3hR1"))
        .thenReturn(organisationUnitGroup2);

    List<String> itemsUid = List.of("Achv9EO3hR1", "Blhv9EO3hR1");

    DimensionalObject dimensionalObject = target.getOrgUnitGroupDimension(itemsUid, UID);

    assertEquals("oug", dimensionalObject.getDimension());
    assertEquals("oug", dimensionalObject.getUid());
    assertEquals(ORGANISATION_UNIT_GROUP, dimensionalObject.getDimensionType());
    assertEquals("Organisation unit group", dimensionalObject.getDimensionDisplayName());

    assertNull(dimensionalObject.getDimensionItemKeywords());
    assertBaseDimensionalObjects(organisationUnitGroup1, dimensionalObject.getItems().get(0));
    assertBaseDimensionalObjects(organisationUnitGroup2, dimensionalObject.getItems().get(1));
  }

  @Test
  void testGetPeriodDimensions() {
    List<String> itemsUid = List.of("LAST_YEAR:LAST_UPDATED", "LAST_5_YEARS:SCHEDULED_DATE");

    when(settings.getAnalyticsFinancialYearStart()).thenReturn(FINANCIAL_YEAR_APRIL);
    when(i18nManager.getI18nFormat()).thenReturn(i18nFormat);
    when(i18nManager.getI18n()).thenReturn(i18n);

    DimensionalObject dimensionalObject = target.getPeriodDimension(itemsUid, new Date());

    assertEquals("pe", dimensionalObject.getDimension());
    assertEquals("pe", dimensionalObject.getUid());
    assertEquals(PERIOD, dimensionalObject.getDimensionType());
    assertEquals("Period", dimensionalObject.getDimensionDisplayName());

    DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
    assertEquals("LAST_YEAR", refDimensionKeywords.getKeywords().get(0).getKey());
    assertEquals("LAST_5_YEARS", refDimensionKeywords.getKeywords().get(1).getKey());

    int currentYear = LocalDate.now().getYear();

    String lastYear = Integer.toString(currentYear - 1);
    String twoYearsAgo = Integer.toString(currentYear - 2);
    String threeYearsAgo = Integer.toString(currentYear - 3);
    String fourYearsAgo = Integer.toString(currentYear - 4);
    String fiveYearsAgo = Integer.toString(currentYear - 5);

    List<DimensionalItemObject> refPeriods = dimensionalObject.getItems();
    assertDailyPeriod(lastYear, "LAST_UPDATED", (PeriodDimension) refPeriods.get(0));
    assertDailyPeriod(fiveYearsAgo, "SCHEDULED_DATE", (PeriodDimension) refPeriods.get(1));
    assertDailyPeriod(fourYearsAgo, "SCHEDULED_DATE", (PeriodDimension) refPeriods.get(2));
    assertDailyPeriod(threeYearsAgo, "SCHEDULED_DATE", (PeriodDimension) refPeriods.get(3));
    assertDailyPeriod(twoYearsAgo, "SCHEDULED_DATE", (PeriodDimension) refPeriods.get(4));
    assertDailyPeriod(lastYear, "SCHEDULED_DATE", (PeriodDimension) refPeriods.get(5));
  }

  private void assertDailyPeriod(String year, String dateField, PeriodDimension period) {
    assertInstanceOf(YearlyPeriodType.class, period.getPeriodType());
    assertEquals(year, period.getIsoDate());
    assertEquals(dateField, period.getDateField());
    assertEquals(year + "-01-01", period.getStartDateString());
    assertEquals(year + "-12-31", period.getEndDateString());
  }

  @Test
  void testGetPeriodDimensionForNonIsoPeriod() {
    List<String> itemsUid = List.of("2021-05-01_2021-06-01:LAST_UPDATED");

    when(settings.getAnalyticsFinancialYearStart()).thenReturn(FINANCIAL_YEAR_APRIL);
    when(i18nManager.getI18nFormat()).thenReturn(i18nFormat);

    DimensionalObject dimensionalObject = target.getPeriodDimension(itemsUid, new Date());

    assertEquals("pe", dimensionalObject.getDimension());
    assertEquals("pe", dimensionalObject.getUid());
    assertEquals(PERIOD, dimensionalObject.getDimensionType());
    assertEquals("Period", dimensionalObject.getDimensionDisplayName());

    DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
    assertEquals("2021-05-01_2021-06-01", refDimensionKeywords.getKeywords().get(0).getKey());

    List<DimensionalItemObject> refPeriods = dimensionalObject.getItems();
    assertInstanceOf(DailyPeriodType.class, ((PeriodDimension) refPeriods.get(0)).getPeriodType());
    assertEquals("20210501", ((PeriodDimension) refPeriods.get(0)).getIsoDate());
    assertEquals("LAST_UPDATED", ((PeriodDimension) refPeriods.get(0)).getDateField());
    assertEquals("2021-05-01", ((PeriodDimension) refPeriods.get(0)).getStartDateString());
    assertEquals("2021-06-01", ((PeriodDimension) refPeriods.get(0)).getEndDateString());
  }

  @Test
  void testDynamicFrom() {
    injectSecurityContextNoSettings(new SystemUser());

    String categoryUid = "L6BswcbPGqs";
    String categoryName = "Category-A";
    Category category = createCategory(categoryName, categoryUid);
    List<String> itemsUid = List.of(categoryUid);

    when(idObjectManager.get(DYNAMIC_DIM_CLASSES, UID, categoryUid)).thenReturn(category);

    Optional<DimensionalObject> dimensionalObject =
        target.getDynamicDimension(categoryUid, itemsUid, DisplayProperty.NAME, UID);

    assertEquals(categoryUid, dimensionalObject.get().getDimension());
    assertEquals(categoryUid, dimensionalObject.get().getUid());
    assertEquals(CATEGORY, dimensionalObject.get().getDimensionType());
    assertEquals(categoryName, dimensionalObject.get().getDimensionDisplayName());
    assertTrue(dimensionalObject.get().getItems().isEmpty());
    assertNull(dimensionalObject.get().getDimensionItemKeywords());
  }

  @Test
  void testDynamicFromWithAllItems() {
    // given
    String categoryUid = "L6BswcbPGqs";
    String categoryName = "CategoryName";
    Category category = createCategory(categoryName, categoryUid);
    List<String> items = List.of("ALL_ITEMS");
    category.setCategoryOptions(List.of(new CategoryOption()));

    injectSecurityContextNoSettings(new SystemUser());

    // when
    when(idObjectManager.get(DYNAMIC_DIM_CLASSES, UID, categoryUid)).thenReturn(category);
    when(aclService.canDataOrMetadataRead(any(UserDetails.class), any(CategoryOption.class)))
        .thenReturn(true);

    Optional<DimensionalObject> dimensionalObject =
        target.getDynamicDimension(categoryUid, items, DisplayProperty.NAME, UID);

    // then
    assertTrue(dimensionalObject.isPresent());
    assertFalse(dimensionalObject.get().getItems().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("providePeDimensionsForPeriodOrder")
  void testOrderOfPeriods(List<String> periods, List<String> expected) {

    // Given
    Date aDayOfJune2024 =
        Date.from(LocalDate.of(2024, 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant());

    when(i18nManager.getI18nFormat()).thenReturn(i18nFormat);
    when(i18nManager.getI18n()).thenReturn(i18n);

    // When
    DimensionalObject baseDimensionalObject = target.getPeriodDimension(periods, aDayOfJune2024);

    // Then
    List<DimensionalItemObject> items = baseDimensionalObject.getItems();
    // we need to assert that baseDimensionalObject.getItems() is ordered as expected
    for (int i = 0; i < items.size(); i++) {
      assertEquals(expected.get(i), items.get(i).getUid());
    }
  }

  // This method provides the periods and the expected order of the periods for the
  // testOrderOfPeriods test
  private static Stream<Arguments> providePeDimensionsForPeriodOrder() {
    return Stream.of(
        Arguments.of(List.of("2024", "LAST_YEAR"), List.of("2024", "2023")),
        Arguments.of(
            List.of("THIS_YEAR", "LAST_5_YEARS"),
            List.of("2024", "2019", "2020", "2021", "2022", "2023")),
        Arguments.of(
            List.of("LAST_YEAR", "LAST_5_YEARS"), List.of("2023", "2019", "2020", "2021", "2022")),
        Arguments.of(
            List.of("2021", "LAST_5_YEARS"), List.of("2021", "2019", "2020", "2022", "2023")),
        Arguments.of(
            List.of("LAST_5_YEARS", "2021"), List.of("2019", "2020", "2021", "2022", "2023")));
  }

  @Test
  void testMetadataHandlerGetDistinctPeriodUids() {

    // Given
    Date aDayOfJune2024 =
        Date.from(LocalDate.of(2024, 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant());

    when(i18nManager.getI18nFormat()).thenReturn(i18nFormat);
    when(i18nManager.getI18n()).thenReturn(i18n);

    // When
    List<DimensionalItemObject> dimensionalItemObjects =
        target
            .getPeriodDimension(
                List.of("LAST_YEAR:LAST_UPDATED", "LAST_5_YEARS:SCHEDULED_DATE"), aDayOfJune2024)
            .getItems();

    // And
    List<String> distinctPeriodUids =
        MetadataDimensionsHandler.getDistinctPeriodUids(dimensionalItemObjects);

    // Then
    assertEquals(5, distinctPeriodUids.size());
    assertEquals(List.of("2023", "2019", "2020", "2021", "2022"), distinctPeriodUids);
  }

  private void assertKeywordForDimensionalObject(
      Keyword keyword, DimensionalItemObject dimensionalItemObject) {
    assertEquals(keyword.getKey(), dimensionalItemObject.getUid());
    assertEquals(keyword.getMetadataItem().getName(), dimensionalItemObject.getName());
    assertEquals(keyword.getMetadataItem().getCode(), dimensionalItemObject.getCode());
    assertEquals(keyword.getMetadataItem().getUid(), dimensionalItemObject.getUid());
  }

  private void assertOrgUnits(OrganisationUnit expected, OrganisationUnit actual) {
    assertEquals(expected.getComment(), actual.getComment());
    assertEquals(expected.getShortName(), actual.getShortName());
    assertEquals(expected.getUid(), actual.getUid());
    assertEquals(expected.getCode(), actual.getCode());
    assertEquals(expected.getName(), actual.getName());
  }

  private void assertBaseDimensionalObjects(
      DimensionalItemObject expected, DimensionalItemObject actual) {
    assertEquals(expected.getShortName(), actual.getShortName());
    assertEquals(expected.getUid(), actual.getUid());
    assertEquals(expected.getCode(), actual.getCode());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.hashCode(), actual.hashCode());
  }
}
