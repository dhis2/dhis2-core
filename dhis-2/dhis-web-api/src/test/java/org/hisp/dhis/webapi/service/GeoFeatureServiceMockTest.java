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

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotools.geojson.geom.GeometryJSON;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.DefaultDataQueryService;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.webdomain.GeoFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author viet@dhis2.org
 */
@ExtendWith(MockitoExtension.class)
class GeoFeatureServiceMockTest {
  @InjectMocks private GeoFeatureService geoFeatureService;

  @Mock private DefaultDataQueryService dataQueryService;

  @Mock private CurrentUserService currentUserService;

  @Mock private AttributeService attributeService;

  private static final String POINT =
      "{"
          + "\"type\": \"Point\","
          + "\"coordinates\": ["
          + "51.17431641,"
          + "15.53705282"
          + "]"
          + "}";

  private final BeanRandomizer rnd =
      BeanRandomizer.create(OrganisationUnit.class, "parent", "geometry");

  @Test
  void verifyGeoFeaturesReturnsOuData() throws Exception {
    OrganisationUnit ouA = createOrgUnitWithCoordinates();
    OrganisationUnit ouB = createOrgUnitWithCoordinates();
    OrganisationUnit ouC = createOrgUnitWithCoordinates();
    // This ou should be filtered out since it has no Coordinates
    OrganisationUnit ouD = createOrgUnitWithoutCoordinates();

    User user = rnd.nextObject(User.class);
    DataQueryParams params =
        DataQueryParams.newBuilder().withOrganisationUnits(getList(ouA, ouB, ouC, ouD)).build();

    when(dataQueryService.getFromRequest(any())).thenReturn(params);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    List<GeoFeature> features =
        geoFeatureService.getGeoFeatures(
            GeoFeatureService.Parameters.builder()
                .request(request)
                .response(response)
                .organisationUnit("ou:LEVEL-2;LEVEL-3")
                .build());

    assertEquals(3, features.size());
  }

  /**
   * GET Request has "coordinateField" parameter.
   *
   * <p>OrganisationUnit has coordinates from geometry property but not GeoJson Attribute.
   *
   * <p>Expected: only return GeoFeature which has the coordinateField value.
   */
  @Test
  void testGeoJsonAttributeWithNoValue() throws IOException {
    OrganisationUnit ouA = createOrgUnitWithCoordinates();
    User user = rnd.nextObject(User.class);
    DataQueryParams params =
        DataQueryParams.newBuilder().withOrganisationUnits(getList(ouA)).build();

    when(dataQueryService.getFromRequest(any())).thenReturn(params);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    Attribute attribute = new Attribute();
    attribute.setValueType(ValueType.GEOJSON);
    attribute.setOrganisationUnitAttribute(true);
    when(attributeService.getAttribute("GeoJSON_Attribute_ID")).thenReturn(attribute);

    List<GeoFeature> features =
        geoFeatureService.getGeoFeatures(
            GeoFeatureService.Parameters.builder()
                .request(request)
                .response(response)
                .coordinateField("GeoJSON_Attribute_ID")
                .organisationUnit("ou:LEVEL-2;LEVEL-3")
                .build());

    assertEquals(0, features.size());
  }

  private OrganisationUnit createOrgUnitWithoutCoordinates() {
    return rnd.nextObject(OrganisationUnit.class);
  }

  private OrganisationUnit createOrgUnitWithCoordinates() throws IOException {
    OrganisationUnit ou = createOrgUnitWithoutCoordinates();
    ou.setGeometry(new GeometryJSON().read(POINT));
    return ou;
  }
}
