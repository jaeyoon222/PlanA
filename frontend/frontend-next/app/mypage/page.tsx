"use client";

import { useEffect, useState } from "react";
import { getMyInfo, logoutUser, apiFetch } from "@/lib/api";
import { toast } from "react-hot-toast";

interface PaymentDetail {
  merchantUid: string;
  impUid: string;
  amount: number;
  seatNames: string;
  startTime: string;
  endTime: string;
  totalMinutes: number;
  status: string;
  zoneName: string;
  createdAt: number;
}

export default function MyPage() {
  const [user, setUser] = useState<any>(null);
  const [myPayments, setMyPayments] = useState<PaymentDetail[]>([]);

  useEffect(() => {
    getMyInfo()
      .then((data) => {
        setUser(data);

        apiFetch("/api/payments/me")
          .then((res: PaymentDetail[]) => {
            const sorted = [...res].sort((a, b) => (b.createdAt ?? 0) - (a.createdAt ?? 0));
            setMyPayments(sorted.slice(0, 3));
          })
          .catch(() => toast.error("예약 내역을 불러올 수 없습니다."));
      })
      .catch(() => {
        toast.error("로그인 후 이용해주세요.", { id: "auth-required" });
        setTimeout(() => (window.location.href = "/login"), 1000);
      });
  }, []);

  const handleLogout = async () => {
    try {
      await logoutUser();
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      sessionStorage.clear();
      toast.success("로그아웃 되었습니다.", { id: "logout-success" });
    } catch (e) {
      toast.error("로그아웃 중 오류가 발생했어요.");
    } finally {
      setTimeout(() => (window.location.href = "/"), 700);
    }
  };

  useEffect(() => {
    console.log("🧪 API BASE:", process.env.NEXT_PUBLIC_API_BASE);
    console.log("🧪 OAUTH BASE:", process.env.NEXT_PUBLIC_OAUTH_BASE); // 여기를 꼭 확인
  }, []);

  if (!user)
    return <p className="text-white text-center mt-10">로딩중...</p>;

  // 이미지 URL 확인용 콘솔 로그
  const profileImageUrl = user.profileImage
    ? `http://14.37.8.141:8080${user.profileImage.startsWith("/") ? "" : "/"}${user.profileImage}`
    : "/default-profile.png";

  console.log("🖼 프로필 이미지 URL:", profileImageUrl);

  return (
    <main
      className="min-h-screen bg-cover bg-center flex flex-col items-center justify-start py-20 px-4"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      {/* 좌우 배치: 마이페이지 | 최근 예약 */}
      <div className="w-full max-w-6xl flex flex-col lg:flex-row justify-between items-start gap-8 mt-10">

        {/* 마이페이지 */}
        <div className="w-full lg:w-1/2 bg-white/20 backdrop-blur-lg rounded-2xl shadow-lg p-8 text-white space-y-6 text-sm sm:text-base">
          <div className="flex flex-col items-center">
            <h1 className="text-2xl font-bold text-white mt-3 mb-5">마이페이지</h1>
            <img
              src={profileImageUrl}
              alt="프로필 이미지"
              className="w-24 h-24 rounded-full border-4 border-white/30 object-cover bg-white mb-2"
            />
          </div>

          <div className="space-y-1">
            <p><span className="font-bold">이름:</span> {user.name}</p>
            <p><span className="font-bold">닉네임:</span> {user.nickname}</p>
            <p><span className="font-bold">이메일:</span> {user.email}</p>
            <p><span className="font-bold">전화번호:</span> {user.phone}</p>
            <p><span className="font-bold">생년월일:</span> {user.birth || "미입력"}</p>
            <p><span className="font-bold">주소:</span> {user.address}</p>
          </div>

          <div className="grid grid-cols-3 gap-2 pt-4">
            <button className="bg-white/20 hover:bg-white/30 rounded py-2" onClick={() => window.location.href = "/mypage/edit"}>정보 수정</button>
            <button className="bg-blue-400/20 hover:bg-blue-400/30 rounded py-2" onClick={() => window.location.href = "/"}>홈으로</button>
            <button className="bg-red-400/20 hover:bg-red-400/30 rounded py-2" onClick={handleLogout}>로그아웃</button>
          </div>
        </div>

        {/* 최근 예약 내역 - 항상 표시됨 */}
        <div className="w-full lg:w-1/2 bg-white/10 p-6 rounded-xl border border-white/30 backdrop-blur-md shadow-md text-white min-h-[300px]">
          <h2 className="text-xl font-bold mb-4">최근 예약 내역</h2>

          {myPayments.length > 0 ? (
            <ul className="space-y-4">
              {myPayments.map((p) => (
                <li key={p.merchantUid} className="border border-white/20 rounded-lg p-4 bg-white/5">
                  <p><strong>{p.zoneName}점</strong> — {new Date(p.startTime).toLocaleString("ko-KR")}</p>
                  <p>좌석: {p.seatNames}</p>
                  <p>상태: {p.status === "PAID" ? "결제 완료" : "취소됨"}</p>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-white/70">예약 내역이 없습니다.</p>
          )}

          <div className="text-right mt-4">
            <button
              onClick={() => window.location.href = "/reservations"}
              className="underline hover:text-purple-300 transition"
            >
              전체 보기 →
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
