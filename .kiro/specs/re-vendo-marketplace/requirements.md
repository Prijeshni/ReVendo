# Requirements Document

## Introduction

ReVendo is a peer-to-peer (P2P) marketplace web application that enables users to buy, sell, and rent pre-owned electronics, furniture, books, home appliances, and household goods. The platform connects individual buyers and sellers, supporting features such as product listings, search and filtering, price negotiation, a messaging system, wishlists, user dashboards, and an admin panel. The application is built with React.js on the frontend, Java Spring Boot on the backend, MySQL as the database, and uses JWT-based authentication via Spring Security.

---

## Glossary

- **ReVendo**: The name of the marketplace web application.
- **Guest**: An unauthenticated visitor browsing the platform.
- **User**: A registered and authenticated individual who can buy, sell, or rent items.
- **Seller**: A User who has listed one or more products for sale or rent.
- **Buyer**: A User who is browsing, purchasing, or renting products.
- **Admin**: A privileged User with access to the Admin Dashboard for managing users, listings, and platform content.
- **Product**: An item listed on the platform with details including title, description, images, condition, price, category, and listing type.
- **Listing**: A Product entry created by a Seller, representing an item available for sale or rent.
- **Listing_Type**: The classification of a Listing as either "For Sale" or "For Rent".
- **Category**: A product classification group. Valid values are: Electronics, Furniture, Books, Home Appliances, Household Goods.
- **Condition**: The physical state of a listed Product. Valid values are: New, Like New, Good, Fair, Poor.
- **Negotiation**: A price offer submitted by a Buyer to a Seller for a specific Listing.
- **Offer**: A specific price amount submitted by a Buyer as part of a Negotiation.
- **Offer_Status**: The state of an Offer. Valid values are: Pending, Accepted, Rejected, Countered.
- **Message**: A text communication sent from one User to another, associated with a specific Listing.
- **Conversation**: A thread of Messages between two Users related to a specific Listing.
- **Wishlist**: A User's personal collection of saved Listings.
- **User_Dashboard**: A private page showing a User's active listings, purchase history, rental history, negotiations, messages, and account settings.
- **Admin_Dashboard**: A private page accessible only to Admin users for managing platform content, users, and listings.
- **JWT**: JSON Web Token used for stateless authentication.
- **Search**: A keyword-based query that filters Listings by title or description.
- **Filter**: A narrowing mechanism applied to Listings based on attributes like Category, Condition, price range, and Listing_Type.
- **Sort**: An ordering mechanism applied to Listings based on attributes like price, date, or relevance.
- **Featured_Products**: A curated set of Listings displayed on the Home Page to highlight popular or promoted items.
- **Authentication_Service**: The backend component responsible for registering users, validating credentials, issuing JWTs, and securing endpoints.
- **Product_Service**: The backend component responsible for creating, updating, retrieving, and deleting Listings.
- **Negotiation_Service**: The backend component responsible for managing Offers and their statuses.
- **Messaging_Service**: The backend component responsible for sending, receiving, and retrieving Messages.
- **Wishlist_Service**: The backend component responsible for managing a User's saved Listings.
- **User_Service**: The backend component responsible for managing User profiles and account data.
- **Admin_Service**: The backend component responsible for admin-level operations such as deactivating users and removing listings.
- **API**: The RESTful interface exposed by the Spring Boot backend consumed by the React frontend via Axios.

---

## Requirements

### Requirement 1: Home Page

**User Story:** As a Guest or User, I want to view an attractive and informative home page, so that I can understand what ReVendo offers and quickly access key features.

#### Acceptance Criteria

1. THE ReVendo SHALL display a navigation bar containing the ReVendo logo, a search bar, links to product categories, and buttons for Login and Register when no User is authenticated.
2. IF a User is authenticated, THEN THE ReVendo SHALL display the navigation bar with the ReVendo logo, a search bar, links to product categories, a Wishlist icon, a Messages icon, and a User profile dropdown menu instead of Login and Register buttons.
3. THE ReVendo SHALL display a hero section on the home page with a headline, a short description of the platform, and a call-to-action button that navigates to the product browsing page.
4. THE ReVendo SHALL display a Featured_Products section on the home page showing between 8 and 20 active Listings, ordered by most recently created, as product cards.
5. THE ReVendo SHALL display each product card in the Featured_Products section with the product image, title, price, category, a condition badge indicating one of (New, Like New, Good, Fair, Poor), and a "View Details" button; IF a product has no image, THEN THE ReVendo SHALL display a placeholder image in its place.
6. THE ReVendo SHALL display a product categories section on the home page showing the 5 categories (Electronics, Furniture, Books, Home Appliances, Household Goods) as clickable icons or cards that navigate to the filtered product browsing page.
7. THE ReVendo SHALL display a footer containing links to About, Contact, Terms of Service, and Privacy Policy pages.
8. WHEN a Guest clicks the "View Details" button on a product card, THE ReVendo SHALL navigate the Guest to the product details page without requiring authentication.

