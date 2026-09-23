# Gateway security

The API gateway is an OAuth 2.0 resource server. It does not store passwords or mint tokens; an OpenID Connect provider authenticates users and issues signed JWTs. This keeps identity credentials outside the payment platform and permits production use of Cognito, Keycloak, or another standards-compliant provider.

Set `JWT_ISSUER_URI` to the token issuer and `JWT_JWK_SET_URI` to its public JWK Set endpoint. Both values are required at startup. The issuer is validated and signing keys are resolved from the configured JWK Set. Unsigned tokens, expired tokens, malformed tokens, or tokens from a different issuer are rejected before a request reaches a service.

The JWT must include a `roles` claim containing role names. The gateway maps each value to Spring Security's `ROLE_` authority.

| Route | Allowed roles |
| --- | --- |
| `GET /actuator/health`, `GET /actuator/info` | Public |
| Other actuator endpoints | `ADMIN` |
| `POST /api/payments/{id}/refund` | `MERCHANT`, `ADMIN` |
| `/api/admin/**`, `/api/fraud/**` | `ADMIN` |
| `/api/payments/**`, `/api/accounts/**`, `/api/notifications/**` | `CUSTOMER`, `MERCHANT`, `ADMIN` |

Authorization at the gateway is the first boundary. Downstream services must later enforce resource ownership—for example, a customer may only read their own payment or account—rather than relying solely on role checks.
