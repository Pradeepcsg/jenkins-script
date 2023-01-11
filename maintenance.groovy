pipeline {
    agent any
    parameters {
        string(name: 'description', defaultValue: '', description: 'Enter Description :')
        string(name: 'start_date', defaultValue: '', description: 'Enter StartDate [YYYY-MM-DDTHH-MM-SSZ]:')
        string(name: 'end_date', defaultValue: '', description: 'Enter EndDate [YYYY-MM-DDTHH-MM-SSZ]:')
        string(name: 'order_id', defaultValue: '', description: 'Enter Notification Policy ID :')
    }
    stages {
        stage('Preparation') {
            steps {
                sh """
                    curl --location --request GET 'https://api.opsgenie.com/v2/policies/notification?teamId=e27f4934-aa2f-41b4-ba2a-d149368285ac' -H 'Accept: application/json'  --header 'Authorization: GenieKey 5b2c51fb-eaf2-4f3d-82a1-9138f356736d' > index.txt
                    cat >> runs.py <<HERE
                    import json
                    order_id=$order_id
                    f = open('index.txt')
                    json_data = json.load(f)
                    for i in json_data['data']:
                        if order_id == i["order"]:
                            print("{} :: {} :: {}".format(i["order"],i["name"],i["id"]))
                    f.close()
                    HERE
                """
            }
        }
        stage('Display Notification Policy Details') {
            steps {
                script {
                    notify_details = sh (script: 'python runs.py',returnStdout: true).trim()
                    echo "Notification Details ==> ${notify_details}"
                    policy_id = notify_details.split("::")[2].trim()
                    println "Policy ID >>> ${policy_id}"
                }
            }
        }
        stage('Create Maintainance Policy') {
            steps {
                sh """
                    curl --location --request POST 'https://api.opsgenie.com/v1/maintenance' \
                --header 'Authorization: GenieKey 78ea084a-90f0-45db-a336-a685d3e3930b' \
                --header 'Content-Type: application/json' \
                --data '{ "description": "$description", "time": { "type" : "schedule", "startDate": "$start_date", "endDate": "$end_date"}, "rules": [{ "state": "enabled", "entity": { "id": "${policy_id}", "type": "policy" }}]}' 
                """
            }
        }
        stage('Clear Data') {
            steps {
                sh """
                    rm index.txt
                    rm runs.py
                """
            }
        }
    }
}
