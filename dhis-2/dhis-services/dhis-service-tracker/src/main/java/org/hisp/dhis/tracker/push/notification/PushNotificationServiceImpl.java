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
package org.hisp.dhis.tracker.push.notification;

import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.onesignal.client.ApiClient;
import com.onesignal.client.ApiException;
import com.onesignal.client.Configuration;
import com.onesignal.client.api.DefaultApi;
import com.onesignal.client.auth.HttpBearerAuth;
import com.onesignal.client.model.*;

@Service
public class PushNotificationServiceImpl implements PushNotificationService
{

    private static final String APP_KEY_TOKEN = "Y2MzNjJiODEtYmIxNS00YjFhLWJmMWQtMDhiNjU5NDhhNzM2";

    private static final String USER_KEY_TOKEN = "35b5e65e-377b-49f4-b613-ed9d206a208a";

    private static final String ICON_URL = "src/main/webapp/dhis-web-commons/security/logo_mobile.png";

    private final DefaultApi defaultApi;

    @Getter
    private final ApiClient defaultClient;

    private final Logger logger;

    public PushNotificationServiceImpl()
    {
        logger = Logger.getLogger( PushNotificationServiceImpl.class.getName() );
        defaultClient = Configuration.getDefaultApiClient();
        defaultApi = new DefaultApi( defaultClient );

        HttpBearerAuth appKey = (HttpBearerAuth) defaultClient.getAuthentication( "app_key" );
        appKey.setBearerToken( APP_KEY_TOKEN );

        HttpBearerAuth userKey = (HttpBearerAuth) defaultClient.getAuthentication( "user_key" );
        userKey.setBearerToken( USER_KEY_TOKEN );
    }

    @SuppressWarnings( "unchecked" )
    private Notification createNotification( boolean isSilent, String teiId )
    {
        Notification notification = new Notification();
        notification.setAppId( USER_KEY_TOKEN );
        notification.setIsAndroid( true );

        StringMap contentStringMap = new StringMap();
        contentStringMap.en( "New tracked entity instance enrollment" );
        notification.setContents( contentStringMap );
        notification.setIncludedSegments( List.of( new String[] { "Subscribed Users" } ) );
        notification.setAdmSmallIcon( ICON_URL );
        notification.setLargeIcon( ICON_URL );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put( "isSilent", isSilent );
        jsonObject.put( "teiId", teiId );
        notification.setData( jsonObject );

        return notification;
    }

    public boolean sendNotification( boolean isSilent, String teiId )
    {
        Notification notification = createNotification( isSilent, teiId );

        try
        {
            CreateNotificationSuccessResponse response = defaultApi
                .createNotification( notification );

            logger.info( "Notification id: " + response.getId() );

            return !response.getId().isEmpty();
        }
        catch ( ApiException e )
        {
            logger.severe( "Push notification not sent: " + e );
            return false;
        }
    }

    public static void main( String[] args )
        throws ApiException
    {
        PushNotificationServiceImpl pushService = new PushNotificationServiceImpl();
        pushService.sendNotification( true, "ay2hFKxwqNR" );
    }
}