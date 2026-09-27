import json
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from push_server import build_message, load_store, make_handler


class PushServerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.store = Path(self.temp.name) / "subscriptions.json"
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(self.store, "secret"))
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.request = {"operation": "register_push", "subscription_id": "watcher-a",
                        "installation_id": "address-1", "firebase_project_id": "sample-project", "watcher": "Tips", "max_length": 80,
                        "title_max_length": 32, "expires_after_seconds": 3}

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def post(self, body, credential="secret"):
        request = Request("http://127.0.0.1:" + str(self.server.server_port) + "/glance",
                          json.dumps(body).encode(), {"Authorization": "Bearer " + credential,
                                                     "Content-Type": "application/json"})
        try:
            with urlopen(request, timeout=2) as response:
                return response.status
        except HTTPError as response:
            with response:
                return response.code

    def test_registration_refresh_revocation_and_late_request(self):
        self.assertEqual(204, self.post(self.request))
        self.request["installation_id"] = "address-2"
        self.assertEqual(204, self.post(self.request))
        subscription = load_store(self.store)["subscriptions"]["watcher-a"]
        self.assertEqual("address-2", subscription["installation_id"])
        payload = build_message(subscription, {"title": "T", "text": "hello"}, 30)
        self.assertEqual("address-2", payload["message"]["fid"])
        self.assertNotIn("notification", payload["message"])
        self.assertEqual("HIGH", payload["message"]["android"]["priority"])
        retired = {"operation": "unregister_push", "subscription_id": "watcher-a"}
        self.assertEqual(204, self.post(retired))
        self.assertEqual(204, self.post(retired))
        self.assertEqual(410, self.post(self.request))
        self.assertEqual({}, load_store(self.store)["subscriptions"])

    def test_bad_auth_and_missing_address_are_refused(self):
        self.assertEqual(401, self.post(self.request, "wrong"))
        del self.request["installation_id"]
        self.assertEqual(400, self.post(self.request))
        self.assertFalse(self.store.exists())

    def test_sender_matches_android_length_and_rejects_empty_content(self):
        subscription = dict(self.request, max_length=2)
        build_message(subscription, {"title": "T", "text": "😀"}, 0)
        with self.assertRaises(ValueError):
            build_message(subscription, {"title": "T", "text": "😀x"}, 30)
        with self.assertRaises(ValueError):
            build_message(subscription, {"title": "", "text": "x"}, 30)

    def test_invalid_target_project_is_refused(self):
        for project in ("", "../other", "project/other", "invalid space", 123):
            with self.subTest(project=project):
                self.assertEqual(400, self.post(dict(self.request, firebase_project_id=project)))
        self.assertFalse(self.store.exists())


if __name__ == "__main__":
    unittest.main()
