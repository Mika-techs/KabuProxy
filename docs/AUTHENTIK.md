# Authentik setup for kabuProxy

kabuProxy logs users in via **OpenID Connect** (Jakarta Security's built-in OIDC mechanism in TomEE).
Authentik is the identity provider. You need one *Provider*, one *Application* and one *Group*.

Examples below use `https://auth.example.com` for Authentik and `https://kabu.example.com` for kabuProxy — replace both.

## 1. Provider (Applications → Providers → Create → *OAuth2/OpenID Provider*)

| Field | Value | Why |
|---|---|---|
| Name | `kabuproxy` | |
| Authorization flow | **`default-provider-authorization-implicit-consent`** | no consent screen → re-login after an expired session is invisible |
| Client type | **Confidential** | kabuProxy keeps a client secret server-side |
| Client ID / Client Secret | copy both | → `KABU_OIDC_CLIENT_ID`, `KABU_OIDC_CLIENT_SECRET` |
| Redirect URIs | Strict: `https://kabu.example.com/callback` | must match `KABU_PUBLIC_URL` + `/callback` exactly |
| Signing Key | `authentik Self-signed Certificate` | RS256-signed tokens, verified via JWKS |
| *Advanced protocol settings* → Access token validity | `hours=8` | matches the 8 h app session |
| Refresh token validity | `days=30` | used for silent token refresh (`offline_access`) |
| Scopes | `openid`, `email`, `profile`, `offline_access` | `profile` carries the `groups` claim |
| Subject mode | *Based on the User's hashed ID* (default) | stable `sub` – kabuProxy keys users on it |
| Include claims in id_token | **on** | `groups` must be in the ID token |

For a local test, add `http://localhost:8080/callback` as a second redirect URI.

## 2. Application (Applications → Applications → Create)

| Field | Value |
|---|---|
| Name | `kabuProxy` |
| Slug | `kabuproxy` (becomes part of the issuer URL) |
| Provider | `kabuproxy` |
| Launch URL | `https://kabu.example.com/` |

Optional: *Policy / Group / User Bindings* → bind a group (e.g. `kabuproxy-users`) so only its members can open
the app at all. Without a binding every Authentik user can log in — they just land on the "waiting for activation"
page until an admin activates them (they can already link their own digikabu account under *Einstellungen*).

## 3. Admin group (Directory → Groups → Create)

Create a group named **`kabuproxy-admin`** (or whatever you set in `KABU_OIDC_ADMIN_GROUP`) and add yourself.
Members see the *Admin* tab, are activated automatically on first login, and can activate users and link digikabu accounts for others.
Group changes take effect on the next login (log out + in).

## 4. "Never log in again" – session length

kabuProxy's own session is 8 h (sliding). After that the browser is sent to Authentik, which answers **silently**
as long as the Authentik session is alive. Make that long:

Flows & Stages → Stages → `default-authentication-login` → **Session duration** `weeks=4`
(and enable *Stay signed in* offering if you like). Now you only type a password once a month.

## 5. kabuProxy environment

```dotenv
KABU_OIDC_PROVIDER_URI=https://auth.example.com/application/o/kabuproxy/
KABU_OIDC_CLIENT_ID=<client id>
KABU_OIDC_CLIENT_SECRET=<client secret>
KABU_PUBLIC_URL=https://kabu.example.com
KABU_OIDC_ADMIN_GROUP=kabuproxy-admin
KABU_DEV_AUTH=false
```

`KABU_OIDC_PROVIDER_URI` is the *OpenID Configuration Issuer* shown on the provider page; kabuProxy appends
`.well-known/openid-configuration` itself. Check it with:

```bash
curl -s https://auth.example.com/application/o/kabuproxy/.well-known/openid-configuration | jq .issuer
```

## 6. Reverse proxy

Terminate TLS in your proxy and forward to the container's port 8080. `KABU_PUBLIC_URL` makes the redirect URI
correct even though the container itself only sees plain http. Logout (`/logout`) ends the kabuProxy session and
redirects to Authentik's end-session endpoint (`…/application/o/kabuproxy/end-session/`).

## Troubleshooting

| Symptom | Cause |
|---|---|
| Authentik says *redirect URI mismatch* | redirect URI ≠ `KABU_PUBLIC_URL/callback` (http vs https, trailing slash) |
| Logged in, but no *Admin* tab | not in `kabuproxy-admin`, or `groups` missing from the ID token (step 1, *Include claims in id_token*) → log out + in |
| Endless redirect loop | clock skew between hosts, or `KABU_PUBLIC_URL` points to another host than the browser uses |
| 500 right after login | check the container log for `OpenIdAuthenticationMechanism` – usually a wrong client secret |
