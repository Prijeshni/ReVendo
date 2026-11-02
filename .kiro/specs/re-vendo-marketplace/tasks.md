# Implementation Plan: ReVendo P2P Marketplace

## Overview

This plan breaks the ReVendo full-stack marketplace into discrete, ordered coding tasks that build on each other incrementally. The structure follows a foundation-first order: project scaffolding → database entities → backend services and API → frontend pages and components → testing. Each task references the specific requirements and design sections it implements.

Tasks marked with `*` are optional (test sub-tasks) and can be skipped for a faster MVP build. All core implementation tasks are required.

---

## Tasks

- [x] 1. Initialize project structure and tooling
  - [x] 1.1 Scaffold the Spring Boot backend project
    - Use Spring Initializr to create a Maven project with dependencies: Spring Web, Spring Security, Spring Data JPA, MySQL Driver, Lombok, Validation, jqwik (test scope)
    - Create the base package `com.revendo` with the sub-packages: `config`, `security`, `controller`, `service`, `repository`, `model`, `dto/request`, `dto/response`, `exception`
    - Add `application.properties` with MySQL datasource config, JPA DDL-auto set to `validate`, and server port 8080
    - Create `JwtConfig.java` with constants for JWT secret (read from env) and 24-hour expiry
    - _Requirements: 3.5 — JWT expiry, 14.1 — endpoint security_

  - [x] 1.2 Scaffold the React frontend project
    - Use Vite to create a React project (`npm create vite@latest revendo-frontend -- --template react`)
    - Install dependencies: `react-router-dom`, `axios`, `bootstrap`, `react-bootstrap`, and `react-hot-toast`
    - Import Bootstrap CSS in `main.jsx`
    - Create the directory structure: `src/api`, `src/components/common`, `src/components/auth`, `src/components/listings`, `src/pages`, `src/context`, `src/hooks`, `src/services`, `src/utils`
    - _Requirements: 15.1 — Bootstrap responsive layout_

  - [x] 1.3 Configure Axios instance with interceptors
    - Create `src/api/axiosInstance.js` with `baseURL` pointing to `http://localhost:8080/api`
    - Add a request interceptor that reads the JWT from `localStorage` and attaches it as `Authorization: Bearer <token>` on every outgoing request
    - Add a response interceptor that catches `401` responses, clears the JWT from `localStorage`, and redirects to `/login`
    - _Requirements: 3.12 — Bearer token on all requests, 3.8 — logout clears JWT_

- [ ] 2. Define database entities and repositories
  - [ ] 2.1 Create the `User` JPA entity and repository
    - Create `model/User.java` with all columns from the `users` table definition: `id`, `firstName`, `lastName`, `email` (unique), `phoneNumber`, `passwordHash`, `role` (ENUM `ROLE_USER`/`ROLE_ADMIN`), `status` (ENUM `Active`/`Suspended`), `failedLoginAttempts`, `lockedUntil`, `createdAt`
    - Annotate with `@Entity`, `@Table(name = "users")`, use `@Enumerated(EnumType.STRING)` for ENUM fields
    - Create `repository/UserRepository.java` extending `JpaRepository<User, Long>` with custom finder: `Optional<User> findByEmail(String email)`
    - _Requirements: 2.1, 3.13, 13.5_

  - [ ] 2.2 Create the `Listing` and `ListingImage` entities and repositories
    - Create `model/Listing.java` with all columns: `id`, `sellerId` (FK), `title`, `description`, `category`, `condition`, `listingType`, `price`, `rentalPeriodUnit`, `status`, `createdAt`, `updatedAt`
    - Use `@ManyToOne` for the `seller` relationship and `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` for `images`
    - Create `model/ListingImage.java` with `id`, `listing`, `imageUrl`, `displayOrder`
    - Create `repository/ListingRepository.java` with a `Page<Listing> findByFilters(...)` method using `@Query` with dynamic WHERE clauses for keyword, category, condition, listingType, minPrice, maxPrice, and status = 'Active'
    - _Requirements: 4.3, 4.4, 4.5, 4.6, 4.7, 5.6_

  - [ ] 2.3 Create `Transaction`, `Offer`, `Conversation`, `Message`, `WishlistItem`, and `Notification` entities and repositories
    - Create each entity class in `model/` matching the table definitions in the design document
    - `Transaction.java`: `id`, `listing`, `buyer`, `transactionType`, `pricePaid`, `transactionDate`, `rentalStartDate`, `rentalEndDate`, `rentalDuration`, `rentalPeriodUnit`
    - `Offer.java`: `id`, `listing`, `buyer`, `offerAmount`, `counterAmount`, `status`, `roundCount`, `createdAt`, `updatedAt`
    - `Conversation.java`: `id`, `listing`, `buyer`, `seller`, `createdAt`, `lastMessageAt`
    - `Message.java`: `id`, `conversation`, `sender`, `text`, `isRead`, `sentAt`
    - `WishlistItem.java`: `id`, `user`, `listing`, `addedAt`; add `@UniqueConstraint(columnNames = {"user_id", "listing_id"})` on the table
    - `Notification.java`: `id`, `user`, `message`, `isRead`, `createdAt`
    - Create a corresponding `Repository` interface for each entity
    - _Requirements: 7.2, 7.4, 8.2, 9.2, 9.4, 10.1, 8.3_

  - [ ] 2.4 Create the MySQL schema migration file
    - Create `src/main/resources/schema.sql` with `CREATE TABLE IF NOT EXISTS` statements for all 9 tables in the correct FK-safe order: `users` → `listings` → `listing_images` → `transactions` → `offers` → `conversations` → `messages` → `wishlist_items` → `notifications`
    - Include all indexes noted in the design: `users.email`, `listings.seller_id`, `listings.created_at`, `messages.conversation_id`, `messages.sent_at`, `notifications.user_id`
    - Set `spring.sql.init.mode=always` in `application.properties` to run on startup
    - _Requirements: full data model_

