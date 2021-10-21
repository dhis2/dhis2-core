/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.system;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.commons.util.TextUtils.LN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.system.util.DhisHttpResponse;
import org.hisp.dhis.system.util.HttpUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.hibernate.HibernateUserCredentialsStore;
import org.springframework.stereotype.Service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;

/**
 * @author Morten Svanaes
 */
@Slf4j
@Service
public class SystemUpdateService
{
    public static final String DHIS_2_ORG_VERSIONS_JSON = "https://releases.dhis2.org/versions.json";

    public static final String NEW_VERSION_AVAILABLE_MESSAGE_SUBJECT = "System update available";

    private static final int MAX_NOTIFING_RECIPIENTS = 100;

    public static final String FIELD_NAME_VERSION = "version";

    public static final String FIELD_NAME_RELEASE_DATE = "releaseDate";

    public static final String FIELD_NAME_NAME = "name";

    public static final String FIELD_NAME_DOWNLOAD_URL = "downloadUrl";

    public static final String FIELD_NAME_DESCRIPTION_URL = "descriptionUrl";

    public static final String FIELD_NAME_URL = "url";

    private final HibernateUserCredentialsStore hibernateUserCredentialsStore;

    private final UserService userService;

    private final MessageService messageService;

    public SystemUpdateService( MessageService messageService,
        HibernateUserCredentialsStore hibernateUserCredentialsStore, UserService userService )
    {
        checkNotNull( userService );
        checkNotNull( messageService );
        checkNotNull( hibernateUserCredentialsStore );

        this.userService = userService;
        this.messageService = messageService;
        this.hibernateUserCredentialsStore = hibernateUserCredentialsStore;
    }

    public static Map<Semver, Map<String, String>> getLatestNewerThanCurrent()
        throws Exception
    {
        return getLatestNewerThan( getCurrentVersion() );
    }

    public static Map<Semver, Map<String, String>> getLatestNewerThan( Semver currentVersion )
        throws Exception
    {
        JsonObject allVersions = fetchAllVersions();

        List<JsonElement> newerPatchVersions = extractNewerPatchVersions( currentVersion, allVersions );

        return convertJsonToMap( newerPatchVersions );
    }

    private static JsonObject fetchAllVersions()
    {
        try
        {
            DhisHttpResponse httpResponse = HttpUtils.httpGET( DHIS_2_ORG_VERSIONS_JSON, false, null, null, null, 0,
                true );

            int statusCode = httpResponse.getStatusCode();
            if ( statusCode != HttpStatusCodes.STATUS_CODE_OK )
            {
                throw new IllegalStateException(
                    "Failed to fetch the version file, non OK(200) response code. Code was: " + statusCode );
            }

            return new JsonParser().parse( httpResponse.getResponse() ).getAsJsonObject();
        }
        catch ( Exception e )
        {
            log.error( "Failed to fetch list of latest versions.", e );
            throw new IllegalStateException( "Failed to fetch list of latest versions.", e );
        }
    }

    protected static List<JsonElement> extractNewerPatchVersions( Semver currentVersion, JsonObject allVersions )
    {
        List<JsonElement> newerPatchVersions = new ArrayList<>();

        for ( JsonElement versionElement : allVersions.getAsJsonArray( "versions" ) )
        {
            // This presumes that the name variable is always in the format e.g.
            // "2.36",
            // we need to at patch version to the string, so we can parse it as
            // a valid SemVer
            Semver semver = new Semver(
                versionElement.getAsJsonObject().getAsJsonPrimitive( FIELD_NAME_NAME ).getAsString() + ".0" );

            // Skip other major/minor versions, we are only interested in the
            // patch versions on the current installed.
            if ( !Objects.equals( currentVersion.getMajor(), semver.getMajor() ) || !Objects.equals(
                currentVersion.getMinor(), semver.getMinor() ) )
            {
                continue;
            }

            int latestPatchVersion = versionElement.getAsJsonObject().getAsJsonPrimitive( "latestPatchVersion" )
                .getAsInt();

            // We are on current latest patch version or a greater one (should
            // not possible, but better to be sure we don't proceed if it is)
            if ( currentVersion.getPatch() >= latestPatchVersion )
            {
                // We are on latest patch version, nothing to report here, break
                // out.
                log.debug( "We are on latest patch release version, nothing to do here." );
                break;
            }

            for ( JsonElement patchElement : versionElement.getAsJsonObject().getAsJsonArray( "patchVersions" ) )
            {
                int patchVersion = patchElement.getAsJsonObject().getAsJsonPrimitive( FIELD_NAME_VERSION ).getAsInt();

                // If new patch version is greater than current patch version,
                // add it to the list of alerts we want to send
                if ( currentVersion.getPatch() < patchVersion )
                {
                    log.debug( "Found a new patch version, adding it the result list; version=" + patchVersion );
                    newerPatchVersions.add( patchElement );
                }
            }
        }

        return newerPatchVersions;
    }

