# UTIS - README




## Installation

At the example of jetty8 in Debian like systems:

### Requirements

Java runtime environment 1.8 

## setting up with docker-compose

here we are using the official `jetty:9.4-jre8-slim` docker image that is provided by the jetty project on [Docker hub](https://hub.docker.com/_/jetty)

Requirement: Install **docker** and **docker-compose**


~~~
mkdir /opt/jetty9-docker
cd /opt/jetty9-docker
~~~

create user and group that are expected in the jetty9.4 container with the respective UID

~~~
addgroup --quiet --system jetty
adduser --quiet --system --ingroup jetty --no-create-home --disabled-password --uid 999 jetty
~~~

create a `docker-compose.yaml` file with the following content:

~~~
version: "2.0"
services:
    utis-jetty:
        restart: unless-stopped
        image: jetty:9.4-jre8-slim
        ports:
        - "8080:8080"
        volumes:
        - ./webapps:/var/lib/jetty/webapps
        - ./log:/usr/local/jetty/log
        - ./utis:/var/lib/jetty/utis
        user: jetty
~~~

create the folders to be bound to the container

~~~
mkdir log utis webapps
~~~

copy the utis war file to the webapps folder

~~~
cp $WORKSPACE/target/eubon-utis.war webapps/eubon-utis.war
~~~

Set the permissions so that the user jetty has read and write access to these folders: 

~~~
chown -R jetty:jetty log utis webapps
chmod 774 jetty:jetty log utis webapps
~~~

start the docker container

~~~
docker-compose up -d
~~~

## Service end-point URLs

utis controllers:

* http://127.0.0.1:8080/eubon-utis/
* http://127.0.0.1:8080/eubon-utis/search.html
* http://127.0.0.1:8080/eubon-utis/capabilities.html

swagger api-doc REST service at:

* http://127.0.0.1:8080/eubon-utis/api-docs.json
* http://127.0.0.1:8080/eubon-utis/api-docs/default/utis-controller.json

swagger ui at:

* http://127.0.0.1:8080/eubon-utis/doc/

## Logfiles

Since version 1.3 on linux systems the logfiles are located at `/var/log/utis`. Previous versions of utis put the logfiles in `/var/log/jetty8`.
The `ContextDependentInitializer` may choose to place the logs into another  directory if it is not possible to write the logs into `/var/log/utis`. 
Please refer to this class for further details.

## Development

### Running in dev mode

UTIS can be configuired for easier development. This encompasses two java system properties which can be specifed by passing environment variables to the jvm:


#### `excludedClients`

    -DexcludedClients=[Client class simple names comma separated]
    
The client adapters identified by their simple class name will be disabled. See `org.bgbm.utis.controller.UtisController` line 9ff for implementation details.

e.g:
     
    -DexcludedClients=EUNIS_Client,GBIFBackboneClient,PlaziClient
    
will disable the named clients "EUNIS_Client,GBIFBackboneClient,PlaziClient" which have time and cpu consuming startup phases.

### `skipStoreUpdating`

This option will cause the `org.cybertaxonomy.utis.store.Neo4jStoreUpdater` to completely skip the continiuous updating of the the cached data which 
is otherwise fetched from the source on a periodic base:

    -DskipStoreUpdating


### Using swagger

* https://github.com/martypitt/swagger-springmvc
* https://github.com/adrianbk/swagger-springmvc-demo/tree/master/swagger-ui


### On Spring MVC
Content Negotiation Using Spring MVC
* http://java.dzone.com/articles/content-negotiation-using