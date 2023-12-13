# elastic-search-observable
Test SpringBoot project to illustrate tracing context propagation

## How to start
1. Start elasticsearch server
   ```
   docker run -d --name es762 -p 9200:9200 -e "discovery.type=single-node" -e "xpack.security.enabled=false" elasticsearch:8.10.4
   ````
2. Run application
   ```
   mvn spring-boot:run
   ```
3. Send request to controller
   ```
    curl localhost:8080/echo
   ```

