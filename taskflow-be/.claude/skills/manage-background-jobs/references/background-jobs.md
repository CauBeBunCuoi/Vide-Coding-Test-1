# Background Jobs

<!-- Describe every background job: what it does, when it runs, what it reads/writes. -->

## Overdue Task Notifier
- **Trigger:** Daily at 08:00 UTC (cron)
- **What it does:** Finds all tasks past due date with status != done, sends email notification to assignee
- **Reads:** tasks table (due_date, status, assignee_id)
- **Writes:** notifications table

## Session Cleanup
- **Trigger:** Every 6 hours (cron)
- **What it does:** Deletes expired refresh tokens from the database
- **Reads/Writes:** refresh_tokens table
