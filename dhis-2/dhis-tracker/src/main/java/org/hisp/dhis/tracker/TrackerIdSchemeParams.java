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
package org.hisp.dhis.tracker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;

/**
 * IdScheme parameter for tracker import and export.
 *
 * <p>The default idScheme is {@link TrackerIdSchemeParam#UID}. The idScheme used for all metadata
 * defaults to the value specified in the {@code idScheme} field. This can be overridden by
 * metadata-specific fields, which take precedence over the general {@code idScheme} field.
 *
 * @author Stian Sandvold
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerIdSchemeParams implements Serializable {
  /** IdScheme used for all metadata unless overridden by a metadata specific parameter. */
  @JsonProperty @Builder.Default private TrackerIdSchemeParam idScheme = TrackerIdSchemeParam.UID;

  /** Specific idScheme to match data elements on. */
  @JsonIgnore private TrackerIdSchemeParam dataElementIdScheme;

  /** Specific idScheme to match organisation units on. */
  @JsonIgnore private TrackerIdSchemeParam orgUnitIdScheme;

  /** Specific idScheme to match programs on. */
  @JsonIgnore private TrackerIdSchemeParam programIdScheme;

  /** Specific idScheme to match program stages on. */
  @JsonIgnore private TrackerIdSchemeParam programStageIdScheme;

  /** Specific idScheme to match category option combos on. */
  @JsonIgnore private TrackerIdSchemeParam categoryOptionComboIdScheme;

  /** Specific idScheme to match category options on. */
  @JsonIgnore private TrackerIdSchemeParam categoryOptionIdScheme;

  @JsonProperty("dataElementIdScheme")
  public void setDataElementIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.dataElementIdScheme = idSchemeParam;
  }

  @JsonProperty("dataElementIdScheme")
  @Nonnull
  public TrackerIdSchemeParam getDataElementIdScheme() {
    return dataElementIdScheme != null ? dataElementIdScheme : idScheme;
  }

  @JsonProperty("orgUnitIdScheme")
  public void setOrgUnitIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.orgUnitIdScheme = idSchemeParam;
  }

  @JsonProperty("orgUnitIdScheme")
  @Nonnull
  public TrackerIdSchemeParam getOrgUnitIdScheme() {
    return orgUnitIdScheme != null ? orgUnitIdScheme : idScheme;
  }

  @JsonProperty("programIdScheme")
  public void setProgramIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.programIdScheme = idSchemeParam;
  }

  @JsonProperty("programIdScheme")
  @Nonnull
  public TrackerIdSchemeParam getProgramIdScheme() {
    return programIdScheme != null ? programIdScheme : idScheme;
  }

  @JsonProperty("programStageIdScheme")
  public void setProgramStageIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.programStageIdScheme = idSchemeParam;
  }

  @JsonProperty("programStageIdScheme")
  @Nonnull
  public TrackerIdSchemeParam getProgramStageIdScheme() {
    return programStageIdScheme != null ? programStageIdScheme : idScheme;
  }

  @JsonProperty("categoryOptionComboIdScheme")
  public void setCategoryOptionComboIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.categoryOptionComboIdScheme = idSchemeParam;
  }

  @JsonProperty("categoryOptionComboIdScheme")
  public TrackerIdSchemeParam getCategoryOptionComboIdScheme() {
    return categoryOptionComboIdScheme != null ? categoryOptionComboIdScheme : idScheme;
  }

  @JsonProperty("categoryOptionIdScheme")
  public void setCategoryOptionIdScheme(TrackerIdSchemeParam idSchemeParam) {
    this.categoryOptionIdScheme = idSchemeParam;
  }

  @JsonProperty("categoryOptionIdScheme")
  @Nonnull
  public TrackerIdSchemeParam getCategoryOptionIdScheme() {
    return categoryOptionIdScheme != null ? categoryOptionIdScheme : idScheme;
  }

  /**
   * Get the identifier using the matching {@code idScheme}. Defaults to {@link #getIdScheme()} if
   * the metadata has no dedicated {@code idScheme} parameter.
   */
  public <T extends IdentifiableObject & MetadataObject> String getIdentifier(T metadata) {
    if (metadata instanceof DataElement dataElement) {
      return getDataElementIdScheme().getIdentifier(dataElement);
    } else if (metadata instanceof OrganisationUnit orgUnit) {
      return getOrgUnitIdScheme().getIdentifier(orgUnit);
    } else if (metadata instanceof Program program) {
      return getProgramIdScheme().getIdentifier(program);
    } else if (metadata instanceof ProgramStage programStage) {
      return getProgramStageIdScheme().getIdentifier(programStage);
    } else if (metadata instanceof CategoryOptionCombo categoryOptionCombo) {
      return getCategoryOptionComboIdScheme().getIdentifier(categoryOptionCombo);
    } else if (metadata instanceof CategoryOption categoryOption) {
      return getCategoryOptionIdScheme().getIdentifier(categoryOption);
    }
    return idScheme.getIdentifier(metadata);
  }

  public TrackerIdSchemeParam getByClass(Class<?> klazz) {
    return switch (klazz.getSimpleName()) {
      case "CategoryOptionCombo" -> getCategoryOptionComboIdScheme();
      case "OrganisationUnit" -> getOrgUnitIdScheme();
      case "CategoryOption" -> getCategoryOptionIdScheme();
      case "DataElement" -> getDataElementIdScheme();
      case "Program" -> getProgramIdScheme();
      case "ProgramStage" -> getProgramStageIdScheme();
      default -> getIdScheme();
    };
  }

  /**
   * Creates metadata identifier for given {@code metadata} using {@link #idScheme}. For more
   * details refer to {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param metadata to create metadata identifier for
   * @return metadata identifier representing metadata using the idScheme
   */
  public MetadataIdentifier toMetadataIdentifier(IdentifiableObject metadata) {
    return getIdScheme().toMetadataIdentifier(metadata);
  }

  /**
   * Creates metadata identifier for given {@code categoryOptionCombo} using {@link
   * #categoryOptionComboIdScheme}. For more details refer to {@link
   * TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param categoryOptionCombo to create metadata identifier for
   * @return metadata identifier representing metadata using the categoryOptionComboIdScheme
   */
  public MetadataIdentifier toMetadataIdentifier(CategoryOptionCombo categoryOptionCombo) {
    return getCategoryOptionComboIdScheme().toMetadataIdentifier(categoryOptionCombo);
  }

  /**
   * Creates metadata identifier for given {@code categoryOption} using {@link
   * #categoryOptionIdScheme}. For more details refer to {@link
   * TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param categoryOption to create metadata identifier for
   * @return metadata identifier representing metadata using the categoryOptionIdScheme
   */
  public MetadataIdentifier toMetadataIdentifier(CategoryOption categoryOption) {
    return getCategoryOptionIdScheme().toMetadataIdentifier(categoryOption);
  }

  /**
   * Creates metadata identifier for given {@code dataElement} using {@link #dataElementIdScheme}.
   * For more details refer to {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param dataElement to create metadata identifier for
   * @return metadata identifier representing dataElement using the idScheme
   */
  public MetadataIdentifier toMetadataIdentifier(DataElement dataElement) {
    return getDataElementIdScheme().toMetadataIdentifier(dataElement);
  }

  /**
   * Creates metadata identifier for given {@code orgUnit} using {@link #orgUnitIdScheme}. For more
   * details refer to {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param orgUnit to create metadata identifier for
   * @return metadata identifier representing metadata using the orgUnitIdScheme
   */
  public MetadataIdentifier toMetadataIdentifier(OrganisationUnit orgUnit) {
    return getOrgUnitIdScheme().toMetadataIdentifier(orgUnit);
  }

  /**
   * Creates metadata identifier for given {@code program} using {@link #programIdScheme}. For more
   * details refer to {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param program to create metadata identifier for
   * @return metadata identifier representing metadata using the programIdScheme
   */
  public MetadataIdentifier toMetadataIdentifier(Program program) {
    return getProgramIdScheme().toMetadataIdentifier(program);
  }

  /**
   * Creates metadata identifier for given {@code programStage} using {@link #programStageIdScheme}.
   * For more details refer to {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
   *
   * @param programStage to create metadata identifier for
   * @return metadata identifier representing metadata using the programStageIdScheme
   */
  public MetadataIdentifier toMetadataIdentifier(ProgramStage programStage) {
    return getProgramStageIdScheme().toMetadataIdentifier(programStage);
  }
}
