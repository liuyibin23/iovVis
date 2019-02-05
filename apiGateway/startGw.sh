sudo kill -9 $(ps aux | grep node | awk '{print $2}')
sudo nohup /usr/local/bin/node ./server > apiGw.log 2>apiGw.err &
