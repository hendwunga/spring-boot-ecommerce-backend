# === Build Stage ===
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src/main ./src/main

# Optional: skip tests biar build lebih cepat
RUN mvn clean package -DskipTests

# === Run Stage ===
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
EXPOSE 9898

# Import self-signed mock OIDC cert (dev only) ke truststore gabungan,
# sehingga app di container bisa memanggil https://oidc:8085 (issuer discovery).
COPY docker/oidc/cert.pem /tmp/oidc-cert.pem
RUN cp "$JAVA_HOME/lib/security/cacerts" /etc/ssl/certs/combined.jks \
    && keytool -importcert -noprompt -alias mock-oidc \
       -file /tmp/oidc-cert.pem \
       -keystore /etc/ssl/certs/combined.jks \
       -storepass changeit

# Copy jar dari build stage
COPY --from=build /app/target/*.jar app.jar

# Jalankan aplikasi dengan truststore gabungan (Stripe + mock OIDC)
ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/etc/ssl/certs/combined.jks -Djavax.net.ssl.trustStorePassword=changeit"
ENTRYPOINT ["java", "-jar", "app.jar"]
