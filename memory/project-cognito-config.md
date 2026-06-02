---
name: project-cognito-config
description: AWS Cognito app client configuration values for ReporteCiudadanoAdmin (extracted from Cognito console sample, 2026-06-01)
metadata:
  type: project
---

Cognito User Pool and app client values confirmed from the AWS console.

**Why:** Needed to wire up backend JWT validation and frontend PKCE login redirect.
**How to apply:** Use these when setting env vars for local dev, ECS task definition, and `window.__ENV__` injection in the CloudFront-served HTML.

| Env var | Value |
|---|---|
| `COGNITO_USER_POOL_ID` | `us-east-1_ljOp2MCne` |
| `COGNITO_CLIENT_ID` | `20c8c0mmq4ijq992bb6pennbsk` |
| `COGNITO_DOMAIN` | `us-east-1ljop2mcne.auth.us-east-1.amazoncognito.com` |

**Other values:**
- CloudFront frontend URL: `https://d84l1y8p4kdic.cloudfront.net`
- Configured callback (redirect_uri): `https://d84l1y8p4kdic.cloudfront.net`
- Scopes configured in app client: `phone openid email`

**Scope mismatch to fix:** `LoginScreen.kt` currently sends `scope=openid+profile+email` — must be changed to `openid+email` (or `phone+openid+email`) to match what the app client allows.

**Still pending:**
- Set a logout URI in the app client settings
