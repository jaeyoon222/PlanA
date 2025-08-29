  'use client';

  import { useCallback, useEffect, useMemo, useState } from 'react';
  import { useParams, useRouter } from 'next/navigation';
  import { apiFetch } from '@/lib/api';
  import { getUserId } from '@/lib/auth';
  import SeatGrid from './SeatGrid';
  import { Client, IMessage } from '@stomp/stompjs';
  import SockJS from 'sockjs-client';
  import { useSearchParams } from 'next/navigation';
  import { DayPicker } from 'react-day-picker';
  import 'react-day-picker/dist/style.css';

  import TimePicker from 'react-time-picker';
  import 'react-time-picker/dist/TimePicker.css';
  import 'react-clock/dist/Clock.css';
  import { toast } from "react-hot-toast";
  import { getMyInfo } from '@/lib/api';

  type Seat = {
    id: number;
    seatName: string;
    posX: number;
    posY: number;
    status: 'available' | 'hold' | 'reserved';
    holdUntil?: string | null; // 서버 SeatDto에 맞춰 추가
  };

  type SeatEvent = {
    seatIds: number[];
    status: 'available' | 'hold' | 'reserved';
    holdUntil?: string | null;   // 서버가 보낼 수 있음
    byUserId?: number | null;    // 서버 이벤트의 주체
    eventType?: 'hold' | 'release' | 'expire' | string; // 서버에서 "hold"/"release" 전송
  };

  export default function SeatPage() {
    const params = useParams();
    const router = useRouter();

    // ============== 공통 유틸 ==============
const fmtLocal = (date: Date, time: string) => {
  const d = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
  return `${d}T${time}:00`;
};
    // ============== zoneId 파싱 ==============
    const zoneIdStr = useMemo(() => {
      const raw = (params?.zoneId ?? '') as string | string[];
      return Array.isArray(raw) ? raw[0] : raw;
    }, [params]);

    const query = useSearchParams();

  const seatIdFromQuery = query.get('seatId');
  const startTimeFromQuery = query.get('startTime');
  const endTimeFromQuery = query.get('endTime');


    const zoneIdNum = useMemo(() => {
      const n = Number(zoneIdStr);
      return Number.isFinite(n) && n > 0 ? n : null;
    }, [zoneIdStr]);

    // ============== 상태 ==============
    const [seats, setSeats] = useState<Seat[]>([]);
    const [selected, setSelected] = useState<number[]>([]);
    const [userId, setUserId] = useState<string | null>(null);

    const today = new Date();
  const [selectedDate, setSelectedDate] = useState<Date>(today);
    const [startTime, setStartTime] = useState('09:00');
    const [endTime, setEndTime] = useState('10:00');

    const calculateTotalPrice = () => {
      const sh = Number(startTime.split(':')[0]);
      const eh = Number(endTime.split(':')[0]);
      const duration = Math.max(0, eh - sh);
      return selected.length * duration * 100;
    };

    const selectedSeats = seats.filter((s) => selected.includes(s.id));
  const seatNames = selectedSeats.map((s) => s.seatName).join(', ');

    // ============== 인증 가드 ==============
    useEffect(() => {
      const uid = getUserId();
      if (!uid) {
        toast.error("로그인 후 이용해주세요.", { id: "auth-required" });
          setTimeout(() => (window.location.href = "/login"), 700);
      } else {
        setUserId(uid);
      }
    }, [router]);

    // ============== zoneId 가드 ==============
    useEffect(() => {
      if (zoneIdNum === null) {
        alert('올바르지 않은 zoneId 입니다.');
        router.push('/zones');
      }
    }, [zoneIdNum, router]);

    // ============== 좌석 재조회 함수 (핵심) ==============
    const reloadSeats = useCallback(async () => {
      if (!userId || zoneIdNum === null){
        console.log('[reloadSeats] userId 또는 zoneIdNum 없음');
        return
      } ;
      const start = fmtLocal(selectedDate, startTime);
      const end = fmtLocal(selectedDate, endTime);
      console.log(`[reloadSeats] 요청 시작: zoneId=${zoneIdNum}, ${start} ~ ${end}`);

      const data = await apiFetch(
        `/api/seats?zoneId=${zoneIdNum}&startTime=${encodeURIComponent(start)}&endTime=${encodeURIComponent(end)}`
      );
      console.log('[reloadSeats] 응답:', data);

      const reservedSeatIds: number[] = data.reservedSeatIds ? Array.from(data.reservedSeatIds) : [];
      const holdingSeatIds: number[] = data.holdingSeatIds ? Array.from(data.holdingSeatIds) : [];
      const holdingByMeSeatIds: number[] = data.holdingByMeSeatIds ? Array.from(data.holdingByMeSeatIds) : [];

      const updated: Seat[] = (data.seats || []).map((s: any) => {
        // 서버 SeatDto: id/seatName/posX/posY/status/holdUntil
        const status: Seat['status'] = s.status;
        return {
          id: s.id,
          seatName: s.seatName,
          posX: s.posX,
          posY: s.posY,
          status,
          holdUntil: s.holdUntil ?? null,
        };
      });

      setSeats(updated);
      // 시간대 기준 내 홀드 상태만 선택 유지
      setSelected(holdingByMeSeatIds);
    }, [userId, zoneIdNum, selectedDate, startTime, endTime]);
    
    //아임포트 결제
    useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://cdn.iamport.kr/js/iamport.payment-1.2.0.js';
    script.async = true;
    document.body.appendChild(script);
  }, []);

    // ============== 초기 로딩/파라미터 변경 시 재조회 ==============
    useEffect(() => {
      reloadSeats().catch((err: any) => {
        console.error('좌석 불러오기 실패:', err?.message || err);
      });
    }, [reloadSeats]);

    // ============== STOMP 구독 (만료 이벤트 포함 즉시 반영) ==============
    useEffect(() => {
      if (zoneIdNum === null) return;

      const WS_URL =
        process.env.NEXT_PUBLIC_WS_URL ??
        `${process.env.NEXT_PUBLIC_API_ORIGIN ?? 'http://14.37.8.141:8080'}/ws-seat`;

      const client = new Client({
        webSocketFactory: () => new SockJS(WS_URL) as any,
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: (m) => console.log('[WebSocket]', m),
        onConnect: () => {
          console.log('[WebSocket] 연결 성공');
          client.subscribe(`/topic/seats/${zoneIdNum}`, (msg: IMessage) => {
            try {
              const payload = JSON.parse(msg.body || '{}') as SeatEvent;
              const { seatIds = [], status, byUserId, holdUntil, eventType } = payload;
              if (!Array.isArray(seatIds) || !status) return;

              // 1) 좌석 상태 즉시 반영 (+ holdUntil 같이 반영)
              setSeats((prev) =>
                prev.map((s) =>
                  seatIds.includes(s.id)
                    ? { ...s, status, holdUntil: holdUntil ?? (status === 'hold' ? s.holdUntil ?? null : null) }
                    : s
                )
              );

              // 2) 선택 목록 동기화 규칙
              // - available(해제/만료)로 바뀌면 선택 해제
              // - hold로 바뀌었는데 byUserId가 "나"이면 선택에 추가(중복 방지)
              setSelected((prev) => {
                let next = prev;
                if (status === 'available') {
                  next = prev.filter((id) => !seatIds.includes(id));
                } else if (status === 'hold' && byUserId && userId && String(byUserId) === String(userId)) {
                  const set = new Set(next);
                  seatIds.forEach((id) => set.add(id));
                  next = Array.from(set);
                }
                return next;
              });
            } catch (e) {
              console.error('WS 메시지 파싱 실패:', e);
            }
          });

          // 연결 직후 한 번 동기화 (혹시 누락된 상태 보정)
          reloadSeats().catch(() => {});
        },
        onStompError: (f) => {
          console.error('[WebSocket] STOMP 에러:', f.headers['message']);
        },
        onWebSocketClose: (e) => {
          console.warn('[WebSocket] 닫힘:', e.code, e.reason);
        },
      });

      client.activate();
      return () => {
        try {
          client.deactivate();
        } catch {}
      };
    }, [zoneIdNum, reloadSeats, userId]);

    // ============== 폴링 + 탭 복귀 동기화 (백업, 핵심) ==============
    useEffect(() => {
      if (zoneIdNum === null || !userId) return;

      const iv = setInterval(() => {
        reloadSeats().catch(() => {});
      }, 15000); // 10~15초 권장

      const onVis = () => {
        if (document.visibilityState === 'visible') {
          reloadSeats().catch(() => {});
        }
      };
      document.addEventListener('visibilitychange', onVis);

      return () => {
        clearInterval(iv);
        document.removeEventListener('visibilitychange', onVis);
      };
    }, [zoneIdNum, userId, reloadSeats]);

    // ============== 클릭 핸들러 ==============
    const toggleSeat = async (seat: Seat) => {
      console.log(`[toggleSeat] 클릭된 좌석:`, seat);

      if (!userId || zoneIdNum === null){
        console.log('[toggleSeat] userId 또는 zoneIdNum 없음');
        return;
      }

      const startLocal = fmtLocal(selectedDate, startTime);
      const endLocal = fmtLocal(selectedDate, endTime);
      console.log(`[toggleSeat] 시간: ${startLocal} ~ ${endLocal}`);

          const startDate = new Date(startLocal);
  if (startDate < new Date()) {
    toast.error("이미 지난 시간은 예약할 수 없습니다.");
    return;
  }

      const isMineSelected = selected.includes(seat.id);
      try {
        if (seat.status === 'available') {
          await apiFetch('/api/seats/hold', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ seatId: seat.id, startTime: startLocal, endTime: endLocal }),
          });
          // 서버/WS가 곧 상태 반영하지만 즉시 UX 보정
          setSelected((prev) => (prev.includes(seat.id) ? prev : [...prev, seat.id]));
          // 서버 기준 정합성 확보
          reloadSeats().catch(() => {});
          } else if (seat.status === 'reserved') {
            toast.error("다른 사용자가 사용 중 입니다.");
        } else if (seat.status === 'hold' && isMineSelected) {
          await apiFetch('/api/seats/release', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ seatId: seat.id }),
          });
          setSelected((prev) => prev.filter((id) => id !== seat.id));
          reloadSeats().catch(() => {});
        } else {
          // 다른 사용자가 홀드 중인 좌석 클릭 시
          toast.error("다른 사용자가 예약 중  입니다.");
        }
      } catch (err: any) {
        alert(err?.message || '좌석 처리 실패');
      }
    };

    // ============== 예약 ==============
    const reserveSeats = async () => {
      const startLocal = fmtLocal(selectedDate, startTime);
      const endLocal = fmtLocal(selectedDate, endTime);

      const startDate = new Date(startLocal);
      const endDate = new Date(endLocal);

      if (startDate < new Date()) {
    toast.error("이미 지난 시간은 예약할 수 없습니다.");
    return;
  }

      if (startDate >= endDate) {
        toast.error("종료시간은 시작시간보다 이후여야 합니다.");
        return;
      }
      if (selected.length === 0) {
        toast.error("좌석을 선택하세요.");
        return;
      }

      try {
        await apiFetch('/api/reserve', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            seatIds: selected,
            startTime: startLocal,
            endTime: endLocal,
          }),
        });
        toast.success("예약이 완료 되었습니다.");
        setSelected([]);
        router.replace('/');
      } catch (err: any) {
        console.error('예약 실패:', err?.message || err);
        toast.error(err?.message ||"예약 중 오류가 발생했습니다.");
      }
    };
  // ✅ 반드시 이 상태들이 선언되어 있어야 합니다
  const [naturalDateText, setNaturalDateText] = useState<string | null>(null);
  const [naturalDurationText, setNaturalDurationText] = useState<string | null>(null);

  useEffect(() => {
    const seatIdsFromQuery = query.get('seatId')?.split(',').map(Number).filter(n => !isNaN(n)) ?? [];
    const startRaw = startTimeFromQuery;
    const endRaw = endTimeFromQuery;

    if (seatIdsFromQuery.length === 0 || !startRaw || !endRaw) return;

    const startDate = new Date(startRaw);
    const endDate = new Date(endRaw);
    if (startDate >= endDate) return;

    // ✅ 날짜와 시간 설정
    setSelectedDate(startDate);
    setStartTime(startDate.toTimeString().slice(0, 5));
    setEndTime(endDate.toTimeString().slice(0, 5));

    // ✅ 자연어 날짜 텍스트: 8월 19일 (화)
    const weekday = ['일', '월', '화', '수', '목', '금', '토'][startDate.getDay()];
    const month = startDate.getMonth() + 1;
    const date = startDate.getDate();
    setNaturalDateText(`${month}월 ${date}일 (${weekday})`);

    // ✅ 총 예약 시간 계산 (시:분)
    const diffMs = endDate.getTime() - startDate.getTime();
    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffMs <= 0) {
      setNaturalDurationText('시간 설정 오류');
    } else if (minutes === 0) {
      setNaturalDurationText(`${hours}시간`);
    } else {
      setNaturalDurationText(`${hours}시간 ${minutes}분`);
    }

    // ✅ 자동 선택할 좌석 임시 저장
    sessionStorage.setItem('pendingSeatIds', JSON.stringify(seatIdsFromQuery));
  }, [query, startTimeFromQuery, endTimeFromQuery]);

 const requestPayment = async () => {
  if (!userId) {
    toast.error("로그인 후 이용해 주세요.");
    return;
  }

  // ✅ 전화번호 없는 경우 마이페이지로 이동
  try {
  const userInfo = await getMyInfo();

  // 🚫 전화번호가 비어 있는 경우
  if (!userInfo?.phone || userInfo.phone.trim() === '') {
    toast.error("전화번호 등록 후 이용해 주세요.");
    setTimeout(() => (window.location.href = "/mypage/edit"), 1000);
    return;
  }

} catch (err) {
  // 🔒 사용자 정보 자체를 못 불러온 경우 (ex. 로그인 만료, 토큰 오류 등)
  toast.error("로그인이 필요합니다. 다시 로그인해주세요.");
  setTimeout(() => (window.location.href = "/login"), 1000);
  return;
}

  const IMP = (window as any).IMP;
  if (!IMP) {
    alert('결제 모듈이 로드되지 않았습니다.');
    return;
  }

  const amount = calculateTotalPrice();
  const merchantUid = `mid_${userId}_${crypto.randomUUID()}`;

  const selectedSeatNames = seats
    .filter((s) => selected.includes(s.id))
    .map((s) => s.seatName);
  const seatNamesStr = selectedSeatNames.join(', ') || '없음';

  const dateStr = [
    selectedDate.getFullYear(),
    String(selectedDate.getMonth() + 1).padStart(2, '0'),
    String(selectedDate.getDate()).padStart(2, '0'),
  ].join('-');

  const startFull = `${dateStr}T${startTime}:00`;
  const endFull = `${dateStr}T${endTime}:00`;

  const startDate = new Date(startFull);
  const endDate = new Date(endFull);

  if (startDate < new Date()) {
    toast.error("이미 지난 시간은 결제할 수 없습니다.");
    return;
  }

  if (startDate >= endDate) {
    toast.error("종료 시간은 시작 시간보다 이후여야 합니다.");
    return;
  }

  if (selected.length === 0) {
    toast.error("좌석을 선택하세요.");
    return;
  }

  const totalHours =
    Number(endTime.split(':')[0]) - Number(startTime.split(':')[0]);

  const itemName = `스터디카페 좌석 예약 - ${seatNamesStr}`;
  const detailText = [
    `Plan A`,
    `- 지점: ${zoneName}`,
    `- 좌석: ${seatNamesStr}`,
    `- 시작: ${dateStr} ${startTime}`,
    `- 종료: ${dateStr} ${endTime}`,
    `- 총 시간: ${totalHours}시간`,
  ].join('\n');

  IMP.init('imp21428454');

  IMP.request_pay(
    {
      pg: 'html5_inicis',
      pay_method: 'card',
      merchant_uid: merchantUid,
      name: itemName,
      amount: amount,
      buyer_name: userId,
      custom_data: {
        detail: detailText,
        seatNames: seatNamesStr,
        startTime: startFull,
        endTime: endFull,
        seatIds: selected,
        zoneId: zoneIdNum,
      },
    },
    async function (rsp: any) {
      if (rsp.success) {
        try {
          await apiFetch('/api/payments/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              impUid: rsp.imp_uid,
              merchantUid: rsp.merchant_uid,
              userId: userId,
              seatIds: selected,
              startTime: startFull,
              endTime: endFull,
              amount: amount,
              zoneName: zoneName,
            }),
          });
          toast.success(`${detailText}\n 결제가 완료 되었습니다.`);
          setSelected([]); // 선택 초기화
          router.refresh(); // 페이지 새로고침
          setTimeout(() => (window.location.href = "/"), 2000);
        } catch (err) {
          console.error('예약 오류:', err);
        }
      }
    }
  );
};


  const [zoneName, setZoneName] = useState<string>('');

