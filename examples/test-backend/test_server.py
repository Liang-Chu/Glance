import contextlib
import io
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from http.client import HTTPConnection
from pathlib import Path
from types import SimpleNamespace
from urllib.error import HTTPError
from urllib.request import Request, urlopen

import server


class PollServerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.previous_content = server.CONTENT
        server.CONTENT = Path(self.temp.name) / 'content.json'
        server.CONTENT.write_text('{"title":"T","text":"hello"}', encoding='utf-8')
        server.ARGS = SimpleNamespace(token='never-log-this-credential')
        self.http = ThreadingHTTPServer(('127.0.0.1', 0), server.Handler)
        self.thread = threading.Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.http.shutdown()
        self.http.server_close()
        self.thread.join()
        server.CONTENT = self.previous_content
        self.temp.cleanup()

    def post(self, data, credential):
        request = Request('http://127.0.0.1:' + str(self.http.server_port) + '/glance', data,
                          {'Authorization': 'Bearer ' + credential})
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, response.read()
        except HTTPError as response:
            with response:
                return response.code, response.read()

    def test_authentication_does_not_disclose_credentials_in_logs(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(200, self.post(b'{}', server.ARGS.token)[0])
            self.assertEqual(401, self.post(b'{}', 'also-private')[0])
        self.assertNotIn(server.ARGS.token, output.getvalue())
        self.assertNotIn('also-private', output.getvalue())

    def test_nonobject_requests_do_not_crash(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(200, self.post(b'[]', server.ARGS.token)[0])

    def test_oversized_request_is_rejected_before_reading_body(self):
        # Send headers only: the server must reject the declared size immediately.
        # Sending the body races with its close and can reset the client socket on Windows.
        with contextlib.closing(HTTPConnection('127.0.0.1', self.http.server_port, timeout=2)) as connection:
            connection.putrequest('POST', '/glance')
            connection.putheader('Authorization', 'Bearer ' + server.ARGS.token)
            connection.putheader('Content-Length', '65537')
            connection.endheaders()
            with connection.getresponse() as response:
                self.assertEqual(413, response.status)
                self.assertEqual(b'', response.read())


if __name__ == '__main__':
    unittest.main()
