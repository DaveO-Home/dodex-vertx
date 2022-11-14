# doDex-vertx, a java asynchronous server for Dodex, Dodex-input and Dodex-mess

## Install Assumptions

1. Using Vertx4 @ <https://vertx.io/introduction-to-vertx-and-reactive/>
2. Java 17 or higher installed with JAVA_HOME set.
3. Gradle 7+ installed. If you have sdkman installed, execute ```sdk install gradle 7.5```
4. npm javascript package manager installed.

__Important Note:__ The `./gradlew run` is much more complex out of the box. The `kotlin, gRPC` web applicaion requires a `gradle` composite build configuration. See the `Kotlin, gRPC Web Application` section below.

## Getting Started

1. ```npm install dodex-vertx``` or download from <https://github.com/DaveO-Home/dodex-vertx>. If you use npm install, move node_modules/dodex-vertx to an appropriate directory.
2. ```cd <install directory>/dodex-vertx/src/main/resources/static``` and execute ```npm install --save``` to install the dodex modules.
3. ```cd <install directory>/dodex-vertx``` and execute ```gradlew run```. This should install java dependencies and startup the server in development mode against the default sqlite3 database. In this mode, any modifications to java source will be recompiled.
4. Execute url ```http://localhost:8087/test``` in a browser.
5. You can also run ```http://localhost:8087/test/bootstrap.html``` for a bootstrap example.
6. Follow instructions for dodex at <https://www.npmjs.com/package/dodex-mess> and <https://www.npmjs.com/package/dodex-input>.
7. You can turn off colors by setting "color": to false in application-conf.json.
8. The Cassandra database has been added via an `Akka` micro-service. See; <https://www.npmjs.com/package/dodex-akka>.
9. Added Cassandra database to the `React` demo allowing the `login` component to use Cassandra.
10. See the `Firebase` section for using Google's `Firestore` backend.

### Operation

1. Execute ```gradlew tasks``` to view all tasks.
2. Execute ```gradlew shadowJar``` to build the production fat jar.
3. Execute ```java -jar build/libs/dodex-vertx-3.8.5.jar``` to startup the production server.
4. Execute url ```http://localhost:8080/dodex``` or ```.../dodex/bootstrap.html``` in a browser. __Note:__ This is a different port and url than development. Also __Note:__ The default database on the backend is "Sqlite3", no further configuation is necessay. Dodex-vertx also has Postgres/Cubrid/Mariadb/Ibmdb2/Cassandra/Firebase implementations. See ```<install directory>/dodex-vertx/src/main/resources/static/database_config.json``` for configuration.
5. Swapping among databases; Use environment variable ```DEFAULT_DB``` by setting it to either ```sqlite3``` ,```postgres```, ```cubrid```, ```mariadb```, ```ibmdb2```, ```cassandra```, ```firebase``` or set the default database in ```database_config.json```.
6. The environment variable ```VERTXWEB_ENVIRONMENT``` can be used to determine the database mode. It can be set to either ``prod`` or unset for production and ``dev`` for the development database as defined in ``database_config.json``.
7. When Dodex-vertx is configured for the Cubrid database, the database must be created using UTF-8. For example ```cubrid createdb dodex en_US.utf8```.
8. Version 1.3.0 adds an auto user clean up process. See ```application-conf.json``` for configuration. It is turned off by default. Users and messages may be orphaned when clients change a handle when the server is offline.

## Debug

* Execute `gradlew run -DDEBUG=true` to debug the Vertx Vertical.
* The default port is 5005, see `build.gradle` to change.
* Tested with VSCode, the `launch.json` =
  
```javascript
    {
            "type": "java",
            "name": "Debug (Launch) - Dodex",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
    }
```

## Test Dodex

1. Make sure the demo Java-vertx server is running in development mode.
2. Test Dodex-mess by entering the URL `localhost:3087/test/index.html` in a browser.
3. Ctrl+Double-Click a dial or bottom card to popup the messaging client.
4. To test the messaging, open up the URL in a different browser and make a connection by Ctrl+Double-Clicking the bottom card. Make sure you create a handle.
5. Enter a message and click send to test.
6. For dodex-input Double-Click a dial or bottom card to popup the input dialog. Allows for uploading, editing and removal of private content. Content in JSON can be defined as arrays to make HTML more readable.

## Java Linting with PMD

* Run `gradlew pmdMain` and `gradlew pmdTest` to verify code using a subset of PMD rules in `dodexstart.xml`
* Reports can be found in `build/reports/pmd`

### Optimizing with Graalvm

* You can run the Vertx Server in native assembled code by compiling the fat jar with graalvm.

  1. Install from <https://github.com/graalvm/graalvm-ce-builds/releases>.
  2. Follow install instructions.
  3. Execute ```<graalvm directory>/bin/gu install native-image``` to install the ```native-image``` program.
  4. In the dodex-vertx directory execute ```gradlew installDist``` and ```gradlew shadowJar```.
  5. Modify the dodexvm8/vm11 or dodexvm.bat shell to the graalvm install directory.
  6. In your dodex-vertx directory execute the dodexvm8 or dodexvm11 shell program. This should create an executable named ```io.vertx.core.Launcher```.
  7. Execute the production server with ```io.vertx.core.Launcher```.

   __Note:__ dodex-vertx will not build as a stand alone, therefore, if you move the executeable to different directory, you must also move the shadowJar to the same directory with structure `build\libs`.

