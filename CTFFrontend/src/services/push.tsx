const API_URL = import.meta.env.VITE_BACKEND_URL + "/api/push";

export async function subscribeUser() {
    if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
        alert("Push notifications are not supported by your browser.");
        return null;
    }

    try {
        const permission = await Notification.requestPermission();

        if (permission !== 'granted') {
            return null;
        }

        const keyRes = await fetch(`${API_URL}/key`);

        if (!keyRes.ok) throw new Error(`Failed to fetch public key: ${keyRes.status}`);

        const { publicKey } = await keyRes.json();

        const registration = await navigator.serviceWorker.register('/service-worker.js');
        await navigator.serviceWorker.ready;

        const subscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(publicKey),
        });

        const subscribeRes = await fetch(`${API_URL}/subscribe`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(subscription.toJSON()),
            credentials: "include",
        });

        if (!subscribeRes.ok) {
            throw new Error(`Backend subscribe failed: ${subscribeRes.status}`);
        }

        return subscription;
    } catch (error) {
        throw error;
    }
}

export async function unsubscribeUser() {
    try {

        if (!("serviceWorker" in navigator)) return;

        const registration = await navigator.serviceWorker.getRegistration();
        if (!registration) return;

        const subscription = await registration.pushManager.getSubscription();
        if (subscription) {

            // const unsubscribed = await subscription.unsubscribe();
            // console.log('Local unsubscribe:', unsubscribed);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 5000);

            try {
                await fetch(`${API_URL}/unsubscribe`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(subscription.toJSON()),
                    credentials: "include",
                    signal: controller.signal
                });
            } catch (fetchError) {
            } finally {
                clearTimeout(timeoutId);
            }
        }
    } catch (error) {
    }
}

export async function getCurrentSubscription() {
    try {
        if (!("serviceWorker" in navigator)) return null;

        const registration = await navigator.serviceWorker.getRegistration();
        if (!registration) return null;

        return await registration.pushManager.getSubscription();
    } catch (error) {
        console.error('Error getting subscription:', error);
        return null;
    }
}

function urlBase64ToUint8Array(base64String: string) {
    try {
        base64String = base64String.trim();
        const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding)
            .replace(/-/g, "+")
            .replace(/_/g, "/");

        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);

        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }

        return outputArray;
    } catch (error) {
        console.error('Base64 conversion failed:', error);
        throw error;
    }
}