package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main interface for working with user settings. Implementation need to get
 * the current user from {@link CurrentUserService}.
 *
 * @author Torgeir Lorange Ostby
 */
public interface UserSettingService
{
    String ID = UserSettingService.class.getName();

    // -------------------------------------------------------------------------
    // UserSettings
    // -------------------------------------------------------------------------

    /**
     * Saves the key/value pair as a user setting connected to the currently
     * logged in user.
     *
     * @param key the user setting key.
     * @param value the setting value.
     */
    void saveUserSetting( UserSettingKey key, Serializable value );

    /**
     * Saves the key/value pair as a user setting connected to user identified 
     * by username.
     *
     * @param key the user setting key.
     * @param value the setting value.
     * @param username the username of user.
     */
    void saveUserSetting( UserSettingKey key, Serializable value, String username );

    /**
     * Saves the name/value pair as a user setting connected to user.
     *
     * @param key the user setting key.
     * @param value the setting value.
     * @param username the username of user.
     */
    void saveUserSetting( UserSettingKey key, Serializable value, User user );

    /**
     * Deletes a UserSetting.
     *
     * @param userSetting the UserSetting to delete.
     */
    void deleteUserSetting( UserSetting userSetting );

    /**
     * Deletes the user setting with the given name.
     *
     * @param key the user setting key.
     */
    void deleteUserSetting( UserSettingKey key );

    /**
     * Deletes the user setting with the given name for the given user.
     *
     * @param key the user setting key.
     * @user the user.
     */
    void deleteUserSetting( UserSettingKey key, User user );

    /**
     * Returns the value of the user setting specified by the given name.
     *
     * @param key the user setting key.
     * @return the value corresponding to the named user setting, or null if
     *         there is no match.
     */
    Serializable getUserSetting( UserSettingKey key );

    /**
     * Returns the value of the user setting specified by the given name.
     *
     * @param key the user setting key.
     * @param user the user.
     * @return the value corresponding to the named user setting, or null if
     *         there is no match.
     */
    Serializable getUserSetting( UserSettingKey key, User user );

    /**
     * Retrieves UserSettings for the given User.
     *
     * @param user the User.
     * @return a List of UserSettings.
     */
    List<UserSetting> getUserSettings( User user );

    /**
     * Returns all user settings belonging to the current user.
     *
     * @return all user settings belonging to the current user.
     */
    List<UserSetting> getAllUserSettings();

    /**
     * Returns all specified user settings. If any user settings have not been set,
     * system settings will be used as a fallback.
     * @param names the settings to retrieve
     * @return a map of setting names and their values
     */
    Map<String, Serializable> getUserSettingsWithFallbackByUserAsMap( User user, Set<String> names, boolean useFallback );

    /**
     * Invalidates in-memory caches.
     */
    void invalidateCache();
}
