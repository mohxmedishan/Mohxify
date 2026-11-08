# Rewrite Mode backend contract

Rewrite Mode deliberately sends text only after the user taps **Rewrite text**. QuickExpand does not call this service and continues to run locally.

## Endpoint

Configure an HTTPS endpoint in Rewrite Mode. The Android client sends:

```http
POST /rewrite
Content-Type: application/json
Accept: application/json
```

```json
{
  "text": "The complete sentence or paragraph supplied by the user.",
  "instruction": "Improve grammar, clarity, and flow while preserving meaning."
}
```

Return a successful JSON response:

```json
{
  "text": "The rewritten sentence or paragraph."
}
```

The app rejects empty output, limits input to 20,000 characters, and uses connection/read timeouts. Non-2xx responses are surfaced as errors. No model provider is bundled or configured by default.

## Security requirements for a production backend

- Keep provider API keys and model credentials exclusively in server-side environment secrets.
- Authenticate and rate-limit requests; do not expose an unrestricted proxy to a paid model.
- Use HTTPS, validate request size, and return only the rewritten text.
- Do not log submitted text by default; define retention and deletion behavior clearly.
- Restrict CORS if a browser client is also supported. CORS is not a substitute for authentication.
- Consider short-lived user/session tokens or platform attestation for abuse prevention. Never ship a provider API key in the APK.

The endpoint setting is local to the device. Until a backend URL is configured, Rewrite Mode reports that AI is unavailable instead of pretending to rewrite text.
