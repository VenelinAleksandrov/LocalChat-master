module.exports = {
    devServer: {
        host: '0.0.0.0',
        port: 8088,
        allowedHosts: ['all'],
        proxy: 'http://localhost:8080'
    }
};
