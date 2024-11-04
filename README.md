# Hasura V2 Athena GDC Connector

This repo contains the Athena connector compatible with the Hasura V2 Data Connector specification.

## Running the application

The application can be run either locally on the host machine, or as a Docker service.

### Local Service

To run the application locally, a Gradle task can be invoked that provides a development environment with hot-reloading enabled.
This can be launched via the command:
- `./gradlew :app:quarkusDev`

The service will be live on `http://localhost:8081`, and the Data Connector root url is `http://localhost:8080/v1/athena`.

### Docker Service

To run the application via Docker, build it using the `Dockerfile` located in the root of this repo, and then `docker run` the application, binding to port `8081` inside of the container.

## Including custom JDBC drivers

To install custom JDBC drivers without Maven coordinates, these drivers can be placed in the `./lib/vendored` directory.

During the Gradle build process, these will be added as dependencies to the core `:app` service.