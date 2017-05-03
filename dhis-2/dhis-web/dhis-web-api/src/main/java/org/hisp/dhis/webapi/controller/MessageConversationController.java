package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.message.MessageConversationPriority;
import org.hisp.dhis.message.MessageConversationStatus;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.MessageConversationSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.MessageConversation;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = MessageConversationSchemaDescriptor.API_ENDPOINT )
public class MessageConversationController
    extends AbstractCrudController<org.hisp.dhis.message.MessageConversation>
{
    @Autowired
    private MessageService messageService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected void postProcessEntity( org.hisp.dhis.message.MessageConversation entity, WebOptions options, Map<String, String> parameters )
        throws Exception
    {
        Boolean markRead = Boolean.parseBoolean( parameters.get( "markRead" ) );

        if ( markRead )
        {
            entity.markRead( currentUserService.getCurrentUser() );
            manager.update( entity );
        }
    }

    @Override
    public RootNode getObject( @PathVariable String uid, Map<String, String> rpParameters, HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        org.hisp.dhis.message.MessageConversation messageConversation = messageService.getMessageConversation( uid );

        if ( messageConversation == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            RootNode responseNode = new RootNode( "reply" );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversation found with UID: " + uid ) );
            return responseNode;
        }

        if ( !canReadMessageConversation( currentUserService.getCurrentUser(), messageConversation ) )
        {
            throw new AccessDeniedException( "Not authorized to access this conversation." );
        }

        return super.getObject( uid, rpParameters, request, response );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<org.hisp.dhis.message.MessageConversation> getEntityList( WebMetadata metadata, WebOptions options,
        List<String> filters, List<Order> orders ) throws QueryParserException
    {
        List<org.hisp.dhis.message.MessageConversation> messageConversations;

        if ( options.getOptions().containsKey( "query" ) )
        {
            messageConversations = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else if ( options.hasPaging() )
        {
            int count = messageService.getMessageConversationCount();

            Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
            metadata.setPager( pager );

            messageConversations = new ArrayList<>( messageService.getMessageConversations( pager.getOffset(), pager.getPageSize() ) );
        }
        else
        {
            messageConversations = new ArrayList<>( messageService.getMessageConversations() );
        }

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, options.getRootJunction() );
        query.setDefaultOrder();
        query.setObjects( messageConversations );

        return (List<org.hisp.dhis.message.MessageConversation>) queryService.query( query );
    }

    //--------------------------------------------------------------------------
    // POST for new MessageConversation
    //--------------------------------------------------------------------------

    @Override
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        MessageConversation messageConversation = renderService
            .fromXml( request.getInputStream(), MessageConversation.class );
        postObject( response, request, messageConversation );
    }

    @Override
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        MessageConversation messageConversation = renderService
            .fromJson( request.getInputStream(), MessageConversation.class );
        postObject( response, request, messageConversation );
    }

    private void postObject( HttpServletResponse response, HttpServletRequest request,
        MessageConversation messageConversation )
        throws WebMessageException
    {
        List<User> users = new ArrayList<>( messageConversation.getUsers() );
        messageConversation.getUsers().clear();

        for ( OrganisationUnit ou : messageConversation.getOrganisationUnits() )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou.getUid() );

            if ( organisationUnit == null )
            {
                throw new WebMessageException(
                    WebMessageUtils.conflict( "Organisation Unit does not exist: " + ou.getUid() ) );
            }

            messageConversation.getUsers().addAll( organisationUnit.getUsers() );
        }

        for ( User u : users )
        {
            User user = userService.getUser( u.getUid() );

            if ( user == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "User does not exist: " + u.getUid() ) );
            }

            messageConversation.getUsers().add( user );
        }

        for ( UserGroup ug : messageConversation.getUserGroups() )
        {
            UserGroup userGroup = userGroupService.getUserGroup( ug.getUid() );

            if ( userGroup == null )
            {
                throw new WebMessageException(
                    WebMessageUtils.notFound( "User Group does not exist: " + ug.getUid() ) );
            }

            messageConversation.getUsers().addAll( userGroup.getMembers() );
        }

        if ( messageConversation.getUsers().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "No recipients selected." ) );
        }

        String metaData = MessageService.META_USER_AGENT + request.getHeader( ContextUtils.HEADER_USER_AGENT );

        int id = messageService.sendPrivateMessage( messageConversation.getSubject(), messageConversation.getText(), metaData,
            messageConversation.getUsers() );

        org.hisp.dhis.message.MessageConversation conversation = messageService.getMessageConversation( id );

        response
            .addHeader( "Location", MessageConversationSchemaDescriptor.API_ENDPOINT + "/" + conversation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Message conversation created" ), response, request );
    }

    //--------------------------------------------------------------------------
    // POST for reply on existing MessageConversation
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.POST )
    public void postMessageConversationReply(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "internal", defaultValue = "false" ) boolean internal,
        @RequestBody String body,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        String metaData = MessageService.META_USER_AGENT + request.getHeader( ContextUtils.HEADER_USER_AGENT );

        org.hisp.dhis.message.MessageConversation conversation = messageService.getMessageConversation( uid );

        if ( conversation == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Message conversation does not exist: " + uid ) );
        }

        if ( internal && !messageService.hasAccessToManageFeedbackMessages( currentUserService.getCurrentUser() ) )
        {
            throw new AccessDeniedException( "Not authorized to send internal messages" );
        }

        messageService.sendReply( conversation, body, metaData, internal );

        response
            .addHeader( "Location", MessageConversationSchemaDescriptor.API_ENDPOINT + "/" + conversation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Message conversation created" ), response, request );
    }

    //--------------------------------------------------------------------------
    // POST for feedback
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/feedback", method = RequestMethod.POST )
    public void postMessageConversationFeedback( @RequestParam( "subject" ) String subject, @RequestBody String body,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        String metaData = MessageService.META_USER_AGENT + request.getHeader( ContextUtils.HEADER_USER_AGENT );

        messageService.sendTicketMessage( subject, body, metaData );

        webMessageService.send( WebMessageUtils.created( "Feedback created" ), response, request );
    }

    //--------------------------------------------------------------------------
    // Assign priority
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/priority", method = RequestMethod.POST, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode setMessagePriority(
        @PathVariable String uid, @RequestParam MessageConversationPriority messageConversationPriority,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User user = currentUserService.getCurrentUser();

        if ( !canModifyUserConversation( user, user ) &&
            (messageService.hasAccessToManageFeedbackMessages( user )) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        org.hisp.dhis.message.MessageConversation messageConversation = messageService
            .getMessageConversation( uid );

        if ( messageConversation == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversation found for the given ID." ) );
            return responseNode;
        }

        CollectionNode marked = responseNode
            .addChild( new CollectionNode( messageConversationPriority.name() ) );
        marked.setWrapping( false );

        messageConversation.setPriority( messageConversationPriority );
        messageService.updateMessageConversation( messageConversation );
        marked.addChild( new SimpleNode( "uid", messageConversation.getUid() ) );
        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Assign status
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/status", method = RequestMethod.POST, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode setMessageStatus(
        @PathVariable String uid,
        @RequestParam MessageConversationStatus messageConversationStatus,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User user = currentUserService.getCurrentUser();

        if ( !canModifyUserConversation( user, user ) &&
            (messageService.hasAccessToManageFeedbackMessages( user )) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        org.hisp.dhis.message.MessageConversation messageConversation = messageService
            .getMessageConversation( uid );

        if ( messageConversation == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversation found for the given ID." ) );
            return responseNode;
        }

        CollectionNode marked = responseNode
            .addChild( new CollectionNode( messageConversationStatus.name() ) );
        marked.setWrapping( false );

        messageConversation.setStatus( messageConversationStatus );
        messageService.updateMessageConversation( messageConversation );
        marked.addChild( new SimpleNode( "uid", messageConversation.getUid() ) );
        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Assign user
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/assign", method = RequestMethod.POST, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode setUserAssigned(
        @PathVariable String uid,
        @RequestParam( required = false ) String userId,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User user = currentUserService.getCurrentUser();

        if ( !canModifyUserConversation( user, user ) &&
            (messageService.hasAccessToManageFeedbackMessages( user )) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        org.hisp.dhis.message.MessageConversation messageConversation = messageService
            .getMessageConversation( uid );

        if ( messageConversation == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversation found for the given ID." ) );
            return responseNode;
        }

        User userToAssign;

        if ( (userToAssign = userService.getUser( userId )) == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "Could not find user to assign" ) );
            return responseNode;
        }

        if ( !configurationService.isUserInFeedbackRecipientUserGroup( userToAssign ) )
        {
            response.setStatus( HttpServletResponse.SC_CONFLICT );
            responseNode.addChild( new SimpleNode( "message", "User provided is not a member of the system's feedback recipient group" ) );
            return responseNode;
        }

        messageConversation.setAssignee( userToAssign );
        messageService.updateMessageConversation( messageConversation );
        responseNode.addChild( new SimpleNode( "message", "User " + userToAssign.getName() + " was assigned to ticket" ) );
        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }
    //--------------------------------------------------------------------------
    // Remove assigned user
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/assign", method = RequestMethod.DELETE, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode removeUserAssigned(
        @PathVariable String uid,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User user = currentUserService.getCurrentUser();

        if ( !canModifyUserConversation( user, user ) &&
            (messageService.hasAccessToManageFeedbackMessages( user )) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        org.hisp.dhis.message.MessageConversation messageConversation = messageService
            .getMessageConversation( uid );

        if ( messageConversation == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversation found for the given ID." ) );
            return responseNode;
        }

        messageConversation.setAssignee( null );
        messageService.updateMessageConversation( messageConversation );
        responseNode.addChild( new SimpleNode( "message", "Message is no longer assigned to user" ) );
        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Mark conversations read
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/read", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode markMessageConversationRead(
        @PathVariable String uid, @RequestParam( required = false ) String userUid, HttpServletResponse response )
    {
        return modifyMessageConversationRead( userUid, Lists.newArrayList( uid ), response, true );
    }

    @RequestMapping( value = "/read", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode markMessageConversationsRead(
        @RequestParam( value = "user", required = false ) String userUid, @RequestBody List<String> uids,
        HttpServletResponse response )
    {
        return modifyMessageConversationRead( userUid, uids, response, true );
    }

    //--------------------------------------------------------------------------
    // Mark conversations unread
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/unread", method = RequestMethod.POST, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode markMessageConversationUnread(
        @PathVariable String uid, @RequestParam( required = false ) String userUid, HttpServletResponse response )
    {
        return modifyMessageConversationRead( userUid, Lists.newArrayList( uid ), response, false );
    }

    @RequestMapping( value = "/unread", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode markMessageConversationsUnread(
        @RequestParam( value = "user", required = false ) String userUid, @RequestBody List<String> uids,
        HttpServletResponse response )
    {
        return modifyMessageConversationRead( userUid, uids, response, false );
    }

    //--------------------------------------------------------------------------
    // Mark conversations for follow up
    //--------------------------------------------------------------------------

    @RequestMapping( value = "followup", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode markMessageConversationFollowup(
        @RequestParam( value = "user", required = false ) String userUid, @RequestBody List<String> uids,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User currentUser = currentUserService.getCurrentUser();
        User user = userUid != null ? userService.getUser( userUid ) : currentUser;

        if ( user == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No user with uid: " + userUid ) );
            return responseNode;
        }

        if ( !canModifyUserConversation( currentUser, user ) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        Collection<org.hisp.dhis.message.MessageConversation> messageConversations = messageService
            .getMessageConversations( user, uids );

        if ( messageConversations.isEmpty() )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversations found for the given UIDs" ) );
            return responseNode;
        }

        CollectionNode marked = responseNode.addChild( new CollectionNode( "markedFollowup" ) );
        marked.setWrapping( false );

        for ( org.hisp.dhis.message.MessageConversation conversation : messageConversations )
        {
            if ( !conversation.isFollowUp() )
            {
                conversation.toggleFollowUp( user );
                messageService.updateMessageConversation( conversation );
            }
            marked.addChild( new SimpleNode( "uid", conversation.getUid() ) );
        }

        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Clear follow up
    //--------------------------------------------------------------------------

    @RequestMapping( value = "unfollowup", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode unmarkMessageConversationFollowup(
        @RequestParam( value = "user", required = false ) String userUid, @RequestBody List<String> uids,
        HttpServletResponse response )
    {
        RootNode responseNode = new RootNode( "response" );

        User currentUser = currentUserService.getCurrentUser();
        User user = userUid != null ? userService.getUser( userUid ) : currentUser;

        if ( user == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No user with uid: " + userUid ) );
            return responseNode;
        }

        if ( !canModifyUserConversation( currentUser, user ) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        Collection<org.hisp.dhis.message.MessageConversation> messageConversations = messageService
            .getMessageConversations( user, uids );

        if ( messageConversations.isEmpty() )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversations found for the given UIDs" ) );
            return responseNode;
        }

        CollectionNode marked = responseNode.addChild( new CollectionNode( "unmarkedFollowup" ) );
        marked.setWrapping( false );

        for ( org.hisp.dhis.message.MessageConversation conversation : messageConversations )
        {
            if ( conversation.isFollowUp() )
            {
                conversation.toggleFollowUp( user );
                messageService.updateMessageConversation( conversation );
            }
            marked.addChild( new SimpleNode( "uid", conversation.getUid() ) );
        }

        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Delete a MessageConversation (requires override auth)
    //--------------------------------------------------------------------------

    /**
     * Deletes a MessageConversation.
     * Note that this is a HARD delete and therefore requires override authority for the current user.
     *
     * @param uid the uid of the MessageConversation to delete.
     * @throws Exception
     */
    @Override
    @PreAuthorize( "hasRole('ALL') or hasRole('F_METADATA_IMPORT')" )
    public void deleteObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        super.deleteObject( uid, request, response );
    }

    //--------------------------------------------------------------------------
    // Remove a user from a MessageConversation
    // In practice a DELETE on MessageConversation <-> User relationship
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{mc-uid}/{user-uid}", method = RequestMethod.DELETE, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode removeUserFromMessageConversation(
        @PathVariable( value = "mc-uid" ) String mcUid, @PathVariable( value = "user-uid" ) String userUid,
        HttpServletResponse response )
        throws DeleteAccessDeniedException
    {
        RootNode responseNode = new RootNode( "reply" );

        User user = userService.getUser( userUid );

        if ( user == null )
        {
            responseNode.addChild( new SimpleNode( "message", "No user with uid: " + userUid ) );
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            return responseNode;
        }

        if ( !canModifyUserConversation( currentUserService.getCurrentUser(), user ) )
        {

            throw new DeleteAccessDeniedException( "Not authorized to modify user: " + user.getUid() );
        }

        org.hisp.dhis.message.MessageConversation messageConversation = messageService.getMessageConversation( mcUid );

        if ( messageConversation == null )
        {
            responseNode.addChild( new SimpleNode( "message", "No messageConversation with uid: " + mcUid ) );
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            return responseNode;
        }

        CollectionNode removed = responseNode.addChild( new CollectionNode( "removed" ) );

        if ( messageConversation.remove( user ) )
        {
            messageService.updateMessageConversation( messageConversation );
            removed.addChild( new SimpleNode( "uid", messageConversation.getUid() ) );
        }

        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Remove a user from one or more MessageConversations (batch operation)
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.DELETE, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE } )
    public @ResponseBody RootNode removeUserFromMessageConversations(
        @RequestParam( "mc" ) List<String> mcUids, @RequestParam( value = "user", required = false ) String userUid,
        HttpServletResponse response )
        throws DeleteAccessDeniedException
    {
        RootNode responseNode = new RootNode( "response" );

        User currentUser = currentUserService.getCurrentUser();

        User user = userUid == null ? currentUser : userService.getUser( userUid );

        if ( user == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "User does not exist: " + userUid ) );
            return responseNode;
        }

        if ( !canModifyUserConversation( currentUser, user ) )
        {
            throw new DeleteAccessDeniedException( "Not authorized to modify user: " + user.getUid() );
        }

        Collection<org.hisp.dhis.message.MessageConversation> messageConversations = messageService
            .getMessageConversations( user, mcUids );

        if ( messageConversations.isEmpty() )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversations found for the given UIDs." ) );
            return responseNode;
        }

        CollectionNode removed = responseNode.addChild( new CollectionNode( "removed" ) );

        for ( org.hisp.dhis.message.MessageConversation mc : messageConversations )
        {
            if ( mc.remove( user ) )
            {
                messageService.updateMessageConversation( mc );
                removed.addChild( new SimpleNode( "uid", mc.getUid() ) );
            }
        }

        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    /**
     * Determines whether the current user has permission to modify the given user in a MessageConversation.
     * <p>
     * The modification is either marking a conversation read/unread for the user or removing the user from the MessageConversation.
     * <p>
     * Since there are no per-conversation authorities provided the permission is given if the current user equals the user
     * or if the current user has update-permission to User objects.
     *
     * @param currentUser the current user to check authorization for.
     * @param user        the user to remove from a conversation.
     * @return true if the current user is allowed to remove the user from a conversation, false otherwise.
     */
    private boolean canModifyUserConversation( User currentUser, User user )
    {
        return currentUser.equals( user ) || currentUser.isSuper();
    }

    /**
     * Determines whether the given user has permission to read the MessageConversation.
     *
     * @param user                the user to check permission for.
     * @param messageConversation the MessageConversation to access.
     * @return true if the user can read the MessageConversation, false otherwise.
     */
    private boolean canReadMessageConversation( User user,
        org.hisp.dhis.message.MessageConversation messageConversation )
    {
        return messageConversation.getUsers().contains( user ) || user.isSuper();
    }

    /**
     * Internal handler for setting the read property of MessageConversation.
     *
     * @param readValue true when setting as read, false when setting unread.
     */
    private RootNode modifyMessageConversationRead( String userUid, List<String> uids, HttpServletResponse response,
        boolean readValue )
    {
        RootNode responseNode = new RootNode( "response" );

        User currentUser = currentUserService.getCurrentUser();
        User user = userUid != null ? userService.getUser( userUid ) : currentUser;

        if ( user == null )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No user with uid: " + userUid ) );
            return responseNode;
        }

        if ( !canModifyUserConversation( currentUser, user ) )
        {
            throw new UpdateAccessDeniedException( "Not authorized to modify this object." );
        }

        Collection<org.hisp.dhis.message.MessageConversation> messageConversations = messageService
            .getMessageConversations( user, uids );

        if ( messageConversations.isEmpty() )
        {
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            responseNode.addChild( new SimpleNode( "message", "No MessageConversations found for the given IDs." ) );
            return responseNode;
        }

        CollectionNode marked = responseNode
            .addChild( new CollectionNode( readValue ? "markedRead" : "markedUnread" ) );
        marked.setWrapping( false );

        for ( org.hisp.dhis.message.MessageConversation conversation : messageConversations )
        {

            boolean success = (readValue ? conversation.markRead( user ) : conversation.markUnread( user ));
            if ( success )
            {
                messageService.updateMessageConversation( conversation );
                marked.addChild( new SimpleNode( "uid", conversation.getUid() ) );
            }
        }

        response.setStatus( HttpServletResponse.SC_OK );

        return responseNode;
    }
}
