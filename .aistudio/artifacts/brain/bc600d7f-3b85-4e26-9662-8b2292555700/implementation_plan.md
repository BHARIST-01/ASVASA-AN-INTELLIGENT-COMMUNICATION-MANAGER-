# ASVASA — Offline-First ML Emergency Mesh & Voice Response System

ASVASA is a mission-critical, offline-capable Android emergency application designed for disaster relief and off-grid communications. It combines peer-to-peer Bluetooth discovery and text messaging, machine learning connection probability forecasting, hands-free and tap-to-talk voice dispatch with Gemini intelligence and Text-To-Speech (TTS), and a GPS-guided emergency assistance locator for nearest police, fire, and medical centers.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and user experience decisions have been synthesized from your answers and requirements:

- **Confirmed Official App Name**: **ASVASA** (derived from Sanskrit/Hindi for 'Reassurance', 'Solace', and 'Sanctuary').
- **Triple Voice Control Engine**:
  1. *Hands-free Continuous Voice Trigger* (e.g., listening for "Call Police", "Call Firestation", "Emergency SOS").
  2. *Tap-to-Talk Quick Microphone* with real-time waveform animation for fast action dispatch.
  3. *Audio Readback (TTS)* powered by Gemini 3.8 Flash TTS for spoken confirmation and emergency guidance.
- **Bluetooth Peer-to-Peer Communication**:
  - Direct nearby Bluetooth device discovery (BLE Scanner / Classic Bluetooth inquiry).
  - One-tap peer pairing and real-time offline text chat channels.
  - Active peer signal telemetry (RSSI, estimated distance, channel noise).
- **ML Communication Probability Engine**:
  - Evaluates signal propagation physics, device battery levels, distance, and environmental path loss to compute the exact **% chance of successful communication** and packet delivery.
  - Recommends optimal transmission frequencies and transmission retries.
- **Emergency Help Hub & Dispatch**:
  - Live GPS location telemetry displaying coordinates, altitude, and accuracy.
  - Curated radar/list of nearest Police Stations, Fire Stations, Hospitals, and Disaster Relief Shelters with direct emergency dialer intent (`Intent.ACTION_DIAL`) and one-touch SMS distress broadcast with exact coordinates.
- **User Onboarding & Medical Profile**:
  - User identity, emergency contacts (ICE), blood group, medical conditions, and disaster triage tags.

---

## 1. Overview & Core Concept

When traditional cellular towers fail during natural disasters, infrastructure blackouts, or remote wilderness emergencies, **ASVASA** acts as an autonomous lifeline.

### What It Does
- **Disaster Profile & Identity**: Secure local user profile setup with critical medical tags, emergency contacts, and personal identifier.
- **Bluetooth Nearby Mesh Messenger**: Discovers active nearby devices using Bluetooth / BLE, provides instant connection and text messaging without Internet access.
- **ML Connection Probability Predictor**: Real-time on-device machine learning heuristic analyzing RF RSSI, packet loss tendency, battery degradation, and distance modeling to display communication success probability (0–100%) and link quality indicators.
- **Voice Emergency Command Dispatch**:
  - Instant speech recognition for commands like *"Call Police"*, *"Call Firestation"*, *"Send SOS"*, *"Find nearest hospital"*.
  - Gemini AI triage assistant using `gemini-3.8-flash` (with `gemini-4-argon` high thinking mode for complex disaster triage queries) and `gemini-3.8-flash-tts` for hands-free audio guidance.
- **Emergency Help Centers**: Proximity calculation to nearest public safety infrastructure with one-touch phone dialer, navigation routes, and emergency broadcast dispatch.

### Target Audience & Persona
- Citizens and first responders operating in blackout zones, disaster-struck regions, or remote rural areas.
- Hikers, travelers, and vulnerable individuals requiring quick voice-activated emergency calling and offline peer communication.

---

## 2. User Experience & Visual Design

### Aesthetic Direction: Tactical Dark & Safety Luminescence
The visual design embodies **Tactical Reliability**: a high-contrast dark theme (`#0D1117` base) accented with **Emergency Crimson** (`#FF3B30`), **Safety Amber** (`#FF9500`), and **Signal Cyan/Emerald** (`#00E5FF` / `#10B981`) for high visibility in low-light and high-stress environments.

