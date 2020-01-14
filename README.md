# doDex-vertx, a java asynchronous server for Dodex, Dodex-input and Dodex-mess

## Install Assumptions

1. Java 8 or higher installed with JAVA_HOME set.
2. Gradle 5 installed. If you have sdkman installed, execute ```sdk install gradle 5.6.4```
3. npm javascript package manager installed.

## Getting Started

1. ```npm install dodex-vertx``` or download from <https://github.com/DaveO-Home/dodex-vertx>. If you use npm install, move node_modules/dodex-vertx to an appropriate directory.
2. ```cd <install directory>/dodex-vertx/src/main/resources/static``` and execute ```npm install --save``` to install the dodex modules.
3. ```cd <install directory>/dodex-vertx``` and execute ```gradlew run```. This should install java dependencies and startup the server in development mode against the default sqlite3 database. In this mode, any modifications to java source will be recompiled.
4. Execute url ```http://localhost:8087/test``` in a browser.
5. You can also run ```http://localhost:8087/test/bootstrap.html``` for a bootstrap example.
6. Follow instructions for dodex at <https://www.npmjs.com/package/dodex-mess> and <https://www.npmjs.com/package/dodex-input>.

### Operation

1. Execute ```gradlew tasks``` to view all tasks.
2. Execute ```gradlew shadowJar``` to build the production fat jar.
3. Execute ```java -jar build/libs/dodex-vertx-3.8.4.jar``` to startup the production server.
4. Execute url ```http://localhost:8080/dodex``` or ```.../dodex/bootstrap.html``` in a browser. __Note;__ This is a different port and url than development. Also __Note;__ The default database on the backend is "Sqlite3", no further configuation is necessay. Dodex-vertx also has Postgres/Cubrid/Mariadb implementations. See ```<install directory>/dodex-vertx/src/main/resources/static/database_config.json``` for configuration.
5. Swapping among databases; Use environment variable ```DEFAULT_DB``` by setting it to either ```sqlite3``` ,```postgres```, ```cubrid```, ```mariadb``` or set the default database in ```database_config.json```.
6. The environment variable ```VERTXWEB_ENVIRONMENT``` can be used to determine the database mode. It can be set to either ``prod`` or unset for production and ``dev`` for the development database as defined in ``database_config.json``.
7. When Dodex-vertx is configured for the Cubrid database, the database must be created using UTF-8. For example ```cubrid createdb dodex en_US.utf8```.
8. Version 1.3.0 adds an auto user clean up process. See ```application-conf.json``` for configuration. It is turned off by default. Users and messages may be orphaned when clients change a handle when the server is offline.

## Test

1. Make sure the demo Java-vertx server is running in development mode.
2. Test Dodex-mess by entering the URL `localhost:3087/test/index.html` in a browser.
3. Ctrl+Double-Click a dial or bottom card to popup the messaging client.
4. To test the messaging, open up the URL in a different browser and make a connection by Ctrl+Double-Clicking the bottom card. Make sure you create a handle.
5. Enter a message and click send to test.
6. For dodex-input Double-Click a dial or bottom card to popup the input dialog. Allows for uploading, editing and removal of private content. Content in JSON can be defined as arrays to make HTML more readable.

### Optimizing with Graalvm

* You can run the Vertx Server in native assembled code by compiling the fat jar with graalvm.

1. Install from <https://github.com/graalvm/graalvm-ce-builds/releases>.
2. Follow install instructions.
3. Execute ```gu install native-image``` to install the ```native-image``` program.
4. In the dodex-vertx directory execute ```gradlew install``` and ```gradlew shadowJar```.
5. Copy the dodexvm shell program located in the dodex-vertx directory to the graalvm directory.
6. Modify the shell to local installed directories.
7. In the graalvm directory execute the dodexvm shell program. This should create an executable named ```io.vertx.core.Launcher```.
8. Execute the production server with ```io.vertx.core.Launcher``` or ```io.vertx.core.Launcher run dmo.fs.vertx.Server```.

### Docker

* Run under Docker, assumes Docker is installed and daemon is running.

1. From dodex-vertx directory; ```centos/buildcontainer``` or ```centos\buildcontainer```
2. Execute ```gradlew run``` to start development. Execute ```ctl-c``` && ```exit``` to shutdown.
3. Execute ```centos/execontainer``` or ```centos\execontainer``` on subsequent startups.

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
