# GraphQL Data Connector Service - JVM Edition ☕

## Installing and Launching The GDC Service For Local Development

### Running Natively

#### Build the gradle project

This process requires Java (OpenJDK 17) installed on your machine.
The easiest way to install it if you don't have it is with `sdkman` (https://sdkman.io/)

The below will install `sdkman` and use it to install OpenJDK 17:

```sh
curl -s "https://get.sdkman.io" | bash
sdk install java
```

#### Build the executable JAR

From `graphql-engine-mono/pro/dc-agents/super-connector` run:
```sh
./gradlew build
``` 

This will produce the following executable jar:

```sh
graphql-engine-mono/pro/dc-agents/super-connector/app/build/quarkus-app/quarkus-run.jar
```

#### Launch a database backend
A docker-compose yaml file is provided in
`graphql-engine-mono/pro/dc-agents/super-connector/docker` with a
handful of database options you can use. If you use the compose file,
be sure to comment out the GDC container before launching your local
copy of agent.

#### Run the SuperWrapper

From `graphql-engine-mono/pro/dc-agents/super-connector` run:

```sh
java -jar app/build/quarkus-app/quarkus-run.jar
```

### Running with Docker

Sorry about this process, I'll upload `.tar.gz` exported Dockerfiles ASAP so that you don't have to jump through these hoops.

#### Build the executable JAR

Follow the running local instructions to build the JAR.

#### Build the docker image**

The application framework used for the super-connector, `Qaurkus`,
automatically generates a `Dockerfile`.  This `Dockerfile` is located
at `app/src/main/docker/Dockerfile.jvm`

From `graphql-engine-mono/pro/dc-agents/super-connector` run:

```sh
docker build -f app/src/main/docker/Dockerfile.jvm -t hasura/data-wrapper-jvm:1.0.0-SNAPSHOT app
```

Note: our current instance of the `Dockerfile` has been modified to
accommodate JPMC's security scan) to build the docker image.

#### Bring up the docker-compose file

From `graphql-engine-mono/pro/dc-agents/super-connector` run:

```sh
docker compose -f docker/docker-compose.yml up --build
```

This will start:
- Hasura with GDC abilities (port 8082)
- Quarkus GDC Server (port 8081)
- Postgres (port 5432)
- Oracle with Chinook schema + data (port 1521)
- MySQL with Chinook schema + data (port 3306)
- MongoDB with Albums/Artists schema + data (port 27017)

**Be sure to comment out the Hasura container if you wish to run your
own local branch of HGE.**

## Using the GDC Service
Once the GDC Service, database of choice, and Hasura GraphQL Engine
are running the last step is to connect the GDC Service to HGE and and
the database as a new source.

**The complete API for the GDC Service is available [here](http://localhost:8081/q/swagger-ui) and logs are available [here](http://localhost:8081/q/logging-manager-ui).**

The GDC Service is stateless, meaning that with every request you must
pass in a blob of JSON to connect the service to the database. This
configuration data is typically a JDBC URL but in the case of Athena
you must provide additional AWS specific metadata.

The GDC Service's per database configuration settings and capabilities
can be checked by calling the `capabilities` endpoint for that backend, eg.:

```
GET http://localhost:8081/api/v1/mysql/capabilities
```

You can verify that the GDC Service can connect to your database by
calling the `schema` endpoint and providing the configuration data in
a header:

```
GET http://localhost:8081/api/v1/mysql/schema
Content-Type: application/json
X-Hasura-DataConnector-Config: { "jdbc_url": "jdbc:mysql://localhost:3306/Chinook?user=root&password=Password123%23&disableMariaDbDriver=true" }
X-Hasura-DataConnector-SourceName: "chinook"
```

Here is a `curl` example for `mysql`:

```
curl --location --request GET 'http://localhost:8081/api/v1/mysql/schema' \
--header 'Content-Type: application/json' \
--header 'X-Hasura-DataConnector-Config: { "jdbc_url": "jdbc:mysql://localhost:3306/Chinook?user=root&password=Password123%23&disableMariaDbDriver=true" }' \
--header 'X-Hasura-DataConnector-SourceName: "chinook"'
```

### Supported Databases
Please verify against [the swagger API](http://localhost:8081/q/swagger-ui) to make sure this list is current.

#### Mysql
**GDC Endpoint:** http://localhost:8081/api/v1/mysql

#### Athena
**GDC Endpoint:** http://localhost:8081/api/v1/athena

### Adding the GDC Service as a Datasource in Hasura

First we add the GDC Service as an Agent for our backend type. Use the
`url` above which corresponds to your backend:

```
  POST http://localhost:8080/v1/metadata
  Content-Type: application/json
  X-Hasura-Admin-Secret: password

  {
      "type" : "dc_add_agent",
      "args": {
	    "name": "mysql_gdc",
	    "url": "http://localhost:8081/api/v1/mysql"
      }
  }
```

Next we add a source. Use the `jdbc_url` from your backend type,
replacing `{{USER}}` and `{{PASSWORD}}` with your db user/password:

```
  POST http://localhost:8080/v1/metadata
  Content-Type: application/json
  x-hasura-admin-secret: password

  {
    "type":"mysql_gdc_add_source",
    "args": {
      "name":"chinook",
      "configuration": {
	    "value": {"jdbc_url": "jdbc:mysql://localhost:3306/Chinook?user=root&password=Password123%23&disableMariaDbDriver=true"}
      }
    },
    "replace_configuration":true
  }
```

Finally we need to track a table:

```
  POST http://localhost:8080/v1/metadata
  Content-Type: application/json
  x-hasura-admin-secret: password

  {
    "type":"mysql_gdc_track_table",
    "args": {
      "source": "chinook",
      "table": ["Chinook", "Artist"]
    },
    "replace_configuration":true
  }
```

Now if you visit the Hasura web console, you should see the `Artist`
table available in the GraphiQL explorer to make queries on.

## Live Development on the Quarkus JVM Docker Service

Quarkus comes with built-in remote development abilities.

After starting the docker services, you can run the following to start
a live-sync from your local filesystems to the deployed GDC server:

```sh
./gradlew --console=plain quarkusRemoteDev`
```
  
Unfortunately, this doesn't seem to work super well with Kotlin.

As opposed to immediate changes on-save (as advertised), it seems to
only apply changes when you stop and re-run the `quarkusRemoteDev`
command.

See the below for more info:
- https://quarkus.io/guides/gradle-tooling#remote-development-mode
- https://developers.redhat.com/blog/2021/02/11/enhancing-the-development-loop-with-quarkus-remote-development

## Notes for deployment

Per the `Dockerfile`, Quarkus is designed for use with
OpenJDK. Currently, we use java version 17.  The docker image can be
deployed per our current deployment standards, but it is perhaps worth
noting that Quarkus represents itself as a [Kubernetes
Native](https://quarkus.io/kubernetes-native/), [Container
First](https://quarkus.io/container-first/) framework.

Note: the libraries we use make it impossible to create native, GraalVM builds.

While the `Dockerfile` uses a RedHat universal base image (ubi) for
its deployment, this is largely a consequence of Quarkus being a
RedHat project.

Accordingly, any Linux distribution with a OpenJDK-17 installed should
suffice

As noted above, the code can be run by a simple java command, which
can be wrapped in whatever shell or init scripts are typically
involved in these dedicated AMI's

As with the `Dockerfile` we are foregoing any JVM options to start
with.  If needs be, these can be amended to address runtime issues.
