package org.hisp.dhis.webapi.controller.event;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.completeness.DataSetCompletenessService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.notification.DataSetNotificationRecipient;
import org.hisp.dhis.dataset.notification.DataSetNotificationTemplate;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.DataSetNotificationTemplateSchemaDescriptor;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by zubair on 02.07.17.
 */
@Controller
@RequestMapping( value = DataSetNotificationTemplateSchemaDescriptor.API_ENDPOINT )
@ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataSetNotificationTemplateController extends
    AbstractCrudController<DataSetNotificationTemplate>
{
    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataSetCompletenessService dataSetCompletenessService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private RenderService renderService;

    @PreAuthorize( "hasRole('ALL')" )
    @RequestMapping( value = "/get1", method = RequestMethod.GET, produces = "application/json" )
    public void getDataSetNotificationTemplate(HttpServletRequest request, HttpServletResponse response )
            throws IOException
    {

        DataSet dataSet = new DataSet();
        dataSet.setAutoFields();



        DataSetNotificationTemplate template = new DataSetNotificationTemplate();
        template.setDeliveryChannels( Sets.newHashSet(DeliveryChannel.SMS ) );
        template.setMessageTemplate( " message body" );
        template.setSubjectTemplate( "subject body" );
        template.setNotificationTrigger( NotificationTrigger.COMPLETION );
        template.setNotificationRecipient( DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT );
        template.setRecipientUserGroup( userGroupService.getAllUserGroups().get( 1 ) );
        template.setDataSets( Sets.newHashSet( dataSet ) );

        renderService.toJson( response.getOutputStream(), template );


    }
}
