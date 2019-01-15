
## Tech

 - [rest-assured](http://rest-assured.io)  - provides rest client, handles logging and validation;
 - [jUnit 5](https://junit.org/junit5/);
 - [docker-compose](https://docs.docker.com/compose/) - defines and runs dhis2 multi-container environment;
## Test execution
#### Required properties

 - baseUrl - points to API under test url. 
Example: https://play.dhis2.org/dev/api

#### Local dev environment
In order to test local version of dhis2 - use docker compose to start dhis2-db and dhis2-web containers. To do that: 

 1. cd dhis-e2e-test
 2. docker-compose up -d
 3. run tests with property baseUrl set to "http://localhost:8070/api"
  
#### Test clean up 
After every test class, created data will be cleaned up starting from latest created object to avoid as much references as possible. 

If more controlled cleanup order is required - it can be explicitly specified. Just call one of the methods in TestCleanUp class. 
*Example: testCleanUp.deleteCreatedEntities("/users", "/dataElements")*
 
 #### Debugging
 Logs can be retrieved from both dhis2-web and dhis2-db containers.
 
 > docker logs < containerId >
 
 To get container id: 
 
 >$ docker ps*
 
  ## Creating tests
  #### Actions
 
 For convenience, every REST endpoint should be represented by object of type RestApiActions. RestApiActions class will provide way of sending different types of requests and will control keeping track of created or deleted data.
 
 *Examples*: 
 1) endpoint that doesn't require any specific actions:
 
 > private RestApiActions optionSetActions = new RestApiActions("/optionSets");