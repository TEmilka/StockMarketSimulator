# Build the app
FROM node:18-alpine as build

# Set working directory and copy package files
WORKDIR /app
COPY package.json package-lock.json ./

RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY config/default.conf /etc/nginx/conf.d/

# Expose port 80
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]
