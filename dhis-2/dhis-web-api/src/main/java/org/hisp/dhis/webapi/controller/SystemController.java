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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.CacheControl.noStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Data;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobKey;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.StyleManager;
import org.hisp.dhis.setting.StyleObject;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.hisp.dhis.webapi.webdomain.CodeList;
import org.hisp.dhis.webapi.webdomain.ObjectCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = Server.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/system")
public class SystemController {

  @Autowired private SystemService systemService;

  @Autowired private StyleManager styleManager;

  @Autowired private Notifier notifier;

  @Autowired private I18nManager i18nManager;

  @Autowired private StatisticsProvider statisticsProvider;

  @Autowired private FieldFilterService fieldFilterService;

  @Autowired private JobConfigurationService jobConfigurationService;

  private static final CsvFactory CSV_FACTORY = new CsvMapper().getFactory();

  // -------------------------------------------------------------------------
  // UID Generator
  // -------------------------------------------------------------------------

  @GetMapping(value = {"/uid", "/id"})
  public @ResponseBody CodeList getUid(
      @RequestParam(required = false, defaultValue = "1") Integer limit,
      HttpServletResponse response) {
    setNoStore(response);
    return generateCodeList(Math.min(limit, 10000), CodeGenerator::generateUid);
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/uid", "/id"},
      produces = "application/csv")
  public void getUidCsv(
      @RequestParam(required = false, defaultValue = "1") Integer limit,
      HttpServletResponse response)
      throws IOException {
    CodeList codeList = generateCodeList(Math.min(limit, 10000), CodeGenerator::generateUid);
    CsvSchema schema = CsvSchema.builder().addColumn("uid").setUseHeader(true).build();

    try (CsvGenerator csvGenerator = CSV_FACTORY.createGenerator(response.getOutputStream())) {
      csvGenerator.setSchema(schema);

      for (String code : codeList.getCodes()) {
        csvGenerator.writeStartObject();
        csvGenerator.writeStringField("uid", code);
        csvGenerator.writeEndObject();
      }

      csvGenerator.flush();
    }
  }

