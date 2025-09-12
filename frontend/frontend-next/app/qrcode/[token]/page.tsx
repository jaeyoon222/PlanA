'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';

export default function QrViewPage() {
  const { token } = useParams();
  const [qrBase64, setQrBase64] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    const fetchQr = async () => {
      try {
        const res = await fetch(`http://43.201.178.143:8080/api/qr/${token}`);
        const base64 = await res.text();
        setQrBase64(base64); // ⬅️ base64 문자열을 그대로 넣음
      } catch (err) {
        console.error('QR 불러오기 실패:', err);
      }
    };

    fetchQr();
  }, [token]);

  return (
    <main
      className="min-h-screen flex flex-col items-center justify-center bg-cover bg-center px-4"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <div className="text-center backdrop-blur-md bg-white/10 border border-white/30 p-8 rounded-xl text-white shadow-xl max-w-sm w-full">
        <h1 className="text-2xl font-bold mb-4">입장용 QR 코드</h1>
        <p className="text-sm mb-4">QR을 스캔해 입장해주세요.</p>

        {qrBase64 ? (
          <img src={qrBase64} alt="QR Code" className="w-full h-auto rounded bg-white p-2" />
        ) : (
          <p>QR을 불러오는 중...</p>
        )}
      </div>
    </main>
  );
}
