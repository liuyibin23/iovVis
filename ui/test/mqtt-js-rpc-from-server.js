var mqtt = require('mqtt');
console.log("env:" + process.env.TOKEN);
// var client  = mqtt.connect('mqtt://yd.kfchain.com',{
var client = mqtt.connect('mqtt://192.168.1.76', {
    username: process.env.TOKEN
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