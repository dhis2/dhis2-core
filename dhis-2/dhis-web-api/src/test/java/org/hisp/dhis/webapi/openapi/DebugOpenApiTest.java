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

import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.webapi.controller.dataelement.DataElementOperandController;
import org.hisp.dhis.webapi.controller.organisationunit.OrganisationUnitController;
import org.hisp.dhis.webapi.openapi.Api.Parameter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Test class to help debug OpenAPI parameter generation issues. Enable for debugging only. */
@Disabled
class DebugOpenApiTest {

  @Test
  void debugParameterGeneration() {
    // Create API scope with problematic controllers
    Set<Class<?>> controllers =
        Set.of(OrganisationUnitController.class, DataElementOperandController.class);
    Api.Scope scope = new Api.Scope(controllers, Map.of(), controllers);

    // Extract API
    Api api = ApiExtractor.extractApi(scope, new ApiExtractor.Configuration(false));

    System.out.println("=== SHARED PARAMETERS ===");
    api.getComponents()
        .getParameters()
        .forEach(
            (clazz, parameters) -> {
              System.out.println("Class: " + clazz.getSimpleName());
              parameters.forEach(
                  param -> {
                    System.out.println(
                        "  - "
                            + param.getSharedName().getValue()
                            + " (name: "
                            + param.getName()
                            + ")");
                  });
            });

    System.out.println("\n=== ENDPOINTS AND THEIR PARAMETERS ===");
    api.getControllers()
        .forEach(
            controller -> {
              System.out.println("Controller: " + controller.getSource().getSimpleName());
              controller
                  .getEndpoints()
                  .forEach(
                      endpoint -> {
                        if (!endpoint.getParameters().isEmpty()) {
                          System.out.println(
                              "  Endpoint: " + endpoint.getName() + " " + endpoint.getPaths());
                          endpoint
                              .getParameters()
                              .forEach(
                                  (name, param) -> {
                                    String sharedName = param.getSharedName().getValue();
                                    System.out.println(
                                        "    - "
                                            + name
                                            + (sharedName != null
                                                ? " (shared: " + sharedName + ")"
                                                : " (local)"));
                                  });
                        }
                      });
            });

    System.out.println("\n=== CHECKING FOR DUPLICATES ===");
    api.getControllers()
        .forEach(
            controller -> {
              controller
                  .getEndpoints()
                  .forEach(
                      endpoint -> {
                        Map<String, List<Parameter>> paramsByName =
                            endpoint.getParameters().values().stream()
                                .collect(groupingBy(Api.Parameter::getName));

                        paramsByName.forEach(
                            (name, params) -> {
                              if (params.size() > 1) {
                                System.out.println(
                                    "DUPLICATE in "
                                        + controller.getSource().getSimpleName()
                                        + "."
                                        + endpoint.getName()
                                        + ": "
                                        + name
                                        + " appears "
                                        + params.size()
                                        + " times");
                                params.forEach(
                                    p -> {
                                      String sharedName = p.getSharedName().getValue();
                                      System.out.println(
                                          "  - "
                                              + (sharedName != null
                                                  ? "shared: " + sharedName
                                                  : "local"));
                                    });
                              }
                            });
                      });
            });

    System.out.println("\n=== CHECKING FOR MISSING REFERENCES ===");
    Set<String> availableSharedParams = new HashSet<>();
    api.getComponents()
        .getParameters()
        .forEach(
            (clazz, parameters) -> {
              parameters.forEach(
                  param -> {
                    if (param.getSharedName().getValue() != null) {
                      availableSharedParams.add(param.getSharedName().getValue());
                    }
                  });
            });

    api.getControllers()
        .forEach(
            controller -> {
              controller
                  .getEndpoints()
                  .forEach(
                      endpoint -> {
                        endpoint
                            .getParameters()
                            .forEach(
                                (name, param) -> {
                                  String sharedName = param.getSharedName().getValue();
                                  if (sharedName != null
                                      && !availableSharedParams.contains(sharedName)) {
                                    System.out.println(
                                        "MISSING REFERENCE in "
                                            + controller.getSource().getSimpleName()
                                            + "."
                                            + endpoint.getName()
                                            + ": references "
                                            + sharedName
                                            + " but it doesn't exist");
                                  }
                                });
                      });
            });
  }

  @Test
  void debugGetObjectListParamsProperties() {
    System.out.println("=== GetObjectListParams Properties ===");
    Collection<Property> properties = Property.getProperties(GetObjectListParams.class);
    properties.forEach(
        prop -> {
          System.out.println(
              "Property: "
                  + prop.getName()
                  + " (declaring class: "
                  + prop.getDeclaringClass().getSimpleName()
                  + ", field: "
                  + prop.getSource()
                  + ")");
        });

    System.out.println("\n=== GetOrganisationUnitObjectListParams Properties ===");
    properties =
        Property.getProperties(
            OrganisationUnitController.GetOrganisationUnitObjectListParams.class);
    properties.forEach(
        prop -> {
          System.out.println(
              "Property: "
                  + prop.getName()
                  + " (declaring class: "
                  + prop.getDeclaringClass().getSimpleName()
                  + ", field: "
                  + prop.getSource()
                  + ")");
        });
  }
}
