
### Kotlin, gRPC Web Application

### Getting Started

#### Client:

* The `html/javascript` client is generated from the `handicap/src/grpc/client` directory. 
    * Execute `npm install` to dowloand the javascript dependencies.

    * Execute `./proto protos/handicap` to generate the `proto3/gRPC` javascript modules. The proto3 configuration is in `./protos/handicap.proto`.

    * Execute either `npm run esbuild:build` or `npm run webpack:build` to package the javascript development client. The output is located in `handicap/src/main/resources/static`. When making changes to javascript/html/css, simply rerun `npm run esbuild:build`, if the verticle is running, it will rebuild. For proto3 changes, rerun `./proto protos/handicap` first.

        __Note:__ `esbuild` is best for development(very fast) and `webpack` is better for production, e.g. `npm run webpack:prod`.

#### Server:

* The server `proto3/gRPC` classes are auto generated from the gradle build script. 

* From the dodex-vertx directory, execute `gradlew run`. Or you can execute `gradlew run jooqGenerate` and then `gradlew run`. Running `gradlew run handicap:jooqGenerate` also works. The `jooqGenerate` task executes the `jooq` code generator. The generated code can be found in `handicap/src/main/kotlin/golf/handicap/generated`. Objects can then be used to define the queries.

    __Note__: `./gradlew run` is a 3 level composite build, it does the following:

    * compiles and executes the `jooq` java code generator located in `dodex-vertx/handicap/generate`, in development this also creates the database tables
    * compiles the java and kotlin code located in `dodex-vertx/handicap`
    * compiles and executes the java and kotlin code in `dodex-vertx` and `handicap`
<br/><br/>
* The next step is to install/startup the `envoy` proxy server. The javascript client needs a proxy to convert `http/1` to `http/2`. Assuming `envoy` is installed <https://www.envoyproxy.io/docs/envoy/latest/start/install>, execute the `start.envoy` script in `dodex-vertx`. The configuration for the proxy server is in `dodex-vertx/handicap/handicap.yaml`.

* If all goes well, only dodex-vertx at port `8087` should be started, the `handicap` application is turned off by default. If there are issues, execute `gradlew run` again. If this fails, execute `gradlew clean` and try `gradlew run` again. __Note:__ Setting `enableHandicap` to `true` in `.../resources/application-conf.json` will change the default startup.

* Kill `dodex-vertx`(ctrl-c), and set the environment variable `export USE_HANDICAP=true` and restart the server. In addition to the usual `8087` port display, there should also be a port `8888` display.

* In a browser enter `localhost:8888/handicap.html` or `localhost:8888/handicap/handicap.html`.

* The frontend html/gRPC javascript client should display. See operation section on how to use the form.
    
    __Note:__ Only `sqlite3`, `mariadb` and `postgres` support the handicap applicaiton. 
    
    * The default database is `sqlite3`. To change the default database, execute `export DEFAULT_DB=mariadb` or `export DEFAULT_DB=postgres`.
    * Also, if gradle generates the error "BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 63", change the `jdk` to `java17` and rerun. Afterwards, the `jdk` can be set back to say `java19`.

### Production Build

* In `dodex-vertx/handicap/src/grpc/client` execute `npm run webpack:prod`
* In `dodex-vertx` execute `./gradlew build` -  __Note:__ The default database should be `sqlite3`
* To start the production verticle, execute `java -jar build/libs/dodex-vertx-2.5.0.jar`
* For the stand alone handicap application execute `java -jar handicap/build/libs/handicap-0.0.1-fat.jar`

### Operation

The following are the steps to add golfer info, courses with tee data and golfer scores.

* First time login simply enter a `pin`(2 alpha characters with between 4-6 addition alpha-numeric characters) with first and last name. Click the login button to create/login to the application. On subsequent logins only the `pin` is needed, don't forget it. The `Country` and `State` should also be selected before creating a `pin` as default values. However, you can change the defaults on any login. Also, `Overlap Years` and `Public` should be set.
    * Overlap will use the previous year's scores for the handicap if needed.
    * Public will allow your scores to show up in the `Scores` tab for public viewing.
<br/>
<br/>
* Add a course by entering it's name with one radio button selected for the tee. You can also change the tee's color. The `rating`, `slope` and `par` values are also required. Click the `Add Tee` button. After the first added tee, the others can be added when needed.

    __Note__: You can disable the course/tee add function by setting `handicap.enableAdmin` to `true` in the `...\resources\application-conf.json` file. And then use the default `admin.pin` to administer the courses/tees. When using this pin, a first and last name must be entered on initial use.

* To add a score, select a course and tee with values for `Gross Score`, `Adjusted Score` and `Tee Time`. Click the `Add Score` button to add the score. The `Remove Last Score` will remove the last entered score, mutiple clicks will remove mutiple scores.

__Note:__ A handicap will be generated after 5 scores have been added.

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