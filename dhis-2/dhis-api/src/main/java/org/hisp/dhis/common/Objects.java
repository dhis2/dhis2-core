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
package org.hisp.dhis.common;

import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.visualization.Visualization;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public enum Objects {
  CONSTANT("constant", Constant.class),
  DATAELEMENT("dataElement", DataElement.class),
  EXTENDEDDATAELEMENT("extendedDataElement", DataElement.class),
  DATAELEMENTGROUP("dataElementGroup", DataElementGroup.class),
  DATAELEMENTGROUPSET("dataElementGroupSet", DataElementGroupSet.class),
  INDICATORTYPE("indicatorType", IndicatorType.class),
  INDICATOR("indicator", Indicator.class),
  INDICATORGROUP("indicatorGroup", IndicatorGroup.class),
  INDICATORGROUPSET("indicatorGroupSet", IndicatorGroupSet.class),
  DATASET("dataSet", DataSet.class),
  ORGANISATIONUNIT("organisationUnit", OrganisationUnit.class),
  ORGANISATIONUNITGROUP("organisationUnitGroup", OrganisationUnitGroup.class),
  ORGANISATIONUNITGROUPSET("organisationUnitGroupSet", OrganisationUnitGroupSet.class),
  ORGANISATIONUNITLEVEL("organisationUnitLevel", OrganisationUnitLevel.class),
  VALIDATIONRULE("validationRule", ValidationRule.class),
  PERIOD("period", Period.class),
  DATAVALUE("dataValue", DataValue.class),
  USER("user", User.class),
  USERGROUP("userGroup", UserGroup.class),
  VISUALIZATION("visualization", Visualization.class),
  EVENTVISUALIZATION("eventVisualization", EventVisualization.class),
  REPORT("report", Report.class),
  MAP("map", Map.class),
  DASHBOARD("dashboard", Dashboard.class),
  PROGRAM("program", Program.class),
  PROGRAMSTAGEINSTANCE("programStageInstance", ProgramStageInstance.class),
  PROGRAMINSTANCE("programInstance", ProgramInstance.class),
  TRACKEDENTITYINSTANCE("trackedEntityInstance", TrackedEntityInstance.class),
  TRACKEDENTITYATTRIBUTE("trackedEntityAttribute", TrackedEntityAttribute.class);

  private String value;

  private Class<?> clazz;

  Objects(String value, Class<?> clazz) {
    this.value = value;
    this.clazz = clazz;
  }

  public static Objects fromClass(Class<?> clazz) throws IllegalAccessException {
    if (clazz == null) {
      throw new NullPointerException();
    }

    for (Objects obj : Objects.values()) {
      if (obj.clazz.equals(clazz)) {
        return obj;
      }
    }

    throw new IllegalAccessException(
        "No item found in enum Objects for class '" + clazz.getSimpleName() + "'. ");
  }

  public String getValue() {
    return value;
  }
}
