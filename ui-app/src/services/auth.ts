import axios from 'axios';

const API_URL = 'http://localhost:8000/api';

export const refreshToken = async () => {
    try {
        const response = await axios.post(
            `${API_URL}/auth/refresh-token`,
            {},
            { withCredentials: true }
        );
        return response.data;
    } catch (error) {
        console.error('Error refreshing token:', error);
        throw error;
    }
};

export const setupTokenRefresh = () => {
    // Odświeżaj token co 15 minut
    setInterval(async () => {
        try {
            await refreshToken();
        } catch (error) {
            console.error('Failed to refresh token:', error);
            // Opcjonalnie: przekieruj do strony logowania
            window.location.href = '/login';
        }
    }, 15 * 60 * 1000); // 15 minut
};
