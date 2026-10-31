# emeritus-capstone-project
Capstone Assignment: FinSecure Pay: Automating a Cloud-Native DevOps Pipeline

Spring Boot 4 with Java 21 service that validates payment transactions.

## Requirements
- JDK 21+
- Maven 3.10+ 

## Build and test
```bash
mvn clean verify
```

## Run locally
```bash
mvn spring-boot:run
```

## Endpoints
| Method | Path                    | Purpose                                                                                |
|--------|-------------------------|----------------------------------------------------------------------------------------|
| GET    | `/health`               | Liveness/readiness check, reports service name and version                             |
| POST   | `/validate`             | Validates a transaction. 200 if valid, 422 with reasons if not, 400 for malformed JSON |
| GET    | `/actuator/prometheus`  | Prometheus metrics                                                                     |

## Validation rules
1. `transactionId` required, max 64 characters
2. `amount` required, greater than 0, at most 2 decimal places
3. `amount` must not exceed the transaction limit (default 10000.00)
4. `currency` required and one of the supported currency codes (default IDR, SGD, USD, EUR)
5. `fromAccount` and `toAccount` required, 12 digits, both cannot be the same.

## Configuration
The limit and currency list can be overridden with environment variables:

| Variable                   | Default           |
|----------------------------|-------------------|
| `VALIDATION_MAX_AMOUNT`    | `10000.00`        |
| `VALIDATION_SUPPORTED_CCY` | `IDR,SGD,USD,EUR` |
