package org.hisp.dhis.webapi.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CategoryContractTest extends H2ControllerIntegrationTestBase {

  private Category category;
  static ObjectMapper mapper = new ObjectMapper();

  @Autowired private IdentifiableObjectManager manager;

  @BeforeAll
  public void beforeAll() {
    category = createCategory('b');
    category.setCode("code1");
    category.setDescription("desc1");
    category.setAggregationType(AggregationType.COUNT);
    manager.save(category);
  }

  @ParameterizedTest(name = "{0} test")
  @MethodSource("getContracts")
  @DisplayName("Test API contracts")
  void contractTest(Contract contract) {
    switch (contract.httpMethod()) {
      case GET -> assertGetContract(contract, category.getUid());
      case POST -> {}
    }
  }

  private void assertGetContract(Contract contract, String resourceUid) {
    HttpResponse response = GET(contract.requestUrl().replace("{id}", resourceUid));

    // assert HTTP status code
    assertEquals(contract.responseStatus(), response.status().code(), "HTTP status code mismatch");

    // assert response against json schema
    try {
      Set<ValidationMessage> errors =
          contract.jsonSchema().validate(mapper.readTree(response.content().toJson()));
      //      errors.forEach(System.out::println);
      assertTrue(errors.isEmpty(), "Valid JSON should pass schema validation, errors: " + errors);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  private static Set<Contract> getContracts() throws Exception {
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
                    System.err.println(e.getMessage());
                  }
                });
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    return contracts;
  }
}
