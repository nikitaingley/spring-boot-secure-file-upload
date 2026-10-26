# Spring Boot Secure File Upload Gateway

[![Java Version](https://img.shields.io/badge/Java-24-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A production-grade, secure REST API for managing file uploads, built with Spring Boot. This project demonstrates a
multi-layered security approach to handle files safely, featuring deep content-type validation, active virus scanning,
temporary **secure access URLs**, and a pluggable architecture for multiple cloud storage providers (AWS S3 & Azure Blob
Storage).

## Core Features

- **Multi-Layered Security Pipeline:** Employs a fail-fast strategy to reject malicious files as early as possible.
- **Secure, Time-Limited Access:** Generates temporary URLs (**AWS Pre-Signed URLs** or **Azure SAS Tokens**) to provide
  short-lived, secure access to private files, preventing public exposure.
- **MIME Type Spoofing Prevention:** Uses **Apache Tika** to validate a file's true content, ignoring misleading file
  extensions.
- **Active Virus & Malware Scanning:** Integrates with **ClamAV** to scan every file for threats before it is persisted.
- **Pluggable Cloud Storage:** A clean adapter pattern allows switching between **AWS S3** and **Azure Blob Storage**
  with a single configuration change.
- **Data-at-Rest Encryption:** Leverages **Jasypt** to automatically encrypt sensitive file metadata (like filenames and
  storage keys) in the database.
- **Declarative Validation:** Showcases custom validation annotations (`@ValidFile`) for clean and reusable validation
  at the controller level.
- **Dockerized Environment:** Includes a `compose.yml` for easily spinning up dependencies (PostgreSQL and ClamAV).
- **API Documentation:** Integrated **Springdoc OpenAPI (Swagger)** for interactive API documentation.

## Security Deep Dive

This service is built with security as a primary concern, addressing several common vulnerabilities in file upload
systems.

### Layer 1: Configuration Hardening

The application enforces size limits at the framework level via `application.yml` to prevent Denial of Service (DoS)
attacks from excessively large files.

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
```

### Layer 2: Content-Type Validation (The Gatekeeper)

The `FileValidator` service uses **Apache Tika** to perform deep content inspection. It reads the file's "magic
numbers" (byte signature) to determine its true MIME type. This is the first line of defense and instantly rejects files
pretending to be something they're not (e.g., an executable renamed to `.png`).

### Layer 3: Virus Scanning (The Security Screener)

If the file format is valid, the `VirusScanService` streams the file to a ClamAV daemon. ClamAV, an open-source
antivirus engine, scans the content against its extensive, auto-updating virus database. If a threat is found, the file
is immediately rejected.

### Layer 4: Secure Access via Temporary URLs (The Gatekeeper for Downloads)

Files in the cloud storage are configured to be **private**. To allow a user to access a file, the API generates a *
*temporary, time-limited URL** (an AWS Pre-Signed URL or Azure SAS URL). This URL contains a cryptographic signature
that grants the bearer read-only access for a short, configurable duration. This is the industry-standard method for
securely serving private content without exposing permanent links or credentials.

### Layer 5: Data-at-Rest Encryption

Sensitive metadata stored in the PostgreSQL database, such as the original filename and the cloud storage key, is
automatically encrypted using a custom JPA `AttributeConverter` powered by **Jasypt**. This ensures that even if the
database is compromised, the file information remains confidential.

## Pluggable Storage Architecture

The application uses a `Storage` interface to abstract away the implementation details of the cloud provider.

- `AwsStorageAdapter` implements the logic for AWS S3 and generates Pre-Signed URLs.
- `AzureStorageAdapter` implements the logic for Azure Blob Storage and generates SAS URLs.

Spring's `@ConditionalOnProperty` annotation is used to activate the correct adapter based on a single property in
`application.yml`, making the system highly flexible.

```yaml
cloud:
  storage:
    provider: azure # or 'aws'
```

## Technologies Used

- **Backend:** Spring Boot, Spring Web, Spring Data JPA
- **Security:** Apache Tika, ClamAV, Jasypt, Secure Temporary URLs (AWS Pre-Signed URLs & Azure SAS Tokens)
- **Database:** PostgreSQL
- **Cloud:** Spring Cloud AWS (S3), Spring Cloud Azure (Blob Storage)
- **API Docs:** Springdoc OpenAPI 3 (Swagger UI)
- **DevOps:** Docker, Docker Compose
- **Build:** Maven

## Getting Started

### Prerequisites

- Java 24+
- Apache Maven 3.8+
- Docker and Docker Compose

### 1. Configuration

The application uses environment variables for sensitive credentials. Create a `.env` file in the project root directory
with the following content:

```bash
# .env

# PostgreSQL Credentials (matches compose.yml)
POSTGRES_USER=username
POSTGRES_PASSWORD=password
POSTGRES_DB=cloud-storage

# Jasypt Encryption Password
JASYPT_PASSWORD=YOUR_STRONG_ENCRYPTION_PASSWORD_HERE

# -- AWS S3 Credentials (if using AWS) --
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_S3_BUCKET_NAME=your_s3_bucket_name
AWS_S3_REGION=your_s3_bucket_region

# -- Azure Blob Storage Credentials (if using Azure) --
AZURE_STORAGE_CONNECTION_STRING="your_azure_storage_connection_string"
```

Then, update `src/main/resources/application.yml` with your desired cloud provider and URL duration.

```yaml
# src/main/resources/application.yml

cloud:
  storage:
    # Set the cloud provider ('aws' or 'azure')
    provider: azure
    # The duration in minutes for which the temporary access URLs are valid.
    access-url-duration-minutes: 30
```

### 2. Run the Application

1. **Start the dependencies (PostgreSQL & ClamAV):**
   ```bash
   docker-compose up -d
   ```
   *Note: The ClamAV container may take a few minutes on its first run to download the latest virus definitions.*

2. **Run the Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will be available at `http://localhost:8080`.

## API Usage

The easiest way to explore the API is via the interactive Swagger UI.

- **Swagger UI:** `http://localhost:8080/docs`

When you upload a file successfully, the API response will include a `url` field. This is the temporary, secure URL you
can use to access the file.

### Example cURL Commands

#### Upload a File

```bash
curl -X POST http://localhost:8080/api/v1/storage/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/document.pdf"
```

**Example Response:**

```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "name": "document.pdf",
  "type": "application/pdf",
  "url": "https://your-bucket.s3.your-region.amazonaws.com/files/uuid-document.pdf?X-Amz-Algorithm=...&X-Amz-Expires=1800&...",
  "size": 123456
}
```

#### Delete a File

```bash
# First, get the document ID from the upload response or by listing documents
DOCUMENT_ID="a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"

curl -X DELETE http://localhost:8080/api/v1/storage/delete/$DOCUMENT_ID
```

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.