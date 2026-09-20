# AquaNexus — Smart Water Reuse Analysis & CTO Compliance Monitoring

AquaNexus is a comprehensive, full-stack intelligence system designed for industrial water monitoring, wastewater recycle analysis, consent-to-operate (CTO) compliance tracking, and predictive AI analytics.

---

## 🏗️ Project Architecture & Tech Stack

The application is structured as a microservices architecture:

1. **Frontend (SPA)**: A modern, responsive single-page application (SPA) built with:
   - TailwindCSS (Styling)
   - Chart.js (Interactive data visualization)
   - Canvas Confetti (UX feedback)
   - Hash-based custom JS router (No-framework SPA)

2. **Backend Services (Spring Boot)**: Rest API service running on port `8080`:
   - Java 17 / Spring Boot 3.2.5
   - Spring Security & JSON Web Tokens (JWT) for secure authentication
   - Spring Data JPA with Hibernate
   - **Database**: MySQL 8.0 (default) with **H2 Database** fallback for zero-install testing.
   - **PDF Generation**: OpenPDF for generating corporate PDF reports.

3. **Machine Learning Microservice (FastAPI)**: ML prediction service running on port `5000`:
   - Python 3.8+ & FastAPI
   - Scikit-Learn, XGBoost, Pandas, NumPy
   - Provides water recycling yields and anomaly detection.

---

## 🛠️ Prerequisites

Make sure the following are installed and configured on your machine:
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Apache Maven**: Added to your system environment variables (`PATH`).
- **Python**: Version 3.8 or higher (with `pip`).
- **MySQL Server**: (Optional, if using MySQL) Running on port `3306`.

---

## 🚀 How to Run the Project (Step-by-Step)

To run the full project, you need to open **three terminal windows**.

### Step 1: Start the ML Microservice (Port 5000)
1. Open a terminal and navigate to the ML folder:
   ```cmd
   cd "backend/ml-service"
   ```
2. Install Python dependencies:
   ```cmd
   pip install -r requirements.txt
   ```
3. Run the ML server:
   ```cmd
   python app.py
   ```
   *The ML service will start running at `http://localhost:5000`.*

---

### Step 2: Start the Spring Boot Backend (Port 8080)
1. Open a second terminal and navigate to the backend folder:
   ```cmd
   cd "backend"
   ```
2. Run the application using Maven:
   ```cmd
   mvn spring-boot:run
   ```
   *The backend server will start running at `http://localhost:8080`.*

> [!TIP]
> **No MySQL installed? Switch to In-Memory Database (H2)**
> If you don't have MySQL running locally, open `backend/src/main/resources/application.properties` and swap out the database configurations with H2 by commenting out the MySQL lines and uncommenting/adding the following:
> ```properties
> # spring.datasource.url=jdbc:h2:mem:aquanexus;DB_CLOSE_DELAY=-1
> # spring.datasource.driverClassName=org.h2.Driver
> # spring.datasource.username=sa
> # spring.datasource.password=
> # spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
> ```

---

### Step 3: Serve the Frontend (Port 8000 or 5500)
Due to browser security restrictions (`CORS`), you cannot double-click the `index.html` file to run it. You must serve it through a local web server:

#### Option A: Using VS Code Live Server (Recommended)
1. Open the project folder in VS Code.
2. Install the **Live Server** extension.
3. Right-click `index.html` in the root folder and select **Open with Live Server**.
4. Access the UI at: `http://127.0.0.1:5500`

#### Option B: Using Python Web Server
1. Open a third terminal in the root project folder:
   ```cmd
   cd "final year project"
   ```
2. Run the built-in Python web server:
   ```cmd
   python -m http.server 8000
   ```
3. Open your browser and navigate to: `http://localhost:8000`

---

## 🔒 Verification & Authentication

- If the backend is running, you can register a new account on the **Register** page or sign in.
- **Offline / Local Mode**: If the Spring Boot backend is down, the frontend automatically degrades gracefully to **Offline Mode**. You can log in with any dummy credentials, and your data will be saved locally inside your browser's `LocalStorage`.

---

## ☁️ Deploying on Render (Step-by-Step)

AquaNexus includes a [`render.yaml`](./render.yaml) Blueprint and a production [`Dockerfile`](./Dockerfile) for 1-click cloud deployment.

### Method 1: Using Render Blueprint (Recommended)
1. Push this updated repository to your GitHub.
2. Go to your [Render Dashboard](https://dashboard.render.com).
3. Click **New +** $\rightarrow$ **Blueprint**.
4. Select your **`Final-year-project`** GitHub repository.
5. Render will detect `render.yaml` and create:
   - **`aquanexus-backend`**: Docker Web Service running the Java Spring Boot API.
   - **`aquanexus-frontend`**: Static Site serving the Vanilla JS SPA.
6. Click **Apply**.

### Method 2: Manual Web Service Setup on Render
1. **Backend Web Service**:
   - In Render, click **New +** $\rightarrow$ **Web Service**.
   - Connect your GitHub repository.
   - **Environment**: `Docker`
   - **Dockerfile Path**: `./Dockerfile`
   - **Environment Variables**:
     - `PORT`: `8080` (or leave default assigned by Render)
     - `CORS_ALLOWED_ORIGINS`: `https://*.onrender.com,http://localhost:3000`
2. **Frontend Static Site**:
   - In Render, click **New +** $\rightarrow$ **Static Site**.
   - **Publish Directory**: `.`
   - **Rewrite Rules**: Source `/*` $\rightarrow$ Destination `/index.html`

---

## 📤 Push Changes to GitHub

Run these commands in your PowerShell or terminal:

```bash
git add .
git commit -m "feat: complete alert & notification system, real-time data architecture, and render cloud deployment setup"
git push origin main
```

