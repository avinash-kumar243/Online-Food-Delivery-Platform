# QuickBite Menu Service

`menu-service` is the menu management microservice for QuickBite. It manages menu categories, menu items, pricing, availability, and optimized restaurant menu reads with in-memory caching.

## API

Base path: `/menu`

- `POST /menu`
- `GET /menu/restaurant/{restaurantId}`
- `GET /menu/category/{categoryId}`
- `GET /menu/item/{itemId}`
- `PUT /menu`
- `PUT /menu/toggleAvailability`
- `DELETE /menu`
- `GET /menu/search?query=...`
- `GET /menu/vegItems?restaurantId=...&availableOnly=true`

Swagger UI: `/swagger-ui.html`

## Run Locally

Run the service:

```bash
mvn spring-boot:run
```

Or start everything together:

```bash
docker compose up --build
```

## Build and Test

```bash
mvn clean verify
```