/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import com.google.common.net.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link DataSetController}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class DataSetControllerMockTest {

  @Mock private ContextService contextService;

  @Mock private MetadataExportService exportService;

  @Mock private DataSetService service;

  @Mock private DataSet dataSet;

  @InjectMocks private DataSetController controller;

  @Test
  void getWithDependencies() throws Exception {
    getWithDependencies(false);
  }

  @Test
  void getWithDependenciesAsDownload() throws Exception {

    getWithDependencies(true);
  }

  private void getWithDependencies(boolean download) throws Exception {
    final Map<String, List<String>> parameterValuesMap = new HashMap<>();
    final MetadataExportParams exportParams = new MetadataExportParams();
    final RootNode rootNode = NodeUtils.createMetadata();

    Mockito.when(service.getDataSet(Mockito.eq("88dshgdga"))).thenReturn(dataSet);
    Mockito.when(contextService.getParameterValuesMap()).thenReturn(parameterValuesMap);
    Mockito.when(exportService.getParamsFromMap(Mockito.same(parameterValuesMap)))
        .thenReturn(exportParams);
    Mockito.when(
            exportService.getMetadataWithDependenciesAsNode(
                Mockito.same(dataSet), Mockito.same(exportParams)))
        .thenReturn(rootNode);

    final ResponseEntity<RootNode> responseEntity =
        controller.getDataSetWithDependencies("88dshgdga", download);
    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    Assertions.assertSame(rootNode, responseEntity.getBody());
    if (download) {
      Assertions.assertEquals(
          "attachment; filename=metadata",
          responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    } else {
      Assertions.assertFalse(
          responseEntity.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
    }
  }
}
