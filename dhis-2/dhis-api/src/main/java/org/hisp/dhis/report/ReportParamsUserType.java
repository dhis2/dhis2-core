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
package org.hisp.dhis.report;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.hisp.dhis.visualization.ReportingParams;

/**
 * Maps the {@link ReportingParams} of a {@link Report} to the {@code paramreportingmonth} and
 * {@code paramorganisationunit} columns. A report only stores the reporting period and organisation
 * unit flags, while {@link ReportingParams} also holds the parent and grandparent organisation unit
 * flags of a visualization, so it cannot be mapped as an embeddable on the report table.
 *
 * <p>A row with both columns null loads as a null {@link ReportingParams}, and a null {@link
 * ReportingParams} is stored as two nulls. Copies, including the second-level cache entry, keep
 * only the two stored flags, so a cached report reads the same as one loaded from the database.
 */
public class ReportParamsUserType implements CompositeUserType {
  private static final String[] PROPERTY_NAMES = {"reportingPeriod", "organisationUnit"};

  private static final Type[] PROPERTY_TYPES = {
    StandardBasicTypes.BOOLEAN, StandardBasicTypes.BOOLEAN
  };

  @Override
  public String[] getPropertyNames() {
    return PROPERTY_NAMES.clone();
  }

  @Override
  public Type[] getPropertyTypes() {
    return PROPERTY_TYPES.clone();
  }

  @Override
  public Object getPropertyValue(Object component, int property) {
    ReportingParams params = (ReportingParams) component;
    return property == 0 ? params.isReportingPeriod() : params.isOrganisationUnit();
  }

  @Override
  public void setPropertyValue(Object component, int property, Object value) {
    ReportingParams params = (ReportingParams) component;
    if (property == 0) {
      params.setReportingPeriod(Boolean.TRUE.equals(value));
    } else {
      params.setOrganisationUnit(Boolean.TRUE.equals(value));
    }
  }

  @Override
  public Class<ReportingParams> returnedClass() {
    return ReportingParams.class;
  }

  @Override
  public boolean equals(Object x, Object y) {
    if (x == y) {
      return true;
    }
    if (x == null || y == null) {
      return false;
    }
    ReportingParams a = (ReportingParams) x;
    ReportingParams b = (ReportingParams) y;
    return a.isReportingPeriod() == b.isReportingPeriod()
        && a.isOrganisationUnit() == b.isOrganisationUnit();
  }

  @Override
  public int hashCode(Object x) {
    ReportingParams params = (ReportingParams) x;
    return Objects.hash(params.isReportingPeriod(), params.isOrganisationUnit());
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    Boolean reportingPeriod = StandardBasicTypes.BOOLEAN.nullSafeGet(rs, names[0], session);
    Boolean organisationUnit = StandardBasicTypes.BOOLEAN.nullSafeGet(rs, names[1], session);

    if (reportingPeriod == null && organisationUnit == null) {
      return null;
    }

    ReportingParams params = new ReportingParams();
    params.setReportingPeriod(Boolean.TRUE.equals(reportingPeriod));
    params.setOrganisationUnit(Boolean.TRUE.equals(organisationUnit));
    return params;
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
      throws SQLException {
    ReportingParams params = (ReportingParams) value;
    Boolean reportingPeriod = params == null ? null : params.isReportingPeriod();
    Boolean organisationUnit = params == null ? null : params.isOrganisationUnit();
    StandardBasicTypes.BOOLEAN.nullSafeSet(st, reportingPeriod, index, session);
    StandardBasicTypes.BOOLEAN.nullSafeSet(st, organisationUnit, index + 1, session);
  }

  @Override
  public Object deepCopy(Object value) {
    if (value == null) {
      return null;
    }
    ReportingParams params = (ReportingParams) value;
    return new ReportingParams(
        params.isReportingPeriod(), false, false, params.isOrganisationUnit());
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public Serializable disassemble(Object value, SharedSessionContractImplementor session) {
    return (Serializable) deepCopy(value);
  }

  @Override
  public Object assemble(
      Serializable cached, SharedSessionContractImplementor session, Object owner) {
    return deepCopy(cached);
  }

  @Override
  public Object replace(
      Object original, Object target, SharedSessionContractImplementor session, Object owner) {
    return deepCopy(original);
  }
}
