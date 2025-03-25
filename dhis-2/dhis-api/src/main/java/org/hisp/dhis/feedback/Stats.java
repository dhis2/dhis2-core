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
package org.hisp.dhis.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * New immutable instances are returned for all operations. Make sure to use the returned instance
 * if updated stats are to be tracked correctly.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author david mackessy
 */
@JsonRootName("stats")
public record Stats(
    @JsonProperty int created,
    @JsonProperty int updated,
    @JsonProperty int deleted,
    @JsonProperty int ignored) {

  @JsonProperty
  public int getTotal() {
    return created + updated + deleted + ignored;
  }

  public Stats createdInc(int amount) {
    return new Stats(this.created + amount, this.updated, this.deleted, this.ignored);
  }

  public Stats updatedInc(int amount) {
    return new Stats(this.created, this.updated + amount, this.deleted, this.ignored);
  }

  public Stats deletedInc(int amount) {
    return new Stats(this.created, this.updated, this.deleted + amount, this.ignored);
  }

  public Stats deletedDec(int amount) {
    return new Stats(this.created, this.updated, this.deleted - amount, this.ignored);
  }

  public Stats ignoredInc(int amount) {
    return new Stats(this.created, this.updated, this.deleted, this.ignored + amount);
  }

  public Stats withStats(@Nonnull Stats stats) {
    return new Stats(
        this.created + stats.created(),
        this.updated + stats.updated(),
        this.deleted + stats.deleted(),
        this.ignored + stats.ignored());
  }

  public static Stats getAccumulatedStatsFromTypeReports(Collection<TypeReport> reports) {
    return reports.stream()
        .map(TypeReport::getStats)
        .reduce(new Stats(0, 0, 0, 0), Stats::withStats);
  }
}
