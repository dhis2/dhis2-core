# Jackson Serializer Best Practices Analysis

## Research Summary

### Thread Safety Requirements

**Key Finding:** Jackson caches and reuses a single serializer instance per type across all threads,
so custom serializers **must be thread-safe**.

**Sources:**

* [Stack Overflow: Jackson's JsonSerializer and thread safety](https://stackoverflow.com/questions/25680728/jacksons-jsonserializer-and-thread-safety)
* Quote: "a single instance of JsonDateTimeSerializer (per type)... will be reused for all
  serializations. It must therefore be thread safe."

### Best Practices for Static vs Instance Fields

**Non-Thread-Safe Objects (e.g., SimpleDateFormat):**

* Must use `ThreadLocal` wrapper when stored as static field
* Recommended pattern:

```java
private static final ThreadLocal<SimpleDateFormat> formatter =
    new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
```

**Thread-Safe Objects (e.g., DateTimeFormatter):**

* Can be safely stored as static field or instance field
* No synchronization or ThreadLocal needed
* `DateTimeFormatter` is explicitly documented as thread-safe and immutable

### Jackson's Own Implementation

Examined Jackson's `InstantSerializer` from jackson-datatype-jsr310:

**Source:** [InstantSerializerBase.java](https://github.com/FasterXML/jackson-datatype-jsr310/blob/master/src/main/java/com/fasterxml/jackson/datatype/jsr310/ser/InstantSerializerBase.java)

**Pattern Used:**

* DateTimeFormatter stored as **instance field**: `private final DateTimeFormatter defaultFormat;`
* Passed via constructor for flexibility
* Falls back to `value.toString()` if no formatter provided

**Serialization Logic:**

```java
String str;
if (_formatter != null) {
    str = _formatter.format(value);
} else if (defaultFormat != null) {
    str = defaultFormat.format(value);
} else {
    str = value.toString();
}
generator.writeString(str);
```

## Our Implementation Analysis

### WriteInstantStdSerializer

```java
public class WriteInstantStdSerializer extends StdSerializer<Instant> {
  private static final DateTimeFormatter ISO8601_NO_TZ =
      DateTimeFormatter.ofPattern(ISO8601_NO_TZ_PATTERN).withZone(ZoneId.systemDefault());

  @Override
  public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeString(ISO8601_NO_TZ.format(value));
  }
}
```

### WriteDateStdSerializer

```java
public class WriteDateStdSerializer extends JsonSerializer<Date> {
  private static final DateTimeFormatter ISO8601_NO_TZ =
      DateTimeFormatter.ofPattern(ISO8601_NO_TZ_PATTERN).withZone(ZoneId.systemDefault());

  @Override
  public void serialize(Date date, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    generator.writeString(ISO8601_NO_TZ.format(date.toInstant()));
  }
}
```

## Comparison with Best Practices

| Practice | Requirement | Our Implementation | Status |
|----------|------------|-------------------|---------|
| Thread Safety | Serializers must be thread-safe | `DateTimeFormatter` is thread-safe | ✅ Pass |
| Static Field Usage | OK for thread-safe objects | Using static final `DateTimeFormatter` | ✅ Pass |
| Immutability | Prefer immutable objects | `DateTimeFormatter` is immutable | ✅ Pass |
| Simplicity | Keep serialization logic simple | Single line: `gen.writeString(formatter.format(value))` | ✅ Pass |
| No Shared Mutable State | Avoid instance fields unless final | No mutable instance fields | ✅ Pass |

## Alternative: Instance Field Pattern

Jackson's own serializers use instance fields for flexibility:

**Pros:**

* Allows different formatter per serializer instance
* More flexible for configuration

**Cons:**

* Slightly more memory per serializer instance (negligible - single instance per type)
* More complex constructor

**Our Choice:**

* Static field is appropriate here because:
  * Format is fixed (`yyyy-MM-dd'T'HH:mm:ss.SSS`)
  * No need for per-instance customization
  * Simpler code
  * Standard practice for fixed formatters

## Conclusion

**Our implementation follows Jackson best practices:**

1. ✅ Thread-safe: Uses `DateTimeFormatter` which is thread-safe
2. ✅ Proper field usage: Static final for immutable, thread-safe formatter
3. ✅ Simple serialization: Direct `gen.writeString()` call
4. ✅ No shared mutable state
5. ✅ Extends appropriate base class (`StdSerializer`)

**Difference from Jackson's pattern:** We use static field instead of instance field, which is
appropriate for our fixed-format use case and is a common pattern in the community.
