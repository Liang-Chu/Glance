#!/usr/bin/env python3
"""
A test backend for Glance. It returns content.json, verbatim. That is all it does.

    python server.py                  # localhost, port 8080
    python server.py --host 0.0.0.0   # trusted LAN access
    python server.py --port 9000

Set GLANCE_BACKEND_CREDENTIAL to require a bearer credential.
It prints Glance's contract fields, and says whether what it is about to return
will be accepted or refused, so the limits are visible rather than guessed.

Edit content.json and the next check picks it up — no restart. An empty file
means 204, which tells Glance there is nothing to say right now.

Because the file is sent verbatim, you can test every rule in
../../docs/CONTRACT.md just by editing it:

    {"title": "Kotlin", "text": "..."}   normal
    (empty file)                         204 — Glance stays silent
    {"text": "..."}                      no title — Glance must refuse
    {"title": "T", "text": ""}           empty text — Glance must refuse
    text longer than max_length          Glance must refuse, not truncate
    not json at all                      Glance must refuse
"""

import argparse
import hmac
import json
import os
import time
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

CONTENT = Path(__file__).with_name("content.json")

# When the app last called. A gap of roughly the interval means Glance woke
# itself; a gap of seconds means somebody pressed a button. Without this the two
# are indistinguishable in the log, which is the one question worth asking.
LAST_APP_CALL = [None]


def say(line):
    """Unbuffered: the log must be visible when stdout is a file, not a terminal."""
    sys.stdout.write(line + "\n")
    sys.stdout.flush()


class Handler(BaseHTTPRequestHandler):

    def do_POST(self):
        self.connection.settimeout(10)
        if self.path != "/glance":
            return self.reply(404, None)
        try:
            size = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            return self.reply(400, None)
        if not 0 <= size <= 65536:
            return self.reply(413, None)
        body = self.rfile.read(size) if size else b""
        auth = self.headers.get("Authorization")

        agent = self.headers.get("User-Agent") or ""
        now = time.time()
        gap = ""
        if agent.startswith("Glance/"):
            if LAST_APP_CALL[0] is not None:
                seconds = now - LAST_APP_CALL[0]
                gap = "   (+%dm %02ds since the app last called)" % (seconds // 60, seconds % 60)
            LAST_APP_CALL[0] = now

        say("")
        say("[%s] -> POST %s%s" % (time.strftime("%H:%M:%S"), self.path, gap))
        say("   %-18s: %s" % ("User-Agent", self.headers.get("User-Agent") or "(absent)"))
        say("   %-18s: %s" % ("Authorization", "(present, redacted)" if auth else "(absent)"))
        asked = self.report_request(body)

        if ARGS.token and not hmac.compare_digest((auth or "").encode(), ("Bearer " + ARGS.token).encode()):
            say("<- 401  invalid credential")
            return self.reply(401, None)

        if not CONTENT.exists():
            say("<- 500  %s does not exist" % CONTENT.name)
            return self.reply(500, b"no content.json")

        payload = CONTENT.read_bytes().strip()
        if not payload:
            say("<- 204  %s is empty, so: nothing to say" % CONTENT.name)
            return self.reply(204, None)

        say("<- 200  %s" % payload.decode("utf-8", "replace"))
        self.report_fit(payload, asked)
        self.reply(200, payload)

    def report_request(self, body):
        """Print the contract fields, so the limits are visible."""
        if not body:
            say("   %-18s: (empty)" % "body")
            return {}
        try:
            asked = json.loads(body.decode("utf-8"))
        except ValueError:
            say("   body: not JSON (not logged)")
            return {}
        if not isinstance(asked, dict):
            return {}
        for key in ("watcher", "max_length", "title_max_length", "expires_after_seconds", "interval_minutes", "client"):
            if key in asked:
                say("   %-18s: %s" % (key, asked[key]))
        return asked

    def report_fit(self, payload, asked):
        """Say whether what is being returned will be accepted or refused."""
        try:
            sending = json.loads(payload.decode("utf-8"))
        except ValueError:
            say("   !! not JSON, so Glance will REFUSE it")
            return

        if not isinstance(sending, dict):
            say("   !! not a JSON object, so Glance will REFUSE it")
            return
        for field, limit_key in (("title", "title_max_length"), ("text", "max_length")):
            value = sending.get(field)
            limit = asked.get(limit_key)
            length = len(value.encode("utf-16-le", "surrogatepass")) // 2 if isinstance(value, str) else 0
            if not isinstance(value, str):
                say("   !! %s is not a string, so Glance will REFUSE it" % field)
            elif value == "":
                say("   !! %s is empty, so Glance will REFUSE it" % field)
            elif type(limit) is not int:
                say("   %-5s %d chars  (no %s was sent, cannot check)"
                    % (field, length, limit_key))
            elif length > limit:
                say("   !! %s is %d chars, limit %d -> Glance will REFUSE it"
                    % (field, length, limit))
            else:
                say("   %-5s %d of %d chars  OK" % (field, length, limit))

    def reply(self, status, body):
        self.send_response(status)
        if body is None:
            self.send_header("Content-Length", "0")
            self.end_headers()
            return
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass  # say() above is the log



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--token", default=os.environ.get("GLANCE_BACKEND_CREDENTIAL"), help="bearer credential (prefer GLANCE_BACKEND_CREDENTIAL)")
    parser.add_argument("--host", default="127.0.0.1", help="use 0.0.0.0 only on a trusted LAN")
    ARGS = parser.parse_args()

    say("Serving %s" % CONTENT)
    say("")
    say("Listening on http://%s:%d/glance" % (ARGS.host, ARGS.port))
    if ARGS.token:
        say("Bearer credential required (not displayed).")
    say("")
    say("For a phone on your LAN, use --host 0.0.0.0 and enter this computer's LAN IP in Glance.")
    say("Development server only. Ctrl+C to stop.")

    ThreadingHTTPServer((ARGS.host, ARGS.port), Handler).serve_forever()
