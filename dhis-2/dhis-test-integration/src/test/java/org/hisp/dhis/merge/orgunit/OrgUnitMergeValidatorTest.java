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
package org.hisp.dhis.merge.orgunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class OrgUnitMergeValidatorTest extends PostgresIntegrationTestBase {

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrgUnitMergeValidator validator;

  @Test
  void testValidateMissingSources() {
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrgUnitMergeRequest request = new OrgUnitMergeRequest.Builder().withTarget(ouB).build();
    assertEquals(ErrorCode.E1500, validator.validateForErrorMessage(request).getErrorCode());
  }

  @Test
  void testValidateMissingTarget() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).build();
    assertEquals(ErrorCode.E1501, validator.validateForErrorMessage(request).getErrorCode());
  }

  @Test
  void testValidateTargetIsSource() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouA).build();
    assertEquals(ErrorCode.E1502, validator.validateForErrorMessage(request).getErrorCode());
  }

  @Test
  void testValidateTargetIsDescendantOfSource() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrganisationUnit ouC = createOrganisationUnit('C', ouA);
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();
    ErrorMessage errorMessage = validator.validateForErrorMessage(request);
    assertEquals(ErrorCode.E1504, errorMessage.getErrorCode());
  }

  @Test
  void testMultiSourceValidateSuccess() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrganisationUnit ouC = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();
    assertNull(validator.validateForErrorMessage(request));
  }

  @Test
  void validateSingleSourceValidateSuccess() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).withTarget(ouB).build();
    assertNull(validator.validateForErrorMessage(request));
  }
}
