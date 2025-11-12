# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Start Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the application (local environment)
./gradlew bootRun

# Build without running tests
./gradlew build -x test
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with verbose output
./gradlew test --info

# Run a single test class
./gradlew test --tests com.C_platform.item.service.CategoryUseCaseTest

# Run a specific test method
./gradlew test --tests com.C_platform.item.service.CategoryUseCaseTest.testMethod
```

### Development Tasks
```bash
# Clean build artifacts
./gradlew clean

# Check project for issues
./gradlew check

# Run tests and build
./gradlew clean test build
```

## Project Structure

This is a **Spring Boot 3.5.4** e-commerce platform (Carhartt second-hand marketplace) using **Java 17**.

```
src/main/java/com/C_platform/
├── Member_woonkim/          # User authentication & profile management
│   ├── domain/              # Member entity, value objects
│   ├── application/         # Use cases for OAuth2, member operations
│   ├── presentation/        # Controllers, DTOs for API
│   ├── infrastructure/      # Repositories, OAuth adapters, configs
│   └── utils/               # Helpers for OAuth, logging
├── item/                    # Product catalog
│   ├── domain/              # Item entity (single-table inheritance), Category, Images
│   ├── application/         # Item use cases
│   ├── service/             # ItemService, CategoryService
│   ├── infrastructure/      # Repositories (ItemRepository, CategoryRepository)
│   └── ui/                  # ItemController, DTOs
├── order/                   # Order management
│   ├── domain/              # Order aggregate, OrderAddress, ItemSnapshot (value objects)
│   ├── application/         # CreateOrderService, order use cases
│   ├── infrastructure/      # OrderRepository, JPA repository
│   └── ui/                  # OrderController, request/response DTOs
├── payment/                 # Payment processing
│   ├── domain/              # Payment entity, PaymentStatus, PaymentMethod
│   ├── application/         # Payment commands and use cases
│   ├── infrastructure/      # PaymentGatewayPort, KakaoPayAdapter, NaverPayAdapter
│   └── ui/                  # PaymentController, DTOs
├── config/                  # Spring configurations
│   ├── SecurityConfig.java  # OAuth2, CSRF, authorization rules
│   ├── WebConfig.java       # HTTP message converters, UTF-8 charset
│   ├── FileConfig.java      # AWS S3, file upload configs
│   └── RestTemplateConfig.java
├── global/                  # Global utilities
│   ├── error/               # ErrorCode enums, exception handlers
│   ├── ApiResponse.java     # Standard response wrapper
│   └── PageResponseDto.java # Pagination DTO
├── annotation/              # @UseCase, custom annotations
└── exception/               # Custom exception classes

src/main/resources/
├── application.properties          # Main config (H2 local, OAuth2 imports)
├── application-oauth2-local.yml    # OAuth2 provider settings (Kakao, Naver)
├── application-oauth2-prod.yml     # Production OAuth2 settings
├── messages.properties             # Application messages
├── messages_errors.properties      # Error code messages
└── *.sql                           # Initial data scripts

src/test/java/com/C_platform/       # Test classes following module structure
```

## Architecture Overview

**Pattern:** Hexagonal (Ports & Adapters) with Domain-Driven Design elements

### Layering
```
Controller/UI → UseCase/Service → Domain/Business Logic → Infrastructure (Repositories, Adapters)
```

### Key Design Patterns

**1. Use Case Pattern**
- Services marked with `@UseCase` annotation
- Commands carry request data (e.g., `CreateOrderCommand`)
- One use case = one user-level feature

Example: `OAuth2UseCase`, `ItemUseCase`, `CreateOrderService`

**2. Port & Adapter (Hexagonal)**
- **Ports** (interfaces): Define external dependencies
  - `PaymentGatewayPort` - abstraction for payment providers
  - `OauthClientPort` - abstraction for OAuth providers
  - `ItemPricingReader` - abstraction for item data
- **Adapters** (implementations): Concrete integrations
  - `KakaoPayAdapter`, `NaverPayAdapter` for payments
  - `KakaoClient`, `NaverClient` for OAuth

**3. Repository Pattern**
- JPA `CrudRepository` / `JpaRepository` interfaces
- Custom implementations for complex queries (e.g., `OrderRepositoryImpl`)
- Query methods use Spring Data conventions

**4. Value Objects**
- `OrderAddress` - embedded snapshot of shipping address at order creation
- `ItemSnapshot` - embedded snapshot of item price at order creation
- Prevents order data from changing if original item/address is updated

**5. Single-Table Inheritance**
- `Item` base class with `TopItem`, `BottomItem`, `OuterItem` subtypes
- Discriminator column `dtype`
- Each subtype has embedded size info (TopItemEmbeddable, BottomItemEmbeddable)

## Critical Data Flows

### Order Creation
```
Member (buyer) selects Item → CreateOrderRequest → CreateOrderService
  → Captures ItemSnapshot (price, seller) & OrderAddress (shipping)
  → Order created with status READY
  → Payment integration next
