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
package org.hisp.dhis.webapi.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags("system")
@Controller
@RequestMapping("/configuration")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ConfigurationController {
  @Autowired private ConfigurationService configurationService;

  @Autowired private DhisConfigurationProvider config;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Autowired private PeriodService periodService;

  @Autowired private RenderService renderService;

  @Autowired private SystemSettingManager systemSettingManager;

  @Autowired private AppManager appManager;

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @GetMapping
  public @ResponseBody Configuration getConfiguration(Model model, HttpServletRequest request) {
    return configurationService.getConfiguration();
  }

  @ResponseStatus(value = HttpStatus.OK)
  @GetMapping("/systemId")
  public @ResponseBody String getSystemId(Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getSystemId();
  }

  @PreAuthorize("hasRole('ALL')")
  @PostMapping("/systemId")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void setSystemId(@RequestBody(required = false) String systemId) {
    systemId = ObjectUtils.firstNonNull(systemId, UUID.randomUUID().toString());

    Configuration configuration = configurationService.getConfiguration();
    configuration.setSystemId(systemId);
    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/feedbackRecipients")
  public @ResponseBody UserGroup getFeedbackRecipients(Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getFeedbackRecipients();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/feedbackRecipients")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setFeedbackRecipients(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    UserGroup group = identifiableObjectManager.get(UserGroup.class, uid);

    if (group == null) {
      throw new NotFoundException(UserGroup.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setFeedbackRecipients(group);

    configurationService.setConfiguration(configuration);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @DeleteMapping("/feedbackRecipients")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeFeedbackRecipients() {
    Configuration configuration = configurationService.getConfiguration();

    configuration.setFeedbackRecipients(null);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/systemUpdateNotificationRecipients")
  public @ResponseBody UserGroup getSystemUpdateNotificationRecipients(
      Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getSystemUpdateNotificationRecipients();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/systemUpdateNotificationRecipients")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setSystemUpdateNotificationRecipients(@RequestBody String uid)
      throws NotFoundException {
    uid = trim(uid);

    UserGroup group = identifiableObjectManager.get(UserGroup.class, uid);

    if (group == null) {
      throw new NotFoundException(UserGroup.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setSystemUpdateNotificationRecipients(group);

    configurationService.setConfiguration(configuration);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @DeleteMapping("/systemUpdateNotificationRecipients")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeSystemUpdateNotificationRecipients() {
    Configuration configuration = configurationService.getConfiguration();

    configuration.setSystemUpdateNotificationRecipients(null);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/offlineOrganisationUnitLevel")
  public @ResponseBody OrganisationUnitLevel getOfflineOrganisationUnitLevel(
      Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getOfflineOrganisationUnitLevel();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/offlineOrganisationUnitLevel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setOfflineOrganisationUnitLevel(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    OrganisationUnitLevel organisationUnitLevel =
        identifiableObjectManager.get(OrganisationUnitLevel.class, uid);

    if (organisationUnitLevel == null) {
      throw new NotFoundException(OrganisationUnitLevel.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setOfflineOrganisationUnitLevel(organisationUnitLevel);

    configurationService.setConfiguration(configuration);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @DeleteMapping("/offlineOrganisationUnitLevel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeOfflineOrganisationUnitLevel() {
    Configuration configuration = configurationService.getConfiguration();

    configuration.setOfflineOrganisationUnitLevel(null);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/infrastructuralIndicators")
  public @ResponseBody IndicatorGroup getInfrastructuralIndicators(
      Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getInfrastructuralIndicators();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/infrastructuralIndicators")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setInfrastructuralIndicators(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    IndicatorGroup group = identifiableObjectManager.get(IndicatorGroup.class, uid);

    if (group == null) {
      throw new NotFoundException(IndicatorGroup.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setInfrastructuralIndicators(group);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/infrastructuralDataElements")
  public @ResponseBody DataElementGroup getInfrastructuralDataElements(
      Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getInfrastructuralDataElements();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/infrastructuralDataElements")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setInfrastructuralDataElements(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    DataElementGroup group = identifiableObjectManager.get(DataElementGroup.class, uid);

    if (group == null) {
      throw new NotFoundException(DataElementGroup.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setInfrastructuralDataElements(group);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/infrastructuralPeriodType")
  public @ResponseBody BaseIdentifiableObject getInfrastructuralPeriodType(
      Model model, HttpServletRequest request) {
    String name =
        configurationService
            .getConfiguration()
            .getInfrastructuralPeriodTypeDefaultIfNull()
            .getName();

    return new BaseIdentifiableObject(name, name, name);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/infrastructuralPeriodType")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setInfrastructuralPeriodType(@RequestBody String name) throws NotFoundException {
    name = trim(name);

    PeriodType periodType = PeriodType.getPeriodTypeByName(name);

    if (periodType == null) {
      throw new NotFoundException("Period type not found: " + name);
    }

    Configuration configuration = configurationService.getConfiguration();

    periodType = periodService.reloadPeriodType(periodType);

    configuration.setInfrastructuralPeriodType(periodType);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/selfRegistrationRole")
  public @ResponseBody UserRole getSelfRegistrationRole(Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getSelfRegistrationRole();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/selfRegistrationRole")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setSelfRegistrationRole(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    UserRole userGroup = identifiableObjectManager.get(UserRole.class, uid);

    if (userGroup == null) {
      throw new NotFoundException(UserRole.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setSelfRegistrationRole(userGroup);

    configurationService.setConfiguration(configuration);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @DeleteMapping("/selfRegistrationRole")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeSelfRegistrationRole() {
    Configuration configuration = configurationService.getConfiguration();

    configuration.setSelfRegistrationRole(null);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/selfRegistrationOrgUnit")
  public @ResponseBody OrganisationUnit getSelfRegistrationOrgUnit(
      Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getSelfRegistrationOrgUnit();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/selfRegistrationOrgUnit")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setSelfRegistrationOrgUnit(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    OrganisationUnit orgunit = identifiableObjectManager.get(OrganisationUnit.class, uid);

    if (orgunit == null) {
      throw new NotFoundException(OrganisationUnit.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setSelfRegistrationOrgUnit(orgunit);

    configurationService.setConfiguration(configuration);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @DeleteMapping("/selfRegistrationOrgUnit")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeSelfRegistrationOrgUnit() {
    Configuration configuration = configurationService.getConfiguration();

    configuration.setSelfRegistrationOrgUnit(null);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/remoteServerUrl")
  public @ResponseBody String getRemoteServerUrl(Model model, HttpServletRequest request) {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_URL);
  }

  @GetMapping("/remoteServerUsername")
  public @ResponseBody String getRemoteServerUsername(Model model, HttpServletRequest request) {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_USERNAME);
  }

  @GetMapping("/facilityOrgUnitGroupSet")
  public @ResponseBody OrganisationUnitGroupSet getFacilityOrgUnitGroupSet() {
    return configurationService.getConfiguration().getFacilityOrgUnitGroupSet();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/facilityOrgUnitGroupSet")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setFacilityOrgUnitGroupSet(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    OrganisationUnitGroupSet groupSet =
        identifiableObjectManager.get(OrganisationUnitGroupSet.class, uid);

    if (groupSet == null) {
      throw new NotFoundException(OrganisationUnitGroupSet.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setFacilityOrgUnitGroupSet(groupSet);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/facilityOrgUnitLevel")
  public @ResponseBody OrganisationUnitLevel getFacilityOrgUnitLevel() {
    return configurationService.getConfiguration().getFacilityOrgUnitLevel();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping("/facilityOrgUnitLevel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setFacilityOrgUnitLevel(@RequestBody String uid) throws NotFoundException {
    uid = trim(uid);

    OrganisationUnitLevel level = identifiableObjectManager.get(OrganisationUnitLevel.class, uid);

    if (level == null) {
      throw new NotFoundException(OrganisationUnitLevel.class, uid);
    }

    Configuration configuration = configurationService.getConfiguration();

    configuration.setFacilityOrgUnitLevel(level);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping(value = "/corsWhitelist", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody Set<String> getCorsWhitelist(Model model, HttpServletRequest request) {
    return configurationService.getConfiguration().getCorsWhitelist();
  }

  @SuppressWarnings("unchecked")
  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @PostMapping(value = "/corsWhitelist", consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setCorsWhitelist(@RequestBody String input) throws IOException {
    Set<String> corsWhitelist = renderService.fromJson(input, Set.class);

    Configuration configuration = configurationService.getConfiguration();

    configuration.setCorsWhitelist(corsWhitelist);

    configurationService.setConfiguration(configuration);
  }

  @GetMapping("/systemReadOnlyMode")
  public @ResponseBody boolean getSystemReadOnlyMode(Model model, HttpServletRequest request) {
    return config.isReadOnlyMode();
  }

  @GetMapping("/appHubUrl")
  public @ResponseBody String getAppHubUrl(Model model, HttpServletRequest request) {
    return appManager.getAppHubUrl();
  }

  /**
   * Trims the given string payload by removing double qoutes.
   *
   * @param string the string.
   * @return a trimmed string.
   */
  private String trim(String string) {
    return StringUtils.remove(string, "\"");
  }
}
