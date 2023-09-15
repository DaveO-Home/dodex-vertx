
# doDex-vertx, a java asynchronous server for Dodex, Dodex-input and Dodex-mess

## Install Assumptions

1. Using Vertx4 @ <https://vertx.io/introduction-to-vertx-and-reactive/>
2. Java 17 or higher installed with JAVA_HOME set.
3. Gradle 8+ installed(app will install gradle 8). If you have sdkman installed, execute `sdk install gradle 8.0.2`
4. Node with npm javascript package manager installed.

**Important Note:** The **`./gradlew run`** is much more complex out of the box. The `kotlin, gRPC` web applicaion requires a `gradle` composite build configuration. See the `Kotlin, gRPC Web Application` section below.

## Getting Started

1. `npm install dodex-vertx` or download from <https://github.com/DaveO-Home/dodex-vertx>. If you use npm install, move node_modules/dodex-vertx to an appropriate directory.
2. `cd <install directory>/dodex-vertx/src/main/resources/static` and execute `npm install --save` to install the dodex modules.
3. `cd <install directory>/dodex-vertx` and execute `./gradlew run`. This should install java dependencies and startup the server in development mode against the default sqlite3 database. In this mode, any modifications to java source will be recompiled.
4. Execute url `http://localhost:8087/test` in a browser.
5. You can also run `http://localhost:8087/test/bootstrap.html` for a bootstrap example.
6. Follow instructions for dodex at <https://www.npmjs.com/package/dodex-mess> and <https://www.npmjs.com/package/dodex-input>.
7. You can turn off colors by setting "color": to false in application-conf.json.
8. The Cassandra database has been added via an `Akka` micro-service. See; <https://www.npmjs.com/package/dodex-akka>.
9. Added Cassandra database to the `React` demo allowing the `login` component to use Cassandra.
10. See the `Firebase` section for using Google's `Firestore` backend.

### Operation

1. Execute `.\gradlew tasks` to view all tasks.
2. Execute `.\gradlew shadowJar` to build the production fat jar.
3. Execute `java -jar build/libs/dodex-vertx-3.1.0-prod.jar` to start up the production server.
4. Execute url `http://localhost:8880/dodex` or `.../dodex/bootstrap.html` in a browser. 
   * **Note:** This is a different port and url than development. 
   * **Note:** The default database on the backend is "Sqlite3", no further configuation is necessay. Dodex-vertx also has Postgres/Cubrid/Mariadb/Ibmdb2/Cassandra/Firebase implementations. See `<install directory>/dodex-vertx/src/main/resources/static/database_config.json` for configuration.
5. Swapping among databases; Use environment variable `DEFAULT_DB` by setting it to either `sqlite3` ,`postgres`, `cubrid`, `mariadb`, `ibmdb2`, `cassandra`, `firebase` or set the default database in `database_config.json`.
6. The environment variable `VERTXWEB_ENVIRONMENT` can be used to determine the database mode. It can be set to either ``prod`` or unset for production and ``dev`` for the development database as defined in ``database_config.json``.
7. When Dodex-vertx is configured for the Cubrid database, the database must be created using UTF-8. For example `cubrid createdb dodex en_US.utf8`.
8. Version 1.3.0 adds an auto user clean up process. See `application-conf.json` for configuration. It is turned off by default. Users and messages may be orphaned when clients change a handle when the server is offline.

## Debug

* Execute `gradlew run -DDEBUG=true` to debug the Vertx Vertical.
* The default port is 5005, see `build.gradle` to change.
* Tested with VSCode, the `launch.json` =
  
`javascript
    {
            "type": "java",
            "name": "Debug (Launch) - Dodex",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
    }
`

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
  3. Execute `<graalvm directory>/bin/gu install native-image` to install the `native-image` program.
  4. In the dodex-vertx directory execute `gradlew installDist` and `gradlew shadowJar`.
  5. Modify the dodexvm8/vm11 or dodexvm.bat shell to the graalvm install directory.
  6. In your dodex-vertx directory execute the dodexvm8 or dodexvm11 shell program. This should create an executable named `io.vertx.core.Launcher`.
  7. Execute the production server with `io.vertx.core.Launcher`.

   **Note:** dodex-vertx will not build as a stand alone, therefore, if you move the executable to different directory, you must also move the shadowJar to the same directory with structure `build\libs`.

### Single Page React Application to demo Development and Integration Testing

* Integrated in ***Dodex-Vertx*** at `src/spa-react`
* Documentation <https://github.com/DaveO-Home/dodex-vertx/blob/master/src/spa-react/README.md>
* Uses ***Sqlite3*** as backend database
* Router added to `src/main/java/dmo/fs/vertx/Server.java`

### Development Environment in a docker container using CentOS

