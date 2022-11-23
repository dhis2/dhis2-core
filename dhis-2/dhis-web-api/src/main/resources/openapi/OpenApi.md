# README

## Difficulties 
A list of inherent complexities that need to be solved to generate a satisfying OpenAPI configuration.

This should also help to understand the steps the implementation makes in generating the configuration.

### Unique Short Names
The names used for named objects (`components`) which can be referenced by name in the configuration
all need to be globally unique. On the other hand names should be as short as possible for easy
reading in the resulting OpenAPI documentation.

Unfortunately the simple names of a `Class` are not always unique within the DHIS2 codebase.

The solution:
* Names are chosen as late as possible during the configuration generation, prioritising those referenced directly
  within the `paths` endpoint descriptions.
* If `Class.getSimpleName()` is available it is used and occupied, otherwise `Class.getCanonicalName()` is used
  (where our base package `org.hisp.dhis` is cut off).

### Alphabetical Sorting
What makes alphabetical sorting of `schemas` difficult is that the set of actually used schemas and the name
they get is also first determined while generating the OpenAPI configuration from the `Api` model.

In addition, the `Ref` "subtypes" are not actual Java types but only exist within the `schemas` section of the
OpenAPI configuration. They are generated from a `Api.Schema` with a `source` of `Ref.class`. The `hint` points
to the referenced type. 

The solution:
* Rely on `Api.getSchemas()` to determine all named schemas (might yield some "unused", that means not referenced in 
  the OpenAPI configuration)
* Collect all schemas originating from a `Ref` separately and write them after all "normal" types have been processed.
  This is necessary to make sure that no more so far unused reference types are "discovered" after starting to write
  any of the reference types. This causes them to be listed after all "normal" types which is also good for readability.

### Endpoint Mismatch
The gist is that Java can express more complex mapping than OpenAPI can model.

In Java, it is quite usual to have multiple methods for the same path, each handling a different media type.
This means in theory each of these can have a completely different set of input parameters.

In OpenAPI a path description has an operation for each HTTP method, each operation has a set of parameters
and possibly a number of different responses both by response status and used media type.
What cannot be expressed is for the same path and same HTTP method to have different parameters.

The solution:
* The `Api` model created during the analysis step reflects what is present in Java
* During OpenAPI configuration generation endpoints are grouped according to OpenAPI needs. 
  When `Api.Endpoint`s "clash" (same path and HTTP method) they are merged into one "synthetic" one.
  This is an imperfect process where some information might get lost or misrepresent one of the actual operations.
* Merge favours the endpoints responsible for JSON handling

### Avoiding Repetition
To keep the size of the generated OpenAPI configuration down any repetition is best avoided.
OpenAPI has the `components` where named `schemas` but also parameters, requests and responses can be detailed once
so that they can be referenced by named in the `paths` section of the configuration.

In Java such reoccurring elements usually occur as the same Java type.
The big question becomes which Java type should become a named type in the OpenAPI configuration.

The solution:
* Any JRE type is considered simple (unnamed)
* Any `enum` type is considered simple (unnamed)
* Any array type is considered unnamed (elements might be of a named type)
* Any `Collection` type is transformed to an array type (and thereby unnamed, again elements might be named)
* `Map`s are unnamed types with special handling (values can be named, keys can only be "strings")
* Both `Ref` and `Unknown` types are unnamed 
* Any other type is named
* Simple types and their mapping into OpenAPI schema equivalents are expected to be known by the generator

### Generic Types

### Polymorphic Types

### JSON Tree Types

### Directly Reading/Writing HTTP Request/Response

### Invisible Defaults