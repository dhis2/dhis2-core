package org.hisp.dhis.commons.jackson.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hisp.dhis.jsontree.JsonValue;

import java.io.IOException;

/**
 * @author Jan Bernitt
 */
public class JsonValueSerializer extends JsonSerializer<JsonValue> {

    @Override
    public void serialize(JsonValue obj, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (obj == null) {
            generator.writeNull();
        } else {
            generator.writeRawValue(obj.node().getDeclaration());
        }
    }
}
