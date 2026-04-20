import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Camera, Wifi, Settings, MessageSquare, Activity, ShieldCheck } from 'lucide-react';
import { SignBridgeClient, SignMLService } from './services/signService';
import './index.css';

const SESSION_ID = `web-${Math.random().toString(36).substr(2, 9)}`;

const App: React.FC = () => {
  const [isLive, setIsLive] = useState(false);
  const [translatedText, setTranslatedText] = useState('수어를 시작해 주세요...');
  const [keywords, setKeywords] = useState<string[]>([]);
  const [bridgeStatus, setBridgeStatus] = useState<'idle' | 'connecting' | 'connected' | 'error'>('idle');

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mlService = useRef(new SignMLService());
  const bridgeClient = useRef(new SignBridgeClient());

  useEffect(() => {
    bridgeClient.current.onResult((data) => {
      if (data.is_final) {
        setTranslatedText(data.text);
        setKeywords(prev => [...prev.slice(-4), data.text.split(' ').pop() || '']);
      }
    });

    return () => bridgeClient.current.disconnect();
  }, []);

  const startCamera = async () => {
    try {
      setIsLive(true);
      setBridgeStatus('connecting');

      // 1. Request Camera Permission Immediately (UX First)
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          width: { ideal: 1280 }, 
          height: { ideal: 720 },
          facingMode: "user"
        } 
      });
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.onloadedmetadata = () => {
          videoRef.current?.play();
        };
      }

      // 2. Load ML & Connect Bridge in Parallel
      setTranslatedText('인식 엔진 로딩 중...');
      await mlService.current.init();
      
      bridgeClient.current.onError(() => setBridgeStatus('error'));
      bridgeClient.current.connect("ws://127.0.0.1:8080/ws/sign");
      setBridgeStatus('connected');
      setTranslatedText('인식 준비 완료. 수어를 시작하세요.');

      // Start processing loop
      requestAnimationFrame(processFrame);
      
    } catch (err: any) {
      console.error("Camera Error:", err);
      setIsLive(false);
      setBridgeStatus('error');
      
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        alert("카메라 권한이 거부되었습니다. 브라우저 설정에서 카메라 권한을 허용해 주세요.");
      } else {
        alert(`카메라를 시작할 수 없습니다: ${err.message}\n주의: HTTPS 환경이나 localhost에서만 카메라 접근이 가능합니다.`);
      }
    }
  };

  const processFrame = () => {
    if (!videoRef.current || !canvasRef.current || !isLive) return;

    const result = mlService.current.detect(videoRef.current, performance.now());
    
    // Draw landmarks and send to bridge
    if (result && (result.landmarks.length > 0)) {
      drawLandmarks(result.landmarks);
      
      // Send every 10th frame or specific logic
      // In a real V2 bridge, we might send every frame and let the bridge buffer
      bridgeClient.current.sendLandmarks(SESSION_ID, [{
        right: result.landmarks[0] ? result.landmarks[0][0] : null // Simplified mapping for demo
      }]);
    }

    requestAnimationFrame(processFrame);
  };

  const drawLandmarks = (landmarks: any[]) => {
    const ctx = canvasRef.current?.getContext('2d');
    if (!ctx || !canvasRef.current) return;

    ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
    ctx.fillStyle = "#3b82f6";
    
    landmarks.forEach(hand => {
      hand.forEach((point: any) => {
        ctx.beginPath();
        ctx.arc(point.x * canvasRef.current!.width, point.y * canvasRef.current!.height, 3, 0, 2 * Math.PI);
        ctx.fill();
      });
    });
  };

  return (
    <div className="gradient-bg flex flex-col items-center p-4 lg:p-8 min-h-screen relative overflow-hidden">
      {/* Decorative Blur Orbs */}
      <div className="absolute top-[-10%] right-[-5%] w-64 h-64 bg-blue-500 rounded-full blur-[120px] opacity-20 pointer-events-none" />
      <div className="absolute bottom-[-10%] left-[-5%] w-80 h-80 bg-purple-600 rounded-full blur-[120px] opacity-15 pointer-events-none" />

      {/* Header */}
      <motion.header 
        initial={{ y: -50, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="w-full max-w-7xl flex justify-between items-center z-10 mb-6 lg:mb-10"
      >
        <div className="flex items-center gap-4">
          <div className="p-2.5 bg-blue-600 rounded-2xl shadow-xl shadow-blue-500/30">
            <Activity size={26} className="text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter glow-text leading-tight">
            Sign<span className="text-blue-500">SampleApp</span>
          </h1>
          <p className="text-[10px] text-text-muted font-bold tracking-widest uppercase opacity-60">Premium Dashboard</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <motion.div 
            animate={{ opacity: [0.6, 1, 0.6] }}
            transition={{ duration: 2, repeat: Infinity }}
            className={`flex items-center gap-2 px-4 py-2 glass-panel rounded-full text-[11px] font-bold border ${bridgeStatus === 'connected' ? 'border-green-500/20' : 'border-red-500/20'}`}
          >
            <div className={`w-1.5 h-1.5 rounded-full ${bridgeStatus === 'connected' ? 'bg-green-400 shadow-[0_0_8px_rgba(74,222,128,0.5)]' : 'bg-red-400'}`} />
            <span className="text-text-muted">BRIDGE</span>
            <span className={bridgeStatus === 'connected' ? "text-green-400" : "text-red-400"}>
              {bridgeStatus.toUpperCase()}
            </span>
          </motion.div>
          <button className="p-2.5 glass-panel rounded-xl hover:bg-white/10 transition-all active:scale-95">
            <Settings size={20} className="text-text-muted" />
          </button>
        </div>
      </motion.header>

      <main className="w-full max-w-7xl grid grid-cols-1 lg:grid-cols-12 gap-8 z-10 flex-1">
        
        {/* Camera Section */}
        <motion.section 
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="lg:col-span-8 flex flex-col gap-4"
        >
          <div className="relative aspect-video glass-panel overflow-hidden group bg-slate-900/50">
            {!isLive ? (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-6">
                <Camera size={64} className="text-text-muted opacity-20" />
                <button 
                  onClick={startCamera}
                  className="px-10 py-4 bg-blue-600 hover:bg-blue-500 rounded-2xl font-bold shadow-xl shadow-blue-600/40 transition-all hover:scale-105 active:scale-95"
                >
                  카메라 시작 (Real-time)
                </button>
                <p className="text-sm text-text-muted">브릿지 서버 연동 및 MediaPipe 가동</p>
              </div>
            ) : (
              <>
                <video ref={videoRef} className="w-full h-full object-cover grayscale-[0.3]" playsInline muted />
                <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" width={1280} height={720} />
                <div className="absolute top-4 left-4 flex gap-2">
                  <span className="px-3 py-1 bg-red-500/80 backdrop-blur rounded-full text-[10px] font-bold uppercase status-pulse">Live</span>
                  <span className="px-3 py-1 bg-black/40 backdrop-blur rounded-full text-[10px] font-medium text-white/80">60 FPS</span>
                </div>
              </>
            )}
          </div>
          
          {/* Subtitle Display */}
          <div className="glass-panel p-6 flex items-center gap-6 min-h-[120px] relative overflow-hidden">
             <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500" />
            <div className="p-4 bg-blue-500/10 rounded-2xl">
              <MessageSquare size={32} className="text-blue-500" />
            </div>
            <div className="flex-1">
              <AnimatePresence mode="wait">
                <motion.p 
                  key={translatedText}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="text-2xl sm:text-3xl font-bold tracking-tight"
                >
                  {translatedText}
                </motion.p>
              </AnimatePresence>
            </div>
          </div>
        </motion.section>

        {/* Analytics Section */}
        <motion.section 
          initial={{ x: 50, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          className="lg:col-span-4 flex flex-col gap-6"
        >
          <div className="glass-panel p-6 flex-1 flex flex-col bg-white/[0.02]">
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-2">
                <ShieldCheck size={18} className="text-blue-400" />
                <h2 className="font-bold text-lg text-text-main">V2 Insight</h2>
              </div>
              <span className="text-[10px] bg-slate-800 text-text-muted px-2 py-1 rounded">SSL ACTIVE</span>
            </div>
            
            <div className="space-y-6">
              <div>
                <div className="text-[11px] uppercase tracking-widest text-text-muted mb-3 font-semibold">Tracked Keywords</div>
                <div className="flex flex-wrap gap-2">
                  {keywords.length > 0 ? keywords.map((kw, i) => (
                    <motion.span 
                      initial={{ scale: 0.8, opacity: 0 }}
                      animate={{ scale: 1, opacity: 1 }}
                      key={`${kw}-${i}`}
                      className="px-3 py-1.5 bg-blue-500/10 border border-blue-500/20 rounded-xl text-sm font-medium text-blue-300"
                    >
                      {kw}
                    </motion.span>
                  )) : (
                    <span className="text-sm italic text-slate-600">대기 중...</span>
                  )}
                </div>
              </div>

              <div className="p-5 bg-white/5 border border-white/5 rounded-2xl">
                <div className="flex items-center gap-2 mb-3">
                  <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse" />
                  <span className="text-xs font-bold text-purple-400">Gemma 2 Agent</span>
                </div>
                <p className="text-sm leading-relaxed text-text-muted font-medium">
                  수어의 비수어적 요소(NMS)와 문맥을 분석하여 가장 자연스러운 한국어 구어체로 실시간 변환하고 있습니다.
                </p>
              </div>
            </div>

            <div className="mt-auto pt-6 border-t border-white/5">
               <div className="flex justify-between text-xs text-text-muted mb-4">
                 <span>Session ID:</span>
                 <span className="font-mono text-[10px]">{SESSION_ID}</span>
               </div>
               <button className="w-full py-4 glass-panel hover:bg-white/5 transition-all text-sm font-bold text-blue-400">
                기록 분석 대시보드
              </button>
            </div>
          </div>
        </motion.section>
      </main>

      <footer className="mt-8 py-4 w-full max-w-7xl flex justify-between items-center text-text-muted text-[10px] tracking-widest uppercase z-10 border-t border-white/5">
        <div className="flex gap-4">
          <span>Bridge 2.0.4-RELEASE</span>
          <span>•</span>
          <span>MediaPipe Tasks Engine</span>
        </div>
        <span>© 2026 Sign-SampleApp Project</span>
      </footer>
    </div>
  );
};

export default App;
