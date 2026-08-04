#!/usr/bin/env python3
"""Dev-only mock OpenID Connect provider (Okta-style) over HTTPS.

Serves issuer discovery, JWKS and a convenience token issuer so a JWT can be
obtained with:  GET /oauth2/default/issue?email=user@example.com
"""
import base64
import json
import os
import ssl
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

PORT = int(os.environ.get("PORT", "8085"))
HOST = os.environ.get("HOST", "0.0.0.0")
ISSUER_BASE = os.environ.get("ISSUER_BASE", "https://127.0.0.1:8085")
KEY_FILE = os.environ.get("KEY_FILE", "/app/key.pem")
CERT_FILE = os.environ.get("CERT_FILE", "/app/cert.pem")
KID = "mock-key-1"

with open(KEY_FILE, "rb") as f:
    key = serialization.load_pem_private_key(f.read(), password=None)

numbers = key.public_key().public_numbers()


def b64u_bytes(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def b64u_int(value: int) -> str:
    length = (value.bit_length() + 7) // 8
    return b64u_bytes(value.to_bytes(length, "big"))


BASE = f"{ISSUER_BASE}/oauth2/default"

JWKS = {"keys": [{
    "kty": "RSA", "use": "sig", "alg": "RS256", "kid": KID,
    "n": b64u_int(numbers.n), "e": b64u_int(numbers.e),
}]}

WELL_KNOWN = {
    "issuer": BASE,
    "authorization_endpoint": BASE + "/authorize",
    "token_endpoint": BASE + "/token",
    "jwks_uri": BASE + "/jwks",
    "response_types_supported": ["code"],
    "subject_types_supported": ["public"],
    "id_token_signing_alg_values_supported": ["RS256"],
    "scopes_supported": ["openid", "email", "profile"],
}


def make_jwt(email: str) -> str:
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT", "kid": KID}
    payload = {
        "iss": BASE, "sub": email, "email": email,
        "aud": "mock-client", "iat": now, "exp": now + 3600,
    }

    def enc(data) -> str:
        return b64u_bytes(json.dumps(data, separators=(",", ":")).encode())

    signing_input = enc(header) + "." + enc(payload)
    sig = key.sign(signing_input.encode(), padding.PKCS1v15(), hashes.SHA256())
    return signing_input + "." + b64u_bytes(sig)


class Handler(BaseHTTPRequestHandler):
    def _json(self, body, status=200):
        data = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path in ("/oauth2/default/.well-known/openid-configuration",
                         "/.well-known/openid-configuration"):
            self._json(WELL_KNOWN)
        elif self.path == "/oauth2/default/jwks":
            self._json(JWKS)
        elif self.path == "/health":
            self._json({"status": "ok"})
        elif self.path.startswith("/oauth2/default/issue"):
            email = parse_qs(urlparse(self.path).query).get("email", ["user@example.com"])[0]
            self._json({"token": make_jwt(email), "issuer": BASE, "expiresIn": 3600})
        else:
            self._json({"error": "not found"}, 404)

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    server = HTTPServer((HOST, PORT), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(CERT_FILE, KEY_FILE)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print(f"Mock OIDC (HTTPS) ready at {BASE}")
    server.serve_forever()
