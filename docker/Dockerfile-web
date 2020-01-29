FROM openjdk:8

COPY [ "perry-1.0-SNAPSHOT-all.jar", "/app/perry.jar" ]
COPY [ "config.yml", "/app" ]
COPY [ "local.properties", "/app/local.properties" ]

WORKDIR "/app"

CMD [ "java", "-jar", "/app/perry.jar", "server", "config.yml" ]

# docker stop perry-web; docker rm perry-web; docker build -f Dockerfile-web -t perry-web:latest . && docker run --rm -d --name perry-web --network bridge -e IS_DOCKER=1 -p 9000:9000 perry-web:latest && docker ps && docker logs perry-web -f

