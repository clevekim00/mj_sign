import { HandLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";
import * as $proto from "../proto/landmark";

// 1. WebSocket Bridge Service
export class SignBridgeClient {
  private socket: WebSocket | null = null;
  private onMessageCallback: (data: any) => void = () => {};
  private onErrorCallback: (err: any) => void = () => {};
  private onOpenCallback: () => void = () => {};
  private onCloseCallback: () => void = () => {};
  private chunkSequence = 0;
  private segmentId = crypto.randomUUID();

  connect(url?: string) {
    const scheme = window.location.protocol === "https:" ? "wss" : "ws";
    const bridgeHost = import.meta.env.VITE_SIGN_BRIDGE_HOST || "127.0.0.1:8080";
    const bridgeUrl = url
      ?? `${scheme}://${bridgeHost}/ws/sign?locale=ko-KR&sign_language=ksl&model_profile=sign-gemma-ko&protocol_version=signbridge-model-v1&stream_protocol_version=signbridge-stream-v2`;
    console.log(`Attempting to connect to Bridge: ${bridgeUrl}`);
    try {
      this.socket = new WebSocket(bridgeUrl);
      this.socket.binaryType = "arraybuffer";

      this.socket.onopen = () => {
        console.log("%c BRIDGE CONNECTED ", "background: #22c55e; color: #fff; font-weight: bold;");
        this.onOpenCallback();
      };
    } catch (e: any) {
      console.error("Critical: WebSocket Creation Failed", e);
      window.alert(`Security Block detected!\nMessage: ${e.message}\nURL: ${bridgeUrl}\n\n이 메시지가 뜨면 브라우저 보안 정책이 로컬 Bridge 접근을 차단한 것입니다.`);
      this.onErrorCallback(e);
      return;
    }

    this.socket.onerror = (error) => {
      console.error("%c BRIDGE ERROR ", "background: #ef4444; color: #fff; font-weight: bold;", error);
      window.alert(`Bridge Connection Failed!\nreadyState: ${this.socket?.readyState}\nURL: ${bridgeUrl}\nError: ${JSON.stringify(error) || "Connection Blocked"}\n\n주의: Mac 방화벽이나 VPN이 켜져 있는지 확인해 주세요.`);
      this.onErrorCallback(error);
    };

    this.socket.onmessage = (event) => {
      if (typeof event.data === "string") {
        try {
          this.onMessageCallback(JSON.parse(event.data));
        } catch (e) {
          console.warn("Failed to parse JSON result", e);
        }
      }
    };

    this.socket.onclose = (event) => {
      console.log(`Bridge connection closed. Code: ${event.code}, Reason: ${event.reason}`);
      this.onCloseCallback();
    };
  }

  onResult(callback: (data: any) => void) {
    this.onMessageCallback = callback;
  }

  onError(callback: (err: any) => void) {
    this.onErrorCallback = callback;
  }

  onOpen(callback: () => void) {
    this.onOpenCallback = callback;
  }

  onClose(callback: () => void) {
    this.onCloseCallback = callback;
  }

  sendLandmarks(sessionId: string, landmarks: any[]) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return false;
    this.chunkSequence += 1;
    const capturedAt = Date.now();

    const chunk = $proto.mj.sign.ClientStreamChunk.create({
      sessionId: sessionId,
      frames: landmarks.map(lm => ({
        timestampMs: lm.timestampMs ?? capturedAt,
        leftHand: (lm.left ?? []).map((point: any) => ({ x: point.x, y: point.y, z: point.z })),
        rightHand: (lm.right ?? []).map((point: any) => ({ x: point.x, y: point.y, z: point.z })),
        pose: lm.pose ?? [],
        faceContour: lm.faceContour ?? [],
      })),
      chunkSequence: this.chunkSequence,
      chunkId: crypto.randomUUID(),
      segmentId: this.segmentId,
      endOfSegment: false,
      sentAtMs: capturedAt,
      schemaVersion: "mj.sign.ClientStreamChunk/v2",
    });

    const buffer = $proto.mj.sign.ClientStreamChunk.encode(chunk).finish();
    this.socket.send(buffer as any);
    return true;
  }

  endSegment(sessionId: string) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return false;
    this.chunkSequence += 1;
    const chunk = $proto.mj.sign.ClientStreamChunk.create({
      sessionId,
      frames: [],
      chunkSequence: this.chunkSequence,
      chunkId: crypto.randomUUID(),
      segmentId: this.segmentId,
      endOfSegment: true,
      sentAtMs: Date.now(),
      schemaVersion: "mj.sign.ClientStreamChunk/v2",
    });
    this.socket.send($proto.mj.sign.ClientStreamChunk.encode(chunk).finish() as any);
    this.segmentId = crypto.randomUUID();
    return true;
  }

  disconnect() {
    this.socket?.close();
    this.socket = null;
    this.chunkSequence = 0;
  }
}

// 2. ML Inference Service (MediaPipe)
export class SignMLService {
  private handLandmarker: HandLandmarker | null = null;

  async init() {
    const vision = await FilesetResolver.forVisionTasks(
      "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
    );
    this.handLandmarker = await HandLandmarker.createFromOptions(vision, {
      baseOptions: {
        modelAssetPath: `https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task`,
        delegate: "GPU"
      },
      runningMode: "VIDEO",
      numHands: 2
    });
  }

  detect(video: HTMLVideoElement, timestamp: number) {
    if (!this.handLandmarker) return null;
    return this.handLandmarker.detectForVideo(video, timestamp);
  }
}
