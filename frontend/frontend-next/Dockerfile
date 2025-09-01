# 1단계: 빌드
FROM node:20-alpine AS builder

WORKDIR /app
COPY . .

# 🔥 빌드 시점에 API 주소 주입
ARG NEXT_PUBLIC_API_BASE
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE

RUN npm install
RUN npm run build

# 2단계: 실행
FROM node:20-alpine

WORKDIR /app
COPY --from=builder /app ./

# 실행 시 3000 포트 오픈
EXPOSE 3000

# ✅ 외부 접속 가능하도록 -H 0.0.0.0
CMD ["npx", "next", "start", "-H", "0.0.0.0"]
