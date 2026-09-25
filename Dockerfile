# --- Stage 1: build frontend (Vite) → /fe/dist ---
FROM node:22-bookworm AS frontend
WORKDIR /fe
COPY frontend-rework/thirteencards/package.json frontend-rework/thirteencards/package-lock.json ./
RUN npm ci
COPY frontend-rework/thirteencards/ ./
RUN npm run build

# --- Stage 2: build backend (JAR) ---
FROM eclipse-temurin:25-jdk-jammy AS backend
WORKDIR /build
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn ./.mvn
RUN ./mvnw -B -DskipTests dependency:go-offline || true
COPY backend/ ./
COPY --from=frontend /fe/dist ./src/main/resources/static/
# Maven toolchains:
RUN mkdir -p /root/.m2 \
 && printf '%s\n' \
  '<?xml version="1.0" encoding="UTF-8"?>' \
  '<toolchains>' \
  '  <toolchain>' \
  '    <type>jdk</type>' \
  '    <provides><version>25</version></provides>' \
  '    <configuration><jdkHome>/opt/java/openjdk</jdkHome></configuration>' \
  '  </toolchain>' \
  '</toolchains>' \
  > /root/.m2/toolchains.xml

RUN ./mvnw -B -DskipTests clean package

# --- Stage 3: runtime ---
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=backend /build/target/backend-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]