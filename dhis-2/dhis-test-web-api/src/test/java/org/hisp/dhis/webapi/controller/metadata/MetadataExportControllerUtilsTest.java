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
package org.hisp.dhis.webapi.controller.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link MetadataExportControllerUtils}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class MetadataExportControllerUtilsTest {

  @Mock private ContextService contextService;

  @Mock private MetadataExportService exportService;

  @Test
  void getWithDependencies() {
    final Map<String, List<String>> parameterValuesMap = new HashMap<>();
    final MetadataExportParams exportParams = new MetadataExportParams();
    final Attribute attribute = new Attribute();
    final ObjectNode rootNode = JacksonObjectMapperConfig.jsonMapper.createObjectNode();

    Mockito.when(contextService.getParameterValuesMap()).thenReturn(parameterValuesMap);
    Mockito.when(exportService.getParamsFromMap(Mockito.same(parameterValuesMap)))
        .thenReturn(exportParams);
    Mockito.when(
            exportService.getMetadataWithDependenciesAsNode(
                Mockito.same(attribute), Mockito.same(exportParams)))
        .thenReturn(rootNode);

    final ResponseEntity<JsonNode> responseEntity =
        MetadataExportControllerUtils.getWithDependencies(
            contextService, exportService, attribute, false);
    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    Assertions.assertSame(rootNode, responseEntity.getBody());
    Assertions.assertFalse(
        responseEntity.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
  }

  @Test
  void getWithDependenciesAsDownload() {
    final Map<String, List<String>> parameterValuesMap = new HashMap<>();
    final MetadataExportParams exportParams = new MetadataExportParams();
    final Attribute attribute = new Attribute();
    final ObjectNode rootNode = JacksonObjectMapperConfig.jsonMapper.createObjectNode();

    Mockito.when(contextService.getParameterValuesMap()).thenReturn(parameterValuesMap);
    Mockito.when(exportService.getParamsFromMap(Mockito.same(parameterValuesMap)))
        .thenReturn(exportParams);
    Mockito.when(
            exportService.getMetadataWithDependenciesAsNode(
                Mockito.same(attribute), Mockito.same(exportParams)))
        .thenReturn(rootNode);

    final ResponseEntity<JsonNode> responseEntity =
        MetadataExportControllerUtils.getWithDependencies(
            contextService, exportService, attribute, true);
    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    Assertions.assertSame(rootNode, responseEntity.getBody());
    Assertions.assertEquals(
        "attachment; filename=metadata",
        responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
  }

  @Test
  void createResponseEntity() {
    final RootNode rootNode = new RootNode("test");
    final ResponseEntity<RootNode> responseEntity =
        MetadataExportControllerUtils.createResponseEntity(rootNode, false);
    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    Assertions.assertSame(rootNode, responseEntity.getBody());
    Assertions.assertFalse(
        responseEntity.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION));
  }

  @Test
  void createResponseEntityAsDownload() {
    final RootNode rootNode = new RootNode("test");
    final ResponseEntity<RootNode> responseEntity =
        MetadataExportControllerUtils.createResponseEntity(rootNode, true);
    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    Assertions.assertSame(rootNode, responseEntity.getBody());
    Assertions.assertEquals(
        "attachment; filename=metadata",
        responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
  }
}
