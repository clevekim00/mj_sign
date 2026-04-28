import { useEffect, useMemo, useState } from "react";
import type {
  SignSynthesisFrame,
  SignSynthesisPoint,
  SignSynthesisResult,
} from "../services/signSynthesisService";

interface SignSynthesisPreviewProps {
  result: SignSynthesisResult | null;
  placeholder?: string;
}

export function SignSynthesisPreview({
  result,
  placeholder = "Text/Speech to Sign 결과를 기다리는 중입니다.",
}: SignSynthesisPreviewProps) {
  const [frameIndex, setFrameIndex] = useState(0);
  const frames = useMemo(() => result?.motion.frames ?? [], [result]);
  const currentFrame = frames[frameIndex] ?? null;

  useEffect(() => {
    setFrameIndex(0);
  }, [result]);

  useEffect(() => {
    if (frames.length <= 1) {
      return;
    }
    const fps = result?.motion.fps ?? 12;
    const timer = window.setInterval(() => {
      setFrameIndex((next) => (next + 1) % frames.length);
    }, Math.max(32, Math.round(1000 / fps)));
    return () => window.clearInterval(timer);
  }, [frames.length, result?.motion.fps]);

  return (
    <section className="glass-panel relative min-h-[320px] overflow-hidden p-5">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_28%,rgba(45,212,191,0.24),transparent_44%)]" />
      <div className="relative z-10 mb-4 flex items-center justify-between gap-4">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-teal-200/70">
            LinguaSign Output
          </p>
          <h2 className="mt-1 text-xl font-black tracking-tight text-white">
            Sign synthesis preview
          </h2>
        </div>
        <span className="rounded-full border border-white/10 bg-black/30 px-3 py-1 text-[10px] font-bold text-white/70">
          {frames.length === 0 ? "READY" : `${frameIndex + 1}/${frames.length}`}
        </span>
      </div>

      {currentFrame === null ? (
        <div className="relative z-10 flex min-h-[220px] items-center justify-center rounded-3xl border border-white/10 bg-white/[0.04] p-8 text-center text-sm font-semibold text-white/70">
          {placeholder}
        </div>
      ) : (
        <svg
          className="relative z-10 h-[240px] w-full rounded-3xl border border-white/10 bg-slate-950/40"
          viewBox="0 0 1000 700"
          role="img"
          aria-label="Sign synthesis landmark playback"
        >
          <SynthesisSkeleton frame={currentFrame} />
        </svg>
      )}

      {result && (
        <div className="relative z-10 mt-4 flex flex-wrap gap-2">
          {result.sign_plan.glosses.map((gloss) => (
            <span
              key={gloss}
              className="rounded-xl border border-teal-300/20 bg-teal-300/10 px-3 py-1 text-xs font-bold text-teal-100"
            >
              {gloss}
            </span>
          ))}
        </div>
      )}
    </section>
  );
}

function SynthesisSkeleton({ frame }: { frame: SignSynthesisFrame }) {
  return (
    <>
      <circle cx="500" cy="170" r="84" fill="rgba(255,255,255,0.05)" />
      <line x1="500" y1="250" x2="500" y2="460" stroke="rgba(255,255,255,0.18)" strokeWidth="10" strokeLinecap="round" />
      <LandmarkGroup points={frame.face_contour} color="#99F6E4" radius={6} />
      <LandmarkGroup points={frame.pose} color="#E2E8F0" radius={8} />
      <LandmarkGroup points={frame.left_hand} color="#38BDF8" radius={9} />
      <LandmarkGroup points={frame.right_hand} color="#FACC15" radius={9} />
    </>
  );
}

function LandmarkGroup({
  points,
  color,
  radius,
}: {
  points: SignSynthesisPoint[];
  color: string;
  radius: number;
}) {
  const polyline = points.map((point) => `${point.x * 1000},${point.y * 700}`).join(" ");
  return (
    <g>
      <polyline
        points={polyline}
        fill="none"
        stroke={color}
        strokeOpacity="0.34"
        strokeWidth="4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {points.map((point, index) => (
        <circle
          key={`${point.x}-${point.y}-${index}`}
          cx={point.x * 1000}
          cy={point.y * 700}
          r={radius + Math.min(Math.abs(point.z) * 24, 4)}
          fill={color}
          fillOpacity="0.86"
        />
      ))}
    </g>
  );
}
