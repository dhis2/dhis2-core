/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pins which data values a user gets from a data value export depending on the data-read sharing of
 * the category options behind each attribute option combo (AOC), and that explicitly requested AOCs
 * are access checked on every path.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class DataExportAocAccessTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private DataElementService dataElementService;
  @Autowired private DataSetService dataSetService;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private PeriodService periodService;
  @Autowired private DataDumpService dataDumpService;
  @Autowired private DataExportPipeline dataExportPipeline;
  @Autowired private DataExportStore dataExportStore;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private ObjectMapper jsonMapper;

  private CategoryOption coOpen;
  private CategoryOption coClosed;
  private CategoryOptionCombo aocOpen;
  private CategoryOptionCombo aocClosed;
  private CategoryOptionCombo cocDisaggClosed;
  private DataSet ds;
  private DataElement de;
  private OrganisationUnit ou;
  private Period pe;
  private User user;

  @BeforeEach
  void setUp() {
    pe =
        createPeriod(
            PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY),
            getDate(2016, 3, 1),
            getDate(2016, 3, 31));
    periodService.addPeriod(pe);
    ou = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(ou);

    // attribute combo with one readable and one restricted option
    coOpen = createCategoryOption('O');
    readable(coOpen);
    coClosed = createCategoryOption('C');
    restrict(coClosed);
    categoryService.addCategoryOption(coOpen, new SystemUser());
    categoryService.addCategoryOption(coClosed, new SystemUser());
    Category attrCategory = createCategory('A', coOpen, coClosed);
    attrCategory.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategory(attrCategory);
    CategoryCombo attrCombo = createCategoryCombo('A', attrCategory);
    attrCombo.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategoryCombo(attrCombo);
    aocOpen = createCategoryOptionCombo(attrCombo, coOpen);
    aocClosed = createCategoryOptionCombo(attrCombo, coClosed);
    categoryService.addCategoryOptionCombo(aocOpen);
    categoryService.addCategoryOptionCombo(aocClosed);

    // disaggregation combo whose only option is restricted (used as the COC, never as an AOC)
    CategoryOption coDisaggClosed = createCategoryOption('D');
    restrict(coDisaggClosed);
    categoryService.addCategoryOption(coDisaggClosed, new SystemUser());
    Category disaggCategory = createCategory('D', coDisaggClosed);
    categoryService.addCategory(disaggCategory);
    CategoryCombo disaggCombo = createCategoryCombo('D', disaggCategory);
    categoryService.addCategoryCombo(disaggCombo);
    cocDisaggClosed = createCategoryOptionCombo(disaggCombo, coDisaggClosed);
    categoryService.addCategoryOptionCombo(cocDisaggClosed);

    de = createDataElement('A');
    de.setCategoryCombo(disaggCombo);
    dataElementService.addDataElement(de);
    ds = createDataSet('A');
    ds.setCategoryCombo(attrCombo);
    ds.addDataSetElement(de);
    dataSetService.addDataSet(ds);

    addDataValues(
        new DataValue(de, pe, ou, cocDisaggClosed, aocOpen, "1"),
        new DataValue(de, pe, ou, cocDisaggClosed, aocClosed, "2"));

    user = makeUser("A");
    user.setOrganisationUnits(Sets.newHashSet(ou));
    userService.addUser(user);
    enableDataSharing(user, ds, AccessStringHelper.DATA_READ_WRITE);
    dataSetService.updateDataSet(ds);
  }

  @Test
  void nonSuperuser_restrictedAocExcluded_restrictedDisaggregationCocKept() throws Exception {
    // value 2 uses the restricted AOC and is excluded; value 1 is kept although its COC
    // (cocDisaggClosed) has a restricted option: only the AOC dimension is filtered
    injectSecurityContextUser(user);
    assertEquals(Set.of("1"), exportedValues(null));
  }

  @Test
  void superuser_seesAllAocs() throws Exception {
    injectSecurityContextUser(superuser("S"));
    assertEquals(Set.of("1", "2"), exportedValues(null));
  }

  @Test
  void nonSuperuser_userAccessOnRestrictedOption_readsIt() throws Exception {
    enableDataSharing(user, coClosed, AccessStringHelper.DATA_READ);
    save(coClosed);
    injectSecurityContextUser(user);
    assertEquals(Set.of("1", "2"), exportedValues(null));
  }

  @Test
  void nonSuperuser_groupAccess_readsRestrictedAoc() throws Exception {
    UserGroup group = createUserGroup('G', Set.of(user));
    manager.save(group);
    user.getGroups().add(group);
    userService.updateUser(user);
    enableDataSharingWithUserGroup(group, coClosed, AccessStringHelper.DATA_READ);
    save(coClosed);
    injectSecurityContextUser(user);
    assertEquals(Set.of("1", "2"), exportedValues(null));
  }

  @Test
  void nonSuperuser_allAocsDenied_getsNothing() throws Exception {
    restrict(coOpen);
    save(coOpen);
    injectSecurityContextUser(user);
    assertEquals(Set.of(), exportedValues(null));
  }

  @Test
  void nonSuperuser_defaultAoc_readableEvenWhenDefaultOptionRestricted() throws Exception {
    // there are no sharing guards on default: a restricted default option (a misconfiguration,
    // see the default category option sharing integrity check) must not hide default AOC data
    CategoryCombo defaultCombo = categoryService.getDefaultCategoryCombo();
    CategoryOptionCombo defaultCoc = categoryService.getDefaultCategoryOptionCombo();
    DataElement deDefault = createDataElement('B');
    deDefault.setCategoryCombo(defaultCombo);
    dataElementService.addDataElement(deDefault);
    DataSet dsDefault = createDataSet('B');
    dsDefault.setCategoryCombo(defaultCombo);
    dsDefault.addDataSetElement(deDefault);
    dataSetService.addDataSet(dsDefault);
    enableDataSharing(user, dsDefault, AccessStringHelper.DATA_READ_WRITE);
    dataSetService.updateDataSet(dsDefault);
    addDataValues(new DataValue(deDefault, pe, ou, defaultCoc, defaultCoc, "4"));

    CategoryOption defaultOption = categoryService.getDefaultCategoryOption();
    Sharing original = defaultOption.getSharing().copy();
    try {
      restrict(defaultOption);
      save(defaultOption);
      injectSecurityContextUser(user);
      // data set with the default attribute combo: the default AOC is requested explicitly
      assertEquals(Set.of("4"), export(slice().dataSet(Set.of(dsDefault.getUid()))));
      // data element selector: no explicit AOC, the readable AOC list applies
      assertEquals(Set.of("4"), export(slice().dataElement(Set.of(deDefault.getUid()))));
    } finally {
      defaultOption.setSharing(original);
      save(defaultOption);
    }
  }

  @Test
  void dataSetExport_excludesAocsOutsideTheDataSetAttributeCombo() throws Exception {
    // invalid for ds (attribute combo attrCombo): a value of one of its data elements stored with
    // the default AOC, which only data sets with the default attribute combo may use
    CategoryOptionCombo defaultCoc = categoryService.getDefaultCategoryOptionCombo();
    addDataValues(new DataValue(de, pe, ou, cocDisaggClosed, defaultCoc, "5"));

    injectSecurityContextUser(user);
    assertEquals(Set.of("1"), exportedValues(null));
    // without a data set there is no attribute combo to scope by
    assertEquals(Set.of("1", "5"), export(slice().dataElement(Set.of(de.getUid()))));

    // superusers are not restricted, so they can find such invalid data
    injectSecurityContextUser(superuser("U"));
    assertEquals(Set.of("1", "2", "5"), exportedValues(null));
  }

  @Test
  void disaggregationTypedAoc_excludedForNonSuperuser() throws Exception {
    // invalid configuration: a value stored with a disaggregation COC as its AOC is not exported to
    // non-superusers; superusers are unchanged
    CategoryOptionCombo cocDisaggOpen = createDisaggregationCoc('E');
    addDataValues(new DataValue(de, pe, ou, cocDisaggClosed, cocDisaggOpen, "3"));

    injectSecurityContextUser(user);
    assertEquals(Set.of("1"), exportedValues(null));

    injectSecurityContextUser(superuser("T"));
    assertEquals(Set.of("1", "2", "3"), exportedValues(null));
  }

  @Test
  void explicitReadableAoc_returnsOnlyThatAoc() throws Exception {
    injectSecurityContextUser(user);
    assertEquals(Set.of("1"), exportedValues(aocOpen.getUid()));
  }

  @Test
  void explicitDeniedAoc_stillE2011() {
    injectSecurityContextUser(user);
    ConflictException ex =
        assertThrows(ConflictException.class, () -> exportedValues(aocClosed.getUid()));
    assertEquals(ErrorCode.E2011, ex.getCode());
  }

  @Test
  void explicitDisaggregationCocAsAoc_E2049() {
    // invalid configuration requested explicitly gets its own error, not the sharing one
    CategoryOptionCombo cocDisaggOpen = createDisaggregationCoc('F');
    injectSecurityContextUser(user);
    ConflictException ex =
        assertThrows(ConflictException.class, () -> exportedValues(cocDisaggOpen.getUid()));
    assertEquals(ErrorCode.E2049, ex.getCode());
  }

  @Test
  void syncExport_explicitDeniedAoc_E2011() {
    // guard for future callers: today's sync callers (DataValueSynchronization,
    // DefaultSynchronizationManager) only pass lastUpdated/limit/offset, so they never carry AOCs
    injectSecurityContextUser(user);
    DataExportParams.Input in =
        DataExportParams.Input.builder()
            .dataSet(Set.of(ds.getUid()))
            .orgUnit(Set.of(ou.getUid()))
            .period(Set.of(pe.getIsoDate()))
            .attributeOptionCombo(Set.of(aocClosed.getUid()))
            .build();
    ConflictException ex =
        assertThrows(
            ConflictException.class,
            () -> dataExportPipeline.exportAsJsonSync(in, new ByteArrayOutputStream()));
    assertEquals(ErrorCode.E2011, ex.getCode());
  }

  @Test
  void store_explicitDeniedAoc_returnsNothingWithoutGate() {
    // calling the store directly (no service gate) must not return a value the user cannot read
    injectSecurityContextUser(user);
    DataExportParams params =
        DataExportParams.builder()
            .dataSets(List.of(UID.of(ds.getUid())))
            .organisationUnits(List.of(UID.of(ou.getUid())))
            .periods(List.of(pe))
            .attributeOptionCombos(List.of(UID.of(aocClosed.getUid())))
            .includeDeleted(true)
            .build();
    List<DataExportValue> values =
        transactionTemplate.execute(status -> dataExportStore.exportValues(params).toList());
    assertEquals(List.of(), values);
  }

  private User superuser(String uniqueCharacter) {
    User admin = makeUser(uniqueCharacter, List.of("ALL"));
    admin.setOrganisationUnits(Sets.newHashSet(ou));
    userService.addUser(admin);
    return admin;
  }

  private CategoryOptionCombo createDisaggregationCoc(char uniqueCharacter) {
    CategoryOption option = createCategoryOption(uniqueCharacter);
    readable(option);
    categoryService.addCategoryOption(option, new SystemUser());
    Category category = createCategory(uniqueCharacter, option);
    categoryService.addCategory(category);
    CategoryCombo combo = createCategoryCombo(uniqueCharacter, category);
    categoryService.addCategoryCombo(combo);
    CategoryOptionCombo coc = createCategoryOptionCombo(combo, option);
    categoryService.addCategoryOptionCombo(coc);
    return coc;
  }

  private void readable(CategoryOption option) {
    // only kept on create by the acting-user overload of addCategoryOption, the plain one resets it
    option.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
  }

  private void save(CategoryOption option) {
    // as a superuser, the test user may not write options whose sharing the test changes
    categoryService.updateCategoryOption(option, new SystemUser());
  }

  private void restrict(CategoryOption option) {
    // not the test user as owner (a null owner counts as readable), no public access
    option.getSharing().setOwner("ownerOther1");
    option.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
  }

  private DataExportParams.Input.InputBuilder slice() {
    return DataExportParams.Input.builder()
        .orgUnit(Set.of(ou.getUid()))
        .period(Set.of(pe.getIsoDate()));
  }

  private Set<String> exportedValues(String aoc) throws Exception {
    DataExportParams.Input.InputBuilder in = slice().dataSet(Set.of(ds.getUid()));
    if (aoc != null) in.attributeOptionCombo(Set.of(aoc));
    return export(in);
  }

  private Set<String> export(DataExportParams.Input.InputBuilder in) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(in.build(), out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    return dvs.getDataValues().stream()
        .map(org.hisp.dhis.dxf2.datavalue.DataValue::getValue)
        .collect(Collectors.toSet());
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }
}
