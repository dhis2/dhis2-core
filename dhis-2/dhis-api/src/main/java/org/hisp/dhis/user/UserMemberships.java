/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.user;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * The user groups and user roles of a user, with the authorities and restrictions of those roles,
 * as stored in the database.
 *
 * <p>They are read with SQL, not through {@link User#getGroups()} and {@link User#getUserRoles()}:
 * those collections are kept in the second-level cache, and another node of a cluster does not
 * evict them for every change, so a principal built from them on that node can miss the change.
 *
 * @param userGroups the UID of each user group of the user, by its primary key
 * @param userRoles the UID of each user role of the user, by its primary key
 * @param authorities the authorities of the user roles of the user
 * @param restrictions the restrictions of the user roles of the user
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public record UserMemberships(
    @Nonnull Map<Long, String> userGroups,
    @Nonnull Map<Long, String> userRoles,
    @Nonnull Set<String> authorities,
    @Nonnull Set<String> restrictions) {}
