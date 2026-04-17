/**
 * MJ Sign Web Sample - Application Logic
 */

class LandmarkVisualizer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.points = [];
        this.animationId = null;
        this.resize();
        window.addEventListener('resize', () => this.resize());
        
        // Mock hand landmarks (21 points)
        this.handBase = [
            {x: 0.5, y: 0.8}, // Wrist
            {x: 0.4, y: 0.75}, {x: 0.35, y: 0.65}, {x: 0.38, y: 0.55}, {x: 0.45, y: 0.5}, // Thumb
            {x: 0.5, y: 0.6}, {x: 0.5, y: 0.45}, {x: 0.5, y: 0.35}, {x: 0.5, y: 0.25}, // Index
            {x: 0.55, y: 0.58}, {x: 0.58, y: 0.43}, {x: 0.6, y: 0.33}, {x: 0.62, y: 0.23}, // Middle
            {x: 0.6, y: 0.62}, {x: 0.65, y: 0.5}, {x: 0.68, y: 0.4}, {x: 0.7, y: 0.3}, // Ring
            {x: 0.65, y: 0.72}, {x: 0.75, y: 0.65}, {x: 0.82, y: 0.58}, {x: 0.85, y: 0.5} // Pinky
        ];

        // Finger connections
        this.connections = [
            [0, 1, 2, 3, 4], // Thumb
            [0, 5, 6, 7, 8], // Index
            [0, 9, 10, 11, 12], // Middle
            [0, 13, 14, 15, 16], // Ring
            [0, 17, 18, 19, 20], // Pinky
            [5, 9, 13, 17] // Palmar
        ];
    }

    resize() {
        const container = this.canvas.parentElement;
        this.canvas.width = container.clientWidth;
        this.canvas.height = container.clientHeight;
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        const time = Date.now() * 0.002;
        const scale = Math.min(this.canvas.width, this.canvas.height) * 0.6;
        const offsetX = this.canvas.width / 2 - scale / 2;
        const offsetY = this.canvas.height / 2 - scale / 2;

        // Draw connections
        this.ctx.strokeStyle = 'rgba(79, 70, 229, 0.4)';
        this.ctx.lineWidth = 2;
        
        this.connections.forEach(conn => {
            this.ctx.beginPath();
            conn.forEach((idx, i) => {
                const base = this.handBase[idx];
                // Add some natural movement
                const x = offsetX + (base.x + Math.sin(time + idx) * 0.02) * scale;
                const y = offsetY + (base.y + Math.cos(time * 0.8 + idx) * 0.02) * scale;
                
                if (i === 0) this.ctx.moveTo(x, y);
                else this.ctx.lineTo(x, y);
            });
            this.ctx.stroke();
        });

        // Draw points
        this.handBase.forEach((base, idx) => {
            const x = offsetX + (base.x + Math.sin(time + idx) * 0.02) * scale;
            const y = offsetY + (base.y + Math.cos(time * 0.8 + idx) * 0.02) * scale;
            
            this.ctx.fillStyle = idx === 0 ? '#8b5cf6' : '#4f46e5';
            this.ctx.beginPath();
            this.ctx.arc(x, y, 4, 0, Math.PI * 2);
            this.ctx.fill();
            
            // Outer glow for points
            this.ctx.shadowBlur = 10;
            this.ctx.shadowColor = 'rgba(79, 70, 229, 0.5)';
            this.ctx.stroke();
            this.ctx.shadowBlur = 0;
        });

        this.animationId = requestAnimationFrame(() => this.draw());
    }

    start() {
        this.draw();
    }
}

class SystemMetrics {
    constructor() {
        this.frames = 12482;
        this.latency = 42;
        this.gpu = 68;
        this.elements = {
            frames: document.getElementById('metric-frames'),
            latency: document.getElementById('metric-latency'),
            gpu: document.getElementById('metric-gpu')
        };
    }

    update() {
        setInterval(() => {
            // Random fluctuations
            this.frames += Math.floor(Math.random() * 5);
            this.latency = 40 + Math.floor(Math.random() * 8);
            this.gpu = 65 + Math.floor(Math.random() * 10);

            this.elements.frames.innerText = this.frames.toLocaleString();
            this.elements.latency.innerText = this.latency + 'ms';
            this.elements.gpu.innerText = this.gpu + '%';
        }, 2000);
    }
}

class TranslationFeed {
    constructor(feedId, currentId) {
        this.feed = document.getElementById(feedId);
        this.current = document.getElementById(currentId);
        this.sentences = [
            "오늘 날씨가 참 좋네요.",
            "병원은 어디에 있나요?",
            "제 이름은 영희입니다.",
            "도와주셔서 감사합니다.",
            "다음에 또 만나요!"
        ];
        this.index = 0;
    }

    async typeOut(text) {
        this.current.innerText = "";
        for (let i = 0; i < text.length; i++) {
            this.current.innerText += text[i];
            await new Promise(r => setTimeout(r, 100 + Math.random() * 100));
        }
        
        await new Promise(r => setTimeout(r, 2000));
        
        // Move current to feed list
        const item = document.createElement('div');
        item.className = 'feed-item final';
        const now = new Date();
        const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
        
        item.innerHTML = `
            <span class="time">${timeStr}</span>
            <p class="text">${text}</p>
        `;
        
        this.feed.insertBefore(item, this.feed.children[0]);
        if (this.feed.children.length > 5) {
            this.feed.removeChild(this.feed.lastChild);
        }

        this.index = (this.index + 1) % this.sentences.length;
        this.typeOut(this.sentences[this.index]);
    }

    start() {
        this.typeOut(this.sentences[this.index]);
    }
}

// Initialize everything
window.addEventListener('DOMContentLoaded', () => {
    const visualizer = new LandmarkVisualizer('landmark-canvas');
    visualizer.start();

    const metrics = new SystemMetrics();
    metrics.update();

    const feed = new TranslationFeed('translation-feed', 'current-translation');
    feed.start();
});
