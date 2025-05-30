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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.CacheControl.noCache;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.cache.Region;
import org.hisp.dhis.common.event.CacheInvalidationEvent;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.webapi.webdomain.sharing.Sharing;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserAccess;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserGroupAccess;
import org.hisp.dhis.webapi.webdomain.sharing.comparator.SharingUserGroupAccessNameComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = Sharing.class,
    classifiers = {"team:platform", "purpose:metadata"})
@Controller
@RequestMapping("/api/sharing")
@Slf4j
public class SharingController {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private UserGroupService userGroupService;

  @Autowired private UserService userService;

  @Autowired private AclService aclService;

  @Autowired private RenderService renderService;

  @Autowired private SchemaService schemaService;

  @Autowired private EntityManager entityManager;

  @Autowired private ApplicationEventPublisher eventPublisher;

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Sharing> getSharing(@RequestParam String type, @RequestParam String id)
      throws WebMessageException, ForbiddenException {
    if (!aclService.isShareable(type)) {
      throw new WebMessageException(conflict("Type " + type + " is not supported."));
    }

    Class<? extends IdentifiableObject> klass = aclService.classForType(type);
    IdentifiableObject object = manager.getNoAcl(klass, id);

    if (object == null) {
      throw new WebMessageException(
          notFound("Object of type " + type + " with ID " + id + " was not found."));
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    if (!aclService.canRead(currentUserDetails, object)) {
      throw new ForbiddenException("You do not have manage access to this object.");
    }

    Sharing sharing = new Sharing();
    sharing.getMeta().setAllowPublicAccess(aclService.canMakePublic(currentUserDetails, object));
    sharing
        .getMeta()
        .setAllowExternalAccess(aclService.canMakeExternal(currentUserDetails, object));

    sharing.getObject().setId(object.getUid());
    sharing.getObject().setName(object.getDisplayName());
    sharing.getObject().setDisplayName(object.getDisplayName());
    sharing.getObject().setExternalAccess(object.getSharing().isExternal());

    if (object.getSharing().getPublicAccess() == null) {
      String access;

      if (aclService.canMakeClassPublic(currentUserDetails, klass)) {
        access =
            AccessStringHelper.newInstance()
                .enable(AccessStringHelper.Permission.READ)
                .enable(AccessStringHelper.Permission.WRITE)
                .build();
      } else {
        access = AccessStringHelper.newInstance().build();
      }

      sharing.getObject().setPublicAccess(access);
    } else {
      sharing.getObject().setPublicAccess(object.getSharing().getPublicAccess());
    }

    if (object.getCreatedBy() != null) {
      sharing.getObject().getUser().setId(object.getCreatedBy().getUid());
      sharing.getObject().getUser().setName(object.getCreatedBy().getDisplayName());
    }

    for (UserGroupAccess userGroupAccess : object.getSharing().getUserGroups().values()) {
      String userGroupDisplayName = userGroupService.getDisplayName(userGroupAccess.getId());

      if (userGroupDisplayName == null) {
        continue;
      }

      SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();
      sharingUserGroupAccess.setId(userGroupAccess.getId());
      sharingUserGroupAccess.setName(userGroupDisplayName);
      sharingUserGroupAccess.setDisplayName(userGroupDisplayName);
      sharingUserGroupAccess.setAccess(userGroupAccess.getAccess());

      sharing.getObject().getUserGroupAccesses().add(sharingUserGroupAccess);
    }

    for (UserAccess userAccess : object.getSharing().getUsers().values()) {
      String userDisplayName = userService.getDisplayName(userAccess.getId());

      if (userDisplayName == null) continue;

      SharingUserAccess sharingUserAccess = new SharingUserAccess();
      sharingUserAccess.setId(userAccess.getId());
      sharingUserAccess.setName(userDisplayName);
      sharingUserAccess.setDisplayName(userDisplayName);
      sharingUserAccess.setAccess(userAccess.getAccess());

      sharing.getObject().getUserAccesses().add(sharingUserAccess);
    }

    sharing.getObject().getUserGroupAccesses().sort(SharingUserGroupAccessNameComparator.INSTANCE);

    return ResponseEntity.ok().cacheControl(noCache()).body(sharing);
  }

  @PutMapping(consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage putSharing(
      @RequestParam String type, @RequestParam String id, HttpServletRequest request)
      throws Exception {
    return postSharing(type, id, request);
  }

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage postSharing(
      @RequestParam String type, @RequestParam String id, HttpServletRequest request)
      throws Exception {
    Class<? extends IdentifiableObject> sharingClass = aclService.classForType(type);

    if (sharingClass == null || !aclService.isClassShareable(sharingClass)) {
      return conflict("Type " + type + " is not supported.");
    }

    IdentifiableObject object = manager.getNoAcl(sharingClass, id);

    if (object == null) {
      return notFound("Object of type " + type + " with ID " + id + " was not found.");
    }

    if ((object instanceof SystemDefaultMetadataObject)
        && ((SystemDefaultMetadataObject) object).isDefault()) {
      return conflict(
          "Sharing settings of system default metadata object of type "
              + type
              + " cannot be modified.");
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    if (!aclService.canManage(currentUserDetails, object)) {
      throw new ForbiddenException("You do not have manage access to this object.");
    }

    Sharing sharing = renderService.fromJson(request.getInputStream(), Sharing.class);

    if (!AccessStringHelper.isValid(sharing.getObject().getPublicAccess())) {
      return conflict("Invalid public access string: " + sharing.getObject().getPublicAccess());
    }

    // ---------------------------------------------------------------------
    // Ignore externalAccess if user is not allowed to make objects external
    // ---------------------------------------------------------------------

    if (aclService.canMakeExternal(currentUserDetails, object)) {
      object.getSharing().setExternal(sharing.getObject().hasExternalAccess());
    }

    // ---------------------------------------------------------------------
    // Ignore publicAccess if user is not allowed to make objects public
    // ---------------------------------------------------------------------

    Schema schema = schemaService.getDynamicSchema(sharingClass);

    if (aclService.canMakePublic(currentUserDetails, object)) {
      object.getSharing().setPublicAccess(sharing.getObject().getPublicAccess());
    }

    if (!schema.isDataShareable()) {
      if (AccessStringHelper.hasDataSharing(object.getSharing().getPublicAccess())) {
        object
            .getSharing()
            .setPublicAccess(
                AccessStringHelper.disableDataSharing(object.getSharing().getPublicAccess()));
      }
    }

    if (object.getCreatedBy() == null && currentUserDetails != null) {
      User user = entityManager.getReference(User.class, currentUserDetails.getId());
      object.setCreatedBy(user);
    }

    object.getSharing().getUserGroups().clear();

    for (SharingUserGroupAccess sharingUserGroupAccess :
        sharing.getObject().getUserGroupAccesses()) {
      UserGroupAccess userGroupAccess = new UserGroupAccess();

      if (!AccessStringHelper.isValid(sharingUserGroupAccess.getAccess())) {
        return conflict("Invalid user group access string: " + sharingUserGroupAccess.getAccess());
      }

      if (!schema.isDataShareable()) {
        if (AccessStringHelper.hasDataSharing(sharingUserGroupAccess.getAccess())) {
          sharingUserGroupAccess.setAccess(
              AccessStringHelper.disableDataSharing(sharingUserGroupAccess.getAccess()));
        }
      }

      userGroupAccess.setAccess(sharingUserGroupAccess.getAccess());

      UserGroup userGroup = manager.get(UserGroup.class, sharingUserGroupAccess.getId());

      if (userGroup != null) {
        userGroupAccess.setUserGroup(userGroup);
        object.getSharing().addUserGroupAccess(userGroupAccess);
      }
    }

    object.getSharing().getUsers().clear();

    for (SharingUserAccess sharingUserAccess : sharing.getObject().getUserAccesses()) {
      UserAccess userAccess = new UserAccess();

      if (!AccessStringHelper.isValid(sharingUserAccess.getAccess())) {
        return conflict("Invalid user access string: " + sharingUserAccess.getAccess());
      }

      if (!schema.isDataShareable()) {
        if (AccessStringHelper.hasDataSharing(sharingUserAccess.getAccess())) {
          sharingUserAccess.setAccess(
              AccessStringHelper.disableDataSharing(sharingUserAccess.getAccess()));
        }
      }

      userAccess.setAccess(sharingUserAccess.getAccess());

      User sharingUser = manager.get(User.class, sharingUserAccess.getId());

      if (sharingUser != null) {
        userAccess.setUser(sharingUser);
        object.getSharing().addUserAccess(userAccess);
      }
    }

    manager.updateNoAcl(object);

    if (object instanceof Program) {
      syncSharingForEventProgram((Program) object);
    } else if (object instanceof Visualization) {
      syncSharingForExpressionDimensionItems(
          (Visualization) object,
          sharing.getObject().getUserAccesses(),
          sharing.getObject().getUserGroupAccesses());
    } else if (object instanceof CategoryOption) {
      eventPublisher.publishEvent(new CacheInvalidationEvent(this, Region.canDataWriteCocCache));
    }

    return ok("Access control set");
  }

  public record SharingSearchResult(
      @JsonProperty List<SharingUserAccess> users,
      @JsonProperty List<SharingUserGroupAccess> userGroups) {}

  @GetMapping(value = "/search", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<SharingSearchResult> searchUserGroups(
      @RequestParam String key, @RequestParam(required = false) Integer pageSize)
      throws WebMessageException {
    if (key == null) {
      throw new WebMessageException(conflict("Search key not specified"));
    }

    int max = pageSize != null ? pageSize : Pager.DEFAULT_PAGE_SIZE;

    List<SharingUserGroupAccess> userGroupAccesses = getSharingUserGroups(key, max);
    List<SharingUserAccess> userAccesses = getSharingUser(key, max);

    SharingSearchResult output = new SharingSearchResult(userAccesses, userGroupAccesses);

    return ResponseEntity.ok().cacheControl(noCache()).body(output);
  }

  private List<SharingUserAccess> getSharingUser(String key, int max) {
    List<SharingUserAccess> sharingUsers = new ArrayList<>();
    List<User> users = userService.getAllUsersBetweenByName(key, 0, max);

    for (User user : users) {
      SharingUserAccess sharingUserAccess = new SharingUserAccess();
      sharingUserAccess.setId(user.getUid());
      sharingUserAccess.setName(user.getDisplayName());
      sharingUserAccess.setDisplayName(user.getDisplayName());
      sharingUserAccess.setUsername(user.getUsername());

      sharingUsers.add(sharingUserAccess);
    }

    return sharingUsers;
  }

  private List<SharingUserGroupAccess> getSharingUserGroups(@RequestParam String key, int max) {
    List<SharingUserGroupAccess> sharingUserGroupAccesses = new ArrayList<>();
    List<UserGroup> userGroups = userGroupService.getUserGroupsBetweenByName(key, 0, max);

    for (UserGroup userGroup : userGroups) {
      SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();

      sharingUserGroupAccess.setId(userGroup.getUid());
      sharingUserGroupAccess.setName(userGroup.getDisplayName());
      sharingUserGroupAccess.setDisplayName(userGroup.getDisplayName());

      sharingUserGroupAccesses.add(sharingUserGroupAccess);
    }

    return sharingUserGroupAccesses;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void syncSharingForEventProgram(Program program) {
    if (ProgramType.WITH_REGISTRATION == program.getProgramType()
        || program.getProgramStages().isEmpty()) {
      return;
    }

    ProgramStage programStage = program.getProgramStages().iterator().next();
    AccessStringHelper.copySharing(program, programStage);

    programStage.setCreatedBy(program.getCreatedBy());
    manager.update(programStage);
  }

  private void syncSharingForExpressionDimensionItems(
      Visualization visualization,
      List<SharingUserAccess> sharingUserAccesses,
      List<SharingUserGroupAccess> sharingUserGroupAccesses) {
    List<IdentifiableObject> expressionDimensionItems =
        visualization.getDataDimensionItems().stream()
            .map(DataDimensionItem::getExpressionDimensionItem)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    expressionDimensionItems.forEach(
        edi -> {
          org.hisp.dhis.user.sharing.Sharing sharing = edi.getSharing();

          Set<UserAccess> userAccess =
              sharingUserAccesses.stream()
                  .map(sua -> new UserAccess(sua.getAccess(), sua.getId()))
                  .collect(Collectors.toUnmodifiableSet());
          sharing.setUserAccesses(userAccess);

          Set<UserGroupAccess> userGroupAccess =
              sharingUserGroupAccesses.stream()
                  .map(sua -> new UserGroupAccess(sua.getAccess(), sua.getId()))
                  .collect(Collectors.toUnmodifiableSet());

          sharing.setUserGroupAccess(userGroupAccess);
        });

    manager.update(expressionDimensionItems);
  }
}
