package org.hisp.dhis.user.job;

import static java.time.ZoneId.systemDefault;

import java.time.LocalDate;
import java.util.Date;

import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component( "disableInactiveUsersJob" )
public class DisableInactiveUsersJob extends AbstractJob
{

    private final UserService userService;

    @Override
    public JobType getJobType()
    {
        return JobType.DISABLE_INACTIVE_USERS;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        DisableInactiveUsersJobParameters parameters = (DisableInactiveUsersJobParameters) jobConfiguration
            .getJobParameters();
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusMonths( parameters.getInactiveMonths() );
        userService.disableUsersInactiveSince( Date.from( since.atStartOfDay( systemDefault() ).toInstant() ) );
    }
}
