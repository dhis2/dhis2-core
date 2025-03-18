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
package org.hisp.dhis.webapi.controller.user;

import static org.hisp.dhis.security.Authorities.F_ACCEPT_DATA_LOWER_LEVELS;
import static org.hisp.dhis.security.Authorities.F_APPROVE_DATA;
import static org.hisp.dhis.security.Authorities.F_APPROVE_DATA_LOWER_LEVELS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Jim Grace
 */
@Component
@RequiredArgsConstructor
public class UserControllerUtils {

  private final DataApprovalService dataApprovalService;
  private final DataApprovalLevelService dataApprovalLevelService;
  private final AclService aclService;
  private final SystemSettingsProvider settingsProvider;

  /**
   * Gets the data approval workflows a user can see, including the workflow levels accessible to
   * the user and the actions (if any) they can take at those levels to approve (and accept if
   * configured) data.
   *
   * @param user the user
   */
  public ObjectNode getUserDataApprovalWorkflows(User user) {
    ObjectMapper objectMapper = JacksonObjectMapperConfig.staticJsonMapper();

    ObjectNode objectNode = objectMapper.createObjectNode();
    ArrayNode arrayNode = objectNode.putArray("dataApprovalWorkflows");

    for (DataApprovalWorkflow workflow : dataApprovalService.getAllWorkflows()) {
      if (!aclService.canRead(user, workflow)) {
        continue;
      }

      ObjectNode node =
          objectMapper
              .createObjectNode()
              .put("id", workflow.getUid())
              .put("name", workflow.getName());

      node.set("dataApprovalLevels", getWorkflowLevelNodes(user, workflow));

      arrayNode.add(node);
    }

    /*
     * collectionNode.getUnorderedChildren() .sort( Comparator.comparing( c
     * -> (String) ((SimpleNode) c.getUnorderedChildren().get( 0
     * )).getValue() ) );
     *
     * RootNode rootNode = NodeUtils.createRootNode( "dataApprovalWorkflows"
     * ); rootNode.addChild( collectionNode );
     */

    return objectNode;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * For a user and workflow, returns a list of levels accessible to the user user and the actions
   * (if any) they can take at those levels to approve (and accept if configured) data.
   *
   * @param user the user
   * @param workflow the approval workflow for which to fetch the levels
   * @return a node with the ordered list of data approval levels
   */
  private ArrayNode getWorkflowLevelNodes(User user, DataApprovalWorkflow workflow) {
    boolean canApprove = user.isAuthorized(F_APPROVE_DATA);
    boolean canApproveLowerLevels = user.isAuthorized(F_APPROVE_DATA_LOWER_LEVELS);
    boolean canAccept = user.isAuthorized(F_ACCEPT_DATA_LOWER_LEVELS);

    boolean acceptConfigured =
        settingsProvider.getCurrentSettings().getAcceptanceRequiredForApproval();

    int lowestUserOrgUnitLevel = getLowsetUserOrgUnitLevel(user);

    ArrayNode levelNodes = JacksonObjectMapperConfig.staticJsonMapper().createArrayNode();

    boolean highestLevelInWorkflow = true;

    for (DataApprovalLevel level :
        dataApprovalLevelService.getUserDataApprovalLevels(user, workflow)) {
      if (level.getOrgUnitLevel() < lowestUserOrgUnitLevel) {
        continue;
      }

      ObjectNode levelNode =
          levelNodes
              .addObject()
              .put("name", level.getName())
              .put("id", level.getUid())
              .put("level", level.getLevel())
              .put("approve", (canApprove && highestLevelInWorkflow) || canApproveLowerLevels);

      if (acceptConfigured) {
        levelNode.put("accept", canAccept && !highestLevelInWorkflow);
      }

      levelNodes.add(levelNode);

      highestLevelInWorkflow = false;
    }

    return levelNodes;
  }

  private int getLowsetUserOrgUnitLevel(User user) {
    Set<OrganisationUnit> userOrgUnits = user.getOrganisationUnits();

    return userOrgUnits.isEmpty()
        ? 9999
        : userOrgUnits.stream()
            .map(OrganisationUnit::getHierarchyLevel)
            .min(Integer::compare)
            .get();
  }
}
