# Changelog

# [v4.0.2](https://github.com/DaveO-Home/dodex-vertx/tree/v4.0.0) (2025-06-03)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v4.0.0..v4.0.2)

* Fixed `vertx` null pointer exception for databases
* Upgraded Javascript dependencies
* Changed Dockerfile to default to Vert.x 5, no `Envoy`
* Added Dockerfile_Envoy to run `Envoy`
* Changed README.md for building docker/podman/minikube applications
* See `./kube/dev_minikube.txt` cheat sheet for building docker/minikube vert.x application exposed to the internet using `loophole` tunnelling software.

# [v4.0.0](https://github.com/DaveO-Home/dodex-vertx/tree/v3.3.1) (2025-05-20)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.3.1..v4.0.0)

* Upgraded to vertx 5.0.0
* `grpc-web` now works out of the box without a proxy(envoy), see `handicap/src/main/kotlin/golf/handicap/vertx/HandicapGrpcServer.kt`
* The Envoy proxy can still be used by setting the environment variable `GRPC_SERVER=true`
* Dodex-Vertx does not use the new Vert.x 5.0.0 Launcher and VerticleBase Class
  * The new Launcher for development uses a maven plugin for reload. Not useful when using Gradle
  * The VerticalBase required more code changes. Anything related to Vert.x classes and rxjava3, e.g. io.vertx.rxjava3.core.Vertx needs to change to io.vertx.core.Vertx.

## [v3.3.1](https://github.com/DaveO-Home/dodex-vertx/tree/v3.3.1) (2025-01-27)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.3.0..v3.3.1)

* Upgraded to vertx 4.5.12
* Upgraded Java and Javascript dependencies
* Fixed deprecated database pool creation, now using a **`builder pattern`**
* Fixed the gRPC javascript client to handle latest `google-protobuf` version
* Added new weather widget to the handicap client
* Made the static router a little more performant

## [v3.3.0](https://github.com/DaveO-Home/dodex-vertx/tree/v3.3.0) (2024-07-31)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.2.1..v3.3.0)

* Upgraded to vertx 4.5.9
* Implemented new gRPC server as described in the docs:  
  ''Vert.x gRPC Server is a new gRPC server powered by Vert.x HTTP server superseding the integrated Netty based gRPC client.  
  This server provides a gRPC request/response oriented API as well as the generated stub approach with a service bridge.''
  * see __.../handicap/src/main/kotlin/golf/handicap/vertx/HandicapGrpcServer.kt__, however, the Netty configuration is still default.
  * To use the new gRPC Vert.x server, execute `export GRPC_SERVER=true` or change `"grpc.server": true` in .../src/main/resources/application-conf.json for a permanent change
