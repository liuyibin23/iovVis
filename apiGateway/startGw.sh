cd /opt/lvyu
svn co https://github.com/lvyv/iovVis/trunk/apiGateway
cd apiGateway
npm install
sudo kill -9 $(ps aux | grep [n]ode | awk '{print $2}')
sudo nohup node ./server > apiGw.log 2>apiGw.err &
echo ===restart apigateway ok ===