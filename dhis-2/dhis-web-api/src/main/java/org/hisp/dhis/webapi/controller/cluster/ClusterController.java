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
package org.hisp.dhis.webapi.controller.cluster;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.leader.election.LeaderNodeInfo;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ameen Mohamed
 */
@OpenApi.Tags("system")
@RestController
@RequestMapping("/cluster")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ClusterController {

  @Autowired private LeaderManager leaderManager;

  @Autowired private DhisConfigurationProvider dhisConfigurationProvider;

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @GetMapping(value = "/leader")
  @PreAuthorize("hasRole('ALL') or hasRole('F_VIEW_SERVER_INFO')")
  public @ResponseBody LeaderNodeInfo getLeaderInfo() throws WebMessageException {
    LeaderNodeInfo leaderInfo = new LeaderNodeInfo();

    leaderInfo.setLeaderNodeId(leaderManager.getLeaderNodeId());
    leaderInfo.setLeaderNodeUuid(leaderManager.getLeaderNodeUuid());
    leaderInfo.setLeader(leaderManager.isLeader());
    leaderInfo.setCurrentNodeUuid(leaderManager.getCurrentNodeUuid());
    leaderInfo.setCurrentNodeId(dhisConfigurationProvider.getProperty(ConfigurationKey.NODE_ID));

    return leaderInfo;
  }
}
