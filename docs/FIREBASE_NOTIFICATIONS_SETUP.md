# Firebase Push Notifications Setup Guide - Flutter

## Why Notifications Don't Show as Popups

If notifications are not appearing as popups (like WhatsApp), it's because:

1. **Missing Local Notifications Plugin**: FCM only delivers the message payload. You need `flutter_local_notifications` to display the actual popup.
2. **Missing Notification Channel (Android)**: Android 8.0+ requires notification channels.
3. **Missing Permissions**: App needs notification permissions.
4. **Device Token Not Registered**: Backend needs your FCM token to send notifications.

---

## Complete Setup (Copy-Paste Ready)

### 1. Dependencies

**pubspec.yaml:**
```yaml
dependencies:
  firebase_core: ^2.24.2
  firebase_messaging: ^14.7.9
  flutter_local_notifications: ^16.3.0
  http: ^1.1.0
```

### 2. Complete Notification Service

**lib/services/notification_service.dart:**
```dart
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();
  final FirebaseMessaging _firebaseMessaging = FirebaseMessaging.instance;

  // Notification channel ID (MUST match backend: "tournament_channel")
  static const String _channelId = 'tournament_channel';
  static const String _channelName = 'Tournament Notifications';
  static const String _channelDescription = 'Notifications for tournaments, wallet updates, and more';

  Future<void> initialize() async {
    // Request permissions
    await _requestPermissions();

    // Initialize local notifications
    await _initializeLocalNotifications();

    // Create notification channel (Android)
    await _createNotificationChannel();

    // Get and register FCM token
    await _registerFCMToken();

    // Setup message handlers
    _setupMessageHandlers();
  }

  Future<void> _requestPermissions() async {
    NotificationSettings settings = await _firebaseMessaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );

    if (settings.authorizationStatus == AuthorizationStatus.authorized) {
      print('✅ User granted notification permission');
    } else if (settings.authorizationStatus == AuthorizationStatus.provisional) {
      print('⚠️ User granted provisional notification permission');
    } else {
      print('❌ User declined notification permission');
    }
  }

  Future<void> _initializeLocalNotifications() async {
    const AndroidInitializationSettings androidSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const DarwinInitializationSettings iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const InitializationSettings initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: (NotificationResponse response) {
        print('🔔 Notification tapped: ${response.payload}');
        _handleNotificationTap(response.payload);
      },
    );

    print('✅ Local notifications initialized');
  }

  Future<void> _createNotificationChannel() async {
    const AndroidNotificationChannel channel = AndroidNotificationChannel(
      _channelId,
      _channelName,
      description: _channelDescription,
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
      showBadge: true,
    );

    await _localNotifications
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(channel);

    print('✅ Notification channel created: $_channelId');
  }

  Future<void> _registerFCMToken() async {
    try {
      String? token = await _firebaseMessaging.getToken();
      if (token != null) {
        print('📱 FCM Token: $token');
        await _sendTokenToBackend(token);
      }

      // Listen for token refresh
      _firebaseMessaging.onTokenRefresh.listen((newToken) {
        print('🔄 FCM Token refreshed: $newToken');
        _sendTokenToBackend(newToken);
      });
    } catch (e) {
      print('❌ Error getting FCM token: $e');
    }
  }

  Future<void> _sendTokenToBackend(String token) async {
    try {
      // Replace with your backend URL and get Firebase ID token
      String? firebaseIdToken = await _getFirebaseIdToken(); // Implement this
      
      if (firebaseIdToken == null) {
        print('⚠️ Firebase ID token not available, skipping token registration');
        return;
      }

      final response = await http.post(
        Uri.parse('http://192.168.1.20:8080/api/users/device-token'),
        headers: {
          'Authorization': 'Bearer $firebaseIdToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'deviceToken': token}),
      );

      if (response.statusCode == 200) {
        print('✅ Device token registered with backend');
      } else {
        print('❌ Failed to register device token: ${response.statusCode}');
        print('Response: ${response.body}');
      }
    } catch (e) {
      print('❌ Error registering device token: $e');
    }
  }

  Future<String?> _getFirebaseIdToken() async {
    // Implement this based on your auth setup
    // Example: return await FirebaseAuth.instance.currentUser?.getIdToken();
    return null; // Replace with actual implementation
  }

  void _setupMessageHandlers() {
    // Handle foreground messages (app is open)
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('📱 Foreground notification received');
      print('Title: ${message.notification?.title}');
      print('Body: ${message.notification?.body}');
      print('Data: ${message.data}');

      _showLocalNotification(message);
    });

    // Handle notification taps when app is in background
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      print('🔔 App opened from background notification');
      _handleNotificationNavigation(message.data);
    });

    // Check if app was opened from notification (terminated state)
    _firebaseMessaging.getInitialMessage().then((RemoteMessage? message) {
      if (message != null) {
        print('🔔 App opened from terminated state notification');
        _handleNotificationNavigation(message.data);
      }
    });
  }

  Future<void> _showLocalNotification(RemoteMessage message) async {
    const AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
      _channelId,
      _channelName,
      channelDescription: _channelDescription,
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
      playSound: true,
      enableVibration: true,
      icon: '@mipmap/ic_launcher',
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const NotificationDetails notificationDetails = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _localNotifications.show(
      message.hashCode,
      message.notification?.title ?? 'New Notification',
      message.notification?.body ?? '',
      notificationDetails,
      payload: jsonEncode(message.data),
    );
  }

  void _handleNotificationTap(String? payload) {
    if (payload == null) return;

    try {
      Map<String, dynamic> data = jsonDecode(payload);
      String? type = data['type'];
      String? notificationId = data['notificationId'];

      print('🔔 Notification tapped - Type: $type, ID: $notificationId');

      // Navigate based on notification type
      // Example: Navigator.pushNamed(context, '/tournament', arguments: data);
    } catch (e) {
      print('❌ Error handling notification tap: $e');
    }
  }

  void _handleNotificationNavigation(Map<String, dynamic> data) {
    String? type = data['type'];
    String? notificationId = data['notificationId'];

    print('🔔 Navigating from notification - Type: $type, ID: $notificationId');

    // Implement navigation logic
    // Example:
    // if (type == 'tournament') {
    //   Navigator.pushNamed(context, '/tournament', arguments: data);
    // } else if (type == 'wallet') {
    //   Navigator.pushNamed(context, '/wallet');
    // }
  }
}
```

