var http = require('http')

function getRandomIntInclusive(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min; 
  //The maximum is inclusive and the minimum is inclusive 
}  
var devHeadToken = '8HjSKu6151uiWNejE0uU'
var devTailToken = 'AoI6jmm0Vo9twSXezKeR'

var options = {
    host:'121.69.130.154',
    port:9099,
    path:'/api/v1/token/telemetry',
    method:'POST',
    headers:{
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36',
        'Content-Type' : 'application/json',// 不写这个参数，后台会接收不到数据
        'Content-Length' : 100 
    }
};

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

function postFun(token,index){
    if(index >= 10000)return;
    var paramStr = getMessage()
    options.headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36',
        'Content-Type' : 'application/json',// 不写这个参数，后台会接收不到数据
        'Content-Length' : paramStr.length
    }
    options.path = '/api/v1/'+ token +'/telemetry'
    var req = http.request(options,function(res){
        console.log('STATUS:' + res.statusCode);
        // console.log('HEADERS:' + JSON.stringify(res.headers));
        res.setEncoding('utf-8');
        res.on('data',function(body){
            // console.log('BODY：' + body);
        });
        res.on('end',function(){
            setTimeout(() => {
                // console.log(index)
                postFun(token,index + 1);
            },850)
        })
        req.on('err',function(err){
            if(e){
                console.info(e);
            }
        });
    });
    // console.log(paramStr);
    req.write(paramStr,'utf-8');
    req.end();
}

postFun(devHeadToken,0)
postFun(devTailToken,0)