```
┌────────────────────────────────────────────────────────┐
│  ASVASA   [📡 88% Signal Probability]       [👤 Profile]│
├────────────────────────────────────────────────────────┤
│  ⚡ EMERGENCY VOICE DISPATCH                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  🎙️ "Listening... Say 'Call Police' or 'SOS'"     │  │
│  │  [|||||||||||||||||| Waveform Indicator |||||||] │  │
│  └──────────────────────────────────────────────────┘  │
│  [🚨 CALL POLICE]   [🚒 CALL FIRE]   [🏥 CALL AMBULANCE]│
├────────────────────────────────────────────────────────┤
│  📶 BLUETOOTH MESH RADAR (3 Peers Nearby)              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ • RescueUnit_Alpha  | -62 dBm | 94% Comm Chance  │  │
│  │ • CitizenNode_44    | -78 dBm | 72% Comm Chance  │  │
│  │ • MedicPost_West    | -85 dBm | 54% Comm Chance  │  │
│  └──────────────────────────────────────────────────┘  │
│  [🔍 Scan Nearby Devices]    [💬 Open Mesh Chat Room]   │
├────────────────────────────────────────────────────────┤
│  📍 NEAREST EMERGENCY HELP CENTRES                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🚔 Metro Central Police Station (0.8 km) [📞 Call]│  │
│  │ 🚒 Station 12 Fire & Rescue     (1.4 km) [📞 Call]│  │
│  │ 🏥 Apex Trauma Care Hospital     (2.1 km) [📞 Call]│  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Key Screens & Navigation
1. **Emergency Command Dashboard (Home)**:
   - Top Status Banner: Active GPS status, Bluetooth radio state, ML network health score.
   - Quick Voice Trigger & Gemini AI triage response banner with audio playback controls.
   - Quick Emergency Dialing cards (Police, Fire, Ambulance, National Disaster Hotline).
2. **Bluetooth Comms & Mesh Chat**:
   - Device scanner with real-time RSSI signal bars and ML communication success probability badge.
   - Active Chat Window: Send and receive offline text messages, emergency broadcasts, and location pings.
3. **Emergency Help Centers & Geolocation Radar**:
   - Real-time GPS coordinates, compass bearing, nearest safety facilities list sorted by distance.
   - Direct phone call launch (`ACTION_DIAL`), Google Maps route launch, and SMS SOS dispatch.
4. **User Profile & Disaster Medical Card**:
   - Name, emergency contact phone numbers, blood type, allergies, special medical notes, and offline data export.

---

## 3. Key Product Decisions & Trade-Offs

- **Hybrid Bluetooth + Local Simulation**:
  - *Decision*: Integrate real Android `BluetoothAdapter` and BLE scanning/discovery APIs, paired with a robust fallback local peer emulator so the app functions seamlessly in the streaming browser environment where physical Bluetooth hardware might be restricted.
  - *Why*: Ensures 100% testability in AI Studio while retaining real production hardware code for real Android devices.
- **Direct REST Gemini Client**:
  - *Decision*: Retrofit with Kotlinx Serialization calling Gemini 3.8 Flash and Gemini 4 Argon with high thinking mode for triage analysis, plus `gemini-3.8-flash-tts` for voice response.
  - *Why*: Lightweight, responsive, zero complex server dependencies, completely compatible with Android API 24+.
- **Android SpeechRecognizer & Live Voice Loop**:
  - *Decision*: Native Android `SpeechRecognizer` + Jetpack Compose microphone ripple UI for instant zero-latency speech-to-text, immediately piped into the local command interpreter (matching "police", "fire", "doctor", "help") and Gemini AI assistance.
- **Room Local Database**:
  - *Decision*: Store user profile, chat history, and offline emergency help centers in local Room database so the app is 100% resilient when network is offline.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────────────────┐
│                             ASVASA UI LAYER                             │
│   ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────────┐   │
│   │ EmergencyDashboard│  │ BluetoothCommsChat│  │ HelpCentersLocator  │   │
│   └─────────┬─────────┘  └─────────┬─────────┘  └──────────┬──────────┘   │
└─────────────┼──────────────────────┼───────────────────────┼─────────────┘
              ▼                      ▼                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           VIEWMODEL & STATE                             │
│   ┌──────────────────────────────────────────────────────────────────┐   │
│   │                   AsvasaMainViewModel                            │   │
│   │ - voiceListeningState: StateFlow<VoiceState>                     │   │
│   │ - bluetoothPeers: StateFlow<List<BluetoothPeer>>                 │   │
│   │ - messages: StateFlow<List<ChatMessage>>                         │   │
│   │ - mlProbability: StateFlow<CommunicationProbability>             │   │
│   │ - helpCenters: StateFlow<List<EmergencyCenter>>                  │   │
│   │ - userProfile: StateFlow<UserProfile>                            │   │
│   └──────────────────────────────────┬───────────────────────────────┘   │
└──────────────────────────────────────┼───────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌───────────────────┐       ┌──────────────────────┐      ┌────────────────────┐
│ Bluetooth Engine  │       │ ML Probability Engine│      │ Voice & Gemini AI  │
│ - Classic & BLE   │       │ - RSSI Path Loss Eq  │      │ - SpeechRecognizer │
│ - RFCOMM & Mesh   │       │ - Packet Loss Model  │      │ - Gemini 3.8 Flash │
│ - Broadcast Relay │       │ - Battery & Distance │      │ - Gemini 3.8 TTS   │
└───────────────────┘       └──────────────────────┘      └────────────────────┘
         │                             │                             │
         └─────────────────────────────┼─────────────────────────────┘
                                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           DATA & PERSISTENCE                             │
│   - Room Database (UserProfileDao, ChatMessageDao, EmergencyCenterDao)   │
│   - Android LocationManager & Telephony/Dialer Intents                   │
└──────────────────────────────────────────────────────────────────────────┘
```

### ML Communication Probability Equation
$$\text{Probability} = \sigma\left(w_1 \cdot \text{RSSI}_{\text{norm}} + w_2 \cdot (1 - \text{Distance}_{\text{norm}}) + w_3 \cdot \text{Battery}_{\text{norm}} - w_4 \cdot \text{ChannelNoise}\right)$$
This gives users an actionable 0–100% confidence rating before transmitting vital distress messages across the mesh.

---

## Execution Next Steps (Upon Approval)
1. **Dependencies & Permissions**: Add Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`), Audio (`RECORD_AUDIO`), and Phone call intent declarations.
2. **Setup Gemini & Room**: Configure Retrofit Gemini API client, TTS synthesizer, and Room database for offline resilience.
3. **Build Core Engines**: Implement `BluetoothMeshManager`, `MlProbabilityEngine`, `VoiceCommandController`, and `EmergencyLocatorService`.
4. **Build UI & Verify**: Create high-fidelity Jetpack Compose screens with Material 3 styling, test tags, and verify compilation via `compile_applet`.