```

### Payment Processing
```
Order (READY) → PaymentGatewayPort.ready()
  → KakaoPayAdapter.ready() → returns TID, redirect URL
  → User completes payment at Kakao Pay → PG token returned
  → PaymentGatewayPort.complete() → Order state → PAID
```

### OAuth2 Member Registration
```
OAuth Login → OAuth2UseCase.getUserInfo()
  → OauthClientRegister.get(provider) → KakaoClient/NaverClient
  → Access token exchange
  → OAuthUserInfoParserRegister.get(provider) → KakaoParser/NaverParser
  → MemberJoinService.ensureOAuthMember()
  → Member created or existing member returned
```

## Database & Entities

**ORM:** Spring Data JPA + Hibernate
**Local DB:** H2 in-memory (configured in application.properties)
**Production DB:** MySQL

**Key Relationships:**
```
Member (1) ──< (N) Item (items listed for sale)
Member (1) ──< (N) Order (as buyer)
Member (1) ──< (N) Order (as seller via item snapshot)
Member (1) ──< (N) Address
Member (1) ──< (N) Wish

Item (1) ──< (N) Images
Item (1) ──< (N) CategoryItem
Item (1) ──< (N) Order (via OrderLine or ItemSnapshot)

Category (1) ──< (N) CategoryItem

