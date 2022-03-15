# Changelog

## [v2.3.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.3.0) (2022-3-15)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.2...v2.3.0)

* Upgraded to vertx 4.2.5 final
* Upgraded spa-react dependencies
* Fixed pointer on tools dropdown
* Added custom @metahub package in karma to remove deprecation
* Changed vertx static handler to remove deprecation
* Upgraded marked to latest.
* Upgraded react router to v6 - Needed changes to "MenuLinks" and "routertest.js"
* Upgraded Bootstap 4 -> 5 - New layout
* Upgraded fontawesome 4 -> 5

## [v2.2.2](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.2) (2021-10-29)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.1...v2.2.2)

* Upgraded to vertx 4.2.0 final
* upgraded dodex and vertx dependencies(cubrid/agroal)

## [v2.2.1](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.1) (2021-10-14)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.2.0...v2.2.1)

* Upgraded and modified firebase tests - src/firebase/testsrc/dodex.tests.ts
* Made dodex connections to server more generic - index.html, bootstrap.html, entry.jsx

## [v2.2.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.2.0) (2021-08-01)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.1.0...v2.2.0)

* Added VERTX_PORT environment variable to change port at execution time
* Setup graalvm with both java8 and java11 - dodexvm8 & dodexvm11
* Upgraded javascript modules in spa react demo
* Upgraded javascript modules in firebase setup 
* Added Firebase to spa react demo for 'login' table  
* Upgraded Vert.x to 4.1.2
* Upgraded Gradle to 7.1.1 - faster rebuilds
* Fixed AKKA connection problem
* Fixed Postgres SPA login problem
* Code clean-up - based on PMD and SonarLint
* Seperated out Cubrid database code from DbDefinitionBase.java to DbCubridOverride.java - fixed in 4.2.0?

## [v2.1.0](https://github.com/DaveO-Home/dodex-vertx/tree/v2.1.0) (2021-05-10)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.6...v2.1.0)

* Added Firebase/Firestore as a backend.
* Cleaned up some code.
* Fixed startup messages.

## [v2.0.6](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.6) (2021-04-20)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.5...v2.0.6)

* Display current database
* Display regex route patterns
* Changed sql "Update" to "Insert" form for Cubrid, Update,Select,Delete are still problematic

## [v2.0.5](https://github.com/DaveO-Home/dodex-vertx/tree/v2.0.5) (2021-04-14)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v2.0.4...v2.0.5)

* Upgraded to Vertx 4.0.3
* Upgraded Cubrid jdbc driver to v11, did not fix null exception
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

* Upgraded the react spa app to remove many vulnerablities.
* Made jsoneditor default to z-index -1 to allow table pager

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
* Converted 'davidmoten:rxjava2-jdbc' library to 'io.vertx.reactivex.*'
* Using Vertx4 PostgreSQL, MySQL, DB2 and JDBC clients

__Just a note:__ 😞 Wondering if anyone tries this code out. __No Issues!!__ not even a 👍.

## [v1.9.1](https://github.com/DaveO-Home/dodex-vertx/tree/v1.9.1) (2020-12-07)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.9.0...v1.9.1)

* Added Cassandra database to the React SPA Demo to allow the `login` component.
* Made the TCP bridge more global to allow multiple handlers. The complete setup now happens in `Server.java`.
* Fixed the determination between `dev/test` and `prod`; See application-conf.json, also moved this file to the `CLASSPATH` so it can be loaded in production (Vertx always wants the command line `--conf` parameter).

## [v1.9.0](https://github.com/DaveO-Home/dodex-vertx/tree/v1.9.0) (2020-11-24)

[Full Changelog](https://github.com/DaveO-Home/dodex-vertx/compare/v1.8.4...v1.9.0)

* Added Cassandra database via an Akka micro-service as an Event-Bus client, see; <https://www.npmjs.com/package/dodex-akka>
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
