/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Analytics table hooks store SQL which is executed as the DHIS2 database role. Creating or
 * updating them requires {@code analytics.table_hooks.enabled = on} in dhis.conf and a superuser.
 * Deletion is always allowed.
 *
 * @author Morten Svanæs
 */
@Component
@AllArgsConstructor
public class AnalyticsTableHookObjectBundleHook
    extends AbstractObjectBundleHook<AnalyticsTableHook> {

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
    User user = bundle.getUser();
    if (user != null && !user.isSuper()) {
      PreheatIdentifier identifier = bundle.getPreheatIdentifier();
      addReports.accept(
          new ErrorReport(
              AnalyticsTableHook.class,
              bundle.isPersisted(analyticsTableHook) ? ErrorCode.E3001 : ErrorCode.E3000,
              identifier.getIdentifiersWithName(user),
              identifier.getIdentifiersWithName(analyticsTableHook)));
    }
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
}
