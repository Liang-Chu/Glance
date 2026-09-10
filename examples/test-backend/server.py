#!/usr/bin/env python3
"""
A test backend for Glance. It returns content.json, verbatim. That is all it does.

    python server.py                  # port 8080
    python server.py --port 9000
    python server.py --token hunter2  # require Authorization: Bearer hunter2

It prints every field Glance sends, and says whether what it is about to return
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
import json
import time
import socket
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
        size = int(self.headers.get("Content-Length") or 0)
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
        say("   %-18s: %s" % ("Authorization", auth or "(absent)"))
        asked = self.report_request(body)

        if ARGS.token and auth != "Bearer " + ARGS.token:
            say("<- 401  expected 'Bearer %s'" % ARGS.token)
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
        """Print every field Glance sent, so the limits are visible."""
        if not body:
            say("   %-18s: (empty)" % "body")
            return {}
        try:
            asked = json.loads(body.decode("utf-8"))
        except ValueError:
            say("   %-18s: %s   (not JSON)" % ("body", body.decode("utf-8", "replace")))
            return {}
        for key in sorted(asked):
            say("   %-18s: %s" % (key, asked[key]))
        return asked

    def report_fit(self, payload, asked):
        """Say whether what is being returned will be accepted or refused."""
        try:
            sending = json.loads(payload.decode("utf-8"))
        except ValueError:
            say("   !! not JSON, so Glance will REFUSE it")
            return

        for field, limit_key in (("title", "title_max_length"), ("text", "max_length")):
            value = sending.get(field)
            limit = asked.get(limit_key)
            if value is None:
                say("   !! no %s, so Glance will REFUSE it" % field)
            elif value == "":
                say("   !! %s is empty, so Glance will REFUSE it" % field)
            elif limit is None:
                say("   %-5s %d chars  (no %s was sent, cannot check)"
                    % (field, len(value), limit_key))
            elif len(value) > limit:
                say("   !! %s is %d chars, limit %d -> Glance will REFUSE it"
                    % (field, len(value), limit))
            else:
                say("   %-5s %d of %d chars  OK" % (field, len(value), limit))

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


def lan_addresses():
    """
    Every address this machine answers on, best guess first.

    One address is not enough: a laptop that moves between wifi, a hotspot and a
    VPN gets a different one each time, and the app goes on pointing at the old
    one. Printing them all makes a stale URL obvious instead of silent.
    """
    found = []
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        found.append(probe.getsockname()[0])
    except OSError:
        pass
    finally:
        probe.close()

    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            address = info[4][0]
            if address not in found and not address.startswith("169.254."):
                found.append(address)
    except OSError:
        pass

    return found or ["127.0.0.1"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--token", default=None, help="require this bearer credential")
    ARGS = parser.parse_args()

    say("Serving %s" % CONTENT)
    say("")
    say("Backend URL for Glance — try the first, fall back to the others:")
    for address in lan_addresses():
        say("    http://%s:%d/glance" % (address, ARGS.port))
    if ARGS.token:
        say("Credential for Glance:   %s" % ARGS.token)
    say("")
    say("These change when you change network. If Glance stops reaching this,")
    say("check the address here before assuming anything else broke.")
    say("Phone must be on the same network. Ctrl+C to stop.")

    ThreadingHTTPServer(("0.0.0.0", ARGS.port), Handler).serve_forever()
