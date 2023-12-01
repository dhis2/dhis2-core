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
package org.hisp.dhis.validation.config;

import java.util.Map;
import org.hisp.dhis.common.ServiceProvider;
import org.hisp.dhis.dataanalysis.DataAnalysisService;
import org.hisp.dhis.dataanalysis.MinMaxOutlierAnalysisService;
import org.hisp.dhis.dataanalysis.StdDevOutlierAnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration("validationServiceConfig")
public class ServiceConfig {
  @Bean("dataAnalysisServiceProvider")
  public ServiceProvider<DataAnalysisService> dataAnalysisServiceProvider(
      StdDevOutlierAnalysisService stdDevOutlierAnalysisService,
      MinMaxOutlierAnalysisService minMaxOutlierAnalysisService) {
    ServiceProvider<DataAnalysisService> serviceProvider = new ServiceProvider<>();
    serviceProvider.setServices(
        Map.of(
            "stddevoutlier",
            stdDevOutlierAnalysisService,
            "minmaxoutlier",
            minMaxOutlierAnalysisService));
    return serviceProvider;
  }
}
