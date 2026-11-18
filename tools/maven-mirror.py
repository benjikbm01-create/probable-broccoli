#!/usr/bin/env python3
"""Local retrying, caching Maven mirror for flaky networks.
Serves http://127.0.0.1:8765/<name>/<path> from the upstream repo <name>, retrying spurious 404s."""
import http.server, os, sys, time, urllib.request, urllib.error, threading
UP = {"central": "https://repo.maven.apache.org/maven2/", "central1": "https://repo1.maven.org/maven2/"}
CACHE = os.path.expanduser("~/.cache/fi-maven-mirror")
class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def _fetch(self):
        parts = self.path.lstrip("/").split("/", 1)
        if len(parts) < 2 or parts[0] not in UP: return None, 404
        local = os.path.join(CACHE, parts[1])
        if os.path.isfile(local): return local, 200
        last = 404
        for attempt in range(12):
            for base in (UP["central"], UP["central1"]):
                try:
                    with urllib.request.urlopen(base + parts[1], timeout=30) as r:
                        data = r.read()
                    os.makedirs(os.path.dirname(local), exist_ok=True)
                    tmp = local + ".tmp%d" % threading.get_ident()
                    open(tmp, "wb").write(data); os.replace(tmp, local)
                    return local, 200
                except urllib.error.HTTPError as e: last = e.code
                except Exception: last = 502
            time.sleep(0.3)
        return None, last
    def do_HEAD(self): self.do_GET(body=False)
    def do_GET(self, body=True):
        local, code = self._fetch()
        if not local:
            self.send_response(code); self.end_headers(); return
        size = os.path.getsize(local)
        self.send_response(200); self.send_header("Content-Length", str(size)); self.end_headers()
        if body:
            with open(local, "rb") as f: self.wfile.write(f.read())
http.server.ThreadingHTTPServer(("127.0.0.1", int(sys.argv[1]) if len(sys.argv) > 1 else 8765), H).serve_forever()
