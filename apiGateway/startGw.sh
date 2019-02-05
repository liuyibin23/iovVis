cd /opt/lvyu
svn co https://github.com/lvyv/iovVis/trunk/apiGateway
cd apiGateway
sudo kill -9 $(ps aux | grep node | awk '{print $2}')
sudo nohup /usr/local/bin/node ./server > apiGw.log 2>apiGw.err &