* Added the Vert.x __Mqtt Broker__ to communicate with the __dodex-akka__ microservice client to process __dodex-mess__ messages.
  * see the __dodex-vertx__ [README](https://github.com/DaveO-Home/dodex-vertx/blob/master/README.md) for the broker and __dodex-akka__ [README](https://github.com/DaveO-Home/dodex-akka/blob/master/README.md) for the client

## [v3.2.1](https://github.com/DaveO-Home/dodex-vertx/tree/v3.2.1) (2024-02-05)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.2.0..v3.2.1)

* Upgraded to vertx 4.5.2
* Fixed handicap to correctly update golfer scores with asterisk indicating which scores are used for handicap
* Upgraded javascript dependencies
* Fixed postgresql table "net_score" with proper precision
  * use `ALTER TABLE scores ALTER COLUMN net_score TYPE numeric(4,1)` if table is already defined.

## [v3.2.0](https://github.com/DaveO-Home/dodex-vertx/tree/v3.2.0) (2024-01-05)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.1.2..v3.2.0)

* Upgraded to vertx 4.5.1
* Added "h2" database, supports dodex and the handicap application
* Made "h2" the default DB - with latest java jdbc-client, sqlite3 no longer returns "generated key"
* Added a verticle for Java21 Virtual Threads; "localhost:8881/threads" when "dodex.virtual.threads" is set to "true" in "application-conf.json"
* Made "static" router a little less problematic (removed unnecessary routing)
* Removed future deprecations from PMD - `dodexstart.xml`
* Added "mongodb" for use with "dodex"
* Organized the db package

## [v3.1.2](https://github.com/DaveO-Home/dodex-vertx/tree/v3.1.2) (2023-11-18)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.1.1..v3.1.2)

* Upgraded to vertx 4.4.6 final
* Upgraded javascript dependencies
* Fixed openapi validation and sqlite3 database functions for dodex openapi addon

## [v3.1.1](https://github.com/DaveO-Home/dodex-vertx/tree/v3.1.1) (2023-10-18)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.1.0..v3.1.1)

* Upgraded to vertx 4.4.6 final
* Upgraded javascript dependencies
* Fixed "duplicate key" when adding group members in the dodex openapi addon

## [v3.1.0](https://github.com/DaveO-Home/dodex-vertx/tree/v3.1.0) (2023-08-15)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.0.2..v3.1.0)

* Upgraded to vertx 4.4.5 final
* Upgraded javascript dependencies
* Added handicap dodex group capability using OpenApi
  * see __Dodex Groups using OpenAPI__ section in the README
* Added group openapi client to static directory for use with other dodex implementations
* Fixed skipped tests, however the "dev" server and "envoy" proxy must be running to complete successfully.
* Fixed `error: incompatible types: DodexDatabasePostgres cannot be converted to DodexDatabase,`
    gradle compile dependency problem and package/class name conflicts - no need to run clean first
* upgraded to Gradle 8.1.1
* Default vertx grpc port changed to 15002
* Changed all mariadb table names to lowercase to make consistent with jooq/postgres generator
* Fixed font-size on handicap form

## [v3.0.2](https://github.com/DaveO-Home/dodex-vertx/tree/v3.0.2) (2023-05-12)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v3.0.1..v3.0.2)

* Upgraded to vertx 4.4.2 final
* Upgraded javascript dependencies
* Fixed dodex-mess (grab credentials)
* Added default generated javascript grpc scripts: handicap_grpc_web_pb.js, handicap_pb.js
  * client can now be generated with just **`npm run webpack:build`** etc.

## [v3.0.0](https://github.com/DaveO-Home/dodex-vertx/tree/v3.0.0) (2023-04-17)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.5.2..v3.0.0)

* Upgraded to vertx 4.4.1 final
* Upgraded to gradle 8.0.2
* Upgraded java/javascript dependencies
* Major upgrade of React to v18.2.0
* Added docker config for grpc
* Added minikube with exposure to internet
* Cleaned up gradle warnings and dependency conflicts
* Changed `centos/Dockfile` to handle latest dependencies
* Added content to handicap dodex widget
* Process Changes:
  * Must run **./gradlew clean** before building production jar
  * Must run **./gradlew shadowJar** or **./gradlew build** to build production jar
  * Should set **USE_HANDICAP=true** before building the jar
  * Making database changes and using **jooqGenerate**
    * Set **DEFAULT_DB** to either **sqlite3** or **postgres** ("mariadb" handles booleans and floats differently)
    * If using **sqlite3** remove **handicap/dodex_tests.db**(Assumes that DbSqlite3.java has been changed and all other used databases)
      * **Note:** In **..../dodex-vertx** directory run for each database, **`find . -name DbSqlite3.java`** to find the db schema
    * Optionally remove **handicap/src/main/kotlin/golf/handicap/generated** directory 
    * Run **./gradlew jooqGenerate**

## [v2.5.2](https://github.com/DaveO-Home/dodex-vertx/tree/v3.0.0) (2022-11-13)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.4.0...v2.5.2)

* Upgraded to vertx 4.3.4 final
* Upgraded java/javascript dependencies
* Added a web application using "kotlin", "protobuf", "gRPC", "envoy", "javascript/tools", "jooq" code generation
* Major modification to "gradle" configuration for new application
* Upgraded gradle to 7.5, requires Java17 or greater

## [v2.4.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.4.0) (2022-9-1)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.3.0...v2.4.0)

* Upgraded to vertx 4.3.3 final
* Upgraded java/javascript dependencies
* Migrated to rxjava3
* Added Neo4j database(using mutiny with database access)
* Added Dodex monitor using Kafka/Zookeeper
* Changed Logger to Log4j to control Kafka logging

## [v2.3.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.3.0) (2022-3-15)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.2...v2.3.0)

* Upgraded to vertx 4.2.5 final
* Upgraded spa-react dependencies
* Fixed pointer on tools dropdown
* Added custom `@metahub` package in karma to remove deprecation
* Changed vertx static handler to remove deprecation
* Upgraded marked to latest.
* Upgraded react router to v6 - Needed changes to "MenuLinks" and `"routertest.js"`
* Upgraded `Bootstap 4` -> 5 - New layout
* Upgraded `fontawesome 4` -> 5

## [v2.2.2](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.2) (2021-10-29)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.1...v2.2.2)

* Upgraded to vertx 4.2.0 final
* upgraded dodex and vertx dependencies(cubrid/agroal)

## [v2.2.1](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.1) (2021-10-14)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.0...v2.2.1)

* Upgraded and modified firebase tests - `src/firebase/testsrc/dodex.tests.ts`
* Made dodex connections to server more generic - index.html, bootstrap.html, entry.jsx

## [v2.2.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.0) (2021-08-01)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.1.0...v2.2.0)

* Added VERTX_PORT environment variable to change port at execution time
* Setup graalvm with both java8 and java11 - `dodexvm8` & `dodexvm11`
* Upgraded javascript modules in spa react demo
* Upgraded javascript modules in firebase setup 
* Added Firebase to spa react demo for 'login' table  
* Upgraded Vert.x to 4.1.2
* Upgraded Gradle to 7.1.1 - faster rebuilds
* Fixed AKKA connection problem
* Fixed Postgres SPA login problem
* Code clean-up - based on PMD and SonarLint
* Separated out `Cubrid` database code from DbDefinitionBase.java to `DbCubridOverride.java` - fixed in 4.2.0?

## [v2.1.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.1.0) (2021-05-10)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.6...v2.1.0)

* Added `Firebase/Firestore` as a backend.
* Cleaned up some code.
* Fixed startup messages.

## [v2.0.6](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.6) (2021-04-20)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.5...v2.0.6)

* Display current database
* Display regex route patterns
* Changed sql "Update" to "Insert" form for `Cubrid`, Update,Select,Delete are still problematic

## [v2.0.5](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.5) (2021-04-14)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.4...v2.0.5)

* Upgraded to Vertx 4.0.3
* Upgraded `Cubrid` jdbc driver to v11, did not fix null exception
* Upgraded javascript React app (removed potentially dangerous vulnerabilities)
* Fixed deprecated karma server configuration
* Added keystore to easily test ssl(commented out by default), see Server.java

## [v2.0.4](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.4) (2021-02-12)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.3...v2.0.4)

* Upgraded to Vertx 4.0.2
* Upgraded to Gradle 6.8.1
* Fixed repository defines in build.gradle

## [v2.0.3](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.3) (2021-01-23)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.2...v2.0.3)

* Upgraded the React spa app to remove many vulnerabilities.
* Made `jsoneditor` default to z-index -1 to allow table pager

## [v2.0.2](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.2) (2020-12-31)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.1...v2.0.2)

* Fixed deprecated merge sql & React Login
* The login was modified for cassandra but caused other dbs to fail
* The jooq merge command was deprecated, replaced with "insertInto" with onConflict

## [v2.0.1](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.1) (2020-12-30)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.0...v2.0.1)

* Fixed Spa-React "login" component for Akka/Cassandra

## [v2.0.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.0) (2020-12-28)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.9.1...v2.0.0)

* Migrated to Vertx4 - basically a re-write
* Converted `'davidmoten:rxjava2-jdbc'` library to 'io.vertx.reactivex.*'
* Using Vertx4 `PostgreSQL`, MySQL, DB2 and JDBC clients

**Just a note:** 😞 Wondering if anyone tries this code out. **No Issues!!** not even a 👍.

## [v1.9.1](https://github.com/DaveO-Home/dodex-vertx/tree/v1.9.1) (2020-12-07)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.9.0...v1.9.1)

* Added Cassandra database to the React SPA Demo to allow the `login` component.
* Made the TCP bridge more global to allow multiple handlers. The complete setup now happens in `Server.java`.
* Fixed the determination between `dev/test` and `prod`; See application-conf.json, also moved this file to the `CLASSPATH` so it can be loaded in production (Vertx always wants the command line `--conf` parameter).

## [v1.9.0](https://github.com/DaveO-Home/dodex-vertx/tree/v1.9.0) (2020-11-24)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.4...v1.9.0)

* Added Cassandra database via an Akka microservice as an Event-Bus client, see; <https://www.npmjs.com/package/dodex-akka>
* Added turning off color in application-conf.json. Also with `ColorUtilConstants.colorOff()` and `colorOn()`
  
## [v1.8.4](https://github.com/DaveO-Home/dodex-vertx/tree/v1.8.4) (2020-06-29)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.3...v1.8.4)

* Fixed Exception when sending private message to multiple users
* Made removing Undelivered/Messages non-blocking
* Upgraded gradle 6.5

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.2...v1.8.3)

* Add databases to SPA application
* Fixed potential thread issue with removing messages
* Upgraded dependencies

## [v1.8.2](https://github.com/DaveO-Home/dodex-vertx/tree/v1.8.2) (2020-06-02)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.1...v1.8.2)

* Detect old login in Browser Refresh (react component LoginC.jsx)
* Upgraded gradle to 6.4.1 to allow Java14

## [v1.8.1](https://github.com/DaveO-Home/dodex-vertx/tree/v1.8.1) (2020-06-01)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.0...v1.8.1)

* Added Change Log README

## [v1.8.0](https://github.com/DaveO-Home/dodex-vertx/tree/v1.8.0) (2020-06-01)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.7.0...v1.8.0)

* Added Java Linting with PMD - See Java Linting section in README
* Changed src to comply with a subset of PMD rules

## [v1.7.0](https://github.com/DaveO-Home/dodex-vertx/tree/v1.7.0) (2020-05-26)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/19093a9f2ce360e48640c6eaeaddfbe822602b48...v1.7.0)

* Made DodexRouter non-blocking with Future and Promise
* All database calls are now non-blocking (removed await)
* Made CleanOrphanedUsers non-blocking
* Added React SPA Demo for testing/development
* Upgraded vert.x to v3.9.1

\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
