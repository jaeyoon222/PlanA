// utils/auth.ts
export function getUserId(): string | null {
  if (typeof window === 'undefined') return null;
  const token = localStorage.getItem('accessToken');
  if (!token) return null;

  try {
    const payloadBase64 = token.split('.')[1];
    const decodedPayload = JSON.parse(atob(payloadBase64));
    return decodedPayload.userId || decodedPayload.sub || null;
  } catch (err) {
    console.error('토큰 디코딩 실패:', err);
    return null;
  }
}

export function isLoggedIn() {
  return !!getUserId();
}
