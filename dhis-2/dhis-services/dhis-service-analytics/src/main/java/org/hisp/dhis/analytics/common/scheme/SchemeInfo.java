/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.common.scheme;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * This class acts as container, centralizing all required objects and settings related to schemes.
 *
 * @author maikel arabori
 */
public record SchemeInfo(Settings settings, Data data) {

  @lombok.Data
  @Builder(toBuilder = true)
  public static class Settings {
    /**
     * Scheme to be used for data, more specifically data elements and attributes which have an
     * option set or legend set, e.g. return the name of the option instead of the code, or the name
     * of the legend instead of the legend ID, in the response.
     */
    private final IdScheme dataIdScheme;

    /** The general identifier scheme, which drives the values in the query response. */
    private final IdScheme outputIdScheme;

    /**
     * The identifier scheme specific for data items, including indicators, data elements and
     * program indicators.
     */
    private final IdScheme outputDataItemIdScheme;

    /** The identifier scheme specific for data elements. */
    private final IdScheme outputDataElementIdScheme;

    /** The identifier scheme specific for org units. */
    private final IdScheme outputOrgUnitIdScheme;

    /** The output format. */
    private final OutputFormat outputFormat;

    /** Indicates whether this query defines an identifier scheme different from UID. */
    public boolean isGeneralOutputIdSchemeSet() {
      return outputIdScheme != null && !IdScheme.UID.equals(outputIdScheme);
    }

    /** Indicates whether this query defines an identifier scheme different from UID. */
    public boolean isDataIdSchemeSet() {
      return dataIdScheme != null && !IdScheme.UID.equals(dataIdScheme);
    }

    public boolean isOutputDataItemIdSchemeSet() {
      return outputDataItemIdScheme != null && !IdScheme.UID.equals(outputDataItemIdScheme);
    }

    /** Indicates whether this query defines an identifier scheme different from UID. */
    public boolean isOutputDataElementIdSchemeSet() {
      return outputDataElementIdScheme != null && !IdScheme.UID.equals(outputDataElementIdScheme);
    }

    /** Indicates whether this query defines an identifier scheme different from UID. */
    public boolean isOutputOrgUnitIdSchemeSet() {
      return outputOrgUnitIdScheme != null && !IdScheme.UID.equals(outputOrgUnitIdScheme);
    }

    /** Indicates whether a non-default identifier scheme is specified. */
    public boolean hasCustomIdSchemeSet() {
      return isGeneralOutputIdSchemeSet()
          || isOutputDataItemIdSchemeSet()
          || isOutputDataElementIdSchemeSet()
          || isOutputOrgUnitIdSchemeSet();
    }

    /** Indicates whether the given output format specified. */
    public boolean isOutputFormat(OutputFormat format) {
      return this.outputFormat != null && this.outputFormat == format;
    }
  }

  @lombok.Data
  @Builder(toBuilder = true)
  public static class Data {
    private final Program program;
    private final ProgramStage programStage;
    private final Set<DimensionalItemObject> dimensionalItemObjects;
    private final List<DimensionalItemObject> dataElements;
    private final List<DimensionalItemObject> indicators;
    private final List<DimensionalItemObject> programIndicators;
    private final List<DimensionalItemObject> dataElementOperands;
    private final List<DimensionalItemObject> organizationUnits;
    private final Set<Option> options;

    // Multi programs.
    private final List<Program> programs;
    private final Set<ProgramStage> programStages;
  }
}
