#!/bin/bash
cd ./file-server-api-distribute
sh installFileServerApi.sh
cd ..
docker-compose -f docker-compose.fastdfs.yml -f docker-compose.api.yml up -d --build file-server