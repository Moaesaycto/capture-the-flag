import { useState, useEffect, useCallback } from 'react';

const API_URL = import.meta.env.VITE_BACKEND_URL + "/api/push";
const STORAGE_KEY = 'push_subscription';

// Check if running in standalone mode (added to home screen)
const isStandalone = (): boolean => {
    // Check both methods of detecting standalone mode
    const windowNav = navigator as any;
    return (
        ('standalone' in navigator && windowNav.standalone) ||
        window.matchMedia('(display-mode: standalone)').matches
    );
};

// Check if push notifications are supported on this browser
const isPushSupported = (): boolean => {
    /* console.log('feature detection:', {
        serviceWorker: 'serviceWorker' in navigator,
        pushManager: 'PushManager' in window,
        notification: 'Notification' in window,
    }); */

    // Basic feature detection - if these exist, the browser supports push
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        return false;
    }

    const ua = navigator.userAgent.toLowerCase();

    // Firefox for Android doesn't support web push
    if (ua.includes('firefox') && ua.includes('android')) {
        return false;
    }

    // For iOS, check if it's iOS 16.4 or higher AND in standalone mode
    // All browsers on iOS use WebKit, so this applies to Chrome, Firefox, Edge, etc. on iOS
    if (/iphone|ipad|ipod/.test(ua)) {
        const match = ua.match(/os (\d+)[._](\d+)?/);
        if (match) {
            const major = parseInt(match[1]);
            const minor = match[2] ? parseInt(match[2]) : 0;

            // iOS 16.4+ supports Web Push, but only in standalone mode
            if (major < 16) return false;
            if (major === 16 && minor < 4) return false;

            // Check if running as standalone (added to home screen)
            if (!isStandalone()) {
                return false;
            }
        }
    }

    return true;
};

// Check if Notification API is available
const isNotificationAPIAvailable = (): boolean => {
    return 'Notification' in window && typeof Notification.requestPermission === 'function';
};

