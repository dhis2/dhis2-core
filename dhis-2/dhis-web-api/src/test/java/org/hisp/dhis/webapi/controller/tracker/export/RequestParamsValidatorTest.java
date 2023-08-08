/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitMode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

class RequestParamsValidatorTest {
  private static final OrganisationUnit orgUnit = new OrganisationUnit();

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeAccessible() {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), ACCESSIBLE));

    assertStartsWith(
        "orgUnitMode ACCESSIBLE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldPassWhenNoOrgUnitSuppliedAndOrgUnitModeAccessible() {
    assertDoesNotThrow(() -> validateOrgUnitMode(emptySet(), ACCESSIBLE));
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCapture() {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), CAPTURE));

    assertStartsWith("orgUnitMode CAPTURE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldPassWhenNoOrgUnitSuppliedAndOrgUnitModeCapture() {
    assertDoesNotThrow(() -> validateOrgUnitMode(emptySet(), CAPTURE));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeSelected() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), SELECTED));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: SELECTED", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeSelected() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), SELECTED));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeDescendants() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), DESCENDANTS));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: DESCENDANTS", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeDescendants() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), DESCENDANTS));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeChildren() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), CHILDREN));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: CHILDREN", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeChildren() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), CHILDREN));
  }
}
