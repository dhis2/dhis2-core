/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.csv.CSV.CsvReader;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.minmax.MinMaxValue;
import org.hisp.dhis.minmax.MinMaxValueKey;
import org.junit.jupiter.api.Test;

class CSVTest {

  @Test
  void testNullForOmittedOptionalColumns() {
    List<MinMaxValue> actual =
        CSV.of(
                """
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10
        """)
            .as(MinMaxValue.class)
            .list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValue(
            UID.of("c6Fi8CNxGJ1"), UID.of("MU1nUGOpV4Q"), UID.of("HllvX50cXC0"), 0, 10, null),
        actual.get(3));
  }

  @Test
  void testNullForOmittedOptionalColumns_QuotesSpace() {
    List<MinMaxValue> actual =
        CSV.of(
                """
          dataElement,  orgUnit , "optionCombo" ,minValue,maxValue
          elD9B1HiTJO,"fKiYlhodhB1",HllvX50cXC0,0,10
         c6Fi8CNxGJ1,  "fKiYlhodhB1" ,"HllvX50cXC0" ,0,10
        elD9B1HiTJO , "MU1nUGOpV4Q" ,HllvX50cXC0  ,0,10
        c6Fi8CNxGJ1 , MU1nUGOpV4Q ,"HllvX50cXC0"  ,0,10
        """)
            .as(MinMaxValue.class)
            .list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValue(
            UID.of("c6Fi8CNxGJ1"), UID.of("MU1nUGOpV4Q"), UID.of("HllvX50cXC0"), 0, 10, null),
        actual.get(3));
  }

  @Test
  void testNullForOptionalColumns() {
    List<MinMaxValue> actual =
        CSV.of(
                """
        dataElement,orgUnit,optionCombo,minValue,maxValue,generated
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10,false
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10,true
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10,
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10,
        """)
            .as(MinMaxValue.class)
            .list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValue(
            UID.of("c6Fi8CNxGJ1"), UID.of("fKiYlhodhB1"), UID.of("HllvX50cXC0"), 0, 10, true),
        actual.get(1));
    assertEquals(
        new MinMaxValue(
            UID.of("c6Fi8CNxGJ1"), UID.of("MU1nUGOpV4Q"), UID.of("HllvX50cXC0"), 0, 10, null),
        actual.get(3));
  }

  @Test
  void testIgnoreIrrelevantColumns() {
    List<MinMaxValueKey> actual =
        CSV.of(
                """
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""")
            .as(MinMaxValueKey.class)
            .list();
    assertEquals(4, actual.size());
    assertEquals(
        new MinMaxValueKey(UID.of("elD9B1HiTJO"), UID.of("fKiYlhodhB1"), UID.of("HllvX50cXC0")),
        actual.get(0));
  }

  @Test
  void testRequiresNonnullProperties_MissingColumn() {
    CsvReader<MinMaxValueKey> reader =
        CSV.of(
                """
        dataElement,xxx,optionCombo,minValue,maxValue
        elD9B1HiTJO,fKiYlhodhB1,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""")
            .as(MinMaxValueKey.class);

    IllegalArgumentException ex = assertThrowsExactly(IllegalArgumentException.class, reader::list);
    assertEquals("Required columns missing: [orgUnit]", ex.getMessage());
  }

  @Test
  void testRequiresNonnullProperties_EmptyValue() {
    CsvReader<MinMaxValueKey> reader =
        CSV.of(
                """
        dataElement,orgUnit,optionCombo,minValue,maxValue
        elD9B1HiTJO,,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,fKiYlhodhB1,HllvX50cXC0,0,10
        elD9B1HiTJO,MU1nUGOpV4Q,HllvX50cXC0,0,10
        c6Fi8CNxGJ1,MU1nUGOpV4Q,HllvX50cXC0,0,10""")
            .as(MinMaxValueKey.class);

    IllegalArgumentException ex = assertThrowsExactly(IllegalArgumentException.class, reader::list);
    assertEquals("Column orgUnit is required and cannot be empty", ex.getMessage());
  }

  @Test
  void testAnyProperties() {
    List<DataEntryValue.Input> actual =
        CSV.of(
                """
        dataelement,period,orgunit,attributeoptioncombo,value,followup,deleted,age,gender
        CxlYcbqio4v,202506,rZxk3S0qN63,HllvX50cXC0,9,false,null,>15,f
        CxlYcbqio4v,202506,nX05QLraDhO,HllvX50cXC0,1,false,null,>20,m
        CxlYcbqio4v,202506,rZxk3S0qN63,HllvX50cXC0,8,false,null,>30,f
        CxlYcbqio4v,202506,rZxk3S0qN63,HllvX50cXC0,1,false,null,>15,m""")
            .as(DataEntryValue.Input.class)
            .list();
    assertEquals(4, actual.size());
    DataEntryValue.Input expectedRow1 =
        new DataEntryValue.Input(
            "CxlYcbqio4v",
            "rZxk3S0qN63",
            null,
            Map.of("age", ">15", "gender", "f"),
            "HllvX50cXC0",
            null,
            null,
            "202506",
            "9",
            null,
            false,
            null);
    assertEquals(expectedRow1, actual.get(0));
  }

  record CollectionsRecord(
      List<String> list,
      Set<Integer> set,
      Collection<ElementType> collection,
      Map<String, Long> map) {}

  @Test
  void testCollections() {
    List<CollectionsRecord> actual =
        CSV.of(
                """
        list,set,map,collection
        a b c,1 2 3,a=4 b=2,TYPE FIELD""")
            .as(CollectionsRecord.class)
            .list();
    assertEquals(1, actual.size());
    assertEquals(
        new CollectionsRecord(
            List.of("a", "b", "c"),
            Set.of(1, 2, 3),
            List.of(ElementType.TYPE, ElementType.FIELD),
            Map.of("a", 4L, "b", 2L)),
        actual.get(0));
  }
}
