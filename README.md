# doDex-vertx, a java asynchronous server for Dodex and Dodex-mess

## Install Assumptions

1. Java 8 or higher installed with JAVA_HOME set.
2. Gradle 5 installed. If you have sdkman installed, execute ```sdk install gradle 5.6.4```
3. npm javascript package manager installed.

## Getting Started

1. ```npm install dodex-vertx``` or download from <https://github.com/DaveO-Home/dodex-vertx>. If you use npm install, move node_modules/dodex-vertx to an appropriate directory.
2. ```cd <install directory>/dodex-vertx/src/main/resources/static``` and execute ```npm install --save``` to install the dodex modules.
3. ```cd <install directory>/dodex-vertx``` and execute ```gradlew run```. This should install java dependencies and startup the server in development mode against the default sqlite3 database. In this mode, any modifications to java source will be recompiled.
4. Execute url ```http://localhost:8087/test``` in a browser.
5. Follow instructions for dodex at <https://github.com/DaveO-Home/dodex-mess>.

### Operation

1. Execute ```gradlew tasks``` to view all tasks.
2. Execute ```gradlew shadowJar``` to build the production fat jar.
3. Execute ```java -jar build/libs/dodex-vertx-3.8.3.jar``` to startup the production server.
4. Execute url ```http://localhost:8080/dodex``` in a browser. __Note;__ This is a different port and url than development. Also __Note;__ The default database on the backend is "Sqlite3", no further configuation is necessay. Dodex-vertx also has a Postgres implementation. See ```<install directory>/dodex-vertx/src/main/resources/static/database_config.json``` for configuration.
5. Swapping between Sqlite3 and Postgres; Use environment variable ```DEFAULT_DB``` by setting it to either ```sqlite3``` or ```postgres```, or set the default database in ```database_config.json```.
6. The environment variable ```VERTXWEB_ENVIRONMENT``` can be used to determine the database mode. It can be set to either ``prod`` or unset for production and ``dev`` for the development database as defined in ``database_config.json``.
7. Dodex-vertx is also configured for the Cubrid database. To use the databases must be created using UTF-8. For example ```cubrid createdb dodex en.utf8```.

## Test

1. Make sure the demo Java-vertx server is running in development mode.
2. Test Dodex-mess by entering the URL `localhost:3087/test/index.html` in a browser.
3. Ctrl+Double-Click a dial or bottom card to popup the messaging client.
4. To test the messaging, open up the URL in a different browser and make a connection by Ctrl+Double-Clicking the bottom card. Make sure you create a handle.
5. Enter a message and click send to test.

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
