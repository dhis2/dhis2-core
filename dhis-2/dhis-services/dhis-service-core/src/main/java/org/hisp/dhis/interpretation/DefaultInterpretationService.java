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
package org.hisp.dhis.interpretation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.interpretation.InterpretationService" )
@Transactional
public class DefaultInterpretationService
    implements InterpretationService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final SchemaService schemaService;

    private final InterpretationStore interpretationStore;

    private CurrentUserService currentUserService;

    private UserService userService;

    private final PeriodService periodService;

    private final MessageService messageService;

    private final AclService aclService;

    private final I18nManager i18nManager;

    private final DhisConfigurationProvider configurationProvider;

    public DefaultInterpretationService( SchemaService schemaService, InterpretationStore interpretationStore,
        CurrentUserService currentUserService, UserService userService, PeriodService periodService,
        MessageService messageService, AclService aclService, I18nManager i18nManager,
        DhisConfigurationProvider configurationProvider )
    {
        checkNotNull( schemaService );
        checkNotNull( interpretationStore );
        checkNotNull( currentUserService );
        checkNotNull( userService );
        checkNotNull( periodService );
        checkNotNull( messageService );
        checkNotNull( aclService );
        checkNotNull( i18nManager );
        checkNotNull( configurationProvider );
        this.schemaService = schemaService;
        this.interpretationStore = interpretationStore;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.periodService = periodService;
        this.messageService = messageService;
        this.aclService = aclService;
        this.i18nManager = i18nManager;
        this.configurationProvider = configurationProvider;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // InterpretationService implementation
    // -------------------------------------------------------------------------

    @Override
    public long saveInterpretation( Interpretation interpretation )
    {
        User user = currentUserService.getCurrentUser();

        Set<User> users = new HashSet<>();

        if ( user != null )
        {
            interpretation.setCreatedBy( user );
        }

        if ( interpretation.getPeriod() != null )
        {
            interpretation.setPeriod( periodService.reloadPeriod( interpretation.getPeriod() ) );
        }

        users = MentionUtils.getMentionedUsers( interpretation.getText(), userService );
        interpretation.setMentionsFromUsers( users );
        updateSharingForMentions( interpretation, users );

        interpretationStore.save( interpretation );
        notifySubscribers( interpretation, null, NotificationType.INTERPRETATION_CREATE );

        sendMentionNotifications( interpretation, null, users );

        return interpretation.getId();
    }

    @Override
    public Interpretation getInterpretation( long id )
    {
        return interpretationStore.get( id );
    }

    @Override
    public Interpretation getInterpretation( String uid )
    {
        return interpretationStore.getByUid( uid );
    }

    @Override
    public void updateComment( Interpretation interpretation, InterpretationComment comment )
    {
        Set<User> users = MentionUtils.getMentionedUsers( comment.getText(), userService );
        comment.setMentionsFromUsers( users );
        comment.setLastUpdated( new Date() );
        if ( updateSharingForMentions( interpretation, users ) )
        {
            interpretationStore.update( interpretation );
        }
        notifySubscribers( interpretation, comment, NotificationType.COMMENT_UPDATE );
        sendMentionNotifications( interpretation, comment, users );
    }

    @Override
    public void updateInterpretation( Interpretation interpretation )
    {
        interpretationStore.update( interpretation );
    }

    @Override
    public void updateInterpretationText( Interpretation interpretation, String text )
    {
        interpretation.setText( text );
        updateInterpretation( interpretation );
        notifySubscribers( interpretation, null, NotificationType.INTERPRETATION_UPDATE );
        Set<User> users = MentionUtils.getMentionedUsers( interpretation.getText(), userService );
        interpretation.setMentionsFromUsers( users );
        updateSharingForMentions( interpretation, users );
        sendMentionNotifications( interpretation, null, users );
    }

    @Override
    public void deleteInterpretation( Interpretation interpretation )
    {
        interpretationStore.delete( interpretation );
    }

    @Override
    public List<Interpretation> getInterpretations()
    {
        return interpretationStore.getAll();
    }

    @Override
    public List<Interpretation> getInterpretations( Visualization visualization )
    {
        return interpretationStore.getInterpretations( visualization );
    }

    @Override
    public List<Interpretation> getInterpretations( EventVisualization eventVisualization )
    {
        return interpretationStore.getInterpretations( eventVisualization );
    }

    @Override
    public List<Interpretation> getInterpretations( Map map )
    {
        return interpretationStore.getInterpretations( map );
    }

    @Override
    public List<Interpretation> getInterpretations( Date lastUpdated )
    {
        return interpretationStore.getAllGeLastUpdated( lastUpdated );
    }

    @Override
    public List<Interpretation> getInterpretations( int first, int max )
    {
        return interpretationStore.getAllOrderedLastUpdated( first, max );
    }

    private long sendNotificationMessage( Set<User> users, Interpretation interpretation, InterpretationComment comment,
        NotificationType notificationType )
    {
        I18n i18n = i18nManager.getI18n();
        String currentUsername = currentUserService.getCurrentUsername();
        String interpretableName = interpretation.getObject().getName();
        String actionString;
        String details;

        switch ( notificationType )
        {
        case INTERPRETATION_CREATE:
            actionString = i18n.getString( "notification_interpretation_create" );
            details = interpretation.getText();
            break;
        case INTERPRETATION_UPDATE:
            actionString = i18n.getString( "notification_interpretation_update" );
            details = interpretation.getText();
            break;
        case INTERPRETATION_LIKE:
            actionString = i18n.getString( "notification_interpretation_like" );
            details = "";
            break;
        case COMMENT_CREATE:
            actionString = i18n.getString( "notification_comment_create" );
            details = comment.getText();
            break;
        case COMMENT_UPDATE:
            actionString = i18n.getString( "notification_comment_update" );
            details = comment.getText();
            break;
        default:
            throw new IllegalArgumentException( "Unknown notification type: " + notificationType );
        }

        String subject = String.join( " ", Arrays.asList(
            i18n.getString( "notification_user" ),
            currentUsername,
            actionString,
            i18n.getString( "notification_object_subscribed" ) ) );

        String fullBody = String.join( "\n\n", Arrays.asList(
            String.format( "%s: %s", subject, interpretableName ),
            Jsoup.parse( details ).text(),
            String.format( "%s %s", i18n.getString( "go_to" ), getInterpretationLink( interpretation ) ) ) );

        return messageService.sendSystemMessage( users, subject, fullBody );
    }

    private void notifySubscribers( Interpretation interpretation, InterpretationComment comment,
        NotificationType notificationType )
    {
        IdentifiableObject interpretableObject = interpretation.getObject();
        Schema interpretableObjectSchema = schemaService
            .getDynamicSchema( HibernateProxyUtils.getRealClass( interpretableObject ) );

        if ( interpretableObjectSchema.isSubscribable() )
        {
            SubscribableObject object = (SubscribableObject) interpretableObject;
            Set<User> subscribers = new HashSet<>( userService.getUsers( object.getSubscribers() ) );
            subscribers.remove( currentUserService.getCurrentUser() );

            if ( !subscribers.isEmpty() )
            {
                sendNotificationMessage( subscribers, interpretation, comment, notificationType );
            }
        }
    }

    private void sendMentionNotifications( Interpretation interpretation, InterpretationComment comment,
        Set<User> users )
    {
        if ( interpretation == null || users.isEmpty() )
        {
            return;
        }
        String link = getInterpretationLink( interpretation );
        StringBuilder messageContent;
        I18n i18n = i18nManager.getI18n();

        if ( comment != null )
        {
            messageContent = new StringBuilder( i18n.getString( "comment_mention_notification" ) ).append( ":" )
                .append( "\n\n" ).append( Jsoup.parse( comment.getText() ).text() );
        }
        else
        {
            messageContent = new StringBuilder( i18n.getString( "interpretation_mention_notification" ) ).append( ":" )
                .append( "\n\n" ).append( Jsoup.parse( interpretation.getText() ).text() );

        }
        messageContent.append( "\n\n" ).append( i18n.getString( "go_to" ) ).append( " " ).append( link );

        User user = currentUserService.getCurrentUser();
        StringBuilder subjectContent = new StringBuilder( user.getDisplayName() ).append( " " )
            .append( i18n.getString( "mentioned_you_in_dhis2" ) );

        messageService.sendSystemMessage( users, subjectContent.toString(), messageContent.toString() );
    }

    private String getInterpretationLink( Interpretation interpretation )
    {
        String path;

        switch ( interpretation.getType() )
        {
        case MAP:
            path = "/dhis-web-maps/index.html?id=" + interpretation.getMap().getUid() + "&interpretationid="
                + interpretation.getUid();
            break;
        case VISUALIZATION:
            path = "/dhis-web-data-visualizer/index.html#/" + interpretation.getVisualization().getUid()
                + "/interpretation/" + interpretation.getUid();
            break;
        case EVENT_REPORT:
            path = "/dhis-web-event-reports/index.html?id=" + interpretation.getEventReport().getUid()
                + "&interpretationid=" + interpretation.getUid();
            break;
        case EVENT_VISUALIZATION:
            path = "/api/apps/line-listing/#/" + interpretation.getEventVisualization().getUid() + "?interpretationId="
                + interpretation.getUid();
            break;
        case EVENT_CHART:
            path = "/dhis-web-event-visualizer/index.html?id=" + interpretation.getEventChart().getUid()
                + "&interpretationid=" + interpretation.getUid();
            break;
        default:
            path = "";
            break;
        }
        return configurationProvider.getServerBaseUrl() + path;
    }

    @Override
    public boolean updateSharingForMentions( Interpretation interpretation, Set<User> users )
    {
        boolean modified = false;
        IdentifiableObject interpretationObject = interpretation.getObject();

        for ( User user : users )
        {
            if ( !aclService.canRead( user, interpretationObject ) )
            {
                interpretationObject.getSharing().addDtoUserAccess( new UserAccess( user, AccessStringHelper.READ ) );
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public InterpretationComment addInterpretationComment( String uid, String text )
    {
        Interpretation interpretation = getInterpretation( uid );
        User user = currentUserService.getCurrentUser();

        InterpretationComment comment = new InterpretationComment( text );
        comment.setLastUpdated( new Date() );
        comment.setUid( CodeGenerator.generateUid() );

        Set<User> users = MentionUtils.getMentionedUsers( text, userService );
        comment.setMentionsFromUsers( users );
        updateSharingForMentions( interpretation, users );

        if ( user != null )
        {
            comment.setCreatedBy( user );
        }

        interpretation.addComment( comment );
        interpretationStore.update( interpretation );

        notifySubscribers( interpretation, comment, NotificationType.COMMENT_CREATE );
        sendMentionNotifications( interpretation, comment, users );

        return comment;
    }

    @Override
    public void updateCurrentUserLastChecked()
    {
        User user = currentUserService.getCurrentUser();

        user.setLastCheckedInterpretations( new Date() );

        userService.updateUser( user );
    }

    @Override
    public long getNewInterpretationCount()
    {
        User user = currentUserService.getCurrentUser();

        long count;

        if ( user != null && user.getLastCheckedInterpretations() != null )
        {
            count = interpretationStore.getCountGeLastUpdated( user.getLastCheckedInterpretations() );
        }
        else
        {
            count = interpretationStore.getCount();
        }

        return count;
    }

    @Override
    @Transactional( isolation = Isolation.REPEATABLE_READ )
    public boolean likeInterpretation( long id )
    {
        Interpretation interpretation = getInterpretation( id );

        if ( interpretation == null )
        {
            return false;
        }

        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            return false;
        }

        boolean userLike = interpretation.like( user );
        notifySubscribers( interpretation, null, NotificationType.INTERPRETATION_LIKE );

        return userLike;
    }

    @Override
    @Transactional( isolation = Isolation.REPEATABLE_READ )
    public boolean unlikeInterpretation( long id )
    {
        Interpretation interpretation = getInterpretation( id );

        if ( interpretation == null )
        {
            return false;
        }

        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            return false;
        }

        return interpretation.unlike( user );
    }
}
