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
package org.hisp.dhis.webapi.controller.organisationunit;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags("metadata")
@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/filledOrganisationUnitLevels")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class FilledOrganisationUnitLevelController {
  private final ObjectMapper jsonMapper;

  private final OrganisationUnitService organisationUnitService;

  private final FieldFilterService fieldFilterService;

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<List<ObjectNode>> getList(
      @RequestParam(defaultValue = "*") List<String> fields) {
    List<OrganisationUnitLevel> organisationUnitLevels =
        organisationUnitService.getFilledOrganisationUnitLevels();

    FieldFilterParams<OrganisationUnitLevel> params =
        FieldFilterParams.of(organisationUnitLevels, fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(params);

    return ResponseEntity.ok(objectNodes);
  }

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public void setList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Metadata metadata = jsonMapper.readValue(request.getInputStream(), Metadata.class);

    List<OrganisationUnitLevel> levels = metadata.getOrganisationUnitLevels();

    for (OrganisationUnitLevel level : levels) {
      if (level.getLevel() <= 0) {
        throw new WebMessageException(conflict("Level must be greater than zero"));
      }

      if (StringUtils.isBlank(level.getName())) {
        throw new WebMessageException(conflict("Name must be specified"));
      }

      organisationUnitService.addOrUpdateOrganisationUnitLevel(
          new OrganisationUnitLevel(level.getLevel(), level.getName(), level.getOfflineLevels()));
    }
  }
}