---

### Requirement 2: User Registration

**User Story:** As a Guest, I want to create an account on ReVendo, so that I can buy, sell, and rent products on the platform.

#### Acceptance Criteria

1. THE ReVendo SHALL provide a registration form with fields for first name (maximum 50 characters), last name (maximum 50 characters), email address (maximum 254 characters), phone number, password, and confirm password.
2. WHEN a Guest submits the registration form, THE Authentication_Service SHALL validate that all required fields are non-empty.
3. IF a Guest submits the registration form with an email address that does not conform to a valid email format (local-part@domain.tld), THEN THE Authentication_Service SHALL return an error message indicating the email address format is invalid.
4. IF a Guest submits the registration form with an email address that is already registered, THEN THE Authentication_Service SHALL return an error message indicating the email address is already in use.
5. IF a Guest submits the registration form with a phone number that is not between 8 and 15 digits, THEN THE Authentication_Service SHALL return an error message indicating the phone number is invalid.
6. IF a Guest submits the registration form with a password shorter than 8 characters or longer than 128 characters, THEN THE Authentication_Service SHALL return an error message indicating the password length requirement.
7. IF a Guest submits the registration form with a password and confirm password that do not match, THEN THE ReVendo SHALL display a validation error before submitting to the API.
8. WHEN a Guest submits a valid registration form, THE Authentication_Service SHALL create a new User account with the password stored as a hashed value and return a success response.
9. WHEN registration is successful, THE ReVendo SHALL redirect the new User to the login page and display a success message indicating that the account was created.
10. THE Authentication_Service SHALL assign the role of "ROLE_USER" to every newly registered User by default.

---

### Requirement 3: User Login and Authentication

**User Story:** As a registered User, I want to log in securely to my account, so that I can access my dashboard and perform authenticated actions on the platform.

#### Acceptance Criteria

1. THE ReVendo SHALL provide a login form with fields for email address (maximum 254 characters) and password (maximum 128 characters).
2. WHEN a User submits the login form with a valid email and password combination, THE Authentication_Service SHALL return a signed JWT.
3. IF the User submits the login form with an email address not associated with a registered account, or with an incorrect password, THEN THE Authentication_Service SHALL return an error response indicating that the credentials are invalid without specifying which field is incorrect.
4. IF the User submits the login form with an empty email address field or an empty password field, THEN THE Authentication_Service SHALL return an error response indicating that both fields are required, without attempting credential validation.
5. WHEN a JWT is issued, THE Authentication_Service SHALL set the token expiry to 24 hours.
6. WHEN THE Authentication_Service returns a successful login response containing a JWT, THE ReVendo SHALL store the JWT in the browser's local storage.
7. WHEN THE Authentication_Service returns a successful login response containing a JWT, THE ReVendo SHALL redirect the User to the home page.
8. WHEN a User clicks the logout option, THE ReVendo SHALL remove the JWT from local storage so that no JWT remains in local storage and no Bearer token is included in subsequent API requests.
9. WHEN a User clicks the logout option, THE ReVendo SHALL redirect the User to the home page in an unauthenticated state.
10. IF the Authentication_Service receives an API request with a JWT whose expiry timestamp has passed, THEN THE Authentication_Service SHALL return a 401 Unauthorized response.
11. IF the Authentication_Service receives an API request with a JWT that fails signature verification, THEN THE Authentication_Service SHALL return a 401 Unauthorized response.
12. WHILE a User is authenticated, THE ReVendo SHALL include the JWT as a Bearer token in the Authorization header of all API requests made via Axios.
13. IF a User submits the login form with invalid credentials 5 or more consecutive times from the same account, THEN THE Authentication_Service SHALL reject further login attempts for that account for 15 minutes and return an error response indicating that the account is temporarily locked.

