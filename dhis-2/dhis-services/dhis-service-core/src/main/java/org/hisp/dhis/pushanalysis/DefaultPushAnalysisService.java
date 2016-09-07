package org.hisp.dhis.pushanalysis;

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

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import org.apache.velocity.VelocityContext;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventchart.EventChartService;
import org.hisp.dhis.fileresource.*;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.sms.MessageResponseStatus;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.ChartUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stian Sandvold
 */
@Transactional
public class DefaultPushAnalysisService
    implements PushAnalysisService
{

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ReportTableService reportTableService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private MapGenerationService mapGenerationService;

    @Autowired
    private EventChartService EventChartService;

    @Resource( name = "emailMessageSender" )
    private MessageSender messageSender;

    @Autowired
    private Notifier notifier;

    @Autowired
    private ExternalFileResourceService externalFileResourceService;

    @Autowired
    private FileResourceService fileResourceService;

    private GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

    public void setPushAnalysisStore( GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore )
    {
        this.pushAnalysisStore = pushAnalysisStore;
    }

    @Override
    public PushAnalysis getByUid( String uid )
    {
        return pushAnalysisStore.getByUid( uid );
    }

    @Override
    public boolean stopPushAnalysis( PushAnalysis pushAnalysis )
    {
        return false;
    }

    @Override
    public boolean startPushAnalysis( PushAnalysis pushAnalysis )
    {
        return false;
    }

    @Override
    public void runPushAnalysis( int id, TaskId taskId )
    {
        PushAnalysis pushAnalysis = pushAnalysisStore.get( id );
        notifier.clear( taskId );

        notifier
            .notify( taskId, "PushAnalysis " + pushAnalysis.getUid() + " (" + pushAnalysis.getName() + ") started" );

        Set<User> receivingUsers = new HashSet<>();

        for ( UserGroup userGroup : pushAnalysis.getReceivingUserGroups() )
        {
            receivingUsers.addAll( userGroup.getMembers() );
        }

        int missingEmail = 0;
        int renderFailed = 0;
        int emailFailed = 0;

        notifier.notify( taskId, "PushAnalysis " + pushAnalysis.getUid() + " found " + receivingUsers.size() +
            " users in target UserGroups" );
        for ( User user : receivingUsers )
        {
            if ( user.hasEmail() )
            {
                try
                {
                    StringWriter writer = new StringWriter();

                    render( pushAnalysis, user, writer );

                    MessageResponseStatus status = messageSender
                        .sendMessage( pushAnalysis.getName(), writer.toString(), "",
                            null, Sets.newHashSet( user ), true );

//                    if ( !status.isOk() )
//                        {
//                            emailFailed++;
//                            notifier.notify( taskId, NotificationLevel.WARN,
//                                "PushAnalysis " + pushAnalysis.getUid() + " failed to send email: " +
//                                status.getResponseMessage(), false );
//                    }

                }
                catch ( IOException e )
                {
                    renderFailed++;
                    e.printStackTrace();
                }
            }
            else
            {
                missingEmail++;
            }
        }

        notifier
            .notify( taskId, NotificationLevel.INFO, "PushAnalysis " + pushAnalysis.getUid() + " results: ",
                false );
        notifier.notify( taskId,
            "Emails sent: " + (receivingUsers.size() - missingEmail - renderFailed - emailFailed) + "/" +
                receivingUsers.size() );
        notifier.notify( taskId, "Missing emails: " + missingEmail );
        notifier.notify( taskId, "Reports failed: " + renderFailed );
        notifier.notify( taskId, "Email failed: " + emailFailed );
        notifier
            .notify( taskId, NotificationLevel.INFO, "PushAnalysis " + pushAnalysis.getUid() + " complete.",
                true );

    }

    @Override
    public void renderPushAnalysis( PushAnalysis pushAnalysis, User user, Writer writer )
        throws Exception
    {
        render( pushAnalysis, user, writer );
    }

    private void render( PushAnalysis pushAnalysis, User user, Writer writer )
        throws IOException
    {
        StringWriter stringWriter = new StringWriter();
        user = (user != null ? user : currentUserService.getCurrentUser());

        final VelocityContext context = new VelocityContext();

        context.put( "pushAnalysis", pushAnalysis );
        context.put( "user", user );
        context.put( "pushAnalysisService", this );
        context.put( "reportTableService", reportTableService );

        new VelocityManager().getEngine().getTemplate( "push-analysis-main-html.vm" ).merge( context, stringWriter );

        writer.write( stringWriter.toString().replaceAll( "\\R", "" ) );
    }

    // Used in vm templates (push-analysis-main-html.vm)
    public String getItemDataUrl( DashboardItem item, User user )
        throws IOException
    {
        // TEMP
        String BASE_URL = "http://localhost:8080";

        FileResource fileResource = createAndUpload( item, user );

        ExternalFileResource externalFileResource = new ExternalFileResource();

        externalFileResource.setFileResource( fileResource );
        externalFileResource.setExpires( null ); // TEMPORARY!! Need system-setting or something for this

        String accessToken = externalFileResourceService.saveExternalFileResource( externalFileResource );

        return BASE_URL + "/api/externalFileResources/" + accessToken;
    }

    private FileResource createAndUpload( DashboardItem item, User user )
        throws IOException
    {
        byte[] bytes = null;

        if ( item.getType() == DashboardItemType.CHART )
        {
            Chart chart = item.getChart();
            JFreeChart jFreechart = chartService
                .getJFreeChart( chart, new Date(), null, i18nManager.getI18nFormat(), user );
            bytes = ChartUtils.getChartAsPngByteArray( jFreechart, 600, 600 );
        }
        else if ( item.getType() == DashboardItemType.MAP )
        {
            Map map = item.getMap();

            BufferedImage image = mapGenerationService.generateMapImage( map, new Date(), null, 600, 600 );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageIO.write( image, "PNG", baos );

            bytes = baos.toByteArray();
        }

        String contentMd5 = ByteSource.wrap( bytes ).hash( Hashing.md5() ).toString();

        FileResource fileResource = new FileResource( item.getUid(), MimeTypeUtils.IMAGE_PNG.toString(), bytes.length,
            contentMd5, FileResourceDomain.EXTERNAL );

        fileResourceService.saveFileResource( fileResource, bytes );

        return fileResource;
    }

    // Used in vm templates (push-analysis-main-html.vm)
    public String getItemDataHtml( DashboardItem item, User user )
        throws Exception
    {

        if ( item.getType() == DashboardItemType.REPORT_TABLE )
        {
            StringWriter stringWriter = new StringWriter();

            GridUtils.toHtmlInlineCss(
                reportTableService
                    .getReportTableGridByUser( item.getReportTable().getUid(), new Date(),
                        user.getOrganisationUnit().getUid(), user ),
                stringWriter
            );

            return stringWriter.toString().replaceAll( "\\R", "" );
        }
        else
        {
            // TODO: Add Event_report tables as well.
            return "";
        }
    }
}