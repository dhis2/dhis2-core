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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UIDConnection;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;

/**
 * Tests the methods of {@link org.hisp.dhis.datavalue.DefaultDataEntryService.ValidationSource}.
 */
class ValidationSourceTest {

  private final DefaultDataEntryService.ValidationSource scopeSource =
      DefaultDataEntryService.ValidationSource.of(
          new DataEntryGroup.Scope(
              List.of(UID.of("ou123456789"), UID.of("ou987654321")),
              List.of(Period.of("2020"), Period.of("2021")),
              List.of(
                  new DataEntryGroup.Scope.Element(
                      UID.of("de123456789"), UID.of("coc12345678"), UID.of("aoc12345678")),
                  new DataEntryGroup.Scope.Element(
                      UID.of("de987654321"), UID.of("coc87654321"), UID.of("aoc87654321")),
                  new DataEntryGroup.Scope.Element(UID.of("deDefault01"), null, null))));

  private final DefaultDataEntryService.ValidationSource valuesSource =
      DefaultDataEntryService.ValidationSource.of(
          List.of(
              new DataEntryValue(
                  0,
                  UID.of("de123456789"),
                  UID.of("ou123456789"),
                  UID.of("coc12345678"),
                  UID.of("aoc12345678"),
                  Period.of("2020"),
                  "value",
                  "comment",
                  false,
                  false),
              new DataEntryValue(
                  0,
                  UID.of("de987654321"),
                  UID.of("ou987654321"),
                  UID.of("coc87654321"),
                  UID.of("aoc87654321"),
                  Period.of("2021"),
                  "value",
                  "comment",
                  false,
                  false),
              new DataEntryValue(
                  0,
                  UID.of("deDefault01"),
                  UID.of("ouDefault01"),
                  null,
                  null,
                  Period.of("2022"),
                  "value",
                  "comment",
                  false,
                  false)));

  @Test
  void testDataElementCategoryOptionComboPairs_Scope() {
    assertEquals(
        List.of(
            new UIDConnection(UID.of("de123456789"), UID.of("coc12345678")),
            new UIDConnection(UID.of("de987654321"), UID.of("coc87654321")),
            new UIDConnection(UID.of("deDefault01"), null)),
        scopeSource.dataElementCategoryOptionComboPairs().toList());
  }

  @Test
  void testOrgUnitAttributeOptionComboPairs_Scope() {
    assertEquals(
        List.of(
            new UIDConnection(UID.of("ou123456789"), UID.of("aoc12345678")),
            new UIDConnection(UID.of("ou987654321"), UID.of("aoc12345678")),
            new UIDConnection(UID.of("ou123456789"), UID.of("aoc87654321")),
            new UIDConnection(UID.of("ou987654321"), UID.of("aoc87654321"))),
        scopeSource
            .orgUnitAttributeOptionComboPairs(Set.of("aoc12345678", "aoc87654321"))
            .toList());

    assertEquals(
        List.of(
            new UIDConnection(UID.of("ou123456789"), UID.of("aoc87654321")),
            new UIDConnection(UID.of("ou987654321"), UID.of("aoc87654321"))),
        scopeSource.orgUnitAttributeOptionComboPairs(Set.of("aoc87654321")).toList());
  }

  @Test
  void testDataElementCategoryOptionComboPairs_Values() {
    assertEquals(
        List.of(
            new UIDConnection(UID.of("de123456789"), UID.of("coc12345678")),
            new UIDConnection(UID.of("de987654321"), UID.of("coc87654321")),
            new UIDConnection(UID.of("deDefault01"), null)),
        valuesSource.dataElementCategoryOptionComboPairs().toList());
  }

  @Test
  void testOrgUnitAttributeOptionComboPairs_values() {
    assertEquals(
        List.of(
            new UIDConnection(UID.of("ou123456789"), UID.of("aoc12345678")),
            new UIDConnection(UID.of("ou987654321"), UID.of("aoc87654321"))),
        valuesSource
            .orgUnitAttributeOptionComboPairs(Set.of("aoc12345678", "aoc87654321"))
            .toList());

    assertEquals(
        List.of(new UIDConnection(UID.of("ou987654321"), UID.of("aoc87654321"))),
        valuesSource.orgUnitAttributeOptionComboPairs(Set.of("aoc87654321")).toList());
  }
}
