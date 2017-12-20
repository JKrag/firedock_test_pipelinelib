def call(ctx) {
    ctx.stage('junit'){
        junit '**/target/surefire-reports/TEST-*.xml'
    }
}