---

### Requirement 4: Product Browsing, Search, Filter, and Sort

**User Story:** As a Guest or User, I want to browse, search, filter, and sort product listings, so that I can find items that match my needs quickly.

#### Acceptance Criteria

1. THE ReVendo SHALL display a product listing page showing all active Listings as product cards in a responsive grid layout, where each card displays at minimum the Listing title, price, condition, and at least one image.
2. WHEN a Guest or User requests a page of Listings, THE Product_Service SHALL return a maximum of 12 Listings per page.
3. WHEN a Guest or User enters a keyword of 1 to 250 characters in the search bar and submits, THE Product_Service SHALL return all active Listings where the title or description contains the keyword (case-insensitive).
4. WHEN a Guest or User applies a Category filter, THE Product_Service SHALL return only Listings that belong to the selected Category.
5. WHEN a Guest or User applies a Condition filter, THE Product_Service SHALL return only Listings that match the selected Condition.
6. WHEN a Guest or User applies a Listing_Type filter, THE Product_Service SHALL return only Listings matching "For Sale" or "For Rent" as selected.
7. WHEN a Guest or User applies a price range filter with a minimum and maximum value where the minimum is greater than or equal to 0 and less than or equal to the maximum, THE Product_Service SHALL return only Listings with a price within the specified range (inclusive).
8. IF a Guest or User applies a price range filter where the minimum value is greater than the maximum value, THEN THE ReVendo SHALL display a validation error indicating the minimum price must not exceed the maximum price and SHALL NOT submit the filter request.
9. WHEN a Guest or User selects a sort option of "Price: Low to High", THE Product_Service SHALL return Listings ordered by price ascending.
10. WHEN a Guest or User selects a sort option of "Price: High to Low", THE Product_Service SHALL return Listings ordered by price descending.
11. WHEN a Guest or User selects a sort option of "Newest First", THE Product_Service SHALL return Listings ordered by creation date descending.
12. THE Product_Service SHALL return Listings ordered by creation date descending by default when no sort option has been selected.
13. IF one or more filters or a sort option are active, THEN THE Product_Service SHALL apply all active filters together and return results ordered by the selected sort option, or by creation date descending if no sort option is selected.
14. WHEN a Guest or User applies or changes any filter or search keyword, THE ReVendo SHALL reset the displayed page to page 1 of the results.
15. WHEN no Listings match the applied search or filter criteria, THE ReVendo SHALL display a "No results found" message and a control to clear all active filters.

---

### Requirement 5: Product Listing Creation

**User Story:** As a User, I want to create a product listing, so that I can sell or rent my pre-owned items to other users.

#### Acceptance Criteria

1. THE ReVendo SHALL provide an authenticated "Sell / List an Item" page accessible only to logged-in Users.
2. WHEN an unauthenticated Guest attempts to access the listing creation page, THE ReVendo SHALL redirect the Guest to the login page.
3. THE ReVendo SHALL provide a listing creation form with the following required fields: title (maximum 150 characters), description (maximum 2000 characters), Category, Condition, Listing_Type ("For Sale" or "For Rent"), and price (minimum 0.01, maximum 999999999.99).
4. WHERE the Listing_Type is "For Rent", THE ReVendo SHALL additionally require the User to specify a rental period unit (per day, per week, or per month).
5. THE ReVendo SHALL allow the User to upload between 1 and 5 product images per Listing.
6. WHEN a User submits the listing creation form with all required fields and at least one image, THE Product_Service SHALL create the Listing, associate it with the authenticated User as the Seller, and set the Listing status to "Active".
7. IF a User submits the listing creation form with a missing required field, THEN THE ReVendo SHALL display a field-level validation error and prevent form submission.
8. IF a User attempts to upload an image file that is not of type JPG, JPEG, or PNG, THEN THE ReVendo SHALL display an error message and reject the file without removing any previously uploaded valid images.
9. IF a User attempts to upload an image file larger than 5 MB, THEN THE ReVendo SHALL display an error message stating the file size limit without removing any previously uploaded valid images.
10. WHEN a Listing is created successfully, THE ReVendo SHALL redirect the User to the newly created Listing's product details page.
11. IF a User attempts to upload a 6th image when 5 images are already attached, THEN THE ReVendo SHALL display an error message stating the maximum of 5 images has been reached and reject the additional file.

---

