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
package org.hisp.dhis.helpers.extensions;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.io.File;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.EndpointTracker;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class CoverageLoggerExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private static Logger logger = Logger.getLogger(CoverageLoggerExtension.class.getName());

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    context.getRoot().getStore(GLOBAL).put("CoverageLoggerExtension", this);
  }

  @Override
  public void close() throws Throwable {
    if (!CollectionUtils.isEmpty(EndpointTracker.getCoverageList())) {
      logger.info("Writing coverage information");
      File csvOutputFile = new File("coverage.csv");
      try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
        EndpointTracker.getCoverageList().stream()
            .filter(
                p ->
                    !p.getMethod().equalsIgnoreCase("DELETE")
                        && !p.getUrl().contains("/tracker//jobs"))
            .map(this::toCsvRow)
            .forEach(pw::println);
      }
    }
  }

  public String toCsvRow(EndpointTracker.Coverage coverage) {
    return Stream.of(coverage.getMethod(), coverage.getUrl(), coverage.getOccurrences().toString())
        .map(value -> value.replaceAll("\"", "\"\""))
        .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
        .collect(Collectors.joining(","));
  }
}
