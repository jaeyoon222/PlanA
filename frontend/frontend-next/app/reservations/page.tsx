"use client";

import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { apiFetch } from "@/lib/api";

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
  createdAt: number; // ✅ epoch millis
}

export default function ReservationListPage() {
  const [payments, setPayments] = useState<PaymentDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);

  // ✅ 한국 시간대 포맷터
  const fmtKST = new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: "Asia/Seoul",
  });

  const safeFormatMs = (ms?: number) =>
    typeof ms === "number" && !Number.isNaN(ms) ? fmtKST.format(new Date(ms)) : "-";

  useEffect(() => {
    apiFetch("/payments/me")
      .then((data: PaymentDetail[]) => {
        // ✅ createdAt 기준 최신순
        const sorted = [...data].sort(
          (a, b) => (b.createdAt ?? 0) - (a.createdAt ?? 0)
        );
        setPayments(sorted);
      })
      .catch((err) => {
        toast.error("예약 내역을 불러올 수 없습니다.");
        console.error(err);
      })
      .finally(() => setLoading(false));
  }, []);

const requestCancel = async (impUid: string) => {
  setIsCancelling(true);
  try {
    const res = await fetch("http://43.201.178.143:8080/api/payments/cancel", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ impUid }),
    });

    const text = await res.text();
    if (text === "SUCCESS") {
      toast.success("환불이 완료되었습니다.");
      setPayments((prev) =>
        prev.map((p) => (p.impUid === impUid ? { ...p, status: "CANCEL" } : p))
      );
    } else {
      toast.error("환불에 실패했습니다.");
    }
  } catch (err) {
    console.error(err);
    toast.error("서버 오류가 발생했습니다.");
  } finally {
    setIsCancelling(false);
  }
};

const handleCancel = (impUid: string) => {
  toast.custom((t) => (
    <div className="bg-white text-black rounded-lg shadow-lg p-4 w-72 space-y-3">
      <p className="font-semibold text-center">정말로 환불하시겠습니까?</p>
      <div className="flex justify-center gap-4">
        <button
          onClick={() => toast.dismiss(t.id)}
          className="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300 text-sm"
        >
          아니오
        </button>
        <button
          onClick={() => {
            toast.dismiss(t.id);
            requestCancel(impUid);
          }}
          className="px-3 py-1 rounded bg-red-500 hover:bg-red-600 text-white text-sm"
        >
          예
        </button>
      </div>
    </div>
  ));
};


  return (
    <main
      className="min-h-screen bg-cover bg-center px-4 py-10"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <button
    onClick={() => (window.location.href = '/')}
    className="absolute top-6 right-6 bg-white/20 hover:bg-white/30 text-white rounded px-4 py-2 transition"
  >
    홈으로
  </button>
  
      <div className="max-w-3xl mx-auto bg-white/10 text-white p-6 rounded-xl border border-white/30 backdrop-blur-md shadow-xl">
        <h1 className="text-3xl font-bold mb-6 text-center">나의 예약 내역</h1>

        {loading ? (
          <p className="text-white text-center mt-10">불러오는 중...</p>
        ) : payments.length === 0 ? (
          <p className="text-white text-center mt-10">예약 내역이 없습니다.</p>
        ) : (
          <ul className="space-y-4">
            {payments.map((p) => (
              <li
                key={p.merchantUid}
                className="border border-white/20 rounded-lg p-4 bg-white/5"
              >
                <h1 className="text-xl font-bold mb-2">{p.zoneName}점 {safeFormatMs(p.createdAt)}</h1>
                <p><strong>좌석:</strong> {p.seatNames}</p>
                <p><strong>시작:</strong> {fmtKST.format(new Date(p.startTime))}</p>
                <p><strong>종료:</strong> {fmtKST.format(new Date(p.endTime))}</p>
                <p><strong>이용 시간:</strong> {p.totalMinutes}분</p>
                <p><strong>결제 금액:</strong> {p.amount.toLocaleString()}원</p>
                <p><strong>상태:</strong> {p.status === "PAID" ? "결제 완료" : "취소됨"}</p>
                

                {p.status === "PAID" && (() => {
  const startMs = new Date(p.startTime).getTime();
  const now = Date.now();
  const diffMs = startMs - now;
  const canCancel = diffMs > 30 * 60 * 1000; // ✅ 30분 이상 남았으면 환불 가능

  return (
    <button
      onClick={() => handleCancel(p.impUid)}
      disabled={isCancelling || !canCancel}
      className={`
        mt-1 w-20 py-1 rounded-full font-semibold text-base text-white
        relative backdrop-blur-md transition-all overflow-hidden
        bg-white/10 border border-white/30 shadow-md
        before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/40 before:to-transparent
        before:rounded-full before:blur-sm before:opacity-0 hover:before:opacity-60
        hover:shadow-xl
        ${isCancelling || !canCancel ? "opacity-40 cursor-not-allowed" : ""}
      `}
    >
      {isCancelling ? "처리 중..." : canCancel ? "환불" : "환불 불가"}
    </button>
  );
})()}
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
