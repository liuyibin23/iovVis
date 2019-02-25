#!/bin/bash
cd ./fastdfs
sh fdfsdockerinstall.sh
cd ../file-server-api
sh installFileServerApi.sh
cd ..
docker-compose -f docker-compose.fastdfs.yml -f docker-compose.file-server-api.yml up -d
