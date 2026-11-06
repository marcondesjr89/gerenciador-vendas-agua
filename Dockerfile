# Stage 1: Build da aplicacao
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copia os descritores do Maven e wrapper para cache eficiente de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia o codigo-fonte e compila gerando o executavel (pulando testes no build da imagem)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime enxuto e seguro
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criacao de usuario sem privilegios de root para execucao segura em producao
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia o artefato compilado a partir do estagio de build
COPY --from=builder /workspace/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
