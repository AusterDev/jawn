FROM node:22.12.0-alpine AS deps
WORKDIR /app

COPY package.json ./
RUN npm install

FROM node:20-alpine AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NODE_ENV=production
RUN npm run build

FROM node:22.12.0-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
USER node

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/node_modules ./node_modules

EXPOSE 4321
ENV PORT=4321
ENV HOST=0.0.0.0

CMD ["node", "./dist/server/entry.mjs"]