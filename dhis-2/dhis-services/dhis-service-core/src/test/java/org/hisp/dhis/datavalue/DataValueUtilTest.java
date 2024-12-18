package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataValueUtilTest {

  @Test
  @DisplayName("Creates a new DataValue with the expected new category option combo")
  void newDataValueWithCocTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new category option combo to use
    CategoryOptionCombo newCoc = new CategoryOptionCombo();
    newCoc.setName("New COC");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewCatOptionCombo.apply(originalDataValue, newCoc);

    // then
    assertWithNewValue(newDataValue, "New COC", "COC");
  }

  @Test
  @DisplayName("Creates a new DataValue with the expected new attribute option combo")
  void newDataValueWithAocTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new attribute option combo to use
    CategoryOptionCombo newAoc = new CategoryOptionCombo();
    newAoc.setName("New AOC");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewAttrOptionCombo.apply(originalDataValue, newAoc);

    // then
    assertWithNewValue(newDataValue, "New AOC", "AOC");
  }

  @Test
  @DisplayName("Creates a new DataValue with the expected new data element")
  void newDataValueWithDeTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new attribute option combo to use
    DataElement newDe = new DataElement("New DE");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewDataElement.apply(originalDataValue, newDe);

    // then
    assertWithNewValue(newDataValue, "New DE", "DE");
  }

  private void assertWithNewValue(@Nonnull DataValue dv, String newValue, String property) {
    if (property.equals("DE")) {
      assertEquals("New DE", dv.getDataElement().getName());
    } else assertEquals("test DE", dv.getDataElement().getName());
    assertEquals("test Period", dv.getPeriod().getName());
    assertEquals("test Org Unit", dv.getSource().getName());
    if (property.equals("COC")) {
      assertEquals("New COC", dv.getCategoryOptionCombo().getName());
    } else assertEquals("test COC", dv.getCategoryOptionCombo().getName());
    if (property.equals("AOC")) {
      assertEquals("New AOC", dv.getAttributeOptionCombo().getName());
    } else assertEquals("test AOC", dv.getAttributeOptionCombo().getName());
    assertEquals("test value", dv.getValue());
    assertEquals("test user", dv.getStoredBy());
    assertEquals(DateUtils.toMediumDate("2024-11-15"), dv.getLastUpdated());
    assertEquals("test comment", dv.getComment());
    assertTrue(dv.isFollowup());
    assertTrue(dv.isDeleted());
    assertEquals(DateUtils.toMediumDate("2024-07-25"), dv.getCreated());
  }

  private DataValue getDataValue() {
    DataElement dataElement = new DataElement("test DE");
    Period period = new Period();
    period.setName("test Period");
    OrganisationUnit orgUnit = new OrganisationUnit("test Org Unit");
    CategoryOptionCombo coc = new CategoryOptionCombo();
    coc.setName("test COC");
    CategoryOptionCombo aoc = new CategoryOptionCombo();
    aoc.setName("test AOC");

    DataValue dv =
        DataValue.builder()
            .dataElement(dataElement)
            .period(period)
            .source(orgUnit)
            .categoryOptionCombo(coc)
            .attributeOptionCombo(aoc)
            .value("test value")
            .storedBy("test user")
            .lastUpdated(DateUtils.toMediumDate("2024-11-15"))
            .comment("test comment")
            .followup(true)
            .deleted(true)
            .build();
    dv.setCreated(DateUtils.toMediumDate("2024-07-25"));
    return dv;
  }
}
