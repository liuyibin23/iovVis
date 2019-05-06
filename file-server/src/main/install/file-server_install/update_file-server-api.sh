#!/bin/bash
cd ./file-server-api
sh installFileServerApi.sh
cd ..
docker-compose -f docker-compose.fastdfs.yml -f docker-compose.file-server-api.yml  up -d --build file-server
