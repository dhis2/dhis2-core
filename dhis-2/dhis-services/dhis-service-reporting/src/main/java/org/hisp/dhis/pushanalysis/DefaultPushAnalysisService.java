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
package org.hisp.dhis.pushanalysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.Encoder;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.ExternalFileResource;
import org.hisp.dhis.fileresource.ExternalFileResourceService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapgeneration.MapUtils;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.ChartUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationGridService;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Stian Sandvold
 */
@Slf4j
@Service("org.hisp.dhis.pushanalysis.PushAnalysisService")
@Transactional
public class DefaultPushAnalysisService implements PushAnalysisService {
  private static final Encoder encoder = new Encoder();

  private final Notifier notifier;

  private final SystemSettingManager systemSettingManager;

  private final DhisConfigurationProvider dhisConfigurationProvider;

  private final ExternalFileResourceService externalFileResourceService;

  private final FileResourceService fileResourceService;

  private final CurrentUserService currentUserService;

  private final MapGenerationService mapGenerationService;

  private final VisualizationGridService visualizationGridService;

  private final ChartService chartService;

  private final I18nManager i18nManager;

  private final MessageSender messageSender;

  private final IdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

  public DefaultPushAnalysisService(
      Notifier notifier,
      SystemSettingManager systemSettingManager,
      DhisConfigurationProvider dhisConfigurationProvider,
      ExternalFileResourceService externalFileResourceService,
      FileResourceService fileResourceService,
      CurrentUserService currentUserService,
      MapGenerationService mapGenerationService,
      VisualizationGridService visualizationGridService,
      ChartService chartService,
      I18nManager i18nManager,
      @Qualifier("emailMessageSender") MessageSender messageSender,
      @Qualifier("org.hisp.dhis.pushanalysis.PushAnalysisStore")
          IdentifiableObjectStore<PushAnalysis> pushAnalysisStore) {
    checkNotNull(notifier);
    checkNotNull(systemSettingManager);
    checkNotNull(dhisConfigurationProvider);
    checkNotNull(externalFileResourceService);
    checkNotNull(fileResourceService);
    checkNotNull(currentUserService);
    checkNotNull(mapGenerationService);
    checkNotNull(visualizationGridService);
    checkNotNull(chartService);
    checkNotNull(i18nManager);
    checkNotNull(messageSender);
    checkNotNull(pushAnalysisStore);

    this.notifier = notifier;
    this.systemSettingManager = systemSettingManager;
    this.dhisConfigurationProvider = dhisConfigurationProvider;
    this.externalFileResourceService = externalFileResourceService;
    this.fileResourceService = fileResourceService;
    this.currentUserService = currentUserService;
    this.mapGenerationService = mapGenerationService;
    this.visualizationGridService = visualizationGridService;
    this.chartService = chartService;
    this.i18nManager = i18nManager;
    this.messageSender = messageSender;
    this.pushAnalysisStore = pushAnalysisStore;
  }

  // ----------------------------------------------------------------------
  // PushAnalysisService implementation
  // ----------------------------------------------------------------------

  @Override
  public PushAnalysis getByUid(String uid) {
    return pushAnalysisStore.getByUid(uid);
  }

  @Override
  public List<PushAnalysis> getAll() {
    return pushAnalysisStore.getAll();
  }

  @Override
  public void runPushAnalysis(String uid, JobConfiguration jobId) {
    // ----------------------------------------------------------------------
    // Set up
    // ----------------------------------------------------------------------

    PushAnalysis pushAnalysis = pushAnalysisStore.getByUid(uid);
    Set<User> receivingUsers = new HashSet<>();
    notifier.clear(jobId);

    // ----------------------------------------------------------------------
    // Pre-check
    // ----------------------------------------------------------------------

    log(jobId, NotificationLevel.INFO, "Starting pre-check on PushAnalysis", false, null);

    if (pushAnalysis == null) {
      log(
          jobId,
          NotificationLevel.ERROR,
          "PushAnalysis with uid '" + uid + "' was not found. Terminating PushAnalysis",
          true,
          null);
      return;
    }

    if (pushAnalysis.getRecipientUserGroups().size() == 0) {
      log(
          jobId,
          NotificationLevel.ERROR,
          "PushAnalysis with uid '"
              + uid
              + "' has no userGroups assigned. Terminating PushAnalysis.",
          true,
          null);
      return;
    }

    if (pushAnalysis.getDashboard() == null) {
      log(
          jobId,
          NotificationLevel.ERROR,
          "PushAnalysis with uid '"
              + uid
              + "' has no dashboard assigned. Terminating PushAnalysis.",
          true,
          null);
      return;
    }

    if (dhisConfigurationProvider.getServerBaseUrl() == null) {
      log(
          jobId,
          NotificationLevel.ERROR,
          "Missing configuration '"
              + ConfigurationKey.SERVER_BASE_URL.getKey()
              + "'. Terminating PushAnalysis.",
          true,
          null);
      return;
    }

    log(jobId, NotificationLevel.INFO, "pre-check completed successfully", false, null);

    // ----------------------------------------------------------------------
    // Compose list of users that can receive PushAnalysis
    // ----------------------------------------------------------------------

    log(jobId, NotificationLevel.INFO, "Composing list of receiving users", false, null);

    for (UserGroup userGroup : pushAnalysis.getRecipientUserGroups()) {
      for (User user : userGroup.getMembers()) {
        if (!user.hasEmail()) {
          log(
              jobId,
              NotificationLevel.WARN,
              "Skipping user: User '" + user.getUsername() + "' is missing a valid email.",
              false,
              null);
          continue;
        }

        receivingUsers.add(user);
      }
    }

    log(
        jobId,
        NotificationLevel.INFO,
        "List composed. " + receivingUsers.size() + " eligible users found.",
        false,
        null);

    // ----------------------------------------------------------------------
    // Generating reports
    // ----------------------------------------------------------------------

    log(jobId, NotificationLevel.INFO, "Generating and sending reports", false, null);

    for (User user : receivingUsers) {
      try {
        String title = pushAnalysis.getTitle();
        String html = generateHtmlReport(pushAnalysis, user, jobId);

        // TODO: Better handling of messageStatus; Might require
        // refactoring of EmailMessageSender
        @SuppressWarnings("unused")
        Future<OutboundMessageResponse> status =
            messageSender.sendMessageAsync(title, html, "", null, Sets.newHashSet(user), true);

      } catch (Exception e) {
        log(
            jobId,
            NotificationLevel.ERROR,
            "Could not create or send report for PushAnalysis '"
                + pushAnalysis.getName()
                + "' and User '"
                + user.getUsername()
                + "': "
                + e.getMessage(),
            false,
            e);
      }
    }
  }

