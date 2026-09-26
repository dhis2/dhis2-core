/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of {@link IdentifiableObjectBundleHook}'s whitespace trimming of metadata text
 * properties ({@code name}, {@code code}, {@code shortName}, {@code description}), run through the
 * real {@link ObjectBundleService} create/validate/commit pipeline.
 */
@Transactional
class IdentifiableObjectBundleHookTest extends PostgresIntegrationTestBase {

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void createTrimsSurroundingWhitespaceFromTextFields() {
    DataElement dataElement = createDataElement('A');
    dataElement.setName("  Trimmed Name  ");
    dataElement.setShortName("  Trimmed Short  ");
    dataElement.setCode("  CODE1  ");
    dataElement.setDescription("  Some description  ");

    commit(ImportStrategy.CREATE, dataElement);

    DataElement persisted = manager.get(DataElement.class, dataElement.getUid());
    assertEquals("Trimmed Name", persisted.getName());
    assertEquals("Trimmed Short", persisted.getShortName());
    assertEquals("CODE1", persisted.getCode());
    assertEquals("Some description", persisted.getDescription());
  }

  @Test
  void createTrimsWhitespaceOnlyDescriptionToEmptyString() {
    DataElement dataElement = createDataElement('B');
    dataElement.setDescription("   ");

    commit(ImportStrategy.CREATE, dataElement);

    DataElement persisted = manager.get(DataElement.class, dataElement.getUid());
    assertEquals("", persisted.getDescription());
  }

  @Test
  void createLeavesAlreadyTrimmedValuesUntouched() {
    DataElement dataElement = createDataElement('C');
    dataElement.setName("Already Trimmed");

    commit(ImportStrategy.CREATE, dataElement);

    DataElement persisted = manager.get(DataElement.class, dataElement.getUid());
    assertEquals("Already Trimmed", persisted.getName());
  }

  @Test
  void updateTrimsSurroundingWhitespaceFromTextFields() {
    DataElement dataElement = createDataElement('D');
    manager.save(dataElement);

    DataElement update = manager.get(DataElement.class, dataElement.getUid());
    update.setName("  Updated Name  ");
    update.setShortName("  Updated Short  ");
    update.setDescription("  Updated description  ");

    commit(ImportStrategy.UPDATE, update);

    DataElement persisted = manager.get(DataElement.class, dataElement.getUid());
    assertEquals("Updated Name", persisted.getName());
    assertEquals("Updated Short", persisted.getShortName());
    assertEquals("Updated description", persisted.getDescription());
  }

  @Test
  void createDetectsUniquenessCollisionThatOnlyEmergesAfterTrimming() {
    DataElement dataElementOne = createDataElement('E');
    dataElementOne.setCode("SHARED");

    DataElement dataElementTwo = createDataElement('F');
    dataElementTwo.setCode("  SHARED  ");

    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.addObject(dataElementOne);
    params.addObject(dataElementTwo);

    ObjectBundle bundle = objectBundleService.create(params);
    ObjectBundleValidationReport report = objectBundleValidationService.validate(bundle);

    assertTrue(report.hasErrorReports());
    assertEquals(1, report.getErrorReportsCount(ErrorCode.E5003));
  }

  private void commit(ImportStrategy importStrategy, DataElement dataElement) {
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(importStrategy);
    params.addObject(dataElement);

    ObjectBundle bundle = objectBundleService.create(params);
    ObjectBundleValidationReport report = objectBundleValidationService.validate(bundle);
    assertFalse(report.hasErrorReports());
    objectBundleService.commit(bundle);
  }
}
