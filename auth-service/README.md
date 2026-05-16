# QuickBite Auth Service - Complete Implementation

> A multi-user authentication service for an online food delivery platform with support for guest, Customers, Restaurant Owners, and Delivery Partners.

## 🎯 Project Overview

The QuickBite Auth Service is a comprehensive authentication and authorization system built with Spring Boot that manages user registration, login, profile management, and account operations for three distinct user types in an online food delivery application.

---

## 📋 Features Implemented

### ✅ Authentication Features
- **User Registration** - Register for all three user types with validation
- **Secure Login** - Password-based authentication with BCrypt encryption
- **JWT Tokens** - Token generation and validation
- **Token Refresh** - Refresh expired tokens
- **Token Blacklisting** - Invalidate tokens on logout
- **Role-Based Access** - Different routes for different user types

### ✅ Profile Management
- **Get Profile** - Retrieve complete user profile
- **Update Profile** - Update user information selectively
- **Password Change** - Change password with old password verification
- **Account Deactivation** - Disable account without deletion

### ✅ Security Features
- **Password Encryption** - BCrypt password hashing
- **Input Validation** - DTO validation on all endpoints
- **Duplicate Prevention** - Unique email and phone per user type
- **Global Exception Handling** - Standardized error responses
- **Field-Level Validation** - Detailed validation error messages

---

## 🏗️ Architecture

### Four Independent User Types

#### 1. **Guest**
- Do not need Authentication
- Can browse near by restaurants 
- View Menu list of that restaurant
- View Profiles of restaurant
- Can't order food

#### 2. **Customer**
- Order food from restaurants
- Manage delivery address
- Track orders
- Profile fields: fullName, email, phone, address, city

#### 3. **Restaurant Owner**
- Manage restaurant details
- Add/manage menu items
- Process orders
- Profile fields: fullName, email, phone, restaurantName, restaurantAddress, licenseNumber, businessRegistration

#### 4. **Delivery Partner**
- Accept delivery orders
- Track delivery history
- Manage vehicle details
- Profile fields: fullName, email, phone, licenseNumber, vehicleType, vehicleNumber, isVerified, rating

---

## 📁 Project Structure

```
auth-service/
├── src/main/java/com/quickbite/auth/
│   ├── entity/
│   │   ├── Customer.java
│   │   ├── RestaurantOwner.java
│   │   ├── DeliveryPartner.java
│   ├── repository/
│   │   ├── CustomerRepository.java
│   │   ├── RestaurantOwnerRepository.java
│   │   └── DeliveryPartnerRepository.java
│   ├── service/
│   │   ├── ICustomerAuthService.java
│   │   ├── CustomerAuthServiceImpl.java
│   │   ├── IRestaurantOwnerAuthService.java
│   │   ├── RestaurantOwnerAuthServiceImpl.java
│   │   ├── IDeliveryPartnerAuthService.java
│   │   ├── DeliveryPartnerAuthServiceImpl.java
│   │   ├── JwtService.java
│   │   ├── TokenBlacklistService.java
│   │   └── CustomUserDetailsService.java
│   ├── controller/
│   │   ├── CustomerAuthController.java
│   │   ├── RestaurantOwnerAuthController.java
│   │   └── DeliveryPartnerAuthController.java
│   ├── dto/
│   │   ├── CustomerRegisterRequestDto.java
│   │   ├── CustomerProfileDto.java
│   │   ├── CustomerUpdateProfileDto.java
│   │   ├── RestaurantOwnerRegisterRequestDto.java
│   │   ├── RestaurantOwnerProfileDto.java
│   │   ├── RestaurantOwnerUpdateProfileDto.java
│   │   ├── DeliveryPartnerRegisterRequestDto.java
│   │   ├── DeliveryPartnerProfileDto.java
│   │   ├── DeliveryPartnerUpdateProfileDto.java
│   │   ├── PasswordChangeRequestDto.java
│   │   ├── LoginRequestDTO.java
│   │   └── ResponseDto.java
│   ├── exception/
│   │   ├── AccountNotFoundException.java
│   │   └── PasswordNotMatchException.java
│   │   └── GlobalExceptionHandler.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtFilter.java
│   │   ├── JWTAuthenticationEntryPoint.java
│   │   ├── UserConfig.java
│   │   ├── OAuth2AuthenticationSuccessHandler.java
│   └── AuthServiceApplication.java
├── src/main/resources/
│   └── application.yml
├── pom.xml
└── README.md (this file)
```

