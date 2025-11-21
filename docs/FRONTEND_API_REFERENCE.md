# Frontend API Reference - Flutter Mobile App

Complete API documentation for the Flutter mobile application with all endpoints, DTOs, request/response formats, and Firebase notification setup guide.

**Base URL**: `http://192.168.1.20:8080` (or your backend URL)

**Authentication**: All protected endpoints require `Authorization: Bearer <firebaseIdToken>` header.

---

## Table of Contents

1. [Authentication & User Management](#1-authentication--user-management)
2. [Tournaments](#2-tournaments)
3. [Slots & Booking](#3-slots--booking)
4. [Wallet & Transactions](#4-wallet--transactions)
5. [Notifications](#5-notifications)
6. [Banners](#6-banners)
7. [Payments](#7-payments)
8. [App Configuration](#8-app-configuration)
9. [DTOs (Data Transfer Objects)](#9-dtos-data-transfer-objects)
10. [Firebase Push Notifications Setup](#10-firebase-push-notifications-setup)
11. [Testing Checklist](#11-testing-checklist)

---

## 1. Authentication & User Management

### Register/Update Current User
```http
POST /api/users/me
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "firebaseUserUID": "string",
  "userName": "string",
  "email": "string"
}
```

**Response:** `UserDTO`
```json
{
  "firebaseUserUID": "abc123",
  "userName": "PlayerOne",
  "email": "player@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2025-11-20T20:15:00",
  "deviceToken": "fcm_token_here"
}
```

### Get Current User Profile
```http
GET /api/users/me
Authorization: Bearer <firebaseIdToken>
```

**Response:** `UserDTO` (same as above)

### Update Device Token for Push Notifications
```http
POST /api/users/device-token
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "deviceToken": "fcm_token_from_firebase_messaging"
}
```

**Response:**
```json
{
  "message": "Device token updated successfully",
  "firebaseUID": "abc123"
}
```

**⚠️ CRITICAL:** This endpoint must be called after Firebase Messaging initialization to receive push notifications!

---

## 2. Tournaments

### Get All Tournaments (Authenticated)
```http
GET /api/tournaments
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<TournamentsDTO>`

### Get Public Tournaments (No Auth Required)
```http
GET /api/public/tournaments
```

**Response:** `List<TournamentsDTO>`

### Get Tournament by ID
```http
GET /api/tournaments/{id}
Authorization: Bearer <firebaseIdToken>
```

**Response:** `TournamentsDTO`

### Get Public Tournament Details
```http
GET /api/public/tournaments/{tournamentId}
```

**Response:**
```json
{
  "id": 1,
  "name": "Free Fire Showdown",
  "title": "Free Fire Showdown",
  "game": "Free Fire",
  "prizePool": 10000,
  "entryFee": 50,
  "maxPlayers": 48,
  "startTime": "2025-11-22T18:30:00",
  "status": "UPCOMING",
  "teamSize": "SOLO",
  "imageLink": "https://cdn.example.com/image.jpg"
}
```

### Get Tournaments by Status
```http
GET /api/tournaments/status/{status}
Authorization: Bearer <firebaseIdToken>
```

**Status values:** `UPCOMING`, `ONGOING`, `COMPLETED`, `CANCELLED`

**Response:** `List<TournamentsDTO>`

### Get Tournament Game Credentials
```http
GET /api/tournaments/{tournamentId}/credentials
Authorization: Bearer <firebaseIdToken>
```

**Response:**
```json
{
  "gameId": "ROOM123",
  "gamePassword": "pass123"
}
```

---

## 3. Slots & Booking

### Get Slot Summary
```http
GET /api/slots/{tournamentId}/summary
Authorization: Bearer <firebaseIdToken>
```

**Response:**
```json
{
  "totalSlots": 48,
  "bookedSlots": 12,
  "availableSlots": 36,
  "slots": [
    {
      "id": 1,
      "tournamentId": 1,
      "slotNumber": 1,
      "firebaseUserUID": "user123",
      "playerName": "PlayerOne",
      "status": "BOOKED",
      "bookedAt": "2025-11-20T21:05:00"
    }
  ]
}
```

**Note:** `slots` array is always present (never null), even if empty.

### Get All Slots for Tournament
```http
GET /api/slots/{tournamentId}
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<SlotsDTO>`

### Book a Slot
```http
POST /api/slots/book
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "tournamentId": 1,
  "slotNumber": 5,
  "playerName": "PlayerOne"
}
```

**Response:** `SlotsDTO`

### Book Next Available Slot
```http
POST /api/slots/book-next/{tournamentId}
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "playerName": "PlayerOne"
}
```

**Response:** `SlotsDTO`

### Book Team Slots (DUO/SQUAD/HEXA)
```http
POST /api/slots/book-team
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "tournamentId": 1,
  "players": [
    {
      "slotNumber": 1,
      "playerName": "Leader"
    },
    {
      "slotNumber": 2,
      "playerName": "Mate"
    }
  ]
}
```

**Response:** `List<SlotsDTO>`

### Get My Bookings
```http
GET /api/slots/my-bookings
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<SlotsDTO>`

### Cancel My Booking
```http
DELETE /api/slots/{slotId}/cancel
Authorization: Bearer <firebaseIdToken>
```

**Response:** `204 No Content`

---

## 4. Wallet & Transactions

### Get My Wallet
```http
GET /api/wallets/{firebaseUID}
Authorization: Bearer <firebaseIdToken>
```

**Response:** `WalletDTO`
```json
{
  "id": 55,
  "firebaseUserUID": "abc123",
  "coins": 1234,
  "lastUpdated": "2025-11-20T21:10:11"
}
```

### Get Wallet Ledger (Transaction History)
```http
GET /api/wallets/{firebaseUID}/ledger
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<WalletLedgerDTO>`
```json
[
  {
    "id": 8801,
    "walletId": 55,
    "userId": "abc123",
    "direction": "CREDIT",
    "amount": 100,
    "balanceAfter": 1234,
    "referenceType": "BOOKING",
    "referenceId": "slot_123",
    "metadata": {},
    "createdBy": "system",
    "createdAt": "2025-11-20T21:10:11"
  }
]
```

### Create Deposit Request
```http
POST /api/transactions/deposit
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "transactionUID": "unique_transaction_id",
  "amount": 500
}
```

**Response:** `TransactionTableDTO`

### Create Withdrawal Request
```http
POST /api/transactions/withdraw
Authorization: Bearer <firebaseIdToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "amount": 200
}
```

**Response:** `TransactionTableDTO`

### Get Transaction History
```http
GET /api/transactions/history
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<TransactionTableDTO>`

### Cancel Transaction
```http
DELETE /api/transactions/{id}/cancel
Authorization: Bearer <firebaseIdToken>
```

**Response:** `204 No Content`

---

## 5. Notifications

### Get My Notifications
```http
GET /api/notifications/my
Authorization: Bearer <firebaseIdToken>
```

**Response:** `List<NotificationsDTO>`
```json
[
  {
    "id": 1,
    "firebaseUserUID": "abc123",
    "title": "Tournament Reminder",
    "message": "Your tournament starts in 15 minutes!",
    "type": "TOURNAMENT",
    "isRead": false,
    "createdAt": "2025-11-20T20:15:00"
  }
]
```

### Mark Notification as Read
```http
PATCH /api/notifications/{notificationId}/read
Authorization: Bearer <firebaseIdToken>
```

**Response:** `204 No Content`

### Get Notification Statistics
```http
GET /api/notifications/stats
Authorization: Bearer <firebaseIdToken>
```

**Response:**
```json
{
  "totalNotifications": 25,
  "unreadCount": 5,
  "readCount": 20
}
```

---

## 6. Banners

### Get Active Banners (Public)
```http
GET /api/banners
```

**Response:** `List<BannerDTO>`
```json
[
  {
    "id": 1,
    "imageUrl": "https://cdn.example.com/banner.jpg",
    "title": "New Tournament",
    "description": "Join now!",
    "actionUrl": "https://app.example.com/tournament/1",
    "type": "IMAGE",
    "order": 1,
    "isActive": true,
    "startDate": "2025-11-20T00:00:00",
    "endDate": "2025-11-30T23:59:59",
    "createdAt": "2025-11-20T10:00:00",
    "updatedAt": "2025-11-20T10:00:00"
  }
]
```

---

## 7. Payments

### Get QR Code by Amount
```http
GET /api/v1/payments/qr/{amount}
```

**Example:** `GET /api/v1/payments/qr/500`

**Response:** `PaymentResponseDTO`
```json
{
  "amount": 500,
  "coin": 500,
  "upiIdQrLink": "upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR",
  "isActive": true
}
```

### Get QR Code (POST)
```http
POST /api/v1/payments/qr
Content-Type: application/json
```

**Request Body:**
```json
{
  "amount": 500
}
```

**Response:** `PaymentResponseDTO` (same as above)

### Get Available Payment Amounts
```http
GET /api/v1/payments/amounts
```

**Response:** `List<AvailableAmountDTO>`
```json
[
  {
    "amount": 100,
    "coin": 100,
    "upiIdQrLink": "upi://...",
    "isActive": true
  },
  {
    "amount": 500,
    "coin": 500,
    "upiIdQrLink": "upi://...",
    "isActive": true
  }
]
```

### Payment Service Health Check
```http
GET /api/v1/payments/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-11-20T20:15:00"
}
```

---

## 8. App Configuration

### Get App Version
```http
GET /api/app/version
```

**Response:**
```json
{
  "minSupported": "1.1.0",
  "latest": "1.3.2",
  "playStoreUrl": "https://play.google.com/store/apps/details?id=com.esport.tournament"
}
```

### Get Filters
```http
GET /api/filters
```

**Response:**
```json
{
  "games": ["Free Fire", "PUBG", "COD Mobile", "BGMI", "Clash Royale"],
  "teamSizes": ["Solo", "Duo", "Squad", "Hexa"],
  "maps": ["Bermuda", "Purgatory", "Kalahari", "Alpine", "NeXTerra"],
  "timeSlots": ["6:00-6:30 PM", "7:00-8:00 PM", "8:00-9:00 PM", "9:00-10:00 PM"]
}
```

### Get Platform Info
```http
GET /api/public/info
```

**Response:**
```json
{
  "name": "ESport Tournament Platform",
  "version": "1.0.0",
  "description": "Competitive gaming tournament platform",
  "supportEmail": "support@esporttournament.com",
  "termsUrl": "https://esporttournament.com/terms",
  "privacyUrl": "https://esporttournament.com/privacy"
}
```

### Get Registration Requirements
```http
GET /api/public/registration/requirements
```

**Response:**
```json
{
  "minimumAge": 13,
  "requiredDocuments": [
    "Valid email address",
    "Firebase authentication",
    "Unique username"
  ],
  "termsAndConditions": "Must be 13+ years old. One account per person.",
  "privacyPolicy": "Your data is protected and will not be shared with third parties."
}
```

---

## 9. DTOs (Data Transfer Objects)

### TypeScript Interfaces for Flutter

#### UserDTO
```typescript
interface UserDTO {
  firebaseUserUID: string;
  userName: string;
  email: string;
  role: "USER" | "ADMIN" | "SUPER_ADMIN" | "MANAGER" | "OPERATOR";
  status: "ACTIVE" | "INACTIVE" | "BANNED";
  createdAt: string; // ISO 8601 datetime
  avatarUrl?: string;
  deviceToken?: string;
}
```

#### TournamentsDTO
```typescript
interface TournamentsDTO {
  id: number;
  name: string;
  title: string; // Alias for name
  game: string;
  map: string;
  imageLink?: string;
  prizePool: number;
  entryFee: number;
  maxPlayers: number;
  teamSize: "SOLO" | "DUO" | "SQUAD" | "HEXA"; // Never null, defaults to "SOLO"
  status: "UPCOMING" | "ONGOING" | "COMPLETED" | "CANCELLED";
  startTime: string; // ISO 8601 datetime
  rules?: string[];
  registeredPlayers: number;
  participants?: ParticipantInfo[];
  scoreboard?: ScoreboardEntry[];
  perKillReward?: number;
  firstPrize?: number;
  secondPrize?: number;
  thirdPrize?: number;
  gameId?: string;
  gamePassword?: string;
}

interface ParticipantInfo {
  playerName: string;
  slotNumber: number;
  userId?: string;
  firebaseUserUID?: string;
}

interface ScoreboardEntry {
  playerName?: string;
  teamName?: string;
  kills?: number;
  coinsEarned?: number;
  placement?: number;
}
```

#### SlotsDTO
```typescript
interface SlotsDTO {
  id: number;
  tournamentId: number;
  slotNumber: number;
  firebaseUserUID: string;
  playerName: string;
  status: "BOOKED" | "AVAILABLE" | "CANCELLED";
  bookedAt: string; // ISO 8601 datetime (camelCase, not snake_case)
}
```

#### WalletDTO
```typescript
interface WalletDTO {
  id: number;
  firebaseUserUID: string;
  coins: number;
  lastUpdated: string; // ISO 8601 datetime
}
```

#### WalletLedgerDTO
```typescript
interface WalletLedgerDTO {
  id: number;
  walletId: number;
  userId: string;
  direction: "CREDIT" | "DEBIT";
  amount: number;
  balanceAfter: number;
  referenceType: "BOOKING" | "REFUND" | "ADMIN_ADJUSTMENT" | "TRANSACTION";
  referenceId: string;
  metadata?: Record<string, any>;
  createdBy: string;
  createdAt: string; // ISO 8601 datetime
}
```

#### TransactionTableDTO
```typescript
interface TransactionTableDTO {
  id: number;
  firebaseUserUID: string;
  transactionUID: string;
  amount: number;
  type: "DEPOSIT" | "WITHDRAWAL" | "CUSTOM";
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  date: string; // ISO 8601 datetime
}
```

#### NotificationsDTO
```typescript
interface NotificationsDTO {
  id: number;
  firebaseUserUID?: string;
  title: string;
  message: string;
  type?: "SYSTEM" | "TOURNAMENT" | "WALLET";
  data?: Record<string, any>;
  isRead?: boolean;
  createdAt: string; // ISO 8601 datetime
  targetAudience?: "ALL" | "USERS" | "ADMINS";
}
```

#### BannerDTO
```typescript
interface BannerDTO {
  id: number;
  imageUrl: string;
  title: string;
  description?: string;
  actionUrl?: string;
  type: "IMAGE" | "VIDEO" | "AD";
  order: number;
  isActive: boolean;
  startDate: string; // ISO 8601 datetime
  endDate: string; // ISO 8601 datetime
  createdAt: string; // ISO 8601 datetime
  updatedAt: string; // ISO 8601 datetime
}
```

#### PaymentResponseDTO
```typescript
interface PaymentResponseDTO {
  amount: number;
  coin: number;
  upiIdQrLink: string;
  isActive: boolean;
  notes?: string;
}
```

#### AvailableAmountDTO
```typescript
interface AvailableAmountDTO {
  amount: number;
  coin: number;
  upiIdQrLink: string;
  isActive: boolean;
}
```

---

## 10. Firebase Push Notifications Setup

### ⚠️ CRITICAL: Why Notifications Don't Show as Popups

If you're not receiving push notifications as popups (like WhatsApp), it's likely due to:

1. **Missing Firebase Cloud Messaging (FCM) initialization**
2. **Missing notification channel setup (Android)**
3. **Missing notification permissions**
4. **Device token not registered with backend**
5. **Firebase configuration issues**

### Step-by-Step Setup Guide

#### 1. Add Firebase Dependencies

**pubspec.yaml:**
```yaml
dependencies:
  firebase_core: ^2.24.2
  firebase_messaging: ^14.7.9
  flutter_local_notifications: ^16.3.0
```

#### 2. Initialize Firebase in Flutter

**main.dart:**
```dart
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

// Initialize Firebase
await Firebase.initializeApp();

// Initialize local notifications plugin
final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

// Android initialization settings
const AndroidInitializationSettings initializationSettingsAndroid =
    AndroidInitializationSettings('@mipmap/ic_launcher');

// iOS initialization settings
const DarwinInitializationSettings initializationSettingsIOS =
    DarwinInitializationSettings(
  requestAlertPermission: true,
  requestBadgePermission: true,
  requestSoundPermission: true,
);

const InitializationSettings initializationSettings = InitializationSettings(
  android: initializationSettingsAndroid,
  iOS: initializationSettingsIOS,
);

await flutterLocalNotificationsPlugin.initialize(
  initializationSettings,
  onDidReceiveNotificationResponse: (NotificationResponse response) {
    // Handle notification tap
    print('Notification tapped: ${response.payload}');
  },
);

// Request notification permissions
await FirebaseMessaging.instance.requestPermission(
  alert: true,
  badge: true,
  sound: true,
);

// Create Android notification channel (REQUIRED for Android 8.0+)
const AndroidNotificationChannel channel = AndroidNotificationChannel(
  'tournament_channel', // Must match backend channel ID
  'Tournament Notifications',
  description: 'Notifications for tournaments, wallet updates, and more',
  importance: Importance.high,
  playSound: true,
  enableVibration: true,
);

await flutterLocalNotificationsPlugin
    .resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>()
    ?.createNotificationChannel(channel);
```

#### 3. Get FCM Token and Register with Backend

```dart
// Get FCM token
String? fcmToken = await FirebaseMessaging.instance.getToken();
print('FCM Token: $fcmToken');

// Register token with backend
if (fcmToken != null) {
  await registerDeviceToken(fcmToken);
}

// Listen for token refresh
FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
  print('New FCM Token: $newToken');
  registerDeviceToken(newToken);
});

// Function to register token
Future<void> registerDeviceToken(String token) async {
  try {
    final response = await http.post(
      Uri.parse('$baseUrl/api/users/device-token'),
      headers: {
        'Authorization': 'Bearer $firebaseIdToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({'deviceToken': token}),
    );
    
    if (response.statusCode == 200) {
      print('✅ Device token registered successfully');
    } else {
      print('❌ Failed to register device token: ${response.statusCode}');
    }
  } catch (e) {
    print('❌ Error registering device token: $e');
  }
}
```

#### 4. Handle Foreground Notifications

```dart
// Handle foreground messages (when app is open)
FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  print('📱 Foreground notification received: ${message.notification?.title}');
  
  // Show local notification
  showLocalNotification(message);
});

Future<void> showLocalNotification(RemoteMessage message) async {
  const AndroidNotificationDetails androidPlatformChannelSpecifics =
      AndroidNotificationDetails(
    'tournament_channel', // Must match channel ID
    'Tournament Notifications',
    channelDescription: 'Notifications for tournaments and updates',
    importance: Importance.high,
    priority: Priority.high,
    showWhen: true,
    playSound: true,
    enableVibration: true,
  );

  const DarwinNotificationDetails iOSPlatformChannelSpecifics =
      DarwinNotificationDetails(
    presentAlert: true,
    presentBadge: true,
    presentSound: true,
  );

  const NotificationDetails platformChannelSpecifics = NotificationDetails(
    android: androidPlatformChannelSpecifics,
    iOS: iOSPlatformChannelSpecifics,
  );

  await flutterLocalNotificationsPlugin.show(
    message.hashCode,
    message.notification?.title ?? 'New Notification',
    message.notification?.body ?? '',
    platformChannelSpecifics,
    payload: message.data.toString(),
  );
}
```

#### 5. Handle Background Notifications

**Create `firebase_messaging_background.dart` in your project root:**
```dart
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  print('📱 Background notification received: ${message.notification?.title}');
  
  // Show notification
  const AndroidNotificationDetails androidPlatformChannelSpecifics =
      AndroidNotificationDetails(
    'tournament_channel',
    'Tournament Notifications',
    importance: Importance.high,
    priority: Priority.high,
  );

  const NotificationDetails platformChannelSpecifics = NotificationDetails(
    android: androidPlatformChannelSpecifics,
  );

  await flutterLocalNotificationsPlugin.show(
    message.hashCode,
    message.notification?.title ?? 'New Notification',
    message.notification?.body ?? '',
    platformChannelSpecifics,
  );
}
```

**In main.dart, register the background handler:**
```dart
import 'firebase_messaging_background.dart' as bg;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  
  // Register background message handler
  FirebaseMessaging.onBackgroundMessage(bg.firebaseMessagingBackgroundHandler);
  
  runApp(MyApp());
}
```

#### 6. Handle Notification Taps

```dart
// Handle notification taps when app is in background/terminated
FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
  print('🔔 Notification opened app: ${message.notification?.title}');
  // Navigate to relevant screen based on message.data
  handleNotificationNavigation(message.data);
});

// Check if app was opened from notification
FirebaseMessaging.instance.getInitialMessage().then((RemoteMessage? message) {
  if (message != null) {
    print('🔔 App opened from notification: ${message.notification?.title}');
    handleNotificationNavigation(message.data);
  }
});

void handleNotificationNavigation(Map<String, dynamic> data) {
  String? type = data['type'];
  String? notificationId = data['notificationId'];
  
  if (type == 'tournament') {
    // Navigate to tournament screen
    Navigator.pushNamed(context, '/tournament', arguments: data);
  } else if (type == 'wallet') {
    // Navigate to wallet screen
    Navigator.pushNamed(context, '/wallet');
  }
  // Handle other types...
}
```

#### 7. Android Configuration

**android/app/src/main/AndroidManifest.xml:**
```xml
<manifest>
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
  
  <application>
    <!-- Firebase Messaging default notification channel -->
    <meta-data
      android:name="com.google.firebase.messaging.default_notification_channel_id"
      android:value="tournament_channel" />
    
    <!-- Firebase Messaging default notification icon -->
    <meta-data
      android:name="com.google.firebase.messaging.default_notification_icon"
      android:resource="@mipmap/ic_launcher" />
    
    <!-- Firebase Messaging default notification color -->
    <meta-data
      android:name="com.google.firebase.messaging.default_notification_color"
      android:resource="@color/colorPrimary" />
  </application>
</manifest>
```

#### 8. iOS Configuration

**ios/Runner/Info.plist:**
```xml
<key>FirebaseAppDelegateProxyEnabled</key>
<false/>
```

**ios/Runner/AppDelegate.swift:**
```swift
import UIKit
import Flutter
import FirebaseCore
import FirebaseMessaging

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    FirebaseApp.configure()
    
    if #available(iOS 10.0, *) {
      UNUserNotificationCenter.current().delegate = self
      let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
      UNUserNotificationCenter.current().requestAuthorization(
        options: authOptions,
        completionHandler: { _, _ in }
      )
    }
    
    application.registerForRemoteNotifications()
    
    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
  
  override func application(_ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    Messaging.messaging().apnsToken = deviceToken
  }
}
```

### Common Issues & Solutions

#### Issue 1: Notifications not showing as popups
**Solution:**
- Ensure notification channel is created (Android 8.0+)
- Check notification permissions are granted
- Verify device token is registered with backend
- Ensure `flutter_local_notifications` is properly initialized

#### Issue 2: Notifications only work when app is open
**Solution:**
- Register background message handler
- Check AndroidManifest.xml configuration
- Verify iOS AppDelegate setup

#### Issue 3: Device token not registered
**Solution:**
- Call `/api/users/device-token` endpoint after getting FCM token
- Handle token refresh events
- Check backend logs for registration errors

#### Issue 4: Notifications arrive but don't show
**Solution:**
- Check notification channel ID matches backend (`tournament_channel`)
- Verify notification permissions
- Check if notifications are disabled in device settings

---

## 11. Testing Checklist

### Authentication & User Management
- [ ] User registration with Firebase token
- [ ] Get current user profile
- [ ] Update device token for push notifications
- [ ] Device token refresh handling

### Tournaments
- [ ] Get all tournaments (authenticated)
- [ ] Get public tournaments (no auth)
- [ ] Get tournament by ID
- [ ] Get tournament by status (UPCOMING, ONGOING, etc.)
- [ ] Get tournament game credentials
- [ ] Verify `teamSize` is never null (defaults to "SOLO")
- [ ] Verify `title` field is present (alias for `name`)
- [ ] Verify `participants` array is present
- [ ] Verify `scoreboard` array is present
- [ ] Verify prize fields (`perKillReward`, `firstPrize`, etc.)

### Slots & Booking
- [ ] Get slot summary (verify `slots` array is never null)
- [ ] Get all slots for tournament
- [ ] Book a slot (solo)
- [ ] Book next available slot
- [ ] Book team slots (DUO/SQUAD/HEXA)
- [ ] Get my bookings
- [ ] Cancel my booking
- [ ] Verify wallet deduction on booking
- [ ] Verify refund on cancellation

### Wallet & Transactions
- [ ] Get my wallet balance
- [ ] Get wallet ledger (transaction history)
- [ ] Create deposit request
- [ ] Create withdrawal request
- [ ] Get transaction history
- [ ] Cancel pending transaction
- [ ] Verify wallet balance updates correctly

### Notifications
- [ ] **CRITICAL:** Receive push notification when app is in foreground
- [ ] **CRITICAL:** Receive push notification when app is in background
- [ ] **CRITICAL:** Receive push notification when app is terminated
- [ ] **CRITICAL:** Notification shows as popup (like WhatsApp)
- [ ] Get my notifications list
- [ ] Mark notification as read
- [ ] Get notification statistics
- [ ] Handle notification tap navigation
- [ ] Receive tournament credentials notification
- [ ] Receive tournament reminder notification
- [ ] Receive wallet update notification

### Banners
- [ ] Get active banners (public endpoint)
- [ ] Display banners in UI
- [ ] Handle banner tap navigation

### Payments
- [ ] Get QR code by amount
- [ ] Get QR code (POST method)
- [ ] Get available payment amounts
- [ ] Display QR code for payment
- [ ] Verify payment flow

### App Configuration
- [ ] Get app version
- [ ] Get filters (games, teamSizes, maps, timeSlots)
- [ ] Get platform info
- [ ] Get registration requirements
- [ ] Check app version compatibility

### Error Handling
- [ ] Handle 401 Unauthorized (redirect to login)
- [ ] Handle 404 Not Found
- [ ] Handle 500 Internal Server Error
- [ ] Handle network errors
- [ ] Handle timeout errors
- [ ] Display user-friendly error messages

### Performance
- [ ] API response times are acceptable
- [ ] Images load efficiently
- [ ] List pagination works correctly
- [ ] Caching works where applicable

### Security
- [ ] Firebase token is sent in all authenticated requests
- [ ] Token refresh is handled correctly
- [ ] Sensitive data is not logged
- [ ] HTTPS is used in production

---

## Quick Reference: Common Endpoints

| Endpoint | Method | Auth Required | Purpose |
|----------|--------|---------------|---------|
| `/api/users/me` | POST | Yes | Register/Update user |
| `/api/users/me` | GET | Yes | Get current user |
| `/api/users/device-token` | POST | Yes | Register FCM token |
| `/api/tournaments` | GET | Yes | Get all tournaments |
| `/api/public/tournaments` | GET | No | Get public tournaments |
| `/api/tournaments/{id}` | GET | Yes | Get tournament details |
| `/api/slots/{tournamentId}/summary` | GET | Yes | Get slot summary |
| `/api/slots/book` | POST | Yes | Book a slot |
| `/api/wallets/{firebaseUID}` | GET | Yes | Get wallet |
| `/api/transactions/deposit` | POST | Yes | Create deposit |
| `/api/notifications/my` | GET | Yes | Get notifications |
| `/api/banners` | GET | No | Get active banners |
| `/api/v1/payments/qr/{amount}` | GET | No | Get payment QR |

---

## Support & Troubleshooting

### Backend Logs
Check backend logs for:
- Firebase initialization errors
- Device token registration failures
- Notification sending errors

### Frontend Debugging
1. Check FCM token is obtained: `print('FCM Token: $token')`
2. Verify token is registered: Check network request to `/api/users/device-token`
3. Test notification locally: Use Firebase Console to send test notification
4. Check notification permissions: Verify in device settings

### Common Error Codes
- `401 Unauthorized`: Invalid or expired Firebase token
- `404 Not Found`: Resource doesn't exist
- `500 Internal Server Error`: Backend issue, check logs

---

**Last Updated:** 2025-11-21  
**Backend Version:** 1.0.0  
**API Version:** v1

