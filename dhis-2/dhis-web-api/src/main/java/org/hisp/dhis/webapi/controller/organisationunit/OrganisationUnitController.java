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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeQuery;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitByLevelComparator;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.split.orgunit.OrgUnitSplitQuery;
import org.hisp.dhis.split.orgunit.OrgUnitSplitService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.version.VersionService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags("metadata")
@Controller
@RequestMapping(value = OrganisationUnitSchemaDescriptor.API_ENDPOINT)
public class OrganisationUnitController extends AbstractCrudController<OrganisationUnit> {
  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private VersionService versionService;

  @Autowired private OrgUnitSplitService orgUnitSplitService;

  @Autowired private OrgUnitMergeService orgUnitMergeService;

  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ALL') or hasRole('F_ORGANISATION_UNIT_SPLIT')")
  @PostMapping(value = "/split", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage splitOrgUnits(@RequestBody OrgUnitSplitQuery query) {
    orgUnitSplitService.split(orgUnitSplitService.getFromQuery(query));

    return ok("Organisation unit split");
  }

  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ALL') or hasRole('F_ORGANISATION_UNIT_MERGE')")
  @PostMapping(value = "/merge", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage mergeOrgUnits(@RequestBody OrgUnitMergeQuery query) {
    orgUnitMergeService.merge(orgUnitMergeService.getFromQuery(query));

    return ok("Organisation units merged");
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<OrganisationUnit> getEntityList(
      WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders)
      throws QueryParserException {
    List<OrganisationUnit> objects = Lists.newArrayList();

    User currentUser = currentUserService.getCurrentUser();

    boolean anySpecialPropertySet =
        ObjectUtils.anyIsTrue(
            options.isTrue("userOnly"),
            options.isTrue("userDataViewOnly"),
            options.isTrue("userDataViewFallback"),
            options.isTrue("levelSorted"));
    boolean anyQueryPropertySet =
        ObjectUtils.firstNonNull(
                    options.get("query"), options.getInt("level"), options.getInt("maxLevel"))
                != null
            || options.isTrue("withinUserHierarchy")
            || options.isTrue("withinUserSearchHierarchy");
    String memberObject = options.get("memberObject");
    String memberCollection = options.get("memberCollection");

    // ---------------------------------------------------------------------
    // Special parameter handling
    // ---------------------------------------------------------------------

    if (options.isTrue("userOnly")) {
      objects = new ArrayList<>(currentUser.getOrganisationUnits());
    } else if (options.isTrue("userDataViewOnly")) {
      objects = new ArrayList<>(currentUser.getDataViewOrganisationUnits());
    } else if (options.isTrue("userDataViewFallback")) {
      if (currentUser.hasDataViewOrganisationUnit()) {
        objects = new ArrayList<>(currentUser.getDataViewOrganisationUnits());
      } else {
        objects = organisationUnitService.getOrganisationUnitsAtLevel(1);
      }
    } else if (options.isTrue("levelSorted")) {
      objects = new ArrayList<>(manager.getAll(getEntityClass()));
      objects.sort(OrganisationUnitByLevelComparator.INSTANCE);
    }

    // ---------------------------------------------------------------------
    // OrganisationUnitQueryParams query parameter handling
    // ---------------------------------------------------------------------

    else if (anyQueryPropertySet) {
      OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
      params.setQuery(options.get("query"));
      params.setLevel(options.getInt("level"));
      params.setMaxLevels(options.getInt("maxLevel"));

      params.setParents(
          options.isTrue("withinUserHierarchy")
              ? currentUser.getOrganisationUnits()
              : options.isTrue("withinUserSearchHierarchy")
                  ? currentUser.getTeiSearchOrganisationUnitsWithFallback()
                  : Sets.newHashSet());

      objects = organisationUnitService.getOrganisationUnitsByQuery(params);
    }

    // ---------------------------------------------------------------------
    // Standard Query handling
    // ---------------------------------------------------------------------

    Query query =
        queryService.getQueryFromUrl(
            getEntityClass(),
            filters,
            orders,
            getPaginationData(options),
            options.getRootJunction());
    query.setUser(currentUser);
    query.setDefaultOrder();
    query.setDefaults(Defaults.valueOf(options.get("defaults", DEFAULTS)));

    if (anySpecialPropertySet || anyQueryPropertySet) {
      query.setObjects(objects);
    }

    List<OrganisationUnit> list = (List<OrganisationUnit>) queryService.query(query);

    // ---------------------------------------------------------------------
    // Collection member count in hierarchy handling
    // ---------------------------------------------------------------------

    if (memberObject != null && memberCollection != null) {
      Optional<? extends IdentifiableObject> member = manager.find(memberObject);
      if (member.isPresent()) {
        for (OrganisationUnit unit : list) {
          Long count =
              organisationUnitService.getOrganisationUnitHierarchyMemberCount(
                  unit, member.get(), memberCollection);

          unit.setMemberCount((count != null ? count.intValue() : 0));
        }
      }
    }

    return list;
  }

  @Override
  protected List<OrganisationUnit> getEntity(String uid, WebOptions options) {
    OrganisationUnit organisationUnit = manager.get(getEntityClass(), uid);

    List<OrganisationUnit> organisationUnits = Lists.newArrayList();

    if (organisationUnit == null) {
      return organisationUnits;
    }

    if (options.contains("includeChildren")) {
      options.getOptions().put("useWrapper", "true");
      organisationUnits.add(organisationUnit);
      organisationUnits.addAll(organisationUnit.getChildren());
    } else if (options.contains("includeDescendants")) {
      options.getOptions().put("useWrapper", "true");
      organisationUnits.addAll(organisationUnitService.getOrganisationUnitWithChildren(uid));
    } else if (options.contains("includeAncestors")) {
      options.getOptions().put("useWrapper", "true");
      organisationUnits.add(organisationUnit);
      List<OrganisationUnit> ancestors = organisationUnit.getAncestors();
      Collections.reverse(ancestors);
      organisationUnits.addAll(ancestors);
    } else if (options.contains("level")) {
      options.getOptions().put("useWrapper", "true");
      int level = options.getInt("level");
      int ouLevel = organisationUnit.getLevel();
      int targetLevel = ouLevel + level;
      organisationUnits.addAll(
          organisationUnitService.getOrganisationUnitsAtLevel(targetLevel, organisationUnit));
    } else {
      organisationUnits.add(organisationUnit);
    }

    return organisationUnits;
  }

  @GetMapping("/{uid}/parents")
  public @ResponseBody List<OrganisationUnit> getEntityList(
      @PathVariable("uid") String uid,
      @RequestParam Map<String, String> parameters,
      TranslateParams translateParams) {
    setTranslationParams(translateParams);
    OrganisationUnit organisationUnit = manager.get(getEntityClass(), uid);
    List<OrganisationUnit> organisationUnits = Lists.newArrayList();

    if (organisationUnit != null) {
      OrganisationUnit organisationUnitParent = organisationUnit.getParent();

      while (organisationUnitParent != null) {
        organisationUnits.add(organisationUnitParent);
        organisationUnitParent = organisationUnitParent.getParent();
      }
    }

    WebMetadata metadata = new WebMetadata();
    metadata.setOrganisationUnits(organisationUnits);

    return organisationUnits;
  }

  @GetMapping(
      value = {"", ".geojson"},
      produces = {"application/json+geo", "application/json+geojson"})
  public void getGeoJson(
      @RequestParam(value = "level", required = false) List<Integer> rpLevels,
      @OpenApi.Param({UID[].class, OrganisationUnit.class})
          @RequestParam(value = "parent", required = false)
          List<String> rpParents,
      @RequestParam(value = "properties", required = false, defaultValue = "true")
          boolean rpProperties,
      @CurrentUser User currentUser,
      HttpServletResponse response)
      throws IOException {
    rpLevels = rpLevels != null ? rpLevels : new ArrayList<>();
    rpParents = rpParents != null ? rpParents : new ArrayList<>();

    List<OrganisationUnit> parents =
        new ArrayList<>(manager.getByUid(OrganisationUnit.class, rpParents));

    if (rpLevels.isEmpty()) {
      rpLevels.add(1);
    }

    if (parents.isEmpty()) {
      parents.addAll(organisationUnitService.getRootOrganisationUnits());
    }

    List<OrganisationUnit> organisationUnits =
        organisationUnitService.getOrganisationUnitsAtLevels(rpLevels, parents);

    response.setContentType(APPLICATION_JSON_VALUE);

    try (JsonGenerator generator = new JsonFactory().createGenerator(response.getOutputStream())) {

      generator.writeStartObject();
      generator.writeStringField("type", "FeatureCollection");
      generator.writeArrayFieldStart("features");

      for (OrganisationUnit organisationUnit : organisationUnits) {
        writeFeature(generator, organisationUnit, rpProperties, currentUser);
      }

      generator.writeEndArray();
      generator.writeEndObject();
    }
  }

  private void writeFeature(
      JsonGenerator generator,
      OrganisationUnit organisationUnit,
      boolean includeProperties,
      User user)
      throws IOException {
    if (organisationUnit.getGeometry() == null) {
      return;
    }

    generator.writeStartObject();

    generator.writeStringField("type", "Feature");
    generator.writeStringField("id", organisationUnit.getUid());

    generator.writeObjectFieldStart("geometry");
    generator.writeObjectField("type", organisationUnit.getGeometry().getGeometryType());

    generator.writeFieldName("coordinates");
    generator.writeRawValue(getCoordinatesFromGeometry(organisationUnit.getGeometry()));

    generator.writeEndObject();

    generator.writeObjectFieldStart("properties");

    if (includeProperties) {
      Set<OrganisationUnit> roots = user.getDataViewOrganisationUnitsWithFallback();

      generator.writeStringField("code", organisationUnit.getCode());
      generator.writeStringField("name", organisationUnit.getName());
      generator.writeStringField("level", String.valueOf(organisationUnit.getLevel()));

      if (organisationUnit.getParent() != null) {
        generator.writeStringField("parent", organisationUnit.getParent().getUid());
      }

      generator.writeStringField("parentGraph", organisationUnit.getParentGraph(roots));

      generator.writeArrayFieldStart("groups");

      for (OrganisationUnitGroup group : organisationUnit.getGroups()) {
        generator.writeString(group.getUid());
      }

      generator.writeEndArray();
    }

    generator.writeEndObject();

    generator.writeEndObject();
  }

  @Override
  protected void postCreateEntity(OrganisationUnit entity) {
    versionService.updateVersion(VersionService.ORGANISATIONUNIT_VERSION);
  }

  @Override
  protected void postUpdateEntity(OrganisationUnit entity) {
    versionService.updateVersion(VersionService.ORGANISATIONUNIT_VERSION);
  }

  @Override
  protected void postDeleteEntity(String entityUID) {
    versionService.updateVersion(VersionService.ORGANISATIONUNIT_VERSION);
  }
}