---

## 📡 API Endpoints Summary

### Customer Endpoints (8 total)
```
POST   /auth/customer/register              - Register customer
POST   /auth/customer/login                 - Login customer
POST   /auth/customer/logout                - Logout customer
POST   /auth/customer/refresh               - Refresh token
GET    /auth/customer/profile/{id}          - Get profile
PUT    /auth/customer/profile/{id}          - Update profile
POST   /auth/customer/change-password/{id}  - Change password
POST   /auth/customer/deactivate/{id}       - Deactivate account
```

### Restaurant Owner Endpoints (8 total)
```
POST   /auth/restaurant/register              - Register owner
POST   /auth/restaurant/login                 - Login owner
POST   /auth/restaurant/logout                - Logout owner
POST   /auth/restaurant/refresh               - Refresh token
GET    /auth/restaurant/profile/{id}          - Get profile
PUT    /auth/restaurant/profile/{id}          - Update profile
POST   /auth/restaurant/change-password/{id}  - Change password
POST   /auth/restaurant/deactivate/{id}       - Deactivate account
```

### Delivery Partner Endpoints (8 total)
```
POST   /auth/delivery-partner/register              - Register partner
POST   /auth/delivery-partner/login                 - Login partner
POST   /auth/delivery-partner/logout                - Logout partner
POST   /auth/delivery-partner/refresh               - Refresh token
GET    /auth/delivery-partner/profile/{id}          - Get profile
PUT    /auth/delivery-partner/profile/{id}          - Update profile
POST   /auth/delivery-partner/change-password/{id}  - Change password
POST   /auth/delivery-partner/deactivate/{id}       - Deactivate account
```

**Total: 24 Endpoints**

---

## 🔐 Authentication Flow

```
1. User Registration
   POST /auth/{type}/register
   → Validate input
   → Check duplicate email/phone
   → Encrypt password
   → Save to database
   → Return JWT token

2. User Login
   POST /auth/{type}/login
   → Find user by email
   → Verify password
   → Generate JWT token
   → Return token

3. Protected Requests
   Include: Authorization: Bearer <token>
   → Filter validates token
   → Extract user information
   → Allow access

4. Token Refresh
   POST /auth/{type}/refresh
   → Validate current token
   → Generate new token
   → Return new token

5. Logout
   POST /auth/{type}/logout
   → Add token to blacklist
   → Prevent reuse
```

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Programming Language |
| Spring Boot | 3.2.2 | Framework |
| Spring Security | 6.0+ | Authentication/Authorization |
| Spring Data JPA | 3.2+ | Database Access |
| JWT (JJWT) | 0.11.5 | Token Management |
| BCrypt | Spring Security | Password Encryption |
| MySQL | 8.0+ | Database |
| Maven | 3.6+ | Build Tool |
| Lombok | Latest | Boilerplate Reduction |
| Validation | Jakarta | Input Validation |

---

## 🔒 Security Features

✅ **Password Security**
- BCrypt hashing
- Salted encryption
- Strong password requirements

✅ **Token Security**
- JWT signing
- Token expiration
- Token blacklisting

✅ **Input Validation**
- DTO validation
- Email format validation
- Phone format validation
- Field length validation

✅ **Database Security**
- Unique constraints on email/phone
- Password hashing
- No plain text passwords

✅ **Exception Handling**
- Standardized error responses
- Information disclosure prevention
- HTTP status mapping

---

## 🔄 Future Enhancements

1. **Email Verification** - Verify email on registration
2. **Password Reset** - Forgot password functionality
3. **Two-Factor Authentication** - OTP support
4. **OAuth2 Integration** - Google, GitHub login
5. **Role-Based Access Control** - Granular permissions
6. **API Rate Limiting** - Request throttling
7. **Audit Logging** - Track all operations
8. **Account Recovery** - Account restoration

---

**Thank you for using QuickBite Auth Service!** 🚀

