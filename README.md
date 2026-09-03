# Product Inventory API

Spring Boot service that imports a product inventory spreadsheet, validates it, and exposes a paginated, sortable catalogue with stock-age metrics. Back end of the [Product Inventory](https://github.com/joseortiz87/product-inventory-ui) technical test; the Angular front end lives in [`product-inventory-ui`](https://github.com/joseortiz87/product-inventory-ui).

| | |
|---|---|
| Runtime | Java 17, Spring Boot 4.1 (Spring Framework 7.0) |
| Persistence | Spring Data JPA, H2 in-memory (no external database needed) |
| Spreadsheet parsing | Apache POI 5.5 with formula evaluation |
| Docs | OpenAPI 3 via springdoc, Swagger UI at `/swagger-ui.html` |
| Tests | JUnit 5, Mockito, MockMvc, `@DataJpaTest`, `@SpringBootTest` |

## Run it

### Everything with Docker Compose (API + UI)

Clone both repositories side by side, then from this one:

```bash
git clone https://github.com/joseortiz87/product-inventory-api.git
git clone https://github.com/joseortiz87/product-inventory-ui.git
cd product-inventory-api
docker compose up --build
```

| Service | URL |
|---|---|
| Dashboard UI | http://localhost:4200 |
| API | http://localhost:8080/api/products |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

Ports clash with something on your machine? `API_PORT=9090 UI_PORT=3000 docker compose up --build`.

### API only, from source

```bash
./mvnw -v >/dev/null 2>&1 || true   # uses your local Maven 3.9+ and JDK 17+
mvn spring-boot:run
```

Then import a sample file:

```bash
curl -F file=@samples/inventory-sample.xlsx http://localhost:8080/api/products/import
curl "http://localhost:8080/api/products?size=5&sort=stockAge,desc"
curl http://localhost:8080/api/products/summary
```

`samples/inventory-with-errors.xlsx` exercises every validation rule at once, including a `#DIV/0!` formula and an in-file duplicate.

## Spreadsheet contract

First sheet, header on row 1, columns in this order (header text is matched case-insensitively):

| Column | Type | Rules |
|---|---|---|
| Product SKU | text | required, max 255 chars |
| Product Name | text | required, max 255 chars |
| Category | text | required, max 255 chars |
| Purchase Date | Excel date **or** text | required; text accepted as `yyyy-MM-dd`, `MM/dd/yyyy`, `M/d/yyyy`, `dd-MMM-yyyy`; not in the future |
| Unit Price | number or currency text (`$1,250.00`) | required, not negative |
| Quantity | number | required, whole, not negative |

**Product SKU + Purchase Date must be unique**, both within the file and against rows already imported. Formulas are evaluated; a formula that fails (or a stored Excel error) is reported with its cell reference.

The import is **atomic**: if any row fails, nothing is written and every problem is returned in one response, so the spreadsheet can be fixed in a single pass.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/products/import` | Multipart upload, field `file`. Returns `201` with `{fileName, importedRows, totalProducts}`. |
| `GET` | `/api/products?page=0&size=20&sort=stockAge,desc` | Paginated listing. Sortable: `sku`, `name`, `category`, `purchaseDate`, `unitPrice`, `quantity`, `stockAge`. |
| `GET` | `/api/products/summary` | `{totalProducts, totalInventoryValue, averageStockAgeDays, oldestPurchaseDate, newestPurchaseDate}` |
| `DELETE` | `/api/products/{id}` | Deletes one product. `404` with `PRODUCT_NOT_FOUND` when the id does not exist. |
| `DELETE` | `/api/products` | Clears the inventory (demo convenience). |

Each product carries `totalValue` (`unitPrice × quantity`) and `stockAgeDays` (days between purchase date and today), computed server-side so every client shows the same figures.

### Error contract

Every error, whether raised by the domain or by the framework, uses one envelope:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The uploaded file contains invalid data.",
    "details": [
      {
        "code": "DUPLICATE_ENTRY_IN_FILE",
        "message": "Row 7: SKU 'ABC-1' with purchase date 2026-01-05 also appears on row 3.",
        "info": { "row": 7, "field": "sku", "sku": "ABC-1", "purchaseDate": "2026-01-05", "firstRow": 3 }
      },
      {
        "code": "NEGATIVE_VALUE",
        "message": "Row 9: 'unitPrice' value -4.5 must not be negative.",
        "info": { "row": 9, "cell": "E9", "field": "unitPrice", "value": -4.5, "min": 0 }
      }
    ]
  }
}
```

| Top-level code | HTTP | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | One or more rows failed a rule |
| `INVALID_FILE` | 400 | Missing/empty/oversized/unsupported/unreadable file, wrong headers, no data rows, too many rows |
| `BAD_REQUEST` | 400 | Unknown sort field, page size over the limit |
| `NOT_FOUND` | 404 | Deleting a product id that does not exist |
| `INTERNAL_ERROR` | 500 | Anything unexpected; details are never leaked |

All codes live in one enum, [`ErrorCode`](src/main/java/com/joseortiz/inventory/error/ErrorCode.java). Each constant has a message template whose `{placeholders}` are filled from the detail's `info` map, so adding a rule is a one-line constant plus the validator that raises it. The `info` map always includes `row`, `field` and `cell` where they apply, which lets a UI highlight the offending cell.

## Architecture

```
controller   ProductController            REST surface, OpenAPI annotations
service      ProductImportService         parse -> validate rows -> validate uniqueness -> persist (one transaction)
             ProductQueryService          paging, sort allow-list, summary
             ProductCommandService        delete one / delete all
             ProductRowMapper             validated row -> entity
validation   ValidationRule<T>            contract: returns a list of ErrorDetail, never throws
             ProductRowValidator          per-row value rules
             DuplicateRuleValidator       SKU + date uniqueness (in file, then one DB query for the batch)
             ValueParsers                 shared lenient parsing (dates, currency text) so validator and mapper agree
excel        SpreadsheetParser            strategy interface, resolved by file extension
             XlsxSpreadsheetParser        Apache POI, formula evaluation, header check
error        ErrorCode / ErrorDetail / ApiErrorResponse / GlobalExceptionHandler
repository   ProductRepository            Spring Data JPA + aggregate query
domain       Product                      entity with unique constraint as last line of defence
config       InventoryProperties          typed view of application.yml, overridable by env vars
```

Design notes:

- **Validators are data, not exceptions.** Rules return details; the service aggregates them and throws once. This is what makes "report every problem at once" trivial.
- **Two validation concerns, two validators.** Value rules need only the row; uniqueness needs the batch and the database. Keeping them apart keeps each one unit-testable without Spring.
- **Parser is a strategy.** Adding CSV support is one `@Component` implementing `SpreadsheetParser`; the resolver and the `accepted-extensions` property already handle discovery.
- **Sorting through an allow-list.** Clients sort by API field names; the service maps them to entity properties and flips the direction for `stockAge`. Nothing user-controlled reaches the query builder.
- **Derived values are computed once, server-side**, with an injectable `Clock` so tests are deterministic.

## Configuration

Everything under `inventory.*` in [`application.yml`](src/main/resources/application.yml) can be overridden with environment variables:

| Property | Env var | Default |
|---|---|---|
| `inventory.cors.allowed-origins` | `INVENTORY_CORS_ALLOWED_ORIGINS` | `http://localhost:4200,...` |
| `inventory.import.max-file-size` | `INVENTORY_IMPORT_MAX_FILE_SIZE` | `5MB` |
| `inventory.import.max-rows` | `INVENTORY_IMPORT_MAX_ROWS` | `10000` |
| `inventory.import.accepted-extensions` | `INVENTORY_IMPORT_ACCEPTED_EXTENSIONS` | `xlsx` |
| `inventory.import.date-patterns` | `INVENTORY_IMPORT_DATE_PATTERNS` | `yyyy-MM-dd,MM/dd/yyyy,M/d/yyyy,dd-MMM-yyyy` |
| `inventory.query.max-page-size` | `INVENTORY_QUERY_MAX_PAGE_SIZE` | `200` |

H2 runs in memory (`jdbc:h2:mem:inventory`); data lives as long as the process. The H2 console is at `/h2-console` for poking around. Swapping to MySQL or PostgreSQL is a datasource change plus a driver dependency; the JPQL and the unique constraint are portable.

## Tests

```bash
mvn verify
```

| Layer | What is covered |
|---|---|
| `ValueParsersTest`, `ProductRowValidatorTest` | every value rule, date patterns, currency text, formula-error skipping (fixed clock) |
| `DuplicateRuleValidatorTest` | in-file duplicates point at the first row; DB collisions use a single query |
| `XlsxSpreadsheetParserTest` | native types, formula evaluation, `#DIV/0!` reporting, blank rows, header checks, row limit, garbage input |
| `ProductQueryServiceTest` | sort allow-list, `stockAge` inversion, page-size cap, summary maths |
| `ProductControllerTest` (`@WebMvcTest`) | HTTP status codes and the exact error envelope, including framework-raised errors |
| `ProductRepositoryTest` (`@DataJpaTest`) | unique constraint, key lookup, aggregate query |
| `ProductImportIntegrationTest` (`@SpringBootTest`) | imports the shipped sample, sorts, summarises, rejects the re-upload as duplicates, and proves a bad file writes nothing |

## Built with Claude Code

This repository was built in a pair-programming session with Claude Code, on purpose: the role this test is for expects AI-assisted development as a daily practice. How the split worked:

- **I decided the shape**: Java 17 + Spring Boot 4.1, H2, controller/service/repository, a validator layer separated from the duplicate rule, the error envelope format, atomic imports, Javadoc everywhere. Claude proposed a plan against those constraints and I approved it before any code was written.
- **Claude generated the bulk of the code and tests**, then ran the build, started the service, and smoke-tested every endpoint with `curl` before I looked at it. Two real bugs were caught that way, not by reading: a record component named `importSettings` silently failed to bind to the `inventory.import` YAML key (fixed with `@Name`), and a `@WebMvcTest` slice could not see `InventoryProperties` (fixed by registering it on the `WebConfig` that needs it).
- **Where I did not trust it blindly**: Spring Boot 4 moved test annotations to new packages and renamed starters, so instead of accepting Claude's memory we inspected the resolved jars to confirm `WebMvcTest` and `DataJpaTest` locations and the `spring.servlet.multipart.*` property keys. Version numbers were looked up on Maven Central rather than assumed.
- **What was reviewed by hand**: the validation rules, the error codes and their wording, the sort allow-list, and the transactional boundary of the import, because those are the parts a lender would actually depend on.

## License

MIT, see [LICENSE](LICENSE).
