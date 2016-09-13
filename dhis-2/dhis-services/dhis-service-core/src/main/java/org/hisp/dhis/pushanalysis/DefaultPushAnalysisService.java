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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.fileresource.*;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
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
import java.util.*;

/**
 * @author Stian Sandvold
 */
@Transactional
public class DefaultPushAnalysisService
    implements PushAnalysisService
{
    private static final Log log = LogFactory.getLog( DefaultPushAnalysisService.class );

    @Autowired
    private Notifier notifier;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private ExternalFileResourceService externalFileResourceService;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ReportTableService reportTableService;

    @Autowired
    private MapGenerationService mapGenerationService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private I18nManager i18nManager;

    @Resource( name = "emailMessageSender" )
    private MessageSender messageSender;

    private GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

    public void setPushAnalysisStore( GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore )
    {
        this.pushAnalysisStore = pushAnalysisStore;
    }

    //----------------------------------------------------------------------
    // PushAnalysisService implementation
    //----------------------------------------------------------------------

    @Override
    public PushAnalysis getByUid( String uid )
    {
        return pushAnalysisStore.getByUid( uid );
    }

    @Override
    public void runPushAnalysis( int id, TaskId taskId )
    {
        //----------------------------------------------------------------------
        // Set up
        //----------------------------------------------------------------------

        PushAnalysis pushAnalysis = pushAnalysisStore.get( id );
        Set<User> receivingUsers = new HashSet<>();
        notifier.clear( taskId );

        //----------------------------------------------------------------------
        // Pre-check
        //----------------------------------------------------------------------

        log( taskId, NotificationLevel.INFO, "Starting pre-check on PushAnalysis", false, null );

        if ( pushAnalysis == null )
        {
            log( taskId, NotificationLevel.ERROR,
                "PushAnalysis with id '" + id + "' was not found. Terminating PushAnalysis", true, null );
            return;
        }

        if ( pushAnalysis.getRecipientUserGroups().size() == 0 )
        {
            log( taskId, NotificationLevel.ERROR,
                "PushAnalysis with id '" + id + "' has no userGroups assigned. Terminating PushAnalysis.", true, null );
            return;
        }

        if ( pushAnalysis.getDashboard() == null )
        {
            log( taskId, NotificationLevel.ERROR,
                "PushAnalysis with id '" + id + "' has no dashboard assigned. Terminating PushAnalysis.", true, null );
            return;
        }

        if ( systemSettingManager.getInstanceBaseUrl() == null )
        {
            log( taskId, NotificationLevel.ERROR,
                "Missing system setting '" + SettingKey.INSTANCE_BASE_URL.getName() + "'. Terminating PushAnalysis.",
                true, null );
            return;
        }

        log( taskId, NotificationLevel.INFO, "pre-check completed successfully", false, null );

        //----------------------------------------------------------------------
        // Compose list of users that can receive PushAnalysis
        //----------------------------------------------------------------------

        log( taskId, NotificationLevel.INFO, "Composing list of receiving users", false, null );

        for ( UserGroup userGroup : pushAnalysis.getRecipientUserGroups() )
        {
            for ( User user : userGroup.getMembers() )
            {
                if ( !user.hasEmail() )
                {
                    log( taskId, NotificationLevel.WARN,
                        "Skipping user: User '" + user.getUsername() + "' is missing a valid email.", false, null );
                    continue;
                }

                receivingUsers.add( user );
            }
        }

        log( taskId, NotificationLevel.INFO, "List composed. " + receivingUsers.size() + " eligible users found.",
            false, null );

        //----------------------------------------------------------------------
        // Generating reports
        //----------------------------------------------------------------------

        log( taskId, NotificationLevel.INFO, "Generating and sending reports", false, null );

        for ( User user : receivingUsers )
        {
            try
            {
                String title = "Report: " + pushAnalysis.getName();
                String html = generateHtmlReport( pushAnalysis, user, taskId );

                // TODO: Better handling of messageStatus; Might require refactoring of EmailMessageSender
                @SuppressWarnings("unused")
                MessageResponseStatus status = messageSender
                    .sendMessage( title, html, "", null, Sets.newHashSet( user ), true );

            }
            catch ( Exception e )
            {
                log( taskId, NotificationLevel.ERROR,
                    "Could not create or send report for PushAnalysis '" + pushAnalysis.getName() + "' and User '" +
                        user.getUsername() + "': " + e.getMessage(), false, e );
            }
        }

        // Update lastRun date:
        pushAnalysis.setLastRun( new Date() );
        pushAnalysisStore.update( pushAnalysis );
    }

    @Override
    public void runAllPushAnalysis( TaskId taskId )
    {
        notifier.clear( taskId );

        List<PushAnalysis> list = pushAnalysisStore.getAll();

        log( taskId, NotificationLevel.INFO, "Found " + list.size() + " PushAnalysis", false, null);

        list.forEach( pushAnalysis -> {
            if ( pushAnalysis.getEnabled() )
            {
                log( taskId, NotificationLevel.INFO, "Starting PushAnalysis '" + pushAnalysis.getUid() + "'.", false,
                    null );
                runPushAnalysis( pushAnalysis.getId(), taskId );
            }
            else
            {
                log( taskId, NotificationLevel.INFO,
                    "PushAnalysis '" + pushAnalysis.getUid() + "' is disabled. Skipping.", false, null );
            }
        } );

        log( taskId, NotificationLevel.INFO, "Finished running all PushAnalysis.", true, null);

    }

    @Override
    public String generateHtmlReport( PushAnalysis pushAnalysis, User user, TaskId taskId )
        throws Exception
    {
        if ( taskId == null )
        {
            taskId = new TaskId( TaskCategory.PUSH_ANALYSIS, currentUserService.getCurrentUser() );
            notifier.clear( taskId );
        }

        user = user == null ? currentUserService.getCurrentUser() : user;
        log( taskId, NotificationLevel.INFO, "Generating PushAnalysis for user '" + user.getUsername() + "'.", false,
            null );

        //----------------------------------------------------------------------
        // Pre-process the dashboardItem and store them as Strings
        //----------------------------------------------------------------------

        HashMap<String, String> itemHtml = new HashMap<>();

        for ( DashboardItem item : pushAnalysis.getDashboard().getItems() )
        {
            itemHtml.put( item.getUid(), getItemHtml( item, user, taskId ) );
        }

        //----------------------------------------------------------------------
        // Set up template context, including pre-processed dashboard items
        //----------------------------------------------------------------------

        final VelocityContext context = new VelocityContext();

        context.put( "pushAnalysis", pushAnalysis );
        context.put( "itemHtml", itemHtml );

        //----------------------------------------------------------------------
        // Render template and return result after removing newline characters
        //----------------------------------------------------------------------

        StringWriter stringWriter = new StringWriter();

        new VelocityManager().getEngine().getTemplate( "push-analysis-main-html.vm" ).merge( context, stringWriter );

        log( taskId, NotificationLevel.INFO, "Finished generating PushAnalysis for user '" + user.getUsername() + "'.",
            false, null );

        return stringWriter.toString().replaceAll( "\\R", "" );

    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private String getItemHtml( DashboardItem item, User user, TaskId taskId )
        throws Exception
    {

        switch ( item.getType() )
        {
        case MAP:
            return generateMapHtml( item.getMap(), user );
        case CHART:
            return generateChartHtml( item.getChart(), user );
        case EVENT_CHART:
            // TODO: Add support for EventCharts
            return "";
        case REPORT_TABLE:
            return generateReportTableHtml( item.getReportTable(), user );
        case EVENT_REPORT:
            // TODO: Add support for EventReports
            return "";
        default:
            log( taskId, NotificationLevel.WARN,
                "Dashboard item of type '" + item.getType() + "' not supported. Skipping.", false, null );
            return "";
        }
    }

    /**
     * Returns an absolute URL to an image representing the map input
     *
     * @param map  map to render and upload
     * @param user user to generate chart for
     * @return absolute URL to uploaded image
     * @throws IOException
     */
    private String generateMapHtml( Map map, User user )
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage image = mapGenerationService.generateMapImageForUser( map, new Date(), null, 600, 600, user );

        ImageIO.write( image, "PNG", baos );

        return uploadImage( map.getUid(), baos.toByteArray() );
    }

    /**
     * Returns an absolute URL to an image representing the chart input
     *
     * @param chart chart to render and upload
     * @param user  user to generate chart for
     * @return absolute URL to uploaded image
     * @throws IOException
     */
    private String generateChartHtml( Chart chart, User user )
        throws IOException
    {
        JFreeChart jFreechart = chartService
            .getJFreeChart( chart, new Date(), null, i18nManager.getI18nFormat(), user );

        return uploadImage( chart.getUid(), ChartUtils.getChartAsPngByteArray( jFreechart, 600, 600 ) );
    }

    /**
     * Builds a HTML table representing the ReportTable input
     *
     * @param reportTable reportTable to generate HTML for
     * @param user        user to generate reportTable data for
     * @return a HTML representation of the reportTable
     * @throws Exception
     */
    private String generateReportTableHtml( ReportTable reportTable, User user )
        throws Exception
    {
        StringWriter stringWriter = new StringWriter();

        GridUtils.toHtmlInlineCss(
            reportTableService
                .getReportTableGridByUser( reportTable.getUid(), new Date(),
                    user.getOrganisationUnit().getUid(), user ),
            stringWriter
        );

        return stringWriter.toString().replaceAll( "\\R", "" );
    }

    /**
     * Uploads a byte array using FileResource and ExternalFileResource
     *
     * @param name  name of the file to be stored
     * @param bytes the byte array representing the file to be stored
     * @return url pointing to the uploaded resource
     * @throws IOException
     */
    private String uploadImage( String name, byte[] bytes )
        throws IOException
    {
        FileResource fileResource = new FileResource(
            name,
            MimeTypeUtils.IMAGE_PNG.toString(), // All files uploaded from PushAnalysis is PNG.
            bytes.length,
            ByteSource.wrap( bytes ).hash( Hashing.md5() ).toString(),
            FileResourceDomain.EXTERNAL // All files generated with PushAnalysis should belong to the EXTERNAL domain
        );

        fileResourceService.saveFileResource( fileResource, bytes );

        ExternalFileResource externalFileResource = new ExternalFileResource();

        externalFileResource.setFileResource( fileResource );
        externalFileResource.setExpires( null ); // TODO: Need system-setting or something for this

        String accessToken = externalFileResourceService.saveExternalFileResource( externalFileResource );

        return systemSettingManager.getInstanceBaseUrl() + "/api/externalFileResources/" + accessToken;

    }

    private void log( TaskId taskId, NotificationLevel notificationLevel, String message, boolean completed,
        Throwable exception )
    {
        notifier.notify( taskId, notificationLevel, message, completed );

        switch ( notificationLevel )
        {
        case INFO:
            log.info( message );
            break;
        case WARN:
            log.warn( message, exception );
            break;
        case ERROR:
            log.error( message, exception );
            break;
        }
    }
}