### Requirement 6: Product Details Page

**User Story:** As a Guest or User, I want to view the full details of a product listing, so that I can evaluate the item and decide whether to buy, rent, or contact the Seller.

#### Acceptance Criteria

1. THE ReVendo SHALL display a product details page for each Listing, accessible via a unique URL containing the Listing ID.
2. THE ReVendo SHALL display the following information on the product details page: all product images in a carousel or gallery, title, description, Category, Condition, Listing_Type, price, seller display name, seller join date, and the date the Listing was created; IF a Listing has no image, THEN THE ReVendo SHALL display a placeholder image.
3. IF the Listing_Type is "For Sale" and the Listing status is "Active", THEN THE ReVendo SHALL display a "Buy Now" button on the product details page.
4. IF the Listing_Type is "For Rent" and the Listing status is "Active", THEN THE ReVendo SHALL display a "Rent Now" button on the product details page.
5. IF the authenticated User is not the Seller of the Listing and the Listing status is "Active", THEN THE ReVendo SHALL display a "Make an Offer" button on the product details page.
6. IF the User is authenticated and is not the Seller, THEN THE ReVendo SHALL display a "Contact Seller" button on the product details page.
7. IF the User is authenticated and is not the Seller, THEN THE ReVendo SHALL display an "Add to Wishlist" button on the product details page.
8. WHEN an authenticated User is the Seller of the Listing being viewed, THE ReVendo SHALL hide the "Buy Now", "Rent Now", "Make an Offer", "Contact Seller", and "Add to Wishlist" buttons and instead display "Edit Listing" and "Delete Listing" buttons.
9. WHEN a Guest clicks the "Buy Now", "Rent Now", "Make an Offer", "Contact Seller", or "Add to Wishlist" button, THE ReVendo SHALL redirect the Guest to the login page.
10. WHEN a Listing is not found or has a status of "Deleted" or "Removed", THE ReVendo SHALL display a 404 error page with a link back to the product browsing page.

---

### Requirement 7: Buy and Rent Functionality

**User Story:** As a User, I want to purchase or rent a product, so that I can complete a transaction with the Seller.

#### Acceptance Criteria

1. WHEN an authenticated User clicks "Buy Now" on a Listing with Listing_Type "For Sale" and status "Active", THE ReVendo SHALL display an order confirmation page showing the product title, image, price, and Seller name.
2. WHEN an authenticated User confirms the purchase on the order confirmation page, THE Product_Service SHALL update the Listing status to "Sold", record the transaction with the Buyer's User ID and the purchase timestamp, and redirect the Buyer to their User_Dashboard.
3. WHEN an authenticated User clicks "Rent Now" on a Listing with Listing_Type "For Rent" and status "Active", THE ReVendo SHALL display a rental confirmation page showing the product title, image, price per rental period unit, Seller name, and a rental duration input field accepting a whole number between 1 and 90.
4. WHEN an authenticated User confirms the rental on the rental confirmation page, THE Product_Service SHALL update the Listing status to "Rented", record the rental transaction with the Buyer's User ID, a system-assigned rental start date of the current UTC date, and the calculated end date based on the rental duration and period unit, and redirect the Buyer to their User_Dashboard.
5. IF a Listing status is "Sold" or "Rented", THEN THE Product_Service SHALL reject any new purchase or rental request for that Listing and return an error response.
6. WHEN a Listing status is "Sold" or "Rented", THE ReVendo SHALL display the Listing with a status badge indicating "Sold" or "Rented" and remove the "Buy Now" and "Rent Now" buttons.
7. IF an authenticated User is the Seller of a Listing, THEN THE Product_Service SHALL reject any purchase or rental request submitted by that User for their own Listing.

---

### Requirement 8: Price Negotiation

**User Story:** As a User, I want to make a price offer on a product, so that I can negotiate a better deal with the Seller.

#### Acceptance Criteria

