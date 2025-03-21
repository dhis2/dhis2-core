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
package org.hisp.dhis.pushanalysis;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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
import lombok.RequiredArgsConstructor;
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
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.ChartUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
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
@Service
@Transactional
@RequiredArgsConstructor
public class DefaultPushAnalysisService implements PushAnalysisService {
  private static final Encoder encoder = new Encoder();

  private final SystemSettingsProvider settingsProvider;

  private final DhisConfigurationProvider dhisConfigurationProvider;

  private final ExternalFileResourceService externalFileResourceService;

  private final FileResourceService fileResourceService;

  private final UserService userService;

  private final MapGenerationService mapGenerationService;

  private final VisualizationGridService visualizationGridService;

  private final ChartService chartService;

  private final I18nManager i18nManager;

  private final MessageSender emailMessageSender;

  @Qualifier("org.hisp.dhis.pushanalysis.PushAnalysisStore")
  private final IdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

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

  private void runPushAnalysis(String uid, JobProgress progress) {
    // ----------------------------------------------------------------------
    // Pre-check
    // ----------------------------------------------------------------------
    PushAnalysis pushAnalysis = pushAnalysisStore.getByUid(uid);
    progress.startingStage(
        "Starting pre-check on PushAnalysis "
            + uid
            + ": "
            + ((pushAnalysis != null) ? pushAnalysis.getName() : ""));

    if (pushAnalysis == null) {
      progress.failedStage(
          "PushAnalysis with uid '{}' was not found. Terminating PushAnalysis.", uid);
      return;
    }
    if (pushAnalysis.getRecipientUserGroups().isEmpty()) {
      progress.failedStage(
          "PushAnalysis with uid '{}' has no userGroups assigned. Terminating PushAnalysis.", uid);
      return;
    }

    if (pushAnalysis.getDashboard() == null) {
      progress.failedStage(
          "PushAnalysis with uid '{}' has no dashboard assigned. Terminating PushAnalysis.", uid);
      return;
    }

    if (dhisConfigurationProvider.getServerBaseUrl() == null) {
      progress.failedStage(
          "Missing configuration '{}'. Terminating PushAnalysis.",
          ConfigurationKey.SERVER_BASE_URL.getKey());
      return;
    }

    progress.completedStage("Pre-check completed successfully");

    // ----------------------------------------------------------------------
    // Compose list of users that can receive PushAnalysis
    // ----------------------------------------------------------------------

    progress.startingStage("Composing list of receiving users");
    Set<User> receivingUsers = new HashSet<>();
    Set<User> skippedUsers = new HashSet<>();
    for (UserGroup userGroup : pushAnalysis.getRecipientUserGroups()) {
      for (User user : userGroup.getMembers()) {
        if (!user.hasEmail()) {
          skippedUsers.add(user);
        } else {
          receivingUsers.add(user);
        }
      }
    }
    progress.completedStage(
        "List composed. {} eligible users found. Skipping users without valid email: {}",
        receivingUsers.size(),
        skippedUsers.stream().map(User::getUsername).collect(joining(",")));

    // ----------------------------------------------------------------------
    // Generating reports
    // ----------------------------------------------------------------------
    String name = pushAnalysis.getName();
    progress.startingStage(
        "Generating and sending reports for PushAnalysis " + name,
        receivingUsers.size(),
        SKIP_ITEM_OUTLIER);
    progress.runStage(
        receivingUsers,
        user ->
            "Generating and sending PushAnalysis "
                + name
                + " for user '"
                + user.getUsername()
                + "'.",
        user -> {
          String title = pushAnalysis.getTitle();
          String html = "";
          try {
            html = generateHtmlReport(pushAnalysis, user);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
          // TODO: Better handling of messageStatus; Might require
          // refactoring of EmailMessageSender
          @SuppressWarnings("unused")
          Future<OutboundMessageResponse> status =
              emailMessageSender.sendMessageAsync(title, html, "", null, Set.of(user), true);
        });
  }

  @Override
  public void runPushAnalysis(List<String> uids, JobProgress progress) {
    uids.forEach(uid -> runPushAnalysis(uid, progress));
  }

  @Override
  public String generateHtmlReport(PushAnalysis pushAnalysis, User user) throws IOException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    user = user == null ? currentUser : user;

    // ----------------------------------------------------------------------
    // Pre-process the dashboardItem and store them as Strings
    // ----------------------------------------------------------------------

    HashMap<String, String> itemHtml = new HashMap<>();
    HashMap<String, String> itemLink = new HashMap<>();

    for (DashboardItem item : pushAnalysis.getDashboard().getItems()) {
      // Preventing NPE when DB data is not consistent.
      // In normal conditions all DashboardItem has a type.
      if (item.getType() != null) {
        itemHtml.put(item.getUid(), getItemHtml(item, user));
        itemLink.put(item.getUid(), getItemLink(item));
      }
    }

    DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
    itemHtml.put("date", dateFormat.format(Calendar.getInstance().getTime()));
    itemHtml.put("instanceBaseUrl", dhisConfigurationProvider.getServerBaseUrl());
    itemHtml.put("instanceName", settingsProvider.getCurrentSettings().getApplicationTitle());

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
   */
  private String getItemHtml(DashboardItem item, User user) throws IOException {
    switch (item.getType()) {
      case MAP:
        return generateMapHtml(item.getMap(), user);
      case VISUALIZATION:
        return generateVisualizationHtml(item.getVisualization(), user);
      default:
        // TODO: Add support for EventCharts
        // TODO: Add support for EventReports
        log.warn("Dashboard item of type '" + item.getType() + "' not supported. Skipping.");
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
  private String generateVisualizationHtml(Visualization visualization, User user)
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
  private String generateChartHtml(Visualization visualization, User user) throws IOException {
    JFreeChart jFreechart =
        chartService.getJFreeChart(
            new PlotData(visualization),
            new Date(),
            null,
            i18nManager.getI18nFormat(),
            user.getUsername());

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
  private String generateReportTableHtml(Visualization visualization, User user) {
    StringWriter stringWriter = new StringWriter();

    GridUtils.toHtmlInlineCss(
        visualizationGridService.getVisualizationGrid(
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

    String fileResourceUid = fileResourceService.asyncSaveFileResource(fileResource, bytes);

    externalFileResource.setFileResource(fileResourceService.getFileResource(fileResourceUid));

    return externalFileResourceService.saveExternalFileResource(externalFileResource);
  }
}