export function usePushNotifications() {
    const [subscription, setSubscription] = useState<PushSubscription | null>(null);
    const [isSubscribing, setIsSubscribing] = useState(false);
    const [isSupported, setIsSupported] = useState(true);

    useEffect(() => {
        /* console.log('=== Push Support Check ===');
        console.log('ServiceWorker:', 'serviceWorker' in navigator);
        console.log('PushManager:', 'PushManager' in window);
        console.log('Standalone:', 'standalone' in navigator ? (navigator as any).standalone : 'N/A');
        console.log('Display Mode:', window.matchMedia('(display-mode: standalone)').matches ? 'standalone' : 'browser');
        console.log('User Agent:', navigator.userAgent); */

        const supported = isPushSupported();
        // console.log('isPushSupported result:', supported);

        if (!supported) {
            // console.log('Push not supported on this browser');
            setIsSupported(false);
            return;
        }

        // console.log('Push is supported, registering service worker...');
        const swPath = `${import.meta.env.BASE_URL}service-worker.js`;

        navigator.serviceWorker.register(swPath)
            .then(reg => {
                // console.log('Service worker registered successfully');
                return reg.update();
            })
            .then(() => restoreSubscription())
            .catch(err => {
                // console.error('Service worker registration failed:', err.message);
                void err;
                setIsSupported(false);
            });
    }, []);

    const restoreSubscription = async () => {
        try {
            const registration = await navigator.serviceWorker.ready;
            const existingSub = await registration.pushManager.getSubscription();
            if (existingSub) {
                setSubscription(existingSub);
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        } catch (error) {
            console.error('Failed to restore subscription:', error);
        }
    };

    const subscribe = useCallback(async () => {
        if (isSubscribing) {
            return;
        }

        // console.log('Subscribe called, isSupported:', isSupported);

        if (!isSupported) {
            const ua = navigator.userAgent.toLowerCase();
            const isIOS = /iphone|ipad|ipod/.test(ua);

            if (isIOS && !isStandalone()) {
                alert('On iOS, push notifications require adding this app to your Home Screen first.\n\n1. Tap the Share button\n2. Tap "Add to Home Screen"\n3. Open the app from your Home Screen\n4. Then enable notifications');
            } else {
                alert('Push notifications are not supported on this browser. Please try Chrome, Safari (iOS 16.4+), or Edge.');
            }
            return;
        }

        setIsSubscribing(true);

        try {
            // Request notification permission
            /* console.log('Checking Notification API...');
            console.log('Notification in window:', 'Notification' in window);
            console.log('requestPermission type:', typeof (window as any).Notification?.requestPermission); */

            if (!isNotificationAPIAvailable()) {
                throw new Error('Notification API not available on this browser');
            }

            // console.log('Requesting permission...');
            const permission = await Notification.requestPermission();
            // console.log('Permission result:', permission);

            if (permission !== 'granted') {
                alert('Notification permission was denied. Please enable it in your browser settings.');
                return;
            }

            // Get public key from backend
            const keyRes = await fetch(`${API_URL}/key`);
            if (!keyRes.ok) {
                throw new Error(`Failed to fetch public key: ${keyRes.status}`);
            }

            const keyData = await keyRes.json();
            const { publicKey } = keyData;

            if (!publicKey) {
                throw new Error('No public key in response');
            }

            // Wait for service worker with timeout
            const registration = await Promise.race([
                navigator.serviceWorker.ready,
                new Promise<never>((_, reject) =>
                    setTimeout(() => reject(new Error('Service worker timeout')), 10000)
                )
            ]);

            const applicationServerKey = urlBase64ToUint8Array(publicKey);

            const sub = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: applicationServerKey
            });

            const subscribeRes = await fetch(`${API_URL}/subscribe`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(sub.toJSON())
            });

            if (!subscribeRes.ok) {
                throw new Error(`Failed to send subscription to backend: ${subscribeRes.status}`);
            }

            setSubscription(sub);
            localStorage.setItem(STORAGE_KEY, 'true');

        } catch (error) {
            console.error('Subscribe error:', error);
            alert(`Subscription failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
        } finally {
            setIsSubscribing(false);
        }
    }, [isSubscribing, isSupported]);

    const unsubscribe = useCallback(async () => {
        try {
            const registration = await navigator.serviceWorker.ready;
            const existingSub = await registration.pushManager.getSubscription();

            if (existingSub) {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 5000);

                await Promise.all([
                    fetch(`${API_URL}/unsubscribe`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ endpoint: existingSub.endpoint }),
                        signal: controller.signal,
                        keepalive: true
                    }).catch(err => console.warn('Backend unsubscribe failed:', err))
                        .finally(() => clearTimeout(timeoutId)),
                    existingSub.unsubscribe()
                ]);

                setSubscription(null);
                localStorage.removeItem(STORAGE_KEY);
            }
        } catch (error) {
            console.error('Error unsubscribing:', error);
            setSubscription(null);
            localStorage.removeItem(STORAGE_KEY);
        }
    }, []);

    return { subscription, subscribe, unsubscribe, isSubscribing, isSupported };
}

// Helper to get user-friendly message for why push isn't supported
export const getPushUnsupportedMessage = (): string => {
    const ua = navigator.userAgent.toLowerCase();
    const isIOS = /iphone|ipad|ipod/.test(ua);

    if (isIOS && !isStandalone()) {
        return 'On iOS: Add this app to your Home Screen first, then enable notifications.';
    }

    if (ua.includes('firefox') && ua.includes('android')) {
        return 'Firefox Android does not support web push notifications. Please use Chrome or Edge.';
    }

    return 'Push notifications are not supported on this browser. Please use Chrome, Safari (iOS 16.4+), or Edge.';
};

function urlBase64ToUint8Array(base64String: string) {
    base64String = base64String.trim();
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
        .replace(/-/g, '+')
        .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }

    return outputArray;
}