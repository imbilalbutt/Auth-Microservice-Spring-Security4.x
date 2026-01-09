pwd
docker-compose -f docker-compose.yml down
docker build -t auth-service:v1.0 .
docker-compose -f docker-compose.yml up -d