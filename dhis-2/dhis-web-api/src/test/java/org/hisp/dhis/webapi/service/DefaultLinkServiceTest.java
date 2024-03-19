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

import static org.hisp.dhis.utils.Assertions.assertEquivalentRelativeUrls;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link DefaultLinkService}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class DefaultLinkServiceTest {
  @Mock private SchemaService schemaService;

  @Mock private ContextService contextService;

  @InjectMocks private DefaultLinkService service;

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  void noLinks() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setApiEndpoint("/organizationUnits");
              return schema;
            });

    final Pager pager = new Pager();
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertNull(pager.getPrevPage());
    assertNull(pager.getNextPage());
  }

  @Test
  void nextLinkDefaultParameters() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits");
    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55")));

    final Pager pager = new Pager(1, 1000);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertNull(pager.getPrevPage());
    assertEquals("/demo/api/456/organizationUnits?page=2", pager.getNextPage());
  }

  @Test
  void nextLinkParameters() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.json");
    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55"),
                    "fields", List.of("id,name,value[id,text]"),
                    "value[x]", List.of("test1", "test2\u00D8")));

    final Pager pager = new Pager(1, 1000);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertNull(pager.getPrevPage());
    assertEquivalentRelativeUrls(
        "/demo/api/456/organizationUnits.json?page=2&fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getNextPage());
  }

  @Test
  void prevLinkDefaultParameters() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.xml");
    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55")));

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertEquals("/demo/api/456/organizationUnits.xml", pager.getPrevPage());
    assertNull(pager.getNextPage());
  }

  @Test
  void nextLink() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("/organizationUnits.xml.gz");
    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55")));

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertEquals("/demo/api/456/organizationUnits.xml.gz", pager.getPrevPage());
    assertNull(pager.getNextPage());
  }

  @Test
  void nextLinkWithDotsInPath() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    request.setRequestURI("https://play.dhis2.org/2.30/api/30/organizationUnits.xml.gz");
    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/2.30/api/30");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55")));

    final Pager pager = new Pager(2, 60);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertEquals("/2.30/api/30/organizationUnits.xml.gz", pager.getPrevPage());
    assertNull(pager.getNextPage());
  }

  @Test
  void prevLinkParameters() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55"),
                    "fields", List.of("id,name,value[id,text]"),
                    "value[x]", List.of("test1", "test2\u00D8")));

    final Pager pager = new Pager(3, 110);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertNull(pager.getNextPage());
    assertEquivalentRelativeUrls(
        "/demo/api/456/organizationUnits?page=2&fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getPrevPage());
  }

  @Test
  void prevLinkParametersPage1() {
    when(schemaService.getDynamicSchema(OrganisationUnit.class))
        .thenAnswer(
            invocation -> {
              Schema schema =
                  new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
              schema.setRelativeApiEndpoint("/organizationUnits");
              return schema;
            });

    when(contextService.getRequest()).thenReturn(request);

    when(contextService.getApiPath()).thenReturn("/demo/api/456");

    when(contextService.getParameterValuesMap())
        .thenAnswer(
            invocation ->
                Map.of(
                    "page", List.of("1"),
                    "pageSize", List.of("55"),
                    "fields", List.of("id,name,value[id,text]"),
                    "value[x]", List.of("test1", "test2\u00D8")));

    final Pager pager = new Pager(2, 90);
    service.generatePagerLinks(pager, OrganisationUnit.class);
    assertNull(pager.getNextPage());
    assertEquivalentRelativeUrls(
        "/demo/api/456/organizationUnits?fields=id%2Cname%2Cvalue%5Bid%2Ctext%5D&value%5Bx%5D=test1&value%5Bx%5D=test2%C3%98",
        pager.getPrevPage());
  }
}
