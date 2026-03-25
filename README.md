# Messaging Backend

## Stack

- Java 17
- Spring Boot 3.2.10
- Spring Security JWT
- Spring WebSocket STOMP
- MySQL 8
- Redis
- RabbitMQ
- Flyway
- OpenAPI

## Production-oriented improvements included

- broker-backed fan-out with RabbitMQ
- Redis-backed session-aware presence
- monotonic delivery and read receipts
- idempotent client message IDs
- direct-chat deduplication
- block and unblock APIs
- Redis rate limiting for auth and messaging
- Dockerfile and docker-compose
- environment-driven configuration

## Run with Docker Compose

```bash
docker compose up --build
```

## Run locally without Docker

```bash
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=messaging_app -p 3306:3306 mysql:8.4
docker run -d --name redis -p 6379:6379 redis:7
docker run -d --name rabbitmq -p 5672:5672 -p 61613:61613 -p 15672:15672 rabbitmq:3-management
```

Enable STOMP in RabbitMQ if needed:

```bash
docker exec rabbitmq rabbitmq-plugins enable rabbitmq_stomp rabbitmq_web_stomp
```

Then run:

```bash
mvn spring-boot:run
```

## WebSocket

- Endpoint: `/ws`
- Send JWT in STOMP CONNECT native header `Authorization: Bearer <token>`
- Subscribe:
  - `/topic/chat.{chatId}`
  - `/topic/chat.{chatId}.typing`
  - `/topic/chat.{chatId}.receipts`
  - `/topic/presence`
  - `/user/queue/messages`
  - `/user/queue/ack`

## REST

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users`
- `POST /api/users/{userId}/block`
- `DELETE /api/users/{userId}/block`
- `POST /api/chats`
- `GET /api/chats`
- `GET /api/chats/{chatId}`
- `POST /api/chats/{chatId}/messages`
- `GET /api/chats/{chatId}/messages`
- `POST /api/chats/{chatId}/messages/receipts`

## E2EE payload

The server stores and relays:

- `cipherText`
- `encryptedKey`
- `nonce`
- `algorithm`
- `metadata`

The server never decrypts message content.


## Frontend

The React test client is embedded into Spring Boot static resources for the combined build. Source lives in `frontend-src/`.
