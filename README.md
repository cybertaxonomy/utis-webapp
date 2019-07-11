UTIS - README
====================



Installation
-----------------------------

At the example of jetty8 in Debian like systems:

**Requirements**
You need to run utis with an Oracle JVM 1.7

1. place the eubon-utis.war in /var/lib/jetty8/webapps/
2. create the folder for local stores: 
   sudo mkdir /var/lib/utis
   sudo chown -R jetty:adm  /var/lib/utis
3. Start jetty
   /etc/init.d/jetty8 start   


The URIs UTIS is listening at
------------------------------

utis controllers:
* http://127.0.0.1:8080/eubon-utis/
* http://127.0.0.1:8080/eubon-utis/search.html
* http://127.0.0.1:8080/eubon-utis/capabilities.html

swagger api-doc REST service at:
* http://127.0.0.1:8080/eubon-utis/api-docs.json
* http://127.0.0.1:8080/eubon-utis/api-docs/default/utis-controller.json

swagger ui at:
* http://127.0.0.1:8080/eubon-utis/doc/

Logfiles
-----------------------------

Since version 1.3 on linux systems the logfiles are located at `/var/log/utis`. Previous versions of utis put the logfiles in `/var/log/jetty8`.
The `ContextDependentInitializer` may choose to place the logs into another  directory if it is not possible to write the logs into `/var/log/utis`. 
Please refer to this class for further details.

Development
--------------------

## Running in dev mode

UTIS can be configuired for easier development. This encompasses two java system properties which can be specifed by passing environment variables to the jvm:


### `excludedClients`

    -DexcludedClients=[Client class simple names comma separated]
    
The client adapters identified by their simple class name will be disabled. See `org.bgbm.utis.controller.UtisController` line 9ff for implementation details.

e.g:
     
    -DexcludedClients=EUNIS_Client,GBIFBackboneClient,PlaziClient
    
will disable the named clients "EUNIS_Client,GBIFBackboneClient,PlaziClient" which have time and cpu consuming startup phases.

### `skipStoreUpdating`

This option will cause the `org.cybertaxonomy.utis.store.Neo4jStoreUpdater` to completely skip the continiuous updating of the the cached data which 
is otherwise fetched from the source on a periodic base:

    -DskipStoreUpdating


## Using swagger

* https://github.com/martypitt/swagger-springmvc
* https://github.com/adrianbk/swagger-springmvc-demo/tree/master/swagger-ui


## On Spring MVC
Content Negotiation Using Spring MVC
* http://java.dzone.com/articles/content-negotiation-using