- [ ] 3. Implement Spring Security and JWT authentication infrastructure
  - [ ] 3.1 Implement `JwtTokenProvider`
    - Create `security/JwtTokenProvider.java` with methods: `generateToken(User user)` → returns a signed JWT string, `validateToken(String token)` → returns boolean, `getUserIdFromToken(String token)` → returns Long, `getRoleFromToken(String token)` → returns String
    - Use HMAC-SHA256 signing with the secret from `JwtConfig`; set expiry to `JwtConfig.EXPIRY_MS` (24 hours in milliseconds)
    - _Requirements: 3.2, 3.5, 3.10, 3.11_

  - [ ]* 3.2 Write property test for JWT round-trip (Property 3)
    - Create `src/test/java/com/revendo/security/JwtTokenProviderPropertyTest.java`
    - **Property 3: JWT issuance and validation round-trip**
    - **Validates: Requirements 3.2, 3.5**
    - Generate arbitrary `userId` (positive Long) and `role` (string) pairs; issue a JWT; parse it back; assert that `getUserIdFromToken` equals the original `userId` and `getRoleFromToken` equals the original `role`
    - Annotate with `@Property(tries = 100)`
    - Add comment: `// Feature: re-vendo-marketplace, Property 3: JWT issuance and validation round-trip`

  - [ ] 3.3 Implement `JwtAuthenticationFilter` and `SecurityConfig`
    - Create `security/JwtAuthenticationFilter.java` extending `OncePerRequestFilter`; extract `Bearer` token from `Authorization` header → call `jwtTokenProvider.validateToken()` → if valid, load `UserDetails` and set `SecurityContextHolder`
    - Create `config/SecurityConfig.java` as a `@Configuration` class: register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`; configure public routes (GET `/api/products/**`, POST `/api/auth/**`); require authentication for all other routes; require `ROLE_ADMIN` for `/api/admin/**`; enable CORS for `http://localhost:5173`
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [ ] 3.4 Implement `GlobalExceptionHandler`
    - Create `exception/GlobalExceptionHandler.java` annotated with `@ControllerAdvice`
    - Map `ResourceNotFoundException` → 404, `UnauthorizedException` → 401, Spring's `AccessDeniedException` → 403, `ValidationException` → 400, `MethodArgumentNotValidException` → 400 with field-level errors map, `ConflictException` → 409, and a catch-all `Exception` → 500
    - All responses use the standard body: `{ status, error, message, timestamp }`
    - _Requirements: 14.3, 14.4, 14.5_

- [ ] 4. Implement Authentication endpoints
  - [ ] 4.1 Implement `AuthService` — registration
    - Create `service/AuthService.java`
    - Implement `register(RegisterRequest dto)`: validate email format, check uniqueness (`userRepository.findByEmail`), validate phone (8–15 digits), validate password length (8–128 chars), hash password with `BCryptPasswordEncoder`, set role `ROLE_USER`, save user, return success message
    - Throw `ValidationException` with appropriate messages for each rule violation
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.8, 2.10_

  - [ ]* 4.2 Write property test for registration validation (Property 1)
    - Create `src/test/java/com/revendo/service/AuthServicePropertyTest.java`
    - **Property 1: Registration validation rejects any invalid input**
    - **Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7**
    - Use jqwik `@Provide` arbitraries to generate invalid inputs: emails without `@`, passwords shorter than 8 chars, phone numbers outside 8–15 digits; assert each triggers a `ValidationException` or non-2xx response

  - [ ]* 4.3 Write property test for password hashing (Property 2)
    - In `AuthServicePropertyTest.java`
    - **Property 2: Registered user round-trip — password is never stored in plaintext**
    - **Validates: Requirements 2.8, 14.6**
    - Generate arbitrary valid password strings; call `authService.register(...)`; query the saved `User`; assert `user.getPasswordHash()` does not equal the plaintext password

  - [ ] 4.4 Implement `AuthService` — login and lockout
    - Implement `login(LoginRequest dto)` in `AuthService.java`: check email exists (generic "Invalid credentials" if not), check `user.status == Suspended` → throw with "This account has been suspended" and 403, check `user.lockedUntil` is past current time → throw 429 with lockout message, verify password with `BCryptPasswordEncoder.matches()`; on failure increment `failedLoginAttempts` and if `>= 5` set `lockedUntil = now + 15 min`; on success reset `failedLoginAttempts = 0`, generate JWT with `JwtTokenProvider`, return `AuthResponse` (token, role, userId, displayName)
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.13, 14.8_

  - [ ]* 4.5 Write property test for login lockout (Property 4)
    - In `AuthServicePropertyTest.java`
    - **Property 4: Login lockout after repeated failures**
    - **Validates: Requirements 3.13**
    - After simulating 5 failed attempts, assert any subsequent login (even with correct credentials) is rejected within the lockout window

  - [ ] 4.6 Create `AuthController` and DTOs
    - Create `dto/request/RegisterRequest.java` with fields: `firstName`, `lastName`, `email`, `phoneNumber`, `password`, `confirmPassword`; annotate with Bean Validation annotations (`@NotBlank`, `@Size`, `@Email`)
    - Create `dto/request/LoginRequest.java` with `email` and `password`
    - Create `dto/response/AuthResponse.java` with `token`, `role`, `userId`, `displayName`
    - Create `controller/AuthController.java` with `POST /api/auth/register` → `authService.register()` → 201, and `POST /api/auth/login` → `authService.login()` → 200
    - _Requirements: 2.1, 3.1, 14.7_

  - [ ] 4.7 Checkpoint — ensure auth endpoints work end-to-end
    - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Product (Listing) backend
  - [x] 5.1 Implement `ProductService` — create and retrieve listings
    - Create `service/ProductService.java`
    - Implement `createListing(CreateListingRequest dto, Long sellerId, List<MultipartFile> images)`: validate all required fields, save images to `src/main/resources/static/images/` (or a configured upload directory) and build `ListingImage` objects, save `Listing` entity with `status = Active`, return `ListingResponse`
    - Implement `getListingById(Long id)`: fetch listing, throw `ResourceNotFoundException` if not found or status is `Deleted`/`Removed`
    - Implement `getListings(ListingFilterParams params, Pageable pageable)`: build dynamic query filtering active listings by keyword (case-insensitive LIKE on title/description), category, condition, listingType, minPrice, maxPrice; apply sort; return `Page<ListingResponse>`
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.9, 4.10, 4.11, 4.12, 5.6_

  - [x]* 5.2 Write property test for keyword search filter (Property 5)
    - Create `src/test/java/com/revendo/service/ProductServicePropertyTest.java`
    - **Property 5: Product search keyword filter**
    - **Validates: Requirements 4.3**
    - Generate a random keyword and a pool of listings (some containing the keyword in title/description, some not); call `getListings` with the keyword; assert every returned listing contains the keyword (case-insensitive); assert no matching listing is absent

  - [x]* 5.3 Write property test for price range filter (Property 6)
    - In `ProductServicePropertyTest.java`
    - **Property 6: Price range filter correctness**
    - **Validates: Requirements 4.7**
    - Generate `minPrice` and `maxPrice` where `minPrice >= 0` and `minPrice <= maxPrice`; assert every returned listing price is within `[minPrice, maxPrice]`

  - [x]* 5.4 Write property test for combined filter intersection (Property 7)
    - In `ProductServicePropertyTest.java`
    - **Property 7: Combined filter intersection**
    - **Validates: Requirements 4.13**
    - Generate random combinations of active filters; assert every returned listing satisfies ALL active filter predicates simultaneously

  - [x]* 5.5 Write property test for pagination size invariant (Property 8)
    - In `ProductServicePropertyTest.java`
    - **Property 8: Pagination size invariant**
    - **Validates: Requirements 4.2**
    - Assert that for any page request the count of returned listings is `<= 12`

  - [x]* 5.6 Write property test for listing ownership invariant (Property 9)
    - In `ProductServicePropertyTest.java`
    - **Property 9: Listing creation ownership invariant**
    - **Validates: Requirements 5.6**
    - For any created listing, assert the stored `seller_id` equals the JWT user's `userId`

  - [x] 5.7 Implement `ProductService` — update and delete listings
    - Implement `updateListing(Long listingId, UpdateListingRequest dto, Long requesterId, List<MultipartFile> newImages)`: verify requester is owner (throw 403 otherwise), validate updated fields, replace images if provided (delete old images from storage, save new ones), save updated entity
    - Implement `deleteListing(Long listingId, Long requesterId)`: verify requester is owner, check no active/pending Transaction exists for the listing (throw `ValidationException` if found), set `status = Deleted`
    - _Requirements: 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

  - [x] 5.8 Create `ProductController` and DTOs
    - Create `dto/request/CreateListingRequest.java`, `UpdateListingRequest.java` with Bean Validation annotations
    - Create `dto/response/ListingResponse.java` with all fields needed by the frontend (id, title, description, category, condition, listingType, price, rentalPeriodUnit, status, imageUrls, sellerDisplayName, sellerJoinDate, createdAt)
    - Create `controller/ProductController.java`: `GET /api/products` (public), `GET /api/products/:id` (public), `POST /api/products` (multipart, USER), `PUT /api/products/:id` (USER owner), `DELETE /api/products/:id` (USER owner)
    - _Requirements: 4.1, 5.1, 5.3, 6.1, 6.2, 12.1_

- [ ] 6. Implement Buy/Rent and Transaction backend
  - [ ] 6.1 Implement `ProductService` — purchase and rental
    - Implement `purchaseListing(Long listingId, Long buyerId)`: verify listing is Active and FOR_SALE, verify buyer is not the seller (throw 403), wrap status check + update in a `@Transactional` block with optimistic locking (`@Version` on Listing), set `status = Sold`, create `Transaction` record (type PURCHASE, pricePaid, transactionDate)
    - Implement `rentListing(Long listingId, Long buyerId, int duration)`: verify listing is Active and FOR_RENT, verify buyer is not the seller, validate duration (1–90), set `status = Rented`, create `Transaction` record (type RENTAL, calculate `rentalEndDate` from `rentalStartDate + duration * unit`)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 6.2 Write property test for sold/rented listing rejection (Property 15)
    - In `ProductServicePropertyTest.java`
    - **Property 15: Sold/Rented listings reject new transactions**
    - **Validates: Requirements 7.5**
    - For any listing whose status is Sold or Rented, assert any purchase or rental request returns a non-2xx response

  - [ ]* 6.3 Write property test for seller-cannot-buy-own-listing (Property 16)
    - In `ProductServicePropertyTest.java`
    - **Property 16: Seller cannot transact on own listing**
    - **Validates: Requirements 7.7**
    - For any listing, assert that a purchase/rental request where `requesterId == seller_id` is rejected

  - [ ] 6.4 Add purchase and rental endpoints to `ProductController`
    - Add `POST /api/products/:id/purchase` (USER) → `productService.purchaseListing()` → 200
    - Add `POST /api/products/:id/rent` (USER) with body `{ duration: int }` → `productService.rentListing()` → 200
    - _Requirements: 7.2, 7.4_

- [x] 7. Implement Negotiation backend
  - [x] 7.1 Implement `NegotiationService`
    - Create `service/NegotiationService.java`
    - Implement `submitOffer(Long listingId, Long buyerId, BigDecimal offerAmount)`: check no Pending/Countered offer exists for same (listingId, buyerId) pair (throw 409 if found), create `Offer` with status Pending and `roundCount = 0`, create `Notification` for seller
    - Implement `acceptOffer(Long offerId, Long actorId)`: verify actor is the seller (for Pending) or buyer (for Countered), set `status = Accepted`, create notification for the other party
    - Implement `rejectOffer(Long offerId, Long actorId)`: same ownership check, set `status = Rejected`, notify
    - Implement `counterOffer(Long offerId, Long sellerId, BigDecimal counterAmount)`: verify actor is seller, check `roundCount < 5` (throw 409 if at limit), increment `roundCount`, set `counterAmount`, set `status = Countered`, notify buyer
    - _Requirements: 8.2, 8.3, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10, 8.11, 8.12_

  - [x]* 7.2 Write property test for offer duplicate prevention (Property 10)
    - Create `src/test/java/com/revendo/service/NegotiationServicePropertyTest.java`
    - **Property 10: Offer duplicate prevention**
    - **Validates: Requirements 8.12**
    - For any (listingId, buyerId) pair with an existing Pending/Countered offer, assert a second `submitOffer` call throws a conflict exception

  - [x]* 7.3 Write property test for round-count enforcement (Property 11)
    - In `NegotiationServicePropertyTest.java`
    - **Property 11: Negotiation round-count enforcement**
    - **Validates: Requirements 8.11**
    - Generate an offer and simulate 5 counter rounds; assert the 6th `counterOffer` call is rejected; assert the seller can still accept or reject

  - [x] 7.4 Create `NegotiationController`
    - Create `controller/NegotiationController.java`
    - `POST /api/negotiations` → `submitOffer` → 201
    - `GET /api/negotiations/listing/:listingId` → seller's view of offers for a listing
    - `GET /api/negotiations/my-offers` → buyer's own offers
    - `PUT /api/negotiations/:offerId/accept` → `acceptOffer`
    - `PUT /api/negotiations/:offerId/reject` → `rejectOffer`
    - `PUT /api/negotiations/:offerId/counter` → `counterOffer`
    - _Requirements: 8.4, 8.5, 8.6, 8.7_

- [x] 8. Implement Messaging backend
  - [x] 8.1 Implement `MessagingService`
    - Create `service/MessagingService.java`
    - Implement `getOrCreateConversation(Long listingId, Long buyerId)`: look up existing Conversation by `(listingId, buyerId, sellerId)` triple; if none exists create it; return `ConversationResponse`
    - Implement `getConversations(Long userId)`: return all conversations where user is buyer or seller, with last message text truncated to 100 chars
    - Implement `getMessages(Long conversationId, Long requesterId)`: verify requester is a participant (throw 403 if not), return all messages ordered by `sentAt` ascending
    - Implement `sendMessage(Long conversationId, Long senderId, String text)`: verify sender is participant, create `Message`, update `conversation.lastMessageAt`
    - Implement `markAsRead(Long conversationId, Long readerId)`: set `isRead = true` on all messages in conversation sent by the other participant
    - Implement `getUnreadCount(Long userId)`: count messages where sender != userId and isRead = false in conversations the user participates in
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9_

  - [x]* 8.2 Write property test for conversation uniqueness (Property 13)
    - Create `src/test/java/com/revendo/service/MessagingServicePropertyTest.java`
    - **Property 13: Conversation uniqueness per buyer-seller-listing triple**
    - **Validates: Requirements 9.2, 9.3**
    - Call `getOrCreateConversation` N times for the same triple; assert exactly one Conversation row exists (verify `save` called only once)

  - [x]* 8.3 Write property test for message access control (Property 14)
    - In `MessagingServicePropertyTest.java`
    - **Property 14: Message access restricted to participants**
    - **Validates: Requirements 9.7**
    - Generate a conversation between two users; call `getMessages` with a third user's ID; assert a 403/`UnauthorizedException` is thrown

  - [x] 8.4 Create `MessagingController`
    - Create `controller/MessagingController.java`
    - `POST /api/messages/conversations` → `getOrCreateConversation`
    - `GET /api/messages/conversations` → `getConversations`
    - `GET /api/messages/conversations/:id` → `getMessages`
    - `POST /api/messages/conversations/:id` → `sendMessage`
    - `PUT /api/messages/conversations/:id/read` → `markAsRead`
    - `GET /api/messages/unread-count` → `getUnreadCount`
    - _Requirements: 9.1–9.10_

- [x] 9. Implement Wishlist backend
  - [x] 9.1 Implement `WishlistService`
    - Create `service/WishlistService.java`
    - Implement `addToWishlist(Long userId, Long listingId)`: check if entry already exists with `wishlistItemRepository.existsByUserIdAndListingId()`; if not, save new `WishlistItem`; if yes, return the existing entry (no-op)
    - Implement `removeFromWishlist(Long userId, Long listingId)`: delete the `WishlistItem` if it exists
    - Implement `getWishlist(Long userId)`: return all `WishlistItem`s for user with full listing data
    - Implement `isWishlisted(Long userId, Long listingId)`: return boolean
    - _Requirements: 10.1, 10.2, 10.3, 10.5, 10.6, 10.8_

  - [x]* 9.2 Write property test for wishlist idempotence (Property 12)
    - Create `src/test/java/com/revendo/service/WishlistServicePropertyTest.java`
    - **Property 12: Wishlist idempotence — no duplicates**
    - **Validates: Requirements 10.8, 10.2**
    - Call `addToWishlist` with the same (userId, listingId) pair `N` times (2–10); assert `wishlistItemRepository.save()` was called exactly once

  - [x] 9.3 Create `WishlistController`
    - Create `controller/WishlistController.java`
    - `GET /api/wishlist` → `getWishlist`
    - `POST /api/wishlist/:listingId` → `addToWishlist`
    - `DELETE /api/wishlist/:listingId` → `removeFromWishlist`
    - `GET /api/wishlist/:listingId/status` → `isWishlisted`
    - _Requirements: 10.1–10.8_

- [x] 10. Implement User and Admin backends
  - [x] 10.1 Implement `UserService` and `UserController`
    - Create `service/UserService.java` with: `getProfile(Long userId)`, `updateProfile(Long userId, UpdateProfileRequest dto)` (first name, last name, phone), `changePassword(Long userId, ChangePasswordRequest dto)` (verify current password with BCrypt, validate new password >= 8 chars, hash and save), `getPurchaseHistory(Long userId)`, `getRentalHistory(Long userId)`
    - Create `controller/UserController.java` with endpoints: `GET /api/users/me`, `PUT /api/users/me`, `PUT /api/users/me/password`, `GET /api/users/me/purchases`, `GET /api/users/me/rentals`, `GET /api/users/me/notifications`
    - _Requirements: 11.3, 11.4, 11.7, 11.8_

  - [x] 10.2 Implement `AdminService` and `AdminController`
    - Create `service/AdminService.java` with:
      - `getSummary()`: count total users, active listings, completed transactions, pending review listings
      - `getUsers(Pageable pageable, String searchQuery)`: paginated at 50/page, filter by partial name/email match
      - `suspendUser(Long userId)`: set user status to Suspended, set all user's Active listings to Suspended
      - `reactivateUser(Long userId)`: set user status to Active, restore all Suspended listings to Active
      - `getListings(Pageable pageable, AdminListingFilterParams params)`: paginated at 50/page, filter by category/status/listingType
      - `removeListing(Long listingId)`: set listing status to Removed, cancel any Pending transactions
    - Create `controller/AdminController.java` with the 6 endpoints from the design; all secured with `ROLE_ADMIN`
    - _Requirements: 13.1–13.9_

  - [x]* 10.3 Write property test for admin suspension cascade (Property 17)
    - Create `src/test/java/com/revendo/service/AdminServicePropertyTest.java`
    - **Property 17: Admin suspension cascades to listings**
    - **Validates: Requirements 13.5**
    - Generate a user with N (1–20) Active listings; call `suspendUser`; assert all N listings are now Suspended; assert no Active listings remain for that user

  - [x]* 10.4 Write property test for reactivation restores listings (Property 18)
    - In `AdminServicePropertyTest.java`
    - **Property 18: Admin reactivation restores suspended listings**
    - **Validates: Requirements 13.6**
    - Take a suspended user with N Suspended listings; call `reactivateUser`; assert all N listings are now Active

  - [x] 10.5 Checkpoint — ensure all backend endpoints pass unit tests
    - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Build shared frontend components
  - [x] 11.1 Implement `AuthContext` and authentication hooks
  - [x] 11.2 Implement `ProtectedRoute` and `AdminRoute`
  - [x] 11.3 Implement `Navbar` component
  - [x] 11.4 Implement `ProductCard`, `LoadingSpinner`, `ToastNotification`, `ConfirmDialog`, and `Footer`
  - [x] 11.5 Implement `Pagination`, `ImageCarousel`, `FilterPanel`, and `ImageUploader`
  - [x] 11.6 Implement `validators.js` and `formatters.js` utilities
  - [x] 11.7 Configure React Router and `App.jsx`
    - Create `src/context/AuthContext.jsx`: use `React.createContext()` to provide `{ currentUser, login(tokenData), logout() }` to the app
    - `login(tokenData)`: save JWT to `localStorage`, parse token payload for userId/role/displayName, set `currentUser` state
    - `logout()`: remove JWT from `localStorage`, set `currentUser` to null
    - Create `src/hooks/useAuth.js` that calls `useContext(AuthContext)`
    - Wrap `<App>` with `<AuthContext.Provider>` in `main.jsx`
    - _Requirements: 3.6, 3.7, 3.8, 3.9_

  - [ ] 11.2 Implement `ProtectedRoute` and `AdminRoute`
    - Create `src/components/auth/ProtectedRoute.jsx`: if `currentUser` is null, redirect to `/login`; otherwise render `<Outlet />`
    - Create `src/components/auth/AdminRoute.jsx`: if `currentUser?.role !== 'ROLE_ADMIN'`, redirect to home page; otherwise render `<Outlet />`
    - _Requirements: 5.2, 13.1, 13.2_

  - [ ] 11.3 Implement `Navbar` component
    - Create `src/components/common/Navbar.jsx`
    - When no user is authenticated: show ReVendo logo, search bar, category links, Login and Register buttons
    - When user is authenticated: show logo, search bar, category links, Wishlist icon with link to `/wishlist`, Messages icon with unread count badge (fetch `GET /api/messages/unread-count`), and a User profile dropdown with links to Dashboard and Logout
    - Clicking logout calls `AuthContext.logout()` and navigates to `/`
    - Use Bootstrap Navbar component; ensure it collapses on mobile
    - _Requirements: 1.1, 1.2, 3.8, 3.9, 9.9_

  - [ ] 11.4 Implement `ProductCard`, `LoadingSpinner`, `ToastNotification`, `ConfirmDialog`, and `Footer`
    - `src/components/common/ProductCard.jsx`: display product image (with placeholder fallback), title, price, category, condition badge, and "View Details" button linking to `/products/:id`; add rounded corners, drop shadow, and hover animation (150–300ms transition) via CSS
    - `src/components/common/LoadingSpinner.jsx`: Bootstrap spinner centered in a container
    - `src/components/common/ToastNotification.jsx`: use `react-hot-toast` to show auto-dismiss toasts (4 s); expose a `showToast(message, type)` helper via `src/hooks/useToast.js`
    - `src/components/common/ConfirmDialog.jsx`: Bootstrap modal with title, message, Confirm and Cancel buttons; takes `onConfirm` and `onCancel` props
    - `src/components/common/Footer.jsx`: links to About, Contact, Terms of Service, Privacy Policy
    - _Requirements: 1.5, 1.7, 11.10, 11.11, 15.3, 15.5, 15.6_

  - [ ] 11.5 Implement `Pagination`, `ImageCarousel`, `FilterPanel`, and `ImageUploader`
    - `src/components/common/Pagination.jsx`: render page number controls; call an `onPageChange(page)` prop callback
    - `src/components/listings/ImageCarousel.jsx`: Bootstrap Carousel showing listing images with placeholder fallback
    - `src/components/listings/FilterPanel.jsx`: panel with controls for category, condition, listingType, price range, and sort; validates that minPrice <= maxPrice before emitting filter change; shows "No results found" state
    - `src/components/listings/ImageUploader.jsx`: drag-and-drop or click-to-upload, validates file type (JPG/JPEG/PNG) and size (<= 5 MB) client-side, max 5 images, shows previews, prevents adding a 6th
    - _Requirements: 4.8, 4.14, 4.15, 5.5, 5.7, 5.8, 5.9, 5.11, 15.7_

  - [ ] 11.6 Implement `validators.js` and `formatters.js` utilities
    - `src/utils/validators.js`: export functions — `isValidEmail(str)`, `isValidPhone(str)` (8–15 digits), `isValidPassword(str)` (8–128 chars), `isValidPriceRange(min, max)`, `isNonEmpty(str)`, `isValidImageFile(file)`, `isValidImageSize(file)`
    - `src/utils/formatters.js`: export `formatCurrency(amount)`, `formatDate(isoString)`, `truncateText(text, maxLength)`
    - _Requirements: 2.3, 2.5, 2.6, 2.7, 4.8, 5.8, 9.5, 15.4_

  - [ ] 11.7 Configure React Router and `App.jsx`
    - Set up all routes in `src/App.jsx` using `createBrowserRouter` or `<Routes>` matching the routing table in the design
    - Wrap protected routes in `<ProtectedRoute>` and admin route in `<AdminRoute>`
    - Include `<NotFoundPage>` as the catch-all `*` route
    - _Requirements: 5.2, 13.2, 6.10_

- [ ] 12. Build authentication pages
  - [ ] 12.1 Implement `RegisterPage`
    - Create `src/pages/RegisterPage.jsx`
    - Form fields: first name (max 50), last name (max 50), email (max 254), phone number, password (max 128), confirm password
    - Run client-side validation with `validators.js` before submitting; display field-level error messages in red below each input (Bootstrap `invalid-feedback`)
    - Validate that password and confirm password match before sending to API (Requirement 2.7)
    - On success: show success toast and redirect to `/login`
    - Call `authService.register(formData)` from `src/services/authService.js`
    - _Requirements: 2.1–2.9, 15.4, 15.6_

  - [ ] 12.2 Implement `LoginPage`
    - Create `src/pages/LoginPage.jsx`
    - Form fields: email (max 254), password (max 128)
    - On success: call `AuthContext.login(tokenData)`, show success toast, redirect to `/`
    - On failure: display the API error message in a Bootstrap alert
    - Show loading spinner while request is pending
    - _Requirements: 3.1–3.7, 15.5, 15.6_

- [ ] 13. Build product browsing and details pages
  - [ ] 13.1 Implement `ProductListingPage`
    - Create `src/pages/ProductListingPage.jsx`
    - On mount, call `productService.getListings(filters, page)` and render `<ProductCard>` components in a Bootstrap responsive grid (12 cards/page)
    - Render `<FilterPanel>` on the side; on filter change reset to page 1 and re-fetch
    - Render `<Pagination>` at the bottom
    - Show `<LoadingSpinner>` while fetching; show "No results found" with clear-filters button when results are empty
    - Sync keyword with URL query param so the Navbar search bar navigates here
    - _Requirements: 4.1–4.15_

  - [ ] 13.2 Implement `ProductDetailsPage`
    - Create `src/pages/ProductDetailsPage.jsx`
    - Fetch listing by ID via `GET /api/products/:id`; navigate to `<NotFoundPage>` on 404
    - Render `<ImageCarousel>`, all listing fields from Requirement 6.2
    - Conditionally render buttons based on auth state and ownership:
      - Active + FOR_SALE + not owner → "Buy Now"
      - Active + FOR_RENT + not owner → "Rent Now"
      - Active + not owner → "Make an Offer" (opens offer modal)
      - Authenticated + not owner → "Contact Seller", "Add to Wishlist"
      - Owner → "Edit Listing", "Delete Listing"
    - Unauthenticated clicks on action buttons redirect to `/login`
    - Sold/Rented listings show status badge, no Buy/Rent buttons
    - _Requirements: 6.1–6.10_

- [ ] 14. Build listing creation and editing pages
  - [ ] 14.1 Implement `CreateListingPage`
    - Create `src/pages/CreateListingPage.jsx` (wrapped in `<ProtectedRoute>`)
    - Form with all required fields from Requirement 5.3 plus `<ImageUploader>`
    - Show `rentalPeriodUnit` dropdown only when `listingType === 'FOR_RENT'`
    - Submit as `multipart/form-data` via `productService.createListing(formData)`
    - On success: show toast and redirect to the new listing's details page
    - _Requirements: 5.1–5.11_

  - [ ] 14.2 Implement `EditListingPage`
    - Create `src/pages/EditListingPage.jsx` (wrapped in `<ProtectedRoute>`)
    - Pre-populate all form fields from the fetched listing data
    - Submit changes via `productService.updateListing(id, formData)`; handle 403 by redirecting to home
    - _Requirements: 12.1–12.5_

- [ ] 15. Build buy/rent confirmation flows
  - [ ] 15.1 Implement order and rental confirmation pages/modals
    - On "Buy Now" click: navigate to an `OrderConfirmationPage` (or render a confirmation modal) showing product title, image, price, and Seller name; on confirm call `productService.purchaseListing(id)` and redirect to `/dashboard`
    - On "Rent Now" click: navigate to a `RentalConfirmationPage` (or modal) showing product details and a duration input (whole number 1–90); on confirm call `productService.rentListing(id, duration)` and redirect to `/dashboard`
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 16. Build the messaging pages
  - [ ] 16.1 Implement `MessagesPage` and `ConversationPage`
    - `src/pages/MessagesPage.jsx`: fetch all conversations with `messagingService.getConversations()`; display list showing other participant name, listing title, and last message truncated to 100 chars; clicking a conversation navigates to `/messages/:conversationId`
    - `src/pages/ConversationPage.jsx`: fetch messages with `messagingService.getMessages(conversationId)`; call `messagingService.markAsRead(conversationId)` on open; render messages ordered by timestamp; provide a text input (max 2000 chars) and send button; on successful send response, append the new message to the view within 2 seconds
    - _Requirements: 9.1–9.10_

  - [ ] 16.2 Wire "Contact Seller" button to messaging flow
    - On `ProductDetailsPage`, clicking "Contact Seller" calls `messagingService.getOrCreateConversation(listingId)` and navigates to `/messages/:conversationId`
    - Pre-populate the conversation context with the listing title
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] 17. Build the Wishlist page
  - [ ] 17.1 Implement `WishlistPage`
    - Create `src/pages/WishlistPage.jsx`
    - Fetch wishlist with `wishlistService.getWishlist()` and render product cards
    - Show "No Longer Available" badge on cards where listing status is Sold or Rented
    - On `ProductCard`, show the Wishlist button in filled/active state for wishlisted items; clicking the filled button calls `wishlistService.removeFromWishlist(listingId)`
    - _Requirements: 10.5, 10.6, 10.7_

- [ ] 18. Build the User Dashboard
  - [ ] 18.1 Implement `UserDashboardPage` — My Listings and Account Settings sections
    - Create `src/pages/UserDashboardPage.jsx` with tabbed or sectioned layout
    - "My Listings" section: fetch user's listings via `GET /api/users/me` (or filtered product endpoint); display title, price, status; show Edit and Delete buttons only for Active listings; Delete triggers `<ConfirmDialog>`, on confirm calls `productService.deleteListing(id)`
    - "Account Settings" section: form to update first name, last name, phone; separate form to change password (current password + new password >= 8 chars); show field-level validation errors
    - _Requirements: 11.1, 11.2, 11.7, 11.8, 11.9, 11.10, 11.11, 11.12_

  - [ ] 18.2 Implement `UserDashboardPage` — Purchases, Rentals, Negotiations, Messages sections
    - "Purchases" section: fetch from `GET /api/users/me/purchases`; display product title, price paid, Seller name, purchase date
    - "Rentals" section: fetch from `GET /api/users/me/rentals`; display product title, price, Seller name, rental start and end dates
    - "Negotiations" section: fetch from `GET /api/negotiations/my-offers`; show offers with Pending/Countered status, offered price, and Accept/Reject controls for countered offers
    - "Messages" summary: show count of unread Conversations
    - _Requirements: 11.3, 11.4, 11.5, 11.6, 8.4, 8.8, 8.9, 8.10_

- [ ] 19. Build the Admin Dashboard
  - [ ] 19.1 Implement `AdminDashboardPage`
    - Create `src/pages/AdminDashboardPage.jsx` (wrapped in `<AdminRoute>`)
    - Summary section: fetch `GET /api/admin/summary`; display total users, active listings, completed transactions, pending review listings
    - "Manage Users" section: paginated table (50/page) with name, email, join date, status; search bar for partial name/email match; Suspend / Reactivate action buttons calling respective admin service methods; show toast on success
    - "Manage Listings" section: paginated table (50/page) with title, seller name, category, status; filter by category/status/listingType; Remove button with confirm dialog
    - _Requirements: 13.1–13.9_

- [ ] 20. Implement Homepage
  - [ ] 20.1 Implement `HomePage`
    - Create `src/pages/HomePage.jsx`
    - Hero section: headline, short description, CTA button navigating to `/products`
    - Featured Products section: fetch 8–20 most recent Active listings and render `<ProductCard>` components
    - Category cards section: 5 category cards (Electronics, Furniture, Books, Home Appliances, Household Goods) each linking to `/products?category=<name>`
    - Render `<Navbar>` and `<Footer>`
    - _Requirements: 1.1–1.8_

- [ ] 21. Apply global styling and responsive design
  - [ ] 21.1 Apply colour palette and responsive CSS
    - In `src/index.css` or a `variables.css` file: define CSS custom properties `--color-bg: #FFFFFF`, `--color-accent: #10B981`, `--color-accent-dark: #065F46`
    - Apply `--color-accent` as the primary button colour (override Bootstrap's `$primary`)
    - Verify Bootstrap grid breakpoints cover 320px (mobile), 768px (tablet), 1200px (desktop) — use Bootstrap's responsive container classes
    - Add CSS for `ProductCard` hover animation: `transform: translateY(-4px)` with `transition: 150ms–300ms ease`
    - Ensure all form validation error messages use red text rendered directly below input fields
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

- [ ] 22. Integration tests and final checkpoint
  - [ ]* 22.1 Write Spring Boot integration tests with Testcontainers
    - Add Testcontainers dependency and configure a `@SpringBootTest` base class that spins up a real MySQL container
    - Write integration test: full registration → login → JWT → create listing → fetch listing flow
    - Write integration test: conversation deduplication (two `POST /api/messages/conversations` calls for same triple → one row)
    - Write integration test: admin suspend user → all active listings become Suspended
    - _Requirements: full stack_

  - [ ]* 22.2 Write frontend unit tests with Jest + React Testing Library
    - Test `validators.js` functions with valid and boundary-edge inputs
    - Test `formatters.js` helpers
    - Test `ProtectedRoute` redirect behaviour for unauthenticated users
    - Test `AdminRoute` redirect behaviour for non-admin users
    - _Requirements: 2.3, 2.5, 2.6, 2.7, 4.8, 13.2_

  - [ ] 22.3 Final checkpoint — ensure all tests pass
    - Run `mvn test` in the Spring Boot project to execute all unit and property-based tests
    - Run `npm test -- --run` in the React project to execute all frontend tests
    - Resolve any failing tests before proceeding to deployment preparation
    - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP build
- Each task references specific requirement numbers for full traceability
- Property-based tests use jqwik's `@Property(tries = 100)` and follow the comment convention: `// Feature: re-vendo-marketplace, Property N: <property_text>`
- Integration tests use Testcontainers to avoid mocking the database
- Build the backend first (Tasks 1–10) before starting the frontend (Tasks 11–21) to have a working API to test against
- Store JWT only in `localStorage`; never include it in API response bodies
- Use `@Transactional` on service methods that perform multiple DB writes to prevent partial saves
- Apply `@Version` on the `Listing` entity for optimistic locking during concurrent purchase/rental

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["2.4", "3.1", "3.4"] },
    { "id": 3, "tasks": ["3.2", "3.3"] },
    { "id": 4, "tasks": ["4.1", "11.6"] },
    { "id": 5, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 6, "tasks": ["4.5", "4.6"] },
    { "id": 7, "tasks": ["5.1", "4.7"] },
    { "id": 8, "tasks": ["5.2", "5.3", "5.4", "5.5", "5.6", "5.7"] },
    { "id": 9, "tasks": ["5.8", "6.1"] },
    { "id": 10, "tasks": ["6.2", "6.3", "6.4", "7.1"] },
    { "id": 11, "tasks": ["7.2", "7.3", "7.4", "8.1"] },
    { "id": 12, "tasks": ["8.2", "8.3", "8.4", "9.1"] },
    { "id": 13, "tasks": ["9.2", "9.3", "10.1"] },
    { "id": 14, "tasks": ["10.2"] },
    { "id": 15, "tasks": ["10.3", "10.4", "10.5"] },
    { "id": 16, "tasks": ["11.1", "11.4", "11.5"] },
    { "id": 17, "tasks": ["11.2", "11.3", "11.7"] },
    { "id": 18, "tasks": ["12.1", "12.2", "13.1", "20.1"] },
    { "id": 19, "tasks": ["13.2", "14.1", "21.1"] },
    { "id": 20, "tasks": ["14.2", "15.1", "16.1", "17.1"] },
    { "id": 21, "tasks": ["16.2", "18.1", "19.1"] },
    { "id": 22, "tasks": ["18.2"] },
    { "id": 23, "tasks": ["22.1", "22.2"] },
    { "id": 24, "tasks": ["22.3"] }
  ]
}
```
