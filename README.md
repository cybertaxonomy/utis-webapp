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

Developer Notes
--------------------

## Using swagger

* https://github.com/martypitt/swagger-springmvc
* https://github.com/adrianbk/swagger-springmvc-demo/tree/master/swagger-ui


## On Spring MVC
Content Negotiation Using Spring MVC
* http://java.dzone.com/articles/content-negotiation-using