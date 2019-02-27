#!/bin/bash
cd ./fastdfs_distributed_deployment
sh fdfsdockerinstall.sh
cd ../file-server-api-disribute
sh installFileServerApi.sh
cd ..
docker-compose -f docker-compose.fastdfs.yml -f docker-compose.api.yml up -d
