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

import lombok.Getter;

import com.onesignal.client.ApiClient;
import com.onesignal.client.ApiException;
import com.onesignal.client.Configuration;
import com.onesignal.client.api.DefaultApi;
import com.onesignal.client.auth.HttpBearerAuth;
import com.onesignal.client.model.*;

public class PushNotificationService
{

    private static final String APP_KEY_TOKEN = "Y2MzNjJiODEtYmIxNS00YjFhLWJmMWQtMDhiNjU5NDhhNzM2";

    private static final String USER_KEY_TOKEN = "35b5e65e-377b-49f4-b613-ed9d206a208a";

    private static final String ICON_URL = "src/main/webapp/dhis-web-commons/security/logo_mobile.png";

    @Getter
    private final ApiClient defaultClient;

    public PushNotificationService()
    {
        defaultClient = Configuration.getDefaultApiClient();

        HttpBearerAuth appKey = (HttpBearerAuth) defaultClient.getAuthentication( "app_key" );
        appKey.setBearerToken( APP_KEY_TOKEN );

        HttpBearerAuth userKey = (HttpBearerAuth) defaultClient.getAuthentication( "user_key" );
        userKey.setBearerToken( USER_KEY_TOKEN );
    }

    private Notification createNotification()
    {
        Notification notification = new Notification();
        notification.setAppId( USER_KEY_TOKEN );
        notification.setIsAndroid( true );

        StringMap contentStringMap = new StringMap();
        contentStringMap.en( "Demo content" );
        notification.setContents( contentStringMap );
        notification.setIncludedSegments( List.of( new String[] { "Subscribed Users" } ) );
        notification.setAdmSmallIcon( ICON_URL );
        notification.setLargeIcon( ICON_URL );
        notification.setHuaweiSmallIcon( ICON_URL );
        notification.setHuaweiLargeIcon( ICON_URL );

        return notification;
    }

    public static void main( String[] args )
        throws ApiException
    {
        PushNotificationService pushService = new PushNotificationService();
        DefaultApi defaultApi = new DefaultApi( pushService.getDefaultClient() );

        CreateNotificationSuccessResponse notificationResponse = defaultApi
            .createNotification( pushService.createNotification() );

        System.out.print( notificationResponse.getId() );
    }
}