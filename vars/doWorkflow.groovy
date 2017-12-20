def call() {
	node {
		stage('checkout'){
			checkout scm
		}
		stage('build docker image'){
			sh 'docker build -t fiery-test:${BRANCH_NAME} . '
		}
	}
	node {
		stage("run docker"){
			// https://vsupalov.com/docker-build-time-env-values/
			sh 'docker container run -e root="${BRANCH_NAME}" --name mylittlefiery --rm -p 8000:8080 fiery-test:${BRANCH_NAME} &'
		}
		stage("test docker") {
			try {		
				sh 'sleep 5'
				out = sh(returnStdout: true, script: 'curl -s http://127.0.0.1:8000/${BRANCH_NAME}/hello/world/')
				echo out
				if (out ==  "<h1>Hello world!</h1>") {
					sh "exit 1"
				}
			} catch (e) {
				currentBuild.result = 'FAILURE'
				throw e
			} finally {
				sh "docker container kill mylittlefiery"
			}
		}
		stage("branch logic"){
			if (BRANCH_NAME == "master") {
				stage("deploy") {
					sh "docker tag fiery-test:${BRANCH_NAME} fiery-test:deploy"
					sh "docker rmi fiery-test:${BRANCH_NAME}"
					sh "docker container kill deploy || true"
					sh "docker container run -d -e root='/' --name deploy --rm -p 10080:8080 fiery-test:deploy"
				}
			} else if (BRANCH_NAME == "predeploy") {
				sshagent(credentials: ['8a5c5968-0508-4d5b-b6e5-819fd464435f']) {
					sh 'git checkout master'
					sh 'git pull'
					sh 'git merge origin/predeploy'
		 			sh 'git push'
				}
			}
		}
	}
}