# QuickBite Cart Service

`cart-service` manages active customer carts, cart items, quantity updates, restaurant switching, and checkout preparation stubs for the QuickBite platform.

## API

Base path: `/cart`

- `GET /cart/{customerId}`
- `POST /cart/add`
- `PUT /cart/update-quantity`
- `DELETE /cart/remove-item/{itemId}`
- `POST /cart/apply-promo`
- `DELETE /cart/clear/{customerId}`
