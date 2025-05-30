/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.e2e.dependsOn;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.test.e2e.dependsOn.services.IndicatorService;
import org.hisp.dhis.test.e2e.dependsOn.services.ProgramIndicatorService;
import org.hisp.dhis.test.e2e.dependsOn.services.ResourceService;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@Slf4j
public class DependsOnExtension
    implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NS =
      ExtensionContext.Namespace.create(DependsOnExtension.class.getName());
  private final Map<ResourceType, org.hisp.dhis.test.e2e.dependsOn.services.ResourceService>
      services =
          Map.of(
              ResourceType.PROGRAM_INDICATOR, new ProgramIndicatorService(),
              ResourceType.INDICATOR, new IndicatorService());

  @Override
  public void beforeEach(ExtensionContext ctx) {
    List<Resource> created = new ArrayList<>();

    Method testMethod = ctx.getRequiredTestMethod();
    DependsOn depends = testMethod.getAnnotation(DependsOn.class);
    if (depends == null) {
      // Nothing to do for this test
      return;
    }

    for (String file : depends.files()) {
      String resourcePath = "dependencies/" + file;
      log.info("Processing dependency file '{}'", resourcePath);

      DependencyFile df = JsonDependencyLoader.load(resourcePath);
      ResourceService svc = services.get(df.type());
      if (svc == null) {
        throw new DependencySetupException("No service for " + df.type());
      }
      // Get the code from the json payload
      String code = df.payload().path("code").asText();
      Optional<String> foundUid = svc.lookup(code);
      if (foundUid.isPresent()) {
        log.info("{} with code='{}' exists – skipping", df.type(), code);
        created.add(new Resource(df.type(), foundUid.get()));
        continue;
      }
      String uid = svc.create(df.payload());
      created.add(new Resource(df.type(), uid));
    }
    // Store created UIDs so afterEach can delete them if requested
    ctx.getStore(NS).put(OperationType.CREATE, created);
    ctx.getStore(NS).put(OperationType.DELETE, depends.delete());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterEach(ExtensionContext ctx) {
    Boolean delete = ctx.getStore(NS).remove(OperationType.DELETE, Boolean.class);
    if (delete == null || !delete) return;

    List<Resource> created = ctx.getStore(NS).remove(OperationType.CREATE, List.class);
    if (created == null) return;

    for (Resource cr : created) {
      services.get(cr.type()).delete(cr.uid());
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext pc, ExtensionContext ec)
      throws ParameterResolutionException {
    return pc.getParameter().getType().equals(List.class)
        && pc.getParameter()
            .getParameterizedType()
            .getTypeName()
            .contains(Resource.class.getName());
  }

  @Override
  public Object resolveParameter(ParameterContext pc, ExtensionContext ec)
      throws ParameterResolutionException {
    return ec.getStore(NS).getOrDefault(OperationType.CREATE, List.class, List.of());
  }
}
