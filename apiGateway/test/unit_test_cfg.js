var should = require('chai').should(),
    expect = require('chai').expect,
    supertest = require('supertest'),
    api = supertest('http://localhost:20050/api/v1/');

// 公用API
exports.API = api;
exports.expect = expect;

// 单元测试参数
exports.unitTestParams = {
     assetId:'a4066dc0-1a63-11e9-81c9-97b575a68222',
     assetIdError: 'xxxxxxx',
     deviceId :'50f94370-5bfc-11e9-ada3-e31340b22c5e',
     deviceIdError : 'xxxxxxx',
     token : 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4ODc0NTk2NUBxcS5jb20iLCJzY29wZXMiOlsiVEVOQU5UX0FETUlOIl0sInVzZXJJZCI6ImYwNjNmOTQwLTE1YWQtMTFlOS1iNWQ3LTgzNzBlNzFjZWI4ZSIsImZpcnN0TmFtZSI6Iuafs-W8uuS4nCIsImxhc3ROYW1lIjoiIiwiZW5hYmxlZCI6ZmFsc2UsImlzUHVibGljIjpmYWxzZSwidGVuYW50SWQiOiI4MzA5NTdkMC1lZTFkLTExZTgtOWVjYS0xMTliYzhmYTkwZGYiLCJjdXN0b21lcklkIjoiMTM4MTQwMDAtMWRkMi0xMWIyLTgwODAtODA4MDgwODA4MDgwIiwiaXNzIjoiYmVpZG91YXBwLmNvbSIsImlhdCI6MTU1Nzg4MjA0OCwiZXhwIjoxNTU3OTE4MDQ4fQ.QC_KOeyQyAX2HksDnER-bkJsOLq6XsCTniPfgEn7R9w8BXa30vxx6O0aXDqKHSMuuhvqAkZULEQ8FSOYwMT1dQ', 
     sysAdminToken: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzaGVuamlAYmVpZG91YXBwLmNvbSIsInNjb3BlcyI6WyJTWVNfQURNSU4iXSwidXNlcklkIjoiYTYxMjNkMDAtMTRiNS0xMWU5LTlmMTgtZmZmMWU0ZGNiNDA4IiwiZmlyc3ROYW1lIjoic2hlbiIsImxhc3ROYW1lIjoiamkiLCJlbmFibGVkIjp0cnVlLCJpc1B1YmxpYyI6ZmFsc2UsInRlbmFudElkIjoiMTM4MTQwMDAtMWRkMi0xMWIyLTgwODAtODA4MDgwODA4MDgwIiwiY3VzdG9tZXJJZCI6IjEzODE0MDAwLTFkZDItMTFiMi04MDgwLTgwODA4MDgwODA4MCIsImlzcyI6ImJlaWRvdWFwcC5jb20iLCJpYXQiOjE1NTc4ODE5NzcsImV4cCI6MTU1NzkxNzk3N30.DJfNt_0l5s4nOPxPQoLQSbm-iWA4zk0-bIXeJHUcqZMLtslU37115Zy9gVsBT-sV-t0jNTCdYIiPhh8qLfhoEQ'
};