// zoneIdNum이 존재하면 API 호출
useEffect(() => {
  if (!zoneIdNum) return;
  apiFetch(`/api/zones/${zoneIdNum}`)
    .then((data) => {
      if (data?.zoneName) setZoneName(data.zoneName);
    })
    .catch(() => {
      toast.error("지점 정보를 불러오지 못했습니다.");
    });
}, [zoneIdNum]);

    // ============== UI ==============
  return (
  <div
    className="min-h-screen bg-cover bg-center px-20 py-20"
    style={{ backgroundImage: "url('/bg-study.png')" }}
  >
    <div className="relative z-10 max-w-7xl mx-auto w-full">
      <h1 className="text-3xl text-white font-bold mb-3 text-center">{zoneName}점</h1>
      <h4 className="text-h4 text-white font-bold mb-1 text-center">좌석 예약</h4>

      {/* ✅ 레이아웃 컨테이너 */}
      <div className="relative w-full min-h-[600px] ">
        {/* 📅 왼쪽: 날짜 & 시간 선택 (화면 왼쪽 고정) */}
        <div className="absolute -left-20 top-23 flex flex-col gap-6 text-white min-w-[100px]">
          
          {/* 시간 선택 */}
        <div className="flex flex-row gap-4 justify-center items-start">
    {/* 시작 시간 */}
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-white">시작 시간</label>
      <TimePicker
        onChange={(val) => val && setStartTime(val)}
        value={startTime}
        disableClock
        format="HH:mm"
        clearIcon={null}
      />
    </div>

    {/* 종료 시간 */}
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-white">종료 시간</label>
      <TimePicker
        onChange={(val) => val && setEndTime(val)}
        value={endTime}
        disableClock
        format="HH:mm"
        clearIcon={null}
      />
    </div>

    {/* 글로벌 스타일은 아래에 한 번만 작성 */}
    <style jsx global>{`
      .react-time-picker__wrapper {
        background: rgba(255, 255, 255, 0.08);
        border-radius: 9999px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        padding: 0.25rem 0.75rem;
        backdrop-filter: blur(10px);
        -webkit-backdrop-filter: blur(10px);
        box-shadow: 0 4px 12px rgba(255, 255, 255, 0.05);
      }

      .react-time-picker__inputGroup input {
        background: transparent;
        border: none;
        color: white;
        text-align: center;
        font-size: 16px;
        font-weight: 500;
        outline: none;
        width: 2.5rem;
      }

      .react-time-picker__inputGroup__divider {
        color: white;
        padding: 0 4px;
      }

      .react-time-picker__clear-button,
      .react-time-picker__clock-button {
        display: none;
      }
    `}</style>
  </div>
          {/* 날짜 선택 */}
    <div
    className="flex flex-col gap-2 items-center"
    style={{
      padding: '2rem',
      borderRadius: '24px',
      background: 'rgba(255, 255, 255, 0.03)',
      backdropFilter: 'blur(30px) saturate(180%)',
      WebkitBackdropFilter: 'blur(30px) saturate(180%)',
      border: '1px solid rgba(255, 255, 255, 0.2)',
      boxShadow: '0 12px 40px rgba(0, 0, 0, 0.2)',
      transition: 'all 0.3s ease',
      width: 'fit-content',
      margin: '0 auto',
    }}
  >
    <span className="text-sm font-medium">날짜 선택</span>
    <DayPicker
      mode="single"
      selected={selectedDate}
      onSelect={(date) => date && setSelectedDate(date)}
      disabled={{ before: new Date() }}
      modifiersClassNames={{
        selected: 'bg-purple-500 text-white rounded-full',
        today: 'text-purple-300 font-bold',
      }}
      styles={{
        caption: { color: 'white' },
        head_cell: { color: 'white' },
        day: { color: 'white' },
      }}
    />
  </div>
        </div>
        {/* 🪑 오른쪽: 좌석 박스 (화면 중앙 정렬) */}
        <div className="flex justify-center">
          <div className="flex flex-col w-[900px]">
            <SeatGrid seats={seats} selected={selected} onToggle={toggleSeat} />

            {/* 좌석 상태 설명 */}
            <div className="flex justify-center gap-4 text-white text-sm mb-4 mt-4">
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded border border-white/30 bg-white/20" />
                <span>빈 자리</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded border border-purple-400 bg-purple-400/30" />
                <span>예약 중</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded border-2 border-[rgba(167,139,250,0.5)] bg-[repeating-linear-gradient(45deg,rgba(167,139,250,0.15),rgba(167,139,250,0.15)_10px,rgba(255,255,255,0.05)_10px,rgba(255,255,255,0.05)_20px)] backdrop-blur-[6px]" />
                <span>예약 완료</span>
              </div>
            </div>

        <div className="absolute -right-6 top-90 w-80 p-6 rounded-2xl bg-white/10 backdrop-blur-sm text-white space-y-5 shadow-xl">
    {/* 좌석 정보 */}
    <div className="flex items-center gap-2 text-base font-semibold">
      <span>
        선택된 좌석:{' '}
        <span className={seatNames ? 'text-white' : 'text-white/50'}>
          {seatNames || '없음'}
        </span>
      </span>
    </div>

    {/* 예약 시간 */}
    <div className="flex items-center gap-2 text-base font-medium text-white/80">
      <span>
        총 {Number(endTime.split(':')[0]) - Number(startTime.split(':')[0])}시간 예약
      </span>
    </div>

    {/* 결제 금액 */}
    <div className="flex items-center gap-2 text-base font-semibold">
      <span>
        <span className="text-purple-400">총 결제 금액:</span>{' '}
        <span className="text-purple-500">
          {calculateTotalPrice().toLocaleString()}원
        </span>
      </span>
    </div>

    {/* 예약 버튼 */}
  <button
    onClick={requestPayment}
    disabled={selected.length === 0}
    className={`
      mt-2 w-full py-3 rounded-full font-semibold text-base text-white
      relative backdrop-blur-md transition-all overflow-hidden
      bg-white/10 border border-white/30 shadow-md
      before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/40 before:to-transparent
      before:rounded-full before:blur-sm before:opacity-0 hover:before:opacity-60
      hover:shadow-xl
      ${selected.length === 0 ? 'opacity-40 cursor-not-allowed' : ''}
    `}
  >
    예약하기
  </button>
  </div>
  </div>
        </div>
      </div>
    </div>
  </div>
  );

  }