'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiFetch } from '@/lib/api';

type Zone = {
  id: number;
  zoneName: string;
  description: string;
  latitude: number;
  longitude: number;
};

declare global {
  interface Window {
    kakao: any;
  }
}

export default function ZoneListPage() {
  const [zones, setZones] = useState<Zone[]>([]);
  const [selectedZone, setSelectedZone] = useState<Zone | null>(null);
  const router = useRouter();
  const mapRef = useRef<HTMLDivElement>(null);

  // 지점 목록 불러오기
  useEffect(() => {
    const fetchZones = async () => {
      try {
        const data = await apiFetch('/zones');
        console.log('✅ 지점 목록 로딩 성공:', data);
        setZones(data);
      } catch (err) {
        console.error('❌ 지점 목록 로딩 실패:', err);
      }
    };
    fetchZones();
  }, []);

  // 지도 로딩 로직
  useEffect(() => {
    console.log('📌 selectedZone 변경됨:', selectedZone);

    if (!selectedZone) return;

    if (!mapRef.current) {
      console.warn('❗ mapRef.current가 null입니다.');
      return;
    }

    console.log('🧩 mapRef.current:', mapRef.current);

    const loadMap = () => {
      const container = mapRef.current;
      if (!container) {
        console.warn('❗ map container 없음');
        return;
      }

      console.log('🗺️ 지도 생성 시작');

      const options = {
        center: new window.kakao.maps.LatLng(selectedZone.latitude, selectedZone.longitude),
        level: 3,
      };

      const map = new window.kakao.maps.Map(container, options);

      console.log('📍 지도 객체 생성됨:', map);

      new window.kakao.maps.Marker({
        position: new window.kakao.maps.LatLng(selectedZone.latitude, selectedZone.longitude),
        map,
      });

      setTimeout(() => {
        console.log('🔁 지도 relayout + setCenter 호출');
        map.relayout();
        map.setCenter(new window.kakao.maps.LatLng(selectedZone.latitude, selectedZone.longitude));
      }, 100);
    };

    if (window.kakao?.maps) {
      console.log('✅ Kakao Maps SDK 이미 로드됨');
      requestAnimationFrame(() => {
        setTimeout(loadMap, 100);
      });
    } else {
      console.log('📦 Kakao Maps SDK 로딩 시작');
      const script = document.createElement('script');
      script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${process.env.NEXT_PUBLIC_KAKAO_KEY}&autoload=false`;
      script.onload = () => {
        console.log('✅ Kakao Maps SDK 로드 완료, 지도 초기화 시작');
        window.kakao.maps.load(() => {
          setTimeout(loadMap, 100);
        });
      };
      script.onerror = () => {
        console.error('❌ Kakao Maps SDK 로딩 실패');
      };
      document.head.appendChild(script);
    }
  }, [selectedZone]);

  return (
    <main
      className="min-h-screen bg-cover bg-center flex items-center justify-center px-4"
      style={{ backgroundImage: "url('/bg-study.png')" }}
    >
      <div className="max-w-3xl w-full space-y-8 text-white">
        <h1 className="text-4xl font-bold text-center">지점 선택</h1>

        <ul className="space-y-4">
          {zones.map((zone) => (
            <li
              key={zone.id}
              onClick={() => {
                console.log('🖱️ 지점 선택됨:', zone);
                setSelectedZone(zone);
              }}
              className="p-6 rounded-xl cursor-pointer bg-white/10 border border-white/30 backdrop-blur-md shadow-md text-white transition-all duration-200 transform hover:scale-105 hover:shadow-xl"
            >
              <h2 className="text-2xl font-semibold mb-2">{zone.zoneName}</h2>
              <p className="text-white/80">{zone.description}</p>
            </li>
          ))}
        </ul>
      </div>

      {/* 지도 모달 */}
{selectedZone && (
  <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
    <div
      key={selectedZone.id}
      className="bg-white/10 backdrop-blur-md border border-white/30 shadow-lg rounded-2xl p-6 max-w-md w-full text-white transition-all duration-300"
    >
      <h2 className="text-2xl font-bold text-center mb-4">
        {selectedZone.zoneName}점
      </h2>

      <div
        ref={mapRef}
        key={selectedZone.id}
        className="w-full h-64 rounded-lg border border-white/20 mb-5 shadow-inner"
      />

      <p className="text-sm text-center text-white/80 mb-6">
         {selectedZone.zoneName}점에서 예약하시겠습니까?
      </p>

      <div className="flex gap-3">
        <button
          onClick={() => {
            console.log('❌ 선택 취소됨');
            setSelectedZone(null);
          }}
          className="w-1/2 py-2 rounded-lg bg-white/20 hover:bg-white/30 text-white transition"
        >
          취소
        </button>
        <button
          onClick={() => {
            console.log('➡️ 예약 페이지로 이동:', selectedZone.id);
            router.push(`/zones/${selectedZone.id}`);
          }}
          className="w-1/2 py-2 rounded-lg bg-purple-600 hover:bg-purple-700 text-white font-semibold transition"
        >
          예약하기
        </button>
      </div>
    </div>
  </div>
)}

    </main>
  );
}
