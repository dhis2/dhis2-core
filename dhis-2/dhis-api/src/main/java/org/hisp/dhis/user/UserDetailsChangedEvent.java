/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Event published after a transaction that changed data held by the session principal ({@link
 * UserDetails}) of users has committed, such as their user roles, organisation units or user group
 * memberships. The principal is a snapshot taken at authentication, so the sessions of these users
 * are marked for a refresh, asynchronously and off the request thread.
 *
 * <p>The users are identified in three ways, since not every writer knows the primary keys of the
 * affected users while it flushes.
 *
 * @param userIds the primary keys of the users whose {@link UserDetails} changed
 * @param memberOfUserGroupIds the primary keys of user groups whose members' {@link UserDetails}
 *     changed, such as a group whose managed groups changed; the members are resolved after commit
 * @param usernames the usernames of users whose {@link UserDetails} changed, used where the user no
 *     longer exists after commit (a deleted user) or where the writer bypasses Hibernate
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public record UserDetailsChangedEvent(
    @Nonnull Set<Long> userIds,
    @Nonnull Set<Long> memberOfUserGroupIds,
    @Nonnull Set<String> usernames) {}
