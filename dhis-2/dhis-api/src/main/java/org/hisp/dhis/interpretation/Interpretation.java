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
package org.hisp.dhis.interpretation;

import static org.hisp.dhis.analytics.AnalyticsFavoriteType.DATASET_REPORT;
import static org.hisp.dhis.analytics.AnalyticsFavoriteType.EVENT_CHART;
import static org.hisp.dhis.analytics.AnalyticsFavoriteType.EVENT_REPORT;
import static org.hisp.dhis.analytics.AnalyticsFavoriteType.EVENT_VISUALIZATION;
import static org.hisp.dhis.analytics.AnalyticsFavoriteType.MAP;
import static org.hisp.dhis.analytics.AnalyticsFavoriteType.VISUALIZATION;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsFavoriteType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "interpretation", namespace = DXF_2_0)
public class Interpretation extends BaseIdentifiableObject {
  private Visualization visualization;

  private EventVisualization eventVisualization;

  private Map map;

  private EventReport eventReport;

  private EventChart eventChart;

  private DataSet dataSet;

  /** Applicable to visualization and data set report. */
  private Period period;

  /** Applicable to visualization and data set report. */
  private OrganisationUnit organisationUnit;

  private String text;

  private List<InterpretationComment> comments = new ArrayList<>();

  private int likes;

  private Set<User> likedBy = new HashSet<>();

  private List<Mention> mentions = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Interpretation() {}

  public Interpretation(
      Visualization visualization, OrganisationUnit organisationUnit, String text) {
    this.visualization = visualization;
    visualization.getInterpretations().add(this);
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  public Interpretation(
      Visualization visualization, Period period, OrganisationUnit organisationUnit, String text) {
    this.visualization = visualization;
    visualization.getInterpretations().add(this);
    this.period = period;
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  public Interpretation(Map map, String text) {
    this.map = map;
    map.getInterpretations().add(this);
    this.text = text;
  }

  public Interpretation(
      EventVisualization eventVisualization,
      EventReport eventReport,
      OrganisationUnit organisationUnit,
      String text) {
    this.eventReport = eventReport;
    eventReport.getInterpretations().add(this);
    this.eventVisualization = eventVisualization;
    eventVisualization.getInterpretations().add(this);
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  public Interpretation(
      EventVisualization eventVisualization,
      EventChart eventChart,
      OrganisationUnit organisationUnit,
      String text) {
    this.eventChart = eventChart;
    eventChart.getInterpretations().add(this);
    this.eventVisualization = eventVisualization;
    eventVisualization.getInterpretations().add(this);
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  public Interpretation(
      EventVisualization eventVisualization, OrganisationUnit organisationUnit, String text) {
    this.eventVisualization = eventVisualization;
    eventVisualization.getInterpretations().add(this);
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  public Interpretation(
      DataSet dataSet, Period period, OrganisationUnit organisationUnit, String text) {
    this.dataSet = dataSet;
    this.period = period;
    this.organisationUnit = organisationUnit;
    this.text = text;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public AnalyticsFavoriteType getType() {
    if (visualization != null) {
      return VISUALIZATION;
    } else if (eventVisualization != null) {
      return EVENT_VISUALIZATION;
    } else if (map != null) {
      return MAP;
    } else if (eventReport != null) {
      return EVENT_REPORT;
    } else if (eventChart != null) {
      return EVENT_CHART;
    } else if (dataSet != null) {
      return DATASET_REPORT;
    }

    return null;
  }

  public IdentifiableObject getObject() {
    if (visualization != null) {
      return visualization;
    } else if (map != null) {
      return map;
    } else if (eventReport != null) {
      return eventReport;
    } else if (eventChart != null) {
      return eventChart;
    } else if (eventVisualization != null) {
      return eventVisualization;
    } else if (dataSet != null) {
      return dataSet;
    }

    return null;
  }

  public void addComment(InterpretationComment comment) {
    this.comments.add(comment);
  }

  public boolean isVisualizationInterpretation() {
    return visualization != null;
  }

  public boolean isEventVisualizationInterpretation() {
    return visualization != null;
  }

  public boolean isEventReportInterpretation() {
    return eventReport != null;
  }

  public boolean isEventChartInterpretation() {
    return eventChart != null;
  }

  public boolean isDataSetReportInterpretation() {
    return dataSet != null;
  }

  public PeriodType getPeriodType() {
    return period != null ? period.getPeriodType() : null;
  }

  /**
   * Attempts to add the given user to the set of users liking this interpretation. If user not
   * already present, increments the like count with one.
   *
   * @param user the user liking this interpretation.
   * @return true if the given user had not already liked this interpretation.
   */
  public boolean like(User user) {
    boolean like = this.likedBy.add(user);

    if (like) {
      this.likes++;
    }

    return like;
  }

  /**
   * Attempts to remove the given user from the set of users liking this interpretation. If user not
   * already present, decrease the like count with one.
   *
   * @param user the user removing the like from this interpretation.
   * @return true if the given user had previously liked this interpretation.
   */
  public boolean unlike(User user) {
    boolean unlike = this.likedBy.remove(user);

    if (unlike) {
      this.likes--;
    }

    return unlike;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @Override
  public String getName() {
    return uid;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Visualization getVisualization() {
    return visualization;
  }

  public void setVisualization(Visualization visualization) {
    this.visualization = visualization;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventVisualization getEventVisualization() {
    return eventVisualization;
  }

  public void setEventVisualization(EventVisualization eventVisualization) {
    this.eventVisualization = eventVisualization;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Map getMap() {
    return map;
  }

  public void setMap(Map map) {
    this.map = map;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventReport getEventReport() {
    return eventReport;
  }

  public void setEventReport(EventReport eventReport) {
    this.eventReport = eventReport;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventChart getEventChart() {
    return eventChart;
  }

  public void setEventChart(EventChart eventChart) {
    this.eventChart = eventChart;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "comments", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "comment", namespace = DXF_2_0)
  public List<InterpretationComment> getComments() {
    return comments;
  }

  public void setComments(List<InterpretationComment> comments) {
    this.comments = comments;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getLikes() {
    return likes;
  }

  public void setLikes(int likes) {
    this.likes = likes;
  }

  @JsonProperty("likedBy")
  @JsonSerialize(contentUsing = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(contentUsing = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlElementWrapper(localName = "likedBy", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "likeByUser", namespace = DXF_2_0)
  public Set<User> getLikedBy() {
    return likedBy;
  }

  public void setLikedBy(Set<User> likedBy) {
    this.likedBy = likedBy;
  }

  @JsonProperty("mentions")
  @JacksonXmlElementWrapper(localName = "mentions", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "mentions", namespace = DXF_2_0)
  public List<Mention> getMentions() {
    return mentions;
  }

  public void setMentions(List<Mention> mentions) {
    this.mentions = mentions;
  }

  @JsonIgnore
  public void setMentionsFromUsers(Set<User> users) {
    this.mentions = MentionUtils.convertUsersToMentions(users);
  }
}
