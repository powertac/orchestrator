# PowerTAC Experiment Scheduler

## Docker Compose deployment

### Requirements

To run the PowerTAC Experiment Scheduler inside containers as a Docker Compose service you only need to install the
following dependencies:

- Docker: https://www.docker.com/get-started
- Docker Compose: https://docs.docker.com/compose/install/

### Configuration

The services are configured via an env file. A basic configuration can be found in `.../deployment/example.env`. 

For the minimal configuration you should specify/change the following environment variables:

- `EXPERIMENT_SCHEDULER_ROOT`: the root directory in which all data related to the experiment scheduler is stored
- `MYSQL_STORAGE_PATH`: path to the directory where the database data is stored
- `MYSQL_PASSWORD`: password for the MySQL/MariaDB database

### Running the services

After adjusting the base configuration to your needs, open a terminal, change to the deployment directory and run the
`docker-compose` command as follows:

```bash
$ docker-compose -f docker-compose.yml --env-file example.env -p powertac_es up
```

## Manual installation

### Requirements
To run the Experiment Scheduler as local services directly on the host machine the following dependencies must be
installed on your system:

- Docker: https://www.docker.com/get-started
- Java 21: http://openjdk.java.net/
- Apache Maven: http://maven.apache.org/
- NodeJS & NPM: Download (https://nodejs.org/en/download/) or install via package manager (https://nodejs.org/en/download/package-manager/)
- A MySQL compliant database
  - MariaDB: https://mariadb.org/
  - MySQL: https://dev.mysql.com/doc/refman/8.0/en/installing.html

Please make sure that there is sufficient storage space for the broker images as well as the log files.

*At this time the Orchestrator setup is only tested on a Linux system. The following instructions describe the
installation and execution of the Orchestrator on Ubuntu 18.04. The installation on Windows or MacOS might differ.*

### MySQL database

Please make sure that your MySQL database is running and accessible from the host system. Create a database as well as a
user with access to this database.

### Orchestrator
To install the orchestrator download or clone this repository (https://github.com/powertac/powertac-experiment-scheduler).

Now switch to the orchestrator's root directory and run the Maven `install` goal to download the required dependencies
and install the orchestrator:

```bash
$ mvn install
```

Afterwards, create a file called `application.properties` in the root folder of the orchestrator and add/edit the parameters below.
If your database is running on your local system the MySQL host(`<MYSQL_HOST>`) should be `localhost`. The default port
for the MySQL database (`<MYSQL_PORT>`) is `3306`. Please make sure that the path you enter as `directory.local.base` is
writeable by the user that will run the orchestrator.

```properties
spring.datasource.username=<MYSQL_USERNAME>
spring.datasource.password=<MYSQL_PASSWORD>
spring.datasource.url=jdbc:mysql://<MYSQL_HOST>:<MYSQL_PORT>/<POWERTAC_DATABASE>
directory.local.base=/path/to/your/powertac/directory/
```

**Please be aware: the path to the orchestrator's base directory must end with a `/` or `\` (depending on you operating system).**

### Web UI
To install the web UI download or clone this repository (https://github.com/powertac/experiment-scheduler-ui).

Switch to its root directory and run `npm install` to download and install its dependencies.


### Docker
The orchestrator requires access to the Docker daemon. Therefore the user that will be running the orchestrator needs
permissions. Please refer to the Docker documentation for details:
https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user.

### Running the scheduler
Start the orchestrator by running the Maven `exec` command. The orchestrator will listen by default to port 8080.

```bash
$ mvn exec:java
```

If the orchestrator is started for the first time it will download the server images and build the broker images.
Therefore it will take some time until the service is available.

Once the orchestrator is ready you can start the web ui by running the `npm run` command:

```bash
$ npm run dev
```

The web UI will be reachable by default via http://localhost:9000. You can run the web UI on a different port by using
the following command, where `<PORT>` represents the desired port:

```bash
$ npm run serve -- --port <PORT> --mode development
```

## Adding brokers

Once you have created broker Docker images you can add them by using the "Brokers" navigation option in the UI. You can
find and build a basic set of brokers by cloning and following the instructions provided by the
[powertac/broker-images](https://github.com/powertac/broker-images) repository.

## Known issues

- The UI currently doesn't react correctly to some changes to experiments and game statuses. Reloading the page should 
do the trick.
- In some cases when a game's containers have been created but not started and the orchestrator shuts down, the created
containers are not removed and the orchestrator will throw an error due to this conflict. When this happens, shut down
the orchestrator, remove the containers manually (`docker rm`) and restart the orchestrator.
