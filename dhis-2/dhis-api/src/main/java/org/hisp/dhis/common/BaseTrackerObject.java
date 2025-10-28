package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import lombok.Setter;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.User;

@MappedSuperclass
public class BaseTrackerObject {
  
  @Setter
  @AuditAttribute
  @Column(name = "uid", unique = true, nullable = false, length = 11)
  protected String uid;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @Setter
  protected Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @Setter
  protected Date lastUpdated;

  @ManyToOne
  @JoinColumn(name = "lastupdatedby")
  @Setter
  protected User lastUpdatedBy;

  @Column(name = "storedby", length = 255)
  @Setter
  protected String storedBy;

  // -------------------------------------------------------------------------------------------
  // Transient fields
  // -------------------------------------------------------------------------------------------

  /**
   * As part of the serializing process, this field can be set to indicate a link to this
   * identifiable object (will be used on the web layer for navigating the REST API)
   */
  @Transient
  @Setter protected String href;

  /** Access information for this object. Applies to current user. */
  @Transient @Setter protected Access access;

  // -------------------------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getHref() {
    return href;
  }

  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @PropertyRange(min = 11, max = 11)
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  public String getUid() {
    return uid;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getStoredBy() {
    return storedBy;
  }
}
