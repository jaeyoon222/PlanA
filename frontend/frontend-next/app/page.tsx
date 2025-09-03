'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';

// ✅ 공통 스타일 변수 정의
const glassBg = 'bg-white/10';
const glassBorder = 'border border-white/30';
const glassText = 'text-white';
const glassShadow = 'shadow-lg';
const glassBlur = 'backdrop-blur-md';
const glassBase = `${glassBg} ${glassBorder} ${glassShadow} ${glassBlur}`;

type Branch = { name: string; zoneId: number };
type Parsed = {
  date: string | null;
  startTime: string | null;
  endTime: string | null;
  partySize: number | null;
  seatTags: string[];
  branch: string | null;
};

type SeatDto = {
  id: number;
  seatName: string;
  posX: number;
  posY: number;
  price: number;
  zoneId: number | null;
  status: 'available' | 'reserved' | 'hold';
  holdUntil?: string | null;
  windowSide?: boolean;
};

export default function Home() {
  const [message, setMessage] = useState('');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [parsed, setParsed] = useState<Parsed | null>(null);
  const [seats, setSeats] = useState<SeatDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [branches, setBranches] = useState<Branch[]>([]);
  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);

  const router = useRouter();

useEffect(() => {
  const fetchBranches = async () => {
    try {
      const response = await fetch('http://43.201.178.143:8080/api/branches');

      if (!response.ok) {
        throw new Error(`지점 정보를 불러오지 못했습니다. (HTTP ${response.status})`);
      }

      const data = await response.json();

      if (!Array.isArray(data)) {
        throw new Error('지점 데이터 형식이 올바르지 않습니다.');
      }

      setBranches(data);
    } catch (error) {
      console.error('[지점 불러오기 실패]', error);
      // Optional: 사용자에게 보여줄 에러 상태 설정
      // setError('지점 정보를 불러오는 데 실패했습니다.');
    }
  };

  fetchBranches();
}, []);

  useEffect(() => {
    fetch('http://43.201.178.143:8080/api/public/hello')
      .then(res => res.ok ? res.text() : Promise.reject(res.status))
      .then(setMessage)
      .catch(console.error);
  }, []);

  const startISO = useMemo(() => {
    if (!parsed?.date || !parsed?.startTime) return null;
    return `${parsed.date}T${parsed.startTime}`;
  }, [parsed]);

  const endISO = useMemo(() => {
    if (!parsed?.date || !parsed?.endTime) return null;
    return `${parsed.date}T${parsed.endTime}`;
  }, [parsed]);

  const handleParse = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    setParsed(null);
    setSeats(null);

    const token = localStorage.getItem('accessToken');
  if (!token) {
    setError('로그인이 필요합니다.');
    router.push('/login');
    return;
  }

    try {
      const res = await fetch('http://43.201.178.143:8080/api/nlu/parse-and-list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: query }),
      });

      if (!res.ok) {
      console.error('[파싱 실패]', await res.text());  // ← 콘솔에 디버깅 용도
      throw new Error('올바르게 입력해주세요. 예: "내일 오후 2~4시 강남점 창가 조용한 자리"');
}
      const data = await res.json();

      setParsed(data.parsed);
      setSeats(data.seats || []);

      const zoneId = matchBranchToZoneId(data.parsed?.branch || '', branches);
      if (zoneId !== null) setSelectedZoneId(zoneId);

      const localStartISO = data.parsed?.date && data.parsed?.startTime
        ? `${data.parsed.date}T${data.parsed.startTime}` : null;
      const localEndISO = data.parsed?.date && data.parsed?.endTime
        ? `${data.parsed.date}T${data.parsed.endTime}` : null;

      if (
        Array.isArray(data.seats) &&
        data.seats.length > 0 &&
        zoneId !== null &&
        localStartISO &&
        localEndISO
      ) {
        const count = data.parsed?.partySize ?? 1;
        const availableSeats: SeatDto[] = data.seats.filter((seat: SeatDto) => seat.status === 'available');
        const seatsToHold: SeatDto[] = availableSeats.slice(0, count);

        if (seatsToHold.length < count) {
          setError(`예약 가능한 좌석이 ${count}개보다 적습니다.`);
          return;
        }

        const token = localStorage.getItem('accessToken');
        const holdErrors: string[] = [];

        for (const seat of seatsToHold) {
          const res = await fetch('http://43.201.178.143:8080/api/seats/hold', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              ...(token && { Authorization: `Bearer ${token}` }),
            },
            body: JSON.stringify({
              seatId: seat.id,
              startTime: localStartISO,
              endTime: localEndISO,
            }),
          });

          if (!res.ok) {
            holdErrors.push(await res.text());
          }
        }

        if (holdErrors.length > 0) {
          setError('좌석 홀딩 중 일부 실패: ' + holdErrors.join(', '));
          return;
        }

        const seatIdsParam = seatsToHold.map((seat) => seat.id).join(',');
        router.push(`/zones/${zoneId}?seatId=${seatIdsParam}&startTime=${localStartISO}&endTime=${localEndISO}`);
      } else if (!localStartISO || !localEndISO) {
        setError('시간 정보를 파싱하지 못했습니다. 예: "내일 오후 2~4시"처럼 입력해 보세요.');
      } else {
        setError('조건에 맞는 좌석이 모두 예약되었습니다.');
      }
    } catch (e: any) {
      setError(e?.message || '파싱에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const goToLogin = () => router.push('/login');
  const goToZoneSelect = () => router.push('/zones');
  const goToMypage = () => router.push('/mypage');

  return (
    <main
      className="relative flex flex-col items-center justify-center min-h-screen bg-cover bg-center"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <div className="absolute inset-0 bg-purple-400/60 backdrop-blur-sm z-0" />
      <div className="relative z-10 w-full max-w-2xl px-4">
        <h1 className="text-6xl font-bold text-white mb-8 text-center  font-serif">Plan A</h1>

        {/* 🔍 검색창 */}
        <div className={`mb-5 flex items-center rounded-full px-10 py-4 focus-within:ring-2 focus-within:ring-white/50 transition-all ${glassBase}`}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleParse(); }}
            className="flex-1 text-xl bg-transparent focus:outline-none text-white placeholder-white/70"
            placeholder="예) 내일 오후 2~4시 강남점 창가 조용한 자리"
          />
          <button
            onClick={handleParse}
            disabled={loading}
            className={` px-5 py-2 text-h1 relative -translate-y-0.5 rounded-full transition-all z-10 ${glassBase} ${glassText}`}
          >
            {loading ? '처리 중...' : '예약 요청'}
          </button>
        </div>

        {error && <p className="text-red-100 text-lg mb-4">{error}</p>}

        {parsed && (
          <div className={`w-full rounded-lg p-4 shadow-md mb-6 ${glassBase}`}>
            <div className="text-base text-white font-medium">파싱 결과</div>
            <div className="mt-2 flex flex-wrap gap-2">
              {parsed.date && Chip(`날짜: ${parsed.date}`)}
              {parsed.startTime && Chip(`시작: ${parsed.startTime}`)}
              {parsed.endTime && Chip(`종료: ${parsed.endTime}`)}
              {parsed.partySize ? Chip(`인원: ${parsed.partySize}명`) : null}
              {parsed.branch ? Chip(`지점: ${parsed.branch}`) : null}
              {parsed.seatTags?.map((t) => Chip(`태그: ${t}`))}
            </div>
          </div>
        )}

        {/* 💧 버튼들 */}
        <div className="flex justify-center gap-4 mt-6">
          {[
            { label: '로그인하기', onClick: goToLogin },
            { label: '좌석 예약', onClick: goToZoneSelect },
            { label: '마이페이지', onClick: goToMypage },
          ].map(({ label, onClick }) => (
            <button
              key={label}
              onClick={onClick}
              className={`relative px-4 py-2 text-h1 font-normar rounded-full overflow-hidden before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/40 before:to-transparent before:rounded-full before:blur-sm before:opacity-30 hover:before:opacity-50 hover:shadow-xl transition-all ${glassBase} ${glassText}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
    </main>
  );
}

function matchBranchToZoneId(nluBranch: string, branches: Branch[]): number | null {
  const key = normalize(nluBranch);
  const exact = branches.find(b => normalize(b.name) === key);
  if (exact) return exact.zoneId;
  const part = branches.find(b => normalize(b.name).includes(key) || key.includes(normalize(b.name)));
  return part ? part.zoneId : null;
}

function normalize(s: string) {
  return s.replace(/점$/, '').replace(/\s+/g, '').toLowerCase();
}

function Chip(label: string) {
  return (
    <span
      key={label}
      className={`text-sm px-4 py-1.5 rounded-full ${glassBase} ${glassText}`}
    >
      {label}
    </span>
  );
}
