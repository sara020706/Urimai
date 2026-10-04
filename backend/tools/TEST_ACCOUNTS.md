# Test accounts

Development accounts, one per role and verification state.

**Password for every account: `Test@1234`**

> These are known-password accounts and one of them is an administrator. They
> exist for local testing only. Before this app is used by real people, delete
> them: `ALLOW_TEST_SEED=true node tools/seed_test_users.js --remove`

| Username | Role | State | What it is for |
|---|---|---|---|
| `test_citizen` | USER | ACTIVE | Ordinary citizen. Browse schemes, ask questions, request lawyer contact. |
| `test_citizen2` | USER | ACTIVE | Second citizen. Confirms one user cannot see another's question. |
| `test_suspended` | USER | SUSPENDED | Sign-in must be refused with `ACCOUNT_SUSPENDED`. |
| `test_lawyer` | LAWYER | VERIFIED | Approved lawyer. Answers questions, appears in the directory. |
| `test_lawyer2` | LAWYER | VERIFIED | Second approved lawyer. Pair with `test_lawyer` for isolation checks. |
| `test_pending` | LAWYER | PENDING | Unapproved. Lawyer features refused with `LAWYER_NOT_VERIFIED`. |
| `test_admin` | ADMIN | ACTIVE | Review lawyer applications, manage users, moderate, read the audit log. |

## Managing them

```bash
cd backend

# Create or reset (also resets passwords back to Test@1234)
ALLOW_TEST_SEED=true node tools/seed_test_users.js

# Remove
ALLOW_TEST_SEED=true node tools/seed_test_users.js --remove
```

The `ALLOW_TEST_SEED` guard exists so the script cannot be run against a real
database by reflex.

## Verified behaviour

Checked against a running backend:

| Route | citizen | lawyer | pending | admin |
|---|---|---|---|---|
| `GET /schemes` | 200 | 200 | 200 | 200 |
| `GET /questions/mine` | 200 | 200 | 200 | 200 |
| `GET /lawyers` | 200 | 200 | 200 | 200 |
| `GET /lawyer/questions` | 403 | 200 | 403 | 403 |
| `GET /lawyer/me` | 403 | 200 | **200** | 403 |
| `GET /admin/users` | 403 | 403 | 403 | 200 |
| `GET /admin/lawyers` | 403 | 403 | 403 | 200 |

`GET /lawyer/me` is open to a PENDING lawyer on purpose: filling in their
details is how they become verified. Every capability route uses
`requireVerifiedLawyer` instead.

## Walkthroughs

**Answer isolation.** Sign in as `test_citizen`, ask a question. Sign in as
`test_lawyer`, answer it. Sign in as `test_lawyer2`, answer the same question —
you will not see the first lawyer's reply. Back as `test_citizen`, both replies
are visible. As `test_citizen2`, the question is a 404.

**Verification flow.** Sign in as `test_pending` and confirm lawyer features are
refused. Sign in as `test_admin`, open Administration → Lawyer applications,
approve `Adv. Priya Pending`, then sign back in as `test_pending` — the feed is
now available.

**Suspension.** As `test_admin`, suspend `test_citizen`. Their existing session
is rejected on the next request (`TOKEN_REVOKED`) and sign-in is refused
(`ACCOUNT_SUSPENDED`), because the status change bumps `token_valid_from`.

**Contact privacy.** As `test_citizen`, open `Adv. Meena Verified` in the
directory — no phone or email is shown. Request contact, then sign in as
`test_lawyer` and accept. The details now appear under the citizen's "My
requests", and nowhere else.

## Seeded lawyer details

Contact details are stored but never browsable; they are released only through
an accepted contact request.

| | `test_lawyer` | `test_lawyer2` | `test_pending` |
|---|---|---|---|
| Name | Adv. Meena Verified | Adv. Ravi Second | Adv. Priya Pending |
| Registration | TN/TEST/0001 | TN/TEST/0002 | TN/TEST/0003 |
| District | Chennai | Madurai | Coimbatore |
| Experience | 12 years | 7 years | 3 years |
| Areas | Welfare Law, Consumer | Family, Property | Employment |
| Languages | Tamil, English | Tamil | Tamil, English |