Order (1) ──< (N) Payment
Order *: contains ItemSnapshot (@Embedded - immutable copy of item data)
Order *: contains OrderAddress (@Embedded - immutable copy of address)
```

**Important:** `ddl-auto=create` in local config (creates fresh schema on startup). Change to `validate` or `update` for production in CI/CD settings.

## Security

**Authentication Method:** Session-based (Spring Security + servlet sessions)

### OAuth2 Integration
- Kakao & Naver providers configured
- Credentials injected via CI/CD secrets into `application-oauth2-*.yml`
- Custom `CustomOAuth2UserService` parses provider-specific user info
- Principal is `CustomOAuth2User` implementing `OAuth2User`

### Local Authentication (Email/Password)
- **Signup:** `POST /v1/local/signup` - Register with email, password, name
- **Login:** `POST /v1/local/login` - Authenticate with email and password, returns member info
- **Login Check:** `GET /v1/local/check` - Verify current login status and retrieve user info (auth-required)
- **Logout:** `POST /v1/local/logout` - End session and clear cookies
- **Email Verification:**
  - `POST /v1/local/email/random_code` - Send 6-digit verification code to email (Redis TTL: 10min)
  - `POST /v1/local/email/verification` - Verify code and mark email as verified
  - **Usage:** Signup stage only (not for login)
  - **Code Format:** 6-digit numeric (000000-999999)
  - **Storage:** Redis with 10-minute TTL
  - **Error Codes:** `LocalAuthErrorCode` enum (E001-E004 for email verification)
- **Password Encoding:** BCrypt (10 rounds)
- **Session Management:** `SessionCreationPolicy.IF_REQUIRED` with HttpOnly, Secure cookies
- **Request Filter:** `JsonUsernamePasswordAuthenticationFilter` - Parses JSON body, validates/trims email/password
- **Authentication Flow:** Custom filter → AuthenticationManager → successfulAuthentication() → SuccessHandler
- **Session Storage:** `HttpSessionSecurityContextRepository` - Persists SecurityContext to session (critical!)
- **Handlers:** Custom success/failure handlers return JSON responses via `ApiResponse<LoginResponseDto>`
- **Error Codes:** `LocalAuthErrorCode` enum (C001-C004 for auth/state failures, M001-M004 for validation/user queries, E001-E004 for email verification)

**Key Classes:**
- `LocalAuthController` - REST endpoints for signup/login/check/logout + email verification endpoints
- `LocalAuthUseCase` - Business logic for signup and user queries (validation, encoding, member creation, user lookup via getMemberByEmail())
- `LocalAuthException` - Custom exception for local auth errors with ErrorCode
- `LocalUserDetailsService` - Implements `UserDetailsService` for loadUserByUsername()
- `JsonUsernamePasswordAuthenticationFilter` - Core authentication filter:
  - `attemptAuthentication()` - Parses JSON, validates/trims email/password, creates authentication token
  - `successfulAuthentication()` ← **Critical** - Stores SecurityContext via `HttpSessionSecurityContextRepository`, creates session, calls SuccessHandler
  - `requiresAuthentication()` - Content-Type validation
- `LocalAuthenticationSuccessHandler` - Handles successful authentication response:
  - Queries Member from DB
  - Returns JSON response with member info
  - **Note:** SecurityContext/session storage handled by filter, not here
- `LocalAuthenticationFailureHandler` - Returns error details on failed login
- `LocalLogoutSuccessHandler` - Returns success response on logout
- `LocalAuthConfig` - Configures `PasswordEncoder`, `AuthenticationManager`, `DaoAuthenticationProvider`

**Email Verification Classes (新規 - NEW):**
- `VerificationCode` - Value object for 6-digit random code generation and validation (domain/value)
- `EmailVerificationUseCase` - Business logic for email verification (sendVerificationCode, verifyCode)
- `EmailService` - JavaMailSender adapter for sending verification code via Gmail SMTP
- `EmailVerificationCodeStore` - Redis adapter for storing/retrieving verification codes with 10-minute TTL
- `EmailException` - Custom exception for email verification errors
- `EmailErrorCode` - Error codes enum (E001-E004): mail send failure, code expired, code mismatch, invalid email

**Security Features:**
- Email/password validation (8-50 char passwords, valid email format)
- Duplicate email checking during signup
- Session-based with CSRF protection
- Password never returned in responses (only stored encrypted in DB)
- Input sanitization: Email/password trimming in filter to prevent whitespace issues
- Session persistence: SecurityContext stored in HttpSession via `HttpSessionSecurityContextRepository`

**Critical Implementation Detail - SecurityContext Session Storage:**

Local authentication uses `HttpSessionSecurityContextRepository` to persist SecurityContext to session. This is essential for login state persistence:

1. **After successful authentication** in `JsonUsernamePasswordAuthenticationFilter.successfulAuthentication()`:
   - SecurityContext created and stored in ThreadLocal via `SecurityContextHolder.setContext()`
   - SecurityContext persisted to session via `repository.saveContext(context, request, response)`
   - HttpSession created to generate JSESSIONID cookie

2. **On subsequent requests** with JSESSIONID cookie:
   - Spring Security automatically loads SecurityContext from session
   - User remains authenticated without re-login
   - `GET /v1/local/check` verifies login status via `SecurityContextHolder.getContext()`

3. **Without this step** (common mistake):
   - SecurityContext exists only in ThreadLocal (per-thread)
   - Session has no stored context → new requests show as unauthenticated
   - Login state is lost after response

**Important Note on C003 Error (Login State/Bad Credentials):**

The `C003` error ("로그인 상태가 아닙니다" = "Not logged in") can indicate:
1. **During `/v1/local/login`:** Bad credentials (incorrect password, email not found, malformed email)
2. **During `/v1/local/check`:** User not logged in or session expired (no valid JSESSIONID cookie)

**Input Validation & Trimming (Applied):**

The `JsonUsernamePasswordAuthenticationFilter` validates/trims all inputs:
- **Email validation** via `validateAndTrimEmail()`:
  - Removes leading/trailing whitespace using `trim()`
  - Validates that email contains '@' character
  - Rejects blank or null emails with clear error messages
- **Password validation** via `validateAndTrimPassword()`:
  - Removes leading/trailing whitespace using `trim()`
  - Rejects blank or null passwords with clear error messages

**Why Input Validation Was Needed:**
- Emails with accidental whitespace couldn't be matched in database
- Invalid email formats silently failed at repository level
- Error logs showed malformed emails instead of clear validation errors

**Troubleshooting C003 Error:**
1. **For login attempt:** Verify email/password are correct and properly formatted
2. **Check for whitespace:** Ensure no leading/trailing spaces in email/password (auto-trimmed by filter)
3. **For check endpoint:** Verify JSESSIONID cookie is present and valid
4. **Database verification:** Ensure user was successfully created during signup
5. **Review logs:** Look for validation errors in JsonUsernamePasswordAuthenticationFilter logs

### CSRF Protection
- Cookie-based double-submit pattern
- Token: `XSRF-TOKEN` cookie & `X-XSRF-TOKEN` header
- Repository: `CookieCsrfTokenRepository`
- Excluded from CSRF: `/v1/local/signup`, `/v1/local/login`, `/v1/local/logout`, `/v1/local/check`

**Protected Endpoints:** `/v1/oauth/logout`, `/v1/myPage/**`, `/v1/items/**(POST/PUT/DELETE)`, `/v1/order/**`, `/v1/local/logout`

**Public/Auth-Required Endpoints:**
- **Public:** `/v1/oauth/login`, OAuth callbacks, `GET /v1/items`, `GET /v1/categories`, `/v1/local/signup`, `/v1/local/login`
- **Auth-Required:** `GET /v1/local/check` - requires active session (local login only)

## External Integrations

### Kakao Pay
- **Config:** `pay.kakao.base-url`, `pay.kakao.authorization`, `pay.kakao.cid` in properties
- **Adapter:** `KakaoPayAdapter` implements `PaymentGatewayPort`
- **Flow:** `ready()` → get TID → `complete()` with PG token → approval

### Naver Pay
- Similar structure to Kakao Pay
- **Config:** `pay.naver.*` properties
- **Adapter:** `NaverPayAdapter` implements `PaymentGatewayPort`

### AWS S3
- **Purpose:** Product image storage
- **Features:** Pre-signed URL generation for client-side uploads
- **Config:** `aws.region`, `aws.bucket`, `file.extensions`, `file.max.size` in properties
- **Usage:** Client requests pre-signed URL → uploads directly to S3 → backend stores S3 URL in DB
- **Local:** Disabled by `cloud.aws.s3.enabled=false`

## Error Handling

**Centralized error codes** in `com/C_platform/global/error/` and `com/C_platform/Member_woonkim/exception/`:
- `ItemErrorCode` (I001, I002, ...)
- `CategoryErrorCode` (C001, ...)
- `CreateOrderErrorCode` (O001-O007)
- `PaymentErrorCode`
- `CommonErrorCode`
- `LocalAuthErrorCode` (C001-C004 for auth/state failures, M001-M004 for signup validation/user queries)
- `EmailErrorCode` (E001-E004 for email verification: mail send failure, code expired, code mismatch, invalid email) **[NEW]**

**Exception flow:**
1. Domain raises custom exception (e.g., `ItemException`)
2. Exception carries error code and message key
3. Global exception handler converts to `ApiResponse` with HTTP status

**Response format:**
```json
{
  "success": true/false,
  "data": {...},
  "error": {
    "code": "I001",
    "message": "Item not found"
  },
  "metaData": {
    "timestamp": "2024-11-07T...",
    "requestId": "..."
  }
}
```

## Common Development Tasks

### Adding a New Feature
1. **Identify the module** (item, order, payment, member)
2. **Create domain layer** (entity, repository interface)
3. **Create application layer** (UseCase class with @UseCase, commands/DTOs)
4. **Create infrastructure** (JPA repository implementation, adapters)
5. **Create UI layer** (controller, request/response DTOs)
6. **Add error codes** in `global/error/` if needed
7. **Write tests** following existing test patterns

### Adding a Payment Provider
1. Create adapter class implementing `PaymentGatewayPort`
2. Register with `@Component("PROVIDERNAME")` (e.g., `@Component("KAKAOPAY")`)
3. Implement `ready()` and `complete()` methods
4. Add provider config in `application-oauth2-*.yml`
5. Update `PaymentService` to select adapter based on order/request

### Adding OAuth Provider
1. Create client class implementing `OauthClientPort` (see `KakaoClient`, `NaverClient`)
2. Register in `OauthClientRegister`
3. Create parser implementing `OAuthUserInfoParser` (see `KakaoOAuthUserInfoParser`)
4. Register in `OAuthUserInfoParserRegister`
5. Add provider config in `application-oauth2-*.yml`

### Running Tests During Development
```bash
# Watch mode would be ideal but Gradle doesn't support it natively
# Run tests after each edit manually:
./gradlew test

# Or run a specific test class:
./gradlew test --tests com.C_platform.item.service.ItemUseCaseTest
```

## Notes for Implementation

- **Character Encoding (UTF-8):** The application supports multilingual content including Korean characters in JSON requests.
  - HTTP message converters configured with UTF-8 charset in `WebConfig.java`
  - Servlet-level character encoding enforced via `application.properties` settings
  - See `claude/json_encoding_fix.md` for details on supporting Korean characters in signup/login
  - Always use `@Valid` on `@RequestBody` DTOs to trigger Spring's validation before request parsing
- **Message externalization:** Error and application messages use `messages.properties` and `messages_errors.properties`. Use `MessageSource` to retrieve localized strings.
- **Validation:** Use Jakarta Bean Validation annotations (`@NotNull`, `@Valid`, etc.) on DTOs.
- **Logging:** Minimal logging in config (see `application.properties` commented debug levels). Enable with care to avoid performance issues.
- **Data initialization:** Test data loaded from `CreateOrderTestData.sql` and `categorydata.sql` on startup (local only).
- **Build artifact:** Output JAR is always named `app.jar` (configured in `build.gradle`).
- **Multi-environment:** Use profiles: `local` (H2, file uploads disabled) vs `prod` (MySQL, S3 enabled). Profile activated via `spring.profiles.active`.

## File Naming & Organization Conventions

- **Entities:** `Item.java`, `Order.java`, `Payment.java` in `domain/`
- **Repositories:** `ItemRepository.java` in `infrastructure/`
- **Services/UseCases:** `ItemUseCase.java` or `ItemService.java` in `application/` or `service/`
- **Controllers:** `ItemController.java` in `ui/` or `presentation/controller/`
- **DTOs:** Request/Response suffixes, e.g., `CreateItemRequestDto.java`, `ItemDetailResponseDto.java`
- **Exceptions:** Custom exceptions extend base, e.g., `ItemException`, `CategoryException`
- **Error Codes:** Enums in `global/error/`, e.g., `ItemErrorCode`, `PaymentErrorCode`

## Key File References

| Component | Primary File |
|-----------|--------------|
| Security & OAuth | `config/SecurityConfig.java`, `Member_woonkim/application/OAuth2UseCase.java` |
| Web Configuration | `config/WebConfig.java` (HTTP message converters, UTF-8 charset for multilingual support) |
| Local Authentication Controller | `Member_woonkim/presentation/controller/LocalAuthController.java` (signup, login, check, logout endpoints) |
| Local Authentication UseCase | `Member_woonkim/application/useCase/LocalAuthUseCase.java` (signup validation, getMemberByEmail for check endpoint) |
| Local Auth Filter | `Member_woonkim/infrastructure/auth/filter/JsonUsernamePasswordAuthenticationFilter.java` - **Core component:**<br/>- `attemptAuthentication()` - JSON parsing, input validation/trimming<br/>- `successfulAuthentication()` ← **Critical** - SecurityContext storage via HttpSessionSecurityContextRepository, session creation<br/>- `requiresAuthentication()` - Content-Type validation |
| Local Auth Success Handler | `Member_woonkim/infrastructure/auth/handler/LocalAuthenticationSuccessHandler.java` (returns JSON response with member info, NOT responsible for session storage) |
| Local Auth Failure Handler | `Member_woonkim/infrastructure/auth/handler/LocalAuthenticationFailureHandler.java` (returns error response) |
| Local Auth Logout Handler | `Member_woonkim/infrastructure/auth/handler/LocalLogoutSuccessHandler.java` (returns logout success response) |
| Local Auth Exceptions | `Member_woonkim/exception/LocalAuthException.java` (custom exception), `Member_woonkim/exception/LocalAuthErrorCode.java` (error codes C001-C004, M001-M004) |
| Local Auth Config | `Member_woonkim/infrastructure/config/LocalAuthConfig.java` (PasswordEncoder, AuthenticationManager, DaoAuthenticationProvider setup) |
| Local User Details Service | `Member_woonkim/infrastructure/auth/service/LocalUserDetailsService.java` (UserDetailsService implementation) |
| Email Verification (NEW) | `Member_woonkim/domain/value/VerificationCode.java` - Value object for 6-digit random code generation |
| Email Verification (NEW) | `Member_woonkim/application/useCase/EmailVerificationUseCase.java` - Business logic for email verification |
| Email Verification (NEW) | `Member_woonkim/infrastructure/mail/EmailService.java` - Gmail SMTP email sender |
| Email Verification (NEW) | `Member_woonkim/infrastructure/cache/EmailVerificationCodeStore.java` - Redis storage for verification codes (10min TTL) |
| Email Verification (NEW) | `Member_woonkim/exception/EmailException.java`, `EmailErrorCode.java` - Email verification error handling |
| Item Domain | `item/domain/Item.java`, `item/infrastructure/ItemRepository.java` |
| Order Processing | `order/domain/Order.java`, `order/application/CreateOrderService.java` |
| Payment | `payment/domain/Payment.java`, `payment/infrastructure/adapter/KakaoPayAdapter.java` |
| Error Handling | `global/error/ErrorCode.java`, `global/error/CommonErrorCode.java`, `Member_woonkim/exception/LocalAuthErrorCode.java`, `EmailErrorCode.java` |
| API Response | `global/ApiResponse.java` |
