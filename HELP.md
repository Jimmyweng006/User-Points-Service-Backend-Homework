## How to Run the Environment

To start the backend service and the required Docker containers (like RocketMQ), run the following command from the project root:

```bash
# The --force-recreate flag ensures a clean state for the containers.
docker compose up -d --force-recreate
```

```bash
# This command starts the Spring Boot app in the background and brings up Docker services.
# Make sure services(Redis, MySQL, MQ) are ready before executing this command.
mvn spring-boot:run
```

**Optional: How to Completely Reset the Environment**

If you want to delete all data (including the database) and start from a completely clean state, run this command first:

```bash
# This stops and removes containers, networks, AND volumes.
docker compose down -v
```

## Manual API Testing Guide

Below are a few examples of `curl` commands to manually test the Points Service API endpoints.

## Prerequisites

- The Spring Boot application and Docker containers must be running.
- The examples below use `test-user-1` and `test-user-2`. You can replace them with any user ID.

---

### 1. Add Points for a User

This command adds points to a user. Here are a few examples:

**Example 1: Add 100 points to `test-user-1`**
```bash
curl -X POST http://localhost:8080/points \
-H "Content-Type: application/json" \
-d '{
  "userId": "test-user-1",
  "amount": 100,
  "reason": "Manual test reward"
}'
```

**Example 2: Add 250 points to `test-user-2`**
```bash
curl -X POST http://localhost:8080/points \
-H "Content-Type: application/json" \
-d '{
  "userId": "test-user-2",
  "amount": 250,
  "reason": "Special bonus"
}'
```

**Example 3: Add 50 more points to `test-user-1`**
```bash
curl -X POST http://localhost:8080/points \
-H "Content-Type: application/json" \
-d '{
  "userId": "test-user-1",
  "amount": 50,
  "reason": "Follow-up reward"
}'
```

### 2. Get Total Points for a User

This command retrieves the total points for `test-user-1`(points should be 150 now).

```bash
curl http://localhost:8080/points/test-user-1
```

### 3. Get Leaderboard

This command retrieves the top 10 users on the leaderboard(test-user-2 should be first before test-user-1).

```bash
curl http://localhost:8080/points/leaderboard
```

### 4. Update the Reason for a Points Record

This command updates the reason for a specific points record. You need to know the `id` of the record you want to update. You can get this ID from the response when you first add points.

**Example: Update the reason for the record with ID `1`**
```bash
curl -X PUT http://localhost:8080/points/1 \
-H "Content-Type: application/json" \
-d '{
  "reason": "Updated reason for the transaction"
}'
```

### 5. Delete a User's Points Data

This command deletes all points records and the total points for `test-user-1`.

```bash
curl -X DELETE http://localhost:8080/points/test-user-1
```

**Verify the deletion**

To confirm that the user's data has been removed, you can query the leaderboard again. You should no longer see the deleted user(test-user-1).

```bash
curl http://localhost:8080/points/leaderboard
```

---

## How to Check Test Reports

To run all unit tests and generate a code coverage report, execute the following command:

```bash
mvn clean verify
```

After the command completes successfully, you can view the detailed report by opening this file in your browser:

`target/site/jacoco/index.html`