### 3. Background Message Handler

**lib/firebase_messaging_background.dart:**
```dart
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  print('📱 Background notification received: ${message.notification?.title}');

  const AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
    'tournament_channel',
    'Tournament Notifications',
    channelDescription: 'Notifications for tournaments and updates',
    importance: Importance.high,
    priority: Priority.high,
  );

  const NotificationDetails notificationDetails = NotificationDetails(
    android: androidDetails,
  );

  await flutterLocalNotificationsPlugin.show(
    message.hashCode,
    message.notification?.title ?? 'New Notification',
    message.notification?.body ?? '',
    notificationDetails,
  );
}
```

### 4. Initialize in main.dart

**lib/main.dart:**
```dart
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'firebase_messaging_background.dart';
import 'services/notification_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Firebase
  await Firebase.initializeApp();

  // Register background message handler
  FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);

  // Initialize notification service
  await NotificationService().initialize();

  runApp(MyApp());
}
```

### 5. Android Configuration

**android/app/src/main/AndroidManifest.xml:**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

  <application
    android:label="ESport Tournament"
    android:name="${applicationName}"
    android:icon="@mipmap/ic_launcher">
    
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
      android:resource="@android:color/white" />

    <activity
      android:name=".MainActivity"
      android:exported="true"
      android:launchMode="singleTop"
      android:theme="@style/LaunchTheme">
      <!-- ... -->
    </activity>
  </application>
</manifest>
```

### 6. iOS Configuration

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
    } else {
      let settings: UIUserNotificationSettings =
        UIUserNotificationSettings(types: [.alert, .badge, .sound], categories: nil)
      application.registerUserNotificationSettings(settings)
    }
    
    application.registerForRemoteNotifications()
    
    Messaging.messaging().delegate = self
    
    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
  
  override func application(_ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    Messaging.messaging().apnsToken = deviceToken
  }
}

extension AppDelegate: MessagingDelegate {
  func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
    print("Firebase registration token: \(String(describing: fcmToken))")
  }
}
```

---

## Testing Checklist

### Step 1: Verify FCM Token
- [ ] FCM token is printed in console
- [ ] Token is sent to backend (`/api/users/device-token`)
- [ ] Backend responds with 200 OK

### Step 2: Test Foreground Notifications
- [ ] App is open
- [ ] Send test notification from Firebase Console
- [ ] Notification appears as popup
- [ ] Notification has title and body
- [ ] Notification plays sound
- [ ] Notification vibrates (Android)

### Step 3: Test Background Notifications
- [ ] App is in background (not terminated)
- [ ] Send test notification
- [ ] Notification appears in notification tray
- [ ] Tap notification opens app
- [ ] App navigates to correct screen

### Step 4: Test Terminated State
- [ ] App is completely closed
- [ ] Send test notification
- [ ] Notification appears in notification tray
- [ ] Tap notification opens app
- [ ] App navigates to correct screen

### Step 5: Test Notification Types
- [ ] Tournament notification
- [ ] Wallet update notification
- [ ] System notification
- [ ] Tournament credentials notification
- [ ] Tournament reminder notification

---

## Troubleshooting

### Issue: Notifications not showing
**Check:**
1. FCM token is registered with backend
2. Notification permissions are granted
3. Notification channel is created (Android)
4. `flutter_local_notifications` is initialized
5. Backend is sending notifications (check logs)

### Issue: Notifications only work when app is open
**Solution:**
- Register background message handler
- Check AndroidManifest.xml configuration
- Verify iOS AppDelegate setup

### Issue: Notification channel ID mismatch
**Solution:**
- Backend uses: `tournament_channel`
- Flutter must use: `tournament_channel`
- Must match exactly!

### Issue: Device token not registered
**Solution:**
- Check network request to `/api/users/device-token`
- Verify Firebase ID token is valid
- Check backend logs for errors

---

## Quick Test

1. **Get FCM Token:**
```dart
String? token = await FirebaseMessaging.instance.getToken();
print('FCM Token: $token');
```

2. **Send Test Notification from Firebase Console:**
   - Go to Firebase Console > Cloud Messaging
   - Click "Send test message"
   - Enter FCM token
   - Send notification
   - Should appear as popup!

3. **Check Backend Logs:**
   - Look for: `✅ Successfully sent push notification`
   - Check for errors: `❌ Error sending push notification`

---

**Remember:** FCM only delivers the message. You MUST use `flutter_local_notifications` to show the popup!

