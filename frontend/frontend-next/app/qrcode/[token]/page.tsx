'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';

export default function QrViewPage() {
  const { token } = useParams();
  const [qrUrl, setQrUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    // QR 이미지 URL 구성 (백엔드가 만든 PNG 파일 위치)
    setQrUrl(`/qr/${token}.png`);
  }, [token]);

  return (
    <main
      className="min-h-screen flex flex-col items-center justify-center bg-cover bg-center px-4"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <div className="text-center backdrop-blur-md bg-white/10 border border-white/30 p-8 rounded-xl text-white shadow-xl max-w-sm w-full">
        <h1 className="text-2xl font-bold mb-4">입장용 QR 코드</h1>
        <p className="text-sm mb-4">입구 단말기에 QR을 스캔해 입장해주세요.</p>

        {qrUrl ? (
          <img src={qrUrl} alt="QR Code" className="w-full h-auto rounded bg-white p-2" />
        ) : (
          <p>QR을 불러오는 중...</p>
        )}
      </div>
    </main>
  );
}