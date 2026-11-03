# emeritus-capstone-project

Capstone Assignment: FinSecure Pay: Automating a Cloud-Native DevOps Pipeline

Spring Boot 4 with Java 21 service that validates payment transactions, delivered by a Jenkins pipeline to Amazon EKS and monitored with Prometheus, Grafana and Alertmanager.

## Requirements
- JDK 21+ (Maven wrapper included, no separate Maven install needed)

## Build and test
```bash
./mvnw clean verify
```

## Run locally
```bash
./mvnw spring-boot:run
```

## Endpoints
| Method | Path                           | Purpose                                                                                |
|--------|--------------------------------|----------------------------------------------------------------------------------------|
| GET    | `/health`                      | Liveness/readiness check, reports service name and version                             |
| POST   | `/validate`                    | Validates a transaction. 200 if valid, 422 with reasons if not, 400 for malformed JSON |
| GET    | `/actuator/health/liveness`    | Kubernetes liveness probe                                                              |
| GET    | `/actuator/health/readiness`   | Kubernetes readiness probe                                                             |
| GET    | `/actuator/prometheus`         | Prometheus metrics                                                                     |

## Validation rules
1. `transactionId` required, max 64 characters
2. `amount` required, greater than 0, at most 2 decimal places
3. `amount` must not exceed the transaction limit (default 10000.00)
4. `currency` required and one of the supported currency codes (default IDR, SGD, USD, EUR)
5. `fromAccount` and `toAccount` required, must be 12 digits and must differ

## Configuration
The limit and currency list can be overridden with environment variables (in Kubernetes they come from a ConfigMap):

| Variable                   | Default           |
|----------------------------|-------------------|
| `VALIDATION_MAX_AMOUNT`    | `10000.00`        |
| `VALIDATION_SUPPORTED_CCY` | `IDR,SGD,USD,EUR` |

## Docker
```bash
docker build -t finsecure-pay:1.0.0 .
docker run --rm -p 8080:8080 finsecure-pay:1.0.0
curl http://localhost:8080/health
```
The image uses a multi-stage build and runs as a non-root user. It is published to Docker Hub as `<dockerhub-user>/finsecure-pay:<version>`, where the version matches the version in `pom.xml`.

## Kubernetes (EKS)
Manifests are in `k8s/`:
- `finsecure-pay.yaml`: ConfigMap, Deployment (2 replicas, readiness and liveness probes) and a LoadBalancer Service
- `servicemonitor.yaml`: tells Prometheus to scrape the application
- `prometheusrule.yaml`: alert when fewer than two pods are available

```bash
kubectl apply -f k8s/finsecure-pay.yaml
kubectl get pods
kubectl get svc finsecure-pay
```
Jenkins deploys `finsecure-pay.yaml` with the image tag set to the pom version. The monitoring files are applied by hand (see below).

## CI/CD (Jenkins)
The `Jenkinsfile` runs these stages: Checkout, Build, Test, Package, SonarCloud, Dependency-Check, Docker Build, Trivy image scan, Push and Deploy. If the rollout does not become healthy, the Deploy stage rolls back automatically and fails the build. Jenkins polls the repository for changes and sends an email with each build result.

### Run Jenkins locally in Docker
Jenkins runs in a Docker container on the developer machine. The image in `/jenkins/Dockerfile` adds the Docker CLI, `kubectl` and the AWS CLI to the official Jenkins image.

1. Build the image:
```bash
   docker build -t jenkins-capstone ./jenkins
```

2. Start the container. The named volume keeps jobs and credentials across restarts, and the Docker socket lets pipeline builds use the host's Docker engine:
```bash
   docker run -d --name jenkins -u root -p 9090:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home -v /var/run/docker.sock:/var/run/docker.sock jenkins-capstone
```

3. Check that the tools work inside the container:
```bash
   docker exec jenkins docker version
   docker exec jenkins kubectl version --client
   docker exec jenkins aws --version
```

4. Open `http://localhost:9090` and unlock Jenkins with the initial admin password:
```bash
   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Install the suggested plugins, create an admin user, then add the **Email Extension** plugin under Manage Jenkins > Plugins.

5. Add the credentials under Manage Jenkins > Credentials > System > Global, using these exact IDs (never commit their values):

| ID             | Kind                   | Contents                                |
|----------------|------------------------|-----------------------------------------|
| `dockerhub`    | Username with password | Docker Hub username and access token    |
| `aws-deployer` | Username with password | AWS access key ID and secret access key |
| `nvd-api-key`  | Secret text            | NVD API key                             |
| `sonar-token`  | Secret text            | SonarCloud token                        |
| `smtp-gmail`   | Username with password | SMTP sender address and app password    |

6. Configure email under Manage Jenkins > System: set the Jenkins URL and system admin address, fill in the Extended E-mail Notification SMTP settings with the `smtp-gmail` credential, and add a global environment variable `NOTIFY_EMAIL` with the address that should receive build results.

7. Create the pipeline job: New Item > Pipeline > Pipeline script from SCM > Git, enter the repository URL, branch `*/main`, script path `Jenkinsfile`. Click **Build Now** once, and the repository is then polled for changes.

Jenkins runs as root with the Docker socket mounted in this project. However, isolated build agents should be used in production. It polls the repository, because a local Jenkins has no public address for GitHub webhooks to reach.

Credentials are stored in the Jenkins credential store and referenced by ID only: `dockerhub`, `aws-deployer`, `nvd-api-key`, `sonar-token` and `smtp-gmail`.

## Monitoring and alerting
Prometheus, Grafana and Alertmanager are installed with `kube-prometheus-stack`.
1. Install the Helm chart into the `monitoring` namespace (choose your own Grafana admin password):
   ```bash
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm repo update
   kubectl create namespace monitoring
   helm install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring --set grafana.adminPassword='<choose-a-password>'
   kubectl get pods -n monitoring
   ```
2. Create the `alertmanager-smtp` Kubernetes Secret with the SMTP app password:
   ```bash
   kubectl create secret generic alertmanager-smtp -n monitoring --from-literal=password='<smtp-app-password>'
   ```
3. Copy `monitoring/monitoring-values.example.yaml` to `monitoring-values.yaml`, fill in your sender and recipient addresses, then upgrade the stack with it:
   ```bash
   helm upgrade monitoring prometheus-community/kube-prometheus-stack --namespace monitoring --reuse-values -f monitoring-values.yaml
   ```
4. Apply the scrape configuration and the alert rule:
   ```bash
   kubectl apply -f k8s/servicemonitor.yaml
   kubectl apply -f k8s/prometheusrule.yaml
   ```
5. Open the dashboards locally (run each command in different terminals):
   ```bash
   kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
   kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 10090:9090
   ```
   Grafana is then at `http://localhost:3000` (user `admin`) and Prometheus at `http://localhost:10090`. Use another local port if one is taken.

To test the alert, scale the deployment down (`kubectl scale deployment finsecure-pay --replicas=1`), wait for the email, then scale back to 2.

Logs from all pods are centralised in CloudWatch Logs. `kubectl logs` and `kubectl describe pod` are used for quick checks.

## Security scanning
The pipeline runs SonarCloud (code quality), OWASP Dependency-Check (dependencies) and Trivy (container image). Each is a release gate that fails the build on a problem. Secrets are never committed to the repository.

This product uses data from the NVD API but is not endorsed or certified by the NVD.