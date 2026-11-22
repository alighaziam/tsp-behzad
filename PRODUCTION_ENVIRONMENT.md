# Production Environment Configuration

The production profile intentionally requires deployment-specific values. Do not commit credentials, private keys, or real taxpayer signing material.

## Required environment variables

| Variable | Purpose |
|---|---|
| `APP_DB_URL` | Oracle JDBC URL |
| `APP_DB_USER` / `APP_DB_PASSWORD` | Oracle application account |
| `OAUTH2_CLIENT_SECRET` | OAuth2 client secret |
| `APP_JWT_SECRET` | Base64-encoded application JWT signing secret (at least 256 bits) |
| `OAUTH2_ISSUER_URI` | Identity provider issuer |
| `OAUTH2_JWK_SET_URI` | Identity provider signing-key endpoint |
| `TAXPAYER_SYSTEM_URL` | Taxpayer system API base URL |
| `TAX_CLIENT_ID` | Taxpayer-system client identifier |
| `TAX_ORG_KEY_ID` | Organization public-key identifier |
| `TAX_ORG_PUBLIC_KEY` | Organization public key (PEM/base64 value expected by the integration) |
| `TAX_SDK_BASE_URL` | Production tax SDK endpoint |
| `TAX_SDK_MEMORY_ID` | Production fiscal memory identifier |
| `TAX_SDK_CLIENT_ID` | Tax SDK client identifier |
| `TAX_SDK_PRIVATE_KEY_PATH` | Absolute path to the mounted private key |
| `TAX_SDK_PRIVATE_KEY_PASSWORD` | Optional private-key password |
| `MAIL_HOST` | Production SMTP host |
| `MAIL_PORT` | SMTP port (defaults to 587) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials |
| `APP_PUBLIC_BASE_URL` | Public HTTPS frontend base URL used to build reset links |

## Deployment checklist

1. Inject all required values through the deployment secret manager; never place them in source control or container images.
2. Mount the private signing key read-only outside the repository and set `TAX_SDK_PRIVATE_KEY_PATH` to that mounted path.
3. Rotate any signing key that was ever committed to Git. Deleting a key from the current branch does not remove it from Git history.
4. Confirm the configured taxpayer-system and SDK URLs are the approved production endpoints before enabling outbound traffic.
5. Start with the production profile and verify Flyway migration validation, Hibernate schema validation, database connectivity, OAuth2 discovery/JWKS, and signing-key readability.
6. Restrict actuator endpoints and network access at the deployment layer; expose only the health endpoints required by the platform.

No production credentials or endpoints are supplied by this repository. They must be provided by the system owner/operator.

## Password reset delivery

The production profile sends reset links through the configured SMTP provider when the identifier is an email address. The public forgot-password response never discloses account existence or returns the reset token. Reset links are single-use and expire after 30 minutes. Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and an HTTPS `APP_PUBLIC_BASE_URL`; the application validates these settings before starting. Username-only reset requests do not have a delivery destination and therefore require the user to submit the registered email address. Never re-enable token disclosure in the API response as a workaround.
