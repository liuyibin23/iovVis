var mqtt = require('mqtt');
// console.log("env:" + process.env.TOKEN);
// var client  = mqtt.connect('mqtt://yd.kfchain.com',{
var client = mqtt.connect('mqtt://cf.beidouapp.com', {
    username: 'GbGuHQkbgeQcAoFd3GLF'    //A监测点
    // username: 'mvjPD7zvq7CqkzmyPOSi'    //A巡检员
});

client.on('connect', function () {
    console.log('connected');
    client.subscribe('v1/devices/me/rpc/request/+')
});

client.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    var requestId = topic.slice('v1/devices/me/rpc/request/'.length);
    //client acts as an echo service
    // console.log('publish to:', message);
    client.publish('v1/devices/me/rpc/response/' + requestId, message);
    // client.publish('v1/devices/me/rpc/response/' + requestId, "{'platform':'linux'}");
});