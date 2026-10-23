# ASVASA — Your Guardian. Always.

> *Be ASVASED, we are here.*

ASVASA is an emergency-assistance project that grows from a one-tap SOS button into an on-device **Guardian**: an app that observes the device context, understands the situation, assesses risk, plans the next safe step and helps the user act on it.

**Project author:** Shrey Kapoor  
**Team:** RUDRAM47  
**University:** BIET, Lucknow — Department of AIML (3rd year)

---

## Status at a glance

This project is honest about what exists today and what is planned. Nothing marked *planned* is claimed as working.

| Area | Status |
|---|---|
| Android app (Kotlin, Jetpack Compose), SOS interface | Implemented foundation |
| Spring Boot backend: emergency creation, mission foundation | Implemented foundation |
| Guardian Core, rule-based reasoning and planning, execution architecture | Implemented foundation |
| Real device-context collection (GPS, battery, network) | In integration |
| Guardian memory, prediction engine | In integration |
| Knowledge base, Guardian chatbot, original small language model | Planned |
| Reliable offline communication, mesh networking | Exploratory / planned |
| Validated predictive risk models, full safety testing | Planned |

No predictive accuracy has been validated or claimed. The reasoning engine today is **rule-based plus a tiny classifier**, not a trained production ML model.

---

## Repository layout

```
asvasa/
├── android/        Android app — Kotlin, Jetpack Compose (package com.asvasa.emergency)
├── backend/        Spring Boot service — emergencies and missions
├── prototype/
│   └── index.html  Browser prototype (single file, no build step)
├── docs/
│   └── RUDRAM_PPT.pdf   Project presentation
├── requirements.txt
└── README.md
```

Adjust the folder names to match your actual repository.

---

## Browser prototype

A single-file, dependency-free web prototype of the Guardian loop. It runs fully on the device and works offline after loading.

### What it does

- **SOS with a 5-second cancellable countdown.** Nothing is sent automatically.
- **Guardian loop:** Observe → Understand → Assess → Plan → Act → Learn, shown stage by stage.
- **Real device context:** location, battery and network are read from browser sensors and tagged `REAL`, `STALE` or `UNAVAILABLE`. Missing readings are never replaced with made-up values.
- **Intent understanding:** a small Naive Bayes text classifier trained in the browser, combined with keyword rules, to identify fire, medical, personal-safety, accident, disaster or lost/stranded situations.
- **Risk assessment:** transparent rule-based score (Low / Moderate / High / Critical) with the reasons listed.
- **Safety guidance:** a small built-in knowledge base (fire, bleeding, burns, collapse, earthquake, flood, lost, personal safety).
- **Voice control:** hands-free commands such as "call police", "call fire station", "call ambulance", "SOS", "send SOS to nearby", "open mesh", "cancel". Calls start after a 5-second window you can cancel by voice.
- **Bluetooth messaging:** scan and connect to nearby Bluetooth LE devices and exchange text, plus a broadcast-SOS action. A simulated device is included so the flow can be demonstrated without hardware.
- **Accounts:** local sign-up and login, emergency contact, and permission management (location, microphone, notifications).
- **Offline-first log:** emergency records are queued when offline and marked synced when back online (sync is simulated).

### Run it

The prototype needs a **secure context** (HTTPS or `localhost`) for location, microphone and Bluetooth.

```bash
cd prototype
python -m http.server 8000
```

Then open `http://localhost:8000` in **Chrome or Edge** (desktop or Android).

Or simply open the published artifact link in Chrome/Edge.

### Browser support

| Feature | Chrome / Edge | Firefox | iOS Safari |
|---|---|---|---|
| Location, network, SOS, chat | Yes | Yes | Yes |
| Voice control (speech recognition) | Yes | No | Limited |
| Bluetooth LE messaging (Web Bluetooth) | Yes | No | No |
| Battery status | Yes | No | No |

### Prototype limitations

- **Bluetooth text delivery** works only with devices that expose a BLE UART (Nordic UART Service) text channel, or an ASVASA Android peer. Ordinary phones, earbuds and speakers cannot receive texts this way. This is a browser and platform restriction, not a bug.
- **Accounts are local to the browser.** Passwords are hashed with SHA-256 and stored in local storage. There is no server, so this is for demonstration only — a production app needs a real backend with salted, slow password hashing.
- **Voice calls** use the `tel:` link, so the phone's dialler opens; the browser cannot place calls silently.
- **Sync** to a backend is simulated in the prototype.
- This is a prototype and **not a substitute for emergency services**. In danger, call your local emergency number (112 in India).

---

## Android app and backend

- **Android:** Kotlin, Jetpack Compose, `HomeViewModel`, repository layer talking to the backend. Open the `android/` folder in Android Studio and let Gradle sync.
- **Backend:** Spring Boot service handling emergency and mission management. Run it with the Gradle or Maven wrapper that your `backend/` folder provides.

Exact SDK and JDK versions are defined by your Gradle/Maven build files — check those rather than this README.

### Architecture (intended data flow)

```
Compose UI → HomeViewModel → Device Context Collection → Guardian Core / Emergency Engine
          → Reasoning & Planning → Action Execution → Emergency Repository / Backend API
          → Emergency & Mission Management (Spring Boot)
```

Calls, recording, location sharing and connectivity changes always respect Android permissions, user consent and platform restrictions. The user stays in control of consequential actions.

---

## Roadmap

Planned milestones, not completed achievements:

1. Stabilise the Android and backend foundation
2. Complete real device-context collection and Guardian integration
3. Develop the conversational interface
4. Build and evaluate an original small language model
5. Connect local inference with reasoning, memory and planning
6. Add validated prediction, offline capability and further assistance
7. Run extensive safety, usability, performance and field testing

---

## Safety and privacy principles

- Safety first: no unsafe automated actions; consequential actions need user consent.
- Permission-aware: every sensor is used only with permission.
- Source and freshness tagged: context readings say where they came from and how old they are.
- Fallbacks are never shown as real readings.

---

## License

Add your license here (for example MIT) before publishing the repository.
