/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.command;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * The table has no code, creator, last updated by, sharing, translations or attribute values
 * columns, so this entity implements {@link IdentifiableObject} directly. Translations are kept in
 * memory only, so the metadata importer can reject them for this non-translatable type. The other
 * unsupported properties are no-op stubs at the end of the class.
 */
@Entity
@Table(name = "smscommands")
@JacksonXmlRootElement(localName = "smscommand", namespace = DxfNamespaces.DXF_2_0)
public class SMSCommand implements IdentifiableObject, MetadataObject {
  public static final String WRONG_FORMAT_MESSAGE = "Wrong command format";

  public static final String MORE_THAN_ONE_ORGUNIT_MESSAGE =
      "Found more than one org unit for this number. Please specify one organisation unit";

  public static final String NO_USER_MESSAGE =
      "No user associated with this phone number. Please contact your supervisor.";

  public static final String ALERT_FEEDBACK = "Your alert message sent";

  public static final String PARAMETER_MISSING = "Mandatory parameter is missing";

  public static final String SUCCESS_MESSAGE = "Command has been processed successfully";

  public static final String NO_OU_FOR_PROGRAM =
      "Program is not assigned to user organisation unit.";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "smscommandid")
  private long id;

  @AuditAttribute
  @Column(name = "uid", nullable = false, length = 11)
  private String uid;

  @Column(name = "name")
  @Type(type = "text")
  private String name;

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastupdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @Enumerated(EnumType.STRING)
  @Column(name = "parsertype", length = 50)
  private ParserType parserType = ParserType.ALERT_PARSER;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasetid")
  private DataSet dataset;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "smscommandcodes",
      joinColumns = @JoinColumn(name = "id"),
      inverseJoinColumns = @JoinColumn(name = "codeid", unique = true))
  private Set<SMSCode> codes = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usergroupid")
  private UserGroup userGroup;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "programid")
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "programstageid")
  private ProgramStage programStage;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "smscommandspecialcharacters",
      joinColumns = @JoinColumn(name = "smscommandid"),
      inverseJoinColumns = @JoinColumn(name = "specialcharacterid", unique = true))
  private Set<SMSSpecialCharacter> specialCharacters = new HashSet<>();

  @Column(name = "currentperiodusedforreporting")
  private boolean currentPeriodUsedForReporting = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "completenessmethod")
  private CompletenessMethod completenessMethod = CompletenessMethod.AT_LEAST_ONE_DATAVALUE;

  @Column(name = "defaultmessage")
  @Type(type = "text")
  private String defaultMessage;

  /** Mapped through {@link #getReceivedMessage()}, which writes the default when none is set. */
  @Transient private String receivedMessage;

  /** Mapped through {@link #getWrongFormatMessage()}, which writes the default when none is set. */
  @Transient private String wrongFormatMessage;

  /** Mapped through {@link #getNoUserMessage()}, which writes the default when none is set. */
  @Transient private String noUserMessage;

  /**
   * Mapped through {@link #getMoreThanOneOrgUnitMessage()}, which writes the default when none is
   * set.
   */
  @Transient private String moreThanOneOrgUnitMessage;

  /** Mapped through {@link #getSuccessMessage()}, which writes the default when none is set. */
  @Transient private String successMessage;

  @Column(name = "separatorkey")
  @Type(type = "text")
  private String separator;

  @Column(name = "codeseparator")
  @Type(type = "text")
  private String codeValueSeparator;

  /**
   * Translations of this command. Not persisted; kept so the metadata importer can reject them for
   * this non-translatable type.
   */
  @Transient private Set<Translation> translations = new HashSet<>();

  /** Link to this object in the API, set when it is rendered by the web layer. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public SMSCommand(
      String name,
      ParserType parserType,
      String separator,
      DataSet dataset,
      Set<SMSCode> codes,
      String codeSeparator,
      String defaultMessage,
      UserGroup userGroup,
      String receivedMessage,
      Set<SMSSpecialCharacter> specialCharacters) {
    this.name = name;
    this.parserType = parserType;
    this.separator = separator;
    this.dataset = dataset;
    this.codes = codes;
    this.codeValueSeparator = codeSeparator;
    this.defaultMessage = defaultMessage;
    this.userGroup = userGroup;
    this.receivedMessage = receivedMessage;
    this.specialCharacters = specialCharacters;
  }

  public SMSCommand() {}

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @Override
  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  /** The display name is the name, as a command has no persisted translations. */
  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return getName();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = FALSE)
  public Date getCreated() {
    return created;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    if (translations == null) {
      translations = new HashSet<>();
    }

    return translations;
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations = translations;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
  }

  @Override
  public void setHref(String href) {
    this.href = href;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
  public void setAccess(Access access) {
    this.access = access;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlProperty(localName = "dataset", namespace = DxfNamespaces.DXF_2_0)
  public DataSet getDataset() {
    return dataset;
  }

  public void setDataset(DataSet dataset) {
    this.dataset = dataset;
  }

  @JsonProperty(value = "smsCodes")
  @JacksonXmlElementWrapper(localName = "smsCodes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "smsCode", namespace = DxfNamespaces.DXF_2_0)
  public Set<SMSCode> getCodes() {
    return codes;
  }

  public void setCodes(Set<SMSCode> codes) {
    this.codes = codes;
  }

  @JsonProperty
  @JacksonXmlProperty
  public ParserType getParserType() {
    return parserType;
  }

  public void setParserType(ParserType parserType) {
    this.parserType = parserType;
  }

  @JsonProperty
  @JacksonXmlProperty
  public boolean isCurrentPeriodUsedForReporting() {
    return currentPeriodUsedForReporting;
  }

  public void setCurrentPeriodUsedForReporting(Boolean currentPeriodUsedForReporting) {
    this.currentPeriodUsedForReporting = currentPeriodUsedForReporting;
  }

  @JsonProperty
  @JacksonXmlProperty
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "receivedmessage")
  @Type(type = "text")
  public String getReceivedMessage() {
    return receivedMessage != null ? receivedMessage : SUCCESS_MESSAGE;
  }

  public void setReceivedMessage(String receivedMessage) {
    this.receivedMessage = receivedMessage;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getSeparator() {
    return separator;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getCodeValueSeparator() {
    return codeValueSeparator;
  }

  public void setCodeValueSeparator(String codeSeparator) {
    this.codeValueSeparator = codeSeparator;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getDefaultMessage() {
    return defaultMessage;
  }

  public void setDefaultMessage(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "specialCharacters", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "specialCharacter", namespace = DxfNamespaces.DXF_2_0)
  public Set<SMSSpecialCharacter> getSpecialCharacters() {
    return specialCharacters;
  }

  public void setSpecialCharacters(Set<SMSSpecialCharacter> specialCharacters) {
    this.specialCharacters = specialCharacters;
  }

  @JsonProperty
  @JacksonXmlProperty
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "wrongformatmessage")
  @Type(type = "text")
  public String getWrongFormatMessage() {
    return wrongFormatMessage != null ? wrongFormatMessage : WRONG_FORMAT_MESSAGE;
  }

  public void setWrongFormatMessage(String wrongFormatMessage) {
    this.wrongFormatMessage = wrongFormatMessage;
  }

  @JsonProperty
  @JacksonXmlProperty
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "nousermessage")
  @Type(type = "text")
  @Nonnull
  public String getNoUserMessage() {
    return noUserMessage != null ? noUserMessage : NO_USER_MESSAGE;
  }

  public void setNoUserMessage(String noUserMessage) {
    this.noUserMessage = noUserMessage;
  }

  @JsonProperty
  @JacksonXmlProperty
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "successmessage")
  @Type(type = "text")
  public @Nonnull String getSuccessMessage() {
    return successMessage != null ? successMessage : SUCCESS_MESSAGE;
  }

  public void setSuccessMessage(String successMessage) {
    this.successMessage = successMessage;
  }

  @JsonProperty
  @JacksonXmlProperty
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "morethanoneorgunitmessage")
  @Type(type = "text")
  @Nonnull
  public String getMoreThanOneOrgUnitMessage() {
    return moreThanOneOrgUnitMessage != null
        ? moreThanOneOrgUnitMessage
        : MORE_THAN_ONE_ORGUNIT_MESSAGE;
  }

  public void setMoreThanOneOrgUnitMessage(String moreThanOneOrgUnitMessage) {
    this.moreThanOneOrgUnitMessage = moreThanOneOrgUnitMessage;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "userGroup", namespace = DxfNamespaces.DXF_2_0)
  public UserGroup getUserGroup() {
    return userGroup;
  }

  public void setUserGroup(UserGroup userGroup) {
    this.userGroup = userGroup;
  }

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlProperty(localName = "program", namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "programStage", namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  public void setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
  }

  @JsonProperty
  @JacksonXmlProperty
  public CompletenessMethod getCompletenessMethod() {
    return completenessMethod;
  }

  public void setCompletenessMethod(CompletenessMethod completenessMethod) {
    this.completenessMethod = completenessMethod;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uid", uid)
        .add("name", name)
        .add("smscodes", codes)
        .add("program", program)
        .add("parsertype", parserType)
        .add("separator", separator)
        .add("dataset", dataset)
        .add("usergroup", userGroup)
        .add("programstage", programStage)
        .toString();
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /**
   * Same real class (Hibernate proxies resolved) and the same {@code uid} and {@code name}, read
   * through the getters. The code is not compared, as a command has none.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof SMSCommand other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** SMSCommand does not support a code. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return null;
  }

  /** SMSCommand does not support a code. */
  @Override
  @Deprecated(since = "2.44")
  public void setCode(String code) {
    // Not supported
  }

  /** SMSCommand does not support last updated by. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return null;
  }

  /** SMSCommand does not support last updated by. */
  @Override
  @Deprecated(since = "2.44")
  public void setLastUpdatedBy(User lastUpdatedBy) {
    // Not supported
  }

  /** SMSCommand does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return null;
  }

  /** SMSCommand does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  public void setCreatedBy(User createdBy) {
    // Not supported
  }

  /** SMSCommand does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return null;
  }

  /** SMSCommand does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  public void setUser(User user) {
    // Not supported
  }

  /** SMSCommand does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setOwner(String owner) {
    // Not supported
  }

  /**
   * SMSCommand does not support sharing. Returns a new {@link Sharing} rather than {@link
   * Sharing#empty()} to keep the exported shape of the former in-memory default.
   */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return new Sharing();
  }

  /** SMSCommand does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** SMSCommand does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** SMSCommand does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** SMSCommand does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** SMSCommand does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
