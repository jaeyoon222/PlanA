'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';

export default function CheckinResultPage() {
  const { token } = useParams();
  const [message, setMessage] = useState('QR 확인 중...');
  const [status, setStatus] = useState<'success' | 'fail' | 'loading'>('loading');

  useEffect(() => {
    if (!token) return;

    fetch(`http://43.201.178.143:8080/api/checkin/${token}`)
      .then(async (res) => {
        const text = await res.text();
        if (res.ok) {
          setStatus('success');
          setMessage(text);
        } else {
          setStatus('fail');
          setMessage(text);
        }
      })
      .catch(() => {
        setStatus('fail');
        setMessage('QR 확인 중 오류가 발생했습니다.');
      });
  }, [token]);

  return (
    <main className="min-h-screen flex items-center justify-center bg-cover bg-center"
      style={{ backgroundImage: "url('/bg-study.png')" }}>
      <div className="text-center p-8 rounded-xl backdrop-blur-md bg-white/10 border border-white/30 text-white shadow-xl">
        <h1 className="text-3xl font-bold mb-4">입장 결과</h1>
        <p className={`text-xl font-semibold ${status === 'success' ? 'text-green-300' : 'text-red-300'}`}>
          {message}
        </p>
      </div>
    </main>
  );
}
