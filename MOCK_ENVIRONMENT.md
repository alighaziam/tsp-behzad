# Offline Mock Environment

The backend test suite is designed to run without a live Oracle instance, tax authority account, identity provider, SMTP account, Redis, or Kafka. This is a test/development simulation only and must never be used as a production integration.

## Mocked boundaries

| Boundary | Test implementation | Verified behavior | Not verified |
|---|---|---|---|
| Database | H2 in Oracle compatibility mode, isolated in-memory schema | Repository/service behavior and supported JPA mappings | Oracle-specific SQL, actual schema compatibility, production migration |
| Identity provider | Spring Security test JWT support; placeholder issuer/JWKS in test profile | Authorization and request behavior with synthetic claims | Real discovery, JWKS rotation, signature validation against real keys |
| Taxpayer / Modian gateway | Mockito fakes for `ModianTransferService` and `PacketConverter` | Confirmed, rejected, pending, missing, and unavailable inquiry outcomes; duplicate-send protection | Live tax authority contract, network, signing keys, credentials |
| Tax SDK | Application-boundary fakes in unit tests | Request/response mapping and error handling | External SDK endpoint and real packet delivery |
| Email | Mockito `JavaMailSender` | Reset-link construction, recipient validation, captured email content, delivery-failure handling | Actual SMTP delivery or mailbox receipt |
| Redis / Kafka / gRPC | Redis and Kafka auto-configuration excluded; listeners disabled; gRPC test port disabled | Core backend tests without infrastructure services | Distributed cache, broker delivery, remote gRPC behavior |

## Test profile

The existing `application-test.yml` configures H2 with `MODE=Oracle`, disables Flyway for isolated test schema creation, uses a simple cache, and disables Redis/Kafka infrastructure connections. The test profile uses only placeholder values.

Run backend tests from the repository root:

```bash
mvn -pl trusted-taxpayers-rs -am test
```

Run the tax submission boundary tests:

```bash
mvn -pl trusted-taxpayers-rs -am -Dtest=ModianTransferAdapterTest test
```

## Expected scenarios

- Existing `CONFIRM` or `SYSTEM_CONFIRM` invoices are mapped to accepted without resubmission.
- Rejected and pending invoices are mapped correctly without resubmission.
- An unavailable inquiry fails closed; no duplicate packet is sent when the previous outcome is unknown.
- A packet is sent with its persisted UID only when inquiry reports no existing packet.
- Password reset messages are captured by a fake `JavaMailSender`; invalid recipients and non-HTTPS reset URLs are rejected.
- Production configuration validation accepts complete placeholder HTTPS configuration and temporary test key material, while rejecting missing properties, insecure URLs, invalid SMTP ports, empty or unreadable signing-key files, and malformed or wildcard webhook allow-lists.

## Production boundary

Mocks provide deterministic coverage of application behavior; they do not certify Oracle compatibility or live taxpayer/identity/SMTP connectivity. Keep the production validator enabled. Never commit real secrets, private keys, or production URLs. Live integrations remain **not tested** until approved non-production environments are available.
