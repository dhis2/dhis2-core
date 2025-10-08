package org.hisp.dhis.dxf2.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.hisp.dhis.common.BaseLinkableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.DataValue;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JacksonXmlRootElement(localName = "event", namespace = DxfNamespaces.DXF_2_0)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Event extends BaseLinkableObject {
    private Long eventId;

    @EqualsAndHashCode.Include
    private String uid;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    @EqualsAndHashCode.Include
    private String event;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private EventStatus status = EventStatus.ACTIVE;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String program;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private ProgramType programType;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String programStage;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String enrollment;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private EnrollmentStatus enrollmentStatus;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String orgUnit;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String orgUnitName;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String trackedEntityInstance;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    @Builder.Default
    private Set<Relationship> relationships = new HashSet<>();

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String eventDate;

    @JsonProperty(required = false)
    @JacksonXmlProperty(isAttribute = true)
    private String dueDate;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String storedBy;

    @JsonProperty
    @JacksonXmlElementWrapper(localName = "dataValues", namespace = DxfNamespaces.DXF_2_0)
    @JacksonXmlProperty(localName = "dataValue", namespace = DxfNamespaces.DXF_2_0)
    @Builder.Default
    private Set<DataValue> dataValues = new HashSet<>();

    @JsonProperty
    @JacksonXmlElementWrapper(localName = "notes", namespace = DxfNamespaces.DXF_2_0)
    @JacksonXmlProperty(localName = "note", namespace = DxfNamespaces.DXF_2_0)
    @Builder.Default
    private List<Note> notes = new ArrayList<>();

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private Boolean followup;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    @Builder.Default
    private Boolean deleted = false;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String created;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private UserInfoSnapshot createdByUserInfo;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String lastUpdated;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private UserInfoSnapshot lastUpdatedByUserInfo;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String createdAtClient;

    @JsonProperty(required = true)
    @JacksonXmlProperty(isAttribute = true)
    private String lastUpdatedAtClient;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String attributeOptionCombo;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String attributeCategoryOptions;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String completedBy;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String completedDate;

    private int optionSize;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private Geometry geometry;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String assignedUser;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String assignedUserUsername;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String assignedUserDisplayName;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String assignedUserFirstName;

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    private String assignedUserSurname;

    public void clear() {
        this.setDeleted(null);
        this.setStatus(null);
        this.setDataValues(null);
        this.setNotes(null);
    }

    @JsonIgnore
    public Long getId() {
        return eventId;
    }

    public void setId(Long eventId) {
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "event='" + event + '\'' +
                ", status=" + status +
                ", program='" + program + '\'' +
                ", programStage='" + programStage + '\'' +
                ", enrollment='" + enrollment + '\'' +
                ", orgUnit='" + orgUnit + '\'' +
                ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
                ", relationships=" + relationships +
                ", eventDate='" + eventDate + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", dataValues=" + dataValues +
                ", notes=" + notes +
                ", deleted=" + deleted +
                '}';
    }
}