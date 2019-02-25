#!/bin/bash
set -e
docker-compose -f docker-compose.file-server-api.yml -f docker-compose.fastdfs.yml down