* Assumes **docker** is installed with the daemon running and the **envoy proxy** is running on the host.

  1. From dodex-vertx directory; execute **`centos/buildcontainer`** or on windows **`centos\buildcontainer`**
  2. Execute `gradlew run` to start development. Execute `ctl-c` && `exit` to shutdown.
  3. Execute `centos/execontainer` or `centos\execontainer` on subsequent startups.

### Firebase

* Create an account: <https://firebase.google.com>
* Getting started: <https://firebase.google.com/docs/admin/setup#java>
* Make sure you create a `Service-Account-Key.json` file as instructed. Dodex-Vertx uses the environment variable option to set the service-account - `GOOGLE_APPLICATION_CREDENTIALS`. See gradle.build as one way to set it.
* You will need to login to the `Firebase` console and create the `dodex-firebase` project. See `src/main/java/dmo/fs/router/FirebaseRouter.java` for usage of the project-id and Google Credentials. **Note:** The `Firebase` rules are not used, so they should be set to `allow read, write:  if false;` which may be the default.
* You only need the `Authentication` and `Firestore` extensions.
* If you want a different project name, change `.firebaserc`.

  #### Testing

  * To make sure your project is created and the setup works, you should run the tests. **Note:** They are written in Typescript.
  * cd `../dodex-vertx/src/firebase` and run `npm install`
  * execute `npm run emulators` to startup the emulators for testing.
  * To test the model and rules after starting the emulators, in a different terminal window, run `npm test`.

### Neo4j

