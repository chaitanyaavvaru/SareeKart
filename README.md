# SareeKart

SareeKart is a full-stack ecommerce app for premium sarees. The frontend is a
React/Vite storefront and admin dashboard, and the backend is a Spring Boot API
with MySQL, JWT auth, product, cart, order, payment, WhatsApp, and AI assistant
services.

## Project Structure

- `frontend/` - React 19, Vite, Tailwind, Redux Toolkit, Playwright tests
- `backend/backend/` - Spring Boot 3 API, JPA, Spring Security, Maven
- `docker-compose.yml` - MySQL, backend, and frontend containers
- `manage.sh` - local helper for starting, stopping, and checking services

## Local Development

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173` and proxies `/api` requests to the
backend at `http://127.0.0.1:8081`.

Start the backend:

```bash
cd backend/backend
./mvnw spring-boot:run
```

The backend expects MySQL on `localhost:3306` with:

- database: `sareekart_db`
- user: `root`
- password: `root123`

Online payments require a real Razorpay key pair. The app intentionally does
not include gateway credentials in source control. Configure matching test
credentials locally before starting the backend:

```bash
export RAZORPAY_KEY_ID='rzp_test_your_key_id'
export RAZORPAY_KEY_SECRET='your_test_key_secret'
cd backend/backend
./mvnw spring-boot:run
```

Use both values from the same Razorpay account and mode. Do not mix a live key
with a test secret. For Docker, pass the same two variables to `docker compose`:

```bash
RAZORPAY_KEY_ID='rzp_test_your_key_id' \
RAZORPAY_KEY_SECRET='your_test_key_secret' \
docker compose up --build
```

You can also use the helper script from the repository root:

```bash
./manage.sh start
./manage.sh status
./manage.sh stop
```

## Docker

Run the complete stack:

```bash
docker compose up --build
```

The frontend container serves the app on `http://localhost`, and proxies API
traffic to the backend container.

## Verification

Frontend production build:

```bash
cd frontend
npm run build
```

Backend tests:

```bash
cd backend/backend
./mvnw test
```

Backend tests use an isolated in-memory H2 database through the `test` profile,
so they do not require a local MySQL server.
