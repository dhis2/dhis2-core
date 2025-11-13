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
package org.hisp.dhis.webapi.controller.organisationunit;

import static java.lang.Math.max;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.query.Filters.eq;
import static org.hisp.dhis.query.Filters.in;
import static org.hisp.dhis.query.Filters.le;
import static org.hisp.dhis.query.Filters.like;
import static org.hisp.dhis.query.Filters.token;
import static org.hisp.dhis.security.Authorities.F_ORGANISATION_UNIT_MERGE;
import static org.hisp.dhis.security.Authorities.F_ORGANISATION_UNIT_SPLIT;
import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeQuery;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.split.orgunit.OrgUnitSplitQuery;
import org.hisp.dhis.split.orgunit.OrgUnitSplitService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.version.VersionService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Controller
@RequestMapping("/api/organisationUnits")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class OrganisationUnitController
    extends AbstractCrudController<
        OrganisationUnit, OrganisationUnitController.GetOrganisationUnitObjectListParams> {

  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private VersionService versionService;
  @Autowired private OrgUnitSplitService orgUnitSplitService;
  @Autowired private OrgUnitMergeService orgUnitMergeService;

  @ResponseStatus(HttpStatus.OK)
  @RequiresAuthority(anyOf = F_ORGANISATION_UNIT_SPLIT)
  @PostMapping(value = "/split", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage splitOrgUnits(@RequestBody OrgUnitSplitQuery query) {
    orgUnitSplitService.split(orgUnitSplitService.getFromQuery(query));

    return ok("Organisation unit split");
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @OpenApi.Property
  public static final class GetOrganisationUnitObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        """
      When set for each organisation unit in the result list a count is added as property `memberCount`.
      This count is the number of organisation units in the unit's subtree (including itself) where
      the `memberObject` is a member of the relation defined by the `memberCollection` property.
      Use with caution, as this is an expensive operation.
      """)
    @OpenApi.Property(UID.class)
    String memberObject;

    @OpenApi.Description(
        """
      Name of the organisation unit collection property to use when checking of the `memberObject` is a member.
      For example, `groups`, `dataSets`, `programs`, `users`, `categoryOptions`.
      """)
    String memberCollection;

    @OpenApi.Ignore Integer parentLevel;

    @OpenApi.Description(
        """
      Limits results to organisation units on the given level (absolute starting with 1 for the root).
      When used for list relative to a parent this is the level relative to the parent level where `level=1` are
      all direct children of the parent.
      Can be combined with further `filter`s and/or one of the `withinUser*`/`userOnly*` limitations.
      """)
    Integer level;

    @OpenApi.Description(
        """
      Limits results to organisation units on the given level or above (absolute starting with 1 for the root).
      Can be combined with further `filter`s and/or one of the `withinUser*`/`userOnly*` limitations.
      """)
    Integer maxLevel;

    @OpenApi.Description(
        """
      Limits result to organisation units for which current user has data capture privileges.
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean withinUserHierarchy;

    @OpenApi.Description(
        """
      Limits result to organisation units for which the current user has data view privileges.
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean withinDataViewUserHierarchy;

    @OpenApi.Description(
        """
      Limits result to organisation units for which the current user has search privileges.
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean withinUserSearchHierarchy;

    @OpenApi.Description(
        """
      Limits result to organisation units that are explicitly listed in the current user's data capture set (excluding any non-listed children).
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean userOnly;

    @OpenApi.Description(
        """
      Limits result to organisation units that are explicitly listed in the current user's data view set (excluding any non-listed children).
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean userDataViewOnly;

    @OpenApi.Description(
        """
      Limits result to organisation units that are explicitly listed in the current user's data view set (excluding any non-listed children)
      with fallback to the user's data capture set if the data view set is empty.
      Can be combined with further `filter`s and/or one of the `level`/`maxLevel` limitations.
      """)
    boolean userDataViewFallback;

    @OpenApi.Description("Shorthand equivalent for the URL parameter `order=level:asc`.")
    boolean levelSorted;
  }

  @ResponseStatus(HttpStatus.OK)
  @RequiresAuthority(anyOf = F_ORGANISATION_UNIT_MERGE)
  @PostMapping(value = "/merge", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage mergeOrgUnits(@RequestBody OrgUnitMergeQuery query) {
    orgUnitMergeService.merge(orgUnitMergeService.getFromQuery(query));

    return ok("Organisation units merged");
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping(value = "/{uid}", params = "includeChildren=true")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getIncludeChildren(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    return getChildren(uid, params, response, currentUser);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping("/{uid}/children")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getChildren(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    OrganisationUnit parent = getEntity(uid);
    List<Filter> children =
        List.of(
            in("level", List.of(parent.getLevel(), parent.getLevel() + 1)),
            like("path", uid, MatchMode.ANYWHERE));
    return getObjectListWith(params, response, currentUser, children);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping(value = "/{uid}", params = "level")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getObjectWithLevel(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      @RequestParam int level,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    return getChildrenWithLevel(uid, level, params, response, currentUser);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping(value = "/{uid}/children", params = "level")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getChildrenWithLevel(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      @RequestParam int level,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    OrganisationUnit parent = getEntity(uid);
    List<Filter> childrenWithLevel = List.of(like("path", parent.getStoredPath(), MatchMode.START));
    params.setParentLevel(parent.getLevel());
    params.setLevel(level);
    return getObjectListWith(params, response, currentUser, childrenWithLevel);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping(value = "/{uid}", params = "includeDescendants=true")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getIncludeDescendants(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, ConflictException {
    return getDescendants(uid, params, response, currentUser);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping("/{uid}/descendants")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getDescendants(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, ConflictException {
    Filter descendants = like("path", uid, MatchMode.ANYWHERE);
    return getObjectListWith(params, response, currentUser, List.of(descendants));
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping(value = "/{uid}", params = "includeAncestors=true")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getIncludeAncestors(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    return getAncestors(uid, params, response, currentUser);
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping("/{uid}/ancestors")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getAncestors(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    OrganisationUnit root = getEntity(uid);
    List<String> ancestorsIds = List.of(root.getStoredPath().split("/"));
    List<String> ancestorPaths = new ArrayList<>();
    for (int i = 1; i < ancestorsIds.size(); i++)
      ancestorPaths.add(String.join("/", ancestorsIds.subList(0, i + 1)));
    Filter ancestors = in("path", ancestorPaths);
    params.addOrder("level:asc");
    return getObjectListWith(params, response, currentUser, List.of(ancestors));
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping("/{uid}/parents")
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getParents(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GetOrganisationUnitObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, NotFoundException, ConflictException {
    OrganisationUnit parent = getEntity(uid);
    // when parent is root => no matches by adding an impossible in filter
    if (parent.getLevel() == 1)
      return getObjectListWith(params, response, currentUser, List.of(in("id", List.<String>of())));
    List<String> ancestorsIds = List.of(parent.getStoredPath().split("/"));
    List<String> parentPaths = new ArrayList<>();
    for (int i = 1; i < ancestorsIds.size() - 1; i++)
      parentPaths.add(String.join("/", ancestorsIds.subList(0, i + 1)));
    Filter parents = in("path", parentPaths);
    params.addOrder("level:asc");
    return getObjectListWith(params, response, currentUser, List.of(parents));
  }

  @Override
  protected void addProgrammaticModifiers(GetOrganisationUnitObjectListParams params) {
    if (params.isLevelSorted()) params.addOrder("level:asc");
  }

  @Nonnull
  @Override
  protected List<Filter> getAdditionalFilters(GetOrganisationUnitObjectListParams params)
      throws ConflictException {
    List<Filter> specialFilters = super.getAdditionalFilters(params);
    Integer parentLevel = params.getParentLevel();
    Integer level = params.getLevel();
    if (level != null)
      specialFilters.add(
          parentLevel != null ? eq("level", parentLevel + max(0, level)) : eq("level", level));
    Integer maxLevel = params.getMaxLevel();
    if (maxLevel != null) specialFilters.add(le("level", maxLevel));
    Set<String> parents = null;
    if (params.isWithinUserHierarchy()) parents = getCurrentUserDetails().getUserOrgUnitIds();
    if (params.isWithinDataViewUserHierarchy()) {
      Set<String> dataViewIds = getCurrentUserDetails().getUserDataOrgUnitIds();
      if (parents == null) {
        parents = dataViewIds;
      } else {
        parents = new HashSet<>(parents);
        parents.addAll(dataViewIds);
      }
    }
    if (params.isWithinUserSearchHierarchy()) {
      UserDetails currentUser = getCurrentUserDetails();
      Set<String> searchIds = currentUser.getUserSearchOrgUnitIds();
      if (searchIds.isEmpty()) {
        if (parents == null) parents = currentUser.getUserOrgUnitIds();
      } else if (parents == null) {
        parents = searchIds;
      } else {
        parents = new HashSet<>(parents);
        parents.addAll(searchIds);
      }
    }
    if (parents != null && !parents.isEmpty()) {
      specialFilters.add(token("path", String.join("|", parents), MatchMode.ANYWHERE));
    }
    if (params.isUserOnly())
      specialFilters.add(in("id", getCurrentUserDetails().getUserOrgUnitIds()));
    if (params.isUserDataViewFallback()) {
      Set<String> ouIds = getCurrentUserDetails().getUserDataOrgUnitIds();
      specialFilters.add(ouIds.isEmpty() ? eq("level", 1) : in("id", ouIds));
    } else if (params.isUserDataViewOnly())
      specialFilters.add(in("id", getCurrentUserDetails().getUserDataOrgUnitIds()));

    return specialFilters;
  }

  @Override
  protected void getEntityListPostProcess(
      GetOrganisationUnitObjectListParams params, List<OrganisationUnit> entities)
      throws BadRequestException {
    String memberObject = params.getMemberObject();
    String memberCollection = params.getMemberCollection();
    if (memberObject != null && memberCollection != null) {
      Optional<? extends IdentifiableObject> member = manager.find(memberObject);
      if (member.isPresent()) {
        for (OrganisationUnit unit : entities) {
          Long count =
              organisationUnitService.getOrganisationUnitHierarchyMemberCount(
                  unit, member.get(), memberCollection);

          unit.setMemberCount((count != null ? count.intValue() : 0));
        }
      }
    }
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
