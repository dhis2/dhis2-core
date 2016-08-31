package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

/**
 * The main interface for working with user settings. Implementation need to get
 * the current user from {@link CurrentUserService}.
 * 
 * @author Torgeir Lorange Ostby
 */
public interface UserSettingService
{
    String ID = UserSettingService.class.getName();

    String AUTO_SAVE_DATA_ENTRY_FORM = "autoSaveDataEntryForm";
    String KEY_CURRENT_DOMAIN_TYPE = "currentDomainType";
    String KEY_STYLE = "stylesheet";
    String KEY_STYLE_DIRECTORY = "stylesheetDirectory";
    String KEY_MESSAGE_EMAIL_NOTIFICATION = "keyMessageEmailNotification";
    String KEY_MESSAGE_SMS_NOTIFICATION = "keyMessageSmsNotification";
    String KEY_UI_LOCALE = "keyUiLocale";
    String KEY_DB_LOCALE = "keyDbLocale";
    String KEY_ANALYSIS_DISPLAY_PROPERTY = "keyAnalysisDisplayProperty";
    String AUTO_SAVE_CASE_ENTRY_FORM = "autoSaveCaseEntryForm";
    String AUTO_SAVE_TRACKED_ENTITY_REGISTRATION_ENTRY_FORM = "autoSavetTrackedEntityForm";
    
    String DEFAULT_ANALYSIS_DISPLAY_PROPERTY = "name";
    
    //TODO use enum for names
    
    // -------------------------------------------------------------------------
    // UserSettings
    // -------------------------------------------------------------------------

    /**
     * Adds a UserSetting.
     *
     * @param userSetting the UserSetting to add.
     */
    void addUserSetting( UserSetting userSetting );
    
    /**
     * Saves the name/value pair as a user setting connected to the currently
     * logged in user.
     * 
     * @param name the name/handle of the value.
     * @param value the value to store.
     * @throws NoCurrentUserException if there is no current user.
     */
    void saveUserSetting( String name, Serializable value );

    /**
     * Saves the name/value pair as a user setting connected to user identified 
     * by username.
     *
     * @param name the name/handle of the value.
     * @param value the value to store.
     * @param username the username of user.
     * @throws NoCurrentUserException if there is no user.
     */
    void saveUserSetting( String name, Serializable value, String username );

    /**
     * Saves the name/value pair as a user setting connected to user.
     *
     * @param name the name/handle of the value.
     * @param value the value to store.
     * @param username the user.
     * @throws NoCurrentUserException if there is no user.
     */
    void saveUserSetting( String name, Serializable value, User user );

    /**
     * Deletes a UserSetting.
     *
     * @param userSetting the UserSetting to delete.
     */
    void deleteUserSetting( UserSetting userSetting );

    /**
     * Deletes the user setting with the given name.
     * 
     * @param name the name of the user setting to delete.
     * @throws NoCurrentUserException if there is no current user.
     */
    void deleteUserSetting( String name );

    /**
     * Deletes the user setting with the given name for the given user.
     * 
     * @param name the name of the user setting to delete.
     * @user the user.
     */
    void deleteUserSetting( String name, User user );
    
    /**
     * Returns the value of the user setting specified by the given name.
     * 
     * @param name the name of the user setting.
     * @return the value corresponding to the named user setting, or null if
     *         there is no match.
     * @throws NoCurrentUserException if there is no current user.
     */
    Serializable getUserSetting( String name );

    /**
     * Returns the value of the user setting specified by the given name. If
     * there is no current user or the user setting doesn't exist, the specified
     * default value is returned.
     * 
     * @param name the name of the user setting.
     * @param defaultValue the value to return if there is no current user or no
     *        user setting corresponding to the given name.
     * @return the value corresponding to the names user setting, or the default
     *         value if there is no current user or matching user setting.
     */
    Serializable getUserSetting( String name, Serializable defaultValue );

    /**
     * Retrieves a user setting value for the given user and setting name. Returns
     * the given default value if the setting does not exist or the setting value
     * is null.
     *
     * @param name         the setting name.
     * @param defaultValue the default value.
     * @param user         the user.
     * @return a setting value.
     */
    Serializable getUserSetting( String name, Serializable defaultValue, User user );

    /**
     * Retrieves all UserSettings for the given User.
     *
     * @param user the User.
     * @return a List of UserSettings.
     */
    List<UserSetting> getAllUserSettings( User user );

    /**
     * Returns all user settings belonging to the current user.
     * 
     * @return all user settings belonging to the current user.
     * @throws NoCurrentUserException if there is no current user.
     */
    List<UserSetting> getAllUserSettings();
    
    /**
     * Invalidates in-memory caches.
     */
    void invalidateCache();
}
