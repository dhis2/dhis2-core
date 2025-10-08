package org.hisp.dhis.dxf2.events;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Setter;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Pager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Zubair Asghar
 */
@JacksonXmlRootElement(localName = "events", namespace = DxfNamespaces.DXF_2_0)
public class Events {
    @Setter
    private String program;

    @Setter
    private String programInstance;

    @Setter
    private List<Event> events = new ArrayList<>();

    private Map<Object, Object> metaData;

    private Pager pager;

    public Events() {}

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getProgram() {
        return program;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getProgramInstance() {
        return programInstance;
    }

    @JsonProperty
    @JacksonXmlElementWrapper(
            localName = "events",
            useWrapping = false,
            namespace = DxfNamespaces.DXF_2_0)
    @JacksonXmlProperty(localName = "event", namespace = DxfNamespaces.DXF_2_0)
    public List<Event> getEvents() {
        return events;
    }

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    public Map<Object, Object> getMetaData() {
        return metaData;
    }

    @JsonIgnore
    public void setMetaData(Map<Object, Object> metaData) {
        this.metaData = metaData;
    }

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    public Pager getPager() {
        return pager;
    }

    @JsonIgnore
    public void setPager(Pager pager) {
        this.pager = pager;
    }

    @Override
    public String toString() {
        return "Events{"
                + "program='"
                + program
                + '\''
                + ", programInstance='"
                + programInstance
                + '\''
                + ", events="
                + events
                + '}';
    }
}

