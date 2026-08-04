FROM python:3.12-slim

WORKDIR /app
COPY mock_oidc.py cert.pem key.pem ./
RUN pip install --no-cache-dir cryptography==43.0.1

EXPOSE 8085
CMD ["python", "mock_oidc.py"]
