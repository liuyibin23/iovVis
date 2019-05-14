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
     token : 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4ODc0NTk2NUBxcS5jb20iLCJzY29wZXMiOlsiVEVOQU5UX0FETUlOIl0sInVzZXJJZCI6ImYwNjNmOTQwLTE1YWQtMTFlOS1iNWQ3LTgzNzBlNzFjZWI4ZSIsImZpcnN0TmFtZSI6Iuafs-W8uuS4nCIsImxhc3ROYW1lIjoiIiwiZW5hYmxlZCI6ZmFsc2UsImlzUHVibGljIjpmYWxzZSwidGVuYW50SWQiOiI4MzA5NTdkMC1lZTFkLTExZTgtOWVjYS0xMTliYzhmYTkwZGYiLCJjdXN0b21lcklkIjoiMTM4MTQwMDAtMWRkMi0xMWIyLTgwODAtODA4MDgwODA4MDgwIiwiaXNzIjoiYmVpZG91YXBwLmNvbSIsImlhdCI6MTU1Nzc5Nzg4OCwiZXhwIjoxNTU3ODMzODg4fQ.4rCUZhVe6syS-5bG6A-WLQ2GkPa9iWdONBzYruD99-9aE57Tc7CmbNACrQXURyD2lULF0svdmWJ236EDsgDbyw',
     sysAdminToken: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzaGVuamlAYmVpZG91YXBwLmNvbSIsInNjb3BlcyI6WyJTWVNfQURNSU4iXSwidXNlcklkIjoiYTYxMjNkMDAtMTRiNS0xMWU5LTlmMTgtZmZmMWU0ZGNiNDA4IiwiZmlyc3ROYW1lIjoic2hlbiIsImxhc3ROYW1lIjoiamkiLCJlbmFibGVkIjp0cnVlLCJpc1B1YmxpYyI6ZmFsc2UsInRlbmFudElkIjoiMTM4MTQwMDAtMWRkMi0xMWIyLTgwODAtODA4MDgwODA4MDgwIiwiY3VzdG9tZXJJZCI6IjEzODE0MDAwLTFkZDItMTFiMi04MDgwLTgwODA4MDgwODA4MCIsImlzcyI6ImJlaWRvdWFwcC5jb20iLCJpYXQiOjE1NTc4MTMwNTcsImV4cCI6MTU1Nzg0OTA1N30.WQZmgSsxw8b-5t4J4_UUUNcazgas4ClQWuXpY_PMTrmU5waQlnndULPmBVdQKGArmuiwuD63djEXeeNAEW3DiA'
};