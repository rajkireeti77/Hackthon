FROM mcr.microsoft.com/playwright:v1.52.0-jammy

WORKDIR /app

COPY package.json ./
RUN npm install

COPY server.js ./

EXPOSE 8002

CMD ["npm", "start"]
