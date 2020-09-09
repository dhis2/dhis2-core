package org.hisp.dhis.dxf2.events.importer;

import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
@Component
public class EventImporterUserService
{

    @Delegate
    private final CurrentUserService currentUserService;

    private final UsernameSupplier usernameSupplier;

    public String getAuditUsername()
    {
        return usernameSupplier.get();
    }
}
