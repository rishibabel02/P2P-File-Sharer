# DropFlo: P2P File Sharing Platform

## Project Overview
DropFlo is a secure, peer-to-peer (P2P) file sharing application. It consists of a Java backend for file serving and a modern Next.js/React frontend for a seamless user experience. Users can share files by generating invite codes (dynamic port numbers) and download files using those codes, enabling direct, serverless file transfers.

---

## Features
- Drag-and-drop file upload (frontend)
- File sharing via invite codes (dynamic port numbers)
- File downloading using invite codes
- Modern, responsive UI (Next.js, Tailwind CSS)
- Simple Java backend for direct P2P file serving
- No persistent storage: files are shared live, session-based

---

## Architecture
- **Backend:** Java 17+, Maven, custom HTTP server, dynamic port allocation for file serving
- **Frontend:** Next.js 14, React 18, TypeScript, Tailwind CSS
- **Communication:** Frontend proxies API requests to backend (see `ui/next.config.js` for rewrites)

---

## Backend Setup (Java)
### Prerequisites
- Java 17+
- Maven

### Build & Run
```bash
cd P2P-FIle-Sharing
mvn clean package
java -jar target/P2P-File-Sharing-1.0-SNAPSHOT.jar
```

### Main Files
- `src/main/java/p2p/App.java`: Entry point (prints Hello World, extend for server start)
- `src/main/java/p2p/controller/FileShareController.java`: (Stub) Controller for HTTP server and file sharing logic
- `src/main/java/p2p/service/FileSharer.java`: Core logic for offering files, starting file servers, and handling file transfers
- `src/main/java/p2p/utils/UploadUtils.java`: Utility for generating dynamic port codes
- `src/test/java/p2p/AppTest.java`: Basic JUnit test

### How P2P File Sharing Works (Backend)
- When a file is offered, a random dynamic port is generated and mapped to the file path
- A server socket is started on that port, waiting for a client connection
- When a client connects (using the invite code/port), the file is streamed directly to the client
- No files are stored on the backend; sharing is live and session-based

---

## Frontend Setup (Next.js/React)
### Prerequisites
- Node.js 18+
- npm

### Install & Run
```bash
cd ui
npm install
npm run dev
```
- Visit [http://localhost:3000](http://localhost:3000)

### Main Files
- `src/app/page.tsx`: Main UI logic, handles upload/download tabs, API calls
- `src/components/FileUpload.tsx`: Drag-and-drop upload component
- `src/components/FileDownload.tsx`: Download form using invite code
- `src/components/InviteCode.tsx`: Displays and copies invite code
- `src/app/layout.tsx`, `globals.css`: Layout and global styles
- `next.config.js`: Proxies `/api/upload` and `/api/download/:port` to backend

### How P2P File Sharing Works (Frontend)
- User uploads a file → receives an invite code (port)
- User shares invite code with peer
- Peer enters invite code to download file directly from the sharer

---

## Project Structure
```
P2P-FIle-Sharing/
├── pom.xml                # Maven config for backend
├── src/main/java/p2p/     # Java backend source
│   ├── App.java
│   ├── controller/
│   ├── service/
│   └── utils/
├── src/test/java/p2p/     # Java tests
├── target/                # Maven build output
├── ui/                    # Frontend (Next.js)
│   ├── package.json
│   ├── src/app/
│   ├── src/components/
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   └── ...
```

---

## Technologies Used
### Backend
- Java 17+
- Maven
- JUnit 5 (for tests)

### Frontend
- Next.js 14 (`next@14.2.28`)
- React 18 (`react@18.3.1`)
- TypeScript
- Tailwind CSS
- Axios (HTTP client)
- React Dropzone (drag-and-drop)
- React Icons

---

## Usage
1. **Start the backend** (see Backend Setup)
2. **Start the frontend** (see Frontend Setup)
3. **Share a file:**
   - Go to "Share a File" tab, upload a file, get an invite code
   - Share the invite code (port) with your peer
4. **Receive a file:**
   - Go to "Receive a File" tab, enter the invite code, download the file

---

## License
This project is licensed under the MIT License. 