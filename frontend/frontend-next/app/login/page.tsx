'use client';

import { useEffect, useState } from 'react';
import { loginUser, setTokens, getMyInfo } from '@/lib/api';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';

const GOOGLE_OAUTH = 'https://dairy-palm-newest-revelation.trycloudflare.com';  // 👈 Cloudflare Tunnel 주소
const DEFAULT_OAUTH = 'http://43.201.178.143:8080';  // 👈 기존 EC2 주소

export default function LoginPage() {
  const router = useRouter();
  const [tab, setTab] = useState<'local' | 'social'>('local');
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

const [redirecting, setRedirecting] = useState(true);

useEffect(() => {
  const q = new URLSearchParams(window.location.search);
  const accessToken = q.get('accessToken');
  const refreshToken = q.get('refreshToken');

  if (accessToken) {
    setTokens(accessToken, refreshToken || undefined);

    // 👉 히스토리에서 토큰 제거
    const clean = window.location.origin + '/login';
    window.history.replaceState({}, '', clean);

    // 👉 사용자 정보 조회 후 리디렉션 처리
    getMyInfo()
      .then((data) => {
        if (!data.phone) {
          toast('정보를 입력 후 이용해주세요.');
          router.replace("/mypage/edit");
        } else {
          router.replace('/');
        }
      })
      .catch(() => {
        router.replace('/login'); // 오류 시 로그인으로
      });
  } else {
    setRedirecting(false); // 일반 로그인 UI 보여주기
  }
}, [router]);

if (redirecting) return null; // 또는 로딩 스피너

const startSocial = (provider: 'kakao' | 'google' | 'naver') => {
  const base = provider === 'google' ? GOOGLE_OAUTH : DEFAULT_OAUTH;
  window.location.href = `${base}/oauth2/authorization/${provider}`;
};

const onSubmitLocal = async (e: React.FormEvent) => {
  e.preventDefault();
  setErr(null);

  if (!userId.trim()) {
    setErr('아이디(또는 이메일)를 입력해주세요.');
    return;
  }

  const isEmail = userId.includes('@');
  if (isEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userId)) {
    setErr('올바른 이메일 형식을 입력해주세요.');
    return;
  }

  setLoading(true);
  try {
    await loginUser(userId, password);
    toast.success('로그인 성공!');
    setTimeout(() => {
      router.replace('/');
    }, 1000);
  } catch (e: any) {
    // ✅ 정확하게 메시지 추출
    const msg =
    e?.message || e?.data?.message || e?.data?.error || '로그인 중 오류가 발생했습니다.';
    toast.error(msg);   // ✅ toast로 표시
    setErr(msg);        // ✅ input 아래에도 표시
  } finally {
    setLoading(false);
  }
};

  return (
    <main
      className="min-h-screen bg-cover bg-center flex items-center justify-center"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <div className="max-w-lg w-full p-10 rounded-xl border border-white/30 bg-white/10 backdrop-blur-md shadow-xl text-white space-y-6">
        <h1 className="text-3xl font-bold text-center">로그인</h1>

        <div className="grid grid-cols-2 border border-white/30 rounded overflow-hidden text-white text-center text-sm">
          <button
            className={`p-3 transition-all ${tab === 'local' ? 'bg-white/20 font-semibold' : ''}`}
            onClick={() => setTab('local')}
          >
            이메일 로그인
          </button>
          <button
            className={`p-3 transition-all ${tab === 'social' ? 'bg-white/20 font-semibold' : ''}`}
            onClick={() => setTab('social')}
          >
            소셜 로그인
          </button>
        </div>

        {tab === 'local' ? (
          <form className="space-y-4" onSubmit={onSubmitLocal}>
            <div>
              <label className="block text-sm mb-1">아이디(또는 이메일)</label>
              <input
  value={userId}
  onChange={(e) => setUserId(e.target.value)}
  className="
    w-full rounded p-3 bg-white/20 placeholder-white/70 text-white
    focus:outline-none focus:scale-105 focus:ring-2 focus:ring-white/50
    transition-all duration-200
  "
  placeholder="your_id_or_email"
  required
/>
            </div>
            <div>
              <label className="block text-sm mb-1">비밀번호</label>
              <input
  type="password"
  value={password}
  onChange={(e) => setPassword(e.target.value)}
  className="
    w-full rounded p-3 bg-white/20 placeholder-white/70 text-white
    focus:outline-none focus:scale-105 focus:ring-2 focus:ring-white/50
    transition-all duration-200
  "
  placeholder="••••••••"
  required
/>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full p-3 rounded bg-white/20 hover:bg-white/30 transition-all"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>

            <p className="text-sm text-center text-white/80">
              아직 계정이 없으신가요?{' '}
              <a className="underline text-white" href="/register">
                회원가입
              </a>
            </p>
          </form>
        ) : (
          <div className="space-y-3">
            <button
              className="w-full p-3 rounded bg-white/20 hover:bg-white/30 text-white transition-all"
              onClick={() => startSocial('kakao')}
            >
              카카오로 시작
            </button>
            <button
              className="w-full p-3 rounded bg-white/20 hover:bg-white/30 text-white transition-all"
              onClick={() => startSocial('google')}
            >
              구글로 시작
            </button>
            <button
              className="w-full p-3 rounded bg-white/20 hover:bg-white/30 text-white transition-all"
              onClick={() => startSocial('naver')}
            >
              네이버로 시작
            </button>
          </div>
        )}
      </div>
    </main>
  );
}