  @GetMapping(
      value = "/uuid",
      produces = {APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public @ResponseBody CodeList getUuid(
      @RequestParam(required = false, defaultValue = "1") Integer limit,
      HttpServletResponse response) {
    CodeList codeList =
        generateCodeList(Math.min(limit, 10000), () -> UUID.randomUUID().toString());
    setNoStore(response);

    return codeList;
  }

  // -------------------------------------------------------------------------
  // Tasks
  // -------------------------------------------------------------------------

  @Data
  public static class DeleteTasksParams {
    @OpenApi.Description("Delete job data but keep the most recently updated `maxCount` jobs")
    private Integer maxCount;

    @OpenApi.Description("Deletes job data but keep those younger than the `maxAge` in days")
    private Integer maxAge;
  }

  @DeleteMapping("/tasks")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotifications(DeleteTasksParams params) {
    Integer maxCount = params.getMaxCount();
    Integer maxAge = params.getMaxAge();
    if (maxAge != null) notifier.capMaxAge(maxAge);
    if (maxCount != null) notifier.capMaxCount(maxCount);
    if (maxCount == null && maxAge == null) notifier.clear();
  }

  @DeleteMapping("/tasks/{jobType}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotificationsByJobType(
      @PathVariable("jobType") JobType jobType, DeleteTasksParams params) {
    Integer maxAge = params.getMaxAge();
    Integer maxCount = params.getMaxCount();
    if (maxAge != null) notifier.capMaxAge(maxAge, jobType);
    if (maxCount != null) notifier.capMaxCount(maxCount, jobType);
    if (maxCount == null && maxAge == null) notifier.clear(jobType);
  }

  @DeleteMapping("/tasks/{jobType}/{jobId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNotificationsByJobId(
      @PathVariable("jobType") JobType jobType,
      @PathVariable("jobId") @OpenApi.Param(value = {UID.class, JobConfiguration.class})
          UID jobId) {
    notifier.clear(jobType, jobId);
  }

  @GetMapping(value = "/tasks", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<JobType, Map<String, Deque<Notification>>>> getTasksAllJobTypes(
      @RequestParam(required = false) Boolean gist) {
    return ResponseEntity.ok().cacheControl(noStore()).body(notifier.getNotifications(gist));
  }

  @GetMapping(value = "/tasks/{jobType}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Deque<Notification>>> getTasksByJobType(
      @PathVariable("jobType") JobType jobType, @RequestParam(required = false) Boolean gist) {
    Map<String, Deque<Notification>> notifications =
        jobType == null ? Map.of() : notifier.getNotificationsByJobType(jobType, gist);

    return ResponseEntity.ok().cacheControl(noStore()).body(notifications);
  }

  @GetMapping(value = "/tasks/{jobType}/{jobId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Collection<Notification>> getTaskJsonByUid(
      @PathVariable("jobType") JobType jobType,
      @PathVariable("jobId") @OpenApi.Param(value = {UID.class, JobConfiguration.class})
          UID jobId) {
    Deque<Notification> notifications = notifier.getNotificationsByJobId(jobType, jobId.getValue());

    if (notifications.isEmpty()) return ResponseEntity.ok().cacheControl(noStore()).body(List.of());

    if (!notifications.getFirst().isCompleted()) {
      JobConfiguration job = jobConfigurationService.getJobConfigurationByUid(jobId.getValue());
      if (job == null || job.getJobStatus() != JobStatus.RUNNING) {
        notifier.clear(new JobKey(jobId, jobType));
        Notification notification = notifications.getFirst();
        notification.setCompleted(true);
        return ResponseEntity.ok().cacheControl(noStore()).body(List.of(notification));
      }
    }
    return ResponseEntity.ok().cacheControl(noStore()).body(notifications);
  }

  // -------------------------------------------------------------------------
  // Tasks summary
  // -------------------------------------------------------------------------

  @GetMapping(value = "/taskSummaries/{jobType}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, JsonValue>> getTaskSummaryExtendedJson(
      @PathVariable("jobType") JobType jobType) {
    Map<String, JsonValue> summary = notifier.getJobSummariesForJobType(jobType);
    if (summary != null) return ResponseEntity.ok().cacheControl(noStore()).body(summary);
    return ResponseEntity.ok().cacheControl(noStore()).build();
  }

  @GetMapping(value = "/taskSummaries/{jobType}/{jobId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonValue> getTaskSummaryJson(
      @PathVariable("jobType") JobType jobType,
      @PathVariable("jobId") @OpenApi.Param(value = {UID.class, JobConfiguration.class})
          UID jobId) {
    JsonValue summary = notifier.getJobSummaryByJobId(jobType, jobId.getValue());
    if (summary != null) return ResponseEntity.ok().cacheControl(noStore()).body(summary);
    return ResponseEntity.ok().cacheControl(noStore()).build();
  }

  // -------------------------------------------------------------------------
  // Various
  // -------------------------------------------------------------------------

  @OpenApi.Response(SystemInfo.class)
  @GetMapping(
      value = "/info",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody ResponseEntity<ObjectNode> getSystemInfo(
      @RequestParam(defaultValue = "*") List<String> fields,
      HttpServletRequest request,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser) {
    SystemInfo info =
        systemService.getSystemInfo().toBuilder()
            .contextPath(HttpServletRequestPaths.getContextPath(request))
            .userAgent(request.getHeader(ContextUtils.HEADER_USER_AGENT))
            .build();

    if (!CurrentUserUtil.getCurrentUserDetails().isSuper()) {
      info = info.withoutSensitiveInfo();
    }

    setNoStore(response);

    FieldFilterParams<SystemInfo> params = FieldFilterParams.of(info, fields);
    params.setUser(currentUser);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(params);

    return ResponseEntity.ok(objectNodes.get(0));
  }

  @GetMapping(value = "/objectCounts")
  public @ResponseBody ObjectCount getObjectCounts() {
    return new ObjectCount(statisticsProvider.getObjectCounts());
  }

  @GetMapping("/ping")
  @ResponseStatus(HttpStatus.OK)
  public @ResponseBody String ping(HttpServletResponse response) {
    setNoStore(response);

    return "pong";
  }

  @GetMapping(value = "/flags", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<StyleObject> getFlags() {
    return getFlagObjects();
  }

  @GetMapping(value = "/styles", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<StyleObject> getStyles() {
    return styleManager.getStyles();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private List<StyleObject> getFlagObjects() {
    I18n i18n = i18nManager.getI18n();
    return FLAGS.stream()
        .map(flag -> new StyleObject(i18n.getString(flag), flag, (flag + ".png")))
        .collect(Collectors.toList());
  }

  private CodeList generateCodeList(Integer limit, Supplier<String> codeSupplier) {
    CodeList codeList = new CodeList();

    for (int i = 0; i < limit; i++) {
      codeList.getCodes().add(codeSupplier.get());
    }

    return codeList;
  }

  private static final List<String> FLAGS =
      List.of(
              "afghanistan",
              "africare",
              "akros",
              "aland_islands",
              "albania",
              "algeria",
              "american_samoa",
              "andorra",
              "angola",
              "anguilla",
              "antarctica",
              "antigua_and_barbuda",
              "argentina",
              "armenia",
              "aruba",
              "australia",
              "austria",
              "azerbaijan",
              "bahamas",
              "bahrain",
              "bangladesh",
              "barbados",
              "belarus",
              "belgium",
              "belize",
              "benin",
              "bermuda",
              "bhutan",
              "bolivia",
              "bosnia_and_herzegovina",
              "botswana",
              "bouvet_island",
              "brazil",
              "british_indian_ocean_territory",
              "british_virgin_islands",
              "brunei",
              "bulgaria",
              "burkina_faso",
              "burkina_faso_coat_of_arms",
              "burundi",
              "cambodia",
              "cameroon",
              "canada",
              "cape_verde",
              "caribbean_netherlands",
              "cayman_islands",
              "central_african_republic",
              "chad",
              "chile",
              "china",
              "christmas_island",
              "cidrz",
              "cocos_keeling_islands",
              "colombia",
              "comoros",
              "congo_brazzaville",
              "congo_kinshasa",
              "cook_islands",
              "cordaid",
              "costa_rica",
              "cote_d_ivoire_ivory_coast",
              "croatia",
              "cuba",
              "curacao",
              "cyprus",
              "czechia",
              "demoland",
              "denmark",
              "djibouti",
              "dominica",
              "dominican_republic",
              "dr_congo",
              "ecowas",
              "ecuador",
              "east_africa_community",
              "egypt",
              "el_salvador",
              "engender_health",
              "england",
              "eritrea",
              "estonia",
              "eswatini_swaziland",
              "ethiopia",
              "equatorial_guinea",
              "european_union",
              "falkland_islands",
              "faroe_islands",
              "fhi360",
              "fiji",
              "finland",
              "forut",
              "france",
              "french_guiana",
              "french_polynesia",
              "french_southern_and_antarctic_lands",
              "gabon",
              "gambia",
              "georgia",
              "germany",
              "ghana",
              "gibraltar",
              "global_fund",
              "greece",
              "greenland",
              "grenada",
              "guadeloupe",
              "guam",
              "guatemala",
              "guernsey",
              "guinea",
              "guinea_bissau",
              "guyana",
              "haiti",
              "heard_island_and_mcdonald_islands",
              "honduras",
              "hong_kong",
              "hungary",
              "icap",
              "iceland",
              "ippf",
              "ima",
              "india",
              "indonesia",
              "irc",
              "iran",
              "iraq",
              "ireland",
              "isle_of_man",
              "israel",
              "italy",
              "ivory_coast",
              "jamaica",
              "japan",
              "jersey",
              "jhpiego",
              "jordan",
              "kazakhstan",
              "kenya",
              "kiribati",
              "kosovo",
              "kurdistan",
              "kuwait",
              "kyrgyzstan",
              "laos",
              "latvia",
              "lebanon",
              "lesotho",
              "liberia",
              "libya",
              "liechtenstein",
              "lithuania",
              "luxembourg",
              "macau",
              "madagascar",
              "malawi",
              "malaysia",
              "malta",
              "marshall_islands",
              "martinique",
              "mauritania",
              "mauritius",
              "maldives",
              "mayotte",
              "mexico",
              "micronesia",
              "moldova",
              "monaco",
              "mongolia",
              "montenegro",
              "montserrat",
              "morocco",
              "mozambique",
              "myanmar",
              "mali",
              "mhrp",
              "msf",
              "msh",
              "msh_white",
              "msi",
              "namibia",
              "nauru",
              "netherlands",
              "new_caledonia",
              "new_zealand",
              "nicaragua",
              "nepal",
              "niger",
              "nigeria",
              "niue",
              "norfolk_island",
              "north_korea",
              "north_macedonia",
              "northern_ireland",
              "northern_mariana_islands",
              "norway",
              "oman",
              "pakistan",
              "palau",
              "palestine",
              "palladium",
              "panama",
              "papua_new_guinea",
              "pepfar",
              "paraguay",
              "pathfinder",
              "philippines",
              "pitcairn_islands",
              "planned_parenthood",
              "peru",
              "poland",
              "portugal",
              "psi",
              "puerto_rico",
              "puntland",
              "qatar",
              "republic_of_the_congo",
              "reunion",
              "romania",
              "russia",
              "rwanda",
              "saint_barthelemy",
              "saint_helena_ascension_and_tristan_da_cunha",
              "saint_kitts_and_nevis",
              "saint_lucia",
              "saint_martin",
              "saint_pierre_and_miquelon",
              "saint_vincent_and_the_grenadines",
              "samoa",
              "san_marino",
              "sao_tome_and_principe",
              "saudi_arabia",
              "save_the_children",
              "scotland",
              "senegal",
              "serbia",
              "seychelles",
              "sierra_leone",
              "sierra_leone_coat_of_arms",
              "singapore",
              "sint_maarten",
              "slovakia",
              "slovenia",
              "solomon_islands",
              "somalia",
              "somaliland",
              "south_africa",
              "south_africa_department_of_health",
              "south_georgia",
              "south_korea",
              "south_sudan",
              "spain",
              "sri_lanka",
              "sudan",
              "suriname",
              "svalbard_and_jan_mayen",
              "swaziland",
              "sweden",
              "switzerland",
              "syria",
              "taiwan",
              "tajikistan",
              "tanzania",
              "thailand",
              "timor_leste",
              "republic_of_trinidad_and_tobago",
              "togo",
              "tokelau",
              "tonga",
              "trinidad_and_tobago",
              "tunisia",
              "turkey",
              "turkmenistan",
              "turks_and_caicos_islands",
              "tuvalu",
              "uganda",
              "ukraine",
              "united_arab_emirates",
              "united_kingdom",
              "united_nations",
              "united_states",
              "united_states_minor_outlying_islands",
              "united_states_virgin_islands",
              "uruguay",
              "usaid",
              "uzbekistan",
              "vatican_city_holy_see",
              "venezuela",
              "vietnam",
              "vanuatu",
              "wales",
              "wallis_and_futuna",
              "western_sahara",
              "yemen",
              "zambia",
              "zanzibar",
              "zimbabwe",
              "who")
          .stream()
          .sorted()
          .toList();
}
