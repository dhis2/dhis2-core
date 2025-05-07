package org.hisp.dhis.csv;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.csv.CSV.CsvReader;
import org.hisp.dhis.minmax.MinMaxValue;
import org.hisp.dhis.minmax.MinMaxValueKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CSVTest {

  @Test
  void testNullForOmittedOptionalColumns() {
    List<MinMaxValue> actual = CSV.of("""
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10
        """).as(MinMaxValue.class).list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValue(
            UID.of("c6Fi8CNxGJ1"), UID.of("MU1nUGOpV4Q"), UID.of("HllvX50cXC0"), 0, 10, null),
        actual.get(3));
  }

  @Test
  void testIgnoreIrrelevantColumns() {
    List<MinMaxValueKey> actual = CSV.of("""
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""").as(MinMaxValueKey.class).list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValueKey(UID.of("elD9B1HiTJO"), UID.of("fKiYlhodhB1"), UID.of("HllvX50cXC0")),
        actual.get(0));
  }

  @Test
  void testRequiresNonnullProperties_MissingColumn() {
    CsvReader<MinMaxValueKey> reader = CSV.of("""
        dataElement,xxx,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""").as(MinMaxValueKey.class);

    IllegalArgumentException ex = assertThrowsExactly(IllegalArgumentException.class, reader::list);
    assertEquals("Required columns are: [dataElement, orgUnit, optionCombo]", ex.getMessage());
  }

  @Test
  void testRequiresNonnullProperties_EmptyValue() {
    CsvReader<MinMaxValueKey> reader = CSV.of("""
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""").as(MinMaxValueKey.class);

    IllegalArgumentException ex = assertThrowsExactly(IllegalArgumentException.class, reader::list);
    assertEquals("Column orgUnit is required and cannot be empty", ex.getMessage());
  }
}
