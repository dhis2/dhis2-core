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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * In-memory representation of a data integrity check.
 *
 * <p>Alongside its informative fields it has a {@link Function} that given the check produces the
 * {@link DataIntegritySummary} and one that produces the {@link DataIntegrityDetails} of the check.
 *
 * <p>If a check does not support one or the other of the two check types the {@link Function}
 * returns {@code null}.
 *
 * @author Jan Bernitt
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataIntegrityCheck implements Serializable {
  @JsonProperty private final String name;

  @JsonProperty private final String displayName;

  @JsonProperty private final String section;

  @JsonProperty private final int sectionOrder;

  @JsonProperty private final DataIntegritySeverity severity;

  @JsonProperty private final String description;

  @JsonProperty private final String introduction;

  @JsonProperty private final String recommendation;

  @JsonProperty private final String issuesIdType;

  @JsonProperty private final boolean isSlow;

  @JsonProperty
  public String getCode() {
    return getCodeFromName(name);
  }

  private final transient Function<DataIntegrityCheck, DataIntegritySummary> runSummaryCheck;

  private final transient Function<DataIntegrityCheck, DataIntegrityDetails> runDetailsCheck;

  /**
   * Method that takes in a name of a {@link DataIntegrityCheck} and converts it to an acronym of
   * its name using the first letter from each word e.g. my_data_integrity_check -> MDIC
   */
  public static String getCodeFromName(@Nonnull String fullName) {
    return Stream.of(fullName.split("_"))
        .map(word -> String.valueOf(word.charAt(0)).toUpperCase())
        .collect(joining());
  }
}
