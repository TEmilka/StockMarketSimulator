# Build the app
FROM node:18-alpine as build

# Set working directory and copy package files
WORKDIR /app
COPY package.json package-lock.json ./

# Install dependencies and build the project
RUN npm install
COPY . .
RUN npm run build

# Serve the app with Nginx
FROM nginx:alpine

# Copy the built files from the previous stage
COPY . .

# Expose port 80
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]