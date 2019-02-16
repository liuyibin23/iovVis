#!/bin/sh
cd /opt/apiGateway
sudo nohup /usr/local/bin/node ./server > log/apitGw.log 2>apGw.err &

