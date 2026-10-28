# Phase 9 — Library Management REST API

Spring Boot 3 / Java 17 / Maven / Spring Data JPA / Hibernate / PostgreSQL implementation of the supplied assignment.

## Included
- Entities and mappings: Author, Category, Book, Member, MembershipCard, BorrowRecord, shared audited BaseEntity.
- CRUD controllers for authors, categories, books and members; member card issuance/status/history; borrow/return/overdue APIs.
- Service-layer transaction boundaries for borrow/return, 3-active-loan limit, available inventory, duplicate active loans, member status/card validity and fines (₹5/day default).
- JPQL title/category search, author search repository query, overdue JOIN FETCH, top borrowed books, fine totals, derived query, modifying queries.
- Pagination response, DTOs, validation, centralized errors, OpenAPI UI, H2 context test, ER diagram source and starter Postman collection.


## Run
Prerequisites: Java 17+, Maven 3.9+, PostgreSQL 14+.

1. Create a PostgreSQL database named `library_db`.
2. Set the database connection environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/library_db
export DB_USERNAME=your_postgres_username
export DB_PASSWORD=your_postgres_password
```

3. Start the application:

```bash
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

Database configuration is environment-driven. Defaults are local development credentials only; do not use them outside local development. Hibernate uses `ddl-auto: update` as required for development. For production, use migrations (Flyway is a bonus task).

## Main endpoints
- Authors: `POST/GET /api/v1/authors`, `GET/PUT/DELETE /api/v1/authors/{id}`, `GET /api/v1/authors/{id}/books`
- Categories: CRUD `/api/v1/categories`
- Books: CRUD `/api/v1/books`, `GET /api/v1/books/search?title=&category=`, `GET /api/v1/books/available`, `GET /api/v1/books/top-borrowed`
- Members: CRUD `/api/v1/members`, `PATCH /api/v1/members/{id}/status`, `POST /api/v1/members/{id}/card`, `GET /api/v1/members/{id}/borrow-history`, `GET /api/v1/members/{id}/fines`
- Borrowing: `POST /api/v1/borrow` body `{"bookId":1,"memberId":1}`, `PUT /api/v1/return/{borrowId}`, `GET /api/v1/borrow/overdue`, `GET /api/v1/borrow/member/{memberId}/active`, paginated `GET /api/v1/borrow`.

All paginated list endpoints accept `page`, `size`, `sortBy`, `direction`. Example: `/api/v1/books?page=0&size=10&sortBy=title&direction=asc`.

## Borrow workflow
1. Create category and author.
2. Create a book with `categoryId`, `authorIds`, and `totalCopies`.
3. Create a member and issue a card using `POST /api/v1/members/{id}/card`.
4. Borrow using `POST /api/v1/borrow`.
5. Return using `PUT /api/v1/return/{borrowId}`. Fine is calculated as configured daily rate × late days.

## Verification status
- Run `mvn test` and exercise all endpoints locally before submission.
- The assignment requires at least 8 Mockito service tests and 3 `@DataJpaTest` repository tests. These are required deliverables and must be present and passing before submission.
- The optional author filter is wired through a dedicated paginated JPQL query. When author is supplied, the current query filters by title and author; combine with category filtering in a future refinement if you need all three filters simultaneously.
- For PostgreSQL-grade concurrent borrow safety, consider pessimistic locking on book/member rows or retry handling for optimistic-lock conflicts. `@Version` helps detect concurrent updates but does not by itself guarantee a friendly API response.
- `OVERDUE` status is refreshed by `POST /api/v1/borrow/refresh-overdue`; the assignment's optional scheduled job is not enabled automatically.
- ER diagram source is in `docs/er-diagram.mmd`. Render it to PNG and place the image in `docs/` for the final deliverable.

## Tests
```bash
mvn test
```

## Sample pagination response
```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```
