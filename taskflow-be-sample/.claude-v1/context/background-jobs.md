# Background Jobs — TaskFlow

Two scheduled cleanup jobs. Implement with Spring `@EnableScheduling` + `@Scheduled`.

## Refresh Token Cleanup

- **Trigger:** Nightly (e.g., `@Scheduled(cron = "0 0 2 * * *")`)
- **What it does:** `DELETE FROM refresh_tokens WHERE expires_at < NOW()`
- **Table:** `refresh_tokens`
- **Why:** Expired tokens are unreachable; without cleanup the table grows unboundedly.

## Login Attempts Cleanup

- **Trigger:** Nightly
- **What it does:** `DELETE FROM login_attempts WHERE attempted_at < NOW() - INTERVAL '24 hours'`
- **Table:** `login_attempts`
- **Why:** Lock checks only look back 15 minutes; 24 hours of history is more than sufficient. Older rows are dead weight.

## Notes

- Both jobs are best-effort — failure does not affect request handling, just table size.
- No transactional requirements; simple bulk DELETE queries.
- Email/push notifications are **out of scope for v1** — do not implement a task overdue notifier.
- Do not add any jobs not listed here without updating this file.
