Accurate Tracking – Backend & Android App Setup Guide
📌 Overview

This project consists of:

Node.js Backend Server

Android Road Vehicle Tracking Application

Damoov Tracking SDK Integration

⚠️ Important:
The backend server must be running before building or launching the Android application.

🚀 Backend Setup
1️⃣ Clone Repository
git clone https://github.com/AccurateIC/damoovbackend_sync.git
cd damoovbackend_sync

2️⃣ Checkout Correct Branch
git branch


There are two branches:

Branch Name	Purpose
master	Road Vehicle Backend
neurofleet_boat_backendsync	Boat Backend

✅ Use master for the Road Vehicle app.

⚠️ Do not switch branches or modify configurations unnecessarily. This may affect user preferences and production data.

3️⃣ Run Backend Server

If running on office local server (via SSH):

cd Desktop/accuratedamov/damoovbackend_sync
git branch


Ensure:

* master


Start the server:

node server.js

✅ Expected Console Output

If everything is configured correctly, you should see:

Database accurate_tracking_db checked/created

Tables created/verified:

admin

users

devices

TrackTable

LastKnownPointTable

EventsStartPointTable

EventsStopPointTable

SampleTable

SystemEventsTable

newSampleTable

Finally:

✅ API running at http://0.0.0.0:5556


If the API does not start:

Check database connection

Verify Node version

Check port conflicts

Review console logs

📱 Android Application Setup
1️⃣ Build the App

Once backend is running:

Build the Android project

Install & launch the app

2️⃣ Initial App Flow

After launching:

SplashActivity

Settings Activity

Enter backend Server IP

Use it as API Cloud URL

Proceed to Login Screen

If login screen does not appear:

Ensure backend is running

Verify correct IP & port (5556)

Check network connectivity

🚗 Application Details
App Type

This application is designed specifically for Road Vehicle Tracking.

📍 Tracking Configuration

Tracking is fully automatic using the Damoov Tracking SDK.

⚠️ Ensure SDK version is updated (latest or recent stable version).

🛠 Tracking Conditions

Trips are recorded only when all conditions are met:

Parameter	Value
Minimum Speed	30 km/h
Minimum Distance	1 km
Speed Timeout	5 minutes

If conditions are not satisfied, trip data will not be created.

🔄 Complete Application Flow
Android App
↓
All Permissions Granted
↓
Tracking SDK Initialized
↓
Tracking Starts Automatically
↓
Data Stored in Local Database
↓
Tracking Stops
↓
Data Synced to Backend Server
↓
Trip Records Created
↓
Trips Visible in Dashboard & Android App

🗄 Database Tables Used

admin

users

devices

TrackTable

LastKnownPointTable

EventsStartPointTable

EventsStopPointTable

SampleTable

SystemEventsTable

newSampleTable

🧪 Troubleshooting

If trips are not visible:

Verify SDK initialization

Check local DB entries

Verify sync API logs

Confirm entries in:

EventsStartPointTable

EventsStopPointTable

Ensure backend is running on port 5556

📞 Notes

Do not change backend branches without requirement.

Do not modify user preference configurations.

Always start backend before testing Android app.