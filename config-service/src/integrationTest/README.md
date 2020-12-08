<h4>How do I run the integration tests locally?</h4>

- Bring up a mongodb container locally using Docker or port forward to the mongo container in Kubernetes

    `docker run --name mongo-local -p 27017:27017 -d mongo`
    or
    `kubectl port-forward mongo-0 27017:27017 -n hypertrace-core`
      
- Run the integration tests from IDE