* See <https://neo4j.com/docs/operations-manual/current/> for usage.
* To use a container with `apoc` you can try: **Note:** this has `--privileged` set.
    ```
    docker run \
    -p 7474:7474 -p 7687:7687 \
    -v $PWD/neo4j/data:/neo4j/data -v $PWD/neo4j/plugins:/neo4j/plugins \
    --name neo4j-apoc \
    --privileged \
    -e 'NEO4J_AUTH=neo4j/secret' \
    -e NEO4J_apoc_export_file_enabled=true \
    -e NEO4J_apoc_import_file_enabled=true \
    -e NEO4J_apoc_import_file_use**neo4j**config=true \
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
    *  **set environment variable `DODEX_KAFKA=true`** or 
    *  **set "dodex.kafka" to true** in the `application-conf.json` file(default is false)
    *  startup Vertx - the monitor should work with any of the databases
    *  the monitor configuration can be found in `application-conf.json`

* Monitor Dodex
    * in a browser enter `localhost:8087/monitor` or `localhost:8880/monitor` in production.
    * as dodex messaging executes the events should be recorded.
    * in the browser's `developer tools` console execute `stop();` and `start();` to stop/start the polling. Polling is started by default.
    
    **Note:** you can open the messaging dialog with `ctrl-doubleclick` on the dials

## Dodex Groups using OpenAPI

* A default javascript client is included in __.../dodex-vertx/src/main/resources/static/group/__. It can be regenerated in __.../dodex-vertx/handicap/src/grpc/client/__ by executing __`npm run group:prod`__.  
* The group javascript client is in __.../src/grpc/client/js/dodex/groups.js__ and __group.js__.  
    __Note:__ The client is included in the __Handicap__ application by default.  
* See __.../src/main/resources/openapi/groupApi31.yml__ for OpenAPI declarations. The Vert.x implementation generates routes for __.../src/main/java/dmo/fs/router/OpenApiRouter.java__ 
  
### Installing in Dodex
1. Implementing in a javascript module; see __.../dodex-vertx/handicap/src/grpc/client/js/dodex/index.js__
   * `import { groupListener } from "./groups";`
   * in the dodex init configuration, add  
    ```
   ...
    .then(function () {
         groupListener();
   ...
    ```
2. Implementing with inline html; see __.../dodex-vertx/main/resources/test/index.html__
   * `<script src="../group/main.min.js"></script>`
   * in the dodex init configuration, add
    ```
   ...
    .then(function () {
         window.groupListener();
   ...
    ```
3. Using dodex-messaging group functionality  
   __Note:__ Grouping is only used to limit the list of "handles" when sending private messages.
* Adding a group using `@group+<name>`
  * select __Private Message__ from the __more__ button dropdown to get the list of handles.
  * enter `@group+<name>` for example `@group+aces`
  * select the handles to include and click "Send". Members can be added at any subsequent time.
* Removing a group using `@group-<name>`
  * enter `@group-<name>` for example `@group-aces` and click "Send". Click the confirmation popup to complete.
* Removing a member
  * enter `@group-<name>` for example `@group-aces`
  * select a "handle" from the dropdown list and click "Send"
* Selecting a group using `@group=<name>`
  * enter `@group=<name>` for example `@group=aces` and click "Send"
  * Select from reduced set of "handles" to send private message.  

__Note:__ By default the entry `"dodex.groups.checkForOwner"` in __application-conf.json__ is set to __false__. This means that any "handle" can delete a "group" or "member". Setting the entry to __true__ prevents global administration, however, if the owner "handle" changes, group administration is lost.

## Kotlin, gRPC Web Application

* This web application can be used to maintain golfer played courses and scores and to calculate a handicap index. The application has many moving parts from the **envoy** proxy server to **kotlin**, **protobuf**, **gRPC**, **jooq**, **bootstrap**, **webpack**, **esbuild**, **gradle**, **java** and **javascript**.

    See documentation at; <https://github.com/DaveO-Home/dodex-vertx/blob/master/handicap/README.md>

## Docker, Podman and Minikube(Kubernetes)
* Assumes **docker**, **podman** and **minikube** are installed

### Building an *image* and *container* with docker
1. cd to the **dodex-vertx** install directory
2. make sure **dodex** and the **handicap** node_modules and application are installed

    * in `src/main/resources/static` execute __`npm install`__
    * in `handicap/src/grpc/client` execute __`npm install`__ and __`npm run webpack:prod`__ or __`npm run esbuild:prod`__
    * startup Vertx in dev mode - __`gradlew run`__
    * optionally install the __spa_react__ application and in **src/spa-react/devl** execute __`npx gulp prod`__ or __`npx gulp prd`__(does not need dodex-vertx started)
    * stop the vertx server - ctrl-c
    * build the production fat jar - execute __`./gradlew clean(optional) shadowJar`__
        * **Important** When building the **Fat** jar, set **DEFAULT_DB**=sqlite3 or postgres and **USE_HANDICAP**=true
    * verify the jar's name - if different than `dodex-vertx-3.1.0-prod.jar`, change in **./kube/Dockerfile** and **run_dodex.sh**

3. execute __`cp build/libs/dodex-vertx-3.1.0-prod.jar`__ to **kube/**
4. execute __`docker build -t dufferdo2/dodex-vertx:latest -f kube/Dockerfile ./kube`__
5. execute __`docker create -t -p 8880:8880 -p 8070:8070 -p 9901:9901 --name dodex_vertx dufferdo2/dodex-vertx`__
6. execute __`docker start dodex_vertx`__, make sure **envoy** is not running on the host.
7. use browser to view - <http://localhost:8880/handicap.html> or <http://localhost:8880/dodex> or <http://localhost:8880/dodex/bootstrap.html>, if the spa-react was installed this link should work, <http://localhost:8880/dist/react-fusebox/appl/testapp.html>
8. execute __`docker stop dodex_vertx`__
9. to clean-up execute __`docker rm dodex_vertx`__ and __`docker rmi dodex-vertx`__. However you should keep the **dufferdo2/dodex-vertx** image if trying out **podman** or **minikube**.
10. to pull and generate a local image from the docker hub, execute __`docker build -t dodex-vertx:latest -f kube/vertx/Dockerfile .`__
11. you can also build/run dufferdo2/dodex-vertx(image) and dufferdo2/dodex_vertx(container) with; __`docker compose -f kube/docker-compose.yaml up -d`__, assumes that **envoy** is running for **dodex-vertx**
12. Use `run` to test different databases; __`docker run --rm -p 8880:8880 -p 8070:8070 -p 9901:9901 -e DEFAULT_DB=postgres -e USE_HANDICAP=true --name dodex_vertx dufferdo2/dodex-vertx`__. To stop, run `docker container stop dodex_vertx`.

__Note:__ When running the dufferdo2/dodex-vertx image based on ./kube/Dockerfile, there is no need to have **envoy** running on the host machine. Envoy is included in the image.

### Building an *image* and *container* with podman
1. generate an empty pod execute __`podman pod create -n vertx-pod -p 0.0.0.0:8880:8880 -p 0.0.0.0:8070:8070 -p 9901:9901`__
2. generate a container execute __`podman create -t --pod vertx-pod --name vertx_server dufferdo2/dodex-vertx:latest`__.
3. start the container execute __`podman start vertx_server`__
4. view in browser
5. to clean-up execute __`podman stop vertx_server`__, __`podman rm vertx_server`__, __`podman pod rm vertx-pod`__
6. before cleaning up, you can generate a yaml template. Execute __`podman generate kube vertx-pod > vertx.yaml`__

### Building a *deployment*, *service* and *persistent volume* with minikube
* Since including the **Handicap** application(multiple exposed ports, persistent volume) to **dodex-vertx**, the **minikube** deployment must be from configuration files.
1. execute __`minikube start`__
2. to make sure the dufferdo2/dodex-vertx image is setup, execute __`docker build -t dufferdo2/dodex-vertx:latest -f kube/Dockerfile ./kube`__
3. edit kube/vertx.yml and change **env:** to desired database(DEFAULT_DB) - defaults to **sqlite3**, no database configuration necessary otherwise set **DEFAULT_DB** to **mariadb** or **postgres**
4. execute `kubectl create -f kube/sqlite3-volume.yml`
5. execute `kubectl create -f kube/vertx.yml`
6. execute `minikube service vertx-service` to start **dodex-vertx** in the default browser - add **--url** to get just the URL
7. verify that **dodex-vertx** started properly - execute `./execpod` and `cat ./logs/vertx.log` - enter `exit` to exit the pod  

For postgres make sure postgres.conf has entry:  
 
```
             listen_addresses = '*'          # what IP address(es) to listen on;
```                
and  pg_hba.conf has entry:  

```
             host    all    all    <ip from minikube vertx-service --url>/32   <whatever you use for security> (default for dodex-vertx "password")
```                   
and database_config.json(also in ../dodex-vertx/generate...resources/database(_spa)_confg.json) entry: postgres... (both dev/prod)  
```          
              "config": {
              "host": "<ip value from `hostname -i`>",
```
`netstat -an |grep 5432` should look like this  

```
             tcp        0      0 0.0.0.0:5432            0.0.0.0:*               LISTEN     
             tcp6       0      0 :::5432                 :::*                    LISTEN     
             unix  2      [ ACC ]     STREAM     LISTENING     57905233 /var/run/postgresql/.s.PGSQL.5432
             unix  2      [ ACC ]     STREAM     LISTENING     57905234 /tmp/.s.PGSQL.5432
```

#### Development
1. Make changes to the **dodex-vertx** code
2. execute `gradlew clean`(optional)
3. build the **fat** jar and **image** as described in the **Operation** and **Building an *image* and *container* with docker** sections, e.g.
    * build the production fat jar - __`./gradlew shadowJar`__
        * **Important** When building the **fat** jar, set **DEFAULT_DB**=sqlite3 or mariadb or postgres and **USE_HANDICAP**=true
        * verify the jar's name - if different than **dodex-vertx-3.1.0-prod.jar**, change in **./kube/Dockerfile**
    * copy the build/**dodex-vertx-3.1.0-prod.jar** to **./kube**
    * if the **dodex_vertx** and/or the **dufferdo2/dodex-vertx** exist, remove them `docker rm dodex_vertx` and `docker rmi dufferdo2/dodex-vertx`
    * build the image `docker build -t dufferdo2/dodex-vertx:latest -f ./kube/Dockerfile ./kube`
4. execute `./deleteapp`
5. execute `minikube image rm dufferdo2/dodex-vertx`
6. execute `minikube load image dufferdo2/qodex-vertx`
7. execute `kubectl create -f kube/vertx.yml`
8. execute `minikube service vertx-service`
9. clean-up execute __`./deleteapp`__, __`kubectl delete pvc vertx-pvc`__, __`kubectl delete pv vertx-pv`__, __`minikube image rm dufferdo2/dodex-vertx`__
10. execute __`minikube stop`__

#### Exposing the minikube **dodex-vertx** container to the internet
1. cd .../dodex-vertx and execute **`npm install`** - this will install **localtunnel**
2. execute `minikube service vertx-service --url` to view the local host **ip** address - can be used for the **--local-host** value
3. in separate terminals
    * execute `npx localtunnel  --host https://localtunnel.me --subdomain my-app --port 30080 --local-host $(minikube service vertx-service --url | cut -d":" -f2 | cut -d"/" -f3)`
    * for the gRPC tunnel, execute `npx localtunnel  --host https://localtunnel.me --subdomain my-app2 --port 30070 --local-host $(minikube service vertx-service --url | cut -d":" -f2 | cut -d"/" -f3)`
        * the **--subdomain** for **my-app** and **my-app2** should be changed to unique values
        * the naming convention is required(otherwise edit src/grpc/client/js/client.js and tweak) e.g. **coolapp** for port 30080 and **coolapp2** for port 30070
    * view <https://YOUR-UNIQUE-APP.loca.lt> or <https://YOUR-UNIQUE-APP.lt/handicap.html> in browser

   __Note:__ Make sure your Ad-Blocker is turned off for the web site.

4. The client also supports **loophole** <https://loophole.cloud/docs>. Much better bandwidth running out of Europe and is also free.
    * loophole http 30080 $(minikube service vertx-service --url | cut -d":" -f2 | cut -d"/" -f3) --hostname my-coolapp
    * loophole http 30070 $(minikube service vertx-service --url | cut -d":" -f2 | cut -d"/" -f3) --hostname my-coolapp2

    __Note:__ You will have to create a login to use **loophole**.


## ChangeLog

<https://github.com/DaveO-Home/dodex-vertx/blob/master/CHANGELOG.md>

## Authors

* *Initial work* - [DaveO-Home](https://github.com/DaveO-Home)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
