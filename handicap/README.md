
### Kotlin, gRPC Web Application

### Getting Started

#### Client:

* The **html/javascript** client is generated from the **dodex-vertx/handicap/src/grpc/client** directory. 
    * Execute **`npm install`** to download the javascript dependencies.

    * Execute **`./proto protos/handicap`** to generate the **proto3/gRPC** javascript modules. The proto3 configuration is in **./protos/handicap.proto**.

      * Execute either **`npm run esbuild:build`** or **`npm run webpack:build`** to package the javascript development client. The output is located in **handicap/src/main/resources/static**. When making changes to javascript/html/css, simply rerun **`npm run esbuild:build`**, if the verticle is running, it will rebuild. For proto3 changes, rerun **`./proto protos/handicap`** first.

        **Note:**
          * **esbuild** is best for development(very fast) and **webpack** is better for production, e.g. **`npm run webpack:prod`**.
          * See handicap/src/grpc/client/js/client.js to setup **ipinfo.io** so the form will default to local **country/state**.

#### Server:

* The server **./proto3/gRPC** classes are auto generated from the gradle build script to **handicap/build/classes/java/main/handicap/grpc/**. 

* From the dodex-vertx directory, execute **`./gradlew run`** to build and execute the application.  The **`./gradlew jooqGenerate`** task executes the **jooq** code generator. The generated code can be found in `handicap/src/main/kotlin/golf/handicap/generated`. Objects can then be used to define the queries. By default the code is included with the project.

    **Note**: The gradle configuration is a 3 level composite build, it does the following:

    * compiles and executes the **jooq** java code generator located in **dodex-vertx/handicap/generate**, in development this can also create the database tables
    * compiles the java and kotlin code located in `dodex-vertx/handicap`
    * compiles and executes the java and kotlin code in `dodex-vertx` and `handicap`
<br/><br/>
* The next step is to install/startup the `envoy` proxy server. The javascript client needs a proxy to convert **http/1** to **http/2** as well as **CORS** processing. Assuming **envoy** is installed <https://www.envoyproxy.io/docs/envoy/latest/start/install>, execute the `start.envoy` script in `dodex-vertx`. The configuration for the proxy server is in **dodex-vertx/handicap/handicap.yaml**.

* If all goes well, only dodex-vertx at port `8087` should be started, the `handicap` application is turned off by default. **Note:** Setting **enableHandicap** to `true` in **.../resources/application-conf.json** will change the default startup.

* Kill **dodex-vertx**(ctrl-c), and set the environment variable **`export USE_HANDICAP=true`** and restart the server. In addition to the usual **8087** port display, there should also be a port `8888` display.

* For the standalone handicap application, view at **localhost:8888/handicap.html**, the **dodex** message client will not work from this port. For the complete application use port **8087**.

* The frontend html/gRPC javascript client should display. See operation section on how to use the form.
    
    **Note:** Only `sqlite3`, `h2`, `mariadb` and `postgres` support the handicap application. 
    
    * The default database is `h2`. To change the default database, execute **`export DEFAULT_DB=mariadb`**, **`export DEFAULT_DB=postgres`** or **`export DEFAULT_DB=sqlite3`**.
    * Gradle 8 configuration can now use jdk 19.
* Making database changes and using **jooqGenerate**
  * Set **DEFAULT_DB** to either **sqlite3**, **postgres**, **h2** or ("mariadb" handles booleans and floats differently)
  * If using **sqlite3** remove **handicap/dodex_tests.db**(Assumes that DbSqlite3.java has been changed as well as all other used databases)
    * **Note:** In **..../dodex-vertx** directory run for each database, **`find . -name DbSqlite3.java`** to find the db schema
  * Optionally remove **handicap/src/main/kotlin/golf/handicap/generated** directory
  * Run **`./gradlew jooqGenerate`**

### Production Build

* In **dodex-vertx/handicap/src/grpc/client** execute **`npm run webpack:prod`** or **`npm run esbuild:prod`**
* execute **`./gradlew clean`** and **`export USE_HANDICAP=true`**
* Make sure your database configurations are correct; **./src/main/resources/database...**, **./handicap/src/main/resources/database...**, **./handicap/generate/src/main/resources/database...**
* In **dodex-vertx** execute **`./gradlew shadowJar`** -  **Note:** The default database should be **sqlite3**
* To start the production verticle, execute **`java -jar build/libs/dodex-vertx-3.1.0-prod.jar`**
* For the stand alone handicap application execute `java -jar handicap/build/libs/handicap-0.0.1-fat.jar`
* Production runs on ports, **8880** and **8890**.

### Operation

The following are the steps to add golfer info, courses with tee data and golfer scores.

* First time login simply enter a **pin**(2 alpha characters with between 4-6 addition alpha-numeric characters) with first and last name. Click the login button to create/login to the application. On subsequent logins only the `pin` is needed, don't forget it. The **Country** and **State** should also be selected before creating a **pin** as default values. However, you can change the defaults on any login. Also, **Overlap Years** and **Public** should be set.

    * Overlap will use the previous year's scores for the handicap if needed.
    * Public will allow your scores to show up in the **Scores** tab for public viewing.
    

* Add a course by entering it's name with one radio button selected for the tee. You can also change the tee's color. The **rating**, **slope** and **par** values are also required. Click the **Add Tee** button. After the first added tee, the others can be added when needed.
* When re-logging in and selecting a previously added course the settings for the default tee(white) may not show. Simply select another radio button and then the default button.

  **Note**: You can disable the course/tee add function by setting **handicap.enableAdmin** to **true** in the **...\resources\application-conf.json** file. And then use the default **admin.pin** to administer the courses/tees. When using this pin, a first and last name must be entered on initial use.

* To add a score, select a course and tee with values for **Gross Score**, **Adjusted Score** and **Tee Time**. Click the **Add Score** button to add the score. The **Remove Last Score** will remove the last entered score, multiple clicks will remove multiple scores.

**Note:** A handicap will be generated after 5 scores have been added.

### Handicap File Structure

```
dodex-vertx/handicap
├── db
├── generate
│   └── src
│       └── main
│           ├── java
│           │   └── dmo
│           │       └── fs
│           │           ├── db
│           │           └── utils
│           ├── kotlin
│           │   └── golf
│           │       └── handicap
│           └── resources
└── src
    ├── grpc
    │   └── client
    │       ├── config
    │       ├── css
    │       │   └── dtsel
    │       ├── html
    │       ├── js
    │       │   ├── country-states
    │       │   │   └── js
    │       │   ├── dodex
    │       │   ├── dtsel
    │       │   ├── handicap
    │       │   │   ├── json
    │       │   │   └── protos
    │       │   └── validate
    │       ├── protos
    │       └── static
    ├── main
    │   ├── java
    │   │   └── dmo
    │   │       └── fs
    │   │           ├── db
    │   │           └── utils
    │   ├── kotlin
    │   │   └── golf
    │   │       └── handicap
    │   │           ├── db
    │   │           ├── generated
    │   │           │   ├── keys
    │   │           │   └── tables
    │   │           │       ├── records
    │   │           │       └── references
    │   │           ├── routes
    │   │           └── vertx
    │   ├── proto
    │   └── resources
    └── test
        └── kotlin
            ├── golf
            │   └── handicap
            └── resources

```