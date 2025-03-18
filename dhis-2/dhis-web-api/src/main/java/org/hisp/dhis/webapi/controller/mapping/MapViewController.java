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
package org.hisp.dhis.webapi.controller.mapping;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping("/api/mapViews")
@OpenApi.Document(classifiers = {"team:analytics", "purpose:metadata"})
public class MapViewController extends AbstractCrudController<MapView, GetObjectListParams> {
  @Autowired private MappingService mappingService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private MapGenerationService mapGenerationService;

  @Autowired private ContextUtils contextUtils;

  // --------------------------------------------------------------------------
  // Get data
  // --------------------------------------------------------------------------

  @GetMapping(value = {"/{uid}/data", "/{uid}/data.png"})
  public void getMapViewData(@PathVariable String uid, HttpServletResponse response)
      throws Exception {
    MapView mapView = mappingService.getMapView(uid);

    if (mapView == null) {
      throw new WebMessageException(notFound("Map view does not exist: " + uid));
    }

    renderMapViewPng(mapView, response);
  }

  @GetMapping(value = {"/data", "/data.png"})
  public void getMapView(
      Model model,
      @RequestParam(value = "in") String indicatorUid,
      @RequestParam(value = "ou") String organisationUnitUid,
      @RequestParam(value = "level", required = false) Integer level,
      HttpServletResponse response)
      throws Exception {
    if (level == null) {
      OrganisationUnit unit = organisationUnitService.getOrganisationUnit(organisationUnitUid);
      level = unit.getLevel();
      level++;
    }

    MapView mapView =
        mappingService.getIndicatorLastYearMapView(indicatorUid, organisationUnitUid, level);

    renderMapViewPng(mapView, response);
  }

  // --------------------------------------------------------------------------
  // Supportive methods
  // --------------------------------------------------------------------------

  private void renderMapViewPng(MapView mapView, HttpServletResponse response) throws Exception {
    BufferedImage image = mapGenerationService.generateMapImage(mapView);

    if (image != null) {
      contextUtils.configureResponse(
          response,
          ContextUtils.CONTENT_TYPE_PNG,
          CacheStrategy.RESPECT_SYSTEM_SETTING,
          "mapview.png",
          false);

      ImageIO.write(image, "PNG", response.getOutputStream());
    } else {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
  }
}