  @Override
  public void runPushAnalysis(List<String> uids, JobConfiguration jobId) {
    uids.forEach(uid -> runPushAnalysis(uid, jobId));
  }

  @Override
  public String generateHtmlReport(PushAnalysis pushAnalysis, User user, JobConfiguration jobId)
      throws IOException {
    if (jobId == null) {
      jobId =
          new JobConfiguration(
              "inMemoryGenerateHtmlReport",
              JobType.PUSH_ANALYSIS,
              currentUserService.getCurrentUser().getUid(),
              true);
      notifier.clear(jobId);
    }

    user = user == null ? currentUserService.getCurrentUser() : user;
    log(
        jobId,
        NotificationLevel.INFO,
        "Generating PushAnalysis for user '" + user.getUsername() + "'.",
        false,
        null);

    // ----------------------------------------------------------------------
    // Pre-process the dashboardItem and store them as Strings
    // ----------------------------------------------------------------------

    HashMap<String, String> itemHtml = new HashMap<>();
    HashMap<String, String> itemLink = new HashMap<>();

    for (DashboardItem item : pushAnalysis.getDashboard().getItems()) {
      // Preventing NPE when DB data is not consistent.
      // In normal conditions all DashboardItem has a type.
      if (item.getType() != null) {
        itemHtml.put(item.getUid(), getItemHtml(item, user, jobId));
        itemLink.put(item.getUid(), getItemLink(item));
      }
    }

    DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    itemHtml.put("date", dateFormat.format(Calendar.getInstance().getTime()));
    itemHtml.put("instanceBaseUrl", dhisConfigurationProvider.getServerBaseUrl());
    itemHtml.put(
        "instanceName", systemSettingManager.getStringSetting(SettingKey.APPLICATION_TITLE));

    // ----------------------------------------------------------------------
    // Set up template context, including pre-processed dashboard items
    // ----------------------------------------------------------------------

    final VelocityContext context = new VelocityContext();

    context.put("pushAnalysis", pushAnalysis);
    context.put("itemHtml", itemHtml);
    context.put("itemLink", itemLink);
    context.put("encoder", encoder);

    // ----------------------------------------------------------------------
    // Render template and return result after removing newline characters
    // ----------------------------------------------------------------------

    StringWriter stringWriter = new StringWriter();

    new VelocityManager()
        .getEngine()
        .getTemplate("push-analysis-main-html.vm")
        .merge(context, stringWriter);

    log(
        jobId,
        NotificationLevel.INFO,
        "Finished generating PushAnalysis for user '" + user.getUsername() + "'.",
        false,
        null);

    return stringWriter.toString().replaceAll("\\R", "");
  }

  // --------------------------------------------------------------------------
  // Supportive methods
  // --------------------------------------------------------------------------

  /**
   * Finds the dashboardItem's type and calls the associated method for generating the resource
   * (either URL or HTML)
   *
   * @param item to generate resource
   * @param user to generate for
   * @param jobId for logging
   */
  private String getItemHtml(DashboardItem item, User user, JobConfiguration jobId)
      throws IOException {
    switch (item.getType()) {
      case MAP:
        return generateMapHtml(item.getMap(), user);
      case VISUALIZATION:
        return generateVisualizationHtml(item.getVisualization(), user);
      case EVENT_CHART:
        // TODO: Add support for EventCharts
        return "";
      case EVENT_REPORT:
        // TODO: Add support for EventReports
        return "";
      default:
        log(
            jobId,
            NotificationLevel.WARN,
            "Dashboard item of type '" + item.getType() + "' not supported. Skipping.",
            false,
            null);
        return "";
    }
  }