1. WHEN an authenticated User clicks "Make an Offer" on an active Listing, THE ReVendo SHALL display an offer form with a field for the proposed offer amount (minimum value of 0.01).
2. WHEN an authenticated User submits an Offer, THE Negotiation_Service SHALL create the Offer with Offer_Status "Pending" and associate it with the Listing and the Buyer.
3. WHEN an Offer is submitted by a Buyer, THE Negotiation_Service SHALL notify the Seller by creating a notification entry for the Seller.
4. WHEN the Seller views a pending Offer in the User_Dashboard, THE ReVendo SHALL display the offered price, the Buyer's display name, and options to Accept, Reject, or Counter the Offer.
5. WHEN the Seller accepts an Offer, THE Negotiation_Service SHALL update the Offer_Status to "Accepted" and notify the Buyer.
6. WHEN the Seller rejects an Offer, THE Negotiation_Service SHALL update the Offer_Status to "Rejected" and notify the Buyer.
7. WHEN the Seller counters an Offer, THE Negotiation_Service SHALL record the counter price, update the Offer_Status to "Countered", and notify the Buyer; one exchange of Buyer offer followed by a Seller response (Accept, Reject, or Counter) constitutes one negotiation round.
8. WHEN the Buyer receives a counter offer (Offer_Status is "Countered"), THE ReVendo SHALL display the counter price in the User_Dashboard with options to Accept or Reject the counter offer.
9. WHEN the Buyer accepts a counter offer, THE Negotiation_Service SHALL update the Offer_Status to "Accepted" and the Buyer may proceed to purchase the Listing at the countered price.
10. WHEN the Buyer rejects a counter offer, THE Negotiation_Service SHALL update the Offer_Status to "Rejected" and the negotiation for that Offer ends.
11. IF the number of completed negotiation rounds for a Listing and Buyer pair reaches 5, THEN THE Negotiation_Service SHALL prevent the Seller from submitting another counter offer and require a final Accept or Reject decision.
12. IF a Buyer has an existing Offer with Offer_Status "Pending" or "Countered" on a Listing, THEN THE Negotiation_Service SHALL prevent the same Buyer from submitting a new Offer on the same Listing.

---

### Requirement 9: Messaging System

**User Story:** As a User, I want to send and receive messages with other Users about a product, so that I can ask questions, negotiate, and coordinate transactions.

#### Acceptance Criteria

1. WHEN an authenticated User clicks "Contact Seller" on a product details page, THE ReVendo SHALL open a messaging interface pre-populated with the Listing title as context.
2. WHEN an authenticated User initiates contact with a Seller on a Listing and no Conversation between that Buyer, that Seller, and that Listing already exists, THE Messaging_Service SHALL create a new Conversation linking the Buyer's User ID, the Seller's User ID, and the Listing ID.
3. WHEN an authenticated User initiates contact with a Seller on a Listing and a Conversation between that Buyer, that Seller, and that Listing already exists, THE Messaging_Service SHALL open the existing Conversation without creating a duplicate.
4. WHEN a User sends a Message in a Conversation, THE Messaging_Service SHALL store the Message with the sender's User ID, the Conversation ID, the message text of up to 2000 characters, and a UTC timestamp.
5. THE ReVendo SHALL display a Messages page listing all Conversations for the authenticated User, showing the other participant's name, the Listing title, and the last Message text truncated to a maximum of 100 characters.
6. WHEN a User opens a Conversation, THE Messaging_Service SHALL return all Messages in that Conversation ordered by timestamp ascending.
7. IF a User attempts to read or send Messages in a Conversation in which they are not a participant, THEN THE Messaging_Service SHALL reject the request and return an error indicating unauthorized access.
8. WHEN a User opens a Conversation, THE Messaging_Service SHALL mark all Messages in that Conversation sent by the other participant as read.
9. WHEN a User has unread Messages, THE ReVendo SHALL display an unread message count badge showing the number of unread Messages on the Messages icon in the navigation bar.
10. WHEN a User sends a message in a Conversation and the API response confirms success, THE ReVendo SHALL display the new Message in the Conversation view within 2 seconds of the successful API response.

---

### Requirement 10: Wishlist

**User Story:** As a User, I want to save products to a wishlist, so that I can revisit items I am interested in purchasing or renting later.

#### Acceptance Criteria

