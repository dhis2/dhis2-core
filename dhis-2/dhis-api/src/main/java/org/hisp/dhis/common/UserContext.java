package org.hisp.dhis.common;

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

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.user.UserSettingKey;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class UserContext
{
    private static final ThreadLocal<User> threadUser = new ThreadLocal<>();

    private static final ThreadLocal<Map<String, Serializable>> threadUserSettings = new ThreadLocal<>();

    public static void reset()
    {
        threadUser.remove();
        threadUserSettings.remove();
    }

    public static void setUser( User user )
    {
        threadUser.set( user );
    }

    public static User getUser()
    {
        return threadUser.get();
    }

    public static boolean haveUser()
    {
        return getUser() != null;
    }

    // TODO need synchronized ?
    public static void setUserSetting( UserSettingKey key, Serializable value )
    {
        UserContext.setUserSetting( key.getName(), value );
    }

    public static void setUserSetting( String key, Serializable value )
    {
        if ( threadUserSettings.get() == null )
        {
            threadUserSettings.set( new HashMap<>() );
        }

        if ( value != null )
        {
            threadUserSettings.get().put( key, value );
        }
        else
        {
            threadUserSettings.get().remove( key );
        }
    }

    public static Serializable getUserSetting( UserSettingKey key )
    {
        return threadUserSettings.get() != null ? threadUserSettings.get().get( key.getName() ) : null;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T getUserSetting( UserSettingKey key, Class<T> klass )
    {
        return threadUserSettings.get() != null ? (T) threadUserSettings.get().get( key.getName() ) : null;
    }

    public static boolean haveUserSetting( UserSettingKey key )
    {
        return getUserSetting( key ) != null;
    }

    public static void setUserSettings( List<UserSetting> userSettings )
    {
        userSettings.stream()
            .filter( userSetting -> !StringUtils.isEmpty( userSetting.getName() ) )
            .forEach( userSetting -> UserContext.setUserSetting( userSetting.getName(), userSetting.getValue() ) );
    }
}
