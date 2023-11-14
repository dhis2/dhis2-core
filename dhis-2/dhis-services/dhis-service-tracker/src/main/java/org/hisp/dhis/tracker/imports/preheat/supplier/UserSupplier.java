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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.mappers.UserMapper;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
public class UserSupplier extends AbstractPreheatSupplier {
  @Nonnull private final IdentifiableObjectManager manager;

  @Nonnull private final UserService userService;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    Set<String> userUids =
        params.getEvents().stream()
            .filter(Objects::nonNull)
            .map(Event::getAssignedUser)
            .filter(Objects::nonNull)
            .map(User::getUid)
            .filter(CodeGenerator::isValidUid)
            .collect(Collectors.toSet());

    Set<String> usernames =
        params.getEvents().stream()
            .filter(Objects::nonNull)
            .map(Event::getAssignedUser)
            .filter(Objects::nonNull)
            .map(User::getUsername)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());

    List<org.hisp.dhis.user.User> users = userService.getUsersByUsernames(usernames);

    Set<org.hisp.dhis.user.User> validUsers =
        new HashSet<>(DetachUtils.detach(UserMapper.INSTANCE, users));
    Set<org.hisp.dhis.user.User> validUsersByUid =
        new HashSet<>(
            DetachUtils.detach(
                UserMapper.INSTANCE, manager.getByUid(org.hisp.dhis.user.User.class, userUids)));

    preheat.addUsers(validUsers);
    preheat.addUsers(validUsersByUid);
  }
}
