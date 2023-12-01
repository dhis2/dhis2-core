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
package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.dashboard.DashboardItemShape;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.jsontree.JsonList;

/**
 * Web API equivalent of a {@link org.hisp.dhis.dashboard.DashboardItem}.
 *
 * @author Jan Bernitt
 */
public interface JsonDashboardItem extends JsonIdentifiableObject {
  default DashboardItemType getType() {
    return getString("type").parsed(DashboardItemType::valueOf);
  }

  default int getInterpretationCount() {
    return getNumber("interpretationCount").intValue();
  }

  default int getInterpretationLikeCount() {
    return getNumber("interpretationLikeCount").intValue();
  }

  default int getContentCount() {
    return getNumber("contentCount").intValue();
  }

  default String getText() {
    return getString("text").string();
  }

  default Boolean getMessages() {
    return getBoolean("messages").bool();
  }

  default String getAppKey() {
    return getString("appKey").string();
  }

  default DashboardItemShape getShape() {
    return getString("shape").parsed(DashboardItemShape::valueOf);
  }

  default Number getX() {
    return getNumber("x").number();
  }

  default Number getY() {
    return getNumber("y").number();
  }

  default Number getHeight() {
    return getNumber("height").number();
  }

  default Number getWidth() {
    return getNumber("width").number();
  }

  default JsonIdentifiableObject getVisualization() {
    return get("visualization", JsonIdentifiableObject.class);
  }

  default JsonIdentifiableObject getChart() {
    return get("chart", JsonIdentifiableObject.class);
  }

  default JsonIdentifiableObject getReportTable() {
    return get("reportTable", JsonIdentifiableObject.class);
  }

  default JsonIdentifiableObject getEventChart() {
    return get("eventChart", JsonIdentifiableObject.class);
  }

  default JsonIdentifiableObject getMap() {
    return get("map", JsonIdentifiableObject.class);
  }

  default JsonIdentifiableObject getEventReport() {
    return get("eventReport", JsonIdentifiableObject.class);
  }

  default JsonList<JsonUser> getUsers() {
    return getList("users", JsonUser.class);
  }

  default JsonList<JsonIdentifiableObject> getReports() {
    return getList("reports", JsonIdentifiableObject.class);
  }

  default JsonList<JsonIdentifiableObject> getResources() {
    return getList("resources", JsonIdentifiableObject.class);
  }
}
