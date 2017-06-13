package org.hisp.dhis.user;

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

import java.util.List;

public interface UserSettingStore
{
    /**
     * Adds a UserSetting.
     *
     * @param userSetting the UserSetting to add.
     */
    void addUserSetting( UserSetting userSetting );

    /**
     * Updates a UserSetting.
     *
     * @param userSetting the UserSetting to update.
     */
    void updateUserSetting( UserSetting userSetting );

    /**
     * Retrieves the UserSetting associated with the given User for the given
     * UserSetting name.
     *
     * @param user the User.
     * @param name the name of the UserSetting.
     * @return the UserSetting.
     */
    UserSetting getUserSetting( User user, String name );

    /**
     * Retrieves all UserSettings for the given User.
     *
     * @param user the User.
     * @return a List of UserSettings.
     */
    List<UserSetting> getAllUserSettings( User user );

    /**
     * Deletes a UserSetting.
     *
     * @param userSetting the UserSetting to delete.
     */
    void deleteUserSetting( UserSetting userSetting );

    /**
     * Removes all user settings associated with the given user.
     * 
     * @param user the user.
     */
    void removeUserSettings( User user );
}
