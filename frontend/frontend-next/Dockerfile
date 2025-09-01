# 1단계: 빌드
FROM node:20-alpine AS builder

WORKDIR /app
COPY . .

# 🔥 빌드 시점에 필요한 환경 변수 주입
ARG NEXT_PUBLIC_API_BASE
ARG NEXT_PUBLIC_OAUTH_BASE
ARG NEXT_PUBLIC_KAKAO_KEY

ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE
ENV NEXT_PUBLIC_OAUTH_BASE=$NEXT_PUBLIC_OAUTH_BASE
ENV NEXT_PUBLIC_KAKAO_KEY=$NEXT_PUBLIC_KAKAO_KEY

RUN npm install
RUN npm run build

# 2단계: 실행
FROM node:20-alpine

WORKDIR /app
COPY --from=builder /app ./

EXPOSE 3000

# ✅ 외부 접속 허용
CMD ["npx", "next", "start", "-H", "0.0.0.0"]
