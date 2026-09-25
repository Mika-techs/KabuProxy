## What this is

kabuProxy crawls digikabu.de (school timetable portal) per linked user into MySQL and serves a nicer JSF UI behind
Authentik OIDC. See README.md (features, env vars, crawl behaviour) and docs/AUTHENTIK.md.

## Commands

```bash
mvn -B clean package   # checkstyle (validate, fails build) + unit tests + war
mvn test               # unit tests only (no containers)
./scripts/dev.sh       # docker MySQL + tomee-embedded:run on :8080, reads .env
```

## Gotchas (learned the hard way)

- `tomee-embedded:run` exits when stdin closes; in a non-interactive shell run `tail -f /dev/null | ./scripts/dev.sh`.
  Never build into `target/` while it runs – stop it first.
- MicroProfile Config (SmallRye): an empty value / `defaultValue = ""` counts as *missing* → optional strings are
  `Optional<String>` in `KabuConfig`.
- EL 6 resolves properties on **records** only via accessor-style methods (`x()`), not `getX()`/`isX()` – view
  records in `web/model` use accessor-style helpers.
- Don't set `hibernate.jdbc.time_zone` – it shifts `LocalTime` columns; `Instant` is stored as UTC anyway.
- Public pages are stateless (`f:view transient`), admin.xhtml is stateful (`<ui:param name="stateful" value="true"/>`).
- Scripted JSF posts need `<formId>_SUBMIT=1`, not `formId=formId`.
- Auth: `KabuAuthenticationMechanismHandler` (Security 4 handler) picks `DevAuthenticationMechanism`
  (`KABU_DEV_AUTH=true`) or the OIDC mechanism from `OidcAuthenticationDefinition`. Caller name = OIDC `sub`.

- MIK's IntelliJ is a Flatpak: no docker CLI, no `flatpak-spawn --host`. IDE tooling must use Maven or IntelliJ's
  Docker plugin (socket `unix:///run/user/1000/docker.sock`, rootless) – see `.run/`. Keep the Dockerfile free of
  BuildKit-only syntax (`COPY --chmod`, `RUN --mount`), the plugin may use the legacy builder.
- i18n: JSF picks de/en from `Accept-Language` (faces-config); texts in `i18n/messages.properties` (German root) +
  `messages_en.properties`, views `#{msg['key']}`, with args `#{i18n.format(...)}`, Java `I18n.text(key, args)`.
  Keep the empty `messages_de.properties` – without it a `de` lookup falls back to the JVM locale (en) in JSF.
- `DotEnvConfigSource` reads `./.env` (ordinal 250 < env vars 300) so IDE runs need no env vars.

## Code style

taa's checkstyle: Allman braces everywhere (also lambdas/records/switch), `}` alone on its line, braces on every
control statement, private ctor for utility classes, `@Inject private Logger logger;` (never Lombok loggers),
imports: others, blank line, `jakarta.*`/`java.*`. Package root `de.mik.kabuproxy`.

## Privacy

Test fixtures are anonymized digikabu pages – never commit raw captures (names, absence reasons). Passwords only
ever exist encrypted (`CredentialCipher`, AAD = digikabu username) and must never be logged. Admins must not see
other users' absences.
