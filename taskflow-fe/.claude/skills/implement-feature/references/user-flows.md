# User Flows

<!-- Describe the key user journeys through the application.
     Focus on what the user does step by step, not how the code works. -->

## Authentication Flow
1. User lands on Login Screen
2. Enters credentials → POST /auth/login
3. On success: token stored, redirect to Home Page
4. On failure: error message shown inline

## Create Task Flow
1. User clicks quick-create button on Home Page
2. Modal opens with title, assignee, due date fields
3. Submit → POST /tasks
4. Task appears in the relevant dashboard card immediately
