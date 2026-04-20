import { HandLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";
import * as $proto from "../proto/landmark";

// 1. WebSocket Bridge Service
export class SignBridgeClient {
  private socket: WebSocket | null = null;
  private onMessageCallback: (data: any) => void = () => {};
  private onErrorCallback: (err: any) => void = () => {};

  connect(url: string = "ws://127.0.0.1:8080/ws/sign") {
    console.log(`Attempting to connect to Bridge: ${url}`);
    try {
      this.socket = new WebSocket(url);
      this.socket.binaryType = "arraybuffer";

      this.socket.onopen = () => {
        console.log("%c BRIDGE CONNECTED ", "background: #22c55e; color: #fff; font-weight: bold;");
      };
    } catch (e: any) {
      console.error("Critical: WebSocket Creation Failed", e);
      window.alert(`Security Block detected!\nMessage: ${e.message}\nURL: ${url}\n\n이 메시지가 뜨면 브라우저 보안 정책이 127.0.0.1 접근을 차단한 것입니다.`);
      this.onErrorCallback(e);
      return;
    }

    this.socket.onerror = (error) => {
      console.error("%c BRIDGE ERROR ", "background: #ef4444; color: #fff; font-weight: bold;", error);
      window.alert(`Bridge Connection Failed!\nreadyState: ${this.socket?.readyState}\nURL: ${url}\nError: ${JSON.stringify(error) || "Connection Blocked"}\n\n주의: Mac 방화벽이나 VPN이 켜져 있는지 확인해 주세요.`);
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
    };
  }

  onResult(callback: (data: any) => void) {
    this.onMessageCallback = callback;
  }

  onError(callback: (err: any) => void) {
    this.onErrorCallback = callback;
  }

  sendLandmarks(sessionId: string, landmarks: any[]) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    // Build the Protobuf message
    const chunk = $proto.mj.sign.ClientStreamChunk.create({
      sessionId: sessionId,
      frames: landmarks.map(lm => ({
        timestampMs: Date.now(),
        leftHand: lm.left ? [{ x: lm.left.x, y: lm.left.y, z: lm.left.z }] : [],
        rightHand: lm.right ? [{ x: lm.right.x, y: lm.right.y, z: lm.right.z }] : []
      }))
    });

    const buffer = $proto.mj.sign.ClientStreamChunk.encode(chunk).finish();
    this.socket.send(buffer as any);
  }

  disconnect() {
    this.socket?.close();
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
