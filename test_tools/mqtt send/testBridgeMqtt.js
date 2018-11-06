var mqtt = require('mqtt');
var devHeadToken = 'eM1ZUGNbwkFNMqQdEBkS'
var devTailToken = 'HrvB8m2k3vtc2OxnVpwT'
var clientHead  = mqtt.connect({
    host:'127.0.0.1',
    // port: 9099,
    username: devHeadToken
});
var clientTail = mqtt.connect({
    host:'127.0.0.1',
    // port: 9099,
    username: devTailToken
});

var headHandle = null;
var tailHandle = null;
function getRandomIntInclusive(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min; 
  //The maximum is inclusive and the minimum is inclusive 
}  
function getMessage() {
    let strainoffset = getRandomIntInclusive(-5.0,5.0)
    let stressoffset = getRandomIntInclusive(-1.0,1.0)
    let shearoffset = getRandomIntInclusive(-10.0,10.0)
    let momentoffset = getRandomIntInclusive(-500.0,500.0)
    let tempoffset = getRandomIntInclusive(25.0,28.0)
    let humioffset = getRandomIntInclusive(65.0,70.0)
    let attr = getRandomIntInclusive(100.0,500.0)
    var currParam = {
        'strain': (121+strainoffset),
        'stress': (70+stressoffset),
        'shear': (700+shearoffset),
        'bending_moment': (5000+momentoffset),
        'temp': tempoffset,
        'humi': humioffset,
        'new_attr' : attr
    };
    return JSON.stringify(currParam)
}

clientHead.on('connect', function () {
    console.log('connected');
    clientHead.subscribe('v1/devices/me/rpc/request/+')
    /*
    headHandle = setInterval(() => {
        clientHead.publish('v1/devices/me/telemetry', getMessage());
    },950)*/
});

clientTail.on('connect', function () {
    console.log('connected');
    clientTail.subscribe('v1/devices/me/rpc/request/+')
    /*
    tailHandle = setInterval(() => {
        clientTail.publish('v1/devices/me/telemetry', getMessage());
    },950)*/
});

clientHead.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    if(JSON.parse(message).params === true)
    {
        headHandle = setInterval(() => {
            clientHead.publish('v1/devices/me/telemetry', getMessage());
        },950)
    } else {
        clearInterval(headHandle) 
    }

    var requestId = topic.slice('v1/devices/me/rpc/request/'.length);
    clientHead.publish('v1/devices/me/rpc/response/' + requestId, message);
});

clientTail.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    if(JSON.parse(message).params === true)
    {
        tailHandle = setInterval(() => {
            clientTail.publish('v1/devices/me/telemetry', getMessage());
        },950)
    } else {
        clearInterval(tailHandle) 
    }
    var requestId = topic.slice('v1/devices/me/rpc/request/'.length);
    clientTail.publish('v1/devices/me/rpc/response/' + requestId, message);
});