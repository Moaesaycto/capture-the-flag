self.addEventListener('push', (event) => {
    // console.log('Push received:', event);

    const data = event.data ? event.data.json() : { title: 'Notification', body: 'New message' };

    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then(clientList => {
                const isPageFocused = clientList.some(client => client.focused);
                if (!isPageFocused) {
                    return self.registration.showNotification(data.title, {
                        body: data.body,
                        icon: '/capture-the-flag/icon.png',
                        badge: '/capture-the-flag/badge.png',
                        tag: 'notification',
                        requireInteraction: false
                    });
                }
            })
    );
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    event.waitUntil(
        clients.openWindow(new URL('/', self.registration.scope).href)
    );
});