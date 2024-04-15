package org.hisp.dhis.merge.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class DataElementMergeProcessorTest extends IntegrationTestBase {

  @Autowired private DataElementMergeProcessor mergeProcessor;
  @Autowired private IdentifiableObjectManager idObjectManager;
  @Autowired private MinMaxDataElementStore minMaxDataElementStore;
  @Autowired private MinMaxDataElementService minMaxDataElementService;
  @Autowired private DataElementService dataElementService;
  @Autowired private OrganisationUnitService orgUnitService;

  private DataElement deSource1;
  private DataElement deSource2;
  private DataElement deTarget;
  private OrganisationUnit ou1;
  private OrganisationUnit ou2;
  private OrganisationUnit ou3;
  private CategoryOptionCombo coc1;

  @Override
  public void setUpTest() {
    // data elements
    deSource1 = createDataElement('A');
    deSource2 = createDataElement('B');
    deTarget = createDataElement('C');
    idObjectManager.save(List.of(deSource1, deSource2, deTarget));

    // org unit
    ou1 = createOrganisationUnit('A');
    ou2 = createOrganisationUnit('B');
    ou3 = createOrganisationUnit('C');
    idObjectManager.save(List.of(ou1, ou2, ou3));

    // cat option combo
    coc1 = categoryService.getDefaultCategoryOptionCombo();
    idObjectManager.save(coc1);
  }

  @Test
  @DisplayName("Ensure setup data is present in system")
  void ensureDataIsPresentInSystem() {
    // given setup is complete
    // when trying to retrieve data
    List<DataElement> dataElements = dataElementService.getAllDataElements();
    List<OrganisationUnit> orgUnits = orgUnitService.getAllOrganisationUnits();

    // then
    assertEquals(3, dataElements.size());
    assertEquals(3, orgUnits.size());
  }

  @Test
  @DisplayName("MinMaxDataElements are merged as expected, sources not deleted")
  void minMaxDataElementMergeTest() throws ConflictException {
    // given
    // min max data elements
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<MinMaxDataElement> minMaxSources =
        minMaxDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<MinMaxDataElement> minMaxTarget =
        minMaxDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, minMaxSources.size());
    assertEquals(3, minMaxTarget.size());
    assertEquals(3, allDataElements.size());
    assertTrue(allDataElements.containsAll(List.of(deTarget, deSource1, deSource2)));
  }

  @Test
  @DisplayName("MinMaxDataElements are merged as expected, sources are deleted")
  void minMaxDataElementMergeDeleteSourcesTest() throws ConflictException {
    // given
    // min max data elements
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou2, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou3, coc1, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));
    mergeParams.setDeleteSources(true);

    // when
    MergeReport report = mergeProcessor.processMerge(mergeParams);

    // then
    List<MinMaxDataElement> minMaxSources =
        minMaxDataElementService.getAllByDataElement(List.of(deSource1, deSource2));
    List<MinMaxDataElement> minMaxTarget =
        minMaxDataElementService.getAllByDataElement(List.of(deTarget));
    List<DataElement> allDataElements = dataElementService.getAllDataElements();

    assertFalse(report.hasErrorMessages());
    assertEquals(0, minMaxSources.size());
    assertEquals(3, minMaxTarget.size());
    assertEquals(1, allDataElements.size());
    assertTrue(allDataElements.contains(deTarget));
  }

  @Test
  @DisplayName("MinMaxDataElements DB constraint error when updating")
  void testMinMaxDataElementMergeDbConstraint() {
    // given unique key DB constraint exists (orgUnit, dataElement, catOptionCombo)
    // create min max data elements all of which have the same org unit and cat option combo
    MinMaxDataElement minMaxDataElement1 =
        new MinMaxDataElement(deSource1, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement2 =
        new MinMaxDataElement(deSource2, ou1, coc1, 0, 100, false);
    MinMaxDataElement minMaxDataElement3 =
        new MinMaxDataElement(deTarget, ou1, coc1, 0, 100, false);
    minMaxDataElementStore.save(minMaxDataElement1);
    minMaxDataElementStore.save(minMaxDataElement2);
    minMaxDataElementStore.save(minMaxDataElement3);

    // params
    MergeParams mergeParams = new MergeParams();
    mergeParams.setSources(UID.of(List.of(deSource1.getUid(), deSource2.getUid())));
    mergeParams.setTarget(UID.of(deTarget.getUid()));

    // when merge operation encounters DB constraint
    DataIntegrityViolationException dataIntegrityViolationException =
        assertThrows(
            DataIntegrityViolationException.class, () -> mergeProcessor.processMerge(mergeParams));
    assertNotNull(dataIntegrityViolationException.getMessage());

    // then DB constraint is thrown
    List<String> expectedStrings =
        List.of(
            "could not execute statement",
            "minmaxdataelement_unique_key",
            "ConstraintViolationException");

    assertTrue(
        expectedStrings.stream()
            .allMatch(exp -> dataIntegrityViolationException.getMessage().contains(exp)));
  }
}
