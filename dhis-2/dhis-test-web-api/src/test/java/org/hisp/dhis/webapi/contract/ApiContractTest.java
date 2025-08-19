package org.hisp.dhis.webapi.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
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

/**
 * This test class executes tests against all contracts from the dhis2-api-contracts repo. There is
 * 1 parameterized test which iterates over all contracts found.
 */
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
      throws InvocationTargetException,
          NoSuchMethodException,
          IllegalAccessException,
          JsonProcessingException {
    assertGetRequestContract(contract);
  }

  private <T extends IdentifiableObject> void assertGetRequestContract(Contract contract)
      throws InvocationTargetException,
          NoSuchMethodException,
          IllegalAccessException,
          JsonProcessingException {
    // Given a object exists
    T identifiableObject = createTypeAndSave(contract);

    // When a GET call is made for that object
    HttpResponse response = GET(contract.requestUrl().replace("{id}", identifiableObject.getUid()));

    // Then the HTTP status code should match
    assertEquals(contract.responseStatus(), response.status().code(), "HTTP status code mismatch");

    // test string schema
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    JsonSchema testSchema = factory.getSchema(jsSchema());

    // And the response body should not have any JSON schema validation errors
    Set<ValidationMessage> errors =
        testSchema.validate(mapper.readTree(response.content().toJson()));
    assertTrue(
        errors.isEmpty(),
        () -> String.format("Valid JSON should pass schema validation, errors: %s", errors));
  }

  private <T extends IdentifiableObject> T createTypeAndSave(Contract contract)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // get type from contract
    String type = contract.name();

    // change to lowercase to use to get schema //todo change contract to have lower?
    String typeLowerCase = Character.toLowerCase(type.charAt(0)) + type.substring(1);
    Schema schema = schemaService.getSchemaBySingularName(typeLowerCase);

    // get class from schema so we can cast a created object to that type
    Class<?> klass = schema.getKlass();

    // invoke 'create' test method in TestBase to create an object to save and test against
    // it is expected that the create+'type' method exists. Create one if not.
    Method method = TestBase.class.getMethod("create" + type, char.class);
    Object createdType = method.invoke(null, 'a');
    Object createdType1 = klass.cast(createdType);
    T identifiableObject = (T) createdType1;
    manager.save(identifiableObject);
    return identifiableObject;
  }

  /**
   * Reads in contracts from a jar at classpath /contracts. Returns a set of instantiated {@link
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

  private String jsSchema() {
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
                "format": "date-time"
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
