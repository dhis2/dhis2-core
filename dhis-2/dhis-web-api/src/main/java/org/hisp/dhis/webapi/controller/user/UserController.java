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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUidsAsSet;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importReport;
import static org.hisp.dhis.security.Authorities.F_REPLICATE_USER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;
import org.hisp.dhis.commons.jackson.jsonpatch.operations.AddOperation;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.schema.MetadataMergeParams;
import org.hisp.dhis.schema.descriptors.UserSchemaDescriptor;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.Users;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    group = OpenApi.Document.GROUP_MANAGE,
    classifiers = {"team:platform", "purpose:metadata"})
@Slf4j
@Controller
@RequestMapping("/api/users")
public class UserController
    extends AbstractCrudController<User, UserController.GetUserObjectListParams> {

  public static final String INVITE_PATH = "/invite";

  public static final String BULK_INVITE_PATH = "/invites";

  @Autowired protected DbmsManager dbmsManager;

  @Autowired private UserGroupService userGroupService;

  @Autowired private UserControllerUtils userControllerUtils;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private PasswordValidationService passwordValidationService;

  @Autowired private TwoFactorAuthService twoFactorAuthService;

  @Autowired private EntityManager entityManager;

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class GetUserObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        "Limits results to users with the given phone number (shorthand for `filter=phoneNumber:eq:{value}`)")
    String phoneNumber;

    @OpenApi.Description(
        "Limits results to users that are members of a group the current user can manage.")
    boolean canManage;

    @OpenApi.Description(
        "Limits result to users that have no authority the current user doesn't have as well.")
    boolean authSubset;

    @OpenApi.Description(
        "Limits results to users that were logged in on or after the given date(-time) (shorthand for `filter=lastLogin:ge:{date}`).")
    Date lastLogin;

    @OpenApi.Description(
        "Limits results to users that haven't logged in for at least this number of months.")
    Integer inactiveMonths;

    @OpenApi.Description(
        "Limits results to users that haven't logged in since this date(-time) (shorthand for `filter=lastLogin:lt:{date}`).")
    Date inactiveSince;

    @OpenApi.Description(
        "Limits results to users that have self registered (shorthand for `filter=selfRegistered:eq:true`)")
    boolean selfRegistered;

    @OpenApi.Description(
        """
      Limits results to users that with the provided invitation status
      (`ALL` equals `filter=invitation:eq:true`, `EXPIRED` also requires the invitation to have expired by now).""")
    UserInvitationStatus invitationStatus;

    @OpenApi.Description("Shorthand for `orgUnitBoundary=DATA_CAPTURE`")
    boolean userOrgUnits;

    @OpenApi.Description(
        """
      Limits results to users that have a common organisation unit connection with the current user.
      The `orgUnitBoundary` determines if the data capture, data view or search sets are considered.
      When `includeChildren=true` is used the comparison includes the subtree of all units in the compared set.
      """)
    UserOrgUnitType orgUnitBoundary;

    @OpenApi.Description("See `orgUnitBoundary`")
    boolean includeChildren;

    @OpenApi.Description(
        """
      Limits results to users that have data capture connection to the given organisation unit.
      The compared set can be changed using `orgUnitBoundary`.
      """)
    @OpenApi.Property({UID.class, OrganisationUnit.class})
    String ou;

    @OpenApi.Description(
        "Shorthand for `canManage=true` + `authSubset=true` (takes precedence over individual parameters)")
    boolean manage;

    @JsonIgnore
    boolean isUsingAnySpecialFilters() {
      return getQuery() != null
          || phoneNumber != null
          || canManage
          || authSubset
          || lastLogin != null
          || inactiveMonths != null
          || inactiveSince != null
          || selfRegistered
          || invitationStatus != null
          || userOrgUnits
          || includeChildren
          || orgUnitBoundary != null
          || ou != null
          || manage;
    }
  }

  @Override
  protected List<UID> getPreQueryMatches(GetUserObjectListParams params) throws ConflictException {
    if (!params.isUsingAnySpecialFilters()) return null;
    UserQueryParams queryParams = toUserQueryParams(params);

    if (params.isManage()) {
      queryParams.setCanManage(true);
      queryParams.setAuthSubset(true);
    }
    return userService.getUserIds(queryParams, params.getOrders());
  }

  private UserQueryParams toUserQueryParams(GetUserObjectListParams params) {
    UserQueryParams res = new UserQueryParams();
    res.setQuery(StringUtils.trimToNull(params.getQuery()));
    res.setPhoneNumber(StringUtils.trimToNull(params.getPhoneNumber()));
    res.setCanManage(params.isCanManage());
    res.setAuthSubset(params.isAuthSubset());
    res.setLastLogin(params.getLastLogin());
    res.setInactiveMonths(params.getInactiveMonths());
    res.setInactiveSince(params.getInactiveSince());
    res.setSelfRegistered(params.isSelfRegistered());
    res.setInvitationStatus(params.getInvitationStatus());
    res.setUserOrgUnits(params.isUserOrgUnits());
    res.setIncludeOrgUnitChildren(params.isIncludeChildren());
    String ou = params.getOu();
    if (ou != null) {
      res.addOrganisationUnit(organisationUnitService.getOrganisationUnit(ou));
    }
    UserOrgUnitType boundary = params.getOrgUnitBoundary();
    if (boundary != null) res.setOrgUnitBoundary(boundary);
    return res;
  }

  @Override
  @Nonnull
  protected User getEntity(String uid) throws NotFoundException {
    User user = userService.getUser(uid);
    if (user == null) {
      throw new NotFoundException(User.class, uid);
    }
    return user;
  }

  @Override
  @GetMapping("/{uid}/{property}")
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
      @OpenApi.Param(UID.class) @PathVariable("uid") String pvUid,
      @OpenApi.Param(OpenApi.PropertyNames.class) @PathVariable("property") String pvProperty,
      @RequestParam(required = false) List<String> fields,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {

    if ("dataApprovalWorkflows".equals(pvProperty)) {
      return getDataApprovalWorkflows(pvUid, currentUser);
    } else {
      return super.getObjectProperty(pvUid, pvProperty, fields, currentUser, response);
    }
  }

  private ResponseEntity<ObjectNode> getDataApprovalWorkflows(
      String pvUid, UserDetails currentUser) throws NotFoundException, ForbiddenException {
    User user = userService.getUser(pvUid);
    if (user == null) {
      throw new NotFoundException("User not found: " + pvUid);
    }
    if (!aclService.canRead(currentUser, user)) {
      throw new ForbiddenException("You don't have the proper permissions to access this user.");
    }
    return ResponseEntity.ok(userControllerUtils.getUserDataApprovalWorkflows(user));
  }

  // -------------------------------------------------------------------------
  // POST
  // -------------------------------------------------------------------------

  @OpenApi.Params(MetadataImportParams.class)
  @OpenApi.Param(OpenApi.EntityType.class)
  @Override
  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage postJsonObject(HttpServletRequest request)
      throws IOException, ForbiddenException, ConflictException {
    return postObject(renderService.fromJson(request.getInputStream(), getEntityClass()));
  }

  private WebMessage postObject(User user) throws ForbiddenException, ConflictException {

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    validateCreateUser(user, currentUser);

    return postObject(getObjectReport(createUser(user, currentUser)));
  }

  @PostMapping(value = INVITE_PATH, consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage postJsonInvite(HttpServletRequest request)
      throws ForbiddenException, ConflictException, IOException {
    User user = renderService.fromJson(request.getInputStream(), getEntityClass());
    return postInvite(request, user);
  }

  @PostMapping(
      value = INVITE_PATH,
      consumes = {APPLICATION_XML_VALUE, TEXT_XML_VALUE})
  @ResponseBody
  public WebMessage postXmlInvite(HttpServletRequest request)
      throws IOException, ForbiddenException, ConflictException {
    User user = renderService.fromXml(request.getInputStream(), getEntityClass());
    return postInvite(request, user);
  }

  private WebMessage postInvite(HttpServletRequest request, User user)
      throws ForbiddenException, ConflictException {

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    validateInviteUser(user, currentUser);

    ObjectReport objectReport = inviteUser(user, currentUser, request);

    return postObject(objectReport);
  }

  @PostMapping(value = BULK_INVITE_PATH, consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void postJsonInvites(HttpServletRequest request) throws Exception {
    Users users = renderService.fromJson(request.getInputStream(), Users.class);
    postInvites(request, users);
  }

  @PostMapping(
      value = BULK_INVITE_PATH,
      consumes = {APPLICATION_XML_VALUE, TEXT_XML_VALUE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void postXmlInvites(HttpServletRequest request) throws Exception {
    Users users = renderService.fromXml(request.getInputStream(), Users.class);
    postInvites(request, users);
  }

  private void postInvites(HttpServletRequest request, Users users)
      throws ForbiddenException, ConflictException {

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    for (User user : users.getUsers()) {
      validateInviteUser(user, currentUser);
    }

    for (User user : users.getUsers()) {
      inviteUser(user, currentUser, request);
    }
  }

  @PostMapping(value = "/{id}" + INVITE_PATH)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resendInvite(@PathVariable String id, HttpServletRequest request)
      throws NotFoundException, ConflictException, WebMessageException {
    User user = userService.getUser(id);
    if (user == null) {
      throw new NotFoundException(User.class, id);
    }

    if (!user.isInvitation()) {
      throw new ConflictException("User account is not an invitation: " + id);
    }

    ErrorCode errorCode = userService.validateRestore(user);
    if (errorCode != null) {
      throw new ConflictException(errorCode);
    }

    if (!userService.sendRestoreOrInviteMessage(
        user,
        HttpServletRequestPaths.getContextPath(request),
        userService.getRestoreOptions(user.getRestoreToken()))) {
      throw new WebMessageException(error("Failed to send invite message"));
    }
  }

  @PostMapping("/{id}/reset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetToInvite(@PathVariable String id, HttpServletRequest request)
      throws NotFoundException, ForbiddenException, ConflictException {
    User user = userService.getUser(id);
    if (user == null) {
      throw new NotFoundException(User.class, id);
    }
    ErrorCode errorCode = userService.validateRestore(user);
    if (errorCode != null) {
      throw new ConflictException(errorCode);
    }
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    if (!aclService.canUpdate(currentUser, user)) {
      throw new ForbiddenException("You don't have the proper permissions to update this user.");
    }
    if (!userService.canAddOrUpdateUser(getUids(user.getGroups()), currentUser)) {
      throw new ForbiddenException(
          "You must have permissions manage at least one user group for the user.");
    }

    userService.prepareUserForInvite(user);
    userService.sendRestoreOrInviteMessage(
        user,
        HttpServletRequestPaths.getContextPath(request),
        RestoreOptions.RECOVER_PASSWORD_OPTION);
  }

  @SuppressWarnings("unchecked")
  @RequiresAuthority(anyOf = F_REPLICATE_USER)
  @PostMapping("/{uid}/replica")
  @ResponseBody
  public WebMessage replicateUser(@PathVariable String uid, HttpServletRequest request)
      throws IOException,
          ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException {
    User existingUser = userService.getUser(uid);
    if (existingUser == null) {
      return conflict("User not found: " + uid);
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    validateCreateUser(existingUser, currentUser);

    Map<String, String> auth = renderService.fromJson(request.getInputStream(), Map.class);

    String username = StringUtils.trimToNull(auth != null ? auth.get("username") : null);
    String password = StringUtils.trimToNull(auth != null ? auth.get("password") : null);

    if (auth == null || username == null) {
      return conflict("Username must be specified");
    }

    if (!ValidationUtils.usernameIsValid(username, false)) {
      return conflict("Username is not valid");
    }

    if (userService.getUserByUsername(username) != null) {
      return conflict("Username already taken: " + username);
    }

    if (password == null) {
      return conflict("Password must be specified");
    }

    CredentialsInfo credentialsInfo =
        new CredentialsInfo(
            username,
            password,
            existingUser.getEmail() != null ? existingUser.getEmail() : "",
            false);

    PasswordValidationResult result = passwordValidationService.validate(credentialsInfo);

    if (!result.isValid()) {
      return conflict(result.getErrorMessage());
    }

    Session session = entityManager.unwrap(Session.class);

    User userReplica = new User();
    metadataMergeService.merge(
        new MetadataMergeParams<>(existingUser, userReplica).setMergeMode(MergeMode.REPLACE));
    copyAttributeValues(userReplica);
    userReplica.setId(0);
    userReplica.setUuid(UUID.randomUUID());
    userReplica.setUid(CodeGenerator.generateUid());
    userReplica.setCode(null);
    userReplica.setCreated(new Date());
    userReplica.setCreatedBy(session.getReference(User.class, currentUser.getId()));
    userReplica.setLdapId(null);
    userReplica.setOpenId(null);
    userReplica.setUsername(username);
    userReplica.setLastLogin(null);
    userService.encodeAndSetPassword(userReplica, password);

    userService.addUser(userReplica);

    userGroupService.addUserToGroups(userReplica, getUids(existingUser.getGroups()), currentUser);

    // ---------------------------------------------------------------------
    // Replicate user settings
    // ---------------------------------------------------------------------

    UserSettings settings = userSettingsService.getUserSettings(existingUser.getUsername(), false);
    userSettingsService.putAll(settings.toMap(), userReplica.getUsername());

    return created("User replica created")
        .setLocation(UserSchemaDescriptor.API_ENDPOINT + "/" + userReplica.getUid());
  }

  @PostMapping("/{uid}/enabled")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void enableUser(@PathVariable("uid") String uid) throws Exception {
    setDisabled(uid, false);
  }

  @PostMapping("/{uid}/disabled")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void disableUser(@PathVariable("uid") String uid) throws Exception {
    setDisabled(uid, true);
  }

  @PostMapping("/{uid}/expired")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void expireUser(@PathVariable("uid") String uid, @RequestParam("date") Date accountExpiry)
      throws Exception {
    setExpires(uid, accountExpiry);
  }

  @PostMapping("/{uid}/unexpired")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void unexpireUser(@PathVariable("uid") String uid) throws Exception {
    setExpires(uid, null);
  }

  /**
   * Disable 2FA for the user with the given uid.
   *
   * @param uid The uid of the user to disable 2FA for.
   * @param currentUser This is the user currently logged in.
   * @return A WebMessage object.
   */
  @PostMapping("/{uid}/twoFA/disabled")
  @ResponseBody
  public WebMessage disableTwoFa(
      @PathVariable("uid") String uid, @CurrentUser UserDetails currentUser)
      throws ForbiddenException, NotFoundException {
    List<ErrorReport> errors = new ArrayList<>();
    twoFactorAuthService.privileged2FADisable(currentUser, uid, errors::add);

    if (errors.isEmpty()) {
      return WebMessageUtils.ok();
    }

    return WebMessageUtils.errorReports(errors);
  }

  // -------------------------------------------------------------------------
  // PUT
  // -------------------------------------------------------------------------

  @Override
  @PutMapping(
      value = "/{uid}",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage putJsonObject(
      @PathVariable("uid") String pvUid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws IOException, ConflictException, ForbiddenException, NotFoundException {
    User inputUser = renderService.fromJson(request.getInputStream(), getEntityClass());
    return importReport(updateUser(pvUid, inputUser));
  }

  protected ImportReport updateUser(String userUid, User inputUser)
      throws ConflictException, ForbiddenException, NotFoundException {
    User user = getEntity(userUid);

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    if (!aclService.canUpdate(currentUser, user)) {
      throw new ForbiddenException("You don't have the proper permissions to update this user.");
    }

    // force initialization of all authorities of current user in order to
    // prevent cases where user must be reloaded later
    // (in case it gets detached)
    currentUser.getAllAuthorities();

    inputUser.setId(user.getId());
    inputUser.setUid(userUid);
    mergeLastLoginAttribute(user, inputUser);

    boolean isPasswordChangeAttempt = inputUser.getPassword() != null;

    List<String> groupsUids = getUids(inputUser.getGroups());

    if (!userService.canAddOrUpdateUser(groupsUids, currentUser)
        || !currentUser.canModifyUser(user)) {
      throw new ConflictException(
          "You must have permissions to create user, "
              + "or ability to manage at least one user group for the user.");
    }

    MetadataImportParams params =
        importService.getParamsFromMap(contextService.getParameterValuesMap());
    params.setImportReportMode(ImportReportMode.FULL);
    params.setImportStrategy(ImportStrategy.UPDATE);

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObject(inputUser));

    if (importReport.getStatus() == Status.OK && importReport.getStats().updated() == 1) {
      updateUserGroups(userUid, inputUser, currentUser);

      // If it was a pw change attempt (input.pw != null) and update was
      // success we assume password has changed...
      // We chose to expire the special case if password is set to the
      // same. i.e. no before & after equals pw check
      if (isPasswordChangeAttempt) {
        userService.invalidateUserSessions(inputUser.getUsername());
      }
    }

    return importReport;
  }

  @Override
  protected void postUpdateItems(User entity, IdentifiableObjects items) {
    aclService.invalidateCurrentUserGroupInfoCache();
  }

  protected void updateUserGroups(String userUid, User parsed, UserDetails currentUser) {
    User user = userService.getUser(userUid);

    if (currentUser != null && currentUser.getId() == user.getId()) {
      currentUser = CurrentUserUtil.getCurrentUserDetails();
    }

    Collection<String> uids = getUidsAsSet(parsed.getGroups());

    userGroupService.updateUserGroups(user, uids, currentUser);
  }

  // -------------------------------------------------------------------------
  // PATCH
  // -------------------------------------------------------------------------
  @Override
  protected void postPatchEntity(JsonPatch patch, User entityAfter) {
    // Make sure we always expire all the user's active sessions if we
    // have disabled the user.
    if (entityAfter != null && entityAfter.isDisabled()) {
      userService.invalidateUserSessions(entityAfter.getUsername());
    }

    updateUserGroups(patch, entityAfter);
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  @Override
  protected void preDeleteEntity(User entity) throws ConflictException {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    if (!userService.canAddOrUpdateUser(getUids(entity.getGroups()), currentUser)
        || !currentUser.canModifyUser(entity)) {
      throw new ConflictException(
          "You must have permissions to create user, or ability to manage at least one user group for the user.");
    }

    if (userService.isLastSuperUser(entity)) {
      throw new ConflictException("Can not remove the last super user.");
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Validates whether the given user can be created.
   *
   * @param user the user.
   */
  private void validateCreateUser(User user, UserDetails currentUser)
      throws ForbiddenException, ConflictException {

    if (!aclService.canCreate(currentUser, getEntityClass())) {
      throw new ForbiddenException("You don't have the proper permissions to create this object.");
    }

    if (!userService.canAddOrUpdateUser(getUids(user.getGroups()), currentUser)) {
      throw new ConflictException(
          "You must have permissions to create user, or ability to manage at least one user group for the user.");
    }

    List<String> uids = getUids(user.getGroups());

    for (String uid : uids) {
      if (!userGroupService.canAddOrRemoveMember(uid, currentUser)) {
        throw new ConflictException("You don't have permissions to add user to user group: " + uid);
      }
    }
  }

  /**
   * Creates a user.
   *
   * @param user user object parsed from the POST request.
   */
  private ImportReport createUser(User user, UserDetails currentUser) {
    MetadataImportParams importParams =
        new MetadataImportParams()
            .setImportReportMode(ImportReportMode.FULL)
            .setImportStrategy(ImportStrategy.CREATE);

    ImportReport importReport =
        importService.importMetadata(importParams, new MetadataObjects().addObject(user));

    if (importReport.getStatus() == Status.OK && importReport.getStats().created() == 1) {
      userGroupService.addUserToGroups(user, getUids(user.getGroups()), currentUser);
    }

    return importReport;
  }

  /**
   * Validates whether a user can be invited / created.
   *
   * @param user the user.
   */
  private void validateInviteUser(User user, UserDetails currentUser)
      throws ForbiddenException, ConflictException {
    if (user == null) {
      throw new ConflictException("User is not present");
    }

    validateCreateUser(user, currentUser);

    ErrorCode errorCode = userService.validateInvite(user);

    if (errorCode != null) {
      throw new IllegalQueryException(errorCode);
    }
  }

  private ObjectReport inviteUser(User user, UserDetails currentUser, HttpServletRequest request) {
    RestoreOptions restoreOptions =
        user.getUsername() == null || user.getUsername().isEmpty()
            ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE
            : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

    userService.prepareUserForInvite(user);

    ImportReport importReport = createUser(user, currentUser);
    ObjectReport objectReport = getObjectReport(importReport);

    if (importReport.getStatus() == Status.OK
        && importReport.getStats().created() == 1
        && objectReport != null) {
      userService.sendRestoreOrInviteMessage(
          user, HttpServletRequestPaths.getContextPath(request), restoreOptions);

      log.info(String.format("An invite email was successfully sent to: %s", user.getEmail()));
    }

    return objectReport;
  }

  private static ObjectReport getObjectReport(ImportReport importReport) {
    return importReport.getFirstObjectReport();
  }

  /**
   * Make a copy of any existing attribute values, so they can be saved as new attribute values.
   * Don't copy unique values.
   *
   * @param userReplica user for which to copy attribute values.
   */
  private void copyAttributeValues(User userReplica) {
    if (userReplica.getAttributeValues().isEmpty()) return;

    List<String> uniqueAttributeIds =
        attributeService.getAttributesByIds(userReplica.getAttributeValues().keys()).stream()
            .filter(Attribute::isUnique)
            .map(Attribute::getUid)
            .toList();

    userReplica.setAttributeValues(
        userReplica.getAttributeValues().removedAll(uniqueAttributeIds::contains));
  }

  private void mergeLastLoginAttribute(User source, User target) {
    if (target == null || target.getLastLogin() != null) return;

    if (source != null && source.getLastLogin() != null) {
      target.setLastLogin(source.getLastLogin());
    }
  }

  /**
   * Either disable or enable a user account
   *
   * @param uid the unique id of the user to enable or disable
   * @param disable boolean value, true for disable, false for enable
   * @throws WebMessageException thrown if "current" user is not allowed to modify the user
   */
  private void setDisabled(String uid, boolean disable)
      throws WebMessageException, ForbiddenException {
    User userToModify = userService.getUser(uid);
    checkCurrentUserCanModify(userToModify);

    if (userToModify.isDisabled() != disable) {
      userToModify.setDisabled(disable);
      userService.updateUser(userToModify);
    }

    if (disable) {
      userService.invalidateUserSessions(userToModify.getUsername());
    }
  }

  private void checkCurrentUserCanModify(User userToModify)
      throws WebMessageException, ForbiddenException {

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    if (!aclService.canUpdate(currentUserDetails, userToModify)) {
      throw new ForbiddenException("You don't have the proper permissions to update this object.");
    }

    if (!userService.canAddOrUpdateUser(getUids(userToModify.getGroups()))
        || !currentUserDetails.canModifyUser(userToModify)) {
      throw new WebMessageException(
          conflict(
              "You must have permissions to create user, or ability to manage at least one user group for the user."));
    }
  }

  private void setExpires(String uid, Date accountExpiry)
      throws WebMessageException, ForbiddenException {
    User userToModify = userService.getUser(uid);
    checkCurrentUserCanModify(userToModify);

    userToModify.setAccountExpiry(accountExpiry);
    userService.updateUser(userToModify);

    if (!userToModify.isAccountNonExpired()) {
      userService.invalidateUserSessions(userToModify.getUsername());
    }
  }

  /** Support patching user.userGroups relation which User is not the owner */
  private void updateUserGroups(JsonPatch patch, User user) {
    if (ObjectUtils.anyNull(patch, user)) {
      return;
    }

    for (JsonPatchOperation op : patch.getOperations()) {
      JsonPointer userGroups = op.getPath().matchProperty("userGroups");
      if (userGroups == null) {
        continue;
      }

      String opName = op.getOp();
      if (StringUtils.equalsAny(
          opName, JsonPatchOperation.ADD_OPERATION, JsonPatchOperation.REPLACE_OPERATION)) {
        List<String> groupIds = new ArrayList<>();
        ((AddOperation) op)
            .getValue()
            .elements()
            .forEachRemaining(node -> groupIds.add(node.get("id").asText()));

        userGroupService.updateUserGroups(user, groupIds, CurrentUserUtil.getCurrentUserDetails());
      }
    }
  }
}