1. WHEN an authenticated User clicks "Add to Wishlist" on a product details page or product card, THE Wishlist_Service SHALL add the Listing to the User's Wishlist.
2. IF a Listing is already present in the authenticated User's Wishlist, THEN THE ReVendo SHALL display the "Add to Wishlist" button in an active/filled state to indicate the item is saved.
3. WHEN an authenticated User clicks the active Wishlist button on a Listing that is already in the Wishlist, THE Wishlist_Service SHALL remove the Listing from the User's Wishlist.
4. IF an unauthenticated User clicks "Add to Wishlist", THEN THE ReVendo SHALL redirect the User to the login page without adding the Listing to any Wishlist.
5. THE ReVendo SHALL provide a dedicated Wishlist page accessible to authenticated Users, displaying all saved Listings as product cards.
6. WHEN a User navigates to the Wishlist page, THE Wishlist_Service SHALL return only the Listings currently in the User's Wishlist.
7. WHEN a Listing in the User's Wishlist has a status of "Sold" or "Rented", THE ReVendo SHALL display the product card with a "No Longer Available" badge on the Wishlist page.
8. THE Wishlist_Service SHALL prevent duplicate entries, ensuring the same Listing cannot be added to the same User's Wishlist more than once.

---

### Requirement 11: User Dashboard

**User Story:** As a User, I want a personal dashboard to manage all my activity on ReVendo, so that I can track my listings, purchases, rentals, negotiations, and messages in one place.

#### Acceptance Criteria

1. THE ReVendo SHALL provide a User_Dashboard accessible only to authenticated Users.
2. THE User_Dashboard SHALL display a "My Listings" section showing all Listings created by the User with their status (Active, Sold, Rented, Deleted), title, price, and Edit and Delete action buttons only for Listings with a status of "Active".
3. THE User_Dashboard SHALL display a "Purchases" section showing all Listings the User has purchased, including the product title, price paid, Seller name, and purchase date.
4. THE User_Dashboard SHALL display a "Rentals" section showing all Listings the User has rented, including the product title, price paid, Seller name, rental start date, and rental end date.
5. THE User_Dashboard SHALL display a "Negotiations" section showing all Offers the User has made or received where the Offer_Status is "Pending" or "Countered", with the current Offer_Status and offered price.
6. THE User_Dashboard SHALL display a "Messages" summary section showing the count of unread Conversations.
7. THE User_Dashboard SHALL display an "Account Settings" section allowing the User to update their first name, last name, phone number, and password.
8. WHEN a User updates their password in Account Settings, THE User_Service SHALL require the User to enter their current password for verification and the new password must be a minimum of 8 characters before updating to the new password.
9. WHEN a User clicks "Edit" on a Listing in the "My Listings" section, THE ReVendo SHALL navigate the User to the listing edit form pre-populated with the current Listing data.
10. WHEN a User clicks "Delete" on a Listing in the "My Listings" section, THE ReVendo SHALL display a confirmation dialog before permanently removing the Listing.
11. WHEN a User dismisses or cancels the confirmation dialog without confirming, THE ReVendo SHALL close the dialog and leave the Listing unchanged.
12. WHEN a User confirms deletion of a Listing, THE Product_Service SHALL set the Listing status to "Deleted" and exclude it from all public search results and product listing pages.

---

### Requirement 12: Product Listing Management (Edit and Delete)

**User Story:** As a Seller, I want to edit or delete my product listings, so that I can keep my listings accurate or remove items I no longer want to sell or rent.

#### Acceptance Criteria

1. IF a User is authenticated and is the Seller of a Listing, THEN THE ReVendo SHALL display the listing edit form pre-populated with the current data of that Listing.
2. IF an authenticated User who is not the Seller of a Listing attempts to access the edit URL for that Listing, THEN THE Product_Service SHALL return a 403 Forbidden response and THE ReVendo SHALL redirect the User to the home page.
3. WHEN a Seller submits the edit form with valid data, THE Product_Service SHALL update only the changed fields and return the updated Listing data.
4. IF the Seller submits the edit form with an invalid field value (e.g., empty title, price below 0.01), THEN THE ReVendo SHALL display a field-level validation error and prevent form submission.
5. WHEN a Seller replaces images during an edit, THE Product_Service SHALL delete the previously stored images and store the newly uploaded images, subject to a maximum of 5 images.
6. IF a Seller attempts to delete a Listing that has an active or pending Transaction, THEN THE Product_Service SHALL reject the deletion and return an error response indicating the Listing has an active transaction.
7. WHEN a Seller deletes a Listing with no active or pending Transactions, THE Product_Service SHALL set the Listing status to "Deleted" and THE ReVendo SHALL exclude the Listing from all public pages, search results, and Wishlists.

---

### Requirement 13: Admin Dashboard

**User Story:** As an Admin, I want a dedicated dashboard to manage platform content and users, so that I can maintain the quality and safety of the ReVendo marketplace.

