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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression test for DHIS2-18980: importing a {@link org.hisp.dhis.relationship.RelationshipType}
 * with a {@link org.hisp.dhis.trackerdataview.TrackerDataView} in the same payload as the
 * Program/TrackedEntityType/TrackedEntityAttribute it depends on used to fail, because the
 * validation looked up those references in the database instead of the import bundle.
 */
@Transactional
class RelationshipTypeImportTest extends PostgresIntegrationTestBase {
  @Autowired private MetadataImportService importService;

  @Autowired private RenderService _renderService;

  @BeforeEach
  void setUp() {
    renderService = _renderService;
  }

  @Test
  void testImportRelationshipTypeWithTrackerDataViewInSamePayload() throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/relationshiptype_with_trackerdataview.json")
                .getInputStream(),
            RenderFormat.JSON);

    MetadataImportParams params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);

    ImportReport report = importService.importMetadata(params, new MetadataObjects(metadata));

    report.forEachErrorReport(errorReport -> System.err.println("Error report: " + errorReport));
    assertEquals(Status.OK, report.getStatus());
  }
}
