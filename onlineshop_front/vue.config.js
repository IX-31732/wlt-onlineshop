// vue.config.js
const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    devServer: {
        port: 8081,
        host: 'localhost',
        proxy: {
            '/api': {
                target: process.env.VUE_APP_API_URL || 'http://localhost:8080',
                changeOrigin: true
            }
        }
    }
})