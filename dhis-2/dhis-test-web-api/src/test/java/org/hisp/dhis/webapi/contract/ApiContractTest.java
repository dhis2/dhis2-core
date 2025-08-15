package org.hisp.dhis.webapi.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
class ApiContractTest extends H2ControllerIntegrationTestBase {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private SchemaService schemaService;

  @ParameterizedTest(name = "{0} test")
  @MethodSource("getContracts")
  @DisplayName("Test API contracts")
  void contractTest(Contract contract)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    assertGetRequestContract(contract);
  }

  private void assertGetRequestContract(Contract contract)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    // create type
    IdentifiableObject identifiableObject = createTypeAndSave(contract);

    // make GET call for type
    HttpResponse response = GET(contract.requestUrl().replace("{id}", identifiableObject.getUid()));

    // assert HTTP status code
    assertEquals(contract.responseStatus(), response.status().code(), "HTTP status code mismatch");

    // assert response against json schema
    try {
      String json = response.content().toJson();
      Set<ValidationMessage> errors = contract.jsonSchema().validate(mapper.readTree(json));
      assertTrue(errors.isEmpty(), "Valid JSON should pass schema validation, errors: " + errors);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private IdentifiableObject createTypeAndSave(Contract contract)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // get type from contract
    String type = contract.name();

    // change to lowercase to use to get schema
    String typeLowerCase = Character.toLowerCase(type.charAt(0)) + type.substring(1);
    Schema schema = schemaService.getSchemaBySingularName(typeLowerCase);

    // get class from schema so we can cast a created object to that type
    Class<?> klass = schema.getKlass();

    // invoke 'create' test method in TestBase to create an object to save and test against
    // it is expected that the create+'type' method exists. Create one if none exist.
    Method method = TestBase.class.getMethod("create" + type, char.class);
    Object createdType = method.invoke(null, 'a');
    IdentifiableObject identifiableObject = (IdentifiableObject) klass.cast(createdType);
    manager.save(identifiableObject);
    return identifiableObject;
  }

  /**
   * Reads in contracts from a jar at class path /contracts. Returns a set of instantiated {@link
   * Contract}s.
   *
   * @return set of instantiated {@link * Contract}s.
   * @throws URISyntaxException URISyntaxException
   */
  private static Set<Contract> getContracts() throws URISyntaxException {
    Set<Contract> contracts = new HashSet<>();

    URI uri =
        Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("contracts"))
            .toURI();

    // impl for jar
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      Path path = fileSystem.getPath("/contracts");

      try (Stream<Path> paths = Files.walk(path)) {
        paths
            .filter(Files::isRegularFile)
            .filter(
                path1 ->
                    path1.toString().endsWith(".json") && path1.toString().contains("-contract"))
            .forEach(
                filePath -> {
                  try {
                    String jsonContent = Files.readString(filePath);
                    JsonNode jsonNode = mapper.readTree(jsonContent);

                    Class<?> clazz;
                    clazz = Class.forName("org.hisp.dhis.webapi.contract.Contract");
                    contracts.add((Contract) mapper.treeToValue(jsonNode, clazz));
                  } catch (Exception e) {
                    log.error(e.getMessage());
                  }
                });
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    }
    return contracts;
  }

  private String categoryJsonSchema() {
    return """
            {
              "type": "object",
              "properties": {
                "code": {
                  "type": "string"
                },
                "dataDimensionType": {
                  "type": "string",
                  "enum": [
                    "DISAGGREGATION",
                    "ATTRIBUTE"
                  ],
                  "default": "DISAGGREGATION"
                },
                "id": {
                  "type": "string"
                },
                "displayName": {
                  "type": "string"
                },
                "created": {
                  "type": "string",
                  "format": "date"
                },
                "createdBy": {
                  "type": "object",
                  "properties": {
                    "id": {
                      "type": "string"
                    },
                    "name": {
                      "type": "string"
                    },
                    "code": {
                      "type": [
                        "string",
                        "null"
                      ]
                    },
                    "displayName": {
                      "type": "string"
                    },
                    "username": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "name",
                    "code",
                    "displayName",
                    "username"
                  ],
                  "additionalProperties": false
                },
                "href": {
                  "type": "string",
                  "format": "uri"
                },
                "lastUpdated": {
                  "type": "string",
                  "format": "date-time"
                },
                "lastUpdatedBy": {
                  "$ref": "#/properties/createdBy"
                },
                "sharing": {
                  "type": "object",
                  "properties": {
                    "public": {
                      "type": "string",
                      "const": "rw------"
                    }
                  },
                  "required": [
                    "public"
                  ],
                  "additionalProperties": false
                },
                "access": {
                  "type": "object",
                  "properties": {
                    "delete": {
                      "type": "boolean"
                    },
                    "externalize": {
                      "type": "boolean"
                    },
                    "manage": {
                      "type": "boolean"
                    },
                    "read": {
                      "type": "boolean"
                    },
                    "update": {
                      "type": "boolean"
                    },
                    "write": {
                      "type": "boolean"
                    },
                    "data": {
                      "type": "object",
                      "properties": {
                        "read": {
                          "type": "boolean"
                        },
                        "write": {
                          "type": "boolean"
                        }
                      },
                      "required": [
                        "read",
                        "write"
                      ],
                      "additionalProperties": false
                    }
                  },
                  "required": [
                    "delete",
                    "externalize",
                    "manage",
                    "read",
                    "update",
                    "write"
                  ],
                  "additionalProperties": false
                },
                "displayShortName": {
                  "type": "string"
                }
              },
              "required": [
                "id",
                "displayName",
                "created",
                "createdBy",
                "href",
                "lastUpdated",
                "sharing",
                "access",
                "displayShortName"
              ],
              "additionalProperties": false
            }
            """;
  }
}
