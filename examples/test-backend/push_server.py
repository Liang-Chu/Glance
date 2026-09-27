#!/usr/bin/env python3
"""Receive Glance subscriptions, or send a data-only message through FCM HTTP v1."""

import argparse
import hmac
import json
import os
import re
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

DEFAULT_STORE = Path(__file__).resolve().parents[2] / "local" / "push-subscriptions.json"


def load_store(path):
    if not path.exists():
        return {"subscriptions": {}, "revoked": []}
    return json.loads(path.read_text(encoding="utf-8"))


def save_store(path, stored):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(stored, indent=2), encoding="utf-8")
    temporary.replace(path)


def registration(request):
    fields = ("subscription_id", "installation_id", "firebase_project_id", "watcher")
    for field in fields:
        if not isinstance(request.get(field), str) or not request[field]:
            raise ValueError("missing or invalid " + field)
    validate_project(request["firebase_project_id"])
    for field in ("max_length", "title_max_length", "expires_after_seconds"):
        if type(request.get(field)) is not int or request[field] <= 0:
            raise ValueError("missing or invalid " + field)
    return {field: request[field] for field in fields +
            ("max_length", "title_max_length", "expires_after_seconds")}


def validate_project(project):
    if not isinstance(project, str) or not re.fullmatch(r"[a-z][a-z0-9-]{4,28}[a-z0-9]", project):
        raise ValueError("invalid Firebase project ID")


def make_handler(store_path, credential):
    lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            self.connection.settimeout(10)
            if self.path != "/glance":
                return self.reply(404)
            auth = self.headers.get("Authorization", "").encode("utf-8")
            if not hmac.compare_digest(auth, ("Bearer " + credential).encode("utf-8")):
                return self.reply(401)
            try:
                size = int(self.headers.get("Content-Length", "0"))
                if not 0 < size <= 65536:
                    return self.reply(413)
                request = json.loads(self.rfile.read(size))
                if not isinstance(request, dict):
                    return self.reply(400)
                operation = request.get("operation")
                key = request.get("subscription_id")
                if not isinstance(key, str) or not key:
                    return self.reply(400)
                with lock:
                    stored = load_store(store_path)
                    if operation == "register_push":
                        # A late registration cannot resurrect a deleted watcher.
                        if key in stored["revoked"]:
                            return self.reply(410)
                        stored["subscriptions"][key] = registration(request)
                    elif operation == "unregister_push":
                        stored["subscriptions"].pop(key, None)
                        if key not in stored["revoked"]:
                            stored["revoked"].append(key)
                    else:
                        return self.reply(400)
                    save_store(store_path, stored)
                self.reply(204)
            except (ValueError, UnicodeError):
                self.reply(400)
            except OSError:
                self.reply(500)

        def reply(self, status):
            self.send_response(status)
            self.send_header("Content-Length", "0")
            self.end_headers()

        def log_message(self, *_):
            # Neither device addresses nor backend credentials belong in logs.
            pass

    return Handler


def build_message(subscription, content, ttl):
    if type(ttl) is not int or not 0 <= ttl <= 2419200:
        raise ValueError("TTL must be seconds between 0 and 2419200")
    for field, limit in (("title", "title_max_length"), ("text", "max_length")):
        value = content.get(field)
        if not isinstance(value, str) or not value:
            raise ValueError(field + " must be a nonempty string")
        # Kotlin String.length measures UTF-16 code units, including surrogate pairs.
        if len(value.encode("utf-16-le")) // 2 > subscription[limit]:
            raise ValueError(field + " exceeds the watcher's limit")
    message = {
        "fid": subscription["installation_id"],
        "data": {"subscription_id": subscription["subscription_id"],
                 "title": content["title"], "text": content["text"]},
        "android": {"priority": "HIGH", "ttl": str(ttl) + "s"},
    }
    if len(json.dumps(message, ensure_ascii=False).encode("utf-8")) > 4096:
        raise ValueError("Message exceeds the FCM payload size limit")
    return {"message": message}


def send(project, payload):
    validate_project(project)
    # Only the sending command needs these packages or Google credentials.
    import google.auth
    from google.auth.transport.requests import AuthorizedSession

    credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/firebase.messaging"])
    with AuthorizedSession(credentials) as session:
        response = session.post(
            "https://fcm.googleapis.com/v1/projects/" + project + "/messages:send",
            json=payload, timeout=30,
        )
        if response.status_code != 200:
            raise RuntimeError("FCM rejected the message: HTTP " + str(response.status_code))
        print("Accepted by FCM. Confirm delivery on the phone and glasses.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--store", type=Path, default=DEFAULT_STORE)
    commands = parser.add_subparsers(dest="command", required=True)
    serve = commands.add_parser("serve")
    serve.add_argument("--host", default="127.0.0.1")
    serve.add_argument("--port", type=int, default=8081)
    sender = commands.add_parser("send")
    sender.add_argument("--project", help="Must match the target project received during registration")
    sender.add_argument("--subscription", required=True)
    sender.add_argument("--content", type=Path, default=Path(__file__).with_name("content.json"))
    sender.add_argument("--ttl", type=int, default=30, help="How long FCM may queue this message")
    sender.add_argument("--dry-run", action="store_true", help="Validate locally without contacting Google")
    args = parser.parse_args()
    if args.command == "serve":
        credential = os.environ.get("GLANCE_BACKEND_CREDENTIAL")
        if not credential:
            parser.error("set GLANCE_BACKEND_CREDENTIAL first")
        print("Registration endpoint: http://" + args.host + ":" + str(args.port) + "/glance", flush=True)
        ThreadingHTTPServer((args.host, args.port), make_handler(args.store, credential)).serve_forever()
    else:
        stored = load_store(args.store)
        if args.subscription not in stored["subscriptions"]:
            parser.error("subscription not found in the store")
        subscription = stored["subscriptions"][args.subscription]
        project = subscription.get("firebase_project_id")
        if not project:
            parser.error("save the watcher again to register its Firebase project")
        validate_project(project)
        if args.project and args.project != project:
            parser.error("--project does not match the registered Firebase project")
        payload = build_message(subscription,
                                json.loads(args.content.read_text(encoding="utf-8")), args.ttl)
        if args.dry_run:
            print("Valid data-only FCM message. Nothing sent.")
        else:
            send(project, payload)


if __name__ == "__main__":
    main()
