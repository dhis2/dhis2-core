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
package org.hisp.dhis.webapi.service;

import static java.util.Map.entry;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_GROUP_DIM_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.GeoJsonObjectVisitor;
import org.geojson.GeometryCollection;
import org.geojson.LineString;
import org.geojson.MultiLineString;
import org.geojson.MultiPoint;
import org.geojson.MultiPolygon;
import org.geojson.Point;
import org.geojson.Polygon;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.coordinate.CoordinateObject;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.GeoFeature;
import org.springframework.stereotype.Service;

/**
 * Take the request parameters from {@link
 * org.hisp.dhis.webapi.controller.mapping.GeoFeatureController}, process it then return List of
 * {@link GeoFeature}
 *
 * @author viet@dhis2.org
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GeoFeatureService {

  private final DataQueryService dataQueryService;

  private final OrganisationUnitGroupService organisationUnitGroupService;

  private final UserService userService;

  private final AttributeService attributeService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * The {@link GeoFeature#getTy} in the response is integer, so we need to map {@link FeatureType}
   * to integer and return to client.
   */
  private static final Map<FeatureType, Integer> FEATURE_TYPE_MAP =
      Map.ofEntries(
          entry(FeatureType.POINT, GeoFeature.TYPE_POINT),
          entry(FeatureType.MULTI_POLYGON, GeoFeature.TYPE_POLYGON),
          entry(FeatureType.POLYGON, GeoFeature.TYPE_POLYGON));

  /**
   * Returns a list of {@link GeoFeature}. Returns null if not modified based on the request.
   *
   * @param parameters the {@link Parameters} passing from controller.
   * @return a list of geo features or null.
   */
  public List<GeoFeature> getGeoFeatures(Parameters parameters) {
    Attribute geoJsonAttribute = validateCoordinateField(parameters.getCoordinateField());

    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(parameters.getOrganisationUnit());
    dimensionParams.add(parameters.getOrganisationUnitGroupId());

    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder()
            .dimension(dimensionParams)
            .aggregationType(AggregationType.SUM)
            .displayProperty(parameters.getDisplayProperty())
            .relativePeriodDate(parameters.getRelativePeriodDate())
            .userOrgUnit(parameters.getUserOrgUnit())
            .build();

    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);

    boolean useOrgUnitGroup = parameters.getOrganisationUnit() == null;
    DimensionalObject dimensionalObject =
        params.getDimension(useOrgUnitGroup ? ORGUNIT_GROUP_DIM_ID : ORGUNIT_DIM_ID);

    if (dimensionalObject == null) {
      throw new IllegalArgumentException(
          "Dimension is present in query without any valid dimension options");
    }

    List<DimensionalItemObject> dimensionalItemObjects =
        DimensionalObjectUtils.asTypedList(dimensionalObject.getItems());

    dimensionalItemObjects =
        dimensionalItemObjects.stream()
            .filter(object -> validateDimensionalItemObject(object, geoJsonAttribute))
            .collect(Collectors.toList());

    if (ContextUtils.isNotModified(
        parameters.getRequest(), parameters.getResponse(), dimensionalItemObjects)) {
      return null;
    }

    return getGeoFeatures(
        params,
        dimensionalItemObjects,
        parameters.isIncludeGroupSets(),
        useOrgUnitGroup,
        geoJsonAttribute);
  }

  /**
   * Returns a list of {@link GeoFeature}.
   *
   * @param params the {@link DataQueryParams}.
   * @param dimensionalItemObjects the list of {@link DimensionalItemObject}.
   * @param includeGroupSets whether to include group sets.
   * @param useOrgUnitGroup whether to use org unit group when retrieving features.
   * @param geoJsonAttribute OrganisationUnit attribute used for retrieving {@link GeoJsonObject}
   * @return a list of {@link GeoFeature}.
   */
  private List<GeoFeature> getGeoFeatures(
      DataQueryParams params,
      List<DimensionalItemObject> dimensionalItemObjects,
      boolean includeGroupSets,
      boolean useOrgUnitGroup,
      Attribute geoJsonAttribute) {
    List<GeoFeature> features = new ArrayList<>();

    List<OrganisationUnitGroupSet> groupSets =
        includeGroupSets
            ? organisationUnitGroupService.getAllOrganisationUnitGroupSets()
            : new ArrayList<>();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    Set<OrganisationUnit> roots = currentUser.getDataViewOrganisationUnitsWithFallback();

    for (DimensionalItemObject unit : dimensionalItemObjects) {
      GeoFeature feature = new GeoFeature();

      CoordinateObject coordinateObject = (CoordinateObject) unit;

      feature.setId(unit.getUid());
      feature.setCode(unit.getCode());
      feature.setHcd(coordinateObject.hasDescendantsWithCoordinates());

      if (!useOrgUnitGroup) {
        OrganisationUnit castUnit = (OrganisationUnit) unit;
        feature.setHcu(castUnit.hasCoordinatesUp());
        feature.setLe(castUnit.getLevel());
        feature.setPg(castUnit.getParentGraph(roots));
        feature.setPi(castUnit.getParent() != null ? castUnit.getParent().getUid() : null);
        feature.setPn(castUnit.getParent() != null ? castUnit.getParent().getDisplayName() : null);

        if (includeGroupSets) {
          for (OrganisationUnitGroupSet groupSet : groupSets) {
            OrganisationUnitGroup group = castUnit.getGroupInGroupSet(groupSet);

            if (group != null) {
              feature.getDimensions().put(groupSet.getUid(), group.getUid());
            }
          }
        }
      }

      updateFeatureCoordinates(feature, unit, geoJsonAttribute);

      feature.setNa(unit.getDisplayProperty(params.getDisplayProperty()));
      features.add(feature);
    }

    features.sort(Comparator.comparing(GeoFeature::getTy));

    return features;
  }

  /**
   * Updates the {@link GeoFeature} coordinate from {@link DimensionalItemObject}
   *
   * @param target the {@link GeoFeature}
   * @param source the {@link DimensionalItemObject} contains the coordinate values.
   */
  private void updateFeatureCoordinates(GeoFeature target, DimensionalItemObject source) {
    if (!(source instanceof CoordinateObject coordinateObject)) return;

    Integer ty =
        coordinateObject.getFeatureType() != null
            ? FEATURE_TYPE_MAP.get(coordinateObject.getFeatureType())
            : null;
    target.setCo(coordinateObject.getCoordinates());
    target.setTy(ObjectUtils.firstNonNull(ty, 0));
  }

  /**
   * Updates the {@link GeoFeature} coordinate from {@link DimensionalItemObject}
   *
   * <p>The coordinate value is retrieved from {@link DimensionalItemObject}'s geoJsonAttribute
   * value.
   *
   * @param target the {@link GeoFeature}
   * @param source the {@link DimensionalItemObject} contains the coordinate values.
   * @param geoJsonAttribute The {@link Attribute} which has {@link ValueType#GEOJSON} and is
   *     assigned to {@link OrganisationUnit}.
   */
  private void updateFeatureCoordinates(
      GeoFeature target, DimensionalItemObject source, Attribute geoJsonAttribute) {
    if (geoJsonAttribute == null) {
      updateFeatureCoordinates(target, source);
      return;
    }

    if (!(source instanceof OrganisationUnit organisationUnit)) return;

    String value = organisationUnit.getAttributeValues().get(geoJsonAttribute.getUid());

    if (value == null || StringUtils.isBlank(value)) {
      updateFeatureCoordinates(target, source);
      return;
    }

    try {
      GeoJsonObject geoJsonObject = OBJECT_MAPPER.readValue(value, GeoJsonObject.class);
      GeoFeature geoJsonFeature = geoJsonObject.accept(new GeoFeatureVisitor());

      if (geoJsonFeature == null) {
        return;
      }

      target.setTy(geoJsonFeature.getTy());
      target.setCo(geoJsonFeature.getCo());
    } catch (JsonProcessingException e) {
      log.error(
          String.format("Couldn't read GeoJson value from organisationUnit %s: ", organisationUnit),
          e);
      updateFeatureCoordinates(target, source);
    }
  }

  /**
   * Check if the given coordinateField is a valid {@link Attribute} ID
   *
   * <p>Also check if that Attribute has {@link ValueType#GEOJSON} and is assigned to {@link
   * OrganisationUnit}
   *
   * @param coordinateField the {@link Attribute} ID
   * @return the {@link Attribute} if valid, otherwise return throws {@link
   *     IllegalArgumentException}
   */
  private Attribute validateCoordinateField(String coordinateField) {
    if (StringUtils.isBlank(coordinateField)) {
      return null;
    }

    Attribute attribute = attributeService.getAttribute(coordinateField);
    if (attribute == null) {
      throw new IllegalArgumentException("Invalid coordinateField: " + coordinateField);
    }

    if (attribute.getValueType() != ValueType.GEOJSON) {
      throw new IllegalArgumentException(
          "ValueType of coordinateField must be GeoJSON but found: " + attribute.getValueType());
    }

    if (!attribute.getSupportedClasses().contains(OrganisationUnit.class)) {
      throw new IllegalArgumentException(
          "coordinateField does not support OrganisationUnit: " + attribute.getName());
    }

    return attribute;
  }

  /**
   * Convert {@link GeoJsonObject} to {@link GeoFeature}
   *
   * <p>Return null if GeoJsonObject type is not supported
   */
  static class GeoFeatureVisitor implements GeoJsonObjectVisitor<GeoFeature> {
    @Override
    public GeoFeature visit(GeometryCollection geometryCollection) {
      // Not support type
      return null;
    }

    @Override
    public GeoFeature visit(FeatureCollection featureCollection) {
      // Not support type
      return null;
    }

    @Override
    public GeoFeature visit(Point point) {
      GeoFeature geoFeature = new GeoFeature();
      geoFeature.setTy(GeoFeature.TYPE_POINT);
      geoFeature.setCo(convertGeoJsonObjectCoordinates(List.of(point.getCoordinates())));
      return geoFeature;
    }

    @Override
    public GeoFeature visit(Feature feature) {
      // Not support type
      return null;
    }

    @Override
    public GeoFeature visit(MultiLineString multiLineString) {
      // Not support type
      return null;
    }

    @Override
    public GeoFeature visit(Polygon polygon) {
      GeoFeature geoFeature = new GeoFeature();
      geoFeature.setTy(GeoFeature.TYPE_POLYGON);
      geoFeature.setCo(convertGeoJsonObjectCoordinates(polygon.getCoordinates()));

      return geoFeature;
    }

    @Override
    public GeoFeature visit(MultiPolygon multiPolygon) {
      GeoFeature geoFeature = new GeoFeature();
      geoFeature.setTy(GeoFeature.TYPE_POLYGON);
      geoFeature.setCo(convertGeoJsonObjectCoordinates(multiPolygon.getCoordinates()));
      return geoFeature;
    }

    @Override
    public GeoFeature visit(MultiPoint multiPoint) {
      GeoFeature geoFeature = new GeoFeature();
      geoFeature.setTy(GeoFeature.TYPE_POINT);
      geoFeature.setCo(convertGeoJsonObjectCoordinates(multiPoint.getCoordinates()));
      return geoFeature;
    }

    @Override
    public GeoFeature visit(LineString lineString) {
      GeoFeature geoFeature = new GeoFeature();
      geoFeature.setTy(GeoFeature.TYPE_POLYGON);
      geoFeature.setCo(convertGeoJsonObjectCoordinates(lineString.getCoordinates()));
      return geoFeature;
    }

    /**
     * Convert coordinates of an {@link GeoJsonObject} to String
     *
     * @param coordinates the coordinate of a GeoJsonObject, usually is a list of {@link
     *     org.geojson.LngLatAlt}
     * @return a String contains given GeoJsonObject's coordinates. Return null if failed to
     *     convert.
     */
    private static String convertGeoJsonObjectCoordinates(List<?> coordinates) {
      try {
        return OBJECT_MAPPER.writeValueAsString(coordinates);
      } catch (JsonProcessingException e) {
        log.error(
            String.format("Failed to write coordinate to String: %s", coordinates),
            DebugUtils.getStackTrace(e));
      }

      return null;
    }
  }

  /**
   * Contains all parameters from {@link
   * org.hisp.dhis.webapi.controller.mapping.GeoFeatureController}
   */
  @Getter
  @Builder
  public static class Parameters {
    /**
     * OrganisationUnit parameter, can include both organisationUnitLevel and OrganisationUnitID.
     *
     * <p>The format is: ou:LEVEL-{levelNumber};{OrganisationUnit ID}
     *
     * <p>Example: To retrieve geo features for organisation units at a level within the boundary of
     * an organisation unit (e.g. at level 2)
     *
     * <p>ou:LEVEL-4;O6uvpzGd5pu
     *
     * <p>Example: to retrieve geo features for all organisation units at level 3 in the
     * organisation unit hierarchy
     *
     * <p>ou:LEVEL-3
     */
    private String organisationUnit;

    /** OrganisationUnit Group ID */
    private String organisationUnitGroupId;

    /** Display property */
    private DisplayProperty displayProperty;

    /** relativePeriodDate the date to use as basis for relative periods. */
    private Date relativePeriodDate;

    /** the user organisation unit parameter. */
    private String userOrgUnit;

    /**
     * The {@link Attribute} ID which should must have {@link
     * org.hisp.dhis.common.ValueType#GEOJSON}
     *
     * <p>If this parameter is provided, then the coordinates will be retrieved from
     *
     * <p>{@link OrganisationUnit}'s GeoJSON attribute value instead of the {@link
     * OrganisationUnit}'s geometry field.
     */
    private String coordinateField;

    /** the HTTP request. */
    private HttpServletRequest request;

    /** the HTTP response. */
    private HttpServletResponse response;

    /** whether to include organisation unit group sets. */
    private boolean includeGroupSets;
  }

  /**
   * Check if the given DimensionalItemObject has coordinates.
   *
   * @param object {@link DimensionalItemObject}
   * @return true if given object has coordinates, otherwise return false.
   */
  private boolean validateDimensionalItemObject(
      IdentifiableObject object, Attribute geoJsonAttribute) {
    if (geoJsonAttribute != null) {
      return hasGeoJsonAttributeCoordinates(object, geoJsonAttribute);
    }
    return hasGeometryCoordinates(object);
  }

  /**
   * Check if given object has {@link org.locationtech.jts.geom.Geometry} property and it has valid
   * coordinates.
   *
   * @param object the object for validating
   * @return true if given object has valid Geometry coordinates, false otherwise.
   */
  private boolean hasGeometryCoordinates(Object object) {
    CoordinateObject coordinateObject = (CoordinateObject) object;
    return coordinateObject != null
        && coordinateObject.getFeatureType() != null
        && coordinateObject.hasCoordinates()
        && (coordinateObject.getFeatureType() != FeatureType.POINT
            || ValidationUtils.coordinateIsValid(coordinateObject.getCoordinates()));
  }

  /**
   * Check if given object has GeoJson Attribute and its value is not blank.
   *
   * @param object the {@link BaseIdentifiableObject} for validating.
   * @param geoJsonAttribute the {@link Attribute} which has {@link ValueType#GEOJSON}.
   * @return true if given object has GeoJson coordinates, false otherwise.
   */
  private boolean hasGeoJsonAttributeCoordinates(
      IdentifiableObject object, Attribute geoJsonAttribute) {
    return geoJsonAttribute != null
        && isNotBlank(object.getAttributeValues().get(geoJsonAttribute.getUid()));
  }
}
