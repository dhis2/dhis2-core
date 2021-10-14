package org.hisp.dhis.webapi.controller.system;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.DataIntegrityController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SystemSoftwareUpdateNotifyController
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SchedulingManager schedulingManager;

    public static final String RESOURCE_PATH = "/checkSystemUpdate";

    @PostMapping( DataIntegrityController.RESOURCE_PATH )
    @ResponseBody
    public WebMessage runAsyncDataIntegrity()
    {
        JobConfiguration jobConfiguration = new JobConfiguration( "runCheckSystemUpdate",
            JobType.SYSTEM_SOFTWARE_UPDATE, null,
            true );
        jobConfiguration.setUserUid( currentUserService.getCurrentUser().getUid() );
        jobConfiguration.setAutoFields();

        schedulingManager.executeNow( jobConfiguration );

        return jobConfigurationReport( jobConfiguration );
    }
}
