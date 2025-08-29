'use client';

import styles from '../../styles/seats.module.css';

type Seat = {
  id: number;
  seatName: string;
  posX: number;
  posY: number;
  status: 'available' | 'hold' | 'reserved';
  holdUntil?: string | null;
};

export default function SeatGrid({
  seats,
  selected,
  onToggle,
}: {
  seats: Seat[];
  selected: number[];
  onToggle: (seat: Seat) => void;
}) {
  const formatHoldUntil = (iso?: string | null) => {
    if (!iso) return undefined;
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  return (
    <div className={styles.grid}>
      {seats.map((seat) => {
        const isSelected = selected.includes(seat.id);
        const title =
          seat.status === 'hold'
            ? `좌석 ${seat.seatName}\n상태: HOLD\n만료: ${formatHoldUntil(seat.holdUntil) ?? '미정'}`
            : `좌석 ${seat.seatName}\n상태: ${seat.status.toUpperCase()}`;

        return (
          <div
            key={seat.id}
            className={`
  ${styles.seat}
  ${seat.status === 'reserved' ? styles.reserved : ''}
  ${seat.status === 'hold' ? styles.hold : ''}
  ${seat.status === 'available' ? styles.available : ''}
  ${isSelected ? styles.selected : ''}
`}
            style={{
              gridColumn: Math.floor(seat.posX / 10) + 1,
              gridRow: Math.floor(seat.posY / 10) + 1,
            }}
            onClick={() => onToggle(seat)}
            // title={title}
            role="button"
            aria-pressed={isSelected}
            aria-label={`Seat ${seat.seatName} - ${seat.status}${isSelected ? ' - selected' : ''}`}
            data-status={seat.status}
            data-selected={isSelected ? 'true' : 'false'}
          >
            {seat.seatName}
          </div>
        );
      })}
    </div>
  );
}