    protected static Map<Semver, Map<String, String>> convertJsonToMap( List<JsonElement> newerPatchVersions )
    {
        Map<Semver, Map<String, String>> versionsAndMetadata = new TreeMap<>();

        // Parse the new versions we "found" in the release json into a sorted
        // map.
        for ( JsonElement newerPatchVersion : newerPatchVersions )
        {
            JsonObject patchJsonObject = newerPatchVersion.getAsJsonObject();

            String versionName = patchJsonObject.getAsJsonPrimitive( FIELD_NAME_NAME ).getAsString();
            log.info( "Found a newer patch version; version=" + versionName );

            Map<String, String> metadata = new HashMap<>();
            metadata.put( FIELD_NAME_VERSION, patchJsonObject.getAsJsonPrimitive( FIELD_NAME_NAME ).getAsString() );
            metadata.put( FIELD_NAME_RELEASE_DATE,
                patchJsonObject.getAsJsonPrimitive( FIELD_NAME_RELEASE_DATE ).getAsString() );
            metadata.put( FIELD_NAME_DOWNLOAD_URL, patchJsonObject.getAsJsonPrimitive( FIELD_NAME_URL ).getAsString() );

            // We need something like this to get metadata
            // metadata.put( DESCRIPTION_URL,
            // patchJsonObject.getAsJsonPrimitive( DESCRIPTION_URL
            // ).getAsString() );

            versionsAndMetadata.put( new Semver( versionName ), metadata );
        }

        return versionsAndMetadata;
    }

    public void sendMessageForEachVersion( Map<Semver, Map<String, String>> patchVersions )
    {
        List<User> recipients = getRecipients();

        for ( Map.Entry<Semver, Map<String, String>> entry : patchVersions.entrySet() )
        {
            String messageText = buildMessageText( entry.getValue() );

            for ( User recipient : recipients )
            {
                // Check to see if message has already been sent before.
                // NOTE: Messages are always soft deleted, so we also search the
                // "deleted" messages.
                List<MessageConversation> messagesConv = messageService
                    .getMessagesConversationFromSenderMatching( recipient, messageText );

                // Only send message if there is no similar message text been
                // sent to the user before.
                if ( messagesConv.isEmpty() )
                {
                    MessageConversationParams params = new MessageConversationParams.Builder()
                        .withRecipients( ImmutableSet.of( recipient ) )
                        .withSubject( NEW_VERSION_AVAILABLE_MESSAGE_SUBJECT )
                        .withText( messageText )
                        .withMessageType( MessageType.SYSTEM ).build();

                    messageService.sendMessage( params );
                }
            }
        }
    }

    private List<User> getRecipients()
    {
        // TODO: Should we use the getFeedbackRecipients only/not or both?
        // final Set<User> recipients = messageService.getFeedbackRecipients();

        List<UserCredentials> usersWithAllAuthority = hibernateUserCredentialsStore.getUsersWithAuthority( "ALL" );

        List<User> recipients = new ArrayList<>();
        for ( UserCredentials userCredentials : usersWithAllAuthority )
        {
            User userByUsername = userService.getUserByUsername( userCredentials.getUsername() );
            recipients.add( userByUsername );
        }

        if ( recipients.size() > MAX_NOTIFING_RECIPIENTS )
        {
            log.warn( "There is more recipients than the max allowed, limiting recipients list to max allowed size: "
                + MAX_NOTIFING_RECIPIENTS );
            recipients = recipients.subList( 0, MAX_NOTIFING_RECIPIENTS - 1 );
        }

        return recipients;
    }

    private String buildMessageText( Map<String, String> messageValues )
    {
        String version = messageValues.get( FIELD_NAME_VERSION );
        String releaseDate = messageValues.get( FIELD_NAME_RELEASE_DATE );
        String downloadUrl = messageValues.get( FIELD_NAME_DOWNLOAD_URL );
        String descriptionUrl = MoreObjects.firstNonNull( messageValues.get( FIELD_NAME_DESCRIPTION_URL ), "" );

        return NEW_VERSION_AVAILABLE_MESSAGE_SUBJECT + LN + LN
            + "Version: " + version + LN
            + "Release date: " + releaseDate + LN
            + "Download URL: " + downloadUrl + LN;
        // + "Description: " + descriptionUrl + LN + LN;
    }

    private Map<String, Collection<MessageConversation>> getAllRecipientsMessages( Set<User> recipients )
    {
        Map<String, Collection<MessageConversation>> usersMessages = new HashMap<>();
        for ( User recipient : recipients )
        {
            Collection<MessageConversation> messages = messageService.getMessageConversationsForUser( recipient, null,
                null );

            usersMessages.put( recipient.getUsername(), messages );
        }

        return usersMessages;
    }

    private boolean hasMessageAlready( Collection<MessageConversation> messageConversations, String messageToSend )
    {
        for ( MessageConversation messageConversation : messageConversations )
        {
            List<Message> messages = messageConversation.getMessages();
            for ( Message message : messages )
            {
                String text = message.getText();
                if ( messageToSend.equals( text ) )
                {
                    log.debug( "Message already exist; message=" + text );
                    return true;
                }
            }
        }

        return false;
    }

    public static Semver getCurrentVersion()
    {
        String buildVersion = DefaultSystemService.loadBuildProperties().getVersion();

        // If we are on a snapshot version, convert '-SNAPSHOT' to
        // '.Int.MAX_VALUE', so we can sort it on top of the list
        if ( buildVersion.contains( "SNAPSHOT" ) )
        {
            log.info( "We are running a SNAPSHOT version, handle current patch version as Integer.MAX_VALUE." );
            buildVersion = buildVersion.replace( "-SNAPSHOT", "." + Integer.MAX_VALUE );
        }

        return new Semver( buildVersion );
    }
}
