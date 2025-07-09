package org.hisp.dhis.webapi.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CategoryContractTest extends H2ControllerIntegrationTestBase {

  private Category category;
  ObjectMapper mapper = new ObjectMapper();

  @Autowired private CategoryService categoryService;

  @Test
  void testCategoryJsonValidation() throws Exception {
    // create category
    category = createCategory('b');
    category.setCode("code1");
    category.setDescription("desc1");
    categoryService.addCategory(category);

    // get json schema
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    JsonSchema schema =
        factory.getSchema(getClass().getResourceAsStream("/contract/category-json-schema.json"));

    // get API response
    JsonNode content = mapper.readTree(GET("/categories/" + category.getUid()).content().toJson());
    Set<ValidationMessage> errors = schema.validate(content);
    errors.forEach(System.out::println);
    assertTrue(errors.isEmpty(), "Valid JSON should pass validation");
  }
}
