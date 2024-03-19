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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.Sets;
import java.net.URI;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  private MockMvc mockMvc;

  @Mock private DashboardService dashboardService;

  @Mock private CascadeSharingService cascadeSharingService;

  @Mock protected IdentifiableObjectManager manager;

  @Mock protected MetadataExportService exportService;

  @Mock protected ContextService contextService;

  @InjectMocks private DashboardController dashboardController;

  private static final String ENDPOINT = "/dashboards/q";

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
  }

  @Test
  void verifyEndpointWithNoArgs() throws Exception {
    mockMvc.perform(get(ENDPOINT)).andExpect(status().isOk());

    verify(dashboardService).search(null, null, null);
  }

  @Test
  void verifyEndpointWithSearchParameterArgs() throws Exception {
    mockMvc.perform(get(new URI("/dashboards/search?q=HIV%2FTB"))).andExpect(status().isOk());

    verify(dashboardService).search("HIV/TB", null, null, null);
  }

  @Test
  void verifyEndpointWithMaxArg() throws Exception {
    mockMvc.perform(get(ENDPOINT).param("max", "VISUALIZATION")).andExpect(status().isOk());

    verify(dashboardService).search(Sets.newHashSet(DashboardItemType.VISUALIZATION), null, null);
  }

  @Test
  void verifyEndpointWithAllArg() throws Exception {
    mockMvc
        .perform(
            get(ENDPOINT)
                .param("max", "VISUALIZATION")
                .param("count", "10")
                .param("maxCount", "20"))
        .andExpect(status().isOk());

    verify(dashboardService).search(Sets.newHashSet(DashboardItemType.VISUALIZATION), 10, 20);
  }

  @Test
  void verifyEndpointWithSearchQueryWithNoArgs() throws Exception {
    mockMvc.perform(get(ENDPOINT + "/alfa")).andExpect(status().isOk());

    verify(dashboardService).search("alfa", null, null, null);
  }

  @Test
  void verifyEndpointWithSearchQueryWithMaxArg() throws Exception {
    mockMvc
        .perform(get(ENDPOINT + "/alfa").param("max", "VISUALIZATION"))
        .andExpect(status().isOk());

    verify(dashboardService)
        .search("alfa", Sets.newHashSet(DashboardItemType.VISUALIZATION), null, null);
  }

  @Test
  void verifyEndpointWithSearchQueryWithAllArg() throws Exception {
    mockMvc
        .perform(
            get(ENDPOINT + "/alfa")
                .param("max", "VISUALIZATION")
                .param("count", "10")
                .param("maxCount", "20"))
        .andExpect(status().isOk());

    verify(dashboardService)
        .search("alfa", Sets.newHashSet(DashboardItemType.VISUALIZATION), 10, 20);
  }
}
