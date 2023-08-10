# enexa-service

Implementation of the ENEXA service



## Build the Dockerfile 

```shell
mvn package -DskipTests
docker build -t enexa-service:latest .
```

## Run the Docker image 




## For local testing

### Triplestore 

You will need a triplestore. 
The following command starts a Fuseki triplestore with the same configurations as is defined in fuseki-deployment.yaml.

```shell
docker pull stain/jena-fuseki
docker run -e ADMIN_PASSWORD=pw123 -e FUSEKI_DATASET_1=enexa -p 3030:3030 stain/jena-fuseki:latest 
```

The triplestore can be tested using this [query](http://localhost:3030/#/dataset/enexa/query?query=PREFIX%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0APREFIX%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0ASELECT%20%2A%20WHERE%20%7B%0A%20%20%3Fsub%20%3Fpred%20%3Fobj%20.%0A%7D%20LIMIT%2010).

### Starting the ENEXA Service

Requirements: You need to create the mentioned local directories (i.e., /tmp/modules, /tmp/enexa, /tmp/enexa-shared-dir-claim)

```shell
export ENEXA_MODULE_DIRECTORY=/tmp/modules; \
export ENEXA_SERVICE_URL=http://localhost:8080; \
export ENEXA_SHARED_DIRECTORY=/tmp/enexa; \
export ENEXA_SHARED_VOLUME=/tmp/enexa-shared-dir-claim; \
export ENEXA_META_DATA_ENDPOINT=http://localhost:3030/enexa/; \
export ENEXA_META_DATA_GRAPH=http://example.org/meta-data; \
export ENEXA_RESOURCE_NAMESPACE=http://example.org/resource/; \
java -jar target/enexa-service-0.0.1-SNAPSHOT.jar 
```

### running fuseki

```shell
sudo docker run --name fuseki-dev -p 3030:3030 -e FUSEKI_DATASET_1=mydataset -e FUSEKI_DATASET_2=otherdataset -e ADMIN_PASSWORD=pw123 -v /home/farshad/test/enexa/metadata:/fuseki stain/jena-fuseki
```