#### Acceptance Criteria

1. THE ReVendo SHALL provide an Admin_Dashboard accessible only to authenticated Users with the role "ROLE_ADMIN".
2. IF a User without "ROLE_ADMIN" attempts to access the Admin_Dashboard URL, THEN THE ReVendo SHALL return a 403 Forbidden response and redirect the User to the home page.
3. WHEN an Admin opens the Admin_Dashboard, THE Admin_Service SHALL display a summary section showing total registered Users, total active Listings, total completed transactions (sold and rented), and total Listings pending review.
4. THE Admin_Dashboard SHALL display a "Manage Users" section listing all registered Users, paginated at 50 Users per page, with each row showing their name, email, join date, status (Active, Suspended), and action buttons (View, Suspend, Reactivate).
5. WHEN an Admin suspends a User, THE Admin_Service SHALL set the User's status to "Suspended", prevent the User from logging in, and set all of that User's Active Listings to a "Suspended" status.
6. WHEN an Admin reactivates a User, THE Admin_Service SHALL set the User's status to "Active" and restore all of that User's Listings that were set to "Suspended" status back to "Active".
7. THE Admin_Dashboard SHALL display a "Manage Listings" section listing all Listings, paginated at 50 Listings per page, with each row showing the title, Seller name, category, status, and action buttons (View, Remove).
8. WHEN an Admin removes a Listing, THE Admin_Service SHALL set the Listing status to "Removed", exclude it from all public pages and search results, and cancel any Pending Transactions associated with that Listing.
9. THE Admin_Dashboard SHALL allow the Admin to search Users by partial, case-insensitive name or email match, and filter Listings by Category, status, or Listing_Type.

---

### Requirement 14: Security and Authorization

**User Story:** As a platform operator, I want all endpoints and pages to be properly secured, so that unauthorized users cannot access or manipulate data they do not own.

#### Acceptance Criteria

1. THE Authentication_Service SHALL protect all API endpoints that modify or read user-specific data by requiring a valid, non-expired JWT in the Authorization header.
2. THE Authentication_Service SHALL expose public (unauthenticated) access only to: home page, product listing page, product details page, product search and browse endpoints, registration endpoint, and login endpoint.
3. IF an API request is received without a JWT for a protected endpoint, THEN THE Authentication_Service SHALL return a 401 Unauthorized response.
4. IF an API request is received with a JWT that is expired or fails signature verification for a protected endpoint, THEN THE Authentication_Service SHALL return a 401 Unauthorized response.
5. IF an API request is received with a valid JWT but the User does not have permission to perform the requested action (e.g., editing another User's Listing), THEN THE Authentication_Service SHALL return a 403 Forbidden response.
6. THE Authentication_Service SHALL hash all User passwords before persisting them to the database.
7. THE ReVendo SHALL not include JWT tokens or User password values in any API response body.
8. WHEN a suspended User attempts to log in, THE Authentication_Service SHALL return a 403 Forbidden response with a message stating the account has been suspended.

---

### Requirement 15: Responsive Design and UI Standards

**User Story:** As a User or Guest, I want the ReVendo platform to be visually appealing and usable on both desktop and mobile devices, so that I can browse and transact comfortably from any device.

#### Acceptance Criteria

1. THE ReVendo SHALL implement a responsive layout using Bootstrap grid system that adapts to screen widths of at least 320px (mobile), 768px (tablet), and 1200px (desktop).
2. THE ReVendo SHALL apply a consistent colour palette using white (#FFFFFF) as the primary background, emerald green (#10B981) as the primary accent, and dark green (#065F46) as the secondary accent throughout all pages.
3. THE ReVendo SHALL display product cards with rounded corners, a drop shadow, and a hover animation with a transition duration between 150ms and 300ms across all product listing views.
4. THE ReVendo SHALL display all form validation error messages in red directly below the respective input field.
5. THE ReVendo SHALL display a loading spinner indicator while waiting for an API response that has not resolved, and remove the spinner once the response is received.
6. THE ReVendo SHALL display toast notification messages that auto-dismiss after 4 seconds for all key user actions including: successful login, successful registration, successful listing creation, item added to wishlist, message sent, and offer submitted.
7. WHEN the ReVendo displays images that have not yet loaded, THE ReVendo SHALL show a placeholder image or skeleton loader in place of the product image.
```
