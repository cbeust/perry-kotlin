FROM postgres:12.1

COPY perry.sql /docker-entrypoint-initdb.d/
#COPY perry.sql /app/perry.sql

# data on Ubuntu: /var/lib/postgresql/data

# docker run -v "/c/program files/Postgresql/11/data:/data" perry-db ls /data

# docker stop perry-db; docker rm perry-db; docker build -f Dockerfile-db -t perry-db:latest . && docker run --hostname db --rm --name perry-db -network host -e -d -p 5432:5432 perry-db

# psql -h 192.168.99.100 -p 6432 -U postgres --password

# docker exec -it perry-db bash

