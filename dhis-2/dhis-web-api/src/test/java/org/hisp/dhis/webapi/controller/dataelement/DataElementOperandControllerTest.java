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
package org.hisp.dhis.webapi.controller.dataelement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig.staticJsonMapper;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.DefaultNodeService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.serializers.Jackson2JsonNodeSerializer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.query.DefaultQueryParser;
import org.hisp.dhis.query.DefaultQueryService;
import org.hisp.dhis.query.InMemoryQueryEngine;
import org.hisp.dhis.query.JpaCriteriaQueryEngine;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.planner.DefaultQueryPlanner;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter;
import org.hisp.dhis.webapi.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DataElementOperandControllerTest {
  private MockMvc mockMvc;

  @Mock private IdentifiableObjectManager manager;

  @Mock private FieldFilterService fieldFilterService;

  @Mock private LinkService linkService;

  @Mock private SchemaService schemaService;

  @Mock private CategoryService dataElementCategoryService;

  @Mock private UserService userService;

  private QueryService queryService;

  private static final String ENDPOINT = "/api/dataElementOperands";

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @BeforeEach
  public void setUp() {
    injectSecurityContextNoSettings(new SystemUser());

    QueryService _queryService =
        new DefaultQueryService(
            new DefaultQueryParser(schemaService),
            new DefaultQueryPlanner(schemaService),
            mock(JpaCriteriaQueryEngine.class),
            new InMemoryQueryEngine(schemaService, mock(AclService.class)));
    // Use "spy" on queryService, because we want a partial mock: we only
    // want to
    // mock the method "count"
    queryService = spy(_queryService);

    // Controller under test
    final DataElementOperandController controller =
        new DataElementOperandController(
            manager, queryService, fieldFilterService, linkService, dataElementCategoryService);

    // Set custom Node Message converter //
    Jackson2JsonNodeSerializer serializer = new Jackson2JsonNodeSerializer(staticJsonMapper());
    NodeService nodeService = new DefaultNodeService(List.of(serializer));
    ReflectionTestUtils.invokeMethod(nodeService, "init");

    JsonMessageConverter jsonMessageConverter =
        new JsonMessageConverter(nodeService, Compression.NONE);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(jsonMessageConverter)
            .build();
    // End mockmvc setup

    when(schemaService.getDynamicSchema(DataElementOperand.class))
        .thenReturn(new Schema(DataElementOperand.class, "", ""));

    User user = rnd.nextObject(User.class);
    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);
  }

  @Test
  void verifyPaginationGetFirstPage() throws Exception {
    int pageSize = 15;
    long totalSize = 150;

    // Given
    final List<DataElement> dataElements =
        rnd.objects(DataElement.class, 1).collect(Collectors.toList());

    when(manager.getAllSorted(DataElement.class)).thenReturn(dataElements);

    final List<DataElementOperand> dataElementOperands =
        rnd.objects(DataElementOperand.class, (int) totalSize).collect(Collectors.toList());
    when(dataElementCategoryService.getOperands(dataElements, true))
        .thenReturn(
            rnd.objects(DataElementOperand.class, (int) totalSize).collect(Collectors.toList()));

    final List<DataElementOperand> first50elements = dataElementOperands.subList(0, pageSize);
    ArgumentCaptor<FieldFilterParams> filterParamsArgumentCaptor =
        ArgumentCaptor.forClass(FieldFilterParams.class);

    when(fieldFilterService.toCollectionNode(
            eq(DataElementOperand.class), filterParamsArgumentCaptor.capture()))
        .thenReturn(buildResponse(first50elements));

    doReturn(totalSize).when(queryService).count(any(Query.class));

    // Then
    ResultActions resultActions =
        mockMvc
            .perform(
                get(ENDPOINT).param("totals", "true").param("pageSize", Integer.toString(pageSize)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.pager.pageSize").value(Integer.toString(pageSize)))
            .andExpect(jsonPath("$.pager.page").value("1"))
            .andExpect(jsonPath("$.pager.total").value(Long.toString(totalSize)))
            .andExpect(jsonPath("$.pager.pageCount").value(Long.toString(totalSize / pageSize)))
            .andExpect(jsonPath("$.dataElementOperands", hasSize(pageSize)));

    final FieldFilterParams fieldFilterParams = filterParamsArgumentCaptor.getValue();
    assertThat(fieldFilterParams.getObjects(), hasSize(pageSize));
    assertThat(fieldFilterParams.getFields(), Matchers.is(Lists.newArrayList("*")));

    // Make sure that the first and last element in the page matches with
    // the
    // original list
    List<Map<String, Object>> jsonDataElementOperands = convertResponse(resultActions);

    assertThat(
        jsonDataElementOperands.get(0).get("id"),
        Matchers.is(dataElementOperands.get(0).getDimensionItem()));
    assertThat(
        jsonDataElementOperands.get(pageSize - 1).get("id"),
        Matchers.is(dataElementOperands.get(pageSize - 1).getDimensionItem()));
  }

  @Test
  void verifyPaginationGetThirdPage() throws Exception {
    int pageSize = 25;
    long totalSize = 100;

    // Given
    final List<DataElement> dataElements =
        rnd.objects(DataElement.class, 1).collect(Collectors.toList());

    when(manager.getAllSorted(DataElement.class)).thenReturn(dataElements);

    final List<DataElementOperand> dataElementOperands =
        rnd.objects(DataElementOperand.class, (int) totalSize).collect(Collectors.toList());
    when(dataElementCategoryService.getOperands(dataElements, true))
        .thenReturn(
            rnd.objects(DataElementOperand.class, (int) totalSize).collect(Collectors.toList()));

    final List<DataElementOperand> thirdPageElements =
        dataElementOperands.subList(50, 50 + pageSize);
    ArgumentCaptor<FieldFilterParams> filterParamsArgumentCaptor =
        ArgumentCaptor.forClass(FieldFilterParams.class);

    when(fieldFilterService.toCollectionNode(
            eq(DataElementOperand.class), filterParamsArgumentCaptor.capture()))
        .thenReturn(buildResponse(thirdPageElements));

    doReturn(totalSize).when(queryService).count(any(Query.class));

    // Then
    ResultActions resultActions =
        mockMvc
            .perform(
                get(ENDPOINT)
                    .param("totals", "true")
                    .param("pageSize", Integer.toString(pageSize))
                    .param("page", Integer.toString(3)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.pager.pageSize").value(Integer.toString(pageSize)))
            .andExpect(jsonPath("$.pager.page").value("3"))
            .andExpect(jsonPath("$.pager.total").value(Long.toString(totalSize)))
            .andExpect(jsonPath("$.pager.pageCount").value(Long.toString(totalSize / pageSize)))
            .andExpect(jsonPath("$.dataElementOperands", hasSize(pageSize)));

    final FieldFilterParams fieldFilterParams = filterParamsArgumentCaptor.getValue();
    assertThat(fieldFilterParams.getObjects(), hasSize(pageSize));
    assertThat(fieldFilterParams.getFields(), Matchers.is(Lists.newArrayList("*")));

    // Make sure that the first and last element in the page matches with
    // the
    // original list
    List<Map<String, Object>> jsonDataElementOperands = convertResponse(resultActions);

    assertThat(
        jsonDataElementOperands.get(0).get("id"),
        Matchers.is(dataElementOperands.get(50).getDimensionItem()));
    assertThat(
        jsonDataElementOperands.get(pageSize - 1).get("id"),
        Matchers.is(dataElementOperands.get(74).getDimensionItem()));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> convertResponse(ResultActions resultActions)
      throws UnsupportedEncodingException {
    return JsonPath.parse(resultActions.andReturn().getResponse().getContentAsString())
        .read("$.dataElementOperands", List.class);
  }

  private CollectionNode buildResponse(List<DataElementOperand> dataElementOperands) {
    CollectionNode collectionNode = new CollectionNode("dataElementOperands");
    collectionNode.setWrapping(true);
    collectionNode.setNamespace("http://dhis2.org/schema/dxf/2.0");

    for (DataElementOperand dataElementOperand : dataElementOperands) {
      ComplexNode complexNode = new ComplexNode("dataElementOperand");
      complexNode.setNamespace(collectionNode.getNamespace());
      complexNode.addChild(
          new SimpleNode("displayName", dataElementOperand.getDisplayName(), false));
      complexNode.addChild(new SimpleNode("id", dataElementOperand.getDimensionItem(), true));
      collectionNode.addChild(complexNode);
    }

    return collectionNode;
  }
}
