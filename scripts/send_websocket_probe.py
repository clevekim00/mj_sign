#!/usr/bin/env python3
import argparse
import base64
import hashlib
import json
import os
import socket
import struct
import sys
import time
from urllib.parse import urlparse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "sign_gemma_mock"))

from schema import landmark_pb2  # type: ignore


def build_chunk(session_id: str, frame_count: int) -> bytes:
    chunk = landmark_pb2.ClientStreamChunk()
    chunk.session_id = session_id
    now_ms = int(time.time() * 1000)
    for index in range(frame_count):
        frame = chunk.frames.add()
        frame.timestamp_ms = now_ms + index
        left = frame.left_hand.add()
        left.x = 0.1
        left.y = 0.2
        left.z = 0.3
        right = frame.right_hand.add()
        right.x = 0.9
        right.y = 0.8
        right.z = 0.7
    return chunk.SerializeToString()


def websocket_connect(ws_url: str) -> socket.socket:
    parsed = urlparse(ws_url)
    host = parsed.hostname or "127.0.0.1"
    port = parsed.port or 80
    path = parsed.path or "/"
    key = base64.b64encode(os.urandom(16)).decode("ascii")

    sock = socket.create_connection((host, port), timeout=10)
    request = (
        f"GET {path} HTTP/1.1\r\n"
        f"Host: {host}:{port}\r\n"
        "Upgrade: websocket\r\n"
        "Connection: Upgrade\r\n"
        f"Sec-WebSocket-Key: {key}\r\n"
        "Sec-WebSocket-Version: 13\r\n\r\n"
    )
    sock.sendall(request.encode("ascii"))
    response = read_http_response(sock)
    if "101" not in response.splitlines()[0]:
        raise RuntimeError(f"WebSocket handshake failed: {response}")

    accept = base64.b64encode(
        hashlib.sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode("ascii")).digest()
    ).decode("ascii")
    if accept not in response:
        raise RuntimeError("WebSocket handshake accept header mismatch")
    return sock


def read_http_response(sock: socket.socket) -> str:
    data = b""
    while b"\r\n\r\n" not in data:
        chunk = sock.recv(4096)
        if not chunk:
            break
        data += chunk
    return data.decode("utf-8", errors="replace")


def send_binary_frame(sock: socket.socket, payload: bytes) -> None:
    frame = bytearray()
    frame.append(0x82)
    length = len(payload)
    mask_key = os.urandom(4)
    if length < 126:
        frame.append(0x80 | length)
    elif length < 65536:
        frame.append(0x80 | 126)
        frame.extend(struct.pack("!H", length))
    else:
        frame.append(0x80 | 127)
        frame.extend(struct.pack("!Q", length))
    frame.extend(mask_key)
    masked = bytes(payload[i] ^ mask_key[i % 4] for i in range(length))
    frame.extend(masked)
    sock.sendall(frame)


def recv_frame(sock: socket.socket):
    header = sock.recv(2)
    if len(header) < 2:
        return None, None
    first, second = header
    opcode = first & 0x0F
    masked = (second >> 7) & 1
    length = second & 0x7F
    if length == 126:
        length = struct.unpack("!H", sock.recv(2))[0]
    elif length == 127:
        length = struct.unpack("!Q", sock.recv(8))[0]
    mask_key = sock.recv(4) if masked else b""
    payload = b""
    while len(payload) < length:
        payload += sock.recv(length - len(payload))
    if masked:
        payload = bytes(payload[i] ^ mask_key[i % 4] for i in range(length))
    return opcode, payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:8080/ws/sign")
    parser.add_argument("--session-id", default=f"probe-{int(time.time())}")
    parser.add_argument("--frame-count", type=int, default=8)
    parser.add_argument("--timeout-seconds", type=float, default=15)
    args = parser.parse_args()

    sock = websocket_connect(args.url)
    sock.settimeout(args.timeout_seconds)
    send_binary_frame(sock, build_chunk(args.session_id, args.frame_count))

    deadline = time.time() + args.timeout_seconds
    saw_final = False
    while time.time() < deadline:
        opcode, payload = recv_frame(sock)
        if opcode is None:
            break
        if opcode == 0x1:
            text = payload.decode("utf-8", errors="replace")
            print(text)
            try:
                message = json.loads(text)
            except json.JSONDecodeError:
                continue
            if message.get("is_final") is True and message.get("text"):
                saw_final = True
                break
        elif opcode == 0x8:
            break

    sock.close()
    return 0 if saw_final else 1


if __name__ == "__main__":
    raise SystemExit(main())
