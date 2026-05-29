# QuickBite Restaurant Service

`restaurant-service` manages restaurant onboarding, profile updates, approval workflow, status toggling, nearby discovery, and search for the QuickBite platform.

## Stack

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Data JPA
- H2 in-memory database
- Spring Validation
- Eureka Discovery Client
- Spring Boot Admin Client
- SpringDoc OpenAPI

## API

Base path: `/restaurants`

- `POST /restaurants`
- `GET /restaurants/{id}`
- `GET /restaurants/owner/{ownerId}`
- `GET /restaurants/search`
- `GET /restaurants/nearby`
- `PUT /restaurants/{id}`
- `PATCH /restaurants/{id}/approve`
- `PATCH /restaurants/{id}/toggle-status`
- `PATCH /restaurants/{id}/rating`
- `DELETE /restaurants/{id}`
