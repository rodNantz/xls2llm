# xls2llm Copilot Instructions

A Spring Boot application that reads Excel files and processes prompts against LLM services (OpenAI). Supports batch processing, categorization, and Excel output generation.

## Build, Test, and Run

**Build the project:**
```bash
./mvnw clean package
```

**Run all tests:**
```bash
./mvnw test
```

**Run a specific test:**
```bash
./mvnw test -Dtest=DocumentManagerTest
# Or specific test method:
./mvnw test -Dtest=DocumentManagerTest#testGetDocumentWithLimit
```

**Run the Spring Boot application:**
```bash
./mvnw spring-boot:run
```
The web portal is accessible at `http://localhost:8080/` by default.

**Skip tests during build:**
```bash
./mvnw clean package -DskipTests
```

## Architecture Overview

### Core Flow
1. **Document Manager** (`document.DocumentManager`) - Handles Excel file I/O
   - `DocumentMgrXlsImpl` - Implementation using Apache POI (v5.3.0)
   - Reads structured questions from Excel, writes categorization results back
   - Supports batch processing with configurable row limits and batch sizes

2. **LLM Service** (`llmapi.LLMService`) - Abstract LLM interaction
   - `OpenAIService` - Production OpenAI API integration using openai-java (v3.0.3)
   - `LocalTestService` - Test/local mock service for development
   - Supports two operation modes:
     - `promptCompletion()` - Direct chat completions
     - `promptCategorization()` - Structured categorization with custom categories/codes

3. **Data Models** (`model.*`)
   - `Question` - Represents Excel row with system/user questions
   - `Request2LLM` - Batch of questions to send to LLM
   - `CategorizationResponse` - LLM output with classifications
   - `Answer` - Individual LLM response with metadata
   - `Code` - Custom code validation with serializer for JSON (uses custom `CodeSerializer`)
   - Classification sub-types: `CategoryCol`, `CommentRow` - Result structure

4. **Web Portal** (`portal.fileupload.FileUploadController`)
   - Single-page form for file upload at `/`
   - Handles multipart file upload (max 10MB by default)
   - Progress tracking via `ProgressBar` model
   - Downloads processed Excel files as attachment

5. **Service Injection** (`factory.ServiceFactory`)
   - Central factory for retrieving `DocumentManager` and `LLMService` instances
   - Uses Spring `@Qualifier` to select between test/production LLM implementations

### Supporting Patterns
- **AOP Logger** (`aop.Logger`) - Custom logging wrapper used by controllers
- **Constants** (`constants.ExcelConstants`) - Excel structure configuration
- **CommandLineRunner** - Disabled CLI mode (profile excludes it with `!test`)

## Key Conventions

### Service Implementation Strategy
Services use the **Strategy Pattern** with interfaces:
- Always autowire the interface (`LLMService`, `DocumentManager`), not concrete implementations
- Use `@Qualifier` to select between multiple implementations:
  ```java
  @Autowired
  @Qualifier("test-service")  // or "gpt-service"
  private LLMService llmService;
  ```
- This allows swapping implementations (e.g., OpenAI ↔ local test) without code changes

### Configuration Management
Properties are in `src/main/resources/application.properties`:
- LLM endpoints, API keys, org/project IDs (separate sections for prod/test)
- Custom validation categories: `custom.valid.categories` (e.g., "V1,V1.1,V2")
- Custom validation codes: `custom.valid.codes` (e.g., "00,01,02")
- Spring profiles control which service is active (test vs. production)

### Excel File Handling
- Files are read from `src/main/resources/xls/` directory
- Row zero contains system/user questions (parsed by `Question` model)
- Subsequent rows are questions to categorize
- `DocumentMgrXlsImpl` uses Apache POI workbook API directly
- Write operations create new files via `Input2xls` container

### Testing Approach
- Uses **JUnit 5** (Jupiter) with `@Test` and `@SpringBootTest` for integration tests
- Test files in `src/test/java/es/rodrigonant/p2ai/xls2llm/`
- Tests categorized by layer: `document/`, `llmapi/`, `model/`, `portal/`, `integration/` (E2ETest)
- Mock/test implementations use `LocalTestService` which doesn't call external APIs
- `GenericTest` appears to be a base for shared test utilities

### Java Version & Project Config
- **Java 23** - uses modern features, compiler configured in pom.xml
- **Spring Boot 3.4.2** with parent pom
- Maven wrapper (`./mvnw`) included for reproducible builds
- No external linters/formatters configured - follow standard Spring Boot conventions

### Batch Processing Pattern
`DocumentManager.getDocument()` has overloads:
```java
// Single batch
Request2LLM getDocument(String xlsFile, Integer rowLimit);

// Multiple batches (each batch is a separate request)
List<Request2LLM> getDocument(String xlsFile, Integer rowLimit, Integer batchSize);

// With custom start line
List<Request2LLM> getDocument(String xlsFile, Integer startLine, Integer rowLimit, Integer batchSize);
```
This enables processing large Excel files by splitting into manageable LLM requests.

## Notes

- **Secrets in properties file:** API keys are hardcoded in `application.properties` - move to environment variables before production use
- **Response serialization:** Some models (e.g., `Code`) use custom JSON serializers; check `model/` for `*Serializer` classes when adding new response types
- **Spring Profiles:** The test profile is used to disable `CommandLineRunnerV1`; keep this pattern when adding new CLI runners
