# 🤖 OfflineAssistant for Android

OfflineAssistant is a fully offline, private AI assistant for Android. It runs a Large Language Model (LLM) entirely on your device's CPU using `llama.cpp` and integrates deeply with native Android APIs to perform real-world tasks without an internet connection.

## ✨ Features
- **100% Offline AI:** Runs GGUF models directly on-device ensuring total privacy.
- **System Integrations:** The AI can set alarms, save notes, turn on the flashlight, make calls, and send texts.
- **Voice Control:** Built-in Speech-to-Text (STT) and Text-to-Speech (TTS) for hands-free usage.
- **Background Access:** Includes a Home Screen Widget, a persistent notification for "Quick Replies", and Quick Settings Tiles.

---

## 🛠️ Step-by-Step Setup Guide (From Scratch)

This guide assumes you are starting from a completely fresh Windows or Mac computer.

### Step 1: Install Prerequisites
1. **Download Android Studio:** Go to [developer.android.com/studio](https://developer.android.com/studio) and install the latest version for your OS (Windows or Mac).
2. **Install Git:** 
   - **Windows:** Download from [git-scm.com](https://git-scm.com/download/win).
   - **Mac:** Open Terminal and type `git --version` (this prompts you to install Apple's command line tools if missing).

### Step 2: Clone the Repositories
The project requires the Android source code and the `llama.cpp` C++ engine. Open your Terminal (Mac) or Command Prompt/PowerShell (Windows) and run:

```bash
# 1. Create a parent folder to hold everything
mkdir OfflineAssistantWorkspace
cd OfflineAssistantWorkspace

# 2. Clone this Android project repository
# (Replace the URL below with your actual GitHub repository URL once uploaded)
git clone https://github.com/YOUR_USERNAME/OfflineAssistant.git

# 3. Clone the llama.cpp engine RIGHT NEXT TO the Android project folder
git clone https://github.com/ggml-org/llama.cpp.git llama_cpp
```

*Note: Your folder structure MUST look exactly like this for the C++ compiler to find the engine:*
```text
OfflineAssistantWorkspace/
├── OfflineAssistant/   <-- The Android project
└── llama_cpp/          <-- The C++ engine
```

### Step 3: Open in Android Studio & Install SDK Tools
1. Open **Android Studio**.
2. Click **Open** and select the `OfflineAssistant` folder you just cloned.
3. Once the project opens, go to the top menu: **Tools → SDK Manager**.
4. In the **SDK Platforms** tab, ensure **Android API 34 or 35** is checked.
5. Switch to the **SDK Tools** tab. You MUST check and install these exact tools:
   - ✅ **NDK (Side by side)** (Expand the checkbox and select version `27.1.12297006`)
   - ✅ **CMake** (Expand and select version `3.31.6`)
   - ✅ **Android SDK Build-Tools**
6. Click **Apply** and let Android Studio download them.
7. Click the **"Sync Project with Gradle Files"** button (the elephant icon in the top right) and wait for it to finish successfully.

### Step 4: Prepare a Device (Emulator or Physical)
**Option A: Android Emulator (Mac/Windows)**
1. Go to **Tools → Device Manager** in Android Studio.
2. Click **Create Virtual Device** (+). Select **Pixel 7** -> Next.
3. Select an **API 34 or 35 (x86_64)** system image -> Next.
4. Click **Show Advanced Settings**. Scroll down to Memory and set **RAM to 4096 MB** (4 GB). *Do not skip this or the AI model will freeze the emulator.*
5. Click Finish and press the ▶️ Play button to start it.

**Option B: Physical Android Device (Recommended)**
1. On your phone, go to **Settings → About Phone** and tap **Build Number** 7 times to unlock Developer Mode.
2. Go to **Settings → Developer Options** and enable **USB Debugging**.
3. Plug the phone into your computer and tap **Allow** on the phone screen.

### Step 5: Build and Run
1. At the top of Android Studio, ensure your device/emulator is selected in the dropdown.
2. Click the green **▶️ Run** button (or press `Shift+F10`).
3. The first build takes 2-5 minutes as it compiles the C++ engine.
4. When the app opens on your device, **Accept all permissions** (Microphone, Notifications).

---

## 🧠 Step 6: Loading the AI Model

Because this is completely offline, you need to provide the AI brain (a GGUF file).

1. Open your computer's web browser and download this lightweight model: 
   [Qwen2.5-0.5B-Instruct-GGUF (Q4_K_M)](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf) *(~398 MB)*.
2. **Transfer to Device:**
   - *If using Emulator:* Drag and drop the `.gguf` file from your computer directly onto the emulator window.
   - *If using Physical Phone:* Copy the file via USB into your phone's `Downloads` folder.
3. Open the OfflineAssistant app.
4. Tap the **Folder Icon** in the top right corner.
5. Navigate to your device's `Downloads` folder and select the `.gguf` file.
6. Wait for the status to say **"Model Ready"**.

**You are done!** You can now type `make a note to buy milk`, `set an alarm for 7am`, or `write a poem about android`!

---

## 🚀 Pushing this code to your own GitHub

If you are the developer and want to push this to GitHub, run these commands in the terminal from the `OfflineAssistant` project root:

```bash
# 1. Initialize git (if not already done)
git init

# 2. Add all project files (The .gitignore file automatically excludes heavy/build files)
git add .

# 3. Commit the code
git commit -m "Initial commit of OfflineAssistant with llama.cpp integration"

# 4. Link to your GitHub repository (replace URL with yours)
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/OfflineAssistant.git

# 5. Push!
git push -u origin main
```
