# https://docs.docker.com/guides/angular
# as-of: 09/14/2026
# Development image with hot reload support
# =========================================

ARG NODE_VERSION=24.12.0-alpine

# Use a lightweight Node.js image
FROM node:${NODE_VERSION}

# Set the working directory inside the container
WORKDIR /app

# Copy package-related files first to leverage Docker's caching mechanism
COPY package.json *package-lock.json* ./

# Install project dependencies using npm ci (ensures a clean, reproducible install)
RUN --mount=type=cache,target=/root/.npm npm ci

# Install Angular CLI globally
RUN npm install -g @angular/cli

# Copy the rest of the application source code into the container
COPY . .

# Expose port 4200 for Angular dev server
EXPOSE 4200

# Start Angular development server with host flag to allow external connections
CMD ["ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--poll", "2000", "--disable-host-check"]