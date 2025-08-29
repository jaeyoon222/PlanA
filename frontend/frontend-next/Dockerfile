# 1단계: 빌드
FROM node:20-alpine AS builder
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

# 2단계: 실행
FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app ./
EXPOSE 3000

# 수정된 부분: -H 0.0.0.0 추가
CMD ["npx", "next", "start", "-H", "0.0.0.0"]