  private String getItemLink(DashboardItem item) {
    String result = dhisConfigurationProvider.getServerBaseUrl();

    switch (item.getType()) {
      case MAP:
        result += "/dhis-web-maps/index.html?id=" + item.getMap().getUid();
        break;
      case VISUALIZATION:
        result += "/dhis-web-data-visualizer/index.html?id=" + item.getVisualization().getUid();
        break;
      default:
        break;
    }

    return result;
  }

  /**
   * Returns an absolute URL to an image representing the map input
   *
   * @param map map to render and upload
   * @param user user to generate chart for
   * @return absolute URL to uploaded image
   */
  private String generateMapHtml(Map map, User user) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    BufferedImage image =
        mapGenerationService.generateMapImageForUser(map, new Date(), null, 578, 440, user);

    if (image == null) {
      image = MapUtils.createErrorImage("No data");
    }

    ImageIO.write(image, "PNG", baos);

    return uploadImage(map.getUid(), baos.toByteArray());
  }

  /**
   * Returns an absolute URL to an image representing the given Visualization.
   *
   * @param visualization the visualization to be rendered and uploaded.
   * @param user the user generate the Visualization.
   * @return absolute URL to the uploaded image.
   */
  private String generateVisualizationHtml(final Visualization visualization, final User user)
      throws IOException {
    switch (visualization.getType()) {
      case PIVOT_TABLE:
        return generateReportTableHtml(visualization, user);
      default:
        return generateChartHtml(visualization, user);
    }
  }

  /**
   * Returns an absolute URL to an image representing the chart input
   *
   * @param visualization chart to render and upload
   * @param user user to generate chart for
   * @return absolute URL to uploaded image
   */
  private String generateChartHtml(final Visualization visualization, User user)
      throws IOException {
    JFreeChart jFreechart =
        chartService.getJFreeChart(
            new PlotData(visualization), new Date(), null, i18nManager.getI18nFormat(), user);

    return uploadImage(
        visualization.getUid(), ChartUtils.getChartAsPngByteArray(jFreechart, 578, 440));
  }

  /**
   * Builds a HTML table representing a Pivot table.
   *
   * @param visualization the input Visualization to generate the HTML from.
   * @param user user generating the Pivot.
   * @return a HTML representation of the Pivot table.
   */
  private String generateReportTableHtml(final Visualization visualization, User user) {
    StringWriter stringWriter = new StringWriter();

    GridUtils.toHtmlInlineCss(
        visualizationGridService.getVisualizationGridByUser(
            visualization.getUid(), new Date(), user.getOrganisationUnit().getUid(), user),
        stringWriter);

    return stringWriter.toString().replaceAll("\\R", "");
  }

  /**
   * Uploads a byte array using FileResource and ExternalFileResource
   *
   * @param name name of the file to be stored
   * @param bytes the byte array representing the file to be stored
   * @return url pointing to the uploaded resource
   */
  private String uploadImage(String name, byte[] bytes) throws IOException {
    FileResource fileResource =
        new FileResource(
            name,
            MimeTypeUtils.IMAGE_PNG.toString(), // All files uploaded from
            // PushAnalysis is PNG.
            bytes.length,
            ByteSource.wrap(bytes).hash(Hashing.md5()).toString(),
            FileResourceDomain.PUSH_ANALYSIS);

    String accessToken = saveFileResource(fileResource, bytes);

    return dhisConfigurationProvider.getServerBaseUrl()
        + "/api/externalFileResources/"
        + accessToken;
  }

  /**
   * Helper method for logging both for custom logger and for notifier.
   *
   * @param jobId associated with the task running (for notifier)
   * @param notificationLevel The level this message should be logged
   * @param message message to be logged
   * @param completed a flag indicating the task is completed (notifier)
   * @param exception exception if one exists (logger)
   */
  private void log(
      JobConfiguration jobId,
      NotificationLevel notificationLevel,
      String message,
      boolean completed,
      Throwable exception) {
    notifier.notify(jobId, notificationLevel, message, completed);

    switch (notificationLevel) {
      case DEBUG:
        log.debug(message);
      case INFO:
        log.info(message);
        break;
      case WARN:
        log.warn(message, exception);
        break;
      case ERROR:
        log.error(message, exception);
        break;
      default:
        break;
    }
  }

  /**
   * Helper method for asynchronous file resource saving. Done to force a new session for each file
   * resource. Adding all the file resources in the same session caused problems with the upload
   * callback.
   *
   * @param fileResource file resource to save
   * @param bytes file data
   * @return access token of the external file resource
   */
  private String saveFileResource(FileResource fileResource, byte[] bytes) {
    ExternalFileResource externalFileResource = new ExternalFileResource();

    externalFileResource.setExpires(null);

    fileResource.setAssigned(true);

    String fileResourceUid = fileResourceService.saveFileResource(fileResource, bytes);

    externalFileResource.setFileResource(fileResourceService.getFileResource(fileResourceUid));

    return externalFileResourceService.saveExternalFileResource(externalFileResource);
  }
}
