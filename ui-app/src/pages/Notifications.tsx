import { useState, useEffect } from "react";
import './Notifications.css';

function Notifications() {
    const [notifications, setNotifications] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);

    const fetchNotifications = async () => {
        setLoading(true);
        try {
            const response = await fetch("http://localhost:8000/api/notifications");
            if (!response.ok) throw new Error("Nie udało się pobrać powiadomień");
            const data = await response.json();
            setNotifications(data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const clearNotifications = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/notifications", {
                method: "DELETE",
            });
            if (response.ok) setNotifications([]);
        } catch (error) {
            console.error(error);
        }
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    return (
        <div className="notifications-container notifications-scrollable">
            <div className="notifications-header">
                <h1>Powiadomienia</h1>
                <div className="notifications-actions">
                    <button onClick={fetchNotifications} disabled={loading}>
                        {loading ? "Ładowanie..." : "Odśwież"}
                    </button>
                    <button onClick={clearNotifications} disabled={loading}>
                        Wyczyść powiadomienia
                    </button>
                </div>
            </div>
            <ul>
                {notifications.length > 0 ? (
                    notifications.map((notification, index) => (
                        <li key={index}>{notification}</li>
                    ))
                ) : (
                    <p>Brak powiadomień</p>
                )}
            </ul>
        </div>
    );
}

export default Notifications;
