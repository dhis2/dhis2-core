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
package org.hisp.dhis.webapi.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link DefaultLinkService}.
 *
 * @author Volker Schmidt
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DefaultLinkServiceTest {
  @Mock private SchemaService schemaService;

  @Mock private ContextService contextService;

  @InjectMocks private DefaultLinkService service;

  private MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  void noLinks() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    final Pager pager = new Pager();
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertNull(pager.getPrevPage());
    Assertions.assertNull(pager.getNextPage());
  }

  @Test
  void nextLinkDefaultParameters() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              return map;
            });

    final Pager pager = new Pager(1, 1000);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertNull(pager.getPrevPage());
    Assertions.assertEquals("/demo/api/456/organizationUnits?page=2", pager.getNextPage());
  }

  @Test
  void nextLinkParameters() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.json");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              map.put("fields", Collections.singletonList("id,name,value[id,text]"));
              map.put("value[x]", Arrays.asList("test1", "test2\u00D8"));
              return map;
            });

    final Pager pager = new Pager(1, 1000);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertNull(pager.getPrevPage());
    Assertions.assertEquals(
        "/demo/api/456/organizationUnits.json?page=2&fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getNextPage());
  }

  @Test
  void prevLinkDefaultParameters() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.xml");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              return map;
            });

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertEquals("/demo/api/456/organizationUnits.xml", pager.getPrevPage());
    Assertions.assertNull(pager.getNextPage());
  }

  @Test
  void nextLink() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.xml.gz");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              return map;
            });

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertEquals("/demo/api/456/organizationUnits.xml.gz", pager.getPrevPage());
    Assertions.assertNull(pager.getNextPage());
  }

  @Test
  void nextLinkWithDotsInPath() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("https://play.dhis2.org/2.30/api/30/organizationUnits.xml.gz");
    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/2.30/api/30");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              return map;
            });

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertEquals("/2.30/api/30/organizationUnits.xml.gz", pager.getPrevPage());
    Assertions.assertNull(pager.getNextPage());
  }

  @Test
  void prevLinkParameters() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              map.put("fields", Collections.singletonList("id,name,value[id,text]"));
              map.put("value[x]", Arrays.asList("test1", "test2\u00D8"));
              return map;
            });

    final Pager pager = new Pager(3, 110);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertNull(pager.getNextPage());
    Assertions.assertEquals(
        "/demo/api/456/organizationUnits?page=2&fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getPrevPage());
  }

  @Test
  void prevLinkParametersPage1() {
    Mockito.when(schemaService.getDynamicSchema(Mockito.eq(OrganisationUnit.class)))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    Mockito.when(contextService.getRequest()).thenReturn(request);

    Mockito.when(contextService.getApiPath()).thenReturn("/demo/api/456");

    Mockito.when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation -> {
              final Map<String, List<String>> map = new HashMap<>();
              map.put("page", Collections.singletonList("1"));
              map.put("pageSize", Collections.singletonList("55"));
              map.put("fields", Collections.singletonList("id,name,value[id,text]"));
              map.put("value[x]", Arrays.asList("test1", "test2\u00D8"));
              return map;
            });

    final Pager pager = new Pager(2, 90);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    Assertions.assertNull(pager.getNextPage());
    Assertions.assertEquals(
        "/demo/api/456/organizationUnits?fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getPrevPage());
  }
}
