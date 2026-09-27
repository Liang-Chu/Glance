# Example backend

The smallest thing that satisfies [`../../docs/CONTRACT.md`](../../docs/CONTRACT.md). It returns
`content.json`, verbatim, and does nothing else. Python 3.8+, no dependencies.

```
python server.py
python server.py --port 9000
python server.py --host 0.0.0.0    # allow access from a trusted LAN
```

Set `GLANCE_BACKEND_CREDENTIAL` to require authentication. Credentials are never printed.

It prints the contract fields Glance sends and whether what it is about to return will be accepted or
refused, which is usually enough to find the problem without touching the phone.

Edit `content.json` between checks and the next one picks it up — no restart.

## Testing the rules by editing one file

The file is sent verbatim, so it exercises the contract including the parts that must fail:

| `content.json` | Glance should |
| --- | --- |
| `{"title": "Kotlin", "text": "..."}` | show it |
| *(empty file)* | show nothing, and not complain — that is `204` |
| `{"text": "..."}` | refuse: no title |
| `{"title": "T", "text": ""}` | refuse: empty text |
| text longer than the max length | refuse, **not** truncate |
| `hello` | refuse: not JSON |
| *(delete the file)* | complain once, then stay quiet until it is back |

A failure notice appears **once**, then Glance goes quiet until a check succeeds. That is deliberate.
Put the file back and wait for the next check to see it recover.

## Reaching it from the phone

Use `--host 0.0.0.0` on a trusted LAN and enter `http://YOUR_PC_LAN_IP:8080/glance` on the
phone. The default bind is loopback. The address changes if the computer changes network.
These are development examples using Python's HTTP server; deploy behind HTTPS with proper
application hosting and account authorization for internet use.

## Push example

`push_server.py` accepts registrations and removals at `/glance`, then provides a separate command
for your backend to send content through FCM. It does not poll content or run a timer. Complete
[Firebase setup](../../docs/PROCESS.md#firebase-setup-for-push) first. Run commands below from the
repository root in PowerShell.

Start the registration endpoint, using a credential of your own:

```powershell
$env:GLANCE_BACKEND_CREDENTIAL = 'your-test-backend-credential'
python examples/test-backend/push_server.py serve --host 0.0.0.0
```

On the phone, choose PUSH, enter `http://YOUR_PC_LAN_IP:8081/glance` and the same credential, and
save. Use a trusted LAN for this HTTP example; use HTTPS and proper server hosting outside it.
The server defaults to loopback when `--host` is omitted. It never prints device addresses or
credentials, and writes the registrations under ignored `local/push-subscriptions.json`.

In a second terminal, inspect that file for the `subscription_id`. Edit `content.json` with a title
and text within the registered limits, then validate without contacting Firebase:

```powershell
python examples/test-backend/push_server.py send --project YOUR_PROJECT_ID --subscription YOUR_SUBSCRIPTION_ID --dry-run
```

For a real send, install Google's authentication library and configure server credentials:

```powershell
python -m pip install 'google-auth[requests]>=2,<3'
$env:GOOGLE_APPLICATION_CREDENTIALS = (Resolve-Path local/service-account.json).Path
python examples/test-backend/push_server.py send --project YOUR_PROJECT_ID --subscription YOUR_SUBSCRIPTION_ID
```

The sender uses the target `firebase_project_id` received during registration; `--project` is
optional and must match it when supplied. Register again after upgrading from the earlier example
without this field. The sending service account may belong to a different project if the user
authorizes it in the target project. Never put that private key on the phone.

The sender uses FCM HTTP v1 with `fid`, the Firebase installation ID provided by Glance. It includes
only `data`, so Glance handles notification expiry in the background too. Default queue TTL is
30 seconds; set `--ttl` to the freshness window for your content. That TTL is separate from the
watcher's display duration. An FCM success response means accepted, not observed on the glasses.

The backend updates existing subscriptions on repeated registration. Removal is idempotent and
tombstones retired IDs to reject late registrations. Failed removals and uninstalled apps may leave
stale records; a production backend should prune these and stop sending to invalid destinations.
The local JSON store and Python HTTP server are examples, not a multi-process production service.

Local integration checks require neither Firebase credentials nor extra Python packages:

```powershell
python -m unittest discover -s examples/test-backend -p 'test_*.py' -v
```

These exercise registration, address updates, authentication, removal, late requests and sender
validation against a real loopback endpoint. They do not exercise Google's delivery network.
