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
package org.hisp.dhis.analytics.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAnalyticsDimensionServiceTest {

  @Mock private DataQueryService dataQueryService;

  @Mock private AclService aclService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @InjectMocks private DefaultAnalyticsDimensionService service;

  @BeforeEach
  void setUp() {
    CurrentUserUtil.injectUserInSecurityContext(UserDetails.empty().username("tester").build());
  }

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  /**
   * Recommended dimensions are derived solely from the data (dx) dimension. The request must be
   * stripped of all other dimensions (notably ou:LEVEL-*) before it is resolved, so that {@link
   * DataQueryService#getFromRequest} never hydrates and sorts potentially huge organisation unit
   * collections that this endpoint never reads.
   */
  @Test
  void getRecommendedDimensionsResolvesOnlyDataDimension() {
    Set<String> dimensions = new LinkedHashSet<>();
    dimensions.add("dx:uSw8GwPO417.ACTUAL_REPORTS;uSw8GwPO417.EXPECTED_REPORTS");
    dimensions.add("ou:LEVEL-st3hrLkzuMb");
    dimensions.add("pe:LAST_12_MONTHS");

    DataQueryRequest request = DataQueryRequest.newBuilder().dimension(dimensions).build();

    when(dataQueryService.getFromRequest(any())).thenReturn(DataQueryParams.newBuilder().build());

    service.getRecommendedDimensions(request);

    ArgumentCaptor<DataQueryRequest> captor = ArgumentCaptor.forClass(DataQueryRequest.class);
    verify(dataQueryService).getFromRequest(captor.capture());

    assertEquals(
        Set.of("dx:uSw8GwPO417.ACTUAL_REPORTS;uSw8GwPO417.EXPECTED_REPORTS"),
        captor.getValue().getDimension(),
        "Only the data (dx) dimension should be resolved; ou/pe must be stripped");
  }
}
