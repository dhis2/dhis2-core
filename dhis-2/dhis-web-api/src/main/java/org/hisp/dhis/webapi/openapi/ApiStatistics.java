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
package org.hisp.dhis.webapi.openapi;

import java.util.List;
import java.util.Map;

/**
 * @param classifications all classifications available
 * @param full the entire API with no filters
 * @param partial the API currently shown
 */
public record ApiStatistics(ApiClassifications classifications, Api full, Api partial) {

  ApiStatistics(Api full, Api partial) {
    this(ApiClassifications.of(full.getScope().controllers()), full, partial);
  }

  record Ratio(String name, int count, int total, int percentage) {
    Ratio(String name, int count, int total) {
      this(name, count, total, 100 * count / total);
    }
  }

  List<Ratio> compute() {
    return List.of(
        new Ratio("operations", operations(partial), operations(full)),
        new Ratio("schemas", schemas(partial), schemas(full)),
        new Ratio("parameters", parameters(partial), parameters(full)));
  }

  private static int operations(Api in) {
    return in.getEndpoints().values().stream().mapToInt(Map::size).sum();
  }

  private static int schemas(Api in) {
    return in.getComponents().getSchemas().size();
  }

  private static int parameters(Api in) {
    return in.getComponents().getParameters().values().stream().mapToInt(List::size).sum();
  }
}
