package org.hisp.dhis.dxf2.events;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.UserInfoSnapshot;

import java.util.Objects;

@JacksonXmlRootElement(localName = "dataValue", namespace = DxfNamespaces.DXF_2_0)
public class DataValue {
    private String created;

    private UserInfoSnapshot createdByUserInfo;

    private String lastUpdated;

    private UserInfoSnapshot lastUpdatedByUserInfo;

    private String value;

    private String dataElement = "";

    private Boolean providedElsewhere = false;

    private String storedBy;

    private boolean skipSynchronization;

    public DataValue() {}

    public DataValue(String dataElement, String value) {
        this.dataElement = dataElement;
        this.value = value;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    public UserInfoSnapshot getCreatedByUserInfo() {
        return createdByUserInfo;
    }

    public void setCreatedByUserInfo(UserInfoSnapshot createdByUserInfo) {
        this.createdByUserInfo = createdByUserInfo;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
    public UserInfoSnapshot getLastUpdatedByUserInfo() {
        return lastUpdatedByUserInfo;
    }

    public void setLastUpdatedByUserInfo(UserInfoSnapshot lastUpdatedByUserInfo) {
        this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getDataElement() {
        return dataElement;
    }

    public void setDataElement(String dataElement) {
        this.dataElement = dataElement;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public Boolean getProvidedElsewhere() {
        return providedElsewhere;
    }

    public void setProvidedElsewhere(Boolean providedElsewhere) {
        this.providedElsewhere = providedElsewhere;
    }

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public String getStoredBy() {
        return storedBy;
    }

    public void setStoredBy(String storedBy) {
        this.storedBy = storedBy;
    }

    @JsonIgnore
    public boolean isSkipSynchronization() {
        return skipSynchronization;
    }

    public void setSkipSynchronization(boolean skipSynchronization) {
        this.skipSynchronization = skipSynchronization;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        DataValue dataValue = (DataValue) object;

        return dataElement.equals(dataValue.dataElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataElement);
    }

    @Override
    public String toString() {
        return "DataValue{"
                + "value='"
                + value
                + '\''
                + ", dataElement='"
                + dataElement
                + '\''
                + ", providedElsewhere="
                + providedElsewhere
                + ", storedBy='"
                + storedBy
                + '\''
                + ", skipSynchronization='"
                + skipSynchronization
                + '\''
                + '}';
    }
}

