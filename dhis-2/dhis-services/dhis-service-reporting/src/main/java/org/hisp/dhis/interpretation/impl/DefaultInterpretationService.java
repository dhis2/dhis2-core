package org.hisp.dhis.interpretation.impl;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationComment;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.interpretation.InterpretationStore;
import org.hisp.dhis.interpretation.MentionUtils;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultInterpretationService 
    implements InterpretationService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private InterpretationStore interpretationStore;

    public void setInterpretationStore( InterpretationStore interpretationStore )
    {
        this.interpretationStore = interpretationStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }
    
    private AclService aclService;

    public void setAclService( AclService aclService )
    {
        this.aclService = aclService;
    }

    private I18nManager i18nManager;

    public void setI18nManager( I18nManager i18nManager )
    {
        this.i18nManager = i18nManager;
    }

    private enum NotificationType
    {
        INTERPRETATION_CREATE,
        INTERPRETATION_UPDATE,
        INTERPRETATION_LIKE,
        COMMENT_CREATE,
        COMMENT_UPDATE
    }

    @Autowired
    private SchemaService schemaService;

    // -------------------------------------------------------------------------
    // InterpretationService implementation
    // -------------------------------------------------------------------------

    @Override
    public int saveInterpretation( Interpretation interpretation )
    {
        User user = currentUserService.getCurrentUser();
        
        Set<User> users = new HashSet<>();

        if ( interpretation != null )
        {
            if ( user != null )
            {
                interpretation.setUser( user );
            }

            if ( interpretation.getPeriod() != null )
            {
                interpretation.setPeriod( periodService.reloadPeriod( interpretation.getPeriod() ) );
            }

            users = MentionUtils.getMentionedUsers( interpretation.getText(), userService );            
            interpretation.setMentionsFromUsers( users );            
            updateSharingForMentions( interpretation, users );
        }

        interpretationStore.save( interpretation );
        notifySubscribers( interpretation, null, NotificationType.INTERPRETATION_CREATE );

        sendMentionNotifications( interpretation, null, users );

        return interpretation.getId();
    }

    @Override
    public Interpretation getInterpretation( int id )
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
        updateSharingForMentions( interpretation, users );
        interpretationStore.update( interpretation );
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
    public List<Interpretation> getInterpretations( Date lastUpdated )
    {
        return interpretationStore.getAllGeLastUpdated( lastUpdated );
    }

    @Override
    public List<Interpretation> getInterpretations( int first, int max )
    {
        return interpretationStore.getAllOrderedLastUpdated( first, max );
    }

    private MessageConversationParams getNotificationMessage(
        Set<User> users,
        Interpretation interpretation,
        InterpretationComment comment,
        NotificationType notificationType
    )
    {
        I18n i18n = i18nManager.getI18n();
        String currentUsername = currentUserService.getCurrentUser().getUsername();
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
                throw new RuntimeException( "Unknown notification type: " + notificationType );
        }

        String subject = String.join( " ", Arrays.asList(
            i18n.getString( "notification_user" ),
            currentUsername,
            actionString,
            i18n.getString( "notification_object_subscribed" )
        ) );

        String fullBody = String.join( "\n\n", Arrays.asList(
            String.format( "%s: %s", subject, interpretableName ),
            Jsoup.parse( details ).text(),
            String.format( "%s %s", i18n.getString( "go_to" ), getInterpretationLink( interpretation ) )
        ) );
        
        return messageService.createSystemMessage( users, subject, fullBody ).build();
    }

    private void notifySubscribers( Interpretation interpretation, InterpretationComment comment, NotificationType notificationType )
    {
        IdentifiableObject interpretableObject = interpretation.getObject();
        Schema interpretableObjectSchema = schemaService.getDynamicSchema( interpretableObject.getClass() );

        if ( interpretableObjectSchema.isSubscribable() ) {
            SubscribableObject object = (SubscribableObject) interpretableObject;
            Set<User> subscribers = new HashSet<>( userService.getUsers( object.getSubscribers() ) );
            subscribers.remove( currentUserService.getCurrentUser() );
            if ( !subscribers.isEmpty() ){
                MessageConversationParams message = getNotificationMessage( subscribers, interpretation, comment, notificationType );
                messageService.sendMessage( message );
            }
        }
    }

    private void sendMentionNotifications( Interpretation interpretation, InterpretationComment comment, Set<User> users )
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
        messageService.sendMessage( messageService
            .createPrivateMessage( users, subjectContent.toString(), messageContent.toString(), "Meta" ).build() );
    }

    private String getInterpretationLink( Interpretation interpretation ) {
        String path;

        switch ( interpretation.getType() )
        {
        case MAP:
            path = "/dhis-web-mapping/index.html?id=" + interpretation.getMap().getUid() + "&interpretationid="
                + interpretation.getUid();
            break;
        case REPORT_TABLE:
            path = "/dhis-web-pivot/index.html?id=" + interpretation.getReportTable().getUid() + "&interpretationid="
                + interpretation.getUid();
            break;
        case CHART:
            path = "/dhis-web-visualizer/index.html?id=" + interpretation.getChart().getUid() + "&interpretationid="
                + interpretation.getUid();
            break;
        case EVENT_REPORT:
            path = "/dhis-web-event-reports/index.html?id=" + interpretation.getEventReport().getUid() + "&interpretationid="
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
        return systemSettingManager.getInstanceBaseUrl() + path;
    }

    @Override
    public void updateSharingForMentions( Interpretation interpretation, Set<User> users )
    {
        for ( User user : users )
        {
            if ( !aclService.canRead( user, interpretation.getObject() ) )
            {
                interpretation.getObject().getUserAccesses().add( new UserAccess( user, AccessStringHelper.READ ) );
            }
        }
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
            comment.setUser( user );
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

        long count = 0;

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

    @Transactional( isolation = Isolation.REPEATABLE_READ )
    public boolean likeInterpretation( int id )
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

    @Transactional( isolation = Isolation.REPEATABLE_READ )
    public boolean unlikeInterpretation( int id )
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

    @Override
    public int countMapInterpretations( Map map )
    {
        return interpretationStore.countMapInterpretations( map );
    }

    @Override
    public int countChartInterpretations( Chart chart )
    {
        return interpretationStore.countChartInterpretations( chart );
    }

    @Override
    public int countReportTableInterpretations( ReportTable reportTable )
    {
        return interpretationStore.countReportTableInterpretations( reportTable );
    }

    @Override
    public Interpretation getInterpretationByChart( int id )
    {
        return interpretationStore.getByChartId( id );
    }
}
