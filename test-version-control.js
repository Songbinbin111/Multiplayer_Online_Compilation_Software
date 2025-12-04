// 测试版本控制API
const axios = require('axios');

// 测试配置
const BASE_URL = 'http://localhost:8080';
const TEST_USER_ID = '1';
const TEST_DOCUMENT_ID = '1';

// 测试创建文档版本
async function testCreateVersion() {
    try {
        const response = await axios.post(`${BASE_URL}/api/version`, {
            docId: TEST_DOCUMENT_ID,
            userId: TEST_USER_ID,
            description: '测试版本',
            content: '这是一个测试版本的内容'
        });
        console.log('✅ 创建版本成功:', response.data);
        return response.data.data;
    } catch (error) {
        console.error('❌ 创建版本失败:', error.response?.data || error.message);
        return null;
    }
}

// 测试获取文档版本列表
async function testGetVersions() {
    try {
        const response = await axios.get(`${BASE_URL}/api/version/list/${TEST_DOCUMENT_ID}`);
        console.log('✅ 获取版本列表成功:', response.data);
        return response.data.data;
    } catch (error) {
        console.error('❌ 获取版本列表失败:', error.response?.data || error.message);
        return null;
    }
}

// 测试回滚到指定版本
async function testRollbackToVersion(versionId) {
    try {
        const response = await axios.post(`${BASE_URL}/api/version/rollback/${versionId}`);
        console.log('✅ 回滚版本成功:', response.data);
        return response.data;
    } catch (error) {
        console.error('❌ 回滚版本失败:', error.response?.data || error.message);
        return null;
    }
}

// 主测试函数
async function runTests() {
    console.log('=== 开始测试版本控制功能 ===');
    
    // 创建一个测试版本
    const createdVersion = await testCreateVersion();
    
    if (createdVersion) {
        // 获取版本列表
        await testGetVersions();
        
        // 测试回滚到刚创建的版本
        await testRollbackToVersion(createdVersion.id);
    }
    
    console.log('=== 版本控制功能测试完成 ===');
}

// 运行测试
runTests();