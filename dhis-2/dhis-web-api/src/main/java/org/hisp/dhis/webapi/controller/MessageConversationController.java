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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.security.Authorities.F_METADATA_IMPORT;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversationPriority;
import org.hisp.dhis.message.MessageConversationStatus;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.message.UserMessage;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.MessageConversationSchemaDescriptor;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.webdomain.MessageConversation;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/messageConversations")
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class MessageConversationController
    extends AbstractCrudController<org.hisp.dhis.message.MessageConversation> {
  private final MessageService messageService;
  private final OrganisationUnitService organisationUnitService;
  private final UserGroupService userGroupService;
  private final ConfigurationService configurationService;
  private final FileResourceUtils fileResourceUtils;
  private final FileResourceService fileResourceService;
  private final DhisConfigurationProvider dhisConfig;

  @Override
  protected void postProcessResponseEntity(
      org.hisp.dhis.message.MessageConversation entity,
      WebOptions options,
      Map<String, String> parameters) {

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    if (!messageService.hasAccessToManageFeedbackMessages(UserDetails.fromUser(currentUser))) {
      entity.setMessages(
          entity.getMessages().stream().filter(message -> !message.isInternal()).toList());
    }

    boolean markRead = Boolean.parseBoolean(parameters.get("markRead"));

    if (markRead) {
      entity.markRead(currentUser);
      manager.update(entity);
    }
  }

  @Override
  public ResponseEntity<?> getObject(
      @PathVariable String uid,
      Map<String, String> rpParameters,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {
    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      ObjectNode objectNode =
          fieldFilterService
              .createObjectNode()
              .put("message", "No MessageConversation found with UID: " + uid);

      return ResponseEntity.ok(objectNode);
    }

    if (!canReadMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to access this conversation.");
    }

    return super.getObject(uid, rpParameters, currentUser, request, response);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<org.hisp.dhis.message.MessageConversation> getEntityList(
      WebMetadata metadata,
      WebOptions options,
      List<String> filters,
      List<Order> orders,
      List<org.hisp.dhis.message.MessageConversation> objects)
      throws QueryParserException {
    List<org.hisp.dhis.message.MessageConversation> messageConversations;

    if (objects == null && options.getOptions().containsKey("query")) {
      messageConversations =
          Lists.newArrayList(manager.filter(getEntityClass(), options.getOptions().get("query")));
    } else {
      messageConversations = new ArrayList<>(messageService.getMessageConversations());
    }

    Query query =
        queryService.getQueryFromUrl(
            getEntityClass(), filters, orders, new Pagination(), options.getRootJunction());
    query.setDefaultOrder();
    query.setDefaults(Defaults.valueOf(options.get("defaults", DEFAULTS)));
    query.setObjects(messageConversations);

    messageConversations =
        (List<org.hisp.dhis.message.MessageConversation>) queryService.query(query);

    if (options.get("queryString") != null) {
      String queryOperator = "token";
      if (options.get("queryOperator") != null) {
        queryOperator = options.get("queryOperator");
      }

      List<String> queryFilter =
          Arrays.asList(
              "subject:" + queryOperator + ":" + options.get("queryString"),
              "messages.text:" + queryOperator + ":" + options.get("queryString"),
              "messages.sender.displayName:" + queryOperator + ":" + options.get("queryString"));
      Query subQuery =
          queryService.getQueryFromUrl(
              getEntityClass(),
              queryFilter,
              Collections.emptyList(),
              new Pagination(),
              Junction.Type.OR);
      subQuery.setObjects(messageConversations);
      messageConversations =
          (List<org.hisp.dhis.message.MessageConversation>) queryService.query(subQuery);
    }

    int count = messageConversations.size();

    Query paginatedQuery =
        queryService.getQueryFromUrl(
            getEntityClass(),
            Collections.emptyList(),
            Collections.emptyList(),
            getPaginationData(options),
            options.getRootJunction());
    paginatedQuery.setObjects(messageConversations);

    messageConversations =
        (List<org.hisp.dhis.message.MessageConversation>) queryService.query(paginatedQuery);

    if (options.hasPaging()) {
      Pager pager = new Pager(options.getPage(), count, options.getPageSize());
      metadata.setPager(pager);
    }

    return messageConversations;
  }

  // --------------------------------------------------------------------------
  // POST for new MessageConversation
  // --------------------------------------------------------------------------

  @Override
  public WebMessage postJsonObject(HttpServletRequest request)
      throws ConflictException, IOException, NotFoundException {
    MessageConversation messageConversation =
        renderService.fromJson(request.getInputStream(), MessageConversation.class);
    return postObject(request, messageConversation);
  }

  private Set<User> getUsersToMessageConversation(
      MessageConversation messageConversation, Set<User> users)
      throws ConflictException, NotFoundException {
    Set<User> usersToMessageConversation = Sets.newHashSet();
    for (OrganisationUnit ou : messageConversation.getOrganisationUnits()) {
      OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(ou.getUid());

      if (organisationUnit == null) {
        throw new ConflictException("Organisation Unit does not exist: " + ou.getUid());
      }

      usersToMessageConversation.addAll(organisationUnit.getUsers());
    }

    for (User u : users) {
      User user = userService.getUser(u.getUid());

      if (user == null) {
        throw new ConflictException("User does not exist: " + u.getUid());
      }

      usersToMessageConversation.add(user);
    }

    for (UserGroup ug : messageConversation.getUserGroups()) {
      UserGroup userGroup = userGroupService.getUserGroup(ug.getUid());

      if (userGroup == null) {
        throw new NotFoundException(UserGroup.class, ug.getUid());
      }

      usersToMessageConversation.addAll(userGroup.getMembers());
    }

    return usersToMessageConversation;
  }

  private WebMessage postObject(HttpServletRequest request, MessageConversation messageConversation)
      throws ConflictException, NotFoundException {
    Set<User> users = Sets.newHashSet(messageConversation.getUsers());
    messageConversation.getUsers().clear();

    messageConversation
        .getUsers()
        .addAll(getUsersToMessageConversation(messageConversation, users));

    if (messageConversation.getUsers().isEmpty()) {
      throw new ConflictException("No recipients selected.");
    }

    String metaData =
        MessageService.META_USER_AGENT + request.getHeader(ContextUtils.HEADER_USER_AGENT);

    Set<FileResource> attachments = new HashSet<>();

    for (FileResource fr : messageConversation.getAttachments()) {
      FileResource fileResource = fileResourceService.getFileResource(fr.getUid());

      if (fileResource == null) {
        throw new ConflictException("Attachment '" + fr.getUid() + "' not found.");
      }

      if (!fileResource.getDomain().equals(FileResourceDomain.MESSAGE_ATTACHMENT)
          || fileResource.isAssigned()) {
        throw new ConflictException(
            "Attachment '" + fr.getUid() + "' is already used or not a valid attachment.");
      }

      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
      attachments.add(fileResource);
    }

    long id =
        messageService.sendPrivateMessage(
            messageConversation.getUsers(),
            messageConversation.getSubject(),
            messageConversation.getText(),
            metaData,
            attachments);

    org.hisp.dhis.message.MessageConversation conversation =
        messageService.getMessageConversation(id);

    return created("Message conversation created")
        .setLocation(
            MessageConversationSchemaDescriptor.API_ENDPOINT + "/" + conversation.getUid());
  }

  // --------------------------------------------------------------------------
  // POST for reply on existing MessageConversation
  // --------------------------------------------------------------------------

  @PostMapping("/{uid}")
  @ResponseBody
  public WebMessage postMessageConversationReply(
      @PathVariable("uid") String uid,
      @RequestBody String message,
      @RequestParam(value = "internal", defaultValue = "false") boolean internal,
      @RequestParam(value = "attachments", required = false) Set<String> attachments,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws ForbiddenException {
    String metaData =
        MessageService.META_USER_AGENT + request.getHeader(ContextUtils.HEADER_USER_AGENT);

    org.hisp.dhis.message.MessageConversation conversation =
        messageService.getMessageConversation(uid);

    if (conversation == null) {
      return notFound("Message conversation does not exist: " + uid);
    }

    if (internal && !messageService.hasAccessToManageFeedbackMessages(currentUser)) {
      throw new ForbiddenException("Not authorized to send internal messages");
    }

    if (!hasAccessToMessageConversation(currentUser, conversation)) {
      throw new ForbiddenException("Not authorized to send messages to this conversation.");
    }

    Set<FileResource> fileResources = new HashSet<>();

    if (attachments == null) {
      attachments = new HashSet<>();
    }

    for (String fileResourceUid : attachments) {
      FileResource fileResource = fileResourceService.getFileResource(fileResourceUid);

      if (fileResource == null) {
        return conflict("Attachment '" + fileResourceUid + "' not found.");
      }

      if (!fileResource.getDomain().equals(FileResourceDomain.MESSAGE_ATTACHMENT)
          || fileResource.isAssigned()) {
        return conflict(
            "Attachment '" + fileResourceUid + "' is already used or not a valid attachment.");
      }

      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);

      fileResources.add(fileResource);
    }

    messageService.sendReply(conversation, message, metaData, internal, fileResources);

    return created("Message conversation created")
        .setLocation(
            MessageConversationSchemaDescriptor.API_ENDPOINT + "/" + conversation.getUid());
  }

  @PostMapping("/{uid}/recipients")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addRecipientsToMessageConversation(
      @PathVariable("uid") String uid,
      @RequestBody MessageConversation messageConversation,
      @CurrentUser UserDetails currentUser)
      throws WebMessageException, ConflictException, NotFoundException, ForbiddenException {

    org.hisp.dhis.message.MessageConversation conversation =
        messageService.getMessageConversation(uid);

    if (conversation == null) {
      throw new WebMessageException(notFound("Message conversation does not exist: " + uid));
    }

    if (!hasAccessToMessageConversation(currentUser, conversation)) {
      throw new ForbiddenException("Not authorized to change recipients in this conversation.");
    }

    Set<User> additionalUsers =
        getUsersToMessageConversation(messageConversation, messageConversation.getUsers());

    additionalUsers.forEach(
        user -> {
          if (!conversation.getUsers().contains(user)) {
            conversation.addUserMessage(new UserMessage(user, false));
          }
        });

    messageService.updateMessageConversation(conversation);
  }

  // --------------------------------------------------------------------------
  // POST for feedback
  // --------------------------------------------------------------------------

  @PostMapping("/feedback")
  @ResponseBody
  public WebMessage postMessageConversationFeedback(
      @RequestParam("subject") String subject,
      @RequestBody String body,
      HttpServletRequest request) {
    String metaData =
        MessageService.META_USER_AGENT + request.getHeader(ContextUtils.HEADER_USER_AGENT);

    long id = messageService.sendTicketMessage(subject, body, metaData);
    org.hisp.dhis.message.MessageConversation conversation =
        messageService.getMessageConversation(id);

    return created("Feedback created")
        .setLocation(
            MessageConversationSchemaDescriptor.API_ENDPOINT + "/" + conversation.getUid());
  }

  // --------------------------------------------------------------------------
  // Assign priority
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "/{uid}/priority",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode setMessagePriority(
      @PathVariable String uid,
      @RequestParam MessageConversationPriority messageConversationPriority,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversation found for the given ID."));
      return responseNode;
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to change priority in this conversation.");
    }

    CollectionNode marked =
        responseNode.addChild(new CollectionNode(messageConversationPriority.name()));
    marked.setWrapping(false);

    messageConversation.setPriority(messageConversationPriority);
    messageService.updateMessageConversation(messageConversation);
    marked.addChild(new SimpleNode("uid", messageConversation.getUid()));
    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Assign status
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "/{uid}/status",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode setMessageStatus(
      @PathVariable String uid,
      @RequestParam MessageConversationStatus messageConversationStatus,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversation found for the given ID."));
      return responseNode;
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to change status in this conversation.");
    }

    CollectionNode marked =
        responseNode.addChild(new CollectionNode(messageConversationStatus.name()));
    marked.setWrapping(false);

    messageConversation.setStatus(messageConversationStatus);
    messageService.updateMessageConversation(messageConversation);
    marked.addChild(new SimpleNode("uid", messageConversation.getUid()));
    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Assign user
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "/{uid}/assign",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode setUserAssigned(
      @PathVariable String uid,
      @RequestParam(required = false) String userId,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversation found for the given ID."));
      return responseNode;
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to assign a user to this conversation.");
    }

    User userToAssign;

    if ((userToAssign = userService.getUser(userId)) == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(new SimpleNode("message", "Could not find user to assign"));
      return responseNode;
    }

    if (messageConversation.getMessageType() == MessageType.TICKET
        && !configurationService.isUserInFeedbackRecipientUserGroup(
            UserDetails.fromUser(userToAssign))) {
      response.setStatus(HttpServletResponse.SC_CONFLICT);
      responseNode.addChild(
          new SimpleNode(
              "message", "User provided is not a member of the system's feedback recipient group"));
      return responseNode;
    }

    messageConversation.setAssignee(userToAssign);
    messageService.updateMessageConversation(messageConversation);
    responseNode.addChild(
        new SimpleNode("message", "User " + userToAssign.getName() + " was assigned successfully"));
    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Remove assigned user
  // --------------------------------------------------------------------------

  @DeleteMapping(
      value = "/{uid}/assign",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode removeUserAssigned(
      @PathVariable String uid, @CurrentUser UserDetails currentUser, HttpServletResponse response)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversation found for the given ID."));
      return responseNode;
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException(
          "Not authorized to remove the assigned user to this conversation.");
    }

    messageConversation.setAssignee(null);
    messageService.updateMessageConversation(messageConversation);
    responseNode.addChild(new SimpleNode("message", "Message is no longer assigned to user"));
    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Mark conversations read
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "/{uid}/read",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode markMessageConversationRead(
      @PathVariable String uid,
      @RequestParam(required = false) String userUid,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    return modifyMessageConversationRead(
        userUid, Lists.newArrayList(uid), response, true, currentUser);
  }

  @PostMapping(
      value = "/read",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode markMessageConversationsRead(
      @RequestParam(value = "user", required = false) String userUid,
      @RequestBody List<String> uids,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    return modifyMessageConversationRead(userUid, uids, response, true, currentUser);
  }

  // --------------------------------------------------------------------------
  // Mark conversations unread
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "/{uid}/unread",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode markMessageConversationUnread(
      @PathVariable String uid,
      @RequestParam(required = false) String userUid,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    return modifyMessageConversationRead(
        userUid, Lists.newArrayList(uid), response, false, currentUser);
  }

  @PostMapping(
      value = "/unread",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode markMessageConversationsUnread(
      @RequestParam(value = "user", required = false) String userUid,
      @RequestBody List<String> uids,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    return modifyMessageConversationRead(userUid, uids, response, false, currentUser);
  }

  // --------------------------------------------------------------------------
  // Mark conversations for follow up
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "followup",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode markMessageConversationFollowup(
      @RequestParam(value = "user", required = false) String userUid,
      @RequestBody List<String> uids,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    User user =
        userUid != null
            ? userService.getUser(userUid)
            : userService.getUserByUsername(currentUser.getUsername());

    if (user == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(new SimpleNode("message", "No user with uid: " + userUid));
      return responseNode;
    }

    Collection<org.hisp.dhis.message.MessageConversation> messageConversations =
        messageService.getMessageConversations(user, uids);

    if (messageConversations.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversations found for the given UIDs"));
      return responseNode;
    }

    for (org.hisp.dhis.message.MessageConversation messageConversation : messageConversations) {
      if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
        throw new ForbiddenException("Not authorized to change followups in this conversation.");
      }
    }

    CollectionNode marked = responseNode.addChild(new CollectionNode("markedFollowup"));
    marked.setWrapping(false);

    for (org.hisp.dhis.message.MessageConversation conversation : messageConversations) {
      if (!conversation.isFollowUp()) {
        conversation.toggleFollowUp(user);
        messageService.updateMessageConversation(conversation);
      }
      marked.addChild(new SimpleNode("uid", conversation.getUid()));
    }

    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Clear follow up
  // --------------------------------------------------------------------------

  @PostMapping(
      value = "unfollowup",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode unmarkMessageConversationFollowup(
      @RequestParam(value = "user", required = false) String userUid,
      @RequestBody List<String> uids,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    User user =
        userUid != null
            ? userService.getUser(userUid)
            : userService.getUserByUsername(currentUser.getUsername());

    if (user == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(new SimpleNode("message", "No user with uid: " + userUid));
      return responseNode;
    }

    Collection<org.hisp.dhis.message.MessageConversation> messageConversations =
        messageService.getMessageConversations(user, uids);

    if (messageConversations.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversations found for the given UIDs"));
      return responseNode;
    }

    for (org.hisp.dhis.message.MessageConversation messageConversation : messageConversations) {
      if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
        throw new ForbiddenException("Not authorized to change unfollowups in this conversation.");
      }
    }

    CollectionNode marked = responseNode.addChild(new CollectionNode("unmarkedFollowup"));
    marked.setWrapping(false);

    for (org.hisp.dhis.message.MessageConversation conversation : messageConversations) {
      if (conversation.isFollowUp()) {
        conversation.toggleFollowUp(user);
        messageService.updateMessageConversation(conversation);
      }
      marked.addChild(new SimpleNode("uid", conversation.getUid()));
    }

    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Delete a MessageConversation (requires override auth)
  // --------------------------------------------------------------------------

  /**
   * Deletes a MessageConversation. Note that this is a HARD delete and therefore requires override
   * authority for the current user.
   *
   * @param uid the uid of the MessageConversation to delete.
   */
  @Override
  @RequiresAuthority(anyOf = F_METADATA_IMPORT)
  public WebMessage deleteObject(
      @PathVariable String uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          HttpRequestMethodNotSupportedException {

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(uid);

    if (messageConversation == null) {
      return notFound("Message conversation does not exist: " + uid);
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to delete this conversation.");
    }

    return super.deleteObject(uid, currentUser, request, response);
  }

  // --------------------------------------------------------------------------
  // Remove a user from a MessageConversation
  // In practice a DELETE on MessageConversation <-> User relationship
  // --------------------------------------------------------------------------

  @DeleteMapping(
      value = "/{mc-uid}/{user-uid}",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode removeUserFromMessageConversation(
      @PathVariable(value = "mc-uid") String mcUid,
      @PathVariable(value = "user-uid") String userUid,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("reply");

    User user = userService.getUser(userUid);

    if (user == null) {
      responseNode.addChild(new SimpleNode("message", "No user with uid: " + userUid));
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return responseNode;
    }

    org.hisp.dhis.message.MessageConversation messageConversation =
        messageService.getMessageConversation(mcUid);

    if (messageConversation == null) {
      responseNode.addChild(new SimpleNode("message", "No messageConversation with uid: " + mcUid));
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return responseNode;
    }

    if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
      throw new ForbiddenException("Not authorized to remove assigned users to this conversation.");
    }

    CollectionNode removed = responseNode.addChild(new CollectionNode("removed"));

    if (messageConversation.remove(user)) {
      messageService.updateMessageConversation(messageConversation);
      removed.addChild(new SimpleNode("uid", messageConversation.getUid()));
    }

    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  // --------------------------------------------------------------------------
  // Remove a user from one or more MessageConversations (batch operation)
  // --------------------------------------------------------------------------

  @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody RootNode removeUserFromMessageConversations(
      @RequestParam("mc") List<String> mcUids,
      @RequestParam(value = "user", required = false) String userUid,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    User user =
        userUid != null
            ? userService.getUser(userUid)
            : userService.getUserByUsername(currentUser.getUsername());

    if (user == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(new SimpleNode("message", "User does not exist: " + userUid));
      return responseNode;
    }

    Collection<org.hisp.dhis.message.MessageConversation> messageConversations =
        messageService.getMessageConversations(user, mcUids);

    if (messageConversations.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversations found for the given UIDs."));
      return responseNode;
    }

    for (org.hisp.dhis.message.MessageConversation messageConversation : messageConversations) {
      if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
        throw new ForbiddenException(
            "Not authorized to remove assigned users to this conversation.");
      }
    }

    CollectionNode removed = responseNode.addChild(new CollectionNode("removed"));

    for (org.hisp.dhis.message.MessageConversation mc : messageConversations) {
      if (mc.remove(user)) {
        messageService.updateMessageConversation(mc);
        removed.addChild(new SimpleNode("uid", mc.getUid()));
      }
    }

    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  @GetMapping("/{mcUid}/{msgUid}/attachments/{fileUid}")
  public void getAttachment(
      @PathVariable(value = "mcUid") String mcUid,
      @PathVariable(value = "msgUid") String msgUid,
      @PathVariable(value = "fileUid") String fileUid,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws WebMessageException, ForbiddenException {

    Message message = getMessage(mcUid, msgUid, currentUser);

    FileResource fr = fileResourceService.getFileResource(fileUid);

    boolean attachmentExists =
        message.getAttachments().stream().filter(att -> att.getUid().equals(fileUid)).count() == 1;

    if (fr == null || !attachmentExists) {
      throw new WebMessageException(
          notFound("No messageattachment found with id '" + fileUid + "'"));
    }

    if (!fr.getDomain().equals(FileResourceDomain.MESSAGE_ATTACHMENT)) {
      throw new WebMessageException(conflict("Invalid messageattachment."));
    }

    fileResourceUtils.configureFileResourceResponse(response, fr, dhisConfig);
  }

  // --------------------------------------------------------------------------
  // Supportive methods
  // --------------------------------------------------------------------------

  /**
   * Determines whether the given user has permission to read the MessageConversation.
   *
   * @param userDetails the user to check permission for.
   * @param messageConversation the MessageConversation to access.
   * @return true if the user can read the MessageConversation, false otherwise.
   */
  private boolean canReadMessageConversation(
      UserDetails userDetails, org.hisp.dhis.message.MessageConversation messageConversation) {
    Set<User> users = messageConversation.getUsers();
    List<String> list = users.stream().map(User::getUsername).toList();
    return list.contains(userDetails.getUsername()) || userDetails.isSuper();
  }

  private boolean hasAccessToMessageConversation(
      UserDetails user, @Nonnull org.hisp.dhis.message.MessageConversation conversation) {

    if (hasConversationAccess(user, conversation)) {
      return true;
    }

    return conversation.getMessageType() == MessageType.TICKET
        && messageService.hasAccessToManageFeedbackMessages(user);
  }

  private static boolean hasConversationAccess(
      UserDetails user, org.hisp.dhis.message.MessageConversation conversation) {
    return conversation.getUsers().stream()
            .anyMatch(u -> u.getUsername().equals(user.getUsername()))
        || user.isSuper();
  }

  /**
   * Internal handler for setting the read property of MessageConversation.
   *
   * @param readValue true when setting as read, false when setting unread.
   */
  private RootNode modifyMessageConversationRead(
      String userUid,
      List<String> uids,
      HttpServletResponse response,
      boolean readValue,
      UserDetails currentUser)
      throws ForbiddenException {
    RootNode responseNode = new RootNode("response");

    User user =
        userUid != null
            ? userService.getUser(userUid)
            : userService.getUserByUsername(currentUser.getUsername());

    if (user == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(new SimpleNode("message", "No user with uid: " + userUid));
      return responseNode;
    }

    Collection<org.hisp.dhis.message.MessageConversation> messageConversations =
        messageService.getMessageConversations(user, uids);

    if (messageConversations.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      responseNode.addChild(
          new SimpleNode("message", "No MessageConversations found for the given IDs."));
      return responseNode;
    }

    for (org.hisp.dhis.message.MessageConversation messageConversation : messageConversations) {
      if (!hasAccessToMessageConversation(currentUser, messageConversation)) {
        throw new ForbiddenException(
            "Not authorized to change read property of this conversation.");
      }
    }

    CollectionNode marked =
        responseNode.addChild(new CollectionNode(readValue ? "markedRead" : "markedUnread"));
    marked.setWrapping(false);

    for (org.hisp.dhis.message.MessageConversation conversation : messageConversations) {

      boolean success = (readValue ? conversation.markRead(user) : conversation.markUnread(user));
      if (success) {
        messageService.updateMessageConversation(conversation);
        marked.addChild(new SimpleNode("uid", conversation.getUid()));
      }
    }

    response.setStatus(HttpServletResponse.SC_OK);

    return responseNode;
  }

  /**
   * /* Returns the specified message after making sure the user has access to it.
   *
   * @param mcUid the message conversation UID.
   * @param msgUid the message UID.
   * @param userDetails the user.
   * @return a {@link Message}.
   * @throws WebMessageException exception
   */
  private Message getMessage(String mcUid, String msgUid, UserDetails userDetails)
      throws WebMessageException, ForbiddenException {
    org.hisp.dhis.message.MessageConversation conversation =
        messageService.getMessageConversation(mcUid);

    if (conversation == null) {
      throw new WebMessageException(
          notFound(String.format("No message conversation with uid '%s'", mcUid)));
    }

    if (!canReadMessageConversation(userDetails, conversation)) {
      throw new ForbiddenException("Not authorized to access this conversation.");
    }

    List<Message> messages =
        conversation.getMessages().stream().filter(msg -> msg.getUid().equals(msgUid)).toList();

    if (messages.isEmpty()) {
      throw new WebMessageException(
          notFound(
              String.format("No message with uid '%s' in messageConversation '%s", msgUid, mcUid)));
    }

    Message message = messages.get(0);

    if (message.isInternal()
        && !configurationService.isUserInFeedbackRecipientUserGroup(userDetails)) {
      throw new WebMessageException(conflict("Not authorized to access this message"));
    }

    return message;
  }
}
