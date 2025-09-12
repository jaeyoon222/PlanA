'use client';

import { useEffect, useState } from 'react';
import { getMyInfo, updateUser, apiFetch } from '../../../lib/api';
import { toast } from 'react-hot-toast';
import { useRouter } from 'next/navigation';

export default function EditProfile() {
  const router = useRouter();
  const [form, setForm] = useState<any>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string>('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [isSocialUser, setIsSocialUser] = useState(false);
  const [cooldown, setCooldown] = useState(0); // ⏱️ 타이머 상태
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;

 useEffect(() => {
  getMyInfo()
    .then(data => {
      setForm(data);

      const profileImageUrl = data.profileImage
  ? data.profileImage.startsWith('http')
    ? data.profileImage // ✅ 절대 URL이면 그대로 사용
    : `http://43.201.178.143:8080${data.profileImage.startsWith("/") ? "" : "/"}${data.profileImage}`
  : "/default-profile.png";

      setPreview(profileImageUrl);
      setIsSocialUser(data.provider !== 'local');
    })
    .catch(() => {
      toast.error("로그인 후 이용해주세요.");
      router.push('/login');
    });
}, []);

  // ⏱️ 타이머 감소
  useEffect(() => {
    if (cooldown === 0) return;
    const timer = setInterval(() => {
      setCooldown(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    setFile(f || null);
    if (f) setPreview(URL.createObjectURL(f));
  };

  const handleSendCode = async () => {
    if (!form?.phone) {
      toast.error("전화번호를 입력해주세요.");
      return;
    }

    try {
      await apiFetch('/send-code', {
        method: 'POST',
        body: JSON.stringify({ phone: form.phone }),
        headers: { 'Content-Type': 'application/json' },
      });
      toast.success("인증번호가 전송되었습니다.");
      setCooldown(300); // 5분 타이머 시작
    } catch {
      toast.error("인증번호 전송 실패");
    }
  };

  const sanitizePayload = (raw: any) => {
  const sanitized: any = {};

  for (const key in raw) {
    const value = raw[key];

    // 빈 문자열, null, undefined 제거
    if (value !== '' && value !== null && value !== undefined) {
      sanitized[key] = value;
    }
  }

  // birth가 문자열이면 yyyy-MM-dd 형식으로 변환
  if (typeof sanitized.birth === 'string') {
    try {
      const date = new Date(sanitized.birth);
      const yyyyMMdd = date.toISOString().split('T')[0]; // '2025-09-02'
      sanitized.birth = yyyyMMdd;
    } catch {
      delete sanitized.birth;
    }
  }

  return sanitized;
};
function extractErrorMessage(error: any): string {
  if (typeof error === 'string') return error;

  if (error instanceof Error) return error.message;

  return (
    error?.data?.message ||
    error?.response?.data?.message ||
    error?.message ||
    '예기치 않은 오류가 발생했습니다.'
  );
}


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isSocialUser) {
      if (!currentPassword) {
        toast.error("현재 비밀번호를 입력해주세요.");
        return;
      }
      if (newPassword && newPassword !== confirmPassword) {
        toast.error("새 비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    try {
      let imageUrl = form.profileImage;

      if (file) {
        const formData = new FormData();
        formData.append('file', file);
        const uploadResponse = await apiFetch('/upload', {
          method: 'POST',
          body: formData,
        });
        imageUrl = uploadResponse.url;
      }

      const payload: any = {
  ...form,
  profileImage: imageUrl,
};

if (!isSocialUser) {
  payload.currentPassword = currentPassword;
  if (newPassword) payload.newPassword = newPassword;
} else {
  if (verificationCode) payload.verificationCode = verificationCode;
}

const cleanedPayload = sanitizePayload(payload);
await updateUser(cleanedPayload);

      toast.success("정보가 수정되었습니다.");
      router.push('/mypage');
    } catch (err: any) {
  const msg = extractErrorMessage(err);

  if (msg.includes('인증번호') || msg.includes('verification')) {
    toast.error("인증번호가 일치하지 않습니다. 다시 입력해 주세요.");
    setVerificationCode('');
  } else {
    toast.error(msg);
  }
}
  };

  if (!form) return <p className="text-white text-center mt-10">로딩중...</p>;

  return (
    <main className="min-h-screen flex items-center justify-center bg-cover bg-center"
          style={{ backgroundImage: "url('/bg-study.png')" }}>
      <form onSubmit={handleSubmit}
            className="w-full max-w-lg p-8 bg-white/10 backdrop-blur-md border border-white/30 rounded-xl text-white space-y-4 shadow-xl">
        <h1 className="text-3xl font-bold text-center">정보 수정</h1>

        {preview && (
          <div className="flex justify-center">
            <img src={preview} alt="미리보기" className="w-24 h-24 rounded-full border-2 object-cover" />
          </div>
        )}

        <input type="file" accept="image/*" onChange={handleFile}
               className="w-full p-2 bg-white/20 rounded" />

        <input name="name" value={form?.name ?? ''} onChange={handleChange} placeholder="이름"
               className="w-full p-3 rounded bg-white/20" />
        <input name="nickname" value={form?.nickname ?? ''} onChange={handleChange} placeholder="닉네임"
               className="w-full p-3 rounded bg-white/20" />
        <input type="birth" name="birth" value={form?.birth ?? ''} onChange={handleChange} placeholder='생년월일(0000-00-00)'
              className="w-full p-3 rounded bg-white/20"/>
        <input name="address" value={form?.address ?? ''} onChange={handleChange} placeholder="주소"
               className="w-full p-3 rounded bg-white/20" />

        <hr className="border-white/30 my-2" />

        {isSocialUser ? (
          <>
            <div className="flex flex-col space-y-2">
              <input name="phone" value={form?.phone ?? ''} onChange={handleChange} placeholder="전화번호"
                     className="w-full p-3 rounded bg-white/20" />

              <input type="text" name="verificationCode" value={verificationCode}
                     onChange={(e) => setVerificationCode(e.target.value)} placeholder="인증번호 입력"
                     className="w-full p-3 rounded bg-white/20" />

              <button type="button" onClick={handleSendCode} disabled={!form?.phone || cooldown > 0}
                      className={`
                        w-full py-3 rounded font-semibold text-white
                        relative backdrop-blur-md transition-all overflow-hidden
                        bg-white/10 border border-white/30 shadow-md
                        before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/40 before:to-transparent
                        before:rounded-full before:blur-sm before:opacity-0 hover:before:opacity-60
                        hover:shadow-xl
                        ${(!form?.phone || cooldown > 0) ? 'opacity-40 cursor-not-allowed' : ''}
                      `}>
                {cooldown > 0
                  ? `재전송 ${Math.floor(cooldown / 60)}:${String(cooldown % 60).padStart(2, '0')}`
                  : '인증번호 전송'}
              </button>
            </div>
          </>
        ) : (
          <>
            <input type="password" name="currentPassword" value={currentPassword}
                   onChange={(e) => setCurrentPassword(e.target.value)} placeholder="현재 비밀번호 (필수)"
                   className="w-full p-3 rounded bg-white/20" />
            <input type="password" name="newPassword" value={newPassword}
                   onChange={(e) => setNewPassword(e.target.value)} placeholder="새 비밀번호 (선택)"
                   className="w-full p-3 rounded bg-white/20" />
            <input type="password" name="confirmPassword" value={confirmPassword}
                   onChange={(e) => setConfirmPassword(e.target.value)} placeholder="새 비밀번호 확인"
                   className="w-full p-3 rounded bg-white/20" />
          </>
        )}

        <button type="submit"
                className="w-full bg-purple-400/30 hover:bg-purple-400/50 p-3 rounded font-semibold">
          저장하기
        </button>
      </form>
    </main>
  );
}