### Single Page React Application to demo Development and Integration Testing

* Integrated in ***Dodex-Vertx*** at `src/spa-react`
* Documentation <https://github.com/DaveO-Home/dodex-vertx/blob/master/src/spa-react/README.md>
* Uses ***Sqlite3*** as backend database
* Router added to `src/main/java/dmo/fs/vertx/Server.java`

### Docker

* Run under Docker, assumes Docker is installed and daemon is running.

  1. From dodex-vertx directory; ```centos/buildcontainer``` or ```centos\buildcontainer```
  2. Execute ```gradlew run``` to start development. Execute ```ctl-c``` && ```exit``` to shutdown.
  3. Execute ```centos/execontainer``` or ```centos\execontainer``` on subsequent startups.

### Firebase

* Create an account: <https://firebase.google.com>
* Getting started: <https://firebase.google.com/docs/admin/setup#java>
* Make sure you create a `Service-Account-Key.json` file as instructed. Dodex-Vertx uses the environment variable option to set the service-account - `GOOGLE_APPLICATION_CREDENTIALS`. See gradle.build as one way to set it.
* You will need to login to the `Firebase` console and create the `dodex-firebase` project. See `src/main/java/dmo/fs/router/FirebaseRouter.java` for usage of the project-id and Google Credentials. __Note:__ The `Firebase` rules are not used, so they should be set to `allow read, write:  if false;` which may be the default.
* You only need the `Authentication` and `Firestore` extensions.
* If you want a different project name, change `.firebaserc`.

  #### Testing

  * To make sure your project is created and the setup works, you should run the tests. __Note:__ They are written in Typescript.
  * cd `../dodex-vertx/src/firebase` and run `npm install`
  * execute `npm run emulators` to startup the emulators for testing.
  * To test the model and rules after starting the emulators, in a different terminal window, run `npm test`.

### Neo4j

* See <https://neo4j.com/docs/operations-manual/current/> for usage.
* To use a container with `apoc` you can try: __Note:__ this has `--privileged` set.
    ```
    docker run \
    -p 7474:7474 -p 7687:7687 \
    -v $PWD/neo4j/data:/neo4j/data -v $PWD/neo4j/plugins:/neo4j/plugins \
    --name neo4j-apoc \
    --privileged \
    -e 'NEO4J_AUTH=neo4j/secret' \
    -e NEO4J_apoc_export_file_enabled=true \
    -e NEO4J_apoc_import_file_enabled=true \
    -e NEO4J_apoc_import_file_use__neo4j__config=true \
    -e NEO4JLABS_PLUGINS=\[\"apoc\"\] \
    -e NEO4J_dbms_security_procedures_unrestricted=apoc.\\\* \
    neo4j:4.3
    ```
To restart and stop: `docker start neo4j-apoc` and `docker stop neo4j-apoc`

The Neo4j was tested with the `apoc` install, however the database should work without it.

Simply execute `export DEFAULT_DB=neo4j` to use, after database setup.

### Dodex Monitoring

#### Getting Started

* Apache Kafka must be installed.
    *  [Kafka Quickstart](https://kafka.apache.org/quickstart) - A container should also work
    *  .../config/server.properties should be modified if using a local install
        * advertised.listeners=PLAINTEXT://localhost:9092
        * num.partitions=2   # at least 2
    * local startup
        *  ./bin/zookeeper-server-start.sh config/zookeeper.properties
        *  ./bin/kafka-server-start.sh config/server.properties

* Setup Vertx for Kafka
    *  __set environment variable `DODEX_KAFKA=true`__ or 
    *  __set "dodex.kafka" to true__ in the `application-conf.json` file(default is false)
    *  startup Vertx - the monitor should work with any of the databases
    *  the monitor configuation can be found in `application-conf.json`

* Monitor Dodex
    * in a browser enter `localhost:8087/monitor` or `localhost:8080/monitor` in production.
    * as dodex messaging executes the events should be recorded.
    * in the browser's `developer tools` console execute `stop();` and `start();` to stop/start the polling. Polling is started by default.
    
    __Note:__ you can open the messaging dialog with `ctrl-doubleclick` on the dials

### Kotlin, gRPC Web Application

    This web application can be used to maintain golfer played courses and scores and to calculate a handicap index. The application has many moving parts from the `envoy` proxy server to `kotlin`, `protobuf`, `gRPC`, `jooq`, `bootstrap`, `webpack`, `esbuild`, `gradle`, `java` and `javascript`.

    See documentation at; <https://github.com/DaveO-Home/dodex-vertx/blob/master/handicap/README.md>

## ChangeLog

<https://github.com/DaveO-Home/dodex-vertx/blob/master/CHANGELOG.md>

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
