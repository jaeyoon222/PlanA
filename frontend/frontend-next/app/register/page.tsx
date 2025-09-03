'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { toast } from 'react-hot-toast';
import { ApiError } from '@/lib/api';

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || 'http://43.201.178.143:8080/api';

async function uploadImage(file: File) {
  console.log('[uploadImage] 업로드 시작. 전달된 file:', file);
  const form = new FormData();
  form.append('file', file);
  for (const [key, value] of form.entries()) {
    console.log(`[uploadImage] form field: ${key}`, value);
  }

  const res = await fetch(`${API_BASE}/upload`, {
    method: 'POST',
    body: form,
    credentials: 'include',
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '(파싱 실패)');
    console.error('[uploadImage] 응답 실패:', res.status, text);
    throw new Error('업로드 실패');
  }

  const data = await res.json();
  console.log('[uploadImage] 업로드 완료. 응답 데이터:', data);
  return data.url as string;
}

async function sendVerificationCode(phone: string) {
  const res = await fetch(`${API_BASE}/send-code`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone }),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('인증번호 전송 실패');
}

async function registerUser(user: any, code: string) {
  const url = `${API_BASE}/register?code=${code}`;
  const payload = {
    email: user.userId,
    password: user.password,
    name: user.name,
    nickname: user.nickname,
    birth: user.birth || undefined,
    phone: user.phone,
    address: user.address,
    profileImage: user.profileImage || undefined,
  };

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    credentials: 'include',
  });

  if (!res.ok) {
    const data = await res.json().catch(() => null);
    const message = data?.message || '회원가입에 실패했습니다.';
    throw new ApiError(message, res.status, data);  // ✅ 수정
  }

  const data = await res.json().catch(() => null);
  return data;
}


export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    userId: '',
    password: '',
    name: '',
    nickname: '',
    birth: '',
    phone: '',
    address: '',
    profileImage: '',
  });
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string>('');
  const [verificationCode, setVerificationCode] = useState('');
  const [cooldown, setCooldown] = useState(0);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    console.log(`[handleChange] ${name} 입력값 변경됨 →`, value); // 🔍 필드 변경 로그
    setForm({ ...form, [name]: value });
  };

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] || null;
    console.log('[handleFile] 선택된 파일:', f);
    setFile(f);
    setPreview(f ? URL.createObjectURL(f) : '');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      console.log('[handleSubmit] 🔔 회원가입 제출 시작');
      console.log('[handleSubmit] 현재 입력된 폼 데이터:', form);
      console.log('[handleSubmit] 비밀번호 확인:', form.password); // 🔍 비번 확인
      console.log('[handleSubmit] 현재 입력된 인증 코드:', verificationCode);

      if (!verificationCode.trim()) {
        toast.error('인증번호를 입력해주세요.');
        return;
      }

      let imageUrl = form.profileImage;

      if (file) {
        console.log('[handleSubmit] 선택된 프로필 이미지 파일:', file);
        if (!file.type.startsWith('image/')) {
          toast.error('이미지 파일만 업로드해주세요.');
          return;
        }
        if (file.size > 5 * 1024 * 1024) {
          toast.error('이미지는 최대 5MB까지 가능합니다.');
          return;
        }
        imageUrl = await uploadImage(file);
        console.log('[handleSubmit] 업로드된 이미지 URL:', imageUrl);
      }

      const profileImageToSend =
        imageUrl && imageUrl !== 'null' && imageUrl !== '0' ? imageUrl : undefined;

      const finalPayload = { ...form, profileImage: profileImageToSend };

      console.log('[handleSubmit] 최종 회원가입 요청 데이터:', finalPayload);
      console.log('[handleSubmit] 요청 인증 코드:', verificationCode);

      await registerUser(finalPayload, verificationCode);

      toast.success('회원가입 성공!');
      router.push('/login');
    } catch (err: any) {
      console.error('[handleSubmit] ❌ 에러 발생:', err);
      toast.error(err.message || '회원가입 실패!');
    }
  };

  const startCooldown = (seconds: number) => {
    setCooldown(seconds);
    const interval = setInterval(() => {
      setCooldown(prev => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSendCode = async () => {
    if (!form.phone) {
      toast.error('전화번호를 입력해주세요.');
      return;
    }
    try {
      await sendVerificationCode(form.phone);
      toast.success('인증번호가 전송되었습니다.');
      startCooldown(300); // 5분
    } catch {
      toast.error('인증번호 전송 실패');
    }
  };

  return (
    <main
      className="min-h-screen bg-cover bg-center flex items-center justify-center"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg p-10 rounded-xl border border-white/30 bg-white/10 backdrop-blur-md shadow-xl text-white space-y-4"
      >
        <h1 className="text-3xl font-bold text-center">회원가입</h1>

        {preview && (
          <div className="flex justify-center">
            <img src={preview} alt="미리보기" className="w-24 h-24 rounded-full border-2 border-white/30 object-cover" />
          </div>
        )}

        <input
          type="file"
          accept="image/*"
          onChange={handleFile}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70"
        />
        <input name="userId" placeholder="이메일" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input type="password" name="password" placeholder="비밀번호" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input name="name" placeholder="이름" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input name="nickname" placeholder="닉네임" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input name="address" placeholder="주소" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input name="birth" placeholder="생일 (예: 19990101)" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />
        <input name="phone" placeholder="전화번호" onChange={handleChange}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />

        <button
          type="button"
          onClick={handleSendCode}
          disabled={cooldown > 0}
          className={`w-full p-2 rounded text-white font-semibold transition-all
            ${cooldown > 0 ? 'bg-gray-500/40 cursor-not-allowed' : 'bg-indigo-500/40 hover:bg-indigo-500/60'}`}
        >
          {cooldown > 0
            ? `재전송 ${Math.floor(cooldown / 60)}:${(cooldown % 60).toString().padStart(2, '0')}`
            : '인증번호 요청'}
        </button>

        <input name="code" placeholder="인증번호" value={verificationCode} onChange={(e) => setVerificationCode(e.target.value)}
          className="w-full p-3 rounded bg-white/20 text-white placeholder-white/70" />

        <button type="submit"
          className="w-full p-3 rounded bg-purple-300/40 hover:bg-purple-300/50 transition-all text-white font-semibold">
          회원가입
        </button>

        <p className="text-sm text-center text-white/80">
          이미 계정이 있으신가요? <a className="underline text-white" href="/login">로그인</a>
        </p>
      </form>
    </main>
  );
}
