/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.JASPER_REPORTS_ENABLED;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Enforces the {@code jasper.reports.enabled} configuration gate for report designs on every
 * metadata write path. {@code POST/PUT/PATCH /api/reports} and {@code POST /api/metadata} share
 * this object bundle hook.
 *
 * <p>A non-HTML report design ({@link Report#getDesignContent()}) is compiled by the JasperReports
 * engine when the report is rendered. When the gate is off (the default), this hook rejects
 * introducing or changing a Jasper design, including promoting an HTML report to a non-HTML type
 * with unchanged content. When the gate is on, only superusers may do so or give a stored report
 * another non-HTML type; other users get the error codes for objects they may not create or update.
 * HTML reports, new reports without design content, updates that keep the type and design of a
 * stored report, and deletion are unaffected. Jasper rendering and the direct {@code PUT
 * /api/reports/{uid}/design} endpoint (which does not use the object bundle and requires ALL) are
 * gated separately in the controller.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class ReportObjectBundleHook extends AbstractObjectBundleHook<Report> {

  private final DhisConfigurationProvider configuration;

  @Override
  public void validate(Report report, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (configuration.isDisabled(JASPER_REPORTS_ENABLED)) {
      if (isJasperDesignChange(report, bundle)) {
        addReports.accept(
            new ErrorReport(Report.class, ErrorCode.E1007, JASPER_REPORTS_ENABLED.getKey()));
      }
      return;
    }

    UserDetails user = bundle.getUserDetails();
    if (user != null
        && !user.isSuper()
        && (isJasperDesignChange(report, bundle) || isJasperTypeChange(report, bundle))) {
      PreheatIdentifier identifier = bundle.getPreheatIdentifier();
      addReports.accept(
          new ErrorReport(
              Report.class,
              bundle.isPersisted(report) ? ErrorCode.E3001 : ErrorCode.E3000,
              identifier.getIdentifiersWithName(user),
              identifier.getIdentifiersWithName(report)));
    }
  }

  /**
   * Validation is skipped entirely for superusers importing with {@code skipValidation=true}, so
   * {@link #validate} alone cannot keep Jasper designs from being stored while the gate is off.
   * Fails the commit before anything is written if the bundle still introduces or changes one.
   * Other users are always validated, so the superuser rule needs no commit-phase check.
   */
  @Override
  public void preCommit(ObjectBundle bundle) {
    if (bundle.getImportMode().isDelete() || configuration.isEnabled(JASPER_REPORTS_ENABLED)) {
      return;
    }

    for (Report report : bundle.getObjects(Report.class)) {
      if (isJasperDesignChange(report, bundle)) {
        throw new IllegalQueryException(ErrorCode.E1007, JASPER_REPORTS_ENABLED.getKey());
      }
    }
  }

  /**
   * @return true when the given report introduces or changes a Jasper design
   */
  private boolean isJasperDesignChange(Report report, ObjectBundle bundle) {
    if (report.isTypeHtml() || !StringUtils.hasText(report.getDesignContent())) {
      return false;
    }

    // On create the preheat returns the in-flight object, so a diff would wrongly report no change;
    // block unconditionally instead.
    if (!bundle.isPersisted(report)) {
      return true;
    }

    Report persisted = bundle.getPreheat().get(bundle.getPreheatIdentifier(), report);
    String persistedDesignContent = persisted == null ? null : persisted.getDesignContent();

    // Promoting HTML to a non-HTML type makes unchanged content a Jasper design too.
    return !report.getDesignContent().equals(persistedDesignContent)
        || (persisted != null && persisted.isTypeHtml());
  }

  /**
   * @return true when the given report is already stored and gets a different non-HTML type
   */
  private boolean isJasperTypeChange(Report report, ObjectBundle bundle) {
    if (report.isTypeHtml() || !bundle.isPersisted(report)) {
      return false;
    }

    Report persisted = bundle.getPreheat().get(bundle.getPreheatIdentifier(), report);
    return persisted != null && persisted.getType() != report.getType();
  }
}
