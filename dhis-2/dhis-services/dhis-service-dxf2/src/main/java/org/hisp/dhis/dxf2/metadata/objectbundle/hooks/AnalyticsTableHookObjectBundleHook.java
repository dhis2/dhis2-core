/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Analytics table hooks store SQL which is executed as the DHIS2 database role. Creating or
 * updating them requires {@code analytics.table_hooks.enabled = on} in dhis.conf and a superuser.
 * Deletion is always allowed.
 */
@Component
@AllArgsConstructor
public class AnalyticsTableHookObjectBundleHook
    extends AbstractObjectBundleHook<AnalyticsTableHook> {

  private final AnalyticsTableHookService analyticsTableHookService;
  private final DhisConfigurationProvider config;

  @Override
  public void validate(
      AnalyticsTableHook analyticsTableHook,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    if (config.isDisabled(ANALYTICS_TABLE_HOOKS_ENABLED)) {
      addReports.accept(
          new ErrorReport(
              AnalyticsTableHook.class, ErrorCode.E1007, ANALYTICS_TABLE_HOOKS_ENABLED.getKey()));
      return;
    }
    UserDetails user = bundle.getUserDetails();
    if (user != null && !user.isSuper()) {
      PreheatIdentifier identifier = bundle.getPreheatIdentifier();
      addReports.accept(
          new ErrorReport(
              AnalyticsTableHook.class,
              bundle.isPersisted(analyticsTableHook) ? ErrorCode.E3001 : ErrorCode.E3000,
              identifier.getIdentifiersWithName(user),
              identifier.getIdentifiersWithName(analyticsTableHook)));
      return;
    }
    checkDuplicateAnalyticsTableHook(analyticsTableHook, addReports);
  }

  /**
   * Validation is skipped entirely for superusers importing with {@code skipValidation=true}, so
   * {@link #validate} alone cannot keep hooks from being stored while the feature is disabled.
   * Fails the commit before anything is written if the bundle still creates or updates hooks.
   */
  @Override
  public void preCommit(ObjectBundle bundle) {
    if (!bundle.getImportMode().isDelete()
        && bundle.getObjectsCount(AnalyticsTableHook.class) > 0
        && config.isDisabled(ANALYTICS_TABLE_HOOKS_ENABLED)) {
      throw new IllegalQueryException(ErrorCode.E1007, ANALYTICS_TABLE_HOOKS_ENABLED.getKey());
    }
  }

  static boolean areEqual(AnalyticsTableHook one, AnalyticsTableHook other) {
    return one.getPhase() == other.getPhase()
        && one.getResourceTableType() == other.getResourceTableType()
        && one.getAnalyticsTableType() == other.getAnalyticsTableType()
        && one.getSql().equals(other.getSql());
  }

  private void checkDuplicateAnalyticsTableHook(
      AnalyticsTableHook analyticsTableHook, Consumer<ErrorReport> addReports) {

    List<AnalyticsTableHook> analyticsTableHooks =
        analyticsTableHookService.getByPhaseAndResourceTableType(
            analyticsTableHook.getPhase(), analyticsTableHook.getResourceTableType());

    analyticsTableHooks.forEach(
        existingAnalyticsTableHook -> {
          if (!Objects.equals(existingAnalyticsTableHook.getUid(), analyticsTableHook.getUid())
              && areEqual(analyticsTableHook, existingAnalyticsTableHook)) {
            addReports.accept(
                new ErrorReport(
                    AnalyticsTableHook.class,
                    ErrorCode.E6400,
                    analyticsTableHook.getName(),
                    existingAnalyticsTableHook.getName()));
          }
        });
  }
}
