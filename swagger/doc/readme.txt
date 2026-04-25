Как пользоваться


i. Вытянуть готовый образ swagger-ui:

docker pull docker.swagger.io/swaggerapi/swagger-ui

1. Запустить проект на порту 8080.

2. В терминале перейти в каталог, где располагается spring.yaml.

Пример:
cd /home/michael/Documents/lottery

3. Запустить сервер Swagger на порту 8081 (чтобы не конфликтовал за порт с проектом).

sudo docker run --rm -p 8081:8080 \
  -e SWAGGER_JSON=/spec/spring.yaml \
  -v "$PWD":/spec \
  --name swaggerui swaggerapi/swagger-ui

4. В браузере http://localhost:8081/

5. Нажимать Try it out / Execute. См. изображения 1, 2, 3.
