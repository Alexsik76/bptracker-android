<!-- MAINTENANCE: atomic additive edit. Do not rewrite surrounding code. English-only. -->

# Backend task 2b-be — accept the native Android passkey origin

Repo: **bptracker-backend**. Single small change so Fido2 accepts assertions coming from the native Android app.

## Why
A WebAuthn assertion from an Android app (via Credential Manager) carries the origin `android:apk-key-hash:<hash>`, not the web origin `https://bptracker.home.vn.ua`. Fido2NetLib validates the assertion's origin against `Fido2Configuration.Origins`. Today `Origins` is derived only from `CORS_ORIGINS`, so the native origin is rejected. We add it via a dedicated env var (kept separate from CORS, which is for browsers).

## Edit — `Program.cs`, the `Fido2Configuration` registration (currently ~line 193–200)
Replace the `Origins = ...` initializer so it unions the CORS origins with an optional `FIDO2_ANDROID_ORIGINS` list. Do not change `ServerDomain` or `ServerName`.

Current:
```csharp
    Origins = (builder.Configuration["CORS_ORIGINS"] ?? "https://bptracker.home.vn.ua")
        .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
        .ToHashSet()
```
New:
```csharp
    Origins = (builder.Configuration["CORS_ORIGINS"] ?? "https://bptracker.home.vn.ua")
        .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
        .Concat((builder.Configuration["FIDO2_ANDROID_ORIGINS"] ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        .ToHashSet()
```

## Edit — `.env.example`
Append a documented entry:
```
# Comma-separated extra origins accepted for native (Android) passkey assertions,
# e.g. android:apk-key-hash:<base64url-sha256-of-signing-cert>
FIDO2_ANDROID_ORIGINS=
```

## Done when
- `dotnet build` clean.
- Existing tests pass.
- When `FIDO2_ANDROID_ORIGINS` is set, that origin is included in the Fido2 `Origins` set (web/CORS behavior unchanged when it is empty).

## Out of scope
- No change to CORS policy, endpoints, or the native login handlers themselves.

---

## MANUAL steps for the user (not the agent)
After the agent's change is merged and the image rebuilt, set on the server `bptracker-backend/.env`:
```
FIDO2_ANDROID_ORIGINS=android:apk-key-hash:yJpN7WGuMsRNBaLAWWyMYfYXIQzPMm7b75i-9JQRzfs
```
Then redeploy (`docker compose up --build -d`). This value is the app's signing-cert SHA-256 (the same key behind assetlinks.json), base64url-encoded — not secret. Native passkey login will keep failing until this is deployed.
