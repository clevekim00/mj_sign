export type SignSynthesisSourceType = "text" | "speech";

export interface SignSynthesisPoint {
  x: number;
  y: number;
  z: number;
}

export interface SignSynthesisFrame {
  timestamp_ms: number;
  left_hand: SignSynthesisPoint[];
  right_hand: SignSynthesisPoint[];
  pose: SignSynthesisPoint[];
  face_contour: SignSynthesisPoint[];
}

export interface SignSynthesisMotion {
  format: "landmark-frames";
  fps: number;
  frame_count: number;
  frames: SignSynthesisFrame[];
}

export interface SignSynthesisPlan {
  glosses: string[];
  non_manual_markers: string[];
  grammar_note: string;
}

export interface SignSynthesisRequest {
  session_id?: string;
  source_type?: SignSynthesisSourceType;
  text?: string;
  transcript?: string;
  audio_b64?: string;
  locale?: string;
  sign_language?: string;
  model_profile?: string;
  output_format?: "landmarks" | "avatar-motion";
  protocol_version?: "signbridge-synthesis-v1";
}

export interface SignSynthesisResult {
  session_id: string;
  event_type: "synthesis_result";
  source_type: SignSynthesisSourceType;
  text: string;
  locale: string;
  sign_language: string;
  model_profile: string;
  protocol_version: string;
  sign_plan: SignSynthesisPlan;
  motion: SignSynthesisMotion;
  is_final: boolean;
  confidence: number;
  error: string | null;
}

export class SignSynthesisHttpClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string = "http://127.0.0.1:8080") {
    this.baseUrl = baseUrl;
  }

  synthesizeText(request: Omit<SignSynthesisRequest, "source_type">) {
    return this.post("/api/v2/sign/synthesize", {
      ...request,
      source_type: "text",
    });
  }

  synthesizeSpeech(request: Omit<SignSynthesisRequest, "source_type">) {
    return this.post("/api/v2/speech/sign", {
      ...request,
      source_type: "speech",
    });
  }

  private async post(path: string, request: SignSynthesisRequest): Promise<SignSynthesisResult> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        locale: "ko-KR",
        sign_language: "ksl",
        model_profile: "sign-gemma-ko",
        output_format: "landmarks",
        protocol_version: "signbridge-synthesis-v1",
        ...request,
      }),
    });

    if (!response.ok) {
      throw new Error(`Sign synthesis failed: ${response.status}`);
    }
    return (await response.json()) as SignSynthesisResult;
  }
}
