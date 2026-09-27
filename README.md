# device-service

## Overview

Spring Boot component for device management, supporting Create, Fetch, Update and Delete operations.

## Implementation details
The application is built as spring-boot service. The API is versioned as "v1" for future extendability.
The application connects to local PostgreSQL database, tests are run vs. in-memery database. Slf4j is used for logging.
Mapstruct framework is used for mapping operations between internal model classes and resources exposed in the API.

## Build and Execution
To build the application run "./gradlew clean build" in the project directory. To start the service run "./gradlew bootRun". 
In order to work correctly, local postgreSQL database should be up and running and devices database should be configured.
Another way to run the service is by running "docker compose up --build" - it will be build the application and start database and 
spring-boot service. 

## Documentation.
The REST endpoints are documented using Swagger: After starting the server, the documentation is available at 
URL http://localhost:8080/swagger-ui/index.html From the Swagger console it's possible to test the specific endpoints.

## Design and Implementation Decisions:
- UUID is used as the Device ID. There was no specific requirement for type of device ID and I decided for UUID, rather than number, to prevent possibility of 
abusing the system but running fetch by ID using big numeric range. Bad Request is returned if non-UUID string is used as deviceID path variable.
- Authorisation and Authentication is out of scope of the project.
- Separate REST endpoints exist for full (PUT) and partial (PATCH) updates
- Group fetch is handled by the same GET endpoint, where Brand and State can be supplied as optional query parameters.
That allows also to fetch devices by combination of brand and state - not listed as the requirement, but supported by application.
- Group fetch return list of Device resources, without wrapping it inside the object. Both approaches are possible, but since paging is not supported in group fetch 
(see next point), it's easier not to define the wrapping object. 
- Bad Request is returned if invalid string is used as state parameter for group fetch (rather than NotFound response)
- Paging is not supported in the group fetch - there was no specific requirement for it and I decided to leave it out of scope. It's the most important point for possible 
improvement.
- Partial updates (PATCH) allow to provide any of updatable fields (name, brand, state) as query parameters. Currently, there is no validation if any of 
those fields is provided, and also it's possible (though not recommended) to execute full update (of all 3 updatable fields) using PATCH operation.
- Delete performs physical delete from the database, rather than marking device as deleted. There was no requirement to keep information on the deleted 
devices and no connection of deleted devices to specific device status, so I decided to perform physical delete.
- Creation time is set as LocalDataTime. The assumption is that all the requests come from the same timezone.

## Possible improvements and missing features
- Group fetch does not support paging and sorting. It may be added as one of the most important improvements.
- Creation time does not support time zones as saved as the server time at the moment of persisting new device in database. 
It can be improved and creation time can be saved as ZonedDateTime.
- Slf4j is configured, but not really used. Log messages should be added over the implementation.
- There is no observability support and no logging in case of errors and unsuccessful responses.
- Authorisation and Authentication is out of scope of the project, no measures against possible abuse are configured.

## Use of AI
Claude-code was used during work on the project, though most of the development was done "by hands". Claude was used for code reviews, advise on possible improvements, 
and advise on best practices and design decisions. Also, Claude-code filled some unit tests (integration tests were developed by myself, to figure out possible problems 
in the project code). See `CLAUDE.md` for detailed architecture and command reference, used by Claude.
