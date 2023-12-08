/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common;

public enum ColumnHeader {
  TEI("tei", "Tracked entity instance"),
  GEOMETRY("geometry", "Geometry"),
  ENROLLMENT_DATE("enrollmentdate", "Enrollment date"),
  INCIDENT_DATE("incidentdate", "Incident date"),
  STORED_BY("storedby", "Stored by"),
  CREATED_BY_DISPLAY_NAME("createdbydisplayname", "Created by"),
  LAST_UPDATED_BY_DISPLAY_NAME("lastupdatedbydisplayname", "Last updated by"),
  LAST_UPDATED("lastupdated", "Last updated on"),
  LONGITUDE("longitude", "Longitude"),
  LATITUDE("latitude", "Latitude"),
  ORG_UNIT_NAME("ouname", "Organisation unit name"),
  ORG_UNIT_NAME_HIERARCHY("ounamehierarchy", "Organisation unit name hierarchy"),
  ORG_UNIT_CODE("oucode", "Organisation unit code"),

  ORG_UNIT("ou", "Organisation unit"),
  PROGRAM_STATUS("programstatus", "Program status"),
  EVENT("psi", "Event"),
  PROGRAM_INSTANCE("pi", "Program instance"),
  PROGRAM_STAGE("ps", "Program stage"),
  EVENT_DATE("eventdate", "Event date"),
  SCHEDULED_DATE("scheduleddate", "Scheduled date"),
  COUNT("count", "Count"),
  CENTER("center", "Center"),
  EXTENT("extent", "Extent"),
  POINTS("points", "Points"),
  EVENT_STATUS("eventstatus", "Event status"),

  DIMENSION("dx", "Data"),
  DIMENSION_NAME("dxname", "Event status"),
  PERIOD("pe", "Event status"),
  CATEGORY_OPTION_COMBO("coc", "Category option combo"),
  CATEGORY_OPTION_COMBO_NAME("cocname", "Category option combo name"),
  ATTRIBUTE_OPTION_COMBO("aoc", "Attribute option combo"),
  ATTRIBUTE_OPTION_COMBO_NAME("aocname", "Attribute option combo name"),
  VALUE("value", "Value"),
  MEDIAN("median", "Median"),
  MEAN("mean", "Mean"),
  MEDIAN_ABS_DEVIATION("medianabsdeviation", "Median absolute deviation"),
  STANDARD_DEVIATION("stddev", "Standard deviation"),
  ABSOLUTE_DEVIATION("absdev", "Absolute deviation"),
  MODIFIED_ZSCORE("modifiedzscore", "Modified zScore"),
  ZSCORE("zscore", "zScore"),
  LOWER_BOUNDARY("lowerbound", "Lower boundary"),
  UPPER_BOUNDARY("upperbound", "Upper boundary");

  private final String item;
  private final String name;

  ColumnHeader(String item, String name) {
    this.item = item;
    this.name = name;
  }

  public String getItem() {
    return item;
  }

  public String getName() {
    return name;
  }
}
